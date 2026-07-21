-- Enable realtime broadcasts for the messages table
-- ALTER PUBLICATION supabase_realtime ADD TABLE public.messages;

-- Restore SELECT policies for storage buckets to ensure users can download songs and covers
CREATE POLICY "Public Read Access for covers" ON storage.objects FOR SELECT USING (bucket_id = 'covers');
CREATE POLICY "Public Read Access for audio_files" ON storage.objects FOR SELECT USING (bucket_id = 'audio_files');
