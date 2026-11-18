# Play Store listing

Everything needed to publish **Perfsentient** (`org.neteinstein.perfsentient`) on
[Google Play](https://play.google.com/console) lives in this folder: the listing texts, graphics,
screenshots, promo video, the Data safety and permission-declaration answers, the privacy policy, and the
release/upload pipeline guide.

## What's here

```
store-listing/
├── README.md                    this file: field-by-field Play Console guide
├── publishing.md                signing, versioning, GitHub release + Play upload workflows
├── PrivacyPolicy.md             the privacy policy (Play requires a public URL)
├── data-safety.md               cheat-sheet for the Data safety questionnaire
├── permissions-declaration.md   QUERY_ALL_PACKAGES declaration + special accesses
├── testing-instructions.md      App access instructions + internal-track QA checklist
├── text/
│   ├── en-US/                   title, short_description, full_description, release_notes
│   └── pt-PT/                   same four files, in European Portuguese
├── graphics/                    icon-512.png, feature-graphic-1024x500.png
├── screenshots/                 phone/ (+ tablets) PNGs, in upload order
└── video/                       brag launch video (mp4 + README autoplay GIF)
```

## Copy-pasting the text fields

| Play Console field | File | Limit |
|---|---|---|
| App name | `text/<locale>/title.txt` | 30 |
| Short description | `text/<locale>/short_description.txt` | 80 |
| Full description | `text/<locale>/full_description.txt` | 4000 |
| What's new | `text/<locale>/release_notes.txt` | 500 |

Paste each file verbatim for its locale (`en-US` is the default language, add `pt-PT` as a translation).
Re-check lengths after editing:

```bash
python3 - <<'PY'
import pathlib
limits = {'title.txt': 30, 'short_description.txt': 80, 'full_description.txt': 4000, 'release_notes.txt': 500}
for d in sorted(pathlib.Path('store-listing/text').iterdir()):
    for name, limit in limits.items():
        n = len((d / name).read_text(encoding='utf-8').rstrip('\n'))
        print(f'{d.name}/{name}: {n}/{limit}', 'OK' if n <= limit else 'OVER!')
PY
```

## Play Console fields not covered by a file

- **App category:** Tools (alternative: Productivity)
- **Tags:** device performance, battery, security, app manager
- **Content rating:** Everyone. Run the questionnaire: no user-generated content, no ads, no web access.
- **Contact email:** the address you use for the developer account (shown publicly)
- **Privacy policy URL:** `https://github.com/neteinstein/Perfsentient/blob/main/store-listing/PrivacyPolicy.md`
- **Website:** `https://github.com/neteinstein/Perfsentient`
- **Target audience:** 18+ (not designed for children)
- **Ads:** No
- **App access:** instructions in [`testing-instructions.md`](testing-instructions.md)
- **Data safety:** answers in [`data-safety.md`](data-safety.md)
- **Declarations:** [`permissions-declaration.md`](permissions-declaration.md)

## Graphics & screenshots

- `graphics/icon-512.png`: 512×512 hi-res icon (the shield + pulse launcher icon).
- `graphics/feature-graphic-1024x500.png`: the feature graphic.
- `screenshots/phone/`: 1080×1920 PNG, upload in filename order (Play needs 2 to 8).
- Tablet folders are optional (7-inch and 10-inch slots under "Large screens").

The screenshots are rendered from the app's real Compose screens (see
[`screenshots/README.md`](screenshots/README.md)); they are **not** device captures. Every app name, number
and finding in them is fictional sample data.

## Promo video

`video/brag.mp4` is a short launch video made with [brag](https://github.com/latent-spaces/brag).
Play Console's **Video** field takes a **YouTube URL**, not a file: upload the MP4 to YouTube (public or
unlisted, ads off) and paste the link. See [`video/README.md`](video/README.md).

## Release checklist

1. First upload is manual (see [`publishing.md`](publishing.md#2-enabling-the-play-store-upload-later)).
2. Fill the **Main store listing** from `text/`, `graphics/`, `screenshots/`.
3. Complete **App content**: privacy policy, App access, Ads, Content rating, Target audience, Data safety,
   Package visibility declaration.
4. Release to **internal testing**, run the checklist in [`testing-instructions.md`](testing-instructions.md).
5. Promote to production with the manual **Publish to Play Store** workflow.
