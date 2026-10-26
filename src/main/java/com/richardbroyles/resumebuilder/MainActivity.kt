package com.richardbroyles.resumebuilder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.richardbroyles.resumebuilder.data.local.AppDatabase
import com.richardbroyles.resumebuilder.data.remote.GeminiService
import com.richardbroyles.resumebuilder.data.remote.GoogleDriveService
import com.richardbroyles.resumebuilder.data.repository.ResumeRepository
import com.richardbroyles.resumebuilder.ui.MainScreen
import com.richardbroyles.resumebuilder.ui.theme.ResumeBuilderTheme
import com.richardbroyles.resumebuilder.viewmodel.ResumeViewModel
import com.richardbroyles.resumebuilder.viewmodel.ResumeViewModelFactory

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: ResumeViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val database = AppDatabase.getInstance(applicationContext)
        val geminiService = GeminiService()
        val driveService = GoogleDriveService(applicationContext)
        val repository = ResumeRepository(
            database = database,
            geminiService = geminiService,
            driveService = driveService,
            context = applicationContext
        )
        val factory = ResumeViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[ResumeViewModel::class.java]
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val isDark = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            ResumeBuilderTheme(darkTheme = isDark) {
                MainScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}



