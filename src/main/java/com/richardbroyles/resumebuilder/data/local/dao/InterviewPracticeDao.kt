package com.richardbroyles.resumebuilder.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.richardbroyles.resumebuilder.data.local.entity.InterviewPracticeSession
import com.richardbroyles.resumebuilder.data.local.entity.InterviewPracticeSessionWithQuestions
import com.richardbroyles.resumebuilder.data.local.entity.InterviewQuestionFeedback
import kotlinx.coroutines.flow.Flow

@Dao
interface InterviewPracticeDao {
    @Query("SELECT * FROM interview_practice_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<InterviewPracticeSession>>

    @Query("SELECT * FROM interview_practice_sessions WHERE id = :sessionId LIMIT 1")
    fun getSessionById(sessionId: String): Flow<InterviewPracticeSession?>

    @Query("SELECT * FROM interview_practice_sessions WHERE job_title LIKE '%' || :jobTitle || '%' ORDER BY timestamp DESC")
    fun getSessionsForJob(jobTitle: String): Flow<List<InterviewPracticeSession>>

    @Query("SELECT * FROM interview_practice_sessions WHERE overall_score >= :minScore ORDER BY overall_score DESC, timestamp DESC")
    fun getSessionsWithMinimumScore(minScore: Int): Flow<List<InterviewPracticeSession>>

    @Query("SELECT AVG(overall_score) FROM interview_practice_sessions")
    fun getAverageScore(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM interview_practice_sessions")
    fun getTotalSessionCount(): Flow<Int>

    @Query("SELECT * FROM interview_practice_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionDirect(sessionId: String): InterviewPracticeSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: InterviewPracticeSession)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<InterviewPracticeSession>)

    @Update
    suspend fun updateSession(session: InterviewPracticeSession)

    @Delete
    suspend fun deleteSession(session: InterviewPracticeSession)

    @Query("DELETE FROM interview_practice_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: String)

    @Query("DELETE FROM interview_practice_sessions")
    suspend fun clearAllSessions()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestionFeedback(questionFeedback: InterviewQuestionFeedback): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestionFeedbacks(questionFeedbacks: List<InterviewQuestionFeedback>): List<Long>

    @Query("SELECT * FROM interview_question_feedbacks WHERE session_id = :sessionId ORDER BY question_number ASC, created_at ASC")
    fun getQuestionsForSession(sessionId: String): Flow<List<InterviewQuestionFeedback>>

    @Query("SELECT * FROM interview_question_feedbacks WHERE session_id = :sessionId ORDER BY question_number ASC, created_at ASC")
    suspend fun getQuestionsForSessionDirect(sessionId: String): List<InterviewQuestionFeedback>

    @Query("SELECT * FROM interview_question_feedbacks ORDER BY created_at DESC LIMIT :limit")
    fun getRecentQuestionFeedbacks(limit: Int = 10): Flow<List<InterviewQuestionFeedback>>

    @Transaction
    @Query("SELECT * FROM interview_practice_sessions ORDER BY timestamp DESC")
    fun getAllSessionsWithQuestions(): Flow<List<InterviewPracticeSessionWithQuestions>>

    @Transaction
    @Query("SELECT * FROM interview_practice_sessions WHERE id = :sessionId LIMIT 1")
    fun getSessionWithQuestions(sessionId: String): Flow<InterviewPracticeSessionWithQuestions?>

    @Transaction
    @Query("SELECT * FROM interview_practice_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionWithQuestionsDirect(sessionId: String): InterviewPracticeSessionWithQuestions?
}

typealias InterviewPracticeSessionDao = InterviewPracticeDao