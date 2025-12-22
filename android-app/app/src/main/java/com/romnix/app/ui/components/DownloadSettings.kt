package com.romnix.app.ui.components

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.romnix.app.R
import com.romnix.app.ui.theme.ButtonStyles
import com.romnix.app.ui.theme.NavyBlue
import com.romnix.app.ui.theme.PalestBlue
import com.romnix.app.ui.theme.SecondaryGray
import com.romnix.app.ui.theme.AppWhite
import com.romnix.app.data.PreferencesManager
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadSettingsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesManager = remember { PreferencesManager(context) }
    
    // Observe current settings
    val currentLocation by preferencesManager.downloadLocationFlow.collectAsState(initial = null)
    val useCustomLocation by preferencesManager.useCustomLocationFlow.collectAsState(initial = false)
    
    // Directory picker launcher
    val directoryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            // Persist permissions
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(selectedUri, takeFlags)
            
            // Save the selected location
            scope.launch {
                preferencesManager.saveDownloadLocation(selectedUri.toString())
                preferencesManager.setUseCustomLocation(true)
            }
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (MaterialTheme.colorScheme.background.luminance() > 0.5) {
                    AppWhite  // Light theme: white modal background
                } else {
                    MaterialTheme.colorScheme.surfaceContainer  // Dark theme: adaptive
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Text(
                    text = "Local destination",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Choose where your files will be saved to.",
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Default location option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch {
                                preferencesManager.setUseCustomLocation(false)
                            }
                        },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (!useCustomLocation) {
                            MaterialTheme.colorScheme.background
                        } else {
                            Color.Transparent
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !useCustomLocation,
                            onClick = {
                                scope.launch {
                                    preferencesManager.setUseCustomLocation(false)
                                }
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Default Location",
                                color = if (!useCustomLocation) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "App storage/Download/roms/",
                                color = if (!useCustomLocation) {
                                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            painter = painterResource(id = R.drawable.folder),
                            contentDescription = "Folder",
                            tint = if (!useCustomLocation) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Custom location option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            directoryPickerLauncher.launch(null)
                        },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (useCustomLocation) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            Color.Transparent
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = useCustomLocation,
                            onClick = {
                                directoryPickerLauncher.launch(null)
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Custom Location",
                                color = if (useCustomLocation) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (useCustomLocation && currentLocation != null) {
                                    getPathFromUri(currentLocation)
                                } else {
                                    "Tap to choose a folder"
                                },
                                color = if (useCustomLocation) {
                                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = "Choose Folder",
                            tint = if (useCustomLocation) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                if (useCustomLocation && currentLocation != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Current path display
                    OutlinedTextField(
                        value = getPathFromUri(currentLocation),
                        onValueChange = { },
                        label = { Text("Current folder") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { directoryPickerLauncher.launch(null) }) {
                                Icon(
                                    Icons.Default.FolderOpen,
                                    contentDescription = "Change folder",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
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
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Save button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = ButtonStyles.ctaButtonShapeRounded,
                    contentPadding = ButtonStyles.ctaButtonPadding
                ) {
                    Text("DONE", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
                
                // Cancel button
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

// Helper function to extract readable path from URI
private fun getPathFromUri(uriString: String?): String {
    if (uriString == null) return "No folder selected"
    
    return try {
        val uri = Uri.parse(uriString)
        val docId = DocumentsContract.getTreeDocumentId(uri)
        
        when {
            docId.startsWith("primary:") -> {
                val path = docId.substring(8)
                if (path.isEmpty()) {
                    "Internal Storage"
                } else {
                    "Internal Storage/$path"
                }
            }
            docId.startsWith("home:") -> {
                "Documents/${docId.substring(5)}"
            }
            docId.contains(":") -> {
                val parts = docId.split(":")
                if (parts.size > 1) {
                    "${parts[0]}/${parts[1]}"
                } else {
                    docId
                }
            }
            else -> docId
        }
    } catch (e: Exception) {
        "Custom folder"
    }
}
