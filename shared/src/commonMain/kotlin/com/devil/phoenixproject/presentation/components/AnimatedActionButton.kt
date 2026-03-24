package com.devil.phoenixproject.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.devil.phoenixproject.presentation.util.LocalWindowSizeClass
import com.devil.phoenixproject.presentation.util.WindowWidthSizeClass
import com.devil.phoenixproject.ui.theme.HomeButtonColors

/**
 * Icon animation types for AnimatedActionButton
 */
enum class IconAnimation {
    NONE,
    PULSE,      // Scale pulse (for Play icon)
    ROTATE,     // Continuous rotation (for Loop icon)
    TILT,       // Oscillating tilt (for Dumbbell icon)
    FIRE        // Kept for API compatibility, treated as NONE
}

/**
 * Animated FAB with press feedback and icon animations.
 *
 * @param label Button text
 * @param icon Button icon
 * @param onClick Click handler
 * @param isPrimary If true, uses solid Royal Blue. If false, uses solid Muted Purple.
 * @param isFireButton If true, uses primary accent colour (for Just Lift)
 * @param iconAnimation Type of icon animation to apply
 * @param modifier Modifier for the button
 */
@Composable
fun AnimatedActionButton(
    label: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    isPrimary: Boolean,
    isFireButton: Boolean = false,
    iconAnimation: IconAnimation = IconAnimation.NONE,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Responsive button height based on device size
    val windowSizeClass = LocalWindowSizeClass.current
    val buttonHeight = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Expanded -> 80.dp
        WindowWidthSizeClass.Medium -> 72.dp
        WindowWidthSizeClass.Compact -> 64.dp
    }

    // Press feedback animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pressScale"
    )

    // Icon animations
    val infiniteTransition = rememberInfiniteTransition(label = "idleTransition")

    val iconRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (iconAnimation == IconAnimation.ROTATE) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "iconRotation"
    )

    val iconTilt by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconTilt"
    )

    val iconPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconPulse"
    )

    // Determine icon transform (FIRE treated as NONE)
    val iconModifier = when (iconAnimation) {
        IconAnimation.PULSE -> Modifier.scale(iconPulse)
        IconAnimation.ROTATE -> Modifier.graphicsLayer { rotationZ = iconRotation }
        IconAnimation.TILT -> Modifier.graphicsLayer { rotationZ = iconTilt }
        IconAnimation.FIRE, IconAnimation.NONE -> Modifier
    }

    // Colors based on button type
    val containerColor = when {
        isFireButton || isPrimary -> MaterialTheme.colorScheme.primary
        else -> HomeButtonColors.Secondary
    }

    val contentColor = Color.White

    // Standard button (unified path for all button types)
    if (icon != null) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            containerColor = containerColor,
            contentColor = contentColor,
            interactionSource = interactionSource,
            modifier = modifier
                .fillMaxWidth()
                .height(buttonHeight)
                .scale(scale),
            icon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = iconModifier.then(Modifier.size(28.dp))
                )
            },
            text = { Text(label, style = MaterialTheme.typography.titleMedium) }
        )
    } else {
        ExtendedFloatingActionButton(
            onClick = onClick,
            containerColor = containerColor,
            contentColor = contentColor,
            interactionSource = interactionSource,
            modifier = modifier
                .fillMaxWidth()
                .height(buttonHeight)
                .scale(scale),
            content = { Text(label, style = MaterialTheme.typography.titleMedium) }
        )
    }
}
