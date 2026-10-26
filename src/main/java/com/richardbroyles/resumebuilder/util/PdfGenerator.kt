package com.richardbroyles.resumebuilder.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.richardbroyles.resumebuilder.data.local.entity.Resume
import com.richardbroyles.resumebuilder.data.model.ResumeProfile
import java.io.File

object PdfGenerator {

    data class ExportResult(
        val success: Boolean,
        val filePathOrUri: String,
        val fileName: String,
        val uri: Uri?,
        val errorMessage: String? = null
    )

    fun exportResumeToDocuments(context: Context, resume: ResumeProfile): ExportResult {
        val result = ResumePdfService.saveResumePdfToDocuments(context, resume)
        return ExportResult(
            success = result.success,
            filePathOrUri = result.filePathOrUri,
            fileName = result.fileName,
            uri = result.uri,
            errorMessage = result.errorMessage
        )
    }

    fun exportResumeToDocuments(context: Context, resume: Resume): ExportResult {
        val result = ResumePdfService.saveResumePdfToDocuments(context, resume)
        return ExportResult(
            success = result.success,
            filePathOrUri = result.filePathOrUri,
            fileName = result.fileName,
            uri = result.uri,
            errorMessage = result.errorMessage
        )
    }

    fun printResumeWithPrintManager(context: Context, resume: ResumeProfile) {
        ResumePdfService.printResumeWithPrintManager(context, resume)
    }

    fun printResumeWithPrintManager(context: Context, resume: Resume) {
        ResumePdfService.printResumeWithPrintManager(context, resume)
    }

    fun exportResumeToLocalFile(resume: ResumeProfile, destinationFile: File): ExportResult {
        val result = ResumePdfService.exportResumeToLocalFile(resume, destinationFile)
        return ExportResult(
            success = result.success,
            filePathOrUri = result.filePathOrUri,
            fileName = result.fileName,
            uri = result.uri,
            errorMessage = result.errorMessage
        )
    }

    fun exportResumeToLocalFile(resume: Resume, destinationFile: File): ExportResult {
        val result = ResumePdfService.exportResumeToLocalFile(resume, destinationFile)
        return ExportResult(
            success = result.success,
            filePathOrUri = result.filePathOrUri,
            fileName = result.fileName,
            uri = result.uri,
            errorMessage = result.errorMessage
        )
    }

    fun exportCoverLetterToDocuments(
        context: Context,
        letterText: String,
        jobTitle: String,
        companyName: String,
        candidateName: String = "",
        candidateEmail: String = "",
        candidatePhone: String = "",
        candidateLocation: String = ""
    ): ExportResult {
        val result = ResumePdfService.saveCoverLetterPdfToDocuments(
            context = context,
            letterText = letterText,
            jobTitle = jobTitle,
            companyName = companyName,
            candidateName = candidateName,
            candidateEmail = candidateEmail,
            candidatePhone = candidatePhone,
            candidateLocation = candidateLocation
        )
        return ExportResult(
            success = result.success,
            filePathOrUri = result.filePathOrUri,
            fileName = result.fileName,
            uri = result.uri,
            errorMessage = result.errorMessage
        )
    }

    fun printCoverLetterWithPrintManager(
        context: Context,
        letterText: String,
        jobTitle: String,
        companyName: String,
        candidateName: String = "",
        candidateEmail: String = "",
        candidatePhone: String = "",
        candidateLocation: String = ""
    ) {
        ResumePdfService.printCoverLetterWithPrintManager(
            context = context,
            letterText = letterText,
            jobTitle = jobTitle,
            companyName = companyName,
            candidateName = candidateName,
            candidateEmail = candidateEmail,
            candidatePhone = candidatePhone,
            candidateLocation = candidateLocation
        )
    }

    fun viewPdfIntent(context: Context, uri: Uri): Intent {
        return ResumePdfService.viewPdfIntent(uri)
    }

    fun sharePdfIntent(uri: Uri, fileName: String): Intent {
        return ResumePdfService.sharePdfIntent(uri, fileName)
    }
}
