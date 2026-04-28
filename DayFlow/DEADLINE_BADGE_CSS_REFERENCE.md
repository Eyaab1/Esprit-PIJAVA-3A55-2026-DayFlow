# Deadline Badge CSS Styling Reference

## CSS Styles Added to `goals_dashboard.css`

The following CSS styles were added to support the deadline countdown display on goal cards:

```css
/* Deadline Badge Styles */
.badge-deadline {
    -fx-padding: 6 12;
    -fx-background-radius: 999;
    -fx-font-size: 11px;
    -fx-font-weight: 700;
    -fx-text-fill: white;
}

.badge-deadline-normal {
    -fx-background-color: #16a34a;
}

.badge-deadline-warning {
    -fx-background-color: #eab308;
    -fx-text-fill: #1f2937;
}

.badge-deadline-urgent {
    -fx-background-color: #f97316;
    -fx-text-fill: white;
}

.badge-deadline-critical {
    -fx-background-color: #dc2626;
    -fx-text-fill: white;
}

.badge-deadline-overdue {
    -fx-background-color: #7f1d1d;
    -fx-text-fill: #fca5a5;
}
```

## Style Breakdown

### Base Style: `.badge-deadline`
- **Padding**: 6px vertical, 12px horizontal
- **Border Radius**: 999px (fully rounded pill shape)
- **Font Size**: 11px
- **Font Weight**: 700 (bold)
- **Text Color**: White (default)

### Color Variants

#### `.badge-deadline-normal` (7+ days remaining)
- **Background**: `#16a34a` (Green)
- **Text**: White
- **Use Case**: Goal has plenty of time

#### `.badge-deadline-warning` (3-7 days remaining)
- **Background**: `#eab308` (Yellow)
- **Text**: `#1f2937` (Dark gray for contrast)
- **Use Case**: Goal deadline is approaching

#### `.badge-deadline-urgent` (1-3 days remaining)
- **Background**: `#f97316` (Orange)
- **Text**: White
- **Use Case**: Goal deadline is very soon

#### `.badge-deadline-critical` (Today or expired)
- **Background**: `#dc2626` (Red)
- **Text**: White
- **Use Case**: Goal deadline is today or has passed

#### `.badge-deadline-overdue` (Past deadline)
- **Background**: `#7f1d1d` (Dark red)
- **Text**: `#fca5a5` (Light red)
- **Use Case**: Goal deadline has been missed

## How It's Used in Java Code

In `GoalsDashboardController.java`, the `createDeadlineLabel()` method:

```java
private Label createDeadlineLabel(Goal g) {
    // ... calculate days remaining ...
    
    String styleClass = "badge-deadline";
    
    if (daysRemaining < 0) {
        styleClass = "badge-deadline-overdue";
    } else if (daysRemaining == 0) {
        styleClass = "badge-deadline-critical";
    } else if (daysRemaining <= 3) {
        styleClass = "badge-deadline-urgent";
    } else if (daysRemaining <= 7) {
        styleClass = "badge-deadline-warning";
    } else {
        styleClass = "badge-deadline-normal";
    }
    
    Label deadlineLabel = new Label(deadlineText);
    deadlineLabel.getStyleClass().addAll("badge", styleClass);
    return deadlineLabel;
}
```

The label is then added to the goal card's badge row:

```java
if (g.getDeadline() != null) {
    Label deadlineLabel = createDeadlineLabel(g);
    badges.getChildren().add(deadlineLabel);
}
```

## Color Palette Reference

| Class | Hex Color | RGB | Use Case |
|-------|-----------|-----|----------|
| `.badge-deadline-normal` | `#16a34a` | rgb(22, 163, 74) | 7+ days |
| `.badge-deadline-warning` | `#eab308` | rgb(234, 179, 8) | 3-7 days |
| `.badge-deadline-urgent` | `#f97316` | rgb(249, 115, 22) | 1-3 days |
| `.badge-deadline-critical` | `#dc2626` | rgb(220, 38, 38) | Today |
| `.badge-deadline-overdue` | `#7f1d1d` | rgb(127, 29, 29) | Past |

## Visual Examples

### Goal Card with Deadline Badge

```
┌─────────────────────────────────────────────────────────┐
│ 🎯 Complete Project Report                    ✏️ 📋 🗑️  │
│    Finish the quarterly project report by end of month  │
│                                                          │
│ [active] [high] [⏰ Still 5 days to end]                │
│ Progression 45%                                         │
│ ████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │
│                                                          │
│ [Rejoindre] [Chatroom] [Détails & routines]            │
└─────────────────────────────────────────────────────────┘
```

The deadline badge appears in the same row as the status and priority badges.

## Testing the Styles

To verify the CSS styles are working:

1. **Create a goal with deadline 10 days away**
   - Expected: Green badge with `⏰ Still 10 days to end`

2. **Create a goal with deadline 5 days away**
   - Expected: Yellow badge with `⏰ Still 5 days to end`

3. **Create a goal with deadline 2 days away**
   - Expected: Orange badge with `⏰ Still 2 days to end`

4. **Create a goal with deadline today**
   - Expected: Red badge with `⏰ TODAY (Xh left)`

5. **Create a goal with deadline yesterday**
   - Expected: Dark red badge with `⏰ OVERDUE`

## File Location

The CSS styles are located in:
```
src/main/resources/user/goals_routines/goals_dashboard.css
```

Lines added at the end of the file (after `.btn-icon-delete:hover`).

## Compilation Status

✅ **Project compiles successfully** with all deadline badge styles included.

The CSS is automatically loaded when the Goals Dashboard view is rendered.
