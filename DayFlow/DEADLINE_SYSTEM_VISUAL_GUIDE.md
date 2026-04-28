# Deadline System - Visual Guide

## What You'll See in the UI

### 1. Goal Card with Deadline Countdown

#### Example 1: Normal (7+ days away)
```
┌─────────────────────────────────────────────────────────────────┐
│ 🎯 Complete Project Report                          ✏️ 📋 🗑️    │
│    Finish the quarterly project report by end of month          │
│                                                                  │
│ [active] [high] [⏰ Still 10 days to end]                       │
│ Progression 45%                                                 │
│ ████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │
│                                                                  │
│ [Rejoindre] [Chatroom] [Détails & routines]                    │
└─────────────────────────────────────────────────────────────────┘
```
**Deadline Badge**: 🟢 Green - "⏰ Still 10 days to end"

---

#### Example 2: Warning (3-7 days away)
```
┌─────────────────────────────────────────────────────────────────┐
│ 🎯 Prepare Presentation                             ✏️ 📋 🗑️    │
│    Create slides and practice for the team meeting              │
│                                                                  │
│ [active] [medium] [⏰ Still 5 days to end]                      │
│ Progression 60%                                                 │
│ ██████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │
│                                                                  │
│ [Rejoindre] [Chatroom] [Détails & routines]                    │
└─────────────────────────────────────────────────────────────────┘
```
**Deadline Badge**: 🟡 Yellow - "⏰ Still 5 days to end"

---

#### Example 3: Urgent (1-3 days away)
```
┌─────────────────────────────────────────────────────────────────┐
│ 🎯 Submit Final Report                              ✏️ 📋 🗑️    │
│    Complete and submit the final project report                 │
│                                                                  │
│ [active] [high] [⏰ Still 2 days to end]                        │
│ Progression 80%                                                 │
│ ████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │
│                                                                  │
│ [Rejoindre] [Chatroom] [Détails & routines]                    │
└─────────────────────────────────────────────────────────────────┘
```
**Deadline Badge**: 🟠 Orange - "⏰ Still 2 days to end"

---

#### Example 4: Critical (Today)
```
┌─────────────────────────────────────────────────────────────────┐
│ 🎯 Finish Documentation                             ✏️ 📋 🗑️    │
│    Complete all documentation before end of day                 │
│                                                                  │
│ [active] [high] [⏰ TODAY (3h left)]                            │
│ Progression 90%                                                 │
│ █████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │
│                                                                  │
│ [Rejoindre] [Chatroom] [Détails & routines]                    │
└─────────────────────────────────────────────────────────────────┘
```
**Deadline Badge**: 🔴 Red - "⏰ TODAY (3h left)"

---

#### Example 5: Overdue (Past deadline)
```
┌─────────────────────────────────────────────────────────────────┐
│ 🎯 Complete Budget Review                           ✏️ 📋 🗑️    │
│    Review and approve the quarterly budget                      │
│                                                                  │
│ [failed] [high] [⏰ OVERDUE]                                    │
│ Progression 50%                                                 │
│ █████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │
│                                                                  │
│ [Rejoindre] [Chatroom] [Détails & routines]                    │
└─────────────────────────────────────────────────────────────────┘
```
**Deadline Badge**: 🔴 Dark Red - "⏰ OVERDUE"

---

### 2. Goal Creation Dialog with Deadline Picker

```
┌─────────────────────────────────────────────────────────────────┐
│                    Créer un objectif                        [X]  │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│ Titre *                                                         │
│ ┌──────────────────────────────────────────────────────────┐   │
│ │ Complete Project Report                                  │   │
│ └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│ Description                                                     │
│ ┌──────────────────────────────────────────────────────────┐   │
│ │ Finish the quarterly project report by end of month     │   │
│ │                                                          │   │
│ └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│ Priorité                                                        │
│ ┌──────────────────────────────────────────────────────────┐   │
│ │ high                                                 ▼   │   │
│ └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│ Deadline (Optional)                                             │
│ ┌──────────────────────────────────────────────────────────┐   │
│ │ [📅 Select Date]  Hour: [10 ▲▼]  Minute: [30 ▲▼]       │   │
│ └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│                                                                  │
│                                    [Annuler]  [Créer]           │
└─────────────────────────────────────────────────────────────────┘
```

