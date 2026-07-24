CREATE TABLE public.typing (
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    participant_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    is_typing BOOLEAN NOT NULL DEFAULT false,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, participant_id),
    CONSTRAINT typing_participants_are_distinct CHECK (user_id <> participant_id)
);

ALTER TABLE public.typing ENABLE ROW LEVEL SECURITY;

CREATE FUNCTION public.set_typing_updated_at()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = ''
AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$;

CREATE TRIGGER set_typing_updated_at
    BEFORE UPDATE ON public.typing
    FOR EACH ROW EXECUTE FUNCTION public.set_typing_updated_at();

CREATE POLICY "Conversation participants can read typing status."
    ON public.typing FOR SELECT TO authenticated
    USING (user_id = (SELECT auth.uid()) OR participant_id = (SELECT auth.uid()));

CREATE POLICY "Users can create their own typing status."
    ON public.typing FOR INSERT TO authenticated
    WITH CHECK (
        user_id = (SELECT auth.uid())
        AND participant_id <> (SELECT auth.uid())
    );

CREATE POLICY "Users can update their own typing status."
    ON public.typing FOR UPDATE TO authenticated
    USING (user_id = (SELECT auth.uid()))
    WITH CHECK (
        user_id = (SELECT auth.uid())
        AND participant_id <> (SELECT auth.uid())
    );

ALTER PUBLICATION supabase_realtime ADD TABLE public.typing;
