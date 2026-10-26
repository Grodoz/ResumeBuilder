package com.richardbroyles.resumebuilder.util

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.MediaStore
import android.util.Log
import com.richardbroyles.resumebuilder.data.local.entity.Resume
import com.richardbroyles.resumebuilder.data.model.EducationItem
import com.richardbroyles.resumebuilder.data.model.ExperienceItem
import com.richardbroyles.resumebuilder.data.model.ResumeProfile
import com.richardbroyles.resumebuilder.data.model.SkillItem
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ResumePdfService {

    private const val TAG = "ResumePdfService"

    data class ExportResult(
        val success: Boolean,
        val filePathOrUri: String,
        val fileName: String,
        val uri: Uri?,
        val errorMessage: String? = null
    )

    /**
     * Converts the provided [ResumeProfile] into a formatted PDF file
     * and saves it directly to the device's Documents directory.
     */
    fun saveResumePdfToDocuments(context: Context, resume: ResumeProfile): ExportResult {
        return try {
            val sanitizedName = resume.fullName.trim()
                .replace("[^a-zA-Z0-9]".toRegex(), "_")
                .ifEmpty { "Resume" }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "${sanitizedName}_TailoredResume_$timeStamp.pdf"

            var outputUri: Uri? = null
            var savedPath = ""
            var writeSuccess = false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/ResumeBuilder")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }

                    outputUri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                    if (outputUri != null) {
                        resolver.openOutputStream(outputUri)?.use { stream ->
                            writeResumePdfToStream(resume, stream)
                            writeSuccess = true
                        }

                        // Clear pending status so other apps can access
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(outputUri, contentValues, null, null)

                        savedPath = "Documents/ResumeBuilder/$fileName"
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "MediaStore insert failed, falling back to app external files: ${e.message}")
                }
            }

            // Fallback for Android < Q or when MediaStore is unavailable/fails
            if (!writeSuccess) {
                val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                    ?: File(context.filesDir, "Documents").apply { mkdirs() }
                val targetDir = File(docsDir, "ResumeBuilder")
                if (!targetDir.exists()) targetDir.mkdirs()
                val file = File(targetDir, fileName)
                FileOutputStream(file).use { stream ->
                    writeResumePdfToStream(resume, stream)
                    writeSuccess = true
                }
                savedPath = file.absolutePath
                outputUri = try {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                } catch (e: Exception) {
                    Uri.fromFile(file)
                }
            }

            if (writeSuccess && outputUri != null) {
                ExportResult(
                    success = true,
                    filePathOrUri = savedPath,
                    fileName = fileName,
                    uri = outputUri
                )
            } else {
                ExportResult(
                    success = false,
                    filePathOrUri = "",
                    fileName = "",
                    uri = null,
                    errorMessage = "Failed to write PDF to storage"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving resume PDF to Documents", e)
            ExportResult(
                success = false,
                filePathOrUri = "",
                fileName = "",
                uri = null,
                errorMessage = e.localizedMessage ?: "Failed to generate and save PDF"
            )
        }
    }

    /**
     * Uses Android's native [PrintManager] to print or save the resume as PDF
     * via the system Print Spooler dialog.
     */
    fun printResumeWithPrintManager(context: Context, resume: ResumeProfile) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager != null) {
            try {
                val jobName = "Resume - ${resume.fullName.ifEmpty { "Candidate" }}"
                val adapter = ResumePrintDocumentAdapter(resume)
                val printAttributes = PrintAttributes.Builder()
                    .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()
                printManager.print(jobName, adapter, printAttributes)
            } catch (e: Throwable) {
                Log.w(TAG, "PrintManager.print call failed: ${e.message}")
            }
        } else {
            Log.w(TAG, "PrintManager service is unavailable on this device.")
        }
    }

    /**
     * Converts a Room [Resume] entity into a formatted PDF file
     * and saves it directly to the device's Documents directory.
     */
    fun saveResumePdfToDocuments(context: Context, resume: Resume): ExportResult {
        return saveResumePdfToDocuments(context, resume.toResumeProfile())
    }

    /**
     * Prints or saves to PDF via [PrintManager] using a Room [Resume] entity.
     */
    fun printResumeWithPrintManager(context: Context, resume: Resume) {
        printResumeWithPrintManager(context, resume.toResumeProfile())
    }

    /**
     * Directly writes the resume PDF into a specific destination [File] on local storage.
     */
    fun exportResumeToLocalFile(
        resume: ResumeProfile,
        destinationFile: File
    ): ExportResult {
        return try {
            destinationFile.parentFile?.mkdirs()
            FileOutputStream(destinationFile).use { out ->
                writeResumePdfToStream(resume, out)
            }
            ExportResult(
                success = true,
                filePathOrUri = destinationFile.absolutePath,
                fileName = destinationFile.name,
                uri = Uri.fromFile(destinationFile)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write PDF to local file: ${destinationFile.absolutePath}", e)
            ExportResult(
                success = false,
                filePathOrUri = destinationFile.absolutePath,
                fileName = destinationFile.name,
                uri = null,
                errorMessage = e.localizedMessage ?: "Failed to write PDF to local file"
            )
        }
    }

    /**
     * Directly writes a Room [Resume] entity as PDF into a specific destination [File] on local storage.
     */
    fun exportResumeToLocalFile(
        resume: Resume,
        destinationFile: File
    ): ExportResult {
        return exportResumeToLocalFile(resume.toResumeProfile(), destinationFile)
    }

    /**
     * Creates an Intent to view the PDF in the system's default PDF viewer.
     */
    fun viewPdfIntent(uri: Uri): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            clipData = ClipData.newRawUri("Resume PDF", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Creates an Intent to share the generated PDF via external apps (Email, Drive, Chat).
     */
    fun sharePdfIntent(uri: Uri, fileName: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            clipData = ClipData.newRawUri(fileName, uri)
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Core renderer: formats the [ResumeProfile] into a multi-page, ATS-friendly [PdfDocument].
     */
    fun generateFormattedPdfDocument(resume: ResumeProfile): PdfDocument {
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // Standard A4 width at 72dpi
        val pageHeight = 842 // Standard A4 height at 72dpi
        val leftMargin = 40f
        val rightMargin = pageWidth - 40f
        val contentWidth = rightMargin - leftMargin
        val bottomMarginLimit = pageHeight - 50f

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var currentPage = pdfDocument.startPage(pageInfo)
        var canvas = currentPage.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var currentY = 50f

        fun startNewPage() {
            pdfDocument.finishPage(currentPage)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            currentPage = pdfDocument.startPage(pageInfo)
            canvas = currentPage.canvas
            canvas.drawColor(Color.WHITE)
            currentY = 45f
        }

        fun checkPageBreak(requiredHeight: Float) {
            if (currentY + requiredHeight > bottomMarginLimit) {
                startNewPage()
            }
        }

        fun drawSectionHeader(title: String) {
            checkPageBreak(30f)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 11f
            paint.color = Color.parseColor("#1E293B")
            canvas.drawText(title.uppercase(Locale.getDefault()), leftMargin, currentY, paint)
            currentY += 4f

            paint.color = Color.parseColor("#2563EB")
            paint.strokeWidth = 1.5f
            canvas.drawLine(leftMargin, currentY, leftMargin + 75f, currentY, paint)
            currentY += 12f
        }

        // --- PAGE 1: Canvas init ---
        canvas.drawColor(Color.WHITE)

        // 1. Header - Candidate Name
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 22f
        paint.color = Color.parseColor("#0F172A")
        val displayName = resume.fullName.ifEmpty { "Professional Candidate" }
        canvas.drawText(displayName, leftMargin, currentY, paint)
        currentY += 20f

        // 2. Target Role
        if (resume.targetRole.isNotEmpty()) {
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 12f
            paint.color = Color.parseColor("#2563EB")
            canvas.drawText(resume.targetRole.uppercase(Locale.getDefault()), leftMargin, currentY, paint)
            currentY += 16f
        }

        // 3. Contact Info
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9f
        paint.color = Color.parseColor("#475569")
        val contactList = listOfNotNull(
            resume.email.takeIf { it.isNotEmpty() },
            resume.phone.takeIf { it.isNotEmpty() },
            resume.location.takeIf { it.isNotEmpty() },
            resume.linkedinUrl.takeIf { it.isNotEmpty() }
        )
        if (contactList.isNotEmpty()) {
            canvas.drawText(contactList.joinToString("  •  "), leftMargin, currentY, paint)
            currentY += 14f
        }

        // Header Divider
        paint.color = Color.parseColor("#CBD5E1")
        paint.strokeWidth = 1f
        canvas.drawLine(leftMargin, currentY, rightMargin, currentY, paint)
        currentY += 16f

        // 4. Professional Summary
        if (resume.summary.isNotEmpty()) {
            drawSectionHeader("Professional Summary")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 9.5f
            paint.color = Color.parseColor("#334155")

            val summaryLines = wrapText(resume.summary, paint, contentWidth)
            for (line in summaryLines) {
                checkPageBreak(14f)
                canvas.drawText(line, leftMargin, currentY, paint)
                currentY += 13f
            }
            currentY += 8f
        }

        // 5. Work Experience
        if (resume.experiences.isNotEmpty()) {
            drawSectionHeader("Work Experience")
            for (exp in resume.experiences) {
                checkPageBreak(35f)

                // Role title & Date
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 10.5f
                paint.color = Color.parseColor("#0F172A")
                canvas.drawText(exp.jobTitle, leftMargin, currentY, paint)

                val dateRange = "${exp.startDate} - ${if (exp.isCurrent) "Present" else exp.endDate}"
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textSize = 9f
                paint.color = Color.parseColor("#64748B")
                val dateWidth = paint.measureText(dateRange)
                canvas.drawText(dateRange, rightMargin - dateWidth, currentY, paint)
                currentY += 13f

                // Company & Location
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 9.5f
                paint.color = Color.parseColor("#2563EB")
                val companyLoc = if (exp.location.isNotEmpty()) "${exp.company} • ${exp.location}" else exp.company
                canvas.drawText(companyLoc, leftMargin, currentY, paint)
                currentY += 14f

                // Bullet points
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textSize = 9f
                paint.color = Color.parseColor("#334155")
                for (bullet in exp.bullets) {
                    val bulletLines = wrapText(bullet, paint, contentWidth - 14f)
                    for ((idx, line) in bulletLines.withIndex()) {
                        checkPageBreak(13f)
                        if (idx == 0) {
                            paint.color = Color.parseColor("#3B82F6")
                            canvas.drawCircle(leftMargin + 4f, currentY - 3f, 2f, paint)
                            paint.color = Color.parseColor("#334155")
                            canvas.drawText(line, leftMargin + 14f, currentY, paint)
                        } else {
                            canvas.drawText(line, leftMargin + 14f, currentY, paint)
                        }
                        currentY += 12f
                    }
                }
                currentY += 6f
            }
            currentY += 4f
        }

        // 6. Core Skills
        if (resume.skills.isNotEmpty()) {
            drawSectionHeader("Core Competencies & Skills")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 9f
            paint.color = Color.parseColor("#1E293B")

            val grouped = resume.skills.groupBy { it.category }
            for ((category, skills) in grouped) {
                checkPageBreak(14f)
                val skillText = "$category: " + skills.joinToString(", ") { it.name }
                val lines = wrapText(skillText, paint, contentWidth)
                for (line in lines) {
                    checkPageBreak(13f)
                    canvas.drawText(line, leftMargin, currentY, paint)
                    currentY += 12f
                }
            }
            currentY += 8f
        }

        // 7. Education
        if (resume.educations.isNotEmpty()) {
            drawSectionHeader("Education")
            for (edu in resume.educations) {
                checkPageBreak(30f)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 10f
                paint.color = Color.parseColor("#0F172A")
                canvas.drawText("${edu.degree} in ${edu.fieldOfStudy}", leftMargin, currentY, paint)

                if (edu.graduationYear.isNotEmpty()) {
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    paint.textSize = 9f
                    paint.color = Color.parseColor("#64748B")
                    val yearWidth = paint.measureText(edu.graduationYear)
                    canvas.drawText(edu.graduationYear, rightMargin - yearWidth, currentY, paint)
                }
                currentY += 13f

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textSize = 9f
                paint.color = Color.parseColor("#475569")
                val schoolLine = if (edu.gpaOrHonors.isNotEmpty()) "${edu.institution} (${edu.gpaOrHonors})" else edu.institution
                canvas.drawText(schoolLine, leftMargin, currentY, paint)
                currentY += 15f
            }
            currentY += 4f
        }

        // 7.5 Projects
        if (resume.projects.isNotEmpty()) {
            drawSectionHeader("Key Technical Projects")
            for (proj in resume.projects) {
                checkPageBreak(30f)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 10f
                paint.color = Color.parseColor("#0F172A")
                val projTitle = if (proj.role.isNotEmpty()) "${proj.title} • ${proj.role}" else proj.title
                canvas.drawText(projTitle, leftMargin, currentY, paint)
                currentY += 13f

                if (proj.technologies.isNotEmpty()) {
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    paint.textSize = 8.5f
                    paint.color = Color.parseColor("#2563EB")
                    canvas.drawText("Tech: ${proj.technologies.joinToString(", ")}", leftMargin, currentY, paint)
                    currentY += 12f
                }

                if (proj.description.isNotEmpty()) {
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    paint.textSize = 9f
                    paint.color = Color.parseColor("#334155")
                    val descLines = wrapText(proj.description, paint, contentWidth)
                    for (line in descLines) {
                        checkPageBreak(13f)
                        canvas.drawText(line, leftMargin, currentY, paint)
                        currentY += 12f
                    }
                }
                currentY += 4f
            }
            currentY += 4f
        }

        // 8. Certifications
        if (resume.certifications.isNotEmpty()) {
            drawSectionHeader("Certifications")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 9f
            paint.color = Color.parseColor("#334155")
            for (cert in resume.certifications) {
                checkPageBreak(14f)
                paint.color = Color.parseColor("#10B981")
                canvas.drawCircle(leftMargin + 4f, currentY - 3f, 2f, paint)
                paint.color = Color.parseColor("#334155")
                canvas.drawText(cert, leftMargin + 14f, currentY, paint)
                currentY += 13f
            }
        }

        pdfDocument.finishPage(currentPage)
        return pdfDocument
    }

    /**
     * Converts the provided cover letter content into a formatted PDF file
     * and saves it directly to the device's Documents directory.
     */
    fun saveCoverLetterPdfToDocuments(
        context: Context,
        letterText: String,
        jobTitle: String,
        companyName: String,
        candidateName: String = "",
        candidateEmail: String = "",
        candidatePhone: String = "",
        candidateLocation: String = ""
    ): ExportResult {
        return try {
            val sanitizedCompany = companyName.trim()
                .replace("[^a-zA-Z0-9]".toRegex(), "_")
                .ifEmpty { "Company" }
            val sanitizedCandidate = candidateName.trim()
                .replace("[^a-zA-Z0-9]".toRegex(), "_")
                .ifEmpty { "Candidate" }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "${sanitizedCandidate}_CoverLetter_${sanitizedCompany}_$timeStamp.pdf"

            val pdfDocument = generateFormattedCoverLetterPdfDocument(
                letterText = letterText,
                jobTitle = jobTitle,
                companyName = companyName,
                candidateName = candidateName,
                candidateEmail = candidateEmail,
                candidatePhone = candidatePhone,
                candidateLocation = candidateLocation
            )

            var outputUri: Uri? = null
            var savedPath = ""
            var writeSuccess = false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/ResumeBuilder")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }

                    outputUri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                    if (outputUri != null) {
                        resolver.openOutputStream(outputUri)?.use { stream ->
                            writeCoverLetterPdfToStream(
                                letterText = letterText,
                                jobTitle = jobTitle,
                                companyName = companyName,
                                candidateName = candidateName,
                                candidateEmail = candidateEmail,
                                candidatePhone = candidatePhone,
                                candidateLocation = candidateLocation,
                                outputStream = stream
                            )
                            writeSuccess = true
                        }

                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(outputUri, contentValues, null, null)

                        savedPath = "Documents/ResumeBuilder/$fileName"
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Cover letter MediaStore insert failed, falling back to app files: ${e.message}")
                }
            }

            if (!writeSuccess) {
                val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                    ?: File(context.filesDir, "Documents").apply { mkdirs() }
                val targetDir = File(docsDir, "ResumeBuilder")
                if (!targetDir.exists()) targetDir.mkdirs()
                val file = File(targetDir, fileName)
                FileOutputStream(file).use { stream ->
                    writeCoverLetterPdfToStream(
                        letterText = letterText,
                        jobTitle = jobTitle,
                        companyName = companyName,
                        candidateName = candidateName,
                        candidateEmail = candidateEmail,
                        candidatePhone = candidatePhone,
                        candidateLocation = candidateLocation,
                        outputStream = stream
                    )
                    writeSuccess = true
                }
                savedPath = file.absolutePath
                outputUri = try {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                } catch (e: Exception) {
                    Uri.fromFile(file)
                }
            }

            if (writeSuccess && outputUri != null) {
                ExportResult(
                    success = true,
                    filePathOrUri = savedPath,
                    fileName = fileName,
                    uri = outputUri
                )
            } else {
                ExportResult(
                    success = false,
                    filePathOrUri = "",
                    fileName = "",
                    uri = null,
                    errorMessage = "Failed to write Cover Letter PDF to storage"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving cover letter PDF to Documents", e)
            ExportResult(
                success = false,
                filePathOrUri = "",
                fileName = "",
                uri = null,
                errorMessage = e.localizedMessage ?: "Failed to generate and save Cover Letter PDF"
            )
        }
    }

    /**
     * Uses Android's native [PrintManager] to print or save the cover letter as PDF
     * via the system Print Spooler dialog.
     */
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
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager != null) {
            try {
                val jobName = "CoverLetter_${companyName.ifEmpty { "Target_Role" }}"
                val adapter = CoverLetterPrintDocumentAdapter(
                    letterText = letterText,
                    jobTitle = jobTitle,
                    companyName = companyName,
                    candidateName = candidateName,
                    candidateEmail = candidateEmail,
                    candidatePhone = candidatePhone,
                    candidateLocation = candidateLocation
                )
                val printAttributes = PrintAttributes.Builder()
                    .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()
                printManager.print(jobName, adapter, printAttributes)
            } catch (e: Throwable) {
                Log.w(TAG, "PrintManager call failed: ${e.message}")
            }
        } else {
            Log.w(TAG, "PrintManager service is unavailable on this device.")
        }
    }

    /**
     * Generates a beautifully formatted, executive-ready [PdfDocument] for the given cover letter.
     */
    fun generateFormattedCoverLetterPdfDocument(
        letterText: String,
        jobTitle: String,
        companyName: String,
        candidateName: String,
        candidateEmail: String = "",
        candidatePhone: String = "",
        candidateLocation: String = ""
    ): PdfDocument {
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // Standard A4 width at 72dpi
        val pageHeight = 842 // Standard A4 height at 72dpi
        val leftMargin = 48f
        val rightMargin = pageWidth - 48f
        val contentWidth = rightMargin - leftMargin
        val bottomMarginLimit = pageHeight - 50f

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var currentPage = pdfDocument.startPage(pageInfo)
        var canvas = currentPage.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var currentY = 50f

        fun startNewPage() {
            pdfDocument.finishPage(currentPage)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            currentPage = pdfDocument.startPage(pageInfo)
            canvas = currentPage.canvas
            canvas.drawColor(Color.WHITE)
            currentY = 48f
        }

        fun checkPageBreak(requiredHeight: Float) {
            if (currentY + requiredHeight > bottomMarginLimit) {
                startNewPage()
            }
        }

        canvas.drawColor(Color.WHITE)

        // 1. Candidate Name Header
        val displayName = candidateName.ifBlank { "Professional Candidate" }
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 20f
        paint.color = Color.parseColor("#0F172A")
        canvas.drawText(displayName, leftMargin, currentY, paint)
        currentY += 18f

        // 2. Candidate Contact details
        val contacts = listOfNotNull(
            candidateEmail.takeIf { it.isNotBlank() },
            candidatePhone.takeIf { it.isNotBlank() },
            candidateLocation.takeIf { it.isNotBlank() }
        )
        if (contacts.isNotEmpty()) {
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 9.5f
            paint.color = Color.parseColor("#475569")
            canvas.drawText(contacts.joinToString("  •  "), leftMargin, currentY, paint)
            currentY += 14f
        }

        // Accent divider
        paint.color = Color.parseColor("#2563EB")
        paint.strokeWidth = 2f
        canvas.drawLine(leftMargin, currentY, rightMargin, currentY, paint)
        currentY += 22f

        // 3. Date
        val formattedDate = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date())
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9.5f
        paint.color = Color.parseColor("#64748B")
        canvas.drawText(formattedDate, leftMargin, currentY, paint)
        currentY += 18f

        // 4. Recipient block
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10f
        paint.color = Color.parseColor("#1E293B")
        canvas.drawText("Hiring Manager & Recruitment Team", leftMargin, currentY, paint)
        currentY += 14f

        if (companyName.isNotBlank()) {
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 9.5f
            paint.color = Color.parseColor("#334155")
            canvas.drawText(companyName, leftMargin, currentY, paint)
            currentY += 14f
        }

        // 5. Subject Line
        currentY += 6f
        val subject = if (jobTitle.isNotBlank()) "Re: Application for $jobTitle Role" else "Re: Application for Position"
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10.5f
        paint.color = Color.parseColor("#2563EB")
        canvas.drawText(subject, leftMargin, currentY, paint)
        currentY += 20f

        // 6. Letter Content (split by paragraphs)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9.5f
        paint.color = Color.parseColor("#334155")

        val rawParagraphs = letterText.split("\n\n")
        for (rawParagraph in rawParagraphs) {
            val cleaned = rawParagraph.trim()
            if (cleaned.isEmpty()) continue

            // Split internal single line breaks if any
            val subLines = cleaned.split("\n")
            for (subLine in subLines) {
                val wrapped = wrapText(subLine.trim(), paint, contentWidth)
                for (line in wrapped) {
                    checkPageBreak(15f)
                    canvas.drawText(line, leftMargin, currentY, paint)
                    currentY += 13.5f
                }
            }
            // Paragraph spacing
            currentY += 9f
        }

        // 7. Sign-off check
        checkPageBreak(40f)
        currentY += 6f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9.5f
        paint.color = Color.parseColor("#334155")
        canvas.drawText("Sincerely,", leftMargin, currentY, paint)
        currentY += 20f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10.5f
        paint.color = Color.parseColor("#0F172A")
        canvas.drawText(displayName, leftMargin, currentY, paint)

        pdfDocument.finishPage(currentPage)
        return pdfDocument
    }

    /**
     * Checks if native [PdfDocument] is operational in the current Android runtime.
     * In unit/Robolectric test environments, native document allocation returns 0.
     */
    fun canGenerateNativePdfDocument(): Boolean {
        return try {
            val doc = PdfDocument()
            val info = PdfDocument.PageInfo.Builder(100, 100, 1).create()
            val page = doc.startPage(info)
            doc.finishPage(page)
            doc.close()
            true
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Writes formatted resume PDF content to the provided [OutputStream].
     * Uses native [PdfDocument] when available, or standard PDF 1.4 specification
     * for headless/testing environments without external library dependencies.
     */
    fun writeResumePdfToStream(resume: ResumeProfile, outputStream: OutputStream) {
        try {
            val pdfDocument = generateFormattedPdfDocument(resume)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
        } catch (e: Throwable) {
            Log.w(TAG, "Native PdfDocument not available (${e.message}), generating standard PDF 1.4 stream")
            val pdfBytes = createStandardPdfBytes(resume)
            outputStream.write(pdfBytes)
            outputStream.flush()
        }
    }

    /**
     * Writes formatted cover letter PDF content to the provided [OutputStream].
     */
    fun writeCoverLetterPdfToStream(
        letterText: String,
        jobTitle: String,
        companyName: String,
        candidateName: String,
        candidateEmail: String = "",
        candidatePhone: String = "",
        candidateLocation: String = "",
        outputStream: OutputStream
    ) {
        try {
            val pdfDocument = generateFormattedCoverLetterPdfDocument(
                letterText = letterText,
                jobTitle = jobTitle,
                companyName = companyName,
                candidateName = candidateName,
                candidateEmail = candidateEmail,
                candidatePhone = candidatePhone,
                candidateLocation = candidateLocation
            )
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
        } catch (e: Throwable) {
            Log.w(TAG, "Native Cover Letter PdfDocument not available (${e.message}), generating standard PDF 1.4 stream")
            val pdfBytes = createStandardCoverLetterPdfBytes(
                letterText = letterText,
                jobTitle = jobTitle,
                companyName = companyName,
                candidateName = candidateName,
                candidateEmail = candidateEmail,
                candidatePhone = candidatePhone,
                candidateLocation = candidateLocation
            )
            outputStream.write(pdfBytes)
            outputStream.flush()
        }
    }

    /**
     * Synthesizes a valid, ATS-optimized PDF 1.4 document from the given [ResumeProfile].
     */
    fun createStandardPdfBytes(resume: ResumeProfile): ByteArray {
        val streamContent = StringBuilder()
        fun escapePdf(text: String): String {
            return text.replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("\r", "")
        }

        streamContent.append("BT\n")

        // Candidate Name
        streamContent.append("/F1 20 Tf\n")
        streamContent.append("40 790 Td\n")
        streamContent.append("(${escapePdf(resume.fullName.ifBlank { "Professional Candidate" })}) Tj\n")
        streamContent.append("0 -22 Td\n")

        // Target Role
        if (resume.targetRole.isNotBlank()) {
            streamContent.append("/F1 11 Tf\n")
            streamContent.append("(${escapePdf(resume.targetRole.uppercase(Locale.getDefault()))}) Tj\n")
            streamContent.append("0 -16 Td\n")
        }

        // Contact Info
        val contacts = listOfNotNull(
            resume.email.takeIf { it.isNotBlank() },
            resume.phone.takeIf { it.isNotBlank() },
            resume.location.takeIf { it.isNotBlank() },
            resume.linkedinUrl.takeIf { it.isNotBlank() }
        ).joinToString("  |  ")
        if (contacts.isNotBlank()) {
            streamContent.append("/F2 9 Tf\n")
            streamContent.append("(${escapePdf(contacts)}) Tj\n")
            streamContent.append("0 -20 Td\n")
        }

        // Professional Summary
        if (resume.summary.isNotBlank()) {
            streamContent.append("/F1 10 Tf\n")
            streamContent.append("(PROFESSIONAL SUMMARY) Tj\n")
            streamContent.append("/F2 9 Tf\n")
            streamContent.append("0 -14 Td\n")
            val summaryWords = resume.summary.split(" ")
            var curLine = StringBuilder()
            for (word in summaryWords) {
                if (curLine.length + word.length > 85) {
                    streamContent.append("(${escapePdf(curLine.toString())}) Tj\n")
                    streamContent.append("0 -12 Td\n")
                    curLine = StringBuilder(word)
                } else {
                    if (curLine.isNotEmpty()) curLine.append(" ")
                    curLine.append(word)
                }
            }
            if (curLine.isNotEmpty()) {
                streamContent.append("(${escapePdf(curLine.toString())}) Tj\n")
                streamContent.append("0 -16 Td\n")
            }
        }

        // Work Experience
        if (resume.experiences.isNotEmpty()) {
            streamContent.append("/F1 10 Tf\n")
            streamContent.append("(WORK EXPERIENCE) Tj\n")
            streamContent.append("0 -14 Td\n")
            for (exp in resume.experiences) {
                streamContent.append("/F1 9.5 Tf\n")
                val header = "${exp.jobTitle} - ${exp.company} (${exp.startDate} - ${if (exp.isCurrent) "Present" else exp.endDate})"
                streamContent.append("(${escapePdf(header)}) Tj\n")
                streamContent.append("/F2 8.5 Tf\n")
                streamContent.append("0 -12 Td\n")
                for (bullet in exp.bullets) {
                    val cleanBullet = bullet.trim().removePrefix("•").trim()
                    streamContent.append("(  * ${escapePdf(cleanBullet)}) Tj\n")
                    streamContent.append("0 -11 Td\n")
                }
                streamContent.append("0 -4 Td\n")
            }
        }

        // Core Skills
        if (resume.skills.isNotEmpty()) {
            streamContent.append("/F1 10 Tf\n")
            streamContent.append("(CORE COMPETENCIES & SKILLS) Tj\n")
            streamContent.append("/F2 9 Tf\n")
            streamContent.append("0 -14 Td\n")
            val skillsText = resume.skills.joinToString(", ") { it.name }
            streamContent.append("(${escapePdf(skillsText)}) Tj\n")
            streamContent.append("0 -16 Td\n")
        }

        // Education
        if (resume.educations.isNotEmpty()) {
            streamContent.append("/F1 10 Tf\n")
            streamContent.append("(EDUCATION) Tj\n")
            streamContent.append("/F2 9 Tf\n")
            streamContent.append("0 -14 Td\n")
            for (edu in resume.educations) {
                val eduText = "${edu.degree} in ${edu.fieldOfStudy} - ${edu.institution} (${edu.graduationYear})"
                streamContent.append("(${escapePdf(eduText)}) Tj\n")
                streamContent.append("0 -12 Td\n")
            }
        }

        streamContent.append("ET\n")

        val streamBytes = streamContent.toString().toByteArray(Charsets.US_ASCII)
        val streamLength = streamBytes.size

        val docBuilder = StringBuilder()
        val offsets = mutableListOf<Int>()

        docBuilder.append("%PDF-1.4\n")

        fun addObj(content: String) {
            offsets.add(docBuilder.length)
            docBuilder.append(content)
        }

        addObj("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")
        addObj("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n")
        addObj("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R /F2 6 0 R >> >> >>\nendobj\n")
        addObj("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>\nendobj\n")
        addObj("6 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n")

        val obj4Header = "4 0 obj\n<< /Length $streamLength >>\nstream\n"
        offsets.add(docBuilder.length)
        docBuilder.append(obj4Header)
        docBuilder.append(streamContent.toString())
        docBuilder.append("endstream\nendobj\n")

        val startXref = docBuilder.length
        docBuilder.append("xref\n")
        docBuilder.append("0 ${offsets.size + 1}\n")
        docBuilder.append("0000000000 65535 f \n")
        for (offset in offsets) {
            docBuilder.append(String.format(Locale.US, "%010d 00000 n \n", offset))
        }
        docBuilder.append("trailer\n<< /Size ${offsets.size + 1} /Root 1 0 R >>\n")
        docBuilder.append("startxref\n")
        docBuilder.append("$startXref\n")
        docBuilder.append("%%EOF\n")

        return docBuilder.toString().toByteArray(Charsets.US_ASCII)
    }

    /**
     * Synthesizes a valid, executive-ready Cover Letter PDF 1.4 document.
     */
    fun createStandardCoverLetterPdfBytes(
        letterText: String,
        jobTitle: String,
        companyName: String,
        candidateName: String,
        candidateEmail: String = "",
        candidatePhone: String = "",
        candidateLocation: String = ""
    ): ByteArray {
        val streamContent = StringBuilder()
        fun escapePdf(text: String): String {
            return text.replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("\r", "")
        }

        streamContent.append("BT\n")

        // Candidate Header
        streamContent.append("/F1 18 Tf\n")
        streamContent.append("48 780 Td\n")
        streamContent.append("(${escapePdf(candidateName.ifBlank { "Professional Candidate" })}) Tj\n")
        streamContent.append("0 -18 Td\n")

        val contacts = listOfNotNull(
            candidateEmail.takeIf { it.isNotBlank() },
            candidatePhone.takeIf { it.isNotBlank() },
            candidateLocation.takeIf { it.isNotBlank() }
        ).joinToString("  |  ")
        if (contacts.isNotBlank()) {
            streamContent.append("/F2 9.5 Tf\n")
            streamContent.append("(${escapePdf(contacts)}) Tj\n")
            streamContent.append("0 -20 Td\n")
        }

        val formattedDate = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date())
        streamContent.append("/F2 9.5 Tf\n")
        streamContent.append("(${escapePdf(formattedDate)}) Tj\n")
        streamContent.append("0 -18 Td\n")

        streamContent.append("/F1 10 Tf\n")
        streamContent.append("(Hiring Manager & Recruitment Team) Tj\n")
        streamContent.append("0 -14 Td\n")
        if (companyName.isNotBlank()) {
            streamContent.append("/F2 9.5 Tf\n")
            streamContent.append("(${escapePdf(companyName)}) Tj\n")
            streamContent.append("0 -16 Td\n")
        }

        val subject = if (jobTitle.isNotBlank()) "Re: Application for $jobTitle Role" else "Re: Application for Position"
        streamContent.append("/F1 10.5 Tf\n")
        streamContent.append("(${escapePdf(subject)}) Tj\n")
        streamContent.append("0 -20 Td\n")

        streamContent.append("/F2 9.5 Tf\n")
        val paragraphs = letterText.split("\n\n")
        for (paragraph in paragraphs) {
            val words = paragraph.split("\\s+".toRegex()).filter { it.isNotBlank() }
            var curLine = StringBuilder()
            for (word in words) {
                if (curLine.length + word.length > 80) {
                    streamContent.append("(${escapePdf(curLine.toString())}) Tj\n")
                    streamContent.append("0 -13 Td\n")
                    curLine = StringBuilder(word)
                } else {
                    if (curLine.isNotEmpty()) curLine.append(" ")
                    curLine.append(word)
                }
            }
            if (curLine.isNotEmpty()) {
                streamContent.append("(${escapePdf(curLine.toString())}) Tj\n")
                streamContent.append("0 -18 Td\n")
            }
        }

        streamContent.append("0 -10 Td\n")
        streamContent.append("(Sincerely,) Tj\n")
        streamContent.append("0 -18 Td\n")
        streamContent.append("/F1 10.5 Tf\n")
        streamContent.append("(${escapePdf(candidateName.ifBlank { "Professional Candidate" })}) Tj\n")

        streamContent.append("ET\n")

        val streamBytes = streamContent.toString().toByteArray(Charsets.US_ASCII)
        val streamLength = streamBytes.size

        val docBuilder = StringBuilder()
        val offsets = mutableListOf<Int>()

        docBuilder.append("%PDF-1.4\n")

        fun addObj(content: String) {
            offsets.add(docBuilder.length)
            docBuilder.append(content)
        }

        addObj("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")
        addObj("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n")
        addObj("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R /F2 6 0 R >> >> >>\nendobj\n")
        addObj("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>\nendobj\n")
        addObj("6 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n")

        val obj4Header = "4 0 obj\n<< /Length $streamLength >>\nstream\n"
        offsets.add(docBuilder.length)
        docBuilder.append(obj4Header)
        docBuilder.append(streamContent.toString())
        docBuilder.append("endstream\nendobj\n")

        val startXref = docBuilder.length
        docBuilder.append("xref\n")
        docBuilder.append("0 ${offsets.size + 1}\n")
        docBuilder.append("0000000000 65535 f \n")
        for (offset in offsets) {
            docBuilder.append(String.format(Locale.US, "%010d 00000 n \n", offset))
        }
        docBuilder.append("trailer\n<< /Size ${offsets.size + 1} /Root 1 0 R >>\n")
        docBuilder.append("startxref\n")
        docBuilder.append("$startXref\n")
        docBuilder.append("%%EOF\n")

        return docBuilder.toString().toByteArray(Charsets.US_ASCII)
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        val paragraphs = text.split("\n")
        for (paragraph in paragraphs) {
            val words = paragraph.split(" ").filter { it.isNotEmpty() }
            if (words.isEmpty()) {
                lines.add("")
                continue
            }
            val currentLine = StringBuilder()
            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (paint.measureText(testLine) <= maxWidth) {
                    currentLine.append(if (currentLine.isEmpty()) word else " $word")
                } else {
                    if (currentLine.isNotEmpty()) {
                        lines.add(currentLine.toString())
                        currentLine.clear()
                    }
                    if (paint.measureText(word) <= maxWidth) {
                        currentLine.append(word)
                    } else {
                        // Word is longer than maxWidth: break characters cleanly
                        var remainder = word
                        while (paint.measureText(remainder) > maxWidth && remainder.isNotEmpty()) {
                            var cut = remainder.length - 1
                            while (cut > 0 && paint.measureText(remainder.substring(0, cut)) > maxWidth) {
                                cut--
                            }
                            if (cut == 0) cut = 1
                            lines.add(remainder.substring(0, cut))
                            remainder = remainder.substring(cut)
                        }
                        if (remainder.isNotEmpty()) {
                            currentLine.append(remainder)
                        }
                    }
                }
            }
            if (currentLine.isNotEmpty()) {
                lines.add(currentLine.toString())
            }
        }
        return lines
    }
}

