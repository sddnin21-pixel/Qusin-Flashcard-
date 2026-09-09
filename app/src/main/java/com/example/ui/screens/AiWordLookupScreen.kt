package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.TtsManager
import com.example.data.gemini.GeminiService
import com.example.data.gemini.WordLookupResult
import com.example.data.local.entity.CardEntity
import com.example.data.repository.FlashcardRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.ui.components.ErrorStateView
import com.example.ui.components.LoadingStateView
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiWordLookupScreen(
    geminiService: GeminiService,
    repository: FlashcardRepository,
    preferences: UserPreferencesRepository,
    ttsManager: TtsManager,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val apiKey by preferences.apiKeyFlow.collectAsState()
    val model by preferences.modelFlow.collectAsState()
    val decks by repository.allDecks.collectAsState(initial = emptyList())

    var searchInput by remember { mutableStateOf("") }
    var lookupResult by remember { mutableStateOf<WordLookupResult?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAddToDeckDialog by remember { mutableStateOf(false) }
    var savedSuccess by remember { mutableStateOf(false) }

    fun performLookup(query: String) {
        if (query.isBlank()) return
        isLoading = true
        errorMessage = null
        savedSuccess = false
        scope.launch {
            val result = geminiService.lookupWord(query.trim(), apiKey, model)
            isLoading = false
            result.fold(
                onSuccess = { lookupResult = it },
                onFailure = { errorMessage = it.localizedMessage ?: it.message }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Word Lookup", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
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
                .padding(16.dp)
        ) {
            // Search Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchInput,
                    onValueChange = { searchInput = it },
                    placeholder = { Text("Search word or idiom (e.g. Resilience, Ephemeral)...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_lookup_input"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { performLookup(searchInput) },
                    enabled = searchInput.isNotBlank() && !isLoading,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(56.dp)
                        .testTag("ai_lookup_button")
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                LoadingStateView(message = "Gemini AI is analyzing linguistic nuances & definitions...")
            } else if (errorMessage != null) {
                ErrorStateView(errorMessage = errorMessage ?: "", onRetry = { performLookup(searchInput) })
            } else if (lookupResult != null) {
                val item = lookupResult!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header Word Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.word,
                                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(onClick = { ttsManager.speak(item.word) }) {
                                        Icon(Icons.Default.VolumeUp, contentDescription = "Pronounce", tint = IndigoPrimary)
                                    }
                                }

                                if (item.cefr.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = item.cefr,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (item.ipa.isNotBlank()) {
                                    Text(
                                        text = item.ipa,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                if (item.partOfSpeech.isNotBlank()) {
                                    Text(
                                        text = "• ${item.partOfSpeech}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = item.englishDefinition,
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp)
                            )

                            if (item.vietnameseTranslation.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = item.vietnameseTranslation,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary),
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Examples
                    if (item.examples.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Example Sentences", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                Spacer(modifier = Modifier.height(8.dp))
                                item.examples.forEachIndexed { i, ex ->
                                    Text("• $ex", style = MaterialTheme.typography.bodyMedium)
                                    val tr = item.exampleTranslations.getOrNull(i)
                                    if (!tr.isNullOrBlank()) {
                                        Text("  $tr", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                    }
                                    if (i < item.examples.size - 1) Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                    }

                    // Memory Tip (Mnemonic)
                    if (item.memoryTip.isNotBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f))
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Memory Tip / Mnemonic", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(item.memoryTip, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }

                    // Synonyms & Antonyms
                    if (item.synonyms.isNotEmpty() || item.antonyms.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                if (item.synonyms.isNotEmpty()) {
                                    Text("Synonyms", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        item.synonyms.forEach { syn ->
                                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                                Text(syn, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                            }
                                        }
                                    }
                                }
                                if (item.antonyms.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Antonyms", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        item.antonyms.forEach { ant ->
                                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                                Text(ant, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Common Mistakes
                    if (item.commonMistakes.isNotBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Common Mistakes", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(item.commonMistakes, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }

                    // Add to Flashcards Button
                    Button(
                        onClick = { showAddToDeckDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("add_to_flashcards_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = if (savedSuccess) Icons.Default.Check else Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (savedSuccess) "Saved to Flashcards!" else "Add to Flashcard Deck")
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Enter any English word, idiom, or academic term above to generate detailed definitions, Vietnamese translations, IPA, mnemonics and examples.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Add to Deck Selection Dialog
    if (showAddToDeckDialog && lookupResult != null) {
        val item = lookupResult!!
        var selectedDeckId by remember { mutableStateOf(decks.firstOrNull()?.id ?: 1L) }
        var dropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddToDeckDialog = false },
            title = { Text("Choose Target Deck") },
            text = {
                Column {
                    Text("Select which deck to add \"${item.word}\" to:")
                    Spacer(modifier = Modifier.height(10.dp))
                    if (decks.isNotEmpty()) {
                        val currentDeck = decks.find { it.id == selectedDeckId } ?: decks.first()
                        ExposedDropdownMenuBox(
                            expanded = dropdownExpanded,
                            onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = currentDeck.name,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false }
                            ) {
                                decks.forEach { deck ->
                                    DropdownMenuItem(
                                        text = { Text(deck.name) },
                                        onClick = {
                                            selectedDeckId = deck.id
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Text("No decks available. Please create a deck first.")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val card = CardEntity(
                                deckId = selectedDeckId,
                                front = item.word,
                                back = item.englishDefinition,
                                translation = item.vietnameseTranslation,
                                example = item.examples.firstOrNull() ?: "",
                                ipa = item.ipa,
                                pronunciation = item.pronunciation,
                                notes = item.commonMistakes,
                                mnemonic = item.memoryTip,
                                tags = "${item.cefr},${item.partOfSpeech}"
                            )
                            repository.createCard(card)
                            savedSuccess = true
                            showAddToDeckDialog = false
                        }
                    },
                    enabled = decks.isNotEmpty()
                ) {
                    Text("Save to Deck")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddToDeckDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
