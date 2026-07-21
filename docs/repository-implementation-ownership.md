# Repository Implementation Ownership

This document defines what each repository interface is responsible for and which backend, local storage, or service component should implement it.

Repositories are app-facing contracts used by ViewModels. They should expose product behavior, not Supabase table names, Room table names, Media3 APIs, WorkManager details, or storage bucket paths. Implementation classes may combine multiple backend/local/service components internally.

## Ownership

- Bagher owns backend and local data implementations: Supabase Auth/PostgREST/Realtime/Storage, Room DAOs, DataStore, WorkManager setup, and data mapping.
- Sina owns music playback service implementations: Media3/ExoPlayer, MediaSession, playback queue, player state, sleep timer, speed, and visualizer.
- Shared touchpoints should be coordinated through repository interfaces and UI models only.

## Shared Rules

- Repository methods return UI/domain models from `model/ui`, never remote DTOs or Room entities.
- Supabase table names, SQL filters, RLS details, storage paths, and realtime channel names stay inside implementation classes.
- Room table names, DAO queries, file paths, and WorkManager worker names stay inside implementation classes.
- Media3 player objects, service binders, notifications, and audio focus details stay inside the player implementation.
- Long lists use Paging repositories. Short dashboard summaries may use list-returning repositories with `offset` and `limit`.
- All blocking I/O must run on `Dispatchers.IO` from the caller or implementation.
- `Result` failures should contain app-safe errors. Do not expose raw backend schema details in messages shown to users.

## Repository Matrix

| Repository | Owner | Implementation component(s) | Responsibility | Notes |
|---|---|---|---|---|
| `AuthRepository` | Bagher | Supabase Auth, optional DataStore session cache | Login, signup, logout, current user flow, current user id | Keep auth provider details inside implementation. Signup can create profile metadata/row internally if needed. |
| `PasswordRecoveryRepository` | Bagher | Supabase Auth password recovery | Request password reset email | The UI should not know whether this is Supabase Auth email, Edge Function, or custom SMTP. |
| `UserRepository` | Bagher | Supabase users/profile table, optional local cache | Load/update profiles and search users | Search fields and ranking are implementation details. |
| `ProfileRepository` | Bagher | Supabase profile/follows/playlists queries, optional local cache | Provide current/target profile plus aggregated profile stats | Can internally delegate to `UserRepository`, `FollowRepository`, and `PlaylistRepository`, or query optimized backend views/functions. |
| `FollowRepository` | Bagher | Supabase follows table, Realtime if available | Follow/unfollow, followers/following page snapshots, follower/following counts | Must block self-follow and handle optimistic rollback where used. |
| `PagedFollowRepository` | Bagher | Paging 3, Supabase follows/users queries, optional Room cache | Paged followers, following, and user search | Used for long follow/search lists. Keep page keys/query strategy private. |
| `MusicRepository` | Bagher | Supabase songs/artists/playlists data, optional Room cache | Song discovery data: daily picks, trending, releases, popular, song details, song search | This is catalog data, not playback. Sina consumes `SongItem` through `PlayerRepository` later. |
| `ArtistRepository` | Bagher | Supabase artists/songs data, Paging 3, optional cache | Artist list, artist summary, artist details and popular songs | Artist ranking and follower count formatting can be mapped here or in a mapper. |
| `InteractionRepository` | Bagher | Supabase/Room interactions: plays, likes, recently played | Recently played, liked songs, record play, like/unlike | `recordPlay` may be called by Sina's player implementation after playback starts. |
| `PagedInteractionRepository` | Bagher | Paging 3, Room/Supabase interactions | Paged liked songs and recently played songs | Used for long liked/recently-played screens. |
| `PlaylistRepository` | Bagher | Supabase playlists, playlist_songs, Room playlist cache | Short playlist summaries and direct playlist mutations | `CreatePlaylistRequest` is app-level. Mapping visibility/type/storage fields is implementation detail. |
| `PagedPlaylistRepository` | Bagher | Paging 3, Supabase/Room playlist queries | Paged global/local/user playlists and playlist songs | Used for long playlist grids/details. |
| `PlaylistDetailsRepository` | Bagher | Supabase playlists, playlist songs, users, optional cache | Full playlist detail model | Can internally combine playlist metadata and songs, or use an optimized backend view. |
| `SearchRepository` | Bagher | Supabase search queries, Room search history, Paging 3 | Unified search over songs, artists, playlists, users plus persisted search history | Debounce stays in ViewModel/use case; backend fields and history DAO stay here. |
| `DownloadRepository` | Bagher, with Sina coordination | Room downloads table, app-private file storage, WorkManager, Supabase Storage/audio URLs | Observe downloads, enqueue/delete/restore downloads, remove audio file | Bagher owns metadata/work/storage. Sina should use downloaded file availability when choosing local vs streaming playback. |
| `ChatRepository` | Bagher | Supabase messages/conversations/typing, Realtime, Room message cache | Conversations, paged messages, typing status, sending text/song messages, read receipts, refresh | This is the single source of truth for chat. Conversation ids are app-level identifiers. |
| `NotificationRepository` | Bagher | Supabase notifications table or derived backend events, Paging 3, optional local cache | Paged notifications and read state | Map each actionable notification to `NotificationTarget`; UI must never parse backend deep-link payloads. |
| `SettingsRepository` | Bagher | DataStore Preferences | Observe and update theme/language/settings | Should support instant UI updates through `Flow<AppSettings>`. |
| `PremiumRepository` | Bagher | Supabase user premium status, billing integration stub, DataStore cache | Observe premium status, plans, purchase/restore operations | If real billing is deferred, keep the same contract and return controlled results from the implementation. |
| `PlayerRepository` | Sina | Media3/ExoPlayer service/controller, MediaSession, notification controls, visualizer source | Playback state, queue, play/pause/seek/skip, shuffle/repeat, speed, sleep timer, visualizer frames | Do not expose ExoPlayer or service binding details. It may call Bagher-owned repositories to record plays or resolve download/local file data. |

