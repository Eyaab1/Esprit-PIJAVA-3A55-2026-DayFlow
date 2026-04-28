# Deadline System - Step-by-Step Testing Scenario

## Complete End-to-End Test (15 minutes)

This guide walks you through testing all deadline features in one complete scenario.

---

## PART 1: CREATE GOAL WITH DEADLINE (2 minutes)

### Step 1: Start Application
```bash
mvn javafx:run
```

### Step 2: Login
- Username: your_username
- Password: your_password

### Step 3: Navigate to Goals Dashboard
- Click "Goals" in main menu
- You should see the Goals Dashboard

### Step 4: Create a Goal with Deadline
1. Click **"Create Goal"** button
2. Fill in the form:
   ```
   Titre:           "Q2 Project Delivery"
   Description:     "Complete all Q2 deliverables"
   Début:           Today (click date picker)
   Fin:             7 days from today
   Deadline:        5 days from today at 17:30
   Statut:          "active"
   ```
3. Click **OK**

### Expected Result:
✅ Goal appears in dashboard with deadline displayed

### Verify in Database:
```sql
SELECT id, title, deadline, status FROM goal 
WHERE title = 'Q2 Project Delivery';

-- Should show: deadline = 2026-05-15 17:30:00
```

---

## PART 2: TEST SMART REMINDERS (5 minutes)

### Step 1: Create Test Goal with Near Deadline
For testing reminders quickly, create a goal with deadline = **1 hour from now**

1. Click **"Create Goal"** again
2. Fill in:
   ```
   Titre:           "Test Reminder Goal"
   Description:     "Testing reminder system"
   Début:           Today
   Fin:             7 days from today
   Deadline:        TODAY at [CURRENT_TIME + 1 HOUR]
   Statut:          "active"
   ```
3. Click **OK**

### Step 2: Wait for Scheduler
- The scheduler runs every 5 minutes
- Wait up to 5 minutes
- Watch the console for log messages:
  ```
  [DeadlineManagementService] Starting deadline processing at 2026-04-28T21:45:00
  [DeadlineManagementService] Sent reminder to user X for goal Y
  [DeadlineManagementService] Deadline processing completed
  ```

### Step 3: Verify Reminder in Database
```sql
-- Check if notification was created
SELECT id, user_id, type, title, created_at 
FROM notification 
WHERE entity_id = (SELECT id FROM goal WHERE title = 'Test Reminder Goal')
ORDER BY created_at DESC;

-- Should show: type = 'DEADLINE_1H' or 'DEADLINE_24H'
```

### Expected Result:
✅ Notification created in database
✅ Log message shows reminder was sent
✅ No duplicate reminders

---

## PART 3: TEST DUPLICATE PREVENTION (2 minutes)

### Step 1: Check Unique Constraint
```sql
-- Verify unique constraint exists
SELECT constraint_name, constraint_type 
FROM information_schema.table_constraints 
WHERE table_name = 'reminder_log' AND constraint_type = 'UNIQUE';

-- Should show: uq_reminder_log | UNIQUE
```

### Step 2: Try to Insert Duplicate
```sql
-- Get a goal ID
SELECT id FROM goal WHERE title = 'Test Reminder Goal' LIMIT 1;

-- Try to insert duplicate reminder
INSERT INTO reminder_log (user_id, entity_type, entity_id, reminder_type, deadline, created_at)
VALUES (1, 'goal', YOUR_GOAL_ID, 'REMINDER_1H', CURRENT_TIMESTAMP + INTERVAL '1 hour', CURRENT_TIMESTAMP);

-- Try again (should fail)
INSERT INTO reminder_log (user_id, entity_type, entity_id, reminder_type, deadline, created_at)
VALUES (1, 'goal', YOUR_GOAL_ID, 'REMINDER_1H', CURRENT_TIMESTAMP + INTERVAL '1 hour', CURRENT_TIMESTAMP);
```

### Expected Result:
✅ First insert succeeds
❌ Second insert fails with error:
```
ERROR: duplicate key value violates unique constraint "uq_reminder_log"
```

---

## PART 4: TEST SELECTIVE NOTIFICATIONS (3 minutes)

### Step 1: Create Goal with Multiple Participants
1. Create a new goal: **"Team Project"**
2. Add participants:
   - User 1 (you): owner, accepted
   - User 2: member, accepted
   - User 3: member, pending
   - User 4: member, rejected

