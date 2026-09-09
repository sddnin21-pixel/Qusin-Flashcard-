package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.CardDifficulty
import com.example.data.model.CardState

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deckId: Long,
    val additionalDeckIds: String = "", // Comma-separated deck IDs e.g. "1,3,5"
    val front: String,
    val back: String,
    val translation: String = "",
    val example: String = "",
    val pronunciation: String = "",
    val ipa: String = "",
    val notes: String = "",
    val mnemonic: String = "",
    val tags: String = "", // Comma-separated tags
    val isFavorite: Boolean = false,
    val difficulty: String = CardDifficulty.MEDIUM.name, // EASY, MEDIUM, HARD
    val state: String = CardState.NEW.name, // NEW, LEARNING, REVIEW, MATURE, RELEARNING
    val intervalDays: Int = 0,
    val easeFactor: Float = 2.5f,
    val repetitions: Int = 0,
    val lapses: Int = 0,
    val lastReviewTimestamp: Long = 0L,
    val nextReviewTimestamp: Long = System.currentTimeMillis(), // New cards are due immediately
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
