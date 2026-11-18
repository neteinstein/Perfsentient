# Permissions declarations (Play Console → App content → Sensitive app permissions)

Perfsentient uses three permissions that Play treats as sensitive or special. Only the first needs a
declaration form; the other two are special accesses the *user* grants in Android settings.

## 1. `QUERY_ALL_PACKAGES` → Package visibility declaration (required)

**Core purpose to select:** *Device search / security* — an app that audits installed apps for harmful or
misbehaving behaviour (similar to antivirus and device-health apps).

Suggested text for the "Describe your use" field:

> Perfsentient is an on-device phone-health monitor. Its single purpose is to find installed apps that
> misbehave: pop-up and after-call ads, overlay abuse, hidden launcher icons, background data and battery
> hogs, notification spam, risky accessibility or device-admin access. To do that it must see the complete
> list of installed apps, including apps without a launcher icon (a hidden icon is itself one of the signals
> we report). A narrower `<queries>` list cannot work because the apps we need to inspect are unknown in
> advance. The list is processed only on the device and is never transmitted: the app has no INTERNET
> permission.

Notes for the reviewer: the permission is used by `AppInventoryDataSource` in `core:data`. The app name
and package are shown to the user in the report and in the Apps tab.

**Video link:** the form asks for a short video of the app being opened and the core feature in use. Upload
[`video/review/review-walkthrough.mp4`](video/review/README.md) to YouTube as *Unlisted* and paste the link.
It is captioned step by step, including how `QUERY_ALL_PACKAGES` is used.

## 2. `PACKAGE_USAGE_STATS` (Usage access) — special access, no form

Granted by the user in **Settings → Apps → Special app access → Usage access**. It powers foreground time,
background activity, launch/close events and per-app network totals. The Settings tab explains this with an
in-app rationale and a deep link to the right system screen before the user grants it. Re-checked on
`ON_RESUME`. Without it the app degrades gracefully (empty data, never crashes).

## 3. Notification access (`NotificationListenerService`) — special access, no form

Granted by the user in **Settings → Notifications → Device & app notifications**. Used **only to count**
notifications per app per day (to detect notification spam). The service never reads, stores or forwards
titles, text or any content. Declare this in the listing description (already done) and in the in-app
rationale.

## 4. Other permissions

| Permission | Purpose |
|---|---|
| `POST_NOTIFICATIONS` | Show the end-of-day report notification (runtime permission, Android 13+). |
| WorkManager's `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE` (merged from the library) | Re-schedule the 15-minute sampler after a reboot. The app runs **no** long-lived foreground service of its own. |

## Policy checklist

- [ ] Package visibility declaration submitted (section 1), with the review video link.
- [ ] Store listing mentions Usage access and Notification access and why (see `text/*/full_description.txt`).
- [ ] Data safety answers match [`data-safety.md`](data-safety.md).
- [ ] Privacy policy URL set (see [`README.md`](README.md#play-console-fields-not-covered-by-a-file)).
- [ ] Not an "Accessibility API" app: Perfsentient does **not** declare an accessibility service; it only *reports*
      other apps that hold one.
