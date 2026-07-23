package com.androidprj.fuzic.data.local.schema

/**
 * `liked_songs` — Room table preview (Sina-owned per AGENTS.md Local Data §17).
 *
 * Source-of-truth schema for the `liked_songs` Room table that backs
 * [com.androidprj.fuzic.repository.InteractionRepository.likeSong] /
 * [unlikeSong] / [getLikedSongs] on the offline path. Currently the
 * repository routes every call through the remote Supabase impl; this
 * schema lands in the Bagher-owned `backend-integration` branch when
 * Sina defines the offline behaviour.
 *
 * ## Schema
 *
 * ```sql
 * CREATE TABLE liked_songs (
 *   songId          TEXT NOT NULL PRIMARY KEY,
 *   likedAtEpochMs  INTEGER NOT NULL,
 *   userId          TEXT NOT NULL DEFAULT ''
 * );
 *
 * CREATE INDEX liked_songs_liked_at_idx ON liked_songs(likedAtEpochMs DESC);
 * CREATE INDEX liked_songs_user_idx ON liked_songs(userId, likedAtEpochMs DESC);
 * ```
 *
 * ## Notes
 *
 * - `(songId)` is the Supabase song id (string). Unique per row —
 *   liking a song twice upserts the timestamp.
 * - `likedAtEpochMs` is wall-clock millis from `System.currentTimeMillis()`.
 * - `userId` is the auth user id (default empty while offline-mode
 *   friendliness is negotiated; rename to `owner_id` later if Bagher
 *   decides we need a composite key).
 * - The descending index supports the Liked Songs screen sorting
 *   "most recently liked first".
 */
object LikedSongsSchema {
    const val TABLE_NAME: String = "liked_songs"

    const val COLUMN_SONG_ID: String = "songId"
    const val COLUMN_LIKED_AT_EPOCH_MS: String = "likedAtEpochMs"
    const val COLUMN_USER_ID: String = "userId"

    const val INDEX_LIKED_AT_DESC: String = "liked_songs_liked_at_idx"
    const val INDEX_USER_LIKED_AT_DESC: String = "liked_songs_user_idx"
}
