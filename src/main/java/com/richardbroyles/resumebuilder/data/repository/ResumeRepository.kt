package com.richardbroyles.resumebuilder.data.repository

import android.content.Context
import com.richardbroyles.resumebuilder.data.local.AppDatabase
import com.richardbroyles.resumebuilder.data.local.dao.CoverLetterDao
import com.richardbroyles.resumebuilder.data.local.dao.InterviewDao
import com.richardbroyles.resumebuilder.data.local.dao.InterviewPracticeDao
import com.richardbroyles.resumebuilder.data.local.dao.JobDao
import com.richardbroyles.resumebuilder.data.local.dao.ResumeDao
import com.richardbroyles.resumebuilder.data.local.dao.ResumeModuleDao
import com.richardbroyles.resumebuilder.data.local.dao.ResumeVersionDao
import com.richardbroyles.resumebuilder.data.local.entity.ContactsEntity
import com.richardbroyles.resumebuilder.data.local.entity.CoverLetterEntity
import com.richardbroyles.resumebuilder.data.local.entity.EducationEntity
import com.richardbroyles.resumebuilder.data.local.entity.ExperienceEntity
import com.richardbroyles.resumebuilder.data.local.entity.InterviewEntity
import com.richardbroyles.resumebuilder.data.local.entity.InterviewPracticeSession
import com.richardbroyles.resumebuilder.data.local.entity.InterviewPracticeSessionWithQuestions
import com.richardbroyles.resumebuilder.data.local.entity.InterviewQuestionFeedback
import com.richardbroyles.resumebuilder.data.local.entity.JobEntity
import com.richardbroyles.resumebuilder.data.local.entity.Resume
import com.richardbroyles.resumebuilder.data.local.entity.ResumeEntity
import com.richardbroyles.resumebuilder.data.local.entity.ResumeVersion
import com.richardbroyles.resumebuilder.data.model.AtsAnalysisResult
import com.richardbroyles.resumebuilder.data.model.ChatMessage
import com.richardbroyles.resumebuilder.data.model.CoverLetter
import com.richardbroyles.resumebuilder.data.model.DriveSyncState
import com.richardbroyles.resumebuilder.data.model.InterviewSession
import com.richardbroyles.resumebuilder.data.model.JobTarget
import com.richardbroyles.resumebuilder.data.model.ResumeProfile
import com.richardbroyles.resumebuilder.data.model.SimulatedInterviewQuestion
import com.richardbroyles.resumebuilder.data.model.SkillsMatrixReport
import com.richardbroyles.resumebuilder.data.remote.GeminiService
import com.richardbroyles.resumebuilder.data.remote.GoogleDriveService
import com.richardbroyles.resumebuilder.data.service.AtsEvaluationService
import com.richardbroyles.resumebuilder.util.AtsScoreCalculator
import com.richardbroyles.resumebuilder.util.AtsScoreResult
import com.richardbroyles.resumebuilder.util.SampleData
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ResumeRepository(
    val resumeDao: ResumeDao,
    private val jobDao: JobDao,
    private val coverLetterDao: CoverLetterDao,
    private val interviewDao: InterviewDao,
    private val interviewPracticeDao: InterviewPracticeDao,
    private val resumeVersionDao: ResumeVersionDao,
    private val geminiService: GeminiService,
    private val driveService: GoogleDriveService,
    private val context: Context,
    private val resumeModulesDao: ResumeModuleDao? = null
) {
    // Secondary constructor accepting AppDatabase directly
    constructor(
        database: AppDatabase,
        geminiService: GeminiService,
        driveService: GoogleDriveService,
        context: Context
    ) : this(
        resumeDao = database.resumeDao(),
        jobDao = database.jobDao(),
        coverLetterDao = database.coverLetterDao(),
        interviewDao = database.interviewDao(),
        interviewPracticeDao = database.interviewPracticeDao(),
        resumeVersionDao = database.resumeVersionDao(),
        geminiService = geminiService,
        driveService = driveService,
        context = context,
        resumeModulesDao = database.resumeModuleDao()
    )

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val resumeAdapter = moshi.adapter(ResumeProfile::class.java)
    private val jobAdapter = moshi.adapter(JobTarget::class.java)
    private val interviewAdapter = moshi.adapter(InterviewSession::class.java)
    val atsEvaluationService = AtsEvaluationService(geminiService)
    val impactAnalysisRepository = ResumeImpactAnalysisRepository(
        geminiService = geminiService,
        resumeDao = resumeDao,
        resumeModuleDao = resumeModulesDao
    )

    suspend fun initializeDefaultsIfNeeded() = withContext(Dispatchers.IO) {
        try {
            val existingResume = resumeDao.getResume()
            if (existingResume == null) {
                val json = resumeAdapter.toJson(SampleData.defaultResume)
                resumeDao.insertResume(ResumeEntity(id = "primary_resume", jsonContent = json))
            }

            val existingArch = resumeDao.getResume("resume_architect")
            if (existingArch == null) {
                val json = resumeAdapter.toJson(SampleData.sampleArchitectResume)
                resumeDao.insertResume(ResumeEntity(id = "resume_architect", jsonContent = json))
            }

            SampleData.sampleJobs.forEach { job ->
                val json = jobAdapter.toJson(job)
                jobDao.insertJob(JobEntity(id = job.id, title = job.title, company = job.company, jsonContent = json))
            }
            val existingStructured = resumeDao.getStructuredResume()
            if (existingStructured == null) {
                val defaultResume = SampleData.defaultResume
                val contactInfoStr = listOfNotNull(
                    defaultResume.email.takeIf { it.isNotBlank() },
                    defaultResume.phone.takeIf { it.isNotBlank() },
                    defaultResume.location.takeIf { it.isNotBlank() }
                ).joinToString(" | ")
                val expStr = defaultResume.experiences.joinToString("\n\n") { exp ->
                    "${exp.jobTitle} at ${exp.company} (${exp.startDate} - ${if (exp.isCurrent) "Present" else exp.endDate}, ${exp.location})\n" +
                            exp.bullets.joinToString("\n") { "• $it" }
                }
                val initialResumeEntity = Resume(
                    id = "primary_resume",
                    name = defaultResume.fullName,
                    fullName = defaultResume.fullName,
                    contactInfo = contactInfoStr,
                    email = defaultResume.email,
                    phone = defaultResume.phone,
                    location = defaultResume.location,
                    linkedinUrl = defaultResume.linkedinUrl,
                    portfolioUrl = defaultResume.portfolioUrl,
                    targetRole = defaultResume.targetRole,
                    professionalSummary = defaultResume.summary,
                    workExperience = expStr,
                    experience = expStr,
                    education = defaultResume.educations.joinToString("\n\n") { edu ->
                        "${edu.degree} in ${edu.fieldOfStudy} - ${edu.institution} (${edu.graduationYear}, ${edu.gpaOrHonors})"
                    },
                    skills = defaultResume.skills.joinToString(", ") { it.name }
                )
                resumeDao.insertStructuredResume(initialResumeEntity)
            }

            val existingVersions = resumeVersionDao.getVersionsForResumeSync("primary_resume")
            if (existingVersions.isEmpty()) {
                val defaultResume = SampleData.defaultResume
                val initialVersion = ResumeVersion(
                    resumeId = "primary_resume",
                    versionName = "v1.0 - Baseline Profile",
                    versionNumber = 1,
                    targetRole = defaultResume.targetRole,
                    targetCompany = "Initial Template",
                    summary = defaultResume.summary,
                    jsonContent = resumeAdapter.toJson(defaultResume),
                    atsScore = 78,
                    changeNotes = "Initial baseline version loaded from profile",
                    createdAt = System.currentTimeMillis() - 86400000L // 1 day ago
                )
                resumeVersionDao.insertVersion(initialVersion)

                // Also add an optimized snapshot example to showcase comparison and reversion
                val optimizedResume = defaultResume.copy(
                    targetRole = "Senior Android Engineer (Optimized)",
                    summary = "Results-driven Senior Android Engineer with 8+ years architecting reactive mobile systems in Kotlin, Jetpack Compose, Coroutines, and Room. Proven record increasing user retention by 28% and slashing cold start latency by 35%."
                )
                val optimizedVersion = ResumeVersion(
                    resumeId = "primary_resume",
                    versionName = "v1.1 - Staff / Senior ATS Optimization",
                    versionNumber = 2,
                    targetRole = "Senior Android Engineer",
                    targetCompany = "Tech Target",
                    summary = optimizedResume.summary,
                    jsonContent = resumeAdapter.toJson(optimizedResume),
                    atsScore = 92,
                    changeNotes = "Tailored summary with quantified metrics, added STAR impact bullets and boosted ATS keyword density.",
                    createdAt = System.currentTimeMillis() - 3600000L // 1 hour ago
                )
                resumeVersionDao.insertVersion(optimizedVersion)
            }

            // Populate modular tables for contact, experience, and education
            if (resumeModulesDao?.getContact("primary_resume") == null) {
                val defaultResume = SampleData.defaultResume
                val contactEntity = ContactsEntity(
                    resumeId = "primary_resume",
                    fullName = defaultResume.fullName,
                    email = defaultResume.email,
                    phone = defaultResume.phone,
                    location = defaultResume.location,
                    linkedinUrl = defaultResume.linkedinUrl,
                    portfolioUrl = defaultResume.portfolioUrl,
                    targetRole = defaultResume.targetRole
                )
                val experienceEntities = defaultResume.experiences.mapIndexed { index, exp ->
                    ExperienceEntity(
                        id = exp.id,
                        resumeId = "primary_resume",
                        jobTitle = exp.jobTitle,
                        company = exp.company,
                        location = exp.location,
                        startDate = exp.startDate,
                        endDate = exp.endDate,
                        isCurrent = exp.isCurrent,
                        bulletsText = exp.bullets.joinToString("\n") { "• $it" },
                        sortOrder = index
                    )
                }
                val educationEntities = defaultResume.educations.mapIndexed { index, edu ->
                    EducationEntity(
                        id = edu.id,
                        resumeId = "primary_resume",
                        degree = edu.degree,
                        fieldOfStudy = edu.fieldOfStudy,
                        institution = edu.institution,
                        graduationYear = edu.graduationYear,
                        gpaOrHonors = edu.gpaOrHonors,
                        sortOrder = index
                    )
                }
                resumeModulesDao?.syncResumeModules(contactEntity, experienceEntities, educationEntities)
            }
        } catch (_: Exception) {
            // Guard against database closed during teardown
        }
    }

    fun getStructuredResumeFlow(id: String = "primary_resume"): Flow<Resume?> {
        return resumeDao.getResumeByIdFlow(id)
    }

    suspend fun getStructuredResume(id: String = "primary_resume"): Resume? {
        return resumeDao.getResumeById(id)
    }

    fun getAllStructuredResumesFlow(): Flow<List<Resume>> {
        return resumeDao.getAllStructuredResumesFlow()
    }

    suspend fun getAllStructuredResumes(): List<Resume> {
        return resumeDao.getAllStructuredResumes()
    }

    suspend fun saveStructuredResume(resume: Resume) = withContext(Dispatchers.IO) {
        try {
            resumeDao.insertStructuredResume(resume)
            val current = resumeDao.getResume()?.let {
                try { resumeAdapter.fromJson(it.jsonContent) } catch (e: Exception) { null }
            } ?: SampleData.defaultResume
            val effectiveName = resume.getEffectiveName()
            val effectiveContact = resume.getEffectiveContactInfo()
            val updated = current.copy(
                fullName = effectiveName.ifBlank { current.fullName },
                email = resume.email.ifBlank { effectiveContact },
                phone = resume.phone,
                location = resume.location,
                linkedinUrl = resume.linkedinUrl,
                portfolioUrl = resume.portfolioUrl,
                targetRole = resume.targetRole,
                summary = resume.professionalSummary
            )
            // Also cache into modular contact table
            resumeModulesDao?.insertContact(
                ContactsEntity(
                    resumeId = resume.id,
                    fullName = effectiveName,
                    email = resume.email,
                    phone = resume.phone,
                    location = resume.location,
                    linkedinUrl = resume.linkedinUrl,
                    portfolioUrl = resume.portfolioUrl,
                    targetRole = resume.targetRole
                )
            )
        } catch (_: Exception) {
            // Guard against database closed during teardown
        }
    }

    // ==========================================
    // Resume Modules Caching (Contact, Experience, Education)
    // ==========================================

    suspend fun cacheContactInfo(contact: ContactsEntity) {
        resumeModulesDao?.insertContact(contact)
    }

    fun getContactInfoFlow(resumeId: String = "primary_resume"): Flow<ContactsEntity?>? {
        return resumeModulesDao?.getContactFlow(resumeId)
    }

    suspend fun getContactInfo(resumeId: String = "primary_resume"): ContactsEntity? {
        return resumeModulesDao?.getContact(resumeId)
    }

    suspend fun cacheExperience(experience: ExperienceEntity) {
        resumeModulesDao?.insertExperience(experience)
    }

    suspend fun cacheExperiences(experiences: List<ExperienceEntity>) {
        resumeModulesDao?.insertExperiences(experiences)
    }

    fun getExperiencesFlow(resumeId: String = "primary_resume"): Flow<List<ExperienceEntity>>? {
        return resumeModulesDao?.getExperiencesFlow(resumeId)
    }

    suspend fun getExperiences(resumeId: String = "primary_resume"): List<ExperienceEntity> {
        return resumeModulesDao?.getExperiences(resumeId) ?: emptyList()
    }

    suspend fun deleteExperience(id: String) {
        resumeModulesDao?.deleteExperienceById(id)
    }

    suspend fun cacheEducation(education: EducationEntity) {
        resumeModulesDao?.insertEducation(education)
    }

    suspend fun cacheEducations(educations: List<EducationEntity>) {
        resumeModulesDao?.insertEducations(educations)
    }

    fun getEducationsFlow(resumeId: String = "primary_resume"): Flow<List<EducationEntity>>? {
        return resumeModulesDao?.getEducationsFlow(resumeId)
    }

    suspend fun getEducations(resumeId: String = "primary_resume"): List<EducationEntity> {
        return resumeModulesDao?.getEducations(resumeId) ?: emptyList()
    }

    suspend fun deleteEducation(id: String) {
        resumeModulesDao?.deleteEducationById(id)
    }

    suspend fun syncResumeModules(
        contact: ContactsEntity,
        experiences: List<ExperienceEntity>,
        educations: List<EducationEntity>
    ) {
        resumeModulesDao?.syncResumeModules(contact, experiences, educations)
    }

    fun getResumeProfile(): Flow<ResumeProfile> {
        return resumeDao.getResumeFlow().map { entity ->
            if (entity != null) {
                try {
                    resumeAdapter.fromJson(entity.jsonContent) ?: SampleData.defaultResume
                } catch (e: Exception) {
                    SampleData.defaultResume
                }
            } else {
                SampleData.defaultResume
            }
        }
    }

    fun getAllResumesFlow(): Flow<List<ResumeProfile>> {
        return resumeDao.getAllResumesFlow().map { entities ->
            entities.mapNotNull { entity ->
                try {
                    resumeAdapter.fromJson(entity.jsonContent)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    suspend fun getAllResumes(): List<ResumeProfile> {
        return resumeDao.getAllResumes().mapNotNull { entity ->
            try {
                resumeAdapter.fromJson(entity.jsonContent)
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getResumeById(id: String): ResumeProfile? {
        val entity = resumeDao.getResume(id) ?: return null
        return try {
            resumeAdapter.fromJson(entity.jsonContent)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveResumeProfile(profile: ResumeProfile) {
        val json = resumeAdapter.toJson(profile)
        val entityId = profile.id.ifEmpty { "primary_resume" }
        resumeDao.insertResume(
            ResumeEntity(
                id = entityId,
                jsonContent = json,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteResume(id: String) {
        resumeDao.deleteResume(id)
        resumeDao.deleteStructuredResume(id)
        resumeVersionDao.deleteAllVersionsForResume(id)
    }

    // ==========================================
    // Resume Version History & Reversion
    // ==========================================

    fun getResumeVersions(resumeId: String = "primary_resume"): Flow<List<ResumeVersion>> {
        return resumeVersionDao.getVersionsForResume(resumeId)
    }

    suspend fun getResumeVersionsSync(resumeId: String = "primary_resume"): List<ResumeVersion> {
        return resumeVersionDao.getVersionsForResumeSync(resumeId)
    }

    suspend fun getResumeVersionById(versionId: Long): ResumeVersion? {
        return resumeVersionDao.getVersionById(versionId)
    }

    suspend fun createResumeVersion(
        resume: ResumeProfile,
        versionName: String = "",
        changeNotes: String = "",
        atsScore: Int? = null,
        targetCompany: String = ""
    ): ResumeVersion {
        val targetResumeId = resume.id.ifBlank { "primary_resume" }
        val nextNum = resumeVersionDao.getNextVersionNumber(targetResumeId)
        val finalVersionName = versionName.ifBlank {
            "v$nextNum - ${resume.targetRole.ifBlank { "Optimized Resume" }}"
        }
        val version = ResumeVersion(
            resumeId = targetResumeId,
            versionName = finalVersionName,
            versionNumber = nextNum,
            targetRole = resume.targetRole,
            targetCompany = targetCompany,
            summary = resume.summary,
            jsonContent = resumeAdapter.toJson(resume),
            atsScore = atsScore,
            changeNotes = changeNotes.ifBlank { "Snapshot saved before modifications" },
            createdAt = System.currentTimeMillis()
        )
        val generatedId = resumeVersionDao.insertVersion(version)
        return version.copy(id = generatedId)
    }

    suspend fun revertToResumeVersion(versionId: Long): ResumeProfile? {
        val version = resumeVersionDao.getVersionById(versionId) ?: return null
        val restoredProfile = try {
            resumeAdapter.fromJson(version.jsonContent)
        } catch (e: Exception) {
            null
        } ?: return null

        // Persist restored profile as the active resume in Room
        saveResumeProfile(restoredProfile)

        // Also sync the structured resume table if applicable
        val contactStr = listOfNotNull(
            restoredProfile.email.takeIf { it.isNotBlank() },
            restoredProfile.phone.takeIf { it.isNotBlank() },
            restoredProfile.location.takeIf { it.isNotBlank() }
        ).joinToString(" | ")
        val workExpStr = restoredProfile.experiences.joinToString("\n\n") { exp ->
            "${exp.jobTitle} at ${exp.company} (${exp.startDate} - ${if (exp.isCurrent) "Present" else exp.endDate}, ${exp.location})\n" +
                    exp.bullets.joinToString("\n") { "• $it" }
        }
        val structured = Resume(
            id = restoredProfile.id.ifBlank { "primary_resume" },
            name = restoredProfile.fullName,
            fullName = restoredProfile.fullName,
            contactInfo = contactStr,
            email = restoredProfile.email,
            phone = restoredProfile.phone,
            location = restoredProfile.location,
            linkedinUrl = restoredProfile.linkedinUrl,
            portfolioUrl = restoredProfile.portfolioUrl,
            targetRole = restoredProfile.targetRole,
            professionalSummary = restoredProfile.summary,
            workExperience = workExpStr,
            experience = workExpStr,
            education = restoredProfile.educations.joinToString("\n\n") { edu ->
                "${edu.degree} in ${edu.fieldOfStudy} - ${edu.institution} (${edu.graduationYear}, ${edu.gpaOrHonors})"
            },
            skills = restoredProfile.skills.joinToString(", ") { it.name }
        )
        resumeDao.insertStructuredResume(structured)

        return restoredProfile
    }

    suspend fun deleteResumeVersion(versionId: Long) {
        resumeVersionDao.deleteVersionById(versionId)
    }

    suspend fun deleteAllResumeVersions(resumeId: String = "primary_resume") {
        resumeVersionDao.deleteAllVersionsForResume(resumeId)
    }

    fun getJobs(): Flow<List<JobTarget>> {
        return jobDao.getAllJobs().map { entities ->
            entities.mapNotNull { entity ->
                try {
                    jobAdapter.fromJson(entity.jsonContent)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    suspend fun saveJobTarget(job: JobTarget) {
        val json = jobAdapter.toJson(job)
        jobDao.insertJob(JobEntity(id = job.id, title = job.title, company = job.company, jsonContent = json))
    }

    suspend fun deleteJob(id: String) {
        jobDao.deleteJob(id)
    }

    fun getCoverLetters(): Flow<List<CoverLetterEntity>> {
        return coverLetterDao.getAllCoverLetters()
    }

    suspend fun saveCoverLetter(letter: CoverLetter) {
        coverLetterDao.insertCoverLetter(
            CoverLetterEntity(
                id = letter.id,
                jobTitle = letter.jobTitle,
                company = letter.company,
                content = letter.content,
                tone = letter.tone,
                createdAt = letter.dateCreated
            )
        )
    }

    suspend fun deleteCoverLetter(id: String) {
        coverLetterDao.deleteCoverLetter(id)
    }

    fun getInterviewSessions(): Flow<List<InterviewSession>> {
        return interviewDao.getAllInterviews().map { entities ->
            entities.mapNotNull { entity ->
                try {
                    interviewAdapter.fromJson(entity.jsonContent)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    suspend fun saveInterviewSession(session: InterviewSession) {
        val json = interviewAdapter.toJson(session)
        interviewDao.insertInterview(
            InterviewEntity(
                id = session.id,
                jobTitle = session.jobTitle,
                company = session.company,
                jsonContent = json,
                createdAt = session.startedAt
            )
        )

        // Automatically map to InterviewPracticeSession and InterviewQuestionFeedback
        try {
            val questionsAskedList = mutableListOf<String>()
            val candidateAnswersList = mutableListOf<String>()
            val feedbacksList = mutableListOf<String>()
            val strengthsList = mutableListOf<String>()
            val improvementsList = mutableListOf<String>()
            val modelAnswersList = mutableListOf<String>()
            val questionFeedbacks = mutableListOf<InterviewQuestionFeedback>()
            var totalScore = 0
            var scoreCount = 0
            var lastUserAnswer = ""
            var qIndex = 1

            for (i in session.messages.indices) {
                val msg = session.messages[i]
                if (msg.sender == "ai") {
                    questionsAskedList.add(msg.text)
                    msg.feedback?.let { fb ->
                        if (fb.score > 0) {
                            totalScore += fb.score
                            scoreCount++
                        }
                        if (fb.starRating.isNotBlank()) {
                            feedbacksList.add("Q$qIndex: ${fb.starRating} (Score: ${fb.score}/100)")
                        }
                        if (fb.strengths.isNotEmpty()) {
                            strengthsList.addAll(fb.strengths)
                        }
                        if (fb.improvements.isNotEmpty()) {
                            improvementsList.addAll(fb.improvements)
                        }
                        if (fb.modelAnswerSuggestion.isNotBlank()) {
                            modelAnswersList.add(fb.modelAnswerSuggestion)
                        }

                        val priorQuestion = if (i >= 2 && session.messages[i - 2].sender == "ai") {
                            session.messages[i - 2].text
                        } else {
                            session.messages.firstOrNull { it.sender == "ai" }?.text ?: ""
                        }

                        questionFeedbacks.add(
                            InterviewQuestionFeedback(
                                sessionId = session.id,
                                questionNumber = qIndex++,
                                questionText = priorQuestion,
                                candidateAnswer = lastUserAnswer,
                                aiFeedback = fb.starRating,
                                score = fb.score,
                                starRating = fb.starRating,
                                strengths = fb.strengths.joinToString("\n• "),
                                improvements = fb.improvements.joinToString("\n• "),
                                modelAnswerSuggestion = fb.modelAnswerSuggestion,
                                clarityScore = fb.clarityScore,
                                pacingScore = fb.pacingScore,
                                pacingWpm = fb.wordsPerMinute,
                                clarityFeedback = fb.clarityFeedback,
                                pacingFeedback = fb.pacingFeedback,
                                createdAt = msg.timestamp
                            )
                        )
                    }
                } else if (msg.sender == "user") {
                    candidateAnswersList.add(msg.text)
                    lastUserAnswer = msg.text
                }
            }

            val lastFeedback = session.messages.lastOrNull { it.sender == "ai" }?.feedback
            val avgScore = if (scoreCount > 0) (totalScore / scoreCount) else 0
            val practiceSession = InterviewPracticeSession(
                id = session.id,
                jobTitle = session.jobTitle,
                companyName = session.company,
                interviewMode = session.mode,
                questionsAsked = questionsAskedList.joinToString("\n\n---\n\n"),
                candidateResponse = candidateAnswersList.joinToString("\n\n---\n\n"),
                aiFeedback = if (feedbacksList.isNotEmpty()) feedbacksList.joinToString("\n\n") else "Interview in progress",
                overallScore = avgScore,
                questionCount = questionsAskedList.size,
                strengths = strengthsList.distinct().joinToString("\n• "),
                improvements = improvementsList.distinct().joinToString("\n• "),
                modelAnswers = modelAnswersList.joinToString("\n\n---\n\n"),
                rawMessagesJson = json,
                timestamp = session.startedAt,
                clarityScore = lastFeedback?.clarityScore ?: 0,
                pacingScore = lastFeedback?.pacingScore ?: 0,
                pacingWpm = lastFeedback?.wordsPerMinute ?: 0,
                clarityFeedback = lastFeedback?.clarityFeedback ?: "",
                pacingFeedback = lastFeedback?.pacingFeedback ?: ""
            )

            interviewPracticeDao.insertSession(practiceSession)
            if (questionFeedbacks.isNotEmpty()) {
                interviewPracticeDao.insertQuestionFeedbacks(questionFeedbacks)
            }
        } catch (_: Exception) {
            // Mapping failure should not prevent primary interview session save
        }
    }

    fun getPastInterviewPracticeSessions(): Flow<List<InterviewPracticeSession>> {
        return interviewPracticeDao.getAllSessions()
    }

    fun getPastInterviewPracticeSessionsWithQuestions(): Flow<List<InterviewPracticeSessionWithQuestions>> {
        return interviewPracticeDao.getAllSessionsWithQuestions()
    }

    fun getInterviewPracticeSession(sessionId: String): Flow<InterviewPracticeSession?> {
        return interviewPracticeDao.getSessionById(sessionId)
    }

    suspend fun saveInterviewPracticeSession(
        session: InterviewPracticeSession,
        questions: List<InterviewQuestionFeedback> = emptyList()
    ) {
        interviewPracticeDao.insertSession(session)
        if (questions.isNotEmpty()) {
            interviewPracticeDao.insertQuestionFeedbacks(questions)
        }
    }

    suspend fun deleteInterviewPracticeSession(sessionId: String) {
        interviewPracticeDao.deleteSessionById(sessionId)
    }

    suspend fun clearAllInterviewPracticeSessions() {
        interviewPracticeDao.clearAllSessions()
    }

    suspend fun analyzeAtsAndImpact(resume: ResumeProfile, job: JobTarget): AtsAnalysisResult {
        return geminiService.analyzeAtsAndImpact(resume, job)
    }

    suspend fun compareSkillsMatrix(resume: ResumeProfile, job: JobTarget): SkillsMatrixReport {
        return geminiService.compareSkillsMatrix(resume, job)
    }

    suspend fun generateCoverLetter(resume: ResumeProfile, job: JobTarget, tone: String): CoverLetter {
        return geminiService.generateCoverLetter(resume, job, tone)
    }

    suspend fun generateCoverLetter(
        resume: Resume,
        jobDescription: String,
        tone: String = "Professional & Results-Oriented",
        jobTitle: String = "Target Position",
        company: String = "Target Company"
    ): CoverLetter {
        return geminiService.generateCoverLetter(resume, jobDescription, tone, jobTitle, company)
    }

    suspend fun coachInterview(
        job: JobTarget,
        resume: ResumeProfile,
        mode: String,
        history: List<ChatMessage>,
        userAnswer: String,
        durationSeconds: Int = 0,
        wordsPerMinute: Int = 0,
        isAudioRecorded: Boolean = false
    ): ChatMessage {
        return geminiService.coachInterviewResponse(
            job = job,
            resume = resume,
            mode = mode,
            history = history,
            userAnswer = userAnswer,
            durationSeconds = durationSeconds,
            wordsPerMinute = wordsPerMinute,
            isAudioRecorded = isAudioRecorded
        )
    }

    suspend fun coachInterview(
        job: JobTarget,
        resume: Resume,
        mode: String,
        history: List<ChatMessage>,
        userAnswer: String
    ): ChatMessage {
        return geminiService.coachInterviewResponse(
            job = job,
            resume = resume,
            mode = mode,
            history = history,
            userAnswer = userAnswer
        )
    }

    suspend fun generateInitialInterviewQuestion(
        resume: Resume,
        job: JobTarget,
        mode: String = "Behavioral"
    ): String {
        return geminiService.generateInitialInterviewQuestion(resume, job, mode)
    }

    suspend fun simulateInterviewQuestions(
        resume: ResumeProfile,
        job: JobTarget,
        mode: String = "All"
    ): List<SimulatedInterviewQuestion> {
        return geminiService.simulateInterviewQuestions(resume, job, mode)
    }

    fun getDriveSyncState(): DriveSyncState {
        return driveService.getSyncState()
    }

    suspend fun syncWithDrive(resume: ResumeProfile): Result<DriveSyncState> = withContext(Dispatchers.IO) {
        val backupJson = resumeAdapter.toJson(resume)
        driveService.syncWithDrive(resume, backupJson)
    }

    suspend fun analyzeRawResume(
        resumeText: String,
        jobTitle: String,
        companyName: String,
        jobDescription: String
    ): AtsAnalysisResult {
        return geminiService.analyzeRawResumeAgainstJob(resumeText, jobTitle, companyName, jobDescription)
    }

    /**
     * Calculates ATS score and actionable improvement suggestions by comparing
     * the user's saved resume data against a provided job description.
     */
    fun calculateAtsScore(
        resume: Resume,
        jobDescription: String
    ): AtsScoreResult {
        return AtsScoreCalculator.calculate(resume, jobDescription)
    }

    fun calculateAtsScore(
        resume: ResumeProfile,
        job: JobTarget
    ): AtsScoreResult {
        return AtsScoreCalculator.calculate(resume, job)
    }

    /**
     * Evaluates a resume against a target job to produce a rich ATS report
     * highlighting matched and missing critical/recommended keywords.
     */
    suspend fun evaluateAtsDetailed(
        resume: ResumeProfile,
        job: JobTarget
    ): AtsEvaluationService.AtsEvaluationReport {
        return atsEvaluationService.evaluateResume(resume, job)
    }

    /**
     * Evaluates raw unformatted resume text against a target job description.
     */
    suspend fun evaluateRawAtsDetailed(
        resumeText: String,
        jobTitle: String,
        companyName: String,
        jobDescription: String
    ): AtsEvaluationService.AtsEvaluationReport {
        return atsEvaluationService.evaluateRawResume(resumeText, jobTitle, companyName, jobDescription)
    }

    /**
     * Calculates the ATS Compatibility Score based on keyword density and formatting best practices.
     */
    suspend fun calculateAtsCompatibility(
        resumeText: String,
        jobDescription: String
    ): AtsEvaluationService.AtsCompatibilityReport {
        return atsEvaluationService.calculateAtsCompatibility(resumeText, jobDescription)
    }

    suspend fun calculateAtsCompatibility(
        resume: ResumeProfile,
        job: JobTarget
    ): AtsEvaluationService.AtsCompatibilityReport {
        return atsEvaluationService.calculateAtsCompatibility(resume, job)
    }

    /**
     * Parses uploaded or pasted raw resume text using the Gemini API, extracts structured
     * contact, experience, education, and skills fields, and persists them into the Room database.
     */
    suspend fun parseAndPopulateDatabaseFromResumeText(
        rawText: String,
        saveAsActive: Boolean = true
    ): ResumeProfile = withContext(Dispatchers.IO) {
        val parsedProfile = geminiService.parseUploadedResumeText(rawText)
        if (saveAsActive) {
            saveResumeProfile(parsedProfile)

            // Sync structured resume table
            val contactInfoStr = listOfNotNull(
                parsedProfile.email.takeIf { it.isNotBlank() },
                parsedProfile.phone.takeIf { it.isNotBlank() },
                parsedProfile.location.takeIf { it.isNotBlank() }
            ).joinToString(" | ")
            val expStr = parsedProfile.experiences.joinToString("\n\n") { exp ->
                "${exp.jobTitle} at ${exp.company} (${exp.startDate} - ${if (exp.isCurrent) "Present" else exp.endDate}, ${exp.location})\n" +
                        exp.bullets.joinToString("\n") { "• $it" }
            }
            val structured = Resume(
                id = parsedProfile.id,
                name = parsedProfile.fullName,
                fullName = parsedProfile.fullName,
                contactInfo = contactInfoStr,
                email = parsedProfile.email,
                phone = parsedProfile.phone,
                location = parsedProfile.location,
                linkedinUrl = parsedProfile.linkedinUrl,
                portfolioUrl = parsedProfile.portfolioUrl,
                targetRole = parsedProfile.targetRole,
                professionalSummary = parsedProfile.summary,
                workExperience = expStr,
                experience = expStr,
                education = parsedProfile.educations.joinToString("\n\n") { edu ->
                    "${edu.degree} in ${edu.fieldOfStudy} - ${edu.institution} (${edu.graduationYear}, ${edu.gpaOrHonors})"
                },
                skills = parsedProfile.skills.joinToString(", ") { it.name }
            )
            resumeDao.insertStructuredResume(structured)

            // Create initial version history snapshot in Room database
            createResumeVersion(
                resume = parsedProfile,
                versionName = "v1.0 - AI Extracted Document",
                changeNotes = "Extracted and structured from uploaded text using Gemini API"
            )
        }
        parsedProfile
    }
}
