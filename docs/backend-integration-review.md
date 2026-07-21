# Backend Integration Review

## Scope and conclusion

This report reviews the `backend-integration` branch against:

- `AGENTS.md`
- `docs/spotify-spec.md`
- `docs/group-decisions.md`
- The existing UI/model contracts on the `ui` branch
- The Supabase and Supabase Postgres best-practices guidance included in the repository

The review excludes the music-playing service owned by Sina, as requested.

### Overall conclusion

The branch is a **good compileable foundation**, but it is **not complete against the product requirements** and the repository interfaces are **not yet ready to be treated as the final application data contract**.

What is already present:

- Supabase Kotlin dependencies and client setup.
- Hilt application setup and repository bindings.
- Supabase Auth login, signup, logout, and current-session observation.
- Remote repository implementations for auth, users, songs, playlists, interactions, follows, and direct messages.
- Initial Supabase migrations with tables, foreign keys, RLS enabled, storage buckets, policies, and several indexes.
- A working compile check: `./gradlew :app:compileDebugKotlin` passes on `backend-integration`.

The most important missing or unsafe areas are:

1. No Room cache, DataStore settings/premium cache, downloads metadata, or WorkManager download layer.
2. No repository support for several required features: artist collections/details, playlist cover upload, notifications, premium plans/purchases, search across all required content types, chat conversations/typing/read receipts, and local playlists.
3. Chat realtime is not correctly scoped to the active conversation, and the migration currently leaves message realtime publication commented out.
4. Follow count methods deliberately throw `NotImplementedError`.
5. `RemotePlaylistRepository.getLocalPlaylists()` always returns an empty list.
6. Several repository methods expose raw network/domain models directly instead of stable app/domain contracts.
7. The backend models and UI models represent the same concepts with different names and shapes, so they should not be merged by simply choosing one file set.
8. Some RLS/storage details need a security review before production use.

The branch should be considered **Phase 1 backend scaffolding**, not a completed backend integration.

---

## 1. What the backend branch implements

### 1.1 Supabase and build integration

The branch adds:

- Supabase PostgREST.
- Supabase Auth.
- Supabase Storage.
- Supabase Realtime.
- Ktor OkHttp client.
- Kotlin serialization.
- Hilt and KSP.
- `BuildConfig.SUPABASE_URL` and `BuildConfig.SUPABASE_ANON_KEY`, loaded from `local.properties`.
- `FuzicApplication` annotated with `@HiltAndroidApp`.
- `SupabaseModule` providing one singleton `SupabaseClient`.
- `RepositoryModule` binding interfaces to remote implementations.

This is structurally aligned with the repository architecture in the project documents: the UI/ViewModels can depend on repository interfaces, while Hilt supplies remote implementations.

Important operational requirement:

```properties
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-publishable-or-anon-key
```

These values must remain local and must not be committed. The client must never receive a Supabase `service_role` or secret key.

### 1.2 Supabase schema and migrations

The migrations define these public tables:

| Table | Current purpose |
|---|---|
| `users` | Profile name, avatar, premium flag |
| `songs` | Song title, artist name, cover URL, audio URL, play count, release date |
| `playlists` | Playlist title, cover URL, owner, type, visibility |
| `playlist_songs` | Playlist/song many-to-many relationship |
| `interactions` | Play and like events |
| `follows` | Follower/followee relationships |
| `messages` | One-to-one text/song-share messages |

The schema includes:

- Foreign keys.
- Cascading deletes in most relationships.
- RLS enabled on the main public tables.
- Public song/profile reads.
- Authenticated ownership policies for playlists and playlist songs.
- User-scoped interaction policies.
- User-scoped message policies.
- Storage buckets named `covers` and `audio_files`.
- Several foreign-key indexes added in the advisor-fix migration.

The schema does not yet include tables required by the complete spec:

