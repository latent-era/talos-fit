# Analytics Dashboard Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the 6 broken/misleading analytics cards with 4 focused, accurate ones: fixed weekly summary, progressive overload chart, workout frequency bars, and volume by exercise.

**Architecture:** Replace `InsightsTab` content entirely. Fix `ThisWeekSummaryCard` counting. Add 3 new card composables to `InsightCards.kt`. Create 1 new chart component (`OverloadLineChart`). Reuse `VolumeTrendChart` period selector pattern. Data comes from existing `WorkoutSession` list — no new queries needed.

**Tech Stack:** Kotlin, Compose Multiplatform, Canvas API for charts

---

### Task 1: Fix ThisWeekSummaryCard workout counting

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/components/InsightCards.kt` (lines 72-119)

- [ ] **Step 1: Fix the workout count calculation**

In `InsightCards.kt`, replace the workout counting logic inside `ThisWeekSummaryCard`. The current code at line 101 uses `thisWeekSessions.size` which counts individual sets. Fix it to group by `routineSessionId`:

Replace lines 100-106:
```kotlin
        val thisWeekSummary = WeekSummary(
            workouts = thisWeekSessions.size,
            totalVolume = thisWeekSessions.sumOf {
                it.effectiveTotalVolumeKg().toDouble()
            }.toFloat(),
            totalReps = thisWeekSessions.sumOf { it.totalReps },
            prsHit = thisWeekPRs.size
        )
```

With:
```kotlin
        val thisWeekSummary = WeekSummary(
            workouts = countRealWorkouts(thisWeekSessions),
            totalVolume = thisWeekSessions.sumOf {
                it.effectiveTotalVolumeKg().toDouble()
            }.toFloat(),
            totalReps = thisWeekSessions.sumOf { it.totalReps },
            prsHit = thisWeekPRs.size
        )
```

And replace lines 109-116 similarly:
```kotlin
        val lastWeekSummary = WeekSummary(
            workouts = countRealWorkouts(lastWeekSessions),
            totalVolume = lastWeekSessions.sumOf {
                it.effectiveTotalVolumeKg().toDouble()
            }.toFloat(),
            totalReps = lastWeekSessions.sumOf { it.totalReps },
            prsHit = lastWeekPRs.size
        )
```

- [ ] **Step 2: Add the countRealWorkouts helper**

Add this private function above `ThisWeekSummaryCard` (around line 70):

```kotlin
/**
 * Count real workout sessions by grouping sets with the same routineSessionId.
 * Sets without a routineSessionId (ad-hoc lifts) count as 1 workout each.
 */
private fun countRealWorkouts(sessions: List<WorkoutSession>): Int {
    val (routineSets, adHocSets) = sessions.partition { it.routineSessionId != null }
    val routineWorkouts = routineSets.map { it.routineSessionId }.distinct().size
    return routineWorkouts + adHocSets.size
}
```

- [ ] **Step 3: Build and verify**

Run: `./gradlew :androidApp:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/components/InsightCards.kt
git commit -m "fix(analytics): count real workouts not individual sets in weekly summary"
```

---

### Task 2: Create ProgressiveOverloadCard

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/components/InsightCards.kt`

- [ ] **Step 1: Add the ProgressiveOverloadCard composable**

Add after the `ThisWeekSummaryCard` composable (after the closing brace of `WeekStatRow`, around line 300). This is the hero card — a line chart showing top set weight per exercise over time with a dropdown selector:

