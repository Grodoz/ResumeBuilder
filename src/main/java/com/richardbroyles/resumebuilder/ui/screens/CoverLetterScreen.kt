package com.richardbroyles.resumebuilder.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.LaunchedEffect
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
import com.richardbroyles.resumebuilder.ui.components.SectionHeader
import com.richardbroyles.resumebuilder.util.PdfGenerator
import com.richardbroyles.resumebuilder.viewmodel.ResumeViewModel

@Composable
fun CoverLetterScreen(
    viewModel: ResumeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedJob by viewModel.selectedJob.collectAsState()
    val activeLetter by viewModel.activeCoverLetter.collectAsState()
    val isGenerating by viewModel.isGeneratingCoverLetter.collectAsState()
    val storedResume by viewModel.resumeState.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Custom Job Description & Resume, 1: Saved Job Match
    var selectedTone by remember { mutableStateOf("Professional & Results-Oriented") }
    var letterTextState by remember { mutableStateOf("") }
    var exportSuccessDialogResult by remember { mutableStateOf<PdfGenerator.ExportResult?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Custom Job & Resume", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("tab_custom_cover_letter")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Saved Job Letter", style = MaterialTheme.typography.labelMedium) },
                modifier = Modifier.testTag("tab_saved_job_letter")
            )
        }

        if (selectedTab == 0) {
            TargetCoverLetterGeneratorScreen(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize(),
                onNavigateBack = null
            )
        } else {
            LaunchedEffect(activeLetter) {
                if (activeLetter != null) {
                    letterTextState = activeLetter?.content ?: ""
                }
            }

            // Auto generate first time if null for saved job
            LaunchedEffect(Unit) {
                if (activeLetter == null) {
                    viewModel.generatePersonalizedCoverLetter(tone = selectedTone)
                }
            }

            val tones = listOf(
                "Professional & Results-Oriented",
                "Confident Executive",
                "Enthusiastic & High-Energy",
                "Concise Technical"
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                // Generator Config Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cover_letter_config_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Auto-Generate Tailored Cover Letter",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Harmonizes resume achievements with ${selectedJob.title} (${selectedJob.company})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Letter Tone & Style:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
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

                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { viewModel.generatePersonalizedCoverLetter(tone = selectedTone) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("generate_cover_letter_btn"),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isGenerating
                            ) {
                                if (isGenerating) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Writing tailored cover letter...", style = MaterialTheme.typography.labelMedium)
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Generate Cover Letter for This Job", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { selectedTab = 0 },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_open_custom_job_cover_letter"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Enter Custom Company & Job Description",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }

                // Active Cover Letter Editor Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cover_letter_editor_card"),
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
                                    title = "Letter Draft",
                                    subtitle = "Edit directly or copy to clipboard"
                                )
                                Row {
                                    // Copy to clipboard
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Tailored Cover Letter", letterTextState)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Cover letter copied to clipboard!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.testTag("copy_cover_letter_btn")
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy letter", tint = MaterialTheme.colorScheme.primary)
                                    }

                                    // Save
                                    IconButton(
                                        onClick = {
                                            viewModel.updateCoverLetterContent(letterTextState)
                                            Toast.makeText(context, "Cover letter saved!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.testTag("save_cover_letter_btn")
                                    ) {
                                        Icon(Icons.Default.Save, contentDescription = "Save letter", tint = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = letterTextState,
                                onValueChange = {
                                    letterTextState = it
                                    viewModel.updateCoverLetterEditorText(it)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("cover_letter_text_field"),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                minLines = 14,
                                maxLines = 22,
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
                                            letterText = letterTextState,
                                            jobTitle = selectedJob.title,
                                            companyName = selectedJob.company,
                                            candidateName = storedResume.fullName,
                                            candidateEmail = storedResume.email,
                                            candidatePhone = storedResume.phone,
                                            candidateLocation = storedResume.location
                                        )
                                        exportSuccessDialogResult = result
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("export_letter_pdf_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = letterTextState.isNotBlank()
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
                                            letterText = letterTextState,
                                            jobTitle = selectedJob.title,
                                            companyName = selectedJob.company,
                                            candidateName = storedResume.fullName,
                                            candidateEmail = storedResume.email,
                                            candidatePhone = storedResume.phone,
                                            candidateLocation = storedResume.location
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("print_letter_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = letterTextState.isNotBlank()
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

                            Spacer(modifier = Modifier.height(12.dp))
                            activeLetter?.let { letter ->
                                if (letter.keyAchievementsCited.isNotEmpty()) {
                                    Text(
                                        text = "Achievements Cited from Resume:",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        letter.keyAchievementsCited.forEach { achievement ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                            ) {
                                                Text(
                                                    text = "✓ $achievement",
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
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    // PDF Export Success Dialog with View & Share options
    exportSuccessDialogResult?.let { result ->
        AlertDialog(
            onDismissRequest = { exportSuccessDialogResult = null },
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
                                exportSuccessDialogResult = null
                            }
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
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
                    OutlinedButton(onClick = { exportSuccessDialogResult = null }) {
                        Text("Dismiss")
                    }
                }
            }
        )
    }
}
