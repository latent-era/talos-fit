package com.devil.phoenixproject.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Theme helper functions for consistent styling across screens.
 */

/**
 * Returns a vertical gradient brush for screen backgrounds.
 * Dark mode: Solid AMOLED black
 * Light mode: Subtle warm wash
 *
 * Uses MaterialTheme.colorScheme to detect dark/light mode,
 * respecting the app's theme preference (not just system theme).
 */
@Composable
fun screenBackgroundBrush(): Brush {
    // Check if we're in dark mode by examining the background color luminance
    // This respects the app's ThemeMode setting, not just system theme
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (isDark) {
        Brush.verticalGradient(
            0.0f to Slate950,
            1.0f to Slate950
        )
    } else {
        Brush.verticalGradient(
            0.0f to Slate50,
            1.0f to Color.White
        )
    }
}
