package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.StudySoundSynthesizer
import com.example.audio.TtsManager
import com.example.data.local.entity.CardEntity
import com.example.data.model.ReviewRating
import com.example.data.repository.FlashcardRepository
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FlashcardFlipView
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun StudyScreen(
    deckId: Long?,
    mode: String = "NORMAL",
    repository: FlashcardRepository,
    ttsManager: TtsManager,
    soundSynthesizer: StudySoundSynthesizer,
    onNavigateBack: () -> Unit,
    onNavigateToMusic: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var studyCards by remember { mutableStateOf<List<CardEntity>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var sessionCompleted by remember { mutableStateOf(false) }
    var isSessionLoading by remember { mutableStateOf(true) }

    // Session Metrics
    val sessionStartTime = remember { System.currentTimeMillis() }
    var cardsReviewedCount by remember { mutableIntStateOf(0) }
    var successfulReviewsCount by remember { mutableIntStateOf(0) }
    var cardReviewStartTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Ambient music playing status
    val isMusicPlaying by soundSynthesizer.isPlaying.collectAsState()

    // Fetch cards for study based on mode and deckId
    LaunchedEffect(deckId, mode) {
        isSessionLoading = true
        val cards = if (deckId != null) {
            if (mode == "QUICK") {
                repository.getDueCardsSync().filter { it.deckId == deckId }
            } else {
                repository.getCardsForDeckSync(deckId)
            }
        } else {
            if (mode == "QUICK") {
                repository.getDueCardsSync()
            } else {
                val due = repository.getDueCardsSync()
                if (due.isNotEmpty()) due else repository.getAllCardsSync()
            }
        }
        studyCards = if (cards.isNotEmpty()) cards.shuffled() else emptyList()
        isSessionLoading = false
        cardReviewStartTime = System.currentTimeMillis()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (mode == "QUICK") "Quick Review" else "Study Session",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (studyCards.isNotEmpty()) {
                            Text(
                                text = "Card ${currentIndex + 1} of ${studyCards.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Exit Session")
                    }
                },
                actions = {
                    // Quick Ambient Music button
                    IconButton(onClick = onNavigateToMusic) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Study Music",
                            tint = if (isMusicPlaying) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isSessionLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading cards...", style = MaterialTheme.typography.bodyMedium)
                }
            } else if (studyCards.isEmpty()) {
                EmptyStateView(
                    title = "No cards due for review! 🎉",
                    subtitle = "All caught up with your spaced repetition schedule.",
                    actionLabel = "Back to Home",
                    onAction = onNavigateBack
                )
            } else if (currentIndex >= studyCards.size) {
                // Session Completed Dialog / View
                SessionCompletedView(
                    reviewedCount = cardsReviewedCount,
                    accuracyPercent = if (cardsReviewedCount > 0) ((successfulReviewsCount.toFloat() / cardsReviewedCount) * 100).toInt() else 100,
                    durationSeconds = ((System.currentTimeMillis() - sessionStartTime) / 1000).toInt(),
                    onFinish = onNavigateBack,
                    onReviewAgain = {
                        currentIndex = 0
                        isFlipped = false
                        cardsReviewedCount = 0
                        successfulReviewsCount = 0
                        studyCards = studyCards.shuffled()
                    }
                )
            } else {
                val currentCard = studyCards[currentIndex]

                Column(modifier = Modifier.fillMaxSize()) {
                    // Progress Bar at the top of study
                    val progress = (currentIndex.toFloat() / studyCards.size).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = IndigoPrimary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    // Card Flip Area with horizontal swipe detection
                    var dragOffset by remember { mutableStateOf(0f) }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .pointerInput(currentCard.id) {
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        if (dragOffset < -150f) {
                                            // Swiped Left -> AGAIN
                                            if (isFlipped) {
                                                handleCardRating(
                                                    card = currentCard,
                                                    rating = ReviewRating.AGAIN,
                                                    timeSpent = ((System.currentTimeMillis() - cardReviewStartTime) / 1000).toInt(),
                                                    repository = repository,
                                                    scope = scope,
                                                    onAdvance = {
                                                        cardsReviewedCount++
                                                        isFlipped = false
                                                        currentIndex++
                                                        cardReviewStartTime = System.currentTimeMillis()
                                                    }
                                                )
                                            } else {
                                                isFlipped = true
                                            }
                                        } else if (dragOffset > 150f) {
                                            // Swiped Right -> GOOD
                                            if (isFlipped) {
                                                handleCardRating(
                                                    card = currentCard,
                                                    rating = ReviewRating.GOOD,
                                                    timeSpent = ((System.currentTimeMillis() - cardReviewStartTime) / 1000).toInt(),
                                                    repository = repository,
                                                    scope = scope,
                                                    onAdvance = {
                                                        cardsReviewedCount++
                                                        successfulReviewsCount++
                                                        isFlipped = false
                                                        currentIndex++
                                                        cardReviewStartTime = System.currentTimeMillis()
                                                    }
                                                )
                                            } else {
                                                isFlipped = true
                                            }
                                        }
                                        dragOffset = 0f
                                    },
                                    onHorizontalDrag = { _, dragAmount ->
                                        dragOffset += dragAmount
                                    }
                                )
                            }
                    ) {
                        AnimatedContent(
                            targetState = currentCard,
                            transitionSpec = {
                                slideInHorizontally { width -> width } + fadeIn() togetherWith
                                        slideOutHorizontally { width -> -width } + fadeOut()
                            },
                            label = "card_transition"
                        ) { card ->
                            FlashcardFlipView(
                                card = card,
                                isFlipped = isFlipped,
                                onFlip = { isFlipped = !isFlipped },
                                onRatingSelected = { rating ->
                                    val timeSpent = ((System.currentTimeMillis() - cardReviewStartTime) / 1000).toInt()
                                    handleCardRating(
                                        card = card,
                                        rating = rating,
                                        timeSpent = timeSpent,
                                        repository = repository,
                                        scope = scope,
                                        onAdvance = {
                                            cardsReviewedCount++
                                            if (rating == ReviewRating.GOOD || rating == ReviewRating.EASY) {
                                                successfulReviewsCount++
                                            }
                                            isFlipped = false
                                            currentIndex++
                                            cardReviewStartTime = System.currentTimeMillis()
                                        }
                                    )
                                },
                                onSpeak = { text -> ttsManager.speak(text) },
                                onToggleFavorite = {
                                    scope.launch { repository.toggleFavorite(card.id) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun handleCardRating(
    card: CardEntity,
    rating: ReviewRating,
    timeSpent: Int,
    repository: FlashcardRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    onAdvance: () -> Unit
) {
    scope.launch {
        repository.processCardReview(
            cardId = card.id,
            rating = rating,
            timeSpentSeconds = timeSpent
        )
        onAdvance()
    }
}

@Composable
private fun SessionCompletedView(
    reviewedCount: Int,
    accuracyPercent: Int,
    durationSeconds: Int,
    onFinish: () -> Unit,
    onReviewAgain: () -> Unit
) {
    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("session_completed_view"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = EmeraldSuccess.copy(alpha = 0.15f),
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = EmeraldSuccess,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Session Complete! 🌟",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your spaced repetition memory schedule has been updated.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$reviewedCount",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Reviewed",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$accuracyPercent%",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSuccess
                        )
                    )
                    Text(
                        text = "Accuracy",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${minutes}m ${seconds}s",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Study Time",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

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
            onClick = onReviewAgain,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Review Again")
        }
    }
}
