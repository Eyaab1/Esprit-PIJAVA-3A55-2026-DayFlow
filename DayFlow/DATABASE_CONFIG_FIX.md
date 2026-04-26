# 🔧 Database Configuration Fix

## ⚠️ Problem
Your `application.properties` file is missing database configuration!

## ✅ Solution

### Step 1: Open the file
Open: `DayFlow/src/main/resources/application.properties`

### Step 2: Update these lines with YOUR database info:

```properties
# Database Configuration
app.db.url=jdbc:postgresql://localhost:5432/YOUR_DATABASE_NAME
app.db.user=YOUR_POSTGRES_USERNAME
app.db.password=YOUR_POSTGRES_PASSWORD
```

### Step 3: Replace with your actual values

**Example:**
```properties
# Database Configuration
app.db.url=jdbc:postgresql://localhost:5432/dayflow
app.db.user=postgres
app.db.password=mypassword123
```

---

## 🔍 How to Find Your Database Info

### Database Name:
- Check what database you created for DayFlow
- Common names: `dayflow`, `dayflow_db`, `pidev`, etc.

### Username:
- Usually: `postgres` (default PostgreSQL user)
- Or your custom username

### Password:
- The password you set when installing PostgreSQL
- Or the password for your database user

### Port:
- Default: `5432`
- Check if you changed it during PostgreSQL installation

---

## 📝 Quick Test

After updating, test the connection:

```bash
psql -U YOUR_USERNAME -d YOUR_DATABASE_NAME
```

If this works, your credentials are correct!

---

## 🎯 Complete Example

Here's what your `application.properties` should look like:

```properties
# Database Configuration
app.db.url=jdbc:postgresql://localhost:5432/dayflow
app.db.user=postgres
app.db.password=admin123

# Groq AI Configuration
groq.api.key=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
groq.api.url=https://api.groq.com/openai/v1/chat/completions
groq.model=llama-3.1-70b-versatile
groq.max.tokens=500
groq.temperature=0.7
```

---

## ✅ After Fixing

1. Save the file
2. Recompile: `mvn clean compile`
3. Run your app again
4. Login with admin credentials

---

## 🆘 Still Having Issues?

### Check if PostgreSQL is running:
```bash
# Windows
Get-Service postgresql*

# Or check in Services app (services.msc)
```

### Verify database exists:
```bash
psql -U postgres -l
```

This will list all databases. Make sure yours is there!

---

**Once you update the file, try logging in again!** 🚀
