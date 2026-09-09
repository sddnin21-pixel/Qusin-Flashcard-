package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CardEntity
import com.example.data.repository.FlashcardRepository
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.RoseDanger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    repository: FlashcardRepository,
    onNavigateBack: () -> Unit,
    onNavigateToStudyCard: () -> Unit
) {
    val cards by repository.allCards.collectAsState(initial = emptyList())
    val userGoal by repository.userGoal.collectAsState(initial = null)
    val reviewLogs by repository.getRecentReviewLogs(10).collectAsState(initial = emptyList())
    val difficultCards by repository.getDifficultCards(10).collectAsState(initial = emptyList())
    val todayStudySeconds by repository.getTodayStudySeconds().collectAsState(initial = 0L)

    val totalCount = cards.size
    val matureCount = cards.count { it.state == "MATURE" || it.intervalDays >= 21 }
    val learningCount = cards.count { it.state == "LEARNING" || it.state == "REVIEW" }
    val newCount = cards.count { it.state == "NEW" }
    val dueCount = cards.count { it.nextReviewTimestamp <= System.currentTimeMillis() }

    val retentionRate = if (totalCount > 0) {
        val successfulCards = cards.count { it.lapses == 0 && it.repetitions > 0 }
        val reviewedCards = cards.count { it.repetitions > 0 }
        if (reviewedCards > 0) ((successfulCards.toFloat() / reviewedCards) * 100).toInt() else 85
    } else 100

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stats & Memory Retention", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("stats_scroll_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Highlight Metrics
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricHighlightBox(
                        title = "Retention Rate",
                        value = "$retentionRate%",
                        icon = Icons.Default.Psychology,
                        tint = EmeraldSuccess,
                        modifier = Modifier.weight(1f)
                    )
                    MetricHighlightBox(
                        title = "Study Streak",
                        value = "${userGoal?.currentStreak ?: 1} Days",
                        icon = Icons.Default.LocalFireDepartment,
                        tint = AmberWarning,
                        modifier = Modifier.weight(1f)
                    )
                    MetricHighlightBox(
                        title = "Total Time",
                        value = "${((todayStudySeconds ?: 0L) / 60)} mins",
                        icon = Icons.Default.Timer,
                        tint = IndigoPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Memory Stage Breakdown
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "SM-2 Spaced Repetition Breakdown",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        StageProgressBar(label = "Mature Cards (Retained > 21d)", count = matureCount, total = totalCount, color = EmeraldSuccess)
                        Spacer(modifier = Modifier.height(8.dp))
                        StageProgressBar(label = "Learning / In Review", count = learningCount, total = totalCount, color = IndigoPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        StageProgressBar(label = "New Cards Unstudied", count = newCount, total = totalCount, color = CyanAccent)
                        Spacer(modifier = Modifier.height(8.dp))
                        StageProgressBar(label = "Cards Due Today", count = dueCount, total = totalCount, color = RoseDanger)
                    }
                }
            }

            // Difficult Cards Section
            if (difficultCards.isNotEmpty()) {
                item {
                    Text(
                        text = "Needs Practice (${difficultCards.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                items(difficultCards) { card ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RoseDanger.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(card.front, fontWeight = FontWeight.Bold)
                                Text(card.back, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(shape = RoundedCornerShape(6.dp), color = RoseDanger.copy(alpha = 0.15f)) {
                                Text(
                                    text = "${card.lapses} lapses",
                                    style = MaterialTheme.typography.labelSmall.copy(color = RoseDanger, fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Recent Review Logs
            if (reviewLogs.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Review History",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                items(reviewLogs) { log ->
                    val dateFormatted = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(log.reviewTimestamp))
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Rating: ${log.rating}", fontWeight = FontWeight.Bold)
                                Text(dateFormatted, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Text(
                                text = "Interval: ${log.intervalDays}d",
                                style = MaterialTheme.typography.labelSmall.copy(color = IndigoPrimary)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricHighlightBox(
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
    }
}

@Composable
private fun StageProgressBar(
    label: String,
    count: Int,
    total: Int,
    color: Color
) {
    val progress = if (total > 0) (count.toFloat() / total).coerceIn(0f, 1f) else 0f
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text("$count ($count/$total)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
