package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.audio.StudySoundSynthesizer
import com.example.audio.TtsManager
import com.example.data.gemini.GeminiService
import com.example.data.local.AppDatabase
import com.example.data.repository.FlashcardRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.notifications.StudyNotificationManager
import com.example.ui.i18n.LocalLanguage
import com.example.ui.i18n.LocalStrings
import com.example.ui.i18n.getAppStrings
import com.example.ui.navigation.Screen
import com.example.ui.screens.AiGeneratorScreen
import com.example.ui.screens.AiHubScreen
import com.example.ui.screens.AiTutorScreen
import com.example.ui.screens.AiVisionOcrScreen
import com.example.ui.screens.AiWordLookupScreen
import com.example.ui.screens.ConversationPracticeScreen
import com.example.ui.screens.DeckDetailScreen
import com.example.ui.screens.DeckListScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PomodoroScreen
import com.example.ui.screens.QuizStudyScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.StudyMusicScreen
import com.example.ui.screens.StudyScreen
import androidx.lifecycle.lifecycleScope
import com.example.ui.theme.MyApplicationTheme

data class BottomNavItem(
    val title: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

class MainActivity : ComponentActivity() {

    private lateinit var ttsManager: TtsManager
    private lateinit var soundSynthesizer: StudySoundSynthesizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getDatabase(applicationContext)
        val repository = FlashcardRepository(db.flashcardDao())
        val preferences = UserPreferencesRepository(applicationContext)
        val geminiService = GeminiService()
        ttsManager = TtsManager(applicationContext)
        soundSynthesizer = StudySoundSynthesizer(lifecycleScope)
        val notificationManager = StudyNotificationManager(applicationContext)

        setContent {
            MyApplicationTheme {
                QusinApp(
                    repository = repository,
                    preferences = preferences,
                    geminiService = geminiService,
                    ttsManager = ttsManager,
                    soundSynthesizer = soundSynthesizer,
                    notificationManager = notificationManager
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
        soundSynthesizer.release()
    }
}

@Composable
fun QusinApp(
    repository: FlashcardRepository,
    preferences: UserPreferencesRepository,
    geminiService: GeminiService,
    ttsManager: TtsManager,
    soundSynthesizer: StudySoundSynthesizer,
    notificationManager: StudyNotificationManager
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val currentLanguage by preferences.languageFlow.collectAsState()
    val strings = getAppStrings(currentLanguage)

    CompositionLocalProvider(
        LocalLanguage provides currentLanguage,
        LocalStrings provides strings
    ) {
        val bottomNavItems = listOf(
            BottomNavItem(strings.navHome, Screen.Home.route, Icons.Filled.Home, Icons.Outlined.Home),
            BottomNavItem(strings.navDecks, Screen.Decks.route, Icons.Filled.Folder, Icons.Outlined.Folder),
            BottomNavItem(strings.navAiHub, Screen.AiHub.route, Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
            BottomNavItem(strings.navStats, Screen.Statistics.route, Icons.Filled.BarChart, Icons.Outlined.BarChart),
            BottomNavItem(strings.navSettings, Screen.Settings.route, Icons.Filled.Settings, Icons.Outlined.Settings)
        )

        val showBottomBar = bottomNavItems.any { it.route == currentDestination?.route }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.route == item.route
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = { Text(item.title) },
                            selected = selected,
                            onClick = {
                                if (currentDestination?.route != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. Home Dashboard
            composable(Screen.Home.route) {
                HomeScreen(
                    repository = repository,
                    preferences = preferences,
                    ttsManager = ttsManager,
                    onNavigateToStudy = { deckId, mode ->
                        navController.navigate(Screen.Study.createRoute(deckId, mode))
                    },
                    onNavigateToDeck = { deckId ->
                        navController.navigate(Screen.DeckDetail.createRoute(deckId))
                    },
                    onNavigateToDecks = {
                        navController.navigate(Screen.Decks.route)
                    },
                    onNavigateToWordLookup = {
                        navController.navigate(Screen.WordLookup.route)
                    },
                    onNavigateToPomodoro = {
                        navController.navigate(Screen.Pomodoro.route)
                    },
                    onNavigateToMusic = {
                        navController.navigate(Screen.Music.route)
                    },
                    onNavigateToAiHub = {
                        navController.navigate(Screen.AiHub.route)
                    }
                )
            }

            // 2. Decks List
            composable(Screen.Decks.route) {
                DeckListScreen(
                    repository = repository,
                    onNavigateToDeckDetail = { deckId ->
                        navController.navigate(Screen.DeckDetail.createRoute(deckId))
                    },
                    onNavigateToStudy = { deckId, mode ->
                        navController.navigate(Screen.Study.createRoute(deckId, mode))
                    },
                    onNavigateToQuiz = { deckId ->
                        navController.navigate(Screen.Quiz.createRoute(deckId))
                    }
                )
            }

            // 3. Deck Detail
            composable(
                route = Screen.DeckDetail.route,
                arguments = listOf(navArgument("deckId") { type = NavType.LongType })
            ) { backStackEntry ->
                val deckId = backStackEntry.arguments?.getLong("deckId") ?: 1L
                DeckDetailScreen(
                    deckId = deckId,
                    repository = repository,
                    preferences = preferences,
                    geminiService = geminiService,
                    ttsManager = ttsManager,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToStudy = { id, mode ->
                        navController.navigate(Screen.Study.createRoute(id, mode))
                    },
                    onNavigateToQuiz = { id ->
                        navController.navigate(Screen.Quiz.createRoute(id))
                    }
                )
            }

            // 4. Study Screen (Card Flip & Spaced Repetition)
            composable(
                route = Screen.Study.route,
                arguments = listOf(
                    navArgument("deckId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("mode") {
                        type = NavType.StringType
                        defaultValue = "NORMAL"
                    }
                )
            ) { backStackEntry ->
                val deckIdStr = backStackEntry.arguments?.getString("deckId")
                val deckId = deckIdStr?.toLongOrNull()
                val mode = backStackEntry.arguments?.getString("mode") ?: "NORMAL"
                StudyScreen(
                    deckId = deckId,
                    mode = mode,
                    repository = repository,
                    ttsManager = ttsManager,
                    soundSynthesizer = soundSynthesizer,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToMusic = { navController.navigate(Screen.Music.route) }
                )
            }

            // 5. Quiz Screen
            composable(
                route = Screen.Quiz.route,
                arguments = listOf(
                    navArgument("deckId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("quizType") {
                        type = NavType.StringType
                        defaultValue = "MCQ"
                    }
                )
            ) { backStackEntry ->
                val deckIdStr = backStackEntry.arguments?.getString("deckId")
                val deckId = deckIdStr?.toLongOrNull()
                val quizType = backStackEntry.arguments?.getString("quizType") ?: "MCQ"
                QuizStudyScreen(
                    deckId = deckId,
                    initialQuizType = quizType,
                    repository = repository,
                    geminiService = geminiService,
                    preferences = preferences,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 6. AI Hub
            composable(Screen.AiHub.route) {
                AiHubScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLookup = { navController.navigate(Screen.WordLookup.route) },
                    onNavigateToGenerator = { navController.navigate(Screen.AiGenerator.route) },
                    onNavigateToVisionOcr = { navController.navigate(Screen.AiVisionOcr.route) },
                    onNavigateToTutor = { navController.navigate(Screen.AiTutor.route) },
                    onNavigateToConversation = { navController.navigate(Screen.ConversationPractice.route) },
                    onNavigateToQuiz = { navController.navigate(Screen.Quiz.createRoute()) }
                )
            }

            // 7. AI Word Lookup
            composable(Screen.WordLookup.route) {
                AiWordLookupScreen(
                    geminiService = geminiService,
                    repository = repository,
                    preferences = preferences,
                    ttsManager = ttsManager,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 8. AI Flashcard Generator
            composable(Screen.AiGenerator.route) {
                AiGeneratorScreen(
                    geminiService = geminiService,
                    repository = repository,
                    preferences = preferences,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 9. Vision & PDF OCR
            composable(Screen.AiVisionOcr.route) {
                AiVisionOcrScreen(
                    geminiService = geminiService,
                    repository = repository,
                    preferences = preferences,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 10. AI Tutor
            composable(Screen.AiTutor.route) {
                AiTutorScreen(
                    geminiService = geminiService,
                    preferences = preferences,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 11. Conversation Practice
            composable(Screen.ConversationPractice.route) {
                ConversationPracticeScreen(
                    geminiService = geminiService,
                    preferences = preferences,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 12. Pomodoro Focus Timer
            composable(Screen.Pomodoro.route) {
                PomodoroScreen(
                    repository = repository,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToMusic = { navController.navigate(Screen.Music.route) }
                )
            }

            // 13. Study Music & Ambience
            composable(Screen.Music.route) {
                StudyMusicScreen(
                    synthesizer = soundSynthesizer,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 14. Statistics & Retention
            composable(Screen.Statistics.route) {
                StatsScreen(
                    repository = repository,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToStudyCard = {
                        navController.navigate(Screen.Study.createRoute(mode = "QUICK"))
                    }
                )
            }

            // 15. Settings
            composable(Screen.Settings.route) {
                SettingsScreen(
                    preferences = preferences,
                    geminiService = geminiService,
                    repository = repository,
                    ttsManager = ttsManager,
                    notificationManager = notificationManager,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
}
