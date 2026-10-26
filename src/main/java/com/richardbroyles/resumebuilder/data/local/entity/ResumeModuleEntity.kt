package com.richardbroyles.resumebuilder.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "resume_contacts")
data class ContactsEntity(
    @PrimaryKey
    val resumeId: String = "primary_resume",
    @ColumnInfo(name = "full_name")
    val fullName: String = "",
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
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toFormattedContactLine(): String {
        return listOfNotNull(
            email.takeIf { it.isNotBlank() },
            phone.takeIf { it.isNotBlank() },
            location.takeIf { it.isNotBlank() }
        ).joinToString(" | ")
    }
}

@Entity(tableName = "resume_experiences")
data class ExperienceEntity(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    @ColumnInfo(name = "resume_id")
    val resumeId: String = "primary_resume",
    @ColumnInfo(name = "job_title")
    val jobTitle: String = "",
    @ColumnInfo(name = "company")
    val company: String = "",
    @ColumnInfo(name = "location")
    val location: String = "",
    @ColumnInfo(name = "start_date")
    val startDate: String = "",
    @ColumnInfo(name = "end_date")
    val endDate: String = "",
    @ColumnInfo(name = "is_current")
    val isCurrent: Boolean = false,
    @ColumnInfo(name = "bullets_text")
    val bulletsText: String = "",
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toFormattedBlock(): String {
        val dateRange = "$startDate - ${if (isCurrent) "Present" else endDate}"
        val header = "$jobTitle at $company ($dateRange, $location)"
        return if (bulletsText.isNotBlank()) "$header\n$bulletsText" else header
    }
}

@Entity(tableName = "resume_educations")
data class EducationEntity(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    @ColumnInfo(name = "resume_id")
    val resumeId: String = "",
    @ColumnInfo(name = "degree")
    val degree: String = "",
    @ColumnInfo(name = "field_of_study")
    val fieldOfStudy: String = "",
    @ColumnInfo(name = "institution")
    val institution: String = "",
    @ColumnInfo(name = "graduation_year")
    val graduationYear: String = "",
    @ColumnInfo(name = "gpa_or_honors")
    val gpaOrHonors: String = "",
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toFormattedLine(): String {
        val degreePart = if(fieldOfStudy.isNotBlank()) "$degree in $fieldOfStudy" else degree
        val yearPart = if (graduationYear.isNotBlank()) " ($graduationYear)" else ""
        val honorsPart = if (gpaOrHonors.isNotBlank()) ", $gpaOrHonors" else " "
        return "$degreePart - $institution$yearPart$honorsPart"
    }
}

