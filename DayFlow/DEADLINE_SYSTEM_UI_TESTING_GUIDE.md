# Deadline System UI Testing Guide

## Overview
The deadline system is now fully integrated into the goal cards. This guide explains how to test all deadline features in the UI.

## What Was Implemented

### 1. **Deadline Countdown Display on Goal Cards**
- Each goal card now displays a deadline countdown badge with color-coded urgency
- Format: `⏰ Still X days to end of the goal`
- Only appears if a deadline is set

### 2. **Color-Coded Urgency Badges**
The deadline badge changes color based on time remaining:

| Status | Days Remaining | Color | Appearance |
|--------|---|---|---|
| **Normal** | 7+ days | 🟢 Green | `⏰ Still 10 days to end` |
| **Warning** | 3-7 days | 🟡 Yellow | `⏰ Still 5 days to end` |
| **Urgent** | 1-3 days | 🟠 Orange | `⏰ Still 2 days to end` |
| **Critical** | Today | 🔴 Red | `⏰ TODAY (2h left)` |
| **Overdue** | Past deadline | 🔴 Dark Red | `⏰ OVERDUE` |

### 3. **Deadline Setting in Goal Creation**
When creating a new goal, you can set a deadline with:
- **Date Picker**: Select the deadline date
- **Time Spinners**: Set hour (0-23) and minute (0-59)
- Default: No deadline set

### 4. **Deadline Recalculation on Duplicate**
When duplicating a goal:
- The new goal's deadline is automatically set to **+7 days** from the original
- Example: Original deadline = May 5, Duplicate deadline = May 12

---

## How to Test in the UI

### Test 1: Create a Goal with a Deadline

**Steps:**
1. Click "Créer un objectif" button in the Goals Dashboard
2. Fill in goal details (title, description, etc.)
3. In the deadline section:
   - Click the date picker and select a date
   - Set the time using the hour and minute spinners
4. Click "Créer" to save

**Expected Result:**
- Goal appears in the dashboard with a deadline badge
- Badge shows countdown like `⏰ Still X days to end`

---

### Test 2: Verify Color-Coded Urgency

**To test different urgency levels, create goals with these deadlines:**

**Test 2a: Normal (Green) - 7+ days away**
- Set deadline to 10 days from today
- Expected: Green badge with `⏰ Still 10 days to end`

**Test 2b: Warning (Yellow) - 3-7 days away**
- Set deadline to 5 days from today
- Expected: Yellow badge with `⏰ Still 5 days to end`

**Test 2c: Urgent (Orange) - 1-3 days away**
- Set deadline to 2 days from today
- Expected: Orange badge with `⏰ Still 2 days to end`

**Test 2d: Critical (Red) - Today**
- Set deadline to today at 3:00 PM
- Expected: Red badge with `⏰ TODAY (Xh left)` where X is hours remaining

**Test 2e: Overdue (Dark Red) - Past deadline**
- Set deadline to yesterday
- Expected: Dark red badge with `⏰ OVERDUE`

---

### Test 3: Duplicate Goal with Deadline Recalculation

**Steps:**
1. Create a goal with deadline = May 5, 2026
2. Click the duplicate button (📋) on the goal card
3. Observe the new duplicated goal

**Expected Result:**
- New goal appears with deadline = May 12, 2026 (7 days later)
- New goal has " (Copie)" suffix in title
- New goal status is "draft"
- Deadline badge shows the new deadline countdown

---

### Test 4: Edit Goal Deadline

**Steps:**
1. Click the edit button (✏️) on a goal card
2. Modify the deadline date/time
3. Click "Modifier" to save

**Expected Result:**
- Goal card updates with new deadline countdown
- Badge color changes based on new deadline urgency

---

### Test 5: Verify Deadline Persistence

**Steps:**
1. Create a goal with a deadline
2. Close the application
3. Reopen the application and navigate to Goals Dashboard

**Expected Result:**
- Goal still displays with the same deadline countdown
- Deadline is correctly persisted in the database

---

### Test 6: Smart Reminders (Background System)

The deadline system runs a scheduler every 5 minutes that:
- Sends reminders at 24 hours before deadline
- Sends reminders at 1 hour before deadline
- Sends reminders when deadline is reached
- Sends reminders when deadline is missed

**To verify reminders are working:**
1. Check the database table `reminder_log` for entries
2. Check the `notification` table for notification records
3. Look for entries with:
   - `reminder_type`: `REMINDER_24H`, `REMINDER_1H`, `REMINDER_REACHED`, `REMINDER_MISSED`
   - `entity_type`: `GOAL`
   - `user_id`: Your user ID

**SQL Query to check:**
```sql
SELECT * FROM reminder_log 
WHERE entity_type = 'GOAL' 
ORDER BY created_at DESC 
LIMIT 10;
```

---

### Test 7: Notification Recipients

Only these users receive deadline notifications:
- **Goal Owner**: Always receives notifications
- **Approved Participants**: Only if their status in `goal_participation` is `'accepted'`

**To verify:**
1. Create a goal with a deadline
2. Add participants to the goal
3. Accept some participation requests (status = 'accepted')
4. Reject others (status = 'rejected')
5. Check the `notification` table - only owner and accepted participants should have entries

---

### Test 8: Deadline Recalculation on Update

When you edit a goal's deadline:
1. Old reminders are deleted from `reminder_log`
2. New reminders are calculated and created
3. No duplicate reminders are created (enforced by UNIQUE constraint)

**To verify:**
1. Create a goal with deadline = May 10
2. Edit the goal and change deadline to May 20
3. Check `reminder_log` - should only have entries for May 20, not May 10

