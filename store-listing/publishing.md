# Releasing & publishing to Google Play

> Part of the [`store-listing`](README.md) folder. The listing texts, graphics, data-safety answers and
> declarations for Play Console are next to this file.

Two workflows handle distribution:

| Workflow | Trigger | What it does |
|---|---|---|
| **Build & Release** (`.github/workflows/release.yml`) | Every push/merge to `main` | Bumps the patch version, builds the signed R8 APK + App Bundle, checks the release manifest has no `INTERNET` permission, creates GitHub release `vX.Y.Z` with `Perfsentient_vX.Y.Z.apk` / `.aab`, keeps the R8 `mapping.txt` as a build artifact and (**once enabled**) uploads the bundle to the Play **internal** track. |
| **Publish to Play Store** (`.github/workflows/publish-play-store.yml`) | Manual (`workflow_dispatch`) | Rebuilds the bundle from `main` and uploads it to `internal`, `alpha`, `beta` or `production`. |

> **The Play Store upload is currently disabled.** Both workflows only upload when the repository
> **variable** `PLAY_STORE_UPLOAD_ENABLED` is `true`. Until then `release.yml` skips the upload
> (and says so in the job summary), and `publish-play-store.yml` fails at its first step.

## Versioning

`versionName` in `app/build.gradle.kts` is bumped by `release.yml` (commit
`chore: bump version to X.Y.Z [skip ci]` pushed back to `main`). The `versionCode` is derived from it:
`major*10000000 + minor*100000 + patch*100 + build`, e.g. `1.0.3` → `10000300`. `build` (0-99,
`-PVERSION_CODE_BUILD`) is only used to re-upload the same version to Play, which rejects a
repeated `versionCode`.

> If `main` is protected, allow `github-actions[bot]` to push (or use a ruleset bypass) so the
> version-bump commit can land.

## 1. Release signing (required for GitHub releases)

1. Create an upload keystore once and keep it safe (losing it means you can't update the app):
   ```bash
   keytool -genkeypair -v -keystore perfsentience-upload.jks -alias upload \
     -keyalg RSA -keysize 4096 -validity 10000
   ```
2. Add these **repository secrets** (*Settings → Secrets and variables → Actions → Secrets*):

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | `base64 -i perfsentience-upload.jks \| tr -d '\n'` |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias (e.g. `upload`) |
| `KEY_PASSWORD` | Key password |

`release.yml` fails at its first step (before bumping the version) if any of these is missing.

Local signed builds: create an untracked `keystore.properties` in the repo root
(`storeFile`, `storePassword`, `keyAlias`, `keyPassword`) or export `KEYSTORE_FILE`,
`KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Without either, release builds are unsigned.

## 2. Enabling the Play Store upload (later)

### Prerequisites
- A Google Play Developer account with the app `com.neteinstein.perfsentience` created.
- The **first upload must be manual**: download `Perfsentient_vX.Y.Z.aab` from a GitHub release and upload it in Play Console (internal testing is enough). Enrol in **Play App Signing** – the keystore above becomes the *upload* key.
- Store listing, content rating, data safety ("no data collected / shared", everything on-device) and a privacy policy URL completed.
- **Policy declarations** (Policy and programs → App content):
  - *Sensitive app permissions → Package visibility (`QUERY_ALL_PACKAGES`)*: core use case "auditing installed apps for misbehaviour (security / antivirus-like)".
  - Usage access (`PACKAGE_USAGE_STATS`) and notification access are special accesses granted by the user; describe them in the listing and the in-app rationale (already shown in the Settings tab).

### Service account
1. In [Google Cloud Console](https://console.cloud.google.com/) → *IAM & Admin → Service Accounts*, create e.g. `play-store-publisher` (no project roles) and download a **JSON key**.
2. In Play Console → *Users and permissions → Invite new users*, paste the service-account email, *App permissions → Add app* → `com.neteinstein.perfsentience`, grant **Release to production, exclude devices, and use Play App Signing** and **Manage store presence**, then *Invite user*.
3. Add the repository secret `PLAY_STORE_SERVICE_ACCOUNT_JSON` with the full JSON key contents.

### Switch it on
*Settings → Secrets and variables → Actions → **Variables** → New repository variable*:
`PLAY_STORE_UPLOAD_ENABLED` = `true`. No code change needed; delete the variable (or set it to anything else) to disable again.

From then on every merge to `main` also uploads to the **internal** track. To promote to another track run **Publish to Play Store** manually with the track and `build_number` `1` (raise it on repeats – the automatic upload already used `build` 0 for that version).

## How the upload works

`.github/scripts/play_publish.py` (unit-tested by `test_play_publish.py`, which also runs in the PR pipeline) talks to the Android Publisher API directly instead of a third-party action: it uploads the bundle, sets the track and commits the edit, automatically flipping Play's `changesNotSentForReview` parameter when Play requires/refuses it, falling back to a draft release while the app is still a draft, and printing what to fix in Play Console for errors code can't fix.

## Troubleshooting

- **"Repository secret … is not set"** – add the signing secrets from step 1.
- **`git push to main failed`** – branch protection blocks the bot's version-bump commit; allow `github-actions[bot]`.
- **403 / "does not have permission"** – the service account lacks app permissions in Play Console.
- **"versionCode … has already been used"** – run *Publish to Play Store* with a higher `build_number`, or merge another change.
- **"sensitive permissions that have not been declared"** – submit the `QUERY_ALL_PACKAGES` declaration in Play Console, then re-run the manual workflow.
- **"Release manifest declares android.permission.INTERNET"** – a dependency added network access; remove it or keep the `tools:node="remove"` entry in `app/src/main/AndroidManifest.xml`.
- **Play upload failed but the GitHub release exists** – re-run the job, or run *Publish to Play Store* for `internal` with `build_number` `1`.

## Security notes
- Never commit the keystore, `keystore.properties` or the service-account JSON (all are git-ignored).
- Secrets are decoded to files only for the build and removed in an `always()` cleanup step.
- All third-party actions are pinned to commit SHAs.
