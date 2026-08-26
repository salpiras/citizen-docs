# CitizenDocs

Scan a document with your phone, give it a name and a date, and keep it in a list you can
search, open, rename and delete. Documents are filed into collapsible month sections, and the
whole library — or just what a search is showing — exports to a ZIP foldered by year and
month. Everything stays on the device.

The app is deliberately small. It exists as a worked example of current Android practice —
multi-module Clean Architecture, MVI, and a test pyramid that runs entirely on the JVM — with
enough room left to grow (search, tags, OCR, backup) without re-laying the foundations.

## Stack

| | |
|---|---|
| Build | AGP 9.3 (built-in Kotlin), Gradle 9.7, Kotlin 2.3.21, KSP |
| UI | Jetpack Compose, Material 3, type-safe Navigation Compose |
| DI | Hilt (via KSP) |
| Storage | Room + app-private files, shared out via FileProvider |
| Scanning | ML Kit Document Scanner |
| Testing | JUnit4, Turbine, Truth, Robolectric, Roborazzi |

## Module graph

```
                        :app
                         │  nav host, Application, MainActivity
            ┌────────────┴────────────┐
      :feature:documents        :feature:scan
            └────────────┬────────────┘
                    :core:domain        use cases + validation
                         │
                    :core:data          DocumentsRepository (+ offline-first impl)
                  ┌──────┴──────┐
           :core:database   :core:storage      Room          files + FileProvider

  :core:scanner        ML Kit wrapper + Compose launcher
  :core:designsystem   theme, tokens, motion, model-agnostic components
  :core:ui             MVI base, DocumentCard, UiText
  :core:model          Document, validation, file-name sanitising   (pure JVM)
  :core:common         dispatcher qualifiers, Clock/TimeZone        (pure JVM)
  :core:testing        fakes, rules, shared fixtures

  :benchmarks          macrobenchmarks + baseline profile generation (needs a device)
```

Dependencies point one way only: UI → domain → data → (database | storage). A feature module
has no dependency on `:core:database`, `:core:storage` or `:core:scanner`, so it *cannot*
import a Room entity or an Android `Uri` — the graph enforces the layering, not code review.
That rule lives in
[`AndroidFeatureConventionPlugin`](build-logic/convention/src/main/kotlin/AndroidFeatureConventionPlugin.kt).

`:core:model` and `:core:common` are plain JVM libraries. They hold most of the logic worth
testing, and their tests need no Android runtime at all.

## Architecture: MVI

Every screen ViewModel extends
[`MviViewModel`](core/ui/src/main/kotlin/com/salpiras/citizendocs/core/ui/mvi/MviViewModel.kt),
which fixes three things:

- **One `state`.** A single immutable `StateFlow`. There is never a second source of truth.
- **One entry point, `onEvent(Event)`.** The UI calls nothing else, so every possible
  interaction is enumerable from the sealed `Event` type.
- **`effects` for one-shot actions** — navigate, open a file, show a snackbar. These are a
  `Channel`, not state, so they don't replay on rotation.

Each screen is split into a stateful `…Route` (DI + effect handling) over a stateless
`…Screen` (pure function of state). Only the stateless half is used by previews, behaviour
tests and screenshot tests.

## Building and testing

```bash
./gradlew assembleDebug          # build the APK
./gradlew testDebugUnitTest test # every unit, behaviour and screenshot test
./gradlew spotlessApply          # format (ktlint, 4-space, 120 cols)
./gradlew lint                   # Android lint
```

The whole suite runs on the JVM — no emulator, no connected device.

`:benchmarks` is the one exception, because measuring ART is the point of it:

```bash
./gradlew :app:generateBaselineProfile              # record; commits into app/src/main/generated
./gradlew :benchmarks:connectedBenchmarkAndroidTest # measure (physical device — emulators are too noisy)
```

The generated `baseline-prof.txt` is **committed**. CI has no emulator and only consumes it.

### Screenshot tests

Goldens live beside the code, in `feature/<name>/src/test/screenshots`, and are committed.

```bash
./gradlew recordRoborazziDebug   # rewrite the goldens after an intentional UI change
./gradlew verifyRoborazziDebug   # compare against them
```

`roborazzi.test.verify=true` in `gradle.properties` means verification also runs as part of
the normal unit test task. Screenshot tests pin two theme parameters. `dynamicColor = false`
keeps the palette off the host wallpaper. `reducedMotion = true` is the stronger requirement:
the loading and empty states run on a `rememberInfiniteTransition`, which never reports idle
at any test-clock setting, and Roborazzi waits for idle — so without it the capture does not
come out wrong, it never comes out at all.

Comparison allows a **1% pixel change** rather than Roborazzi's default of zero. Goldens
recorded on macOS and verified on Linux CI differ by a few pixels on anti-aliased rounded
corners and on the FAB's elevation shadow, while text, layout and colour stay identical —
zero tolerance makes the suite fail on a change of host OS alone. The allowance is set once
in [`CitizenDocsRoborazziOptions`](core/testing/src/main/kotlin/com/salpiras/citizendocs/core/testing/ScreenshotOptions.kt)
and is far below the footprint of any real UI change.

Transient states are not screenshotted. The save screen's in-flight spinner has no
reproducible rotation phase across machines even with a frozen test clock, so it is covered
by a behaviour test asserting the Save button is unavailable while saving.

The rename dialog is covered by behaviour tests and `@PreviewLightDark` previews rather than
screenshots: `AlertDialog` renders into its own window and, under Robolectric, that window's
scrim animation never reports idle, so the capture times out.

### Test layers

