package com.richardbroyles.resumebuilder.ui.screens

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.richardbroyles.resumebuilder.data.model.ExperienceItem
import com.richardbroyles.resumebuilder.data.model.SkillItem
import com.richardbroyles.resumebuilder.ui.components.ResumeVersionHistoryDialog
import com.richardbroyles.resumebuilder.ui.components.SectionHeader
import com.richardbroyles.resumebuilder.util.PdfGenerator
import com.richardbroyles.resumebuilder.util.ResumePdfService
import com.richardbroyles.resumebuilder.viewmodel.ResumeViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResumeEditorScreen(
    viewModel: ResumeViewModel,
    modifier: Modifier = Modifier,
    onNavigateToAiCoach: (() -> Unit)? = null,
    onNavigateToSettings: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val resume by viewModel.resumeState.collectAsState()
    val versions by viewModel.resumeVersions.collectAsState()

    var showResumeForm by remember { mutableStateOf(false) }
    var showVersionHistoryDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAddExperienceDialog by remember { mutableStateOf(false) }
    var showAddSkillDialog by remember { mutableStateOf(false) }
    var exportSuccessDialogResult by remember { mutableStateOf<PdfGenerator.ExportResult?>(null) }

    if (showResumeForm) {
        BackHandler { showResumeForm = false }
        ResumeFormScreen(
            viewModel = viewModel,
            modifier = modifier,
            onNavigateBack = { showResumeForm = false }
        )
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Top Header Card with Export PDF Action
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("resume_header_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = resume.fullName.ifEmpty { "Your Name" },
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = resume.targetRole.ifEmpty { "Target Role" },
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                            IconButton(
                                onClick = { showEditProfileDialog = true },
                                modifier = Modifier.testTag("edit_profile_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Profile",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        val contactParts = listOfNotNull(
                            resume.email.takeIf { it.isNotEmpty() },
                            resume.phone.takeIf { it.isNotEmpty() },
                            resume.location.takeIf { it.isNotEmpty() }
                        )
                        if (contactParts.isNotEmpty()) {
                            Text(
                                text = contactParts.joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        // PDF Actions: Direct Save to Documents & Android PrintManager
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    exportSuccessDialogResult = PdfGenerator.exportResumeToDocuments(context, resume)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_pdf_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Save PDF to Documents",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Save to Documents",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    PdfGenerator.printResumeWithPrintManager(context, resume)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("print_manager_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Print,
                                    contentDescription = "Print or System PDF",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Print / System PDF",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showResumeForm = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_resume_form_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit in Full Resume Form",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Edit in Full Resume Database Form",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showVersionHistoryDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("version_history_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Resume Version History",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Version History (${versions.size}) • Revert",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }

            // Professional Summary
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader(
                            title = "Professional Summary",
                            subtitle = "Tailor your executive overview to target roles"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = resume.summary.ifEmpty { "No summary provided yet. Tap edit above to add your professional summary." },
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // Work Experience Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(
                        title = "Work Experience",
                        subtitle = "${resume.experiences.size} roles listed"
                    )
                }
            }

            items(resume.experiences.size) { index ->
                val exp = resume.experiences[index]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("experience_card_$index"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = exp.jobTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${exp.company} • ${exp.location}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Text(
                                    text = "${exp.startDate} - ${if (exp.isCurrent) "Present" else exp.endDate}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                )
                            }
                            IconButton(
                                onClick = {
                                    val updatedList = resume.experiences.toMutableList().also { it.removeAt(index) }
                                    viewModel.updateResume(resume.copy(experiences = updatedList))
                                },
                                modifier = Modifier.testTag("delete_exp_$index")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete role",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Key Achievements & STAR Bullets:",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        exp.bullets.forEachIndexed { bulletIdx, bullet ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 7.dp, end = 8.dp)
                                        .size(6.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                                Text(
                                    text = bullet,
                                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = { showAddExperienceDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_experience_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Experience", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Work Experience")
                }
            }

            // Skills Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                            SectionHeader(
                                title = "Skills & Competencies",
                                subtitle = "${resume.skills.size} skills documented"
                            )
                            IconButton(
                                onClick = { showAddSkillDialog = true },
                                modifier = Modifier.testTag("add_skill_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Skill", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            resume.skills.forEach { skill ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = skill.name,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove skill",
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable {
                                                    val updatedSkills = resume.skills.filter { it.id != skill.id }
                                                    viewModel.updateResume(resume.copy(skills = updatedSkills))
                                                },
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Education & Credentials
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader(
                            title = "Education & Certifications",
                            subtitle = "Academic background and accredited credentials"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        resume.educations.forEach { edu ->
                            Text(
                                text = "${edu.degree} in ${edu.fieldOfStudy}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${edu.institution} • ${edu.graduationYear} (${edu.gpaOrHonors})",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        if (resume.certifications.isNotEmpty()) {
                            Text(
                                text = "Certifications: " + resume.certifications.joinToString(", "),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // --- Edit Profile Dialog ---
    if (showEditProfileDialog) {
        var name by remember { mutableStateOf(resume.fullName) }
        var role by remember { mutableStateOf(resume.targetRole) }
        var email by remember { mutableStateOf(resume.email) }
        var phone by remember { mutableStateOf(resume.phone) }
        var location by remember { mutableStateOf(resume.location) }
        var summary by remember { mutableStateOf(resume.summary) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Profile & Target Role") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("Target Role") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = summary,
                        onValueChange = { summary = it },
                        label = { Text("Professional Summary") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateResume(
                            resume.copy(
                                fullName = name,
                                targetRole = role,
                                email = email,
                                phone = phone,
                                location = location,
                                summary = summary
                            )
                        )
                        showEditProfileDialog = false
                    }
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) { Text("Cancel") }
            }
        )
    }

    // --- Add Experience Dialog ---
    if (showAddExperienceDialog) {
        var jobTitle by remember { mutableStateOf("") }
        var company by remember { mutableStateOf("") }
        var location by remember { mutableStateOf("") }
        var startDate by remember { mutableStateOf("") }
        var endDate by remember { mutableStateOf("Present") }
        var bulletText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddExperienceDialog = false },
            title = { Text("Add Work Experience") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = jobTitle, onValueChange = { jobTitle = it }, label = { Text("Job Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = company, onValueChange = { company = it }, label = { Text("Company") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = startDate, onValueChange = { startDate = it }, label = { Text("Start Date") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = endDate, onValueChange = { endDate = it }, label = { Text("End Date") }, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(
                        value = bulletText,
                        onValueChange = { bulletText = it },
                        label = { Text("Accomplishments (1 per line)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (jobTitle.isNotEmpty() && company.isNotEmpty()) {
                            val bullets = bulletText.lines().map { it.trim() }.filter { it.isNotEmpty() }
                            val newExp = ExperienceItem(
                                jobTitle = jobTitle,
                                company = company,
                                location = location,
                                startDate = startDate.ifEmpty { "2023" },
                                endDate = endDate,
                                isCurrent = endDate.contains("present", true),
                                bullets = bullets.ifEmpty { listOf("Delivered key features and optimized performance.") }
                            )
                            viewModel.updateResume(resume.copy(experiences = listOf(newExp) + resume.experiences))
                            showAddExperienceDialog = false
                        }
                    }
                ) {
                    Text("Add Role")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExperienceDialog = false }) { Text("Cancel") }
            }
        )
    }

    // --- Add Skill Dialog ---
    if (showAddSkillDialog) {
        var skillName by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Technical") }

        AlertDialog(
            onDismissRequest = { showAddSkillDialog = false },
            title = { Text("Add Skill") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = skillName,
                        onValueChange = { skillName = it },
                        label = { Text("Skill Name (e.g. Docker, Python)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (skillName.isNotBlank()) {
                            val newSkill = SkillItem(name = skillName.trim(), category = category)
                            viewModel.updateResume(resume.copy(skills = resume.skills + newSkill))
                            showAddSkillDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSkillDialog = false }) { Text("Cancel") }
            }
        )
    }

    // --- Export Result Dialog ---
    exportSuccessDialogResult?.let { result ->
        AlertDialog(
            onDismissRequest = { exportSuccessDialogResult = null },
            icon = {
                Icon(
                    imageVector = if (result.success) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (result.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(if (result.success) "Resume Exported to Documents!" else "Export Failed")
            },
            text = {
                Column {
                    if (result.success) {
                        Text(
                            text = "A formatted PDF copy of your tailored resume has been successfully saved to your device's Documents folder:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = result.filePathOrUri,
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    } else {
                        Text(text = result.errorMessage ?: "Unknown error while creating PDF.")
                    }
                }
            },
            confirmButton = {
                if (result.success && result.uri != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    val shareIntent = Intent.createChooser(
                                        ResumePdfService.sharePdfIntent(result.uri, result.fileName),
                                        "Share Resume PDF"
                                    )
                                    context.startActivity(shareIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cannot share: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share")
                        }

                        Button(
                            onClick = {
                                try {
                                    val intent = ResumePdfService.viewPdfIntent(result.uri)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Saved: ${result.filePathOrUri}", Toast.LENGTH_SHORT).show()
                                }
                                exportSuccessDialogResult = null
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open PDF")
                        }
                    }
                } else {
                    Button(onClick = { exportSuccessDialogResult = null }) {
                        Text("OK")
                    }
                }
            },
            dismissButton = {
                if (result.success && result.uri != null) {
                    TextButton(onClick = { exportSuccessDialogResult = null }) {
                        Text("Close")
                    }
                }
            }
        )
    }

    if (showVersionHistoryDialog) {
        ResumeVersionHistoryDialog(
            versions = versions,
            currentResume = resume,
            onDismiss = { showVersionHistoryDialog = false },
            onCreateSnapshot = { name, notes ->
                viewModel.createResumeSnapshot(name, notes)
            },
            onRevertToVersion = { version ->
                viewModel.revertToVersion(version)
            },
            onDeleteVersion = { versionId ->
                viewModel.deleteResumeVersion(versionId)
            }
        )
    }
}
