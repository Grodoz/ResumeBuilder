package com.richardbroyles.resumebuilder.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.richardbroyles.resumebuilder.data.local.entity.ResumeVersion
import kotlinx.coroutines.flow.Flow

@Dao
interface ResumeVersionDao {
    @Query("SELECT * FROM resume_version WHERE resume_id = :resumeId ORDER BY created_at DESC")
    fun getVersionsForResume(resumeId: String = "primary_resume"): Flow<List<ResumeVersion>>

    @Query("SELECT * FROM resume_version WHERE resume_id = :resumeId ORDER BY created_at DESC")
    suspend fun getVersionsForResumeSync(resumeId: String = "primary_resume"): List<ResumeVersion>

    @Query("SELECT * FROM resume_version ORDER BY created_at DESC")
    fun getAllVersions(): Flow<List<ResumeVersion>>

    @Query("SELECT * FROM resume_version WHERE id = :versionId LIMIT 1")
    suspend fun getVersionById(versionId: Long): ResumeVersion?

    @Query("SELECT * FROM resume_version WHERE resume_id = :resumeId ORDER BY version_number DESC LIMIT 1")
    suspend fun getLatestVersion(resumeId: String = "primary_resume"): ResumeVersion?

    @Query("SELECT COALESCE(MAX(version_number), 0) + 1 FROM resume_version WHERE resume_id = :resumeId")
    suspend fun getNextVersionNumber(resumeId: String = "primary_resume"): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersion(version: ResumeVersion): Long

    @Update
    suspend fun updateVersion(version: ResumeVersion)

    @Delete
    suspend fun deleteVersion(version: ResumeVersion)

    @Query("DELETE FROM resume_version WHERE id = :versionId")
    suspend fun deleteVersionById(versionId: Long)

    @Query("DELETE FROM resume_version WHERE resume_id = :resumeId")
    suspend fun deleteAllVersionsForResume(resumeId: String = "primary_resume")

    @Query("SELECT COUNT(*) FROM resume_version WHERE resume_id = :resumeId")
    fun getVersionCount(resumeId: String = "primary_resume"): Flow<Int>
}