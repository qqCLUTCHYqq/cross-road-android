# Android proof-of-concept boundaries

## Architecture

Kotlin Activity → AndroidX WebViewAssetLoader → bundled web/WASM runtime → synchronous worker range requests → native HTTPS client → public read-only R2 Content. Saves remain in WebView IndexedDB under `https://appassets.androidplatform.net/assets/web/` and this application's private profile. Android backup is disabled. Saves are not uploaded.

The shell blocks external navigation/subresources, file/content URL access and mixed content, denies Web permission requests, and exposes no JavaScript-to-native object. Content requests accept only manifest paths, aligned offsets and the pinned version. Redirects/full-body responses are rejected. Content-Range and exact bounded byte count must match.

`remote-files.js` remains unchanged: its native read callback must immediately return bytes. **Do not convert synchronous worker XHR to async fetch without redesigning the native synchronous call contract.** Android intercepts `__content` on WebView's background request thread. The worker has its original 24-block memory LRU; Android caches at most 256 one-MiB blocks (256 MiB) in app-private cache storage, separate from saves. No PWA service worker or R2 CORS change is needed; native HTTPS requests are outside browser CORS. No account credentials are used.

The 11-file Content inventory is pinned. Each requested block verifies the remote total size via Content-Range; this is not a new cryptographic verification of the whole archive. Cache failure falls back to the valid online block. A cold launch requires network access.

## Reuse and provenance

`runtime-lock.json` records the reference commit and every input SHA-256. `scripts/prepare-runtime.mjs` downloads immutable inputs into ignored generated assets, rejecting mismatches. `--source /path/to/verified/Web` supports local preparation. No files are written to the iOS repository.

Unchanged: runtime-worker, native/WASM bridge, wasm32 artifacts/metadata, file adapters, Content inventory, native touch-dispatch fixes, save format/schema, PCM mixer/timing/refill protocol. The historical database name `crossroad-ios-v1` is retained as part of the runtime contract; Android's isolated app profile makes this Android-local storage.

Adapted: exactly two app.js imports select Android helpers. game.html retains upstream markup/styles/credits, removes Apple/PWA bootstrap, and adds Android loader/CSS. Generated bundles are not reformatted or manually maintained. Android helpers do not import Safari terminal-touch fallbacks, iOS audio-route recreation, Home Screen lifecycle code or iPhone menus.

## Platform responsibilities

- Activity/browser visibility stops refill reporting, discards pending PCM and suspends the same AudioContext. Foreground/tap resumes it. Audio epochs reject obsolete worker PCM; the scheduler is unchanged.
- Standard Pointer Events use normalized canvas coordinates, capture and one terminal up/cancel event. No duplicate Touch Events stream.
- Android handles system insets; web canvas uses contain scaling. Native toolbar offers Restart and Diagnostics only.
- DOM storage stays enabled, with no storage deletion on launch/restart. The game decides when to save; no fake Save button.
- Debug builds enable WebView inspection and logcat. They are test builds, not hardened production distribution.

## Validation limits

`scripts/test-platform.mjs` checks normalized touch/release/cancel/cleanup, Android audio lifecycle epochs and platform imports. `ContentRangesTest` checks cache reuse/eviction, partial final block, invalid paths/offsets/version and rejected full/truncated responses. CI runs unit tests/lint, builds an APK and checks native constructors/JNI inside an Android emulator WebView.

The emulator uses software graphics and muted output. Initialization does not certify physical audio, gameplay touch or an actual KHUX save round-trip. Use the README checklist before marking the full milestone complete.

References: [Android local WebView content](https://developer.android.com/develop/ui/views/layout/webapps/load-local-content), [AGP 8.13 compatibility](https://developer.android.com/build/releases/agp-8-13-0-release-notes), [AndroidX WebKit](https://developer.android.com/jetpack/androidx/releases/webkit).

Deferred: release signing/distribution, polished menus, backup UI, complete offline Content management, performance tuning and original-client/server restoration.
