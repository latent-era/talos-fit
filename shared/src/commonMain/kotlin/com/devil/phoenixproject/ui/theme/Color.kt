package com.devil.phoenixproject.ui.theme

import androidx.compose.ui.graphics.Color

// ==============================================================================
// THEME: TALOS HEALTH
// Concept: AMOLED black + Rose accent for clean, modern health tracking
// ==============================================================================

// --- CORE BRAND COLORS ---
// Primary: "Talos Rose" - Used for FABs, Main Actions, Active States
val PhoenixOrangeLight = Color(0xFFE11D48)  // Rose dark (light mode)
val PhoenixOrangeDark = Color(0xFFF43F5E)   // Talos rose (dark mode)

// Accent rose (unified — no gradient)
val FlameOrange = Color(0xFFF43F5E)   // Rose accent
val FlameYellow = Color(0xFFF43F5E)   // Rose accent
val FlameRed = Color(0xFFF43F5E)      // Rose accent

// Secondary: "Rose Light" - Used for Secondary Actions, Toggles
val EmberYellowLight = Color(0xFFBE123C)    // Rose darker (light mode)
val EmberYellowDark = Color(0xFFFB7185)     // Rose light (dark mode)

// Tertiary: "Neutral Grey" - Used for accents to balance the rose
val AshBlueLight = Color(0xFF6B7280)        // Neutral grey (light mode)
val AshBlueDark = Color(0xFF9BA1A6)         // Neutral grey (dark mode)

// --- TALOS DARK PALETTE ---
// Talos design language dark neutrals
val Slate950 = Color(0xFF0D0E11)  // Talos background
val Slate900 = Color(0xFF16171B)  // Talos elevated/cards
val Slate800 = Color(0xFF1E2025)  // Talos surface2
val Slate700 = Color(0xFF2C2E33)  // Talos borders
val Slate400 = Color(0xFF9499A1)  // Talos text secondary
val Slate200 = Color(0xFFE6E8EB)  // Talos text primary
val Slate50 = Color(0xFFF8FAFC)   // Light mode background

// --- TALOS TEXT COLORS ---
val TalosTextPrimary = Color(0xFFE6E8EB)
val TalosTextSecondary = Slate400  // #9499A1
val TalosTextTertiary = Color(0xFF808691)

// --- SIGNAL COLORS (Status) ---
// Intentionally NOT orange to avoid confusion with primary
val SignalSuccess = Color(0xFF22C55E)  // Green
val SignalError = Color(0xFFEF4444)    // Red
val SignalWarning = Color(0xFFF59E0B)  // Amber

// --- MATERIAL 3 DARK MODE TOKENS ---
val Primary80 = PhoenixOrangeDark
val Primary20 = Color(0xFF4C0519)
val PrimaryContainerDark = Color(0xFF9F1239)
val OnPrimaryContainerDark = Color(0xFFFFE4E6)

val Secondary80 = EmberYellowDark
val Secondary20 = Color(0xFF4C0519)
val SecondaryContainerDark = Color(0xFF9F1239)
val OnSecondaryContainerDark = Color(0xFFFDA4AF)

val Tertiary80 = AshBlueDark
val Tertiary20 = Color(0xFF374151)

// --- MATERIAL 3 LIGHT MODE TOKENS ---
val PrimaryContainerLight = Color(0xFFFFE4E6)
val OnPrimaryContainerLight = Color(0xFF4C0519)

// --- SURFACE CONTAINERS (Dark Mode) ---
// Using Slate scale for depth without opacity hacks
val SurfaceDimDark = Slate950
val SurfaceContainerDark = Slate900
val SurfaceContainerHighDark = Slate800
val SurfaceContainerHighestDark = Slate700
val OnSurfaceDark = Color(0xFFE6E8EB)
val OnSurfaceVariantDark = Slate400

// --- SURFACE CONTAINERS (Light Mode) ---
val SurfaceDimLight = Color(0xFFDED8E1)
val SurfaceBrightLight = Color(0xFFFDF8FF)
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFF7F2FA)
val SurfaceContainerLight = Slate50
val SurfaceContainerHighLight = Slate200
val SurfaceContainerHighestLight = Color(0xFFE6E0E9)

// --- METRIC ICON COLORS ---
val MetricSteps = Color(0xFF4CB782)
val MetricHeartRate = Color(0xFFE5484D)
val MetricSleep = Color(0xFF52A9FF)
val MetricCalories = Color(0xFFE5B454)
val MetricHRV = Color(0xFF8B5CF6)
val MetricForce = Color(0xFFF43F5E)
val MetricPower = Color(0xFFF59E0B)
val MetricVelocity = Color(0xFF10B981)
