-- ============================================================
-- Phase 2 Schema Updates: artists, albums, and missing columns
-- ============================================================

-- 1. Create artists table
CREATE TABLE public.artists (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    avatar_url TEXT,
    bio TEXT,
    monthly_listeners INT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE public.artists ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Artists are viewable by everyone."
    ON public.artists FOR SELECT
    USING (true);

-- 2. Create albums table
CREATE TABLE public.albums (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    artist_id UUID REFERENCES public.artists(id) ON DELETE CASCADE,
    cover_image_url TEXT,
    release_date TIMESTAMPTZ DEFAULT now(),
    created_at TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE public.albums ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Albums are viewable by everyone."
    ON public.albums FOR SELECT
    USING (true);

CREATE INDEX IF NOT EXISTS idx_albums_artist_id ON public.albums(artist_id);

-- 3. Add missing columns to songs
ALTER TABLE public.songs ADD COLUMN IF NOT EXISTS duration_seconds INT;
ALTER TABLE public.songs ADD COLUMN IF NOT EXISTS album_name TEXT;
ALTER TABLE public.songs ADD COLUMN IF NOT EXISTS is_explicit BOOLEAN DEFAULT false;
ALTER TABLE public.songs ADD COLUMN IF NOT EXISTS artist_id UUID REFERENCES public.artists(id) ON DELETE SET NULL;
ALTER TABLE public.songs ADD COLUMN IF NOT EXISTS album_id UUID REFERENCES public.albums(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_songs_artist_id ON public.songs(artist_id);
CREATE INDEX IF NOT EXISTS idx_songs_album_id ON public.songs(album_id);

-- 4. Add description to playlists
ALTER TABLE public.playlists ADD COLUMN IF NOT EXISTS description TEXT;

-- 5. Add chat-related columns to messages
ALTER TABLE public.messages ADD COLUMN IF NOT EXISTS message_type TEXT DEFAULT 'text';
ALTER TABLE public.messages ADD COLUMN IF NOT EXISTS conversation_id TEXT;
ALTER TABLE public.messages ADD COLUMN IF NOT EXISTS delivered_at TIMESTAMPTZ;
ALTER TABLE public.messages ADD COLUMN IF NOT EXISTS read_at TIMESTAMPTZ;

-- Generate a deterministic conversation_id for existing and new messages
-- conversation_id = sorted concatenation of sender_id and receiver_id
CREATE OR REPLACE FUNCTION public.generate_conversation_id()
RETURNS trigger AS $$
BEGIN
    IF NEW.conversation_id IS NULL THEN
        NEW.conversation_id := CASE
            WHEN NEW.sender_id::text < NEW.receiver_id::text
            THEN NEW.sender_id::text || ':' || NEW.receiver_id::text
            ELSE NEW.receiver_id::text || ':' || NEW.sender_id::text
        END;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER set_conversation_id
    BEFORE INSERT ON public.messages
    FOR EACH ROW EXECUTE FUNCTION public.generate_conversation_id();

-- Backfill conversation_id for existing messages
UPDATE public.messages
SET conversation_id = CASE
    WHEN sender_id::text < receiver_id::text
    THEN sender_id::text || ':' || receiver_id::text
    ELSE receiver_id::text || ':' || sender_id::text
END
WHERE conversation_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_messages_conversation_id ON public.messages(conversation_id);

-- 6. Update handle_new_user trigger to also populate username
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER SET search_path = ''
AS $$
BEGIN
    INSERT INTO public.users (id, name, username, avatar_url, is_premium)
    VALUES (
        new.id,
        new.raw_user_meta_data->>'full_name',
        COALESCE(
            new.raw_user_meta_data->>'username',
            LOWER(REPLACE(COALESCE(new.raw_user_meta_data->>'full_name', new.id::text), ' ', '_'))
        ),
        new.raw_user_meta_data->>'avatar_url',
        false
    );
    RETURN new;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.handle_new_user() FROM PUBLIC, anon, authenticated;
