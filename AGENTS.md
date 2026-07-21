# AGENTS.md

This file is the single agent-facing guide for Fuzic. It aggregates the project specification and group decisions from `docs/spotify-spec.md` and `docs/group-decisions.md`.

## Project

Fuzic is a Spotify-like Android music streaming app with social features. It supports music streaming, playlists, search, downloads for premium users, profiles, following, settings, and real-time direct messages.

Course context:

- Course: Mobile Device Programming, Amirkabir University of Technology
- Professor: Dr. Masoumeh Taromirad
- Deadline: 29 Tir 1405 (20 Jul 2026)
- Deliverables: app output file (`.apk` or `.aab`) and a short demo video

## Required Agent Behavior

- Read this file before planning or coding.
- Check Android Skills guidance before coding.
- If Android Skills conflicts with this file, stop and ask the developer which rule to follow.
- Do not resolve project policy conflicts independently.
- If required assets are missing, notify the developer and ask for them.
- Keep features loosely coupled so teammates can work in parallel.
- Follow SOLID and DRY principles.
- Use the [gitmoji.dev](https://gitmoji.dev) standard as a commit-message prefix. Each commit subject must begin with a single gitmoji emoji drawn from the official cheat sheet, followed by a space, followed by a Conventional Commits subject: `<gitmoji> <type>(<scope>): <subject>`. Examples: `✨ feat(home): add daily picks carousel`, `🐛 fix(player): prevent null cover in mini-player`, `📝 docs(backend): switch to supabase`, `♻️ refactor(chat): extract SongShareCard composable`, `🧪 test(search): cover debounce edge cases`. The gitmoji is decoration only — the Conventional Commits `type` (feat, fix, docs, style, refactor, test, chore, build, ci, perf) is still what tooling should parse. Scope and body are optional but recommended for non-trivial changes.
- After adding a feature, run relevant tests and make sure they pass.

## Required Setup

- Android Skills should be installed before AI agents code in the project.
- AI agents must have access to project documentation before making code changes.
- Eraser MCP must be available and authorized before agents update diagrams.
- AI agents must have access to the project's diagrams in Eraser via the Eraser MCP before making code changes. The project lives in **parsa's Team 2** under the file **Android Project**. Use the Eraser MCP `list_files` tool on that team to locate the file by title — search-by-title alone may miss it.
- Do not use Eraser AI credits. Create or update diagrams only through non-AI MCP APIs.

## Locked Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI toolkit | Jetpack Compose |
| Min SDK | TBD, likely 26 / Android 8.0 |
| Target SDK | Latest stable, 34 or 35 |
| Architecture | MVVM with Unidirectional Data Flow |
| UI state | `StateFlow<UiState>` collected with `collectAsStateWithLifecycle()` |
| One-time events | `SharedFlow` or `Channel` |
| Dependency injection | Hilt |
| Backend | Supabase (self-hosted: Postgres + Auth + Storage + Realtime + Edge Functions) |
| Local DB | Room |
| Preferences | DataStore Preferences |
| Media playback | ExoPlayer / Media3 |
| Background work | WorkManager |
| Realtime | Supabase Realtime (Postgres changes via websocket) |
| Networking | OkHttp or Ktor-client, TBD |
| Image loading | Coil |
| Paging | Paging 3 for all long lists |
| Navigation | Compose Navigation with type-safe routes |

Global rules:

- All I/O runs on `Dispatchers.IO`, never on Main.
- No raw user-facing strings in code. Use `strings.xml`.
- No hardcoded colors, typography, or padding values in Composables. Use shared theme tokens.
- Navigation routes must be type-safe. Do not use raw string routes.

## Package Structure

Use this structure unless the team agrees on a new folder.

```text
.
├── ui/
│   ├── navigation/
│   │   └── AppNavGraph.kt
│   ├── theme/
│   ├── screens/
│   └── components/
├── repository/
├── model/
├── domain/ (optional)
├── di/
├── data/
│   ├── settings/
│   ├── serialization/ (optional)
│   ├── local/
│   │   ├── entity/
│   │   ├── dao/
│   │   ├── database/
│   │   └── converter/ (optional)
│   └── remote/
└── util/ (optional)
```

Folder responsibilities:

- `ui/`: UI files, navigation, themes, and ViewModels.
- `ui/navigation/`: app navigation graph, including `AppNavGraph.kt`.
- `ui/theme/`: colors, typography, shapes, and app theme.
- `ui/screens/`: one folder per screen, containing composables and ViewModel.
- `ui/components/`: reusable composables used by more than one screen.
- `repository/`: repository interfaces and implementations.
- `model/`: UI-facing data classes, enum classes, data objects, and similar model types.
- `domain/` (optional): business logic and use cases between UI and data when useful.
- `di/`: Hilt modules.
- `data/settings/`: app settings storage and restoration.
- `data/serialization/` (optional): serialization helpers such as `JsonProvider`.
- `data/local/entity/`: Room entities.
- `data/local/dao/`: DAOs.
- `data/local/database/`: app database definitions.
- `data/local/converter/` (optional): SQLite converters.
- `data/remote/`: networking files.
- `util/` (optional): truly shared generic helpers.

Use `domain/` only when it removes real complexity. Good domain candidates include use cases that combine multiple repositories, enforce product rules, or contain business logic reused by multiple ViewModels. Do not create use cases that only call one repository method with no extra logic.

Avoid adding to `util/` unless the helper is generic and truly shared.

## Architecture

- Data layer: repositories, network clients, Room DAOs, DataStore wrappers.
- Domain layer: optional use cases and business models.
- UI layer: Composables, ViewModels, and UiState data classes.
- Every screen ViewModel exposes `StateFlow<UiState>`.
- UiState is a single data class holding loading, content, and error state.
- UI dispatches intents/events to ViewModel through a function such as `onIntent(intent)`.
- One-time events such as snackbars and navigation use `SharedFlow` or `Channel`.
- Repositories, use cases, databases, and network clients must be injected with Hilt.

## UI Rules

- The app uses a bottom navigation bar with five tabs: Home, Search, Downloads, Playlists, Profile.
- A persistent mini-player sits above the bottom nav when music is playing.
- Tapping the mini-player opens the full player with a shared element transition.
- The top app bar appears on main screens.
- Break large UI components into smaller subcomponents when it improves reuse, readability, or testing.
- Each screen has a top-level composable used by navigation.
- The navigation-level top-level composable does not need a preview.
- ViewModels are passed only to the navigation-level composable.
- The navigation-level composable extracts state and callbacks, then passes plain state and functions into the screen's main composable.
- Every screen must handle loading, content, empty, and error states.

## Compose Preview Rules

Every reusable or screen-level UI composable should have useful previews.

Required preview coverage:

1. Provide sample models for composables that require model input.
2. Include empty-state previews where applicable.
3. Include delayed-loading previews for shimmer effects, intended for emulator testing.
4. Do not pass empty lambdas when interaction matters. Preview callbacks should demonstrate realistic sample behavior.
5. Include previews for every meaningful component state.
6. Prefer one reusable preview composable when it can generate multiple previews cleanly.
7. Include English and Persian previews. Use English for the default preview. Use Persian for default and additional state previews.
8. Group previews by composable.
9. Make animations and subcomponent interactions visible and testable in previews.

## Design System And Localization

- Use a custom `MaterialTheme` with colors, typography, shapes, and spacing.
- Required themes: light, dark, and system default.
- Discuss and approve the color palette before defining colors and themes.
- Typography scale should cover display, headline, title, body, and label.
- Shape scale should cover small, medium, and large components.
- Spacing scale: 4dp, 8dp, 16dp, 24dp, 32dp, exact values TBD.
- The app must have a custom adaptive icon.
- The app supports Persian (`fa`, RTL) and English (`en`, LTR).
- All strings live in `res/values/strings.xml` and `res/values-fa/strings.xml`.
- Use `start` and `end`, not `left` and `right`.
- Language changes should happen live without restart if feasible.

## Testing Rules

- Write tests for non-UI components.
- Use networking test tools/packages that simulate network behavior.
- Test networking delays and errors.
- Use Paging 3 for all long lists.
- Do not load all long-list data into one `LazyColumn` at once.
- Run relevant tests before finishing a feature.

## Branching And Planning

- Develop each feature in its own branch.
- Develop subfeatures in their own branches when useful.
- Merge subfeature branches into parent feature branches.
- Merge completed feature branches into `master`.
- UI-specific and data-specific feature work may happen in parallel.
- Before coding, create a top-level plan.
- Plans should define project criteria, feature dependencies, ownership, and shared phases.

## Diagrams

All project diagrams must be maintained in Eraser.

- Workspace/team: Parsa's Team 2.
- Project name: Android Project. In Eraser MCP/API terms, this project is an Eraser file named `Android Project`, not a separate project object.
- When locating the project in Eraser, use the file listing API (`list_files`) in Parsa's Team 2 and select the file titled `Android Project`. Do not rely only on search-by-title, because search may return no results even when the file exists.
- Use diagram-as-code for architecture diagrams, process diagrams, feature graphs, and ERDs.
- Arrange components cleanly when the API allows positioning.
- Show all processes, graphs, and ERDs in Eraser.
- Diagrams are for human understanding and may change over time.

## Phase 1 Responsibilities

The split mirrors the "Phase 1" BPMN diagram in the Android Project Eraser file.

- Parsa: set up the GitHub project, define the app UI, and integrate the chosen backend client in the Android app. (The Phase 1 BPMN originally sketched Firebase; the gateway result was "no — use an alternative backend", so Parsa's final Android-side step is "Use Alternative Backend in Android App".)
- Bagher: own the backend. This includes evaluating the candidate (Firebase vs alternative), defining the App–Backend API, and developing the chosen backend. The alternative chosen for Fuzic is **Supabase** — Bagher's deliverable is a self-hosted Supabase project with schema, auth, storage, and realtime ready for the Android client.
- Sina: prepare AI tooling and learn media playback well enough to define the player service. Media Playback Ready is Sina's Phase 1 exit event.

The chat feature belongs in later phases.

## Feature Requirements

### Home

Goal: show recommendations, recently played, top picks, and quick library navigation.

Required sections:

- Daily picks carousel with large images for albums, picks, and trending songs.
- Quick actions: Liked songs, Recently played, My playlists, Top artists.
- Horizontal carousels: Most popular, New releases, Global playlists, Local playlists.

Implementation:

- Pull data from Supabase.
- Use `HorizontalPager` for daily picks with auto-scroll.
- Use `LazyRow` for recommendation carousels.
- Use a vertical `LazyColumn` to stack sections.
- Use shimmer placeholders while loading.
- Tap cards to navigate to song, playlist, or artist details.

Acceptance:

- Sections load within 2s on normal network.
- Carousels scroll smoothly.
- Shimmer appears on first load, not subsequent cache loads.

### Search

Goal: search songs, artists, playlists, and users with saved history.

Implementation:

- Debounce input, e.g. 300ms.
- Use filter chips for Songs, Artists, Playlists, Users.
- Store search history in Room.
- Use Paging 3 for result lists.
- Handle no results, network errors, and very long queries.

Acceptance:

- Search does not hit backend on every keystroke.
- Filters narrow results correctly.
- History persists across restarts.

### Downloads

Goal: show downloaded songs for offline listening.

Implementation:

- Store metadata in Room.
- Store audio files in app-private storage.
- Use WorkManager for downloads.
- Support sorting by date, title, or artist.
- Support swipe-to-delete with undo snackbar.
- Play local files without network.

Acceptance:

- Downloads survive restart and reboot.
- Delete removes file and Room entry.
- Local playback uses no network data.

### Playlists

Goal: show global, local, and user playlists.

Implementation:

- Use `LazyVerticalGrid(columns = 2)`.
- Playlist cards may use generated colors based on playlist ID hash.
- Create playlist flow has name input and optional cover picker.
- Use Paging 3 for long playlist lists.

Acceptance:

- Grid scrolls smoothly.
- Newly created playlists appear in "My playlists" within 1s.

### Profile

Goal: show current user profile, follow stats, premium status, settings, chat, and sub-pages.

Required sections:

- Avatar, username, edit button.
- Premium status badge.
- Followers, Following, Playlist count.
- Entry points for followers, following, liked songs, recently played, settings, chat, logout.

Acceptance:

- Profile data loads within 2s.
- Tapping rows navigates correctly.

### Media Playback

Goal: professional music playback with mini-player, full-player, controls, queue, timers, speed, and visualizer.

Required:

- Persistent mini-player.
- Full player with large cover art, title, artist, progress seek bar, previous/play/next, shuffle, repeat, like, share-to-chat, add-to-playlist, sleep timer, playback speed, and Canvas-drawn visualizer.
- Notification and lock-screen controls through MediaSession.

Implementation:

- Use ExoPlayer / Media3.
- Wrap player in a `PlayerController` service or singleton.
- Use Compose Canvas for visualizer.
- Use shared element transition for mini-player to full-player.
- Play local file if downloaded, otherwise stream from URL.

Acceptance:

- Mini-player to full-player transition is smooth.
- Sleep timer pauses within 1s of expiry.
- Speed changes immediately.
- Visualizer renders continuously.

### Premium Status

Rules:

- Free users can stream unlimited music but cannot download.
- Premium users can stream and download offline.
- Premium users have a gold profile badge.

Implementation:

- User has `isPremium: boolean`.
- Store on Supabase and cache in DataStore.
- Download button visibility is driven by premium status.
- Free-user download attempt shows upgrade prompt.

Acceptance:

- Free users cannot trigger downloads.
- Premium badge updates within one profile refresh.

### Real-Time Chat

Goal: direct messages with read receipts, typing indicators, offline cache, and song sharing.

Required flow:

- Chat list sorted by most recent.
- Chat detail with newest messages at bottom and input at bottom.
- Realtime sending and receiving.
- Typing indicator.
- Message status icons: sending, sent, read.
- Share-song flow sends a song card message.
- Offline mode shows cached chat history.

Data model TBD, but expected collections include:

- `messages`: roomId, senderId, text, type, songId, status, createdAt, readAt, deliveredAt.
- `typing`: roomId, userId, updatedAt with short TTL behavior.
- `conversations`: participantIds, lastMessageAt, lastMessagePreview.
- Room cache for messages.

Implementation:

- `ChatRepository` is the single source of truth.
- Use optimistic local insert on send.
- Write new messages to Room.
- Mark visible unread incoming messages as read.
- Throttle typing updates to max once every 2s.
- Song share messages render as `SongShareCard`.

Acceptance:

- Two users can exchange messages with less than 2s perceived latency.
- Status icons update correctly.
- Typing appears within 1s and disappears within 5s of stopping.
- Shared songs are tappable and trigger playback.
- Cached chat history appears after app restart offline.

### Follow System

Goal: search users, follow/unfollow, and view followers/following.

Implementation:

- `FollowRepository` exposes user search, follow, unfollow, following, and followers.
- Use debounce and `distinctUntilChanged` for search.
- Use Paging 3 for lists.
- Block self-follow.
- Use optimistic update for follow/unfollow with rollback on persistent failure.

Acceptance:

- Search finds users in less than 1s on typical network.
- Follow/unfollow reflects in both users' lists within 2s.
- Lists scroll smoothly.

### Settings

Goal: change language, theme, and logout.

Implementation:

- Store language and theme in DataStore Preferences.
- Theme options: system, light, dark.
- Language options: system, en, fa.
- Apply theme through shared `ThemeManager`.
- Logout calls shared auth service and navigates to auth flow.

Acceptance:

- Language changes instantly and RTL flips correctly.
- Theme changes instantly.
- Logout signs out and returns to auth.

## Local Data

Expected Room tables:

| Table | Used for | Owner |
|---|---|---|
| `messages` | Offline chat history cache | Sina |
| `search_history` | Saved music/user search queries | TBD |
| `downloads` | Downloaded song metadata and file path | TBD |
| `liked_songs` | Liked songs | TBD |
| `recently_played` | Recently played songs | TBD |
| `playlists` | Local playlist cache | TBD |
| `chat_search_history` | Saved user search queries for follow system | Sina |

## Navigation

- Top-level navigation uses bottom tabs: Home, Search, Downloads, Playlists, Profile.
- Sub-navigation uses normal back stacks within each tab.
- Cross-cutting screens include chat, settings, follow lists, and full-player.
- Use type-safe navigation routes.

## Open Questions

Resolve these with the team before implementing affected areas:

1. Is Supabase Realtime enabled for the required tables (messages, conversations, follows)?
2. Who owns the shared `MaterialTheme`?
3. Who owns `UserService`, `AuthService`, `PlayerService`, and `ThemeManager`?
4. Who owns the full Profile tab?
5. Does logout clear offline chat cache?
6. What happens to downloaded songs on logout?
7. Which Supabase tables/columns are searchable, and which Postgres full-text indexes do we need?
8. Who curates the required minimum of 50 real songs, and where do audio files live?
9. What exact git workflow, PR review rules, and sync time should the team use?
10. Which formatter/linter should be used: ktlint, detekt, or Android Studio formatter?
11. Can Paging 3 in chat history v1 be deferred if time-constrained?
12. Who owns the Canvas-drawn full-player visualizer?

## Agent Checklist

Before implementing:

- Read this file.
- Check Android Skills.
- Confirm no rule conflicts apply.
- Create or update the plan.
- Identify missing assets.
- Identify required tests and previews.

During implementation:

- Follow the approved folder structure.
- Use Hilt, Coil, type-safe navigation, and theme tokens.
- Keep features loosely coupled.
- Add previews for UI components.
- Add tests for non-UI behavior and network edge cases.

Before finishing:

- Run relevant tests.
- Confirm preview coverage.
- Confirm docs or diagrams are updated when needed.
- Use gitmoji.dev as a commit-message prefix on top of Conventional Commits `<type>(<scope>): <subject>` (see Required Agent Behavior).
