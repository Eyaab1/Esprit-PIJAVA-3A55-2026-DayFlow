# 🔐 Admin User Setup Guide

## Quick Summary

You need an admin user to test the reclamations management interface. Here are **3 easy methods** to create one.

---

## 🚀 Method 1: Run Java Utility (EASIEST)

### Steps:

1. **Compile the project** (if not already done):
   ```bash
   cd DayFlow
   mvn compile
   ```

2. **Run the admin creator utility**:
   ```bash
   mvn exec:java -Dexec.mainClass="utils.CreateAdminUser"
   ```

3. **Login with these credentials**:
   - **Email**: `admin@dayflow.com`
   - **Password**: `Admin123!`

### Expected Output:
```
=== DayFlow Admin User Creator ===

✅ Admin user created successfully!

Credentials:
  Email: admin@dayflow.com
  Password: Admin123!
  ID: 123

⚠️  IMPORTANT: Change the password after first login!
```

---

## 🗄️ Method 2: Direct SQL (FAST)

### Steps:

1. **Generate password hash**:
   ```bash
   cd DayFlow
   mvn exec:java -Dexec.mainClass="utils.PasswordHasher" -Dexec.args="Admin123!"
   ```
   
   Copy the hash output (starts with `$2a$10$...`)

2. **Open your PostgreSQL client** (pgAdmin, DBeaver, psql, etc.)

3. **Run this SQL** (replace `YOUR_HASH_HERE` with the hash from step 1):
   ```sql
   INSERT INTO "user" (
       first_name, last_name, email, password, roles,
       phone_number, age, status, review_count,
       created_at, updated_at
   ) VALUES (
       'Admin',
       'DayFlow',
       'admin@dayflow.com',
       'YOUR_HASH_HERE',
       '["ROLE_ADMIN"]',
       '+33612345678',
       30,
       'active',
       0,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
   );
   ```

4. **Verify**:
   ```sql
   SELECT id, first_name, last_name, email, roles
   FROM "user"
   WHERE email = 'admin@dayflow.com';
   ```

5. **Login**:
   - **Email**: `admin@dayflow.com`
   - **Password**: `Admin123!`

---

## 🎨 Method 3: Through Your Application (MANUAL)

### Steps:

1. **Sign up normally** through your app with any email

2. **Get your user ID** from the database:
   ```sql
   SELECT id, email FROM "user" WHERE email = 'your-email@example.com';
   ```

3. **Update your role to ADMIN**:
   ```sql
   UPDATE "user"
   SET roles = '["ROLE_ADMIN"]'
   WHERE id = YOUR_USER_ID;
   ```

4. **Logout and login again** to refresh your session

---

## 🔍 Verify Admin Access

### Check in Database:
```sql
SELECT id, first_name, last_name, email, roles, status
FROM "user"
WHERE 'ROLE_ADMIN' = ANY(
    SELECT jsonb_array_elements_text(roles::jsonb)
);
```

### Check in Application:
1. Login with admin credentials
2. You should see the admin dashboard
3. Navigate to "Réclamations" in the sidebar
4. You should see the reclamations management interface

---

## 📋 Default Admin Credentials

| Field | Value |
|-------|-------|
| **Email** | admin@dayflow.com |
| **Password** | Admin123! |
| **First Name** | Admin |
| **Last Name** | DayFlow |
| **Phone** | +33612345678 |
| **Age** | 30 |
| **Status** | active |
| **Role** | ROLE_ADMIN |

⚠️ **Security**: Change the password after first login!

---

## 🛠️ Troubleshooting

### Problem: "Email already exists"
**Solution**: An admin already exists! Try logging in with `admin@dayflow.com` / `Admin123!`

### Problem: "Cannot connect to database"
**Solution**: 
1. Check your database is running
2. Verify connection settings in `AppConfig.java`
3. Test connection: `psql -U your_user -d your_database`

### Problem: "Admin dashboard not showing"
**Solution**:
1. Verify role in database:
   ```sql
   SELECT roles FROM "user" WHERE email = 'admin@dayflow.com';
   ```
2. Should show: `["ROLE_ADMIN"]`
3. Logout and login again

### Problem: "Password hash generation fails"
**Solution**: Use an online BCrypt generator:
1. Go to: https://bcrypt-generator.com/
2. Enter: `Admin123!`
3. Rounds: 10
4. Copy the hash

---

## 🎯 Quick Test Checklist

After creating admin:

- [ ] Can login with admin credentials
- [ ] Admin dashboard appears
- [ ] Can see "Réclamations" in sidebar
- [ ] Can click on "Réclamations"
- [ ] Can see reclamations list
- [ ] Can click "Répondre" button
- [ ] Can see "✨ Suggérer une réponse (IA)" button
- [ ] AI suggestion works

---

## 🔐 Security Best Practices

### After First Login:

1. **Change the default password**:
   - Go to user profile
   - Update password to something secure
   - Use at least 12 characters
   - Mix uppercase, lowercase, numbers, symbols

2. **Secure the database**:
   - Don't commit database credentials to Git
   - Use environment variables for production
   - Restrict database access

3. **Monitor admin actions**:
   - Review admin activity logs
   - Track who responds to reclamations
   - Audit sensitive operations

---

## 📞 Need Help?

### Common Commands:

**Check if admin exists**:
```sql
SELECT * FROM "user" WHERE email = 'admin@dayflow.com';
```

**Delete admin (to recreate)**:
```sql
DELETE FROM "user" WHERE email = 'admin@dayflow.com';
```

**List all admins**:
```sql
SELECT id, email, first_name, last_name
FROM "user"
WHERE roles::text LIKE '%ROLE_ADMIN%';
```

**Reset admin password**:
```bash
# Generate new hash
mvn exec:java -Dexec.mainClass="utils.PasswordHasher" -Dexec.args="NewPassword123!"

# Update in database
UPDATE "user"
SET password = 'NEW_HASH_HERE'
WHERE email = 'admin@dayflow.com';
```

---

## ✅ Success!

Once you have an admin user, you can:
- ✅ Access the admin dashboard
- ✅ Manage reclamations
- ✅ Test AI response suggestions
- ✅ Filter and search reclamations
- ✅ Reply to users

**Ready to test!** 🚀
