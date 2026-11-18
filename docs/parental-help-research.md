# Parental help: research behind the concerning-apps list

Parental mode compares the apps installed on the phone with a bundled catalogue of apps that child-safety
organisations, regulators, courts and investigative journalists have raised concerns about, and checks each
one against the child's age. This document records **why each app is on the list, where its ages come from
and how severity is worked out**, so the list can be reviewed and kept current.

- Code: [`ParentalAppCatalog`](../core/domain/src/main/kotlin/org/neteinstein/perfsentient/core/domain/parental/ParentalAppCatalog.kt)
  (the list) and [`ParentalRiskAssessor`](../core/domain/src/main/kotlin/org/neteinstein/perfsentient/core/domain/parental/ParentalRiskAssessor.kt) (the age rules).
- Research date: November 2026. Every package name was checked against Google Play (title, developer and
  content rating). Apps removed from Play (Tango, LiveMe, ASKfm's old ID, Whisper) keep their last known ID
  because they can still be installed from old installs or APK sites.

## How an app is assessed

Each entry has:

| Field | Meaning |
|---|---|
| `minimumAge` | The age the platform itself requires in its terms (0 when it sets none or it differs by country). |
| `recommendedAge` | The age from which the app is considered reasonable: the higher of expert guidance and the Google Play (IARC) content rating. |
| `baseSeverity` | How serious a warning is for a child between the two ages. |
| `concerns` | Documented hazards, shown as chips with an explanation (grooming cases, sextortion, strangers, adult content, …). |

Rules ([`ParentalRiskAssessor`](../core/domain/src/main/kotlin/org/neteinstein/perfsentient/core/domain/parental/ParentalRiskAssessor.kt)):

1. The app is flagged only while the child is **younger than `recommendedAge`**.
2. Severity starts at `baseSeverity`, goes **up one level** if the child is younger than the platform's own
   `minimumAge`, and **up one more** if the child is **5 or more years** below `recommendedAge`. It never goes above High.
3. Parents can mark a warning as **reviewed** (e.g. after setting up the app's own parental controls). It
   moves to a separate *Reviewed* list; nothing is hidden.

Example (child aged 10): Roblox (suggested 13+, base Medium) → **Medium**; Discord (minimum 13, suggested 16+,
base Medium) → below the minimum (+1) and 6 years under (+1) → **High**; any dating app → **High**.

## Where the ages come from

- **Platform terms**: 13 for most US-based social apps (COPPA); 18 for dating, Replika, Nomi, Kindroid and
  real-money gambling. Telegram requires 18 only in the EU, UK and (since January 2026) Australia, so its
  `minimumAge` is left at 0 and the 17+ Play rating drives the warning.
- **Google Play content rating (US, IARC)**: Everyone 10+ → 10, Teen → 13, Mature 17+ → 17, Adults only → 18.
  E.g. Kik, Telegram, X, Reddit, Tumblr, Yubo, Monkey, Azar, OmeTV, Chai and PolyBuzz are rated Mature 17+.
- **Australia's Social Media Minimum Age** (in force 10 Dec 2025): accounts under 16 are banned on Facebook,
  Instagram, Snapchat, TikTok, X, Reddit, Threads, YouTube, Kick and Twitch. Used as the suggested age (16) for
  mainstream social media.
- **Common Sense Media**: Roblox 13+; Kik, Tellonym, Yubo, Monkey, Telegram 17+; Whisper 18+; AI companions
  "not for anyone under 18" (2025 risk assessment with Stanford).

## The catalogue

### Games with open chat (suggested 13+ unless noted)

| App | Package(s) | Notes |
|---|---|---|
| Roblox | `com.roblox.client` | **Grooming.** Lawsuits by several US state attorneys general (Louisiana, Kentucky, Texas, Florida, Iowa, Nebraska, Oklahoma, 2025–26), settlements with Nevada, Alabama and West Virginia (2026) and 100+ private cases consolidated in federal MDL 3166. Adults move children to Discord/Snapchat. Base Medium. |
| Minecraft | `com.mojang.minecraftpe` | Public servers with open chat. Play: Everyone 10+ → suggested 10+. |
| Fortnite | `com.epicgames.fortnite` | Voice/text chat with strangers, violence, V-Bucks. |
| Free Fire / MAX | `com.dts.freefireth`, `com.dts.freefiremax` | Chat with strangers, violence, spending. |
| PUBG Mobile | `com.tencent.ig` | Chat with strangers, violence, spending. |
| Call of Duty: Mobile | `com.activision.callofduty.shooter` | Play: Mature 17+ → suggested 17+, base Medium. |
| Steam / Steam Chat | `com.valvesoftware.android.steam.community`, `…steam.friendsui` | 13+ terms; chat rooms and adult games. |

### Avatar worlds and social VR (suggested 13+, base Medium)

Avakin Life (`com.lockwoodpublishing.avakinlife`), IMVU (`com.imvu.mobilecordova`, 16+), ZEPETO (`me.zepeto.main`,
Low), Highrise (`com.pz.life.android`), VRChat (`com.vrchat.mobile.playstore`, 16+), Rec Room (`com.recroomapp.app`, Low).
Evidence: BBC investigation of VRChat (researcher posing as a 13-year-old met adult men and simulated sex);
NSPCC research on immersive tech (children under 16 reaching "erotic role-play" areas); ActiveFence on
private rooms in public VR worlds used to isolate children.

### Social media (suggested 16+, minimum 13)

| App | Package(s) | Notes |
|---|---|---|
| Instagram | `com.instagram.android` | **Sextortion**: the most-used platform for financial sextortion of teens (NCRI). 40+ state AGs sued Meta over youth harms (2023). |
| TikTok | `com.zhiliaoapp.musically`, `com.ss.android.ugc.trill`, `com.zhiliaoapp.musically.go` | Live video, endless feed; 14 state AGs sued TikTok (2024). |
| Facebook | `com.facebook.katana` | Base Low; part of the 2023 multistate action against Meta. |
| Threads | `com.instagram.barcelona` | Base Low. |
| X | `com.twitter.android` | Play Mature 17+: adult content allowed. |
| Reddit | `com.reddit.frontpage` | Play Mature 17+: NSFW communities, anonymous. |
| Tumblr | `com.tumblr` | Play Mature 17+. |

### Private and disappearing messaging

| App | Package(s) | Notes |
|---|---|---|
| Snapchat | `com.snapchat.android` | **Sextortion** (#2 platform in NCRI's research); disappearing messages, Snap Map location. New Mexico AG sued Snap (Sept 2024) over predators and sextortion. Suggested 16+. |
| Discord | `com.discord` | **Grooming**: the usual destination when predators move children off games; New Jersey AG lawsuit (April 2025); joint Roblox/Discord grooming suits. Suggested 16+. |
| Kik | `kik.android` | **Grooming**, anonymous sign-up without a phone number; NCOSE "Dirty Dozen"; US Senate inquiry (June 2026). Play 17+, Common Sense 17+. Base High. |
| Telegram / Telegram X | `org.telegram.messenger`, `org.thunderdog.challegram` | Ofcom investigation over child sexual abuse material (April 2026); 18+ in EU/UK/AU terms. Play 17+. Base High. |
| WhatsApp, Messenger | `com.whatsapp`, `com.facebook.orca` | Everyday messengers: only flagged **below their own minimum age (13)**. |

### Friend-finding / "Tinder for teens" (suggested 17+)

| App | Package(s) | Notes |
|---|---|---|
| Wizz | `info.wizzapp` | **Sextortion**: #3 platform in NCRI's research; pulled from the App Store and Google Play in January 2024 after NCOSE complaints; the Canadian Centre for Child Protection warned parents after it returned. Suggested 18+, base High. |
| Yubo | `co.yellw.yellowapp` | Live video rooms with strangers. Play 17+, Common Sense 17+. |
| Hoop, purp, BeFriend (ex-Spotafriend), Wink / Wink Social, LMK | `com.dazz.hoop`, `com.hubolabs.hubo`, `com.guppir.swipr`, `co.ninecount.wink`, `co.ninecount.soda`, `com.lightspace.lmk` | Swipe to add strangers, often by location. LMK was suspended from Snapchat's developer platform in 2021 after a lawsuit over a teen's suicide. |

### Random video chat (18+, High)

HOLLA (`com.exutech.chacha`), Omega (`ly.omegle.android`), Camsea (`com.camsea.videochat`), Azar (`com.azarlive.android`),
Chatous (`com.chatous.chatous`), Minichat (`mini.video.chat`), OmeTV (`omegle.tv`), Chat Alternative
(`com.chatroullete.alternative`), Chatspin (`com.Chatspin`), Emerald Chat (`com.cookie.emerald`), Monkey (`cool.monkey.android`).
Omegle shut down in November 2023 after a child-abuse lawsuit; these successors keep the same model: live
video with random strangers, frequent nudity and no reliable age checks. All rated Mature 17+ on Play.

### Anonymous messages and Q&A (suggested 17+)

| App | Package(s) | Notes |
|---|---|---|
| NGL | `com.nglreactnative` | FTC/LA DA settlement (July 2024, $5M): **banned from offering the app to minors**; fake AI-moderation and fake-message claims. 18+, High. |
| Sendit | `com.fullsenders.sendit` | FTC complaint (2025): signed up under-13s, sent fake provocative messages, deceptive subscriptions. 18+, High. |
| Tellonym | `de.tellonym.app` | Common Sense 17+. |
| ASKfm | `com.askfm`, `app.askfm.mobile` | Anonymous Q&A long linked to bullying. |
| Whisper | `sh.whisper` | Common Sense 18+; anonymous confessions and contact. High. |
| Jodel | `com.tellm.android.app` | Anonymous, hyper-local feed. |

### Live streaming with paid gifts (suggested 18+ unless noted)

Bigo Live (`sg.bigo.live`), LiveMe (`com.cmcm.live`, `com.plusme.live`), Tango (`com.sgiggle.production`), MICO
(`com.mico`, 17+), YouNow (`younow.live`, 16+), Likee (`video.like`, 16+), Twitch (`tv.twitch.android.app`, 16+, Low),
Kick (`com.kick.mobile`, gambling streams). Evidence: New York Times (Dec 2024) on Bigo Live, Tango and others
where app reviews described child sexual exploitation, and ~100 US federal cases of paid livestreamed child
abuse; earlier Channel 4 / FOX investigations of Live.me (2017–18) on predators "tipping" child broadcasters.
Gifts are a recognised grooming tactic.

### Adult dating (18+, High)

Tinder, Bumble, Hinge, Grindr, Badoo, Plenty of Fish, OkCupid, happn, Feeld, Hily, Tagged, MeetMe, Skout
(packages in the code). All Mature 17+ on Play with 18+ terms.

### AI companions (18+, High)

Character.AI (`ai.character.app`), Replika (`ai.replika.app`), Chai (`com.Beauchamp.Messenger.external`), Talkie
(`com.weaver.app.prod`), PolyBuzz (`ai.socialapps.speakmaster`), Nomi (`ai.nomi.twa`), Kindroid (`com.kindroid.app`).
Evidence: Common Sense Media + Stanford risk assessment (April 2025) found sexual role-play and harmful advice
and recommends **no use under 18**; FTC 6(b) orders to seven companion makers (Sept 2025); lawsuits over teen
suicides led Character.AI to end open-ended chats for under-18s (Nov 2025); Washington Post / NYT reporting on
teens' sexual chats with PolyBuzz and Talkie.

### Secret vaults and app hiders (suggested 18+, Medium)

Hide It Pro (`com.hideitpro`), Calculator – photo vault (`com.hld.anzenbokusucal`), Keepsafe (`com.kii.safe`),
LockMyPix (`com.fourchars.lmpfree`, `com.fourchars.lmp`), Gallery Vault (`com.thinkyeah.galleryvault`), Vaulty
(`com.theronrogers.vaultyfree`), Photo Lock App (`vault.gallery.lock`), Calculator Vault – App Hider
(`com.app.calculator.vault.hider`), HideU (`com.calculator.hideu`). Disguised as calculators or utilities; used to
hide images (including ones used for sextortion) and other apps. The copy acknowledges legitimate privacy use
and suggests talking rather than snooping. Perfsentient's existing *Hidden icon* detector complements this.

### Gambling (18+)

bet365, DraftKings, FanDuel (real money, minimum 18, High); Slotomania and Zynga Poker (casino-style, Medium).

### Filter bypass (suggested 16+, Low)

Tor Browser (`org.torproject.torbrowser`) and Orbot (`org.torproject.android`): route around network filters and
parental controls; reach hidden services. The copy notes privacy is a fair reason for older teens.

## Left out on purpose

- **YouTube, WhatsApp/Messenger above 13, Pinterest, Spotify, Canva, Google Docs**: chat or comments have been
  misused, but flagging every mainstream app would bury the real warnings. The "Warning signs" card covers the
  pattern (adults moving chats to private apps).
- **Yik Yak / Sidechat**: Android listing could not be confirmed with confidence.
- **"Nudify" deepfake apps** (e.g. CrushAI, sued by Meta in June 2025): not on Google Play and change names
  constantly, so package names cannot be listed reliably.
- **VPNs**: too many to list; Tor is included as the best-known filter bypass.

## Sources

- FBI / NCRI financial sextortion research (Instagram, Snapchat, Wizz) via [Scripps News](https://www.scrippsnews.com/us-news/crime/popular-social-media-apps-fuel-a-disturbing-surge-in-child-sextortion) and [NBC](https://www.nbcbayarea.com/news/business/money-report/fbi-financial-sextortion-of-teens-is-a-rapidly-escalating-threat-how-parents-can-protect-their-kids/3440540/)
- Roblox: [Malwarebytes (2026)](https://www.malwarebytes.com/blog/news/2026/04/roblox-clamps-down-on-chats-and-age-checks-as-legal-pressure-builds), [Oklahoma AG (2026)](https://oklahoma.gov/oag/news/newsroom/2026/may/drummond-files-lawsuit-against-roblox-for-endangering-children-and-deceiving-parents.html), [Nebraska Public Media](https://nebraskapublicmedia.org/en/news/news-articles/attorney-general-sues-gaming-platform-roblox-over-child-safety-issues/), [KPTV (2026)](https://www.kptv.com/2026/07/15/new-lawsuit-filed-against-roblox-alleges-oregon-girl-groomed-through-in-game-chat/), [RNZ](https://www.rnz.co.nz/news/national/531463/roblox-a-hunting-ground-for-sex-offenders-to-groom-child-gamers-online-safety)
- Discord: [New Jersey Monitor (2025)](https://newjerseymonitor.com/2025/04/17/new-jersey-attorney-general-sues-discord-app-for-alarming-security-lapses/); Snap / Meta / TikTok state actions: [JD Supra](https://jdsupra.com/legalnews/states-crack-down-on-tech-platforms-8702517)
- Wizz: [NBC News](https://www.nbcnews.com/tech/social-media/wizz-tinder-app-aimed-teens-removed-apple-google-stores-rcna136607), [NBC News (Canada warning)](https://www.nbcnews.com/tech/tech-news/canadas-child-safety-watchdog-warns-parents-wizz-tinder-app-teens-rcna138732)
- Kik: [Sen. Blackburn (2026)](https://www.blackburn.senate.gov/2026/6/technology/blackburn-investigates-kik-for-child-exploitation-and-sexual-abuse); Telegram: [IWF on Ofcom investigation (2026)](https://www.iwf.org.uk/news-media/iwf-in-the-news/uk-regulator-probes-telegram-over-child-sexual-abuse-concerns/), [Open Terms Archive (age 18)](https://opentermsarchive.org/en/memos/telegram-expands-its-age-eligibility-requirement-to-australian-users/)
- NGL: [TechCrunch (2024)](https://techcrunch.com/2024/07/09/ftc-bans-ngl-from-offering-its-anonymous-social-app-to-minors); Sendit: [Malwarebytes (2025)](https://www.malwarebytes.com/blog/news/2025/10/sendit-app-tricked-kids-harvested-their-data-and-faked-messages-ftc-claims)
- Live streaming: [Business & Human Rights Resource Centre on NYT investigation](https://business-humanrights.org/en/latest-news/apple-google-livestream-apps-allegedly-facilitate-child-sexual-exploitation)
- VR / avatar worlds: [ActiveFence](https://activefence.com/blog/the-real-risks-children-vr), [Newshub on VRChat report](https://www.newshub.co.nz/home/technology/2022/02/children-exposed-to-rape-threats-racism-sexual-activity-in-vrchat-app-report.html)
- AI companions: [Common Sense Media risk assessment](https://institute.commonsensemedia.org/risk-assessments/social-ai-companions), [Fortune on Character.AI under-18 ban](https://fortune.com/2025/10/29/character-ai-ban-children-teens-chatbots-regulatory-pressure-age-verification-online-harms), [Washington Times on FTC orders](https://m.washingtontimes.com/news/2025/sep/11/ftc-probes-ai-companions-bonding-children-teens/)
- Random video chat: [Internet Matters on Omegle](https://www.internetmatters.org/?p=13697), [Aura](https://www.aura.com/learn/is-omegle-safe)
- Vault apps: [Security.org](https://www.security.org/news/teen-hiding-apps/), [Bitdefender](https://bitdefender.com/en-us/blog/hotforsecurity/kids-using-secret-decoy-apps-what-parents-need-to-know-about-the-dangers)
- Messaging inside everyday apps: [Bark](https://www.bark.us/blog/top-ten-apps-hidden-messages/)
- Australia's under-16 law: [eSafety](https://esafety.gov.au/newsroom/media-releases/platforms-on-notice-to-comply-with-social-media-minimum-age), [Biometric Update](https://www.biometricupdate.com/202511/australian-esafety-commissioner-announces-platforms-covered-by-social-media-law)
- Common Sense Media guides: [Discord](https://www.commonsensemedia.org/articles/parents-ultimate-guide-to-discord), [Snapchat](https://www.commonsensemedia.org/articles/parents-ultimate-guide-to-snapchat), [Kik](https://www.commonsensemedia.org/app-reviews/kik), [Yubo](https://www.commonsensemedia.org/app-reviews/yubo), [Tellonym](https://www.commonsensemedia.org/app-reviews/tellonym), [Monkey](https://www.commonsensemedia.org/app-reviews/monkey)
- Nudify apps: [TechCrunch on Meta v. CrushAI](https://techcrunch.com/2025/06/12/meta-sues-ai-nudify-app-crush-ai-for-advertising-on-its-platforms)
- NCMEC CyberTipline 2025 data: [missingkids.org](https://www.missingkids.org/cybertiplinedata)

Many secondary sources are advocacy groups or law firms; claims were cross-checked against news reports or
official releases where possible, and only well-documented concerns were encoded as `GROOMING_REPORTS`,
`SEXTORTION_REPORTS` or `SAFETY_ACTION`.

## Keeping the list current

1. Add or edit an entry in `ParentalAppCatalog.DEFAULT_APPS` (pick the category; override `minimumAge`,
   `recommendedAge`, `severity` or add `extra` concerns only with a source).
2. Verify the package on Google Play: `https://play.google.com/store/apps/details?id=<package>&hl=en&gl=US`
   (check the title, developer and content rating).
3. Add the source to this document. `ParentalAppCatalogTest` checks ids, package-name format, duplicates and ages.