- Search history.
- Downloads metadata.
- Liked songs as a dedicated local cache.
- Recently played as a dedicated local cache.
- Chat conversations.
- Typing indicators.
- Message delivery/read timestamps.
- Notifications.
- Premium plans, subscriptions, or purchase records.
- Artist-specific metadata or artist collections.
- Albums.
- Local playlist cache or an explicit local/remote playlist strategy.

The current `interactions` table can represent likes and plays, but it needs stronger constraints and semantics before it is used as the sole source for all interaction behavior.

### 1.3 Authentication

`RemoteAuthRepository` implements:

- Email/password login.
- Email/password signup.
- Logout.
- Current-user/session observation.
- Current-user ID lookup.

Signup stores `full_name` in Supabase Auth metadata. A database trigger copies the name and avatar metadata into `public.users`.

The UI can use it for the existing authentication screens, but the contract currently returns `Result<Unit>` for login/signup. That makes it difficult for a ViewModel to immediately obtain the canonical profile, session, or confirmation state. The ViewModel will need to observe `getCurrentUserFlow()` separately.

Potential issue:

- The repository reads `userMetadata` and manually removes quote characters with `.toString().replace("\"", "")`. This is brittle. The metadata should be decoded using the JSON element API rather than string replacement.

### 1.4 Music repository

`RemoteMusicRepository` implements:

- Daily picks.
- Trending songs.
- New releases.
- Most popular songs.
- Song by ID.
- Song search.

The implementation supports offset/range pagination and sorting by `play_count` or `release_date`.

Limitations:

- Daily picks are currently just an unsorted slice of `songs`; there is no recommendation or curated daily-pick rule.
- Search only searches `songs.title` and `songs.artist_name`.
- It cannot search artists, playlists, or users, although the Search requirement explicitly requires all four.
- It does not expose artist details or artist song collections.
- It does not expose albums.
- It returns database `Song` objects rather than the UI-facing `SongItem`.
- No cache fallback exists.
- No explicit result metadata or page information is returned.
- Empty results and transport errors are collapsed into `Result.failure`/empty lists without a domain error taxonomy.

### 1.5 Playlist repository

`RemotePlaylistRepository` implements:

- Global/public playlist retrieval.
- User playlist retrieval.
- Playlist songs.
- Playlist creation.
- Playlist deletion.
- Adding/removing songs.

Limitations:

- `getLocalPlaylists()` is a stub returning `Result.success(emptyList())`.
- Playlist creation does not accept `cover_image_url`, despite the UI now supporting cover selection.
- There is no cover upload method through Supabase Storage.
- There is no playlist update/rename method.
- There is no reorder method.
- There is no duplicate-name validation contract.
- The global/local distinction is currently inferred from `is_public` versus a stub, not from a documented product rule.
- The implementation manually creates UUIDs even though the database has `gen_random_uuid()` defaults. This is not necessarily wrong, but it creates an avoidable client/database responsibility split.
- `addSongToPlaylist` does not define duplicate behavior at the repository contract level. The database primary key will reject duplicates, but the ViewModel receives only a generic failure.

### 1.6 Interaction repository

`RemoteInteractionRepository` implements:

- Recently played query.
- Liked songs query.
- Record play.
- Like.
- Unlike.

Limitations:

- `likeSong` inserts a new interaction each time. There is no idempotency/upsert behavior, so repeated like calls can create duplicates unless the ViewModel prevents them.
- `recordPlay` creates one row per play, which may be intended, but there is no retention or deduplication policy.
- There is no method to ask whether a particular song is liked.
- There is no method to toggle or atomically update a like.
- There is no local Room cache.
- The spec requires liked/recently-played data to be usable offline; this repository only queries Supabase.
- The returned `Song` model does not contain the interaction timestamp, so recently played UI cannot accurately display recency from this method alone.

### 1.7 Follow repository

`RemoteFollowRepository` implements:

- Follow.
- Unfollow.
- Followers list.
- Following list.
- Interface placeholders for follower/following counts.

Limitations:

