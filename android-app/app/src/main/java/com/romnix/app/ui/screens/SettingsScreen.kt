package com.romnix.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.romnix.app.Foldername
import com.romnix.app.FoldernameManager
import com.romnix.app.R
import com.romnix.app.ui.theme.ButtonStyles
import com.romnix.app.ui.theme.NavyBlue
import com.romnix.app.ui.theme.PalestBlue
import com.romnix.app.ui.theme.AppWhite

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
fun SettingsScreen(
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var editingFoldername by remember { mutableStateOf<Foldername?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }
    
    // Force recomposition when platforms change
    LaunchedEffect(refreshTrigger) {
        FoldernameManager.loadCustomFoldernames(context)
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Modern Header with gradient background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                Color(0xFF8B5CF6)
                            )
                        )
                    )
                    .padding(top = 48.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackPressed,
                        modifier = Modifier
                            .background(
                                Color.White.copy(alpha = 0.2f),
                                RoundedCornerShape(12.dp)
                            )
                            .size(44.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Foldername Links",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 24.sp
                        )
                        Text(
                            text = "Manage your ROM sources",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }
                    
                    // Add Button
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier
                            .background(
                                Color.White.copy(alpha = 0.2f),
                                RoundedCornerShape(12.dp)
                            )
                            .size(44.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Foldername",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            // Foldernames List Section
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                if (FoldernameManager.all.isEmpty()) {
                    // Modern Empty State
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceBright  // Bright modal background
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.romnix_emptystate),
                                contentDescription = null,
                                modifier = Modifier.size(120.dp),
                                tint = surfaceVariantDarker()
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "No Foldername Links",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF374151),
                                fontSize = 20.sp
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Add your first custom foldername to start downloading files",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp
                            )
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Add Foldername Link",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(FoldernameManager.all) { platform ->
                            ModernFoldernameItem(
                                platform = platform,
                                onEdit = if (platform.isCustom) {
                                    { editingFoldername = platform }
                                } else null,
                                onDelete = if (platform.isCustom) {
                                    {
                                        FoldernameManager.removeCustomFoldername(context, platform.id)
                                        refreshTrigger++
                                    }
                                } else null
                            )
                        }
                        
                        // Add button at the end of the list
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { showAddDialog = true },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "Add New Foldername",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Add/Edit dialog
    if (showAddDialog || editingFoldername != null) {
        FoldernameEditDialog(
            platform = editingFoldername,
            onDismiss = {
                showAddDialog = false
                editingFoldername = null
            },
            onSave = { platform ->
                if (editingFoldername != null) {
                    FoldernameManager.updateCustomFoldername(context, platform)
                } else {
                    FoldernameManager.addCustomFoldername(context, platform)
                }
                showAddDialog = false
                editingFoldername = null
                refreshTrigger++
            }
        )
    }
}

@Composable
private fun ModernFoldernameItem(
    platform: Foldername,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (MaterialTheme.colorScheme.background.luminance() > 0.5) {
                MaterialTheme.colorScheme.surfaceBright  // Bright modal background
            } else {
                MaterialTheme.colorScheme.surfaceContainer  // Dark theme: contrasting
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Foldername Icon with background
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            when (platform.id) {
                                "nes", "snes", "genesis" -> Icons.Default.SportsEsports
                                "gb", "gba", "gbc", "gamegear", "ngp" -> Icons.Default.Gamepad
                                "n64", "ps1", "dreamcast", "saturn", "tg16" -> Icons.Default.VideogameAsset
                                else -> Icons.Default.Games
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = platform.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp
                            )
                            
                            if (platform.isCustom) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Custom",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        
                        if (platform.archiveUrl.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = platform.archiveUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                fontSize = 12.sp
                            )
                        }
                        
                        if (platform.extensions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Extensions: ${platform.extensions.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD1D5DB),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                
                if (platform.isCustom) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (onEdit != null) {
                            IconButton(
                                onClick = onEdit,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.edit),
                                    contentDescription = "Edit",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (onDelete != null) {
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.trash),
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoldernameEditDialog(
    platform: Foldername?,
    onDismiss: () -> Unit,
    onSave: (Foldername) -> Unit
) {
    var name by remember { mutableStateOf(platform?.label ?: "") }
    var url by remember { mutableStateOf(platform?.archiveUrl ?: "") }
    var extensions by remember { mutableStateOf(platform?.extensions?.joinToString(", ") ?: "") }
    var showUrlError by remember { mutableStateOf(false) }
    var showNameError by remember { mutableStateOf(false) }
    
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
                    text = if (platform != null) "Edit folder" else "Add your source",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        showNameError = false
                    },
                    label = { 
                        Text(
                            "Folder name",
                            fontWeight = FontWeight.Medium
                        ) 
                    },
                    trailingIcon = if (name.isNotEmpty()) {
                        {
                            IconButton(onClick = { name = "" }) {
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
                    isError = showNameError,
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
                    singleLine = true,
                    supportingText = if (showNameError) {
                        { Text("Foldername name is required") }
                    } else null
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = url,
                    onValueChange = { 
                        url = it
                        showUrlError = false
                    },
                    label = { 
                        Text(
                            "Type your URL",
                            fontWeight = FontWeight.Medium
                        ) 
                    },
                    trailingIcon = if (url.isNotEmpty()) {
                        {
                            IconButton(onClick = { url = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear URL",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    isError = showUrlError,
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
                    supportingText = if (showUrlError) {
                        { Text("Valid URL is required") }
                    } else null
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
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
                                    contentDescription = "Clear extensions",
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
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Save Button (at top)
                Button(
                    onClick = {
                        val trimmedName = name.trim()
                        val trimmedUrl = url.trim()
                        
                        showNameError = trimmedName.isEmpty()
                        showUrlError = trimmedUrl.isEmpty() || !trimmedUrl.startsWith("http")
                        
                        if (!showNameError && !showUrlError) {
                            val extensionsList = extensions.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            val id = platform?.id ?: trimmedName.lowercase().replace(" ", "")
                            
                            onSave(
                                Foldername(
                                    id = id,
                                    label = trimmedName,
                                    archiveUrl = trimmedUrl,
                                    extensions = extensionsList,
                                    isCustom = true
                                )
                            )
                        }
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
        // Dark theme: Slightly lighter surfaceVariant
        Color(
            red = (colorScheme.surfaceVariant.red * 1.15f).coerceIn(0f, 1f),
            green = (colorScheme.surfaceVariant.green * 1.15f).coerceIn(0f, 1f),
            blue = (colorScheme.surfaceVariant.blue * 1.15f).coerceIn(0f, 1f),
            alpha = colorScheme.surfaceVariant.alpha
        )
    }
}
