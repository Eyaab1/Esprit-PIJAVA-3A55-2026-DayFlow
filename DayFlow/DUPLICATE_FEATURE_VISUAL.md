# Goal Duplicate Feature - Visual Guide

## Goal Card with All Actions

```
┌────────────────────────────────────────────────────────────────┐
│                                                                │
│  🎯  Goal Title                                ✏️  📋  🗑️     │
│      Goal description text goes here...                        │
│                                                                │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  ACTIVE  priority                                             │
│                                                                │
│  Progression 0%                                               │
│  ████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │
│                                                                │
│  [Rejoindre]  [Chatroom]  [Détails & routines]               │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

## Action Icons

### Edit Button (✏️)
- **Color**: Blue
- **Action**: Edit goal details
- **Position**: Left

### Duplicate Button (📋)
- **Color**: Purple
- **Action**: Create a copy of the goal
- **Position**: Middle

### Delete Button (🗑️)
- **Color**: Red
- **Action**: Delete the goal
- **Position**: Right

## Duplicate Process Flow

```
User clicks 📋 icon
    ↓
System retrieves original goal data
    ↓
Creates new goal with:
  • Same title + " (Copie)"
  • Same description
  • Same dates
  • Same priority
  • Status = "draft"
  • Progress = 0%
    ↓
Inserts new goal into database
    ↓
Creates chatroom for new goal
    ↓
Creates participation record (user as owner)
    ↓
Shows success message
    ↓
Dashboard refreshes
    ↓
New goal appears in list
```

## Before and After

### Before Duplicate
```
Goal List:
┌─────────────────────────────────────────┐
│ 🎯 Learn JavaFX                         │
│    Complete JavaFX tutorial             │
│ ACTIVE                                  │
│ Progression 45%                         │
└─────────────────────────────────────────┘
```

### After Clicking 📋
```
Goal List:
┌─────────────────────────────────────────┐
│ 🎯 Learn JavaFX                         │
│    Complete JavaFX tutorial             │
│ ACTIVE                                  │
│ Progression 45%                         │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│ 🎯 Learn JavaFX (Copie)                 │
│    Complete JavaFX tutorial             │
│ DRAFT                                   │
│ Progression 0%                          │
└─────────────────────────────────────────┘
```

## Data Comparison

### Original Goal
```
Title: Learn JavaFX
Description: Complete JavaFX tutorial
Start Date: 2026-04-28
End Date: 2026-05-28
Status: ACTIVE
Priority: HIGH
Progress: 45%
Required Tasks: 10
```

### Duplicated Goal
```
Title: Learn JavaFX (Copie)
Description: Complete JavaFX tutorial
Start Date: 2026-04-28
End Date: 2026-05-28
Status: DRAFT
Priority: HIGH
Progress: 0%
Required Tasks: 10
```

## Color Scheme

### Button Colors
```
Edit Button:
  Default: #3b82f6 (Blue)
  Hover: #2563eb (Darker Blue)

Duplicate Button:
  Default: #8b5cf6 (Purple)
  Hover: #7c3aed (Darker Purple)

Delete Button:
  Default: #ef4444 (Red)
  Hover: #dc2626 (Darker Red)
```

## Success Message

When you duplicate a goal, you'll see:

```
┌─────────────────────────────────────────┐
│  Information                            │
├─────────────────────────────────────────┤
│                                         │
│  Objectif dupliqué avec succès.         │
│  La copie a été créée en tant que       │
│  brouillon.                             │
│                                         │
│  [OK]                                   │
│                                         │
└─────────────────────────────────────────┘
```

## Use Cases

### 1. Recurring Goals
```
Original: "Weekly Exercise"
Duplicate: "Weekly Exercise (Copie)"
→ Edit to change dates for next week
```

### 2. Similar Projects
```
Original: "Project A - Phase 1"
Duplicate: "Project A - Phase 1 (Copie)"
→ Edit to create "Project A - Phase 2"
```

### 3. Template Goals
```
Original: "Monthly Review Template"
Duplicate: "Monthly Review Template (Copie)"
→ Edit for each month
```

## Interaction Timeline

```
T0: User sees goal card with 3 icons
    ✏️ Edit | 📋 Duplicate | 🗑️ Delete

T1: User hovers over 📋 icon
    Button changes to darker purple

T2: User clicks 📋 icon
    System processes duplicate request

T3: New goal is created
    Success message appears

T4: Dashboard refreshes
    New goal appears in list with "(Copie)" suffix
    New goal has status "DRAFT"
    New goal has progress 0%
```

## Features Summary

✅ One-click duplication
✅ Automatic title suffix
✅ All data copied
✅ Status reset to draft
✅ Progress reset to 0%
✅ Automatic chatroom creation
✅ Automatic participation creation
✅ Success notification
✅ Dashboard auto-refresh
✅ Ready to edit immediately

## Tips

1. **Duplicate then Edit**: Create a copy and immediately edit it to customize
2. **Batch Creation**: Duplicate multiple times to create similar goals
3. **Template Goals**: Create a template goal and duplicate it for each instance
4. **Safe Operation**: Duplicating doesn't affect the original goal
5. **Draft Status**: New goals are draft, so you can review before activating