- `observeFollowersCount()` throws `NotImplementedError`.
- `observeFollowingCount()` throws `NotImplementedError`.
- No user-search method exists in `FollowRepository`; user search is placed in `UserRepository`. That can work, but it differs from the requirement wording and makes follow-related ViewModels depend on two repositories.
- No explicit self-follow prevention exists in the repository or database.
- No optimistic update/rollback support is represented in the contract.
- The follower list query uses embedded relationship decoding that needs an integration test against the real Supabase schema.
- The `follows` SELECT policy is `USING (true)` for authenticated users. This means any authenticated user can read all follow rows, not merely data needed for a specific list. It may be acceptable for this course app, but it is broader than necessary.

### 1.8 Chat repository

`RemoteChatRepository` implements:

- History for a two-user conversation.
- Send text or song-share messages.
- Mark a message read.
- A realtime message flow.

Limitations against the chat requirement:

- No conversation/chat-list query.
- No conversation model or last-message aggregation.
- No typing indicator API.
- No delivered timestamp/status update API.
- No read receipt query/update beyond setting a generic `status`.
- No Room cache.
- No optimistic local insert.
- No offline mode.
- No retry/outbox behavior.
- No paging abstraction beyond raw offset/limit.
- No explicit sender/receiver validation in the insert contract.

Critical implementation issue:

`observeMessages(userId)` obtains `currentUserId` but never uses either `currentUserId` or `userId` to filter the realtime event. It subscribes to `public:messages` and decodes every inserted message received on that channel. A Chat ViewModel must not rely on this flow as-is for a conversation screen; it can leak unrelated message events into the wrong conversation state.

Critical database issue:

The migration contains:

```sql
-- ALTER PUBLICATION supabase_realtime ADD TABLE public.messages;
```

Because the statement is commented out, realtime broadcasts may not be enabled for `messages` in a fresh database. The repository has a realtime implementation, but the schema setup does not guarantee that it receives events.

### 1.9 User repository

`RemoteUserRepository` implements:

- Profile fetch.
- Profile update.
- User search by name.

Limitations:

- Search only uses `name`, while the UI distinguishes username and display name.
- The database `users` table has no username column.
- It does not return follower/following/playlist counts.
- It does not expose premium updates as a separate state.
- There is no avatar upload method.
- `updateProfile` sends the complete `User` object instead of a focused update request, which can accidentally overwrite fields.

---

## 2. Repository interface assessment

### Overall assessment

The interfaces are a useful first abstraction, but they are not yet complete or stable enough for the final ViewModel layer.

Strengths:

- Interfaces are separated by feature.
- Methods are suspendable for I/O.
- `Result<T>` makes failure visible to ViewModels.
- Pagination parameters are present on most long-list queries.
- Realtime is represented with `Flow`.
- Hilt binds interfaces rather than exposing concrete Supabase classes to UI.

Weaknesses:

- Pagination uses raw `offset`/`limit` instead of Paging 3 `PagingSource`/`Pager`, despite Paging 3 being required for long lists.
- `Result<List<T>>` does not expose `hasNextPage`, total count, cursor, or end-of-pagination information.
- Interfaces return transport/database models instead of domain models.
- User identity is passed manually to many methods even though AuthRepository already knows the current session. This can permit accidental cross-user queries and creates repetitive ViewModel code.
- No explicit `Dispatcher`/repository threading policy is visible. Network calls are suspend functions, but the architecture should still guarantee I/O execution off Main.
- No typed domain errors.
- No cache/source-of-truth contract.
- No idempotency or optimistic-operation contract.
- No cancellation/retry guidance.
- No offline behavior.
- No notification, settings, premium, download, or storage contracts.

### Recommended interface direction

Do not make the UI depend directly on the Supabase models. Keep three layers conceptually separate:

1. **Remote DTO/database models**: serialization names and Supabase schema shape.
2. **Domain models**: stable feature-level models used by repositories/use cases/ViewModels.
3. **UI models**: `SongItem`, `ProfileUiState`, `ChatMessage`, and other Compose-facing state.