---

## Database Verification

### Check Deadline Data
```sql
-- View all goals with deadlines
SELECT id, title, deadline, status 
FROM goal 
WHERE deadline IS NOT NULL 
ORDER BY deadline ASC;
```

### Check Reminders
```sql
-- View all reminders for a specific goal
SELECT * FROM reminder_log 
WHERE entity_id = <GOAL_ID> 
ORDER BY created_at DESC;
```

### Check Notifications
```sql
-- View all notifications for a user
SELECT * FROM notification 
WHERE user_id = <USER_ID> 
ORDER BY created_at DESC;
```

### Check Constraint Validation
```sql
-- Verify constraints are VALID (not NOT VALID)
SELECT constraint_name, is_valid 
FROM information_schema.table_constraints 
WHERE table_name = 'goal' 
AND constraint_name LIKE '%deadline%';
```

---

## Architecture Overview

### Clean Architecture Implementation

The deadline system follows production-level clean architecture:

#### 1. **Database Layer** (`V11__add_deadline_and_notification_system.sql`)
- `goal.deadline` column (LocalDateTime)
- `notification` table with full audit trail
- `reminder_log` table with duplicate prevention
- Database functions for automatic status updates
- Views for easy querying

#### 2. **Model Layer**
- `Goal.java`: LocalDateTime deadline field
- `Notification.java`: Full notification model with enums
- `ReminderLog.java`: Reminder tracking model

#### 3. **Service Layer** (Business Logic)
- **DeadlineManagementService**: Core deadline processing
  - Calculates reminder times (24h, 1h, reached, missed)
  - Prevents duplicate reminders via UNIQUE constraints
  - Only notifies owner and accepted participants
  - Handles deadline recalculation on updates
  
- **NotificationService**: Notification CRUD and queries
  - Create, read, update, delete notifications
  - Query by user, entity, type
  - Mark as read functionality
  
- **ReminderService**: Reminder tracking
  - Log reminder attempts
  - Prevent duplicates
  - Query reminder history

#### 4. **Scheduler Layer** (Background Tasks)
- **DeadlineScheduler**: Runs every 5 minutes
  - Singleton pattern (only one instance)
  - Processes all pending reminders
  - Integrated into GuiApp startup
  - Graceful error handling

#### 5. **Controller Layer** (UI)
- **GoalsDashboardController**: 
  - `createDeadlineLabel()`: Generates color-coded deadline badges
  - Deadline picker in goal creation dialog
  - Deadline display on goal cards
  - Deadline recalculation on duplicate

#### 6. **Presentation Layer** (CSS)
- `goals_dashboard.css`: Deadline badge styling
  - `.badge-deadline-normal`: Green (7+ days)
  - `.badge-deadline-warning`: Yellow (3-7 days)
  - `.badge-deadline-urgent`: Orange (1-3 days)
  - `.badge-deadline-critical`: Red (today)
  - `.badge-deadline-overdue`: Dark red (past)

---

## Key Features Implemented

✅ **Deadline Setting**: Set deadlines when creating/editing goals, routines, activities
✅ **Smart Reminders**: Automatic reminders at 24h, 1h, reached, and missed
✅ **Duplicate Prevention**: UNIQUE constraints prevent duplicate reminders
✅ **Selective Notifications**: Only owner and accepted participants notified
✅ **Automatic Overdue Marking**: Status changes to 'failed' when deadline passes
✅ **Deadline Recalculation**: On duplicate (+7 days) and on update (recalculate reminders)
✅ **Color-Coded UI**: Visual urgency indicators on goal cards
✅ **Production Architecture**: Clean separation of concerns, service layer, scheduler pattern
✅ **Database Integrity**: Constraints, views, functions for data consistency
✅ **Persistence**: All deadline data persists across application restarts

---

## Troubleshooting

### Deadline not showing on goal card
- **Check**: Goal has a deadline set (`goal.deadline IS NOT NULL`)
- **Check**: Goal is displayed in the dashboard (not filtered out)
- **Check**: CSS styles are loaded (check browser console for CSS errors)

### Reminders not being sent
- **Check**: DeadlineScheduler is running (check logs for "Deadline scheduler started")
- **Check**: Database has entries in `reminder_log` table
- **Check**: User is owner or has accepted participation status

### Deadline badge color not changing
- **Check**: CSS file was updated with deadline badge styles
- **Check**: Application was recompiled (`mvn clean compile`)
- **Check**: JavaFX is using the updated CSS file

### Duplicate goal deadline not recalculated
- **Check**: Original goal has a deadline set
- **Check**: Duplicate goal was created successfully
- **Check**: New deadline is exactly 7 days after original

---

## Next Steps (Optional Enhancements)

1. **Notification UI Component**: Display unread notifications in a sidebar
2. **Mark as Read**: Add functionality to mark notifications as read
3. **Real-time Refresh**: Auto-refresh notifications after login
4. **Database Cleanup**: Scheduled task to archive old notifications
5. **Email Notifications**: Send email reminders in addition to in-app
6. **Customizable Reminder Times**: Allow users to set custom reminder times
7. **Recurring Deadlines**: Support for recurring goals with automatic deadline recalculation

---

## Summary

The deadline system is now fully functional with:
- ✅ Database schema with constraints and functions
- ✅ Service layer with business logic
- ✅ Background scheduler running every 5 minutes
- ✅ UI deadline countdown display with color-coded urgency
- ✅ Deadline picker in goal creation dialog
- ✅ Automatic deadline recalculation on duplicate
- ✅ Production-level clean architecture

You can now test all features in the UI following the test cases above!
