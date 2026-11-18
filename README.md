<p align="center">
  <img src="docs/banner.svg" alt="Perfsentient: your phone's watchdog" width="100%">
</p>

<p align="center">
  <a href="https://github.com/neteinstein/Perfsentience/actions/workflows/ci.yml"><img alt="CI" src="https://github.com/neteinstein/Perfsentience/actions/workflows/ci.yml/badge.svg"></a>
  <a href="https://github.com/neteinstein/Perfsentience/releases/latest"><img alt="Latest release" src="https://img.shields.io/github/v/release/neteinstein/Perfsentience?color=5B4BDB&label=release"></a>
  <img alt="Android 12L+" src="https://img.shields.io/badge/Android-12L%2B%20(API%2032)-3DDC84?logo=android&logoColor=white">
  <img alt="targetSdk 37" src="https://img.shields.io/badge/targetSdk-37-00897B">
  <img alt="Kotlin 2.4" src="https://img.shields.io/badge/Kotlin-2.4-7F52FF?logo=kotlin&logoColor=white">
  <img alt="Jetpack Compose" src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white">
  <img alt="No INTERNET permission" src="https://img.shields.io/badge/INTERNET%20permission-none-D0386F">
</p>

<p align="center">
  <b>Find the app that's draining your battery, showing pop-up ads or hiding its icon.</b><br>
  Perfsentient watches your phone in the background, works everything out on the device, and gives you<br>
  <b>one plain report a day</b>: a health score, the apps that misbehaved, and the evidence.
</p>

<p align="center">
  <a href="https://github.com/neteinstein/Perfsentience/releases/latest"><b>⬇️ Download the APK</b></a> ·
  <a href="#-what-it-catches">What it catches</a> ·
  <a href="#-setup">Setup</a> ·
  <a href="#-privacy">Privacy</a> ·
  <a href="#-build">Build</a>
</p>

---

## ✨ Highlights

- 🩺 **Daily health report.** An animated health score, a one-line verdict, the apps that misbehaved (with the reason for each), device vitals and a 7-day trend.
- 🔔 **One notification a day.** At a time you choose, it tells you whether the day was good or bad and how many apps caused trouble. Tap it to open the report.
- 🕵️ **Catches ad tricks.** Pop-up ads, ads after a phone call, apps drawing over other apps, and apps that hide their launcher icon.
- 🔋 **Finds battery and data hogs.** Apps that run all day in the background, wake up at night, or use a lot of mobile data you never asked for.
- 🔒 **Stays on your phone.** The app has no `INTERNET` permission, so it can't send anything anywhere. The release build checks this.
- 🎨 **Modern Android.** Material 3 with dynamic colour, light and dark themes, edge-to-edge, predictive back, and a layout that adapts to tablets and foldables.

## 🔍 What it catches

| | Behaviour | How it's spotted (public, Play-compliant APIs only) |
|---|---|---|
| 📢 | **Pop-up / out-of-context ads** | A short screen from another app appears in the middle of the one you're using (A → X → A), or right after you unlock |
| 📞 | **After-call ads** | A third-party screen appears right after the dialer or in-call screen closes |
| 🪟 | **Overlay abuse** | Has "display over other apps" **and** either hides its icon or is busy in the background while you rarely open it |
| 👻 | **Hidden app** | Installed by you, has no launcher icon, but runs in the background |
| ♾️ | **Always running** | Foreground service for ≥ 2 h a day while you use it for < 15 min (Android vitals wake-lock threshold) |
| 🌙 | **Night owl** | Starts services or screens with the screen off between 00:00 and 06:00 |
| 📶 | **Background data hog** | ≥ 50 MB a day of background traffic (Android vitals threshold), from `NetworkStatsManager` |
| 🔕 | **Notification spam** | Notification counts from a `NotificationListenerService`. The content is never read |
| ♿ | **Accessibility / device admin** | Has an accessibility service or device-admin rights turned on. Rated more serious for apps not installed from a store |
| 🔓 | **Unrestricted battery** | Exempt from battery optimisation **and** doing heavy background work |
| 💾 | **Storage hog** | A very large cache (`StorageStatsManager`) |
| 🌡️ | **Device issues** | Battery drain with the screen off, temperature, thermal throttling and memory pressure, sampled every 15 minutes |

Each finding has a severity, and the severities feed the day's health score. The thresholds are in
[`DetectionThresholds`](core/domain/src/main/kotlin/com/neteinstein/perfsentience/core/domain/analysis/DetectionThresholds.kt).

## ⚙️ How it works

```mermaid
flowchart LR
    subgraph Phone["📱 Your phone (nothing leaves it)"]
        direction LR
        S["⏱️ Every 15 min<br/>WorkManager sampler<br/><sub>battery · temp · memory</sub>"] --> DB[("🗄️ Room")]
        N["🔔 Notification listener<br/><sub>counts only</sub>"] --> DB
        DB --> A["🧠 Behaviour analyser<br/><sub>10 app detectors + device checks</sub>"]
        U["📊 Usage events · network stats<br/>installed apps · storage"] --> A
        A --> R["🩺 Daily report<br/><sub>score · offenders · trend</sub>"]
        R --> X["🔔 One notification<br/>at your chosen time"]
    end
```

