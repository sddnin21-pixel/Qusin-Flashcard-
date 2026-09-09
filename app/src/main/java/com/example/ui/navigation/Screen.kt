package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Decks : Screen("decks")
    object DeckDetail : Screen("deck_detail/{deckId}") {
        fun createRoute(deckId: Long) = "deck_detail/$deckId"
    }
    object Study : Screen("study?deckId={deckId}&mode={mode}") {
        fun createRoute(deckId: Long? = null, mode: String = "NORMAL"): String {
            return if (deckId != null) "study?deckId=$deckId&mode=$mode" else "study?mode=$mode"
        }
    }
    object Quiz : Screen("quiz?deckId={deckId}&quizType={quizType}") {
        fun createRoute(deckId: Long? = null, quizType: String = "MCQ"): String {
            return if (deckId != null) "quiz?deckId=$deckId&quizType=$quizType" else "quiz?quizType=$quizType"
        }
    }
    object AiHub : Screen("ai_hub")
    object WordLookup : Screen("word_lookup")
    object AiGenerator : Screen("ai_generator")
    object AiVisionOcr : Screen("ai_vision_ocr")
    object AiTutor : Screen("ai_tutor")
    object ConversationPractice : Screen("conversation_practice")
    object Pomodoro : Screen("pomodoro")
    object Music : Screen("music")
    object Statistics : Screen("statistics")
    object Settings : Screen("settings")
}
