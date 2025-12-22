@file:OptIn(ExperimentalFoundationApi::class)

package com.romnix.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import kotlin.math.max
import kotlin.math.min
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.romnix.app.MainViewModel
import com.romnix.app.R
import com.romnix.app.network.RemoteFile
import com.romnix.app.ui.theme.ButtonStyles
import com.romnix.app.ui.theme.PalestBlue
import com.romnix.app.ui.theme.NavyBlue
import com.romnix.app.ui.theme.ErrorRed
import com.romnix.app.ui.theme.AppWhite
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import android.os.Environment
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

// Custom darker surfaceVariant for focused search bars
@Composable
private fun ColorScheme.surfaceVariantDarker(): Color {
    return if (background.luminance() > 0.5) {
        // Light theme: Slightly darker surfaceVariant
        Color(
            red = (surfaceVariant.red * 0.9f).coerceIn(0f, 1f),
            green = (surfaceVariant.green * 0.9f).coerceIn(0f, 1f),
            blue = (surfaceVariant.blue * 0.9f).coerceIn(0f, 1f),
            alpha = 1f
        )
    } else {
        // Dark theme: Slightly darker surfaceVariant
        Color(
            red = (surfaceVariant.red * 0.85f).coerceIn(0f, 1f),
            green = (surfaceVariant.green * 0.85f).coerceIn(0f, 1f),
            blue = (surfaceVariant.blue * 0.85f).coerceIn(0f, 1f),
            alpha = 1f
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    viewModel: MainViewModel,
    deviceName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    var currentPath by remember { mutableStateOf(viewModel.remoteBasePath.ifEmpty { "/" }) }
    var files by remember { mutableStateOf<List<RemoteFile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedFile by remember { mutableStateOf<RemoteFile?>(null) }
    // Removed showOptionsMenu - using swipe actions instead
    var showRenameDialog by remember { mutableStateOf(false) }
    var showCopyDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0L) }
    var downloadTotal by remember { mutableStateOf(0L) }
    var isDownloading by remember { mutableStateOf(false) }
    var selectedDestinationUri by remember { mutableStateOf<Uri?>(null) }
    var destinationDisplayName by remember { mutableStateOf("Downloads/Romnix/") }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0L) }
    var uploadTotal by remember { mutableStateOf(0L) }
    
    // Pull to refresh state
    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    
    // Folder picker launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            selectedDestinationUri = it
            // Get display name of the selected folder
            val docFile = DocumentFile.fromTreeUri(context, it)
            destinationDisplayName = docFile?.name ?: "Selected folder"
        }
    }
    
    // File picker launcher for import
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                isUploading = true
                uploadProgress = 0L
                uploadTotal = 0L
                
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val fileName = DocumentFile.fromSingleUri(context, it)?.name ?: "uploaded_file"
                    val remotePath = if (currentPath.endsWith("/")) "$currentPath$fileName" else "$currentPath/$fileName"
                    
                    if (inputStream != null) {
                        // Create a temporary file to work with
                        val tempFile = File.createTempFile("upload_temp", ".tmp", context.cacheDir)
                        tempFile.outputStream().use { output ->
                            inputStream.copyTo(output)
                        }
                        inputStream.close()
                        
                        // Get file size for progress
                        uploadTotal = tempFile.length()
                        
                        // Upload the file using viewModel
                        android.widget.Toast.makeText(context, "Uploading $fileName...", android.widget.Toast.LENGTH_SHORT).show()
                        
                        // Upload file using viewModel.uploadFileToPath (we need to create this method)
                        viewModel.uploadFileToPath(tempFile, remotePath) { success, message ->
                            tempFile.delete()
                            
                            if (success) {
                                android.widget.Toast.makeText(context, "File uploaded successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                // Force reload the current directory to show new file
                                viewModel.listRemoteFiles(currentPath) { listResult ->
                                    listResult.fold(
                                        onSuccess = { fileList ->
                                            files = fileList
                                            errorMessage = null
                                        },
                                        onFailure = { error ->
                                            errorMessage = error.message ?: "Failed to refresh files"
                                        }
                                    )
                                }
                            } else {
                                android.widget.Toast.makeText(context, "Upload failed: ${message ?: "Unknown error"}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Upload failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                } finally {
                    // Always reset upload state
                    isUploading = false
                    uploadProgress = 0L
                    uploadTotal = 0L
                }
            }
        }
    }
    
    // Refresh function
    fun refreshFiles() {
        isLoading = true
        isRefreshing = true
        errorMessage = null
        viewModel.listRemoteFiles(currentPath) { result ->
            isLoading = false
            isRefreshing = false
            result.fold(
                onSuccess = { fileList ->
                    files = fileList
                    errorMessage = null
                },
                onFailure = { error ->
                    errorMessage = error.message ?: "Failed to load files"
                    files = emptyList()
                }
            )
        }
    }
    
    // Load files when path changes
    LaunchedEffect(currentPath) {
        refreshFiles()
    }
    
    // Handle pull to refresh
    LaunchedEffect(pullToRefreshState.isRefreshing) {
        if (pullToRefreshState.isRefreshing) {
            refreshFiles()
        }
    }
    
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) {
            pullToRefreshState.endRefresh()
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Back Arrow (above everything)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 12.dp, end = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Header with title and actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp, start = 20.dp, end = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = deviceName,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 24.sp
                    )
                    Text(
                        text = "Swipe left for more actions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer,  // Same as card background
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { showCreateFolderDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CreateNewFolder,
                            contentDescription = "Create Folder",
                            tint =  MaterialTheme.colorScheme.tertiary,  // Theme-aware icon
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    // Import Button - using Box to reduce overdraw
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                CircleShape
                            )
                            .clickable { importFileLauncher.launch(arrayOf("*/*")) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Import Files",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            
            // Path breadcrumb
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer) // path box background
                    .clickable { 
                        scope.launch { 
                            listState.animateScrollToItem(0) 
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = currentPath,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Content with pull-to-refresh
            Box(
                modifier = Modifier
                    .weight(1f)
                    .nestedScroll(pullToRefreshState.nestedScrollConnection)
            ) {
                when {
                    isLoading && !isRefreshing -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            ShapeMorphingLoader(
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(55.dp)  // 15% bigger
                            )
                        }
                    }
                    errorMessage != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = errorMessage ?: "Error",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Parent directory item - always show if not root
                            if (currentPath != "/") {
                                item {
                                    FileItem(
                                        file = RemoteFile(
                                            name = "..",
                                            path = currentPath.substringBeforeLast('/').ifEmpty { "/" },
                                            isDirectory = true,
                                            size = 0,
                                            permissions = "",
                                            modifiedTime = 0
                                        ),
                                        onClick = {
                                            currentPath = currentPath.substringBeforeLast('/').ifEmpty { "/" }
                                        },
                                        onLongClick = {}
                                    )
                                }
                            }
                            
                            // Show empty folder message if no files
                            if (files.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.romnix_emptystate),
                                                contentDescription = null,
                                                tint = surfaceVariantDarker(),
                                                modifier = Modifier.size(120.dp)
                                            )
                                            
                                            Spacer(modifier = Modifier.height(16.dp))
                                            
                                            Text(
                                                text = "Empty directory",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            Text(
                                                text = "No files or folders here",
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Files and directories
                            items(files) { file ->
                                SwipeableFileItem(
                                    file = file,
                                    onClick = {
                                        if (file.isDirectory) {
                                            currentPath = file.path
                                        }
                                    },
                                    onRename = {
                                        selectedFile = file
                                        showRenameDialog = true
                                    },
                                    onCopy = {
                                        selectedFile = file
                                        showCopyDialog = true
                                    },
                                    onDelete = {
                                        selectedFile = file
                                        showDeleteDialog = true
                                    },
                                    onDownload = if (!file.isDirectory && file.name != "..") {
                                        {
                                            selectedFile = file
                                            showDownloadDialog = true
                                        }
                                    } else null
                                )
                            }
                            
                            // Bottom spacing
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
                
                // Pull to refresh indicator - only show when pulling or refreshing
                if (pullToRefreshState.isRefreshing || pullToRefreshState.progress > 0f) {
                    PullToRefreshContainer(
                        modifier = Modifier.align(Alignment.TopCenter),
                        state = pullToRefreshState,
                        containerColor = MaterialTheme.colorScheme.surfaceBright,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // Rename dialog
        if (showRenameDialog && selectedFile != null) {
            var newName by remember { mutableStateOf(selectedFile!!.name) }
            
            Dialog(onDismissRequest = { showRenameDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright  // Bright modal background
                    )
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "Rename",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (MaterialTheme.colorScheme.background.luminance() > 0.5) {
                                MaterialTheme.colorScheme.onBackground
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("New name") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    val parentDir = selectedFile!!.path.substringBeforeLast('/')
                                    val newPath = if (parentDir.isEmpty()) "/$newName" else "$parentDir/$newName"
                                    viewModel.renameRemoteFile(selectedFile!!.path, newPath) { result ->
                                        result.fold(
                                            onSuccess = {
                                                android.widget.Toast.makeText(context, "Renamed successfully", android.widget.Toast.LENGTH_SHORT).show()
                                                currentPath = currentPath // Trigger reload
                                            },
                                            onFailure = { error ->
                                                android.widget.Toast.makeText(context, "Failed: ${error.message}", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                    showRenameDialog = false
                                    selectedFile = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = ButtonStyles.ctaButtonShapeRounded,
                            contentPadding = ButtonStyles.ctaButtonPadding
                        ) {
                            Text("RENAME", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                        
                        TextButton(
                            onClick = { showRenameDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "CANCEL",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
        
        // Copy dialog
        if (showCopyDialog && selectedFile != null) {
            var destPath by remember { mutableStateOf(selectedFile!!.path + "_copy") }
            
            Dialog(onDismissRequest = { showCopyDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright  // Bright modal background
                    )
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "Copy",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (MaterialTheme.colorScheme.background.luminance() > 0.5) {
                                MaterialTheme.colorScheme.onBackground
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = destPath,
                            onValueChange = { destPath = it },
                            label = { Text("Destination path") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    viewModel.copyRemoteFile(selectedFile!!.path, destPath) { result ->
                                        result.fold(
                                            onSuccess = {
                                                android.widget.Toast.makeText(context, "Copied successfully", android.widget.Toast.LENGTH_SHORT).show()
                                                currentPath = currentPath // Trigger reload
                                            },
                                            onFailure = { error ->
                                                android.widget.Toast.makeText(context, "Failed: ${error.message}", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                    showCopyDialog = false
                                    selectedFile = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = ButtonStyles.ctaButtonShapeRounded,
                            contentPadding = ButtonStyles.ctaButtonPadding
                        ) {
                            Text("COPY", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                        
                        TextButton(
                            onClick = { showCopyDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "CANCEL",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
        
        // Delete dialog
        if (showDeleteDialog && selectedFile != null) {
            Dialog(onDismissRequest = { showDeleteDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright  // Bright modal background
                    )
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "Delete ${if (selectedFile!!.isDirectory) "Folder" else "File"}?",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = NavyBlue
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Are you sure you want to delete \"${selectedFile!!.name}\"? This action cannot be undone.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    viewModel.deleteRemoteFile(selectedFile!!.path, selectedFile!!.isDirectory) { result ->
                                        result.fold(
                                            onSuccess = {
                                                android.widget.Toast.makeText(context, "Deleted successfully", android.widget.Toast.LENGTH_SHORT).show()
                                                currentPath = currentPath // Trigger reload
                                            },
                                            onFailure = { error ->
                                                android.widget.Toast.makeText(context, "Failed: ${error.message}", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                    showDeleteDialog = false
                                    selectedFile = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                            shape = ButtonStyles.ctaButtonShapeRounded,
                            contentPadding = ButtonStyles.ctaButtonPadding
                        ) {
                            Text("DELETE", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        
                        TextButton(
                            onClick = { showDeleteDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "CANCEL",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
        
        // Create folder dialog
        if (showCreateFolderDialog) {
            var folderName by remember { mutableStateOf("") }
            
            Dialog(onDismissRequest = { showCreateFolderDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright  // Bright modal background
                    )
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "Create Folder",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (MaterialTheme.colorScheme.background.luminance() > 0.5) {
                                MaterialTheme.colorScheme.onBackground
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = folderName,
                            onValueChange = { folderName = it },
                            label = { Text("Folder name") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = {
                                if (folderName.isNotBlank()) {
                                    scope.launch {
                                        val newPath = if (currentPath.endsWith("/")) {
                                            "$currentPath$folderName"
                                        } else {
                                            "$currentPath/$folderName"
                                        }
                                        viewModel.createRemoteDirectory(newPath) { result ->
                                            result.fold(
                                                onSuccess = {
                                                    android.widget.Toast.makeText(context, "Folder created", android.widget.Toast.LENGTH_SHORT).show()
                                                    currentPath = currentPath // Trigger reload
                                                },
                                                onFailure = { error ->
                                                    android.widget.Toast.makeText(context, "Failed: ${error.message}", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                        showCreateFolderDialog = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = ButtonStyles.ctaButtonShapeRounded,
                            contentPadding = ButtonStyles.ctaButtonPadding
                        ) {
                            Text("CREATE", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                        
                        TextButton(
                            onClick = { showCreateFolderDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "CANCEL",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
        
        // Download to local storage dialog
        if (showDownloadDialog && selectedFile != null && !selectedFile!!.isDirectory) {
            Dialog(onDismissRequest = { 
                if (!isDownloading) {
                    showDownloadDialog = false 
                    downloadProgress = 0L
                    downloadTotal = 0L
                }
            }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright  // Bright modal background
                    )
                ) {
                    Column(modifier = Modifier.padding(28.dp)) {
                        Text(
                            text = "Copy to Android",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (MaterialTheme.colorScheme.background.luminance() > 0.5) {
                                MaterialTheme.colorScheme.onBackground
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Destination:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = destinationDisplayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            
                            TextButton(
                                onClick = { folderPickerLauncher.launch(null) },
                                enabled = !isDownloading
                            ) {
                                Icon(
                                    Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Choose")
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Progress bar
                        if (isDownloading) {
                            if (downloadTotal > 0) {
                                val progress = downloadProgress.toFloat() / downloadTotal.toFloat()
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Text(
                                    text = "${formatFileSize(downloadProgress)} / ${formatFileSize(downloadTotal)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.tertiary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Text(
                                    text = "Downloading...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        if (!isDownloading) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        isDownloading = true
                                        downloadProgress = 0L
                                        downloadTotal = 0L
                                        
                                        try {
                                            if (selectedDestinationUri != null) {
                                                // Download to selected custom folder using DocumentFile
                                                val documentTree = DocumentFile.fromTreeUri(context, selectedDestinationUri!!)
                                                if (documentTree != null && documentTree.canWrite()) {
                                                    val newFile = documentTree.createFile("*/*", selectedFile!!.name)
                                                    if (newFile != null) {
                                                        // Create a temp file first, then copy to DocumentFile
                                                        val tempFile = File.createTempFile("romnix_temp", ".tmp", context.cacheDir)
                                                        
                                                        viewModel.downloadRemoteFile(
                                                            remotePath = selectedFile!!.path,
                                                            localFile = tempFile,
                                                            onProgress = { progress, total ->
                                                                downloadProgress = progress
                                                                downloadTotal = total
                                                            }
                                                        ) { result ->
                                                            isDownloading = false
                                                            result.fold(
                                                                onSuccess = {
                                                                    // Copy temp file to selected location
                                                                    try {
                                                                        context.contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                                                                            tempFile.inputStream().use { inputStream ->
                                                                                inputStream.copyTo(outputStream)
                                                                            }
                                                                        }
                                                                        tempFile.delete() // Clean up temp file
                                                                        android.widget.Toast.makeText(context, "Downloaded to $destinationDisplayName", android.widget.Toast.LENGTH_LONG).show()
                                                                        showDownloadDialog = false
                                                                        downloadProgress = 0L
                                                                        downloadTotal = 0L
                                                                    } catch (e: Exception) {
                                                                        tempFile.delete()
                                                                        android.widget.Toast.makeText(context, "Failed to save to selected folder: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                                                        downloadProgress = 0L
                                                                        downloadTotal = 0L
                                                                    }
                                                                },
                                                                onFailure = { error ->
                                                                    tempFile.delete()
                                                                    android.widget.Toast.makeText(context, "Download failed: ${error.message}", android.widget.Toast.LENGTH_LONG).show()
                                                                    downloadProgress = 0L
                                                                    downloadTotal = 0L
                                                                }
                                                            )
                                                            selectedFile = null
                                                        }
                                                    } else {
                                                        isDownloading = false
                                                        android.widget.Toast.makeText(context, "Cannot create file in selected folder", android.widget.Toast.LENGTH_LONG).show()
                                                    }
                                                } else {
                                                    isDownloading = false
                                                    android.widget.Toast.makeText(context, "Cannot write to selected folder", android.widget.Toast.LENGTH_LONG).show()
                                                }
                                            } else {
                                                // Download to default location
                                                val downloadsDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Romnix")
                                                downloadsDir.mkdirs()
                                                val localFile = File(downloadsDir, selectedFile!!.name)
                                                
                                                viewModel.downloadRemoteFile(
                                                    remotePath = selectedFile!!.path,
                                                    localFile = localFile,
                                                    onProgress = { progress, total ->
                                                        downloadProgress = progress
                                                        downloadTotal = total
                                                    }
                                                ) { result ->
                                                    isDownloading = false
                                                    result.fold(
                                                        onSuccess = {
                                                            android.widget.Toast.makeText(context, "Downloaded to Downloads/Romnix/", android.widget.Toast.LENGTH_LONG).show()
                                                            showDownloadDialog = false
                                                            downloadProgress = 0L
                                                            downloadTotal = 0L
                                                        },
                                                        onFailure = { error ->
                                                            android.widget.Toast.makeText(context, "Download failed: ${error.message}", android.widget.Toast.LENGTH_LONG).show()
                                                            downloadProgress = 0L
                                                            downloadTotal = 0L
                                                        }
                                                    )
                                                    selectedFile = null
                                                }
                                            }
                                        } catch (e: Exception) {
                                            isDownloading = false
                                            android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                            downloadProgress = 0L
                                            downloadTotal = 0L
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                                shape = ButtonStyles.ctaButtonShapeRounded,
                                contentPadding = ButtonStyles.ctaButtonPadding
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.downloadsimple_new),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("DOWNLOAD", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                            
                            TextButton(
                                onClick = { 
                                    showDownloadDialog = false
                                    downloadProgress = 0L
                                    downloadTotal = 0L
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "CANCEL",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        } else {
                            // Show cancel button during download
                            TextButton(
                                onClick = { 
                                    // Note: In a production app, you might want to add ability to cancel download
                                    android.widget.Toast.makeText(context, "Download in progress...", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "DOWNLOADING...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeableFileItem(
    file: RemoteFile,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onDownload: (() -> Unit)? = null
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val maxSwipeDistance = -480f // Limit swipe to show three action buttons
    
    val draggableState = rememberDraggableState { delta ->
        offsetX = max(maxSwipeDistance, min(0f, offsetX + delta))
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        // Background action buttons positioned behind the card
        if (file.name != "..") { // Don't show actions for parent directory
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .matchParentSize(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    modifier = Modifier.padding(end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rename button - no background
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable {
                                onRename()
                                offsetX = 0f // Reset position
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.edit),
                            contentDescription = "Rename",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    // Copy button - no background
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable {
                                onCopy()
                                offsetX = 0f // Reset position
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    // Delete button - keep red background for destructive action
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                ButtonStyles.trashButtonBackground(),
                                ButtonStyles.trashButtonShape
                            )
                            .clickable {
                                onDelete()
                                offsetX = 0f // Reset position
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.trash),
                            contentDescription = "Delete",
                            tint = ButtonStyles.trashButtonIconTint(),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
        
        // Main file card content with draggable
        Card(
            onClick = {
                // Reset swipe state when card is tapped and execute click action
                offsetX = 0f
                onClick()
            },
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .then(
                    if (file.name != "..") {
                        Modifier.draggable(
                            state = draggableState,
                            orientation = Orientation.Horizontal,
                            onDragStopped = {
                                // Snap to position based on swipe distance
                                offsetX = if (offsetX < maxSwipeDistance / 2) {
                                    maxSwipeDistance // Snap to revealed state
                                } else {
                                    0f // Snap back to original state
                                }
                            }
                        )
                    } else Modifier
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (MaterialTheme.colorScheme.background.luminance() > 0.5) {
                    MaterialTheme.colorScheme.surfaceBright  // Bright modal background
                } else {
                    MaterialTheme.colorScheme.surfaceContainer  // Dark theme: contrasting
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                        contentDescription = null,
                        tint = if (file.isDirectory) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (MaterialTheme.colorScheme.background.luminance() > 0.5) {
                                MaterialTheme.colorScheme.onBackground
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        if (!file.isDirectory && file.name != "..") {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatFileSize(file.size),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,  // Secondary text
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                
                // Download button for files (not directories and not parent directory)
                if (!file.isDirectory && file.name != ".." && onDownload != null) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                            .clickable { onDownload() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.downloadsimple_new),
                            contentDescription = "Download",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                if (file.isDirectory && file.name != "..") {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileItem(
    file: RemoteFile,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDownload: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer  // .. folder color
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                    contentDescription = null,
                    tint = if (file.isDirectory) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,  // Standard text on surface
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (!file.isDirectory && file.name != "..") {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatFileSize(file.size),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,  // Secondary text
                            fontSize = 14.sp
                        )
                    }
                }
            }
            
            // Download button for files (not directories and not parent directory)
            if (!file.isDirectory && file.name != ".." && onDownload != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                        .clickable { onDownload() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.downloadsimple_new),
                        contentDescription = "Download",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            if (file.isDirectory && file.name != "..") {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun FileActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isDestructive) {
                ErrorRed  // Consistent red for delete in both themes
            } else {
                MaterialTheme.colorScheme.secondary  // Material 3 adaptive darker blue
            }
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Color.White
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    val gb = mb / 1024.0
    return "%.1f GB".format(gb)
}

@Composable
private fun surfaceVariantDarker(): Color {
    val colorScheme = MaterialTheme.colorScheme
    return if (colorScheme.background.luminance() > 0.5) {
        // Light theme: Slightly darker surfaceVariant
        Color(
            red = (colorScheme.surfaceVariant.red * 0.9f).coerceIn(0f, 1f),
            green = (colorScheme.surfaceVariant.green * 0.9f).coerceIn(0f, 1f),
            blue = (colorScheme.surfaceVariant.blue * 0.9f).coerceIn(0f, 1f),
            alpha = colorScheme.surfaceVariant.alpha
        )
    } else {
        // Dark theme: Lighter surfaceVariant
        Color(
            red = (colorScheme.surfaceVariant.red * 1.3f).coerceIn(0f, 1f),
            green = (colorScheme.surfaceVariant.green * 1.3f).coerceIn(0f, 1f),
            blue = (colorScheme.surfaceVariant.blue * 1.3f).coerceIn(0f, 1f),
            alpha = colorScheme.surfaceVariant.alpha
        )
    }
}