The existing backend `Song`, `User`, `Playlist`, and `Message` classes should be treated as remote DTOs. They should be mapped before reaching the UI.

For long lists, the preferred final shape is similar to:

```kotlin
interface MusicRepository {
    fun searchSongs(query: String): Flow<PagingData<Song>>
}
```

or a repository-specific `PagingSource`, with the ViewModel calling:

```kotlin
val songs = repository.searchSongs(query)
    .cachedIn(viewModelScope)
```

For one-shot detail requests, `suspend fun ...: Result<DomainModel>` remains appropriate.

---

## 3. How ViewModels can use the current interfaces

The current interfaces can be used immediately for a first integration pass, but ViewModels should own UI state and convert repository models to UI models.

### 3.1 General ViewModel pattern

```kotlin
@HiltViewModel
class ExampleViewModel @Inject constructor(
    private val repository: MusicRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExampleUiState(isLoading = true))
    val uiState: StateFlow<ExampleUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            repository.getTrendingSongs()
                .onSuccess { songs ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            songs = songs.map { song -> song.toSongItem() },
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.toUserMessage(),
                        )
                    }
                }
        }
    }
}
```

The actual app should use localized error resources or typed error codes rather than exposing exception messages directly.

### 3.2 HomeViewModel

The current repository layer can supply:

- Daily picks from `getDailyPicks()`.
- Trending/most popular from `getTrendingSongs()` or `getMostPopular()`.
- New releases from `getNewReleases()`.

It cannot supply all Home requirements yet:

- Global/local playlist carousels require `PlaylistRepository`.
- Top artists require an artist query that does not exist.
- Cached Home snapshot does not exist.

The ViewModel should load independent sections concurrently and preserve partial success:

```kotlin
val dailyPicks = async { musicRepository.getDailyPicks() }
val popular = async { musicRepository.getMostPopular() }
val releases = async { musicRepository.getNewReleases() }
val globalPlaylists = async { playlistRepository.getGlobalPlaylists() }
```

Do not fail the entire Home screen because one carousel fails. The UI already supports loading, cached, empty, and error presentation states.

### 3.3 SearchViewModel

The current repository only supports song search:

```kotlin
viewModelScope.launch {
    query
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { value ->
            if (value.isBlank()) flowOf(emptyList())
            else flow {
                emit(musicRepository.searchSongs(value).getOrThrow())
            }
        }
        .catch { error -> ... }
        .collect { songs -> ... }
}
```

This is not enough for the UI filter tabs:

- Songs: `MusicRepository.searchSongs`.
- Artists: missing.
- Playlists: missing.
- Users: `UserRepository.searchUsers`.

The final design should either:

- Add a unified `SearchRepository` with typed search results; or
- Add artist/playlist search methods and let the ViewModel coordinate all four repositories.

Search history remains a local Room concern and is not provided by the backend branch.

### 3.4 DownloadsViewModel

The current backend has no download contract. Downloads should not be implemented by directly downloading `Song.audioUrl` inside a ViewModel.

The required design needs:

- `DownloadRepository`.
- Room `downloads` table.
- File storage manager.
- WorkManager worker.
- Premium capability check.
- Progress flow.
- Delete/undo behavior.

The ViewModel should observe Room/download state and call a use case such as `startDownload(songId)`. Supabase only supplies the remote audio URL and metadata.

### 3.5 PlaylistsViewModel

The current `PlaylistRepository` can support a first pass of:

- Global playlists.
- User playlists.
- Playlist songs.
- Create/delete.
- Add/remove songs.

It cannot yet support:

- Local playlists.
- Cover upload.
- Cover selection persistence.
- Rename/update.
- Paging 3.
- Duplicate-name validation.

The current UI's create form contains `selectedCoverUri`, so the repository contract must eventually accept either:

```kotlin
suspend fun createPlaylist(request: CreatePlaylistRequest): Result<Playlist>
```

