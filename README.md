# StudeCo Android – Final (Ready to deploy)

**Package ID:** `com.celmatech.studeco`  
**Version:** 2.0.0  

Native Kotlin + Jetpack Compose app synced with the same Supabase project as the web platform.

---

## Feature coverage (all phases)

| Area | Status |
|------|--------|
| Auth (login / signup / reset / email gate) | ✅ |
| Campus Feed (create, like, comments) | ✅ |
| Communities / Groups (list, create, join/leave) | ✅ |
| Messaging (conversations + chat) | ✅ |
| Marketplace (list + create) | ✅ |
| Opportunities | ✅ |
| Resources | ✅ |
| Planner (tasks create + done) | ✅ |
| Profile + bio edit | ✅ |
| Notifications inbox | ✅ |
| Friends (list + accept/decline) | ✅ |
| Admin feature flags (live) | ✅ |
| Security hardening | ✅ |
| GitHub Actions → **signed APK + AAB** | ✅ |

AI Tutor remains flag-gated (shell when enabled). Google Sign-In and avatar upload can be added later with your OAuth / Storage setup.

---

## Local build

1. Open the project in **Android Studio** (Hedgehog or newer).
2. Let Gradle sync (wrapper jar is generated automatically if missing).
3. Run on emulator or device.

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/
```

---

## GitHub Actions – signed APK & AAB

Workflow file: `.github/workflows/android-ci.yml`

### 1. Create a release keystore (once)

```bash
keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias studeco
```

### 2. Add GitHub repository secrets

| Secret name | Value |
|-------------|--------|
| `RELEASE_KEYSTORE_BASE64` | `base64 -w0 release.jks` (Linux) or `base64 -i release.jks` (macOS) |
| `RELEASE_KEYSTORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | `studeco` (or your alias) |
| `RELEASE_KEY_PASSWORD` | Key password |

### 3. Push or create a tag

- Every push to `main` / `master` / `develop` → builds **debug APK** + **release APK** (signed if secrets exist) + **AAB**
- Tag `v1.0.0` (or any `v*`) → also creates a **GitHub Release** with the artifacts attached

Artifacts available to download from the Actions run:
- `studeco-debug-apk`
- `studeco-release-apk`  ← installable signed APK
- `studeco-release-aab`  ← for Play Store

---

## Supabase

Uses the same project configured in the web app (`js/config.js`).  
No service_role key is embedded. All access goes through the anon key + RLS.

---

## Support

helpstudeco@gmail.com · +265 896 144 664
