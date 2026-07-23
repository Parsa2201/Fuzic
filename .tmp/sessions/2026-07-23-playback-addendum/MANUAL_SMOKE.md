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

## 3. Crossfade primitives (increment 05)

The full dual-player MediaSession swap is intentionally deferred to a
follow-up increment to keep the FuzicPlaybackService stable. Verified:

- [ ] `VolumeRamp` math: unit-tested (see VolumeRampTest).
- [ ] `CrossfadingPlayer` wraps `ForwardingSimpleBasePlayer` and exposes
      `swapTo(other)` — code-reviewed in
      `app/src/main/java/com/androidprj/fuzic/player/crossfade/CrossfadingPlayer.kt`.
- [ ] `CrossfadeController.setCrossfadeDurationMs(0)` is the default;
      the controller's `onMediaItemTransition` returns early when
      `durationMs == 0`, so no behavior change against pre-merge.
- [ ] `PlayerRepository.setCrossfadeDurationMs(ms)` is exposed (line in
      PlayerRepository.kt) and routed via the singleton
      `CrossfadeController`. Future UI Track increments can drive it
      without service-side surgery.

To exercise the dual-player wiring manually:

- [ ] Implement `CrossfadeController.attach(primary, secondary, wrapper)`
      from a follow-up FuzicPlaybackService.onCreate change: build TWO
      ExoPlayer instances with the same `MediaSourceFactory`, build a
      `CrossfadingPlayer(playerA)`, bind `MediaSession.Builder(this, crossfadingPlayer)`.
- [ ] Add a `Player.Listener` on the wrapper that triggers
      `crossfadeController.onMediaItemTransition(mediaItem, wrapper.repeatMode)`.
- [ ] Set `crossfadeDurationMs = 6_000` from a debug-only settings hook.
- [ ] Queue two tracks with substantial runtime, observe a 6-second
      overlap in audio meter logs.
