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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.DeckEntity
import com.example.data.repository.FlashcardRepository
import com.example.ui.components.EmptyStateView
import com.example.ui.i18n.LocalLanguage
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckListScreen(
    repository: FlashcardRepository,
    onNavigateToDeckDetail: (Long) -> Unit,
    onNavigateToStudy: (Long, String) -> Unit,
    onNavigateToQuiz: (Long) -> Unit
) {
    val decks by repository.allDecks.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val strings = LocalStrings.current
    val currentLanguage = LocalLanguage.current

    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("UPDATED") } // UPDATED, NAME, DUE
    var showCreateDeckDialog by remember { mutableStateOf(false) }
    var editingDeck by remember { mutableStateOf<DeckEntity?>(null) }
    var deletingDeck by remember { mutableStateOf<DeckEntity?>(null) }

    val filteredDecks = decks.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true) ||
                it.tags.contains(searchQuery, ignoreCase = true)
    }.sortedWith { d1, d2 ->
        when (sortBy) {
            "NAME" -> d1.name.compareTo(d2.name, ignoreCase = true)
            else -> d2.updatedAt.compareTo(d1.updatedAt)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = strings.navDecks,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    IconButton(onClick = {
                        sortBy = if (sortBy == "UPDATED") "NAME" else "UPDATED"
                    }) {
                        Icon(imageVector = Icons.Default.Sort, contentDescription = "Sort Decks")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDeckDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("create_deck_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = strings.createDeck)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("deck_search_field"),
                placeholder = { Text(strings.searchDecks) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            if (filteredDecks.isEmpty()) {
                EmptyStateView(
                    title = if (searchQuery.isNotBlank()) (if (currentLanguage == "vi") "Không tìm thấy bộ thẻ phù hợp" else "No matching decks found") else strings.noDecksYet,
                    subtitle = if (currentLanguage == "vi") "Tạo bộ thẻ đầu tiên để bắt đầu học tập và quản lý từ vựng." else "Create your first deck to organize your flashcards.",
                    actionLabel = strings.createDeck,
                    onAction = { showCreateDeckDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredDecks) { deck ->
                        DeckCardRow(
                            deck = deck,
                            repository = repository,
                            onOpen = { onNavigateToDeckDetail(deck.id) },
                            onStudy = { onNavigateToStudy(deck.id, "NORMAL") },
                            onQuiz = { onNavigateToQuiz(deck.id) },
                            onEdit = { editingDeck = deck },
                            onDuplicate = {
                                scope.launch { repository.duplicateDeck(deck.id) }
                            },
                            onDelete = { deletingDeck = deck }
                        )
                    }
                }
            }
        }
    }

    // Create / Edit Deck Dialog
    if (showCreateDeckDialog || editingDeck != null) {
        DeckEditDialog(
            deckToEdit = editingDeck,
            onDismiss = {
                showCreateDeckDialog = false
                editingDeck = null
            },
            onSave = { name, desc, color, icon, tags ->
                scope.launch {
                    if (editingDeck != null) {
                        repository.updateDeck(
                            editingDeck!!.copy(
                                name = name,
                                description = desc,
                                colorHex = color,
                                iconName = icon,
                                tags = tags
                            )
                        )
                    } else {
                        repository.createDeck(
                            name = name,
                            description = desc,
                            colorHex = color,
                            iconName = icon,
                            tags = tags
                        )
                    }
                    showCreateDeckDialog = false
                    editingDeck = null
                }
            }
        )
    }

    // Delete confirmation dialog
    if (deletingDeck != null) {
        AlertDialog(
            onDismissRequest = { deletingDeck = null },
            title = { Text(strings.deleteDeck) },
            text = { Text(if (currentLanguage == "vi") "Bạn có chắc chắn muốn xóa bộ thẻ '${deletingDeck?.name}' cùng toàn bộ thẻ bên trong?" else "Are you sure you want to delete '${deletingDeck?.name}' and all flashcards inside it?") },
            confirmButton = {
                Button(
                    onClick = {
                        deletingDeck?.let { deck ->
                            scope.launch { repository.deleteDeck(deck) }
                        }
                        deletingDeck = null
                    }
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingDeck = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun DeckCardRow(
    deck: DeckEntity,
    repository: FlashcardRepository,
    onOpen: () -> Unit,
    onStudy: () -> Unit,
    onQuiz: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val cards by repository.getCardsForDeck(deck.id).collectAsState(initial = emptyList())
    val dueCards by repository.getDueCardsForDeck(deck.id).collectAsState(initial = emptyList())
    var menuExpanded by remember { mutableStateOf(false) }
    val strings = LocalStrings.current
    val currentLanguage = LocalLanguage.current

    val deckColor = try {
        Color(android.graphics.Color.parseColor(deck.colorHex))
    } catch (_: Exception) {
        IndigoPrimary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .testTag("deck_item_${deck.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(deckColor.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = deckColor,
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
                                maxLines = 2
                            )
                        }
                    }
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(strings.editDeck) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (currentLanguage == "vi") "Nhân bản bộ thẻ" else "Duplicate Deck") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onDuplicate()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.deleteDeck, color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Card statistics & tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "${cards.size} ${strings.cardsUnit}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (dueCards.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${dueCards.size} ${strings.cardsDue}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Quick Action Buttons (Study & Quiz)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onStudy,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.studyButton)
                    }

                    OutlinedButton(
                        onClick = onQuiz,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.quizMode)
                    }
                }
            }
        }
    }
}

@Composable
fun DeckEditDialog(
    deckToEdit: DeckEntity? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, desc: String, color: String, icon: String, tags: String) -> Unit
) {
    val strings = LocalStrings.current
    val currentLanguage = LocalLanguage.current
    var name by remember { mutableStateOf(deckToEdit?.name ?: "") }
    var desc by remember { mutableStateOf(deckToEdit?.description ?: "") }
    var selectedColor by remember { mutableStateOf(deckToEdit?.colorHex ?: "#4F46E5") }
    var tags by remember { mutableStateOf(deckToEdit?.tags ?: "") }

    val presetColors = listOf("#4F46E5", "#0EA5E9", "#10B981", "#F59E0B", "#EF4444", "#8B5CF6", "#EC4899")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (deckToEdit == null) strings.createDeck else strings.editDeck) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("${strings.deckName} *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text(strings.deckDescription) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text(if (currentLanguage == "vi") "Thẻ phân loại (cách nhau dấu phẩy)" else "Tags (comma-separated)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(if (currentLanguage == "vi") "Màu đại diện bộ thẻ" else "Deck Color", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    presetColors.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(color, CircleShape)
                                .then(
                                    if (selectedColor == hex) {
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else Modifier
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), desc.trim(), selectedColor, "book", tags.trim())
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
