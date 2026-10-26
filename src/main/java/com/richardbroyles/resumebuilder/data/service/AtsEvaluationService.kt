package com.richardbroyles.resumebuilder.data.service

import com.richardbroyles.resumebuilder.data.model.AtsAnalysisResult
import com.richardbroyles.resumebuilder.data.model.JobTarget
import com.richardbroyles.resumebuilder.data.model.ResumeProfile
import com.richardbroyles.resumebuilder.data.remote.GeminiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.collections.sortedWith
import kotlin.math.roundToInt

class AtsEvaluationService(private val geminiService: GeminiService) {
    data class DetailedKeywordReport(
        val keyword: String,
        val isMatched: Boolean,
        val importance: KeywordImportance,
        val category: String,
        val recommendation: String
    )

    enum class KeywordImportance {
        CRITICAL,
        RECOMMENDED,
        BONUS
    }

    data class AtsEvaluationReport(
        val analysisResult: AtsAnalysisResult,
        val detailedKeywords: List<DetailedKeywordReport>,
        val missingCriticalKeywords: List<String>,
        val missingRecommendedKeywords: List<String>,
        val keywordCoverageRatio: Float
    )

    enum class KeywordDensityStatus {
        OPTIMAL,
        MODERATE,
        LOW_OR_MISSING,
        STUFFING
    }

    data class KeywordDensityItem(
        val keyword: String,
        val jobFrequency: Int,
        val resumeFrequency: Int,
        val resumeDensityPercent: Float,
        val targetDensityPercent: Float,
        val status: KeywordDensityStatus,
        val category: String,
        val recommendation: String
    )

    enum class FormattingStatus {
        PASSED,
        WARNING,
        FAILED
    }

    data class FormattingRuleCheck(
        val id: String,
        val ruleName: String,
        val status: FormattingStatus,
        val scoreContribution: Int,
        val maxScore: Int,
        val detail: String,
        val recommendation: String
    )

    data class AtsCompatibilityReport(
        val atsCompatibilityScore: Int,
        val keywordDensityScore: Int,
        val formattingBestPracticesScore: Int,
        val keywordDensityItems: List<KeywordDensityItem>,
        val formattingRuleChecks: List<FormattingRuleCheck>,
        val totalResumeWords: Int,
        val totalJobWords: Int,
        val matchedKeywordsCount: Int,
        val missingKeywordsCount: Int,
        val optimalDensityKeywordsCount: Int,
        val passedFormattingRulesCount: Int,
        val totalFormattingRulesCount: Int,
        val summaryEvaluation: String,
        val keyActionItems: List<String>
    )

    suspend fun evaluateResume(resume: ResumeProfile, job: JobTarget): AtsEvaluationReport =
        withContext(
            Dispatchers.Default
        ) {
            val atsResult = geminiService.analyzeAtsAndImpact(resume, job)
            val resumeCorpus = buildResumeCorpus(resume)
            val detailedKeywords = extractAndAnalyzeKeywords(
                resumeCorpus = resumeCorpus,
                jobDescription = job.rawDescription + " " + job.requiredQualifications.joinToString(
                    " "
                ) + " " + job.preferredQualifications.joinToString(" "),
                geminiMissingKeywords = atsResult.missingKeywords,
                geminiMatchedKeywords = atsResult.matchedKeywords
            )
            val missingCritical = detailedKeywords
                .filter { !it.isMatched && it.importance == KeywordImportance.CRITICAL }
                .map { it.keyword }
            val missingRecommended = detailedKeywords
                .filter { !it.isMatched && it.importance == KeywordImportance.RECOMMENDED }
                .map { it.keyword }
            val totalKeywords = detailedKeywords.size.coerceAtLeast(1)
            val matchedCount = detailedKeywords.count { it.isMatched }
            val coverageRatio = matchedCount.toFloat() / totalKeywords.toFloat()
            AtsEvaluationReport(
                analysisResult = atsResult,
                detailedKeywords = detailedKeywords,
                missingCriticalKeywords = missingCritical,
                missingRecommendedKeywords = missingRecommended,
                keywordCoverageRatio = coverageRatio
            )
        }

