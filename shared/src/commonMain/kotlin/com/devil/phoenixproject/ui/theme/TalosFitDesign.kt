package com.devil.phoenixproject.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon

/**
 * Talos Fit Design System
 * Central design tokens and composable helpers for the Talos visual language.
 */
object TalosFitDesign {

    // --- CARD BORDER ---
    val cardBorder: BorderStroke
        @Composable get() = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

    // --- METRIC COLORS ---
    val metricColors = mapOf(
        "steps" to MetricSteps,
        "heart_rate" to MetricHeartRate,
        "sleep" to MetricSleep,
        "calories" to MetricCalories,
        "hrv" to MetricHRV,
        "force" to MetricForce,
        "power" to MetricPower,
        "velocity" to MetricVelocity
    )
}

/**
 * Talos section header — uppercase, tracked, tertiary color.
 * Usage: TalosSectionHeader("YOUR ROUTINES") or TalosSectionHeader("PERSONAL RECORDS", trailingText = "5d streak")
 */
@Composable
fun TalosSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailingText: String? = null,
    trailingColor: Color? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = TalosTextTertiary,
            letterSpacing = 1.5.sp
        )
        if (trailingText != null) {
            val badgeColor = trailingColor ?: MaterialTheme.colorScheme.primary
            Text(
                text = trailingText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = badgeColor,
                modifier = Modifier
                    .background(
                        color = badgeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

/**
 * Talos icon badge — colored icon with tinted background.
 * Used in metric cards, list items, quick actions.
 */
@Composable
fun TalosIconBadge(
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    iconSize: Dp = 18.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .background(
                color = color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Talos metric value display — large bold colored number with unit.
 */
@Composable
fun TalosMetricValue(
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (unit != null) {
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = TalosTextSecondary,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}
