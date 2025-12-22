package com.romnix.app

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import kotlin.system.exitProcess
import com.romnix.app.ui.theme.ButtonStyles
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import com.romnix.app.ui.screens.DevicesScreen
import com.romnix.app.ui.screens.BrowseScreen
import com.romnix.app.ui.screens.ConsoleSelectScreen
import com.romnix.app.ui.screens.SettingsScreen
import com.romnix.app.ui.screens.FileBrowserScreen
import com.romnix.app.ui.screens.BacklogScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.romnix.app.network.HostConfig
import com.romnix.app.ui.theme.AppTheme
import com.romnix.app.ui.theme.NavyBlue
import com.romnix.app.network.HostScanner
import com.romnix.app.network.SshUploader
import com.romnix.app.network.SshTester
import com.romnix.app.network.SshFileManager
import com.romnix.app.network.RemoteFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URLDecoder
import java.util.Locale

class MainViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels { 
        MainViewModelFactory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // System bars will be handled dynamically in setContent based on theme
        
        setContent {
            AppTheme {
                // Configure system bars based on theme
                val colorScheme = MaterialTheme.colorScheme
                LaunchedEffect(colorScheme) {
                    window.statusBarColor = colorScheme.surfaceContainer.toArgb()
                    window.navigationBarColor = colorScheme.surfaceContainer.toArgb()
                    
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        window.decorView.post {
                            val isLightTheme = colorScheme.surfaceContainer.luminance() > 0.5
                            window.insetsController?.setSystemBarsAppearance(
                                if (isLightTheme) {
                                    android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                                            android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                                } else 0,
                                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                                        android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                            )
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        run {
                            val isLightTheme = colorScheme.background.luminance() > 0.5
                            var flags = if (isLightTheme) android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR else 0
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && isLightTheme) {
                                flags = flags or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                            }
                            window.decorView.post { window.decorView.systemUiVisibility = flags }
                        }
                    }
                }
                
                var showDisclaimer by remember { mutableStateOf(isFirstLaunch()) }
                
                if (showDisclaimer) {
                    DisclaimerDialog(
                        onAccept = {
                            showDisclaimer = false
                            setFirstLaunchCompleted()
                        },
                        onCancel = {
                            finish()
                            exitProcess(0)
                        }
                    )
                } else {
                    RomApp(viewModel = viewModel, downloader = AndroidDownloader(this))
                }
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Save backlog state when app goes to background
        viewModel.saveBacklogState()
    }
    
    override fun onStop() {
        super.onStop()
        // Additional save when app is stopped
        viewModel.saveBacklogState()
    }
    
    private fun isFirstLaunch(): Boolean {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return !prefs.getBoolean("disclaimer_accepted", false)
    }
    
    private fun setFirstLaunchCompleted() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("disclaimer_accepted", true).apply()
    }
}

