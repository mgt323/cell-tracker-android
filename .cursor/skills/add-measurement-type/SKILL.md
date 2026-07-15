---
name: add-measurement-type
description: Step-by-step workflow for adding a new measurement metric/data type to the cell tracker Android app, covering the Room entity and migration, the CellInfo/PhysicalChannelConfig mapper, the ViewModel/StateFlow, the Compose UI and chart, and the mapper unit test. Use when the user asks to add, track, or record a new measurement type, metric, or data point.
---

# Add Measurement Type

Adding a new tracked metric (e.g. RSRP, RSRQ, carrier aggregation band count)
touches five layers, in this order. Follow the package conventions in
`.cursor/rules/android-telephony.mdc`: `data/telephony` (mapping),
`domain/model` (plain domain models), `ui/screens` (Compose).

Copy this checklist and track progress:

```
Task Progress:
- [ ] Step 1: Room entity field + migration
- [ ] Step 2: Extend the mapper (CellInfo/PhysicalChannelConfig -> domain)
- [ ] Step 3: Update ViewModel + StateFlow
- [ ] Step 4: Add to Compose UI + chart
- [ ] Step 5: Unit test the mapper
```

## Step 1: Room entity field + migration

- Add the new column to the relevant `@Entity` data class in
  `data/local/entity`, with a sensible default (nullable or default value —
  never a non-null field without a default, or existing rows break).
- Bump the `version` in the `@Database` annotation on `AppDatabase`.
- Add a `Migration(oldVersion, newVersion)` object with the `ALTER TABLE ...
  ADD COLUMN ...` SQL, and register it via `.addMigrations(...)` wherever the
  database is built (see `di/DatabaseModule.kt`).
- Never rely on `fallbackToDestructiveMigration()` for this — it silently
  drops user data.

## Step 2: Extend the mapper

- Find (or create) the mapper in `data/telephony` responsible for turning
  Android framework types (`CellInfo`, `PhysicalChannelConfig`,
  `SignalStrength`, etc.) into the domain model.
- Extract the new raw value from the framework type and map it onto the
  corresponding field of the domain model in `domain/model` (e.g.
  `CarrierAggregationState`). Domain models must stay free of Android
  telephony types — do the extraction/conversion entirely inside the mapper.
- If the value can be absent/unknown on some devices or API levels, model it
  as nullable in the domain model rather than a sentinel value like `-1`.
- Propagate the new field through the Room entity mapper too (domain model
  ⇄ Room entity), so persistence round-trips the new value.

## Step 3: Update ViewModel + StateFlow

- Add the new field to the screen's UI state data class (e.g.
  `HomeScreenState`/`DetailScreenState`).
- In the `@HiltViewModel`, read the new field from the domain
  model/repository flow and fold it into the `StateFlow` exposed to the UI
  (e.g. via `.map { }` on the upstream flow, or in the `combine`/`update`
  block that builds the UI state).
- Keep the ViewModel unaware of `CellInfo`/`PhysicalChannelConfig` — it
  should only ever see the domain model.

## Step 4: Add to Compose UI + chart

- Surface the new field in the relevant screen under `ui/screens`, following
  the existing layout pattern (label + value, or a list item).
- Add it to the chart/time-series view if the metric is meant to be tracked
  over time: add a new series/line keyed by the same timestamp axis as
  existing metrics, with its own color and legend entry.
- Read the value from the `StateFlow`-backed UI state only — never call
  telephony APIs directly from Composables.

## Step 5: Unit test the mapper

- Add a test in `app/src/test/.../data/telephony/` for the mapper touched in
  Step 2.
- Construct the framework input (`CellInfo`/`PhysicalChannelConfig`) with a
  fake/mock and assert the domain model's new field maps correctly, including:
  - the typical/expected value case
  - the absent/unknown case (maps to `null`, not a sentinel)
  - any boundary values specific to the metric (e.g. min/max signal strength)
- Run the test with `./gradlew testDebugUnitTest` (or the module's test task)
  before considering the change complete.
