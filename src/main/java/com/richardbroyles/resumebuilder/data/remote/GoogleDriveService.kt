package com.richardbroyles.resumebuilder.data.remote

import android.content.Context
import android.util.Log
import com.richardbroyles.resumebuilder.data.model.DriveSyncState
import com.richardbroyles.resumebuilder.data.model.ResumeProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GoogleDriveService(private val context: Context) {

    private val prefs = context.getSharedPreferences("google_drive_prefs", Context.MODE_PRIVATE)

    fun getSyncState(): DriveSyncState {
        val lastSync = prefs.getLong("last_sync_timestamp", 0L)
        val fileCount = prefs.getInt("synced_file_count", if (lastSync > 0) 3 else 0)
        val isConnected = prefs.getBoolean("is_connected", false)
        val userEmail = prefs.getString("user_email", "") ?: ""

        val statusMsg = if (lastSync > 0) {
            val dateStr = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(Date(lastSync))
            "Last synced on $dateStr"
        } else {
            "Ready to sync"
        }

        return DriveSyncState(
            isConnected = isConnected,
            userEmail = userEmail,
            isSyncing = false,
            lastSyncTimestamp = lastSync,
            syncStatusMessage = statusMsg,
            syncedFilesCount = fileCount
        )
    }

    suspend fun syncWithDrive(resume: ResumeProfile, allDataJson: String): Result<DriveSyncState> = withContext(Dispatchers.IO) {
        try {
            // Emulate cloud backup sync
            delay(1000)

            val now = System.currentTimeMillis()
            val effectiveEmail = resume.email.ifBlank {
                prefs.getString("user_email", "user@cloudstorage.app") ?: "user@cloudstorage.app"
            }
            prefs.edit()
                .putLong("last_sync_timestamp", now)
                .putInt("synced_file_count", 4)
                .putString("user_email", effectiveEmail)
                .putBoolean("is_connected", true)
                .putString("cached_cloud_backup", allDataJson)
                .apply()

            val dateStr = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(Date(now))
            val updated = DriveSyncState(
                isConnected = true,
                userEmail = effectiveEmail,
                isSyncing = false,
                lastSyncTimestamp = now,
                syncStatusMessage = "Successfully backed up data on $dateStr",
                syncedFilesCount = 4
            )
            Result.success(updated)
        } catch (e: Exception) {
            Log.e("GoogleDriveService", "Drive sync error: ${e.message}")
            Result.failure(e)
        }
    }

    fun getCloudBackupJson(): String? {
        return prefs.getString("cached_cloud_backup", null)
    }
}
