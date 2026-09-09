package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.gemini.GeminiService
import com.example.data.local.entity.CardEntity
import com.example.data.local.entity.DeckEntity
import com.example.data.model.CardDifficulty
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardEditDialog(
    initialCard: CardEntity? = null,
    availableDecks: List<DeckEntity>,
    defaultDeckId: Long,
    geminiService: GeminiService,
    apiKey: String,
    selectedModel: String,
    onDismiss: () -> Unit,
    onSave: (CardEntity) -> Unit
) {
    var front by remember { mutableStateOf(initialCard?.front ?: "") }
    var back by remember { mutableStateOf(initialCard?.back ?: "") }
    var translation by remember { mutableStateOf(initialCard?.translation ?: "") }
    var example by remember { mutableStateOf(initialCard?.example ?: "") }
    var ipa by remember { mutableStateOf(initialCard?.ipa ?: "") }
    var pronunciation by remember { mutableStateOf(initialCard?.pronunciation ?: "") }
    var notes by remember { mutableStateOf(initialCard?.notes ?: "") }
    var mnemonic by remember { mutableStateOf(initialCard?.mnemonic ?: "") }
    var tags by remember { mutableStateOf(initialCard?.tags ?: "") }
    var difficulty by remember { mutableStateOf(initialCard?.difficulty ?: CardDifficulty.MEDIUM.name) }
    var selectedDeckId by remember { mutableStateOf(initialCard?.deckId ?: defaultDeckId) }

    var isAiLoading by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }
    var deckDropdownExpanded by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialCard == null) "Create Flashcard" else "Edit Flashcard",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Deck Selector
                if (availableDecks.isNotEmpty()) {
                    val currentDeck = availableDecks.find { it.id == selectedDeckId } ?: availableDecks.first()
                    ExposedDropdownMenuBox(
                        expanded = deckDropdownExpanded,
                        onExpandedChange = { deckDropdownExpanded = !deckDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = currentDeck.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Deck") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deckDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = deckDropdownExpanded,
                            onDismissRequest = { deckDropdownExpanded = false }
                        ) {
                            availableDecks.forEach { deck ->
                                DropdownMenuItem(
                                    text = { Text(deck.name) },
                                    onClick = {
                                        selectedDeckId = deck.id
                                        deckDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Front Term & AI Auto-fill Button
                OutlinedTextField(
                    value = front,
                    onValueChange = { front = it },
                    label = { Text("Front (Word / Term / Phrase) *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_input_front")
                )

                // Gemini AI Auto-fill trigger
                OutlinedButton(
                    onClick = {
                        if (front.isNotBlank()) {
                            isAiLoading = true
                            aiError = null
                            scope.launch {
                                val res = geminiService.lookupWord(front.trim(), apiKey, selectedModel)
                                isAiLoading = false
                                res.fold(
                                    onSuccess = { result ->
                                        if (back.isBlank()) back = result.englishDefinition
                                        if (translation.isBlank()) translation = result.vietnameseTranslation
                                        if (ipa.isBlank()) ipa = result.ipa
                                        if (pronunciation.isBlank()) pronunciation = result.pronunciation
                                        if (example.isBlank() && result.examples.isNotEmpty()) {
                                            example = result.examples.first()
                                        }
                                        if (mnemonic.isBlank()) mnemonic = result.memoryTip
                                        if (notes.isBlank() && result.commonMistakes.isNotBlank()) {
                                            notes = result.commonMistakes
                                        }
                                    },
                                    onFailure = { err ->
                                        aiError = err.localizedMessage ?: err.message
                                    }
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isAiLoading && front.isNotBlank()
                ) {
                    if (isAiLoading) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
                        Text("AI Generating Details...")
                    } else {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Auto-fill with Gemini AI")
                    }
                }

                if (aiError != null) {
                    Text(
                        text = aiError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Back Definition
                OutlinedTextField(
                    value = back,
                    onValueChange = { back = it },
                    label = { Text("Back (Definition / Meaning) *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_input_back"),
                    minLines = 2
                )

                // Translation
                OutlinedTextField(
                    value = translation,
                    onValueChange = { translation = it },
                    label = { Text("Vietnamese Translation") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Example
                OutlinedTextField(
                    value = example,
                    onValueChange = { example = it },
                    label = { Text("Example Sentence") },
                    modifier = Modifier.fillMaxWidth()
                )

                // IPA & Pronunciation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = ipa,
                        onValueChange = { ipa = it },
                        label = { Text("IPA (/.../)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pronunciation,
                        onValueChange = { pronunciation = it },
                        label = { Text("Phonetic Guide") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Mnemonic / Memory Tip
                OutlinedTextField(
                    value = mnemonic,
                    onValueChange = { mnemonic = it },
                    label = { Text("Mnemonic / Memory Tip") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Common Mistakes") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Tags
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags (comma-separated)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Difficulty selector
                Text(
                    text = "Difficulty",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    CardDifficulty.values().forEachIndexed { index, diff ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = CardDifficulty.values().size),
                            onClick = { difficulty = diff.name },
                            selected = difficulty == diff.name
                        ) {
                            Text(diff.label)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (front.isNotBlank() && back.isNotBlank()) {
                        val card = initialCard?.copy(
                            deckId = selectedDeckId,
                            front = front.trim(),
                            back = back.trim(),
                            translation = translation.trim(),
                            example = example.trim(),
                            ipa = ipa.trim(),
                            pronunciation = pronunciation.trim(),
                            notes = notes.trim(),
                            mnemonic = mnemonic.trim(),
                            tags = tags.trim(),
                            difficulty = difficulty,
                            updatedAt = System.currentTimeMillis()
                        ) ?: CardEntity(
                            deckId = selectedDeckId,
                            front = front.trim(),
                            back = back.trim(),
                            translation = translation.trim(),
                            example = example.trim(),
                            ipa = ipa.trim(),
                            pronunciation = pronunciation.trim(),
                            notes = notes.trim(),
                            mnemonic = mnemonic.trim(),
                            tags = tags.trim(),
                            difficulty = difficulty
                        )
                        onSave(card)
                    }
                },
                enabled = front.isNotBlank() && back.isNotBlank(),
                modifier = Modifier.testTag("save_card_button")
            ) {
                Text("Save Card")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
