# Testing instructions for Google Play

Two audiences: **App content → App access** (what a Google reviewer needs) and the **internal testing
track** (what a human tester does before promoting a release).

## 1. App content → App access

Perfsentient has **no login or account system**, so no credentials are needed. Because the app is only
useful once two special accesses are granted, still answer **"Yes, provide access instructions"** and paste:

> Perfsentient needs no account. On first launch open the Settings tab and tap Grant on each card:
> 1. Usage access: Android opens "Usage access". Choose Perfsentient and turn on "Allow usage access".
> 2. Notification access: Android opens the notification-access list. Choose Perfsentient and turn it on.
> 3. Notifications: allow the runtime prompt.
> When all three show a green tick the monitoring status says "Active".
> Go back to the Report tab and tap "Analyse now" to generate today's report immediately (normally it is
> generated at the time set in Settings, 21:30 by default). The Apps tab lists every app that was flagged now
> or in the past. Tap one to trust it (whitelist) or stop trusting it. Everything runs on the device; the app
> has no internet permission.

On a fresh device there may be nothing to flag. To see findings quickly, use a device with a few ad-supported
or sideloaded apps, or ask the developer for a build seeded with sample data.

## 2. Internal-track QA checklist

- [ ] Fresh install: Report tab shows the setup card; Settings shows 0/3 permissions.
- [ ] Grant all three accesses (leaving the app and returning re-checks them). Status becomes **Active**.
- [ ] **Analyse now** generates a report without crashing, also with nothing to flag (all-clear card).
- [ ] Tap an offender: the details sheet opens; **Open app info** opens the system App info screen.
- [ ] **Trust this app** in the sheet: the app disappears from the report, the score recalculates, the snackbar
      offers **Undo**.
- [ ] **Apps tab**: lists flagged apps; trusted apps have the teal tint and "Trusted" chip; tapping an app opens
      its sheet with **Trust** or **Stop trusting**; the colour flips immediately.
- [ ] A trusted app that was uninstalled still appears in the Apps tab and can be removed from the allowlist.
- [ ] Settings: report time picker, theme (light / dark / dynamic) work; changes survive process death.
- [ ] Revoke Usage access in system settings, return to the app: it degrades gracefully (no crash).
- [ ] Rotate / resize (foldable, tablet, split-screen): navigation rail on large screens, no clipped content.
- [ ] Edge-to-edge: nothing hidden behind the status or navigation bars; predictive back works.
- [ ] TalkBack: icons have descriptions, touch targets are at least 48 dp.
- [ ] Release build (R8) installs and behaves like debug.
- [ ] Merged release manifest has **no** `INTERNET` permission (the release workflow checks this).
