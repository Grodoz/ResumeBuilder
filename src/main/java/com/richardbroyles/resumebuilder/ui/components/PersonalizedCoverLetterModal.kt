package com.richardbroyles.resumebuilder.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.richardbroyles.resumebuilder.data.model.CoverLetter
import com.richardbroyles.resumebuilder.data.model.ResumeProfile
import com.richardbroyles.resumebuilder.ui.theme.AtsScoreHigh
import com.richardbroyles.resumebuilder.util.PdfGenerator
import com.richardbroyles.resumebuilder.viewmodel.ResumeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalizedCoverLetterModal(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    viewModel: ResumeViewModel,
    initialJobDescription: String? = null,
    initialCompanyName: String? = null,
    initialJobTitle: String? = null,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    val currentResume by viewModel.resumeState.collectAsState()
    val isGenerating by viewModel.isGeneratingCoverLetter.collectAsState()
    val selectedJob by viewModel.selectedJob.collectAsState()
    val activeCoverLetter by viewModel.activeCoverLetter.collectAsState()

    var jobDescription by remember {
        mutableStateOf(
            initialJobDescription?.ifBlank { null }
                ?: selectedJob.rawDescription.ifBlank {
                    "We are seeking an experienced Senior Mobile Engineer to lead Android architecture, mentor engineers, and build high-performance, offline-first applications with Jetpack Compose, Kotlin Coroutines, and Room SQLite."
                }
        )
    }

    var companyName by remember {
        mutableStateOf(
            initialCompanyName?.ifBlank { null }
                ?: selectedJob.company.ifBlank { "InnovateTech Global" }
        )
    }

    var jobTitle by remember {
        mutableStateOf(
            initialJobTitle?.ifBlank { null }
                ?: selectedJob.title.ifBlank { currentResume.targetRole.ifBlank { "Senior Android Engineer" } }
        )
    }

    var letterContent by remember {
        mutableStateOf(activeCoverLetter?.content ?: "")
    }

    // Keep letter content synchronized when Gemini finishes generation
    LaunchedEffect(activeCoverLetter?.content) {
        if (!activeCoverLetter?.content.isNullOrBlank()) {
            letterContent = activeCoverLetter!!.content
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
                .testTag("personalized_cover_letter_modal"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            PersonalizedCoverLetterContent(
                currentResume = currentResume,
                jobDescription = jobDescription,
                onJobDescriptionChange = { jobDescription = it },
                companyName = companyName,
                onCompanyChange = { companyName = it },
                jobTitle = jobTitle,
                onJobTitleChange = { jobTitle = it },
                isGenerating = isGenerating,
                letterContent = letterContent,
                onLetterContentChange = { letterContent = it },
                onGenerateCoverLetter = { tone ->
                    viewModel.generatePersonalizedCoverLetter(
                        jobDescription = jobDescription,
                        resume = currentResume,
                        jobTitle = jobTitle,
                        companyName = companyName,
                        tone = tone,
                        onComplete = { result ->
                            letterContent = result
                        }
                    )
                },
                onSaveCoverLetter = { letterToSave ->
                    viewModel.saveCoverLetter(letterToSave)
                },
                onDismiss = onDismissRequest
            )
        }
    }
}

/**
 * Reusable full content for generating and copying a personalized cover letter.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonalizedCoverLetterContent(
    currentResume: ResumeProfile,
    jobDescription: String,
    onJobDescriptionChange: (String) -> Unit,
    companyName: String,
    onCompanyChange: (String) -> Unit,
    jobTitle: String,
    onJobTitleChange: (String) -> Unit,
    isGenerating: Boolean,
    letterContent: String,
    onLetterContentChange: (String) -> Unit,
    onGenerateCoverLetter: (tone: String) -> Unit,
    onSaveCoverLetter: (CoverLetter) -> Unit,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTone by remember { mutableStateOf("Professional & Results-Oriented") }
    var copiedToClipboard by remember { mutableStateOf(false) }
    var exportResultDialog by remember { mutableStateOf<PdfGenerator.ExportResult?>(null) }
    val scrollState = rememberScrollState()

    val tones = listOf(
        "Professional & Results-Oriented",
        "Technical & Architecture-Driven",
        "Passionate & Direct",
        "Executive Leadership"
    )

    fun copyLetterToClipboard() {
        if (letterContent.isNotBlank()) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Cover Letter", letterContent)
            clipboard.setPrimaryClip(clip)
            copiedToClipboard = true
            Toast.makeText(context, "Cover letter copied to clipboard!", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareLetter() {
        if (letterContent.isNotBlank()) {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, letterContent)
                putExtra(Intent.EXTRA_SUBJECT, "Cover Letter - $jobTitle at $companyName")
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share Cover Letter via"))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header with Title and Close Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "AI Cover Letter Generator",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Powered by Gemini • Personalized to your resume",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
            }

            if (onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("cover_letter_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close dialog"
                    )
                }
            }
        }

        // 2. Active Candidate Resume Summary Card
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("cover_letter_resume_summary_chip"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Active Resume: ${currentResume.fullName.ifBlank { "Professional Candidate" }}",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${currentResume.targetRole.ifBlank { "Software Engineer" }} • ${currentResume.skills.take(4).joinToString(", ") { it.name }}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AtsScoreHigh.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Room Cached",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AtsScoreHigh
                        )
                    )
                }
            }
        }

        // 3. Target Role & Company Fields
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = companyName,
                onValueChange = onCompanyChange,
                label = { Text("Target Company") },
                leadingIcon = {
                    Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("cover_letter_company_input"),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = jobTitle,
                onValueChange = onJobTitleChange,
                label = { Text("Target Role") },
                leadingIcon = {
                    Icon(Icons.Default.Work, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("cover_letter_job_title_input"),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // 4. Job Description Input Area
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Target Job Description",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (jobDescription.isNotBlank()) {
                    TextButton(
                        onClick = { onJobDescriptionChange("") },
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Clear", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            OutlinedTextField(
                value = jobDescription,
                onValueChange = onJobDescriptionChange,
                placeholder = {
                    Text(
                        "Paste the complete job description or key requirements here...",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .testTag("cover_letter_job_description_input"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                )
            )
        }

        // 5. Tone Selector Chips
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Tone of Voice",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                tones.forEach { tone ->
                    FilterChip(
                        selected = selectedTone == tone,
                        onClick = { selectedTone = tone },
                        label = { Text(tone, style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = if (selectedTone == tone) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        // 6. Action Button: Generate Cover Letter
        Button(
            onClick = { onGenerateCoverLetter(selectedTone) },
            enabled = !isGenerating && jobDescription.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("cover_letter_generate_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            if (isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(22.dp)
                        .testTag("cover_letter_loading_indicator"),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Gemini is Drafting Cover Letter...",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (letterContent.isNotBlank()) "Regenerate Cover Letter with Gemini" else "Generate Personalized Cover Letter",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        if (isGenerating) {
            Spacer(modifier = Modifier.height(2.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // 7. Output Section: Copyable Text Area & Action Bar
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Personalized Cover Letter",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (letterContent.isNotBlank()) {
                    val wordCount = letterContent.split("\\s+".toRegex()).count { it.isNotBlank() }
                    Text(
                        text = "$wordCount words • ~${(wordCount / 200).coerceAtLeast(1)} min read",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Quick Action Bar: Copy & Share
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = { copyLetterToClipboard() },
                    enabled = letterContent.isNotBlank(),
                    modifier = Modifier.testTag("cover_letter_copy_clipboard_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (copiedToClipboard) AtsScoreHigh else MaterialTheme.colorScheme.secondary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = if (copiedToClipboard) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy to clipboard",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (copiedToClipboard) "Copied!" else "Copy",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                IconButton(
                    onClick = { shareLetter() },
                    enabled = letterContent.isNotBlank(),
                    modifier = Modifier.testTag("cover_letter_share_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share cover letter",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = {
                        if (letterContent.isNotBlank()) {
                            val letter = CoverLetter(
                                jobTitle = jobTitle,
                                company = companyName,
                                tone = selectedTone,
                                content = letterContent
                            )
                            onSaveCoverLetter(letter)
                            Toast.makeText(context, "Cover letter saved to database!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = letterContent.isNotBlank(),
                    modifier = Modifier.testTag("cover_letter_save_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save to database",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = {
                        if (letterContent.isNotBlank()) {
                            val result = PdfGenerator.exportCoverLetterToDocuments(
                                context = context,
                                letterText = letterContent,
                                jobTitle = jobTitle,
                                companyName = companyName,
                                candidateName = currentResume.fullName,
                                candidateEmail = currentResume.email,
                                candidatePhone = currentResume.phone,
                                candidateLocation = currentResume.location
                            )
                            exportResultDialog = result
                        }
                    },
                    enabled = letterContent.isNotBlank(),
                    modifier = Modifier.testTag("cover_letter_export_pdf_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Export Cover Letter as PDF",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = {
                        if (letterContent.isNotBlank()) {
                            PdfGenerator.printCoverLetterWithPrintManager(
                                context = context,
                                letterText = letterContent,
                                jobTitle = jobTitle,
                                companyName = companyName,
                                candidateName = currentResume.fullName,
                                candidateEmail = currentResume.email,
                                candidatePhone = currentResume.phone,
                                candidateLocation = currentResume.location
                            )
                        }
                    },
                    enabled = letterContent.isNotBlank(),
                    modifier = Modifier.testTag("cover_letter_print_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = "Print or System PDF",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // The Copyable and Editable Text Area
        OutlinedTextField(
            value = letterContent,
            onValueChange = {
                onLetterContentChange(it)
                copiedToClipboard = false
            },
            placeholder = {
                Text(
                    "Your personalized cover letter will appear here after generation. You can edit any paragraph and copy it directly to your clipboard.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 280.dp, max = 460.dp)
                .testTag("cover_letter_copyable_text_area"),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 22.sp,
                fontSize = 14.5.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }

    exportResultDialog?.let { result ->
        AlertDialog(
            onDismissRequest = { exportResultDialog = null },
            icon = {
                Icon(
                    imageVector = if (result.success) Icons.Default.FileDownload else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (result.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = if (result.success) "Cover Letter PDF Exported" else "Export Failed",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    if (result.success) {
                        Text(
                            text = "Your cover letter has been converted to an executive-ready PDF and saved to your device's Documents folder:",
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
                            text = result.errorMessage ?: "Unknown error while creating Cover Letter PDF.",
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
