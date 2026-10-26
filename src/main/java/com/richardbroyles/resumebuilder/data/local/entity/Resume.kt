package com.richardbroyles.resumebuilder.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "resumes_v2")
data class Resume(
    @PrimaryKey val id: String = "primary_resume",
    @ColumnInfo(name = "personal_info")
    val personalInfo: String = "",
    @ColumnInfo(name = "name")
    val name: String = "",
    @ColumnInfo(name = "full_name")
    val fullName: String = name,
    @ColumnInfo(name = "contact_info")
    val contactInfo: String = "",
    @ColumnInfo(name = "email")
    val email: String = "",
    @ColumnInfo(name = "phone")
    val phone: String = "",
    @ColumnInfo(name = "location")
    val location: String = "",
    @ColumnInfo(name = "linkedin_url")
    val linkedinUrl: String = "",
    @ColumnInfo(name = "portfolio_url")
    val portfolioUrl: String = "",
    @ColumnInfo(name = "target_role")
    val targetRole: String = "",
    @ColumnInfo(name = "professional_summary")
    val professionalSummary: String = "",
    @ColumnInfo(name = "work_experience")
    val workExperience: String = "",
    @ColumnInfo(name = "summary")
    val summary: String = professionalSummary,
    @ColumnInfo(name = "experience")
    val experience: String = "",
    @ColumnInfo(name = "education")
    val education: String = "",
    @ColumnInfo(name = "skills")
    val skills: String = "",
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun getEffectiveName(): String = name.ifBlank { fullName }
    fun getEffectiveContactInfo(): String = contactInfo.ifBlank {
        listOfNotNull(
            email.takeIf{ it.isNotBlank() },
            phone.takeIf { it.isNotBlank() },
            location.takeIf { it.isNotBlank() },
            linkedinUrl.takeIf { it.isNotBlank() },
            portfolioUrl.takeIf { it.isNotBlank() }
        ).joinToString(" | ")
    }
    fun getEffectivePersonalInfo(): String = personalInfo.ifBlank {
        listOfNotNull(
            getEffectiveName().takeIf { it.isNotBlank() },
            getEffectiveContactInfo().takeIf { it.isNotBlank() },
            targetRole.takeIf { it.isNotBlank() }
        ).joinToString(" | ")
    }
    fun getEffectiveSummary(): String = summary.ifBlank { professionalSummary }
    fun getEffectiveWorkExperience(): String = workExperience.ifBlank { experience }
}