# Analytics Dashboard Redesign

## Context

The current analytics dashboard has 6 cards that are mostly broken or misleading because `WorkoutSession` stores individual sets, not full workouts. "Monthly Consistency" counts sets as workouts, "Volume vs Intensity" shows 7 random sets, "Mode Distribution" is useless with a single mode, and "Muscle Balance" only counts PRs. The dashboard needs to show data that actually helps track training progress.

## Design

Replace all 6 current insight cards with 4 focused, accurate cards. Remove the `InsightsTab` content and replace it entirely. Keep the 3-tab structure (Dashboard / Progress / History) — only the Dashboard tab content changes.

### Card 1: This Week Summary (fixed)

**What:** 4 stats — workouts, volume, reps, PRs — with week-over-week % change.

**Fix:** Group sets by `routineSessionId` to count real workouts. Sets without a `routineSessionId` (ad-hoc lifts) count as 1 workout each. Volume uses `effectiveTotalVolumeKg()` (already correct). Duration uses wall-clock time: `max(timestamp + duration) - min(timestamp)` across sets in same `routineSessionId`.

**UI:** Keep the existing `ThisWeekSummaryCard` layout — it already looks good. Just fix the "workouts" count and duration.

### Card 2: Progressive Overload Chart (new, hero)

**What:** Line chart showing the heaviest working set weight (per cable) for a selected exercise over time.

**Data:** For each exercise on each date, take `max(weightPerCableKg)` across all sets for that exercise. Plot as a line chart with date on X-axis, weight (kg) on Y-axis.

**UI:**
- Dropdown at the top listing all exercises from history, sorted by most recent
- Line chart with data points at each session date
- Y-axis shows weight in user's preferred unit (kg/lbs)
- Data points are tappable to show exact value + date
- Default selection: most recently trained exercise

**Data source:** `WorkoutSession` table, grouped by `exerciseName` and date, taking `max(weightPerCableKg)`.

### Card 3: Workout Frequency (new)

**What:** Bar chart showing number of real workout sessions per week for the last 8 weeks.

**Data:** Group sets by `routineSessionId` (or count individually if null) per ISO week. Each unique `routineSessionId` = 1 workout. Each orphan set = 1 workout.

**UI:**
- Vertical bar chart, one bar per week
- X-axis: week labels (e.g., "W12", "W13" or "Mar 17", "Mar 24")
- Y-axis: session count
- Dashed horizontal target line at 3 (hardcoded default; can make configurable later)
- Current week bar highlighted with primary color, past weeks use muted color

### Card 4: Volume by Exercise (new)

**What:** Horizontal bar chart showing total volume per exercise for a selectable time period.

**Data:** Sum `effectiveTotalVolumeKg()` per `exerciseName` for the selected period. Sort descending by volume.

**UI:**
- Period selector chips: "This Week" / "This Month" / "All Time"
- Horizontal bars, one per exercise, sorted by volume descending
- Each bar labeled with exercise name and volume value
- Bars colored by relative proportion

## Files to Modify

### Replace content
- `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/InsightsTab.kt` — gut and replace with 4 new cards

### Modify
- `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/components/InsightCards.kt` — fix `ThisWeekSummaryCard` workout counting; add 3 new card composables (ProgressiveOverloadCard, WorkoutFrequencyCard, VolumeByExerciseCard)
- `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/HomeScreen.kt` — already fixed wall-clock duration; ensure consistency

### New (if needed)
- Chart composables can reuse existing chart components in `presentation/components/charts/` where they fit, or add new ones

### Keep unchanged
- `AnalyticsScreen.kt` (tab orchestrator — no changes needed)
- `ProgressTab.kt` (lifetime stats + PRs — working fine)
- `HistoryTab.kt` (workout log — working fine)

### Remove (dead code after replacement)
- `ConsistencyGaugeCard`, `VolumeVsIntensityCard`, `WorkoutModeDistributionCard`, `MuscleBalanceRadarCard` from `InsightCards.kt`
- `GaugeChart.kt`, `ComboChart.kt`, `CircleChart.kt`, `RadarChart.kt` from `charts/` (only if no longer referenced elsewhere)

## Data Model Notes

- `WorkoutSession.routineSessionId` — groups sets into a single workout. Null for ad-hoc Just Lift sets.
- `WorkoutSession.exerciseName` — exercise name, used for grouping in overload chart and volume breakdown.
- `WorkoutSession.weightPerCableKg` — configured weight per cable for the set.
- `WorkoutSession.effectiveTotalVolumeKg()` — measured volume (v0.2.1+) with fallback formula.
- `WorkoutSession.timestamp` — epoch millis, start of set.
- `WorkoutSession.duration` — active lifting time in ms (NOT wall-clock).

## Verification

1. Build compiles: `./gradlew :androidApp:assembleDebug`
2. Deploy and check analytics tab shows 4 cards
3. Progressive overload chart: select an exercise, verify data points match actual session weights
4. Workout frequency: verify bar count matches real workout sessions (not set count)
5. Volume by exercise: verify totals match manual calculation (reps x weight x cableCount)
6. Weekly summary: "workouts" count should show real session count, not set count
