-- ============================================
-- Script to Create Admin User for DayFlow
-- ============================================
-- 
-- OPTION 1: Run this SQL directly in your PostgreSQL database
-- 
-- Instructions:
-- 1. Open pgAdmin or your PostgreSQL client
-- 2. Connect to your DayFlow database
-- 3. Run this script
-- 
-- Default Admin Credentials:
-- Email: admin@dayflow.com
-- Password: Admin123!
-- 
-- ⚠️ IMPORTANT: Change the password after first login!
-- ============================================

-- Insert admin user
-- Password hash for "Admin123!" using BCrypt
INSERT INTO "user" (
    first_name,
    last_name,
    email,
    password,
    roles,
    phone_number,
    age,
    status,
    review_count,
    created_at,
    updated_at
) VALUES (
    'Admin',
    'DayFlow',
    'admin@dayflow.com',
    '$2a$10$rZ5qhHqKqYxKqYxKqYxKqOqYxKqYxKqYxKqYxKqYxKqYxKqYxKqYxK', -- This is a placeholder, see below
    '["ROLE_ADMIN"]',
    '+33612345678',
    30,
    'active',
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (email) DO NOTHING;

-- ============================================
-- IMPORTANT: The password hash above is a placeholder!
-- 
-- To generate the correct BCrypt hash for "Admin123!":
-- 
-- METHOD 1: Use the Java utility (recommended)
-- Run: java -cp target/classes utils.PasswordHasher Admin123!
-- 
-- METHOD 2: Use online BCrypt generator
-- Go to: https://bcrypt-generator.com/
-- Enter: Admin123!
-- Rounds: 10
-- Copy the hash and replace the password value above
-- 
-- METHOD 3: Use the CreateAdminUser.java utility (see below)
-- ============================================

-- Verify the admin was created
SELECT id, first_name, last_name, email, roles, status, created_at
FROM "user"
WHERE email = 'admin@dayflow.com';
