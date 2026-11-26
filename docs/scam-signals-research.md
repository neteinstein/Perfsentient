# Scam & fraud signals: research behind the detectors

Perfsentient looks at the *app* side of phone scams: how an app arrived, what it can read and control, and
whether it imitates something trusted. This document records **why each rule exists, where each list entry
comes from, and how severity is worked out**, so the lists can be reviewed and kept current.

- Code: [`ScamSignalCatalog`](../core/domain/src/main/kotlin/org/neteinstein/perfsentient/core/domain/scam/ScamSignalCatalog.kt)
  (the lists), [`LabelSkeleton`](../core/domain/src/main/kotlin/org/neteinstein/perfsentient/core/domain/scam/LabelSkeleton.kt)
  (look-alike names) and the detectors in [`analysis/detector`](../core/domain/src/main/kotlin/org/neteinstein/perfsentient/core/domain/analysis/detector).
- Research date: November 2026. Every input is read on the phone with public APIs that need no extra
  permission (`QUERY_ALL_PACKAGES` is already declared for the app audit). Nothing leaves the phone.

## The scam chain we look for

Phone fraud on Android follows a well-documented chain:

1. **A message or a call** pushes the victim to install something: a parcel-tracking app (FluBot), a "security
   update", a bank's "verification app", or a remote-support tool.
2. **The app is installed outside a store**, from a link in a chat, text or email, or from a browser download.
3. **It asks for access** that lets it read one-time codes or control the phone: SMS, notification access,
   accessibility, "display over other apps", or "install unknown apps" to fetch the real payload.
4. **The account is drained** while the victim watches, or the attacker watches the screen remotely.

Google's own countermeasures target the same steps:

