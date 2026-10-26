package com.richardbroyles.resumebuilder.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.richardbroyles.resumebuilder.data.remote.GeminiService
import com.richardbroyles.resumebuilder.ui.screens.AtsResumeAnalyzerScreen
import com.richardbroyles.resumebuilder.ui.screens.CoverLetterScreen
import com.richardbroyles.resumebuilder.ui.screens.InterviewCoachScreen
import com.richardbroyles.resumebuilder.ui.screens.JobMatcherScreen
import com.richardbroyles.resumebuilder.ui.screens.ResumeDashboardScreen
import com.richardbroyles.resumebuilder.ui.screens.ResumeEditorScreen
import com.richardbroyles.resumebuilder.ui.screens.ResumeFormScreen
import com.richardbroyles.resumebuilder.ui.screens.SettingsScreen
import com.richardbroyles.resumebuilder.ui.screens.SimpleChatScreen
import com.richardbroyles.resumebuilder.viewmodel.ChatViewModel
import com.richardbroyles.resumebuilder.viewmodel.ChatViewModelFactory
import com.richardbroyles.resumebuilder.viewmodel.ResumeViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    viewModel: ResumeViewModel,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Dashboard.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() }
    ) {
        composable(Screen.Dashboard.route) {
            ResumeDashboardScreen(
                viewModel = viewModel,
                onNavigateToEditor = {
                    navController.navigate(Screen.ResumeEditor.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToAtsScanner = {
                    navController.navigate(Screen.AtsScanner.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToJobs = {
                    navController.navigate(Screen.JobMatcher.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToCoverLetter = {
                    navController.navigate(Screen.CoverLetter.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToAiCoach = {
                    navController.navigate(Screen.AiCoaching.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToResumeForm = {
                    navController.navigate(Screen.ResumeForm.route) {
                        launchSingleTop = true
                    }
                }
            )
        }
        // Resume Editor Screen (Core feature)
        composable(Screen.ResumeEditor.route) {
            ResumeEditorScreen(
                viewModel = viewModel,
                onNavigateToAiCoach = {
                    navController.navigate(Screen.AiCoaching.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route) {
                        launchSingleTop = true
                    }
                }
            )
        }
        // AI Interview Coaching Screen (Core Feature)
        composable(Screen.AiCoaching.route) {
            InterviewCoachScreen(
                viewModel = viewModel,
                onNavigateToEditor = {
                    navController.navigate(Screen.ResumeEditor.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route) {
                        launchSingleTop = true
                    }
                }
            )
        }
        // Settings Screen (Core Feature)
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateToEditor = {
                    navController.navigate(Screen.ResumeEditor.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToAiCoach = {
                    navController.navigate(Screen.AiCoaching.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToAtsScanner = {
                    navController.navigate(Screen.AtsScanner.route) {
                        launchSingleTop = true
                    }
                }
            )
        }
        // ATS Scanner & Keyword Optimization
        composable(Screen.AtsScanner.route) {
            AtsResumeAnalyzerScreen(
                viewModel = viewModel,
                onNavigateToEditor = {
                    navController.navigate(Screen.ResumeEditor.route) {
                        launchSingleTop = true
                    }
                }
            )
        }
        // Job Matcher & Skills Matrix Screen
        composable(Screen.JobMatcher.route) {
            JobMatcherScreen(
                viewModel = viewModel
            )
        }
        // Cover letter generator & Editor Screen
        composable(Screen.CoverLetter.route) {
            CoverLetterScreen(
                viewModel = viewModel
            )
        }
        // Interactive Interview Coaching Chat
        composable(Screen.SimpleChat.route) {
            val chatViewModel: ChatViewModel = viewModel(
                factory = ChatViewModelFactory(
                    geminiService = GeminiService(),
                    repository = viewModel.repository
                )
            )
            SimpleChatScreen(
                viewModel = chatViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        // Input & Save Resume Data to Room Database Screen
        composable(Screen.ResumeForm.route) {
            ResumeFormScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
