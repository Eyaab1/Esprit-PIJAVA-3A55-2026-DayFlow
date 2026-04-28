-- Debug script to check reclamation statuses in database

-- 1. Check all unique status values in database
SELECT DISTINCT status, COUNT(*) as count
FROM reclamation
GROUP BY status
ORDER BY status;

-- 2. Check specific "PENDING" reclamations
SELECT id, status, type, created_at, user_id
FROM reclamation
WHERE status = 'PENDING'
ORDER BY created_at DESC
LIMIT 10;

-- 3. Check if there are case variations
SELECT id, status, LENGTH(status) as len, 
       status = 'PENDING' as exact_match,
       status ILIKE 'PENDING' as case_insensitive_match
FROM reclamation
WHERE status ILIKE 'pending'
LIMIT 10;

-- 4. Check for whitespace issues
SELECT id, status, 
       LENGTH(status) as len,
       LENGTH(TRIM(status)) as trimmed_len,
       status = 'PENDING' as exact_match
FROM reclamation
WHERE status ILIKE '%pending%'
LIMIT 10;
