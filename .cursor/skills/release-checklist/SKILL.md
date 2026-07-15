---
name: release-checklist
description: Runs pre-flight release verification for the Android app — full test suite (including carrier aggregation mapper tests), Android Lint, ProGuard/R8 keep-rule review for telephony/domain classes, and version bump — then reports whether the codebase is ready to assemble a release build. Use when the user asks to prepare, verify, or cut a release, or run release checks.
disable-model-invocation: true
---

# Release Checklist

Copy this checklist and track progress:

```
Task Progress:
- [ ] Step 1: Run full test suite
- [ ] Step 2: Run Android Lint
- [ ] Step 3: Review ProGuard/R8 keep rules
- [ ] Step 4: Bump versionCode/versionName
- [ ] Step 5: Generate final report
```

## Step 1: Run full test suite

Run unit tests, then instrumented tests (requires a connected device/emulator):

```bash
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
```

On Windows use `gradlew.bat` instead of `./gradlew`.

- Treat any failure as a blocking issue — do not proceed past this step on
  failure.
- Specifically confirm that mapper tests for network/cell mapping and
  carrier aggregation logic pass. If these specific tests can't be
  identified by name, search test sources for
  `CarrierAggregation`/`PhysicalChannelConfig`/mapper test classes and
  confirm they ran and passed.

## Step 2: Run Android Lint

```bash
./gradlew lint
```

- Read the generated report (`app/build/reports/lint-results-debug.html` or
  `.xml`).
- Treat any unresolved warning about runtime permissions (missing
  permission checks, `MissingPermission`) or coroutine context leaks
  (e.g. `GlobalScope` usage, unclosed scopes) as blocking, even if lint
  categorizes them below "error" severity.
- Other warnings can be listed as non-blocking notes in the final report.

## Step 3: Review ProGuard/R8 keep rules

Read `app/proguard-rules.pro` and verify it has explicit `-keep` rules for:
- Domain models (`domain/model`, e.g. `CarrierAggregationState`,
  `RadioMastEntity` if applicable)
- Classes that map Telephony API objects (`CellInfo`, `CellInfoLte`,
  `CellInfoNr`, `PhysicalChannelConfig`, and the mappers in
  `data/telephony`)

If minification is disabled (`minifyEnabled false`, as currently configured
in `app/build.gradle`), note this explicitly in the report — keep rules are
moot until minification is turned on, but flag any classes that would be at
risk if it's enabled later.

## Step 4: Bump versionCode/versionName

The project currently uses Groovy `app/build.gradle` (not
`build.gradle.kts`). Edit the values there:

```groovy
defaultConfig {
    versionCode <increment by 1>
    versionName "<new version string>"
}
```

- Increment `versionCode` by exactly 1.
- Update `versionName` following the project's existing versioning scheme
  (ask the user if no scheme is evident, e.g. semantic versioning vs. date-based).

## Step 5: Generate final report

Output a terminal-style report:

```
Release Readiness Report
-------------------------
Unit tests:            PASS/FAIL
Instrumented tests:    PASS/FAIL
Carrier aggregation tests: PASS/FAIL
Lint:                  PASS/FAIL (n blocking, n non-blocking warnings)
ProGuard/R8 keep rules: OK/MISSING (<details>)
Version bump:           <old> -> <new>

READY FOR RELEASE: YES/NO
```

If any step failed or is blocking, set `READY FOR RELEASE: NO` and list the
specific blockers with file/line references.
