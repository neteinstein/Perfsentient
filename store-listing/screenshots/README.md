# Screenshots

`phone/` holds eight 1080×2160 PNGs (24-bit, no alpha, 2:1 ratio: within Play's 2 to 8 images and 9:16 to 2:1 rules).
Upload them in filename order:

| # | File | Shows |
|---|---|---|
| 1 | `01-report.png` | Today's health score and device issues |
| 2 | `02-offenders.png` | The apps that misbehaved, with what each one did |
| 3 | `03-apps.png` | Apps tab: flagged apps in severity colours, trusted apps in teal |
| 4 | `04-settings.png` | Permissions, report time, trusted apps |
| 5 | `05-report-dark.png` | Report, dark theme |
| 6 | `06-apps-dark.png` | Apps tab, dark theme |
| 7 | `07-parental.png` | Parental help: apps on the phone flagged for the child's age (parental mode on) |
| 8 | `08-parental-dark.png` | Parental help warnings and the reviewed list, dark theme |

**How they are made.** `StoreScreenshotsTest` (in `app/src/test`) renders the app's real Compose UI
(`PerfsentienceAppUi`, all tabs, real ViewModels and use cases) on Robolectric with fake repositories and
fictional sample data (`SampleData.kt`). They are **not device captures**, and no emulator is involved.
Every app name, package and number in them is made up (the parental screenshots use a fictional catalogue,
so no real app is named). The test also runs in CI as a smoke test that each tab renders.

Regenerate after a UI change:

```bash
./gradlew :app:testDebugUnitTest --tests '*StoreScreenshotsTest*'
cp app/build/screenshots/*.png store-listing/screenshots/phone/
node store-listing/scripts/store_assets.js    # icon + feature graphic
```

The same English screenshots are used for every listing language; Play falls back to the default language's
images when a translation has none.

Tablet screenshots are optional and not included: the app uses an adaptive navigation rail on large screens, but
no tablet-specific layout was captured yet.