    suspend fun evaluateRawResume(
        resumeText: String,
        jobTitle: String,
        companyName: String,
        jobDescription: String
    ): AtsEvaluationReport = withContext(Dispatchers.Default) {
        val atsResult = geminiService.analyzeRawResumeAgainstJob(
            resumeText = resumeText,
            jobTitle = jobTitle,
            companyName = companyName,
            jobDescription = jobDescription
        )
        val detailedKeywords = extractAndAnalyzeKeywords(
            resumeCorpus = resumeText,
            jobDescription = jobDescription,
            geminiMissingKeywords = atsResult.missingKeywords,
            geminiMatchedKeywords = atsResult.matchedKeywords
        )
        val missingCritical = detailedKeywords
            .filter { !it.isMatched && it.importance == KeywordImportance.CRITICAL }
            .map { it.keyword }
        val missingRecommended = detailedKeywords
            .filter { !it.isMatched && it.importance == KeywordImportance.RECOMMENDED }
            .map { it.keyword }
        val totalKeywords = detailedKeywords.size.coerceAtLeast(1)
        val matchedCount = detailedKeywords.count { it.isMatched }
        val coverageRatio = matchedCount.toFloat() / totalKeywords.toFloat()
        AtsEvaluationReport(
            analysisResult = atsResult,
            detailedKeywords = detailedKeywords,
            missingCriticalKeywords = missingCritical,
            missingRecommendedKeywords = missingRecommended,
            keywordCoverageRatio = coverageRatio
        )
    }

    private fun buildResumeCorpus(resume: ResumeProfile): String {
        return buildString {
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
            resume.projects.forEach { proj ->
                append(proj.title).append(" ")
                append(proj.description).append(" ")
                proj.technologies.forEach { append(it).append(" ") }
            }
            resume.certifications.forEach { append(it).append(" ") }
        }
    }

