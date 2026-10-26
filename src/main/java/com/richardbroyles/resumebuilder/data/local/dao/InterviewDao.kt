package com.richardbroyles.resumebuilder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.richardbroyles.resumebuilder.data.local.entity.InterviewEntity
import com.richardbroyles.resumebuilder.data.local.entity.InterviewPracticeSession
import kotlinx.coroutines.flow.Flow

@Dao
interface InterviewDao {
    @Query("SELECT * FROM interview_sessions ORDER BY createdAt DESC")
    fun getAllInterviews(): Flow<List<InterviewEntity>>

    @Query("SELECT * FROM interview_sessions WHERE id = :id LIMIT 1")
    suspend fun getInterview(id: String): InterviewEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInterview(session: InterviewEntity)

    @Query("DELETE FROM interview_sessions WHERE id = :id")
    suspend fun deleteInterview(id: String)

    @Query("SELECT * FROM interview_practice_sessions ORDER BY timestamp DESC")
    fun getAllPracticeSessions(): Flow<List<InterviewPracticeSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPracticeSession(session: InterviewPracticeSession)

    @Query("DELETE FROM interview_practice_sessions WHERE id = :id")
    suspend fun deletePracticeSession(id: String)
}
