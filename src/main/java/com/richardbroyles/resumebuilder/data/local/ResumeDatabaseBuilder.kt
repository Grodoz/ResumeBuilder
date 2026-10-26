package com.richardbroyles.resumebuilder.data.local

import android.content.Context
import androidx.room.Room
import com.richardbroyles.resumebuilder.data.local.dao.ResumeDao

object ResumeDatabaseBuilder {
    const val DEFAULT_DB_NAME = AppDatabase.DATABASE_NAME

    fun build(context: Context, databaseName: String = DEFAULT_DB_NAME): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            databaseName
        ).fallbackToDestructiveMigration(true).build()
    }

    fun buildInMemory(context: Context): AppDatabase {
        return Room.inMemoryDatabaseBuilder(
            context.applicationContext,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    fun getInstance(context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    fun getResumeDao(context: Context): ResumeDao {
        return getInstance(context).resumeDao()
    }
}