where the request contains an optional local image URI, or a two-step flow:

1. Upload cover to Storage.
2. Create playlist with the resulting public/path URL.

The ViewModel should not know Supabase bucket names.

### 3.6 ProfileViewModel

The current repositories can provide:

- `UserRepository.getUserProfile`.
- `FollowRepository.observe...Count()` only after those methods are implemented.
- Playlist count only by loading user playlists and counting them, which is inefficient.

The Profile UI also exposes settings, chat, logout, liked songs, and recently played. Those should be separate navigation events and ViewModels, not one ProfileViewModel making every repository call.

The backend should eventually expose a profile summary query or RPC/view returning:

- User profile.
- Followers count.
- Following count.
- Playlist count.

### 3.7 Follow ViewModels

The current interfaces can support basic list loading and follow/unfollow:

```kotlin
fun onFollowClicked(user: FollowUser) {
    val previous = user.isFollowing
    updateOptimistically(user.id, !previous)

    viewModelScope.launch {
        val result = if (previous) {
            repository.unfollowUser(user.id)
        } else {
            repository.followUser(user.id)
        }
        result.onFailure {
            updateOptimistically(user.id, previous)
        }
    }
}
```

Before using this in production:

- Reject self-follow.
- Add count implementations.
- Replace raw lists with Paging 3.
- Verify Supabase embedded relationship decoding.
- Decide whether search belongs in `UserRepository` or `FollowRepository`.

### 3.8 Chat ViewModels

The current interface is enough for a prototype text-message screen:

1. Load history.
2. Collect `observeMessages`.
3. Append messages matching the active conversation.
4. Send a message.
5. Mark visible incoming messages read.

However, because the repository's realtime flow is not filtered, the ViewModel must not trust it without fixing the repository. The correct filtering belongs in the repository subscription or in a shared conversation filter that is guaranteed to be applied before exposing the flow.

The production ViewModel also needs:

- Conversation list loading.
- Room cache.
- Optimistic `Sending` state.
- Retry/failure state.
- Typing updates.
- Delivered/read state.
- Song-share message mapping from backend `Message` to UI `ChatMessage`.

### 3.9 AuthViewModel

The current interface can power sign-in/sign-up:

```kotlin
viewModelScope.launch {
    authRepository.login(email, password)
        .onSuccess { /* observe session and emit navigation event */ }
        .onFailure { error -> /* set localized error state */ }
}
```

The ViewModel should collect `getCurrentUserFlow()` and use it as the authentication source of truth. It should not assume a successful signup always means an authenticated session, because email confirmation settings may change that behavior.

---

## 4. Model comparison with the UI branch

### 4.1 Same concepts, different classes

These backend models overlap with UI concepts but should not be blindly merged:

| Backend model | UI model(s) | Main differences | Recommendation |
|---|---|---|---|
| `Song` | `SongItem`, `FeaturedMusicItem`, `DownloadedSongItem` | Backend has DB fields and URLs; UI has display labels, album, duration, explicit flag, download state | Keep `Song` as remote DTO; map to UI models |
| `User` | `ProfileUser`, `FollowUser`, `ArtistItem` | Backend has nullable `name`, avatar, premium; UI needs username/display name/follow state/artist-specific fields | Keep separate feature/domain projections; fix schema for username/artist data |
| `Playlist` | `PlaylistItem`, `PlaylistDetails` | Backend has owner/type/public/cover; UI needs subtitle, song count, description, owner display name | Map remote playlist plus related data into UI models |
| `Message` | `ChatMessage`, `ChatConversation`, `NotificationItem` | Backend message is sender/receiver/content/song ID; UI needs status enum, song object, mine flag, time label, conversation summary | Keep separate; add conversation and message mapping |
| `Follow` | `FollowUser`, `FollowListUiState` | Backend is relationship row; UI needs user profile and optimistic follow state | Keep `Follow` as persistence DTO |
| `Interaction` | No direct UI model; used by liked/recently played screens | Backend event row; UI needs `SongItem` and display time/context | Keep as persistence DTO; return richer domain result |
| `PlaylistSong` | `PlaylistDetails.songs` | Backend pivot row; UI needs resolved song list and ordering | Keep pivot model; map joined songs |

