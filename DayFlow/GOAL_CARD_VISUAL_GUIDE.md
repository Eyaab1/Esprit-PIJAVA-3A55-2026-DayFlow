# Goal Card Visual Guide

## Goal Card Layout

```
┌────────────────────────────────────────────────────────────────┐
│                                                                │
│  🎯  Goal Title                                    ✏️  🗑️     │
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

## Icon Buttons

### Edit Button (✏️)
- **Color**: Blue (#3b82f6)
- **Hover**: Darker blue (#2563eb)
- **Action**: Opens edit dialog
- **Position**: Top-right corner

### Delete Button (🗑️)
- **Color**: Red (#ef4444)
- **Hover**: Darker red (#dc2626)
- **Action**: Shows confirmation dialog
- **Position**: Top-right corner (next to edit)

## Edit Dialog

When you click the ✏️ icon:

```
┌─────────────────────────────────────────┐
│  Modifier l'objectif                    │
├─────────────────────────────────────────┤
│                                         │
│  Titre *                                │
│  [Goal Title Input Field]               │
│                                         │
│  Description                            │
│  [Multi-line Description Field]         │
│                                         │
│  Début *                                │
│  [Date Picker]                          │
│                                         │
│  Fin *                                  │
│  [Date Picker]                          │
│                                         │
│  Statut                                 │
│  [Dropdown: active, draft, paused...]   │
│                                         │
│  [OK]  [CANCEL]                         │
│                                         │
└─────────────────────────────────────────┘
```

## Delete Confirmation Dialog

When you click the 🗑️ icon:

```
┌─────────────────────────────────────────┐
│  Supprimer l'objectif                   │
├─────────────────────────────────────────┤
│                                         │
│  Êtes-vous sûr?                         │
│                                         │
│  Cette action est irréversible.         │
│  Tous les données associées seront      │
│  supprimées.                            │
│                                         │
│  [OK]  [CANCEL]                         │
│                                         │
└─────────────────────────────────────────┘
```

## Color Scheme

### Edit Button
- **Default**: #3b82f6 (Blue)
- **Hover**: #2563eb (Darker Blue)
- **Text**: White

### Delete Button
- **Default**: #ef4444 (Red)
- **Hover**: #dc2626 (Darker Red)
- **Text**: White

## Interaction Flow

### Edit Goal
```
User clicks ✏️
    ↓
Edit dialog opens with current data
    ↓
User modifies fields
    ↓
User clicks OK
    ↓
Goal is updated in database
    ↓
Dashboard refreshes
    ↓
Success message shown
```

### Delete Goal
```
User clicks 🗑️
    ↓
Confirmation dialog appears
    ↓
User clicks OK
    ↓
Goal is deleted from database
    ↓
Dashboard refreshes
    ↓
Success message shown
```

## Features

✅ Edit any goal field
✅ Delete goals with confirmation
✅ Real-time dashboard refresh
✅ Success/error messages
✅ Responsive design
✅ Hover effects on buttons
✅ Keyboard accessible

## Status Options in Edit Dialog

- active
- draft
- paused
- completed
- failed
- archived

## Validation

- Title is required
- Start date is required
- End date is required
- End date must be after start date
- Description is optional
- Status defaults to "active"
