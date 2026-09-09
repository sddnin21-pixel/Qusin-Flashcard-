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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.i18n.LocalLanguage
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.PurpleAi

private data class AiFeatureItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val tintColor: Color,
    val routeKey: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiHubScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLookup: () -> Unit,
    onNavigateToGenerator: () -> Unit,
    onNavigateToVisionOcr: () -> Unit,
    onNavigateToTutor: () -> Unit,
    onNavigateToConversation: () -> Unit,
    onNavigateToQuiz: () -> Unit
) {
    val strings = LocalStrings.current
    val currentLanguage = LocalLanguage.current

    val aiFeatures = listOf(
        AiFeatureItem(
            title = strings.aiWordLookup,
            description = if (currentLanguage == "vi") "Định nghĩa chi tiết, dịch nghĩa tiếng Việt, phiên âm IPA, mẹo ghi nhớ mnemonics, cụm từ & từ đồng nghĩa." else "Deep definitions, translations, IPA phonetics, mnemonics, collocations & synonyms.",
            icon = Icons.Default.Search,
            tintColor = IndigoPrimary,
            routeKey = "lookup"
        ),
        AiFeatureItem(
            title = strings.aiCardGenerator,
            description = if (currentLanguage == "vi") "Tạo bộ flashcard theo chủ đề, bài viết hoặc tài liệu với khả năng xem trước & lưu trữ thông minh." else "Generate graded vocabulary sets from any topic, subject, or pasted article with preview.",
            icon = Icons.Default.AutoAwesome,
            tintColor = PurpleAi,
            routeKey = "generator"
        ),
        AiFeatureItem(
            title = strings.aiVisionOcr,
            description = if (currentLanguage == "vi") "Quét trang sách giáo khoa, bài tập và tài liệu bằng Gemini Vision để tạo flashcard tức thì." else "Scan textbook pages, worksheets, and documents with Gemini Vision into instant cards.",
            icon = Icons.Default.CameraAlt,
            tintColor = CyanAccent,
            routeKey = "vision"
        ),
        AiFeatureItem(
            title = strings.aiTutor,
            description = if (currentLanguage == "vi") "Gia sư AI 24/7 giải thích ngữ pháp, cấu trúc câu, sắc thái từ ngữ và lộ trình học tập." else "24/7 personal tutor for grammar explanations, sentence nuance, and academic guidance.",
            icon = Icons.Default.School,
            tintColor = EmeraldSuccess,
            routeKey = "tutor"
        ),
        AiFeatureItem(
            title = strings.conversationPractice,
            description = if (currentLanguage == "vi") "Luyện hội thoại nhập vai với AI: Giáo viên, Bạn bè, Nhà tuyển dụng, Giám khảo IELTS có phản hồi thời gian thực." else "Roleplay with AI: Teacher, Friend, Job Interviewer, IELTS Examiner with real-time feedback.",
            icon = Icons.Default.Chat,
            tintColor = AmberWarning,
            routeKey = "conversation"
        ),
        AiFeatureItem(
            title = strings.quizMaster,
            description = if (currentLanguage == "vi") "Trắc nghiệm, Đúng/Sai, gõ từ, điền từ vào chỗ trống và nối cặp thẻ kèm giải thích chi tiết." else "Multiple choice, True/False, typing, fill in the blanks, and matching pairs with explanations.",
            icon = Icons.Default.Quiz,
            tintColor = IndigoPrimary,
            routeKey = "quiz"
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.aiHubTitle, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
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
                .testTag("ai_hub_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = IndigoPrimary.copy(alpha = 0.2f),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(28.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(strings.aiHubBannerTitle, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                strings.aiHubBannerDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(aiFeatures) { feature ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            when (feature.routeKey) {
                                "lookup" -> onNavigateToLookup()
                                "generator" -> onNavigateToGenerator()
                                "vision" -> onNavigateToVisionOcr()
                                "tutor" -> onNavigateToTutor()
                                "conversation" -> onNavigateToConversation()
                                "quiz" -> onNavigateToQuiz()
                            }
                        }
                        .testTag("ai_feature_${feature.routeKey}"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = feature.tintColor.copy(alpha = 0.15f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = feature.icon,
                                    contentDescription = null,
                                    tint = feature.tintColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = feature.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = feature.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
