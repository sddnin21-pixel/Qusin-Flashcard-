package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.gemini.GeminiService
import com.example.data.gemini.QuizQuestion
import com.example.data.local.entity.CardEntity
import com.example.data.repository.FlashcardRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.RoseDanger
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizStudyScreen(
    deckId: Long?,
    initialQuizType: String = "MCQ",
    repository: FlashcardRepository,
    geminiService: GeminiService,
    preferences: UserPreferencesRepository,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val allCards by repository.allCards.collectAsState(initial = emptyList())
    val apiKey by preferences.apiKeyFlow.collectAsState()
    val model by preferences.modelFlow.collectAsState()

    var currentQuizType by remember { mutableStateOf(initialQuizType) }
    var questions by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var typingInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    // Matching mode state
    val matchingSelections = remember { mutableStateMapOf<String, String>() }
    var activeMatchingLeft by remember { mutableStateOf<String?>(null) }

    // Function to generate questions either via AI or locally from cards
    fun loadQuestions(type: String) {
        isLoading = true
        currentIndex = 0
        score = 0
        selectedAnswer = null
        isSubmitted = false
        typingInput = ""
        matchingSelections.clear()

        scope.launch {
            val sourceCards = if (deckId != null) {
                repository.getCardsForDeckSync(deckId)
            } else {
                repository.getAllCardsSync()
            }

            if (sourceCards.isEmpty()) {
                questions = emptyList()
                isLoading = false
                return@launch
            }

            // Local fallback generation ensures 100% reliability offline
            val localQuestions = generateLocalQuiz(sourceCards, type)
            questions = localQuestions
            isLoading = false
        }
    }

    LaunchedEffect(currentQuizType, deckId) {
        loadQuestions(currentQuizType)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Practice Quiz", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        if (questions.isNotEmpty()) {
                            Text("Question ${currentIndex + 1} of ${questions.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close Quiz")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Quiz Mode Filter Chips (MCQ, True/False, Fill Blank, Typing, Translation, Matching, Mixed)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val types = listOf(
                    "MCQ" to "Multiple Choice",
                    "TRUE_FALSE" to "True / False",
                    "FILL_BLANK" to "Fill in Blank",
                    "TYPING" to "Typing",
                    "TRANSLATION" to "Translation",
                    "MATCHING" to "Matching Pairs",
                    "MIXED" to "Mixed Mode"
                )
                items(types) { (t, label) ->
                    FilterChip(
                        selected = currentQuizType == t,
                        onClick = { currentQuizType = t },
                        label = { Text(label) }
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = IndigoPrimary)
                }
            } else if (questions.isEmpty()) {
                EmptyStateView(
                    title = "Not enough cards for quiz",
                    subtitle = "Add at least 3 cards to this deck to generate a quiz.",
                    actionLabel = "Back",
                    onAction = onNavigateBack
                )
            } else if (currentIndex >= questions.size) {
                // Quiz Results Summary
                QuizResultsView(
                    totalQuestions = questions.size,
                    score = score,
                    onRetry = { loadQuestions(currentQuizType) },
                    onFinish = onNavigateBack
                )
            } else {
                val q = questions[currentIndex]
                val progress = (currentIndex.toFloat() / questions.size).coerceIn(0f, 1f)

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = IndigoPrimary
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        // Question Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = q.type,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = q.question,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 20.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Answer Options depending on Question Type
                        when (q.type) {
                            "MCQ", "TRUE_FALSE", "TRANSLATION" -> {
                                q.options.forEach { option ->
                                    val isChosen = selectedAnswer == option
                                    val isCorrect = option.equals(q.correctAnswer, ignoreCase = true)
                                    val btnColor = when {
                                        !isSubmitted -> if (isChosen) IndigoPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                                        isCorrect -> EmeraldSuccess.copy(alpha = 0.2f)
                                        isChosen && !isCorrect -> RoseDanger.copy(alpha = 0.2f)
                                        else -> MaterialTheme.colorScheme.surface
                                    }
                                    val borderColor = when {
                                        !isSubmitted -> if (isChosen) IndigoPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                        isCorrect -> EmeraldSuccess
                                        isChosen && !isCorrect -> RoseDanger
                                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable(enabled = !isSubmitted) {
                                                selectedAnswer = option
                                            },
                                        shape = RoundedCornerShape(14.dp),
                                        color = btnColor,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = option,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal
                                                ),
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isSubmitted && isCorrect) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldSuccess)
                                            } else if (isSubmitted && isChosen && !isCorrect) {
                                                Icon(Icons.Default.Close, contentDescription = null, tint = RoseDanger)
                                            }
                                        }
                                    }
                                }
                            }

                            "TYPING", "FILL_BLANK" -> {
                                OutlinedTextField(
                                    value = typingInput,
                                    onValueChange = { if (!isSubmitted) typingInput = it },
                                    label = { Text("Type the correct answer") },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isSubmitted,
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true
                                )

                                if (isSubmitted) {
                                    val isCorrect = typingInput.trim().equals(q.correctAnswer.trim(), ignoreCase = true)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (isCorrect) "Correct! 🎉" else "Correct answer: ${q.correctAnswer}",
                                        color = if (isCorrect) EmeraldSuccess else RoseDanger,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }

                            "MATCHING" -> {
                                Text("Select a word on the left, then its match on the right:", style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.height(8.dp))
                                val leftItems = q.matchingPairs.keys.toList()
                                val rightItems = q.matchingPairs.values.shuffled()

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    leftItems.forEach { leftKey ->
                                        val matchedRight = matchingSelections[leftKey]
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (activeMatchingLeft == leftKey) IndigoPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(enabled = !isSubmitted) {
                                                    activeMatchingLeft = leftKey
                                                }
                                        ) {
                                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(leftKey, fontWeight = FontWeight.Bold)
                                                Text(matchedRight ?: "(tap right to pair)", color = if (matchedRight != null) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Options:", style = MaterialTheme.typography.labelSmall)
                                    rightItems.forEach { rightVal ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(enabled = !isSubmitted && activeMatchingLeft != null) {
                                                    activeMatchingLeft?.let { key ->
                                                        matchingSelections[key] = rightVal
                                                        activeMatchingLeft = null
                                                    }
                                                }
                                        ) {
                                            Text(rightVal, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }

                        // Explanation Card after submission
                        if (isSubmitted && q.explanation.isNotBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Explanation & Context", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(q.explanation, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    // Bottom Submit or Next Button
                    Spacer(modifier = Modifier.height(16.dp))
                    if (!isSubmitted) {
                        Button(
                            onClick = {
                                isSubmitted = true
                                val isCorrect = when (q.type) {
                                    "TYPING", "FILL_BLANK" -> typingInput.trim().equals(q.correctAnswer.trim(), ignoreCase = true)
                                    "MATCHING" -> matchingSelections.all { (k, v) -> q.matchingPairs[k] == v }
                                    else -> selectedAnswer?.equals(q.correctAnswer, ignoreCase = true) == true
                                }
                                if (isCorrect) score++
                            },
                            enabled = when (q.type) {
                                "TYPING", "FILL_BLANK" -> typingInput.isNotBlank()
                                "MATCHING" -> matchingSelections.size == q.matchingPairs.size
                                else -> selectedAnswer != null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Check Answer", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                currentIndex++
                                isSubmitted = false
                                selectedAnswer = null
                                typingInput = ""
                                matchingSelections.clear()
                                activeMatchingLeft = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(if (currentIndex + 1 < questions.size) "Next Question" else "View Results", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun generateLocalQuiz(cards: List<CardEntity>, quizType: String): List<QuizQuestion> {
    val shuffled = cards.shuffled()
    val list = mutableListOf<QuizQuestion>()

    for (i in shuffled.indices) {
        val current = shuffled[i]
        val otherCards = shuffled.filter { it.id != current.id }
        val effectiveType = if (quizType == "MIXED") {
            val types = listOf("MCQ", "TRUE_FALSE", "FILL_BLANK", "TYPING", "TRANSLATION")
            types[i % types.size]
        } else quizType

        when (effectiveType) {
            "MCQ" -> {
                val wrongOptions = otherCards.map { it.back }.shuffled().take(3)
                val options = (wrongOptions + current.back).shuffled()
                list.add(
                    QuizQuestion(
                        id = "q_$i",
                        type = "MCQ",
                        question = "What is the meaning of \"${current.front}\"?",
                        options = options,
                        correctAnswer = current.back,
                        explanation = "${current.front} (${current.ipa}): ${current.back}. ${if (current.example.isNotBlank()) "Example: " + current.example else ""}"
                    )
                )
            }
            "TRUE_FALSE" -> {
                val isTrue = (i % 2 == 0)
                val shownDef = if (isTrue) current.back else (otherCards.firstOrNull()?.back ?: current.back)
                list.add(
                    QuizQuestion(
                        id = "q_$i",
                        type = "TRUE_FALSE",
                        question = "Does \"${current.front}\" mean:\n\n\"$shownDef\"?",
                        options = listOf("True", "False"),
                        correctAnswer = if (isTrue) "True" else "False",
                        explanation = "The true meaning of \"${current.front}\" is: ${current.back}"
                    )
                )
            }
            "FILL_BLANK" -> {
                val sentence = if (current.example.isNotBlank() && current.example.contains(current.front, ignoreCase = true)) {
                    current.example.replace(Regex("(?i)" + Regex.escape(current.front)), "_____")
                } else {
                    "Fill in the blank with the target word: \"_____ means ${current.back}\""
                }
                list.add(
                    QuizQuestion(
                        id = "q_$i",
                        type = "FILL_BLANK",
                        question = sentence,
                        correctAnswer = current.front,
                        explanation = "Missing word was: ${current.front}. Meaning: ${current.back}"
                    )
                )
            }
            "TYPING" -> {
                list.add(
                    QuizQuestion(
                        id = "q_$i",
                        type = "TYPING",
                        question = "Type the English word for:\n\n\"${if (current.translation.isNotBlank()) current.translation else current.back}\"",
                        correctAnswer = current.front,
                        explanation = "Word: ${current.front} (${current.ipa})"
                    )
                )
            }
            "TRANSLATION" -> {
                val promptText = if (current.translation.isNotBlank()) {
                    "Translate: \"${current.front}\""
                } else {
                    "Identify English word: \"${current.back}\""
                }
                val wrongTranslations = otherCards.map { if (it.translation.isNotBlank()) it.translation else it.back }.shuffled().take(3)
                val targetMeaning = if (current.translation.isNotBlank()) current.translation else current.back
                val options = (wrongTranslations + targetMeaning).shuffled()
                list.add(
                    QuizQuestion(
                        id = "q_$i",
                        type = "TRANSLATION",
                        question = promptText,
                        options = options,
                        correctAnswer = targetMeaning,
                        explanation = "${current.front} -> $targetMeaning"
                    )
                )
            }
            "MATCHING" -> {
                val pairCards = shuffled.take(4)
                val pairs = pairCards.associate { it.front to (if (it.translation.isNotBlank()) it.translation else it.back) }
                list.add(
                    QuizQuestion(
                        id = "q_match",
                        type = "MATCHING",
                        question = "Match words with their meanings:",
                        correctAnswer = "",
                        matchingPairs = pairs,
                        explanation = "Matching complete."
                    )
                )
                break // One matching question is enough per batch
            }
            else -> {
                val options = (otherCards.take(3).map { it.back } + current.back).shuffled()
                list.add(
                    QuizQuestion(
                        id = "q_$i",
                        type = "MCQ",
                        question = "What is \"${current.front}\"?",
                        options = options,
                        correctAnswer = current.back
                    )
                )
            }
        }
    }
    return list
}

@Composable
private fun QuizResultsView(
    totalQuestions: Int,
    score: Int,
    onRetry: () -> Unit,
    onFinish: () -> Unit
) {
    val percent = ((score.toFloat() / Math.max(1, totalQuestions)) * 100).toInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("quiz_results_view"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = if (percent >= 70) EmeraldSuccess.copy(alpha = 0.15f) else IndigoPrimary.copy(alpha = 0.15f),
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = if (percent >= 70) EmeraldSuccess else IndigoPrimary,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = if (percent >= 80) "Outstanding Job! 🎉" else if (percent >= 50) "Good Practice! 👍" else "Keep Practicing! 💪",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "You scored $score out of $totalQuestions ($percent%)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Done", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry Quiz")
        }
    }
}