    private fun extractAndAnalyzeKeywords(
        resumeCorpus: String,
        jobDescription: String,
        geminiMissingKeywords: List<String>,
        geminiMatchedKeywords: List<String>
    ): List<DetailedKeywordReport> {
        val lowerResume = resumeCorpus.lowercase(Locale.getDefault())
        val lowerJob = jobDescription.lowercase(Locale.getDefault())
        val candidateKeywords = mutableSetOf<String>()
        geminiMatchedKeywords.forEach { kw ->
            val clean = kw.trim()
            if (clean.length > 1) candidateKeywords.add(clean)
        }
        geminiMissingKeywords.forEach { kw ->
            val clean = kw.trim()
            if (clean.length > 1) candidateKeywords.add(clean)
        }
        val commonDictionary = listOf(
            "Kotlin",
            "Jetpack Compose",
            "Coroutines",
            "Flow",
            "Room",
            "Dagger",
            "Hilt",
            "MVVM",
            "Clean Architecture",
            "REST API",
            "Retrofit",
            "GraphQL",
            "CI/CD",
            "Unit Testing",
            "UI Testing",
            "Robolectric",
            "Git",
            "Android SDK",
            "Java",
            "Performance Optimization",
            "Memory Leaks",
            "Profiling",
            "Firebase",
            "Offline First",
            "WorkManager",
            "Accessibility",
            "Design Patterns",
            "Agile",
            "Scrum",
            "Code Review",
            "Microservices",
            "Docker",
            "Kubernetes",
            "AWS",
            "GCP",
            "SQL",
            "NoSQL",
            "Python",
            "TypeScript",
            "React",
            "State Management",
            "Data Structures",
            "Algorithms",
            "Kafka",
            "Terraform",
            "Redis",
            "Swift",
            "Flutter",
            "Cross-Functional Leadership",
            "System Design",
            "Scalability",
            "API Design",
            "Security",
            "OAuth",
            "Encryption",
            "Automated Testing",
            "STAR Methodology"
        )
        for (dictWord in commonDictionary) {
            val pattern = "\\b${Regex.escape(dictWord.lowercase(Locale.getDefault()))}\b".toRegex()
            if (pattern.containsMatchIn(lowerJob)) {
                candidateKeywords.add(dictWord)
            }
        }
        val capitalizedPhraseRegex = "[A-Z][a-zA-Z0-9+#.]*(?:\\s+[A-Z][a-zA-Z0-9+#.]*)+".toRegex()
        capitalizedPhraseRegex.findAll(jobDescription).take(15).forEach { match ->
            val phrase = match.value.trim()
            if (phrase.length in 4..35 && !phrase.startsWith("The") && !phrase.startsWith("Our")) {
                candidateKeywords.add(phrase)
            }
        }
        return candidateKeywords.map { rawKeyword ->
            val keywordLower = rawKeyword.lowercase(Locale.getDefault())
            val isMatchedInResume =
                lowerResume.contains(keywordLower) || geminiMatchedKeywords.any {
                    it.equals(
                        rawKeyword,
                        ignoreCase = true
                    )
                }
            val isExplicitlyMissingInGemini =
                geminiMissingKeywords.any { it.equals(rawKeyword, ignoreCase = true) }
            val finalIsMatched = isMatchedInResume && !isExplicitlyMissingInGemini
            val importance = when {
                lowerJob.contains("required") && lowerJob.indexOf(keywordLower).let {
                    it != -1 && it < (lowerJob.indexOf(
                        "preferred"
                    ).takeIf { p -> p != -1 } ?: Int.MAX_VALUE)
                } ->
                    KeywordImportance.CRITICAL

                lowerJob.contains("must have") || lowerJob.contains("essential") -> KeywordImportance.CRITICAL
                rawKeyword in listOf(
                    "Kotlin",
                    "Jetpack Compose",
                    "Coroutines",
                    "MVVM",
                    "Clean Architecture",
                    "Android SDK"
                ) ->
                    KeywordImportance.CRITICAL

                lowerJob.contains("bonus") || lowerJob.contains("nice to have") -> KeywordImportance.BONUS
                else -> KeywordImportance.RECOMMENDED
            }
            val category = when {
                rawKeyword in listOf(
                    "Kotlin",
                    "Java",
                    "Python",
                    "SQL",
                    "TypeScript"
                ) -> "Programming Languages"

                rawKeyword in listOf(
                    "Jetpack Compose",
                    "Coroutines",
                    "Room",
                    "Hilt",
                    "Retrofit",
                    "Docker"
                ) -> "Framework & Library"

                rawKeyword in listOf(
                    "MVVM",
                    "Clean Architecture",
                    "Offline First",
                    "System Design"
                ) -> "Architecture & Methodology"

                rawKeyword in listOf(
                    "Unit Testing",
                    "CI/CD",
                    "Profiling",
                    "Code Review"
                ) -> "Engineering Practices"

                else -> "Domain Skill"
            }
            val recommendation = if (finalIsMatched) {
                "Well aligned: present in resume profile."
            } else {
                when (importance) {
                    KeywordImportance.CRITICAL -> "High Priority: Add '$rawKeyword' directly into your work experience bullet points or summary to pass hard ATS filters."
                    KeywordImportance.RECOMMENDED -> "Recommended: Weave '$rawKeyword' into a STAR achievement demonstrating measurable impact."
                    KeywordImportance.BONUS -> "Bonus: Include in Core Competencies or relevant project tools."
                }
            }
            DetailedKeywordReport(
                keyword = rawKeyword,
                isMatched = finalIsMatched,
                importance = importance,
                category = category,
                recommendation = recommendation
            )
        }.sortedWith(
            compareBy<DetailedKeywordReport> { it.isMatched }
                .thenBy { it.importance.ordinal }
                .thenBy { it.keyword }
        )
    }

