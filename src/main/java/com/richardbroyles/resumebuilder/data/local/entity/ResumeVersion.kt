package com.richardbroyles.resumebuilder.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "resume_version",
    indices = [
        Index(value = ["resume_id", "created_at"]),
    Index(value = ["resume_id", "version_number"])
    ]
)
data class ResumeVersion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "resume_id")
    val resumeId: String = "primary_resume",
    @ColumnInfo(name = "version_name")
    val versionName: String,
    @ColumnInfo(name = "version_number")
    val versionNumber: Int = 1,
    @ColumnInfo(name = "target_role")
    val targetRole: String = "",
    @ColumnInfo(name = "target_company")
    val targetCompany: String = "",
    @ColumnInfo(name = "summary")
    val summary: String = "",
    @ColumnInfo(name = "json_content")
    val jsonContent: String,
    @ColumnInfo(name = "ats_score")
    val atsScore: Int? = null,
    @ColumnInfo(name = "change_notes")
    val changeNotes: String = "",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