```kotlin
/**
 * Progressive Overload Chart — shows heaviest working set weight per exercise over time.
 * Dropdown selects the exercise; line chart plots max(weightPerCableKg) per session date.
 */
@Composable
fun ProgressiveOverloadCard(
    workoutSessions: List<WorkoutSession>,
    weightUnit: WeightUnit,
    modifier: Modifier = Modifier
) {
    // Get all exercises sorted by most recently trained
    val exercises = remember(workoutSessions) {
        workoutSessions
            .filter { !it.exerciseName.isNullOrBlank() }
            .groupBy { it.exerciseName!! }
            .entries
            .sortedByDescending { entry -> entry.value.maxOf { it.timestamp } }
            .map { it.key }
    }

    var selectedExercise by remember(exercises) {
        mutableStateOf(exercises.firstOrNull() ?: "")
    }

    // Build data points: date → max weight for selected exercise
    val dataPoints = remember(workoutSessions, selectedExercise) {
        if (selectedExercise.isBlank()) return@remember emptyList()
        workoutSessions
            .filter { it.exerciseName == selectedExercise }
            .groupBy { session ->
                Instant.fromEpochMilliseconds(session.timestamp)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
            }
            .map { (date, sessions) ->
                val maxWeight = sessions.maxOf { it.weightPerCableKg }
                date to maxWeight
            }
            .sortedBy { it.first }
    }

    val convertedPoints = remember(dataPoints, weightUnit) {
        dataPoints.map { (date, kg) ->
            val displayWeight = if (weightUnit == WeightUnit.LB) kg * 2.20462f else kg
            date to displayWeight
        }
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Progressive Overload",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Top set weight per session",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            // Exercise dropdown
            if (exercises.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedExercise,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        exercises.forEach { exercise ->
                            DropdownMenuItem(
                                text = { Text(exercise, style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    selectedExercise = exercise
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Line chart
            if (convertedPoints.size >= 2) {
                val unitLabel = if (weightUnit == WeightUnit.LB) "lbs" else "kg"
                OverloadLineChart(
                    dataPoints = convertedPoints,
                    unitLabel = unitLabel,
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
            } else if (convertedPoints.size == 1) {
                Text(
                    "Need at least 2 sessions to show progression",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            } else {
                Text(
                    "No data for this exercise",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            }
        }
    }
}
```

- [ ] **Step 2: Add the OverloadLineChart composable**

Add this below `ProgressiveOverloadCard` — a simple Canvas-based line chart:

```kotlin
@Composable
private fun OverloadLineChart(
    dataPoints: List<Pair<LocalDate, Float>>,
    unitLabel: String,
    modifier: Modifier = Modifier
) {
    if (dataPoints.isEmpty()) return

    val primary = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val textStyle = MaterialTheme.typography.labelSmall

    val minVal = dataPoints.minOf { it.second }
    val maxVal = dataPoints.maxOf { it.second }
    val range = (maxVal - minVal).coerceAtLeast(1f)

    Canvas(modifier = modifier) {
        val leftPadding = 48f
        val bottomPadding = 40f
        val chartWidth = size.width - leftPadding
        val chartHeight = size.height - bottomPadding

        // Draw Y-axis labels
        val ySteps = 4
        for (i in 0..ySteps) {
            val value = minVal + (range * i / ySteps)
            val y = chartHeight - (chartHeight * i / ySteps)
            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    "${value.toInt()}",
                    4f, y + 4f,
                    android.graphics.Paint().apply {
                        color = onSurfaceVariant.hashCode()
                        textSize = 28f
                        isAntiAlias = true
                    }
                )
            }
            // Grid line
            drawLine(
                color = onSurfaceVariant.copy(alpha = 0.15f),
                start = androidx.compose.ui.geometry.Offset(leftPadding, y),
                end = androidx.compose.ui.geometry.Offset(size.width, y),
                strokeWidth = 1f
            )
        }

        // Plot points and lines
        val points = dataPoints.mapIndexed { index, (_, value) ->
            val x = leftPadding + (chartWidth * index / (dataPoints.size - 1).coerceAtLeast(1))
            val y = chartHeight - (chartHeight * (value - minVal) / range)
            androidx.compose.ui.geometry.Offset(x, y)
        }

        // Draw line
        for (i in 0 until points.size - 1) {
            drawLine(
                color = primary,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 3f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }

        // Draw dots
        points.forEach { point ->
            drawCircle(color = primary, radius = 6f, center = point)
        }
    }
}
```

- [ ] **Step 3: Add required imports at top of InsightCards.kt**

Add these imports if not already present:
```kotlin
import androidx.compose.foundation.Canvas
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MenuAnchorType
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
```