    // Calculates an 'ATS Compatibility Score' based specifically on keyword density
    // and formatting best practices, providing a comprehensive audit of parser compliance.
    suspend fun calculateAtsCompatibility(
        resumeText: String,
        jobDescription: String
    ): AtsCompatibilityReport = withContext(Dispatchers.Default) {
        val resumeWords = resumeText.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val jobWords = jobDescription.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val totalResumeWords = resumeWords.size.coerceAtLeast(1)
        val totalJobWords = jobWords.size.coerceAtLeast(1)
        val keywordDensityReports =
            computeKeywordDensity(resumeText, jobDescription, totalResumeWords, totalJobWords)
        val formattingChecks = auditFormattingBestPractices(resumeText, totalResumeWords)
        val totalKeywords = keywordDensityReports.size.coerceAtLeast(1)
        val matchedCount = keywordDensityReports.count { it.resumeFrequency > 0 }
        val optimalCount = keywordDensityReports.count { it.status == KeywordDensityStatus.OPTIMAL }
        val stuffingCount =
            keywordDensityReports.count { it.status == KeywordDensityStatus.STUFFING }
        val coverageRatio = matchedCount.toFloat() / totalKeywords.toFloat()
        val optimalRatio = optimalCount.toFloat() / totalKeywords.toFloat()
        val rawCoverageScore = coverageRatio * 65f
        val rawOptimalScore = optimalRatio * 35f
        val stuffingPenalty = (stuffingCount * 6f)
        val keywordDensityScore =
            (rawCoverageScore + rawOptimalScore - stuffingPenalty).roundToInt().coerceIn(0, 100)
        val formattingScore = formattingChecks.sumOf { it.scoreContribution }.coerceIn(0, 100)
        val compositeScore =
            ((keywordDensityScore * 0.55f) + (formattingScore * 0.45f)).roundToInt()
                .coerceIn(0, 100)
        val passedFormattingCount = formattingChecks.count { it.status == FormattingStatus.PASSED }
        val missingCount = keywordDensityReports.count { it.resumeFrequency == 0 }
        val summary = when {
            compositeScore >= 85 -> "Outstanding ATS compatibility!  Your resume exhibits ideal keyword density and conforms to automated parser formatting best practices."
            compositeScore >= 70 -> "Strong ATS alignment. Addressing missing role keywords or minor formatting adjustments will boost your candidate ranking."
            compositeScore >= 50 -> "Moderate compatibility. Several critical role keywords are missing or keyword density is uneven, and some formatting standards require improvement."
            else -> "High screening risk.  The resume lacks key role terminology and contains formatting patterns that may disrupt automated ATS parsing."
        }
        val actionItems = mutableListOf<String>()
        val criticalMissing =
            keywordDensityReports.filter { it.status == KeywordDensityStatus.LOW_OR_MISSING }
                .take(3)
        if (criticalMissing.isNotEmpty()) {
            actionItems.add(
                "Incorporate missing keywords into experience or skills: ${
                    criticalMissing.joinToString(
                        ", "
                    ) { it.keyword }
                }"
            )
        }
        val stuffed = keywordDensityReports.filter { it.status == KeywordDensityStatus.STUFFING }
        if (stuffed.isNotEmpty()) {
            actionItems.add("Reduce excessive repetition of '${stuffed.joinToString { it.keyword }}' to avoid ATS spam flags.")
        }
        val failedFormatting =
            formattingChecks.filter { it.status == FormattingStatus.FAILED || it.status == FormattingStatus.WARNING }
        failedFormatting.forEach {
            actionItems.add(it.recommendation)
        }
        AtsCompatibilityReport(
            atsCompatibilityScore = compositeScore,
            keywordDensityScore = keywordDensityScore,
            formattingBestPracticesScore = formattingScore,
            keywordDensityItems = keywordDensityReports,
            formattingRuleChecks = formattingChecks,
            totalResumeWords = totalResumeWords,
            totalJobWords = totalJobWords,
            matchedKeywordsCount = matchedCount,
            missingKeywordsCount = missingCount,
            optimalDensityKeywordsCount = optimalCount,
            passedFormattingRulesCount = passedFormattingCount,
            totalFormattingRulesCount = formattingChecks.size,
            summaryEvaluation = summary,
            keyActionItems = actionItems.take(5)
        )
    }

    suspend fun calculateAtsCompatibility(
        resume: ResumeProfile,
        job: JobTarget
    ): AtsCompatibilityReport = withContext(Dispatchers.Default) {
        val resumeCorpus = buildResumeCorpus(resume)
        val jobCorpus =
            job.rawDescription + " " + job.requiredQualifications.joinToString(" ") + " " + job.preferredQualifications.joinToString(
                " "
            )
        calculateAtsCompatibility(resumeCorpus, jobCorpus)
    }

    private fun computeKeywordDensity(
        resumeText: String,
        jobDescription: String,
        totalResumeWords: Int,
        totalJobWords: Int
    ): List<KeywordDensityItem> {
        val lowerResume = resumeText.lowercase(Locale.getDefault())
        val lowerJob = jobDescription.lowercase(Locale.getDefault())
        val candidates = mutableSetOf<String>()
        val coreIndustrySkills = listOf(
            "Kotlin",
            "Jetpack Compose",
            "Coroutines",
            "Flow",
            "Room",
            "Dagger",
            "Hilt",
            "MVVM",
            "Clean Architecture",
            "REST API",
            "Retrofit",
            "GraphQL",
            "CI/CD",
            "Unit Testing",
            "UI Testing",
            "Robolectric",
            "Git",
            "Android SDK",
            "Java",
            "Python",
            "SQL",
            "NoSQL",
            "Git",
            "Profiling",
            "Memory Leaks",
            "Microservices",
            "System Design",
            "AWS",
            "GCP",
            "Redis",
            "Kafka",
            "Terraform",
            "Security",
            "Scalability",
            "Agile"
        )
        for (skill in coreIndustrySkills) {
            val pattern = "\\b${Regex.escape(skill.lowercase(Locale.getDefault()))}\\b".toRegex()
            if (pattern.containsMatchIn(lowerJob)) {
                candidates.add(skill)
            }
        }
        val capitalizeRegex = "[A-Z][a-zA-Z0-9+#.]*(?:\\s+[A-Z][a-zA-Z0-9+#.]*)+".toRegex()
        capitalizeRegex.findAll(jobDescription).take(12).forEach { m ->
            val phrase = m.value.trim()
            if (phrase.length in 4..30 && !phrase.startsWith("The") && !phrase.startsWith("Our")) {
                candidates.add(phrase)
            }
        }
        return candidates.map { keyword ->
            val kwLower = keyword.lowercase(Locale.getDefault())
            val pattern = "\\b${Regex.escape(kwLower)}\\b".toRegex()
            val jobMatches = pattern.findAll(lowerJob).count()
            val resumeMatches = pattern.findAll(lowerResume).count()
            val resumeDensity = (resumeMatches.toFloat() / totalResumeWords.toFloat()) * 100f
            val targetDensity = (jobMatches.toFloat() / totalJobWords.toFloat()) * 100f
            val status = when {
                resumeMatches == 0 -> KeywordDensityStatus.LOW_OR_MISSING
                resumeDensity > 4.5f -> KeywordDensityStatus.STUFFING
                resumeDensity in 0.6f..3.5f -> KeywordDensityStatus.OPTIMAL
                else -> KeywordDensityStatus.MODERATE
            }
            val category = when {
                keyword in listOf("Kotlin", "Java'", "Python", "SQL") -> "Programming Language"
                keyword in listOf(
                    "Jetpack Compose",
                    "Coroutines",
                    "Room",
                    "Hilt",
                    "Docker",
                    "Kubernetes"
                ) -> "Framework & Tool"

                keyword in listOf(
                    "MVVM",
                    "Clean Architecture",
                    "System Design",
                    "Agile"
                ) -> "Architecture & Process"

                else -> "Core Competency"
            }
            val recommendation = when (status) {
                KeywordDensityStatus.OPTIMAL -> "Optimal density (${
                    String.format(
                        Locale.US,
                        "%.1f",
                        resumeDensity
                    )
                }%). Well-balanced for recruiter search without trigger penalty."

                KeywordDensityStatus.LOW_OR_MISSING -> "Missing: Mention '$keyword' at least 1-2 times in your experience bullets or summary."
                KeywordDensityStatus.MODERATE -> "Present (${
                    String.format(
                        Locale.US,
                        "%.1f",
                        resumeDensity
                    )
                }%). Consider strengthening context in an accomplishment."

                KeywordDensityStatus.STUFFING -> "Warning: High density (${
                    String.format(
                        Locale.US,
                        "%.1f",
                        resumeDensity
                    )
                }%).  Rephrase instances to prevent automated spam filters."
            }
            KeywordDensityItem(
                keyword = keyword,
                jobFrequency = jobMatches,
                resumeFrequency = resumeMatches,
                resumeDensityPercent = resumeDensity,
                targetDensityPercent = targetDensity,
                status = status,
                category = category,
                recommendation = recommendation
            )
        }.sortedWith(
            compareBy<KeywordDensityItem> { it.status.ordinal }
                .thenByDescending { it.jobFrequency }
        )
    }

    private fun auditFormattingBestPractices(
        resumeText: String,
        totalResumeWords: Int
    ): List<FormattingRuleCheck> {
        val upperResume = resumeText.uppercase(Locale.getDefault())
        val lines = resumeText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val checks = mutableListOf<FormattingRuleCheck>()
        // Rule 1: Standard Section Headings
        val expectedHeadings = listOf(
            listOf("EXPERIENCE", "WORK EXPERIENCE", "EMPLOYMENT", "PROFESSIONAL EXPERIENCE"),
            listOf("EDUCATION", "ACADEMIC BACKGROUND", "UNIVERSITY"),
            listOf("SKILLS", "TECHNICAL SKILLS", "CORE COMPETENCIES"),
            listOf("SUMMARY", "PROFESSIONAL SUMMARY", "OBJECTIVE", "PROFILE")
        )
        val foundHeadingsCount = expectedHeadings.count { group ->
            group.any { heading ->
                lines.any { line ->
                    line.uppercase(Locale.getDefault())
                        .startsWith(heading) || line.uppercase(Locale.getDefault()) == heading
                }
            }
        }
        val headingScore = when {
            foundHeadingsCount >= 4 -> 20
            foundHeadingsCount == 3 -> 15
            foundHeadingsCount == 2 -> 10
            else -> 5
        }
        checks.add(
            FormattingRuleCheck(
                id = "standard_headings",
                ruleName = "Standard Section Headings",
                status = if (foundHeadingsCount >= 4) FormattingStatus.PASSED else if (foundHeadingsCount >= 3) FormattingStatus.WARNING else FormattingStatus.FAILED,
                scoreContribution = headingScore,
                maxScore = 20,
                detail = "Detected $foundHeadingsCount of 4 standard sections (Summary, Experience, Education, Skills).",
                recommendation = if (foundHeadingsCount >= 4) {
                    "ATS parsers will reliably categorize all sections."
                } else {
                    "Ensure sections use standard headers like 'WORK EXPERIENCE', 'EDUCATION', and 'SKILLS' for seamless parsing."
                }
            )
        )
        // Rule 2: Contact information & identity completeness
        val emailRegex = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}".toRegex()
        val phoneRegex =
            "(?:\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[.\\s-]?\\d{3}[.\\s-]?\\d{4}".toRegex()
        val hasEmail = emailRegex.containsMatchIn(resumeText)
        val hasPhone = phoneRegex.containsMatchIn(resumeText)
        val hasLocation = listOf(
            "San Francisco",
            "CA",
            "NY",
            "New York",
            "Austin",
            "TX",
            "Seattle",
            "Remote",
            "London",
            "USA"
        )
            .any { resumeText.contains(it, ignoreCase = true) }
        var contactScore = 0
        if (hasEmail) contactScore += 8
        if (hasPhone) contactScore += 8
        if (hasLocation) contactScore += 4
        checks.add(
            FormattingRuleCheck(
                id = "contact_info",
                ruleName = "Contact Info Completeness",
                status = if (contactScore >= 20) FormattingStatus.PASSED else if (contactScore >= 16) FormattingStatus.WARNING else FormattingStatus.FAILED,
                scoreContribution = contactScore,
                maxScore = 20,
                detail = "Email: ${if (hasEmail) "✓" else "✗"} | Phone: ${if (hasPhone) "✓" else "✗"} | Location: ${if (hasLocation) "✓" else "✗"}",
                recommendation = if (contactScore >= 20) {
                    "Contact details are standard and machine-readable."
                } else {
                    "Include a clean professional email, phone number, and city/state at the top of the resume."
                }
            )
        )
        // Rule 3: Bullet points & Strong Action Verbs
        val bulletLines = lines.filter { it.startsWith("•") || it.startsWith("-") || it.startsWith("*") }
        val actionVerbs = listOf(
            "Architected", "Engineered", "Led", "Spearheaded", "Developed", "Built",
            "Reduced", "Optimized", "Delivered", "Implemented", "Designed", "Managed",
            "Created", "Automated", "Mentored", "Resolved", "Pioneered", "Orchestrated"
        )
        val actionVerbMatches = actionVerbs.count { verb ->
            lines.any { it.contains(verb, ignoreCase = true)}
        }
        val bulletScore = when {
            bulletLines.size >= 4 && actionVerbMatches >= 3 -> 20
            bulletLines.size >= 3 && actionVerbMatches >= 2 -> 14
            else -> 6
        }
        checks.add(
            FormattingRuleCheck(
                id = "bullet_structure",
                ruleName = "Bullet Structure & Action Verbs",
                status = if (bulletScore >= 20) FormattingStatus.PASSED else if (bulletScore >= 14) FormattingStatus.WARNING else FormattingStatus.FAILED,
                scoreContribution = bulletScore,
                maxScore = 20,
                detail = "Found ${bulletLines.size} bullet points and $actionVerbMatches strong action verbs.",
                recommendation = if (bulletScore >= 20) {
                    "Strong active phrasing and bulleted readability."
                } else {
                    "Begin work experience bullets with strong past-tense action verbs (e.g., 'Spearheaded', 'Optimized', 'Architected'."
                }
            )
        )
        // Rule 4: Quantifiable metrics & STAR / XYZ Formula
        val metricPatterns = listOf(
            "\\b\\d+%\\b".toRegex(),
            "\\b\\d+(?:\\.\\d+)?\\s*(?:M|K|B|users|req|ms|x|engineers)\\b".toRegex(RegexOption.IGNORE_CASE),
            "\\$\\s*\\d+".toRegex()
        )
        val metricsFound = metricPatterns.sumOf{ p -> p.findAll(resumeText).count() }
        val metricScore = when {
            metricsFound >= 3 -> 20
            metricsFound >= 1 -> 13
            else -> 5
        }
        checks.add(
            FormattingRuleCheck(
                id = "quantifiable_impact",
                ruleName = "Quantifiable Impact & Metrics",
                status = if (metricScore >= 20) FormattingStatus.PASSED else if (metricScore >= 13) FormattingStatus.WARNING else FormattingStatus.FAILED,
                scoreContribution = metricScore,
                maxScore = 20,
                detail = "Detected $metricsFound quantifiable achievements (percentages, scales, or numbers).",
                recommendation = if (metricScore >= 20) {
                    "Excellent XYZ formula evidence: accomplishments paired with numerical impact."
                } else {
                    "Quantify results with measurable metrics."
                }
            )
        )
        // Rule 5: Resume word count & parsing cleanliness
        val isWordCountOptimal = totalResumeWords in 350..1000
        val isWordCountAcceptable = totalResumeWords in 200..1300
        val wordScore = when {
            isWordCountOptimal -> 20
            isWordCountAcceptable -> 14
            else -> 6
        }
        checks.add(
            FormattingRuleCheck(
                id = "length_and_cleanliness",
                ruleName = "Resume Length & ATS Readability",
                status = if (isWordCountOptimal) FormattingStatus.PASSED else if (isWordCountAcceptable) FormattingStatus.WARNING else FormattingStatus.FAILED,
                scoreContribution = wordScore,
                maxScore = 20,
                detail = "Resume length is $totalResumeWords words (standard ATS range: 350 ~ 950 words).",
                recommendation = if (isWordCountOptimal) {
                    "Ideal length for a 1-to-2 page ATS parse without pagination data drops."
                } else if (totalResumeWords < 350) {
                    "Consider expanding on your technical achievements to reach at least 350 words."
                } else {
                    "Condense older roles or redundant phrasing to stay under 950 words."
                }
            )
        )
        return checks
    }
}

