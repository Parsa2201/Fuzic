-- 1. Missing RLS Policies
-- public.follows
CREATE POLICY "Authenticated users can view follows" ON public.follows FOR SELECT TO authenticated USING (true);
CREATE POLICY "Users can insert own follows" ON public.follows FOR INSERT TO authenticated WITH CHECK (follower_id = (select auth.uid()));
CREATE POLICY "Users can delete own follows" ON public.follows FOR DELETE TO authenticated USING (follower_id = (select auth.uid()));

-- public.playlists
CREATE POLICY "Authenticated users can view public or own playlists" ON public.playlists FOR SELECT TO authenticated USING (is_public = true OR owner_id = (select auth.uid()));
CREATE POLICY "Users can insert own playlists" ON public.playlists FOR INSERT TO authenticated WITH CHECK (owner_id = (select auth.uid()));
CREATE POLICY "Users can update own playlists" ON public.playlists FOR UPDATE TO authenticated USING (owner_id = (select auth.uid())) WITH CHECK (owner_id = (select auth.uid()));
CREATE POLICY "Users can delete own playlists" ON public.playlists FOR DELETE TO authenticated USING (owner_id = (select auth.uid()));

-- public.playlist_songs
CREATE POLICY "Authenticated users can view songs of public or own playlists" ON public.playlist_songs FOR SELECT TO authenticated USING (
  EXISTS (
    SELECT 1 FROM public.playlists p 
    WHERE p.id = playlist_songs.playlist_id AND (p.is_public = true OR p.owner_id = (select auth.uid()))
  )
);
CREATE POLICY "Users can insert songs to own playlists" ON public.playlist_songs FOR INSERT TO authenticated WITH CHECK (
  EXISTS (
    SELECT 1 FROM public.playlists p 
    WHERE p.id = playlist_songs.playlist_id AND p.owner_id = (select auth.uid())
  )
);
CREATE POLICY "Users can delete songs from own playlists" ON public.playlist_songs FOR DELETE TO authenticated USING (
  EXISTS (
    SELECT 1 FROM public.playlists p 
    WHERE p.id = playlist_songs.playlist_id AND p.owner_id = (select auth.uid())
  )
);

-- 2. Public Bucket Allows Listing
DROP POLICY IF EXISTS "Public Read Access for covers" ON storage.objects;
DROP POLICY IF EXISTS "Public Read Access for audio_files" ON storage.objects;

-- 3. SECURITY DEFINER Executable
REVOKE EXECUTE ON FUNCTION public.handle_new_user() FROM PUBLIC, anon, authenticated;

-- 4. Auth RLS Initialization Plan
-- users
DROP POLICY IF EXISTS "Users can update own profile." ON public.users;
CREATE POLICY "Users can update own profile." ON public.users FOR UPDATE TO authenticated USING (id = (select auth.uid())) WITH CHECK (id = (select auth.uid()));

-- messages
DROP POLICY IF EXISTS "Users can select own messages." ON public.messages;
CREATE POLICY "Users can select own messages." ON public.messages FOR SELECT TO authenticated USING (sender_id = (select auth.uid()) OR receiver_id = (select auth.uid()));

DROP POLICY IF EXISTS "Users can insert own messages." ON public.messages;
CREATE POLICY "Users can insert own messages." ON public.messages FOR INSERT TO authenticated WITH CHECK (sender_id = (select auth.uid()) OR receiver_id = (select auth.uid()));

-- interactions
DROP POLICY IF EXISTS "Users can view own interactions." ON public.interactions;
CREATE POLICY "Users can view own interactions." ON public.interactions FOR SELECT TO authenticated USING (user_id = (select auth.uid()));

DROP POLICY IF EXISTS "Users can insert own interactions." ON public.interactions;
CREATE POLICY "Users can insert own interactions." ON public.interactions FOR INSERT TO authenticated WITH CHECK (user_id = (select auth.uid()));

DROP POLICY IF EXISTS "Users can delete own interactions." ON public.interactions;
CREATE POLICY "Users can delete own interactions." ON public.interactions FOR DELETE TO authenticated USING (user_id = (select auth.uid()));

-- 5. Unindexed Foreign Keys
CREATE INDEX IF NOT EXISTS idx_follows_followee_id ON public.follows(followee_id);
CREATE INDEX IF NOT EXISTS idx_interactions_song_id ON public.interactions(song_id);
CREATE INDEX IF NOT EXISTS idx_interactions_user_id ON public.interactions(user_id);
CREATE INDEX IF NOT EXISTS idx_messages_receiver_id ON public.messages(receiver_id);
CREATE INDEX IF NOT EXISTS idx_messages_sender_id ON public.messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_messages_shared_song_id ON public.messages(shared_song_id);
CREATE INDEX IF NOT EXISTS idx_playlist_songs_song_id ON public.playlist_songs(song_id);
CREATE INDEX IF NOT EXISTS idx_playlists_owner_id ON public.playlists(owner_id);
