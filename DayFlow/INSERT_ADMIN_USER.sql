-- Insert Admin User into Database
-- Email: admin@dayflow.com
-- Password: Admin123!
-- BCrypt Hash: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGjmsnvg.WJSgog6i6

-- First, check if admin user already exists
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM "user" WHERE email = 'admin@dayflow.com') THEN
        -- Insert admin user
        INSERT INTO "user" (
            first_name,
            last_name,
            email,
            password,
            role,
            created_at,
            updated_at,
            specialities,
            profile_picture_name,
            profile_picture_size
        ) VALUES (
            'Admin',
            'DayFlow',
            'admin@dayflow.com',
            '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGjmsnvg.WJSgog6i6',
            'ADMIN',
            NOW(),
            NOW(),
            NULL,
            NULL,
            NULL
        );
        
        RAISE NOTICE 'Admin user created successfully!';
        RAISE NOTICE 'Email: admin@dayflow.com';
        RAISE NOTICE 'Password: Admin123!';
    ELSE
        RAISE NOTICE 'Admin user already exists!';
    END IF;
END $$;

-- Verify the admin user
SELECT 
    id,
    first_name,
    last_name,
    email,
    role,
    created_at
FROM "user"
WHERE email = 'admin@dayflow.com';
