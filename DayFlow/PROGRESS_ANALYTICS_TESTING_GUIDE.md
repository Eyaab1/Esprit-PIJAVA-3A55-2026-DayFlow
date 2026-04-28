# Progress Analytics System - Testing Guide

## How to Test the Analytics System

---

## Test 1: Progress Calculation

### Scenario
Create a goal with activities and verify automatic progress calculation.

### Steps
1. Create a goal with 10 activities
2. Mark 7 activities as completed
3. Check goal progress

### Expected Result
```
Goal Progress: 70%
(7 completed / 10 total = 70%)
```

### Verification
```java
ProgressAnalyticsService service = new ProgressAnalyticsService();
int progress = service.calculateGoalProgress(goalId);
// Should return: 70
```

### Database Verification
```sql
SELECT id, title, progress FROM goal WHERE id = ?;
-- progress should be 70
```

---

## Test 2: Productivity Score

### Scenario
Calculate overall user productivity score.

### Steps
1. User has 10 goals, 7 completed (70%)
2. User has 50 activities, 40 completed (80%)
3. User has 5 goals with deadlines, 4 completed on time (80%)

### Expected Result
```
Productivity Score = (70 * 0.4) + (80 * 0.4) + (80 * 0.2)
                   = 28 + 32 + 16
                   = 76.0
```

### Verification
```java
double score = service.calculateProductivityScore(userId);
// Should return: 76.0
```

---

## Test 3: Overdue Detection

### Scenario
Detect goals that are overdue.

### Steps
1. Create a goal with deadline = yesterday
2. Set progress = 50%
3. Wait for scheduler to run (or call manually)

### Expected Result
```
Goal Status: 'failed' (OVERDUE)
Notification: "⚠️ Goal 'Test Goal' is now OVERDUE!"
```

### Verification
```java
List<Goal> overdueGoals = service.detectOverdueGoals();
// Should contain the test goal

service.markGoalAsOverdue(goalId);
// Goal status should be 'failed'
```

### Database Verification
```sql
SELECT * FROM overdue_goals WHERE user_id = ?;
-- Should show the overdue goal

SELECT * FROM notification 
WHERE entity_type = 'GOAL' 
AND entity_id = ? 
AND type = 'DEADLINE_MISSED';
-- Should have notification
```

---

## Test 4: Inactive Routine Detection

### Scenario
Detect routines with no activity for 7+ days.

### Steps
1. Create a routine with activities
2. Complete an activity 8 days ago
3. Wait for scheduler to run (or call manually)

### Expected Result
```
Routine Status: 'paused' (INACTIVE)
Notification: "⚠️ Routine 'Test Routine' has been inactive for 7+ days"
```

### Verification
```java
List<Routine> inactiveRoutines = service.detectInactiveRoutines(7);
// Should contain the test routine

int days = service.getDaysSinceLastActivity(routineId);
// Should return: 8
```

### Database Verification
```sql
SELECT * FROM inactive_routines;
-- Should show the inactive routine
```

---

## Test 5: Deadline Warnings

### Scenario
Generate warnings for goals with approaching deadlines.

### Steps
1. Create a goal with deadline = 2 days from now
2. Set progress = 30%
3. Wait for scheduler to run (or call manually)

### Expected Result
```
Warning: "⚠️ WARNING: Deadline in 2 days but progress is only 30%!"
Notification created
```

### Verification
```java
List<Goal> warningGoals = service.detectGoalsNeedingWarning();
// Should contain the test goal

String warning = service.generateWarningMessage(goal);
// Should return warning message
```

---

## Test 6: Performance Report

### Scenario
Generate comprehensive performance report for a goal.

### Steps
1. Create a goal with:
   - Progress: 85%
   - Deadline: 10 days away
   - 10 activities, 8 completed
   - 3 routines, 2 completed

### Expected Result
```
Performance Level: EXCELLENT_PROGRESS
Progress: 85%
Completion Rate: 80%
Days Until Deadline: 10
Total Activities: 10
Completed Activities: 8
Pending Activities: 2
Total Routines: 3
Completed Routines: 2
Active Routines: 1
Warning: null
Recommendation: "✅ Great work! Keep up the momentum to complete this goal."
Needs Attention: false
At Risk: false
```

### Verification
```java
GoalPerformanceReport report = service.generatePerformanceReport(goalId);

System.out.println("Performance: " + report.getPerformanceLevel());
System.out.println("Progress: " + report.getProgress() + "%");
System.out.println("Completion Rate: " + report.getCompletionRate() + "%");
System.out.println("Recommendation: " + report.getRecommendation());
```

---

## Test 7: Statistics Dashboard

### Scenario
Get comprehensive user statistics.

### Steps
1. User has:
   - 15 total goals
   - 10 completed goals
   - 3 active goals
   - 2 overdue goals
   - 50 total activities
   - 35 completed activities

### Expected Result
```
Total Goals: 15
Completed Goals: 10
Active Goals: 3
Overdue Goals: 2
Goal Completion Rate: 66.67%
Total Activities: 50
Completed Activities: 35
Activity Completion Rate: 70.0%
Productivity Score: ~68.0
```

