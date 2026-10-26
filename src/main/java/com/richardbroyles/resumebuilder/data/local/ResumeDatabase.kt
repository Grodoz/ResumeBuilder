package com.richardbroyles.resumebuilder.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.richardbroyles.resumebuilder.data.local.dao.ResumeDao
import com.richardbroyles.resumebuilder.data.local.entity.Resume
import com.richardbroyles.resumebuilder.data.local.entity.ResumeEntity

@Database(
    entities = [ResumeEntity::class, Resume::class],
    version = 2,
    exportSchema = false
)
abstract class ResumeDatabase : RoomDatabase() {
    abstract fun resumeDao(): ResumeDao
    companion object {
        const val DATABASE_NAME = "resume_database.db"

        @Volatile
        private var INSTANCE: ResumeDatabase? = null
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE resumes ADD COLUMN title TEXT NOT NULL DEFAULT '")
                db.execSQL("ALTER TABLE resumes ADD COLUMN rawContent TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE resumes ADD COLUMN lastModified INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): ResumeDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        fun resetForTesting() {
            synchronized(this) {
                if (INSTANCE?.isOpen == true) {
                    INSTANCE?.close()
                }
                INSTANCE = null
            }
        }

        fun buildDatabase(context: Context, databaseName: String = DATABASE_NAME): ResumeDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                ResumeDatabase::class.java,
                databaseName
            )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration(true)
                .build()
        }

        fun buildInMemory(context: Context): ResumeDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                ResumeDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()
        }
    }
}