# Goal Participation Constraint Error - Complete Fix Guide

## Problem Summary
When creating a new goal, you get this error:
```
ERREUR: la nouvelle ligne de la relation « goal_participation » viole la contrainte de vérification « chk_participation_role »
```

**What happens**:
1. Goal is successfully inserted into the `goal` table ✅
2. Goal appears after refresh ✅
3. But goal_participation insert fails ❌
4. Error popup appears ❌

## Root Cause

### Primary Cause: Old Data with Uppercase Role Values
Your database contains old `goal_participation` records with **UPPERCASE** role values (`'OWNER'`, `'ADMIN'`, `'MEMBER'`) from before the Java constants were fixed.

**Why this causes the error**:
- Database constraint expects: `CHECK (role IN ('owner', 'admin', 'member'))` (lowercase)
- Old data has: `'OWNER'`, `'ADMIN'`, `'MEMBER'` (uppercase)
- When PostgreSQL validates the constraint, it rejects the uppercase values
- The error occurs even though the new code is correct

### Secondary Cause: Duplicate Insertion Risk
The original code could attempt to insert the same participation twice if called concurrently, violating the UNIQUE constraint.

## The Fix (3 Steps)

### Step 1: Clean Up Old Data in Database (CRITICAL)

**Using pgAdmin or DBeaver**:
1. Connect to your PostgreSQL database
2. Open a SQL query window
3. Run the script: `CLEANUP_GOAL_PARTICIPATION_DATA.sql`

**Or using psql command line**:
```bash
psql -U your_username -d your_database -f CLEANUP_GOAL_PARTICIPATION_DATA.sql
```

**Or manually run these SQL commands**:
```sql
-- Convert uppercase role values to lowercase
UPDATE goal_participation SET role = 'owner' WHERE role = 'OWNER';
UPDATE goal_participation SET role = 'admin' WHERE role = 'ADMIN';
UPDATE goal_participation SET role = 'member' WHERE role = 'MEMBER';

-- Convert uppercase status values to lowercase
UPDATE goal_participation SET status = 'accepted' WHERE status = 'APPROVED';
UPDATE goal_participation SET status = 'pending' WHERE status = 'PENDING';
UPDATE goal_participation SET status = 'rejected' WHERE status = 'REJECTED';

-- Verify the fix
SELECT DISTINCT role FROM goal_participation;
SELECT DISTINCT status FROM goal_participation;
```

**Expected result**:
```
role: owner, admin, member (all lowercase)
status: pending, accepted, rejected (all lowercase)
```

### Step 2: Update Java Code (ALREADY DONE)

The `GoalChatroomLifecycleService.java` has been updated with:

**Improvements**:
1. ✅ Checks if participation already exists BEFORE attempting insert
2. ✅ Ensures chatroom exists regardless of participation state
3. ✅ Handles UNIQUE constraint violations gracefully
4. ✅ Prevents duplicate insertions

**New code**:
```java
public void ensureChatroomAndOwner(int goalId, int creatorUserId) throws SQLException {
    // First, check if participation already exists (prevent duplicates)
    var existingParticipation = participationService.findByUserAndGoal(creatorUserId, goalId);
    
    // Ensure chatroom exists
    if (chatroomService.findByGoalId(goalId).isEmpty()) {
        Chatroom c = new Chatroom(goalId, "active");
        chatroomService.insert(c);
    }
    
    // Create participation if it doesn't exist
    if (existingParticipation.isEmpty()) {
        GoalParticipation gp = new GoalParticipation();
        gp.setUserId(creatorUserId);
        gp.setGoalId(goalId);
        gp.setRole(GoalParticipation.ROLE_OWNER);
        gp.setStatus(GoalParticipation.STATUS_APPROVED);
        
        try {
            participationService.insert(gp);
        } catch (SQLException e) {
            // Handle UNIQUE constraint violation gracefully
            if (e.getMessage() != null && e.getMessage().contains("uq_goal_participation")) {
                System.out.println("Participation already exists for user " + creatorUserId + " in goal " + goalId);
            } else {
                throw e;
            }
        }
    }
}
```

