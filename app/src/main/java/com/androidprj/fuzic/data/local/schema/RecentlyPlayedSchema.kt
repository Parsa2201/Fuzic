package com.androidprj.fuzic.data.local.schema

/**
 * `recently_played` — Room table preview (Sina-owned per AGENTS.md Local Data §17).
 *
 * Source-of-truth schema for the `recently_played` Room table that backs
 * [com.androidprj.fuzic.repository.InteractionRepository.recordPlay] on
 * the offline path. Currently `Media3PlayerRepository.recordPlayListener`
 * calls the remote Supabase impl; this schema lands in the
 * Bagher-owned `backend-integration` branch when Sina defines the
 * offline behaviour.
 *
 * Per the spec, a song is recorded **once per playback session** (~5 s
 * threshold) so each row represents a single listening event, not a
 * per-playback frame. This keeps the cache small and matches the UX
 * shown in the Recently Played screen.
 *
 * ## Schema
 *
 * ```sql
 * CREATE TABLE recently_played (
 *   songId             TEXT NOT NULL PRIMARY KEY,
 *   lastPlayedAtEpochMs INTEGER NOT NULL,
 *   playCount          INTEGER NOT NULL DEFAULT 1,
 *   userId             TEXT NOT NULL DEFAULT ''
 * );
 *
 * CREATE INDEX recently_played_played_at_idx ON recently_played(lastPlayedAtEpochMs DESC);
 * CREATE INDEX recently_played_user_played_at_idx ON recently_played(userId, lastPlayedAtEpochMs DESC);
 * ```
 *
 * ## Notes
 *
 * - `(songId)` is the Supabase song id (string). The most-recent
 *   transition overwrites the row and increments `playCount`.
 * - `lastPlayedAtEpochMs` is the timestamp of the most recent playback
 *   (millis from `System.currentTimeMillis()`).
 * - `playCount` is a small counter the UI can show as a hint
 *   ("played 12 times"), capped at `INT_MAX` or rolled over to avoid
 *   overflow.
 * - `userId` is the auth user id (default empty until we know the
 *   remote mapping is settled).
 * - The descending index supports the Recently Played screen sorting
 *   "most recently played first". A user-scoped composite index covers
 *   the per-user filter used by the screen.
 */
object RecentlyPlayedSchema {
    const val TABLE_NAME: String = "recently_played"

    const val COLUMN_SONG_ID: String = "songId"
    const val COLUMN_LAST_PLAYED_AT_EPOCH_MS: String = "lastPlayedAtEpochMs"
    const val COLUMN_PLAY_COUNT: String = "playCount"
    const val COLUMN_USER_ID: String = "userId"

    const val INDEX_PLAYED_AT_DESC: String = "recently_played_played_at_idx"
    const val INDEX_USER_PLAYED_AT_DESC: String = "recently_played_user_played_at_idx"
}
