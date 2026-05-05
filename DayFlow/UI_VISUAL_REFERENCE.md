# 🎨 UI VISUAL REFERENCE - Session Counter Display

## Calendar Interface Layout

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│  📅 Disponibilités - Coach Name                    [View Mode ▼] │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────────┐
│  │ Sessions futures: 0/3                                       │
│  │ Vous pouvez réserver 3 session(s)                           │
│  └─────────────────────────────────────────────────────────────┘
│                                                                 │
│  ◀ Précédent        Mai 2026        Suivant ▶                 │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────────┐
│  │ Calendar Grid (7 columns x 6 rows)                          │
│  │ Lun  Mar  Mer  Jeu  Ven  Sam  Dim                           │
│  │  1    2    3    4    5    6    7                            │
│  │  8    9   10   11   12   13   14                            │
│  │ ...                                                         │
│  └─────────────────────────────────────────────────────────────┘
│                                                                 │
│  ┌──────────────────────┐  ┌──────────────────────┐            │
│  │ Date sélectionnée    │  │ Créneau sélectionné  │            │
│  │ Sélectionnez une date│  │ Sélectionnez un      │            │
│  │                      │  │ créneau              │            │
│  └──────────────────────┘  └──────────────────────┘            │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────────┐
│  │ Créneaux disponibles                                        │
│  │ ┌─────────────────────────────────────────────────────────┐ │
│  │ │ 09:00 - 10:00  [Sélectionner]                          │ │
│  │ │ 10:00 - 11:00  [Sélectionner]                          │ │
│  │ │ 14:00 - 15:00  [Sélectionner]                          │ │
│  │ └─────────────────────────────────────────────────────────┘ │
│  └─────────────────────────────────────────────────────────────┘
│                                                                 │
│  ✓ Session réservée!                                           │
│                                                    ✓ Réserver   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Session Counter States

