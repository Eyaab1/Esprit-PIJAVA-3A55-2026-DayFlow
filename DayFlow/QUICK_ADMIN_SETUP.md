# ⚡ Quick Admin Setup - 30 Seconds

## 🚀 Fastest Method (Recommended)

### Step 1: Run This Command
```bash
cd DayFlow
mvn exec:java -Dexec.mainClass="utils.CreateAdminUser"
```

### Step 2: Login
- **Email**: `admin@dayflow.com`
- **Password**: `Admin123!`

### Done! ✅

---

## 🎯 Alternative: SQL Script

### Step 1: Generate Password Hash
```bash
cd DayFlow
mvn exec:java -Dexec.mainClass="utils.PasswordHasher" -Dexec.args="Admin123!"
```

### Step 2: Copy the hash output (starts with `$2a$10$...`)

### Step 3: Run in PostgreSQL
```sql
INSERT INTO "user" (
    first_name, last_name, email, password, roles,
    phone_number, age, status, review_count,
    created_at, updated_at
) VALUES (
    'Admin', 'DayFlow', 'admin@dayflow.com',
    'PASTE_HASH_HERE',
    '["ROLE_ADMIN"]',
    '+33612345678', 30, 'active', 0,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);
```

### Step 4: Login
- **Email**: `admin@dayflow.com`
- **Password**: `Admin123!`

---

## ✅ Verify It Works

1. **Run your application**
2. **Login** with admin credentials
3. **Check** if you see the admin dashboard
4. **Navigate** to "Réclamations"
5. **Test** the AI response feature!

---

## 🐛 Troubleshooting

**"Email already exists"**
→ Admin already created! Just login with the credentials above.

**"Cannot connect to database"**
→ Make sure your PostgreSQL database is running.

**"Admin dashboard not showing"**
→ Logout and login again to refresh your session.

---

## 📞 Need More Help?

See the complete guide: `ADMIN_USER_SETUP_GUIDE.md`

---

**That's it! You're ready to test! 🎉**
