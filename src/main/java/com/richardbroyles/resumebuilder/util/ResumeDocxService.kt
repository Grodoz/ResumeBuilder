package com.richardbroyles.resumebuilder.util

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.richardbroyles.resumebuilder.data.local.entity.Resume
import com.richardbroyles.resumebuilder.data.model.ResumeProfile
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ResumeDocxService {
    private const val TAG = "ResumeDocxService"
    const val DOCX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

    data class DocxExportResult(
        val success: Boolean,
        val filePathOrUri: String,
        val fileName: String,
        val uri: Uri?,
        val errorMessage: String? = null
    )

    fun createDocxBytes(resume: ResumeProfile): ByteArray {
        val byteStream = ByteArrayOutputStream()
        ZipOutputStream(byteStream).use { zip ->
            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            zip.write(getContentTypesXml().toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("_rels/.rels"))
            zip.write(getGlobalRelsXml().toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("word/_rels/document.xml.rels"))
            zip.write(getDocumentRelsXml().toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("word/styles.xml"))
            zip.write(getStylesXml().toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("word/document.xml"))
            val documentXml = generateDocumentXml(resume)
            zip.write(documentXml.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()
        }
        return byteStream.toByteArray()
    }

    fun saveResumeDocxToDocuments(context: Context, resume: ResumeProfile): DocxExportResult {
        return try {
            val sanitizedName = resume.fullName.replace("[^a-zA-Z0-9]".toRegex(), "_").ifEmpty { "Resume" }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "${sanitizedName}_OptimizedResume_$timeStamp.docx"
            val docxBytes = createDocxBytes(resume)
            var outputUri: Uri? = null
            var savedPath = ""
            var writeSuccess = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, DOCX_MIME_TYPE)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/ResumeBuilder")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                    outputUri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                    if (outputUri != null) {
                        val stream = resolver.openOutputStream(outputUri)
                        if (stream != null) {
                            stream.use { it.write(docxBytes) }
                            writeSuccess = true
                            contentValues.clear()
                            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                            resolver.update(outputUri, contentValues, null, null)
                            savedPath = "Documents/ResumeBuilder/$fileName"
                        } else {
                            outputUri = null
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "MediaStore docx insert failed, falling back to app files: ${e.message}")
                    outputUri = null
                }
            }
            if (!writeSuccess) {
                val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: File(context.filesDir, "Documents").apply { mkdirs() }
                val targetDir = File(docsDir, "ResumeBuilder")
                if (!targetDir.exists()) targetDir.mkdirs()
                val file = File(targetDir, fileName)
                FileOutputStream(file).use { stream ->
                    stream.write(docxBytes)
                    writeSuccess = true
                }
                savedPath = file.absolutePath
                outputUri = try{
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                } catch (e: Exception) {
                    Uri.fromFile(file)
                }
            }
            if (writeSuccess && outputUri != null) {
                DocxExportResult(
                    success = true,
                    filePathOrUri = savedPath,
                    fileName = fileName,
                    uri = outputUri
                )
            } else {
                DocxExportResult(
                    success = false,
                    filePathOrUri = "",
                    fileName = "",
                    uri = null,
                    errorMessage = "Failed to write Word document to storage."
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving Word (.docx) resume to Documents", e)
            DocxExportResult(
                success = false,
                filePathOrUri = "",
                fileName = "",
                uri = null,
                errorMessage = e.message
            )
        }
    }

    fun saveResumeDocxToDocuments(context: Context, resume: Resume): DocxExportResult {
        return saveResumeDocxToDocuments(context, resume.toResumeProfile())
    }

    fun exportResumeDocxToLocalFile(resume: ResumeProfile, destinationFile: File): DocxExportResult {
        return try {
            destinationFile.parentFile?.mkdirs()
            val docxBytes = createDocxBytes(resume)
            FileOutputStream(destinationFile).use { it.write(docxBytes) }
            DocxExportResult(
                success = true,
                filePathOrUri = destinationFile.absolutePath,
                fileName = destinationFile.name,
                uri = Uri.fromFile(destinationFile)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write DOCX to local file.", e)
            DocxExportResult(
                success = false,
                filePathOrUri = destinationFile.absolutePath,
                fileName = destinationFile.name,
                uri = null,
                errorMessage = e.localizedMessage ?: "Failed to write DOCX to local file."
            )
        }
    }

    fun viewDocxIntent(uri: Uri): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, DOCX_MIME_TYPE)
            clipData = ClipData.newRawUri("Resume Word Document", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun shareDocxIntent(uri: Uri, fileName: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = DOCX_MIME_TYPE
            clipData = ClipData.newRawUri(fileName, uri)
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun getContentTypesXml(): String {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
              <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
              <Default Extension="xml" ContentType="application/xml"/>
              <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
              <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
            </Types>
        """.trimIndent()
    }

    private fun getGlobalRelsXml(): String {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
            </Relationships>
        """.trimIndent()
    }

    private fun getDocumentRelsXml(): String {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
            </Relationships>
        """.trimIndent()
    }

    private fun getStylesXml(): String {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
              <w:docDefaults>
                <w:rPrDefault>
                  <w:rPr>
                    <w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:cs="Calibri"/>
                    <w:sz w:val="22"/>
                    <w:color w:val="1E293B"/>
                  </w:rPr>
                </w:rPrDefault>
              </w:docDefaults>
              <w:style w:type="paragraph" w:default="1" w:styleId="Normal">
                <w:name w:val="Normal"/>
                <w:pPr>
                  <w:spacing w:after="120" w:line="260" w:lineRule="auto"/>
                </w:pPr>
              </w:style>
              <w:style w:type="paragraph" w:styleId="Heading1">
                <w:name w:val="heading 1"/>
                <w:pPr>
                  <w:spacing w:before="240" w:after="80"/>
                  <w:pBdr>
                    <w:bottom w:val="single" w:sz="12" w:space="4" w:color="1E40AF"/>
                  </w:pBdr>
                </w:pPr>
                <w:rPr>
                  <w:rFonts w:ascii="Calibri" w:hAnsi="Calibri"/>
                  <w:b/>
                  <w:sz w:val="26"/>
                  <w:color w:val="1E40AF"/>
                </w:rPr>
              </w:style>
            </w:styles>
        """.trimIndent()
    }

    private fun generateDocumentXml(resume: ResumeProfile): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
""")
        val name = resume.fullName.ifBlank { "Professional Candidate" }
        sb.append(
            """
    <w:p>
      <w:pPr>
        <w:jc w:val="center"/>
        <w:spacing w:before="0" w:after="60"/>
      </w:pPr>
      <w:r>
        <w:rPr>
          <w:b/>
          <w:sz w:val="48"/>
          <w:color w:val="0F172A"/>
        </w:rPr>
        <w:t>${escapeXml(name)}</w:t>
      </w:r>
    </w:p>
"""
        )
        if (resume.targetRole.isNotBlank()) {
            sb.append(
                """
    <w:p>
      <w:pPr>
        <w:jc w:val="center"/>
        <w:spacing w:before="0" w:after="60"/>
      </w:pPr>
      <w:r>
        <w:rPr>
          <w:b/>
          <w:sz w:val="24"/>
          <w:color w:val="1E40AF"/>
        </w:rPr>
        <w:t>${escapeXml(resume.targetRole)}</w:t>
      </w:r>
    </w:p>
"""
            )
        }
        val contactItems = listOfNotNull(
            resume.email.takeIf { it.isNotBlank() },
            resume.phone.takeIf { it.isNotBlank() },
            resume.location.takeIf { it.isNotBlank() },
            resume.linkedinUrl.takeIf { it.isNotBlank() },
            resume.portfolioUrl.takeIf { it.isNotBlank() }
        )
        if (contactItems.isNotEmpty()) {
            val contactLine = contactItems.joinToString("  |  ")
            sb.append(
                """
    <w:p>
      <w:pPr>
        <w:jc w:val="center"/>
        <w:spacing w:before="0" w:after="200"/>
      </w:pPr>
      <w:r>
        <w:rPr>
          <w:sz w:val="20"/>
          <w:color w:val="475569"/>
        </w:rPr>
        <w:t>${escapeXml(contactLine)}</w:t>
      </w:r>
    </w:p>
"""
            )
        }
        if (resume.summary.isNotBlank()) {
            sb.append(generateSectionHeader("PROFESSIONAL SUMMARY"))
            sb.append(
                """
    <w:p>
      <w:pPr>
        <w:spacing w:before="60" w:after="160" w:line="276" w:lineRule="auto"/>
      </w:pPr>
      <w:r>
        <w:rPr>
          <w:sz w:val="22"/>
          <w:color w:val="334155"/>
        </w:rPr>
        <w:t>${escapeXml(resume.summary)}</w:t>
      </w:r>
    </w:p>
"""
            )
        }
        if (resume.skills.isNotEmpty()) {
            sb.append(generateSectionHeader("CORE COMPETENCIES & TECHNICAL SKILLS"))
            val groupedSkills = resume.skills.groupBy { it.category.ifBlank { "Technical" } }
            for ((category, skillsList) in groupedSkills) {
                val skillNames = skillsList.joinToString(", ") { it.name }
                sb.append("""
    <w:p>
      <w:pPr>
        <w:spacing w:before="40" w:after="60"/>
      </w:pPr>
      <w:r>
        <w:rPr>
          <w:b/>
          <w:sz w:val="21"/>
          <w:color w:val="0F172A"/>
        </w:rPr>
        <w:t>${escapeXml(category)}: </w:t>
      </w:r>
      <w:r>
        <w:rPr>
          <w:sz w:val="21"/>
          <w:color w:val="334155"/>
        </w:rPr>
        <w:t>${escapeXml(skillNames)}</w:t>
      </w:r>
    </w:p>
""")
            }
        }

        // 6. Professional Experience Section
        if (resume.experiences.isNotEmpty()) {
            sb.append(generateSectionHeader("PROFESSIONAL EXPERIENCE"))
            for (exp in resume.experiences) {
                // Job Title & Dates
                val dateRange = when {
                    exp.isCurrent -> "${exp.startDate.ifBlank { "Start" }} - Present"
                    exp.startDate.isNotBlank() || exp.endDate.isNotBlank() -> "${exp.startDate} - ${exp.endDate}"
                    else -> ""
                }

                sb.append("""
    <w:p>
      <w:pPr>
        <w:spacing w:before="140" w:after="20"/>
      </w:pPr>
      <w:r>
        <w:rPr>
          <w:b/>
          <w:sz w:val="23"/>
          <w:color w:val="0F172A"/>
        </w:rPr>
        <w:t>${escapeXml(exp.jobTitle.ifBlank { "Role" })}</w:t>
      </w:r>
    </w:p>
""")

                // Company & Location & Date
                val compLoc = buildString {
                    append(exp.company.ifBlank { "Company" })
                    if (exp.location.isNotBlank()) append(" • ").append(exp.location)
                    if (dateRange.isNotBlank()) append("   [ ").append(dateRange).append(" ]")
                }
                sb.append("""
    <w:p>
      <w:pPr>
        <w:spacing w:before="0" w:after="60"/>
      </w:pPr>
      <w:r>
        <w:rPr>
          <w:i/>
          <w:b/>
          <w:sz w:val="20"/>
          <w:color w:val="1E40AF"/>
        </w:rPr>
        <w:t>${escapeXml(compLoc)}</w:t>
      </w:r>
    </w:p>
""")

                // Bullets
                for (bullet in exp.bullets) {
                    if (bullet.isNotBlank()) {
                        sb.append("""
    <w:p>
      <w:pPr>
        <w:ind w:left="360" w:hanging="200"/>
        <w:spacing w:before="20" w:after="40" w:line="260" w:lineRule="auto"/>
      </w:pPr>
      <w:r>
        <w:rPr>
          <w:sz w:val="21"/>
          <w:color w:val="1E40AF"/>
        </w:rPr>
        <w:t>• </w:t>
      </w:r>
      <w:r>
        <w:rPr>
          <w:sz w:val="21"/>
          <w:color w:val="334155"/>
        </w:rPr>
        <w:t>${escapeXml(bullet)}</w:t>
      </w:r>
    </w:p>
""")
                    }
                }
            }
        }

        // 7. Education Section
        if (resume.educations.isNotEmpty()) {
            sb.append(generateSectionHeader("EDUCATION"))
            for (edu in resume.educations) {
                val degreeInfo = buildString {
                    append(edu.degree.ifBlank { "Degree" })
                    if (edu.fieldOfStudy.isNotBlank()) append(" in ").append(edu.fieldOfStudy)
                }
                val institutionInfo = buildString {
                    append(edu.institution.ifBlank { "University" })
                    if (edu.graduationYear.isNotBlank()) append(" • ").append(edu.graduationYear)
                    if (edu.gpaOrHonors.isNotBlank()) append(" • ").append(edu.gpaOrHonors)
                }

                sb.append("""
    <w:p>
      <w:pPr>
        <w:spacing w:before="100" w:after="20"/>
      </w:pPr>
      <w:r>
        <w:rPr>
          <w:b/>
          <w:sz w:val="22"/>
          <w:color w:val="0F172A"/>
        </w:rPr>
        <w:t>${escapeXml(degreeInfo)}</w:t>
      </w:r>
    </w:p>
    <w:p>
      <w:pPr>
        <w:spacing w:before="0" w:after="60"/>
      </w:pPr>
      <w:r>
        <w:rPr>
          <w:sz w:val="20"/>
          <w:color w:val="475569"/>
        </w:rPr>
        <w:t>${escapeXml(institutionInfo)}</w:t>
      </w:r>
    </w:p>
""")
            }
        }

        // 8. Projects (if present)
        if (resume.projects.isNotEmpty()) {
            sb.append(generateSectionHeader("KEY PROJECTS"))
            for (proj in resume.projects) {
                sb.append("""
    <w:p>
      <w:pPr>
        <w:spacing w:before="100" w:after="20"/>
      </w:pPr>
      <w:r>
        <w:rPr>
          <w:b/>
          <w:sz w:val="22"/>
          <w:color w:val="0F172A"/>
        </w:rPr>
        <w:t>${escapeXml(proj.title)}</w:t>
      </w:r>
      ${if (proj.role.isNotBlank()) "<w:r><w:rPr><w:i/><w:sz w:val=\"20\"/><w:color w:val=\"1E40AF\"/></w:rPr><w:t> - ${escapeXml(proj.role)}</w:t></w:r>" else ""}
    </w:p>
""")
                if (proj.description.isNotBlank()) {
                    sb.append("""
    <w:p>
      <w:pPr>
        <w:spacing w:before="0" w:after="40"/>
      </w:pPr>
      <w:r>
        <w:rPr>
          <w:sz w:val="21"/>
          <w:color w:val="334155"/>
        </w:rPr>
        <w:t>${escapeXml(proj.description)}</w:t>
      </w:r>
    </w:p>
""")
                }
            }
        }

        // 9. Certifications (if present)
        if (resume.certifications.isNotEmpty()) {
            sb.append(generateSectionHeader("CERTIFICATIONS"))
            for (cert in resume.certifications) {
                sb.append("""
    <w:p>
      <w:pPr>
        <w:ind w:left="360" w:hanging="200"/>
        <w:spacing w:before="20" w:after="40"/>
      </w:pPr>
      <w:r>
        <w:rPr>
          <w:b/>
          <w:sz w:val="21"/>
          <w:color w:val="1E40AF"/>
        </w:rPr>
        <w:t>• </w:t>
      </w:r>
      <w:r>
        <w:rPr>
          <w:sz w:val="21"/>
          <w:color w:val="334155"/>
        </w:rPr>
        <w:t>${escapeXml(cert)}</w:t>
      </w:r>
    </w:p>
""")
            }
        }

        // Page Margins (Standard 0.75 in / 1080 twips)
        sb.append("""
    <w:sectPr>
      <w:pgSz w:w="12240" w:h="15840"/>
      <w:pgMar w:top="1080" w:right="1080" w:bottom="1080" w:left="1080"/>
    </w:sectPr>
  </w:body>
</w:document>
""")

        return sb.toString()
    }

    private fun generateSectionHeader(title: String): String {
        return """
    <w:p>
      <w:pPr>
        <w:pStyle w:val="Heading1"/>
        <w:spacing w:before="220" w:after="80"/>
        <w:pBdr>
          <w:bottom w:val="single" w:sz="12" w:space="4" w:color="1E40AF"/>
        </w:pBdr>
      </w:pPr>
      <w:r>
        <w:rPr>
          <w:b/>
          <w:sz w:val="26"/>
          <w:color w:val="1E40AF"/>
        </w:rPr>
        <w:t>${escapeXml(title)}</w:t>
      </w:r>
    </w:p>
"""

    }
}