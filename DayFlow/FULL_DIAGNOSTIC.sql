-- FULL DIAGNOSTIC FOR GOAL PARTICIPATION ISSUE

-- 1. Check current data
SELECT '=== CURRENT DATA ===' as step;
SELECT DISTINCT role FROM goal_participation ORDER BY role;
SELECT DISTINCT status FROM goal_participation ORDER BY status;

-- 2. Check constraint status
SELECT '=== CONSTRAINT STATUS ===' as step;
SELECT con.conname, con.contype, con.convalidated
FROM pg_constraint con
WHERE con.conrelid = 'goal_participation'::regclass;

-- 3. Check for invalid data
SELECT '=== INVALID DATA ===' as step;
SELECT * FROM goal_participation WHERE role NOT IN ('owner', 'admin', 'member');
SELECT * FROM goal_participation WHERE status NOT IN ('pending', 'accepted', 'rejected');

-- 4. If there's invalid data, fix it
SELECT '=== FIXING DATA ===' as step;
UPDATE goal_participation SET role = 'owner' WHERE role = 'OWNER';
UPDATE goal_participation SET role = 'admin' WHERE role = 'ADMIN';
UPDATE goal_participation SET role = 'member' WHERE role = 'MEMBER';
UPDATE goal_participation SET status = 'accepted' WHERE status = 'APPROVED';
UPDATE goal_participation SET status = 'pending' WHERE status = 'PENDING';
UPDATE goal_participation SET status = 'rejected' WHERE status = 'REJECTED';

-- 5. Validate constraints
SELECT '=== VALIDATING CONSTRAINTS ===' as step;
ALTER TABLE goal_participation VALIDATE CONSTRAINT chk_participation_role;
ALTER TABLE goal_participation VALIDATE CONSTRAINT chk_participation_status;

-- 6. Verify fix
SELECT '=== VERIFICATION ===' as step;
SELECT DISTINCT role FROM goal_participation ORDER BY role;
SELECT DISTINCT status FROM goal_participation ORDER BY status;
SELECT * FROM goal_participation WHERE role NOT IN ('owner', 'admin', 'member');
SELECT * FROM goal_participation WHERE status NOT IN ('pending', 'accepted', 'rejected');

SELECT '=== DONE ===' as step;
