package com.richardbroyles.resumebuilder.util

import com.richardbroyles.resumebuilder.data.local.entity.Resume
import com.richardbroyles.resumebuilder.data.model.JobTarget
import com.richardbroyles.resumebuilder.data.model.ResumeProfile
import java.util.Locale
import kotlin.math.roundToInt

data class AtsScoreResult(
    val score: Int,
    val matchedKeywords: List<String>,
    val missingKeywords: List<String>,
    val improvementSuggestions: List<String>,
    val keywordMatchRate: Float,
    val skillsMatchScore: Int,
    val experienceRelevanceScore: Int,
    val formattingScore: Int,
    val summaryEvaluation: String,
    val strengths: List<String>
) {
    val overallScore: Int get() = score
}

object AtsScoreCalculator {
    private val standardTechnicalDictionary = listOf(
        "Kotlin", "Jetpack Compose", "Coroutines", "Flow", "Room", "Hilt", "Dagger",
        "MVVM", "MVI", "Clean Architecture", "REST API", "GraphQL", "CI/CD", "Docker",
        "Kubernetes", "Unit Testing", "UI Testing", "Robolectric", "Android SDK", "Java",
        "Python", "SQL", "NoSQL", "Git", "Profiling", "Memory Leaks", "Microservices",
        "System Design", "AWS", "GCP", "Redis", "Kafka", "Terraform", "Security",
        "Scalability", "Agile", "Scrum", "Code Review", "Data Structures", "Algorithms",
        "Retrofit", "Ktor", "WorkManager", "State Management", "Performance Optimization"
    )
    private val strongActionVerbs = listOf(
        "Architected", "Engineered", "Spearheaded", "Led", "Designed", "Developed",
        "Implemented", "Optimized", "Reduced", "Delivered", "Automated", "Mentored",
        "Refactored", "Built", "Orchestrated", "Accelerated", "Scaled", "Revamped"
    )
    fun calculate(
        resume: Resume,
        jobDescription: String
    ) : AtsScoreResult {
        val resumeCorpus = buildString {
            append(resume.getEffectiveName()).append(" ")
            append(resume.targetRole).append(" ")
            append(resume.getEffectiveSummary()).append(" ")
            append(resume.getEffectiveContactInfo()).append(" ")
            append(resume.getEffectiveWorkExperience()).append(" ")
            append(resume.education).append(" ")
            append(resume.skills).append(" ")
        }
        return evaluate(
            resumeCorpus = resumeCorpus,
            workExperienceText = resume.getEffectiveWorkExperience(),
            skillsText = resume.skills,
            contactInfoText = resume.getEffectiveContactInfo(),
            jobDescription = jobDescription
        )
    }

    fun calculate(
        name: String,
        contactInfo: String,
        workExperience: String,
        education: String,
        skills: String,
        jobDescription: String,
        targetRole: String = "",
        summary: String = ""
    ): AtsScoreResult {
        val resumeCorpus = buildString {
            append(name).append(" ")
            append(targetRole).append(" ")
            append(summary).append(" ")
            append(contactInfo).append(" ")
            append(workExperience).append(" ")
            append(education).append(" ")
            append(skills).append(" ")
        }
        return evaluate(
            resumeCorpus = resumeCorpus,
            workExperienceText = workExperience,
            skillsText = skills,
            contactInfoText = contactInfo,
            jobDescription = jobDescription
        )
    }

    // Compares a structured resume against a job target.
    fun calculate(
        resume: ResumeProfile,
        job: JobTarget
    ): AtsScoreResult {
        val resumeCorpus = buildString {
            append(resume.fullName).append(" ")
            append(resume.targetRole).append(" ")
            append(resume.summary).append(" ")
            resume.skills.forEach { append(it.name).append(" ") }
            resume.experiences.forEach { exp ->
                append(exp.jobTitle).append(" ")
                append(exp.company).append(" ")
                exp.bullets.forEach { append(it).append(" ") }
            }
            resume.educations.forEach { edu ->
                append(edu.degree).append(" ")
                append(edu.fieldOfStudy).append(" ")
                append(edu.institution).append(" ")
            }
        }
        val workExp = resume.experiences.joinToString("\n") { exp ->
            "${exp.jobTitle} at ${exp.company}\n" + exp.bullets.joinToString("\n") { "• $it"}
        }
        val skillsStr = resume.skills.joinToString(", ") { it.name }
        val contactStr = listOfNotNull(
            resume.email.takeIf { it.isNotBlank() },
            resume.phone.takeIf { it.isNotBlank() },
            resume.location.takeIf { it.isNotBlank() }
        ).joinToString(" | ")
        val jobCorpus = job.rawDescription + " " + job.requiredQualifications.joinToString(" ") + " " + job.preferredQualifications.joinToString(" ")
        return evaluate(
            resumeCorpus = resumeCorpus,
            workExperienceText = workExp,
            skillsText = skillsStr,
            contactInfoText = contactStr,
            jobDescription = jobCorpus
        )
    }

