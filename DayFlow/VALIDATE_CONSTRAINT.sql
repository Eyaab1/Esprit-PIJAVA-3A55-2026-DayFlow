-- Check if constraint exists and its status
SELECT constraint_name, constraint_type, is_deferrable, initially_deferred
FROM information_schema.table_constraints
WHERE table_name = 'goal_participation' AND constraint_name = 'chk_participation_role';

-- Check constraint details
SELECT con.conname, con.contype, con.convalidated
FROM pg_constraint con
WHERE con.conrelid = 'goal_participation'::regclass
AND con.conname = 'chk_participation_role';

-- Validate the constraint (if it's NOT VALID)
ALTER TABLE goal_participation VALIDATE CONSTRAINT chk_participation_role;

-- Do the same for status constraint
ALTER TABLE goal_participation VALIDATE CONSTRAINT chk_participation_status;

-- Verify constraints are now valid
SELECT con.conname, con.convalidated
FROM pg_constraint con
WHERE con.conrelid = 'goal_participation'::regclass
AND con.conname IN ('chk_participation_role', 'chk_participation_status');
