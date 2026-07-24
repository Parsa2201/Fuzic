-- Recipients may change only the status of messages addressed to them.
-- Column privileges prevent a receipt update from altering message content or ownership.
REVOKE UPDATE ON public.messages FROM authenticated;
GRANT UPDATE (status) ON public.messages TO authenticated;

CREATE POLICY "Recipients can mark messages as read."
    ON public.messages FOR UPDATE TO authenticated
    USING (receiver_id = (SELECT auth.uid()))
    WITH CHECK (
        receiver_id = (SELECT auth.uid())
        AND status = 'read'
    );
