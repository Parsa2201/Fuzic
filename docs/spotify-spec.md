# Spotify-Like Music Streaming App — Project SPEC

**Author:** Sina (responsible for SPEC.md maintenance, **Media Playback chapter only** — chapter ownership is set per section)
**Date:** 19 Tir 1405 (10 Jul 2026)
**Course:** Mobile Device Programming, Amirkabir University of Technology
**Prof:** Dr. Masoumeh Taromirad
**Deadline:** 29 Tir 1405 (20 Jul 2026)
**Disclaimer:** This document is the legacy feature spec. The current source of truth for responsibilities is the Eraser diagrams (file: "Android Project", team: Parsa's Team 2) — see AGENTS.md → "Service-lane split (post Phase 1)". Spec sections still describe the feature surface; ownership lives in the diagrams.

---

## 1. Project Overview

A Spotify-like music streaming Android app with social network features. The app allows users to stream music, manage playlists, search for content, download songs for offline listening, and communicate with friends through real-time chat.

The full feature set is defined in the course spec PDF. This document restates that spec in a structured, agent-friendly format suitable for consumption by vibe-coding AI tools (opencode, etc.) and as a single source of truth for the team.

---

## 2. Tech Stack (locked)

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI toolkit | Jetpack Compose |
| Min SDK | TBD (likely 26 / Android 8.0) |
| Target SDK | Latest stable (34 or 35) |
| Architecture | MVVM with Unidirectional Data Flow (UDF) |
| State | `StateFlow<UiState>` in ViewModel, collected in Compose with `collectAsStateWithLifecycle()` |
| One-time events | `SharedFlow` or `Channel` (e.g. snackbar errors) |
| DI | Hilt |
| Backend | Supabase (self-hosted: Postgres + Auth + Storage + Realtime + Edge Functions) |
| Local DB | Room (offline cache for chat history, search history, downloads metadata, favorites) |
| Preferences | DataStore Preferences (language, theme, premium status) |
| Media playback | ExoPlayer (Media3) |
| Background work | WorkManager (downloads) |
| Realtime | Supabase Realtime (Postgres changes via websocket) |
| Networking | OkHttp / Ktor-client — TBD |
| Image loading | Coil |
| Paging | Paging 3 (required by spec for all long lists) |
| Navigation | Compose Navigation library |

**Rules:**
- All I/O runs on `Dispatchers.IO`, never on Main thread.
- No raw strings in code — use `strings.xml`.
- No hardcoded colors, typography, or padding values — use shared `MaterialTheme` tokens.

---

## 3. Shared Design System

The app uses a custom `MaterialTheme` with `Colors`, `Typography`, and `Shapes`. All UI in the app must consume these tokens — no hardcoded values.

**Required theme configurations:**
- Light theme + Dark theme + System default
- Color palette optimized for both themes (readability preserved)
- Typography scale (display / headline / title / body / label)
- Shape scale (small / medium / large components)
- Spacing scale (4dp / 8dp / 16dp / 24dp / 32dp) — [TBD by team, exact values to confirm]

**Custom app icon (adaptive):**
- The app must have a custom adaptive icon. Default Android icons are not accepted.
- [TBD by team — icon design]

---

## 4. Bilingual & Localization

- App supports Persian (fa, RTL) and English (en, LTR). Layout must flip automatically on language change.
- All strings live in `res/values/strings.xml` (English, default) and `res/values-fa/strings.xml` (Persian).
- Layouts use `start`/`end` padding (not `left`/`right`) for RTL compatibility.
- The app must be fully usable in both languages — no untranslated strings allowed.

**Language change behavior:**
- Switches the app's language live (no restart needed).
- [TBD by team — implementation: `AppCompatDelegate.setApplicationLocales()` for Android 13+, manual `Configuration` override for older]

---

## 5. Architecture & Code Standards

### 5.1 Clean Architecture layering
- **Data layer:** repositories, network clients, Room DAOs, DataStore wrappers.
- **Domain layer:** use-cases (optional, depending on feature), business models.
- **UI layer:** Composables, ViewModels, UiState data classes.

### 5.2 MVVM with UDF
- Every screen has a ViewModel exposing a `StateFlow<UiState>`.
- UiState is a single data class that holds all screen state (loading, content, error).
- UI dispatches `Intent`s (or `Event`s) to ViewModel via a function like `onIntent(intent: Intent)`.
- One-time events (snackbar messages, navigation) use `SharedFlow` or `Channel`.

### 5.3 Dependency injection
- Use Hilt.
- Repositories, use-cases, databases, network clients must be injected (not constructed in-place).

### 5.4 Paging 3
- All long lists (search results, playlist songs, chat history) must use Paging 3.
- Loading all data into a `LazyColumn` at once is not accepted.

### 5.5 Threading
- No network or DB calls on the Main thread.
- Use `Dispatchers.IO` explicitly or `withContext(Dispatchers.IO)` inside repository methods.
- Use coroutines for async work.

### 5.6 Strings & resources
- No hardcoded user-facing strings. Use `strings.xml`.
- No hardcoded colors, dimensions, or typography in Composables. Use theme tokens.

---

## 6. Top-Level App Structure

The app is organized around a **bottom navigation bar with 5 tabs**:

1. **Home** — featured content, quick actions, recommendation carousels.
2. **Search** — search for songs, artists, playlists, users.
3. **Downloads** — offline-downloaded songs.
4. **Playlists** — user playlists + global/local playlists.
5. **Profile** — current user's profile, follow lists, settings, premium status.

A persistent **mini player** sits above the bottom nav bar when music is playing. Tapping it expands to the full player with a shared element transition (cover art smoothly grows into the full-player screen).

The top app bar appears on all main screens:
- **Left:** profile avatar, notification bell, settings gear.
- **Right:** app logo + name.

---

## PDF Source Addendum — Mandatory Requirements Not To Miss

This section is transcribed from the original course requirements PDF, `Android Project.pdf`, reviewed on 22 July 2026. It exists to make the source-PDF requirements visible during implementation. If another part of this markdown accidentally omits one of these items, this addendum still applies.

### App-wide UI motion and loading

- The app must use fluid, polished motion throughout: smooth screen transitions and visible scale feedback when buttons are pressed.
- Every loading list/card area must use animated shimmer skeletons. Static tinted placeholders and spinners are not substitutes for the required list/card loading treatment.

### Additional media-playback requirements

- Handle Android audio focus: pause or duck for calls/other audio, then resume appropriately.
- Use ExoPlayer `CacheDataSource` (or an equivalent Media3 cache) so streamed audio is cached for replay and seeking.
- Crossfade between consecutive tracks.
- Display the now-playing cover as a CD-style disc that rotates while playing and stops while paused.
- Derive a dynamic full-player background gradient from the dominant cover-art color, using Palette API or an equivalent image-palette approach.

### Settings and premium requirements

- Persist a user-selected font-size preference in DataStore alongside language, theme, and premium state.
- Provide a demonstrative premium purchase/renewal flow that makes the user premium after successful completion; premium users can download, while free users receive an upgrade prompt.

### Social and library sub-pages

- Users must be able to view and play public playlists belonging to followed users.
- Liked Songs and Recently Played need more than a plain list: provide an engaging header, Play All/Shuffle action, and quick item removal such as swipe-to-dismiss.

---

## 7. Feature: Home Tab

### 7.1 Goal
The Home tab is the app's showcase. It surfaces recommendations, recently played, top picks, and quick navigation to library sections.

### 7.2 Sections
- **Daily picks carousel (HeroBanner / HorizontalPager):** horizontally swipeable, large images, shows new albums, daily picks, trending songs.
- **Quick actions (4 buttons):**
  - Liked songs
  - Recently played
  - My playlists
  - Top artists
- **Horizontal carousels (LazyRow):**
  - Most popular
  - New releases
  - Global playlists
  - Local playlists (Iranian / Persian content)

### 7.3 Data
- All sections pull from the backend (Supabase).
- [TBD by team — what table(s) hold these recommendations]

### 7.4 Technical approach
- Use `HorizontalPager` for the daily-picks carousel with auto-scroll.
- Use `LazyRow` for the recommendation carousels.
- Use `LazyColumn` (vertical) to stack the sections, with each section being a `LazyRow`.
- Apply **shimmer effect** while data is loading (skeleton placeholders, not spinners).
- Tapping a card in any carousel navigates to the relevant detail screen (song, playlist, artist).

### 7.5 Edge cases
- No data yet → empty state with "Discover new music" prompt.
- Network error → snackbar + retry button.
- Offline mode → show last cached Home snapshot.

### 7.6 Acceptance criteria
- All sections load within 2s on a normal network.
- Carousels scroll smoothly (60fps target).
- Shimmer effect appears on first load, not on subsequent loads from cache.

---

## 8. Feature: Search Tab

### 8.1 Goal
Allow users to search across songs, artists, playlists, and users, with debounced live search and a saved search history.

### 8.2 User flow
1. User opens the Search tab.
2. User types a query → results appear after a short debounce (e.g. 300ms).
3. User can apply filter chips: Songs / Artists / Playlists / Users.
4. User can tap a result to navigate to its detail screen.
5. Search history is shown when the field is empty; user can delete individual entries or clear all.

### 8.3 Data
- Backend exposes search across multiple tables (Postgres full-text / RPC functions).
- [TBD by team — exact search endpoint, full-text vs prefix match]
- Search history is stored in Room (local only).

### 8.4 Technical approach
- Use Flow + `debounce(300ms)` + `distinctUntilChanged()` for the input pipeline.
- Filter chips update the result query in the ViewModel.
- Search history: write on every successful search; expose as a separate `Flow<List<String>>` from the ViewModel.
- Paging 3 for result lists.

### 8.5 Edge cases
- No results → empty state with "Try a different query" message.
- Network error → snackbar.
- Very long queries → truncate input to a max length.

### 8.6 Acceptance criteria
- Search input does not hit the backend on every keystroke.
- Filter chips correctly narrow the results.
- History persists across app restarts.

---

## 9. Feature: Downloads Tab

### 9.1 Goal
Show all songs the user has downloaded for offline listening. Allow sorting and swiping to delete.

### 9.2 User flow
1. User opens the Downloads tab.
2. User sees a list of downloaded songs (cover, title, artist, file size).
3. User can sort by date downloaded, title, or artist.
4. User can swipe a row to delete (with undo snackbar).
5. Tapping a song plays it from local file (not network).

### 9.3 Data
- Metadata for downloads is stored in Room.
- Actual audio files are stored in app-private storage.

### 9.4 Technical approach
- `LazyColumn` with `SwipeToDismiss` (or equivalent) for delete.
- `WorkManager` handles the actual download (runs in background, survives process death).
- Sort options are local-only (in-memory sort on the loaded list, or a Room query with ORDER BY).

### 9.5 Edge cases
- No downloads → empty state with "Download songs for offline listening" prompt.
- Download in progress → show progress indicator on the row.
- Storage full → snackbar with "Free up space" CTA.

### 9.6 Acceptance criteria
- Downloads survive app restart and device reboot.
- Deleting a download removes both the file and the Room entry.
- Playing a downloaded song does not consume network data.

---

## 10. Feature: Playlists Tab

### 10.1 Goal
Show all playlists the user has access to: global playlists, local (Iranian) playlists, and the user's own playlists.

### 10.2 User flow
1. User opens the Playlists tab.
2. User sees a 2-column grid of playlist cards.
3. Sections: Global playlists, Local playlists, My playlists.
4. Tapping a playlist opens its detail screen (list of songs + play all).
5. Tapping "+ New Playlist" creates a new playlist (name, optional cover image).

### 10.3 Data
- Playlists live in Supabase (Postgres `playlists` table).
- Each playlist has songs (ordered list of song IDs).

### 10.4 Technical approach
- `LazyVerticalGrid(columns = 2)` for the cards.
- Cards have colored backgrounds (gradient based on playlist ID hash for visual variety).
- Create-playlist flow is a separate Composable with a simple form (name input + cover picker).
- Paging 3 for long playlist lists.

### 10.5 Edge cases
- No playlists → empty state with "Create your first playlist" CTA.
- Creating a playlist with an existing name → confirm dialog.
- Empty playlist (no songs) → empty state inside the playlist detail screen.

### 10.6 Acceptance criteria
- Grid scrolls smoothly.
- Newly created playlist appears in "My playlists" within 1s.

---

## 11. Feature: Profile Tab

### 11.1 Goal
Show the current user's profile, follow stats, premium status, and provide access to settings, chat, follow lists, and other sub-pages.

### 11.2 Sections
- **Avatar + username + edit button.**
- **Premium status badge** (gold badge if user is premium).
- **Stats row:** Followers count, Following count, Playlist count.
- **Tap to view:** Followers list, Following list, Liked songs, Recently played.
- **Entry points:** Settings, Chat list, Logout.

### 11.3 Data
- User profile from Supabase `profiles` table (joined to `auth.users`).
- Follow counts derived from the `follows` table.
- Premium status from a `isPremium` boolean on the user.

### 11.4 Technical approach
- Standard `LazyColumn` for the profile sections.
- Each tappable row is a navigation entry.
- Stats use the shared theme typography.
- **Ownership:** Profile tab UI is reassigned out of Sina's lane (see AGENTS.md "Service-lane split"). The Follow Lists screens and the Settings screen, which Sina previously planned to own, are part of the Chat Track / later-phase deliverables and no longer belong to the playback slice.

### 11.5 Edge cases
- No avatar set → default avatar.
- Premium status changes (e.g. subscription expires) → badge updates within one screen refresh.

### 11.6 Acceptance criteria
- All profile data loads within 2s.
- Tapping any row navigates to the correct screen.

---

## 12. Feature: Media Playback

### 12.1 Goal
Provide professional music playback: mini-player, full-player with shared element transition, controls, queue management, sleep timer, playback speed, and visualizer.

### 12.2 Components

**Mini-player (persistent):**
- Sits above the bottom navigation bar.
- Shows: cover, title, artist, play/pause button, expand-to-full-player tap target.
- Clicking it expands to the full player with a shared element transition (cover smoothly grows into center of full player).

**Full-player:**
- Large cover art (centered).
- Title + artist below.
- Progress bar with seek.
- Controls: previous, play/pause, next, shuffle, repeat.
- Like button.
- Share-to-chat button (calls into the chat feature).
- Add-to-playlist button.
- Sleep timer (15 / 30 / 45 / 60 min / off).
- Playback speed control (1x / 1.5x / 2x).
- **Visualizer / equalizer (Canvas-drawn waveform).** Must be drawn with Compose Canvas, not a static image.

**Notification controls (MediaSession):**
- Standard lock-screen + notification media controls.

### 12.3 Data
- Audio URLs come from the `songs` table in Supabase (or from local file paths for downloaded songs).
- Local file paths for downloaded songs.

### 12.4 Technical approach
- ExoPlayer (Media3) wrapped in a `PlayerController` service or singleton.
- Compose Canvas for the visualizer.
- `SharedTransitionLayout` (Compose 1.7+) or `SharedElement` API for the mini→full transition.
- ExoPlayer's `setPlaybackParameters` for speed control.
- Coroutines for the sleep timer (`delay()` then `pause()`).
- Repository layer checks if a downloaded file exists; plays local file if so, otherwise streams from URL.

### 12.5 Edge cases
- Network drops mid-playback → buffer, then resume; show "Reconnecting…" on the player.
- Local file deleted while in queue → skip + show toast.
- Backgrounded → playback continues; lock-screen controls work.

### 12.6 Acceptance criteria
- Mini→full transition is smooth (60fps, no jank).
- Sleep timer pauses playback within 1s of timer expiry.
- Playback speed change is immediate.
- Visualizer renders continuously without dropping frames.

---

## 13. Feature: Business Logic — Premium Status

### 13.1 Goal
Differentiate free vs premium users. The only behavioral difference is offline downloads — free users can stream unlimited music, but cannot download.

### 13.2 Rules
- Free users: full streaming, no ads (per spec), **no download button** (or download button shows upgrade prompt on tap).
- Premium users: full streaming + offline downloads + the gold badge on profile.

### 13.3 Data
- User has `isPremium: boolean` field.
- Persistence: stored on the backend (Supabase) and cached in DataStore for offline access.

### 12.4.1 Ownership note (per diagrams)

Per the Development Dependency Graph diagram, the Playback Track (Sina) owns Media3 Controller, Playback State, Mini Player Logic, Full Player Logic, and Notification Controls. The full-player Canvas-drawn visualizer is part of Full Player Logic and is therefore Sina's.

The PlayerController service exposes a public Playback API that:

- Streams from URL if no local file exists for the songId.
- Plays local file (under app-private storage) when one exists.
- Exposes playback state (current song, queue, position, duration, playing/paused, shuffle, repeat, speed, sleep timer).
- Exposes controls (play, pause, seekTo, next, prev, skipToIndex, setShuffle, setRepeat, setSpeed, setQueue).
- Exposes a media-session integration for lock-screen / notification controls.

Callers (Home, Search, Downloads, Playlists, the later-phase Chat SongShareCard) only depend on that public Playback API — never on Media3 internals.

### 13.4 Technical approach
- Premium check lives in a `UserService` (or `SubscriptionService`).
- Download button visibility is driven by `isPremium` from the ViewModel.
- Tapping download as a free user → snackbar or dialog: "Upgrade to Premium to download."

### 13.5 Edge cases
- User's premium expires while offline → on next launch, downloads remain but new download attempts are blocked.
- Free user tries to share a song in chat → allowed (sharing is not a premium feature).

### 13.6 Acceptance criteria
- Free user cannot trigger a download.
- Premium badge appears/disappears within one refresh of profile data.

---

## 14. Feature: Real-Time Chat (Direct Messages)

**Lane:** Chat Track (Development Dependency Graph → pink group). Out of Sina's lane for this project. Awaiting reassignment after Phase 1. The shared-media playback path in §14.4 ("Share-song-as-message") calls the public PlayerController API (§12.4.1); chat never touches Media3 internals.

### 14.1 Goal
Allow users to send and receive direct messages in real time, with read receipts, typing indicators, and the ability to share songs inline.

### 14.2 User flow
1. User opens the chat list screen → sees all conversations, sorted by most recent.
2. User taps a conversation → opens chat detail screen.
3. Chat detail shows message history (newest at bottom), text input at the bottom.
4. User types a message → message is sent to the recipient in real time.
5. As the recipient types, the sender sees a "typing…" indicator.
6. As messages are sent and read, the sender sees status icons (sending clock → sent single check → read double check).
7. User can tap a share-song button inside chat → opens a song picker → selected song appears as a special message with a mini-card UI → tapping the card triggers playback via the shared player.
8. If the user has no internet, the app should still show the last cached chat history.

### 14.3 Data model (Supabase / Postgres) — exact table names TBD

```
Collection: messages
  - id: string (auto)
  - roomId: string (conversation id, e.g. sorted-uid-pair)
  - senderId: string (user id)
  - text: string (nullable, empty for share-song messages)
  - type: enum("text", "song_share")
  - songId: string (nullable, present when type = "song_share")
  - status: enum("sending", "sent", "delivered", "read")
  - createdAt: datetime
  - readAt: datetime (nullable)
  - deliveredAt: datetime (nullable)

Collection: typing
  - roomId: string (id)
  - userId: string
  - updatedAt: datetime (TTL: 5s — client considers stale if older)

Collection: conversations (or derived from messages)
  - id: string (= roomId)
  - participantIds: array<string>
  - lastMessageAt: datetime
  - lastMessagePreview: string
```

**Room offline cache (Room):**
```
@Entity Messages
  - id (PK)
  - roomId (indexed)
  - senderId
  - text
  - type
  - songId (nullable)
  - status
  - createdAt
  - readAt (nullable)
  - deliveredAt (nullable)
  - cachedAt: long (when we stored locally)
```

### 14.4 Technical approach
- **Realtime transport:** Supabase Realtime over websocket. Subscriptions are created against the `messages`, `conversations`, and `typing` tables (with the realtime publication enabled per table). Verify with Bagher that the required tables are added to the `supabase_realtime` publication.
- **ViewModel pattern:**
  - `ChatListViewModel` exposes `StateFlow<ChatListUiState>`.
  - `ChatDetailViewModel` exposes `StateFlow<ChatDetailUiState>`.
  - Both send `Intent`s to their VMs; VMs call `ChatRepository`.
- **ChatRepository** is the single source of truth:
  - Subscribes to `messages` table filtered by `roomId` via Supabase Realtime.
  - On every new message → write to Room + update `StateFlow`.
  - On send → optimistic local insert with `status = sending` → Supabase insert → on success update to `sent`.
- **Read receipts:** When chat detail screen is open and messages are visible, mark unread incoming messages as `read` via a batched update. Optimistic local update first.
- **Typing indicator:** Throttled — write to `typing` table on every keystroke, max once every 2s. Read other side via Supabase Realtime subscription on the same row. TTL of 5s.
- **Share-song-as-message:**
  - User taps share → opens song picker (uses PlayerService's catalog).
  - User picks a song → send a message with `type = "song_share"`, `songId = ...`, `text = null`.
  - Receiver renders a custom `SongShareCard` Composable (cover, title, artist, play button).
  - Tapping the play button calls the shared player service.

### 14.5 Edge cases
- Network loss mid-send → message stays in `sending` status; on reconnect, retry.
- Recipient is offline → message stored; status flips when recipient comes online.
- User opens the same chat on two devices → last-write-wins on `read` status.
- Empty conversation → empty state with prompt to send the first message.
- Long messages → text wraps.
- RTL/LTR mixing → message bubble alignment follows the sender's locale.

### 14.6 Acceptance criteria
- Two users on different devices can exchange messages with <2s perceived latency.
- Status icons (sending/sent/read) update correctly across both devices.
- Typing indicator appears within 1s of the other user starting to type and disappears within 5s of stopping.
- A song shared in chat appears as a tappable mini-card that triggers playback.
- Killing the app and reopening it shows the cached chat history offline.

---

## 15. Feature: Follow System

**Lane:** Chat Track (later phase, paired with the Chat Track per Development Dependency Graph). Was previously assigned to Sina; the service-lane split reassigned this out of the Playback Track.

### 15.1 Goal
Let users search for other users, follow/unfollow them, and view lists of who they follow and who follows them.

### 15.2 User flow
1. User opens the Follow screen from the Profile tab.
2. User types a query in the search field → debounced search returns matching users.
3. User taps "Follow" on a user → the user appears in the user's following list.
4. User opens the Following list → sees all users they follow → can unfollow with a single tap.
5. User opens the Followers list → sees all users following them.
6. Unfollowing a user removes them from the Following list.

### 15.3 Data model (Supabase / Postgres) — exact table names TBD

```
Collection: follows
  - id: string (auto)
  - followerId: string (user who initiated the follow)
  - followeeId: string (user being followed)
  - createdAt: datetime

  Indexes:
  - (followerId, followeeId) unique
  - followeeId
  - followerId
```

### 15.4 Technical approach
- `FollowRepository` exposes:
  - `searchUsers(query: String): Flow<List<User>>` — debounced (300ms).
  - `follow(userId)`, `unfollow(userId)`.
  - `following(userId): Flow<List<User>>`, `followers(userId): Flow<List<User>>`.
- ViewModels: `FollowSearchViewModel`, `FollowingListViewModel`, `FollowersListViewModel`.
- Search uses debounce + `distinctUntilChanged`.
- Lists use Paging 3.

### 15.5 Edge cases
- Self-follow → blocked.
- Following count is 0 → empty state with prompt to discover users.
- Search returns 0 results → empty state.
- Network loss during follow → optimistic update, retry on reconnect, rollback on persistent failure.

### 15.6 Acceptance criteria
- User can search and find another user in <1s on a typical network.
- Follow/unfollow reflects in both users' lists within 2s.
- Following and Followers lists scroll smoothly with Paging 3.

---

## 16. Feature: Settings

**Lane:** UI Track / Screen Integration Track (per Development Dependency Graph). Was previously planned under Sina's slice; the service-lane split reassigned this out of the Playback Track.

### 16.1 Goal
Allow the user to change app language, theme, and log out.

### 16.2 User flow
1. User opens the Settings screen from the Profile tab or top app bar gear icon.
2. Language toggle: switches the app's language live. Layout reflows RTL↔LTR.
3. Theme toggle: Light / Dark / System. App theme updates immediately.
4. Logout button: confirms with dialog, then signs out and navigates to the auth flow.

### 16.3 Data (DataStore Preferences)
```
- language: enum("system", "en", "fa")
- theme: enum("system", "light", "dark")
```

### 16.4 Technical approach
- `SettingsRepository` wraps DataStore.
- `SettingsViewModel` exposes `StateFlow<SettingsUiState>`.
- Theme is applied via the shared `ThemeManager`.
- Logout: calls the shared auth service, then navigates to the auth flow.

### 16.5 Edge cases
- User changes language mid-conversation in chat → chat rebuilds with the new locale without losing scroll position.
- User changes theme → no flash of unstyled content.
- Logout clears local chat cache — [TBD by team policy: clear vs keep for next login]

### 16.6 Acceptance criteria
- Language change is instant, no app restart needed, RTL flips correctly.
- Theme change is instant.
- Logout signs out and returns to auth screen.

---

## 17. Feature: Local Data (Room)

Expected Room tables. Final ownership per lane is recorded in AGENTS.md → "Local Data". This spec section keeps the schema shape only.

| Table | Used for | Lane |
|---|---|---|
| `messages` | Offline chat history cache | Chat Track (later phase) |
| `search_history` | Saved search queries (music + user search) | UI / Search Track |
| `downloads` | Downloaded song metadata + file path | Playback Track (Sina) |
| `liked_songs` | Liked songs | Playback Track (Sina) |
| `recently_played` | Recently played songs | Playback Track (Sina) |
| `playlists` | Local cache of playlists | Backend Track (Bagher) |
| `chat_search_history` | Saved user search queries (follow system) | Chat Track (later phase) |

**Schema decisions are made per-lane in PRs.** Sina drafts tentative schemas only for the Playback Track tables (`downloads`, `liked_songs`, `recently_played`) since those are his lane.

---

## 18. Feature: Sub-Pages, Creativity, and Developer Independence

The course spec states: "The exact structure of key pages, system architecture, and critical flows is described in detail. However, not every detail of every micro-interaction and sub-page is dictated to the developer. You are expected to design and implement other sub-pages and supplementary parts of the application based on your understanding of user experience (UX) while maintaining the integrity of the app's design system."

**Sub-pages expected to exist (some details left to the developer):**
- Liked songs page
- Recently played page
- Artists list / followed users list
- Settings page
- Empty states (playlist empty, no search results, no internet)
- Error states
- Loading states (shimmer placeholders)
- Auth flow (sign in / sign up) — [TBD by team — owner]
- Onboarding (if any) — [TBD by team]

**Design rules for sub-pages:**
- Maintain visual consistency with the shared design system.
- Reuse existing Composables and theme tokens.
- Empty states should have illustrations or friendly messages.
- Every screen must handle: loading, content, empty, error.

---

## 19. Feature: Navigation

Top-level navigation: bottom navigation with 5 tabs (Home, Search, Downloads, Playlists, Profile).

Sub-navigation: standard back-stack within each tab.

Cross-cutting screens (chat, settings, follow lists, full-player) are reached from multiple entry points.

**Sina's Playback Track contributes these navigation destinations:** full-player screen (reachable from mini-player shared-element transition and from any tab that opens the queue — Home, Search, Downloads, Playlists, later Chat SongShareCard).

Chat Track destinations (chat list, chat detail, follow lists, settings, song-share composable routes) are not part of Sina's Playback Track and are owned by the assignee of that lane.

---

## 20. Deliverables

Per the course spec:
- App output file (`.apk` or `.aab`) — no need to upload source code.
- A short video demonstrating the app's functionality.

---

## 21. Open Questions (carried from the original planning pass)

> Status note: the answers below are populated against the current Eraser diagrams. Anything still genuinely unresolved stays below as an Open Question.

1. **Supabase Realtime:** Are the `messages`, `conversations`, and `typing` tables added to the `supabase_realtime` publication? Who confirms with Bagher? *(still open — needed by the Chat Track owner, not by Sina)*
2. **Shared `MaterialTheme`:** Which teammate owns the design system file? *(open)*
3. **Shared services (UserService, AuthService, PlayerService, ThemeManager):** Who owns each? *(partly resolved: `PlayerService` / `PlayerController` is Sina, per §12.4.1; the other three still open)*
4. ~~**Profile tab ownership:** Sina planned to own the follow lists + settings entry, but not the rest of the Profile tab. Confirm split.~~ **Resolved:** Profile tab UI and the follow-lists / Settings entry are no longer in Sina's lane (Chat Track / later phase). See §11.4.
5. **Logout policy:** Does logging out clear offline chat cache? *(open — Chat Track territory)*
6. **Logout behavior on cached songs:** What happens to downloads on logout? *(open; the download-row lifecycle in §17 is Sina's since downloads are in his lane, but the logout decision is a product call)*
7. **Search endpoint:** Backend owner (Bagher) to confirm which Supabase tables/columns are searchable and which Postgres full-text indexes exist (or need to be added). *(open)*
8. **Song catalog minimum:** Spec requires at least 50 real songs. Who curates the list? Where do audio files live? *(open; playback consumes the URLs regardless of who curates)*
9. **Git workflow:** Branch strategy (trunk-based? feature branches?), PR review rules, daily sync time. *(open)*
10. **Code style / formatter:** ktlint? detekt? Standard Android Studio formatter? *(open)*
11. **Paging 3 in chat history v1:** Spec requires it for long lists; chat can defer to a v1.5 if time-constrained. *(open — Chat Track call)*
12. ~~**Visualizer:** Canvas-drawn waveform in the full player. Who owns this?~~ **Resolved:** Playback Track (Sina) owns it, as part of Full Player Logic (§12.4.1).

---

## 22. Build & Run

[TBD after the team meeting once the repo structure is locked. Will include: clone URL, gradle commands, Supabase local-dev setup (`supabase start`), emulator instructions.]

---

**End of SPEC.md.** Feature surface stays in this document; service-lane ownership lives in the Eraser diagrams and AGENTS.md's "Service-lane split (post Phase 1)". Update via PR when a feature spec changes.
