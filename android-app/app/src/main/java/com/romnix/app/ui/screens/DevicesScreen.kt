package com.romnix.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.roundToInt
import kotlin.math.max
import kotlin.math.min
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.romnix.app.MainViewModel
import com.romnix.app.FoldernameManager
import com.romnix.app.R
import com.romnix.app.ui.theme.ButtonStyles
import com.romnix.app.ui.theme.SecondaryGray
import com.romnix.app.ui.theme.PalestBlue
import com.romnix.app.ui.theme.NavyBlue
import com.romnix.app.ui.theme.ErrorRed
import com.romnix.app.ui.theme.SuccessGreen
import com.romnix.app.ui.theme.AppWhite
import com.romnix.app.ui.theme.LightGray
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableFloatStateOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    var showConnectionDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showAddDeviceDialog by remember { mutableStateOf(false) }
    var selectedTemplate by remember { mutableStateOf<String?>(null) }
    var connectionStatus by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showFileBrowser by remember { mutableStateOf(false) }
    var browsedDeviceName by remember { mutableStateOf("") }
    
    // Only update connection status when user explicitly tests connection
    // Don't automatically show connected state based on persisted template
    
    LaunchedEffect(Unit) { 
        viewModel.loadTemplates(context) 
        viewModel.initializeFoldernames(context)
        
        // Don't automatically set connection status - let user test connection first
        // This ensures muOS template starts with "CONNECT" button by default
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
                android.widget.Toast.makeText(context, "Foldernames and devices exported successfully!", android.widget.Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
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
                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                    } else {
                        android.widget.Toast.makeText(context, "No valid data found in file", android.widget.Toast.LENGTH_LONG).show()
                    }
                } else {
                    android.widget.Toast.makeText(context, "Failed to read file", android.widget.Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Import failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
    
    if (showFileBrowser) {
        FileBrowserScreen(
            viewModel = viewModel,
            deviceName = browsedDeviceName,
            onBack = { showFileBrowser = false }
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Modern Header with Add Button and Menu
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, bottom = 24.dp, start = 20.dp, end = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Devices",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 24.sp
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Add a device to connect to it",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 16.sp
                        )
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 3-dot menu
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
                                            fontSize = 16.sp,
                                            maxLines = 1
                                        ) 
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
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
                                            fontSize = 16.sp,
                                            maxLines = 1
                                        ) 
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
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
                                            fontSize = 16.sp,
                                            maxLines = 1
                                        ) 
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    onClick = {
                                        showOverflowMenu = false
                                        showClearDialog = true
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
                        
                        // Add Button in Header - using Box to reduce overdraw
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    CircleShape
                                )
                                .clickable { showAddDeviceDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add Device",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                
                // Connection Cards Section or Empty State
                if (viewModel.deviceTemplates.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.romnix_emptystate),
                                contentDescription = "No devices",
                                modifier = Modifier.size(120.dp),
                                tint = surfaceVariantDarker()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "NO DEVICES",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Tap + to manage your files",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp) // Small spacing between cards
                    ) {
                        // Dynamic device template cards (from imports or custom additions)
                        itemsIndexed(viewModel.deviceTemplates) { index, template ->
                            val isFirst = index == 0
                            val isLast = index == viewModel.deviceTemplates.size - 1
                            
                            SwipeableConnectionCard(
                                title = template.name,
                                isFirst = isFirst,
                                isLast = isLast,
                                subtitle = null,
                                isConnected = connectionStatus[template.name] ?: false,
                                connectionStatus = connectionStatus,
                                onConnect = { 
                                    selectedTemplate = template.name
                                    showConnectionDialog = true
                                },
                                onEdit = {
                                    selectedTemplate = template.name
                                    showEditDialog = true
                                },
                                onDelete = {
                                    viewModel.removeDeviceTemplate(context, template.name)
                                    android.widget.Toast.makeText(
                                        context,
                                        "Device '${template.name}' deleted",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onBrowse = {
                                    if (viewModel.selectedHost != null) {
                                        browsedDeviceName = template.name
                                        showFileBrowser = true
                                    } else {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Please connect to device first",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                        }
                        
                        // Manual Configuration Card (with subtitle, no edit) - Hidden
                        // item {
                        //     EnhancedConnectionCard(
                        //         title = "Manual Config",
                        //         subtitle = "Set up custom connection",
                        //         isConnected = false,
                        //         showEditButton = false,
                        //         connectionStatus = connectionStatus,
                        //         onConnect = {
                        //             selectedTemplate = "Manual"
                        //             showConnectionDialog = true
                        //         },
                        //         onEdit = {}
                        //     )
                        // }
                    }
                }
            }
        
            // Connection Dialog
            if (showConnectionDialog && selectedTemplate != null) {
                ConnectionDialog(
                    templateName = selectedTemplate!!,
                    viewModel = viewModel,
                    context = context,
                    onDismiss = { showConnectionDialog = false },
                    onConnected = { templateName, success ->
                        connectionStatus = connectionStatus + (templateName to success)
                    }
                )
            }
            
            // Edit Dialog
            if (showEditDialog && selectedTemplate != null) {
                EditTemplateDialog(
                    templateName = selectedTemplate!!,
                    viewModel = viewModel,
                    context = context,
                    onDismiss = { showEditDialog = false }
                )
            }
            
            // Add Custom Device Dialog
            if (showAddDeviceDialog) {
                AddCustomDeviceDialog(
                    viewModel = viewModel,
                    context = context,
                    onDismiss = { showAddDeviceDialog = false }
                )
            }
            
            // Clear All Dialog
            if (showClearDialog) {
                Dialog(onDismissRequest = { showClearDialog = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceBright  // Standard surface for both themes
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "Delete all devices?",
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
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "This will delete all device template cards. You can add them back using the + button.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Button(
                                onClick = {
                                    viewModel.clearAllTemplates(context)
                                    showClearDialog = false
                                    android.widget.Toast.makeText(context, "All device cards deleted", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ButtonStyles.destructiveButtonBackground()
                                ),
                                shape = ButtonStyles.ctaButtonShapeRounded,
                                contentPadding = ButtonStyles.ctaButtonPadding
                            ) {
                                Text(
                                    "DELETE ALL",
                                    color = ButtonStyles.destructiveButtonText(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            TextButton(
                                onClick = { showClearDialog = false },
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
        }
    }
}

@Composable
private fun SwipeableConnectionCard(
    title: String,
    subtitle: String?,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    isConnected: Boolean,
    connectionStatus: Map<String, Boolean>,
    onConnect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onBrowse: (() -> Unit)? = null
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val maxSwipeDistance = -380f // Limit swipe to show action buttons
    
    val draggableState = rememberDraggableState { delta ->
        offsetX = max(maxSwipeDistance, min(0f, offsetX + delta))
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        // Background action buttons positioned behind the card
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
                // Edit button - use Box instead of IconButton to avoid default ripple background
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.background, // Root background color
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
                        tint = MaterialTheme.colorScheme.onBackground, // Dark icon color
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Delete button - use Box instead of IconButton to avoid default ripple background
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
        
        // Main card content with draggable
        Card(
            onClick = {
                // Reset swipe state when card is tapped
                offsetX = 0f
            },
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .draggable(
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
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceBright  // Standard surface for both themes
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
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (MaterialTheme.colorScheme.background.luminance() > 0.5) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        fontSize = 18.sp
                    )
                    
                    // Only show subtitle if provided
                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
                
                // Device icon and Connect button
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Browse Files icon (moved from left side)
                    if (onBrowse != null) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    when (connectionStatus[title]) {
                                        true -> SuccessGreen
                                        false -> MaterialTheme.colorScheme.error
                                        null -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onBrowse() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.devices),
                                contentDescription = "Browse Files",
                                modifier = Modifier.size(24.dp),
                                tint = when (connectionStatus[title]) {
                                    true, false -> Color.White  // White on both success and error backgrounds
                                    null -> MaterialTheme.colorScheme.onSurfaceVariant  // Match CONNECT button text color
                                }
                            )
                        }
                    }
                    
                    // Connect button
                    val connectionState = connectionStatus[title]
                    val (buttonColor, buttonText) = when (connectionState) {
                        true -> SuccessGreen to "ACTIVE"  // Primary theme color for success
                        false -> MaterialTheme.colorScheme.error to "FAILED"    // Red for failure
                        null -> MaterialTheme.colorScheme.surfaceVariant to "CONNECT"    // Same as device icon background
                    }
                    
                    Button(
                        onClick = onConnect,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonColor
                        ),
                        shape = ButtonStyles.ctaButtonShapeRounded,
                        contentPadding = ButtonStyles.ctaButtonPadding,
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            disabledElevation = 0.dp
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .widthIn(min = 120.dp)
                    ) {
                        Text(
                            buttonText,
                            color = if (connectionState == null) {
                                MaterialTheme.colorScheme.onSurfaceVariant  // Match text color for surfaceVariant background
                            } else {
                                Color.White  // White text on success/error states
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedConnectionCard(
    title: String,
    subtitle: String?,
    isConnected: Boolean,
    showEditButton: Boolean,
    connectionStatus: Map<String, Boolean>,
    onConnect: () -> Unit,
    onEdit: () -> Unit,
    onBrowse: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright  // Standard surface for both themes
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Browse Files icon on the left
                if (onBrowse != null && title != "Manual Configuration") {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                when (connectionStatus[title]) {
                                    true -> SuccessGreen
                                    false -> MaterialTheme.colorScheme.error
                                    null -> MaterialTheme.colorScheme.background
                                },
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onBrowse() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.devices),
                            contentDescription = "Browse Files",
                            modifier = Modifier.size(24.dp),
                            tint = when (connectionStatus[title]) {
                                true, false -> Color.White  // White on both success and error backgrounds
                                null -> MaterialTheme.colorScheme.onBackground
                            }
                        )
                    }
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (MaterialTheme.colorScheme.background.luminance() > 0.5) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        fontSize = 18.sp
                    )
                    
                    // Only show subtitle if provided
                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            
            // Connect/Configure button
            if (title != "Manual Configuration") {
                val connectionState = connectionStatus[title]
                val (buttonColor, buttonText) = when (connectionState) {
                    true -> SuccessGreen to "CONNECTED"  // Green for success to match ConnectionCard
                    false -> MaterialTheme.colorScheme.error to "FAILED"    // Red for failure
                    null -> MaterialTheme.colorScheme.background to "CONNECT"    // Same as device icon background
                }
                
                Button(
                    onClick = onConnect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor
                    ),
                    shape = ButtonStyles.ctaButtonShapeRounded,
                    contentPadding = ButtonStyles.ctaButtonPadding,
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        disabledElevation = 0.dp
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .widthIn(min = 120.dp)
                ) {
                    Text(
                        buttonText,
                        color = if (connectionState == null) {
                            MaterialTheme.colorScheme.onBackground  // Match text color for background
                        } else {
                            Color.White  // White text on success/error states
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            } else {
                Button(
                    onClick = onConnect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer  // Secondary container color
                    ),
                    shape = ButtonStyles.ctaButtonShapeRounded,
                    contentPadding = ButtonStyles.ctaButtonPadding,
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        disabledElevation = 0.dp
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .widthIn(min = 120.dp)
                ) {
                    Text(
                        "CONNECT",
                        color = MaterialTheme.colorScheme.onSecondaryContainer,  // onSecondaryContainer text
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConnectionDialog(
    templateName: String,
    viewModel: MainViewModel,
    context: android.content.Context,
    onDismiss: () -> Unit,
    onConnected: (String, Boolean) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var remotePath by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var hostIp by remember { mutableStateOf("") }
    
    // Load template values from deviceTemplates list
    LaunchedEffect(templateName) {
        val template = viewModel.deviceTemplates.find { t -> t.name == templateName }
        if (template != null) {
            username = template.username
            password = template.password
            remotePath = template.remoteBasePath
            port = template.port
            hostIp = template.hostIp
        } else if (templateName == "Manual") {
            username = "root"
            password = "root"
            remotePath = "mnt/mmc/Roms"
            port = "22"
            hostIp = ""
        }
    }
    
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
                    text = templateName.uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (MaterialTheme.colorScheme.background.luminance() > 0.5) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    fontSize = 18.sp
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Username Field
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    placeholder = { 
                        Text(
                            "Username",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ) 
                    },
                    trailingIcon = if (username.isNotEmpty()) {
                        {
                            IconButton(onClick = { username = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear username",
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
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    placeholder = { 
                        Text(
                            "Password",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ) 
                    },
                    trailingIcon = if (password.isNotEmpty()) {
                        {
                            IconButton(onClick = { password = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear password",
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
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Host IP Field
                OutlinedTextField(
                    value = hostIp,
                    onValueChange = { hostIp = it },
                    label = { Text("Host IP") },
                    placeholder = { 
                        Text(
                            "192.168.0.102",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ) 
                    },
                    trailingIcon = if (hostIp.isNotEmpty()) {
                        {
                            IconButton(onClick = { hostIp = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear host IP",
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
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Remote Path Field
                    OutlinedTextField(
                        value = remotePath,
                        onValueChange = { remotePath = it },
                        label = { Text("Remote path") },
                        placeholder = { 
                            Text(
                                "",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ) 
                        },
                        trailingIcon = if (remotePath.isNotEmpty()) {
                            {
                                IconButton(onClick = { remotePath = "" }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear remote path",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        } else null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,  // Bright white when focused (matches reference)
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,        // Gray when unfocused
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Port Field
                OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("Port") },
                        placeholder = { 
                            Text(
                                "22",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ) 
                        },
                        trailingIcon = if (port.isNotEmpty()) {
                            {
                                IconButton(onClick = { port = "" }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear port",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        } else null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,  // Bright white when focused (matches reference)
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,        // Gray when unfocused
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Scan Network Button
                Button(
                    onClick = {
                        // Apply the template using the deviceTemplates list
                        val existingTemplate = viewModel.deviceTemplates.find { t -> t.name == templateName }
                        
                        if (existingTemplate != null) {
                            // Update template with edited values from dialog
                            val updatedTemplate = existingTemplate.copy(
                                username = username,
                                password = password,
                                hostIp = hostIp,
                                port = port,
                                remoteBasePath = remotePath
                            )
                            // Save the updated template
                            viewModel.updateDeviceTemplate(context, updatedTemplate)
                            // Apply the updated template
                            viewModel.applyDeviceTemplate(context, updatedTemplate)
                            // Don't scan network - we already have the specific IP from the template
                            
                            // Test actual SSH connection using the edited values
                            if (hostIp.isNotEmpty()) {
                                viewModel.testConnection(
                                    hostIp = hostIp,
                                    username = username,
                                    password = password,
                                    port = port.toIntOrNull() ?: 22,
                                    onResult = { success ->
                                        onConnected(templateName, success)
                                    }
                                )
                            } else {
                                onConnected(templateName, false)
                            }
                        } else if (templateName == "Manual") {
                            // Save manual settings using edited values
                            viewModel.sshUsername = username
                            viewModel.sshPassword = password
                            viewModel.sshPort = port
                            viewModel.remoteBasePath = remotePath
                            viewModel.updateConnectedTemplate("Manual")
                            viewModel.saveManualSettings(context)
                            // Add host if IP is provided
                            if (hostIp.isNotEmpty()) {
                                viewModel.addHostFromIp(hostIp)
                                onConnected(templateName, true)
                            } else {
                                // For manual config without IP, scan network to find available hosts
                                viewModel.scanHosts()
                                onConnected(templateName, true)
                            }
                        }
                        
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = ButtonStyles.ctaButtonShapeRounded,
                    contentPadding = ButtonStyles.ctaButtonPadding
                ) {
                    Text(
                        "SCAN NETWORK",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Cancel Button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "CANCEL",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTemplateDialog(
    templateName: String,
    viewModel: MainViewModel,
    context: android.content.Context,
    onDismiss: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var remotePath by remember { mutableStateOf("") }
    var hostIp by remember { mutableStateOf("") }
    
    // Load current template values
    LaunchedEffect(templateName) {
        val template = viewModel.deviceTemplates.find { t -> t.name == templateName }
        if (template != null) {
            username = template.username
            password = template.password
            remotePath = template.remoteBasePath
            hostIp = template.hostIp
        }
    }
    
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
                    text = "Edit ${templateName.lowercase().replaceFirstChar { it.uppercase() }}",
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
                
                Spacer(modifier = Modifier.height(20.dp))
                
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    placeholder = { 
                        Text(
                            "Username",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ) 
                    },
                    trailingIcon = if (username.isNotEmpty()) {
                        {
                            IconButton(onClick = { username = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear username",
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
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    placeholder = { 
                        Text(
                            "Password",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ) 
                    },
                    trailingIcon = if (password.isNotEmpty()) {
                        {
                            IconButton(onClick = { password = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear password",
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
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = hostIp,
                    onValueChange = { hostIp = it },
                    label = { Text("Host IP") },
                    placeholder = { 
                        Text(
                            "192.168.1.100",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ) 
                    },
                    trailingIcon = if (hostIp.isNotEmpty()) {
                        {
                            IconButton(onClick = { hostIp = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear host IP",
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
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = remotePath,
                    onValueChange = { remotePath = it },
                    label = { Text("Remote Path") },
                    placeholder = { 
                        Text(
                            "/storage/roms",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ) 
                    },
                    trailingIcon = if (remotePath.isNotEmpty()) {
                        {
                            IconButton(onClick = { remotePath = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear remote path",
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
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        val existingTemplate = viewModel.deviceTemplates.find { t -> t.name == templateName }
                        if (existingTemplate != null) {
                            val updatedTemplate = existingTemplate.copy(
                                username = username,
                                password = password,
                                hostIp = hostIp,
                                remoteBasePath = remotePath
                            )
                            viewModel.updateDeviceTemplate(context, updatedTemplate)
                        }
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = ButtonStyles.ctaButtonShapeRounded,
                    contentPadding = ButtonStyles.ctaButtonPadding
                ) {
                    Text(
                        "SAVE", 
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCustomDeviceDialog(
    viewModel: MainViewModel,
    context: android.content.Context,
    onDismiss: () -> Unit
) {
    var deviceName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var hostIp by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("22") }
    var remotePath by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(2.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceBright  // Standard modal background
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Add your device",
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
                    text = "Transfer files directly to your devices, make sure SSH is open. ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Device Name Field
                OutlinedTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("Device Name") },
                    placeholder = { 
                        Text(
                            "My Custom Device",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ) 
                    },
                    trailingIcon = if (deviceName.isNotEmpty()) {
                        {
                            IconButton(onClick = { deviceName = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear device name",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,  // Bright white when focused (matches Port field)
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,  // Clean gray fill (matches Port field)
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Username Field
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    trailingIcon = if (username.isNotEmpty()) {
                        {
                            IconButton(onClick = { username = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear username",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,  // Bright white when focused (matches Port field)
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,  // Clean gray fill (matches Port field)
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    trailingIcon = if (password.isNotEmpty()) {
                        {
                            IconButton(onClick = { password = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear password",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,  // Bright white when focused (matches Port field)
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,  // Clean gray fill (matches Port field)
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Host IP Field
                OutlinedTextField(
                    value = hostIp,
                    onValueChange = { hostIp = it },
                    label = { Text("Host IP") },
                    placeholder = { Text("") },
                    trailingIcon = if (hostIp.isNotEmpty()) {
                        {
                            IconButton(onClick = { hostIp = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear host IP",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,  // Bright white when focused (matches Port field)
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,  // Clean gray fill (matches Port field)
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Port and Remote Path Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("Port") },
                        modifier = Modifier.weight(0.4f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,  // Bright white when focused (matches reference)
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,        // Gray when unfocused
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Remote Path Field
                OutlinedTextField(
                    value = remotePath,
                    onValueChange = { remotePath = it },
                    label = { Text("Remote Path") },
                    placeholder = { 
                        Text(
                            "",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ) 
                    },
                    trailingIcon = if (remotePath.isNotEmpty()) {
                        {
                            IconButton(onClick = { remotePath = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear remote path",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,  // Bright white when focused (matches Port field)
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,  // Clean gray fill (matches Port field)
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        if (deviceName.isNotBlank()) {
                            viewModel.addCustomDeviceTemplate(
                                context = context,
                                name = deviceName,
                                username = username,
                                password = password,
                                hostIp = hostIp,
                                port = port,
                                remotePath = remotePath
                            )
                            android.widget.Toast.makeText(context, "Device added!", android.widget.Toast.LENGTH_SHORT).show()
                            onDismiss()
                        } else {
                            android.widget.Toast.makeText(context, "Please enter device name", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = ButtonStyles.ctaButtonShapeRounded,
                    contentPadding = ButtonStyles.ctaButtonPadding
                ) {
                    Text(
                        "ADD DEVICE", 
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "CANCEL",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
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
            red = (colorScheme.surfaceVariant.red * 1.3f).coerceIn(0f, 1f),
            green = (colorScheme.surfaceVariant.green * 1.3f).coerceIn(0f, 1f),
            blue = (colorScheme.surfaceVariant.blue * 1.3f).coerceIn(0f, 1f),
            alpha = colorScheme.surfaceVariant.alpha
        )
    }
}
