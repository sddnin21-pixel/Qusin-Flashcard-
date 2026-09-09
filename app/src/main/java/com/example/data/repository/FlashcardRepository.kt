package com.example.data.repository

import com.example.data.local.dao.FlashcardDao
import com.example.data.local.entity.CardEntity
import com.example.data.local.entity.DeckEntity
import com.example.data.local.entity.ReviewLogEntity
import com.example.data.local.entity.StudySessionEntity
import com.example.data.local.entity.UserGoalEntity
import com.example.data.model.CardDifficulty
import com.example.data.model.CardState
import com.example.data.model.ReviewRating
import com.example.data.model.Sm2Engine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.StringReader
import java.util.Calendar
import java.util.concurrent.TimeUnit

class FlashcardRepository(private val dao: FlashcardDao) {

    // --- Decks ---
    val allDecks: Flow<List<DeckEntity>> = dao.getAllDecks()
    val totalDeckCount: Flow<Int> = dao.getDeckCount()

    suspend fun getDeckById(deckId: Long): DeckEntity? = withContext(Dispatchers.IO) {
        dao.getDeckById(deckId)
    }

    fun getDeckByIdFlow(deckId: Long): Flow<DeckEntity?> = dao.getDeckByIdFlow(deckId)

    suspend fun createDeck(
        name: String,
        description: String,
        colorHex: String = "#4F46E5",
        iconName: String = "book",
        tags: String = ""
    ): Long = withContext(Dispatchers.IO) {
        dao.insertDeck(
            DeckEntity(
                name = name.trim(),
                description = description.trim(),
                colorHex = colorHex,
                iconName = iconName,
                tags = tags.trim(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateDeck(deck: DeckEntity) = withContext(Dispatchers.IO) {
        dao.updateDeck(deck.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteDeck(deck: DeckEntity) = withContext(Dispatchers.IO) {
        dao.deleteCardsByDeck(deck.id)
        dao.deleteDeck(deck)
    }

    suspend fun duplicateDeck(deckId: Long): Long = withContext(Dispatchers.IO) {
        val original = dao.getDeckById(deckId) ?: return@withContext -1L
        val newDeckId = dao.insertDeck(
            original.copy(
                id = 0,
                name = "${original.name} (Copy)",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
        val originalCards = dao.getCardsForDeckSync(deckId)
        val copiedCards = originalCards.map { card ->
            card.copy(
                id = 0,
                deckId = newDeckId,
                state = CardState.NEW.name,
                repetitions = 0,
                intervalDays = 0,
                easeFactor = 2.5f,
                lapses = 0,
                nextReviewTimestamp = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
        dao.insertCards(copiedCards)
        newDeckId
    }

    // --- Cards ---
    val allCards: Flow<List<CardEntity>> = dao.getAllCards()
    val totalCardCount: Flow<Int> = dao.getTotalCardCount()
    val newCardCount: Flow<Int> = dao.getNewCardCount()
    val matureCardCount: Flow<Int> = dao.getMatureCardCount()
    val learningCardCount: Flow<Int> = dao.getLearningCardCount()
    val favoriteCards: Flow<List<CardEntity>> = dao.getFavoriteCards()
    val difficultCards: Flow<List<CardEntity>> = dao.getDifficultCards()

    fun getCardsForDeck(deckId: Long): Flow<List<CardEntity>> = dao.getCardsForDeck(deckId)

    suspend fun getCardsForDeckSync(deckId: Long): List<CardEntity> = withContext(Dispatchers.IO) {
        dao.getCardsForDeckSync(deckId)
    }

    fun getDueCards(currentTime: Long = System.currentTimeMillis()): Flow<List<CardEntity>> =
        dao.getDueCards(currentTime)

    suspend fun getDueCardsSync(currentTime: Long = System.currentTimeMillis()): List<CardEntity> =
        withContext(Dispatchers.IO) {
            dao.getDueCardsSync(currentTime)
        }

    fun getDueCardsForDeck(deckId: Long, currentTime: Long = System.currentTimeMillis()): Flow<List<CardEntity>> =
        dao.getDueCardsForDeck(deckId, currentTime)

    fun getDueCardCount(currentTime: Long = System.currentTimeMillis()): Flow<Int> =
        dao.getDueCardCount(currentTime)

    fun getRecentCards(limit: Int = 10): Flow<List<CardEntity>> = dao.getRecentCards(limit)

    fun searchCards(query: String): Flow<List<CardEntity>> = dao.searchCards(query)

    suspend fun getCardById(cardId: Long): CardEntity? = withContext(Dispatchers.IO) {
        dao.getCardById(cardId)
    }

    suspend fun createCard(card: CardEntity): Long = withContext(Dispatchers.IO) {
        dao.insertCard(card)
    }

    suspend fun createCards(cards: List<CardEntity>): List<Long> = withContext(Dispatchers.IO) {
        dao.insertCards(cards)
    }

    suspend fun updateCard(card: CardEntity) = withContext(Dispatchers.IO) {
        dao.updateCard(card.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteCard(card: CardEntity) = withContext(Dispatchers.IO) {
        dao.deleteCard(card)
    }

    suspend fun toggleFavorite(cardId: Long) = withContext(Dispatchers.IO) {
        val card = dao.getCardById(cardId) ?: return@withContext
        dao.updateCard(card.copy(isFavorite = !card.isFavorite, updatedAt = System.currentTimeMillis()))
    }

    // --- Spaced Repetition (SM-2 Processing) ---
    suspend fun processCardReview(
        cardId: Long,
        rating: ReviewRating,
        timeSpentSeconds: Int = 0
    ): CardEntity? = withContext(Dispatchers.IO) {
        val card = dao.getCardById(cardId) ?: return@withContext null
        val now = System.currentTimeMillis()

        // Calculate SM-2 update
        val sm2Result = Sm2Engine.calculate(
            currentRepetitions = card.repetitions,
            currentIntervalDays = card.intervalDays,
            currentEaseFactor = card.easeFactor,
            rating = rating,
            nowTimestamp = now
        )

        val updatedLapses = if (rating == ReviewRating.AGAIN) card.lapses + 1 else card.lapses

        val updatedCard = card.copy(
            state = sm2Result.newState.name,
            repetitions = sm2Result.newRepetitions,
            intervalDays = sm2Result.newIntervalDays,
            easeFactor = sm2Result.newEaseFactor,
            lapses = updatedLapses,
            lastReviewTimestamp = now,
            nextReviewTimestamp = sm2Result.nextReviewTimestamp,
            updatedAt = now
        )

        dao.updateCard(updatedCard)

        // Log the review event
        dao.insertReviewLog(
            ReviewLogEntity(
                cardId = cardId,
                rating = rating.value,
                reviewTimestamp = now,
                intervalDays = sm2Result.newIntervalDays,
                timeSpentSeconds = timeSpentSeconds
            )
        )

        // Update streaks and daily study record
        updateUserStudyStreak(now)

        updatedCard
    }

    private suspend fun updateUserStudyStreak(now: Long) {
        val goal = dao.getUserGoalSync() ?: UserGoalEntity()
        val lastCal = Calendar.getInstance().apply { timeInMillis = goal.lastStudyDayTimestamp }
        val nowCal = Calendar.getInstance().apply { timeInMillis = now }

        val isSameDay = lastCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                lastCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

        if (isSameDay) {
            // Already counted today
            return
        }

        // Check if yesterday
        val yesterdayCal = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val isConsecutive = lastCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
                lastCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)

        val newStreak = if (isConsecutive) goal.currentStreak + 1 else 1
        val newLongest = Math.max(goal.longestStreak, newStreak)

        dao.insertOrUpdateUserGoal(
            goal.copy(
                currentStreak = newStreak,
                longestStreak = newLongest,
                lastStudyDayTimestamp = now
            )
        )
    }

    suspend fun logStudySession(
        durationSeconds: Int,
        cardsReviewed: Int,
        mode: String = "NORMAL"
    ) = withContext(Dispatchers.IO) {
        dao.insertStudySession(
            StudySessionEntity(
                timestamp = System.currentTimeMillis(),
                durationSeconds = durationSeconds,
                cardsReviewed = cardsReviewed,
                mode = mode
            )
        )
        updateUserStudyStreak(System.currentTimeMillis())
    }

    // --- Statistics & Goals ---
    val userGoal: Flow<UserGoalEntity?> = dao.getUserGoal()
    val totalReviewCount: Flow<Int> = dao.getTotalReviewCount()
    val allReviewLogs: Flow<List<ReviewLogEntity>> = dao.getAllReviewLogs()
    val totalStudySeconds: Flow<Long?> = dao.getTotalStudySeconds()

    fun getTodayReviewCount(): Flow<Int> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return dao.getTodayReviewCount(cal.timeInMillis)
    }

    fun getTodayStudySeconds(): Flow<Long?> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return dao.getTodayStudySeconds(cal.timeInMillis)
    }

    suspend fun updateGoals(cardsGoal: Int, minutesGoal: Int) = withContext(Dispatchers.IO) {
        val current = dao.getUserGoalSync() ?: UserGoalEntity()
        dao.insertOrUpdateUserGoal(
            current.copy(
                dailyCardsGoal = cardsGoal,
                dailyMinutesGoal = minutesGoal
            )
        )
    }

    // --- Import / Export / Backup / Restore ---
    suspend fun exportCardsToJson(deckId: Long?): String = withContext(Dispatchers.IO) {
        val cards = if (deckId != null) dao.getCardsForDeckSync(deckId) else dao.getAllCardsSync()
        val jsonArray = JSONArray()
        for (card in cards) {
            val obj = JSONObject().apply {
                put("front", card.front)
                put("back", card.back)
                put("translation", card.translation)
                put("example", card.example)
                put("pronunciation", card.pronunciation)
                put("ipa", card.ipa)
                put("notes", card.notes)
                put("mnemonic", card.mnemonic)
                put("tags", card.tags)
                put("difficulty", card.difficulty)
            }
            jsonArray.put(obj)
        }
        jsonArray.toString(2)
    }

    suspend fun exportCardsToCsv(deckId: Long?): String = withContext(Dispatchers.IO) {
        val cards = if (deckId != null) dao.getCardsForDeckSync(deckId) else dao.getAllCardsSync()
        val sb = StringBuilder()
        sb.append("Front,Back,Translation,Example,IPA,Tags\n")
        for (card in cards) {
            sb.append("\"${escapeCsv(card.front)}\",")
            sb.append("\"${escapeCsv(card.back)}\",")
            sb.append("\"${escapeCsv(card.translation)}\",")
            sb.append("\"${escapeCsv(card.example)}\",")
            sb.append("\"${escapeCsv(card.ipa)}\",")
            sb.append("\"${escapeCsv(card.tags)}\"\n")
        }
        sb.toString()
    }

    suspend fun createFullBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())

        val decks = dao.getAllDecks()
        // Synchronous deck read
        val deckList = mutableListOf<DeckEntity>()
        // Let's get cards
        val cards = dao.getAllCardsSync()
        val cardArray = JSONArray()
        for (c in cards) {
            cardArray.put(JSONObject().apply {
                put("deckId", c.deckId)
                put("front", c.front)
                put("back", c.back)
                put("translation", c.translation)
                put("example", c.example)
                put("pronunciation", c.pronunciation)
                put("ipa", c.ipa)
                put("notes", c.notes)
                put("mnemonic", c.mnemonic)
                put("tags", c.tags)
                put("isFavorite", c.isFavorite)
                put("difficulty", c.difficulty)
                put("state", c.state)
                put("intervalDays", c.intervalDays)
                put("easeFactor", c.easeFactor)
                put("repetitions", c.repetitions)
                put("lapses", c.lapses)
            })
        }
        root.put("cards", cardArray)
        root.toString(2)
    }

    suspend fun parseCardsFromText(
        text: String,
        format: String, // JSON, CSV, TXT
        targetDeckId: Long
    ): List<CardEntity> = withContext(Dispatchers.Default) {
        val results = mutableListOf<CardEntity>()
        when (format.uppercase()) {
            "JSON" -> {
                try {
                    val array = JSONArray(text.trim())
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        results.add(
                            CardEntity(
                                deckId = targetDeckId,
                                front = obj.optString("front", ""),
                                back = obj.optString("back", ""),
                                translation = obj.optString("translation", ""),
                                example = obj.optString("example", ""),
                                pronunciation = obj.optString("pronunciation", ""),
                                ipa = obj.optString("ipa", ""),
                                notes = obj.optString("notes", ""),
                                mnemonic = obj.optString("mnemonic", ""),
                                tags = obj.optString("tags", ""),
                                difficulty = obj.optString("difficulty", CardDifficulty.MEDIUM.name)
                            )
                        )
                    }
                } catch (_: Exception) {}
            }
            "CSV" -> {
                try {
                    val reader = BufferedReader(StringReader(text))
                    var line: String?
                    var isFirst = true
                    while (reader.readLine().also { line = it } != null) {
                        if (line.isNullOrBlank()) continue
                        if (isFirst && (line!!.contains("Front", true) || line!!.contains("Term", true))) {
                            isFirst = false
                            continue
                        }
                        isFirst = false
                        val parts = line!!.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
                            .map { it.trim().removeSurrounding("\"") }
                        if (parts.isNotEmpty()) {
                            val front = parts.getOrNull(0) ?: ""
                            val back = parts.getOrNull(1) ?: ""
                            if (front.isNotBlank()) {
                                results.add(
                                    CardEntity(
                                        deckId = targetDeckId,
                                        front = front,
                                        back = back,
                                        translation = parts.getOrNull(2) ?: "",
                                        example = parts.getOrNull(3) ?: "",
                                        ipa = parts.getOrNull(4) ?: "",
                                        tags = parts.getOrNull(5) ?: ""
                                    )
                                )
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
            "TXT" -> {
                // Delimiter: tab or semicolon or colon or dash
                val lines = text.lines()
                for (line in lines) {
                    if (line.isBlank()) continue
                    val parts = when {
                        line.contains("\t") -> line.split("\t")
                        line.contains(";") -> line.split(";")
                        line.contains(" - ") -> line.split(" - ")
                        line.contains(":") -> line.split(":")
                        else -> emptyList()
                    }
                    if (parts.size >= 2) {
                        results.add(
                            CardEntity(
                                deckId = targetDeckId,
                                front = parts[0].trim(),
                                back = parts[1].trim(),
                                translation = parts.getOrNull(2)?.trim() ?: ""
                            )
                        )
                    }
                }
            }
        }
        results
    }

    suspend fun restoreBackup(jsonString: String, replace: Boolean) = withContext(Dispatchers.IO) {
        if (replace) {
            dao.deleteAllCards()
        }
        val root = JSONObject(jsonString)
        val cardsArray = root.optJSONArray("cards") ?: return@withContext
        val newCards = mutableListOf<CardEntity>()
        // default deck if needed
        val decks = dao.getAllDecks()
        for (i in 0 until cardsArray.length()) {
            val c = cardsArray.getJSONObject(i)
            newCards.add(
                CardEntity(
                    deckId = c.optLong("deckId", 1L),
                    front = c.optString("front", ""),
                    back = c.optString("back", ""),
                    translation = c.optString("translation", ""),
                    example = c.optString("example", ""),
                    pronunciation = c.optString("pronunciation", ""),
                    ipa = c.optString("ipa", ""),
                    notes = c.optString("notes", ""),
                    mnemonic = c.optString("mnemonic", ""),
                    tags = c.optString("tags", ""),
                    isFavorite = c.optBoolean("isFavorite", false),
                    difficulty = c.optString("difficulty", CardDifficulty.MEDIUM.name),
                    state = c.optString("state", CardState.NEW.name),
                    intervalDays = c.optInt("intervalDays", 0),
                    easeFactor = c.optDouble("easeFactor", 2.5).toFloat(),
                    repetitions = c.optInt("repetitions", 0),
                    lapses = c.optInt("lapses", 0)
                )
            )
        }
        if (newCards.isNotEmpty()) {
            dao.insertCards(newCards)
        }
    }

    suspend fun deleteAllData() = withContext(Dispatchers.IO) {
        dao.deleteAllCards()
        dao.deleteAllDecks()
        dao.deleteAllReviewLogs()
        dao.deleteAllStudySessions()
        dao.insertOrUpdateUserGoal(UserGoalEntity(id = 1, currentStreak = 0, longestStreak = 0))
    }

    suspend fun getAllCardsSync(): List<CardEntity> = withContext(Dispatchers.IO) {
        dao.getAllCardsSync()
    }

    fun getRecentReviewLogs(limit: Int): Flow<List<ReviewLogEntity>> = dao.getRecentReviewLogs(limit)

    fun getDifficultCards(limit: Int = 10): Flow<List<CardEntity>> = dao.getDifficultCards()

    suspend fun exportBackupJson(): String = createFullBackupJson()

    suspend fun exportDecksToCsv(): String = exportCardsToCsv(null)

    suspend fun importBackupJson(json: String): Int = withContext(Dispatchers.IO) {
        restoreBackup(json, replace = false)
        dao.getAllCardsSync().size
    }

    private fun escapeCsv(str: String): String {
        return str.replace("\"", "\"\"")
    }
}
