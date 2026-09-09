package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "review_logs")
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cardId: Long,
    val rating: Int, // 1=Again, 2=Hard, 3=Good, 4=Easy
    val reviewTimestamp: Long = System.currentTimeMillis(),
    val intervalDays: Int = 0,
    val timeSpentSeconds: Int = 0
)

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0,
    val cardsReviewed: Int = 0,
    val mode: String = "NORMAL"
)

@Entity(tableName = "user_goals")
data class UserGoalEntity(
    @PrimaryKey
    val id: Int = 1,
    val dailyCardsGoal: Int = 20,
    val dailyMinutesGoal: Int = 15,
    val newCardsPerDay: Int = 10,
    val currentStreak: Int = 1,
    val longestStreak: Int = 1,
    val lastStudyDayTimestamp: Long = System.currentTimeMillis()
)
