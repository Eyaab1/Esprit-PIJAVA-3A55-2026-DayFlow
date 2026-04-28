# Deadline System - Testing Summary

## Quick Answer: How to Test in the UI

### 🎯 The 7 Features You Can Test

---

## 1️⃣ SET DEADLINE WHEN CREATING GOALS

### In the UI:
1. Click **"Create Goal"** button
2. You'll see a **NEW deadline picker** with:
   - 📅 Date picker
   - ⏰ Hour spinner (0-23)
   - ⏰ Minute spinner (0-59)
3. Set deadline: e.g., May 15, 2026 at 17:30
4. Click OK

### What to Look For:
✅ Goal appears in dashboard
✅ Deadline is displayed on the goal card
✅ Deadline shows both date AND time

### Database Verification:
```sql
SELECT deadline FROM goal WHERE id = YOUR_GOAL_ID;
-- Should show: 2026-05-15 17:30:00
```

---

## 2️⃣ SMART REMINDERS (24h, 1h, Reached, Missed)

### In the UI:
1. Create a goal with deadline = **tomorrow at 14:00**
2. Wait 5 minutes (scheduler runs every 5 minutes)
3. Check console for log messages

### What to Look For:
✅ Console shows: `[DeadlineManagementService] Sent reminder to user X for goal Y`
✅ Notifications appear in database

### Database Verification:
```sql
-- Check notifications created
SELECT type, title FROM notification 
WHERE entity_id = YOUR_GOAL_ID
ORDER BY created_at DESC;

-- You should see:
-- DEADLINE_24H: "Goal Deadline Reminder: ..."
-- DEADLINE_1H: "Goal Deadline Reminder: ..."
-- DEADLINE_REACHED: "Goal Deadline Reminder: ..."
-- DEADLINE_MISSED: "Goal Deadline Reminder: ..."
```

### Timeline:
- **24 hours before:** DEADLINE_24H notification
- **1 hour before:** DEADLINE_1H notification
- **At deadline:** DEADLINE_REACHED notification
- **After deadline:** DEADLINE_MISSED notification

---

## 3️⃣ DUPLICATE PREVENTION

### In the UI:
1. Create a goal with deadline
2. Try to manually insert duplicate reminder in database

### What to Look For:
✅ First insert succeeds
❌ Second insert fails with UNIQUE constraint error

### Database Verification:
```sql
-- Try to insert duplicate
INSERT INTO reminder_log (user_id, entity_type, entity_id, reminder_type, deadline, created_at)
VALUES (1, 'goal', YOUR_GOAL_ID, 'REMINDER_24H', '2026-05-15 17:30:00', CURRENT_TIMESTAMP);

-- Try again (should fail)
INSERT INTO reminder_log (user_id, entity_type, entity_id, reminder_type, deadline, created_at)
VALUES (1, 'goal', YOUR_GOAL_ID, 'REMINDER_24H', '2026-05-15 17:30:00', CURRENT_TIMESTAMP);

-- Error: duplicate key value violates unique constraint "uq_reminder_log"
```

---

## 4️⃣ ONLY NOTIFIES OWNER AND APPROVED PARTICIPANTS

### In the UI:
1. Create a goal
2. Add multiple participants:
   - User 1: owner, accepted ✅ Gets notification
   - User 2: member, accepted ✅ Gets notification
   - User 3: member, pending ❌ NO notification
   - User 4: member, rejected ❌ NO notification

### What to Look For:
✅ Only users 1 and 2 receive notifications
❌ Users 3 and 4 do NOT receive notifications

### Database Verification:
```sql
-- Check who got notifications
SELECT DISTINCT user_id FROM notification 
WHERE entity_id = YOUR_GOAL_ID;

-- Should show: 1, 2 (NOT 3 or 4)

-- Verify participation status
SELECT user_id, role, status FROM goal_participation 
WHERE goal_id = YOUR_GOAL_ID;
```

---

## 5️⃣ AUTOMATIC OVERDUE MARKING

### In the UI:
1. Create a goal with deadline = **yesterday at 14:00**
2. Wait 5 minutes for scheduler
3. Check the goal status

### What to Look For:
✅ Goal status changes to 'failed'
✅ Goal card shows "OVERDUE" indicator
✅ Notification type = 'DEADLINE_MISSED'