**Deadline Picker Components**:
- 📅 Date Picker: Click to select deadline date
- Hour Spinner: 0-23 (24-hour format)
- Minute Spinner: 0-59

---

### 3. Deadline Badge Colors

#### Color Palette

| Status | Color | Hex | RGB | Days |
|--------|-------|-----|-----|------|
| Normal | 🟢 Green | #16a34a | rgb(22, 163, 74) | 7+ |
| Warning | 🟡 Yellow | #eab308 | rgb(234, 179, 8) | 3-7 |
| Urgent | 🟠 Orange | #f97316 | rgb(249, 115, 22) | 1-3 |
| Critical | 🔴 Red | #dc2626 | rgb(220, 38, 38) | Today |
| Overdue | 🔴 Dark Red | #7f1d1d | rgb(127, 29, 29) | Past |

---

### 4. Goal Card Action Buttons

```
┌─────────────────────────────────────────────────────────────────┐
│ 🎯 Complete Project Report                          ✏️ 📋 🗑️    │
│    Finish the quarterly project report by end of month          │
│                                                                  │
│ [active] [high] [⏰ Still 5 days to end]                        │
│ Progression 45%                                                 │
│ ████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │
│                                                                  │
│ [Rejoindre] [Chatroom] [Détails & routines]                    │
└─────────────────────────────────────────────────────────────────┘
```

**Action Buttons**:
- ✏️ **Edit** (Blue): Click to edit goal details and deadline
- 📋 **Duplicate** (Purple): Click to create a copy with deadline +7 days
- 🗑️ **Delete** (Red): Click to delete the goal

---

### 5. Deadline Countdown Examples

#### Timeline View

```
Today: May 1, 2026

Goal 1: Deadline May 11 (10 days away)
├─ Badge: 🟢 Green "⏰ Still 10 days to end"
└─ Status: Normal

Goal 2: Deadline May 6 (5 days away)
├─ Badge: 🟡 Yellow "⏰ Still 5 days to end"
└─ Status: Warning

Goal 3: Deadline May 3 (2 days away)
├─ Badge: 🟠 Orange "⏰ Still 2 days to end"
└─ Status: Urgent

Goal 4: Deadline May 1 (Today)
├─ Badge: 🔴 Red "⏰ TODAY (3h left)"
└─ Status: Critical

Goal 5: Deadline April 30 (1 day ago)
├─ Badge: 🔴 Dark Red "⏰ OVERDUE"
└─ Status: Overdue
```

---

### 6. Duplicate Goal Example

#### Original Goal
```
┌─────────────────────────────────────────────────────────────────┐
│ 🎯 Complete Project Report                          ✏️ 📋 🗑️    │
│    Finish the quarterly project report by end of month          │
│                                                                  │
│ [active] [high] [⏰ Still 5 days to end]                        │
│ Progression 45%                                                 │
│ ████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │
│                                                                  │
│ [Rejoindre] [Chatroom] [Détails & routines]                    │
└─────────────────────────────────────────────────────────────────┘
```
**Original Deadline**: May 6, 2026

#### After Clicking Duplicate (📋)
```
┌─────────────────────────────────────────────────────────────────┐
│ 🎯 Complete Project Report (Copie)                 ✏️ 📋 🗑️    │
│    Finish the quarterly project report by end of month          │
│                                                                  │
│ [draft] [high] [⏰ Still 12 days to end]                        │
│ Progression 0%                                                  │
│ ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │
│                                                                  │
│ [Rejoindre] [Chatroom] [Détails & routines]                    │
└─────────────────────────────────────────────────────────────────┘
```
**New Deadline**: May 13, 2026 (May 6 + 7 days)
**New Status**: draft
**New Progress**: 0%

---

### 7. Edit Goal Dialog