1. A cheap WorkManager job records device vitals every 15 minutes. There's no foreground service.
2. At the report time you choose, the analyser reads the day's usage events, network stats, installed apps and the stored samples, and runs every detector.
3. The result is saved as the day's report and announced with a single notification.

## 🚀 Setup

1. **Install:** download `Perfsentient_vX.Y.Z.apk` from the [latest release](https://github.com/neteinstein/Perfsentience/releases/latest) and open it on your phone (Android 12L / API 32 or newer). You may need to allow installing apps from your browser or file manager.
2. **Grant access:** open the **Settings** tab. Each permission says *what* it's for and *why*. Tap **Grant** and the button turns into a green tick.

   | Permission | Why |
   |---|---|
   | **Usage access** | Reads app usage events, foreground-service time, background data and cache sizes. It never sees what you do inside apps. |
   | **Notifications** | Lets the app post its one daily report notification. |
   | **Notification access** | Counts notifications per app to spot spam. Titles and messages are never read or stored. |
   | **Installed apps list** | Granted at install. Needed to find hidden icons, overlays, accessibility services and device admins. |

3. **That's it.** Monitoring starts as soon as everything is granted, and your first report arrives that evening. Tap **Analyse now** on the report tab if you want a preview of the day so far.

> [!TIP]
> Usage access and notification access are special permissions that Android only lets you turn on in system settings. The **Grant** button opens the right screen for you.

## 🔒 Privacy

- **No internet.** The manifest *removes* `android.permission.INTERNET`, so no library can add it back. The release workflow fails if it shows up in the merged manifest.
- **No accounts, analytics or ads.**
- **Counts, not content.** Notifications are counted per app. Their text is never read.
- **Your data stays local.** Reports and samples are kept in a local Room database on the phone.

## 🏗️ Architecture

Clean Architecture + MVVM, with one Gradle module per feature. Koin for DI, Coroutines/Flow, Room, DataStore and WorkManager.

```mermaid
flowchart TD
    app[":app"] --> report[":feature:report"]
    app --> settings[":feature:settings"]
    app --> monitoring[":feature:monitoring"]
    app --> data[":core:data"]
    report & settings & monitoring --> domain[":core:domain"]
    report & settings & monitoring --> ds[":core:designsystem"]
    data -. implements .-> domain
    domain --> model[":core:model"]
    domain --> common[":core:common"]
```

| Module | Role |
|---|---|
| `core:domain` | Pure Kotlin: use cases, repository interfaces and the detectors in `analysis/` |
| `core:data` | Room, DataStore and the Android system data sources (usage, network, storage, vitals) |
| `core:designsystem` | Aurora theme (light/dark/dynamic), animated gauge, chips and cards |
| `feature:*` | Report tab, Settings tab, and the background monitoring (workers, scheduler, notification listener) |

Features never depend on each other. See [AGENTS.md](AGENTS.md) for the full module map, conventions and quality gates.

## 🛠️ Build

Requires **JDK 21** and the Android SDK (platform 37).

```bash
./gradlew assembleDebug                          # debug APK
./gradlew assembleRelease                        # R8-minified release APK (signed when a keystore is configured)
./gradlew test testDebugUnitTest                 # unit tests
./gradlew koverVerify :core:domain:koverVerify   # coverage: ≥ 50 % overall, ≥ 90 % domain
./gradlew ktlintCheck :app:lintDebug             # style + Android Lint (zero warnings)
```

| Tech | |
|---|---|
| Language | Kotlin 2.4 (K2), JVM 21 |
| UI | Jetpack Compose, Material 3, `NavigationSuiteScaffold` |
| DI | Koin 4 |
| Storage | Room (KSP), DataStore Preferences |
| Background | WorkManager |
| Build | AGP 9, version catalog, `build-logic` convention plugins, R8 full mode |
| Quality | ktlint, Android Lint, Kover |

## 🚢 CI/CD

- **Every PR:** four parallel jobs: compile (debug + R8 release), unit tests, coverage (with a summary on the job page) and lint.
- **Every merge to `main`:** bumps the patch version, builds the signed APK + AAB, checks there's no `INTERNET` permission, and publishes a [GitHub release](https://github.com/neteinstein/Perfsentience/releases).
- **Google Play:** the upload is set up but switched off for now. See [PublishToStore.md](PublishToStore.md).

## 🏪 Play Store notes

- `PACKAGE_USAGE_STATS` and notification access are special permissions that the user turns on in system settings.
- `QUERY_ALL_PACKAGES` needs the *Package visibility* declaration in Play Console. The core purpose is auditing installed apps, like antivirus and security apps do.
