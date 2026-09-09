package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.TtsManager
import com.example.data.gemini.GeminiService
import com.example.data.repository.FlashcardRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.notifications.StudyNotificationManager
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.RoseDanger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: UserPreferencesRepository,
    geminiService: GeminiService,
    repository: FlashcardRepository,
    ttsManager: TtsManager,
    notificationManager: StudyNotificationManager,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = LocalStrings.current

    val currentLanguage by preferences.languageFlow.collectAsState()
    val currentApiKey by preferences.apiKeyFlow.collectAsState()
    val currentModel by preferences.modelFlow.collectAsState()
    val dailyReminderEnabled by preferences.dailyReminderEnabledFlow.collectAsState()
    val reminderHour by preferences.reminderHourFlow.collectAsState()
    val ttsSpeed by preferences.ttsSpeedFlow.collectAsState()

    var inputApiKey by remember(currentApiKey) { mutableStateOf(currentApiKey) }
    var showApiKey by remember { mutableStateOf(false) }
    var selectedModel by remember(currentModel) { mutableStateOf(currentModel) }
    var modelDropdownExpanded by remember { mutableStateOf(false) }

    var isTestingConnection by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testSuccess by remember { mutableStateOf(false) }

    var importStatus by remember { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val stream = context.contentResolver.openInputStream(uri)
                    val content = stream?.bufferedReader()?.use { it.readText() } ?: ""
                    stream?.close()
                    val count = repository.importBackupJson(content)
                    withContext(Dispatchers.Main) {
                        importStatus = if (currentLanguage == "vi") "Đã khôi phục thành công $count thẻ!" else "Successfully restored $count cards from backup!"
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        importStatus = "Error: ${e.message}"
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.settingsTitle, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .testTag("settings_scroll_column"),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 0. Language Selector Section (Bilingual Vietnamese / English)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_language_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Translate, contentDescription = null, tint = IndigoPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.languageSection, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    Text(
                        strings.languageDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Vietnamese
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    scope.launch { preferences.setLanguage("vi") }
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = if (currentLanguage == "vi") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = if (currentLanguage == "vi") androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("🇻🇳", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Tiếng Việt",
                                    fontWeight = if (currentLanguage == "vi") FontWeight.Bold else FontWeight.Medium,
                                    color = if (currentLanguage == "vi") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                if (currentLanguage == "vi") {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // English
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    scope.launch { preferences.setLanguage("en") }
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = if (currentLanguage == "en") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = if (currentLanguage == "en") androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("🇺🇸", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "English",
                                    fontWeight = if (currentLanguage == "en") FontWeight.Bold else FontWeight.Medium,
                                    color = if (currentLanguage == "en") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                if (currentLanguage == "en") {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 1. Gemini AI Configuration Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = IndigoPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.geminiConfig, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    Text(
                        strings.geminiConfigDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = inputApiKey,
                        onValueChange = {
                            inputApiKey = it
                            scope.launch { preferences.setApiKey(it) }
                        },
                        label = { Text(strings.apiKeyLabel) },
                        placeholder = { Text("AIzaSy...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_api_key_field"),
                        singleLine = true,
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                            }
                        }
                    )

                    // Supported Gemini Models (including Gemini 3.5 flash lite, 3.5 flash, 3.6 flash, 3.7 flash)
                    val modelList = listOf(
                        Triple("gemini-3.5-flash-lite", "Gemini 3.5 Flash Lite", "Siêu tốc / Ultra-fast"),
                        Triple("gemini-3.5-flash", "Gemini 3.5 Flash", "Khuyên dùng / Recommended"),
                        Triple("gemini-3.6-flash", "Gemini 3.6 Flash", "Nâng cấp / Enhanced"),
                        Triple("gemini-3.7-flash", "Gemini 3.7 Flash", "Mới nhất / Flagship"),
                        Triple("gemini-3.1-flash-lite-preview", "Gemini 3.1 Flash Lite", "Tối ưu / Fast"),
                        Triple("gemini-2.5-flash", "Gemini 2.5 Flash", "Ổn định / Stable"),
                        Triple("gemini-3.1-pro-preview", "Gemini 3.1 Pro", "Suy luận sâu / Deep Reasoning")
                    )

                    ExposedDropdownMenuBox(
                        expanded = modelDropdownExpanded,
                        onExpandedChange = { modelDropdownExpanded = !modelDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedModel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(strings.modelVersion) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = modelDropdownExpanded,
                            onDismissRequest = { modelDropdownExpanded = false }
                        ) {
                            modelList.forEach { (modelId, displayName, tag) ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    displayName,
                                                    fontWeight = if (selectedModel == modelId) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (selectedModel == modelId) IndigoPrimary else MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = IndigoPrimary.copy(alpha = 0.12f)
                                                ) {
                                                    Text(
                                                        tag,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = IndigoPrimary,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                modelId,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedModel = modelId
                                        modelDropdownExpanded = false
                                        scope.launch { preferences.setModel(modelId) }
                                    }
                                )
                            }
                        }
                    }

                    // Test Connection Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                isTestingConnection = true
                                testResult = null
                                scope.launch {
                                    val result = geminiService.testConnection(inputApiKey, selectedModel)
                                    isTestingConnection = false
                                    testSuccess = result.isSuccess
                                    testResult = if (result.isSuccess) {
                                        if (currentLanguage == "vi") "Kết nối thành công! Mô hình $selectedModel đã sẵn sàng." else (result.getOrNull() ?: "Connection OK")
                                    } else {
                                        if (currentLanguage == "vi") "Lỗi kết nối: ${result.exceptionOrNull()?.message}" else (result.exceptionOrNull()?.message ?: "Error connecting")
                                    }
                                }
                            },
                            enabled = !isTestingConnection,
                            modifier = Modifier.testTag("test_gemini_connection_button")
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.testing)
                            } else {
                                Text(strings.testConnection)
                            }
                        }
                    }

                    if (testResult != null) {
                        Text(
                            text = testResult!!,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (testSuccess) EmeraldSuccess else RoseDanger
                            )
                        )
                    }
                }
            }

            // 2. TTS Voice Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = IndigoPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.ttsSection, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    Text("${strings.ttsSpeedLabel}: ${String.format("%.1f", ttsSpeed)}x", style = MaterialTheme.typography.bodySmall)

                    Slider(
                        value = ttsSpeed,
                        onValueChange = {
                            ttsManager.setSpeed(it)
                            scope.launch { preferences.setTtsSpeed(it) }
                        },
                        valueRange = 0.5f..1.5f,
                        steps = 4
                    )

                    OutlinedButton(
                        onClick = {
                            val sample = if (currentLanguage == "vi") "Chào mừng bạn đến với hệ thống ôn tập lặp lại ngắt quãng Qusin Flashcard." else "Welcome to Qusin Flashcard spaced repetition system."
                            ttsManager.speak(sample)
                        }
                    ) {
                        Text(strings.testVoice)
                    }
                }
            }

            // 3. Spaced Repetition Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = IndigoPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.sm2Section, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    Text(if (currentLanguage == "vi") "• Hệ số khởi đầu (Ease Factor): 2.5 (Chuẩn Anki)" else "• Starting Ease Factor: 2.5 (Standard Anki)", style = MaterialTheme.typography.bodyMedium)
                    Text(if (currentLanguage == "vi") "• Hệ số tối thiểu: 1.3" else "• Minimum Ease Factor: 1.3", style = MaterialTheme.typography.bodyMedium)
                    Text(if (currentLanguage == "vi") "• Các bước ôn tập: 1 ngày, 6 ngày, sau đó khoảng cách * EF" else "• Review Steps: 1 day, 6 days, then interval * EF", style = MaterialTheme.typography.bodyMedium)
                    Text(if (currentLanguage == "vi") "• Quên (Again): Tăng số lần quên, đặt lại khoảng cách 1 ngày" else "• Again: Lapses incremented, reset to 1 day", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // 4. Daily Notifications & Reminders
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = IndigoPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.dailyReminder, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }

                        Switch(
                            checked = dailyReminderEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    preferences.setDailyReminderEnabled(enabled)
                                    if (enabled) {
                                        notificationManager.scheduleDailyReminder(reminderHour, 0)
                                    } else {
                                        notificationManager.cancelDailyReminder()
                                    }
                                }
                            }
                        )
                    }

                    if (dailyReminderEnabled) {
                        Text("${strings.reminderTime}: $reminderHour:00", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(8 to "8:00", 12 to "12:00", 18 to "18:00", 21 to "21:00").forEach { (hr, label) ->
                                FilterChip(
                                    selected = reminderHour == hr,
                                    onClick = {
                                        scope.launch {
                                            preferences.setReminderHour(hr)
                                            notificationManager.scheduleDailyReminder(hr, 0)
                                        }
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }

            // 5. Data Backup, Export & Import
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SettingsBackupRestore, contentDescription = null, tint = IndigoPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.backupSection, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    Text(strings.backupDesc, style = MaterialTheme.typography.bodySmall)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val json = repository.exportBackupJson()
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        putExtra(Intent.EXTRA_TEXT, json)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Export Qusin Flashcards JSON")
                                    context.startActivity(shareIntent)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.exportJson)
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val csv = repository.exportDecksToCsv()
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        putExtra(Intent.EXTRA_TEXT, csv)
                                        type = "text/csv"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Export Flashcards CSV")
                                    context.startActivity(shareIntent)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.exportCsv)
                        }
                    }

                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.importJson)
                    }

                    if (importStatus != null) {
                        Text(
                            text = importStatus!!,
                            color = EmeraldSuccess,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
