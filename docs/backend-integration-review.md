# Backend Integration Review

**Branch reviewed:** `backend-integration` at `f0345e6`

**Reviewed:** 22 July 2026

**Scope:** Supabase integration, local persistence, repository bindings, background downloads, and the contracts consumed by the merged UI.

## Current conclusion

The branch has moved well beyond backend scaffolding. It now contains a Room database, DataStore-backed settings and premium caching, a real paged search implementation, a Room-backed chat cache, Supabase schema/mappers, and WorkManager download infrastructure.

It is **not ready to merge or hand off as a working app yet**. The project currently fails Kotlin compilation due to duplicate imports in `RepositoryModule.kt`. After that blocker is fixed, several requirement-critical features remain either bound to placeholder repositories or only partially implemented.

## Validation result

`./gradlew testDebugUnitTest` was run on this branch.

Result: **failed during `:app:compileDebugKotlin`**.

Immediate blocker:

- `app/src/main/java/com/androidprj/fuzic/di/RepositoryModule.kt` imports `DownloadRepository`, `PremiumRepository`, `SettingsRepository`, and `SearchRepository` twice. Kotlin reports conflicting imports, so neither unit tests nor an APK can be built.

Also clean up the duplicate `android.permission.INTERNET` declaration in `AndroidManifest.xml`; it is currently a manifest warning, not the build failure.

## Completed integration work

| Area | Status | Evidence |
|---|---|---|
| UI merge | Done | The `ui` history, type-safe Navigation Compose 2 routes, and UI contracts are present in this branch. |
| Room foundation | Done | `AppDatabase`, DAOs, converters, and entities exist for chat, downloads, playlists, likes, recent plays, and search history. |
| Settings persistence | Done | `LocalSettingsRepository` persists theme and language through DataStore and is bound by Hilt. |
| Premium local cache | Done | `LocalPremiumRepository` provides a DataStore-backed premium state and Hilt binding. |
| Search | Done | `RemoteSearchRepository` uses `Pager`/`SupabaseSearchPagingSource`; search history is stored in Room. |
| Chat history cache | Partial | `RemoteChatRepository` syncs the latest messages into Room and exposes Room Paging 3 data. |
| Chat optimistic send/read state | Partial | Messages are inserted locally before network submission, and read updates target Room and Supabase. |
| Recent conversations | Done | A Supabase migration creates the `recent_conversations` view and the repository maps it to UI conversations. |
| Schema and mappers | Done | Artists, albums, message fields, playlist/song fields, and mapping extensions were added. |
| Download scheduling | Partial | A Hilt-enabled `DownloadWorker` is scheduled through `LocalDownloadRepository`. |

## Remaining work, in priority order

### P0 — restore a buildable branch

1. Remove the duplicate imports in `RepositoryModule.kt`.
2. Run `./gradlew testDebugUnitTest` again and fix every compile/test failure before merging.
3. Remove the duplicated `INTERNET` manifest permission while touching the integration.

### P0 — replace remaining production placeholders

The following final-named classes in `data/repository/ProductionRepositoryImplementations.kt` are still deliberately unavailable stubs and are currently bound in Hilt:

| Required capability | Current binding | Required replacement |
|---|---|---|
| Playback | `PlayerRepositoryImpl` | A real Media3-based player controller/service. It must provide streaming, downloaded-file playback, queue/progress state, sleep timer, MediaSession/notification/lock-screen controls, and real visualizer frames. |
| Playlist detail | `PlaylistDetailsRepositoryImpl` | Fetch playlist metadata and songs from Supabase; support play-all and add/remove flow requirements. |
| Artist detail/list | `ArtistRepositoryImpl` | Fetch artist details, songs, and the paged artist collection. |
| Notifications | `NotificationRepositoryImpl` | Add the Supabase schema/query implementation and read-state updates, or remove the feature from the product scope with team approval. |

Do not merge while these capabilities are expected to work but remain bound to `unavailable()`/empty data implementations.

### P0 — make downloads real

The current Worker is infrastructure only. `DownloadWorker` creates a file containing `stub_audio_data`, reports a hard-coded `3.2 MB`, and does not download the supplied `audioUrl`.

Required completion:

1. Download the actual audio from approved Supabase Storage URLs into app-private storage.
2. Record real file path, byte size, progress, completion, and failure in Room.
3. Delete the physical file before/with the Room entry; `deleteDownload`, `restoreDownload`, and `removeDownloadFile` are currently placeholders.
4. Make `PlayerController` choose the local path for a completed download and the stream URL otherwise.
5. Keep the existing premium UI gate, but ensure the repository/worker also rejects non-premium requests defensively.

Without these changes, offline playback and delete/undo acceptance criteria are not met.

### P0 — finish realtime chat and typing

The cache/Paging integration is a good base, but the realtime requirement is incomplete:

- `observeMessageInserts()` is defined but never collected/subscribed, so incoming live messages do not update Room.
- `observeTypingStatus()` always emits `null`.
- `setTyping()` returns success without writing any remote typing state.
- History sync only fetches the latest 50 messages; paging backward from the server still needs a defined remote-mediator or paging-source strategy if conversations can exceed the cache.
- Delivery/read timestamps and status transitions need to match the final schema and be verified between two users.

Complete the Supabase Realtime subscription lifecycle, write throttled typing updates (maximum once every two seconds), expire stale typing state, and test two signed-in users plus offline restart.

### P1 — complete server-backed premium state

`LocalPremiumRepository` correctly caches a value, but it is not a server-backed premium/purchase implementation. Decide and implement the authoritative source:

- Supabase user/profile premium flag and refresh path; and
- the purchase/upgrade contract, if real payments are in scope.

The UI should continue to read the cached state, but a profile refresh must update it as required.

### P1 — verify and complete existing remote repositories

Before release, exercise real Supabase data for these implementations and confirm RLS policies match the app flows:

- auth and password recovery;
- profile/user updates;
- home/music queries;
- playlists and playlist mutation;
- likes/recent plays;
- follow/unfollow and paged followers/following;
- paged search across songs, artists, playlists, and users.

In particular, bind the paged playlist/download/follow contracts wherever the UI requests them and test error, empty, and pagination boundaries against real data.

## Recommended handoff order

1. Fix `RepositoryModule.kt` and regain a passing build.
2. Replace the playback stub with the Media3 player service/controller; this also owns PCM/FFT visualizer work.
3. Replace the fake download worker with real file download/deletion and local playback selection.
4. Complete realtime chat subscriptions and typing.
5. Implement playlist detail, artist, and notifications repositories.
6. Run device tests with real Supabase accounts in English/Persian, online/offline, free/premium, and with two chat users.

## Merge status

The UI integration is present on `backend-integration`, but `master` is still at `b3cf29c`. Do **not** merge this branch into `master` until the P0 build, playback, download, and chat items above have been completed and the full test suite passes.
