# AGENTS.md

Guidance for AI coding agents (and humans) working on **Persentient** – an on-device Android app
that monitors phone performance and flags misbehaving apps (pop-up/after-call ads, overlay abuse,
hidden icons, background hogs, notification spam, …).

## Stack

| Concern            | Choice                                                                     |
|--------------------|----------------------------------------------------------------------------|
| Language           | Kotlin 2.4 (K2), JVM target 21                                             |
| UI                 | Jetpack Compose + Material 3 (BOM), adaptive `NavigationSuiteScaffold`     |
| Architecture       | Clean Architecture + MVVM, unidirectional data flow (`StateFlow` UI state) |
| Async              | Kotlin Coroutines & Flow                                                   |
| DI                 | Koin 4 (`koin-android`, `koin-androidx-compose`, `koin-androidx-workmanager`) |
| Persistence        | Room (KSP) for telemetry/reports, DataStore Preferences for settings       |
| Background work    | WorkManager (15-min vitals sampler + end-of-day report worker)             |
| Build              | Gradle Kotlin DSL, version catalog (`gradle/libs.versions.toml`), convention plugins (`build-logic/`) |
| SDK levels         | `compileSdk`/`targetSdk` 37, `minSdk` 32                                   |
| Quality            | ktlint, Android Lint (`checkDependencies`), Kover coverage, R8 full mode  |

## Module map

```
app                    -> Application, MainActivity, root navigation, Koin graph assembly
core/common            -> (JVM) dispatchers, time provider, constants
core/model             -> (JVM) pure domain models – no Android imports
core/domain            -> (JVM) repository interfaces, use cases, detection heuristics (analysis/)
core/data              -> (Android) repository implementations, Room, DataStore, system data sources
core/designsystem      -> (Android) theme (light/dark/dynamic), animated components, behaviour copy
core/testing           -> (JVM) fakes, builders and rules shared by all unit tests
feature/report         -> Tab 1: daily report UI + ReportViewModel
feature/settings       -> Tab 2: permissions, report time, appearance + SettingsViewModel
feature/monitoring     -> WorkManager workers, scheduler, notification + NotificationListenerService
```

Dependency rules (enforced by module boundaries):

- `feature:*` → `core:domain`, `core:model`, `core:designsystem`, `core:common`. **Features never depend on each other.**
- `core:domain` is pure Kotlin. It must not import `android.*`. It only depends on `core:model`/`core:common`.
- `core:data` implements `core:domain` interfaces. Nothing outside `app` depends on `core:data`.
- Cross-feature contracts (e.g. `MonitoringScheduler`) live as interfaces in `core:domain`; the implementing feature binds them in its Koin module.

## Conventions

- **Use cases**: one action per class, `operator fun invoke`, named `VerbNounUseCase`. ViewModels talk to use cases, never to repositories directly.
- **ViewModels** expose a single immutable `StateFlow<XUiState>` built with `stateIn(viewModelScope, WhileSubscribed(5_000), initial)` and one-off events via a `Channel`. Collect with `collectAsStateWithLifecycle()`.
- **Composables**: a stateful `XRoute` (gets the VM from `koinViewModel()`) wraps a stateless `XScreen(state, callbacks)`. Pass `Modifier` as the first optional parameter. No business logic in composables.
- **Dispatchers/time** are injected (`DispatcherProvider`, `TimeProvider`) – never call `Dispatchers.IO` or `Instant.now()` directly in domain/data code.
- **Strings** live in each module's `res/values/strings.xml`; use plurals for counts. No hard-coded user-facing text.
- **Detection heuristics** go in `core/domain/analysis/detector`, implement `BehaviorDetector`, use thresholds from `DetectionThresholds`, are registered in `BehaviorAnalyzer.defaultDetectors` and **must** have unit tests (positive, negative and severity-escalation cases). Add UI copy/icon in `core/designsystem/behavior/BehaviorPresentation.kt`.
- Version numbers go in `gradle/libs.versions.toml` only. Shared build config goes in `build-logic` convention plugins, not copy-pasted into module scripts.

## Android best practices to keep

- **Edge-to-edge** is mandatory (target 35+): `enableEdgeToEdge()` + handle `WindowInsets` (`safeDrawing`) in every screen. Never hard-code status/navigation bar heights.
- **Adaptive layouts**: keep `NavigationSuiteScaffold`; do not lock orientation or resizability (ignored on large screens since Android 16).
- **Predictive back** is enabled (`enableOnBackInvokedCallback`). Use `BackHandler`/`PredictiveBackHandler`, never override `onBackPressed`.
- **Permissions**: request at the moment of use with a clear rationale; special accesses (usage access, notification access) are deep links to system settings. Re-check grants on `ON_RESUME`.
- **Privacy**: everything is on-device. The manifest *removes* `INTERNET`; do not add network libraries. `QUERY_ALL_PACKAGES` and `PACKAGE_USAGE_STATS` require Play Console declarations – keep their use tied to the core purpose.
- **Background work**: WorkManager only (no long-running foreground services). Respect battery – sample cheaply.
- **R8**: release builds are minified + resource-shrunk. Libraries ship `consumer-rules.pro`; anything reflective/serialised must have keep rules. Always verify `./gradlew assembleRelease` after adding dependencies.
- **Accessibility**: content descriptions for meaningful icons, 48dp touch targets, sufficient contrast in both themes.
- **Performance**: stable/immutable UI models, `key`s in lazy lists, heavy work off the main thread (`withContext(dispatchers.io)`).

## Quality gates (must pass before opening a PR)

```bash
./gradlew ktlintFormat                    # auto-format
./gradlew ktlintCheck :app:lintDebug      # style + Android Lint (warnings are fixed, not baselined)
./gradlew test testDebugUnitTest          # all unit tests
./gradlew koverVerify :core:domain:koverVerify   # coverage: >=50% total, >=90% domain
./gradlew assembleDebug assembleRelease   # compiles, R8 release works
```

- Every new use case, detector, mapper and ViewModel needs unit tests. Prefer fakes from `core:testing` over mocks; use `MainDispatcherRule` + Turbine for flows; create ViewModels lazily (after the rule installs `Dispatchers.Main`).
- Coverage is measured with Kover (`build/reports/kover/html/index.html`). Do not lower thresholds – add tests instead.
- Lint must stay at zero errors **and** zero warnings; suppress only with a justification comment.
- The `KoinGraphTest` verifies the DI graph – update it if you add constructor types that are provided outside Koin.

## CI

`.github/workflows/ci.yml` runs four **parallel** jobs on every PR: compile (debug + R8 release),
unit tests, coverage (summary in the job page + threshold check) and lint. PRs follow
`.github/pull_request_template.md` (ticket, description, how to test).

## Gotchas

- AGP 9 has built-in Kotlin support: do **not** apply `org.jetbrains.kotlin.android`.
- Adaptive launcher icons must stay in `mipmap-anydpi-v26` (AAPT2 requirement) – lint is configured to accept it.
- `UsageStatsManager`/`NetworkStatsManager` return nothing until Usage access is granted; data sources must degrade gracefully (return empty lists, never crash).
