# Auto-Progression Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** After completing a set, show a progression suggestion ("Add 0.5kg next time" / "Same weight" / "Reduce 0.5kg") in the set summary with a toggle to auto-update the routine weight.

**Architecture:** Compute `ProgressionSuggestion` in `ActiveSessionEngine.handleSetCompletion()` using rep count vs target and safety events. Add the suggestion as a new field on `WorkoutState.SetSummary`. Display in `SetSummaryCard` with a toggle that calls `RoutineFlowManager` to update the exercise weight.

**Tech Stack:** Kotlin, Compose Multiplatform

---

### Task 1: Add ProgressionSuggestion to data model

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/domain/model/Models.kt`

- [ ] **Step 1: Add the ProgressionSuggestion enum and field**

Add above the `WorkoutState` sealed class (around line 40):

```kotlin
enum class ProgressionDirection { INCREASE, HOLD, DECREASE }

data class ProgressionSuggestion(
    val direction: ProgressionDirection,
    val deltaKg: Float,
    val message: String
)
```

- [ ] **Step 2: Add progressionSuggestion field to SetSummary**

Add a new field to the `WorkoutState.SetSummary` data class (after the `peakWeightKg` field, around line 87):

```kotlin
    val peakWeightKg: Float = 0f,
    val progressionSuggestion: ProgressionSuggestion? = null
) : WorkoutState()
```

- [ ] **Step 3: Build and verify**

Run: `./gradlew :androidApp:assembleDebug`
Expected: BUILD SUCCESSFUL (new field has default null, so no callers break)

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/domain/model/Models.kt
git commit -m "feat(progression): add ProgressionSuggestion data model"
```

---

### Task 2: Compute progression suggestion at set completion

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/manager/ActiveSessionEngine.kt`

- [ ] **Step 1: Add computeProgressionSuggestion function**

Add a private function to ActiveSessionEngine (near the other calculation helpers, around line 276):

```kotlin
    /**
     * Compute a progression suggestion based on completed set performance.
     * - Safety events (spotter/deload) → decrease
     * - Missed reps → hold
     * - All reps hit → increase by 0.5kg
     */
    private fun computeProgressionSuggestion(
        workingReps: Int,
        targetReps: Int,
        safetyFlags: Int,
        isJustLift: Boolean
    ): ProgressionSuggestion? {
        // No suggestion for Just Lift or AMRAP (no fixed target)
        if (isJustLift || targetReps <= 0) return null

        val hadSafetyEvent = safetyFlags > 0

        return when {
            hadSafetyEvent -> ProgressionSuggestion(
                direction = ProgressionDirection.DECREASE,
                deltaKg = 0.5f,
                message = "Reduce 0.5kg — safety event detected"
            )
            workingReps < targetReps -> ProgressionSuggestion(
                direction = ProgressionDirection.HOLD,
                deltaKg = 0f,
                message = "Same weight, aim for $targetReps reps"
            )
            else -> ProgressionSuggestion(
                direction = ProgressionDirection.INCREASE,
                deltaKg = 0.5f,
                message = "Add 0.5kg next time"
            )
        }
    }
```

- [ ] **Step 2: Call it in handleSetCompletion and attach to summary**

In `handleSetCompletion()`, after the summary is computed (around line 1908), add the progression suggestion:

Find this block (around line 1900-1908):
```kotlin
            val summary = calculateSetSummaryMetrics(
                metrics = metricsList,
                repCount = completedReps,
                fallbackWeightKg = params.weightPerCableKg,
                configuredWeightKgPerCable = params.weightPerCableKg,
                isEchoMode = params.isEchoMode,
                warmupRepsCount = warmupReps,
                workingRepsCount = completedReps
            )
```

Add after it:
```kotlin
            val progression = computeProgressionSuggestion(
                workingReps = completedReps,
                targetReps = params.reps,
                safetyFlags = coordinator.currentSafetyFlags,
                isJustLift = isJustLift
            )
            val summaryWithProgression = if (progression != null) {
                summary.copy(progressionSuggestion = progression)
            } else {
                summary
            }
```

Then update ALL references to `summary` in the rest of `handleSetCompletion` to use `summaryWithProgression` instead. Specifically:
- Line ~1923: `coordinator._workoutState.value = summary` → `coordinator._workoutState.value = summaryWithProgression`

- [ ] **Step 3: Verify coordinator.currentSafetyFlags exists**

Check if `WorkoutCoordinator` has a `currentSafetyFlags` field. If not, add a simple counter. Search for where safety events are tracked. The simplest approach: add a mutable `var currentSafetyFlags: Int = 0` to WorkoutCoordinator and increment it when deload/spotter events occur. Reset it in `startWorkout()`.

If this field doesn't exist, add it to `WorkoutCoordinator`:
```kotlin
var currentSafetyFlags: Int = 0
```

And reset it in `startWorkout()` (in ActiveSessionEngine) alongside the other resets:
```kotlin
coordinator.currentSafetyFlags = 0
```

- [ ] **Step 4: Build and verify**

Run: `./gradlew :androidApp:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/manager/ActiveSessionEngine.kt
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/manager/WorkoutCoordinator.kt
git commit -m "feat(progression): compute suggestion at set completion"
```

---

### Task 3: Add progression UI to SetSummaryCard

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/SetSummaryCard.kt`

- [ ] **Step 1: Add the progression suggestion section**

In SetSummaryCard, after the RPE section (around line 299) and before the Done button (around line 303), add:

