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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.gemini.GeminiService
import com.example.data.gemini.GeneratedCardItem
import com.example.data.local.entity.CardEntity
import com.example.data.repository.FlashcardRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.ui.components.ErrorStateView
import com.example.ui.components.LoadingStateView
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiGeneratorScreen(
    geminiService: GeminiService,
    repository: FlashcardRepository,
    preferences: UserPreferencesRepository,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val apiKey by preferences.apiKeyFlow.collectAsState()
    val model by preferences.modelFlow.collectAsState()
    val decks by repository.allDecks.collectAsState(initial = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) } // 0: From Topic, 1: From Text
    var topicInput by remember { mutableStateOf("IELTS Band 7 Academic Vocabulary") }
    var textInput by remember { mutableStateOf("") }
    var selectedCefr by remember { mutableStateOf("B2") }
    var selectedDifficulty by remember { mutableStateOf("Medium") }
    var selectedCount by remember { mutableIntStateOf(5) }
    var selectedCardType by remember { mutableStateOf("Standard (Word + Meaning + Example)") }

    var generatedCards = remember { mutableStateListOf<GeneratedCardItem>() }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var savedSuccessCount by remember { mutableStateOf<Int?>(null) }
    var selectedTargetDeckId by remember { mutableStateOf(decks.firstOrNull()?.id ?: 1L) }
    var deckDropdownExpanded by remember { mutableStateOf(false) }

    fun generateCards() {
        val input = if (selectedTab == 0) topicInput else textInput
        if (input.isBlank()) return
        isLoading = true
        errorMessage = null
        savedSuccessCount = null
        generatedCards.clear()

        scope.launch {
            val result = geminiService.generateFlashcards(
                topicOrInput = input.trim(),
                cefr = selectedCefr,
                difficulty = selectedDifficulty,
                count = selectedCount,
                languagePair = "English - Vietnamese",
                cardType = selectedCardType,
                apiKey = apiKey,
                model = model
            )
            isLoading = false
            result.fold(
                onSuccess = { list ->
                    generatedCards.addAll(list)
                },
                onFailure = { err ->
                    errorMessage = err.localizedMessage ?: err.message
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Flashcard Generator", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("From Topic") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("From Text / Article") })
            }

            if (isLoading) {
                LoadingStateView(message = "Gemini AI is generating flashcards...")
            } else if (errorMessage != null) {
                ErrorStateView(errorMessage = errorMessage ?: "", onRetry = { generateCards() })
            } else if (generatedCards.isNotEmpty()) {
                // Preview & Save Screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Generated Cards (${generatedCards.count { it.isSelected }}/${generatedCards.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        TextButton(onClick = {
                            val allSelected = generatedCards.all { it.isSelected }
                            generatedCards.forEachIndexed { i, c ->
                                generatedCards[i] = c.copy(isSelected = !allSelected)
                            }
                        }) {
                            Text(if (generatedCards.all { it.isSelected }) "Deselect All" else "Select All")
                        }
                    }

                    // Target Deck Selector
                    if (decks.isNotEmpty()) {
                        val currentDeck = decks.find { it.id == selectedTargetDeckId } ?: decks.first()
                        ExposedDropdownMenuBox(
                            expanded = deckDropdownExpanded,
                            onExpandedChange = { deckDropdownExpanded = !deckDropdownExpanded },
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            OutlinedTextField(
                                value = "Save to: ${currentDeck.name}",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deckDropdownExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = deckDropdownExpanded,
                                onDismissRequest = { deckDropdownExpanded = false }
                            ) {
                                decks.forEach { deck ->
                                    DropdownMenuItem(
                                        text = { Text(deck.name) },
                                        onClick = {
                                            selectedTargetDeckId = deck.id
                                            deckDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(generatedCards) { index, item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Checkbox(
                                        checked = item.isSelected,
                                        onCheckedChange = { checked ->
                                            generatedCards[index] = item.copy(isSelected = checked)
                                        }
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = item.front,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            if (item.ipa.isNotBlank()) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = item.ipa,
                                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.secondary)
                                                )
                                            }
                                        }
                                        Text(text = item.back, style = MaterialTheme.typography.bodyMedium)
                                        if (item.translation.isNotBlank()) {
                                            Text(text = item.translation, style = MaterialTheme.typography.bodySmall.copy(color = IndigoPrimary))
                                        }
                                        if (item.example.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(text = "\"${item.example}\"", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (savedSuccessCount != null) {
                        Text(
                            text = "Successfully saved $savedSuccessCount cards to deck! 🎉",
                            color = EmeraldSuccess,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { generatedCards.clear() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Discard")
                        }

                        Button(
                            onClick = {
                                val selected = generatedCards.filter { it.isSelected }
                                scope.launch {
                                    val entities = selected.map { c ->
                                        CardEntity(
                                            deckId = selectedTargetDeckId,
                                            front = c.front,
                                            back = c.back,
                                            translation = c.translation,
                                            example = c.example,
                                            ipa = c.ipa,
                                            pronunciation = c.pronunciation,
                                            notes = c.notes,
                                            mnemonic = c.mnemonic,
                                            tags = c.tags,
                                            difficulty = c.difficulty
                                        )
                                    }
                                    repository.createCards(entities)
                                    savedSuccessCount = entities.size
                                }
                            },
                            enabled = generatedCards.any { it.isSelected },
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save (${generatedCards.count { it.isSelected }})")
                        }
                    }
                }
            } else {
                // Generator Config Form
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (selectedTab == 0) {
                        OutlinedTextField(
                            value = topicInput,
                            onValueChange = { topicInput = it },
                            label = { Text("Topic or Subject *") },
                            placeholder = { Text("e.g. TOEFL iBT Academic Words, Restaurant Dialogues, Python Programming") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            label = { Text("Paste English Text / Article *") },
                            placeholder = { Text("Paste any English article, story or lecture text here...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4
                        )
                    }

                    // CEFR Level
                    Text("CEFR Level", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val cefrLevels = listOf("A1", "A2", "B1", "B2", "C1", "C2")
                        items(cefrLevels) { level ->
                            FilterChip(
                                selected = selectedCefr == level,
                                onClick = { selectedCefr = level },
                                label = { Text(level) }
                            )
                        }
                    }

                    // Number of cards
                    Text("Number of Cards", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val counts = listOf(3, 5, 10, 15, 20)
                        items(counts) { cnt ->
                            FilterChip(
                                selected = selectedCount == cnt,
                                onClick = { selectedCount = cnt },
                                label = { Text("$cnt cards") }
                            )
                        }
                    }

                    // Difficulty
                    Text("Difficulty", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val diffs = listOf("Easy", "Medium", "Hard")
                        items(diffs) { d ->
                            FilterChip(
                                selected = selectedDifficulty == d,
                                onClick = { selectedDifficulty = d },
                                label = { Text(d) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { generateCards() },
                        enabled = (if (selectedTab == 0) topicInput.isNotBlank() else textInput.isNotBlank()) && !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("generate_cards_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate Flashcards with Gemini AI", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
