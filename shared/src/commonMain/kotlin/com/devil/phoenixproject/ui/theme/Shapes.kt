package com.devil.phoenixproject.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape System
 * Talos Design Language corner radii
 */
object ExpressiveShapeValues {
    val ExtraSmall = RoundedCornerShape(4.dp)
    val Small = RoundedCornerShape(6.dp)
    val Medium = RoundedCornerShape(8.dp)
    val Large = RoundedCornerShape(12.dp)
    val ExtraLarge = RoundedCornerShape(16.dp)
}

/**
 * Material 3 Expressive Shapes for MaterialTheme
 */
val ExpressiveShapes = Shapes(
    extraSmall = ExpressiveShapeValues.ExtraSmall,
    small = ExpressiveShapeValues.Small,
    medium = ExpressiveShapeValues.Medium,
    large = ExpressiveShapeValues.Large,
    extraLarge = ExpressiveShapeValues.ExtraLarge
)
