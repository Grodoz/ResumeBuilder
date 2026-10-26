package com.richardbroyles.resumebuilder.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.richardbroyles.resumebuilder.data.local.entity.Resume
import com.richardbroyles.resumebuilder.data.local.entity.ResumeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ResumeDao {
    /**
     * Inserts or replaces a user resume entity locally.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(resume: Resume)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResume(resume: Resume)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStructuredResume(resume: Resume)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveResume(resume: Resume)

    /**
     * Updates an existing resume entity.
     */
    @Update
    suspend fun update(resume: Resume)

    @Update
    suspend fun updateResume(resume: Resume)

    /**
     * Deletes a resume entity.
     */
    @Delete
    suspend fun delete(resume: Resume)

    /**
     * Queries a resume by its ID.
     */
    @Query("SELECT * FROM resumes_v2 WHERE id = :id LIMIT 1")
    suspend fun getResumeById(id: String): Resume?

    /**
     * Observes a resume by its ID as a reactive Flow.
     */
    @Query("SELECT * FROM resumes_v2 WHERE id = :id LIMIT 1")
    fun getResumeByIdFlow(id: String): Flow<Resume?>

    @Query("SELECT * FROM resumes_v2 WHERE id = :id LIMIT 1")
    fun getStructuredResumeFlow(id: String = "primary_resume"): Flow<Resume?>

    @Query("SELECT * FROM resumes_v2 WHERE id = :id LIMIT 1")
    suspend fun getStructuredResume(id: String = "primary_resume"): Resume?

    /**
     * Observes all locally stored resumes ordered by last updated timestamp descending.
     */
    @Query("SELECT * FROM resumes_v2 ORDER BY last_updated DESC")
    fun getAllStructuredResumesFlow(): Flow<List<Resume>>

    @Query("SELECT * FROM resumes_v2 ORDER BY last_updated DESC")
    fun getStructuredResumesFlow(): Flow<List<Resume>>

    @Query("SELECT * FROM resumes_v2 ORDER BY last_updated DESC")
    suspend fun getAllStructuredResumes(): List<Resume>

    /**
     * Deletes a resume by its ID.
     */
    @Query("DELETE FROM resumes_v2 WHERE id = :id")
    suspend fun deleteStructuredResume(id: String)

    @Query("DELETE FROM resumes_v2 WHERE id = :id")
    suspend fun deleteResumeById(id: String)

    @Query("DELETE FROM resumes_v2 WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Deletes all structured resumes from the database.
     */
    @Query("DELETE FROM resumes_v2")
    suspend fun deleteAllStructuredResumes()

    // ========================================================================
    // ResumeEntity Data Access Methods (resumes table: id, title, raw content, last modification timestamp)
    // ========================================================================

    // --- Insertion Methods ---

    /**
     * Inserts or replaces a ResumeEntity in the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResume(entity: ResumeEntity)

    /**
     * Inserts or replaces a ResumeEntity and returns the newly inserted row ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResumeEntity(entity: ResumeEntity): Long

    /**
     * Inserts a collection of ResumeEntity objects in a single batch transaction.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllResumeEntities(entities: List<ResumeEntity>): List<Long>

    /**
     * Saves or replaces a ResumeEntity in the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveResumeEntity(entity: ResumeEntity)

    // --- Update Methods ---

    /**
     * Updates an existing ResumeEntity in the database.
     */
    @Update
    suspend fun updateResumeEntity(entity: ResumeEntity)

    /**
     * Updates a batch of ResumeEntity records in the database.
     */
    @Update
    suspend fun updateResumeEntities(entities: List<ResumeEntity>)

    /**
     * Updates the title, raw content, and modification timestamps for a specific ResumeEntity.
     */
    @Query("UPDATE resumes SET title = :title, rawContent = :rawContent, lastModified = :lastModified, updatedAt = :lastModified WHERE id = :id")
    suspend fun updateResumeTitleAndContent(
        id: String,
        title: String,
        rawContent: String,
        lastModified: Long = System.currentTimeMillis()
    )

    /**
     * Updates raw content and timestamps for a specific ResumeEntity.
     */
    @Query("UPDATE resumes SET rawContent = :rawContent, jsonContent = :rawContent, lastModified = :lastModified, updatedAt = :lastModified WHERE id = :id")
    suspend fun updateResumeContent(
        id: String,
        rawContent: String,
        lastModified: Long = System.currentTimeMillis()
    )

    // --- Deletion Methods ---

    /**
     * Deletes a ResumeEntity from the database.
     */
    @Delete
    suspend fun deleteResumeEntity(entity: ResumeEntity)

    /**
     * Deletes a batch of ResumeEntity objects from the database.
     */
    @Delete
    suspend fun deleteResumeEntities(entities: List<ResumeEntity>)

    /**
     * Deletes a ResumeEntity by its unique identifier.
     */
    @Query("DELETE FROM resumes WHERE id = :id")
    suspend fun deleteResume(id: String)

    /**
     * Deletes a ResumeEntity by its unique identifier.
     */
    @Query("DELETE FROM resumes WHERE id = :id")
    suspend fun deleteResumeEntityById(id: String)

    /**
     * Deletes multiple ResumeEntity records matching the provided list of IDs.
     */
    @Query("DELETE FROM resumes WHERE id IN (:ids)")
    suspend fun deleteResumeEntitiesByIds(ids: List<String>)

    /**
     * Deletes all ResumeEntity records from the database.
     */
    @Query("DELETE FROM resumes")
    suspend fun deleteAllResumeEntities()

    // --- Querying Methods ---

    /**
     * Retrieves a single ResumeEntity by id synchronously.
     */
    @Query("SELECT * FROM resumes WHERE id = :id LIMIT 1")
    suspend fun getResume(id: String = "primary_resume"): ResumeEntity?

    /**
     * Observes a single ResumeEntity by id as a reactive Flow.
     */
    @Query("SELECT * FROM resumes WHERE id = :id LIMIT 1")
    fun getResumeFlow(id: String = "primary_resume"): Flow<ResumeEntity?>

    /**
     * Retrieves a ResumeEntity by id synchronously.
     */
    @Query("SELECT * FROM resumes WHERE id = :id LIMIT 1")
    suspend fun getResumeEntityById(id: String): ResumeEntity?

    /**
     * Observes a ResumeEntity by id as a reactive Flow.
     */
    @Query("SELECT * FROM resumes WHERE id = :id LIMIT 1")
    fun getResumeEntityByIdFlow(id: String): Flow<ResumeEntity?>

    /**
     * Observes all ResumeEntity records sorted by updatedAt descending.
     */
    @Query("SELECT * FROM resumes ORDER BY updatedAt DESC")
    fun getAllResumesFlow(): Flow<List<ResumeEntity>>

    /**
     * Retrieves all ResumeEntity records sorted by updatedAt descending.
     */
    @Query("SELECT * FROM resumes ORDER BY updatedAt DESC")
    suspend fun getAllResumes(): List<ResumeEntity>

    /**
     * Observes all ResumeEntity records sorted by last modification timestamp descending.
     */
    @Query("SELECT * FROM resumes ORDER BY lastModified DESC")
    fun getAllResumesByLastModified(): Flow<List<ResumeEntity>>

    /**
     * Retrieves all ResumeEntity records sorted by last modification timestamp descending.
     */
    @Query("SELECT * FROM resumes ORDER BY lastModified DESC")
    suspend fun getAllResumesByLastModifiedSync(): List<ResumeEntity>

    /**
     * Searches ResumeEntity records matching the title substring, ordered by last modified descending.
     */
    @Query("SELECT * FROM resumes WHERE title LIKE '%' || :query || '%' ORDER BY lastModified DESC")
    fun searchResumesByTitle(query: String): Flow<List<ResumeEntity>>

    /**
     * Searches ResumeEntity records across title, raw content, or json content.
     */
    @Query("SELECT * FROM resumes WHERE title LIKE '%' || :query || '%' OR rawContent LIKE '%' || :query || '%' OR jsonContent LIKE '%' || :query || '%' ORDER BY lastModified DESC")
    fun searchResumesByContent(query: String): Flow<List<ResumeEntity>>

    /**
     * Observes the total count of stored ResumeEntity records.
     */
    @Query("SELECT COUNT(*) FROM resumes")
    fun getResumeEntityCount(): Flow<Int>

    /**
     * Returns the total count of stored ResumeEntity records synchronously.
     */
    @Query("SELECT COUNT(*) FROM resumes")
    suspend fun getResumeEntityCountSync(): Int
}