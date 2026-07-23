-- User profile pictures live separately from public album covers.  Objects are
-- publicly readable, but only their folder owner may create, replace, or remove them.
INSERT INTO storage.buckets (id, name, public, file_size_limit)
VALUES (
    'avatars',
    'avatars',
    true,
    5242880
)
ON CONFLICT (id) DO UPDATE
SET public = EXCLUDED.public,
    file_size_limit = EXCLUDED.file_size_limit;

CREATE POLICY "Users can read their avatar objects"
    ON storage.objects FOR SELECT TO authenticated
    USING (
        bucket_id = 'avatars'
        AND (storage.foldername(name))[1] = (select auth.uid()::text)
    );

CREATE POLICY "Users can upload their avatar objects"
    ON storage.objects FOR INSERT TO authenticated
    WITH CHECK (
        bucket_id = 'avatars'
        AND (storage.foldername(name))[1] = (select auth.uid()::text)
    );

CREATE POLICY "Users can replace their avatar objects"
    ON storage.objects FOR UPDATE TO authenticated
    USING (
        bucket_id = 'avatars'
        AND (storage.foldername(name))[1] = (select auth.uid()::text)
    )
    WITH CHECK (
        bucket_id = 'avatars'
        AND (storage.foldername(name))[1] = (select auth.uid()::text)
    );

CREATE POLICY "Users can delete their avatar objects"
    ON storage.objects FOR DELETE TO authenticated
    USING (
        bucket_id = 'avatars'
        AND (storage.foldername(name))[1] = (select auth.uid()::text)
    );
