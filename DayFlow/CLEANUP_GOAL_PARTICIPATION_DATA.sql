-- ============================================================================
-- CLEANUP SCRIPT: Fix Goal Participation Role Values
-- ============================================================================
-- This script fixes any existing goal_participation records that have
-- uppercase role values (OWNER, ADMIN, MEMBER) to match the database
-- constraint which expects lowercase values (owner, admin, member).
-- ============================================================================

-- Step 1: Check current state
SELECT 'Current role values in database:' as step;
SELECT DISTINCT role FROM goal_participation ORDER BY role;

-- Step 2: Count records by role
SELECT 'Count of records by role:' as step;
SELECT role, COUNT(*) as count FROM goal_participation GROUP BY role ORDER BY role;

-- Step 3: Convert uppercase to lowercase
SELECT 'Converting uppercase role values to lowercase...' as step;

UPDATE goal_participation SET role = 'owner' WHERE role = 'OWNER';
UPDATE goal_participation SET role = 'admin' WHERE role = 'ADMIN';
UPDATE goal_participation SET role = 'member' WHERE role = 'MEMBER';

-- Step 4: Verify the fix
SELECT 'Verification - role values after cleanup:' as step;
SELECT DISTINCT role FROM goal_participation ORDER BY role;

SELECT 'Final count of records by role:' as step;
SELECT role, COUNT(*) as count FROM goal_participation GROUP BY role ORDER BY role;

-- Step 5: Check for any invalid status values (should be: pending, accepted, rejected)
SELECT 'Checking status values:' as step;
SELECT DISTINCT status FROM goal_participation ORDER BY status;

-- Step 6: Convert status values if needed
UPDATE goal_participation SET status = 'accepted' WHERE status = 'APPROVED';
UPDATE goal_participation SET status = 'pending' WHERE status = 'PENDING';
UPDATE goal_participation SET status = 'rejected' WHERE status = 'REJECTED';

SELECT 'Final verification - all values:' as step;
SELECT 
    role, 
    status, 
    COUNT(*) as count 
FROM goal_participation 
GROUP BY role, status 
ORDER BY role, status;

-- Step 7: Summary
SELECT 'CLEANUP COMPLETE' as result;
SELECT 'All goal_participation records now have valid lowercase role and status values.' as message;
