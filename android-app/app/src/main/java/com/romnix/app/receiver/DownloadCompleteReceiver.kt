package com.romnix.app.receiver

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.romnix.app.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class DownloadCompleteReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
        
        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        if (downloadId == -1L) return
        
        // Process in background
        CoroutineScope(Dispatchers.IO).launch {
            handleDownloadComplete(context, downloadId)
        }
    }
    
    private suspend fun handleDownloadComplete(context: Context, downloadId: Long) {
        // Check if this is a transfer download (should NOT be moved to custom location)
        if (com.romnix.app.AndroidDownloader.transferDownloads.contains(downloadId)) {
            Log.d("DownloadCompleteReceiver", "Transfer download detected, keeping in default location")
            com.romnix.app.AndroidDownloader.transferDownloads.remove(downloadId)
            return
        }
        
        val preferencesManager = PreferencesManager(context)
        val customLocationUri = preferencesManager.downloadLocationFlow.first()
        val useCustomLocation = preferencesManager.useCustomLocationFlow.first()
        
        // Only process if custom location is enabled
        if (!useCustomLocation || customLocationUri == null) {
            Log.d("DownloadCompleteReceiver", "Using default location, no move needed")
            return
        }
        
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)
        
        if (cursor.moveToFirst()) {
            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val status = cursor.getInt(statusIndex)
            
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                val localUriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                val titleIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)
                
                val localUri = cursor.getString(localUriIndex)
                val fileName = cursor.getString(titleIndex)
                
                cursor.close()
                
                // Extract platform from the file path
                val sourceFile = File(Uri.parse(localUri).path ?: return)
                val platformId = extractFoldernameFromPath(sourceFile.absolutePath)
                
                if (sourceFile.exists()) {
                    moveToCustomLocation(context, sourceFile, fileName, platformId, customLocationUri)
                }
            } else {
                cursor.close()
                Log.e("DownloadCompleteReceiver", "Download failed with status: $status")
            }
        }
    }
    
    private fun extractFoldernameFromPath(path: String): String {
        // Path format: .../Download/roms/platform/filename
        val parts = path.split("/")
        val romsIndex = parts.indexOf("roms")
        return if (romsIndex != -1 && romsIndex + 1 < parts.size) {
            parts[romsIndex + 1]
        } else {
            "downloads" // Default folder name if platform can't be extracted
        }
    }
    
    private fun moveToCustomLocation(
        context: Context,
        sourceFile: File,
        fileName: String,
        platformId: String,
        customLocationUri: String
    ) {
        try {
            val uri = Uri.parse(customLocationUri)
            val documentTree = DocumentFile.fromTreeUri(context, uri) ?: return
            
            // Create platform folder if it doesn't exist
            val platformFolder = documentTree.findFile(platformId)
                ?: documentTree.createDirectory(platformId)
            
            if (platformFolder == null) {
                Log.e("DownloadCompleteReceiver", "Failed to create platform folder: $platformId")
                return
            }
            
            // Delete existing file if it exists
            platformFolder.findFile(fileName)?.delete()
            
            // Create new file
            val mimeType = getMimeType(fileName)
            val newFile = platformFolder.createFile(mimeType, fileName)
            
            if (newFile == null) {
                Log.e("DownloadCompleteReceiver", "Failed to create file: $fileName")
                return
            }
            
            // Copy file content
            context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(output)
                }
            }
            
            // Delete the original file after successful copy
            sourceFile.delete()
            
            Log.d("DownloadCompleteReceiver", "Successfully moved $fileName to custom location")
            
        } catch (e: Exception) {
            Log.e("DownloadCompleteReceiver", "Error moving file to custom location", e)
        }
    }
    
    private fun getMimeType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "zip" -> "application/zip"
            "7z" -> "application/x-7z-compressed"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "mp4" -> "video/mp4"
            "mov" -> "video/quicktime"
            "chd" -> "application/octet-stream"
            "rar" -> "application/x-rar-compressed"
            "muxzip" -> "application/zip"
            "muxthm" -> "application/octet-stream"
            "pdf" -> "application/pdf"
            else -> "application/octet-stream"
        }
    }
}
