package com.romnix.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.PopupProperties
import com.romnix.app.MainViewModel
import com.romnix.app.R
import com.romnix.app.ui.theme.ButtonStyles
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

data class TimeSession(
    val id: String = UUID.randomUUID().toString(),
    val startTime: Long,
    val endTime: Long? = null
) {
    val duration: Long
        get() = if (endTime != null) endTime - startTime else 0L
    
    val durationSeconds: Int
        get() = (duration / 1000).toInt()
}

data class BacklogItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val sessions: List<TimeSession> = emptyList(),
    val currentSession: TimeSession? = null // Active session if any
) {
    val totalPlayedSeconds: Int
        get() = sessions.sumOf { it.durationSeconds }
    
    val isTracking: Boolean
        get() = currentSession != null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BacklogScreen(viewModel: MainViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val backlogItems by remember { derivedStateOf { viewModel.backlogItems } }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<BacklogItem?>(null) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    
    // File picker launcher for export
    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            try {
                val exportData = exportBacklogData(backlogItems)
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(exportData.toByteArray())
                }
                android.widget.Toast.makeText(context, "Backlog data exported successfully!", android.widget.Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
    
    // File picker launcher for import
    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                val fileContent = context.contentResolver.openInputStream(it)?.use { inputStream ->
                    inputStream.bufferedReader().readText()
                }
                if (fileContent != null) {
                    val importedItems = parseBacklogImport(fileContent)
                    if (importedItems.isNotEmpty()) {
                        val existingTitles = backlogItems.map { item -> item.title.lowercase() }.toSet()
                        val newItems = importedItems.filter { item -> 
                            !existingTitles.contains(item.title.lowercase())
                        }
                        val mergedItems = backlogItems + newItems
                        viewModel.updateBacklogItems(mergedItems)
                        android.widget.Toast.makeText(
                            context, 
                            "Imported ${newItems.size} new goals", 
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    } else {
                        android.widget.Toast.makeText(context, "No valid backlog data found in file", android.widget.Toast.LENGTH_LONG).show()
                    }
                } else {
                    android.widget.Toast.makeText(context, "Failed to read file", android.widget.Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Import failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with title and add button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, bottom = 24.dp, start = 20.dp, end = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Goals backlog",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 24.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Track your time sessions",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Overflow menu button
                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true }
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                            modifier = Modifier
                                .clip(RoundedCornerShape(28.dp))
                                .background(color = MaterialTheme.colorScheme.surfaceBright),
                            properties = PopupProperties(
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
                                        fontSize = 14.sp
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    val fileName = "time_tracker_${dateFormat.format(Date())}.txt"
                                    exportLauncher.launch(fileName)
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
                                        fontSize = 14.sp
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    importLauncher.launch(arrayOf("text/plain"))
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
                                        "Clear All",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                },
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
                    
                    // Add button
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
                            contentDescription = "Add goal",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            
            // Content
            if (backlogItems.isEmpty()) {
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
                            text = "NO GOALS, YET",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Tap + to track your time",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                // Item list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp)
                ) {
                    itemsIndexed(backlogItems) { index, item ->
                        val isFirst = index == 0
                        val isLast = index == backlogItems.size - 1
                        
                        BacklogItemCard(
                            item = item,
                            isFirst = isFirst,
                            isLast = isLast,
                            onToggleTracking = {
                                val currentItems = viewModel.backlogItems
                                val updatedItems = currentItems.map { backlogItem ->
                                    if (backlogItem.id == item.id) {
                                        if (item.isTracking) {
                                            // Stop tracking - end current session
                                            val endedSession = item.currentSession?.copy(
                                                endTime = System.currentTimeMillis()
                                            )
                                            if (endedSession != null) {
                                                backlogItem.copy(
                                                    sessions = backlogItem.sessions + endedSession,
                                                    currentSession = null
                                                )
                                            } else {
                                                backlogItem
                                            }
                                        } else {
                                            // Start tracking - create new session
                                            backlogItem.copy(
                                                currentSession = TimeSession(
                                                    startTime = System.currentTimeMillis()
                                                )
                                            )
                                        }
                                    } else {
                                        // Stop any other tracking items
                                        if (backlogItem.isTracking && !item.isTracking) {
                                            val endedSession = backlogItem.currentSession?.copy(
                                                endTime = System.currentTimeMillis()
                                            )
                                            if (endedSession != null) {
                                                backlogItem.copy(
                                                    sessions = backlogItem.sessions + endedSession,
                                                    currentSession = null
                                                )
                                            } else {
                                                backlogItem
                                            }
                                        } else {
                                            backlogItem
                                        }
                                    }
                                }
                                viewModel.updateBacklogItems(updatedItems)
                            },
                            onEdit = {
                                editingItem = item
                                showAddDialog = true
                            },
                            onDelete = {
                                val currentItems = viewModel.backlogItems
                                val updatedItems = currentItems.filter { it.id != item.id }
                                viewModel.updateBacklogItems(updatedItems)
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Add/Edit Dialog
    if (showAddDialog) {
        AddEditBacklogDialog(
            item = editingItem,
            onDismiss = {
                showAddDialog = false
                editingItem = null
            },
            onSave = { title ->
                val currentItems = viewModel.backlogItems
                if (editingItem != null) {
                    val updatedItems = currentItems.map { 
                        if (it.id == editingItem!!.id) {
                            it.copy(title = title)
                        } else it
                    }
                    viewModel.updateBacklogItems(updatedItems)
                } else {
                    val newItem = BacklogItem(title = title)
                    val updatedItems = currentItems + newItem
                    viewModel.updateBacklogItems(updatedItems)
                }
                showAddDialog = false
                editingItem = null
            }
        )
    }
    
    // Clear All Confirmation Dialog
    if (showClearDialog) {
        Dialog(onDismissRequest = { showClearDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (MaterialTheme.colorScheme.background.luminance() > 0.5) {
                        MaterialTheme.colorScheme.surfaceVariant  // Light theme: lighter modal
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant  // Dark theme: elevated surface
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Clear all goals?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        letterSpacing = 1.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "This will delete all goals and their time sessions. This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            viewModel.updateBacklogItems(emptyList())
                            showClearDialog = false
                            android.widget.Toast.makeText(
                                context,
                                "All goals cleared",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ButtonStyles.destructiveButtonBackground()
                        ),
                        shape = ButtonStyles.ctaButtonShape,
                        contentPadding = ButtonStyles.ctaButtonPadding
                    ) {
                        Text(
                            text = "CLEAR ALL",
                            color = ButtonStyles.destructiveButtonText(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TextButton(
                        onClick = { showClearDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "CANCEL",
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

@Composable
fun BacklogItemCard(
    item: BacklogItem,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    onToggleTracking: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val density = LocalDensity.current
    var offsetX by remember { mutableFloatStateOf(0f) }
    val maxSwipeDistance = with(density) { 120.dp.toPx() }
    var isExpanded by remember { mutableStateOf(false) }
    
    val draggableState = rememberDraggableState { delta ->
        offsetX = (offsetX + delta).coerceIn(-maxSwipeDistance, 0f)
    }
    
    Box(modifier = Modifier.fillMaxWidth()) {
        // Background actions (Edit and Delete)
        if (offsetX < 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(end = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Edit button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            onEdit()
                            offsetX = 0f
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
                
                // Delete button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            ButtonStyles.trashButtonBackground(),
                            ButtonStyles.trashButtonShape
                        )
                        .clickable {
                            onDelete()
                            offsetX = 0f
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
        
        // Main card content
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = draggableState
                ),
            shape = when {
                isFirst && isLast -> RoundedCornerShape(28.dp)
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
                else -> RoundedCornerShape(0.dp)
            },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceBright
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Title and play button row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Play/Stop button
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (item.isTracking) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.tertiaryContainer,
                                CircleShape
                            )
                            .clickable { onToggleTracking() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (item.isTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (item.isTracking) "Stop" else "Start",
                            tint = if (item.isTracking) MaterialTheme.colorScheme.onError
                            else MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Current session (if tracking)
                if (item.isTracking && item.currentSession != null) {
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Tracking since ${timeFormat.format(Date(item.currentSession.startTime))}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Always show total time at the top if there are sessions
                if (item.sessions.isNotEmpty() || item.totalPlayedSeconds > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total time tracked",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formatTotalTime(item.totalPlayedSeconds),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Sessions list (collapsible)
                if (item.sessions.isNotEmpty()) {
                    // Group sessions by date
                    val sessionsByDate = item.sessions.groupBy { session ->
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(session.startTime))
                    }.toSortedMap(compareByDescending { it })
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Expandable header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isExpanded = !isExpanded }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${item.sessions.size} sessions",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        // Show sessions when expanded
                        if (isExpanded) {
                            sessionsByDate.forEach { (date, sessions) ->
                                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                val dateObj = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)
                                
                                // Date header
                                Text(
                                    text = dateFormat.format(dateObj ?: Date()),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                                
                                // Sessions for this date
                                sessions.sortedByDescending { it.startTime }.forEach { session ->
                                    SessionRow(session)
                                }
                                
                                // Daily total
                                val dailyTotal = sessions.sumOf { it.durationSeconds }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = "Day total: ${formatDuration(dailyTotal)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        } else {
                            // Show summary when collapsed
                            val recentSessions = item.sessions.takeLast(3)
                            recentSessions.forEach { session ->
                                SessionRow(session)
                            }
                            if (item.sessions.size > 3) {
                                Text(
                                    text = "... and ${item.sessions.size - 3} more sessions",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(top = 4.dp, start = 12.dp)
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
fun SessionRow(session: TimeSession) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "${dateFormat.format(Date(session.startTime))} • ${timeFormat.format(Date(session.startTime))} - ${
                if (session.endTime != null) timeFormat.format(Date(session.endTime)) else "..."
            }",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = formatDuration(session.durationSeconds),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBacklogDialog(
    item: BacklogItem?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var titleText by remember { mutableStateOf(item?.title ?: "") }
    var showNameError by remember { mutableStateOf(false) }
    
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
                    text = if (item != null) "Edit goal" else "Add a goal",
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
                    text = if (item != null) "Update your goal name" else "Create a new goal to track time",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { 
                        titleText = it
                        showNameError = false
                    },
                    label = { 
                        Text(
                            "Goal name",
                            fontWeight = FontWeight.Medium
                        ) 
                    },
                    trailingIcon = if (titleText.isNotEmpty()) {
                        {
                            IconButton(onClick = { titleText = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear goal name",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    isError = showNameError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,  // Bright white when focused (matches AddCustomDeviceDialog)
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,  // Clean gray fill (matches AddCustomDeviceDialog)
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
                    singleLine = true,
                    supportingText = if (showNameError) {
                        { Text("Goal name is required") }
                    } else null
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        val trimmedTitle = titleText.trim()
                        showNameError = trimmedTitle.isEmpty()
                        
                        if (!showNameError) {
                            onSave(trimmedTitle)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = ButtonStyles.ctaButtonShapeRounded,
                    contentPadding = ButtonStyles.ctaButtonPadding
                ) {
                    Text(
                        if (item != null) "SAVE" else "ADD GOAL", 
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Cancel Button (at bottom)
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

fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    
    return when {
        hours > 0 -> String.format("%dh %dm", hours, minutes)
        minutes > 0 -> String.format("%dm %ds", minutes, secs)
        else -> String.format("%ds", secs)
    }
}

fun formatTotalTime(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    
    return String.format("%02d:%02d:%02d", hours, minutes, secs)
}

// Export function for the new data structure
fun exportBacklogData(items: List<BacklogItem>): String {
    val sb = StringBuilder()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    sb.appendLine("# Time Tracker Export")
    sb.appendLine("# Exported: ${dateFormat.format(Date())}")
    sb.appendLine()
    
    items.forEach { item ->
        sb.appendLine("Tracker: ${item.title}")
        sb.appendLine("Total Time: ${formatTotalTime(item.totalPlayedSeconds)}")
        
        if (item.sessions.isNotEmpty()) {
            sb.appendLine("Sessions:")
            item.sessions.forEach { session ->
                val start = timeFormat.format(Date(session.startTime))
                val end = if (session.endTime != null) timeFormat.format(Date(session.endTime)) else "ongoing"
                sb.appendLine("  - $start to $end (${formatDuration(session.durationSeconds)})")
            }
        }
        sb.appendLine()
    }
    
    return sb.toString()
}

// Import function for the new data structure
fun parseBacklogImport(content: String): List<BacklogItem> {
    val items = mutableListOf<BacklogItem>()
    var currentTitle: String? = null
    val sessions = mutableListOf<TimeSession>()
    
    content.lines().forEach { line ->
        when {
            line.startsWith("Tracker: ") -> {
                // Save previous item if exists
                currentTitle?.let {
                    items.add(BacklogItem(title = it, sessions = sessions.toList()))
                }
                // Start new item
                currentTitle = line.substringAfter("Tracker: ").trim()
                sessions.clear()
            }
            line.trim().startsWith("- ") && currentTitle != null -> {
                // Parse session (simplified - just count the duration)
                val durationMatch = Regex("""\((\d+)h (\d+)m\)|\((\d+)m (\d+)s\)|\((\d+)s\)""").find(line)
                durationMatch?.let { match ->
                    val groups = match.groupValues
                    val seconds = when {
                        groups[1].isNotEmpty() -> groups[1].toInt() * 3600 + groups[2].toInt() * 60
                        groups[3].isNotEmpty() -> groups[3].toInt() * 60 + groups[4].toInt()
                        groups[5].isNotEmpty() -> groups[5].toInt()
                        else -> 0
                    }
                    if (seconds > 0) {
                        val endTime = System.currentTimeMillis()
                        val startTime = endTime - (seconds * 1000L)
                        sessions.add(TimeSession(startTime = startTime, endTime = endTime))
                    }
                }
            }
        }
    }
    
    // Save last item if exists
    currentTitle?.let {
        items.add(BacklogItem(title = it, sessions = sessions.toList()))
    }
    
    return items
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