```
┌─────────────────────────────────────────────────────────────────┐
│                    Modifier l'objectif                      [X]  │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│ Titre *                                                         │
│ ┌──────────────────────────────────────────────────────────┐   │
│ │ Complete Project Report                                  │   │
│ └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│ Description                                                     │
│ ┌──────────────────────────────────────────────────────────┐   │
│ │ Finish the quarterly project report by end of month     │   │
│ │                                                          │   │
│ └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│ Priorité                                                        │
│ ┌──────────────────────────────────────────────────────────┐   │
│ │ high                                                 ▼   │   │
│ └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│ Deadline (Optional)                                             │
│ ┌──────────────────────────────────────────────────────────┐   │
│ │ [📅 May 6, 2026]  Hour: [10 ▲▼]  Minute: [30 ▲▼]       │   │
│ └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│                                                                  │
│                                    [Annuler]  [Modifier]        │
└─────────────────────────────────────────────────────────────────┘
```

**To Change Deadline**:
1. Click the date picker to select new date
2. Adjust hour and minute spinners
3. Click "Modifier" to save

---

### 8. Goals Dashboard with Multiple Deadlines

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                  │
│  🎯 GOALS DASHBOARD                                             │
│  Manage your goals and track progress                           │
│                                                                  │
│  [Create Goal]                                                  │
│                                                                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ 🎯 Complete Project Report                    ✏️ 📋 🗑️    │ │
│ │    Finish the quarterly project report                      │ │
│ │ [active] [high] [⏰ Still 10 days to end]                  │ │
│ │ Progression 45%                                            │ │
│ │ ████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │ │
│ │ [Rejoindre] [Chatroom] [Détails & routines]               │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                  │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ 🎯 Prepare Presentation                       ✏️ 📋 🗑️    │ │
│ │    Create slides and practice for the team meeting         │ │
│ │ [active] [medium] [⏰ Still 5 days to end]                │ │
│ │ Progression 60%                                            │ │
│ │ ██████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │ │
│ │ [Rejoindre] [Chatroom] [Détails & routines]               │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                  │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ 🎯 Submit Final Report                        ✏️ 📋 🗑️    │ │
│ │    Complete and submit the final project report            │ │
│ │ [active] [high] [⏰ Still 2 days to end]                  │ │
│ │ Progression 80%                                            │ │
│ │ ████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │ │
│ │ [Rejoindre] [Chatroom] [Détails & routines]               │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                  │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ 🎯 Finish Documentation                       ✏️ 📋 🗑️    │ │
│ │    Complete all documentation before end of day            │ │
│ │ [active] [high] [⏰ TODAY (3h left)]                      │ │
│ │ Progression 90%                                            │ │
│ │ █████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │ │
│ │ [Rejoindre] [Chatroom] [Détails & routines]               │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

### 9. Deadline Badge Styling Details

#### Base Badge Style
```
Padding: 6px vertical, 12px horizontal
Border Radius: 999px (fully rounded pill)
Font Size: 11px
Font Weight: Bold (700)
Text Color: White (default)
```

#### Normal Badge (7+ days)
```
Background: #16a34a (Green)
Text: White
Example: "⏰ Still 10 days to end"
```

#### Warning Badge (3-7 days)
```
Background: #eab308 (Yellow)
Text: #1f2937 (Dark gray for contrast)
Example: "⏰ Still 5 days to end"
```

#### Urgent Badge (1-3 days)
```
Background: #f97316 (Orange)
Text: White
Example: "⏰ Still 2 days to end"
```

#### Critical Badge (Today)
```
Background: #dc2626 (Red)
Text: White
Example: "⏰ TODAY (3h left)"
```

#### Overdue Badge (Past)
```
Background: #7f1d1d (Dark red)
Text: #fca5a5 (Light red)
Example: "⏰ OVERDUE"
```

---

## Summary

The deadline system provides:

1. ✅ **Visual Countdown**: See how many days until deadline
2. ✅ **Color-Coded Urgency**: Quick visual indication of deadline status
3. ✅ **Easy Deadline Setting**: Simple date/time picker
4. ✅ **Automatic Recalculation**: Duplicate goals get +7 days
5. ✅ **Smart Reminders**: Background notifications at key times
6. ✅ **Persistent Data**: Deadlines saved across sessions

**All features are now visible and functional in the UI!**