### State 1: Initial (0/3) - GREEN
```
┌─────────────────────────────────────────────────────────────────┐
│ Sessions futures: 0/3                                           │
│ Vous pouvez réserver 3 session(s)                               │
└─────────────────────────────────────────────────────────────────┘
```
- Counter: **0/3** (Green #10b981)
- Message: "Vous pouvez réserver 3 session(s)"
- Button: **ENABLED** (Green)

---

### State 2: After 1st Reservation (1/3) - ORANGE
```
┌─────────────────────────────────────────────────────────────────┐
│ Sessions futures: 1/3                                           │
│ Vous pouvez réserver 2 session(s)                               │
└─────────────────────────────────────────────────────────────────┘
```
- Counter: **1/3** (Orange #f59e0b)
- Message: "Vous pouvez réserver 2 session(s)"
- Button: **ENABLED** (Green)

---

### State 3: After 2nd Reservation (2/3) - ORANGE
```
┌─────────────────────────────────────────────────────────────────┐
│ Sessions futures: 2/3                                           │
│ Vous pouvez réserver 1 session(s)                               │
└─────────────────────────────────────────────────────────────────┘
```
- Counter: **2/3** (Orange #f59e0b)
- Message: "Vous pouvez réserver 1 session(s)"
- Button: **ENABLED** (Green)

---

### State 4: After 3rd Reservation (3/3) - RED
```
┌─────────────────────────────────────────────────────────────────┐
│ Sessions futures: 3/3                                           │
│ Limite atteinte - Vous ne pouvez plus réserver                  │
└─────────────────────────────────────────────────────────────────┘
```
- Counter: **3/3** (Red #ef4444)
- Message: "Limite atteinte - Vous ne pouvez plus réserver"
- Button: **DISABLED** (Gray)

---

## Button States

### Button ENABLED (Green)
```
┌──────────────────────────────────────────────────────────────────┐
│                                                                  │
│                                          ✓ Réserver session      │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```
- Background: Green (#10b981)
- Text: White
- Clickable: YES
- Cursor: pointer

---

### Button DISABLED (Gray)
```
┌──────────────────────────────────────────────────────────────────┐
│                                                                  │
│                                          ✓ Réserver session      │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```
- Background: Gray (#d1d5db)
- Text: Dark Gray (#6b7280)
- Clickable: NO
- Cursor: not-allowed

---

## Color Reference

### Counter Colors
| Count | Color | Hex | RGB | Usage |
|-------|-------|-----|-----|-------|
| 0 | 🟢 Green | #10b981 | rgb(16, 185, 129) | Can book |
| 1-2 | 🟠 Orange | #f59e0b | rgb(245, 158, 11) | Can book |
| 3 | 🔴 Red | #ef4444 | rgb(239, 68, 68) | Cannot book |

### UI Colors
| Element | Color | Hex | Usage |
|---------|-------|-----|-------|
| Counter Box Background | Light Blue | #eff6ff | Container |
| Counter Box Border | Blue | #bfdbfe | Border |
| Counter Label | Dark Blue | #1e40af | Text |
| Button Enabled | Green | #10b981 | Active |
| Button Disabled | Gray | #d1d5db | Inactive |
| Button Text (Disabled) | Dark Gray | #6b7280 | Inactive text |

---

## Message Examples

### Success Messages
```
✓ Session réservée!
```
- Color: Green (#10b981)
- Font size: 12px
- Display time: 3-5 seconds

---

### Error Messages
```
❌ Sélectionnez une date et un créneau
```
- Color: Red (#ef4444)
- Font size: 12px
- Display time: Until dismissed

---

### Limit Reached Message
```
Limite atteinte - Vous ne pouvez plus réserver
```
- Color: Red (#dc2626)
- Font size: 11px
- Location: In counter box
- Persistent: Until a reservation is cancelled

---

## Responsive Design

### Desktop (1200px+)
```
┌─────────────────────────────────────────────────────────────────┐
│ Sessions futures: 0/3  Vous pouvez réserver 3 session(s)        │
└─────────────────────────────────────────────────────────────────┘
```
- Full width counter box
- All elements on one line
- Spacing: 15px between elements

---

### Tablet (768px - 1199px)
```
┌─────────────────────────────────────────────────────────────────┐
│ Sessions futures: 0/3                                           │
│ Vous pouvez réserver 3 session(s)                               │
└─────────────────────────────────────────────────────────────────┘
```
- Full width counter box
- Elements may wrap
- Spacing: 10px between elements

---

### Mobile (< 768px)
```
┌──────────────────────────────────────┐
│ Sessions futures: 0/3                │
│ Vous pouvez réserver 3 session(s)    │
└──────────────────────────────────────┘
```
- Full width counter box
- Stacked layout
- Spacing: 8px between elements
- Font size: 11px for labels

---

## Animation & Transitions

### Counter Update Animation
```
Before: 0/3 (Green)
  ↓ [Fade out]
  ↓ [Update value]
  ↓ [Change color]
  ↓ [Fade in]
After: 1/3 (Orange)
```
- Duration: 300ms
- Easing: ease-in-out

---

### Button State Change
```
Before: Enabled (Green)
  ↓ [Disable]
  ↓ [Change color to gray]
  ↓ [Disable click]
After: Disabled (Gray)
```
- Duration: 200ms
- Easing: ease-in

---

## Accessibility Features

### Color Contrast
- Counter text on background: WCAG AA compliant
- Button text on background: WCAG AA compliant
- Error messages: High contrast

### Font Sizes
- Counter label: 12px (readable)
- Counter value: 14px (prominent)
- Message: 11px (readable)
- Button text: 12px (readable)

### Focus States
- Button has visible focus ring
- Labels are properly associated
- Error messages are announced

---

## User Interaction Flow

### 1. Initial Load
```
User opens calendar
    ↓
Counter displays: 0/3 (Green)
Message: "Vous pouvez réserver 3 session(s)"
Button: Enabled
```

### 2. Select Slot
```
User selects date and time
    ↓
Button remains enabled
    ↓
User clicks "Réserver"
```

### 3. Confirm Reservation
```
Confirmation dialog appears
    ↓
User clicks OK
    ↓
Request created
    ↓
Counter updates: 1/3 (Orange)
Message: "Vous pouvez réserver 2 session(s)"
Button: Still enabled
```

### 4. Repeat Until Limit
```
After 3rd reservation:
    ↓
Counter updates: 3/3 (Red)
Message: "Limite atteinte - Vous ne pouvez plus réserver"
Button: Disabled (Gray)
    ↓
User cannot select new slots
```

---

## Visual Hierarchy

### Priority 1 (Most Important)
- Counter value (0/3, 1/3, 2/3, 3/3)
- Font size: 14px
- Font weight: Bold
- Color: Changes based on status

### Priority 2 (Important)
- Counter label ("Sessions futures:")
- Font size: 12px
- Font weight: Bold
- Color: Dark blue

### Priority 3 (Supporting)
- Remaining slots message
- Font size: 11px
- Font weight: Normal
- Color: Green or red

---

## Summary

The session counter provides clear visual feedback to users about:
1. **How many sessions they have booked** (X/3)
2. **How many more they can book** (message)
3. **Whether they can book more** (button state)

The color scheme and messaging make it immediately clear when the limit is reached, preventing user frustration and errors.
