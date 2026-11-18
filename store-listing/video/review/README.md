# Play Console review video (QUERY_ALL_PACKAGES)

For **App content → Sensitive app permissions → Package visibility (QUERY_ALL_PACKAGES)**, Play asks for:

> a link to a short video (ideally 90 seconds or shorter) which shows your app being opened and the core feature
> you described being used. If it isn't obvious from the user interface how the QUERY_ALL_PACKAGES permission is
> being used in your app, provide a voice-over or captions to help explain.

`review-walkthrough.mp4` is that video: 1920×1080, 30 fps, about 60 s, with on-screen captions (no voice-over).

| Time | Step | Shown |
|---|---|---|
| 0–5 s | Title | What the video shows |
| 5–8.5 s | 1 · Opening the app | Tap the launcher icon, app opens |
| | 1 · Report | Health score, "142 apps analysed" (every installed app is checked) |
| | 2 · Core feature | Scroll to the misbehaving apps and what each did |
| | 3 · Apps tab | Every flagged installed app, trusted apps in teal |
| | 4 · Why QUERY_ALL_PACKAGES | Tap "Coupon Hunter": an app with **no launcher icon**, which a `<queries>` filter could not find |
| | 5 · Settings | Permissions, "no internet access" |
| | 6 · "Installed apps list" | Expand "Why is this needed?": the in-app QUERY_ALL_PACKAGES rationale |
| last 6 s | Outro | On-device only, no `INTERNET` permission |

## Uploading

Play wants a **link**, not a file. Upload the MP4 to YouTube as **Unlisted** (not Private: reviewers must be able
to open it without signing in to your account), then paste the URL in the declaration form. A Google Drive link
with "Anyone with the link can view" also works.

## How it is made (and what it is not)

The phone footage is the app's **real Compose UI** (`PerfsentienceAppUi`, real ViewModels and use cases), driven by
real taps and finger drags in `ReviewVideoFramesTest` (Robolectric) and recorded frame by frame. The data is the
fictional `SampleData` used for the store screenshots: every app name is made up (`com.example.*`). The launcher
screen and the app-open zoom in step 1 are drawn by the compositor; there is no emulator or device capture.

If a reviewer asks for a recording from a real device, capture one with the same route (open the app → Report →
Apps → tap a flagged app → Settings → "Installed apps list" → "Why is this needed?"):

```bash
adb shell screenrecord --time-limit 90 /sdcard/perfsentient-review.mp4
```

```bash
adb pull /sdcard/perfsentient-review.mp4
```

## Rebuilding

Needs the Android SDK, Node 22 + Playwright (Chromium) and ffmpeg. From the repo root:

```bash
REVIEW_VIDEO_DIR=/tmp/review-frames ./gradlew :app:testDebugUnitTest --tests '*ReviewVideoFramesTest*'
```

```bash
node store-listing/scripts/review_video/render.js /tmp/review-frames store-listing/video/review/review-walkthrough.mp4
```

The test is skipped unless `REVIEW_VIDEO_DIR` is set, so CI does not record frames. Captions live in
`store-listing/scripts/review_video/scene.html` (`STEPS`), keyed to the markers the test writes to `timeline.json`.
