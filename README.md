# Persentient

> Your phone's watchdog. Persentient monitors performance on-device and tells you – once a day –
> which apps are hurting your battery, spamming you or showing sneaky ads.

![minSdk](https://img.shields.io/badge/minSdk-32-blue) ![targetSdk](https://img.shields.io/badge/targetSdk-37-blue) ![Kotlin](https://img.shields.io/badge/Kotlin-2.4-purple) ![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-green)

## Features

- **Daily health report** (tab 1): animated health score, verdict, misbehaving apps with evidence, device vitals and a 7-day trend.
- **End-of-day notification** saying whether the day was good or bad (score + number of offenders); tapping it opens the report.
- **Settings** (tab 2): every permission listed with *what* and *why*, a grant button that turns into a green check, report time, light/dark/system theme and dynamic colour.
- Monitoring starts automatically as soon as all permissions are granted.
- 100 % on-device – the app has **no internet permission**.

## What gets detected

| Behaviour | Signal (public, Play-compliant APIs only) |
|---|---|
| Pop-up / out-of-context ads | Usage events: brief foreground session interrupting another app (A → X → A) or right after unlock |
| After-call ads | Third-party screen right after the dialer / in-call UI |
| Overlay abuse | `SYSTEM_ALERT_WINDOW` granted + hidden icon or busy in background while rarely opened |
| Hidden app | User-installed app without a launcher icon that is active in background |
| Always running | Foreground-service time ≥ 2 h/day with < 15 min of use (Android vitals wake-lock threshold) |
| Night owl | Services / activities started with the screen off between 00:00 and 06:00 |
| Background data hog | ≥ 50 MB/day background traffic (Android vitals threshold), via `NetworkStatsManager` |
| Notification spam | Notification counts from a `NotificationListenerService` (content is never read) |
| Accessibility / device admin | Enabled accessibility services, active device admins – escalated for sideloaded apps |
| Unrestricted battery | Battery-optimisation exemption + heavy background work |
| Storage hog | Very large cache (`StorageStatsManager`) |
| Device issues | Screen-off battery drain, temperature, thermal throttling, memory pressure (sampled every 15 min) |

## Architecture

Clean Architecture + MVVM, one Gradle module per feature, Koin DI, Coroutines/Flow, Room, DataStore and WorkManager. See [AGENTS.md](AGENTS.md) for the module map, conventions and quality gates.

```
app ─┬─ feature:report ─────┐
     ├─ feature:settings ───┼─ core:domain ─ core:model / core:common
     ├─ feature:monitoring ─┘        ▲
     ├─ core:designsystem            │ implements
     └─ core:data ───────────────────┘
```

## Build

Requirements: JDK 21 and the Android SDK (platform 37).

```bash
./gradlew assembleDebug                 # debug APK
./gradlew assembleRelease               # R8-minified release APK (debug-signed – configure your key before publishing)
./gradlew test testDebugUnitTest        # unit tests
./gradlew koverHtmlReport               # coverage report in build/reports/kover/html
./gradlew ktlintCheck :app:lintDebug    # lint
```

## Play Store notes

- `PACKAGE_USAGE_STATS` and notification access are special accesses granted by the user in system settings.
- `QUERY_ALL_PACKAGES` requires the *Package visibility* declaration in Play Console (core purpose: auditing installed apps, comparable to antivirus/security apps).
- Edge-to-edge, predictive back, adaptive navigation (bottom bar / rail), themed monochrome icon and no orientation locks.