- [ ] **Step 4: Build and verify**

Run: `./gradlew :androidApp:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/components/InsightCards.kt
git commit -m "feat(analytics): add progressive overload chart with exercise dropdown"
```

---

### Task 3: Create WorkoutFrequencyCard

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/components/InsightCards.kt`

- [ ] **Step 1: Add the WorkoutFrequencyCard composable**

Add after `OverloadLineChart`:

```kotlin
/**
 * Workout Frequency — bar chart showing real workout sessions per week for last 8 weeks.
 */
@Composable
fun WorkoutFrequencyCard(
    workoutSessions: List<WorkoutSession>,
    modifier: Modifier = Modifier
) {
    val weeklyData = remember(workoutSessions) {
        val now = Instant.fromEpochMilliseconds(KmpUtils.currentTimeMillis())
        val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val weeks = (0 until 8).reversed().map { weeksAgo ->
            val weekStart = today.minus(weeksAgo * 7 + today.dayOfWeek.ordinal, DateTimeUnit.DAY)
            val weekEnd = weekStart.plus(7, DateTimeUnit.DAY)
            val startMs = weekStart.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
            val endMs = weekEnd.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()

            val weekSessions = workoutSessions.filter { it.timestamp >= startMs && it.timestamp < endMs }
            val count = countRealWorkouts(weekSessions)

            val label = "${weekStart.dayOfMonth} ${weekStart.month.name.take(3)}"
            label to count
        }
        weeks
    }

    val target = 3

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Workout Frequency",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Sessions per week (target: $target)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            FrequencyBarChart(
                weeklyData = weeklyData,
                target = target,
                modifier = Modifier.fillMaxWidth().height(160.dp)
            )
        }
    }
}

