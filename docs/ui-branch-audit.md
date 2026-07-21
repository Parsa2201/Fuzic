# UI Branch Audit

**Branch reviewed:** `ui`  
**Reviewed:** 22 July 2026  
**Scope:** Compose UI, previews, UI-facing view models, and Navigation Compose. Repository implementations are deliberately out of scope; the findings below only identify the UI contracts those implementations must satisfy.

## Tracking legend

- **Done** — implemented and committed on `ui`.
- **Partial** — the incorrect behavior is contained, but the complete product requirement still needs work.
- **Pending** — not implemented yet.

## Executive summary

The branch has a substantial UI foundation: the five-tab shell, type-safe destinations, Hilt view models, theme tokens, loading/empty/error compositions, and most of the product screens are present. The project builds and all current debug unit tests pass.

Before this can be treated as feature-complete, prioritize fixing the confirmed state-collection and navigation defects below. Then replace in-memory/list-based UI contracts with Paging contracts where the spec requires it, finish the incomplete flows, and reduce the navigation graph's responsibility.

## Confirmed defects

| Status | Priority | Area | Finding and effect | Recommended correction |
|---|---|---|---|
| Done (`847ea86`) | P0 | Chat | `ChatDetailViewModel` collects `observeMessages()` but discards each emitted list. Opening a conversation therefore never renders repository/cached/realtime messages; only a locally sent message is appended. | Now stored as `PagingData` and rendered through `LazyPagingItems`. Preserve the paging contract for realtime/cache implementations. |
| Done (`847ea86`) | P0 | Notifications | `NotificationsViewModel` collects `observeNotifications()` but only changes loading/error state. The notifications list is never populated. | Now stored as `PagingData` and rendered through `LazyPagingItems`. |
| Done (`893fa65`) | P0 | Welcome | In `FuzicNavigation`, `WelcomeScreen` receives empty callbacks for `onPageChanged` and `onNextClick`. The Next button does nothing, so button-only users are stuck on the first page (swiping happens to work). | Next now animates the pager forward; the route callback remains optional. |
| Done (`ui`) | P1 | Password recovery | `PasswordRecoveryDestination` owns `email` and `isSubmitted` with `rememberSaveable` and sets success locally. It never calls `PasswordRecoveryRepository`, cannot report an error/loading state, and claims success without sending a reset request. | A Hilt `PasswordRecoveryViewModel` now calls Supabase through `PasswordRecoveryRepository` on `Dispatchers.IO` and exposes loading, error, and success state. |
| Done (`ui`) | P1 | Logout entry | Choosing `ProfileEntry.Logout` merely opens Settings instead of requesting logout or opening the confirmation dialog. This is misleading and adds an unexpected extra step. | Profile now opens Settings with a typed `showLogoutConfirmation` argument, so the existing confirmation and logout flow is presented directly. |
| Partial (`893fa65`) | P1 | Music navigation | `navigateForItem()` only recognises `"Playlist"` and `"Artist"`; `MusicItemType.Album` falls through to `SongDestination`. Album cards will open a song-detail screen. | Routing is now type-safe and Albums no longer open song detail. Add a real album destination or hide album cards before release. |
| Pending | P1 | Song download | Song details wires Download to a generic “action unavailable” snackbar, regardless of premium state. The required upgrade prompt is not reached from this entry point. | Add a UI-facing premium/download eligibility state and show `PremiumScreen`/upgrade dialog for free users; dispatch an actual download request for premium users. |
| Pending | P1 | Chat read receipts | `MarkMessagesRead` exists but no navigation/UI code dispatches it for visible unread incoming messages. | Have the chat route derive visible unread messages and send the intent once per visible set, with an idempotency guard. |

## Remaining UI work by requirement

### Navigation and app shell

- Keep Navigation Compose 2 with `@Serializable` destinations, as required by the team decision. Do **not** migrate to Navigation 3 merely for adaptive layouts.
- Split `FuzicNavigation.kt` into a route/destination file, a root graph, and feature graph builders. It currently owns all destination declarations, Hilt acquisition, authentication redirects, screen callbacks, snackbars, bottom navigation, player sheet routing, and item-type mapping in one file. This makes each feature hard to test or change independently.
- **Done (`c43f949`):** `FollowListDestination.type` is now `FollowListType`; the string parsing fallback is removed.
- Centralize shell visibility (`showShell`, mini-player visibility, top bar) as route metadata or helper functions. The two manual boolean chains will drift as destinations grow.
- Establish an explicit authenticated root/start destination. The app always starts at Welcome and redirects later; restoration and a slow current-user flow can produce visible onboarding flashes. On logout, clear the entire authenticated stack rather than only relying on a later `currentUser == null` redirect.
- Add one-time UI events (`SharedFlow`/`Channel`) for navigation and snackbars. At present, the root graph infers completion from retained state (`isComplete`) and performs side effects in `LaunchedEffect`; returning to a destination can replay a snackbar/pop.
- The spec calls for a shared-element mini-player → full-player transition. The mini-player currently navigates normally; implement the transition after choosing the approved Compose API/version.

### Screen/UI completion

