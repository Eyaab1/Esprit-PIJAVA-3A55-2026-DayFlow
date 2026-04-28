-- Query to check moderation incident logs in the database

-- 1. Count total moderation incidents
SELECT COUNT(*) as total_incidents 
FROM moderation_incident;

-- 2. View recent moderation incidents (last 10)
SELECT 
    id,
    created_at,
    user_email,
    entity_type,
    content_preview,
    highest_attribute,
    highest_score,
    threshold_used,
    incident_status
FROM moderation_incident
ORDER BY created_at DESC
LIMIT 10;

-- 3. Count incidents by entity type
SELECT 
    entity_type,
    COUNT(*) as count
FROM moderation_incident
GROUP BY entity_type
ORDER BY count DESC;

-- 4. Count incidents by user
SELECT 
    user_email,
    COUNT(*) as incident_count
FROM moderation_incident
WHERE user_email IS NOT NULL
GROUP BY user_email
ORDER BY incident_count DESC;

-- 5. View incidents with highest toxicity scores
SELECT 
    created_at,
    user_email,
    entity_type,
    content_preview,
    highest_attribute,
    highest_score
FROM moderation_incident
WHERE highest_score > 0.8
ORDER BY highest_score DESC
LIMIT 10;

-- 6. Check if table exists and has correct structure
SELECT 
    column_name,
    data_type,
    is_nullable
FROM information_schema.columns
WHERE table_name = 'moderation_incident'
ORDER BY ordinal_position;

-- 7. View most recent incident with full details
SELECT *
FROM moderation_incident
ORDER BY created_at DESC
LIMIT 1;
