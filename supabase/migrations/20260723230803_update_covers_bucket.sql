-- Allow users to update their own cover images
CREATE POLICY "Users can update their cover objects"
    ON storage.objects FOR UPDATE TO authenticated
    USING (
        bucket_id = 'covers'
        AND (storage.foldername(name))[1] = (select auth.uid()::text)
    )
    WITH CHECK (
        bucket_id = 'covers'
        AND (storage.foldername(name))[1] = (select auth.uid()::text)
    );

-- Allow users to delete their own cover images
CREATE POLICY "Users can delete their cover objects"
    ON storage.objects FOR DELETE TO authenticated
    USING (
        bucket_id = 'covers'
        AND (storage.foldername(name))[1] = (select auth.uid()::text)
    );
