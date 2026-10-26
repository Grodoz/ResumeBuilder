package com.richardbroyles.resumebuilder.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.richardbroyles.resumebuilder.data.model.SkillItem
import com.richardbroyles.resumebuilder.ui.components.AtsScoreFeedbackCard
import com.richardbroyles.resumebuilder.ui.components.AtsScoreProgressCard
import com.richardbroyles.resumebuilder.ui.components.DocumentExportSuccessDialog
import com.richardbroyles.resumebuilder.ui.components.KeywordChip
import com.richardbroyles.resumebuilder.ui.components.ResumeExportActionRow
import com.richardbroyles.resumebuilder.ui.components.ResumeExportButton
import com.richardbroyles.resumebuilder.ui.components.SectionHeader
import com.richardbroyles.resumebuilder.ui.components.StarBadge
import com.richardbroyles.resumebuilder.util.UnifiedExportResult
import com.richardbroyles.resumebuilder.viewmodel.ResumeViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JobMatcherScreen(
    viewModel: ResumeViewModel,
    modifier: Modifier = Modifier
) {
    val jobs by viewModel.jobsState.collectAsState()
    val selectedJob by viewModel.selectedJob.collectAsState()
    val atsAnalysis by viewModel.atsAnalysis.collectAsState()
    val isAnalyzing by viewModel.isAnalyzingAts.collectAsState()
    val resume by viewModel.resumeState.collectAsState()

    var showJobDropdown by remember { mutableStateOf(false) }
    var showAddJobDialog by remember { mutableStateOf(false) }
    var showGapAnalyzer by remember { mutableStateOf(false) }
    var exportResultDialog by remember { mutableStateOf<UnifiedExportResult?>(null) }

    if (showGapAnalyzer) {
        BackHandler { showGapAnalyzer = false }
        SkillsGapAnalyzerScreen(
            viewModel = viewModel,
            modifier = modifier,
            initialJobDescription = selectedJob.rawDescription,
            initialJobTitle = selectedJob.title,
            initialCompanyName = selectedJob.company,
            onNavigateBack = { showGapAnalyzer = false }
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Job Selector Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("job_selector_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Target Job Description",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { showJobDropdown = true }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("select_job_dropdown_btn"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.Work,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = selectedJob.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${selectedJob.company} • ${selectedJob.location}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Open dropdown")
                        }

                        DropdownMenu(
                            expanded = showJobDropdown,
                            onDismissRequest = { showJobDropdown = false }
                        ) {
                            jobs.forEach { job ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(job.title, fontWeight = FontWeight.Bold)
                                            Text("${job.company} • ${job.location}", style = MaterialTheme.typography.bodySmall)
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectJob(job)
                                        showJobDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddJobDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("paste_custom_job_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Paste New Job", style = MaterialTheme.typography.labelMedium)
                        }

                        Button(
                            onClick = { viewModel.runAtsAnalysis() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("analyze_ats_btn"),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isAnalyzing
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Re-Analyze", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { showGapAnalyzer = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_analyze_job_skill_gaps"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Identify Missing Skills & Gaps in this Job",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Export Tailored Document:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        ResumeExportButton(
                            viewModel = viewModel,
                            resume = resume,
                            buttonText = "PDF Export",
                            isCompact = true
                        )
                    }
                }
            }
        }

        // Live Visual Progress Bar for current Job Match
        if (atsAnalysis == null && selectedJob.rawDescription.isNotBlank()) {
            item {
                val liveScoreResult = remember(selectedJob.rawDescription, resume) {
                    viewModel.calculateAtsScoreForJob(selectedJob.rawDescription)
                }

                AtsScoreProgressCard(
                    score = liveScoreResult.score,
                    jobTitle = selectedJob.title,
                    companyName = selectedJob.company,
                    keywordMatchPercent = (liveScoreResult.keywordMatchRate * 100).toInt(),
                    impactScorePercent = liveScoreResult.experienceRelevanceScore,
                    formatScorePercent = liveScoreResult.formattingScore,
                    modifier = Modifier.testTag("job_matcher_ats_progress_card")
                )
            }
        }

        // ATS Score & Actionable Feedback from Gemini API
        atsAnalysis?.let { analysis ->
            item {
                AtsScoreFeedbackCard(
                    analysis = analysis,
                    jobTitle = selectedJob.title,
                    companyName = selectedJob.company,
                    onApplyBulletImprovement = { improvement ->
                        val updatedExps = resume.experiences.map { exp ->
                            if (exp.bullets.contains(improvement.original)) {
                                val newBullets = exp.bullets.map {
                                    if (it == improvement.original) improvement.improvedStar else it
                                }
                                exp.copy(bullets = newBullets)
                            } else {
                                exp
                            }
                        }
                        viewModel.updateResume(resume.copy(experiences = updatedExps))
                    },
                    onAddMissingKeyword = { kw ->
                        val newSkill = SkillItem(name = kw, category = "Technical")
                        viewModel.updateResume(resume.copy(skills = resume.skills + newSkill))
                    }
                )
            }

            // Export Currently Optimized Resume Section (PDF & Word .docx)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_optimized_resume_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Export Tailored Resume",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Ready to submit for ${selectedJob.title.ifBlank { "this position" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "PDF & DOCX",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        ResumeExportActionRow(
                            viewModel = viewModel,
                            resume = resume,
                            modifier = Modifier.testTag("optimized_resume_export_action_row")
                        )
                    }
                }
            }

            // Keyword Alignment Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader(
                            title = "Keyword Alignment",
                            subtitle = "Target keywords detected in job requirements"
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Matched in Your Resume (${analysis.matchedKeywords.size})",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            analysis.matchedKeywords.forEach { kw ->
                                KeywordChip(text = kw, isMatched = true)
                            }
                        }

                        if (analysis.missingKeywords.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Missing / Recommended to Add (${analysis.missingKeywords.size})",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                analysis.missingKeywords.forEach { kw ->
                                    KeywordChip(text = kw, isMatched = false)
                                }
                            }
                        }
                    }
                }
            }

            // STAR Bullet Point Rewrites
            if (analysis.bulletImprovements.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Optimized STAR Bullet Points",
                        subtitle = "AI rewrites to maximize quantifiable impact"
                    )
                }

                items(analysis.bulletImprovements.size) { index ->
                    val improvement = analysis.bulletImprovements[index]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bullet_improvement_card_$index"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StarBadge()
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = improvement.metricsAdded,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Original Bullet:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                text = improvement.original,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Optimized STAR Revision:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = improvement.improvedStar,
                                    modifier = Modifier.padding(10.dp),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        lineHeight = 19.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Why this works: ${improvement.rationale}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            // One-tap apply to resume
                            Button(
                                onClick = {
                                    // Update the resume experiences by substituting the original bullet with the improved STAR bullet
                                    val updatedExps = resume.experiences.map { exp ->
                                        if (exp.bullets.contains(improvement.original)) {
                                            val newBullets = exp.bullets.map {
                                                if (it == improvement.original) improvement.improvedStar else it
                                            }
                                            exp.copy(bullets = newBullets)
                                        } else {
                                            exp
                                        }
                                    }
                                    viewModel.updateResume(resume.copy(experiences = updatedExps))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("apply_star_bullet_$index"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Apply STAR Revision to Resume", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // High Impact Tips
            if (analysis.highImpactTips.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SectionHeader(
                                title = "Executive Advice for this Role",
                                subtitle = "Key considerations from senior recruiters"
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            analysis.highImpactTips.forEach { tip ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = tip,
                                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }

    // --- Add Custom Job Dialog ---
    if (showAddJobDialog) {
        var jobTitle by remember { mutableStateOf("") }
        var companyName by remember { mutableStateOf("") }
        var descriptionText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddJobDialog = false },
            title = { Text("Add / Paste Job Description") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = jobTitle,
                        onValueChange = { jobTitle = it },
                        label = { Text("Job Title (e.g. Senior Android Engineer)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text("Company Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = descriptionText,
                        onValueChange = { descriptionText = it },
                        label = { Text("Paste Job Description & Requirements") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 8
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (jobTitle.isNotEmpty() && descriptionText.isNotEmpty()) {
                            viewModel.addJobTarget(
                                title = jobTitle,
                                company = companyName.ifEmpty { "Target Company" },
                                description = descriptionText,
                                qualifications = emptyList()
                            )
                            showAddJobDialog = false
                        }
                    }
                ) {
                    Text("Analyze Job")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddJobDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Export Success/Error Dialog for PDF & Word .docx
    exportResultDialog?.let { result ->
        DocumentExportSuccessDialog(
            result = result,
            onDismiss = { exportResultDialog = null }
        )
    }
}
