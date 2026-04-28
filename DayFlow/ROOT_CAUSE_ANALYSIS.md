# Root Cause Analysis: Goal Participation Constraint Violation

## The Problem
When creating a new goal, you get this error:
```
ERREUR: la nouvelle ligne de la relation « goal_participation » viole la contrainte de vérification « chk_participation_role »
ERROR: new row for relation "goal_participation" violates check constraint "chk_participation_role"
```

The goal IS inserted successfully (you see it after refresh), but the automatic goal_participation insert fails.

## Database Constraint
From `V9__create_goal_tables.sql`:
```sql
ALTER TABLE goal_participation ADD CONSTRAINT chk_participation_role 
CHECK (role IN ('owner', 'admin', 'member')) NOT VALID;
```

The constraint expects **lowercase** values only.

## Root Cause: TWO Possible Issues

### Issue 1: Old Data in Database (Most Likely)
**Problem**: Your database contains old `goal_participation` records with **UPPERCASE** role values (`'OWNER'`, `'ADMIN'`, `'MEMBER'`) from before the constants were fixed.

**Why it happens**:
1. Old code used uppercase constants
2. Old data was inserted with uppercase values
3. New code uses lowercase constants
4. When the app tries to load or manipulate old records, PostgreSQL rejects them

**Evidence**: The goal is saved (new code works), but the error happens on participation insert (old data conflict).

### Issue 2: Duplicate Insertion Attempt (Secondary)
**Problem**: The `ensureChatroomAndOwner()` method might be called twice, causing a UNIQUE constraint violation that manifests as a role constraint error.

**Why it could happen**:
- The UNIQUE constraint on `(user_id, goal_id)` prevents duplicate insertions
- If called twice, the second insert fails
- The error message might be misleading

## Code Flow Analysis

### Current Flow (Correct):
```
1. GoalsDashboardController.onSaveGoal()
   ↓
2. goalService.insert(g)  ← Inserts goal into goal table
   ↓
3. lifecycle.ensureChatroomAndOwner(g.getId(), uid.get())
   ↓
4. Creates chatroom (if needed)
   ↓
5. Creates goal_participation with role='owner', status='accepted'
```

### The Issue:
- Step 5 tries to insert with role='owner' (lowercase)
- But if old data exists with role='OWNER' (uppercase), the constraint check fails
- OR if the method is called twice, the UNIQUE constraint fails

## Solution

### Step 1: Clean Up Old Data (CRITICAL)
Run this SQL to convert any uppercase role values to lowercase:

```sql
-- Check what role values exist
SELECT DISTINCT role FROM goal_participation;

-- Convert uppercase to lowercase
UPDATE goal_participation SET role = 'owner' WHERE role = 'OWNER';
UPDATE goal_participation SET role = 'admin' WHERE role = 'ADMIN';
UPDATE goal_participation SET role = 'member' WHERE role = 'MEMBER';

-- Verify
SELECT DISTINCT role FROM goal_participation;
```

### Step 2: Add Duplicate Prevention (BEST PRACTICE)
Modify `GoalChatroomLifecycleService.ensureChatroomAndOwner()` to prevent duplicate insertions:

```java
public void ensureChatroomAndOwner(int goalId, int creatorUserId) throws SQLException {
    // Check if participation already exists
    if (participationService.findByUserAndGoal(creatorUserId, goalId).isPresent()) {
        // Already exists, just ensure chatroom exists
        if (chatroomService.findByGoalId(goalId).isEmpty()) {
            Chatroom c = new Chatroom(goalId, "active");
            chatroomService.insert(c);
        }
        return;
    }
    
    // Create chatroom if needed
    if (chatroomService.findByGoalId(goalId).isEmpty()) {
        Chatroom c = new Chatroom(goalId, "active");
        chatroomService.insert(c);
    }
    
    // Create participation as owner
    GoalParticipation gp = new GoalParticipation();
    gp.setUserId(creatorUserId);
    gp.setGoalId(goalId);
    gp.setRole(GoalParticipation.ROLE_OWNER);
    gp.setStatus(GoalParticipation.STATUS_APPROVED);
    participationService.insert(gp);
}
```

### Step 3: Add Error Handling (DEFENSIVE)
Wrap the participation insert in try-catch to handle UNIQUE constraint violations:

```java
try {
    participationService.insert(gp);
} catch (SQLException e) {
    if (e.getMessage().contains("uq_goal_participation")) {
        // Participation already exists, this is OK
        System.out.println("Participation already exists for user " + creatorUserId + " in goal " + goalId);
    } else {
        throw e;
    }
}
```

## Why This Happens

1. **Database constraint mismatch**: The constraint expects lowercase, but old data has uppercase
2. **No duplicate prevention**: If the method is called twice, it tries to insert twice
3. **No error handling**: The app doesn't gracefully handle UNIQUE constraint violations

## Prevention Going Forward

1. ✅ All Java constants are now lowercase
2. ✅ All SQL queries use lowercase values
3. ✅ Database constraint expects lowercase
4. ✅ New data will be inserted correctly

## Files Involved

- `src/main/resources/db/migration/V9__create_goal_tables.sql` - Constraint definition
- `src/main/java/model/goals_activity_management/GoalParticipation.java` - Constants (already lowercase)
- `src/main/java/services/chatroom/GoalChatroomLifecycleService.java` - Participation creation
- `src/main/java/controllers/goals_routines/GoalsDashboardController.java` - Goal creation flow
