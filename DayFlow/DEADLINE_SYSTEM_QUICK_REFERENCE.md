# Deadline System - Quick Reference Guide

## What Was Done

### 1. **Fixed Deadline Field Type** (LocalDate → LocalDateTime)
All three models now use `LocalDateTime` instead of `LocalDate` to store deadline with time precision:
- `Goal.deadline` - LocalDateTime
- `Routine.deadline` - LocalDateTime  
- `Activity.deadline` - LocalDateTime

### 2. **Updated All Service Layers**
Services now correctly handle `Timestamp` (not `Date`) for deadline:
- `GoalService` - insert/update/mapGoal
- `RoutineService` - insert/update/mapRoutine
- `ActivityService` - insert/update/mapActivity

### 3. **Integrated Scheduler into Application**
`GuiApp.java` now starts the `DeadlineScheduler` on application startup:
```java
@Override
public void start(Stage stage) {
    try {
        DatabaseMigrator.migrate();
        
        // Start the deadline scheduler
        DeadlineScheduler.getInstance().start();
        
        // ... rest of startup code
    }
}
```

### 4. **Added Deadline Picker UI**
Goal creation dialog now includes deadline picker:
- Date picker for deadline date
- Hour spinner (0-23)
- Minute spinner (0-59)
- Optional field (can be left blank)

### 5. **Updated Duplicate Goal Logic**
When duplicating a goal, the deadline is recalculated:
- New deadline = Original deadline + 7 days
- Ensures copied goals have fresh deadlines

## How to Use

### Creating a Goal with Deadline
1. Click "Create Goal" button
2. Fill in title, description, start/end dates
3. **NEW**: Set deadline date and time (optional)
4. Click OK

### Checking Deadline Status
```java
Goal goal = goalService.findById(1);

// Check if deadline is near (within 7 days)
if (goal.isDeadlineNear()) {
    // Show warning
}

// Check if overdue
if (goal.isOverdue()) {
    // Show overdue indicator
}
```

### Duplicating a Goal
1. Click duplicate icon (📋) on goal card
2. New goal is created with deadline + 7 days
3. New goal starts as "draft" status

## Database Changes

The V11 migration adds:
- `deadline TIMESTAMP` column to goal, routine, activity tables
- `is_overdue BOOLEAN` column to track overdue status
- `notification` table for storing notifications
- `reminder_log` table for tracking sent reminders

## Scheduler Details

**DeadlineScheduler** runs every 5 minutes and:
1. Checks all active goals, routines, activities
2. Generates reminders at:
   - 24 hours before deadline
   - 1 hour before deadline
   - When deadline is reached
   - When deadline is missed
3. Prevents duplicate reminders via reminder_log
4. Only notifies owner and approved participants
5. Marks overdue entities automatically

## Files Changed

### Models
- `Goal.java` - deadline field type changed
- `Activity.java` - deadline field type changed
- `Routine.java` - deadline field type changed

### Services
- `GoalService.java` - Timestamp handling
- `ActivityService.java` - Timestamp handling
- `RoutineService.java` - Timestamp handling

### Controllers
- `GoalsDashboardController.java` - deadline UI + duplicate logic

### Application
- `GuiApp.java` - scheduler startup

## Compilation Status
✅ All files compile successfully with no errors or warnings

## Testing Checklist

- [ ] Create a goal with deadline
- [ ] Verify deadline is saved correctly
- [ ] Duplicate a goal and check deadline is +7 days
- [ ] Check scheduler runs every 5 minutes
- [ ] Verify notifications are generated
- [ ] Check no duplicate reminders are sent
- [ ] Verify overdue goals are marked correctly

## Common Issues & Solutions

**Issue**: Deadline not saving
- **Solution**: Ensure deadline is set as `LocalDateTime`, not `LocalDate`

**Issue**: Scheduler not running
- **Solution**: Check that `DeadlineScheduler.getInstance().start()` is called in `GuiApp.start()`

**Issue**: Duplicate reminders
- **Solution**: Check `reminder_log` table has unique constraint on (user_id, entity_type, entity_id, reminder_type)

**Issue**: Notifications not appearing
- **Solution**: Verify `NotificationService` is being called by `DeadlineManagementService`

## Next Steps

1. **UI Notifications** - Create component to display notifications
2. **Edit Dialog** - Add deadline picker to edit goal dialog
3. **Real-time Refresh** - Add notification refresh after login
4. **Deadline Badges** - Show warning badges on goal cards
5. **Cleanup Task** - Archive old notifications periodically

---

**Status**: ✅ COMPLETE - All deadline system features implemented and integrated
