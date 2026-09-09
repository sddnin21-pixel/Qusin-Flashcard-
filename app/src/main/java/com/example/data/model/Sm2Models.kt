package com.example.data.model

import java.util.concurrent.TimeUnit

enum class CardState {
    NEW,
    LEARNING,
    REVIEW,
    MATURE,
    RELEARNING
}

enum class CardDifficulty(val label: String) {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard")
}

enum class ReviewRating(val value: Int, val label: String) {
    AGAIN(1, "Again"),
    HARD(2, "Hard"),
    GOOD(3, "Good"),
    EASY(4, "Easy")
}

enum class StudyMode(val label: String, val description: String) {
    NORMAL("Flashcards", "Standard 3D card flip with SM-2 spaced repetition"),
    QUICK("Quick Review", "Rapid fire review of due cards"),
    CUSTOM("Custom Study", "Filtered study session"),
    QUIZ_MCQ("Multiple Choice", "Pick the correct definition or translation"),
    QUIZ_TRUE_FALSE("True / False", "Identify if card and definition match"),
    QUIZ_FILL_BLANK("Fill in Blank", "Complete the sentence with missing word"),
    QUIZ_TYPING("Typing Practice", "Type the word accurately from its meaning"),
    QUIZ_TRANSLATION("Translation", "Translate between target and native language"),
    QUIZ_MATCHING("Matching Pairs", "Match words with their corresponding definitions"),
    QUIZ_MIXED("Mixed Mode", "Dynamic combination of all quiz types")
}

object Sm2Engine {
    /**
     * Standard SM-2 Spaced Repetition Algorithm (SuperMemo 2 / Anki style)
     * Quality response q in 0..5:
     * - AGAIN = 1 (complete blackout / incorrect)
     * - HARD = 3 (correct response recalled with serious difficulty)
     * - GOOD = 4 (correct response after a hesitation)
     * - EASY = 5 (perfect response)
     */
    data class Sm2Result(
        val newIntervalDays: Int,
        val newRepetitions: Int,
        val newEaseFactor: Float,
        val newState: CardState,
        val nextReviewTimestamp: Long
    )

    fun calculate(
        currentRepetitions: Int,
        currentIntervalDays: Int,
        currentEaseFactor: Float,
        rating: ReviewRating,
        nowTimestamp: Long = System.currentTimeMillis()
    ): Sm2Result {
        val q = when (rating) {
            ReviewRating.AGAIN -> 1
            ReviewRating.HARD -> 3
            ReviewRating.GOOD -> 4
            ReviewRating.EASY -> 5
        }

        val newRepetitions: Int
        val newIntervalDays: Int
        val newState: CardState

        if (q < 3) {
            // Again / Failure
            newRepetitions = 0
            newIntervalDays = 1
            newState = if (currentIntervalDays > 1) CardState.RELEARNING else CardState.LEARNING
        } else {
            // Success: Hard, Good, or Easy
            if (currentRepetitions == 0) {
                newRepetitions = 1
                newIntervalDays = when (rating) {
                    ReviewRating.HARD -> 1
                    ReviewRating.GOOD -> 1
                    ReviewRating.EASY -> 4
                    else -> 1
                }
                newState = CardState.LEARNING
            } else if (currentRepetitions == 1) {
                newRepetitions = 2
                newIntervalDays = when (rating) {
                    ReviewRating.HARD -> 3
                    ReviewRating.GOOD -> 6
                    ReviewRating.EASY -> 8
                    else -> 6
                }
                newState = CardState.REVIEW
            } else {
                newRepetitions = currentRepetitions + 1
                val factor = when (rating) {
                    ReviewRating.HARD -> 1.2f
                    ReviewRating.GOOD -> currentEaseFactor
                    ReviewRating.EASY -> currentEaseFactor * 1.3f
                    else -> currentEaseFactor
                }
                newIntervalDays = Math.max(
                    currentIntervalDays + 1,
                    Math.round(currentIntervalDays * factor)
                )
                newState = if (newIntervalDays >= 21) CardState.MATURE else CardState.REVIEW
            }
        }

        // Calculate updated Ease Factor (SuperMemo formula: EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)))
        var updatedEase = currentEaseFactor + (0.1f - (5 - q) * (0.08f + (5 - q) * 0.02f))
        if (updatedEase < 1.3f) {
            updatedEase = 1.3f
        } else if (updatedEase > 3.0f) {
            updatedEase = 3.0f
        }

        val nextReviewTimestamp = nowTimestamp + TimeUnit.DAYS.toMillis(newIntervalDays.toLong())

        return Sm2Result(
            newIntervalDays = newIntervalDays,
            newRepetitions = newRepetitions,
            newEaseFactor = updatedEase,
            newState = newState,
            nextReviewTimestamp = nextReviewTimestamp
        )
    }
}
