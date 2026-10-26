package com.richardbroyles.resumebuilder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.richardbroyles.resumebuilder.data.local.entity.CoverLetterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CoverLetterDao {
    @Query("SELECT * FROM cover_letters ORDER BY createdAt DESC")
    fun getAllCoverLetters(): Flow<List<CoverLetterEntity>>

    @Query("SELECT * FROM cover_letters WHERE id = :id LIMIT 1")
    fun getCoverLetterById(id: String): Flow<CoverLetterEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoverLetter(coverLetter: CoverLetterEntity)

    @Query("DELETE FROM cover_letters WHERE id = :id")
    suspend fun deleteCoverLetter(id: String)
}
