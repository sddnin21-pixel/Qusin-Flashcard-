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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.TtsManager
import com.example.data.local.entity.DeckEntity
import com.example.data.repository.FlashcardRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.ui.components.StatCard
import com.example.ui.i18n.LocalLanguage
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoDark
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.RoseDanger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: FlashcardRepository,
    preferences: UserPreferencesRepository,
    ttsManager: TtsManager,
    onNavigateToStudy: (Long?, String) -> Unit,
    onNavigateToDeck: (Long) -> Unit,
    onNavigateToDecks: () -> Unit,
    onNavigateToWordLookup: () -> Unit,
    onNavigateToPomodoro: () -> Unit,
    onNavigateToMusic: () -> Unit,
    onNavigateToAiHub: () -> Unit
) {
    val decks by repository.allDecks.collectAsState(initial = emptyList())
    val dueCards by repository.getDueCards().collectAsState(initial = emptyList())
    val newCardsCount by repository.newCardCount.collectAsState(initial = 0)
    val totalCardsCount by repository.totalCardCount.collectAsState(initial = 0)
    val userGoal by repository.userGoal.collectAsState(initial = null)
    val todayReviewCount by repository.getTodayReviewCount().collectAsState(initial = 0)
    val todayStudySeconds by repository.getTodayStudySeconds().collectAsState(initial = 0L)
    val recentCards by repository.getRecentCards(5).collectAsState(initial = emptyList())

    val streak = userGoal?.currentStreak ?: 1
    val dailyCardsGoal = userGoal?.dailyCardsGoal ?: 20
    val dailyMinutesGoal = userGoal?.dailyMinutesGoal ?: 15
    val minutesStudiedToday = ((todayStudySeconds ?: 0L) / 60).toInt()

    val strings = LocalStrings.current
    val currentLanguage = LocalLanguage.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = IndigoPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = IndigoPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Qusin Flashcard",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = strings.homeSubtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Study Streak Badge
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = AmberWarning.copy(alpha = 0.15f),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Study Streak",
                                tint = AmberWarning,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val streakUnit = if (currentLanguage == "vi") "ngày" else if (streak == 1) "day" else "days"
                            Text(
                                text = "$streak $streakUnit",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = AmberWarning
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("home_screen_scroll"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. DAILY GOAL PROGRESS HERO CARD
            item {
                DailyGoalHeroCard(
                    cardsDone = todayReviewCount,
                    cardsGoal = dailyCardsGoal,
                    minutesDone = minutesStudiedToday,
                    minutesGoal = dailyMinutesGoal,
                    dueCardsCount = dueCards.size,
                    onContinueStudy = {
                        val firstDeckWithCards = decks.firstOrNull()?.id
                        onNavigateToStudy(firstDeckWithCards, "NORMAL")
                    },
                    onQuickReview = {
                        onNavigateToStudy(null, "QUICK")
                    }
                )
            }

            // 2. KEY STATS ROW
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = strings.dueCards,
                        value = "${dueCards.size}",
                        subtitle = strings.waitingReview,
                        icon = Icons.Default.Schedule,
                        iconTint = RoseDanger,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToStudy(null, "NORMAL") }
                    )
                    StatCard(
                        title = strings.newCards,
                        value = "$newCardsCount",
                        subtitle = strings.toMemorize,
                        icon = Icons.Default.Bookmark,
                        iconTint = CyanAccent,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToStudy(null, "NORMAL") }
                    )
                    StatCard(
                        title = strings.today,
                        value = "$todayReviewCount",
                        subtitle = strings.reviewsDone,
                        icon = Icons.Default.CheckCircle,
                        iconTint = EmeraldSuccess,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3. QUICK AI & FOCUS TOOLS
            item {
                Text(
                    text = strings.quickTools,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        QuickToolChip(
                            title = strings.aiWordLookup,
                            icon = Icons.Default.Search,
                            tint = CyanAccent,
                            onClick = onNavigateToWordLookup
                        )
                    }
                    item {
                        QuickToolChip(
                            title = strings.aiCardGenerator,
                            icon = Icons.Default.AutoAwesome,
                            tint = IndigoPrimary,
                            onClick = onNavigateToAiHub
                        )
                    }
                    item {
                        QuickToolChip(
                            title = strings.pomodoroTimer,
                            icon = Icons.Default.Timer,
                            tint = RoseDanger,
                            onClick = onNavigateToPomodoro
                        )
                    }
                    item {
                        QuickToolChip(
                            title = strings.studyMusic,
                            icon = Icons.Default.MusicNote,
                            tint = AmberWarning,
                            onClick = onNavigateToMusic
                        )
                    }
                }
            }

            // 4. DECKS SECTION
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${strings.yourDecks} (${decks.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = strings.viewAll,
                        style = MaterialTheme.typography.labelMedium.copy(color = IndigoPrimary),
                        modifier = Modifier.clickable(onClick = onNavigateToDecks)
                    )
                }
            }

            if (decks.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(strings.noDecksYet)
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(onClick = onNavigateToDecks) {
                                Text(strings.createDeck)
                            }
                        }
                    }
                }
            } else {
                items(decks) { deck ->
                    DeckListItem(
                        deck = deck,
                        onOpen = { onNavigateToDeck(deck.id) },
                        onStudy = { onNavigateToStudy(deck.id, "NORMAL") }
                    )
                }
            }

            // 5. RECENT FLASHCARDS
            if (recentCards.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.recentlyAdded,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                items(recentCards) { card ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = card.front,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = card.back,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            IconButton(onClick = { ttsManager.speak(card.front) }) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Speak",
                                    tint = IndigoPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyGoalHeroCard(
    cardsDone: Int,
    cardsGoal: Int,
    minutesDone: Int,
    minutesGoal: Int,
    dueCardsCount: Int,
    onContinueStudy: () -> Unit,
    onQuickReview: () -> Unit
) {
    val strings = LocalStrings.current
    val cardProgress = (cardsDone.toFloat() / Math.max(1, cardsGoal)).coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_goal_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.dailyGoal,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "$cardsDone / $cardsGoal ${strings.cardsDoneToday}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (cardsDone >= cardsGoal) EmeraldSuccess.copy(alpha = 0.15f) else IndigoPrimary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${(cardProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (cardsDone >= cardsGoal) EmeraldSuccess else IndigoPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { cardProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (cardsDone >= cardsGoal) EmeraldSuccess else IndigoPrimary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons: Continue Studying & Quick Review
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onContinueStudy,
                    modifier = Modifier
                        .weight(1.2f)
                        .height(48.dp)
                        .testTag("continue_study_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.continueStudy, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onQuickReview,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("quick_review_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.quickReview)
                }
            }
        }
    }
}

@Composable
private fun QuickToolChip(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = Modifier.height(44.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@Composable
private fun DeckListItem(
    deck: DeckEntity,
    onOpen: () -> Unit,
    onStudy: () -> Unit
) {
    val strings = LocalStrings.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            try {
                                Color(android.graphics.Color.parseColor(deck.colorHex))
                            } catch (_: Exception) {
                                IndigoPrimary
                            }.copy(alpha = 0.15f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = try {
                            Color(android.graphics.Color.parseColor(deck.colorHex))
                        } catch (_: Exception) {
                            IndigoPrimary
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = deck.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (deck.description.isNotBlank()) {
                        Text(
                            text = deck.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }

            Button(
                onClick = onStudy,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(strings.studyButton)
            }
        }
    }
}