- **Home:** auto-scroll, cards, sections, shimmer, cached-content banner, and state branches exist. Ensure cached snapshots are driven by repository cache metadata; add a real empty-state CTA target. Album support must be resolved as above.
- **Search:** debounce and filters exist, but the UI state is a full `List<SearchResultItem>`. Convert results to Paging 3 (`PagingData`, `collectAsLazyPagingItems`) before real backend integration, enforce the specified maximum query length, and persist history through Room implementation.
- **Downloads:** UI contains sorting, progress, swipe-to-dismiss, undo, and storage warning. Connect `FreeUpSpace` to a meaningful OS/storage action or remove the CTA; ensure delete waits for both file and Room removal. Add a premium-aware download entry from song/player screens. Download worker/file work belongs to the repository/data owners.
- **Playlists and playlist detail:** grid/create/detail/play-all UI exists. **Done (`ui`):** the playlist grid uses `GridCells.Adaptive(160.dp)` instead of a fixed two-column layout. Pending: Paging 3 for long section/list data and an optional photo-picker cover flow.
- **Profile/follow:** profile editor, public profile, follow search/list, and premium badge exist. Add a route from profile to follow search if it is intended to be reachable; make logout direct as noted above.
- **Player:** queue, timer, speed, visualizer, and action sheets are present. Verify the repository/player service supplies continuous progress and visualizer frames, then implement MediaSession/notification/lock-screen controls in the player layer. Ensure SongActionSheet offers all intended actions consistently (including like/download where appropriate).
- **Chat:** list/detail/share UI and optimistic local send exist. Fix message collection first, then add reverse-layout/newest-at-bottom behavior, visible-message read dispatch, scroll-to-new-message behavior, and Paging 3 for history if v1 cannot safely load a full cache.
- **Premium:** plans/upgrade UI exists. Define the purchase result contract with the owner; show a deterministic post-purchase state and connect every download gate to it.
- **Settings/localization:** theme and locale are applied through `FuzicApp`; verify language changes using an emulator because `createConfigurationContext`/composition locals are not an app-wide locale API. Persisted Android resources and activity-level configuration must both update. Use a single settings source of truth rather than letting `AppSettingsViewModel` and `SettingsViewModel` independently collect settings.

## View-model refactoring

- Keep the current `StateFlow<UiState>` and intent direction; it is broadly aligned with MVVM/UDF.
- Standardize each feature on: one `UiState`, one intent type, cancellation of repeated collection jobs, and `UiEvent` for transient events. A few small routes retain local mutable state instead of a view model (notably password recovery).
- Ensure every repository Flow emission updates content, not only loading flags. This applies immediately to chat and notifications and should be a review checklist item for all future collectors.
- Avoid collecting long-lived repository flows each time `Retry` runs unless the prior job is cancelled. `SettingsViewModel`, `ChatListViewModel`, `NotificationsViewModel`, and `PremiumViewModel` should retain/cancel their collection `Job`, as Downloads already does.
- Move duplicated error-result mapping into a small, explicit UI-state helper only if it remains shared after the repository contracts settle; do not introduce empty pass-through use cases.
- The `PlayerViewModel` owns an optimistic `isLiked` toggle without a persistence operation. Either give liking to the interaction repository with rollback or remove the control until that contract is available.

## Preview and visual QA findings

- Coverage is generally strong: most screens include English, Persian, content, empty, loading, and error previews. The home shimmer previews are useful delayed-loading scaffolding.
- Missing reusable-component previews: `ContentDetailComponents.kt` and `SongActionSheet.kt` have no previews. Add normal/long-text/RTL and actionable states for each public reusable composable.
- `ProfileEditorScreen.kt` and `SongCollectionScreen.kt` lack screen-state previews. Add English and Persian content, loading, empty, and error coverage where applicable.
- `ChatPickerScreen` has no loading/error preview; `UserProfileScreen` has no loading/empty preview. Add these states.
- Preview callbacks are still empty in several component/app-shell previews. Use `remember` state and visible feedback where interaction matters, per the project preview rules.
- There are no screenshot tests. Add Compose Preview Screenshot Testing (or the project-approved equivalent) before visual regressions are expensive. At minimum record phone, foldable, tablet, and desktop cases for the shell, home, playlist grid, chat list/detail, player, and settings.
- Only the app-shell preview uses `@PreviewScreenSizes`; screen previews do not systematically test larger form factors. The adaptive navigation suite is a good foundation, but content still needs width-aware verification.

## Design-system and adaptive refactors

- Most layout spacing uses `MaterialTheme.spacing`, but several reusable/screen components hardcode dp sizes and shapes (for example `MiniPlayer`, chat bubbles/cards, avatars, artwork, auth indicators, and playlist artwork). Move repeated dimensions to the spacing/component-token system and use `MaterialTheme.shapes` for semantic shapes.
- Preserve `start`/`end` usage; the observed UI is mostly RTL-safe. Visually inspect Persian previews for icons that need auto-mirroring and for mixed Latin/Persian text truncation.
- For adaptive content, use `GridCells.Adaptive` for playlist grids and evaluate two-pane list/detail layouts only after team approval. Navigation 3 is explicitly disallowed by `docs/group-decisions.md`, so retain Navigation Compose 2.
- The current navigation suite adapts the main navigation area, but no screen-level tablet/foldable tests prove it. Add those tests before claiming adaptive support.

## Suggested implementation order

1. Fix the two discarded Flow emissions, Welcome callbacks, and password-recovery flow; add unit tests for each.
2. Repair logout/auth-stack behavior, typed follow-list arguments, album handling, and premium download gating.
3. Break the root graph into feature graphs and introduce one-time UI events.
4. Change search, playlists, downloads (if large), follow lists, and chat history to their agreed Paging contracts while repository owners integrate their sources.
5. Close preview gaps, add screenshot coverage across form factors, then run device QA in English/dark, English/light, Persian/dark, and Persian/light.

## Verification performed

`./gradlew testDebugUnitTest` completed successfully on the reviewed `ui` branch (33 tasks; 14 executed, 19 up-to-date). This verifies compilation and existing unit tests, not runtime behavior, previews, accessibility, or real backend/realtime integration.
