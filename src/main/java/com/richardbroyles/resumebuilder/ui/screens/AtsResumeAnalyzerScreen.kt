package com.richardbroyles.resumebuilder.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.richardbroyles.resumebuilder.data.model.ResumeProfile
import com.richardbroyles.resumebuilder.ui.components.AtsCompatibilityHeroCard
import com.richardbroyles.resumebuilder.ui.components.AtsScoreFeedbackCard
import com.richardbroyles.resumebuilder.ui.components.AtsScoreOptimizationCard
import com.richardbroyles.resumebuilder.ui.components.AtsScoreProgressBar
import com.richardbroyles.resumebuilder.ui.components.FormattingBestPracticesSection
import com.richardbroyles.resumebuilder.ui.components.KeywordDensitySection
import com.richardbroyles.resumebuilder.ui.components.ResumeExportButton
import com.richardbroyles.resumebuilder.util.SampleData
import com.richardbroyles.resumebuilder.viewmodel.ResumeViewModel

@Composable
fun AtsResumeAnalyzerScreen(
    viewModel: ResumeViewModel,
    modifier: Modifier = Modifier,
    onNavigateToEditor: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val allResumes by viewModel.allResumesState.collectAsState()
    val activeResume by viewModel.resumeState.collectAsState()
    val rawAnalysis by viewModel.rawAtsAnalysis.collectAsState()
    val compatibilityReport by viewModel.atsCompatibilityReport.collectAsState()
    val isAnalyzing by viewModel.isAnalyzingRawAts.collectAsState()
    val isCalculatingCompatibility by viewModel.isCalculatingAtsCompatibility.collectAsState()
    val isParsingResume by viewModel.isParsingResume.collectAsState()

    var currentAnalysisTab by remember { mutableStateOf(0) } // 0: Keyword Density, 1: Formatting Best Practices, 2: AI Suggestions
    var resumeSourceTab by remember { mutableStateOf(0) } // 0: Paste text, 1: Pick from Room DB
    var resumeInputText by remember {
        mutableStateOf(
            buildString {
                append("Alex Morgan • Senior Full-Stack & Mobile Engineer\n")
                append("Email: alex.morgan.dev@gmail.com | Phone: (415) 555-0192 | San Francisco, CA\n\n")
                append("SUMMARY:\n")
                append("Results-driven Senior Software Engineer with 6+ years of experience architecting high-scale cloud platforms, responsive Android mobile apps, and distributed microservices.\n\n")
                append("EXPERIENCE:\n")
                append("• Nexus Cloud Technologies - Senior Software Engineer (2022 - Present)\n")
                append("  - Architected high-throughput Kotlin and GraphQL microservices serving 4.2M daily active users with 99.99% uptime.\n")
                append("  - Reduced p99 API response latency by 42% through distributed Redis caching and query batching.\n")
                append("  - Led cross-functional squad of 7 engineers delivering real-time telemetry streaming in Jetpack Compose.\n\n")
                append("SKILLS:\n")
                append("Kotlin, Jetpack Compose, Android SDK, Coroutines, Room, GraphQL, System Architecture, GCP, Docker, Kubernetes, CI/CD, Microservices")
            }
        )
    }

    var targetJobTitle by remember { mutableStateOf("Senior Android / Mobile Solutions Architect") }
    var targetCompanyName by remember { mutableStateOf("Starlight Global Systems") }
    var targetJobDescription by remember {
        mutableStateOf(
            """
Starlight Global Systems is seeking a Senior Android / Mobile Solutions Architect to drive next-generation connected mobile experiences.

Key Responsibilities:
- Architect, build, and deploy mission-critical Android applications using modern Kotlin, Jetpack Compose, and M3 standards.
- Collaborate with cloud teams to design high-throughput REST and GraphQL APIs.
- Optimize application performance, frame rates, and memory footprint.
- Mentor engineers, lead code reviews, and champion engineering best practices.
- Lead CI/CD automation pipelines and test-driven development (TDD).

Qualifications & Skills:
- 5+ years of production Android development with Kotlin & Jetpack Compose.
- Deep expertise in Android concurrency, Coroutines, Flow, and Room SQLite.
- Strong knowledge of distributed microservices, REST/GraphQL APIs, and cloud infrastructure.
- Experience with performance profiling, memory leak detection, and automated testing.
- Track record of mentoring engineering squads and technical leadership.
            """.trimIndent()
        )
    }

    var showResumeDropdown by remember { mutableStateOf(false) }

    fun formatResumeForText(resume: ResumeProfile): String {
        return buildString {
            append("${resume.fullName.ifEmpty { "Candidate" }} • ${resume.targetRole.ifEmpty { "Specialist" }}\n")
            val contact = listOfNotNull(
                resume.email.takeIf { it.isNotEmpty() },
                resume.phone.takeIf { it.isNotEmpty() },
                resume.location.takeIf { it.isNotEmpty() }
            ).joinToString(" | ")
            if (contact.isNotEmpty()) append("$contact\n\n")

            if (resume.summary.isNotEmpty()) {
                append("PROFESSIONAL SUMMARY:\n${resume.summary}\n\n")
            }

            if (resume.experiences.isNotEmpty()) {
                append("WORK EXPERIENCE:\n")
                resume.experiences.forEach { exp ->
                    append("• ${exp.jobTitle} at ${exp.company} (${exp.startDate} - ${if (exp.isCurrent) "Present" else exp.endDate})\n")
                    exp.bullets.forEach { b -> append("  - $b\n") }
                }
                append("\n")
            }

            if (resume.skills.isNotEmpty()) {
                append("SKILLS:\n${resume.skills.joinToString(", ") { it.name }}\n\n")
            }

            if (resume.educations.isNotEmpty()) {
                append("EDUCATION:\n")
                resume.educations.forEach { edu ->
                    append("${edu.degree} in ${edu.fieldOfStudy} - ${edu.institution} (${edu.graduationYear})\n")
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Header Banner
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Gemini ATS Resume Analyzer",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Analyze resume vs job posting for instant ATS score & actionable suggestions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        // --- STEP 1: CANDIDATE RESUME INPUT ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = CenterVertically
                    ) {
                        Row(verticalAlignment = CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "1",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Candidate Resume Input",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        // Load Sample Candidate Button
                        OutlinedButton(
                            onClick = {
                                resumeInputText = formatResumeForText(SampleData.defaultResume)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Sample Resume", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tab selector: Paste text vs Choose from Room DB
                    TabRow(
                        selectedTabIndex = resumeSourceTab,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        divider = {}
                    ) {
                        Tab(
                            selected = resumeSourceTab == 0,
                            onClick = { resumeSourceTab = 0 },
                            text = { Text("Input / Paste Resume", style = MaterialTheme.typography.labelMedium) }
                        )
                        Tab(
                            selected = resumeSourceTab == 1,
                            onClick = { resumeSourceTab = 1 },
                            text = { Text("Pick from Room DB (${allResumes.size})", style = MaterialTheme.typography.labelMedium) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (resumeSourceTab == 1) {
                        // Dropdown to pick from Room DB
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { showResumeDropdown = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("select_room_resume_btn"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Selected: ${activeResume.title.ifEmpty { activeResume.targetRole }}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null
                                )
                            }

                            DropdownMenu(
                                expanded = showResumeDropdown,
                                onDismissRequest = { showResumeDropdown = false }
                            ) {
                                allResumes.forEach { r ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = r.title.ifEmpty { r.targetRole.ifEmpty { "Resume" } },
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "${r.fullName} • ${r.experiences.size} roles, ${r.skills.size} skills",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.selectActiveResume(r)
                                            resumeInputText = formatResumeForText(r)
                                            showResumeDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    OutlinedTextField(
                        value = resumeInputText,
                        onValueChange = { resumeInputText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("resume_input_textfield"),
                        label = { Text("Resume Content (Summary, Experience, Skills, Education)") },
                        placeholder = { Text("Paste or type your resume details here...") },
                        minLines = 7,
                        maxLines = 14,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${resumeInputText.length} characters • ${resumeInputText.split("\\s+".toRegex()).filter { it.isNotEmpty() }.size} words",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.align(Alignment.End)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            viewModel.parseAndImportResume(resumeInputText) {
                                resumeSourceTab = 1
                            }
                        },
                        enabled = resumeInputText.isNotBlank() && !isParsingResume,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("parse_and_populate_db_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isParsingResume) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Extracting with Gemini AI...", style = MaterialTheme.typography.labelMedium)
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Parse & Populate Database with Gemini AI",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        // --- STEP 2: TARGET JOB DESCRIPTION INPUT ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = CenterVertically
                    ) {
                        Row(verticalAlignment = CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "2",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Target Job Description",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val sample = SampleData.sampleJobs.firstOrNull()
                                if (sample != null) {
                                    targetJobTitle = sample.title
                                    targetCompanyName = sample.company
                                    targetJobDescription = sample.rawDescription
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Sample Job", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = targetJobTitle,
                            onValueChange = { targetJobTitle = it },
                            label = { Text("Target Job Title") },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("target_job_title_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = targetCompanyName,
                            onValueChange = { targetCompanyName = it },
                            label = { Text("Company") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("target_company_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = targetJobDescription,
                        onValueChange = { targetJobDescription = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("job_description_input"),
                        label = { Text("Job Description (Requirements, Qualifications & Responsibilities)") },
                        placeholder = { Text("Paste the target job requirements and duties...") },
                        minLines = 7,
                        maxLines = 14,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${targetJobDescription.length} characters",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }

        // --- STEP 3: ANALYZE ACTION BUTTONS ---
        item {
            val isBusy = isAnalyzing || isCalculatingCompatibility
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (resumeInputText.isBlank() || targetJobDescription.isBlank()) {
                                Toast.makeText(context, "Please input both resume and job description.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.analyzeResumeAgainstJob(
                                resumeText = resumeInputText,
                                jobTitle = targetJobTitle,
                                companyName = targetCompanyName,
                                jobDescription = targetJobDescription
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("analyze_ats_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        enabled = !isBusy
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Calculating ATS Score...",
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Calculate ATS Compatibility Score",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    if (rawAnalysis != null || compatibilityReport != null) {
                        OutlinedButton(
                            onClick = { viewModel.clearRawAtsAnalysis() },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("clear_ats_button")
                        ) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                }

                if (compatibilityReport != null && !isBusy) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            viewModel.calculateAtsCompatibility(
                                resumeText = resumeInputText,
                                jobDescription = targetJobDescription
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recalculate_ats_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Recalculate Score with Updated Text", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // --- STEP 4: ATS COMPATIBILITY RESULTS & AUDIT ---
        val hasResults = compatibilityReport != null || rawAnalysis != null
        if (hasResults) {
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }

            item {
                // Header for analysis
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ATS Compatibility Score & Audit",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Evaluated against $targetJobTitle at $targetCompanyName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(verticalAlignment = CenterVertically) {
                        ResumeExportButton(
                            viewModel = viewModel,
                            resume = activeResume,
                            buttonText = "PDF Export",
                            isCompact = true
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Copy Full Report Button
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val report = buildString {
                                    append("=== ATS COMPATIBILITY & OPTIMIZATION REPORT ===\n")
                                    append("Role: $targetJobTitle • $targetCompanyName\n")
                                    if (compatibilityReport != null) {
                                        append("Overall ATS Compatibility Score: ${compatibilityReport?.atsCompatibilityScore}/100\n")
                                        append("• Keyword Density Score: ${compatibilityReport?.keywordDensityScore}/100\n")
                                        append("• Formatting Best Practices Score: ${compatibilityReport?.formattingBestPracticesScore}/100\n")
                                        append("• Rules Passed: ${compatibilityReport?.passedFormattingRulesCount}/${compatibilityReport?.totalFormattingRulesCount}\n\n")
                                        append("Evaluation Summary:\n${compatibilityReport?.summaryEvaluation}\n\n")
                                        append("Formatting Best Practices Checklist:\n")
                                        compatibilityReport?.formattingRuleChecks?.forEach { check ->
                                            append("[${check.status}] ${check.ruleName}: ${check.detail} (${check.scoreContribution}/${check.maxScore} pts)\n")
                                            append(" -> ${check.recommendation}\n")
                                        }
                                        append("\nKeyword Density Breakdown:\n")
                                        compatibilityReport?.keywordDensityItems?.forEach { kw ->
                                            append("• ${kw.keyword} [${kw.status}]: Resume ${kw.resumeFrequency}x (${String.format(java.util.Locale.US, "%.1f", kw.resumeDensityPercent)}%) vs Job ${kw.jobFrequency}x\n")
                                        }
                                    } else if (rawAnalysis != null) {
                                        append("Overall ATS Score: ${rawAnalysis?.overallScore}/100\n")
                                        append("Matched: ${rawAnalysis?.matchedKeywords?.joinToString(", ")}\n")
                                        append("Missing: ${rawAnalysis?.missingKeywords?.joinToString(", ")}\n")
                                    }
                                }
                                clipboard?.setPrimaryClip(ClipData.newPlainText("ATS Compatibility Report", report))
                                Toast.makeText(context, "Full ATS Compatibility Report copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("copy_ats_report_btn")
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Report")
                        }
                    }
                }
            }

            // Visual Progress Bar Component indicating percentage match against job description
            item {
                val currentScore = compatibilityReport?.atsCompatibilityScore ?: rawAnalysis?.overallScore ?: 0
                AtsScoreProgressBar(
                    score = currentScore,
                    jobTitle = targetJobTitle,
                    companyName = targetCompanyName,
                    barHeight = 16.dp,
                    showPercentage = true,
                    showRatingBadge = true,
                    showThresholdMarkers = true,
                    showRecommendation = true,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Hero Card displaying calculated ATS Compatibility Score
            compatibilityReport?.let { report ->
                item {
                    AtsCompatibilityHeroCard(
                        report = report,
                        jobTitle = targetJobTitle,
                        companyName = targetCompanyName
                    )
                }
            } ?: rawAnalysis?.let { analysisResult ->
                item {
                    AtsScoreOptimizationCard(
                        score = analysisResult.overallScore,
                        title = "Resume ATS Optimization Level",
                        subtitle = "$targetJobTitle • $targetCompanyName",
                        keywordScore = analysisResult.keywordScore,
                        impactScore = analysisResult.impactScore,
                        formatScore = analysisResult.formatScore,
                        verbScore = analysisResult.verbStrengthScore
                    )
                }
            }

            // Tab Navigation for Keyword Density vs Formatting Audit vs AI STAR Optimizer
            item {
                TabRow(
                    selectedTabIndex = currentAnalysisTab,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    divider = {}
                ) {
                    Tab(
                        selected = currentAnalysisTab == 0,
                        onClick = { currentAnalysisTab = 0 },
                        text = {
                            Row(verticalAlignment = CenterVertically) {
                                Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (compatibilityReport != null) "Keyword Density (${compatibilityReport?.keywordDensityItems?.size})" else "Keywords",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        },
                        modifier = Modifier.testTag("keyword_density_tab")
                    )

                    Tab(
                        selected = currentAnalysisTab == 1,
                        onClick = { currentAnalysisTab = 1 },
                        text = {
                            Row(verticalAlignment = CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.FactCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (compatibilityReport != null) "Formatting Audit (${compatibilityReport?.passedFormattingRulesCount}/${compatibilityReport?.totalFormattingRulesCount})" else "Formatting",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        },
                        modifier = Modifier.testTag("formatting_audit_tab")
                    )

                    Tab(
                        selected = currentAnalysisTab == 2,
                        onClick = { currentAnalysisTab = 2 },
                        text = {
                            Row(verticalAlignment = CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "STAR Bullets",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        },
                        modifier = Modifier.testTag("star_optimizer_tab")
                    )
                }
            }

            // Tab Content
            when (currentAnalysisTab) {
                0 -> {
                    // TAB 0: Keyword Density Breakdown
                    compatibilityReport?.let { report ->
                        item {
                            KeywordDensitySection(
                                items = report.keywordDensityItems,
                                onAddKeyword = { keyword ->
                                    if (!resumeInputText.contains(keyword, ignoreCase = true)) {
                                        resumeInputText = if (resumeInputText.contains("SKILLS:", ignoreCase = true)) {
                                            resumeInputText.replace("SKILLS:", "SKILLS:\n$keyword,")
                                        } else {
                                            "$resumeInputText\nSkill: $keyword"
                                        }
                                        Toast.makeText(context, "Added '$keyword' to resume!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }

                1 -> {
                    // TAB 1: Formatting Best Practices Checklist
                    compatibilityReport?.let { report ->
                        item {
                            FormattingBestPracticesSection(
                                checks = report.formattingRuleChecks
                            )
                        }
                    }
                }

                2 -> {
                    // TAB 2: AI STAR Bullet Optimizer & Gemini Feedback
                    rawAnalysis?.let { analysisResult ->
                        item {
                            AtsScoreFeedbackCard(
                                analysis = analysisResult,
                                jobTitle = targetJobTitle,
                                companyName = targetCompanyName,
                                onApplyBulletImprovement = { improvement ->
                                    resumeInputText = resumeInputText.replace(
                                        improvement.original,
                                        improvement.improvedStar
                                    )
                                    Toast.makeText(context, "Applied STAR rewrite to resume text!", Toast.LENGTH_SHORT).show()
                                },
                                onAddMissingKeyword = { keyword ->
                                    if (!resumeInputText.contains(keyword, ignoreCase = true)) {
                                        resumeInputText = if (resumeInputText.contains("SKILLS:", ignoreCase = true)) {
                                            resumeInputText.replace("SKILLS:", "SKILLS:\n$keyword,")
                                        } else {
                                            "$resumeInputText\nSkill: $keyword"
                                        }
                                        Toast.makeText(context, "Added '$keyword' to resume input!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }

            item {
                // Save Job or Return Actions
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Next Steps",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val quals = (compatibilityReport?.keywordDensityItems?.map { it.keyword } ?: emptyList()) +
                                            (rawAnalysis?.matchedKeywords ?: emptyList())
                                    viewModel.addJobTarget(
                                        title = targetJobTitle,
                                        company = targetCompanyName,
                                        description = targetJobDescription,
                                        qualifications = quals.distinct()
                                    )
                                    Toast.makeText(context, "Saved job to Room targets!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("save_job_target_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Work, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Job Target", style = MaterialTheme.typography.labelMedium)
                            }

                            if (onNavigateToEditor != null) {
                                OutlinedButton(
                                    onClick = onNavigateToEditor,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open Full Editor", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
