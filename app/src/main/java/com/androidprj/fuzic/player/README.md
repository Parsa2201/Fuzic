# Fuzic Player Package

This package is the **Playback Track** deliverable for the Fuzic app. It owns the
background `MediaSessionService` that hosts a Media3 `ExoPlayer` and exposes it
to the rest of the app and the system via a `MediaSession`. UI, repositories,
queues, persistence, and visualizer code do not live here.

This increment (`feature/media3-wire`) wires the minimum required surface so
later playback increments can build on a verified foundation. It deliberately
contains **no playback UI, no controller, no Room, no DataStore, no queue, no
timer, and no visualizer.**

## Current scope

| Concern | Owner in this increment |
|---|---|
| Foreground playback service | `FuzicPlaybackService` |
| Media3 1.10.1 dependency aliases | `gradle/libs.versions.toml` |
| Media3 implementation deps | `app/build.gradle.kts` |
| Permissions + service declaration | `app/src/main/AndroidManifest.xml` |

### `FuzicPlaybackService`

```kotlin
class FuzicPlaybackService : MediaSessionService() {
    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus = */ true)
            .build()
        exoPlayer = player
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        exoPlayer?.release()
        exoPlayer = null
        super.onDestroy()
    }
}
```

Lifecycle invariants:

- The service is a singleton bound by the system when a controller wants to
  play media. `onCreate` runs once per service process; `onDestroy` runs when
  the last controller unbinds and the service is stopped.
- `ExoPlayer` is constructed with default `AudioAttributes` and
  `handleAudioFocus = true`, so the platform will automatically pause the
  player when another app requests focus (e.g. a phone call).
- `MediaSession` wraps the `ExoPlayer` and is returned from `onGetSession`.
  Media3 uses this to expose transport controls, metadata, and the
  notification surface.
- `onDestroy` releases the session **before** the player, because the session
  holds a reference to the player. Both fields are nulled after release.
- No `MediaLibraryService` is used here; the service is a plain
  `MediaSessionService` because Fuzic has no media-browser tree in this
  increment.

## Media3 dependencies

All four artifacts are pinned to **Media3 1.10.1** via a single `media3`
version in `gradle/libs.versions.toml`:

```toml
media3 = "1.10.1"
androidx-media3-exoplayer    = { group = "androidx.media3", name = "media3-exoplayer",    version.ref = "media3" }
androidx-media3-session      = { group = "androidx.media3", name = "media3-session",      version.ref = "media3" }
androidx-media3-ui-compose   = { group = "androidx.media3", name = "media3-ui-compose",   version.ref = "media3" }
androidx-media3-common       = { group = "androidx.media3", name = "media3-common",       version.ref = "media3" }
```

Wired into `app/build.gradle.kts`:

```kotlin
implementation(libs.androidx.media3.exoplayer)
implementation(libs.androidx.media3.session)
implementation(libs.androidx.media3.ui.compose)
implementation(libs.androidx.media3.common)
```

`media3-ui-compose` is included now so later mini-player / full-player UI
increments do not need another Gradle change.

## Manifest surface

`app/src/main/AndroidManifest.xml` adds the four playback permissions and
declares the service. Exactly one `INTERNET` permission is preserved.

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.WAKE_LOCK" />

<service
    android:name=".player.FuzicPlaybackService"
    android:exported="true"
    android:foregroundServiceType="mediaPlayback">
    <intent-filter>
        <action android:name="androidx.media3.session.MediaSessionService" />
    </intent-filter>
</service>
```

The `MediaSessionService` intent action is what `MediaController` clients use
to bind to the service. `foregroundServiceType="mediaPlayback"` is required
on Android 14+ for the eventual foreground notification that Media3 will
post when audio actually starts playing.

## Intentionally deferred

The following items are **out of scope** for this increment and will be added
by later playback-track increments, in line with the lane ownership in
`AGENTS.md`:

- `PlayerController` singleton / DI exposure and `MediaController` client.
- Queue management, shuffle, repeat, seek, play/pause/next/previous wiring.
- Media item sources, streaming URLs, local file / download integration with
  the `downloads` Room table.
- Notification action handling and lock-screen controls (Media3 wires these
  automatically once the service starts playing).
- Mini-player and full-player Composables (`media3-ui-compose` is already
  present for this).
- Playback state observation, `PlaybackState` flow, and ViewModel integration.
- Sleep timer, playback speed, like, share-to-chat, add-to-playlist actions.
- Canvas-drawn visualizer.
- Audio focus loss / transient loss handlers beyond the default
  `handleAudioFocus = true` behaviour.
- Tests beyond the default `:app:testDebugUnitTest` smoke run; service-level
  instrumentation tests will live in a later increment.

## Verifying this increment

The verified command for this worktree is:

```sh
ANDROID_HOME=/home/grandmaster/Sdk \
ANDROID_SDK_ROOT=/home/grandmaster/Sdk \
JAVA_HOME=/opt/android-studio/jbr \
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

This worktree ships no `local.properties` and `gradle.properties` does not
pin `sdk.dir`, so the Android SDK and JDK locations must be supplied through
the environment. After rebasing onto the current `origin/master`,
`:app:assembleDebug` and `:app:testDebugUnitTest` pass. `:app:lintDebug`
reproduces the existing `WrongNavigateRouteType` failures in
`FuzicNavigation.kt`; the same failures occur on an untouched `origin/master`
checkout, and this increment adds no new lint errors. A clean run of all three
remains the merge target once those baseline navigation errors are fixed.