@Composable
private fun FrequencyBarChart(
    weeklyData: List<Pair<String, Int>>,
    target: Int,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    val targetColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val maxCount = (weeklyData.maxOfOrNull { it.second } ?: 1).coerceAtLeast(target + 1)

    Canvas(modifier = modifier) {
        val bottomPadding = 40f
        val chartHeight = size.height - bottomPadding
        val barWidth = size.width / weeklyData.size * 0.6f
        val gap = size.width / weeklyData.size

        // Target line
        val targetY = chartHeight - (chartHeight * target / maxCount)
        drawLine(
            color = targetColor,
            start = androidx.compose.ui.geometry.Offset(0f, targetY),
            end = androidx.compose.ui.geometry.Offset(size.width, targetY),
            strokeWidth = 2f,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        )

        // Bars
        weeklyData.forEachIndexed { index, (_, count) ->
            val isCurrentWeek = index == weeklyData.size - 1
            val barHeight = chartHeight * count / maxCount
            val x = gap * index + (gap - barWidth) / 2

            drawRoundRect(
                color = if (isCurrentWeek) primary else muted,
                topLeft = androidx.compose.ui.geometry.Offset(x, chartHeight - barHeight),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
        }

        // X-axis labels
        weeklyData.forEachIndexed { index, (label, _) ->
            val x = gap * index + gap / 2
            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    label,
                    x - 20f, size.height - 4f,
                    android.graphics.Paint().apply {
                        color = onSurfaceVariant.hashCode()
                        textSize = 22f
                        isAntiAlias = true
                    }
                )
            }
        }
    }
}
```

- [ ] **Step 2: Make countRealWorkouts internal**

Change `countRealWorkouts` from `private` to `internal` so both `ThisWeekSummaryCard` and `WorkoutFrequencyCard` can use it:

```kotlin
internal fun countRealWorkouts(sessions: List<WorkoutSession>): Int {
```

- [ ] **Step 3: Add DateTimeUnit import if missing**

```kotlin
import kotlinx.datetime.DateTimeUnit
```

- [ ] **Step 4: Build and verify**

Run: `./gradlew :androidApp:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/components/InsightCards.kt
git commit -m "feat(analytics): add workout frequency bar chart with weekly target"
```

---

### Task 4: Create VolumeByExerciseCard

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/components/InsightCards.kt`

- [ ] **Step 1: Add the VolumeByExerciseCard composable**

Add after `FrequencyBarChart`:

```kotlin
/**
 * Volume by Exercise — horizontal bar chart showing total volume per exercise for a selectable period.
 */
@Composable
fun VolumeByExerciseCard(
    workoutSessions: List<WorkoutSession>,
    weightUnit: WeightUnit,
    modifier: Modifier = Modifier
) {
    var selectedPeriod by remember { mutableStateOf("This Month") }
    val periods = listOf("This Week", "This Month", "All Time")

    val filteredSessions = remember(workoutSessions, selectedPeriod) {
        val now = KmpUtils.currentTimeMillis()
        val oneDayMs = 24L * 60 * 60 * 1000
        when (selectedPeriod) {
            "This Week" -> workoutSessions.filter { it.timestamp >= now - 7 * oneDayMs }
            "This Month" -> workoutSessions.filter { it.timestamp >= now - 30 * oneDayMs }
            else -> workoutSessions
        }
    }

    val exerciseVolumes = remember(filteredSessions, weightUnit) {
        filteredSessions
            .filter { !it.exerciseName.isNullOrBlank() }
            .groupBy { it.exerciseName!! }
            .map { (name, sessions) ->
                val totalKg = sessions.sumOf { it.effectiveTotalVolumeKg().toDouble() }.toFloat()
                val display = if (weightUnit == WeightUnit.LB) totalKg * 2.20462f else totalKg
                name to display
            }
            .sortedByDescending { it.second }
    }

    val unitLabel = if (weightUnit == WeightUnit.LB) "lbs" else "kg"

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Volume by Exercise",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            // Period chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                periods.forEach { period ->
                    val isSelected = selectedPeriod == period
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedPeriod = period },
                        label = {
                            Text(
                                period,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.White
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White,
                            containerColor = Color.Transparent
                        ),
                        border = if (!isSelected) FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = false,
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                            borderWidth = 1.dp
                        ) else null
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            if (exerciseVolumes.isEmpty()) {
                Text(
                    "No workouts in this period",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val maxVolume = exerciseVolumes.first().second.coerceAtLeast(1f)
                exerciseVolumes.forEach { (name, volume) ->
                    ExerciseVolumeRow(
                        exerciseName = name,
                        volume = volume,
                        maxVolume = maxVolume,
                        unitLabel = unitLabel
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ExerciseVolumeRow(
    exerciseName: String,
    volume: Float,
    maxVolume: Float,
    unitLabel: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                exerciseName.trim(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                "${volume.toInt()} $unitLabel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (volume / maxVolume).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}
```

- [ ] **Step 2: Build and verify**

Run: `./gradlew :androidApp:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/components/InsightCards.kt
git commit -m "feat(analytics): add volume by exercise card with period selector"
```

---

### Task 5: Rewire InsightsTab to use new cards

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/InsightsTab.kt`

- [ ] **Step 1: Replace InsightsTab card layout**

Replace the entire `LazyColumn` content (lines 89-264) with the 4 new cards. Keep the header, remove time period filter chips (the overload chart has its own exercise selector, volume card has its own period selector):

```kotlin
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
        item {
            Text(
                text = "DASHBOARD",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Your training overview",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. This Week Summary (fixed counting)
        item {
            ResponsiveCardWrapper {
                ThisWeekSummaryCard(
                    workoutSessions = workoutSessions,
                    personalRecords = prs,
                    weightUnit = weightUnit,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 2. Progressive Overload (hero chart)
        if (workoutSessions.isNotEmpty()) {
            item {
                ResponsiveCardWrapper {
                    ProgressiveOverloadCard(
                        workoutSessions = workoutSessions,
                        weightUnit = weightUnit,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 3. Workout Frequency
        if (workoutSessions.isNotEmpty()) {
            item {
                ResponsiveCardWrapper {
                    WorkoutFrequencyCard(
                        workoutSessions = workoutSessions,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 4. Volume by Exercise
        if (workoutSessions.isNotEmpty()) {
            item {
                ResponsiveCardWrapper {
                    VolumeByExerciseCard(
                        workoutSessions = workoutSessions,
                        weightUnit = weightUnit,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Empty state
        if (prs.isEmpty() && workoutSessions.isEmpty()) {
            item {
                ResponsiveCardWrapper {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Insights,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Insights Yet",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Complete workouts to unlock insights",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
```

- [ ] **Step 2: Remove unused imports and state**

Remove from InsightsTab.kt:
- `var selectedPeriod` state variable and `filteredSessions` derivation (lines 67-87)
- Import for `HistoryTimePeriod` (line 22)
- The `selectedPeriod` / filter chip code is no longer needed

The `exerciseRepository` parameter can also be removed from the `InsightsTab` signature since no card uses it anymore. Update the call site in `AnalyticsScreen.kt` too.

- [ ] **Step 3: Update AnalyticsScreen.kt call site**

In `AnalyticsScreen.kt` at lines 389-396, remove the `exerciseRepository` parameter:

```kotlin
0 -> InsightsTab(
    prs = personalRecords,
    workoutSessions = workoutHistory,
    weightUnit = weightUnit,
    formatWeight = viewModel::formatWeight,
    modifier = Modifier.fillMaxSize()
)
```

- [ ] **Step 4: Build and verify**

Run: `./gradlew :androidApp:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/InsightsTab.kt
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/AnalyticsScreen.kt
git commit -m "feat(analytics): replace dashboard with 4 focused cards"
```

---

### Task 6: Remove dead card composables and unused charts

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/components/InsightCards.kt`
- Check: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/components/charts/` files

- [ ] **Step 1: Remove unused card composables from InsightCards.kt**

Delete these composables (they are no longer called from any screen):
- `MuscleBalanceRadarCard` (lines ~302-397)
- `ConsistencyGaugeCard` (lines ~399-446)
- `VolumeVsIntensityCard` (lines ~449-520)
- `WorkoutModeDistributionCard` (lines ~523-573)
- `TotalVolumeCard` (lines ~576-620)

Keep: `ThisWeekSummaryCard`, `LifetimeStatsCard`, `NextBadgeProgressCard`, and all the new cards.

- [ ] **Step 2: Check chart component usage**

Search the codebase for references to `RadarChart`, `GaugeChart`, `ComboChart`, `CircleChart`/`MuscleGroupCircleChart`. If they are ONLY used by the removed cards, delete those chart files too:
- `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/components/charts/RadarChart.kt`
- `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/components/charts/GaugeChart.kt`
- `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/components/charts/ComboChart.kt`
- `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/components/charts/CircleChart.kt`

Keep `VolumeTrendChart.kt` — it contains the `HistoryTimePeriod` enum which may be used elsewhere.

- [ ] **Step 3: Remove unused imports**

Clean up any orphaned imports in `InsightCards.kt` from the removed cards.

- [ ] **Step 4: Build and verify**

Run: `./gradlew :androidApp:assembleDebug`
Expected: BUILD SUCCESSFUL — if anything fails, a removed chart is still referenced somewhere. Restore it and check.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor(analytics): remove unused insight cards and chart components"
```

---

### Task 7: Deploy and verify on device

- [ ] **Step 1: Install on phone**

Run: `./gradlew :androidApp:installDebug`

- [ ] **Step 2: Verify all 4 cards**

Open the app → Analytics tab:
1. **Weekly Summary** — should show "1 workout" for this week (not "15 sets")
2. **Progressive Overload** — select "Incline Bench Press" from dropdown, should see data points at 30kg and 26kg across sessions
3. **Workout Frequency** — should show bars for the last 8 weeks, with 1 workout each for the week of Mar 24 and Mar 30
4. **Volume by Exercise** — select "This Month", should show all 5 exercises ranked by volume with Bent Over Row near the top

- [ ] **Step 3: Final commit with duration fix**

```bash
git add -A
git commit -m "feat(analytics): complete dashboard redesign — overload, frequency, volume cards"
```