### 4.2 Backend-only models

These are appropriate backend/persistence models without direct UI equivalents:

- `Follow`
- `Interaction`
- `PlaylistSong`

They should remain outside the Compose-facing model package conceptually. The package can remain temporarily shared, but a cleaner final structure is:

```text
model/
  remote/
    SongDto.kt
    UserDto.kt
    PlaylistDto.kt
    MessageDto.kt
    FollowDto.kt
    InteractionDto.kt
  domain/
    Song.kt
    UserProfile.kt
    Playlist.kt
    ChatMessage.kt
  ui/
    ... existing Compose UI state and UI models ...
```

### 4.3 UI-only models

These have no backend equivalent and should remain UI/domain concerns:

- `AuthUiState`
- `WelcomeUiState`
- `HomeUiState`
- `SearchUiState`
- `DownloadsUiState`
- `PlaylistsUiState`
- `ProfileUiState`
- `SettingsUiState`
- `PlayerUiState`
- `NotificationsUiState`
- `PremiumUiState`
- `ArtistCollectionUiState`
- `ChatListUiState`
- `ChatDetailUiState`
- `FollowSearchUiState`
- `FollowListUiState`

These are not database entities. Do not replace them with Supabase serialization classes.

### 4.4 Fields that need reconciliation

#### Song

Backend:

- `id`
- `title`
- `artistName`
- `coverImageUrl`
- `audioUrl`
- `playCount`
- `releaseDate`

UI:

- `id`
- `title`
- `artist`
- `artworkUrl`
- `album`
- `durationLabel`
- `isExplicit`

Required mapping:

```kotlin
fun Song.toSongItem() = SongItem(
    id = id,
    title = title,
    artist = artistName,
    artworkUrl = coverImageUrl,
)
```

The backend schema must add album, duration, and explicit-content fields if the UI is expected to display real values.

#### User/profile/follow

Backend has `name` but no username. UI requires both `username` and `displayName`.

Recommended schema change:

- Add a unique `username` column to `public.users`.
- Keep `name` as display name, or rename it to `display_name` consistently.
- Add update and validation rules for username.

Then map:

```kotlin
fun User.toProfileUser() = ProfileUser(
    id = id,
    username = username,
    displayName = name.orEmpty(),
    avatarUrl = avatarUrl,
    isPremium = isPremium,
)
```

#### Playlist

Backend lacks:

- Description.
- Song count.
- Cover upload request semantics.
- Owner display name.

These can be calculated with joins or a profile/playlist-summary query, but should not be fabricated in the ViewModel.

#### Message/chat

Backend has one `status` string and `sharedSongId`. UI has:

- `ChatMessageType`.
- `ChatMessageStatus`.
- Resolved `SongItem`.
- `isMine`.
- Conversation preview.
- Typing/offline state.

The database should eventually add:

- `delivered_at`.
- `read_at`.
- `message_type`.
- A conversation/room identifier or a deterministic conversation key.
- Conversation summary table or query.
- Typing state table/channel.

---

## 5. Requirements coverage matrix

