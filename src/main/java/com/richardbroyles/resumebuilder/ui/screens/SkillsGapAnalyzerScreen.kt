package com.richardbroyles.resumebuilder.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.richardbroyles.resumebuilder.data.model.SkillItem
import com.richardbroyles.resumebuilder.data.model.SkillMatchStatus
import com.richardbroyles.resumebuilder.data.model.SkillsMatrixReport
import com.richardbroyles.resumebuilder.ui.components.AtsScoreDial
import com.richardbroyles.resumebuilder.ui.components.SectionHeader
import com.richardbroyles.resumebuilder.ui.theme.AtsGapColor
import com.richardbroyles.resumebuilder.ui.theme.AtsMatchedColor
import com.richardbroyles.resumebuilder.ui.theme.AtsTransferableColor
import com.richardbroyles.resumebuilder.viewmodel.ResumeViewModel

@Composable
fun SkillsGapAnalyzerScreen(
    viewModel: ResumeViewModel,
    modifier: Modifier = Modifier,
    initialJobDescription: String = "",
    initialJobTitle: String = "",
    initialCompanyName: String = "",
    onNavigateBack: (() -> Unit)? = null
) {
    if (onNavigateBack != null) {
        androidx.activity.compose.BackHandler(onBack = onNavigateBack)
    }

    val context = LocalContext.current
    val storedResume by viewModel.resumeState.collectAsState()
    val isAnalyzing by viewModel.isComparingSkills.collectAsState()

    var pastedJobDescription by remember(initialJobDescription) { mutableStateOf(initialJobDescription) }
    var targetRoleTitle by remember(initialJobTitle) { mutableStateOf(initialJobTitle) }
    var targetCompanyName by remember(initialCompanyName) { mutableStateOf(initialCompanyName) }

    var reportState by remember { mutableStateOf<SkillsMatrixReport?>(null) }
    var addedSkillIds by remember { mutableStateOf(setOf<String>()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Top Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("skills_gap_header_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            if (onNavigateBack != null) {
                                IconButton(onClick = onNavigateBack) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Column {
                                Text(
                                    text = "Skills & Qualifications Gap Analyzer",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Identify missing skills from any pasted job post",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // Candidate Resume Snapshot
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("stored_resume_snapshot_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Stored Resume: ${storedResume.fullName}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${storedResume.skills.size} recorded skills • ${storedResume.experiences.size} roles in work history",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                }
            }
        }

        // Pasted Job Description Input Form
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pasted_job_input_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        title = "Paste Target Job Posting",
                        subtitle = "Paste responsibilities and required qualifications to run gap analysis"
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = targetRoleTitle,
                            onValueChange = { targetRoleTitle = it },
                            label = { Text("Target Role (optional)") },
                            placeholder = { Text("e.g. Lead Mobile Engineer") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_target_role"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = targetCompanyName,
                            onValueChange = { targetCompanyName = it },
                            label = { Text("Company (optional)") },
                            placeholder = { Text("e.g. OpenAI, Stripe") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_target_company"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = pastedJobDescription,
                        onValueChange = { pastedJobDescription = it },
                        label = { Text("Pasted Job Description & Requirements *") },
                        placeholder = {
                            Text(
                                "Paste the full job posting, bulleted requirements, or qualifications here...\n" +
                                        "e.g. 'Must have 5+ years Kotlin, deep GraphQL experience, Docker deployments...'"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_pasted_job_description"),
                        shape = RoundedCornerShape(10.dp),
                        minLines = 5,
                        maxLines = 10
                    )

                    Button(
                        onClick = {
                            if (pastedJobDescription.isNotBlank()) {
                                viewModel.analyzePastedJobSkillsGap(
                                    jobTitle = targetRoleTitle,
                                    companyName = targetCompanyName,
                                    jobDescription = pastedJobDescription
                                ) { report ->
                                    reportState = report
                                }
                            } else {
                                Toast.makeText(context, "Please paste a job description first", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isAnalyzing && pastedJobDescription.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_analyze_skill_gaps"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Analyzing skill qualifications...", style = MaterialTheme.typography.labelMedium)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Identify Missing Skills & Gaps",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        // Analysis Results Section
        reportState?.let { report ->
            // Match Overview Dashboard
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gap_analysis_overview_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader(
                            title = "Qualification Match & Gap Overview",
                            subtitle = "Comparison against candidate profile"
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AtsScoreDial(
                                score = report.matchPercentage,
                                modifier = Modifier.padding(end = 16.dp)
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Missing gaps metric (Highlighted prominently)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = AtsGapColor.copy(alpha = 0.12f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Missing Qualification Gaps:",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = AtsGapColor
                                        )
                                        Text(
                                            text = "${report.gapCount}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                            color = AtsGapColor
                                        )
                                    }
                                }

                                // Transferable
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Transferable Experience:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = "${report.transferableCount}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = AtsTransferableColor
                                        )
                                    )
                                }

                                // Matched
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Exact Qualifications Matched:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = "${report.matchedCount}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = AtsMatchedColor
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 1: HIGHLIGHTED MISSING GAPS (PRIMARY FOCUS)
            val gapItems = report.items.filter { it.status == SkillMatchStatus.GAP }
            if (gapItems.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = AtsGapColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Missing Skill Gaps to Address (${gapItems.size})",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AtsGapColor
                                )
                            )
                            Text(
                                text = "These qualifications were not found in your resume and pose screening risks",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                        }
                    }
                }

                itemsIndexed(gapItems) { idx, gap ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gap_item_$idx"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, AtsGapColor.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = AtsGapColor.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Error,
                                            contentDescription = null,
                                            tint = AtsGapColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "MISSING QUALIFICATION",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = AtsGapColor,
                                                fontSize = 9.5.sp
                                            )
                                        )
                                    }
                                }

                                Text(
                                    text = "Gap #${idx + 1}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = gap.qualification,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 20.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Actionable Bridging Advice Box
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = "Recommendation",
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .padding(top = 1.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "How to Address This Gap:",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = gap.bridgingAdvice.ifEmpty { "Highlight related projects or enroll in a targeted certification to bridge this qualification." },
                                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }

                            // Quick Add Skill Button if candidate already possesses it
                            val isSkillAdded = addedSkillIds.contains(gap.qualification)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val cleanedSkillName = gap.qualification.take(35)
                                    val newSkill =
                                        SkillItem(name = cleanedSkillName, category = "Technical")
                                    viewModel.updateResume(storedResume.copy(skills = storedResume.skills + newSkill))
                                    addedSkillIds = addedSkillIds + gap.qualification
                                    Toast.makeText(context, "Added '$cleanedSkillName' to Resume Skills!", Toast.LENGTH_SHORT).show()
                                },
                                enabled = !isSkillAdded,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("add_gap_skill_btn_$idx"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSkillAdded) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = if (isSkillAdded) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSkillAdded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isSkillAdded) "Added to Resume Skills" else "I have this skill — Add to Resume",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSkillAdded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }

            // SECTION 2: TRANSFERABLE QUALIFICATIONS
            val transferableItems = report.items.filter { it.status == SkillMatchStatus.TRANSFERABLE }
            if (transferableItems.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Transferable Qualifications (${transferableItems.size})",
                        subtitle = "Skills where you have adjacent experience to reframe"
                    )
                }

                itemsIndexed(transferableItems) { idx, item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transferable_item_$idx"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, AtsTransferableColor.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = AtsTransferableColor.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = AtsTransferableColor, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "TRANSFERABLE",
                                            style = MaterialTheme.typography.labelSmall.copy(color = AtsTransferableColor, fontWeight = FontWeight.Bold, fontSize = 9.5.sp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = item.qualification, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Candidate Evidence: ${item.candidateEvidence ?: "Related project experience"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            if (item.bridgingAdvice.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Reframe Tip: ${item.bridgingAdvice}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = AtsTransferableColor, fontSize = 11.5.sp)
                                )
                            }
                        }
                    }
                }
            }

            // SECTION 3: EXACT MATCHES
            val matchedItems = report.items.filter { it.status == SkillMatchStatus.MATCHED }
            if (matchedItems.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Exact Matches (${matchedItems.size})",
                        subtitle = "Qualifications validated in your active resume"
                    )
                }

                itemsIndexed(matchedItems) { idx, item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("matched_item_$idx"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, AtsMatchedColor.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AtsMatchedColor, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.qualification, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                Text(
                                    text = "Direct Match • Verified in resume experience & skills",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = AtsMatchedColor
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}
