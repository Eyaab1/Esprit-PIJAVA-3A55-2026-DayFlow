-- FIX CONSTRAINT VALIDATION ISSUE

-- Step 1: Drop the NOT VALID constraints
ALTER TABLE goal_participation DROP CONSTRAINT IF EXISTS chk_participation_role;
ALTER TABLE goal_participation DROP CONSTRAINT IF EXISTS chk_participation_status;

-- Step 2: Fix the data
UPDATE goal_participation SET role = 'owner' WHERE role = 'OWNER';
UPDATE goal_participation SET role = 'admin' WHERE role = 'ADMIN';
UPDATE goal_participation SET role = 'member' WHERE role = 'MEMBER';
UPDATE goal_participation SET status = 'accepted' WHERE status = 'APPROVED';
UPDATE goal_participation SET status = 'pending' WHERE status = 'PENDING';
UPDATE goal_participation SET status = 'rejected' WHERE status = 'REJECTED';

-- Step 3: Recreate the constraints as VALID
ALTER TABLE goal_participation ADD CONSTRAINT chk_participation_role CHECK (role IN ('owner', 'admin', 'member'));
ALTER TABLE goal_participation ADD CONSTRAINT chk_participation_status CHECK (status IN ('pending', 'accepted', 'rejected'));

-- Step 4: Verify
SELECT 'Constraints recreated successfully' as result;
SELECT con.conname, con.convalidated
FROM pg_constraint con
WHERE con.conrelid = 'goal_participation'::regclass
AND con.conname IN ('chk_participation_role', 'chk_participation_status');