```kotlin
            // Progression suggestion
            if (!isHistoryView && summary.progressionSuggestion != null) {
                val suggestion = summary.progressionSuggestion
                var applyProgression by remember { mutableStateOf(suggestion.direction == ProgressionDirection.INCREASE) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            val (icon, color) = when (suggestion.direction) {
                                ProgressionDirection.INCREASE -> Icons.Default.TrendingUp to Color(0xFF4CAF50)
                                ProgressionDirection.HOLD -> Icons.Default.TrendingFlat to MaterialTheme.colorScheme.onSurfaceVariant
                                ProgressionDirection.DECREASE -> Icons.Default.TrendingDown to Color(0xFFFF9800)
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = suggestion.message,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = color
                            )
                        }

                        if (onProgressionApplied != null) {
                            Switch(
                                checked = applyProgression,
                                onCheckedChange = { checked ->
                                    applyProgression = checked
                                    if (checked) {
                                        onProgressionApplied(suggestion)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = when (suggestion.direction) {
                                        ProgressionDirection.INCREASE -> Color(0xFF4CAF50)
                                        ProgressionDirection.DECREASE -> Color(0xFFFF9800)
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                )
                            )
                        }
                    }
                }
            }
```

- [ ] **Step 2: Add onProgressionApplied callback to SetSummaryCard signature**

Update the function signature to include the callback:

```kotlin
@Composable
fun SetSummaryCard(
    summary: WorkoutState.SetSummary,
    workoutMode: String,
    weightUnit: WeightUnit,
    kgToDisplay: (Float, WeightUnit) -> Float,
    formatWeight: (Float, WeightUnit) -> String,
    onContinue: () -> Unit,
    autoplayEnabled: Boolean,
    summaryCountdownSeconds: Int,
    onRpeLogged: ((Int) -> Unit)? = null,
    isHistoryView: Boolean = false,
    savedRpe: Int? = null,
    buttonLabel: String = "Done",
    onProgressionApplied: ((ProgressionSuggestion) -> Unit)? = null
)
```

Add required imports at top:
```kotlin
import com.devil.phoenixproject.domain.model.ProgressionSuggestion
import com.devil.phoenixproject.domain.model.ProgressionDirection
```

- [ ] **Step 3: Build and verify**

Run: `./gradlew :androidApp:assembleDebug`
Expected: BUILD SUCCESSFUL (new parameter has default null, existing callers unaffected)

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/SetSummaryCard.kt
git commit -m "feat(progression): add suggestion UI with toggle to set summary card"
```

---

### Task 4: Wire the progression toggle to update routine weight

**Files:**
- Modify: the screen that hosts SetSummaryCard (find where SetSummaryCard is called and pass the callback)
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/manager/RoutineFlowManager.kt`

- [ ] **Step 1: Add applyProgression method to RoutineFlowManager**

Add this method to RoutineFlowManager:

```kotlin
    /**
     * Apply a progression suggestion to the current exercise in the loaded routine.
     * Updates weightPerCableKg by the suggestion's delta and saves the routine.
     */
    fun applyProgression(suggestion: ProgressionSuggestion) {
        val routine = coordinator._loadedRoutine.value ?: return
        val exerciseIndex = coordinator._currentExerciseIndex.value
        val exercise = routine.exercises.getOrNull(exerciseIndex) ?: return

        val delta = when (suggestion.direction) {
            ProgressionDirection.INCREASE -> suggestion.deltaKg
            ProgressionDirection.DECREASE -> -suggestion.deltaKg
            ProgressionDirection.HOLD -> return
        }
        val newWeight = (exercise.weightPerCableKg + delta).coerceAtLeast(0f)

        val updatedExercises = routine.exercises.toMutableList()
        updatedExercises[exerciseIndex] = exercise.copy(weightPerCableKg = newWeight)
        val updatedRoutine = routine.copy(exercises = updatedExercises)

        coordinator._loadedRoutine.value = updatedRoutine

        // Persist to database
        scope.launch {
            workoutRepository.updateRoutine(updatedRoutine)
        }
    }
```

Add import:
```kotlin
import com.devil.phoenixproject.domain.model.ProgressionSuggestion
import com.devil.phoenixproject.domain.model.ProgressionDirection
```

- [ ] **Step 2: Verify updateRoutine exists on workoutRepository**

Check if `workoutRepository.updateRoutine(routine)` exists. If not, check for `routineRepository` or similar. The method needs to save the updated routine with the new exercise weight. Search for the actual save method and use it. It may be called `saveRoutine`, `updateRoutine`, or exist on a `RoutineRepository`.

- [ ] **Step 3: Expose applyProgression through the ViewModel**

In the caller that renders SetSummaryCard, pass the callback. Find where SetSummaryCard is called (likely in `ActiveWorkoutScreen.kt` or `WorkoutTab.kt` or `RoutineFlowScreen.kt`) and add:

```kotlin
onProgressionApplied = { suggestion ->
    viewModel.workoutSessionManager.routineFlowManager.applyProgression(suggestion)
}
```

- [ ] **Step 4: Build and verify**

Run: `./gradlew :androidApp:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(progression): wire toggle to update routine exercise weight"
```

---

### Task 5: Deploy and verify

- [ ] **Step 1: Install on phone**

Run: `./gradlew :androidApp:installDebug`

- [ ] **Step 2: Verify progression flow**

1. Start a routine workout
2. Complete all sets of an exercise, hitting target reps
3. Set summary should show "Add 0.5kg next time" with toggle ON
4. Let the toggle stay ON
5. Continue to next exercise or finish
6. Start the same routine again → the exercise weight should be 0.5kg higher

- [ ] **Step 3: Verify edge cases**

1. Miss reps on a set → should show "Same weight, aim for X reps"
2. Just Lift mode → no progression suggestion shown

- [ ] **Step 4: Commit and push**

```bash
git push origin main
```
