-- ============================================
-- DayFlow Database Migration
-- Add missing columns for user table
-- ============================================
-- 
-- Run this SQL script in your PostgreSQL database
-- to add the required columns for the application
-- 
-- This is safe to run multiple times (uses IF NOT EXISTS)
-- ============================================

-- Add missing user table columns
ALTER TABLE "user" 
ADD COLUMN IF NOT EXISTS specialities TEXT,
ADD COLUMN IF NOT EXISTS profile_picture_name VARCHAR(255),
ADD COLUMN IF NOT EXISTS profile_picture_size INTEGER;

-- Verify columns were added
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'user' 
  AND column_name IN ('specialities', 'profile_picture_name', 'profile_picture_size');

-- ============================================
-- Success! Columns added.
-- ============================================
