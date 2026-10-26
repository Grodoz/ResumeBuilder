package com.richardbroyles.resumebuilder.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.richardbroyles.resumebuilder.data.model.CoverLetter
import com.richardbroyles.resumebuilder.ui.components.SectionHeader
import com.richardbroyles.resumebuilder.util.PdfGenerator
import com.richardbroyles.resumebuilder.util.SampleData
import com.richardbroyles.resumebuilder.viewmodel.ResumeViewModel

@Composable
fun TargetCoverLetterGeneratorScreen(
    viewModel: ResumeViewModel,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val storedResume by viewModel.resumeState.collectAsState()
    val allResumes by viewModel.allResumesState.collectAsState()
    val isGenerating by viewModel.isGeneratingCoverLetter.collectAsState()

    var selectedResume by remember(storedResume) { mutableStateOf(storedResume) }
    var showResumeDropdown by remember { mutableStateOf(false) }

    var companyName by remember { mutableStateOf("") }
    var jobTitle by remember { mutableStateOf("") }
    var jobDescription by remember { mutableStateOf("") }
    var selectedTone by remember { mutableStateOf("Professional & Results-Oriented") }

    var generatedLetterContent by remember { mutableStateOf("") }
    var generatedLetterModel by remember { mutableStateOf<CoverLetter?>(null) }
    var isSavedNotification by remember { mutableStateOf(false) }
    var exportResultDialog by remember { mutableStateOf<PdfGenerator.ExportResult?>(null) }

    val tones = listOf(
        "Professional & Results-Oriented",
        "Confident Executive",
        "Enthusiastic & High-Energy",
        "Concise Technical"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Screen Top Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("target_cover_letter_banner"),
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
                                    text = "Target Cover Letter Generator",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Powered by Gemini AI & your selected saved resume",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // Stored Resume Context Card with Dropdown Selector
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("stored_resume_info_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader(
                            title = "Select Saved Resume",
                            subtitle = "Pick which resume Gemini uses to tailor your cover letter"
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dropdown to choose among all saved resumes in Room
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { showResumeDropdown = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("select_saved_resume_for_cover_letter_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Selected: ${selectedResume.title.ifEmpty { selectedResume.targetRole.ifEmpty { selectedResume.fullName } }}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Dropdown"
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
                                                text = r.title.ifEmpty { r.targetRole.ifEmpty { "Resume Profile" } },
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${r.fullName} • ${r.experiences.size} roles, ${r.skills.size} skills",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedResume = r
                                        viewModel.selectActiveResume(r)
                                        showResumeDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "${selectedResume.fullName} • ${selectedResume.targetRole}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${selectedResume.experiences.size} roles listed • ${selectedResume.skills.size} skills • ${selectedResume.educations.firstOrNull()?.institution ?: "Higher Education"}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Job & Company Inputs Form Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("job_inputs_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader(
                            title = "Target Opportunity Details",
                            subtitle = "Enter company and job description for personalization"
                        )
                        OutlinedButton(
                            onClick = {
                                val sample = SampleData.sampleJobs.firstOrNull()
                                if (sample != null) {
                                    companyName = sample.company
                                    jobTitle = sample.title
                                    jobDescription = sample.rawDescription
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Sample Job", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text("Target Company Name *") },
                        placeholder = { Text("e.g. Anthropic, Google, Stripe, Vanguard") },
                        leadingIcon = {
                            Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_company_name"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = jobTitle,
                        onValueChange = { jobTitle = it },
                        label = { Text("Target Job Title *") },
                        placeholder = { Text("e.g. Staff Android Engineer, Lead Architect") },
                        leadingIcon = {
                            Icon(Icons.Default.Work, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_job_title"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = jobDescription,
                        onValueChange = { jobDescription = it },
                        label = { Text("Target Job Description & Qualifications *") },
                        placeholder = {
                            Text(
                                "Paste the responsibilities, required qualifications, and company mission here. " +
                                        "Gemini will cross-reference with your resume accomplishments."
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_job_description"),
                        shape = RoundedCornerShape(10.dp),
                        minLines = 5,
                        maxLines = 10
                    )

                    // Tone Selection
                    Text(
                        text = "Preferred Letter Tone:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tones.forEach { tone ->
                            FilterChip(
                                selected = selectedTone == tone,
                                onClick = { selectedTone = tone },
                                label = { Text(tone, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.testTag("tone_chip_$tone")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Generation Action Button
                    Button(
                        onClick = {
                            if (companyName.isNotBlank() && jobDescription.isNotBlank()) {
                                isSavedNotification = false
                                viewModel.generatePersonalizedCoverLetter(
                                    jobDescription = jobDescription,
                                    resume = selectedResume,
                                    jobTitle = jobTitle.ifBlank { "Target Role" },
                                    companyName = companyName,
                                    tone = selectedTone
                                ) { content ->
                                    generatedLetterContent = content
                                    generatedLetterModel = viewModel.activeCoverLetter.value
                                }
                            } else {
                                Toast.makeText(context, "Please enter company name and job description", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isGenerating && companyName.isNotBlank() && jobDescription.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_generate_target_cover_letter"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Gemini AI is crafting your tailored letter...", style = MaterialTheme.typography.labelMedium)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Generate Cover Letter with Gemini",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        // Generated Result Card
        if (generatedLetterContent.isNotBlank()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("generated_cover_letter_result_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionHeader(
                                title = "Personalized Letter Output",
                                subtitle = "Generated for ${if (companyName.isNotBlank()) companyName else "Target Organization"}"
                            )

                            Row {
                                // Copy Button
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Cover Letter", generatedLetterContent)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Cover letter copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.testTag("btn_copy_letter")
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy to clipboard", tint = MaterialTheme.colorScheme.primary)
                                }

                                // Save Button
                                IconButton(
                                    onClick = {
                                        viewModel.updateCoverLetterContent(generatedLetterContent)
                                        isSavedNotification = true
                                        Toast.makeText(context, "Cover letter saved to database!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.testTag("btn_save_letter")
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = "Save letter", tint = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }

                        if (isSavedNotification) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Saved to Room database (cover_letters table)", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold))
                                }
                            }
                        }

                        // Achievements Cited chips
                        generatedLetterModel?.let { model ->
                            if (model.keyAchievementsCited.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Accomplishments Cited from Resume:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    model.keyAchievementsCited.forEach { citation ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        ) {
                                            Text(
                                                text = "✓ $citation",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Full Editable OutlinedTextField
                        OutlinedTextField(
                            value = generatedLetterContent,
                            onValueChange = {
                                generatedLetterContent = it
                                viewModel.updateCoverLetterEditorText(it)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("generated_cover_letter_editor"),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                            minLines = 14,
                            maxLines = 24,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        // PDF Actions: Direct Save to Documents & Android PrintManager
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val result = PdfGenerator.exportCoverLetterToDocuments(
                                        context = context,
                                        letterText = generatedLetterContent,
                                        jobTitle = jobTitle.ifBlank { "Target Role" },
                                        companyName = companyName.ifBlank { "Organization" },
                                        candidateName = selectedResume.fullName,
                                        candidateEmail = selectedResume.email,
                                        candidatePhone = selectedResume.phone,
                                        candidateLocation = selectedResume.location
                                    )
                                    exportResultDialog = result
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_save_cover_letter_pdf"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Save PDF to Documents",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Save to Documents",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    PdfGenerator.printCoverLetterWithPrintManager(
                                        context = context,
                                        letterText = generatedLetterContent,
                                        jobTitle = jobTitle.ifBlank { "Target Role" },
                                        companyName = companyName.ifBlank { "Organization" },
                                        candidateName = selectedResume.fullName,
                                        candidateEmail = selectedResume.email,
                                        candidatePhone = selectedResume.phone,
                                        candidateLocation = selectedResume.location
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_print_cover_letter"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Print,
                                    contentDescription = "Print or System PDF",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Print / Spooler",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }

    // PDF Export Success Dialog with View & Share options
    exportResultDialog?.let { result ->
        AlertDialog(
            onDismissRequest = { exportResultDialog = null },
            icon = {
                Icon(
                    imageVector = if (result.success) Icons.Default.FileDownload else Icons.Default.Check,
                    contentDescription = null,
                    tint = if (result.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = if (result.success) "Cover Letter PDF Ready" else "Export Failed",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    if (result.success) {
                        Text(
                            text = "Your tailored cover letter has been compiled into a professional PDF and saved directly to device Documents folder:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = result.filePathOrUri,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    } else {
                        Text(
                            text = result.errorMessage ?: "Unknown error while creating PDF file.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                if (result.success && result.uri != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    val shareIntent = PdfGenerator.sharePdfIntent(result.uri, result.fileName)
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Cover Letter PDF"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No app available to share PDF", Toast.LENGTH_SHORT).show()
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
                                    val viewIntent = PdfGenerator.viewPdfIntent(context, result.uri)
                                    context.startActivity(viewIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No PDF viewer installed", Toast.LENGTH_SHORT).show()
                                }
                                exportResultDialog = null
                            }
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open PDF")
                        }
                    }
                } else {
                    Button(onClick = { exportResultDialog = null }) {
                        Text("OK")
                    }
                }
            },
            dismissButton = {
                if (result.success && result.uri != null) {
                    OutlinedButton(onClick = { exportResultDialog = null }) {
                        Text("Dismiss")
                    }
                }
            }
        )
    }
}
