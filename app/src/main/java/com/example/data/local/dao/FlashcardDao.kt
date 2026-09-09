package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CardEntity
import com.example.data.local.entity.DeckEntity
import com.example.data.local.entity.ReviewLogEntity
import com.example.data.local.entity.StudySessionEntity
import com.example.data.local.entity.UserGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {
    // --- Decks ---
    @Query("SELECT * FROM decks ORDER BY updatedAt DESC")
    fun getAllDecks(): Flow<List<DeckEntity>>

    @Query("SELECT * FROM decks WHERE id = :deckId LIMIT 1")
    suspend fun getDeckById(deckId: Long): DeckEntity?

    @Query("SELECT * FROM decks WHERE id = :deckId LIMIT 1")
    fun getDeckByIdFlow(deckId: Long): Flow<DeckEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: DeckEntity): Long

    @Update
    suspend fun updateDeck(deck: DeckEntity)

    @Delete
    suspend fun deleteDeck(deck: DeckEntity)

    @Query("SELECT COUNT(*) FROM decks")
    fun getDeckCount(): Flow<Int>

    // --- Cards ---
    @Query("SELECT * FROM cards ORDER BY id DESC")
    fun getAllCards(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards ORDER BY id DESC")
    suspend fun getAllCardsSync(): List<CardEntity>

    @Query("SELECT * FROM cards WHERE deckId = :deckId OR additionalDeckIds LIKE '%' || :deckId || '%' ORDER BY id DESC")
    fun getCardsForDeck(deckId: Long): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE deckId = :deckId OR additionalDeckIds LIKE '%' || :deckId || '%' ORDER BY id DESC")
    suspend fun getCardsForDeckSync(deckId: Long): List<CardEntity>

    @Query("SELECT * FROM cards WHERE id = :cardId LIMIT 1")
    suspend fun getCardById(cardId: Long): CardEntity?

    @Query("SELECT * FROM cards WHERE id = :cardId LIMIT 1")
    fun getCardByIdFlow(cardId: Long): Flow<CardEntity?>

    @Query("SELECT * FROM cards WHERE nextReviewTimestamp <= :currentTime ORDER BY nextReviewTimestamp ASC")
    fun getDueCards(currentTime: Long): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE nextReviewTimestamp <= :currentTime ORDER BY nextReviewTimestamp ASC")
    suspend fun getDueCardsSync(currentTime: Long): List<CardEntity>

    @Query("SELECT * FROM cards WHERE (deckId = :deckId OR additionalDeckIds LIKE '%' || :deckId || '%') AND nextReviewTimestamp <= :currentTime ORDER BY nextReviewTimestamp ASC")
    fun getDueCardsForDeck(deckId: Long, currentTime: Long): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE state = 'NEW' ORDER BY id ASC")
    fun getNewCards(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoriteCards(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE lapses > 0 OR easeFactor < 2.1 OR difficulty = 'HARD' ORDER BY lapses DESC")
    fun getDifficultCards(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards ORDER BY updatedAt DESC LIMIT :limit")
    fun getRecentCards(limit: Int): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE front LIKE '%' || :query || '%' OR back LIKE '%' || :query || '%' OR translation LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%'")
    fun searchCards(query: String): Flow<List<CardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<CardEntity>): List<Long>

    @Update
    suspend fun updateCard(card: CardEntity)

    @Delete
    suspend fun deleteCard(card: CardEntity)

    @Query("DELETE FROM cards WHERE deckId = :deckId")
    suspend fun deleteCardsByDeck(deckId: Long)

    @Query("SELECT COUNT(*) FROM cards")
    fun getTotalCardCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM cards WHERE nextReviewTimestamp <= :currentTime")
    fun getDueCardCount(currentTime: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM cards WHERE state = 'NEW'")
    fun getNewCardCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM cards WHERE state = 'MATURE'")
    fun getMatureCardCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM cards WHERE state = 'LEARNING' OR state = 'RELEARNING'")
    fun getLearningCardCount(): Flow<Int>

    // --- Review Logs ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviewLog(log: ReviewLogEntity): Long

    @Query("SELECT * FROM review_logs ORDER BY reviewTimestamp DESC")
    fun getAllReviewLogs(): Flow<List<ReviewLogEntity>>

    @Query("SELECT * FROM review_logs ORDER BY reviewTimestamp DESC LIMIT :limit")
    fun getRecentReviewLogs(limit: Int): Flow<List<ReviewLogEntity>>

    @Query("SELECT * FROM review_logs WHERE reviewTimestamp >= :sinceTimestamp ORDER BY reviewTimestamp ASC")
    fun getReviewLogsSince(sinceTimestamp: Long): Flow<List<ReviewLogEntity>>

    @Query("SELECT * FROM review_logs WHERE reviewTimestamp >= :startOfDay ORDER BY reviewTimestamp DESC")
    fun getTodayReviewLogs(startOfDay: Long): Flow<List<ReviewLogEntity>>

    @Query("SELECT COUNT(*) FROM review_logs")
    fun getTotalReviewCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM review_logs WHERE reviewTimestamp >= :startOfDay")
    fun getTodayReviewCount(startOfDay: Long): Flow<Int>

    // --- Study Sessions ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudySession(session: StudySessionEntity): Long

    @Query("SELECT * FROM study_sessions ORDER BY timestamp DESC")
    fun getAllStudySessions(): Flow<List<StudySessionEntity>>

    @Query("SELECT SUM(durationSeconds) FROM study_sessions")
    fun getTotalStudySeconds(): Flow<Long?>

    @Query("SELECT SUM(durationSeconds) FROM study_sessions WHERE timestamp >= :startOfDay")
    fun getTodayStudySeconds(startOfDay: Long): Flow<Long?>

    // --- User Goals ---
    @Query("SELECT * FROM user_goals WHERE id = 1 LIMIT 1")
    fun getUserGoal(): Flow<UserGoalEntity?>

    @Query("SELECT * FROM user_goals WHERE id = 1 LIMIT 1")
    suspend fun getUserGoalSync(): UserGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUserGoal(goal: UserGoalEntity)

    // --- Reset / Purge ---
    @Query("DELETE FROM cards")
    suspend fun deleteAllCards()

    @Query("DELETE FROM decks")
    suspend fun deleteAllDecks()

    @Query("DELETE FROM review_logs")
    suspend fun deleteAllReviewLogs()

    @Query("DELETE FROM study_sessions")
    suspend fun deleteAllStudySessions()
}
