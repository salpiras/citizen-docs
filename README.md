# CitizenDocs

Scan a document with your phone, give it a name and a date, and keep it in a list you can
open, rename and delete. Everything stays on the device.

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
  :core:designsystem   theme, tokens, model-agnostic components
  :core:ui             MVI base, DocumentCard, UiText
  :core:model          Document, validation, file-name sanitising   (pure JVM)
  :core:common         dispatcher qualifiers, Clock/TimeZone        (pure JVM)
  :core:testing        fakes, rules, shared fixtures
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

### Screenshot tests

Goldens live beside the code, in `feature/<name>/src/test/screenshots`, and are committed.

```bash
./gradlew recordRoborazziDebug   # rewrite the goldens after an intentional UI change
./gradlew verifyRoborazziDebug   # compare against them
```

`roborazzi.test.verify=true` in `gradle.properties` means verification also runs as part of
the normal unit test task. Screenshot tests force `dynamicColor = false`; with Material You
enabled the palette would follow the host wallpaper and goldens would differ per machine.

**Goldens are recorded on Linux**, because that is what CI verifies on. Robolectric's native
graphics backend is a per-platform binary and font metrics differ enough between macOS and
Linux to shift text layout — well beyond any sane pixel tolerance. Recording locally on a Mac
and pushing will fail CI. After an intentional UI change, re-record through the
[Record screenshots](.github/workflows/record-screenshots.yml) workflow rather than locally;
it runs `recordRoborazziDebug` on Linux and commits the result back to your branch.

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

## Keeping dependencies current

Versions live in [`gradle/libs.versions.toml`](gradle/libs.versions.toml) and are pinned to
released stable artifacts — no `-alpha`, `-beta` or `-rc` pins anywhere. Kotlin deliberately
stays on the 2.3 line rather than 2.4: KSP has no 2.4.x release, and KSP drives both Hilt and
Room. Check for updates against the repositories rather than another sample project:

```bash
curl -s https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/maven-metadata.xml
curl -s https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-gradle-plugin/maven-metadata.xml
```