    // Compares a structured Resume Profile against a job description string.
    fun calculate(
        resume: ResumeProfile,
        jobDescription: String
    ): AtsScoreResult {
        val resumeCorpus = buildString {
            append(resume.fullName).append(" ")
            append(resume.targetRole).append(" ")
            append(resume.summary).append(" ")
            resume.skills.forEach { append(it.name).append(" ") }
            resume.experiences.forEach { exp ->
                append(exp.jobTitle).append(" ")
                append(exp.company).append(" ")
                exp.bullets.forEach { append(it).append(" ") }
            }
            resume.educations.forEach { edu ->
                append(edu.degree).append(" ")
                append(edu.fieldOfStudy).append(" ")
                append(edu.institution).append(" ")
            }
        }
        val workExp = resume.experiences.joinToString("\n") { exp ->
            "${exp.jobTitle} at ${exp.company}\n" + exp.bullets.joinToString("\n") { "• $it"}
        }
        val skillsStr = resume.skills.joinToString(", ") { it.name }
        val contactStr = listOfNotNull(
            resume.email.takeIf { it.isNotBlank() },
            resume.phone.takeIf { it.isNotBlank() },
            resume.location.takeIf { it.isNotBlank() }
        ).joinToString(" | ")
        return evaluate(
            resumeCorpus = resumeCorpus,
            workExperienceText = workExp,
            skillsText = skillsStr,
            contactInfoText = contactStr,
            jobDescription = jobDescription
        )
    }

    // Compares raw resume against a job description.
    fun calculate(
        resumeText: String,
        jobDescription: String
    ): AtsScoreResult {
        return evaluate(
            resumeCorpus = resumeText,
            workExperienceText = resumeText,
            skillsText = resumeText,
            contactInfoText = resumeText,
            jobDescription = jobDescription
        )
    }

