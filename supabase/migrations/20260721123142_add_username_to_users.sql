-- Add username column
ALTER TABLE public.users ADD COLUMN username text UNIQUE;

-- Create function to automatically generate a username from full_name if not provided
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger AS $$
BEGIN
  INSERT INTO public.users (id, name, username, avatar_url)
  VALUES (
    new.id, 
    new.raw_user_meta_data->>'full_name', 
    COALESCE(
      new.raw_user_meta_data->>'username', 
      LOWER(REPLACE(new.raw_user_meta_data->>'full_name', ' ', '_'))
    ),
    new.raw_user_meta_data->>'avatar_url'
  );
  RETURN new;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