| Requirement | Backend status | Notes |
|---|---|---|
| Auth sign-in/sign-up/logout | Partial | Implemented through Supabase Auth; confirmation/profile/session behavior needs ViewModel handling |
| Home daily picks | Partial | Raw song slice, no recommendation/cache layer |
| Home popular/new releases | Partial | Song queries exist |
| Home global/local playlists | Partial | Global exists; local is stub |
| Home top artists | Missing | No artist repository/schema |
| Search songs | Partial | Song title/artist only |
| Search artists/playlists/users | Missing/partial | Users exist; artists/playlists missing |
| Search history | Missing | Requires Room |
| Downloads | Missing | Requires Room, files, WorkManager, premium policy |
| Playlist global/user lists | Partial | Remote queries exist |
| Playlist local list | Missing | Stub |
| Playlist create/delete/add/remove | Partial | Core calls exist |
| Playlist cover picker/upload | Missing | UI exists, storage/repository flow missing |
| Profile | Partial | Basic user profile exists; counts/username incomplete |
| Liked songs | Partial | Remote interaction query exists; no cache/liked-state query |
| Recently played | Partial | Remote query exists; no cache/timestamp projection |
| Follow/unfollow | Partial | Core operations exist; self-follow/counts/paging incomplete |
| Followers/following | Partial | Queries exist; relationship decoding needs tests |
| Chat list | Missing | No conversation repository method/table |
| Chat history | Partial | Basic two-user history exists |
| Chat realtime | Incomplete | Flow is unfiltered; publication setup is commented out |
| Typing indicators | Missing | No schema or repository API |
| Delivered/read receipts | Incomplete | Generic status update only |
| Song sharing | Partial | `shared_song_id` exists; no resolved song mapping |
| Settings | Missing | DataStore/SettingsRepository not present |
| Premium profile flag | Partial | `is_premium` exists |
| Premium upgrade/purchases | Missing | UI exists, no backend/payment contract |
| Notifications | Missing | No table/repository |
| Media playback data | Partial | Song audio URL exists; player service excluded from this review |
| Offline cache | Missing | No Room implementation |
| Paging 3 | Missing | Raw offset/limit only |

---

## 6. Schema and security findings

### High priority

#### Realtime publication is not enabled in the committed migration

The only realtime publication statement is commented out:

```sql
-- ALTER PUBLICATION supabase_realtime ADD TABLE public.messages;
```

This should be verified in the actual Supabase project and enabled through a migration if chat realtime is required.

#### Chat realtime is not conversation-scoped

The current flow can emit every inserted message to every subscriber. Filter by both participants before exposing the flow.

#### Follow counts are not implemented

The interface promises `Flow<Int>`, but both implementations throw. Any ProfileViewModel using them will crash.

#### Storage policies were removed and recreated broadly

The migrations drop storage read policies and recreate public read access. This may be intentional for public music assets, but it should be explicitly approved because audio and cover files are then publicly readable by URL.

### Medium priority

#### Missing constraints

Consider adding:

- `CHECK (interaction_type IN ('play', 'like'))`.
- A unique partial index for one active like per `(user_id, song_id)`.
- `CHECK` constraints for message status/type.
- `CHECK (follower_id <> followee_id)`.
- Non-null constraints where product behavior requires them.

#### Users are created from editable auth metadata

The trigger uses `raw_user_meta_data`. This is acceptable for initial profile creation, but it must not be used for authorization decisions. Premium authorization must not rely on editable user metadata; the public `users.is_premium` field also needs carefully protected update policy or a server-controlled mechanism.

#### No explicit grants/API exposure verification

The migration enables RLS, but the project still needs verification that required tables are exposed through Supabase's Data API and that `anon`/`authenticated` grants match the intended access.

#### No schema test data or integration tests

There is no evidence in the branch of:

- Seed data for the required minimum song catalog.
- Repository integration tests.
- RLS tests.
- Realtime tests.
- Pagination tests.
- Auth confirmation/error tests.

---

## 7. Recommended merge strategy for the models

Do not merge by deleting the UI models or copying backend models into the UI files. Use a deliberate adapter boundary.

### Step 1: Rename backend classes as DTOs

Prefer names that communicate their role:

- `SongDto`
- `UserDto`
- `PlaylistDto`
- `MessageDto`
- `FollowDto`
- `InteractionDto`
- `PlaylistSongDto`

If renaming immediately is too disruptive, keep the current names temporarily but place them under a remote/data package and document that they are not UI models.

### Step 2: Keep existing UI state/models

