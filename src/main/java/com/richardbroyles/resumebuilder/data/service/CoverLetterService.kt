package com.richardbroyles.resumebuilder.data.service

import com.richardbroyles.resumebuilder.data.local.dao.ResumeDao
import com.richardbroyles.resumebuilder.data.local.entity.Resume
import com.richardbroyles.resumebuilder.data.model.CoverLetter
import com.richardbroyles.resumebuilder.data.remote.GeminiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CoverLetterService(
    private val geminiService: GeminiService,
    private val resumeDao: ResumeDao? = null
) {
    suspend fun generateCoverLetter(
        resume: Resume,
        jobDescription: String,
        tone: String = "Professional & Results-Oriented",
        jobTitle: String = "Target Position",
        company: String = "Target Company"
    ): CoverLetter = withContext(Dispatchers.IO) {
        geminiService.generateCoverLetter(
            resume = resume,
            jobDescription = jobDescription,
            tone = tone,
            jobTitle = jobTitle,
            company = company
        )
    }

    suspend fun generateCoverLetterFromSaved(
        resumeId: String = "primary_resume",
        jobDescription: String,
        tone: String = "Professional & Results-Oriented",
        jobTitle: String = "Target Position",
        company: String = "Target Company"
    ): CoverLetter = withContext(Dispatchers.IO) {
        val savedResume = resumeDao?.getResumeById(resumeId) ?: Resume(
            id = resumeId,
            name = "Professional Candidate",
            targetRole = jobTitle,
            skills = "Technical skills, software engineering, problem solving",
            workExperience = "Proven track record delivering software projects."
        )
        geminiService.generateCoverLetter(
            resume = savedResume,
            jobDescription = jobDescription,
            tone = tone,
            jobTitle = jobTitle,
            company = company
        )
    }
}