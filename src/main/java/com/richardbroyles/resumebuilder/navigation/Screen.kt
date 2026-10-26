package com.richardbroyles.resumebuilder.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    object Dashboard: Screen(
        route = "dashboard",
        title = "Dashboard",
        icon = Icons.Default.Dashboard,
        testTag = "tab_dashboard"
    )
    object AtsScanner: Screen(
        route = "ats_scanner",
        title = "ATS Scan",
        icon = Icons.Default.AutoAwesome,
        testTag = "tab_ats_scanner"
    )
    object ResumeEditor: Screen(
        route = "resume_editor",
        title = "Resume Editor",
        icon = Icons.Default.Description,
        testTag = "tab_resume_editor"
    )
    object AiCoaching: Screen(
        route = "ai_coaching",
        title = "AI Coaching",
        icon = Icons.Default.Psychology,
        testTag = "tab_ai_coaching"
    )
    object Settings: Screen(
        route = "settings",
        title = "Settings",
        icon = Icons.Default.Settings,
        testTag = "tab_settings"
    )
    object JobMatcher: Screen(
        route = "job_matcher",
        title = "Job Matcher",
        icon = Icons.Default.Assessment,
        testTag = "tab_job_ats"
    )
    object CoverLetter: Screen(
        route = "cover_letter",
        title = "Cover Letter",
        icon = Icons.Default.MailOutline,
        testTag = "tab_cover_letter"
    )
    object SimpleChat: Screen(
        route = "simple_chat",
        title = "Simple Chat",
        icon = Icons.Default.Psychology,
        testTag = "tab_simple_chat"
    )
    object ResumeForm: Screen(
        route = "resume_form",
        title = "Resume Form",
        icon = Icons.Default.Description,
        testTag = "tab_resume_form"
    )
    companion object {
        val bottomNavItems: List<Screen>
            get() = listOf(
                Dashboard,
                ResumeEditor,
                AiCoaching,
                AtsScanner,
                Settings
            )
        fun fromRoute(route: String?): Screen {
            return when (route?.substringBefore("/")) {
                Dashboard.route -> Dashboard
                ResumeEditor.route -> ResumeEditor
                AiCoaching.route -> AiCoaching
                AtsScanner.route -> AtsScanner
                Settings.route -> Settings
                JobMatcher.route -> JobMatcher
                CoverLetter.route -> CoverLetter
                SimpleChat.route -> SimpleChat
                ResumeForm.route -> ResumeForm
                else -> Dashboard
            }
        }

    }
}