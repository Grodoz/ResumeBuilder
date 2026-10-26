package com.richardbroyles.resumebuilder.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.richardbroyles.resumebuilder.data.local.dao.AppDao
import com.richardbroyles.resumebuilder.data.local.dao.CoverLetterDao
import com.richardbroyles.resumebuilder.data.local.dao.InterviewDao
import com.richardbroyles.resumebuilder.data.local.dao.InterviewPracticeDao
import com.richardbroyles.resumebuilder.data.local.dao.JobDao
import com.richardbroyles.resumebuilder.data.local.dao.ResumeDao
import com.richardbroyles.resumebuilder.data.local.dao.ResumeModuleDao
import com.richardbroyles.resumebuilder.data.local.dao.ResumeVersionDao
import com.richardbroyles.resumebuilder.data.local.entity.ContactsEntity
import com.richardbroyles.resumebuilder.data.local.entity.CoverLetterEntity
import com.richardbroyles.resumebuilder.data.local.entity.EducationEntity
import com.richardbroyles.resumebuilder.data.local.entity.ExperienceEntity
import com.richardbroyles.resumebuilder.data.local.entity.InterviewEntity
import com.richardbroyles.resumebuilder.data.local.entity.InterviewPracticeSession
import com.richardbroyles.resumebuilder.data.local.entity.InterviewQuestionFeedback
import com.richardbroyles.resumebuilder.data.local.entity.JobEntity
import com.richardbroyles.resumebuilder.data.local.entity.Resume
import com.richardbroyles.resumebuilder.data.local.entity.ResumeEntity
import com.richardbroyles.resumebuilder.data.local.entity.ResumeVersion

@Database(
    entities = [
        Resume::class,
        ResumeEntity::class,
        CoverLetterEntity::class,
        InterviewEntity::class,
    JobEntity::class,
    InterviewPracticeSession::class,
    InterviewQuestionFeedback::class,
    ResumeVersion::class,
    ContactsEntity::class,
    ExperienceEntity::class,
    EducationEntity::class
    ],
    version = 10,
    exportSchema = false
)

abstract class AppDatabase : RoomDatabase() {
    abstract fun resumeDao(): ResumeDao
    abstract fun resumeModuleDao(): ResumeModuleDao
    abstract fun coverLetterDao(): CoverLetterDao
    abstract fun interviewDao(): InterviewDao
    abstract fun jobDao(): JobDao
    abstract fun appDao(): AppDao
    abstract fun interviewPracticeDao(): InterviewPracticeDao
    abstract fun resumeVersionDao(): ResumeVersionDao
    fun interviewPracticeSessionDao(): InterviewPracticeDao = interviewPracticeDao()

    companion object {
       const val DATABASE_NAME = "ai_tailored_resume.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        fun buildDatabase(context: Context, databaseName: String = DATABASE_NAME): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                databaseName
            ).fallbackToDestructiveMigration(true).build()
        }

        fun buildInMemoryDatabase(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AppDatabase::class.java
            ).allowMainThreadQueries().build()
        }

    }
}