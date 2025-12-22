package com.romnix.app.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object ButtonStyles {
    // CTA Button Configuration
    val ctaButtonShape = RoundedCornerShape(12.dp)
    val ctaButtonPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
    
    // Alternative shapes for different contexts
    val ctaButtonShapeRounded = RoundedCornerShape(12.dp)
    
    // Trash/Delete Button Colors - Consistent across all screens
    @Composable
    fun trashButtonBackground(): Color = MaterialTheme.colorScheme.tertiaryContainer
    
    @Composable
    fun trashButtonIconTint(): Color = MaterialTheme.colorScheme.onTertiaryContainer
    
    // Trash button shape
    val trashButtonShape = RoundedCornerShape(12.dp)
    
    // Destructive CTA Button Colors - Consistent for all delete/clear modals
    @Composable
    fun destructiveButtonBackground(): Color = MaterialTheme.colorScheme.error
    
    @Composable
    fun destructiveButtonText(): Color = MaterialTheme.colorScheme.onError
}
