package com.romnix.app.ui.screens

import android.app.DownloadManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.romnix.app.*
import com.romnix.app.R
import com.romnix.app.ui.theme.ButtonStyles
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// Import the ShapeMorphingLoader from ConsoleSelectScreen
import com.romnix.app.ui.screens.ShapeMorphingLoader

@Composable
fun BrowseScreen(
    viewModel: MainViewModel,
    downloader: Downloader,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedFoldername by remember { mutableStateOf<Foldername?>(null) }
    var showDownloadSettings by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingFoldername by remember { mutableStateOf<Foldername?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    
    // Clear selection when screen is first shown
    LaunchedEffect(Unit) {
        viewModel.setFoldername(null)
        viewModel.clearResults()
        selectedFoldername = null
    }
    
    // Force recomposition when platforms change
    LaunchedEffect(refreshTrigger) {
        FoldernameManager.loadCustomFoldernames(context)
    }
    
    // Update selected platform when viewModel changes
    LaunchedEffect(viewModel.selectedFoldername) {
        selectedFoldername = viewModel.selectedFoldername
    }
    
    // File picker launcher for export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            try {
                val foldernamesData = FoldernameManager.exportFoldernames()
                val devicesData = viewModel.exportDevices()
                val exportData = foldernamesData + "\n" + devicesData
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(exportData.toByteArray())
                }
                Toast.makeText(context, "Foldernames and devices exported successfully!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    // File picker launcher for import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                val fileContent = context.contentResolver.openInputStream(it)?.use { inputStream ->
                    inputStream.bufferedReader().readText()
                }
                if (fileContent != null) {
                    val foldernamesCount = FoldernameManager.importFoldernames(context, fileContent)
                    val devicesCount = viewModel.importDevices(context, fileContent)
                    val totalCount = foldernamesCount + devicesCount
                    
                    if (totalCount > 0) {
                        val message = buildString {
                            append("Import successful! ")
                            if (foldernamesCount > 0) append("$foldernamesCount foldernames ")
                            if (devicesCount > 0) {
                                if (foldernamesCount > 0) append("and ")
                                append("$devicesCount devices")
                            }
                        }
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        refreshTrigger++
                    } else {
                        Toast.makeText(context, "No valid data found in file", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "Failed to read file", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if (selectedFoldername != null) {
            // Back Button Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 8.dp, end = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                        onClick = {
                            selectedFoldername = null
                            viewModel.setFoldername(null)
                            viewModel.clearResults()
                        },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                    }
                }
            }
            
            // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                    .padding(top = if (selectedFoldername != null) 8.dp else 48.dp, bottom = 24.dp, start = 20.dp, end = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (selectedFoldername != null) selectedFoldername!!.label else "Browse files",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = if (selectedFoldername != null) 32.sp else 24.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = if (selectedFoldername != null) {
                            "${viewModel.results.size} files available"
                        } else if (FoldernameManager.all.isEmpty()) {
                            "Content auto-loads in folder"
                        } else {
                            "Select folder names you added"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 16.sp
                    )
                }
                
                if (selectedFoldername != null) {
                // Download Settings Button
                IconButton(
                    onClick = { showDownloadSettings = true },
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.folder),
                        contentDescription = "Download Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    // Overflow menu for folder management
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            IconButton(
                                onClick = { showOverflowMenu = true },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "More options",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceBright
                                    ),
                                properties = androidx.compose.ui.window.PopupProperties(
                                    focusable = true,
                                    dismissOnBackPress = true,
                                    dismissOnClickOutside = true
                                )
                            ) {
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            "Export data",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 16.sp
                                        ) 
                                    },
                                    onClick = {
                                        val fileName = "rom_data_${java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())}.txt"
                                        exportLauncher.launch(fileName)
                                        showOverflowMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.export),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            "Import data",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 16.sp
                                        ) 
                                    },
                                    onClick = {
                                        importLauncher.launch(arrayOf("text/plain", "text/*", "*/*"))
                                        showOverflowMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.import_icon),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            "Delete All",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 16.sp
                                        ) 
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        FoldernameManager.clearAllCustomFoldernames(context)
                                        refreshTrigger++
                                        Toast.makeText(context, "All foldernames deleted", Toast.LENGTH_SHORT).show()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.trash),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                )
                            }
                        }
                        
                        // Add Button
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    CircleShape
                                )
                                .clickable { showAddDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add Foldername",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                
                if (showDownloadSettings) {
                    com.romnix.app.ui.components.DownloadSettingsDialog(
                        onDismiss = { showDownloadSettings = false }
                    )
                }
            }
            
            // Only show search section when a folder is selected and has content
            if (selectedFoldername != null) {
            // Search Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Search Field
                OutlinedTextField(
                    value = viewModel.searchTerm,
                    onValueChange = { viewModel.updateSearchTerm(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { 
                        Text(
                                "Search in ${selectedFoldername!!.label}...",
                            fontWeight = FontWeight.Medium
                        ) 
                    },
                    trailingIcon = if (viewModel.searchTerm.isNotEmpty()) {
                        {
                            IconButton(onClick = { 
                                viewModel.updateSearchTerm("")
                                viewModel.clearResults()
                            }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.surfaceTint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else null,
                    colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = surfaceVariantDarker(),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                    
                // Search Button (Circular) - Always shows magnifying glass
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                        )
                        .clickable {
                                if (viewModel.searchTerm.isNotEmpty()) {
                                viewModel.searchSelected()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                            imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Content Area
            if (selectedFoldername == null) {
                // Show folder list when no folder is selected
                if (FoldernameManager.all.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.romnix_emptystate),
                                contentDescription = null,
                                modifier = Modifier.size(120.dp),
                                tint = surfaceVariantDarker()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "NO SOURCES",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(2.dp))
                            
                            Text(
                                text = "Tap + to add a folder",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Show folder list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp) // No spacing for continuous cards
                    ) {
                        itemsIndexed(FoldernameManager.all) { index, foldername ->
                            val isFirst = index == 0
                            val isLast = index == FoldernameManager.all.size - 1
                            
                            FoldernameCard(
                                foldername = foldername,
                                isFirst = isFirst,
                                isLast = isLast,
                                onSelect = {
                                    selectedFoldername = foldername
                                    viewModel.setFoldername(foldername)
                                    // Auto search when folder is selected
                                    viewModel.searchSelected()
                                },
                                onEdit = {
                                    editingFoldername = foldername
                                },
                                onDelete = {
                                    FoldernameManager.removeCustomFoldername(context, foldername.id)
                                    refreshTrigger++
                                    Toast.makeText(context, "${foldername.label} deleted", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            } else {
                // Show search results when folder is selected
            if (viewModel.isSearching) {
                    // Loading State
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        ShapeMorphingLoader(
                            color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(55.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        AnimatedLoadingText(
                                baseText = "Loading ${selectedFoldername!!.label} files"
                        )
                    }
                }
            } else if (viewModel.results.isEmpty()) {
                // Empty State (no results)
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.romnix_emptystate),
                            contentDescription = null,
                            modifier = Modifier.size(120.dp),
                                tint = surfaceVariantDarker()
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = if (viewModel.searchTerm.isNotEmpty()) {
                                "No results for \"${viewModel.searchTerm}\""
                            } else {
                                    "No files found in ${selectedFoldername!!.label}"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = if (viewModel.searchTerm.isNotEmpty()) {
                                "Try a different search term"
                            } else {
                                "Check the folder path in ${selectedFoldername!!.label}"
                            },
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Column {
                    // Bulk Download Card (when multiple results)
                    if (viewModel.results.size > 1) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                                .padding(horizontal = 20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (MaterialTheme.colorScheme.background.luminance() > 0.5) {
                                        MaterialTheme.colorScheme.surfaceBright
                } else {
                                        MaterialTheme.colorScheme.surfaceContainer
                }
            ),
            shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                        text = "Bulk Download",
                                        style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 16.sp
                            )
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                            Text(
                                        text = "${viewModel.results.size} files found",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }
                                
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Bulk Download Button (Transparent)
                                    IconButton(
                                        onClick = {
                                            viewModel.results.forEach { rom ->
                                                downloader.download(context, rom)
                                            }
                                            Toast.makeText(context, "Downloading ${viewModel.results.size} files...", Toast.LENGTH_LONG).show()
                                        },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.downloadsimple_new),
                                            contentDescription = "Bulk Download",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    
                                        // Bulk Transfer Button
                                    if (viewModel.selectedHost != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .background(
                                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    viewModel.downloadAndTransferAll(context, downloader,
                                                            onProgress = { _, _, _ -> },
                                                        onComplete = { success, failed ->
                                                            Toast.makeText(context, "Bulk transfer complete: $success success, $failed failed", Toast.LENGTH_LONG).show()
                                                        }
                                                    )
                                                    Toast.makeText(context, "Starting bulk transfer of ${viewModel.results.size} files...", Toast.LENGTH_LONG).show()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.arrowsleftright),
                                                contentDescription = "Bulk Transfer",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                }
            }
        }
        
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    // ROM List
            LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp) // No spacing for continuous cards
            ) {
                itemsIndexed(viewModel.results) { index, rom ->
                    val isFirst = index == 0
                    val isLast = index == viewModel.results.size - 1
                    
                                RomCard(
                                rom = rom,
                        isFirst = isFirst,
                        isLast = isLast,
                        onDownload = {
                            downloader.download(context, rom)
                                },
                                    onTransfer = if (viewModel.selectedHost != null) {
                                        {
                                        viewModel.downloadAndTransfer(context, downloader, rom) { success, message ->
                                                val msg = if (success) message ?: "Transfer completed" else "Transfer failed: ${message ?: "unknown error"}"
                                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    } else null
                                )
                            }
                        }
                        }
                    }
                }
            }
        }
    
    // Add Foldername Modal
    if (showAddDialog) {
        AddFoldernameModal(
            onDismiss = { showAddDialog = false },
            onSave = { platform ->
                FoldernameManager.addCustomFoldername(context, platform)
                showAddDialog = false
                refreshTrigger++
                Toast.makeText(
                    context, 
                    "${platform.label} added successfully", 
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
    
    // Edit Foldername Modal
    if (editingFoldername != null) {
        AddFoldernameModal(
            foldername = editingFoldername,
            onDismiss = { editingFoldername = null },
            onSave = { platform ->
                FoldernameManager.updateCustomFoldername(context, platform)
                editingFoldername = null
                refreshTrigger++
                Toast.makeText(
                    context,
                    "${platform.label} updated successfully",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
}

@Composable
private fun FoldernameCard(
    foldername: Foldername,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val maxSwipeDistance = if (foldername.isCustom) -360f else 0f // Only swipeable if custom
    
    val draggableState = rememberDraggableState { delta ->
        if (foldername.isCustom) {
            offsetX = kotlin.math.max(maxSwipeDistance, kotlin.math.min(0f, offsetX + delta))
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        // Background action buttons (only for custom foldernames)
        if (foldername.isCustom) {
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
                    // Edit button with transparent background
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                onEdit()
                                offsetX = 0f // Reset position
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.edit),
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    // Delete button with red background
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
        
        // Main card content with draggable
    Card(
            onClick = {
                // Only select if not swiped
                if (offsetX > -10f) {
                    onSelect()
                }
                offsetX = 0f // Reset position
            },
        modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .then(
                    if (foldername.isCustom) {
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
                    } else {
                        Modifier
                    }
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceBright
            ),
            shape = when {
                isFirst && isLast -> RoundedCornerShape(28.dp) // Single card
                isFirst -> RoundedCornerShape(
                    topStart = 28.dp,
                    topEnd = 28.dp,
                    bottomStart = 0.dp,
                    bottomEnd = 0.dp
                )
                isLast -> RoundedCornerShape(
                    topStart = 0.dp,
                    topEnd = 0.dp,
                    bottomStart = 28.dp,
                    bottomEnd = 28.dp
                )
                else -> RoundedCornerShape(0.dp) // Middle cards
            },
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
                Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = foldername.label,
                        style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (MaterialTheme.colorScheme.background.luminance() > 0.5) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                        fontSize = 18.sp
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = foldername.archiveUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                }
                
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Navigate",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun RomCard(
    rom: RomItem,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    onDownload: () -> Unit,
    onTransfer: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        ),
        shape = when {
            isFirst && isLast -> RoundedCornerShape(28.dp) // Single card
            isFirst -> RoundedCornerShape(
                topStart = 28.dp,
                topEnd = 28.dp,
                bottomStart = 0.dp,
                bottomEnd = 0.dp
            )
            isLast -> RoundedCornerShape(
                topStart = 0.dp,
                topEnd = 0.dp,
                bottomStart = 28.dp,
                bottomEnd = 28.dp
            )
            else -> RoundedCornerShape(0.dp) // Middle cards
        },
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rom.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Download Button
                IconButton(
                    onClick = onDownload,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.downloadsimple_new),
                        contentDescription = "Download",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Transfer Button
                if (onTransfer != null) {
                Box(
                    modifier = Modifier
                            .size(36.dp)
                        .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onTransfer() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.arrowsleftright),
                        contentDescription = "Transfer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                    )
                    }
                }
            }
        }
    }
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
            red = (colorScheme.surfaceVariant.red * 1.15f).coerceIn(0f, 1f),
            green = (colorScheme.surfaceVariant.green * 1.15f).coerceIn(0f, 1f),
            blue = (colorScheme.surfaceVariant.blue * 1.15f).coerceIn(0f, 1f),
            alpha = colorScheme.surfaceVariant.alpha
        )
    }
}

@Composable
private fun AnimatedLoadingText(baseText: String) {
    var dots by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        while (true) {
            dots = when (dots.length) {
                0 -> "."
                1 -> ".."
                2 -> "..."
                else -> ""
            }
            delay(500)
        }
    }
    
    Text(
        text = "$baseText$dots",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFoldernameModal(
    foldername: Foldername? = null,
    onDismiss: () -> Unit,
    onSave: (Foldername) -> Unit
) {
    var platformName by remember { mutableStateOf(foldername?.label ?: "") }
    var archiveUrl by remember { mutableStateOf(foldername?.archiveUrl ?: "") }
    var extensions by remember { mutableStateOf(foldername?.extensions?.joinToString(", ") ?: "zip, 7z, png, pdf") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceBright  // Standard modal background
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = if (foldername != null) "Edit your source" else "Add your source",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (MaterialTheme.colorScheme.background.luminance() > 0.5) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    fontSize = 20.sp,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Choose where to get files, we'll group them for you.",
                    style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 16.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = platformName,
                    onValueChange = { platformName = it },
                    label = { 
                        Text(
                            "Folder name",
                            fontWeight = FontWeight.Medium
                        ) 
                    },
                    trailingIcon = if (platformName.isNotEmpty()) {
                        {
                            IconButton(onClick = { platformName = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear foldername",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else null,
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
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = archiveUrl,
                    onValueChange = { archiveUrl = it },
                    label = { 
                        Text(
                            "Type your URL",
                            fontWeight = FontWeight.Medium
                        ) 
                    },
                    trailingIcon = if (archiveUrl.isNotEmpty()) {
                        {
                            IconButton(onClick = { archiveUrl = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear archive URL",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else null,
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
                    shape = RoundedCornerShape(8.dp),
                    maxLines = 2
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = extensions,
                    onValueChange = { extensions = it },
                    label = { 
                        Text(
                            "File extensions",
                            fontWeight = FontWeight.Medium
                        ) 
                    },
                    trailingIcon = if (extensions.isNotEmpty()) {
                        {
                            IconButton(onClick = { extensions = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear file extensions",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else null,
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
                    shape = RoundedCornerShape(8.dp),
                    maxLines = 2
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        if (platformName.isNotBlank() && archiveUrl.isNotBlank()) {
                            val extensionsList = extensions.split(",").map { it.trim() }
                            onSave(
                                Foldername(
                                    id = foldername?.id ?: platformName.lowercase().replace(" ", ""),
                                    label = platformName,
                                    archiveUrl = archiveUrl,
                                    extensions = extensionsList,
                                    isCustom = true
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = ButtonStyles.ctaButtonShape,
                    contentPadding = ButtonStyles.ctaButtonPadding
                ) {
                    Text("SAVE", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
                
                TextButton(
                    onClick = onDismiss,
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