### Database Verification:
```sql
-- Check if goal is marked overdue
SELECT id, title, deadline, status, is_overdue FROM goal 
WHERE id = YOUR_GOAL_ID;

-- Should show: status='failed', is_overdue=true

-- Check for missed deadline notification
SELECT type FROM notification 
WHERE entity_id = YOUR_GOAL_ID AND type = 'DEADLINE_MISSED';
```

---

## 6️⃣ DEADLINE RECALCULATION ON DUPLICATE

### In the UI:
1. Create a goal with deadline = **May 15, 2026 at 17:30**
2. Click the **📋 Duplicate** button on the goal card
3. Check the new goal's deadline

### What to Look For:
✅ New goal title = "Original Title (Copie)"
✅ New goal deadline = **May 22, 2026 at 17:30** (original + 7 days)
✅ New goal status = 'draft'
✅ New goal progress = 0%

### Database Verification:
```sql
-- Check original goal
SELECT id, title, deadline FROM goal 
WHERE title = 'Original Title';

-- Check duplicated goal
SELECT id, title, deadline FROM goal 
WHERE title = 'Original Title (Copie)';

-- Calculate difference
SELECT 
  (SELECT deadline FROM goal WHERE title = 'Original Title (Copie)') - 
  (SELECT deadline FROM goal WHERE title = 'Original Title') AS deadline_diff;

-- Should show: 7 days
```

---

## 7️⃣ PRODUCTION-LEVEL CLEAN ARCHITECTURE

### In the Code:
Check that the system is properly organized:

#### Service Layer (Business Logic)
```
src/main/java/services/
├── deadline/
│   ├── DeadlineManagementService.java (core logic)
│   └── DeadlineScheduler.java (runs every 5 min)
├── notification/
│   ├── NotificationService.java (CRUD)
│   └── ReminderService.java (tracking)
└── goals_routines/
    ├── GoalService.java (updated)
    ├── ActivityService.java (updated)
    └── RoutineService.java (updated)
```

#### Model Layer (Data Objects)
```
src/main/java/model/
├── goals_activity_management/
│   ├── Goal.java (with deadline field)
│   ├── Activity.java (with deadline field)
│   └── Routine.java (with deadline field)
└── notification/
    ├── Notification.java
    └── ReminderLog.java
```

#### Controller Layer (UI Integration)
```
src/main/java/controllers/goals_routines/
├── GoalsDashboardController.java (deadline picker)
├── ActivityDetailsController.java (deadline handling)
├── RoutineDetailController.java (deadline handling)
└── RoutineDetailsController.java (deadline handling)
```

#### Scheduler Integration
```
src/main/java/GuiApp.java
// Line: DeadlineScheduler.getInstance().start();
```

### What to Look For:
✅ Services are separated by concern
✅ Models have deadline fields
✅ Controllers handle deadline UI
✅ Scheduler is integrated in startup
✅ Database has proper schema

---

## 🧪 COMPLETE TEST SCENARIO (15 minutes)

### Timeline:
1. **0-2 min:** Create goal with deadline
2. **2-7 min:** Test reminders (wait for scheduler)
3. **7-9 min:** Test duplicate prevention
4. **9-12 min:** Test selective notifications
5. **12-14 min:** Test overdue marking
6. **14-16 min:** Test deadline recalculation
7. **16-17 min:** Verify architecture

### Commands:
```bash
# Start application
mvn javafx:run

# In another terminal, monitor database
psql -U postgres -d dayflow -c "SELECT * FROM notification ORDER BY created_at DESC LIMIT 10;"
```

---

## 📊 VERIFICATION CHECKLIST

### UI Features:
- [ ] Deadline picker appears in "Create Goal" dialog
- [ ] Deadline shows date and time
- [ ] Duplicate button (📋) works
- [ ] New goal has deadline +7 days
- [ ] Goal card displays deadline
- [ ] Warning badge appears if deadline < 7 days

### Database Features:
- [ ] Notifications are created
- [ ] No duplicate reminders
- [ ] Only owner/accepted get notifications
- [ ] Overdue goals are marked
- [ ] Deadline columns exist
- [ ] Unique constraint prevents duplicates

