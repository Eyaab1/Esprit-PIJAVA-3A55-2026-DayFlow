-- Check current state of goal_participation table
SELECT 'Current role values:' as check_type;
SELECT DISTINCT role FROM goal_participation ORDER BY role;

SELECT 'Current status values:' as check_type;
SELECT DISTINCT status FROM goal_participation ORDER BY status;

SELECT 'Sample records:' as check_type;
SELECT id, user_id, goal_id, role, status FROM goal_participation LIMIT 10;

SELECT 'Count by role:' as check_type;
SELECT role, COUNT(*) as count FROM goal_participation GROUP BY role ORDER BY role;

SELECT 'Count by status:' as check_type;
SELECT status, COUNT(*) as count FROM goal_participation GROUP BY status ORDER BY status;

-- Check for any invalid values
SELECT 'Invalid role values:' as check_type;
SELECT DISTINCT role FROM goal_participation WHERE role NOT IN ('owner', 'admin', 'member');

SELECT 'Invalid status values:' as check_type;
SELECT DISTINCT status FROM goal_participation WHERE status NOT IN ('pending', 'accepted', 'rejected');