### Step 2: Database Setup
```sql
-- Get the goal ID
SELECT id FROM goal WHERE title = 'Team Project' LIMIT 1;

-- Add participants
INSERT INTO goal_participation (goal_id, user_id, role, status, created_at) VALUES
(YOUR_GOAL_ID, 1, 'owner', 'accepted', CURRENT_TIMESTAMP),
(YOUR_GOAL_ID, 2, 'member', 'accepted', CURRENT_TIMESTAMP),
(YOUR_GOAL_ID, 3, 'member', 'pending', CURRENT_TIMESTAMP),
(YOUR_GOAL_ID, 4, 'member', 'rejected', CURRENT_TIMESTAMP);
```

### Step 3: Set Deadline and Wait
1. Update goal deadline to 1 hour from now
2. Wait 5 minutes for scheduler

### Step 4: Verify Notifications
```sql
-- Check who received notifications
SELECT DISTINCT user_id 
FROM notification 
WHERE entity_id = YOUR_GOAL_ID
ORDER BY user_id;

-- Should show: user_id = 1, 2 (NOT 3 or 4)
```

### Expected Result:
✅ Notifications sent to user 1 (owner) and user 2 (accepted)
❌ NO notifications for user 3 (pending) or user 4 (rejected)

---

## PART 5: TEST OVERDUE MARKING (2 minutes)

### Step 1: Create Overdue Goal
```sql
-- Create goal with deadline in the past
INSERT INTO goal (title, description, start_date, end_date, deadline, status, user_id, created_at, updated_at)
VALUES (
  'Overdue Goal',
  'Testing overdue marking',
  CURRENT_DATE - INTERVAL '10 days',
  CURRENT_DATE - INTERVAL '3 days',
  CURRENT_TIMESTAMP - INTERVAL '1 hour',
  'active',
  1,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
);

-- Get the goal ID
SELECT id FROM goal WHERE title = 'Overdue Goal' LIMIT 1;

-- Add participation
INSERT INTO goal_participation (goal_id, user_id, role, status, created_at)
VALUES (YOUR_GOAL_ID, 1, 'owner', 'accepted', CURRENT_TIMESTAMP);
```

### Step 2: Wait for Scheduler
- Wait 5 minutes for scheduler to run
- Check console for log messages

### Step 3: Verify Overdue Status
```sql
-- Check if goal is marked as overdue
SELECT id, title, deadline, status, is_overdue 
FROM goal 
WHERE title = 'Overdue Goal';

-- Should show: status = 'failed', is_overdue = true

-- Check for missed deadline notification
SELECT type, title 
FROM notification 
WHERE entity_id = YOUR_GOAL_ID;

-- Should show: type = 'DEADLINE_MISSED'
```

### Expected Result:
✅ Goal status changed to 'failed'
✅ is_overdue flag set to true
✅ Notification created with type 'DEADLINE_MISSED'

---

## PART 6: TEST DEADLINE RECALCULATION ON DUPLICATE (2 minutes)

### Step 1: Create Goal with Specific Deadline
1. Click **"Create Goal"**
2. Fill in:
   ```
   Titre:           "Original Project"
   Description:     "Original project description"
   Début:           Today
   Fin:             7 days from today
   Deadline:        May 15, 2026 at 17:30
   Statut:          "active"
   ```
3. Click **OK**

### Step 2: Duplicate the Goal
1. Find the goal card in dashboard
2. Click the **📋 Duplicate** button
3. Confirm the action

### Step 3: Verify Duplicated Goal
In the UI, you should see:
```
Original Project
Deadline: May 15, 2026 at 17:30
Status: active
Progress: 45%

Original Project (Copie)
Deadline: May 22, 2026 at 17:30  ← +7 days
Status: draft
Progress: 0%
```

### Step 4: Verify in Database
```sql
-- Check both goals
SELECT id, title, deadline, status, progress 
FROM goal 
WHERE title LIKE 'Original Project%'
ORDER BY id;

-- Calculate deadline difference
SELECT 
  (SELECT deadline FROM goal WHERE title = 'Original Project (Copie)') - 
  (SELECT deadline FROM goal WHERE title = 'Original Project') AS deadline_diff;

-- Should show: 7 days
```

### Expected Result:
✅ New goal created with "(Copie)" suffix
✅ Deadline = original deadline + 7 days
✅ Status = 'draft'
✅ Progress = 0%

---

## PART 7: VERIFY PRODUCTION ARCHITECTURE (1 minute)

### Step 1: Check Service Layer
```bash
# Verify service files exist
ls -la src/main/java/services/deadline/
ls -la src/main/java/services/notification/
ls -la src/main/java/services/goals_routines/
```

### Step 2: Check Model Layer
```bash
# Verify model files exist
ls -la src/main/java/model/goals_activity_management/
ls -la src/main/java/model/notification/
```

### Step 3: Check Scheduler Integration
```bash
# Verify scheduler is started in GuiApp
grep -n "DeadlineScheduler" src/main/java/GuiApp.java
```

