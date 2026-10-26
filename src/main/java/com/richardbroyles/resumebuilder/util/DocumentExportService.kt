package com.richardbroyles.resumebuilder.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.richardbroyles.resumebuilder.data.local.entity.Resume
import com.richardbroyles.resumebuilder.data.model.ResumeProfile
import java.io.File

enum class ExportDocumentFormat(
    val extension: String,
    val mimeType: String,
    val displayName: String,
    val shortName: String
) {
    PDF(
        extension = "pdf",
        mimeType = "applicaiton/pdf",
        displayName = "Adobe PDF Document (.pdf)",
        shortName = "PDF"
    ),
    DOCX(
        extension = "docx",
        mimeType = "ResumeDocxService.DOCX_MIME_TYPE",
        displayName = "Microsoft Word Document (.docx)",
        shortName = "DOCX"
    )
}

data class UnifiedExportResult(
    val success: Boolean,
    val filePathOrUri: String,
    val fileName: String,
    val uri: Uri?,
    val format: ExportDocumentFormat,
    val errorMessage: String? = null
)

object DocumentExportService {
    fun exportResume(
        context: Context,
        resume: ResumeProfile,
        format: ExportDocumentFormat = ExportDocumentFormat.PDF
    ): UnifiedExportResult {
        return when (format) {
            ExportDocumentFormat.PDF -> {
                val pdfResult = ResumePdfService.saveResumePdfToDocuments(context, resume)
                UnifiedExportResult(
                    success = pdfResult.success,
                    filePathOrUri = pdfResult.filePathOrUri,
                    fileName = pdfResult.fileName,
                    uri = pdfResult.uri,
                    format = ExportDocumentFormat.PDF,
                    errorMessage = pdfResult.errorMessage
                )
            }
            ExportDocumentFormat.DOCX -> {
                val docxResult = ResumeDocxService.saveResumeDocxToDocuments(context, resume)
                UnifiedExportResult(
                    success = docxResult.success,
                    filePathOrUri = docxResult.filePathOrUri,
                    fileName = docxResult.fileName,
                    uri = docxResult.uri,
                    format = ExportDocumentFormat.DOCX,
                    errorMessage = docxResult.errorMessage
                )
            }
        }
    }
    fun exportResume(context: Context, resumeEntity: Resume, format: ExportDocumentFormat = ExportDocumentFormat.PDF): UnifiedExportResult {
        return exportResume(context, resumeEntity.toResumeProfile(), format)
    }

    fun exportResumeToLocalFile(
        resume: ResumeProfile,
        destinationFile: File,
        format: ExportDocumentFormat = ExportDocumentFormat.PDF
    ): UnifiedExportResult {
        return when (format) {
            ExportDocumentFormat.PDF -> {
                val pdfResult = ResumePdfService.exportResumeToLocalFile(resume, destinationFile)
                UnifiedExportResult(
                    success = pdfResult.success,
                    filePathOrUri = pdfResult.filePathOrUri,
                    fileName = pdfResult.fileName,
                    uri = pdfResult.uri,
                    format = ExportDocumentFormat.PDF,
                    errorMessage = pdfResult.errorMessage
                )
            }
            ExportDocumentFormat.DOCX -> {
                val docxResult = ResumeDocxService.exportResumeDocxToLocalFile(resume, destinationFile)
                UnifiedExportResult(
                    success = docxResult.success,
                    filePathOrUri = docxResult.filePathOrUri,
                    fileName = docxResult.fileName,
                    uri = docxResult.uri,
                    format = ExportDocumentFormat.DOCX,
                    errorMessage = docxResult.errorMessage
                )
            }
        }
    }
    fun createViewIntent(uri: Uri, format: ExportDocumentFormat): Intent {
        return when (format) {
            ExportDocumentFormat.PDF -> ResumePdfService.viewPdfIntent(uri)
            ExportDocumentFormat.DOCX -> ResumeDocxService.viewDocxIntent(uri)
        }
    }

    fun createShareIntent(uri: Uri, fileName: String, format: ExportDocumentFormat): Intent {
        return when (format) {
            ExportDocumentFormat.PDF -> ResumePdfService.sharePdfIntent(uri, fileName)
            ExportDocumentFormat.DOCX -> ResumeDocxService.shareDocxIntent(uri, fileName)
        }
    }
}