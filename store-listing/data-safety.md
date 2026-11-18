# Data safety cheat-sheet (Play Console → App content → Data safety)

A starting point for the **Data safety** questionnaire, derived from [`PrivacyPolicy.md`](PrivacyPolicy.md)
and the app's code. It is **not legal advice**: complete the live questionnaire yourself, wording and
categories change over time.

Google's rule: **"collected" means transmitted off the device** to the developer or a third party.

Perfsentient removes the `INTERNET` permission from its manifest (`tools:node="remove"`, and the release
workflow fails if it comes back), so it **cannot transmit anything**. Everything it reads (app inventory,
usage stats, per-app network byte counts, notification counts, battery and memory samples) is processed and
stored on the phone, in a local Room database that is excluded from Android cloud backup and device transfer
(`app/src/main/res/xml/backup_rules.xml`, `data_extraction_rules.xml`).

## Suggested answers

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |
| Is all of the user data collected by your app encrypted in transit? | Not applicable (nothing is transmitted). If the form insists: *No data is transmitted*. |
| Do you provide a way for users to request that their data be deleted? | Not applicable (nothing is collected). Users can clear it by clearing app storage or uninstalling. |
| Data types | None collected, none shared |
| Third-party SDKs that collect data | None. No Firebase, no analytics, no ads, no crash reporting. |

Why each sensitive input is *not* "collected":

| Data the app reads | Why it is not "collected" |
|---|---|
| Installed apps (`QUERY_ALL_PACKAGES`) | Read and analysed on-device only, never leaves the phone. |
| App usage / foreground time (`PACKAGE_USAGE_STATS`) | Same. Vitals samples and notification counts are kept for 7 days, daily reports for 30 days, then deleted. |
| Notification counts per app (notification listener) | Only a counter per app per day is stored. Titles, text and content are never read or stored. |
| Per-app network byte counts | Local statistics from `NetworkStatsManager`. No traffic is inspected. |
| Battery, temperature, memory samples | Local only. |
| Trusted apps list | Package names and labels the user chose, stored in a local database. |

## Security practices section

- **Data is encrypted in transit:** n/a / No data is transmitted.
- **You can request that data is deleted:** n/a. Data never leaves the device; uninstalling or clearing storage removes it.
- **Committed to follow the Play Families policy:** No (the app is not designed for children).
- **Independent security review:** No.

## Keep in sync

If a future change adds a network permission, an SDK (analytics, crash reporting, ads) or any export or
share feature, this form and [`PrivacyPolicy.md`](PrivacyPolicy.md) must be updated **before** the release.
