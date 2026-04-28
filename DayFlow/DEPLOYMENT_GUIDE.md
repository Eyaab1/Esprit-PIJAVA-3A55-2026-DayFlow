# 🚀 Deployment Guide - Admin Reclamations with AI

## 📋 What's New

### Features Added:
1. ✅ **Admin Reclamations Management Interface**
   - Search and filter reclamations
   - View reclamation details
   - Reply to users

2. ✅ **AI-Powered Response Suggestions (Groq)**
   - Automatic French response generation
   - Editable suggestions
   - Professional and empathetic tone

3. ✅ **Database Schema Updates**
   - Added missing user columns

---

## 🔧 Setup Instructions for Team Members

### Step 1: Pull the Latest Code
```bash
git pull origin main
```

### Step 2: Run Database Migration
Open your PostgreSQL client and run:
```bash
psql -U your_username -d your_database_name -f DATABASE_MIGRATION.sql
```

Or manually run the SQL from `DATABASE_MIGRATION.sql`

### Step 3: Configure Application Properties
```bash
# Copy the example file
cp src/main/resources/application.properties.example src/main/resources/application.properties

# Edit with your values
nano src/main/resources/application.properties
```

Fill in:
- Your database credentials
- Your Groq API key (get from https://console.groq.com/keys)

### Step 4: Compile and Run
```bash
mvn clean compile
mvn javafx:run
```

---

## 🔑 Get Groq API Key (Free)

1. Go to: https://console.groq.com/
2. Sign up (free, no credit card)
3. Go to: https://console.groq.com/keys
4. Create API Key
5. Copy and paste into `application.properties`

**Limits:** 30 requests/min, 14,400/day (free forever)

---

## 👤 Create Admin User

Run this SQL to create an admin account:

```sql
INSERT INTO "user" (
    first_name, last_name, email, password, roles,
    phone_number, age, status, review_count,
    created_at, updated_at
) VALUES (
    'Admin',
    'DayFlow',
    'admin@dayflow.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    '["ROLE_ADMIN"]',
    '+33612345678',
    30,
    'active',
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
```

**Login:**
- Email: `admin@dayflow.com`
- Password: `Admin123!`

---

## 📁 New Files Added

### Java Files:
- `controllers/admin/AdminReclamationsController.java` - Admin reclamations interface
- `services/ai/GroqAIService.java` - AI response generation
- `utils/CreateAdminUser.java` - Utility to create admin users

### Resources:
- `admin/admin_reclamations.fxml` - Admin UI layout
- `application.properties.example` - Configuration template

### Documentation:
- `ADMIN_RECLAMATIONS_GUIDE.md` - Complete feature guide
- `GROQ_AI_INTEGRATION_GUIDE.md` - AI integration details
- `DATABASE_MIGRATION.sql` - Database updates
- `DEPLOYMENT_GUIDE.md` - This file

---

## 🧪 Testing the Feature

1. **Login as admin**
2. **Navigate to:** Admin Dashboard → Réclamations
3. **Test filters:** Search, status, type
4. **Click "Répondre"** on any reclamation
5. **Click "✨ Suggérer une réponse (IA)"**
6. **Wait 2-5 seconds** for AI suggestion
7. **Edit if needed** and send

---

## 🐛 Troubleshooting

### "Column does not exist" error
→ Run `DATABASE_MIGRATION.sql`

### "Missing required config key: app.db.url"
→ Create `application.properties` from the example file

### "Groq API error"
→ Check your API key in `application.properties`

### "Email ou mot de passe incorrect"
→ Run the admin user creation SQL

---

## 🔒 Security Notes

### ⚠️ IMPORTANT:
- **DO NOT commit `application.properties`** (it's in `.gitignore`)
- **DO NOT share API keys** publicly
- **Change default admin password** after first login

### For Production:
- Use environment variables for sensitive data
- Rotate API keys regularly
- Use strong passwords
- Enable database SSL

---

## 📊 Database Schema Changes

### Added Columns to `user` table:
```sql
specialities TEXT
profile_picture_name VARCHAR(255)
profile_picture_size INTEGER
```

---

## 🎯 Feature Checklist

- [ ] Pull latest code
- [ ] Run database migration
- [ ] Configure application.properties
- [ ] Get Groq API key
- [ ] Create admin user
- [ ] Test login
- [ ] Test reclamations interface
- [ ] Test AI suggestions
- [ ] Share setup with team

---

## 📞 Support

### Documentation:
- Admin Interface: `ADMIN_RECLAMATIONS_GUIDE.md`
- AI Integration: `GROQ_AI_INTEGRATION_GUIDE.md`
- Quick Setup: `QUICK_ADMIN_SETUP.md`

### Groq Resources:
- Dashboard: https://console.groq.com/
- Docs: https://console.groq.com/docs
- API Keys: https://console.groq.com/keys

---

## ✅ Success Criteria

You're ready when:
- ✅ Application compiles without errors
- ✅ Can login as admin
- ✅ Can see Réclamations page
- ✅ Can filter and search reclamations
- ✅ Can generate AI suggestions
- ✅ Can reply to reclamations

---

**Happy coding! 🚀**