/**
 * Adapter enabling Android's [PrintManager] to render and print or save the resume as PDF
 * through the native system print workflow.
 */
class ResumePrintDocumentAdapter(
    private val resume: ResumeProfile
) : PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }

        val info = PrintDocumentInfo.Builder("Resume_${resume.fullName}.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
            .build()

        callback.onLayoutFinished(info, newAttributes != oldAttributes)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback.onWriteCancelled()
            return
        }

        val pdfDocument = ResumePdfService.generateFormattedPdfDocument(resume)
        try {
            FileOutputStream(destination.fileDescriptor).use { output ->
                pdfDocument.writeTo(output)
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: IOException) {
            callback.onWriteFailed(e.message)
        } finally {
            pdfDocument.close()
        }
    }
}

/**
 * Adapter enabling Android's [PrintManager] to render and print or save the Cover Letter as PDF.
 */
class CoverLetterPrintDocumentAdapter(
    private val letterText: String,
    private val jobTitle: String,
    private val companyName: String,
    private val candidateName: String,
    private val candidateEmail: String = "",
    private val candidatePhone: String = "",
    private val candidateLocation: String = ""
) : PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }

        val info = PrintDocumentInfo.Builder("CoverLetter_${companyName.ifEmpty { "Job" }}.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
            .build()

        callback.onLayoutFinished(info, newAttributes != oldAttributes)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: PrintDocumentAdapter.WriteResultCallback
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback.onWriteCancelled()
            return
        }

        val pdfDocument = ResumePdfService.generateFormattedCoverLetterPdfDocument(
            letterText = letterText,
            jobTitle = jobTitle,
            companyName = companyName,
            candidateName = candidateName,
            candidateEmail = candidateEmail,
            candidatePhone = candidatePhone,
            candidateLocation = candidateLocation
        )
        try {
            FileOutputStream(destination.fileDescriptor).use { output ->
                pdfDocument.writeTo(output)
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: IOException) {
            callback.onWriteFailed(e.message)
        } finally {
            pdfDocument.close()
        }
    }
}

