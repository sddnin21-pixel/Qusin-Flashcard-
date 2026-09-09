package com.example.data.gemini

data class WordLookupResult(
    val word: String,
    val partOfSpeech: String = "",
    val ipa: String = "",
    val pronunciation: String = "",
    val englishDefinition: String = "",
    val vietnameseTranslation: String = "",
    val cefr: String = "B1",
    val examples: List<String> = emptyList(),
    val exampleTranslations: List<String> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val collocations: List<String> = emptyList(),
    val wordFamily: List<String> = emptyList(),
    val commonMistakes: String = "",
    val memoryTip: String = ""
)

data class GeneratedCardItem(
    val front: String,
    val back: String,
    val translation: String = "",
    val example: String = "",
    val ipa: String = "",
    val pronunciation: String = "",
    val notes: String = "",
    val mnemonic: String = "",
    val tags: String = "",
    val difficulty: String = "Medium",
    var isSelected: Boolean = true
)

data class QuizQuestion(
    val id: String,
    val type: String, // MCQ, TRUE_FALSE, FILL_BLANK, TYPING, TRANSLATION, MATCHING
    val question: String,
    val options: List<String> = emptyList(),
    val correctAnswer: String,
    val explanation: String = "",
    val matchingPairs: Map<String, String> = emptyMap()
)

data class TutorMessage(
    val sender: String, // "user" or "tutor"
    val text: String,
    val corrections: String = "",
    val suggestedVocab: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)
