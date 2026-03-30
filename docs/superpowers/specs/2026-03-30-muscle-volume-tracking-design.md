# Muscle Volume Tracking Design

## Context

Users have no visibility into whether they're training each muscle group enough for growth. The app tracks sets per exercise but doesn't aggregate by muscle group or show whether weekly volume is in a productive range. This feature adds per-muscle volume tracking with evidence-based training zones.

## Feature: Muscle Volume Card

A new analytics card in InsightsTab showing weekly set count per muscle group with training zone indicators.

### Data Model

Uses existing data — no schema changes:
- `WorkoutSession.exerciseId` → look up exercise via `ExerciseRepository`
- `Exercise.muscleGroups` — comma-separated string (e.g., "ARMS,CHEST")
- 6 muscle groups: CHEST, BACK, SHOULDERS, ARMS, LEGS, CORE
- Each `WorkoutSession` row with `workingReps > 0` = 1 set
- A set counts toward ALL muscle groups listed in the exercise's `muscleGroups`

### Training Zones

Based on hypertrophy research (Schoenfeld 2017, Baz-Valle 2022):
- **0-4 sets/week** — Maintaining (blue) — won't gain or lose
- **5-9 sets/week** — Growth zone (green) — productive stimulus
- **10+ sets/week** — Focus zone (gold) — continued growth, diminishing returns

Target line at 10 sets (full growth marker). Hardcoded — not configurable.

### Card UI

**Location:** New card in InsightsTab, added as the 5th card after VolumeByExerciseCard.

**Layout:**
- Title: "Muscle Volume" with subtitle "Last 7 days"
- Filter chips: All | Chest | Back | Shoulders | Arms | Legs | Core
- Per-muscle row (one per group, sorted by set count descending):
  - Muscle name (bold)
  - "X of 10 weekly sets" (muted text) + zone label (colored text: blue/green/gold)
  - 10-segment color bar: filled segments gradient from blue → green → gold, unfilled segments dark/muted

### Data Flow

1. Filter `workoutSessions` to last 7 days
2. For each session with `workingReps > 0`, resolve `exerciseId` → `Exercise.muscleGroups`
3. Split comma-separated `muscleGroups`, normalize to title case (CHEST → Chest)
4. Count sets per group
5. Apply zone thresholds
6. Render card

### Dependencies

- Needs `ExerciseRepository` passed to InsightsTab (re-add the parameter removed in the dashboard redesign)
- Update `AnalyticsScreen.kt` call site to pass `exerciseRepository` again

## Files to Modify

- `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/components/InsightCards.kt` — add MuscleVolumeCard composable
- `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/InsightsTab.kt` — add card to layout, re-add exerciseRepository param
- `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/AnalyticsScreen.kt` — pass exerciseRepository to InsightsTab

## Verification

1. Build compiles
2. Analytics tab shows Muscle Volume card
3. With "All" filter: all 6 groups listed with correct set counts
4. Chest filter: only shows Chest row
5. Zone labels match thresholds (0-4 = Maintaining, 5-9 = Growth, 10+ = Focus)
6. Color bar segments match count (e.g., 3 sets = 3 blue segments, 7 dark)
