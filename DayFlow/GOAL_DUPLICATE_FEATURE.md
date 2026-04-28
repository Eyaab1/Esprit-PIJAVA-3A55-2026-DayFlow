# Goal Duplicate Feature - Implementation Summary

## What Was Added

Added a duplicate goal feature (📋 icon) to each goal card in the "Mes Objectifs" dashboard.

## Features

### Duplicate Goal (📋 icon)
- Click the purple duplicate icon on any goal card
- Creates an exact copy of the goal with all the same data
- The duplicated goal is created with:
  - Same title + " (Copie)" suffix
  - Same description
  - Same start and end dates
  - Same priority
  - Status set to "draft" (new goals start as draft)
  - Progress reset to 0%
  - New chatroom and participation created automatically

## Changes Made

### 1. Updated GoalsDashboardController.java

**Modified Method**: `buildGoalCard(GoalListRow row)`
- Added duplicate button (📋) between edit and delete buttons
- Button appears in the top-right corner of each card

**New Method Added**:
- `onDuplicateGoal(int goalId)` - Creates a copy of the goal

### 2. Updated goals_dashboard.css

**New CSS Class**:
- `.btn-icon-duplicate` - Purple duplicate button styling

**Styling Details**:
- Duplicate button: Purple (#8b5cf6) with hover effect (#7c3aed)
- Rounded corners and proper padding

## UI Layout

```
┌─────────────────────────────────────────────────────┐
│ 🎯 Goal Title                      ✏️  📋  🗑️      │
│    Goal description...                              │
├─────────────────────────────────────────────────────┤
│ ACTIVE  priority                                    │
│ Progression 0%                                      │
│ ████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │
│ [Rejoindre] [Chatroom] [Détails & routines]        │
└─────────────────────────────────────────────────────┘
```

## How It Works

### Duplicate Process
1. User clicks 📋 icon on a goal card
2. System retrieves the original goal data
3. Creates a new goal with:
   - Same title + " (Copie)"
   - Same description
   - Same dates
   - Same priority
   - Status = "draft"
   - Progress = 0%
4. Inserts the new goal into the database
5. Creates a chatroom for the new goal
6. Creates a participation record (user as owner)
7. Shows success message
8. Dashboard refreshes to show the new goal

## Code Implementation

### GoalsDashboardController.java

```java
// In buildGoalCard method:
Button duplicateBtn = new Button("📋");
duplicateBtn.getStyleClass().add("btn-icon-duplicate");
duplicateBtn.setStyle("-fx-font-size: 16px; -fx-padding: 4px 8px;");
duplicateBtn.setOnAction(e -> onDuplicateGoal(goalId));

actionIcons.getChildren().addAll(editBtn, duplicateBtn, deleteBtn);

// New method:
private void onDuplicateGoal(int goalId) {
    // Get original goal
    // Create new goal with same data
    // Set status to "draft"
    // Reset progress to 0
    // Insert new goal
    // Create chatroom and participation
    // Show success message
    // Refresh dashboard
}
```

### goals_dashboard.css

```css
.btn-icon-duplicate {
    -fx-background-color: #8b5cf6;
    -fx-text-fill: white;
    -fx-font-weight: 600;
    -fx-padding: 6 10;
    -fx-background-radius: 6;
    -fx-cursor: hand;
}

.btn-icon-duplicate:hover {
    -fx-background-color: #7c3aed;
}
```

## Button Colors

| Button | Color | Hover | Icon |
|--------|-------|-------|------|
| Edit | Blue (#3b82f6) | #2563eb | ✏️ |
| Duplicate | Purple (#8b5cf6) | #7c3aed | 📋 |
| Delete | Red (#ef4444) | #dc2626 | 🗑️ |

## What Gets Copied

✅ Title (with " (Copie)" suffix)
✅ Description
✅ Start date
✅ End date
✅ Priority
✅ Required tasks

## What Gets Reset

🔄 Status → "draft"
🔄 Progress → 0%
🔄 ID → New ID (auto-generated)
🔄 Created date → Current date/time
🔄 Updated date → Current date/time

## What Gets Created

✨ New chatroom for the duplicated goal
✨ New participation record (user as owner)

## Testing

1. Run the application: `mvn javafx:run`
2. Navigate to "Mes Objectifs"
3. Create or view a goal
4. Click the 📋 icon on any goal card
5. A new goal will be created with " (Copie)" suffix
6. The new goal will appear in the list with status "draft"

## Files Modified

1. `src/main/java/controllers/goals_routines/GoalsDashboardController.java`
   - Updated `buildGoalCard()` method
   - Added `onDuplicateGoal()` method

2. `src/main/resources/user/goals_routines/goals_dashboard.css`
   - Added `.btn-icon-duplicate` styling

## Compilation Status

✅ Code compiles successfully
✅ No errors or warnings
✅ Ready to test

## User Experience

1. User sees goal card with three action icons
2. Clicks 📋 to duplicate
3. System creates a copy instantly
4. Success message appears
5. Dashboard refreshes
6. New goal appears in the list with "(Copie)" suffix
7. New goal is in "draft" status and ready to be edited

## Benefits

- Quick way to create similar goals
- No need to manually re-enter all data
- Useful for recurring goal patterns
- New goals start as draft for review before activation
- Automatic chatroom creation for collaboration
