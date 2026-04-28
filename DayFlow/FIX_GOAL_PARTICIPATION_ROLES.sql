-- Fix existing goal_participation records with uppercase role values
-- This script converts any uppercase role values to lowercase to match the database constraint

-- First, let's see what role values currently exist
SELECT DISTINCT role FROM goal_participation;

-- Update any uppercase role values to lowercase
UPDATE goal_participation SET role = 'owner' WHERE role = 'OWNER';
UPDATE goal_participation SET role = 'admin' WHERE role = 'ADMIN';
UPDATE goal_participation SET role = 'member' WHERE role = 'MEMBER';

-- Verify the changes
SELECT DISTINCT role FROM goal_participation;

-- Also check for any other unexpected values
SELECT role, COUNT(*) as count FROM goal_participation GROUP BY role;
