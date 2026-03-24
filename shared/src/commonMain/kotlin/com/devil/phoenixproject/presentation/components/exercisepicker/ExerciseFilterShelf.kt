package com.devil.phoenixproject.presentation.components.exercisepicker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Unified horizontal filter shelf combining favorites, custom, muscle, and equipment filters.
 * Replaces the previous 4-row filter UI with a single scrollable row.
 */
@Composable
fun ExerciseFilterShelf(
    showFavoritesOnly: Boolean,
    onToggleFavorites: () -> Unit,
    showCustomOnly: Boolean,
    onToggleCustom: () -> Unit,
    selectedMuscles: Set<String>,
    onToggleMuscle: (String) -> Unit,
    selectedEquipment: Set<String>,
    onToggleEquipment: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val muscleGroups = listOf("Chest", "Back", "Legs", "Shoulders", "Arms", "Core")
    val equipmentTypes = listOf("Long Bar", "Short Bar", "Handles", "Rope", "Belt", "Ankle Strap", "Bench", "Bodyweight")

    val hasActiveFilters = showFavoritesOnly || showCustomOnly ||
        selectedMuscles.isNotEmpty() || selectedEquipment.isNotEmpty()

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.height(48.dp)
    ) {
        // Clear button (only when filters active)
        if (hasActiveFilters) {
            item {
                InputChip(
                    selected = false,
                    onClick = onClearAll,
                    label = { Text("Clear") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear filters",
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = InputChipDefaults.inputChipColors(
                        containerColor = Color.Transparent,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }

        // Favorites chip
        item {
            FilterChip(
                selected = showFavoritesOnly,
                onClick = onToggleFavorites,
                label = { Text("Favorites") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (showFavoritesOnly) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                shape = RoundedCornerShape(8.dp),
                border = if (!showFavoritesOnly) {
                    FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = false,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        borderWidth = 1.dp
                    )
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.Transparent,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White
                )
            )
        }

        // Custom chip
        item {
            FilterChip(
                selected = showCustomOnly,
                onClick = onToggleCustom,
                label = { Text("Custom") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (showCustomOnly) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                shape = RoundedCornerShape(8.dp),
                border = if (!showCustomOnly) {
                    FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = false,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        borderWidth = 1.dp
                    )
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.Transparent,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White
                )
            )
        }

        // Divider
        item {
            VerticalDivider(
                modifier = Modifier.height(24.dp).padding(horizontal = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }

        // Muscle group chips
        items(muscleGroups) { muscle ->
            val isSelected = muscle in selectedMuscles
            FilterChip(
                selected = isSelected,
                onClick = { onToggleMuscle(muscle) },
                label = { Text(muscle) },
                shape = RoundedCornerShape(8.dp),
                border = if (!isSelected) {
                    FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = false,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        borderWidth = 1.dp
                    )
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.Transparent,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White
                )
            )
        }

        // Divider
        item {
            VerticalDivider(
                modifier = Modifier.height(24.dp).padding(horizontal = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }

        // Equipment chips
        items(equipmentTypes) { equipment ->
            val isSelected = equipment in selectedEquipment
            FilterChip(
                selected = isSelected,
                onClick = { onToggleEquipment(equipment) },
                label = { Text(equipment) },
                shape = RoundedCornerShape(8.dp),
                border = if (!isSelected) {
                    FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = false,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        borderWidth = 1.dp
                    )
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.Transparent,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}