/**
 * Extension bridging Room [Resume] entity to domain [ResumeProfile]
 * for full compatibility with PDF and Print rendering pipelines.
 */
fun Resume.toResumeProfile(): ResumeProfile {
    val expBullets = getEffectiveWorkExperience()
        .split("\n")
        .map { it.trim().removePrefix("•").removePrefix("-").trim() }
        .filter { it.isNotEmpty() }

    val expList = if (expBullets.isNotEmpty()) {
        listOf(
            ExperienceItem(
                jobTitle = targetRole.ifBlank { "Professional Experience" },
                company = "Career History",
                bullets = expBullets
            )
        )
    } else emptyList()

    val skillItems = skills.split(",", "•", "|", ";")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { SkillItem(name = it, category = "Core Competencies") }

    val eduItems = if (education.isNotBlank()) {
        listOf(
            EducationItem(
                degree = education,
                institution = "Academic Institution"
            )
        )
    } else emptyList()

    return ResumeProfile(
        id = id,
        fullName = getEffectiveName(),
        email = email,
        phone = phone,
        location = location,
        linkedinUrl = linkedinUrl,
        portfolioUrl = portfolioUrl,
        targetRole = targetRole,
        summary = getEffectiveSummary(),
        experiences = expList,
        skills = skillItems,
        educations = eduItems
    )
}
