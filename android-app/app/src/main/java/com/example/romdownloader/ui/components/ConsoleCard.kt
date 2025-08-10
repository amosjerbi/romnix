package com.example.romdownloader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.romdownloader.ui.theme.LightPurple
import com.example.romdownloader.ui.theme.Purple

@Composable
fun ConsoleCard(
    title: String,
    subtitle: String? = null,
    icon: @Composable () -> Unit = {},
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(LightPurple.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
        }
    }
}

@Composable
fun HandheldCard(
    title: String,
    isConnected: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) Purple.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(LightPurple.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                // Icon placeholder
                Text(
                    text = title.take(1),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Purple
                )
            }
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (isConnected) {
                Text(
                    text = "Connected",
                    style = MaterialTheme.typography.bodySmall,
                    color = Purple,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Purple
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Connect", fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun RomCard(
    title: String,
    platform: String,
    size: String? = null,
    onDownload: () -> Unit,
    onUpload: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // File icon
            Box(
                modifier = Modifier
                    .size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                val painter = androidx.compose.ui.res.painterResource(id = com.example.romdownloader.R.drawable.file)
                androidx.compose.foundation.Image(
                    painter = painter,
                    contentDescription = "ROM File",
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Title and platform
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = platform,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            
            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Download button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Purple)
                        .clickable { onDownload() },
                    contentAlignment = Alignment.Center
                ) {
                    val downloadPainter = androidx.compose.ui.res.painterResource(id = com.example.romdownloader.R.drawable.downloadsimple)
                    androidx.compose.foundation.Image(
                        painter = downloadPainter,
                        contentDescription = "Download",
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Upload button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .clickable { onUpload() },
                    contentAlignment = Alignment.Center
                ) {
                    val uploadPainter = androidx.compose.ui.res.painterResource(id = com.example.romdownloader.R.drawable.paperplane)
                    androidx.compose.foundation.Image(
                        painter = uploadPainter,
                        contentDescription = "Upload",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