### Step 4: Check Database Schema
```sql
-- Verify deadline columns
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name IN ('goal', 'routine', 'activity') 
AND column_name = 'deadline';

-- Verify notification table
SELECT table_name FROM information_schema.tables 
WHERE table_name IN ('notification', 'reminder_log');

-- Verify indexes
SELECT indexname FROM pg_indexes 
WHERE tablename IN ('notification', 'reminder_log');
```

### Expected Result:
✅ All service files exist and are organized
✅ All model files exist with deadline fields
✅ Scheduler is integrated in GuiApp
✅ Database schema has all required columns and tables

---

## COMPLETE TEST SUMMARY

### ✅ All Features Tested:

| Feature | Status | Evidence |
|---------|--------|----------|
| Create goal with deadline | ✅ | Goal appears in UI with deadline |
| Smart reminders (24h, 1h, reached, missed) | ✅ | Notifications in database |
| Duplicate prevention | ✅ | UNIQUE constraint prevents duplicates |
| Selective notifications | ✅ | Only owner/accepted get notifications |
| Overdue marking | ✅ | Status changed to 'failed', is_overdue=true |
| Deadline recalculation | ✅ | New goal deadline = original + 7 days |
| Production architecture | ✅ | Services, models, scheduler all in place |

---

## QUICK REFERENCE: KEY QUERIES

### Check All Notifications
```sql
SELECT user_id, type, COUNT(*) as count
FROM notification
GROUP BY user_id, type
ORDER BY user_id;
```

### Check All Reminders
```sql
SELECT user_id, entity_type, reminder_type, COUNT(*) as count
FROM reminder_log
GROUP BY user_id, entity_type, reminder_type
ORDER BY user_id;
```

### Check Goals with Deadlines
```sql
SELECT id, title, deadline, status, is_overdue
FROM goal
WHERE deadline IS NOT NULL
ORDER BY deadline ASC;
```

### Check for Duplicate Reminders
```sql
SELECT user_id, entity_id, reminder_type, COUNT(*) as count
FROM reminder_log
GROUP BY user_id, entity_id, reminder_type
HAVING COUNT(*) > 1;
```

### Check Scheduler Logs
```bash
# In console, look for:
[DeadlineManagementService] Starting deadline processing
[DeadlineManagementService] Sent reminder to user
[DeadlineManagementService] Deadline processing completed
```

---

## TROUBLESHOOTING DURING TEST

### Problem: Deadline not showing in UI
**Solution:**
1. Check if goal was saved: `SELECT * FROM goal WHERE title = 'Q2 Project Delivery'`
2. Verify deadline column exists: `SELECT column_name FROM information_schema.columns WHERE table_name = 'goal' AND column_name = 'deadline'`
3. Restart application

### Problem: Reminders not being sent
**Solution:**
1. Check scheduler is running (look for log messages)
2. Verify deadline is in future: `SELECT deadline FROM goal WHERE id = ?`
3. Check user is owner/accepted: `SELECT * FROM goal_participation WHERE goal_id = ?`
4. Wait another 5 minutes for scheduler

### Problem: Duplicate reminders appearing
**Solution:**
1. Check unique constraint: `SELECT constraint_name FROM information_schema.table_constraints WHERE table_name = 'reminder_log'`
2. If missing, run migration: `mvn flyway:migrate`

### Problem: Overdue goal not being marked
**Solution:**
1. Check deadline is in past: `SELECT deadline FROM goal WHERE id = ?`
2. Check goal status is not already 'completed': `SELECT status FROM goal WHERE id = ?`
3. Wait for scheduler to run

---

## EXPECTED TIMELINE

- **0-2 min:** Create goal with deadline
- **2-7 min:** Test reminders (wait for scheduler)
- **7-9 min:** Test duplicate prevention
- **9-12 min:** Test selective notifications
- **12-14 min:** Test overdue marking
- **14-16 min:** Test deadline recalculation
- **16-17 min:** Verify architecture

**Total: ~17 minutes**

---

## NEXT STEPS AFTER TESTING

1. ✅ All features working? Great!
2. Add notification UI component to display notifications
3. Add deadline warning badges to goal cards
4. Add real-time notification refresh
5. Add edit dialog with deadline picker
6. Add notification cleanup task

---

## FINAL CHECKLIST

- [ ] Application starts without errors
- [ ] Can create goal with deadline
- [ ] Deadline displays in UI
- [ ] Scheduler runs every 5 minutes
- [ ] Reminders are created in database
- [ ] No duplicate reminders
- [ ] Only owner/accepted get notifications
- [ ] Overdue goals are marked correctly
- [ ] Duplicate goal has deadline +7 days
- [ ] All services are properly organized
- [ ] Database schema is correct

**✅ All tests passed? Deadline system is production-ready!**