## Coordination Points

### Playback And Downloads

Sina's player implementation should decide whether to play a local downloaded file or a stream URL without changing `PlayerRepository`. Bagher's `DownloadRepository` should expose enough download metadata for the player/service layer to resolve downloaded files internally or through a small service collaborator.

Do not add file paths or WorkManager ids to UI models unless a screen truly needs to display them.

### Playback And Interactions

When playback starts or reaches the agreed threshold, `PlayerRepository` implementation can call `InteractionRepository.recordPlay(songId)`. The UI should not manually record plays from composables.

### Premium And Downloads

Premium checks should be handled before enqueueing or inside `DownloadRepository`, depending on final flow ownership. The UI can show premium prompts based on `PremiumRepository.observePremiumStatus()`, but backend enforcement must still exist in Bagher's implementation.

### Chat Song Sharing

`ChatRepository.sendSongMessage(...)` only receives a `songId`. The implementation decides how to store and hydrate the song card. Do not make ViewModels know message table columns or join strategy.

### Notification Targets

Bagher's `NotificationRepository` implementation must map supported backend notification payloads to the optional `NotificationTarget` on `NotificationItem`. Supported targets are song, playlist, artist, user profile, conversation, and premium. System or malformed notifications should return a null target; the UI will mark them read and show an unavailable message rather than attempting to interpret backend-specific data.

### Profile Stats

`ProfileRepository.getProfileStats(...)` may aggregate from follows/playlists using multiple repositories or a backend view/function. ViewModels should use the repository result and avoid counting full long lists when possible.

## Suggested Implementation Classes

Use these names unless the team agrees otherwise:

| Interface | Suggested implementation |
|---|---|
| `AuthRepository` | `SupabaseAuthRepository` |
| `PasswordRecoveryRepository` | `SupabasePasswordRecoveryRepository` |
| `UserRepository` | `SupabaseUserRepository` |
| `ProfileRepository` | `DefaultProfileRepository` |
| `FollowRepository` | `SupabaseFollowRepository` |
| `PagedFollowRepository` | `SupabasePagedFollowRepository` |
| `MusicRepository` | `SupabaseMusicRepository` |
| `ArtistRepository` | `SupabaseArtistRepository` |
| `InteractionRepository` | `SupabaseInteractionRepository` |
| `PagedInteractionRepository` | `SupabasePagedInteractionRepository` |
| `PlaylistRepository` | `SupabasePlaylistRepository` |
| `PagedPlaylistRepository` | `SupabasePagedPlaylistRepository` |
| `PlaylistDetailsRepository` | `SupabasePlaylistDetailsRepository` |
| `SearchRepository` | `DefaultSearchRepository` |
| `DownloadRepository` | `DefaultDownloadRepository` |
| `ChatRepository` | `DefaultChatRepository` |
| `NotificationRepository` | `SupabaseNotificationRepository` |
| `SettingsRepository` | `DataStoreSettingsRepository` |
| `PremiumRepository` | `DefaultPremiumRepository` |
| `PlayerRepository` | `Media3PlayerRepository` |

`Default*Repository` means the implementation probably combines more than one component, such as Supabase plus Room, DataStore, WorkManager, or another repository.
