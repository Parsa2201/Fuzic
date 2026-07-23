# Manual smoke checklist — feature/playback-addendum

This file tracks the manual verifications the JVM test harness cannot
cover (no Robolectric). Run these in the order listed on a real device
or emulator after pulling feature/playback-addendum.

## 1. SimpleCache (increment 03)

- [ ] Stream a song from `https://…` once. Wait for it to complete.
- [ ] Restart the app. Verify the song plays back from cache (no
      network) by toggling airplane mode and replaying the same song.
      If the song starts within ~500 ms while offline, the cache is
      populated and being read.
- [ ] Open `adb shell ls /data/data/com.androidprj.fuzic/cache/fuzic_media_cache`
      — the directory should exist and contain a SQLite index file.
- [ ] After 20+ streamed tracks, observe disk usage stays under
      `MediaCache.MAX_CACHE_SIZE_BYTES` (256 MB). Use
      `adb shell du -sh /data/data/com.androidprj.fuzic/cache/fuzic_media_cache`.

## 2. Dominant color extraction (increment 04)

- [ ] Play an album with high-saturation cover art (e.g. a red cover).
      In Logcat (filter on `Media3PlayerRepo`), expect a `dominantColor`
      ARGB int to be reported shortly after `onMediaItemTransition`.
- [ ] Play an album with a black-and-white cover. Expect
      `dominantColor = null` (the grey filter rejects every pixel).
- [ ] Play an album with no artwork URL. Expect `dominantColor` to stay
      `null` (the URL null-check short-circuits).

## 3. Crossfade dual-player wiring (increment 06 — `FuzicPlaybackService` now builds two ExoPlayers + `CrossfadingPlayer`)

By default, `CrossfadeController.setCrossfadeDurationMs(0)` keeps the
service in the legacy single-player-effective behaviour. To enable the
dual-player crossfade at runtime the UI Track must call
`PlayerRepository.setCrossfadeDurationMs(ms)` with `ms > 0` (capped at
30 000). Until then, the second ExoPlayer sits idle, costing ~10 MB.

- [ ] Default (crossfade disabled): every transport command works
      exactly as in the merged playback (#8): play, pause, seekTo,
      skipToNext, skipToPrevious, skipToIndex, setPlaybackSpeed,
      setShuffleEnabled, setRepeatMode, setSleepTimer, addToQueue,
      removeFromQueue, clearQueue, stop. Visualizer frames still flow.
      Notification still works.
- [ ] Queue two medium-length tracks (≥ 12 s each). Confirm via
      `adb shell dumpsys media_session` that
      `com.androidprj.fuzic/.player.FuzicPlaybackService` is up.
- [ ] Toggle `setCrossfadeDurationMs(6_000)` from a debug-only hook
      (or wait for the UI Track crossfade toggle). Play the queue. The
      last 6 seconds of track A and the first few seconds of track B
      should overlap audibly. Use `adb shell dumpsys audio` if available
      to confirm two concurrent output streams during the 6 s window.
- [ ] Stop the queue mid-crossfade (tap pause). Both players should
      quiet immediately. Restart — the active player (post-swap) is
      the one that resumes.
- [ ] App kill (recent-apps → swipe up) during a crossfade. Re-launch.
      Playback resumes from the last position without app-side state
      loss.

## 4. Crossfade primitives (already shipped in increment 05)

- [ ] `VolumeRamp` math: unit-tested (see VolumeRampTest, 7 cases).
- [ ] `CrossfadingPlayer` wraps `ForwardingSimpleBasePlayer` and exposes
      `swapTo(other)` — code-reviewed in
      `app/src/main/java/com/androidprj/fuzic/player/crossfade/CrossfadingPlayer.kt`.
- [ ] `CrossfadeController.setCrossfadeDurationMs(0)` is the default;
      the controller's `onMediaItemTransition` returns early when
      `durationMs == 0`, so no behavior change against pre-merge.
- [ ] `PlayerRepository.setCrossfadeDurationMs(ms)` is exposed and routed
      via the singleton `CrossfadeController`. Negative values return
      `Result.failure`; `> 30_000` clamps to `30_000`.

