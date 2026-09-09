package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.FlashcardDao
import com.example.data.local.entity.CardEntity
import com.example.data.local.entity.DeckEntity
import com.example.data.local.entity.ReviewLogEntity
import com.example.data.local.entity.StudySessionEntity
import com.example.data.local.entity.UserGoalEntity
import com.example.data.model.CardDifficulty
import com.example.data.model.CardState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        DeckEntity::class,
        CardEntity::class,
        ReviewLogEntity::class,
        StudySessionEntity::class,
        UserGoalEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun flashcardDao(): FlashcardDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "qusin_flashcards.db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        seedInitialData(database.flashcardDao())
                    }
                }
            }

            private suspend fun seedInitialData(dao: FlashcardDao) {
                // Seed Default User Goal
                dao.insertOrUpdateUserGoal(
                    UserGoalEntity(
                        id = 1,
                        dailyCardsGoal = 20,
                        dailyMinutesGoal = 15,
                        newCardsPerDay = 10,
                        currentStreak = 1,
                        longestStreak = 1,
                        lastStudyDayTimestamp = System.currentTimeMillis()
                    )
                )

                // Seed Deck 1: Oxford Essential English
                val deck1Id = dao.insertDeck(
                    DeckEntity(
                        name = "Oxford Essential Vocabulary",
                        description = "High-frequency academic and daily communication words with phonetic IPA and Vietnamese meanings.",
                        colorHex = "#4F46E5",
                        iconName = "school",
                        tags = "English,Vocabulary,Oxford,B2"
                    )
                )

                // Seed Deck 2: Daily Idioms & Colloquialisms
                val deck2Id = dao.insertDeck(
                    DeckEntity(
                        name = "Daily English Idioms",
                        description = "Natural expressions, collocations and conversational idioms used by native speakers.",
                        colorHex = "#0EA5E9",
                        iconName = "chat",
                        tags = "Idioms,Conversational,Fluency"
                    )
                )

                val now = System.currentTimeMillis()

                val sampleCardsDeck1 = listOf(
                    CardEntity(
                        deckId = deck1Id,
                        front = "Resilience",
                        back = "The capacity to recover quickly from difficulties; toughness.",
                        translation = "Khả năng phục hồi, sự kiên cường",
                        example = "Her emotional resilience helped her overcome severe setbacks in life.",
                        pronunciation = "ri-zil-yuhns",
                        ipa = "/rɪˈzɪl.jəns/",
                        notes = "Noun. Often used in psychology, business, and ecology.",
                        mnemonic = "Remember 're-silience' as silently springing back like rubber.",
                        tags = "Character,Psychology,B2",
                        difficulty = CardDifficulty.MEDIUM.name,
                        state = CardState.NEW.name,
                        nextReviewTimestamp = now
                    ),
                    CardEntity(
                        deckId = deck1Id,
                        front = "Eloquent",
                        back = "Fluent or persuasive in speaking or writing.",
                        translation = "Hùng biện, lưu loát, truyền cảm",
                        example = "The barrister gave an eloquent speech to the jury.",
                        pronunciation = "el-uh-kwuhnt",
                        ipa = "/ˈel.ə.kwənt/",
                        notes = "Adjective. Derived from Latin 'eloqui' (speak out).",
                        mnemonic = "Echoes like an elegant liquid flowing speech.",
                        tags = "Communication,Advanced,C1",
                        difficulty = CardDifficulty.MEDIUM.name,
                        state = CardState.NEW.name,
                        nextReviewTimestamp = now
                    ),
                    CardEntity(
                        deckId = deck1Id,
                        front = "Perseverance",
                        back = "Persistence in doing something despite difficulty or delay in achieving success.",
                        translation = "Sự kiên trì, bền bỉ",
                        example = "Through hard work and perseverance, she earned her medical degree.",
                        pronunciation = "pur-suh-veer-uhns",
                        ipa = "/ˌpɜː.sɪˈvɪə.rəns/",
                        notes = "Noun. Key virtue in academic learning.",
                        mnemonic = "Severing through all obstacles continuously.",
                        tags = "Virtue,Success",
                        difficulty = CardDifficulty.EASY.name,
                        state = CardState.NEW.name,
                        nextReviewTimestamp = now
                    ),
                    CardEntity(
                        deckId = deck1Id,
                        front = "Ambiguous",
                        back = "Open to more than one interpretation; having a double meaning.",
                        translation = "Mơ hồ, nhập nhằng",
                        example = "The legal treaty contained several ambiguous clauses.",
                        pronunciation = "am-big-yoo-uhs",
                        ipa = "/æmˈbɪɡ.ju.əs/",
                        notes = "Antonym: Clear, unequivocal, explicit.",
                        mnemonic = "Ambi (both sides) + guous: unclear which direction it leans.",
                        tags = "Logic,Academic,B2",
                        difficulty = CardDifficulty.HARD.name,
                        state = CardState.NEW.name,
                        nextReviewTimestamp = now
                    ),
                    CardEntity(
                        deckId = deck1Id,
                        front = "Ubiquitous",
                        back = "Present, appearing, or found everywhere.",
                        translation = "Phổ biến ở khắp mọi nơi",
                        example = "Smartphones have become ubiquitous across modern society.",
                        pronunciation = "yoo-bik-wi-tuhs",
                        ipa = "/juːˈbɪk.wɪ.təs/",
                        notes = "Synonym: Omnipresent, pervasive.",
                        mnemonic = "You-be-quite-everywhere!",
                        tags = "Technology,Society,C1",
                        difficulty = CardDifficulty.MEDIUM.name,
                        state = CardState.NEW.name,
                        nextReviewTimestamp = now
                    )
                )

                val sampleCardsDeck2 = listOf(
                    CardEntity(
                        deckId = deck2Id,
                        front = "Bite the bullet",
                        back = "To decide to do something difficult or unpleasant that one has been putting off.",
                        translation = "Cắn răng chịu đựng, chấp nhận khó khăn",
                        example = "I decided to bite the bullet and talk to my manager about the workload.",
                        pronunciation = "bahyt th-uh bool-it",
                        ipa = "/baɪt ðə ˈbʊl.ɪt/",
                        notes = "Originates from wounded soldiers biting lead bullets during battlefield surgery.",
                        mnemonic = "Biting on bullet to tolerate sudden pain.",
                        tags = "Idiom,Daily,Courage",
                        difficulty = CardDifficulty.EASY.name,
                        state = CardState.NEW.name,
                        nextReviewTimestamp = now
                    ),
                    CardEntity(
                        deckId = deck2Id,
                        front = "Under the weather",
                        back = "Slightly unwell or in low spirits.",
                        translation = "Cảm thấy không khỏe, hơi mệt",
                        example = "I was feeling under the weather yesterday, so I stayed home to rest.",
                        pronunciation = "uhn-der th-uh weth-er",
                        ipa = "/ˈʌn.dər ðə ˈweð.ər/",
                        notes = "Informal common spoken English phrase.",
                        mnemonic = "Sheltering under storm clouds feeling sick.",
                        tags = "Health,Daily,Spoken",
                        difficulty = CardDifficulty.EASY.name,
                        state = CardState.NEW.name,
                        nextReviewTimestamp = now
                    ),
                    CardEntity(
                        deckId = deck2Id,
                        front = "Burn the midnight oil",
                        back = "To study or work late into the night.",
                        translation = "Thức khuya làm việc hoặc học bài",
                        example = "Chris burned the midnight oil finishing his final year thesis.",
                        pronunciation = "burn th-uh mid-nahyt oyl",
                        ipa = "/bɜːn ðə ˈmɪd.naɪt ɔɪl/",
                        notes = "Historically refers to burning oil lamps to work at night.",
                        mnemonic = "Oil lamp burning late at 12 midnight.",
                        tags = "Study,Work,Idiom",
                        difficulty = CardDifficulty.MEDIUM.name,
                        state = CardState.NEW.name,
                        nextReviewTimestamp = now
                    )
                )

                dao.insertCards(sampleCardsDeck1)
                dao.insertCards(sampleCardsDeck2)
            }
        }
    }
}