| Layer | Where | Runs on |
|---|---|---|
| Pure logic (validation, file-name sanitising) | `:core:model` | JVM |
| Repository, including file/row rollback | `:core:data` | JVM + fakes |
| Use cases | `:core:domain` | JVM + fakes |
| DAO against real SQLite | `:core:database` | Robolectric |
| File store against a real filesystem | `:core:storage` | Robolectric |
| ViewModels | `:feature:*` | JVM (+ Robolectric where nav decoding needs a Bundle) |
| Compose behaviour + screenshots | `:feature:*` | Robolectric |

Fakes are hand-written and live in `:core:testing`; there is no mocking framework. A fake
enforces the same invariants as the real implementation, so a passing test reflects behaviour
rather than a recorded call sequence.

## Design system and motion

The palette is hand-authored in
[`Color.kt`](core/designsystem/src/main/kotlin/com/salpiras/citizendocs/core/designsystem/theme/Color.kt),
not exported from the Material Theme Builder: warm sienna primary, sage and brass in support,
paper-white ground in light and warm charcoal in dark. Every neutral carries the accents' hue,
so surfaces read as paper rather than as grey chrome. `dynamicColor` defaults to **false** —
with it on, almost nobody on Android 12+ would ever see any of this.

One colour sits deliberately outside the M3 scheme, in
[`AccentColors.kt`](core/designsystem/src/main/kotlin/com/salpiras/citizendocs/core/designsystem/theme/AccentColors.kt):
a marigold spot used only when a scan lands. Keeping it out of `ColorScheme` is what stops it
drifting into ordinary chrome.

No font assets are bundled. Display and headline roles use the platform serif; everything read
in bulk stays on the platform sans.

### The motion rule

**Animated values are read in the layout or draw phase, never at composition scope.** Every
animation in the app is either an `Animatable` dereferenced inside a `graphicsLayer {}` or
`drawWithCache {}` lambda, or a modifier that animates without recomposing — card press scale,
the month chevron, the arrival wash, the loading shimmer. `Modifier.animateItem` handles list
insert, remove and reorder in the layout pass; the FAB collapses on a `derivedStateOf`, so it
recomposes on the two threshold crossings rather than on every frame of a scroll.

Springs, not durations, from
[`CitizenDocsMotion`](core/designsystem/src/main/kotlin/com/salpiras/citizendocs/core/designsystem/motion/CitizenDocsMotion.kt):
an interrupted animation continues from its current velocity instead of restarting. M3 ships
this idea as `MotionScheme`, but that and `MaterialExpressiveTheme` are `internal` in
material3 1.4.0, so these are ours until the expressive APIs go public.

Decorative motion — the shimmer, the empty state's drift — is gated on
[`LocalReducedMotion`](core/designsystem/src/main/kotlin/com/salpiras/citizendocs/core/designsystem/motion/ReducedMotion.kt),
which follows the platform animator duration scale on device and is pinned in tests.

Check the compiler's own view of any of this with:

```bash
./gradlew assembleDebug -PcomposeMetrics=true
```

That writes skippability and stability reports to `<module>/build/compose-reports`. Off by
default because it slows every Compose compilation down.

## Notes on the data model

- A document's `fileName` is **relative** to the store root, never an absolute path, so a
  record survives the app's data directory moving.
- Titles are sanitised before they touch the filesystem
  ([`slugify`](core/model/src/main/kotlin/com/salpiras/citizendocs/core/model/FileNames.kt)) —
  `../../etc/passwd` cannot escape the documents directory.
- Two documents may share a title; the store appends `_1`, `_2`, … and the database enforces
  a unique index on the file name.
- The file is written before the row is inserted, so `add` deletes the file again if the
  insert fails. Nothing is ever orphaned on disk.
- `Clock` is injected, which is what makes `createdAt` assertable in tests.
- The database is `citizen-docs.db`, schema version 1, with `exportSchema = true`; schema
  JSON is committed under `core/database/schemas`. The earlier prototype's `docs` table is
  abandoned rather than migrated.

## Search, grouping and export

These three compose deliberately, and none of them needed a schema change:

- **Search** is a single `LIKE` query in which an empty term matches everything, so the plain
  list and the search results are the same statement — there is no second code path to keep
  in step. The field updates immediately while the query itself is debounced, so typing never
  feels laggy but the database isn't asked on every keystroke. "No matches" is a distinct
  state from "no documents": telling someone with forty documents that they have none because
  they mistyped would be wrong.
- **Month sections** are derived from `documentDate`, never stored. Labels are pre-formatted
  in `groupByMonth()` for the same reason `formattedDate` is — locale work stays out of
  layout, and screenshot goldens stay host-independent.
- **Export** writes whatever the list is currently showing, so an active search narrows the
  archive for free. The destination comes from `ACTION_CREATE_DOCUMENT`, so no storage
  permission is needed, and entry paths (`2026/01/Tax_return_2025.pdf`) mirror the month
  sections on screen. A failure deletes the partial file: a truncated archive is worse than
  none, because it looks like a valid backup.

## Keeping dependencies current

Versions live in [`gradle/libs.versions.toml`](gradle/libs.versions.toml) and are pinned to
released stable artifacts. There is exactly one exception, and it is recorded next to the
pin: `androidxBenchmark` is on `1.5.0-rc01` because the stable 1.4.1 Baseline Profile plugin
refuses to apply to an AGP 9 application module, so no stable version of it works with this
build at all. Kotlin deliberately
stays on the 2.3 line rather than 2.4: KSP has no 2.4.x release, and KSP drives both Hilt and
Room. Check for updates against the repositories rather than another sample project:

```bash
curl -s https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/maven-metadata.xml
curl -s https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-gradle-plugin/maven-metadata.xml
```
