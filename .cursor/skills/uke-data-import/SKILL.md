---
name: uke-data-import
description: Imports the latest UKE (Polish telecom regulator) mast dataset CSV, parses operator/station/coordinate/technology/band fields, upserts into the Room database as RadioMastEntity records, normalizes coordinates to WGS84, and reports import counts. Use when the user asks to import, refresh, or sync UKE mast/station data.
disable-model-invocation: true
---

# UKE Data Import

Copy this checklist and track progress:

```
Task Progress:
- [ ] Step 1: Get dataset file path
- [ ] Step 2: Parse the CSV
- [ ] Step 3: Map to RadioMastEntity
- [ ] Step 4: Upsert by station ID
- [ ] Step 5: Normalize coordinates to WGS84
- [ ] Step 6: Report import summary
```

## Step 1: Get dataset file path

Ask the user for the absolute path to the latest UKE dataset file (typically
`.csv`). Do not guess a path or search the filesystem for it.

## Step 2: Parse the CSV

Extract these fields per row (UKE column names vary by export; map by header,
not by column index):
- operator name
- station ID (eNodeB/gNodeB identifier)
- latitude / longitude
- supported technologies (e.g. GSM, UMTS, LTE, NR)
- frequency bands

The project has no CSV library dependency yet. Parse with Kotlin stdlib
(`File(path).readLines()`, split on the delimiter, respect quoted fields).
Do not add a CSV parsing library (e.g. kotlin-csv, Apache Commons CSV)
without asking first — see `.cursor/rules/tech-stack.mdc`.

## Step 3: Map to RadioMastEntity

Follow the existing Room conventions (`data/entity`, `data/dao`,
`data/repository` — see `ExampleEntity`/`ExampleDao`/`ExampleRepository`).

- If `RadioMastEntity` does not exist yet, create it in `data/entity` with a
  unique/indexed `stationId` column (`@Entity(tableName = "radio_mast_table")`,
  and an index or unique constraint on `stationId`).
- Map each parsed row into a `RadioMastEntity` instance. Skip and record any
  row missing a station ID or coordinates — these cannot be safely upserted.

## Step 4: Upsert by station ID

- Use Room's `@Upsert` (or an `@Insert(onConflict = OnConflictStrategy.REPLACE)`
  keyed on a unique `stationId` index) in the DAO so re-importing the same
  dataset updates existing masts and inserts new ones without duplicating
  rows.
- A row updates an existing mast if its `stationId` already exists;
  otherwise it inserts a new row.

## Step 5: Normalize coordinates to WGS84

- UKE exports are typically already WGS84 (decimal degrees), but validate
  and sanitize before insert:
  - Reject/flag rows with latitude outside [-90, 90] or longitude outside
    [-180, 180].
  - If coordinates are in a different format (e.g. DMS, or a projected CRS
    like PUWG92/EPSG:2180), convert to WGS84 decimal degrees before
    mapping to `RadioMastEntity`. Ask the user to confirm the source CRS if
    it's ambiguous from the data — do not silently assume.
- Store latitude/longitude as `Double` decimal degrees to match `osmdroid`'s
  expected `GeoPoint` format for map rendering.

## Step 6: Report import summary

After the import completes, output a summary report:

```
UKE Import Summary
------------------
Total rows processed: <n>
Inserted (new masts):  <n>
Updated (existing):    <n>
Skipped (invalid row): <n>
```

List the specific reason for each skipped row (missing station ID, invalid
coordinates, unparseable row) rather than a bare count only.
