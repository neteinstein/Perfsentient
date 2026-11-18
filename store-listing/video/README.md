# Launch video

Made with [brag](https://github.com/latent-spaces/brag) (the `brag-slim` workflow: story, visuals, audio and
render built from the project's own material). 20 s, 1920×1080, 30 fps, H.264 + AAC, 120 BPM soundtrack.

| File | What it is |
|---|---|
| `brag.mp4` | The video. Frame 0 is the poster. |
| `brag.jpg` | Poster / thumbnail (settled outro frame). |
| `brag-preview.gif` | Looping, auto-playing preview used in the repo README (GitHub does not autoplay `<video>`). |
| `brag-plan.md` | The angle, tone and scene-by-scene storyboard. |
| `share-copy.txt` | Post-ready caption. |

What it shows: the real Compose UI (rendered by `StoreScreenshotsTest`) with fictional apps and numbers. Nothing
in it is a device capture, and no real user data appears.

## Using it on Play

Play Console's **Video** field takes a **YouTube URL**, not a file: upload `brag.mp4` to YouTube
(public or unlisted, ads off) and paste the link. This is a promo video; it is **not** a permissions
demonstration video.

## Rebuilding

Needs Node 22 + Playwright (Chromium), ffmpeg and Python 3 with numpy. The screenshots in
`../screenshots/phone/` are the input.

```bash
node store-listing/scripts/brag_video/render.js /tmp/brag-output          # frames -> brag.mp4 + brag.jpg
store-listing/scripts/brag_video/gif.sh /tmp/brag-output/brag.mp4 store-listing/video/brag-preview.gif
```

`scene.html` holds the whole timeline (every frame is a pure function of time) and `music.py` synthesises the
soundtrack on the same beat grid as the cuts.
