package com.richardbroyles.resumebuilder.di

import android.content.Context
import com.richardbroyles.resumebuilder.data.local.AppDatabase
import com.richardbroyles.resumebuilder.data.local.dao.ResumeDao
import com.richardbroyles.resumebuilder.data.remote.GeminiService
import com.richardbroyles.resumebuilder.data.remote.GoogleDriveService
import com.richardbroyles.resumebuilder.data.repository.ResumeRepository
import com.richardbroyles.resumebuilder.viewmodel.ResumeViewModel

object ResumeModule {
    @Volatile
    private var appDatabaseInstance: AppDatabase? = null

    fun provideAppDatabase(context: Context): AppDatabase {
        return appDatabaseInstance ?: synchronized(this) {
            appDatabaseInstance ?: AppDatabase.getInstance(context).also {
                appDatabaseInstance = it
            }
        }
    }

    fun provideResumeDao(database: AppDatabase): ResumeDao {
        return database.resumeDao()
    }

    fun provideResumeDao(context: Context): ResumeDao {
        return provideAppDatabase(context).resumeDao()
    }

    fun resetDatabaseForTesting() {
        synchronized(this) {
            appDatabaseInstance = null
        }
    }

    fun provideResumeRepository(context: Context): ResumeRepository {
        val appDb = provideAppDatabase(context)
        return ResumeRepository(
            database = appDb,
            geminiService = GeminiService(),
            driveService = GoogleDriveService(context),
            context = context
        )
    }

    fun provideResumeViewModel(context: Context): ResumeViewModel {
        val repository = provideResumeRepository(context)
        return ResumeViewModel(repository = repository, resumeDao = repository.resumeDao)
    }
}
