package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.sp
import com.example.data.gemini.GeminiService
import com.example.data.gemini.TutorMessage
import com.example.data.repository.UserPreferencesRepository
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationPracticeScreen(
    geminiService: GeminiService,
    preferences: UserPreferencesRepository,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val apiKey by preferences.apiKeyFlow.collectAsState()
    val model by preferences.modelFlow.collectAsState()

    val roles = listOf("Teacher", "Friend", "Interviewer", "IELTS Examiner", "Tourist", "Customer", "Coworker")
    var selectedRole by remember { mutableStateOf("Friend") }

    val messages = remember {
        mutableStateListOf(
            TutorMessage(
                sender = "tutor",
                text = "Hey there! Great to meet up. What have you been up to today?"
            )
        )
    }

    var textInput by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    fun resetConversation(newRole: String) {
        selectedRole = newRole
        messages.clear()
        val initialGreeting = when (newRole) {
            "Teacher" -> "Hello! Today we'll practice conversational English. How can I help you improve?"
            "Interviewer" -> "Welcome to the interview. Could you please introduce yourself and tell me about your background?"
            "IELTS Examiner" -> "Good morning. My name is the examiner. In this first part, I'd like to ask you some questions about your hometown."
            "Tourist" -> "Excuse me! Could you help me with directions to the central train station?"
            "Customer" -> "Hi, I'm looking for a gift for my friend's birthday. Could you recommend something?"
            "Coworker" -> "Hi, do you have five minutes to discuss the timeline for our upcoming project sprint?"
            else -> "Hey! Great to see you. How's everything going with you?"
        }
        messages.add(TutorMessage(sender = "tutor", text = initialGreeting))
    }

    fun sendMessage(query: String) {
        if (query.isBlank()) return
        messages.add(TutorMessage(sender = "user", text = query.trim()))
        textInput = ""
        isSending = true

        scope.launch {
            val result = geminiService.chatConversationPractice(
                role = selectedRole,
                userMessage = query.trim(),
                history = messages.toList(),
                apiKey = apiKey,
                model = model
            )
            isSending = false
            result.fold(
                onSuccess = { reply ->
                    messages.add(reply)
                },
                onFailure = { err ->
                    messages.add(
                        TutorMessage(
                            sender = "tutor",
                            text = "Connection error: ${err.localizedMessage ?: err.message}"
                        )
                    )
                }
            )
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Conversation Practice", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Role: $selectedRole with instant feedback", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // Role Selector Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(roles) { r ->
                    FilterChip(
                        selected = selectedRole == r,
                        onClick = { resetConversation(r) },
                        label = { Text(r) }
                    )
                }
            }

            // Message Stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { msg ->
                    val isUser = msg.sender == "user"
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isUser) 16.dp else 4.dp,
                                bottomEnd = if (isUser) 4.dp else 16.dp
                            ),
                            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.widthIn(max = 310.dp)
                        ) {
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        // Structured AI Feedback: Corrections & Useful Vocab
                        if (!isUser && msg.corrections.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Card(
                                modifier = Modifier.widthIn(max = 310.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Feedback & Nuance", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(msg.corrections, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)

                                    if (msg.suggestedVocab.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Useful vocab: ${msg.suggestedVocab.joinToString(", ")}", style = MaterialTheme.typography.labelSmall, color = IndigoPrimary, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                if (isSending) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = IndigoPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$selectedRole is responding...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Input Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Reply to $selectedRole...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("conversation_input"),
                        shape = RoundedCornerShape(20.dp),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { sendMessage(textInput) },
                        enabled = textInput.isNotBlank() && !isSending,
                        modifier = Modifier
                            .background(IndigoPrimary, CircleShape)
                            .testTag("conversation_send_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }
        }
    }
}
