-- Create a view for easy querying of recent conversations per user
CREATE OR REPLACE VIEW public.recent_conversations AS
WITH ranked_messages AS (
    SELECT 
        m.conversation_id,
        m.sender_id,
        m.receiver_id,
        m.content,
        m.created_at,
        m.status,
        ROW_NUMBER() OVER (PARTITION BY m.conversation_id ORDER BY m.created_at DESC) as rn
    FROM public.messages m
),
latest_messages AS (
    SELECT * FROM ranked_messages WHERE rn = 1
),
unread_counts AS (
    SELECT 
        conversation_id,
        receiver_id AS user_id,
        COUNT(*) as unread_count
    FROM public.messages
    WHERE status != 'read'
    GROUP BY conversation_id, receiver_id
)
SELECT 
    lm.conversation_id,
    u.id AS user_id,
    ou.id AS other_user_id,
    ou.name AS other_user_name,
    ou.username AS other_user_username,
    ou.avatar_url AS other_user_avatar_url,
    lm.content AS last_message_preview,
    lm.created_at AS last_message_time,
    COALESCE(uc.unread_count, 0) AS unread_count
FROM latest_messages lm
CROSS JOIN LATERAL (
    SELECT lm.sender_id AS id UNION SELECT lm.receiver_id AS id
) u
JOIN public.users ou ON ou.id = CASE 
    WHEN u.id = lm.sender_id THEN lm.receiver_id 
    ELSE lm.sender_id 
END
LEFT JOIN unread_counts uc ON uc.conversation_id = lm.conversation_id AND uc.user_id = u.id;

-- Grant access
GRANT SELECT ON public.recent_conversations TO authenticated, anon;
