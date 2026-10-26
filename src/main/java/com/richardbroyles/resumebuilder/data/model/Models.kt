package com.richardbroyles.resumebuilder.data.model
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ResumeProfile(
    val id: String = "primary_resume",
    val title: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = "",
    val linkedinUrl: String = "",
    val portfolioUrl: String = "",
    val targetRole: String = "",
    val summary: String = "",
    val experiences: List<ExperienceItem> = emptyList(),
    val skills: List<SkillItem> = emptyList(),
    val educations: List<EducationItem> = emptyList(),
    val projects: List<ProjectItem> = emptyList(),
    val certifications: List<String> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class ExperienceItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val jobTitle: String = "",
    val company: String = "",
    val location: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val isCurrent: Boolean = false,
    val bullets: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SkillItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val category: String = "Technical",
    val yearsExperience: Int = 1
)

@JsonClass(generateAdapter = true)
data class EducationItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val institution: String = "",
    val degree: String = "",
    val fieldOfStudy: String = "",
    val graduationYear: String = "",
    val gpaOrHonors: String = ""
)

@JsonClass(generateAdapter = true)
data class ProjectItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val role: String = "",
    val description: String = "",
    val technologies: List<String> = emptyList(),
    val url: String = ""
)

@JsonClass(generateAdapter = true)
data class JobTarget(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val company: String = "",
    val location: String = "",
    val rawDescription: String = "",
    val requiredQualifications: List<String> = emptyList(),
    val preferredQualifications: List<String> = emptyList(),
    val dateAdded: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class AtsAnalysisResult(
    val overallScore: Int = 0,
    val keywordScore: Int = 0,
    val impactScore: Int = 0,
    val formatScore: Int = 0,
    val verbStrengthScore: Int = 0,
    val matchedKeywords: List<String> = emptyList(),
    val missingKeywords: List<String> = emptyList(),
    val summaryFeedback: String = "",
    val bulletImprovements: List<BulletImprovement> = emptyList(),
    val highImpactTips: List<String> = emptyList(),
    val groundingScore: Int = 100,
    val unverifiedKeywordsRemoved: List<String> = emptyList(),
    val hallucinationWarning: String? = null
)

@JsonClass(generateAdapter = true)
data class BulletImprovement(
    val original: String = "",
    val improvedStar: String = "",
    val rationale: String = "",
    val metricsAdded: String = "",
    val groundingNote: String = "",
    val isFactChecked: Boolean = true
)

enum class SkillMatchStatus {
    MATCHED,
    TRANSFERABLE,
    GAP
}

@JsonClass(generateAdapter = true)
data class QualificationComparison(
    val qualification: String = "",
    val status: SkillMatchStatus = SkillMatchStatus.GAP,
    val matchingCandidateSkill: String? = null,
    val candidateEvidence: String? = null,
    val bridgingAdvice: String = "",
    val isVerifiedResume: Boolean = true,
    val isVerifiedInResume: Boolean = true
)

@JsonClass(generateAdapter = true)
data class SkillsMatrixReport(
    val matchPercentage: Int = 0,
    val totalQualifications: Int = 0,
    val matchedCount: Int = 0,
    val transferableCount: Int = 0,
    val gapCount: Int = 0,
    val items: List<QualificationComparison> = emptyList(),
    val keyStrengths: List<String> = emptyList(),
    val priorityFocusAreas: List<String> = emptyList(),
    val groundingScore: Int = 100,
    val ungroundedClaimsFiltered: Int = 0
)

@JsonClass(generateAdapter = true)
data class CoverLetter(
    val id: String = java.util.UUID.randomUUID().toString(),
    val jobTitle: String = "",
    val company: String = "",
    val tone: String = "Professional & Results-Oriented",
    val content: String = "",
    val keyAchievementsCited: List<String> = emptyList(),
    val dateCreated: Long = System.currentTimeMillis(),
    val isFactChecked: Boolean = true,
    val groundingNotice: String = "100% verified against candidate resume"
)

@JsonClass(generateAdapter = true)
data class GroundingVerificationResult(
    val isGrounded: Boolean = true,
    val groundingScore: Int = 100,
    val verifiedClaims: List<String> = emptyList(),
    val potentialHallucinations: List<String> = emptyList(),
    val feedback: String = ""
)

@JsonClass(generateAdapter = true)
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String = "ai",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val feedback: AnswerCoachingFeedback? = null,
    val audioPath: String? = null,
    val audioDurationSeconds: Int = 0,
    val wordsPerMinute: Int = 0
)

@JsonClass(generateAdapter = true)
data class AnswerCoachingFeedback(
    val score: Int = 0,
    val starRating: String = "",
    val strengths: List<String> = emptyList(),
    val improvements: List<String> = emptyList(),
    val modelAnswerSuggestion: String = "",
    val clarityScore: Int = 85,
    val pacingScore: Int = 85,
    val wordsPerMinute: Int = 145,
    val durationSeconds: Int = 0,
    val pacingAssessment: String = "Optimal Cadence (145 WPM)",
    val clarityFeedback: String = "",
    val pacingFeedback: String = "",
    val fillerWordsDetected: List<String> = emptyList(),
    val deliveryTips: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class InterviewSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val jobTitle: String = "",
    val company: String = "",
    val mode: String = "Behavioral (STAR)",
    val messages: List<ChatMessage> = emptyList(),
    val startedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class DriveSyncState(
    val isConnected: Boolean = true,
    val userEmail: String = "rbroyles1k@gmail.com",
    val isSyncing: Boolean = false,
    val lastSyncTimestamp: Long = 0L,
    val syncStatusMessage: String = "Ready to sync with Google Drive",
    val syncedFilesCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class SimulatedInterviewQuestion(
    val id: String = java.util.UUID.randomUUID().toString(),
    val question: String = "",
    val category: String = "Behavioral (STAR)",
    val difficulty: String = "Senior / Lead",
    val rationale: String = "",
    val keySkillsTested: List<String> = emptyList(),
    val suggestedStarOutline: String = "",
    val sampleTip: String = ""
)