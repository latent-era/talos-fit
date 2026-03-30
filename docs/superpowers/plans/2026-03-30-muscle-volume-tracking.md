# Muscle Volume Tracking Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a per-muscle-group weekly volume card to the analytics dashboard showing set counts with training zone indicators (Maintaining/Growth/Focus).

**Architecture:** New `MuscleVolumeCard` composable added to `InsightCards.kt`. Uses `ExerciseRepository.getExerciseById()` to resolve exercise → muscle groups. Counts working sets per group from the last 7 days. Renders 10-segment color bars with zone labels. Wired into `InsightsTab` as the 5th card.

**Tech Stack:** Kotlin, Compose Multiplatform, ExerciseRepository (suspend calls via LaunchedEffect)

---

### Task 1: Add MuscleVolumeCard composable

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/components/InsightCards.kt`

- [ ] **Step 1: Add the MuscleVolumeCard composable**

Add after the `VolumeByExerciseCard` composable in InsightCards.kt:

```kotlin
/**
 * Muscle Volume card — shows weekly set count per muscle group with training zones.
 * Zones: 0-4 Maintaining (blue), 5-9 Growth (green), 10+ Focus (gold).
 */
@Composable
fun MuscleVolumeCard(
    workoutSessions: List<WorkoutSession>,
    exerciseRepository: ExerciseRepository,
    modifier: Modifier = Modifier
) {
    val muscleGroups = listOf("All", "Chest", "Back", "Shoulders", "Arms", "Legs", "Core")
    var selectedFilter by remember { mutableStateOf("All") }

    // Resolve exercise IDs to muscle groups (async)
    val muscleCounts = produceState<Map<String, Int>>(initialValue = emptyMap(), workoutSessions) {
        val now = KmpUtils.currentTimeMillis()
        val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
        val recentSessions = workoutSessions.filter {
            it.timestamp >= now - sevenDaysMs && it.workingReps > 0
        }

        val counts = mutableMapOf<String, Int>()
        for (session in recentSessions) {
            val exerciseId = session.exerciseId ?: continue
            val exercise = exerciseRepository.getExerciseById(exerciseId) ?: continue
            val groups = exercise.muscleGroups.split(",").map { it.trim() }
            for (group in groups) {
                val normalized = when {
                    group.contains("CHEST", ignoreCase = true) -> "Chest"
                    group.contains("BACK", ignoreCase = true) -> "Back"
                    group.contains("SHOULDER", ignoreCase = true) -> "Shoulders"
                    group.contains("ARM", ignoreCase = true) -> "Arms"
                    group.contains("LEG", ignoreCase = true) -> "Legs"
                    group.contains("CORE", ignoreCase = true) -> "Core"
                    else -> null
                }
                if (normalized != null) {
                    counts[normalized] = (counts[normalized] ?: 0) + 1
                }
            }
        }
        value = counts
    }

    val filteredCounts = remember(muscleCounts.value, selectedFilter) {
        if (selectedFilter == "All") {
            muscleCounts.value.entries.sortedByDescending { it.value }
        } else {
            muscleCounts.value.entries.filter { it.key == selectedFilter }.sortedByDescending { it.value }
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
                "Muscle Volume",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Last 7 days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            // Filter chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                muscleGroups.forEach { group ->
                    val isSelected = selectedFilter == group
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = group },
                        label = {
                            Text(
                                group,
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

            if (filteredCounts.isEmpty() && muscleCounts.value.isEmpty()) {
                Text(
                    "No working sets in the last 7 days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (filteredCounts.isEmpty()) {
                Text(
                    "No sets for $selectedFilter this week",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                filteredCounts.forEach { (group, count) ->
                    MuscleGroupRow(group = group, setCount = count, target = 10)
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun MuscleGroupRow(group: String, setCount: Int, target: Int) {
    val zone = when {
        setCount >= target -> "Focus zone"
        setCount >= 5 -> "Growth zone"
        else -> "Maintaining"
    }
    val zoneColor = when {
        setCount >= target -> Color(0xFFD4A017) // Gold
        setCount >= 5 -> Color(0xFF4CAF50) // Green
        else -> Color(0xFF42A5F5) // Blue
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    group,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "$setCount of $target weekly sets",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                zone,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = zoneColor
            )
        }
        Spacer(Modifier.height(6.dp))

        // 10-segment color bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            val segments = target
            for (i in 0 until segments) {
                val isFilled = i < setCount
                val segmentColor = if (isFilled) {
                    when {
                        i >= target - 1 -> Color(0xFFD4A017) // Gold for last segments
                        i >= 5 -> Color(0xFF4CAF50).copy(alpha = 0.6f + (i - 5) * 0.08f) // Green gradient
                        else -> Color(0xFF42A5F5).copy(alpha = 0.7f + i * 0.06f) // Blue gradient
                    }
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(segmentColor)
                )
            }
        }
    }
}
```

- [ ] **Step 2: Add required import**

At the top of InsightCards.kt, add if not present:
```kotlin
import androidx.compose.runtime.produceState
```

- [ ] **Step 3: Build and verify**

Run: `./gradlew :androidApp:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/components/InsightCards.kt
git commit -m "feat(analytics): add muscle volume card with training zones"
```

---

### Task 2: Wire MuscleVolumeCard into InsightsTab

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/InsightsTab.kt`
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/AnalyticsScreen.kt`

- [ ] **Step 1: Re-add exerciseRepository parameter to InsightsTab**

Update the InsightsTab signature to accept exerciseRepository again:

```kotlin
@Composable
fun InsightsTab(
    prs: List<PersonalRecord>,
    workoutSessions: List<WorkoutSession>,
    exerciseRepository: ExerciseRepository,
    modifier: Modifier = Modifier,
    weightUnit: WeightUnit = WeightUnit.KG
)
```

Add the import at top if missing:
```kotlin
import com.devil.phoenixproject.data.repository.ExerciseRepository
```

- [ ] **Step 2: Add MuscleVolumeCard to the LazyColumn**

After the VolumeByExerciseCard item block, add:

```kotlin
        // 5. Muscle Volume
        if (workoutSessions.isNotEmpty()) {
            item {
                ResponsiveCardWrapper {
                    MuscleVolumeCard(
                        workoutSessions = workoutSessions,
                        exerciseRepository = exerciseRepository,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
```

- [ ] **Step 3: Update AnalyticsScreen call site**

In AnalyticsScreen.kt, update the InsightsTab call to pass exerciseRepository:

```kotlin
0 -> InsightsTab(
    prs = personalRecords,
    workoutSessions = workoutHistory,
    exerciseRepository = viewModel.exerciseRepository,
    weightUnit = weightUnit,
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
git commit -m "feat(analytics): wire muscle volume card into dashboard"
```

---

### Task 3: Deploy and verify

- [ ] **Step 1: Install on phone**

Run: `./gradlew :androidApp:installDebug`

- [ ] **Step 2: Verify**

Open Analytics tab. Scroll to the 5th card (Muscle Volume). Verify:
- "All" filter shows all muscle groups with set counts from last 7 days
- Tap "Chest" chip → only Chest row shown
- Color bars match set counts (blue segments for low, green for mid, gold for high)
- Zone labels: 0-4 = Maintaining, 5-9 = Growth, 10+ = Focus

- [ ] **Step 3: Commit and push**

```bash
git push origin main
```
