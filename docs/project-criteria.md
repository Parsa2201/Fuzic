### 1. Core Standards & Development Guidelines

* **Design System:** Hardcoding colors, sizes, and dimensions is strictly prohibited. A custom `Typography`, `Shapes`, `Colors`, and `Padding/Margin` must be defined using Material Theme.


* **Clean Code:** Direct text hardcoding is forbidden; all strings must be extracted from the `strings.xml` file.


* **Localization:** The application must fully support two languages (Persian and English). The layout must automatically adapt to RTL for Persian and LTR for English.


* **Theme Support:** Both Dark and Light modes must be supported, togglable via system settings or in-app preferences. The color palette must be optimized for readability in both modes.


* **App Icon:** An exclusive Adaptive Icon is required; default Android icons are not accepted.



### 2. Architecture & Technical Infrastructure

* **Architecture:** Strict adherence to Clean Architecture principles, properly separating Data, Domain, and UI layers.


* **Design Pattern:** Implementation of MVVM or MVI approaches using Unidirectional Data Flow (UDF).


* **State Management:** Screen states must be aggregated into a `UiState` data class, managed via `StateFlow`, and passed down to Compose functions.


* **Event Handling:** User interactions must be sent to the ViewModel as Intents or Events. One-time events (like network error Snackbars) must be handled using `SharedFlow` or `Channel`.


* **Dependency Injection:** Utilizing Hilt or Koin is mandatory for injecting Repositories, Use Cases, the database, and the network client.


* **List Management:** The Paging 3 library is mandatory for all long lists (search results, playlist songs, chat history).


* **Thread Safety:** All network and database operations must be executed on `Dispatchers.IO` using Coroutines, strictly avoiding the Main Thread.



### 3. Backend & Initial Data

* **Backend Implementation:** A custom backend is required (either using BaaS like Supabase/Firebase or custom code via Ktor/Node.js).


* **Initial Content:** The database must be seeded with a minimum of 50 real songs.


* **Song Metadata:** Required fields include `id`, `title`, `artist name`, `cover_image_url`, and `audio_url`.



### 4. UI/UX in Jetpack Compose

* **Visual Elements:** The UI must be modern and fluid, featuring scale effects on clicks and smooth transitions.


* **Top Bar:** The main screens must display the app logo and name on the right, and the user's avatar, notification bell, and settings gear on the left.


* **Shimmer Effect:** A skeleton loading animation (Shimmer) is required for lists and cards before data is fetched.


* **Screen Transitions:** Shared Element Transitions must be used for smooth navigation between the mini-player and the expanded Now Playing screen.



### 5. Main Application Tabs

* **Home Tab - Carousel:** A moving horizontal slider at the top for daily suggestions, new albums, or trending tracks.


* **Home Tab - Quick Actions:** Four buttons for Liked Songs, Recently Played, My Playlists, and Top Artists.


* **Home Tab - Sliders:** Horizontal scrolling lists (`LazyRow`) for Most Popular, Newest, Global Playlists, and Local Playlists.


* **Search Tab - Functionality:** Real-time search with an appropriate delay using the `debounce` operator in Flow.


* **Search Tab - Filters:** Filter chips for narrowing down results by song, artist, etc.


* **Search Tab - History:** Display of past search queries stored in Room, with deletion capabilities.


* **Downloads Tab:** A list of offline songs with sorting and Swipe-to-Dismiss deletion features.


* **Playlists Tab:** A two-column layout using `LazyVerticalGrid` to display categorized colored cards (Global, Local, User Playlists).


* **Profile Tab:** Displays the user avatar (changeable), a Premium Status badge, and a subscription renewal button that mocks upgrading to Premium (`true`).


* **Profile Tab - Settings:** Access to app settings, theme toggle, and language preferences.



### 6. Advanced Media Playback

* **Background Service:** Music must play via a background service (`ExoPlayer` and `MediaSession`) to prevent interruption upon app exit.


* **Audio Focus:** Smart audio handling to temporarily pause or duck volume during incoming calls or system sounds, resuming automatically.


* **Smart Caching:** Utilizing `ExoPlayer CacheDataSource` to save streamed files locally, preventing redundant data usage on replays.


* **Crossfade:** Smoothly fading out the end of a track while blending it with the beginning of the next.


* **Notification Controls:** Playback status and Play/Pause/Next controls must be visible in the system notification tray and lock screen.


* **Mini Player:** A persistent floating bar above the Bottom Navigation showing the currently playing track.


* **Now Playing Screen - Cover:** A rotating CD-style record in the center that spins during playback and pauses when stopped.


* **Now Playing Screen - Dynamic Colors:** The background color must dynamically adapt to the song cover's dominant color using the Palette API.


* **Now Playing Screen - Visualizer:** An animated audio waveform or equalizer custom-drawn with Compose `Canvas` (Lottie or GIFs are forbidden).


* **Now Playing Screen - Sleep Timer:** Coroutine-based timer to automatically stop playback after a user-defined period.


* **Now Playing Screen - Playback Speed:** Functionality to change playback speed to `1.5` or `2` via ExoPlayer controllers.



### 7. Business Logic

* **Premium Users:** All users can listen to unlimited music; however, the offline download button is strictly restricted to Premium users, prompting an upgrade message for standard users.


* **Download Management:** Background file downloading for Premium users must be managed via `WorkManager`.


* **Smart Playback:** The Repository layer must seamlessly play downloaded local files instead of fetching them from the network if available.



### 8. Social Network & Real-time Chat

* **User Following:** The ability to search for friends, follow them, and view/play their public playlists.


* **Live Connection:** Direct Messaging (DM) must utilize WebSockets (or similar real-time connections) instead of Polling.


* **Message Receipts:** Support for message statuses including "sending", "sent" (single tick), and "read" (double tick).


* **Typing Indicator:** Real-time display of the `is typing...` status.


* **Song Sharing:** Sending a track to a friend via a custom UI Mini Card that can be played directly within the chat.


* **Offline Chat:** Saving received messages in the Room database for offline accessibility.



### 9. Local Data Management

* **Room Database:** Mandatory integration for storing search history, liked songs, downloaded file paths, and offline message history.


* **DataStore Preferences:** Utilized for saving application settings such as Dark Mode, font size, language, and Premium status.



### 10. Secondary Screens & Developer Autonomy

* **Liked/Recent Tracks Screens:** Must feature an attractive header, a Shuffle/Play All button, and Swipe-to-Dismiss item removal animations.


* **Following/Artists List:** A clean grid or list interface displaying followed accounts with an Unfollow button.


* **Settings Screen:** Standard page for dynamic RTL/LTR language toggling, Dark/Light theme switching, and account logout.


* **Empty States:** Appropriate UI illustrations or animations for empty playlists, zero search results, or offline network errors.



### 11. Final Deliverables

* **Application File:** The compiled APK file (source code upload is not required).


* **Showcase Video:** A short video demonstrating the application's functionality.