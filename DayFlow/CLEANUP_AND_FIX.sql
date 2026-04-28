-- ============================================
-- FLYWAY CLEANUP AND FIX SCRIPT
-- ============================================
-- This script safely cleans up failed migration V9
-- and prepares the database for a clean retry
-- ============================================

-- Step 1: Check current Flyway state
SELECT 'Current Flyway migration status:' AS info;
SELECT version, description, type, script, checksum, installed_rank, installed_on, execution_time, success
FROM flyway_schema_history
WHERE version IN ('8', '9', '10')
ORDER BY installed_rank;

-- Step 2: Remove failed migration V9 from history
-- This is safe because Flyway marks it as failed (success = false)
DELETE FROM flyway_schema_history 
WHERE version = '9' AND success = false;

-- Step 3: Drop tables created by the failed migration
-- Using IF EXISTS makes this safe to run multiple times
DROP TABLE IF EXISTS activity CASCADE;
DROP TABLE IF EXISTS routine CASCADE;
DROP TABLE IF EXISTS goal_participation CASCADE;
DROP TABLE IF EXISTS goal CASCADE;

-- Step 4: Verify cleanup
SELECT 'After cleanup - Flyway history:' AS info;
SELECT version, description, success
FROM flyway_schema_history
WHERE version IN ('8', '9', '10')
ORDER BY installed_rank;

-- Step 5: Check that tables are gone
SELECT 'Remaining tables (should not include goal, routine, activity):' AS info;
SELECT tablename 
FROM pg_tables 
WHERE schemaname = 'public' 
  AND tablename IN ('goal', 'routine', 'activity', 'goal_participation')
ORDER BY tablename;

-- ============================================
-- NEXT STEPS:
-- ============================================
-- 1. Run this script in pgAdmin or psql
-- 2. Recompile your project: mvn clean compile
-- 3. Run the application: mvn javafx:run
-- 
-- Flyway will now execute the corrected V9 migration
-- ============================================