- **Play Protect enhanced fraud protection** (pilot in Singapore with the Cyber Security Agency, February 2024,
  later extended to more countries) blocks apps installed from *internet-sideloading sources* (browsers,
  messaging apps, file managers) when they ask for sensitive permissions that fraud apps abuse to read one-time
  codes. Sources: [Google Security Blog, "Piloting new ways to protect Android users from financial fraud"](https://security.googleblog.com/2024/02/),
  [CSA Singapore press release](https://www.csa.gov.sg/news-events/press-releases/google-partners-with-the-cyber-security-agency-of-singapore-to-pilot-a-new-enhanced-security-feature-to-better-protect-android-mobile-users-from-scams/),
  [TechCrunch, 7 February 2024](https://techcrunch.com/2024/02/07/google-starts-blocking-users-from-sideloading-certain-apps-in-singapore/).
- **Android 16 in-call protections** (May 2025) stop the user from enabling sideloading or granting accessibility
  during a call with an unknown number, because scammers talk victims through both. Source:
  [Google Security Blog, "What's new in Android security and privacy in 2025"](https://security.googleblog.com/2025/05/whats-new-in-android-security-privacy-2025.html),
  [Android Authority](https://www.androidauthority.com/new-anti-scam-features-android-show-3556831).

Perfsentient cannot block anything, but it can tell the user afterwards, on any Android version from 12L.

## Detectors

| Behaviour | Fires when | Severity | Why |
|---|---|---|---|
| `RISKY_INSTALL_SOURCE` | Not from a store, and installed from a messaging/email app; or downloaded through a browser / file manager (or `PackageSource` DOWNLOADED_FILE / LOCAL_FILE on Android 13+) **and** holding a sensitive capability | Messaging → Medium; + capability → High; download + capability → Medium | Mirrors Play Protect's fraud pilot: internet-sideloaded apps that ask for SMS, notification or accessibility access |
| `DATA_THEFT_RISK` ("can read your codes") | Has notification access, a granted `READ_SMS`/`RECEIVE_SMS`, or is the default SMS app | Accessibility + overlay on top → High (banking-trojan set); not from a store → High; store app reading texts without being the SMS app → Medium; store app with notification access only → Low | One-time codes arrive by SMS and in notifications. A store-installed default SMS app is not flagged for doing its job |
| `REMOTE_ACCESS` | A catalogue remote-access app was installed in the last 72 h | Medium; High when installed during a call or within 30 min after one (dialer screen in usage events), or with accessibility on | Bank-impersonation and tech-support scams |
| `IMPERSONATION` | Not from a store, and the label (case, accents, spacing, look-alike characters ignored) matches an impersonated name while the package is not the genuine one | Medium; High when it also hides its icon or holds a sensitive capability | Fake updates, fake parcel apps, modded messengers |
| `CAN_INSTALL_APPS` | Allowed to "install unknown apps" | Not from a store → Medium; store messaging app or browser → Low. App stores (F-Droid, Aurora…) are skipped | Droppers fetch the real malware after install |
| `ACCESSIBILITY_RISK` (changed) | A store app with accessibility on that was installed in the last 72 h | Low → **Medium** | Play "dropper" apps get accessibility within days of install |
| `HIDDEN_ICON` (changed) | Notification access now counts as a risky capability | High | A hidden app that reads every notification is a spy |

"Sensitive capability" = accessibility, notification access, device admin, display over other apps, default
SMS app, or a granted `READ_SMS`, `RECEIVE_SMS`, `SEND_SMS`, `READ_CALL_LOG` or `ANSWER_PHONE_CALLS`.

### Evidence for the rules

- **Accessibility + overlay banking trojans**: Anatsa (TeabBot) spreads through Google Play "dropper" apps (PDF
  readers, cleaners) that turn malicious weeks after release, abuse accessibility to click through screens,
  and steal credentials with overlays. [ThreatFabric, "Anatsa Trojan returns"](https://www.threatfabric.com/blogs/anatsa-trojan-returns-targeting-europe-and-expanding-its-reach),
  [BleepingComputer](https://www.bleepingcomputer.com/news/security/anatsa-android-malware-downloaded-150-000-times-via-google-play/),
  [Zscaler ThreatLabz](https://www.zscaler.com/blogs/security-research/technical-analysis-anatsa-campaigns-android-banking-malware-active-google).
- **Fake parcel / voicemail apps sent by text**: FluBot posed as DHL, FedEx and voicemail apps, then asked for
  accessibility to steal banking credentials. Europol took its infrastructure down in June 2022.
  [Help Net Security](https://www.helpnetsecurity.com/2022/06/01/flubot-takedown/),
  [Proofpoint](https://www.proofpoint.com/uk/blog/threat-insight/flubot-android-malware-spreading-rapidly-through-europe-may-hit-us-soon),
  [NCC Group](https://nccgroup.com/us/research-blog/flubot-the-evolution-of-a-notorious-android-banking-malware).
- **Remote-access scams**: callers posing as banks or tech support ask victims to install AnyDesk, TeamViewer
  QuickSupport or similar and read out the access code. [Which?](https://www.which.co.uk/news/2020/11/remote-access-scams-the-call-that-could-wipe-out-your-life-savings/),
  [Starling Bank](https://www.starlingbank.com/docs/security/remote-access-apps.pdf),
  [Scamwatch (Australia)](https://www.scamwatch.gov.au/node/1166), [Guardio](https://guard.io/blog/anydesk-scams-the-good-the-bad-and-the-risky).
  Banks in India (SBI, HDFC) and the RBI issued public warnings about AnyDesk on Android.

## Lists

### Messaging and email apps (`MESSAGING_APPS`)

An APK opened from one of these was sent to the user as a link or attachment. WhatsApp (`com.whatsapp`,
`com.whatsapp.w4b`), Telegram (`org.telegram.messenger`, `org.telegram.messenger.web`, Telegram X
`org.thunderdog.challegram`), Signal (`org.thoughtcrime.securesms`), Messenger (`com.facebook.orca`,
`com.facebook.mlite`), Facebook (`com.facebook.katana`, `com.facebook.lite`), Instagram, Viber, WeChat
(`com.tencent.mm`), LINE, KakaoTalk, imo, Discord, Snapchat, TikTok (in-app browser), Skype, Google Messages,
Samsung Messages, AOSP Messaging, Gmail, Outlook, Samsung Email, Yahoo Mail, Proton Mail. These are the apps'
long-standing Google Play IDs.

### Browsers and file managers (`BROWSERS_AND_FILES`)

Chrome (stable, beta, dev, canary), Firefox (+ beta, Focus), Edge (`com.microsoft.emmx`), Brave, Opera (+ Mini,
GX), Samsung Internet (`com.sec.android.app.sbrowser`), DuckDuckGo, Vivaldi, Kiwi, UC Browser, Mi Browser,
Huawei Browser, Yandex Browser; Files by Google, the system Files app (`com.android.documentsui`,
`com.google.android.documentsui`), Samsung My Files, Xiaomi File Manager. On Android 13+ the system's own
`PackageSource` (DOWNLOADED_FILE / LOCAL_FILE) covers browsers not listed here.

### Remote-access apps (`REMOTE_ACCESS_APPS`)

| App | Package | Checked |
|---|---|---|
| AnyDesk | `com.anydesk.anydeskandroid` | Vendor's Play listing ID; named in bank warnings above |
| TeamViewer QuickSupport | `com.teamviewer.quicksupport.market` | Vendor's Play listing ID; most misused brand per Which? |
| TeamViewer Host | `com.teamviewer.host.market` | Vendor's Play listing ID |
| RustDesk | `com.carriez.flutter_hbb` | [F-Droid listing](https://f-droid.org/packages/com.carriez.flutter_hbb) |
| AirDroid | `com.sand.airdroid` | Vendor's Play listing ID |
| Splashtop SOS | `com.splashtop.sos` | Product page: [Splashtop SOS](https://www.splashtop.com/SOS); ID not re-checked online, re-check on Play |
| Zoho Assist – Customer | `com.zoho.assist.agent` | Product page: [Zoho Assist for Android](https://www.zoho.com/assist/downloads/android.html); ID not re-checked online, re-check on Play. Scamwatch lists Zoho among misused tools |

Remote-access apps are legitimate tools; only a **recent** install is reported, and the copy tells the user
what to do if someone asked them to install it.

### Impersonated names (`IMPERSONATED_NAMES`)

Only apps that did **not** come from a store are checked, so a wrong or missing genuine package can at worst
flag a sideloaded copy of a real app. Labels are compared after [`LabelSkeleton`](../core/domain/src/main/kotlin/org/neteinstein/perfsentient/core/domain/scam/LabelSkeleton.kt)
lower-cases, strips accents, maps Cyrillic/Greek look-alikes and digit swaps (`Wh4tsApp`) and joins spaced
letters (`W h a t s A p p`), ported from the [Scam-Detector](https://github.com/neteinstein/Scam-Detector)
extension (`src/lib/lookalike.js`). Keywords starting with `=` must be the whole label, so "Settings" matches
but "Settings Backup" does not.

| Group | Names | Source |
|---|---|---|
| System components | System update, Google Play services, Play Protect, Play Store, Android System, Settings, Chrome | Fake "updates" are the classic lure: FluBot, Anatsa's "PDF Update", and Europol's FluBot advice |
| Parcels and voicemail | DHL, FedEx, UPS tracking, parcel/package tracking, Voicemail, Flash Player | FluBot lures (above) |
| Messengers and social | WhatsApp, Telegram, Signal, Instagram, Facebook, TikTok, Netflix, Spotify | Modded "GB"/"Plus" builds distributed outside Play are a common malware carrier |
| Payments, banks, crypto, authenticators | PayPal, Revolut, Wise, N26, Cash App, Venmo, MB WAY, Santander, BBVA, CaixaBank, Millennium BCP, CGD, Novo Banco, ING, Deutsche Bank, Sparkasse, Barclays, HSBC, Lloyds, NatWest, Chase, Bank of America, Wells Fargo, Coinbase, Binance, MetaMask, Trust Wallet, Ledger, Google and Microsoft Authenticator | Scam-Detector's impersonated-brand list (`src/lib/brands.js`), limited to brands with Android apps that fraud apps copy |

## Device security posture

| Issue | Read from | Severity | Why |
|---|---|---|---|
| `NO_SCREEN_LOCK` | `KeyguardManager.isDeviceSecure` | Medium | Without a lock, anyone holding the phone can read codes and reset accounts |
| `OUTDATED_SECURITY_PATCH` | `Build.VERSION.SECURITY_PATCH` | Low after 90 days, Medium after 180 | Android publishes [security bulletins](https://source.android.com/docs/security/bulletin) monthly; three missed months is a clear lag |
| `USB_DEBUGGING` | `Settings.Global.ADB_ENABLED` | Low | A connected computer can install and control apps; useful for developers, risky to leave on |

## Adding or changing an entry

1. Add the package or name to `ScamSignalCatalog` and a row (or a line) here with its source.
2. Prefer the vendor's own Play listing or an F-Droid listing for package IDs; mark IDs you could not check.
3. Add or update a test in `ScamSignalCatalogTest` / `ScamDetectorsTest` for anything that changes a match.
