# Privacy Policy for Perfsentient

**Effective date:** November 18, 2026

Perfsentient ("the App", "we", "us") is an Android app developed by [neteinstein](https://github.com/neteinstein).
It monitors how your phone behaves and tells you which apps misbehave (pop-up ads, battery or data hogs,
notification spam and similar).

**The short version:** the App works entirely on your phone. It has **no internet permission**, so it
cannot send anything anywhere. We run no server, have no accounts, and collect nothing.

## At a glance

| Question | Answer |
|---|---|
| Does the App send data off your phone? | **No.** The `INTERNET` permission is removed from the app manifest. |
| Do we run a server or account system? | **No.** |
| Ads, analytics, crash-reporting SDKs? | **None.** |
| Do we sell or share your data? | **No**, we do not have it. |
| Where is data stored? | In a private database on your phone, excluded from Android cloud backup and device transfer. |

## 1. What the App reads on your phone

To do its job the App reads the following **on the device only**. None of it leaves the phone.

- **The list of installed apps** (`QUERY_ALL_PACKAGES`): names, package names, whether an app has a launcher
  icon, whether it can draw over other apps, holds an accessibility service or device-admin role, is exempt
  from battery optimisation, and the size of its cache.
- **App usage statistics** (`PACKAGE_USAGE_STATS`, "Usage access", which you grant in Android settings):
  foreground time, service time, activity launch/close events, and per-app network byte counts.
- **Notification counts** ("Notification access", which you grant in Android settings): **only a counter per
  app per day.** The App never reads, stores or forwards the title, text or content of a notification.
- **Phone vitals** sampled about every 15 minutes: battery level, charging state, temperature, thermal status,
  screen on/off and memory pressure.

## 2. What the App stores

- Vitals samples and notification counters: kept for **7 days**.
- Daily reports (score, verdict, findings per app): kept for **30 days**.
- Your settings (report time, theme) and your **trusted apps** list (package names and labels of apps you chose
  to trust).

All of it is in a private database on your phone. Android cloud backup and device transfer **exclude** the
database. Clearing the App's storage or uninstalling it removes everything.

## 3. Permissions and why

| Permission | Why |
|---|---|
| Query all packages | To see every installed app, including hidden ones, so misbehaving apps can be found. |
| Usage access | To measure how apps behave over the day. |
| Notification access | To count notifications per app (content is never read). |
| Post notifications | To show your end-of-day report. |

## 4. Sharing and third parties

None. The App contains no third-party analytics, advertising or tracking SDKs and cannot make network requests.

## 5. Children

The App is not directed at children and collects no personal information from anyone.

## 6. Your choices

You can revoke any permission in Android settings at any time (the App then stops that part of the analysis),
trust or un-trust any app in the Apps tab, and delete all data by clearing storage or uninstalling.

## 7. Changes

If this policy changes, the new version is published at the URL below and the effective date is updated.
Any change that adds network access would also be reflected in the Play Data safety form.

## 8. Contact

Questions: open an issue at [github.com/neteinstein/Perfsentient](https://github.com/neteinstein/Perfsentient/issues).

Source code (so every statement here can be checked): [github.com/neteinstein/Perfsentient](https://github.com/neteinstein/Perfsentient).
