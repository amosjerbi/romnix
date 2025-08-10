package com.example.romdownloader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.romdownloader.Platform
import com.example.romdownloader.MainViewModel
import com.example.romdownloader.ui.components.ConsoleCard
import com.example.romdownloader.ui.theme.Purple
import com.example.romdownloader.ui.theme.LightPurple

data class ConsoleItem(
    val platform: Platform,
    val icon: ImageVector = Icons.Default.Games
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsoleSelectScreen(
    viewModel: MainViewModel,
    onConsoleSelected: (Platform) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val consoles = listOf(
        ConsoleItem(Platform.NES, Icons.Default.SportsEsports),
        ConsoleItem(Platform.SNES, Icons.Default.SportsEsports),
        ConsoleItem(Platform.GENESIS, Icons.Default.SportsEsports),
        ConsoleItem(Platform.GB, Icons.Default.Gamepad),
        ConsoleItem(Platform.GBA, Icons.Default.Gamepad),
        ConsoleItem(Platform.GBC, Icons.Default.Gamepad),
        ConsoleItem(Platform.N64, Icons.Default.VideogameAsset),
        ConsoleItem(Platform.PS1, Icons.Default.VideogameAsset),
        ConsoleItem(Platform.DREAMCAST, Icons.Default.VideogameAsset),
        ConsoleItem(Platform.SATURN, Icons.Default.VideogameAsset),
        ConsoleItem(Platform.GAMEGEAR, Icons.Default.Gamepad),
        ConsoleItem(Platform.NGP, Icons.Default.Gamepad)
    )
    
    val filteredConsoles = if (searchQuery.isEmpty()) {
        consoles
    } else {
        consoles.filter { 
            it.platform.label.contains(searchQuery, ignoreCase = true) 
        }
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
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Select Console",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search consoles...") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Purple
                        )
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
            }
        }
        
        // Console Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredConsoles) { console ->
                ConsoleGridCard(
                    console = console,
                    onClick = { onConsoleSelected(console.platform) }
                )
            }
        }
    }
}

@Composable
fun ConsoleGridCard(
    console: ConsoleItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp)),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(LightPurple.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = console.icon,
                    contentDescription = console.platform.label,
                    modifier = Modifier.size(32.dp),
                    tint = Purple
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = console.platform.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
