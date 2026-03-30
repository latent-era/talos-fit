# Auto-Progression Design

## Context

Users tend to stagnate at the same weight without a nudge to progress. The Vitruvian captures rich data (reps, velocity, force, safety events) that can drive intelligent progression suggestions. This feature adds a progression suggestion to the set summary screen with a toggle to auto-apply weight changes to the routine.

## Feature: Progression Suggestion in Set Summary

After completing all sets of an exercise, show a progression recommendation in the SetSummaryCard with a toggle to auto-update the routine weight.

### Decision Logic

Uses data already in `WorkoutState.SetSummary` and `WorkoutSession`:

```
IF spotterActivations > 0 OR deloadWarningCount > 1:
    → DECREASE: "Reduce 0.5kg — safety events detected" (amber)

ELIF workingReps < targetReps (on any set):
    → HOLD: "Same weight, aim for [target] reps" (neutral/white)

ELIF workingReps >= targetReps on ALL sets:
    → INCREASE: "Add 0.5kg next time" (green)
```

Future enhancement (not in v1): velocity degradation check using MetricSample data.

### Increment

0.5kg — the Vitruvian's minimum weight step. Hardcoded for now.

### UI

**Location:** Bottom of `SetSummaryCard` (or the set summary screen), below existing metrics.

**Layout:**
- Horizontal row with:
  - Direction icon: ↑ (green), → (neutral), ↓ (amber)
  - Text: "Add 0.5kg next time" / "Same weight, aim for 6 reps" / "Reduce 0.5kg"
  - Toggle switch (right-aligned)
- Toggle default: ON for INCREASE suggestions, OFF for DECREASE suggestions
- When toggled ON: updates `RoutineExercise.weightPerCableKg` for the current exercise in the loaded routine

### Applying the Update

When the toggle is ON and the suggestion is INCREASE or DECREASE:
1. Get the current `RoutineExercise` for this exercise from the loaded routine
2. Update `weightPerCableKg` by ±0.5kg
3. Persist to database via the routine repository
4. Next time the routine is loaded, the weight is already adjusted

For Just Lift mode (no routine): show the suggestion as informational only — no toggle, no persistence.

### Data Sources

All from existing captured data — no new data collection:
- `WorkoutState.SetSummary.repCount` — actual reps performed
- `WorkoutParameters.reps` — target reps
- `WorkoutSession.spotterActivations` — safety event count
- `WorkoutSession.deloadWarningCount` — deload warning count
- Current routine exercise's `weightPerCableKg` — for calculating new weight

## Files to Modify

- `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/SetSummaryCard.kt` — add progression suggestion UI
- `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/manager/ActiveSessionEngine.kt` — compute progression suggestion at set completion
- `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/manager/RoutineFlowManager.kt` — apply weight update to routine exercise
- `shared/src/commonMain/kotlin/com/devil/phoenixproject/domain/model/Models.kt` — add ProgressionSuggestion data class to WorkoutState.SetSummary

## Verification

1. Build compiles
2. Complete a set where all reps hit target → see "Add 0.5kg next time" with toggle ON
3. Toggle stays ON → start next session of same routine → weight is 0.5kg higher
4. Complete a set with missed reps → see "Same weight" suggestion
5. Trigger spotter activation → see "Reduce 0.5kg" suggestion with toggle OFF
6. In Just Lift mode → suggestion shown but no toggle (informational only)