### Verification
```java
ProgressStatistics stats = service.getProgressStatistics(userId);

System.out.println("Total Goals: " + stats.getTotalGoals());
System.out.println("Completed Goals: " + stats.getCompletedGoals());
System.out.println("Overdue Goals: " + stats.getOverdueGoals());
System.out.println("Productivity Score: " + stats.getOverallProductivityScore());
```

---

## Test 8: Automatic Trigger

### Scenario
Verify automatic progress update when activity status changes.

### Steps
1. Create a goal with 10 activities
2. Initial progress: 0%
3. Mark 1 activity as completed
4. Check goal progress (should auto-update)

### Expected Result
```
Before: Progress = 0%
After marking 1 activity completed: Progress = 10%
(Automatic update via database trigger)
```

### Verification
```sql
-- Before
SELECT progress FROM goal WHERE id = ?;
-- Returns: 0

-- Mark activity as completed
UPDATE activity SET status = 'completed' WHERE id = ?;

-- After (automatic update)
SELECT progress FROM goal WHERE id = ?;
-- Returns: 10
```

---

## Test 9: Scheduler Integration

### Scenario
Verify scheduler runs automatically every 30 minutes.

### Steps
1. Start application
2. Check logs for scheduler startup
3. Wait 30 minutes
4. Check logs for scheduler execution

### Expected Result
```
[AnalyticsScheduler] Starting analytics scheduler...
[AnalyticsScheduler] Scheduler started. Running every 30 minutes

... 30 minutes later ...

[AnalyticsScheduler] Running analytics check at 2026-04-28T22:30:00
[AnalyticsScheduler] Processed 2 overdue goals
[AnalyticsScheduler] Processed 1 inactive routines
[AnalyticsScheduler] Processed 3 deadline warnings
```

### Verification
Check application logs for scheduler messages.

---

## Test 10: Performance Levels

### Scenario
Test all 4 performance levels.

### Test Cases

**Case 1: EXCELLENT_PROGRESS**
```
Progress: 90%
Deadline: 5 days away
Expected: PerformanceLevel.EXCELLENT_PROGRESS
```

**Case 2: ON_TRACK**
```
Progress: 60%
Deadline: 10 days away
Created: 5 days ago
Expected: PerformanceLevel.ON_TRACK
```

**Case 3: NEEDS_ATTENTION**
```
Progress: 40%
Deadline: 3 days away
Expected: PerformanceLevel.NEEDS_ATTENTION
```

**Case 4: CRITICAL_DELAY**
```
Progress: 30%
Deadline: 1 day away
Expected: PerformanceLevel.CRITICAL_DELAY
```

### Verification
```java
for (int goalId : testGoalIds) {
    GoalPerformanceReport report = service.generatePerformanceReport(goalId);
    System.out.println("Goal " + goalId + ": " + report.getPerformanceLevel());
}
```

---

## Database Testing

### Test Views

**Test goal_progress_summary**
```sql
SELECT * FROM goal_progress_summary WHERE user_id = ?;
-- Should show all goals with calculated progress
```

**Test user_productivity_summary**
```sql
SELECT * FROM user_productivity_summary WHERE user_id = ?;
-- Should show user-level metrics
```

**Test overdue_goals**
```sql
SELECT * FROM overdue_goals WHERE user_id = ?;
-- Should show only overdue goals
```

**Test inactive_routines**
```sql
SELECT * FROM inactive_routines;
-- Should show routines inactive for 7+ days
```

**Test goals_needing_attention**
```sql
SELECT * FROM goals_needing_attention WHERE user_id = ?;
-- Should show goals with urgency levels
```

### Test Functions

**Test calculate_goal_progress**
```sql
SELECT calculate_goal_progress(123);
-- Should return progress percentage
```

**Test calculate_routine_progress**
```sql
SELECT calculate_routine_progress(456);
-- Should return progress percentage
```

### Test Triggers

**Test trigger_update_goal_progress**
```sql
-- Insert activity
INSERT INTO activity (title, start_time, duration, status, routine_id)
VALUES ('Test Activity', NOW(), '01:00:00', 'completed', 456);

-- Check goal progress (should auto-update)
SELECT progress FROM goal WHERE id = (
    SELECT goal_id FROM routine WHERE id = 456
);
```

---

## Performance Testing

### Test 1: Large Dataset
```
Create 100 goals with 1000 activities
Measure time to calculate all progress
Expected: < 5 seconds
```

### Test 2: Concurrent Access
```
10 users accessing statistics simultaneously
Measure response time
Expected: < 2 seconds per user
```

### Test 3: Scheduler Performance
```
1000 goals to check for overdue
Measure scheduler execution time
Expected: < 10 seconds
```

---

## Integration Testing

### Test Complete Workflow

**Scenario**: User creates goal, adds activities, completes them, gets performance report