Retain the current UI types because they describe presentation state and preview scenarios. They should not become `@Serializable` Supabase records.

### Step 3: Add mapping extensions

Create mapper files such as:

```text
data/mapper/SongMappers.kt
data/mapper/UserMappers.kt
data/mapper/PlaylistMappers.kt
data/mapper/MessageMappers.kt
```

Examples:

```kotlin
fun SongDto.toSongItem(): SongItem = SongItem(
    id = id,
    title = title,
    artist = artistName,
    artworkUrl = coverImageUrl,
)

fun PlaylistDto.toPlaylistItem(
    ownerName: String,
    songCount: Int,
): PlaylistItem = PlaylistItem(
    id = id,
    title = title,
    subtitle = ownerName,
    songCountLabel = songCount.toString(),
    artworkUrl = coverImageUrl,
)
```

### Step 4: Add domain request/result types where needed

Examples:

- `CreatePlaylistRequest`.
- `SearchResult`.
- `ConversationSummary`.
- `ProfileSummary`.
- `DownloadMetadata`.
- `PremiumEntitlement`.

These types prevent ViewModels from passing Supabase-specific details such as bucket names, raw `type` strings, or database status strings.

### Step 5: Update repository interfaces toward feature contracts

Add missing contracts instead of making one interface responsible for everything:

- `SearchRepository`.
- `NotificationRepository`.
- `PremiumRepository`.
- `DownloadRepository`.
- `SettingsRepository`.
- `ArtistRepository`.
- `ConversationRepository` or expanded `ChatRepository`.

### Step 6: Integrate ViewModels one screen at a time

Recommended order:

1. Auth.
2. Profile and follow counts.
3. Home song sections.
4. Search songs/users.
5. Playlists.
6. Liked/recently played.
7. Chat after realtime/cache work is fixed.
8. Downloads/premium/settings using Room/DataStore/WorkManager.

This order gives early visible progress while leaving the most stateful infrastructure for after the contracts are stable.

---

## 8. Recommended next work for the backend owner

### Must fix before ViewModel integration

1. Verify migrations apply cleanly to a fresh Supabase project.
2. Enable and verify `messages` realtime publication.
3. Filter `observeMessages` to the active conversation.
4. Implement follower/following count flows.
5. Replace `getLocalPlaylists()` stub with the agreed local/remote design.
6. Add repository tests for auth, songs, playlists, follows, and chat.
7. Add RLS tests for every user-owned table.
8. Confirm Data API exposure/grants.
9. Add schema constraints for likes, follow self-blocking, and statuses.

### Must add for requirement completion

1. Room cache and repository source-of-truth strategy.
2. DataStore settings and premium cache.
3. WorkManager download repository.
4. Artist and album support.
5. Search across songs/artists/playlists/users.
6. Notifications.
7. Premium entitlement/upgrade contract.
8. Chat conversations, typing, delivery/read timestamps, and offline cache.
9. Playlist cover upload and update.
10. Paging 3 repository integration.

---

## 9. Final recommendation to the team

The backend branch should be merged as **infrastructure scaffolding only**, not advertised as feature-complete backend integration.

Before connecting all existing UI screens, agree on these shared contracts:

- Canonical remote/domain/UI model separation.
- User fields: `username`, `displayName`, `avatar`, premium ownership.
- Search API shape.
- Playlist cover upload flow.
- Chat conversation identity and realtime filtering.
- Local cache ownership.
- Paging strategy.
- Premium entitlement authority.

Once those decisions are recorded, the current repository interfaces are a reasonable starting point for simple one-shot ViewModels, but they should be extended before building production ViewModels around them.

## Validation performed

- Reviewed the backend branch files, repository interfaces, remote implementations, DI modules, migrations, and model classes.
- Compared them with the UI branch model files and the documented requirements.
- Ran:

```bash
./gradlew :app:compileDebugKotlin
```

- Result: `BUILD SUCCESSFUL`.

