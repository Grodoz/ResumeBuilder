package com.richardbroyles.resumebuilder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.richardbroyles.resumebuilder.data.local.entity.CoverLetterEntity
import com.richardbroyles.resumebuilder.data.local.entity.InterviewEntity
import com.richardbroyles.resumebuilder.data.local.entity.JobEntity
import com.richardbroyles.resumebuilder.data.local.entity.ResumeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM resumes WHERE id = :id LIMIT 1")
    fun getResumeFlow(id: String = "primary_resume"): Flow<ResumeEntity?>

    @Query("SELECT * FROM resumes WHERE id = :id LIMIT 1")
    suspend fun getResume(id: String = "primary_resume"): ResumeEntity?

    @Query("SELECT * FROM resumes ORDER BY updatedAt DESC")
    fun getAllResumes(): Flow<List<ResumeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveResume(entity: ResumeEntity)

    @Query("DELETE FROM resumes WHERE id = :id")
    suspend fun deleteResume(id: String)

    @Query("SELECT * FROM jobs ORDER BY addedAt DESC")
    fun getAllJobs(): Flow<List<JobEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveJob(job: JobEntity)

    @Query("DELETE FROM jobs WHERE id = :id")
    suspend fun deleteJob(id: String)

    @Query("SELECT * FROM cover_letters ORDER BY createdAt DESC")
    fun getAllCoverLetters(): Flow<List<CoverLetterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCoverLetter(coverLetter: CoverLetterEntity)

    @Query("DELETE FROM cover_letters WHERE id = :id")
    suspend fun deleteCoverLetter(id: String)

    @Query("SELECT * FROM interview_sessions ORDER BY createdAt DESC")
    fun getAllInterviews(): Flow<List<InterviewEntity>>

    @Query("SELECT * FROM interview_sessions WHERE id = :id LIMIT 1")
    suspend fun getInterview(id: String): InterviewEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveInterview(session: InterviewEntity)

}