@Composable
fun DisclaimerDialog(
    onAccept: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = { }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Before you continue",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827),
                    fontSize = 18.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Only download content you own or have permission to access.\n\nMisuse of this app may breach copyright laws and terms of service.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280),
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = ButtonStyles.ctaButtonShape,
                        contentPadding = ButtonStyles.ctaButtonPadding
                    ) {
                        Text(
                            "I UNDERSTAND",
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "CANCEL",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

data class Foldername(
    val id: String, 
    val label: String, 
    val archiveUrl: String, 
    val extensions: List<String>,
    val isCustom: Boolean = false
)

object FoldernameManager {
    private val defaultFoldernames = emptyList<Foldername>()
    
    private var customFoldernames = mutableListOf<Foldername>()
    
    val all: List<Foldername>
        get() = defaultFoldernames + customFoldernames
    
    fun loadCustomFoldernames(context: Context) {
        val sp = context.getSharedPreferences("custom_platforms", Context.MODE_PRIVATE)
        val platformsJson = sp.getString("platforms", "[]")
        try {
            customFoldernames.clear()
            // Simple JSON parsing for custom platforms
            if (platformsJson != "[]") {
                val platformsData = parseCustomFoldernamesJson(platformsJson!!)
                customFoldernames.addAll(platformsData)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun saveCustomFoldernames(context: Context) {
        val sp = context.getSharedPreferences("custom_platforms", Context.MODE_PRIVATE)
        val json = customFoldernamesToJson(customFoldernames)
        sp.edit().putString("platforms", json).apply()
    }
    
    fun addCustomFoldername(context: Context, platform: Foldername) {
        val customFoldername = platform.copy(isCustom = true)
        customFoldernames.removeAll { it.id == customFoldername.id }
        customFoldernames.add(customFoldername)
        saveCustomFoldernames(context)
    }
    
    fun removeCustomFoldername(context: Context, platformId: String) {
        customFoldernames.removeAll { it.id == platformId }
        saveCustomFoldernames(context)
    }
    
    fun updateCustomFoldername(context: Context, platform: Foldername) {
        val index = customFoldernames.indexOfFirst { it.id == platform.id }
        if (index != -1) {
            customFoldernames[index] = platform.copy(isCustom = true)
            saveCustomFoldernames(context)
        }
    }
    
    fun exportFoldernames(): String {
        val exportData = StringBuilder()
        exportData.appendLine("# Romnix backup file")
        exportData.appendLine("# Created: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
        exportData.appendLine()
        
        all.forEach { platform ->
            exportData.appendLine("[Foldername]")
            exportData.appendLine("Name=${platform.label}")
            exportData.appendLine("URL=${platform.archiveUrl}")
            exportData.appendLine("Extensions=${platform.extensions.joinToString(",")}")
            exportData.appendLine("Custom=${platform.isCustom}")
            exportData.appendLine()
        }
        
        return exportData.toString()
    }
    
    fun clearAllCustomFoldernames(context: Context) {
        customFoldernames.clear()
        saveCustomFoldernames(context)
    }
    
    fun importFoldernames(context: Context, fileContent: String): Int {
        try {
            val lines = fileContent.lines()
            var currentFoldername: MutableMap<String, String>? = null
            var importedCount = 0
            
            lines.forEach { line ->
                val trimmedLine = line.trim()
                when {
                    trimmedLine.startsWith("[Foldername]") -> {
                        currentFoldername = mutableMapOf()
                    }
                    trimmedLine.startsWith("Name=") -> {
                        currentFoldername?.put("name", trimmedLine.substringAfter("="))
                    }
                    trimmedLine.startsWith("URL=") -> {
                        currentFoldername?.put("url", trimmedLine.substringAfter("="))
                    }
                    trimmedLine.startsWith("Extensions=") -> {
                        currentFoldername?.put("extensions", trimmedLine.substringAfter("="))
                    }
                    trimmedLine.isEmpty() && currentFoldername != null -> {
                        // End of platform section, create platform
                        val name = currentFoldername!!["name"]
                        val url = currentFoldername!!["url"]
                        val extensionsStr = currentFoldername!!["extensions"]
                        
                        if (!name.isNullOrBlank() && !url.isNullOrBlank()) {
                            val extensions = extensionsStr?.split(",")?.map { it.trim() } ?: emptyList()
                            val platform = Foldername(
                                id = name.lowercase().replace(" ", ""),
                                label = name,
                                archiveUrl = url,
                                extensions = extensions,
                                isCustom = true
                            )
                            addCustomFoldername(context, platform)
                            importedCount++
                        }
                        currentFoldername = null
                    }
                }
            }
            
            // Handle last platform if file doesn't end with empty line
            if (currentFoldername != null) {
                val name = currentFoldername!!["name"]
                val url = currentFoldername!!["url"]
                val extensionsStr = currentFoldername!!["extensions"]
                
                if (!name.isNullOrBlank() && !url.isNullOrBlank()) {
                    val extensions = extensionsStr?.split(",")?.map { it.trim() } ?: emptyList()
                    val platform = Foldername(
                        id = name.lowercase().replace(" ", ""),
                        label = name,
                        archiveUrl = url,
                        extensions = extensions,
                        isCustom = true
                    )
                    addCustomFoldername(context, platform)
                    importedCount++
                }
            }
            
            return importedCount
        } catch (e: Exception) {
            return -1 // Error indicator
        }
    }
    
    private fun parseCustomFoldernamesJson(json: String): List<Foldername> {
        val platforms = mutableListOf<Foldername>()
        try {
            // Simple manual JSON parsing
            val cleanJson = json.trim().removePrefix("[").removeSuffix("]")
            if (cleanJson.isNotEmpty()) {
                val platformObjects = cleanJson.split("},{").map { 
                    if (!it.startsWith("{")) "{$it" else it
                }.map { 
                    if (!it.endsWith("}")) "$it}" else it
                }
                
                for (obj in platformObjects) {
                    val platform = parseFoldernameObject(obj)
                    if (platform != null) platforms.add(platform)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return platforms
    }
    
    private fun parseFoldernameObject(obj: String): Foldername? {
        try {
            val fields = mutableMapOf<String, String>()
            val content = obj.trim().removePrefix("{").removeSuffix("}")
            val parts = content.split("\",\"")
            
            for (part in parts) {
                val keyValue = part.split("\":\"", limit = 2)
                if (keyValue.size == 2) {
                    val key = keyValue[0].trim().removePrefix("\"").removeSuffix("\"")
                    val value = keyValue[1].trim().removePrefix("\"").removeSuffix("\"")
                    fields[key] = value
                }
            }
            
            val id = fields["id"] ?: return null
            val label = fields["label"] ?: return null
            val archiveUrl = fields["archiveUrl"] ?: return null
            val extensionsStr = fields["extensions"] ?: ""
            val extensions = extensionsStr.split(",").map { it.trim() }
            
            return Foldername(id, label, archiveUrl, extensions, true)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    private fun customFoldernamesToJson(platforms: List<Foldername>): String {
        if (platforms.isEmpty()) return "[]"
        
        val platformStrings = platforms.map { platform ->
            "{\"id\":\"${platform.id}\",\"label\":\"${platform.label}\",\"archiveUrl\":\"${platform.archiveUrl}\",\"extensions\":\"${platform.extensions.joinToString(",")}\"}"
        }
        return "[${platformStrings.joinToString(",")}]"
    }
}

data class RomItem(val displayName: String, val downloadUrl: String, val platform: Foldername)

data class DiscoveredHost(val ip: String)

interface Downloader { fun download(context: Context, item: RomItem) }

data class HostTemplate(
    val name: String,
    val username: String,
    val password: String,
    val port: String,
    val remoteBasePath: String,
    val hostIp: String,
    val useGuest: Boolean,
    val isDhcpNetwork: Boolean
)

class AndroidDownloader(private val context: Context) : Downloader {
    private val preferencesManager = com.romnix.app.data.PreferencesManager(context)
    
    companion object {
        private val activeDownloads = mutableMapOf<String, Long>() // filename -> downloadId
        val transferDownloads = mutableSetOf<Long>() // download IDs that are for transfers (don't move to custom location)
        
        // Convert GitHub URLs from blob format to raw format
        fun convertToRawUrl(url: String): String {
            return when {
                // Convert github.com/user/repo/blob/branch/file to raw.githubusercontent.com/user/repo/branch/file
                url.contains("github.com") && url.contains("/blob/") -> {
                    url.replace("github.com", "raw.githubusercontent.com")
                        .replace("/blob/", "/")
                }
                else -> url
            }
        }
    }
    
    override fun download(context: Context, item: RomItem) {
        // Always download to default location first
        // The DownloadCompleteReceiver will move it to custom location if needed
        
        // Convert GitHub URLs to raw format to download actual file instead of HTML page
        val downloadUrl = convertToRawUrl(item.downloadUrl)
        android.util.Log.d("AndroidDownloader", "Original URL: ${item.downloadUrl}")
        android.util.Log.d("AndroidDownloader", "Converted URL: $downloadUrl")
        
        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle(item.displayName)
            .setDescription("Downloading to ${item.platform.label}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .addRequestHeader("Accept-Encoding", "identity")  // Prevent automatic decompression
            .addRequestHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                "roms/${item.platform.id}/${item.displayName}"
            )
        
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = dm.enqueue(request)
        
        // Store the download ID for tracking
        activeDownloads[item.displayName] = downloadId
        
        android.util.Log.d("AndroidDownloader", "Started download with ID: $downloadId for ${item.displayName} in platform ${item.platform.id}")
        
        // The DownloadCompleteReceiver will handle moving the file to custom location if enabled
    }
    
    fun downloadForTransfer(context: Context, item: RomItem) {
        // Special download method for transfers - marks the download to NOT be moved to custom location
        
        // Convert GitHub URLs to raw format to download actual file instead of HTML page
        val downloadUrl = convertToRawUrl(item.downloadUrl)
        android.util.Log.d("AndroidDownloader", "Transfer - Original URL: ${item.downloadUrl}")
        android.util.Log.d("AndroidDownloader", "Transfer - Converted URL: $downloadUrl")
        
        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle(item.displayName)
            .setDescription("Downloading for transfer to ${item.platform.label}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .addRequestHeader("Accept-Encoding", "identity")  // Prevent automatic decompression
            .addRequestHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                "roms/${item.platform.id}/${item.displayName}"
            )
        
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = dm.enqueue(request)
        
        // Store the download ID for tracking AND mark it as a transfer download
        activeDownloads[item.displayName] = downloadId
        transferDownloads.add(downloadId) // Mark this as a transfer download
        
        android.util.Log.d("AndroidDownloader", "Started transfer download with ID: $downloadId for ${item.displayName}")
    }
    
    fun isDownloadComplete(context: Context, fileName: String): Boolean {
        val downloadId = activeDownloads[fileName]
        if (downloadId == null) {
            // No active download found - might have already completed
            // Check if the file exists as a fallback
            return false
        }
        
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = dm.query(query)
        
        return try {
            if (cursor.moveToFirst()) {
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val status = cursor.getInt(statusIndex)
                
                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        // Get the actual file path from DownloadManager
                        val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                        val localUri = cursor.getString(uriIndex)
                        android.util.Log.d("AndroidDownloader", "Download $fileName completed successfully")
                        android.util.Log.d("AndroidDownloader", "File saved at: $localUri")
                        activeDownloads.remove(fileName)
                        // Note: transferDownloads cleanup is handled by DownloadCompleteReceiver
                        true
                    }
                    DownloadManager.STATUS_FAILED -> {
                        val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                        val reason = cursor.getInt(reasonIndex)
                        android.util.Log.e("AndroidDownloader", "Download $fileName failed with reason: $reason")
                        activeDownloads.remove(fileName)
                        false
                    }
                    DownloadManager.STATUS_RUNNING -> {
                        val bytesIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        val totalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                        val bytesDownloaded = cursor.getLong(bytesIndex)
                        val totalBytes = cursor.getLong(totalIndex)
                        android.util.Log.d("AndroidDownloader", "Download $fileName in progress: $bytesDownloaded/$totalBytes bytes")
                        false
                    }
                    DownloadManager.STATUS_PAUSED -> {
                        android.util.Log.d("AndroidDownloader", "Download $fileName is paused")
                        false
                    }
                    DownloadManager.STATUS_PENDING -> {
                        android.util.Log.d("AndroidDownloader", "Download $fileName is pending")
                        false
                    }
                    else -> {
                        android.util.Log.d("AndroidDownloader", "Download $fileName has unknown status: $status")
                        false
                    }
                }
            } else {
                // Download ID not found in DownloadManager - might have been cleared
                android.util.Log.w("AndroidDownloader", "Download ID $downloadId not found for $fileName")
                activeDownloads.remove(fileName)
                false
            }
        } finally {
            cursor.close()
        }
    }
}

class RomRepository(private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
    .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
    .followRedirects(true)
    .followSslRedirects(true)
    .addInterceptor { chain ->
        val request = chain.request()
        val response = chain.proceed(request)
        android.util.Log.d("RomSearch", "Interceptor - Response headers: ${response.headers}")
        response
    }
    .build()) {
    // Match <a ... href="..." ...>
    private val linkRegex = Regex("<a\\s+[^>]*href=\\\"([^\\\"]+)\\\"", RegexOption.IGNORE_CASE)
    
    // Detect if URL uses direct file links format by checking HTML content
    private fun usesDirectDownloadLinks(url: String): Boolean {
        // Try to fetch a sample of the HTML to detect direct download links
        try {
            val html = fetchHtml(url)
            val sampleHtml = html.take(50000) // Check first 50KB
            
            // Check if the page contains direct links to ROM files with extensions
            val hasDirectLinks = Regex("""href=["'][^"']*\.(7z|zip|rar|gb|gbc|gba|nes|snes|iso|chd)["']""", RegexOption.IGNORE_CASE)
                .containsMatchIn(sampleHtml)
            
            if (hasDirectLinks) {
                return true
            }
            
            // Also check for sites like lolroms.com that have a distinctive structure
            // with folders and files clearly separated, even without visible extensions
            val hasGameListStructure = (sampleHtml.contains("## Folders", ignoreCase = true) && 
                                       sampleHtml.contains("## Files", ignoreCase = true)) ||
                                       (sampleHtml.contains("lolroms.com", ignoreCase = true) && 
                                        sampleHtml.contains("href=", ignoreCase = true)) ||
                                       (sampleHtml.contains("Sort by:", ignoreCase = true) && 
                                        sampleHtml.contains("Folders", ignoreCase = true) &&
                                        sampleHtml.contains("Files", ignoreCase = true))
            
            if (hasGameListStructure) {
                android.util.Log.d("RomSearch", "Detected game listing site structure (like lolroms.com)")
                return true
            }
            
            return false
        } catch (e: Exception) {
            android.util.Log.w("RomSearch", "Could not detect link format, using default parser")
            return false
        }
    }
    
    // Parse directory with direct download links
    private fun parseDirectDownloadSite(baseUrl: String, searchTerm: String = ""): List<RomItem> {
        android.util.Log.d("RomSearch", "Parsing directory with direct links: $baseUrl")
        val results = mutableListOf<RomItem>()
        
        try {
            val html = fetchHtml(baseUrl)
            if (html.isEmpty()) {
                android.util.Log.w("RomSearch", "Empty HTML response")
                return results
            }
            
            // Optimized single-pass parsing for better performance
            val romExtensions = "7z|zip|rar|gb|gbc|gba|nes|snes|smc|sfc|md|gen|n64|z64|nds|iso|chd|bin|cue"
            val allLinkPattern = Regex(
                """<a[^>]*href=["']([^"']+)["'][^>]*>([^<]+)</a>""",
                RegexOption.IGNORE_CASE
            )
            
            var processedCount = 0
            val addedItems = mutableSetOf<String>()
            val maxItems = if (searchTerm.isBlank()) 500 else 1000 // Increased limit for browsing
            
            android.util.Log.d("RomSearch", "Starting single-pass parsing (max $maxItems items)...")
            
            // Single pass through all links for better performance
            for (match in allLinkPattern.findAll(html)) {
                if (results.size >= maxItems) {
                    android.util.Log.d("RomSearch", "Reached max items limit ($maxItems), stopping...")
                    break
                }
                
                processedCount++
                if (processedCount % 100 == 0) {
                    android.util.Log.d("RomSearch", "Processed $processedCount links, found ${results.size} items...")
                }
                
                val href = match.groupValues.getOrNull(1) ?: continue
                val linkText = match.groupValues.getOrNull(2)?.trim() ?: continue
                
                // Debug first few links to see what we're getting
                if (processedCount <= 10) {
                    android.util.Log.d("RomSearch", "Link #$processedCount: '$linkText' -> '$href'")
                }
                
                // Skip navigation and system links
                if (linkText.contains("Back to") || linkText.contains("←") || linkText.contains("Home") ||
                    linkText.contains("Sort by") || linkText.contains("Append") || linkText.contains("JDownloader") ||
                    href.startsWith("?") || href.startsWith("#") || href == "/" || href.startsWith("../") ||
                    href.startsWith("mailto:") || href.startsWith("javascript:") || linkText.length < 2) {
                    continue
                }
                
                // No folder detection - process all links as potential ROM files for maximum speed
                
                // Avoid duplicates
                if (addedItems.contains(linkText)) {
                    continue
                }
                addedItems.add(linkText)
                
                // Process all valid links as potential ROM files (no folder detection)
                if (linkText.length >= 3 && 
                    !linkText.contains("http") && 
                    !linkText.matches(Regex("\\d{4}-\\d{2}-\\d{2}.*"))) { // Skip date patterns
                    
                    val itemUrl = buildAbsoluteUrl(baseUrl, href)
                    results += RomItem(
                        displayName = linkText,
                        downloadUrl = itemUrl,
                        platform = FoldernameManager.all.firstOrNull() ?: Foldername("unknown", "Unknown", "", listOf())
                    )
                    
                    if (results.size <= 5) {
                        android.util.Log.d("RomSearch", "Added item: $linkText")
                    }
                } else if (results.size <= 5) {
                    android.util.Log.d("RomSearch", "Skipped: $linkText - length=${linkText.length}")
                }
            }
            
            android.util.Log.d("RomSearch", "Single-pass parsing complete: processed $processedCount links, collected ${results.size} items")
            
        } catch (e: Exception) {
            android.util.Log.e("RomSearch", "Error parsing directory: ${e.message}")
            e.printStackTrace()
        }
        
        android.util.Log.d("RomSearch", "Total items found: ${results.size}")
        return results
    }
    
    // Extract actual download URL from a ROM detail page (not currently used)
    private fun extractDownloadUrlFromDetailPage(detailUrl: String): String? {
        try {
            val html = fetchHtml(detailUrl)
            if (html.isEmpty()) {
                android.util.Log.w("RomSearch", "Empty HTML for detail page: $detailUrl")
                return null
            }
            
            android.util.Log.d("RomSearch", "Searching for download link in: $detailUrl")
            
            // Look for download link patterns
            // Try multiple patterns in order of specificity
            val downloadPatterns = listOf(
                // Pattern 1: Links with "download" in the URL and file extension
                Regex("""href=["']([^"']*download[^"']*\.(?:zip|7z|rar|gb|gbc|gba|nes|snes|smc|sfc|md|gen|n64|z64|nds|iso|chd|bin|cue)[^"']*)["']""", RegexOption.IGNORE_CASE),
                
                // Pattern 2: Download button/link with class or id containing "download"
                Regex("""<(?:a|button)[^>]*(?:class|id)=["'][^"']*download[^"']*["'][^>]*href=["']([^"']+)["']""", RegexOption.IGNORE_CASE),
                
                // Pattern 3: PHP download scripts
                Regex("""href=["']([^"']*(?:download|get|fetch)\.php\?[^"']+)["']""", RegexOption.IGNORE_CASE),
                
                // Pattern 4: Direct file links with ROM extensions
                Regex("""href=["']([^"']*\.(?:zip|7z|rar|gb|gbc|gba|nes|snes|smc|sfc|md|gen|n64|z64|nds|iso|chd|bin|cue))["']""", RegexOption.IGNORE_CASE),
                
                // Pattern 5: Links with download-related text in onclick or data attributes
                Regex("""<a[^>]*(?:onclick|data-[^=]*)=["'][^"']*download[^"']*["'][^>]*href=["']([^"']+)["']""", RegexOption.IGNORE_CASE),
                
                // Pattern 6: Look for any href in a download button (more lenient)
                Regex("""<button[^>]*download[^>]*>.*?href=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            )
            
            for ((index, pattern) in downloadPatterns.withIndex()) {
                val match = pattern.find(html)
                if (match != null) {
                    val downloadHref = match.groupValues.getOrNull(1)
                    if (downloadHref != null && downloadHref.isNotEmpty()) {
                        val fullUrl = buildAbsoluteUrl(detailUrl, downloadHref)
                        android.util.Log.d("RomSearch", "Found download link (pattern $index): $fullUrl")
                        return fullUrl
                    }
                }
            }
            
            // If no patterns matched, log a snippet of the HTML for debugging
            val snippet = html.take(500).replace("\n", " ")
            android.util.Log.w("RomSearch", "No download link found. HTML snippet: $snippet...")
            
            return null
        } catch (e: Exception) {
            android.util.Log.e("RomSearch", "Error extracting download URL: ${e.message}")
            e.printStackTrace()
            return null
        }
    }

    private fun fetchHtml(url: String): String {
        android.util.Log.d("RomSearch", "Fetching HTML from: $url")
        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            )
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("DNT", "1")
            .header("Connection", "keep-alive")
            .header("Upgrade-Insecure-Requests", "1")
            .get()
            .build()
        return client.newCall(request).execute().use { response ->
            android.util.Log.d("RomSearch", "HTTP Response: ${response.code} ${response.message}")
            android.util.Log.d("RomSearch", "Final URL after redirects: ${response.request.url}")
            
            if (!response.isSuccessful) {
                android.util.Log.e("RomSearch", "HTTP request failed: ${response.code} ${response.message}")
                return@use ""
            }
            
            val responseBody = response.body
            if (responseBody == null) {
                android.util.Log.e("RomSearch", "Response body is null")
                return@use ""
            }
            
            // Check content encoding
            val contentEncoding = response.header("Content-Encoding")
            android.util.Log.d("RomSearch", "Content-Encoding: $contentEncoding")
            
            val html = try {
                responseBody.string()
            } catch (e: Exception) {
                android.util.Log.e("RomSearch", "Error reading response body: ${e.message}")
                ""
            }
            
            android.util.Log.d("RomSearch", "HTML length: ${html.length}")
            
            if (html.length > 0) {
                // Check if content looks like binary/compressed data
                val hasNullBytes = html.contains('\u0000')
                val hasControlChars = html.take(100).count { it.code < 32 && it != '\n' && it != '\r' && it != '\t' } > 5
                
                if (hasNullBytes || hasControlChars) {
                    android.util.Log.w("RomSearch", "⚠ HTML appears to be binary/compressed data")
                    android.util.Log.d("RomSearch", "First 50 bytes as hex: ${html.take(50).map { "%02x".format(it.code.toByte()) }.joinToString(" ")}")
                } else {
                    android.util.Log.d("RomSearch", "HTML preview: ${html.take(200)}...")
                    // Check if we got the right page
                    if (html.contains("GameBoyColor") || html.contains(".gbc") || html.contains("10-pin bowling")) {
                        android.util.Log.d("RomSearch", "✓ Found GameBoy Color content")
                    } else {
                        android.util.Log.w("RomSearch", "⚠ HTML doesn't contain GameBoy Color content - might be wrong page")
                    }
                }
            }
            html
        }
    }

    private fun buildAbsoluteUrl(base: String, href: String): String {
        // If href is already absolute, return as-is
        if (href.startsWith("http://") || href.startsWith("https://")) {
            return href
        }
        
        // If href starts with /, it's an absolute path from domain root
        if (href.startsWith("/")) {
            // Extract domain from base URL
            val domain = try {
                val url = java.net.URL(base)
                "${url.protocol}://${url.host}"
            } catch (e: Exception) {
                android.util.Log.e("RomSearch", "Error parsing base URL: ${e.message}")
                base.substringBefore("/", base)
            }
            return domain + href
        }
        
        // Otherwise, it's a relative path
        val normalizedBase = base.trimEnd('/') + "/"
        return normalizedBase + href.trimStart('/')
    }

    private fun isDirectoryLink(href: String): Boolean {
        if (href.isBlank()) return false
        if (href.startsWith("?")) return false
        if (href == "/") return false
        if (href.startsWith("../")) return false
        return href.endsWith("/")
    }

    private fun parseDirectoryRecursively(baseUrl: String, exts: Set<String>, depth: Int): List<RomItem> {
        android.util.Log.d("RomSearch", "Parsing directory: $baseUrl (depth: $depth)")
        val results = mutableListOf<RomItem>()
        
        try {
        val html = fetchHtml(baseUrl)
            if (html.isEmpty()) {
                android.util.Log.w("RomSearch", "Empty HTML response for: $baseUrl")
                return results
            }
            
        val links = linkRegex.findAll(html).mapNotNull { it.groupValues.getOrNull(1) }.toList()
            android.util.Log.d("RomSearch", "Found ${links.size} links in $baseUrl")
            
            var processedLinks = 0
        for (href in links) {
                processedLinks++
                if (processedLinks % 50 == 0) {
                    android.util.Log.d("RomSearch", "Processed $processedLinks/${links.size} links...")
                }
                
            val decoded = try { URLDecoder.decode(href, "UTF-8") } catch (e: Exception) { href }
            val lower = decoded.lowercase(Locale.ROOT)
                
                // Debug: Log first few files to see what we're getting
                if (results.isEmpty() && !isDirectoryLink(href) && processedLinks <= 3) {
                    android.util.Log.d("RomSearch", "Sample file: '$decoded' (lowercase: '$lower')")
                    android.util.Log.d("RomSearch", "Extensions looking for: $exts")
                    android.util.Log.d("RomSearch", "Checking if ends with: ${exts.map { ".$it" }}")
                }
                
            if (isDirectoryLink(href)) {
                if (depth > 0) {
                        android.util.Log.d("RomSearch", "Following subdirectory: $href")
                    val nextUrl = buildAbsoluteUrl(baseUrl, href)
                        try {
                    results += parseDirectoryRecursively(nextUrl, exts, depth - 1)
                        } catch (e: Exception) {
                            android.util.Log.e("RomSearch", "Error parsing subdirectory $nextUrl: ${e.message}")
                        }
                }
                continue
            }
                
            if (exts.any { lower.endsWith(".$it") }) {
                val nameOnly = decoded.substringAfterLast('/')
                val fullUrl = buildAbsoluteUrl(baseUrl, href)
                    android.util.Log.d("RomSearch", "Found file: $nameOnly")
                    results += RomItem(displayName = nameOnly, downloadUrl = fullUrl, platform = FoldernameManager.all.firstOrNull() ?: Foldername("unknown", "Unknown", "", listOf()))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("RomSearch", "Error parsing directory $baseUrl: ${e.message}")
            e.printStackTrace()
        }
        
        android.util.Log.d("RomSearch", "Total files found for $baseUrl: ${results.size}")
        return results
    }

    suspend fun search(platform: Foldername, term: String): List<RomItem> = withContext(Dispatchers.IO) {
        val startUrl = platform.archiveUrl.trimEnd('/') + "/"
        val exts = platform.extensions.map { it.lowercase(Locale.ROOT).removePrefix(".") }.toSet()
        
        android.util.Log.d("RomSearch", "Starting search for platform: ${platform.label}")
        android.util.Log.d("RomSearch", "URL: $startUrl")
        android.util.Log.d("RomSearch", "Extensions: $exts")
        android.util.Log.d("RomSearch", "Search term: '$term'")
        
        try {
            // Use withTimeout to prevent hanging
            val rawItems = withTimeout(30_000) { // 30 second timeout (optimized parsing)
                // Use special parser for sites with direct download links
                if (usesDirectDownloadLinks(startUrl)) {
                    android.util.Log.d("RomSearch", "Detected direct download link site, using optimized parser")
                    parseDirectDownloadSite(startUrl, term)
                } else {
                    parseDirectoryRecursively(startUrl, exts, depth = 1) // Reduced depth to prevent infinite loops
                }
            }
        val items = rawItems.map { it.copy(platform = platform) }
            val filteredItems = if (term.isBlank()) items else items.filter { it.displayName.contains(term, ignoreCase = true) }
            
            android.util.Log.d("RomSearch", "Final results: ${filteredItems.size} items")
            return@withContext filteredItems
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            android.util.Log.e("RomSearch", "Search timed out after 30 seconds")
            return@withContext emptyList()
        } catch (e: Exception) {
            android.util.Log.e("RomSearch", "Search failed: ${e.message}")
            e.printStackTrace()
            return@withContext emptyList()
        }
    }
}

class MainViewModel(private val context: Context) : ViewModel() {
    var selectedFoldername by mutableStateOf<Foldername?>(null)
        private set
    var searchTerm by mutableStateOf("")
        private set
    var results by mutableStateOf<List<RomItem>>(emptyList())
        private set
    var isSearching by mutableStateOf(false)
        private set

    var hosts by mutableStateOf<List<DiscoveredHost>>(emptyList())
        private set
    var selectedHost by mutableStateOf<DiscoveredHost?>(null)
        private set
    var isScanning by mutableStateOf(false)
        private set
    
    // Backlog items
    var backlogItems by mutableStateOf<List<com.romnix.app.ui.screens.BacklogItem>>(emptyList())
        private set

    private val repo = RomRepository()
    private val scanner = HostScanner()
    private val uploader = SshUploader()
    private val tester = SshTester()
    private val fileManager = SshFileManager()
    private val preferencesManager = com.romnix.app.data.PreferencesManager(context)
    
    init {
        // Load backlog items on initialization
        viewModelScope.launch {
            try {
                val loadedItems = preferencesManager.getBacklogItems()
                backlogItems = loadedItems
                Log.d("BacklogPersistence", "Loaded ${loadedItems.size} items from storage")
                loadedItems.forEach { item ->
                    Log.d("BacklogPersistence", "Loaded: ${item.title}, Sessions: ${item.sessions.size}")
                }
                
                // If nothing loaded from DataStore, try SharedPreferences directly
                if (loadedItems.isEmpty()) {
                    val sharedPrefs = context.getSharedPreferences("backlog_prefs", Context.MODE_PRIVATE)
                    val json = sharedPrefs.getString("backlog_items", "[]") ?: "[]"
                    Log.d("BacklogPersistence", "SharedPrefs JSON: $json")
                }
            } catch (e: Exception) {
                Log.e("BacklogPersistence", "Failed to load backlog items", e)
                // Try loading from SharedPreferences directly as fallback
                try {
                    val sharedPrefs = context.getSharedPreferences("backlog_prefs", Context.MODE_PRIVATE)
                    val json = sharedPrefs.getString("backlog_items", "[]") ?: "[]"
                    backlogItems = parseBacklogItemsFromJson(json)
                    Log.d("BacklogPersistence", "Loaded from SharedPrefs fallback: ${backlogItems.size} items")
                } catch (e2: Exception) {
                    Log.e("BacklogPersistence", "SharedPrefs fallback also failed", e2)
                    backlogItems = emptyList()
                }
            }
        }
    }
    
    private fun parseBacklogItemsFromJson(json: String): List<com.romnix.app.ui.screens.BacklogItem> {
        if (json == "[]" || json.isEmpty()) return emptyList()
        
        return try {
            val items = mutableListOf<com.romnix.app.ui.screens.BacklogItem>()
            val cleanJson = json.trim().removePrefix("[").removeSuffix("]")
            if (cleanJson.isNotEmpty()) {
                val itemObjects = cleanJson.split("},{").map { 
                    if (!it.startsWith("{")) "{$it" else it
                }.map { 
                    if (!it.endsWith("}")) "$it}" else it
                }
                
                for (obj in itemObjects) {
                    val item = parseBacklogItemFromJson(obj)
                    if (item != null) items.add(item)
                }
            }
            items
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    private fun parseBacklogItemFromJson(obj: String): com.romnix.app.ui.screens.BacklogItem? {
        return try {
            val fields = mutableMapOf<String, Any?>()
            val content = obj.trim().removePrefix("{").removeSuffix("}")
            
            // Better JSON parsing to handle different value types including null
            val regex = """"([^"]+)"\s*:\s*("([^"]*)"|(\d+)|(true|false)|(null))""".toRegex()
            regex.findAll(content).forEach { match ->
                val key = match.groupValues[1]
                val stringValue = match.groupValues[3]
                val numericValue = match.groupValues[4]
                val booleanValue = match.groupValues[5]
                val nullValue = match.groupValues[6]
                
                fields[key] = when {
                    nullValue == "null" -> null
                    stringValue.isNotEmpty() -> stringValue
                    numericValue.isNotEmpty() -> {
                        // Try to parse as Long first (for timestamps), then as Int
                        numericValue.toLongOrNull() ?: numericValue.toInt()
                    }
                    booleanValue.isNotEmpty() -> booleanValue.toBoolean()
                    else -> ""
                }
            }
            
            // For now, create a simple BacklogItem with the new structure
            com.romnix.app.ui.screens.BacklogItem(
                id = fields["id"] as? String ?: "",
                title = fields["title"] as? String ?: "",
                sessions = emptyList(),
                currentSession = null
            )
        } catch (e: Exception) {
            Log.e("BacklogPersistence", "Failed to parse item: $obj", e)
            null
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Save backlog items when ViewModel is cleared
        viewModelScope.launch {
            // End any active tracking sessions
            val currentTime = System.currentTimeMillis()
            val itemsToSave = backlogItems.map { item ->
                if (item.currentSession != null) {
                    // End the current session and add it to sessions list
                    val endedSession = item.currentSession.copy(endTime = currentTime)
                    item.copy(
                        sessions = item.sessions + endedSession,
                        currentSession = null
                    )
                } else {
                    item
                }
            }
            preferencesManager.saveBacklogItems(itemsToSave)
        }
    }

    fun setFoldername(p: Foldername?) { selectedFoldername = p }
    fun updateSearchTerm(t: String) { searchTerm = t }
    
    fun clearResults() { results = emptyList() }
    
    // Backlog methods
    fun updateBacklogItems(items: List<com.romnix.app.ui.screens.BacklogItem>) {
        backlogItems = items
        viewModelScope.launch {
            try {
                preferencesManager.saveBacklogItems(items)
                Log.d("BacklogPersistence", "Saved ${items.size} items to storage")
                items.forEach { item ->
                    Log.d("BacklogPersistence", "Item: ${item.title}, Sessions: ${item.sessions.size}, Tracking: ${item.isTracking}")
                }
            } catch (e: Exception) {
                Log.e("BacklogPersistence", "Failed to save backlog items", e)
            }
        }
    }
    
    fun saveBacklogState() {
        viewModelScope.launch {
            // Save current state with active sessions ended
            val currentTime = System.currentTimeMillis()
            val itemsToSave = backlogItems.map { item ->
                if (item.currentSession != null) {
                    // End the current session and add it to sessions list
                    val endedSession = item.currentSession.copy(endTime = currentTime)
                    item.copy(
                        sessions = item.sessions + endedSession,
                        currentSession = null
                    )
                } else {
                    item
                }
            }
            preferencesManager.saveBacklogItems(itemsToSave)
        }
    }
    
    fun forceBacklogSave() {
        // Also save synchronously to SharedPreferences directly
        val sharedPrefs = context.getSharedPreferences("backlog_prefs", Context.MODE_PRIVATE)
        val json = backlogItemsToJsonDirect(backlogItems)
        val saved = sharedPrefs.edit().putString("backlog_items", json).commit()
        Log.d("BacklogPersistence", "Direct save result: $saved for ${backlogItems.size} items")
        
        viewModelScope.launch {
            try {
                // Force save current state immediately
                Log.d("BacklogPersistence", "Force saving ${backlogItems.size} items")
                preferencesManager.saveBacklogItems(backlogItems)
                
                // Verify save by reading back
                val savedItems = preferencesManager.getBacklogItems()
                Log.d("BacklogPersistence", "Verified: ${savedItems.size} items saved")
                savedItems.forEach { item ->
                    Log.d("BacklogPersistence", "Verified: ${item.title}, Sessions: ${item.sessions.size}")
                }
            } catch (e: Exception) {
                Log.e("BacklogPersistence", "Force save failed", e)
            }
        }
    }
    
    private fun backlogItemsToJsonDirect(items: List<com.romnix.app.ui.screens.BacklogItem>): String {
        if (items.isEmpty()) return "[]"
        
        val itemStrings = items.map { item ->
            val sessionsJson = item.sessions.joinToString(",") { session ->
                "{\"id\":\"${session.id}\",\"startTime\":${session.startTime},\"endTime\":${session.endTime ?: "null"}}"
            }
            
            val currentSessionJson = item.currentSession?.let { session ->
                "{\"id\":\"${session.id}\",\"startTime\":${session.startTime},\"endTime\":${session.endTime ?: "null"}}"
            } ?: "null"
            
            "{\"id\":\"${item.id}\",\"title\":\"${item.title}\",\"sessions\":[${sessionsJson}],\"currentSession\":${currentSessionJson}}"
        }
        return "[${itemStrings.joinToString(",")}]"
    }

    fun searchAll() {
        val term = searchTerm
        isSearching = true
        results = emptyList()
        viewModelScope.launch {
            val aggregated = mutableListOf<RomItem>()
            for (p in FoldernameManager.all) {
                val list = runCatching { repo.search(p, term) }.getOrDefault(emptyList())
                aggregated += list
            }
            results = aggregated
            isSearching = false
        }
    }

    fun searchSelected() {
        val term = searchTerm
        val p = selectedFoldername ?: return
        isSearching = true
        results = emptyList()
        viewModelScope.launch {
            val list = runCatching { repo.search(p, term) }.getOrDefault(emptyList())
            results = list
            isSearching = false
        }
    }
    
    fun browseAllInFoldername() {
        val p = selectedFoldername ?: return
        isSearching = true
        results = emptyList()
        viewModelScope.launch {
            // Browse all content by searching with empty string
            val list = runCatching { repo.search(p, "") }.getOrDefault(emptyList())
            results = list
            isSearching = false
        }
    }

    fun scanHosts() {
        isScanning = true
        hosts = emptyList()
        selectedHost = null
        viewModelScope.launch(Dispatchers.IO) {
            val prefix = scanner.getLocalSubnetPrefix()
            val found = if (prefix != null) scanner.scanQuick(prefix) else emptyList()
            withContext(Dispatchers.Main) {
                hosts = found.map { DiscoveredHost(it) }
                selectedHost = hosts.firstOrNull()
                isScanning = false
            }
        }
    }

    fun selectHost(h: DiscoveredHost?) { 
        selectedHost = h
        // Try to determine template based on IP
        if (h != null) {
            val detectedTemplate = when (h.ip) {
                rocknixTemplate.hostIp -> "Rocknix"
                muosTemplate.hostIp -> "muOS"
                arkosTemplate.hostIp -> "ArkOS"
                anbernicTemplate.hostIp -> "Anbernic"
                else -> connectedTemplate // Keep current template if no match
            }
            updateConnectedTemplate(detectedTemplate)
            android.util.Log.d("RomTransfer", "Host selected: ${h.ip}, detected template: $connectedTemplate")
        }
    }

    fun addHostFromIp(ip: String): Boolean {
        val trimmed = ip.trim()
        val ipv4 = Regex("^((25[0-5]|2[0-4]\\d|[0-1]?\\d{1,2})\\.){3}(25[0-5]|2[0-4]\\d|[0-1]?\\d{1,2})$")
        if (!ipv4.matches(trimmed)) return false
        val host = DiscoveredHost(trimmed)
        if (hosts.none { it.ip == host.ip }) {
            hosts = hosts + host
        }
        selectedHost = host
        return true
    }

    // Connection settings
    var sshUsername by mutableStateOf("root")
    var sshPassword by mutableStateOf("root")
    var sshPort by mutableStateOf("22")
    var useGuest by mutableStateOf(false)
    var isDhcpNetwork by mutableStateOf(true)
    var remoteBasePath by mutableStateOf("")
    
    // Track which template was used for connection (persistent)
    var connectedTemplate by mutableStateOf<String?>(null)
        private set

    // Templates state
    var rocknixTemplate by mutableStateOf(
        HostTemplate(
            name = "Rocknix",
            username = "root",
            password = "rocknix",
            port = "22",
            remoteBasePath = "/storage/roms",
            hostIp = "192.168.0.132",
            useGuest = false,
            isDhcpNetwork = true
        )
    )
    var muosTemplate by mutableStateOf(
        HostTemplate(
            name = "muOS",
            username = "root",
            password = "root",
            port = "22",
            remoteBasePath = "/mnt/mmc/Roms",
            hostIp = "192.168.0.101",
            useGuest = false,
            isDhcpNetwork = true
        )
    )
    var arkosTemplate by mutableStateOf(
        HostTemplate(
            name = "ArkOS",
            username = "ark",
            password = "ark",
            port = "22",
            remoteBasePath = "/roms",
            hostIp = "192.168.0.132",
            useGuest = false,
            isDhcpNetwork = true
        )
    )
    var anbernicTemplate by mutableStateOf(
        HostTemplate(
            name = "Anbernic",
            username = "root",
            password = "root",
            port = "22",
            remoteBasePath = "/mnt/mmc/Roms",
            hostIp = "192.168.0.101",
            useGuest = false,
            isDhcpNetwork = true
        )
    )
    
    // Dynamic list of all device templates
    var deviceTemplates = mutableStateListOf<HostTemplate>()

    private fun templateKey(name: String, field: String): String = "template.${name}.${field}"

    private fun getStringPref(sp: android.content.SharedPreferences, key: String, default: String): String {
        val anyValue = sp.all[key]
        return when (anyValue) {
            is String -> anyValue
            else -> default
        }
    }

    fun loadTemplates(context: Context) {
        val sp = context.getSharedPreferences("templates", Context.MODE_PRIVATE)
        fun readDefaulting(base: HostTemplate): HostTemplate {
            val n = base.name
            return HostTemplate(
                name = n,
                username = getStringPref(sp, templateKey(n, "username"), base.username),
                password = getStringPref(sp, templateKey(n, "password"), base.password),
                port = getStringPref(sp, templateKey(n, "port"), base.port),
                remoteBasePath = getStringPref(sp, templateKey(n, "remoteBasePath"), base.remoteBasePath),
                hostIp = getStringPref(sp, templateKey(n, "hostIp"), base.hostIp),
                useGuest = sp.getBoolean(templateKey(n, "useGuest"), base.useGuest),
                isDhcpNetwork = sp.getBoolean(templateKey(n, "isDhcpNetwork"), base.isDhcpNetwork)
            )
        }
        rocknixTemplate = readDefaulting(rocknixTemplate)
        muosTemplate = readDefaulting(muosTemplate)
        arkosTemplate = readDefaulting(arkosTemplate)
        anbernicTemplate = readDefaulting(anbernicTemplate)
        
        // Load manual SSH settings
        sshUsername = getStringPref(sp, "manual_username", "root")
        sshPassword = getStringPref(sp, "manual_password", "root")
        sshPort = getStringPref(sp, "manual_port", "22")
        remoteBasePath = getStringPref(sp, "manual_remotePath", "/storage/roms")
        useGuest = sp.getBoolean("manual_useGuest", false)
        isDhcpNetwork = sp.getBoolean("manual_isDhcpNetwork", true)
        
        // Load connected template state
        connectedTemplate = sp.getString("connected_template", null)
        
        // Load device templates list
        loadDeviceTemplatesList(context)
    }

    fun updateTemplate(context: Context, updated: HostTemplate) {
        when (updated.name) {
            "Rocknix" -> rocknixTemplate = updated
            "muOS" -> muosTemplate = updated
            "ArkOS" -> arkosTemplate = updated
            "Anbernic" -> anbernicTemplate = updated
        }
        val sp = context.getSharedPreferences("templates", Context.MODE_PRIVATE)
        val n = updated.name
        sp.edit()
            .putString(templateKey(n, "username"), updated.username)
            .putString(templateKey(n, "password"), updated.password)
            .putString(templateKey(n, "port"), updated.port)
            .putString(templateKey(n, "remoteBasePath"), updated.remoteBasePath)
            .putString(templateKey(n, "hostIp"), updated.hostIp)
            .putBoolean(templateKey(n, "useGuest"), updated.useGuest)
            .putBoolean(templateKey(n, "isDhcpNetwork"), updated.isDhcpNetwork)
            .apply()
    }
    
    fun saveManualSettings(context: Context) {
        val sp = context.getSharedPreferences("templates", Context.MODE_PRIVATE)
        sp.edit()
            .putString("manual_username", sshUsername)
            .putString("manual_password", sshPassword)
            .putString("manual_port", sshPort)
            .putString("manual_remotePath", remoteBasePath)
            .putBoolean("manual_useGuest", useGuest)
            .putBoolean("manual_isDhcpNetwork", isDhcpNetwork)
            .putString("connected_template", connectedTemplate)
            .apply()
    }
    
    fun updateConnectedTemplate(template: String?) {
        connectedTemplate = template
        // Save immediately to persist across tab switches
        android.util.Log.d("RomTransfer", "Connected template changed to: $template")
    }

    fun getTemplate(name: String): HostTemplate = when (name) {
        "Rocknix" -> rocknixTemplate
        "muOS" -> muosTemplate
        "ArkOS" -> arkosTemplate
        "Anbernic" -> anbernicTemplate
        else -> rocknixTemplate
    }
    
    fun exportDevices(): String {
        val exportData = StringBuilder()
        // Header is already added by exportFoldernames()
        
        deviceTemplates.forEach { template ->
            exportData.appendLine("[Device]")
            exportData.appendLine("Name=${template.name}")
            exportData.appendLine("Username=${template.username}")
            exportData.appendLine("Password=${template.password}")
            exportData.appendLine("Port=${template.port}")
            exportData.appendLine("RemotePath=${template.remoteBasePath}")
            exportData.appendLine("HostIP=${template.hostIp}")
            exportData.appendLine("UseGuest=${template.useGuest}")
            exportData.appendLine("IsDhcpNetwork=${template.isDhcpNetwork}")
            exportData.appendLine()
        }
        
        return exportData.toString()
    }
    
    fun importDevices(context: Context, fileContent: String): Int {
        try {
            val lines = fileContent.lines()
            var currentDevice: MutableMap<String, String>? = null
            var importedCount = 0
            
            lines.forEach { line ->
                val trimmedLine = line.trim()
                when {
                    trimmedLine.startsWith("[Device]") -> {
                        currentDevice = mutableMapOf()
                    }
                    trimmedLine.startsWith("Name=") -> {
                        currentDevice?.put("name", trimmedLine.substringAfter("="))
                    }
                    trimmedLine.startsWith("Username=") -> {
                        currentDevice?.put("username", trimmedLine.substringAfter("="))
                    }
                    trimmedLine.startsWith("Password=") -> {
                        currentDevice?.put("password", trimmedLine.substringAfter("="))
                    }
                    trimmedLine.startsWith("Port=") -> {
                        currentDevice?.put("port", trimmedLine.substringAfter("="))
                    }
                    trimmedLine.startsWith("RemotePath=") -> {
                        currentDevice?.put("remotePath", trimmedLine.substringAfter("="))
                    }
                    trimmedLine.startsWith("HostIP=") -> {
                        currentDevice?.put("hostIp", trimmedLine.substringAfter("="))
                    }
                    trimmedLine.startsWith("UseGuest=") -> {
                        currentDevice?.put("useGuest", trimmedLine.substringAfter("="))
                    }
                    trimmedLine.startsWith("IsDhcpNetwork=") -> {
                        currentDevice?.put("isDhcpNetwork", trimmedLine.substringAfter("="))
                    }
                    trimmedLine.isEmpty() && currentDevice != null -> {
                        // End of device section, add to device templates
                        val name = currentDevice!!["name"]
                        if (name != null && name.isNotBlank()) {
                            val template = HostTemplate(
                                name = name,
                                username = currentDevice!!["username"] ?: "",
                                password = currentDevice!!["password"] ?: "",
                                port = currentDevice!!["port"] ?: "22",
                                remoteBasePath = currentDevice!!["remotePath"] ?: "",
                                hostIp = currentDevice!!["hostIp"] ?: "",
                                useGuest = currentDevice!!["useGuest"]?.toBoolean() ?: false,
                                isDhcpNetwork = currentDevice!!["isDhcpNetwork"]?.toBoolean() ?: false
                            )
                            // Add to deviceTemplates list if not already present
                            if (deviceTemplates.none { t -> t.name == name }) {
                                deviceTemplates.add(template)
                            }
                            importedCount++
                        }
                        currentDevice = null
                    }
                }
            }
            
            // Handle last device if file doesn't end with empty line
            if (currentDevice != null) {
                val name = currentDevice!!["name"]
                if (name != null && name.isNotBlank()) {
                    val template = HostTemplate(
                        name = name,
                        username = currentDevice!!["username"] ?: "",
                        password = currentDevice!!["password"] ?: "",
                        port = currentDevice!!["port"] ?: "22",
                        remoteBasePath = currentDevice!!["remotePath"] ?: "",
                        hostIp = currentDevice!!["hostIp"] ?: "",
                        useGuest = currentDevice!!["useGuest"]?.toBoolean() ?: false,
                        isDhcpNetwork = currentDevice!!["isDhcpNetwork"]?.toBoolean() ?: false
                    )
                    // Add to deviceTemplates list if not already present
                    if (deviceTemplates.none { t -> t.name == name }) {
                        deviceTemplates.add(template)
                    }
                    importedCount++
                }
            }
            
            // Save the device templates list
            if (importedCount > 0) {
                saveDeviceTemplatesList(context)
            }
            
            return importedCount
        } catch (e: Exception) {
            android.util.Log.e("RomTransfer", "Error importing devices", e)
            return 0
        }
    }
    
    fun addCustomDeviceTemplate(
        context: Context,
        name: String,
        username: String,
        password: String,
        hostIp: String,
        port: String,
        remotePath: String
    ) {
        val customTemplate = HostTemplate(
            name = name,
            username = username,
            password = password,
            port = port,
            remoteBasePath = remotePath,
            hostIp = hostIp,
            useGuest = false,
            isDhcpNetwork = false
        )
        deviceTemplates.add(customTemplate)
        saveDeviceTemplatesList(context)
    }
    
    fun updateDeviceTemplate(context: Context, updated: HostTemplate) {
        val index = deviceTemplates.indexOfFirst { template -> template.name == updated.name }
        if (index != -1) {
            deviceTemplates[index] = updated
            saveDeviceTemplatesList(context)
        }
    }
    
    fun clearAllTemplates(context: Context) {
        // Clear the device templates list completely
        deviceTemplates.clear()
        saveDeviceTemplatesList(context)
        
        // Clear connected template
        connectedTemplate = null
        saveManualSettings(context)
    }
    
    fun removeDeviceTemplate(context: Context, templateName: String) {
        val filtered = deviceTemplates.filter { template -> template.name != templateName }
        deviceTemplates.clear()
        deviceTemplates.addAll(filtered)
        saveDeviceTemplatesList(context)
    }
    
    private fun loadDeviceTemplatesList(context: Context) {
        val sp = context.getSharedPreferences("device_templates", Context.MODE_PRIVATE)
        val templateNames = sp.getStringSet("template_names", null)
        
        deviceTemplates.clear()
        
        if (templateNames != null && templateNames.isNotEmpty()) {
            // Load saved templates
            templateNames.forEach { name ->
                val template = HostTemplate(
                    name = sp.getString("${name}_name", name) ?: name,
                    username = sp.getString("${name}_username", "root") ?: "root",
                    password = sp.getString("${name}_password", "") ?: "",
                    port = sp.getString("${name}_port", "22") ?: "22",
                    remoteBasePath = sp.getString("${name}_remotePath", "/storage/roms") ?: "/storage/roms",
                    hostIp = sp.getString("${name}_hostIp", "") ?: "",
                    useGuest = sp.getBoolean("${name}_useGuest", false),
                    isDhcpNetwork = sp.getBoolean("${name}_isDhcpNetwork", false)
                )
                deviceTemplates.add(template)
            }
        }
        // If empty, start with no templates - user can add via + button or use Manual Configuration
    }
    
    private fun saveDeviceTemplatesList(context: Context) {
        val sp = context.getSharedPreferences("device_templates", Context.MODE_PRIVATE)
        val editor = sp.edit()
        
        // Save template names set
        val names = deviceTemplates.map { template -> template.name }.toSet()
        editor.putStringSet("template_names", names)
        
        // Save each template's data
        deviceTemplates.forEach { template ->
            val name = template.name
            editor.putString("${name}_name", template.name)
            editor.putString("${name}_username", template.username)
            editor.putString("${name}_password", template.password)
            editor.putString("${name}_port", template.port)
            editor.putString("${name}_remotePath", template.remoteBasePath)
            editor.putString("${name}_hostIp", template.hostIp)
            editor.putBoolean("${name}_useGuest", template.useGuest)
            editor.putBoolean("${name}_isDhcpNetwork", template.isDhcpNetwork)
        }
        
        editor.apply()
    }

    fun applyDeviceTemplate(context: Context, template: HostTemplate) {
        sshUsername = template.username
        sshPassword = template.password
        sshPort = template.port
        useGuest = template.useGuest
        isDhcpNetwork = template.isDhcpNetwork
        remoteBasePath = template.remoteBasePath
        updateConnectedTemplate(template.name)
        saveManualSettings(context) // Save state immediately
        if (template.hostIp.isNotBlank()) addHostFromIp(template.hostIp)
    }
    
    fun applyTemplateRocknix(context: Context) {
        val t = rocknixTemplate
        sshUsername = t.username
        sshPassword = t.password
        sshPort = t.port
        useGuest = t.useGuest
        isDhcpNetwork = t.isDhcpNetwork
        remoteBasePath = t.remoteBasePath
        updateConnectedTemplate("Rocknix")
        saveManualSettings(context) // Save state immediately
        if (t.hostIp.isNotBlank()) addHostFromIp(t.hostIp)
    }

    fun applyTemplateMuOS(context: Context) {
        val t = muosTemplate
        sshUsername = t.username
        sshPassword = t.password
        sshPort = t.port
        useGuest = t.useGuest
        isDhcpNetwork = t.isDhcpNetwork
        remoteBasePath = t.remoteBasePath
        updateConnectedTemplate("muOS")
        saveManualSettings(context) // Save state immediately
        if (t.hostIp.isNotBlank()) addHostFromIp(t.hostIp)
    }

    fun applyTemplateArkOS(context: Context) {
        val t = arkosTemplate
        sshUsername = t.username
        sshPassword = t.password
        sshPort = t.port
        useGuest = t.useGuest
        isDhcpNetwork = t.isDhcpNetwork
        remoteBasePath = t.remoteBasePath
        updateConnectedTemplate("ArkOS")
        saveManualSettings(context) // Save state immediately
        if (t.hostIp.isNotBlank()) addHostFromIp(t.hostIp)
    }

    fun applyTemplateAnbernic(context: Context) {
        val t = anbernicTemplate
        sshUsername = t.username
        sshPassword = t.password
        sshPort = t.port
        useGuest = t.useGuest
        isDhcpNetwork = t.isDhcpNetwork
        remoteBasePath = t.remoteBasePath
        updateConnectedTemplate("Anbernic")
        saveManualSettings(context) // Save state immediately
        if (t.hostIp.isNotBlank()) addHostFromIp(t.hostIp)
    }
    
    fun initializeFoldernames(context: Context) {
        FoldernameManager.loadCustomFoldernames(context)
        if (selectedFoldername == null && FoldernameManager.all.isNotEmpty()) {
            selectedFoldername = FoldernameManager.all.firstOrNull()
        }
    }
    
    fun testConnection(
        hostIp: String,
        username: String,
        password: String,
        port: Int,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val hostConfig = com.romnix.app.network.HostConfig(
                    host = hostIp,
                    port = port,
                    username = username,
                    password = password
                )
                val result = tester.test(hostConfig)
                withContext(Dispatchers.Main) {
                    onResult(result.isSuccess)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false)
                }
            }
        }
    }

    fun uploadFile(localFile: File, platform: Foldername, onResult: (Boolean, String?) -> Unit) {
        val hostIp = selectedHost?.ip ?: return onResult(false, "No host selected")
        viewModelScope.launch(Dispatchers.IO) {
            val port = sshPort.toIntOrNull() ?: 22
            val username = if (useGuest) sshUsername.ifEmpty { "root" } else sshUsername.ifEmpty { "root" }
            val password = if (useGuest) "" else sshPassword
            val res = uploader.upload(localFile, platform.id, HostConfig(host = hostIp, port = port, username = username, password = password), baseOverride = remoteBasePath)
            val msg = res.exceptionOrNull()?.message ?: res.getOrNull() ?: ""
            Log.d("RomDL", "Upload result isSuccess=${res.isSuccess} pathOrError=${msg}")
            withContext(Dispatchers.Main) { onResult(res.isSuccess, msg) }
        }
    }

    fun uploadFileToPath(localFile: File, remotePath: String, onResult: (Boolean, String?) -> Unit) {
        val hostIp = selectedHost?.ip ?: return onResult(false, "No host selected")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val port = sshPort.toIntOrNull() ?: 22
                val username = if (useGuest) sshUsername.ifEmpty { "root" } else sshUsername.ifEmpty { "root" }
                val password = if (useGuest) "" else sshPassword
                
                val hostConfig = HostConfig(
                    host = hostIp, 
                    port = port, 
                    username = username, 
                    password = password
                )
                
                // Use SshFileManager to upload file directly to specified path
                val result = fileManager.uploadFileToPath(hostConfig, localFile, remotePath)
                
                withContext(Dispatchers.Main) { 
                    onResult(result.isSuccess, result.getOrNull() ?: result.exceptionOrNull()?.message)
                }
            } catch (e: Exception) {
                Log.e("Upload", "Upload failed", e)
                withContext(Dispatchers.Main) { 
                    onResult(false, e.message)
                }
            }
        }
    }

    fun downloadAndTransfer(context: Context, downloader: Downloader, item: RomItem, onResult: (Boolean, String?) -> Unit) {
        if (selectedHost?.ip == null) {
            onResult(false, "No host selected")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            // Check if custom download location is enabled
            val preferencesManager = com.romnix.app.data.PreferencesManager(context)
            val useCustomLocation = preferencesManager.useCustomLocationFlow.first()
            val customLocationUri = preferencesManager.downloadLocationFlow.first()
            
            // First, check if file already exists and delete it
            val defaultFile = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "roms/${item.platform.id}/${item.displayName}"
            )
            
            // The actual file location depends on whether custom location is enabled
            var localFile = defaultFile
            
            // Also create parent directories if they don't exist
            localFile.parentFile?.mkdirs()
            
            android.util.Log.d("RomTransfer", "Custom location enabled: $useCustomLocation")
            android.util.Log.d("RomTransfer", "Expected file location: ${localFile.absolutePath}")
            android.util.Log.d("RomTransfer", "Parent directory exists: ${localFile.parentFile?.exists()}")
            
            if (localFile.exists()) {
                localFile.delete()
                android.util.Log.d("RomTransfer", "Deleted existing file: ${localFile.absolutePath}")
            }
            
            // Start the download (use special transfer download if AndroidDownloader)
            withContext(Dispatchers.Main) {
                if (downloader is AndroidDownloader) {
                    downloader.downloadForTransfer(context, item)
                } else {
                    downloader.download(context, item)
                }
            }
            
            // Wait for download to complete using DownloadManager status
            var downloadComplete = false
            var attempts = 0
            val maxAttempts = 300 // 5 minutes timeout (300 seconds)
            
            android.util.Log.d("RomTransfer", "Starting download monitoring for: ${item.displayName}")
            
            while (!downloadComplete && attempts < maxAttempts) {
                delay(1000) // Check every second
                attempts++
                
                // Log progress every 10 seconds
                if (attempts % 10 == 0) {
                    android.util.Log.d("RomTransfer", "Still waiting for download... (${attempts}s elapsed)")
                }
                
                // Check download status through DownloadManager
                if (downloader is AndroidDownloader) {
                    val isComplete = downloader.isDownloadComplete(context, item.displayName)
                    if (isComplete) {
                        // Give the system a moment to finish writing the file
                        delay(1000)
                        
                        // Try to find the file with retries
                        // Since we use downloadForTransfer, the file will stay in default location
                        var fileCheckAttempts = 0
                        while (fileCheckAttempts < 10) {
                            // Check the default location
                            if (defaultFile.exists() && defaultFile.length() > 0) {
                                android.util.Log.d("RomTransfer", "Download verified complete at default location: ${defaultFile.absolutePath}, size: ${defaultFile.length()} bytes")
                                localFile = defaultFile
                                downloadComplete = true
                                break
                            }
                            
                            // Check alternative location (Downloads root without subdirectories)
                            val alternativeFile = File(
                                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                                item.displayName
                            )
                            if (alternativeFile.exists() && alternativeFile.length() > 0) {
                                android.util.Log.d("RomTransfer", "Found file in Downloads root: ${alternativeFile.absolutePath}")
                                // Move it to expected location for transfer
                                defaultFile.parentFile?.mkdirs()
                                if (alternativeFile.renameTo(defaultFile)) {
                                    android.util.Log.d("RomTransfer", "Moved file to expected location")
                                    localFile = defaultFile
                                    downloadComplete = true
                                    break
                                }
                            }
                            
                            if (fileCheckAttempts % 3 == 0) {
                                android.util.Log.d("RomTransfer", "Waiting for file (attempt ${fileCheckAttempts + 1}/10)...")
                            }
                            fileCheckAttempts++
                            if (fileCheckAttempts < 10) {
                                delay(1000) // Wait 1 second before retry
                            }
                        }
                        
                        if (!downloadComplete) {
                            android.util.Log.e("RomTransfer", "Download reported complete but file never appeared at: ${localFile.absolutePath}")
                        }
                    }
                } else {
                    android.util.Log.d("RomTransfer", "Using fallback download detection (not AndroidDownloader)")
                    // Fallback: Check if file exists and is not being written to
                    // Always check default location
                    if (defaultFile.exists()) {
                        val initialSize = defaultFile.length()
                        delay(500) // Wait half a second
                        val currentSize = defaultFile.length()
                        
                        // If file size hasn't changed and is greater than 0, download is likely complete
                        if (initialSize == currentSize && initialSize > 0) {
                            android.util.Log.d("RomTransfer", "Download complete (size check): ${defaultFile.absolutePath}, size: $currentSize bytes")
                            localFile = defaultFile
                            downloadComplete = true
                        }
                    }
                }
            }
            
            if (!downloadComplete) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Download timeout - file not ready after ${maxAttempts} seconds")
                }
                return@launch
            }
            
            // File exists, now transfer it using template credentials
            android.util.Log.d("RomTransfer", "Download complete! Starting transfer process...")
            
            // Log file hash for debugging
            try {
                val md5 = java.security.MessageDigest.getInstance("MD5")
                localFile.inputStream().use { input ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        md5.update(buffer, 0, bytesRead)
                    }
                }
                val hash = md5.digest().joinToString("") { "%02x".format(it) }
                android.util.Log.d("RomTransfer", "Downloaded file MD5: $hash")
                android.util.Log.d("RomTransfer", "File size: ${localFile.length()} bytes")
                android.util.Log.d("RomTransfer", "File path: ${localFile.absolutePath}")
            } catch (e: Exception) {
                android.util.Log.e("RomTransfer", "Could not compute file hash: ${e.message}")
            }
            
            val hostIp = selectedHost?.ip ?: return@launch
            
            android.util.Log.d("RomTransfer", "=== TRANSFER DEBUG START ===")
            android.util.Log.d("RomTransfer", "Selected host IP: $hostIp")
            android.util.Log.d("RomTransfer", "Connected template: '$connectedTemplate'")
            android.util.Log.d("RomTransfer", "Manual SSH settings - username: $sshUsername, port: $sshPort")
            
            // First, try to find the template in deviceTemplates list (dynamic templates with updated IPs)
            val template = if (connectedTemplate != null) {
                val dynamicTemplate = deviceTemplates.find { t -> t.name == connectedTemplate }
                if (dynamicTemplate != null) {
                    android.util.Log.d("RomTransfer", "✓ Using dynamic template from deviceTemplates: ${dynamicTemplate.name}")
                    dynamicTemplate
                } else {
                    // Fall back to static templates if not in deviceTemplates
                    when (connectedTemplate) {
                        "Rocknix" -> {
                            android.util.Log.d("RomTransfer", "✓ Using static Rocknix template")
                            rocknixTemplate
                        }
                        "muOS" -> {
                            android.util.Log.d("RomTransfer", "✓ Using static muOS template")
                            muosTemplate
                        }
                        "ArkOS" -> {
                            android.util.Log.d("RomTransfer", "✓ Using static ArkOS template")
                            arkosTemplate
                        }
                        "Anbernic" -> {
                            android.util.Log.d("RomTransfer", "✓ Using static Anbernic template")
                            anbernicTemplate
                        }
                        "Manual" -> {
                            android.util.Log.d("RomTransfer", "⚠ Using manual settings")
                            HostTemplate(
                                name = "Manual",
                                username = sshUsername.ifEmpty { "root" },
                                password = if (useGuest) "" else sshPassword.ifEmpty { "root" },
                                port = sshPort.ifEmpty { "22" },
                                remoteBasePath = remoteBasePath.ifEmpty { "/storage/roms" },
                                hostIp = hostIp,
                                useGuest = useGuest,
                                isDhcpNetwork = isDhcpNetwork
                            )
                        }
                        else -> null
                    }
                }
            } else {
                null
            } ?: run {
                android.util.Log.d("RomTransfer", "⚠ No connected template found, using manual settings")
                HostTemplate(
                    name = "Manual",
                    username = sshUsername.ifEmpty { "root" },
                    password = if (useGuest) "" else sshPassword.ifEmpty { "root" },
                    port = sshPort.ifEmpty { "22" },
                    remoteBasePath = remoteBasePath.ifEmpty { "/storage/roms" },
                    hostIp = hostIp,
                    useGuest = useGuest,
                    isDhcpNetwork = isDhcpNetwork
                )
            }
            
            android.util.Log.d("RomTransfer", "Final template details:")
            android.util.Log.d("RomTransfer", "- Name: ${template.name}")
            android.util.Log.d("RomTransfer", "- Username: '${template.username}'")
            android.util.Log.d("RomTransfer", "- Password: '${template.password}' (length: ${template.password.length})")
            android.util.Log.d("RomTransfer", "- Port: '${template.port}'")
            android.util.Log.d("RomTransfer", "- Remote path: '${template.remoteBasePath}'")
            android.util.Log.d("RomTransfer", "=== TRANSFER DEBUG END ===")
            
            // Use template IP instead of selectedHost IP for template connections
            val actualHostIp = if (template.name != "Manual" && template.hostIp.isNotEmpty()) {
                android.util.Log.d("RomTransfer", "Using template IP: ${template.hostIp} instead of selected host: $hostIp")
                template.hostIp
            } else {
                android.util.Log.d("RomTransfer", "Using selected host IP: $hostIp")
                hostIp
            }
            
            val res = uploader.upload(
                localFile, 
                item.platform.id, 
                HostConfig(
                    host = actualHostIp, 
                    port = template.port.toIntOrNull() ?: 22, 
                    username = template.username.ifEmpty { "root" }, 
                    password = template.password
                ), 
                baseOverride = template.remoteBasePath.ifEmpty { "/storage/roms" }
            )
            val msg = res.exceptionOrNull()?.message ?: res.getOrNull() ?: ""
            Log.d("RomDL", "Download & Transfer result isSuccess=${res.isSuccess} pathOrError=${msg}")
            
            withContext(Dispatchers.Main) {
                if (res.isSuccess) {
                    onResult(true, "Downloaded and transferred to: ${msg}")
                } else {
                    val errorMsg = when {
                        msg.contains("failed to connect") -> "Cannot connect to device. Check IP address and network."
                        msg.contains("SSH auth failed") -> "Authentication failed. Check username/password."
                        msg.contains("timeout") -> "Connection timeout. Device may be unreachable."
                        else -> msg
                    }
                    onResult(false, errorMsg)
                }
            }
        }
    }

    fun downloadAndTransferAll(context: Context, downloader: Downloader, onProgress: (Int, Int, String) -> Unit, onComplete: (Int, Int) -> Unit) {
        if (selectedHost?.ip == null) {
            onProgress(0, 0, "No host selected")
            return
        }

        if (results.isEmpty()) {
            onProgress(0, 0, "No files found")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val totalItems = results.size
            var successCount = 0
            var failureCount = 0

            withContext(Dispatchers.Main) {
                onProgress(0, totalItems, "Starting bulk download and transfer...")
            }

            for ((index, item) in results.withIndex()) {
                val currentProgress = index + 1
                
                withContext(Dispatchers.Main) {
                    onProgress(currentProgress, totalItems, "Processing: ${item.displayName}")
                }

                try {
                    // Check and delete existing file first
                    val defaultFile = File(
                        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                        "roms/${item.platform.id}/${item.displayName}"
                    )
                    
                    // We'll use the default location for transfer
                    var localFile = defaultFile
                    
                    // Create parent directories if they don't exist
                    defaultFile.parentFile?.mkdirs()
                    
                    if (defaultFile.exists()) {
                        defaultFile.delete()
                        android.util.Log.d("RomTransfer", "Bulk: Deleted existing file: ${defaultFile.name}")
                    }
                    
                    // Start download (use special transfer download if AndroidDownloader)
                    withContext(Dispatchers.Main) {
                        if (downloader is AndroidDownloader) {
                            downloader.downloadForTransfer(context, item)
                        } else {
                            downloader.download(context, item)
                        }
                    }
                    
                    // Wait for download completion using DownloadManager status
                    var downloadComplete = false
                    var attempts = 0
                    val maxAttempts = 300 // 5 minutes timeout for each file
                    
                    while (!downloadComplete && attempts < maxAttempts) {
                        delay(1000)
                        attempts++
                        
                        // Check download status through DownloadManager
                        if (downloader is AndroidDownloader) {
                            val isComplete = downloader.isDownloadComplete(context, item.displayName)
                            if (isComplete) {
                                // Double check that file exists and has content at default location
                                if (defaultFile.exists() && defaultFile.length() > 0) {
                                    android.util.Log.d("RomTransfer", "Bulk: Download verified complete for ${item.displayName}, size: ${defaultFile.length()} bytes")
                                    localFile = defaultFile
                                    downloadComplete = true
                                }
                            }
                        } else {
                            // Fallback: Check if file exists and is not being written to
                            if (defaultFile.exists()) {
                                val initialSize = defaultFile.length()
                                delay(500) // Wait half a second
                                val currentSize = defaultFile.length()
                                
                                // If file size hasn't changed and is greater than 0, download is complete
                                if (initialSize == currentSize && initialSize > 0) {
                                    android.util.Log.d("RomTransfer", "Bulk: Download complete (size check) for ${item.displayName}, size: $currentSize bytes")
                                    localFile = defaultFile
                                    downloadComplete = true
                                }
                            }
                        }
                    }
                    
                    if (!downloadComplete) {
                        failureCount++
                        withContext(Dispatchers.Main) {
                            onProgress(currentProgress, totalItems, "Download timeout: ${item.displayName}")
                        }
                        continue
                    }
                    
                    // Transfer the file using connected template credentials
                    val hostIp = selectedHost?.ip ?: continue
                    
                    // First, try to find the template in deviceTemplates list (dynamic templates with updated IPs)
                    val template = if (connectedTemplate != null) {
                        val dynamicTemplate = deviceTemplates.find { t -> t.name == connectedTemplate }
                        if (dynamicTemplate != null) {
                            android.util.Log.d("RomTransfer", "Bulk: ✓ Using dynamic template from deviceTemplates: ${dynamicTemplate.name}")
                            dynamicTemplate
                        } else {
                            // Fall back to static templates if not in deviceTemplates
                            when (connectedTemplate) {
                                "Rocknix" -> {
                                    android.util.Log.d("RomTransfer", "Bulk: Using static Rocknix template")
                                    rocknixTemplate
                                }
                                "muOS" -> {
                                    android.util.Log.d("RomTransfer", "Bulk: Using static muOS template")
                                    muosTemplate
                                }
                                "ArkOS" -> {
                                    android.util.Log.d("RomTransfer", "Bulk: Using static ArkOS template")
                                    arkosTemplate
                                }
                                "Anbernic" -> {
                                    android.util.Log.d("RomTransfer", "Bulk: Using static Anbernic template")
                                    anbernicTemplate
                                }
                                "Manual" -> {
                                    android.util.Log.d("RomTransfer", "Bulk: Using manual settings")
                                    HostTemplate(
                                        name = "Manual",
                                        username = sshUsername.ifEmpty { "root" },
                                        password = if (useGuest) "" else sshPassword.ifEmpty { "root" },
                                        port = sshPort.ifEmpty { "22" },
                                        remoteBasePath = remoteBasePath.ifEmpty { "/storage/roms" },
                                        hostIp = hostIp,
                                        useGuest = useGuest,
                                        isDhcpNetwork = isDhcpNetwork
                                    )
                                }
                                else -> null
                            }
                        }
                    } else {
                        null
                    } ?: run {
                        android.util.Log.d("RomTransfer", "Bulk: No connected template found, using manual settings")
                        HostTemplate(
                            name = "Manual",
                            username = sshUsername.ifEmpty { "root" },
                            password = if (useGuest) "" else sshPassword.ifEmpty { "root" },
                            port = sshPort.ifEmpty { "22" },
                            remoteBasePath = remoteBasePath.ifEmpty { "/storage/roms" },
                            hostIp = hostIp,
                            useGuest = useGuest,
                            isDhcpNetwork = isDhcpNetwork
                        )
                    }
                    
                    // Use template IP instead of selectedHost IP for template connections
                    val actualHostIp = if (template.name != "Manual" && template.hostIp.isNotEmpty()) {
                        android.util.Log.d("RomTransfer", "Bulk: Using template IP: ${template.hostIp}")
                        template.hostIp
                    } else {
                        android.util.Log.d("RomTransfer", "Bulk: Using selected host IP: $hostIp")
                        hostIp
                    }
                    
                    val res = uploader.upload(
                        localFile, 
                        item.platform.id, 
                        HostConfig(
                            host = actualHostIp, 
                            port = template.port.toIntOrNull() ?: 22, 
                            username = template.username.ifEmpty { "root" }, 
                            password = template.password
                        ), 
                        baseOverride = template.remoteBasePath.ifEmpty { "/storage/roms" }
                    )
                    
                    if (res.isSuccess) {
                        successCount++
                        withContext(Dispatchers.Main) {
                            onProgress(currentProgress, totalItems, "✓ Completed: ${item.displayName}")
                        }
                    } else {
                        failureCount++
                        withContext(Dispatchers.Main) {
                            onProgress(currentProgress, totalItems, "✗ Transfer failed: ${item.displayName}")
                        }
                    }
                    
                    // Small delay between transfers to avoid overwhelming the system
                    delay(500)
                    
                } catch (e: Exception) {
                    failureCount++
                    withContext(Dispatchers.Main) {
                        onProgress(currentProgress, totalItems, "✗ Error: ${item.displayName} - ${e.message}")
                    }
                }
            }

            withContext(Dispatchers.Main) {
                onComplete(successCount, failureCount)
            }
        }
    }
    
    // File browser functions
    fun listRemoteFiles(remotePath: String, onResult: (Result<List<RemoteFile>>) -> Unit) {
        val hostIp = selectedHost?.ip ?: return onResult(Result.failure(IllegalStateException("No host selected")))
        val port = sshPort.toIntOrNull() ?: 22
        val username = if (useGuest) sshUsername.ifEmpty { "root" } else sshUsername.ifEmpty { "root" }
        val password = if (useGuest) "" else sshPassword
        
        viewModelScope.launch(Dispatchers.IO) {
            val hostConfig = HostConfig(host = hostIp, port = port, username = username, password = password)
            val result = fileManager.listFiles(hostConfig, remotePath)
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }
    
    fun renameRemoteFile(oldPath: String, newPath: String, onResult: (Result<String>) -> Unit) {
        val hostIp = selectedHost?.ip ?: return onResult(Result.failure(IllegalStateException("No host selected")))
        val port = sshPort.toIntOrNull() ?: 22
        val username = if (useGuest) sshUsername.ifEmpty { "root" } else sshUsername.ifEmpty { "root" }
        val password = if (useGuest) "" else sshPassword
        
        viewModelScope.launch(Dispatchers.IO) {
            val hostConfig = HostConfig(host = hostIp, port = port, username = username, password = password)
            val result = fileManager.renameFile(hostConfig, oldPath, newPath)
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }
    
    fun deleteRemoteFile(remotePath: String, isDirectory: Boolean, onResult: (Result<String>) -> Unit) {
        val hostIp = selectedHost?.ip ?: return onResult(Result.failure(IllegalStateException("No host selected")))
        val port = sshPort.toIntOrNull() ?: 22
        val username = if (useGuest) sshUsername.ifEmpty { "root" } else sshUsername.ifEmpty { "root" }
        val password = if (useGuest) "" else sshPassword
        
        viewModelScope.launch(Dispatchers.IO) {
            val hostConfig = HostConfig(host = hostIp, port = port, username = username, password = password)
            val result = fileManager.deleteFile(hostConfig, remotePath, isDirectory)
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }
    
    fun copyRemoteFile(sourcePath: String, destPath: String, onResult: (Result<String>) -> Unit) {
        val hostIp = selectedHost?.ip ?: return onResult(Result.failure(IllegalStateException("No host selected")))
        val port = sshPort.toIntOrNull() ?: 22
        val username = if (useGuest) sshUsername.ifEmpty { "root" } else sshUsername.ifEmpty { "root" }
        val password = if (useGuest) "" else sshPassword
        
        viewModelScope.launch(Dispatchers.IO) {
            val hostConfig = HostConfig(host = hostIp, port = port, username = username, password = password)
            val result = fileManager.copyFile(hostConfig, sourcePath, destPath)
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }
    
    fun createRemoteDirectory(remotePath: String, onResult: (Result<String>) -> Unit) {
        val hostIp = selectedHost?.ip ?: return onResult(Result.failure(IllegalStateException("No host selected")))
        val port = sshPort.toIntOrNull() ?: 22
        val username = if (useGuest) sshUsername.ifEmpty { "root" } else sshUsername.ifEmpty { "root" }
        val password = if (useGuest) "" else sshPassword
        
        viewModelScope.launch(Dispatchers.IO) {
            val hostConfig = HostConfig(host = hostIp, port = port, username = username, password = password)
            val result = fileManager.createDirectory(hostConfig, remotePath)
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }
    
    fun downloadRemoteFile(remotePath: String, localFile: java.io.File, onProgress: ((Long, Long) -> Unit)? = null, onResult: (Result<String>) -> Unit) {
        val hostIp = selectedHost?.ip ?: return onResult(Result.failure(IllegalStateException("No host selected")))
        val port = sshPort.toIntOrNull() ?: 22
        val username = if (useGuest) sshUsername.ifEmpty { "root" } else sshUsername.ifEmpty { "root" }
        val password = if (useGuest) "" else sshPassword
        
        viewModelScope.launch(Dispatchers.IO) {
            val hostConfig = HostConfig(host = hostIp, port = port, username = username, password = password)
            val result = fileManager.downloadFile(hostConfig, remotePath, localFile, onProgress)
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RomApp(viewModel: MainViewModel, downloader: Downloader) {
    var selectedTab by remember { mutableStateOf(0) } // 0=Home, 1=Search, 2=Devices, 3=Backlog
    var showOverflowMenu by remember { mutableStateOf(false) }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer, // Theme-aware background
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(top = 8.dp, bottom = 52.dp) // Added extra bottom padding for system nav bar
            ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    CustomTabItem(
                        icon = com.romnix.app.R.drawable.backlog,
                        selectedIcon = com.romnix.app.R.drawable.backlog_selected,
                        label = "Backlog",
                        isSelected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )
                    
                    CustomTabItem(
                        icon = com.romnix.app.R.drawable.search_nav,
                        selectedIcon = com.romnix.app.R.drawable.search_selected,
                        label = "Search",
                        isSelected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    )
                    
                    CustomTabItem(
                        icon = com.romnix.app.R.drawable.devices,
                        selectedIcon = com.romnix.app.R.drawable.devices_selected,
                        label = "Devices",
                        isSelected = selectedTab == 2,
                        onClick = { selectedTab = 2 }
                    )
                    
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            when (selectedTab) {
                0 -> BacklogScreen(viewModel)
                1 -> BrowseScreen(
                    viewModel = viewModel,
                    downloader = downloader
                )
                2 -> DevicesScreen(viewModel)
                else -> BacklogScreen(viewModel)
            }
            
        }
    }
}

@Composable
private fun CustomTabItem(
    icon: Int,
    selectedIcon: Int,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
    ) {
        // Icon with pill-shaped background and ripple on pill only
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp)) // Pill shape
                .background(
                    if (isSelected) {
                        MaterialTheme.colorScheme.surfaceVariant // Pill background when selected
                    } else {
                        Color.Transparent
                    }
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current // Material 3 ripple on pill only
                ) { onClick() }
                .padding(horizontal = 20.dp, vertical = 8.dp), // Horizontal pill padding
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = if (isSelected) selectedIcon else icon),
                contentDescription = label,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onBackground
                },
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Label text outside the pill - clickable but no visual feedback
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
        )
    }
}

@Composable
private fun OldBrowseScreen(viewModel: MainViewModel, downloader: Downloader) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Top) {
        Spacer(Modifier.height(8.dp))
        // Search row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            var showPicker by remember { mutableStateOf(false) }
            var platformLabel by remember { mutableStateOf(viewModel.selectedFoldername?.label ?: "All Foldernames") }

            Button(onClick = { showPicker = true }) { Text(platformLabel, style = MaterialTheme.typography.labelLarge) }
            if (showPicker) {
                AlertDialog(
                    onDismissRequest = { showPicker = false },
                    title = { Text("Select Console") },
                    text = {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            item {
                                Button(onClick = {
                                    viewModel.setFoldername(null)
                                    platformLabel = "All Foldernames"
                                    showPicker = false
                                }, modifier = Modifier.fillMaxWidth()) { Text("All Foldernames") }
                            }
                            items(FoldernameManager.all) { p ->
                                Button(onClick = {
                                    viewModel.setFoldername(p)
                                    platformLabel = p.label
                                    showPicker = false
                                }, modifier = Modifier.fillMaxWidth()) { Text(p.label) }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showPicker = false }) { Text("Close") }
                    }
                )
            }

            OutlinedTextField(
                value = viewModel.searchTerm,
                onValueChange = { viewModel.updateSearchTerm(it) },
                label = { Text("Search") },
                modifier = Modifier.weight(1f)
            )

            Button(onClick = {
                if (viewModel.selectedFoldername == null) viewModel.searchAll() else viewModel.searchSelected()
            }) { Text("Search") }
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        // Quick filter chips for common platforms
        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val quick = FoldernameManager.all.take(5)
            items(quick) { p ->
                val selected = viewModel.selectedFoldername == p
                FilterChip(
                    selected = selected,
                    onClick = {
                        viewModel.setFoldername(if (selected) null else p)
                    },
                    label = { Text(p.label) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        ResultsList(
            results = viewModel.results, 
            onDownload = { item ->
                downloader.download(context = ctx, item = item)
            }, 
            onUpload = { item ->
                val localFile = File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "roms/${item.platform.id}/${item.displayName}")
                viewModel.uploadFile(localFile, item.platform) { ok, remote ->
                    val msg = if (ok) "Uploaded to: ${remote ?: "unknown"}" else "Upload failed"
                    android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_LONG).show()
                }
            },
            onDownloadAndTransfer = if (viewModel.selectedHost != null) {
                { item ->
                    viewModel.downloadAndTransfer(ctx, downloader, item) { ok, message ->
                        val msg = if (ok) message ?: "Download and transfer completed" else "Download and transfer failed: ${message ?: "unknown error"}"
                        android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            } else null
        )
    }
}

@Composable
fun ResultsList(
    results: List<RomItem>,
    onDownload: (RomItem) -> Unit,
    onUpload: (RomItem) -> Unit,
    onDownloadAndTransfer: ((RomItem) -> Unit)? = null
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(results) { item ->
            androidx.compose.material3.ListItem(
                headlineContent = { Text(item.displayName) },
                supportingContent = { Text(item.platform.label) },
                leadingContent = { androidx.compose.material3.Icon(Icons.Default.List, contentDescription = null) },
                trailingContent = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.IconButton(onClick = { onDownload(item) }) {
                            androidx.compose.material3.Icon(Icons.Default.Download, contentDescription = "Download")
                        }
                        androidx.compose.material3.IconButton(onClick = { onUpload(item) }) {
                            androidx.compose.material3.Icon(Icons.Default.Send, contentDescription = "Upload")
                        }
                        if (onDownloadAndTransfer != null) {
                            androidx.compose.material3.IconButton(onClick = { onDownloadAndTransfer(item) }) {
                                androidx.compose.material3.Icon(Icons.Default.CheckCircle, contentDescription = "Download & Transfer")
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun OldHandheldScreen(viewModel: MainViewModel) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Top) {
        val appCtx = androidx.compose.ui.platform.LocalContext.current
        LaunchedEffect(Unit) { 
            viewModel.loadTemplates(appCtx)
            viewModel.initializeFoldernames(appCtx)
        }
        var showTemplatesEditor by remember { mutableStateOf(false) }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Handheld", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = { showTemplatesEditor = true }) { Text("Edit templates") }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Settings, contentDescription = null)
                        Text("Rocknix", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { viewModel.applyTemplateRocknix(appCtx) }, modifier = Modifier.fillMaxWidth()) { Text("Connect") }
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Settings, contentDescription = null)
                        Text("muOS", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { viewModel.applyTemplateMuOS(appCtx) }, modifier = Modifier.fillMaxWidth()) { Text("Connect") }
                }
            }
        }
        if (showTemplatesEditor) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showTemplatesEditor = false },
                title = { Text("Edit Templates") },
                text = {
                    // Local editable copies
                    var rock by remember(showTemplatesEditor) { mutableStateOf(viewModel.rocknixTemplate) }
                    var mu by remember(showTemplatesEditor) { mutableStateOf(viewModel.muosTemplate) }
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        item { Text("Rocknix", style = MaterialTheme.typography.titleMedium) }
                        item { Spacer(Modifier.height(8.dp)) }
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = rock.username,
                                    onValueChange = { rock = rock.copy(username = it) },
                                    label = { Text("Username") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = rock.password,
                                    onValueChange = { rock = rock.copy(password = it) },
                                    label = { Text("Password") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = rock.port,
                                    onValueChange = { rock = rock.copy(port = it.filter { ch -> ch.isDigit() }.take(5)) },
                                    label = { Text("Port") },
                                    modifier = Modifier.weight(0.6f)
                                )
                            }
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                        item {
                            OutlinedTextField(
                                value = rock.remoteBasePath,
                                onValueChange = { rock = rock.copy(remoteBasePath = it) },
                                label = { Text("Remote base path") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                        item {
                            OutlinedTextField(
                                value = rock.hostIp,
                                onValueChange = { rock = rock.copy(hostIp = it) },
                                label = { Text("Host IP") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                var rockGuest by remember(rock.useGuest) { mutableStateOf(rock.useGuest) }
                                Button(onClick = {
                                    rockGuest = !rockGuest
                                    rock = rock.copy(useGuest = rockGuest)
                                    if (rockGuest) rock = rock.copy(password = "")
                                }) { Text(if (rockGuest) "Guest: ON" else "Guest: OFF") }
                                var rockDhcp by remember(rock.isDhcpNetwork) { mutableStateOf(rock.isDhcpNetwork) }
                                Button(onClick = {
                                    rockDhcp = !rockDhcp
                                    rock = rock.copy(isDhcpNetwork = rockDhcp)
                                }) { Text(if (rockDhcp) "DHCP" else "Manual") }
                            }
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                        item { Button(onClick = { viewModel.updateTemplate(appCtx, rock) }) { Text("Save Rocknix") } }

                        item { Spacer(Modifier.height(16.dp)) }
                        item { Text("muOS", style = MaterialTheme.typography.titleMedium) }
                        item { Spacer(Modifier.height(8.dp)) }
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = mu.username,
                                    onValueChange = { mu = mu.copy(username = it) },
                                    label = { Text("Username") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = mu.password,
                                    onValueChange = { mu = mu.copy(password = it) },
                                    label = { Text("Password") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = mu.port,
                                    onValueChange = { mu = mu.copy(port = it.filter { ch -> ch.isDigit() }.take(5)) },
                                    label = { Text("Port") },
                                    modifier = Modifier.weight(0.6f)
                                )
                            }
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                        item {
                            OutlinedTextField(
                                value = mu.remoteBasePath,
                                onValueChange = { mu = mu.copy(remoteBasePath = it) },
                                label = { Text("Remote base path") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                        item {
                            OutlinedTextField(
                                value = mu.hostIp,
                                onValueChange = { mu = mu.copy(hostIp = it) },
                                label = { Text("Host IP") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                var muGuest by remember(mu.useGuest) { mutableStateOf(mu.useGuest) }
                                Button(onClick = {
                                    muGuest = !muGuest
                                    mu = mu.copy(useGuest = muGuest)
                                    if (muGuest) mu = mu.copy(password = "")
                                }) { Text(if (muGuest) "Guest: ON" else "Guest: OFF") }
                                var muDhcp by remember(mu.isDhcpNetwork) { mutableStateOf(mu.isDhcpNetwork) }
                                Button(onClick = {
                                    muDhcp = !muDhcp
                                    mu = mu.copy(isDhcpNetwork = muDhcp)
                                }) { Text(if (muDhcp) "DHCP" else "Manual") }
                            }
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                        item { Button(onClick = { viewModel.updateTemplate(appCtx, mu) }) { Text("Save muOS") } }
                    }
                },
                confirmButton = { TextButton(onClick = { showTemplatesEditor = false }) { Text("Close") } }
            )
        }
    }
}

@Composable
private fun CustomHostsScreen(viewModel: MainViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Network page title
            Text(
                text = "Network",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        
        item {
            // Host Discovery Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text("Host Discovery", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.scanHosts() },
                                shape = RoundedCornerShape(12.dp)
                            ) { 
                                Text(if (viewModel.isScanning) "Scanning..." else "Scan") 
                            }
                            val ctx = androidx.compose.ui.platform.LocalContext.current
                            Button(
                                onClick = {
                                    val ip = viewModel.selectedHost?.ip
                                    if (ip == null) {
                                        android.widget.Toast.makeText(ctx, "No host selected", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.viewModelScope.launch(Dispatchers.IO) {
                                            val resultText = try {
                                                val port = viewModel.sshPort.toIntOrNull() ?: 22
                                                val username = if (viewModel.useGuest) viewModel.sshUsername.ifEmpty { "root" } else viewModel.sshUsername.ifEmpty { "root" }
                                                val password = if (viewModel.useGuest) "" else viewModel.sshPassword
                                                com.romnix.app.network.SshTester().test(HostConfig(host = ip, port = port, username = username, password = password)).getOrThrow()
                                            } catch (t: Throwable) {
                                                val err = t.message ?: "Connection failed"
                                                Log.e("RomDL", "Test connection failed", t)
                                                err
                                            }
                                            withContext(Dispatchers.Main) {
                                                val shortMsg = resultText.take(80)
                                                android.widget.Toast.makeText(ctx, shortMsg, android.widget.Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) { 
                                Text("Test") 
                            }
                        }
                    }
                    
                    // Manual IP entry
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        var ip by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = ip,
                            onValueChange = { ip = it },
                            label = { Text("Host IP Address") },
                            placeholder = { Text("192.168.0.159") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Button(
                            onClick = { viewModel.addHostFromIp(ip) },
                            shape = RoundedCornerShape(12.dp)
                        ) { 
                            Text("Add") 
                        }
                    }
                }
            }
        }
        
        item {
            // SSH Configuration Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("SSH Configuration", style = MaterialTheme.typography.titleMedium)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.sshUsername,
                            onValueChange = { viewModel.sshUsername = it },
                            label = { Text("Username") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = viewModel.sshPassword,
                            onValueChange = { viewModel.sshPassword = it },
                            label = { Text("Password") },
                            placeholder = { Text("Leave blank for guest") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = viewModel.sshPort,
                            onValueChange = { viewModel.sshPort = it.filter { ch -> ch.isDigit() }.take(5) },
                            label = { Text("Port") },
                            placeholder = { Text("22") },
                            modifier = Modifier.weight(0.6f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    
                    // Settings toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            var guest by remember { mutableStateOf(viewModel.useGuest) }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Text("Guest Mode", style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = guest, 
                                    onCheckedChange = {
                                        guest = it
                                        viewModel.useGuest = it
                                        if (it) viewModel.sshPassword = ""
                                    }
                                )
                            }
                            var dhcp by remember { mutableStateOf(viewModel.isDhcpNetwork) }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Text("DHCP Network", style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = dhcp, 
                                    onCheckedChange = {
                                        dhcp = it
                                        viewModel.isDhcpNetwork = it
                                    }
                                )
                            }
                        }
                        val ctx2 = androidx.compose.ui.platform.LocalContext.current
                        Button(
                            onClick = {
                                val ip = viewModel.selectedHost?.ip
                                val port = viewModel.sshPort
                                val user = viewModel.sshUsername
                                android.widget.Toast.makeText(ctx2, "Using $user@$ip:$port", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) { 
                            Text("Apply") 
                        }
                    }
                    
                    // Remote path
                    OutlinedTextField(
                        value = viewModel.remoteBasePath,
                        onValueChange = { viewModel.remoteBasePath = it },
                        label = { Text("Remote Base Path") },
                        placeholder = { Text("/roms or /mnt/mmc/ROMS") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
        
        item {
            // Discovered Hosts Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Discovered Hosts", style = MaterialTheme.typography.titleMedium)
                    
                    if (viewModel.hosts.isEmpty()) {
                        Text(
                            "No hosts found. Use the Scan button to discover devices on your network.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        viewModel.hosts.forEach { host ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (viewModel.selectedHost == host) 
                                        NavyBlue.copy(alpha = 0.1f)
                                    else 
                                        MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Text(host.ip, style = MaterialTheme.typography.bodyLarge)
                                    Button(
                                        onClick = { viewModel.selectHost(host) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = if (viewModel.selectedHost == host) 
                                            ButtonDefaults.buttonColors(containerColor = NavyBlue)
                                        else 
                                            ButtonDefaults.buttonColors()
                                    ) { 
                                        Text(if (viewModel.selectedHost == host) "Selected" else "Select") 
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

object LocalAppContext {
    val current: Context
        @Composable get() = androidx.compose.ui.platform.LocalContext.current
}
