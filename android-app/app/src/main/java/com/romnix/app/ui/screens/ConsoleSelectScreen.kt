package com.romnix.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import com.romnix.app.ui.theme.ButtonStyles
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romnix.app.Foldername
import com.romnix.app.FoldernameManager
import com.romnix.app.MainViewModel
import com.romnix.app.RomItem
import com.romnix.app.R
import com.romnix.app.ui.theme.SecondaryGray
import com.romnix.app.ui.theme.PalestBlue
import com.romnix.app.ui.theme.NavyBlue
import com.romnix.app.ui.theme.ErrorRed
import com.romnix.app.ui.theme.AppWhite
import kotlinx.coroutines.delay
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.drawscope.rotate

data class ConsoleItem(
    val platform: Foldername,
    val icon: ImageVector = Icons.Default.Games
)

// Shape Morphing Loader Animation - Material 3 compliant
@Composable
fun ShapeMorphingLoader(
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ShapeMorph")
    
    val morphProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MorphProgress"
    )
    
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )
    
    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val baseRadius = (size.minDimension / 3f) * 1.15f // 15% bigger and more prominent
        
        rotate(rotationAngle, Offset(centerX, centerY)) {
            // Shape morphing: Circle -> Square -> Rounded Rectangle with better transitions
            when {
                morphProgress < 0.33f -> {
                    // Circle - more prominent
                    drawCircle(
                        color = color,
                        radius = baseRadius * 0.8f,
                        center = Offset(centerX, centerY)
                    )
                }
                morphProgress < 0.66f -> {
                    // Square - more dramatic change
                    val squareSize = baseRadius * 1.6f
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(centerX - squareSize/2, centerY - squareSize/2),
                        size = Size(squareSize, squareSize),
                        cornerRadius = CornerRadius(2.dp.toPx())
                    )
                }
                else -> {
                    // Rounded Rectangle - more distinct shape
                    val rectWidth = baseRadius * 2.2f
                    val rectHeight = baseRadius * 1.4f
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(centerX - rectWidth/2, centerY - rectHeight/2),
                        size = Size(rectWidth, rectHeight),
                        cornerRadius = CornerRadius(16.dp.toPx())
                    )
                }
            }
        }
    }
}

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
fun ConsoleSelectScreen(
    viewModel: MainViewModel,
    downloader: com.romnix.app.Downloader,
    onConsoleSelected: (Foldername) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var platformToEdit by remember { mutableStateOf<Foldername?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }
    
    // Trigger ROM search when search query changes
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            // Search for files across all platforms
            viewModel.updateSearchTerm(searchQuery)
            viewModel.searchAll()
        } else {
            viewModel.clearResults()
        }
    }
    
    // Force recomposition when platforms change
    LaunchedEffect(refreshTrigger) {
        FoldernameManager.loadCustomFoldernames(context)
    }
    
    val consoles = FoldernameManager.all.map { platform ->
        val icon = when (platform.id) {
            "nes", "snes", "genesis" -> Icons.Default.SportsEsports
            "gb", "gba", "gbc", "gamegear", "ngp" -> Icons.Default.Gamepad
            "n64", "ps1", "dreamcast", "saturn", "tg16" -> Icons.Default.VideogameAsset
            "segacd", "sega32x" -> Icons.Default.Album
            else -> Icons.Default.Games
        }
        ConsoleItem(platform, icon)
    }
    
    val filteredConsoles = if (searchQuery.isEmpty()) {
        consoles
    } else {
        consoles.filter { 
            it.platform.label.contains(searchQuery, ignoreCase = true) 
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header (no gradient, clean white)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)
            ) {
                Text(
                    text = if (searchQuery.isNotEmpty()) "Search Results" else "File search",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 24.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (searchQuery.isNotEmpty()) {
                        "${viewModel.results.size} files found"
                    } else {
                        "Select a Folder name & search"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp
                )
            }
            
            // Search Bar with filled background
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                placeholder = { 
                    Text(
                        "Search across all folders",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    ) 
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.search_nav),
                        contentDescription = "Search",
                        modifier = Modifier.size(20.dp)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariantDarker(),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,  // Match ROM results search bar
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            
            // Content Area
            if (searchQuery.isNotEmpty() && viewModel.results.isNotEmpty()) {
                // Show ROM search results with bulk download
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Bulk Download Card
                    if (viewModel.results.size > 1) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .padding(top = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (MaterialTheme.colorScheme.background.luminance() > 0.5) {
                    MaterialTheme.colorScheme.surfaceBright  // Light theme: bright cards for high contrast
                } else {
                    MaterialTheme.colorScheme.surfaceContainer  // Dark theme: contrasting
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
                                        color = if (MaterialTheme.colorScheme.background.luminance() > 0.5) {
                                            MaterialTheme.colorScheme.onBackground
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
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
                                
                                // Bulk Download Button
                                IconButton(
                                    onClick = {
                                        viewModel.results.forEach { rom ->
                                            downloader.download(context, rom)
                                        }
                                        android.widget.Toast.makeText(
                                            context, 
                                            "Downloading ${viewModel.results.size} files...", 
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
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
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    // ROM Results List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(viewModel.results.size) { index ->
                            val rom = viewModel.results[index]
                            RomSearchResultCard(
                                rom = rom,
                                onClick = {
                                    // Select the platform and navigate to browse
                                    viewModel.setFoldername(rom.platform)
                                    onConsoleSelected(rom.platform)
                                }
                            )
                        }
                    }
                }
            } else if (searchQuery.isNotEmpty() && viewModel.isSearching) {
                // Show loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ShapeMorphingLoader(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(55.dp)  // 15% bigger (48 * 1.15 = 55.2)
                    )
                }
            } else if (FoldernameManager.all.isEmpty() || filteredConsoles.isEmpty()) {
                // Empty State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // ROM NIX Logo (light)
                    Icon(
                        painter = painterResource(id = R.drawable.romnix_emptystate),
                        contentDescription = "ROM NIX Logo",
                        modifier = Modifier.size(120.dp),
                        tint = surfaceVariantDarkerConsole()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Add a folder name",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Add Button - using Box to reduce overdraw
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
            } else {
                // Console List (when platforms exist) - Full width rows
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredConsoles.size) { index ->
                        CleanConsoleRow(
                            console = filteredConsoles[index],
                            onClick = { onConsoleSelected(filteredConsoles[index].platform) },
                            onEdit = {
                                platformToEdit = filteredConsoles[index].platform
                                showEditDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Add Foldername Dialog
    if (showAddDialog) {
        AddFoldernameDialog(
            onDismiss = { showAddDialog = false },
            onSave = { platform ->
                FoldernameManager.addCustomFoldername(context, platform)
                showAddDialog = false
                refreshTrigger++
            }
        )
    }
    
    // Edit Foldername Dialog
    if (showEditDialog && platformToEdit != null) {
        EditFoldernameDialog(
            platform = platformToEdit!!,
            onDismiss = { 
                showEditDialog = false
                platformToEdit = null
            },
            onSave = { updatedFoldername ->
                FoldernameManager.updateCustomFoldername(context, updatedFoldername)
                showEditDialog = false
                platformToEdit = null
                refreshTrigger++
            }
        )
    }
}

@Composable
fun CleanConsoleRow(
    console: ConsoleItem,
    onClick: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable { onClick() },
            colors = CardDefaults.cardColors(
                containerColor = if (MaterialTheme.colorScheme.background.luminance() > 0.5) {
                    MaterialTheme.colorScheme.surfaceBright  // Light theme: bright cards for high contrast
                } else {
                    MaterialTheme.colorScheme.surfaceContainer  // Dark theme: contrasting
                }
            ),
            shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
                Text(
                    text = console.platform.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (MaterialTheme.colorScheme.background.luminance() > 0.5) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    fontSize = 18.sp
                )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Edit icon button (same as home screen)
            IconButton(
                onClick = { 
                    onEdit()
                },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.edit),
                    contentDescription = "Edit",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFoldernameDialog(
    onDismiss: () -> Unit,
    onSave: (Foldername) -> Unit
) {
    var platformName by remember { mutableStateOf("") }
    var archiveUrl by remember { mutableStateOf("") }
    var extensions by remember { mutableStateOf("zip, 7z, png, pdf") }
    
    Dialog(onDismissRequest = onDismiss) {
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
                    text = "Add your source",
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
                    text = "Choose where to get files, we’ll group them for you.",
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
                                    id = platformName.lowercase().replace(" ", ""),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditFoldernameDialog(
    platform: Foldername,
    onDismiss: () -> Unit,
    onSave: (Foldername) -> Unit
) {
    var platformName by remember { mutableStateOf(platform.label) }
    var archiveUrl by remember { mutableStateOf(platform.archiveUrl) }
    var extensions by remember { mutableStateOf(platform.extensions.joinToString(", ")) }
    
    Dialog(onDismissRequest = onDismiss) {
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
                    text = "EDIT FOLDER",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    letterSpacing = 1.sp
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
                    singleLine = true
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
                    maxLines = 2,
                    supportingText = {
                        Text(
                            "Comma-separated list of file extensions",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Save Button (at top)
                Button(
                    onClick = {
                        val updatedFoldername = platform.copy(
                            label = platformName,
                            archiveUrl = archiveUrl,
                            extensions = extensions.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        )
                        onSave(updatedFoldername)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = ButtonStyles.ctaButtonShape,
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
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Cancel Button (at bottom)
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

@Composable
fun RomSearchResultCard(
    rom: RomItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable { onClick() },
            colors = CardDefaults.cardColors(
                containerColor = if (MaterialTheme.colorScheme.background.luminance() > 0.5) {
                    MaterialTheme.colorScheme.surfaceContainerLow  // Light theme: surfaceBright
                } else {
                    MaterialTheme.colorScheme.surfaceContainer  // Dark theme: contrasting
                }
            ),
            shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = rom.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (MaterialTheme.colorScheme.background.luminance() > 0.5) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = rom.platform.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
            
            // Arrow icon indicating navigation
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = "Open",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun surfaceVariantDarkerConsole(): Color {
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
        // Dark theme: Slightly lighter surfaceVariant
        Color(
            red = (colorScheme.surfaceVariant.red * 1.15f).coerceIn(0f, 1f),
            green = (colorScheme.surfaceVariant.green * 1.15f).coerceIn(0f, 1f),
            blue = (colorScheme.surfaceVariant.blue * 1.15f).coerceIn(0f, 1f),
            alpha = colorScheme.surfaceVariant.alpha
        )
    }
}
