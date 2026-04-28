# Goal Participation Role Constraint Error - Explanation

## The Problem You're Experiencing

**Error Message**: "ERROR: the new line of the 'goal_participation' relation violates the verification constraint 'chk_participation_role'..."

**Behavior**: 
- When you create a goal, it appears in the UI
- When you close and restart the application, the goal appears (it was saved)
- But you get a constraint violation error

## Root Cause

The database constraint `chk_participation_role` only allows these lowercase values:
```sql
CHECK (role IN ('owner', 'admin', 'member'))
```

However, **your database contains old goal_participation records with UPPERCASE role values** (`'OWNER'`, `'ADMIN'`, `'MEMBER'`) from before we fixed the Java constants.

When the application restarts and tries to load or manipulate these old records, PostgreSQL rejects them because they violate the constraint.

## Why This Happens

1. **Before the fix**: Java code used uppercase constants (`"OWNER"`, `"ADMIN"`, `"MEMBER"`)
2. **Old data was inserted**: Goal participation records were saved with uppercase role values
3. **After the fix**: We changed Java constants to lowercase (`"owner"`, `"admin"`, `"member"`)
4. **On restart**: The application tries to load old records with uppercase values
5. **Constraint violation**: PostgreSQL rejects the uppercase values because they don't match the constraint

## The Solution

You need to clean up the existing data in the database by converting all uppercase role values to lowercase.

### Option 1: Run the SQL Cleanup Script (Recommended)

Execute the SQL script `FIX_GOAL_PARTICIPATION_ROLES.sql`:

```sql
UPDATE goal_participation SET role = 'owner' WHERE role = 'OWNER';
UPDATE goal_participation SET role = 'admin' WHERE role = 'ADMIN';
UPDATE goal_participation SET role = 'member' WHERE role = 'MEMBER';
```

### Option 2: Delete and Recreate the Database

If you want a fresh start:
1. Drop the `goal_participation` table
2. Run the migrations again
3. Create new goals (they will use the correct lowercase values)

### Option 3: Use pgAdmin or DBeaver

1. Connect to your PostgreSQL database
2. Run the UPDATE statements above
3. Verify with: `SELECT DISTINCT role FROM goal_participation;`

## How to Apply the Fix

### Step 1: Connect to Your Database
Use pgAdmin, DBeaver, or psql:
```bash
psql -U your_user -d your_database
```

### Step 2: Run the Cleanup
```sql
UPDATE goal_participation SET role = 'owner' WHERE role = 'OWNER';
UPDATE goal_participation SET role = 'admin' WHERE role = 'ADMIN';
UPDATE goal_participation SET role = 'member' WHERE role = 'MEMBER';
```

### Step 3: Verify
```sql
SELECT DISTINCT role FROM goal_participation;
```

You should see only: `'owner'`, `'admin'`, `'member'` (all lowercase)

### Step 4: Restart Your Application
```bash
mvn javafx:run
```

## Why This Happened

The database constraint was created with lowercase values, but the Java code was using uppercase constants. This mismatch caused the error. We've now fixed the Java code, but the old data in the database still has the uppercase values.

## Prevention

Going forward:
- All new goal participation records will use lowercase role values
- The Java constants are now: `"owner"`, `"admin"`, `"member"`
- The database constraint expects: `'owner'`, `'admin'`, `'member'`
- Everything is now aligned and consistent

## Files Modified

- `src/main/java/model/goals_activity_management/GoalParticipation.java` - Constants changed to lowercase
- `src/main/java/services/chatroom/GoalParticipationService.java` - SQL queries updated
- `src/main/java/services/goals_routines/GoalService.java` - SQL queries updated
- `src/main/java/services/admin/AdminGoalService.java` - SQL queries updated
