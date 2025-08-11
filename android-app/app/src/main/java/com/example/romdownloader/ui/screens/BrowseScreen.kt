package com.example.romdownloader.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.romdownloader.Downloader
import com.example.romdownloader.MainViewModel
import com.example.romdownloader.Platform
import com.example.romdownloader.RomItem
import com.example.romdownloader.ui.components.RomCard
import com.example.romdownloader.ui.theme.Purple
import com.example.romdownloader.ui.theme.LightPurple
import android.os.Environment
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    viewModel: MainViewModel,
    downloader: Downloader
) {
    val context = LocalContext.current
    var showPlatformPicker by remember { mutableStateOf(false) }
    var selectedPlatform by remember { mutableStateOf(viewModel.selectedPlatform) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header with Search
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Browse ROMs",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Search Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = viewModel.searchTerm,
                        onValueChange = { viewModel.updateSearchTerm(it) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search games...") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Purple
                            )
                        },
                        trailingIcon = {
                            if (viewModel.searchTerm.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchTerm("") }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Purple,
                            unfocusedBorderColor = LightPurple,
                            focusedLeadingIconColor = Purple,
                            unfocusedLeadingIconColor = Purple
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    
                    Button(
                        onClick = {
                            if (viewModel.selectedPlatform == null) {
                                viewModel.searchAll()
                            } else {
                                viewModel.searchSelected()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Purple
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        if (viewModel.isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Platform Filter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { showPlatformPicker = true },
                        label = {
                            Text(
                                text = selectedPlatform?.label ?: "All Platforms",
                                fontWeight = FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (selectedPlatform != null) Purple.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                    
                    if (selectedPlatform != null) {
                        AssistChip(
                            onClick = {
                                selectedPlatform = null
                                viewModel.setPlatform(null)
                            },
                            label = { Text("Clear") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        )
                    }
                }
            }
        }
        
        // Quick Filters
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val quickPlatforms = listOf(
                Platform.GENESIS, Platform.SNES, Platform.PS1, 
                Platform.N64, Platform.GBA, Platform.NES
            )
            items(quickPlatforms) { platform ->
                FilterChip(
                    selected = selectedPlatform == platform,
                    onClick = {
                        selectedPlatform = if (selectedPlatform == platform) null else platform
                        viewModel.setPlatform(selectedPlatform)
                    },
                    label = { Text(platform.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Purple.copy(alpha = 0.2f),
                        selectedLabelColor = Purple
                    )
                )
            }
        }
        
        // Bulk Download & Transfer Button (if results exist and host is selected)
        if (viewModel.results.isNotEmpty() && viewModel.selectedHost != null) {
            var showBulkProgress by remember { mutableStateOf(false) }
            var bulkProgressText by remember { mutableStateOf("") }
            var bulkProgress by remember { mutableStateOf(0f) }
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Purple.copy(alpha = 0.1f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bulk Download & Transfer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${viewModel.results.size} ROMs found • Host: ${viewModel.selectedHost?.ip}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Button(
                            onClick = {
                                showBulkProgress = true
                                viewModel.downloadAndTransferAll(
                                    context = context,
                                    downloader = downloader,
                                    onProgress = { current, total, message ->
                                        bulkProgress = if (total > 0) current.toFloat() / total.toFloat() else 0f
                                        bulkProgressText = message
                                    },
                                    onComplete = { success, failures ->
                                        showBulkProgress = false
                                        android.widget.Toast.makeText(
                                            context,
                                            "Bulk operation completed: $success successful, $failures failed",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )
                            },
                            enabled = !showBulkProgress,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Purple
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (showBulkProgress) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = if (showBulkProgress) "Processing..." else "Download All & Transfer",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    if (showBulkProgress) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LinearProgressIndicator(
                                progress = { bulkProgress },
                                modifier = Modifier.fillMaxWidth(),
                                color = Purple,
                                trackColor = LightPurple.copy(alpha = 0.3f)
                            )
                            Text(
                                text = bulkProgressText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
        
        // Results
        if (viewModel.isSearching) {
            ShimmerLoadingList()
        } else if (viewModel.results.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = if (viewModel.searchTerm.isNotEmpty()) 
                            "No results found" 
                        else 
                            "Search for ROMs to get started",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(viewModel.results) { rom ->
                    RomCard(
                        title = rom.displayName,
                        platform = rom.platform.label,
                        size = null, // You can calculate file size if needed
                        onDownload = {
                            downloader.download(context, rom)
                            // Show toast
                            android.widget.Toast.makeText(
                                context,
                                "Downloading ${rom.displayName}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        },
                        onUpload = {
                            val localFile = File(
                                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                                "roms/${rom.platform.id}/${rom.displayName}"
                            )
                            viewModel.uploadFile(localFile, rom.platform) { success, message ->
                                val toastMessage = if (success) {
                                    "Uploaded to: ${message ?: "device"}"
                                } else {
                                    "Upload failed: ${message ?: "Unknown error"}"
                                }
                                android.widget.Toast.makeText(
                                    context,
                                    toastMessage,
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        onDownloadAndTransfer = if (viewModel.selectedHost != null) {
                            {
                                viewModel.downloadAndTransfer(context, downloader, rom) { success, message ->
                                    val toastMessage = if (success) {
                                        message ?: "Download and transfer completed successfully"
                                    } else {
                                        "Download and transfer failed: ${message ?: "Unknown error"}"
                                    }
                                    android.widget.Toast.makeText(
                                        context,
                                        toastMessage,
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        } else null
                    )
                }
            }
        }
    }
    
    // Platform Picker Dialog
    if (showPlatformPicker) {
        PlatformPickerDialog(
            currentPlatform = selectedPlatform,
            onPlatformSelected = { platform ->
                selectedPlatform = platform
                viewModel.setPlatform(platform)
                showPlatformPicker = false
            },
            onDismiss = { showPlatformPicker = false }
        )
    }
}

@Composable
private fun ShimmerLoadingList(placeholderCount: Int = 8) {
    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val highlightColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val brush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Small header indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Purple)
            Text("Searching ROMs...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(placeholderCount) { _ ->
                PlaceholderCard(brush = brush)
            }
        }
    }
}

@Composable
private fun PlaceholderCard(brush: Brush) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Title placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(brush)
            )
            // Subtitle placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(brush)
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Action row placeholders
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(width = 96.dp, height = 36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .size(width = 96.dp, height = 36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(brush)
                )
            }
        }
    }
}

@Composable
fun PlatformPickerDialog(
    currentPlatform: Platform?,
    onPlatformSelected: (Platform?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Select Platform",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Card(
                        onClick = { onPlatformSelected(null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (currentPlatform == null) 
                                Purple.copy(alpha = 0.2f) 
                            else 
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Text(
                            text = "All Platforms",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (currentPlatform == null) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
                
                items(Platform.all) { platform ->
                    Card(
                        onClick = { onPlatformSelected(platform) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (currentPlatform == platform) 
                                Purple.copy(alpha = 0.2f) 
                            else 
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Text(
                            text = platform.label,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (currentPlatform == platform) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Purple)
            }
        }
    )
}