### Step 3: Restart Your Application

```bash
mvn javafx:run
```

## Verification

After applying the fix, test the following:

### Test 1: Create a New Goal
1. Open the application
2. Navigate to "Mes Objectifs"
3. Click "Create Goal"
4. Fill in the form and save
5. **Expected**: Goal appears without error popup ✅

### Test 2: Verify Database
```sql
-- Check the new goal_participation record
SELECT * FROM goal_participation 
WHERE goal_id = (SELECT MAX(id) FROM goal)
ORDER BY created_at DESC LIMIT 1;

-- Should show:
-- role: 'owner' (lowercase)
-- status: 'accepted' (lowercase)
```

### Test 3: Restart Application
1. Close the application
2. Restart it
3. Navigate to "Mes Objectifs"
4. **Expected**: All goals appear without errors ✅

## Why This Fix Works

### Before Fix:
```
1. Create goal → goal inserted ✅
2. Call ensureChatroomAndOwner()
3. Check if chatroom exists → NO
4. Insert chatroom ✅
5. Check if participation exists → NO
6. Try to insert participation with role='owner'
7. PostgreSQL checks constraint: 'owner' IN ('owner', 'admin', 'member') ✅
8. But old data has 'OWNER' which fails constraint ❌
9. Error popup ❌
```

### After Fix:
```
1. Create goal → goal inserted ✅
2. Call ensureChatroomAndOwner()
3. Check if participation already exists → NO
4. Check if chatroom exists → NO
5. Insert chatroom ✅
6. Insert participation with role='owner' ✅
7. PostgreSQL checks constraint: 'owner' IN ('owner', 'admin', 'member') ✅
8. All old data is now lowercase ✅
9. Success! No error ✅
```

## Compatibility with Teammates

**Important**: This fix maintains compatibility with teammates' code:

- ✅ All Java constants are lowercase: `ROLE_OWNER = "owner"`
- ✅ All SQL queries use lowercase values
- ✅ Database constraint expects lowercase values
- ✅ New code is consistent across the team

If teammates have code using uppercase values, they should update their constants to match.

## Files Modified

1. **src/main/java/services/chatroom/GoalChatroomLifecycleService.java**
   - Improved `ensureChatroomAndOwner()` method
   - Added duplicate prevention
   - Added error handling for UNIQUE constraint violations

2. **CLEANUP_GOAL_PARTICIPATION_DATA.sql** (New)
   - Script to clean up old data
   - Converts uppercase to lowercase
   - Verifies the fix

## Prevention Going Forward

1. ✅ All Java constants are lowercase
2. ✅ All SQL queries use lowercase values
3. ✅ Database constraints expect lowercase values
4. ✅ Code prevents duplicate insertions
5. ✅ Error handling for edge cases

## Troubleshooting

### If you still get the error after applying the fix:

**Check 1**: Verify the database cleanup was applied
```sql
SELECT DISTINCT role FROM goal_participation;
-- Should show only: owner, admin, member (lowercase)
```

**Check 2**: Check for any other tables with uppercase values
```sql
SELECT * FROM goal_participation WHERE role NOT IN ('owner', 'admin', 'member');
-- Should return 0 rows
```

**Check 3**: Verify the Java code was updated
```bash
grep -n "ensureChatroomAndOwner" src/main/java/services/chatroom/GoalChatroomLifecycleService.java
# Should show the new implementation with duplicate prevention
```

**Check 4**: Recompile the project
```bash
mvn clean compile
```

## Questions?

If you have questions about this fix:
1. Check the `ROOT_CAUSE_ANALYSIS.md` file for detailed explanation
2. Review the SQL cleanup script: `CLEANUP_GOAL_PARTICIPATION_DATA.sql`
3. Check the updated Java code in `GoalChatroomLifecycleService.java`