**Steps**:
1. Create goal with deadline = 7 days from now
2. Add 10 activities
3. Complete 5 activities (progress = 50%)
4. Get performance report
5. Complete 3 more activities (progress = 80%)
6. Get updated performance report
7. Wait for deadline to pass
8. Verify overdue detection

**Expected Results**:
- Step 4: Performance = ON_TRACK
- Step 6: Performance = EXCELLENT_PROGRESS
- Step 8: Status = 'failed', Notification created

---

## Error Handling Testing

### Test 1: Invalid Goal ID
```java
try {
    int progress = service.calculateGoalProgress(99999);
    // Should return 0 or handle gracefully
} catch (SQLException e) {
    // Should not crash
}
```

### Test 2: Null Values
```java
Goal goal = new Goal();
goal.setDeadline(null);
String warning = service.generateWarningMessage(goal);
// Should return null (no warning)
```

### Test 3: Database Connection Error
```java
// Simulate database connection failure
// Scheduler should log error and continue
```

---

## Visual Testing Examples

### Example 1: Statistics Display

```
┌─────────────────────────────────────────────────────────┐
│              USER PRODUCTIVITY DASHBOARD                │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  📊 Overall Productivity Score: 82.5%                   │
│                                                          │
│  🎯 Goals                                               │
│     Total: 15                                           │
│     Completed: 10 (66.67%)                              │
│     Active: 3                                           │
│     Overdue: 2                                          │
│                                                          │
│  📋 Activities                                          │
│     Total: 50                                           │
│     Completed: 35 (70.0%)                               │
│     Pending: 15                                         │
│                                                          │
│  ⏰ Deadlines                                           │
│     Missed: 2                                           │
│     Upcoming (7 days): 3                                │
│                                                          │
│  📈 This Week                                           │
│     Goals Completed: 2                                  │
│     Activities Completed: 12                            │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### Example 2: Performance Report

```
┌─────────────────────────────────────────────────────────┐
│           GOAL PERFORMANCE REPORT                       │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Goal: Complete Project Report                          │
│  Status: active                                         │
│                                                          │
│  🌟 Performance: EXCELLENT_PROGRESS                     │
│  📊 Progress: 85%                                       │
│  ⏰ Days Until Deadline: 10                             │
│  ✅ Completion Rate: 80.0%                              │
│                                                          │
│  Activities:                                            │
│     Total: 10                                           │
│     Completed: 8                                        │
│     Pending: 2                                          │
│                                                          │
│  Routines:                                              │
│     Total: 3                                            │
│     Completed: 2                                        │
│     Active: 1                                           │
│                                                          │
│  💡 Recommendation:                                     │
│     ✅ Great work! Keep up the momentum to complete    │
│     this goal.                                          │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### Example 3: Warning Display

```
┌─────────────────────────────────────────────────────────┐
│              GOALS NEEDING ATTENTION                    │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  🚨 CRITICAL DELAY                                      │
│     Goal: Submit Final Report                           │
│     Progress: 30%                                       │
│     Deadline: 1 day away                                │
│     Warning: Deadline is tomorrow with low progress!    │
│     Action: Immediate action required!                  │
│                                                          │
│  ⚠️ NEEDS ATTENTION                                     │
│     Goal: Prepare Presentation                          │
│     Progress: 45%                                       │
│     Deadline: 3 days away                               │
│     Warning: Deadline approaching with low progress     │
│     Action: Increase effort to meet deadline            │
│                                                          │
│  ⚠️ OVERDUE                                             │
│     Goal: Complete Budget Review                        │
│     Progress: 60%                                       │
│     Days Overdue: 2                                     │
│     Warning: This goal is OVERDUE!                      │
│     Action: Reassess priorities                         │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

---

## Testing Checklist

### Unit Tests
- [ ] Progress calculation methods
- [ ] Productivity score calculation
- [ ] Overdue detection logic
- [ ] Inactive routine detection
- [ ] Warning message generation
- [ ] Performance evaluation algorithm
- [ ] Recommendation generation

### Integration Tests
- [ ] Database views
- [ ] Database functions
- [ ] Database triggers
- [ ] Scheduler execution
- [ ] Notification creation
- [ ] Service layer integration

### System Tests
- [ ] Complete workflow
- [ ] Real data testing
- [ ] Performance under load
- [ ] Error handling
- [ ] Edge cases
- [ ] Concurrent access

### User Acceptance Tests
- [ ] Statistics accuracy
- [ ] Performance report accuracy
- [ ] Warning relevance
- [ ] Recommendation usefulness
- [ ] Notification timeliness

---

## Summary

The Progress Analytics System provides comprehensive testing capabilities:

✅ **10 test scenarios** covering all features
✅ **Database testing** for views, functions, triggers
✅ **Performance testing** for large datasets
✅ **Integration testing** for complete workflows
✅ **Error handling testing** for edge cases
✅ **Visual examples** for UI implementation

All tests are documented with:
- Clear scenarios
- Step-by-step instructions
- Expected results
- Verification methods
- Database queries

**The system is ready for comprehensive testing!**
