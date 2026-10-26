package com.richardbroyles.resumebuilder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.richardbroyles.resumebuilder.data.local.entity.ContactsEntity
import com.richardbroyles.resumebuilder.data.local.entity.EducationEntity
import com.richardbroyles.resumebuilder.data.local.entity.ExperienceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ResumeModuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactsEntity)

    @Update
    suspend fun updateContact(contact: ContactsEntity)

    @Query("SELECT * FROM resume_contacts WHERE resumeId = :resumeId LIMIT 1")
    fun getContactFlow(resumeId: String = "primary_resume"): Flow<ContactsEntity?>

    @Query("SELECT * FROM resume_contacts WHERE resumeId = :resumeId LIMIT 1")
    suspend fun getContact(resumeId: String = "primary_resume"): ContactsEntity?

    @Query("DELETE FROM resume_contacts WHERE resumeId = :resumeId")
    suspend fun deleteContact(resumeId: String = "primary_resume")

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExperience(experience: ExperienceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExperiences(experiences: List<ExperienceEntity>)

    @Update
    suspend fun updateExperience(experience: ExperienceEntity)

    @Query("SELECT * FROM resume_experiences WHERE resume_id = :resumeId ORDER BY sort_order ASC, start_date DESC")
    fun getExperiencesFlow(resumeId: String = "primary_resume"): Flow<List<ExperienceEntity>>

    @Query("SELECT * FROM resume_experiences WHERE resume_id = :resumeId ORDER BY sort_order ASC, start_date DESC")
    suspend fun getExperiences(resumeId: String = "primary_resume"): List<ExperienceEntity>

    @Query("SELECT * FROM resume_experiences WHERE id = :id LIMIT 1")
    suspend fun getExperienceById(id: String): ExperienceEntity?

    @Query("DELETE FROM resume_experiences WHERE id = :id")
    suspend fun deleteExperienceById(id: String)

    @Query("DELETE FROM resume_experiences WHERE resume_id = :resumeId")
    suspend fun deleteAllExperiences(resumeId: String = "primary_resume")

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEducation(education: EducationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEducations(educations: List<EducationEntity>)

    @Update
    suspend fun updateEducation(education: EducationEntity)

    @Query("SELECT * FROM resume_educations WHERE resume_id = :resumeId ORDER BY sort_order ASC, graduation_year DESC")
    fun getEducationsFlow(resumeId: String = "primary_resume"): Flow<List<EducationEntity>>

    @Query("SELECT * FROM resume_educations WHERE resume_id = :resumeId ORDER BY sort_order ASC, graduation_year DESC")
    suspend fun getEducations(resumeId: String = "primary_resume"): List<EducationEntity>

    @Query("SELECT * FROM resume_educations WHERE id = :id LIMIT 1")
    suspend fun getEducationById(id: String): EducationEntity?

    @Query("DELETE FROM resume_educations WHERE id = :id")
    suspend fun deleteEducationById(id: String)

    @Query("DELETE FROM resume_educations WHERE resume_id = :resumeId")
    suspend fun deleteAllEducations(resumeId: String = "primary_resume")

    @Transaction
    suspend fun syncResumeModules(
        contact: ContactsEntity,
        experiences: List<ExperienceEntity>,
        educations: List<EducationEntity>
    ) {
        insertContact(contact)
        deleteAllExperiences(contact.resumeId)
        insertExperiences(experiences)
        deleteAllEducations(contact.resumeId)
        insertEducations(educations)
    }
}