# Status Values Fix - Summary

## 🔧 Problem Fixed

The application was using **uppercase status values** in Java code, but the PostgreSQL database constraint only accepts **lowercase values**.

### Database Constraint
```sql
CONSTRAINT chk_participation_status CHECK (status IN ('pending', 'accepted', 'rejected'))
```

### Java Code Was Using
- `'PENDING'` ❌ (uppercase)
- `'APPROVED'` ❌ (uppercase, also wrong name)
- `'REJECTED'` ❌ (uppercase)

### Correct Values Now
- `'pending'` ✅ (lowercase)
- `'accepted'` ✅ (lowercase, replaces 'APPROVED')
- `'rejected'` ✅ (lowercase)

---

## 📝 Changes Made

### 1. GoalParticipation.java (Model)
Updated the status constants:

```java
// BEFORE
public static final String STATUS_PENDING = "PENDING";
public static final String STATUS_APPROVED = "APPROVED";
public static final String STATUS_REJECTED = "REJECTED";

// AFTER
public static final String STATUS_PENDING = "pending";
public static final String STATUS_APPROVED = "accepted";
public static final String STATUS_REJECTED = "rejected";
```

### 2. GoalParticipationService.java (Service Layer)
Updated all SQL queries to use lowercase status values:

| Method | Changed From | Changed To |
|--------|--------------|-----------|
| `isOwnerOrAdmin()` | `'APPROVED'` | `'accepted'` |
| `countPendingByGoal()` | `'PENDING'` | `'pending'` |
| `promoteToAdmin()` | `STATUS_APPROVED` | `"accepted"` |
| `demoteToMember()` | `STATUS_APPROVED` | `"accepted"` |
| `findPendingByGoal()` | `'PENDING'` | `'pending'` |
| `findApprovedByGoal()` | `'APPROVED'` | `'accepted'` |
| `countApprovedByGoal()` | `'APPROVED'` | `'accepted'` |
| `listGoalIdsForUserApproved()` | `'APPROVED'` | `'accepted'` |
| `getActivityStats()` | `'APPROVED'` | `'accepted'` |
| `isApprovedMember()` | `'APPROVED'` | `'accepted'` |

---

## 🎯 Impact

### What This Fixes
✅ Resolves the constraint violation error when creating/updating goal participation records
✅ Ensures all status values match the database constraints
✅ Maintains consistency between Java code and database schema

### What Stays the Same
- Role values remain uppercase: `'MEMBER'`, `'ADMIN'`, `'OWNER'`
- All business logic remains unchanged
- All method signatures remain the same

---

## 📊 Status Value Mapping

### Participation Lifecycle

```
User requests to join goal
        ↓
Status: 'pending' (waiting for approval)
        ↓
Admin approves/rejects
        ↓
Status: 'accepted' (approved) OR 'rejected' (denied)
```

### Valid Transitions
- `pending` → `accepted` (approved)
- `pending` → `rejected` (denied)
- `accepted` → `rejected` (revoke access)
- `rejected` → `accepted` (restore access)

---

## ✅ Verification

After these changes, the following operations will work correctly:

```java
// Creating a new participation request
GoalParticipation gp = new GoalParticipation(userId, goalId);
gp.setStatus(GoalParticipation.STATUS_PENDING);  // 'pending'
goalParticipationService.insert(gp);  // ✅ No constraint violation

// Approving a request
goalParticipationService.updateStatus(participationId, GoalParticipation.STATUS_APPROVED);  // 'accepted'

// Rejecting a request
goalParticipationService.updateStatus(participationId, GoalParticipation.STATUS_REJECTED);  // 'rejected'
```

---

## 🔍 Files Modified

1. **src/main/java/model/goals_activity_management/GoalParticipation.java**
   - Updated 3 status constants

2. **src/main/java/services/chatroom/GoalParticipationService.java**
   - Updated 10 SQL queries
   - Updated 2 method calls to use new constants

---

## 📚 Related Database Schema

```sql
CREATE TABLE goal_participation (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    goal_id INTEGER NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'member',
    status VARCHAR(50) NOT NULL DEFAULT 'pending',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_goal_participation_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE,
    CONSTRAINT fk_goal_participation_goal FOREIGN KEY (goal_id) REFERENCES goal(id) ON DELETE CASCADE,
    CONSTRAINT uq_goal_participation UNIQUE (user_id, goal_id),
    CONSTRAINT chk_participation_status CHECK (status IN ('pending', 'accepted', 'rejected'))
);
```

---

## 🎉 Result

The application will now correctly handle goal participation status values without constraint violations!