    private fun evaluate(
        resumeCorpus: String,
        workExperienceText: String,
        skillsText: String,
        contactInfoText: String,
        jobDescription: String
    ): AtsScoreResult {
        val lowerResume = resumeCorpus.lowercase(Locale.getDefault())
        val lowerJob = jobDescription.lowercase(Locale.getDefault())
        val lowerWorkExp = workExperienceText.lowercase(Locale.getDefault())
        val lowerSkills = skillsText.lowercase(Locale.getDefault())
        // 1. Extract candidate keywords from Job Description
        val jobKeywords = extractKeywordsFromJobDescription(jobDescription, lowerJob)
        // 2. Classify keywords as Matched or Missing
        val matchedKeywords = mutableListOf<String>()
        val missingKeywords = mutableListOf<String>()
        jobKeywords.forEach { keyword ->
            val kwLower = keyword.lowercase(Locale.getDefault())
            val pattern = "\\b${Regex.escape(kwLower)}\\b".toRegex()
            if (pattern.containsMatchIn(lowerResume)) {
                matchedKeywords.add(keyword)
            } else {
                missingKeywords.add(keyword)
            }
        }
        val totalKeywords = jobKeywords.size.coerceAtLeast(1)
        val keywordMatchRate = matchedKeywords.size.toFloat() / totalKeywords.toFloat()
        //3. Skills Match Score
        val skillsMatchCount = jobKeywords.count { kw ->
            val kwLower = kw.lowercase(Locale.getDefault())
            lowerSkills.contains(kwLower) || lowerResume.contains(kwLower)
        }
        val skillsMatchScore = ((skillsMatchCount.toFloat() / totalKeywords.toFloat()) * 100f)
            .roundToInt()
            .coerceIn(0, 100)
        // 4. Work Experience Relevance Score
        val expKeywordsMatched = jobKeywords.count { kw ->
            val kwLower = kw.lowercase(Locale.getDefault())
            lowerWorkExp.contains(kwLower)
        }
        val actionVerbsFound = strongActionVerbs.count { verb ->
            workExperienceText.contains(verb, ignoreCase = true)
        }
        val hasMetrics = listOf("%", "$", "K", "M","users", "ms", "x", "latency").count { metric ->
            lowerWorkExp.contains(metric)
        }
        val expKeywordRatio = (expKeywordsMatched.toFloat() / totalKeywords.toFloat()).coerceIn(0f, 1f)
        val actionVerbBonus = (actionVerbsFound.coerceAtMost(5) * 5)
        val metricBonus = (hasMetrics.coerceAtMost(3) * 5)
        val experienceRelevanceScore = ((expKeywordRatio * 60f) + actionVerbBonus + metricBonus).roundToInt().coerceIn(0, 100)
        // 5. Formatting & Structure
        var formattingScore = 30 // baseline value
        val emailRegex = "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}".toRegex()
        val phoneRegex = "(?:\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}".toRegex()
        if (emailRegex.containsMatchIn(contactInfoText) || emailRegex.containsMatchIn(resumeCorpus)) formattingScore += 20
        if (phoneRegex.containsMatchIn(contactInfoText) || phoneRegex.containsMatchIn(resumeCorpus)) formattingScore += 15
        if (workExperienceText.lines().any { it.trim().startsWith("•") || it.trim().startsWith("-") }) formattingScore += 20
        val wordCount = resumeCorpus.split("\\s+".toRegex()).count { it.isNotBlank() }
        if (wordCount in 30..1500) formattingScore += 15
        formattingScore = formattingScore.coerceIn(0, 100)
        // 6. Overall composite score(Weighted: 50/30/20)
        val compositeScore = ((skillsMatchScore * 0.50f) + (experienceRelevanceScore * 0.30f) + (formattingScore * 0.20f))
            .roundToInt()
            .coerceIn(0, 100)
        // 7. Generate targeted improvement suggestions
        val suggestions = mutableListOf<String>()
        if (missingKeywords.isNotEmpty()) {
            val topMissing = missingKeywords.take(4).joinToString(", ")
            suggestions.add("Add missing key technologies to your resume: $topMissing")
            suggestions.add("Incorporate missing keywords ($topMissing) directly into your work experience bullet points to demonstrate practical application.")
        }
        if (hasMetrics < 2) {
            suggestions.add("Quantify your achievements using the XYZ formula.")
        }
        if (actionVerbsFound < 3) {
            suggestions.add("Begin work experience bullet points with strong past-tense action verbs like 'Architected', 'Spearheaded', or 'Optimized'.")
        }
        if (!emailRegex.containsMatchIn(contactInfoText) && !emailRegex.containsMatchIn(resumeCorpus)) {
            suggestions.add("Ensure your professional email address is clearly visible at the top of your resume.")
        }
        if (!workExperienceText.lines().any { it.trim().startsWith("•") || it.trim().startsWith("-")}) {
            suggestions.add("Format work experience accomplishments using bullet points for reliable automated ATS parsing.")
        }
        // 8. Generate strengths
        val strengths = mutableListOf<String>()
        if (matchedKeywords.isNotEmpty()) {
            strengths.add("Strong alignment on core technologies: ${matchedKeywords.take(4).joinToString(", ")}.")
        }
        if (actionVerbsFound >= 3) {
            strengths.add("Effective use of strong action verbs across work experience")
        }
        if (hasMetrics >= 2) {
            strengths.add("Good inclusion of quantifiable metrics and measurable outcomes.")
        }
        if (formattingScore >= 75) {
            strengths.add("Clean structure and machine-readable contact information.")
        }
        val summary = when {
            compositeScore >= 85 -> "Excellent ATS compatibility!"
            compositeScore >= 70 -> "Good ATS compatibility"
            compositeScore >= 50 -> "Moderate Match."
            else -> "Low ATS Match. Significant skill and terminology gaps detected."
        }
        return AtsScoreResult(
            score = compositeScore,
            matchedKeywords = matchedKeywords,
            missingKeywords = missingKeywords,
            improvementSuggestions = suggestions,
            keywordMatchRate = keywordMatchRate,
            skillsMatchScore = skillsMatchScore,
            experienceRelevanceScore = experienceRelevanceScore,
            formattingScore = formattingScore,
            summaryEvaluation = summary,
            strengths = strengths
        )
    }

    private fun extractKeywordsFromJobDescription(
        jobDescription: String,
        lowerJob: String
    ): List<String> {
        val extracted = linkedSetOf<String>()
        // 1. Match from standard technical dictionary
        for (term in standardTechnicalDictionary) {
            val pattern = "\\b${Regex.escape(term.lowercase(Locale.getDefault()))}\b".toRegex()
            if (pattern.containsMatchIn(lowerJob)) {
                extracted.add(term)
            }
        }
        // 2. Extract capitalized technical phrases
        val phraseRegex = "[A-Z][a-zA-Z0-9+#.]*(?:\\s+[A-Z][a-zA-Z0-9+#.]*)+".toRegex()
        phraseRegex.findAll(jobDescription).take(22).forEach { match ->
            val phrase = match.value.trim()
            if (phrase.length in 4..30 && !phrase.startsWith("The") && !phrase.startsWith("Our") && !phrase.startsWith("We") && !phrase.startsWith("You")) {
                extracted.add(phrase)
            }
        }
        // 3. Fallback - if no keywords found, extract non-trivial words from description
        if (extracted.isEmpty()) {
            val words = jobDescription.split("\\W+".toRegex())
                .filter { it.length > 3 && it.lowercase() !in listOf("with", "have", "from", "will", "this", "that", "your", "they", "been") }
                .distinct()
                .take(6)
            extracted.addAll(words)
        }
        return extracted.toList()
    }
}
