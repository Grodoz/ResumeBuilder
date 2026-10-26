package com.richardbroyles.resumebuilder.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "resumes")
data class ResumeEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = "primary_resume",
    @ColumnInfo(name = "title")
    val title: String = "",
    @ColumnInfo(name = "rawContent")
    val rawContent: String = "",
    @ColumnInfo(name = "jsonContent")
    val jsonContent: String = rawContent,
    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "lastModified")
    val lastModified: Long = updatedAt
) {
    val resumeId : String get() = id
    val content: String
        get() = if (rawContent.isNotBlank()) rawContent else jsonContent
    val lastModifiedTimestamp: Long
        get() = if(lastModified != 0L) lastModified else updatedAt
}

@Entity(tableName = "jobs")
data class JobEntity(
    @PrimaryKey val id: String,
    val title: String,
    val company: String,
    val jsonContent: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cover_letters")
data class CoverLetterEntity(
    @PrimaryKey val id: String,
    val jobTitle: String,
    val company: String,
    val content: String,
    val tone: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "interview_sessions")
data class InterviewEntity(
    @PrimaryKey val id: String,
    val jobTitle: String,
    val company: String,
    val jsonContent: String,
    val createdAt: Long = System.currentTimeMillis()
)