-- Create users table extending auth.users
CREATE TABLE public.users (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT,
    avatar_url TEXT,
    is_premium BOOLEAN DEFAULT false
);

-- Enable RLS for users
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;

-- Create songs table
CREATE TABLE public.songs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    artist_name TEXT NOT NULL,
    cover_image_url TEXT,
    audio_url TEXT,
    play_count INT DEFAULT 0,
    release_date TIMESTAMPTZ DEFAULT now()
);

-- Enable RLS for songs
ALTER TABLE public.songs ENABLE ROW LEVEL SECURITY;

-- Create playlists table
CREATE TABLE public.playlists (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    cover_image_url TEXT,
    owner_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
    type TEXT,
    is_public BOOLEAN DEFAULT false
);

-- Enable RLS for playlists
ALTER TABLE public.playlists ENABLE ROW LEVEL SECURITY;

-- Create playlist_songs pivot table
CREATE TABLE public.playlist_songs (
    playlist_id UUID REFERENCES public.playlists(id) ON DELETE CASCADE,
    song_id UUID REFERENCES public.songs(id) ON DELETE CASCADE,
    PRIMARY KEY (playlist_id, song_id)
);

-- Enable RLS for playlist_songs
ALTER TABLE public.playlist_songs ENABLE ROW LEVEL SECURITY;

-- Create interactions table
CREATE TABLE public.interactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
    song_id UUID REFERENCES public.songs(id) ON DELETE CASCADE,
    interaction_type TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Enable RLS for interactions
ALTER TABLE public.interactions ENABLE ROW LEVEL SECURITY;

-- Create follows table
CREATE TABLE public.follows (
    follower_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
    followee_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT now(),
    PRIMARY KEY (follower_id, followee_id)
);

-- Enable RLS for follows
ALTER TABLE public.follows ENABLE ROW LEVEL SECURITY;

-- Create messages table
CREATE TABLE public.messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
    receiver_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
    content TEXT,
    shared_song_id UUID REFERENCES public.songs(id) ON DELETE SET NULL,
    status TEXT DEFAULT 'sent',
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Enable RLS for messages
ALTER TABLE public.messages ENABLE ROW LEVEL SECURITY;

-- Create a function to handle new user signups
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER SET search_path = ''
AS $$
BEGIN
    -- Insert a new row into public.users
    INSERT INTO public.users (id, name, avatar_url, is_premium)
    VALUES (
        new.id,
        new.raw_user_meta_data->>'full_name',
        new.raw_user_meta_data->>'avatar_url',
        false
    );
    
    -- Return the new user record
    RETURN new;
END;
$$;

-- Create a trigger to call the function on new user signup
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE PROCEDURE public.handle_new_user();

-- RLS Policies

-- Public profiles are viewable by everyone
CREATE POLICY "Public profiles are viewable by everyone."
    ON public.users FOR SELECT
    USING (true);

-- Users can update own profile
CREATE POLICY "Users can update own profile."
    ON public.users FOR UPDATE
    USING (auth.uid() = id)
    WITH CHECK (auth.uid() = id);

-- Songs are viewable by everyone
CREATE POLICY "Songs are viewable by everyone."
    ON public.songs FOR SELECT
    USING (true);

-- Users can select own messages
CREATE POLICY "Users can select own messages."
    ON public.messages FOR SELECT
    USING (auth.uid() = sender_id OR auth.uid() = receiver_id);

-- Users can insert own messages
CREATE POLICY "Users can insert own messages."
    ON public.messages FOR INSERT
    WITH CHECK (auth.uid() = sender_id OR auth.uid() = receiver_id);

-- Users can view own interactions
CREATE POLICY "Users can view own interactions."
    ON public.interactions FOR SELECT
    USING (auth.uid() = user_id);

-- Users can insert own interactions
CREATE POLICY "Users can insert own interactions."
    ON public.interactions FOR INSERT
    WITH CHECK (auth.uid() = user_id);

-- Users can delete own interactions
CREATE POLICY "Users can delete own interactions."
    ON public.interactions FOR DELETE
    USING (auth.uid() = user_id);

-- Storage Buckets Setup

-- Insert covers bucket
INSERT INTO storage.buckets (id, name, public)
VALUES ('covers', 'covers', true)
ON CONFLICT (id) DO NOTHING;

-- Insert audio_files bucket
INSERT INTO storage.buckets (id, name, public)
VALUES ('audio_files', 'audio_files', true)
ON CONFLICT (id) DO NOTHING;

-- Storage Policies for covers bucket

-- Allow public read access for covers
CREATE POLICY "Public Read Access for covers"
    ON storage.objects FOR SELECT
    USING (bucket_id = 'covers');

-- Allow authenticated upload access for covers
CREATE POLICY "Authenticated Upload Access for covers"
    ON storage.objects FOR INSERT
    WITH CHECK (bucket_id = 'covers' AND auth.role() = 'authenticated');

-- Storage Policies for audio_files bucket

-- Allow public read access for audio_files
CREATE POLICY "Public Read Access for audio_files"
    ON storage.objects FOR SELECT
    USING (bucket_id = 'audio_files');

-- Allow authenticated upload access for audio_files
CREATE POLICY "Authenticated Upload Access for audio_files"
    ON storage.objects FOR INSERT
    WITH CHECK (bucket_id = 'audio_files' AND auth.role() = 'authenticated');
