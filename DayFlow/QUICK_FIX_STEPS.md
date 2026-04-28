# Quick Fix Steps - Goal Participation Error

## TL;DR - Do This Now

### Step 1: Run This SQL (2 minutes)
```sql
UPDATE goal_participation SET role = 'owner' WHERE role = 'OWNER';
UPDATE goal_participation SET role = 'admin' WHERE role = 'ADMIN';
UPDATE goal_participation SET role = 'member' WHERE role = 'MEMBER';
UPDATE goal_participation SET status = 'accepted' WHERE status = 'APPROVED';
UPDATE goal_participation SET status = 'pending' WHERE status = 'PENDING';
UPDATE goal_participation SET status = 'rejected' WHERE status = 'REJECTED';
```

### Step 2: Recompile (1 minute)
```bash
mvn clean compile
```

### Step 3: Test (1 minute)
```bash
mvn javafx:run
```

Create a new goal. It should work! ✅

---

## Detailed Steps

### Using pgAdmin (Easiest)

1. Open pgAdmin
2. Connect to your database
3. Right-click on your database → Query Tool
4. Copy and paste the SQL above
5. Click Execute (or press F5)
6. You should see: "UPDATE 0" or "UPDATE X" (X = number of records updated)
7. Close pgAdmin

### Using DBeaver

1. Open DBeaver
2. Connect to your database
3. Right-click on your database → SQL Editor → Open SQL Script
4. Copy and paste the SQL above
5. Click Execute (or press Ctrl+Enter)
6. You should see: "UPDATE 0" or "UPDATE X"
7. Close DBeaver

### Using psql (Command Line)

1. Open terminal/command prompt
2. Run:
```bash
psql -U your_username -d your_database
```

3. Paste the SQL commands:
```sql
UPDATE goal_participation SET role = 'owner' WHERE role = 'OWNER';
UPDATE goal_participation SET role = 'admin' WHERE role = 'ADMIN';
UPDATE goal_participation SET role = 'member' WHERE role = 'MEMBER';
UPDATE goal_participation SET status = 'accepted' WHERE status = 'APPROVED';
UPDATE goal_participation SET status = 'pending' WHERE status = 'PENDING';
UPDATE goal_participation SET status = 'rejected' WHERE status = 'REJECTED';
```

4. Press Enter after each command
5. Type `\q` to exit

### Using a SQL File

1. Create a file named `fix.sql` with the SQL commands above
2. Run:
```bash
psql -U your_username -d your_database -f fix.sql
```

---

## Verify the Fix

### Check 1: Verify Database
```sql
SELECT DISTINCT role FROM goal_participation;
-- Should show: owner, admin, member (all lowercase)

SELECT DISTINCT status FROM goal_participation;
-- Should show: pending, accepted, rejected (all lowercase)
```

### Check 2: Recompile
```bash
mvn clean compile
```
Should complete with no errors.

### Check 3: Test Application
```bash
mvn javafx:run
```

1. Create a new goal
2. Fill in the form
3. Click Save
4. **Expected**: Goal appears without error popup ✅

### Check 4: Verify Persistence
1. Close the application
2. Restart it
3. Navigate to "Mes Objectifs"
4. **Expected**: All goals appear without errors ✅

---

## What Changed

### In the Database
- All `'OWNER'` → `'owner'`
- All `'ADMIN'` → `'admin'`
- All `'MEMBER'` → `'member'`
- All `'APPROVED'` → `'accepted'`
- All `'PENDING'` → `'pending'`
- All `'REJECTED'` → `'rejected'`

### In the Code
- `GoalChatroomLifecycleService.ensureChatroomAndOwner()` now:
  - Checks if participation already exists
  - Prevents duplicate insertions
  - Handles errors gracefully

---

## If It Still Doesn't Work

### Check 1: Did you run the SQL?
```sql
SELECT COUNT(*) FROM goal_participation WHERE role = 'OWNER';
-- Should return: 0
```

### Check 2: Did you recompile?
```bash
mvn clean compile
```

### Check 3: Did you restart the app?
```bash
mvn javafx:run
```

### Check 4: Check the logs
Look for any error messages in the console output.

---

## That's It!

The fix is simple:
1. ✅ Run the SQL to fix old data
2. ✅ Recompile the code
3. ✅ Test the application

Your goal creation should now work perfectly! 🎉
