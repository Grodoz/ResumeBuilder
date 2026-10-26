package com.richardbroyles.resumebuilder.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.richardbroyles.resumebuilder.data.model.ResumeProfile
import com.richardbroyles.resumebuilder.ui.theme.AtsScoreHigh
import com.richardbroyles.resumebuilder.util.DocumentExportService
import com.richardbroyles.resumebuilder.util.ExportDocumentFormat
import com.richardbroyles.resumebuilder.util.PdfGenerator
import com.richardbroyles.resumebuilder.util.UnifiedExportResult
import com.richardbroyles.resumebuilder.viewmodel.ResumeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ResumeExportButton(
    viewModel: ResumeViewModel,
    modifier: Modifier = Modifier,
    resume: ResumeProfile? = null,
    buttonText: String = "PDF Export",
    isCompact: Boolean = false
) {
    val context = LocalContext.current
    val currentResumeState by viewModel.resumeState.collectAsState()
    val targetResume = resume ?: currentResumeState

    val coroutineScope = rememberCoroutineScope()
    var isGenerating by remember { mutableStateOf(false) }
    var showFormatDropdown by remember { mutableStateOf(false) }
    var exportResult by remember { mutableStateOf<UnifiedExportResult?>(null) }

    fun triggerExport(format: ExportDocumentFormat) {
        isGenerating = true
        showFormatDropdown = false
        coroutineScope.launch(Dispatchers.IO) {
            val result = DocumentExportService.exportResume(
                context = context,
                resume = targetResume,
                format = format
            )
            withContext(Dispatchers.Main) {
                isGenerating = false
                exportResult = result
            }
        }
    }

    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Main Action Button (Triggers primary PDF export)
            Button(
                onClick = { triggerExport(ExportDocumentFormat.PDF) },
                enabled = !isGenerating,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = if (isCompact) {
                    ButtonDefaults.ContentPadding
                } else {
                    androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                },
                modifier = Modifier.testTag("export_pdf_button")
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Generating...",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "PDF Export",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = buttonText,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            // Dropdown Menu trigger for Word (.docx) and Print options
            FilledTonalButton(
                onClick = { showFormatDropdown = true },
                enabled = !isGenerating,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("export_format_dropdown_button"),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Export Format Options",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Format Selection Dropdown Menu
        DropdownMenu(
            expanded = showFormatDropdown,
            onDismissRequest = { showFormatDropdown = false }
        ) {
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Adobe PDF (.pdf)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "ATS-optimized vector layout",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                onClick = { triggerExport(ExportDocumentFormat.PDF) },
                modifier = Modifier.testTag("menu_export_pdf")
            )

            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = Color(0xFF1E40AF),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Microsoft Word (.docx)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Fully editable Office document",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                onClick = { triggerExport(ExportDocumentFormat.DOCX) },
                modifier = Modifier.testTag("menu_export_docx")
            )

            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Print via Android PrintManager",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                onClick = {
                    showFormatDropdown = false
                    PdfGenerator.printResumeWithPrintManager(context, targetResume)
                },
                modifier = Modifier.testTag("menu_print_resume")
            )
        }
    }

    // Success Dialog on Document Generation
    exportResult?.let { result ->
        DocumentExportSuccessDialog(
            result = result,
            onDismiss = { exportResult = null }
        )
    }
}

/**
 * Split Action Row providing prominent dedicated buttons for both PDF and Word (.docx) exports.
 */
@Composable
fun ResumeExportActionRow(
    viewModel: ResumeViewModel,
    modifier: Modifier = Modifier,
    resume: ResumeProfile? = null
) {
    val context = LocalContext.current
    val currentResumeState by viewModel.resumeState.collectAsState()
    val targetResume = resume ?: currentResumeState

    val coroutineScope = rememberCoroutineScope()
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var isGeneratingDocx by remember { mutableStateOf(false) }
    var exportResult by remember { mutableStateOf<UnifiedExportResult?>(null) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // PDF Export Button
        Button(
            onClick = {
                isGeneratingPdf = true
                coroutineScope.launch(Dispatchers.IO) {
                    val result = DocumentExportService.exportResume(
                        context = context,
                        resume = targetResume,
                        format = ExportDocumentFormat.PDF
                    )
                    withContext(Dispatchers.Main) {
                        isGeneratingPdf = false
                        exportResult = result
                    }
                }
            },
            enabled = !isGeneratingPdf && !isGeneratingDocx,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("row_export_pdf_btn")
        ) {
            if (isGeneratingPdf) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generating...", style = MaterialTheme.typography.labelMedium)
            } else {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Export PDF",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // Microsoft Word (.docx) Export Button
        OutlinedButton(
            onClick = {
                isGeneratingDocx = true
                coroutineScope.launch(Dispatchers.IO) {
                    val result = DocumentExportService.exportResume(
                        context = context,
                        resume = targetResume,
                        format = ExportDocumentFormat.DOCX
                    )
                    withContext(Dispatchers.Main) {
                        isGeneratingDocx = false
                        exportResult = result
                    }
                }
            },
            enabled = !isGeneratingPdf && !isGeneratingDocx,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, Color(0xFF1E40AF)),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("row_export_docx_btn")
        ) {
            if (isGeneratingDocx) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF1E40AF)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generating...", style = MaterialTheme.typography.labelMedium)
            } else {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = Color(0xFF1E40AF),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Export Word (.docx)",
                    color = Color(0xFF1E40AF),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }

    // Success Dialog on Document Generation
    exportResult?.let { result ->
        DocumentExportSuccessDialog(
            result = result,
            onDismiss = { exportResult = null }
        )
    }
}

/**
 * Polished Dialog shown when a document (PDF or Word .docx) has been generated successfully.
 */
@Composable
fun DocumentExportSuccessDialog(
    result: UnifiedExportResult,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(18.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = if (result.success) AtsScoreHigh.copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (result.success) Icons.Default.CheckCircle else Icons.Default.Description,
                            contentDescription = null,
                            tint = if (result.success) AtsScoreHigh else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (result.success) "${result.format.shortName} Generated Successfully" else "Export Failed",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (result.success) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (result.format == ExportDocumentFormat.PDF) Icons.Default.PictureAsPdf else Icons.Default.Description,
                                contentDescription = null,
                                tint = if (result.format == ExportDocumentFormat.PDF) Color(0xFFEF4444) else Color(0xFF1E40AF),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = result.fileName,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = result.format.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Saved to: ${result.filePathOrUri}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = result.errorMessage ?: "Failed to generate document.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            if (result.success && result.uri != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Open in Viewer (Microsoft Word / Google Docs / PDF Viewer)
                    Button(
                        onClick = {
                            try {
                                val intent = DocumentExportService.createViewIntent(result.uri, result.format)
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "No app available to open this document.", Toast.LENGTH_SHORT).show()
                            }
                            onDismiss()
                        },
                        modifier = Modifier.testTag("dialog_open_document_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open")
                    }

                    // Share Document
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = DocumentExportService.createShareIntent(result.uri, result.fileName, result.format)
                                context.startActivity(Intent.createChooser(intent, "Share ${result.fileName}"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to share document: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                            onDismiss()
                        },
                        modifier = Modifier.testTag("dialog_share_document_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }
                }
            } else {
                Button(onClick = onDismiss) {
                    Text("OK")
                }
            }
        },
        dismissButton = {
            if (result.success) {
                TextButton(onClick = onDismiss) {
                    Text("Done")
                }
            }
        }
    )
}
