package com.example.romdownloader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.romdownloader.MainViewModel
import com.example.romdownloader.ui.components.HandheldCard
import com.example.romdownloader.ui.theme.Purple
import com.example.romdownloader.ui.theme.LightPurple
import kotlinx.coroutines.launch
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandheldScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    var showTemplatesEditor by remember { mutableStateOf(false) }
    var selectedTemplate by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) { 
        viewModel.loadTemplates(context) 
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Handhelds",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Connect to your handheld device",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    IconButton(
                        onClick = { showTemplatesEditor = true }
                    ) {
                        val painter = painterResource(id = com.example.romdownloader.R.drawable.edit)
                        Icon(
                            painter = painter,
                            contentDescription = "Edit",
                            tint = Color.Unspecified
                        )
                    }
                }
            }
        }
        
        // Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Quick Connect Section
            item {
                Text(
                    text = "Quick Connect",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HandheldCard(
                        title = "Rocknix",
                        isConnected = selectedTemplate == "Rocknix",
                        onClick = {
                            viewModel.applyTemplateRocknix()
                            selectedTemplate = "Rocknix"
                        }
                    )
                    
                    HandheldCard(
                        title = "muOS",
                        isConnected = selectedTemplate == "muOS",
                        onClick = {
                            viewModel.applyTemplateMuOS()
                            selectedTemplate = "muOS"
                        }
                    )
                }
            }
            
            // Connection Status
            if (selectedTemplate != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Purple.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Connected",
                                    tint = Purple,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "Connected to $selectedTemplate",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = Purple
                                    )
                                    Text(
                                        text = viewModel.selectedHost?.ip ?: "No IP",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            TextButton(
                                onClick = { selectedTemplate = null }
                            ) {
                                Text("Disconnect", color = Purple)
                            }
                        }
                    }
                }
            }
            
            // Manual Configuration Section
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Manual Configuration",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.sshUsername,
                            onValueChange = { viewModel.sshUsername = it },
                            label = { Text("Username") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Purple,
                                unfocusedBorderColor = LightPurple
                            )
                        )
                        
                        OutlinedTextField(
                            value = viewModel.sshPassword,
                            onValueChange = { viewModel.sshPassword = it },
                            label = { Text("Password") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Purple,
                                unfocusedBorderColor = LightPurple
                            )
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = viewModel.sshPort,
                                onValueChange = { viewModel.sshPort = it.filter { ch -> ch.isDigit() }.take(5) },
                                label = { Text("Port") },
                                modifier = Modifier.weight(0.3f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Purple,
                                    unfocusedBorderColor = LightPurple
                                )
                            )
                            
                            OutlinedTextField(
                                value = viewModel.remoteBasePath,
                                onValueChange = { viewModel.remoteBasePath = it },
                                label = { Text("Remote Path") },
                                modifier = Modifier.weight(0.7f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Purple,
                                    unfocusedBorderColor = LightPurple
                                )
                            )
                        }
                        
                        Button(
                            onClick = { viewModel.scanHosts() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Purple
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (viewModel.isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scanning...")
                            } else {
                                Icon(Icons.Default.Search, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scan Network")
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Templates Editor Dialog
    if (showTemplatesEditor) {
        TemplatesEditorDialog(
            viewModel = viewModel,
            onDismiss = { showTemplatesEditor = false }
        )
    }
}

@Composable
fun TemplatesEditorDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var rocknixTemplate by remember { mutableStateOf(viewModel.rocknixTemplate) }
    var muosTemplate by remember { mutableStateOf(viewModel.muosTemplate) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Edit Templates",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Rocknix Section
                item {
                    Text(
                        "Rocknix",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Purple
                    )
                }
                
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = rocknixTemplate.username,
                            onValueChange = { rocknixTemplate = rocknixTemplate.copy(username = it) },
                            label = { Text("Username") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = rocknixTemplate.password,
                            onValueChange = { rocknixTemplate = rocknixTemplate.copy(password = it) },
                            label = { Text("Password") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = rocknixTemplate.port,
                            onValueChange = { rocknixTemplate = rocknixTemplate.copy(port = it.filter { ch -> ch.isDigit() }.take(5)) },
                            label = { Text("Port") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = rocknixTemplate.remoteBasePath,
                            onValueChange = { rocknixTemplate = rocknixTemplate.copy(remoteBasePath = it) },
                            label = { Text("Remote Path") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = rocknixTemplate.hostIp,
                            onValueChange = { rocknixTemplate = rocknixTemplate.copy(hostIp = it) },
                            label = { Text("Host IP") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
                
                // muOS Section
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "muOS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Purple
                    )
                }
                
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = muosTemplate.username,
                            onValueChange = { muosTemplate = muosTemplate.copy(username = it) },
                            label = { Text("Username") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = muosTemplate.password,
                            onValueChange = { muosTemplate = muosTemplate.copy(password = it) },
                            label = { Text("Password") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = muosTemplate.port,
                            onValueChange = { muosTemplate = muosTemplate.copy(port = it.filter { ch -> ch.isDigit() }.take(5)) },
                            label = { Text("Port") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = muosTemplate.remoteBasePath,
                            onValueChange = { muosTemplate = muosTemplate.copy(remoteBasePath = it) },
                            label = { Text("Remote Path") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = muosTemplate.hostIp,
                            onValueChange = { muosTemplate = muosTemplate.copy(hostIp = it) },
                            label = { Text("Host IP") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.updateTemplate(context, rocknixTemplate)
                    viewModel.updateTemplate(context, muosTemplate)
                    onDismiss()
                }
            ) {
                Text("Save", color = Purple)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
