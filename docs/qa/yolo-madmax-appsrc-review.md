# YOLO MADMAX app/src review

Scope: `app/src/main` screens, theme, data, and utility layers.

## Verified strengths

- The app already uses a cohesive Compose theme with custom typography and soft neutral/mint colors.
- Core calorie math is isolated in `MiniCutRules`, and the existing unit tests cover most of the state calculations.
- Screen flows are logically split across Home, Plan, and Calendar, which makes cross-screen QA straightforward.

## Review findings

1. **Validation is mostly toast-driven**
   - The meal entry sheet validates via toast messages only.
   - This is lightweight, but it is not the most accessible pattern for form feedback.
   - Recommendation: prefer inline helper text or field-level errors for invalid calories/food name input.

2. **Current-date refresh logic is duplicated**
   - `HomeViewModel` and `CalendarViewModel` each own their own midnight ticker loop.
   - The logic is nearly identical and increases maintenance cost if the refresh cadence ever changes.
   - Recommendation: extract the ticker into a shared clock utility or shared flow producer.

3. **Migration strategy is destructive**
   - `AppContainer` currently uses `fallbackToDestructiveMigration(dropAllTables = true)`.
   - That is acceptable for early development, but it risks silent data loss in future releases.
   - Recommendation: replace with explicit migrations once schema stability matters.

## Verification notes

- Added unit coverage for formatter and date-presentation helpers:
  - `app/src/test/java/com/minicut/timer/ui/util/UiFormattersTest.kt`
- Existing tests already cover:
  - `MiniCutRules`
  - `DateTicker`
  - notification scheduling
- The Compose screens already use the shared `MiniCutBackdrop`, `MiniCutCardShape`, `MiniCutMetricTile`, and `MiniCutSectionHeader` helpers, so the visual layer is aligned with the current design language.

## Suggested next pass

- Rework the static badge row in `PlanScreen`.
- Introduce reusable design-system composition in the screen cards.
- Add one UI/instrumentation test around the plan or home flow once the build verification lane is stable.