### Architecture:
- [ ] Services are organized
- [ ] Models have deadline fields
- [ ] Controllers handle deadline UI
- [ ] Scheduler runs every 5 minutes
- [ ] Database schema is correct

---

## 🔍 KEY DATABASE QUERIES

### See All Notifications:
```sql
SELECT user_id, type, COUNT(*) FROM notification 
GROUP BY user_id, type ORDER BY user_id;
```

### See All Reminders:
```sql
SELECT user_id, entity_type, reminder_type, COUNT(*) FROM reminder_log 
GROUP BY user_id, entity_type, reminder_type ORDER BY user_id;
```

### See Goals with Deadlines:
```sql
SELECT id, title, deadline, status, is_overdue FROM goal 
WHERE deadline IS NOT NULL ORDER BY deadline ASC;
```

### Check for Duplicates:
```sql
SELECT user_id, entity_id, reminder_type, COUNT(*) FROM reminder_log 
GROUP BY user_id, entity_id, reminder_type HAVING COUNT(*) > 1;
```

---

## 🎯 WHAT YOU'LL SEE IN THE UI

### Goal Card:
```
┌──────────────────────────────────────────┐
│ 📌 Q2 Project Delivery                   │
│ ─────────────────────────────────────────│
│ Status: Active | Progress: 45%           │
│ Deadline: May 15, 2026 at 17:30          │
│ ⏰ 3 days remaining                       │
│ ⚠️ WARNING: Deadline approaching         │
│ [✏️ Edit] [🗑️ Delete] [📋 Duplicate]    │
└──────────────────────────────────────────┘
```

### Create Goal Dialog:
```
┌──────────────────────────────────────────┐
│ Nouvel objectif                          │
├──────────────────────────────────────────┤
│ Titre: [________________]                │
│ Description: [________________]          │
│ Début: [📅 2026-05-01]                   │
│ Fin: [📅 2026-05-08]                     │
│ Deadline: [📅 2026-05-15] à [17]h [30]   │
│ Statut: [active ▼]                      │
│ [OK] [CANCEL]                           │
└──────────────────────────────────────────┘
```

---

## ✅ FINAL SUMMARY

### All 7 Features Are Fully Implemented:

| # | Feature | UI Test | DB Verify | Status |
|---|---------|---------|-----------|--------|
| 1 | Create with deadline | ✅ Dialog | ✅ Query | ✅ Ready |
| 2 | Smart reminders | ✅ Wait 5min | ✅ Notifications | ✅ Ready |
| 3 | Duplicate prevention | ✅ DB test | ✅ Constraint | ✅ Ready |
| 4 | Selective notify | ✅ Add users | ✅ Check users | ✅ Ready |
| 5 | Overdue marking | ✅ Past date | ✅ Status change | ✅ Ready |
| 6 | Deadline recalc | ✅ Duplicate | ✅ +7 days | ✅ Ready |
| 7 | Clean architecture | ✅ Code review | ✅ Schema check | ✅ Ready |

---

## 🚀 START TESTING NOW

```bash
# 1. Start application
mvn javafx:run

# 2. Follow the step-by-step guide
# See: DEADLINE_SYSTEM_STEP_BY_STEP_TEST.md

# 3. Verify in database
# See: Key database queries above

# 4. Check console logs
# Look for: [DeadlineManagementService] messages
```

---

## 📚 DOCUMENTATION FILES

1. **DEADLINE_SYSTEM_UI_TESTING_GUIDE.md** - Detailed testing guide
2. **DEADLINE_SYSTEM_UI_WALKTHROUGH.md** - Visual UI walkthrough
3. **DEADLINE_SYSTEM_STEP_BY_STEP_TEST.md** - Step-by-step scenario
4. **DEADLINE_SYSTEM_TESTING_SUMMARY.md** - This file

---

## 🎓 WHAT YOU'LL LEARN

By testing these features, you'll understand:
- ✅ How deadline system works end-to-end
- ✅ How scheduler processes deadlines
- ✅ How notifications are generated
- ✅ How duplicate prevention works
- ✅ How selective notifications work
- ✅ How overdue marking works
- ✅ How deadline recalculation works
- ✅ How clean architecture is implemented

---

**Ready to test? Start with: `mvn javafx:run`**
