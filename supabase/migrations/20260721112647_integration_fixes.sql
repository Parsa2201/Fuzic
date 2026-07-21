-- Enable realtime broadcasts for the messages table
ALTER PUBLICATION supabase_realtime ADD TABLE public.messages;

-- Add interaction type constraint
ALTER TABLE public.interactions ADD CONSTRAINT check_interaction_type CHECK (interaction_type IN ('play', 'like'));

-- Add unique active like constraint per user per song
CREATE UNIQUE INDEX unique_active_like ON public.interactions(user_id, song_id) WHERE interaction_type = 'like';

-- Add message status constraint
ALTER TABLE public.messages ADD CONSTRAINT check_message_status CHECK (status IN ('sending', 'sent', 'delivered', 'read', 'failed'));

-- Add check to prevent self-following
ALTER TABLE public.follows ADD CONSTRAINT check_no_self_follow CHECK (follower_id <> followee_id);

-- Update messages INSERT policy to securely enforce sender_id check
DROP POLICY "Users can insert own messages." ON public.messages;
CREATE POLICY "Users can insert own messages."
    ON public.messages FOR INSERT
    WITH CHECK ((SELECT auth.uid()) = sender_id);

-- Drop broad storage listing policies for public buckets to fix security warnings
DROP POLICY "Public Read Access for covers" ON storage.objects;
DROP POLICY "Public Read Access for audio_files" ON storage.objects;
