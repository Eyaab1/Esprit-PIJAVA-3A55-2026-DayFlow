# Ban User Feature - UI Guide

## Visual Walkthrough

### 1. Admin Reclamations List
```
┌─────────────────────────────────────────────────────────────┐
│  Admin - Gestion des Réclamations                           │
├─────────────────────────────────────────────────────────────┤
│  [Search: ________] [Status: ▼] [Type: ▼]                  │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ Réclamation #123                    [En attente]       │ │
│  │ 👤 John Doe (john@example.com)      [Bug]             │ │
│  │ This post contains inappropriate content...            │ │
│  │ 📅 28/04/2026 à 14:30                                  │ │
│  │                              [👁 Voir] [✉ Répondre]    │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

---

### 2. Reclamation Detail - Normal User (Not Banned)
```
┌─────────────────────────────────────────────────────────────┐
│  Détail de la réclamation                                    │
│  Réclamation #123                                            │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ 🛡️ Actions de modération :                             │ │
│  │                                                          │ │
│  │  [⏱️ Bannir temporairement] [🚫 Bannir définitivement] │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│  [🌐 Traduire le contenu en français]                       │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ ═══ INFORMATIONS ═══                                    │ │
│  │                                                          │ │
│  │ ID: 123                                                  │ │
│  │ Type: Bug                                                │ │
│  │ Statut: En attente                                       │ │
│  │ Utilisateur: John Doe (john@example.com)                │ │
│  │ Date: 28/04/2026 à 14:30                                │ │
│  │ Post signalé: #456                                       │ │
│  │                                                          │ │
│  │ ═══ CONTENU ═══                                         │ │
│  │                                                          │ │
│  │ This post contains inappropriate content that violates  │ │
│  │ our community guidelines...                             │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│                                              [OK]            │
└─────────────────────────────────────────────────────────────┘
```

---

### 3. Reclamation Detail - Temporarily Banned User
```
┌─────────────────────────────────────────────────────────────┐
│  Détail de la réclamation                                    │
│  Réclamation #123                                            │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ ⚠️ Utilisateur banni temporairement jusqu'au           │ │
│  │    05/05/2026 à 14:30                                   │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ 🛡️ Actions de modération :                             │ │
│  │                                                          │ │
│  │  [⏱️ Bannir temporairement] [🚫 Bannir définitivement] │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│  [Content details...]                                        │
└─────────────────────────────────────────────────────────────┘
```

---

### 4. Reclamation Detail - Permanently Banned User
```
┌─────────────────────────────────────────────────────────────┐
│  Détail de la réclamation                                    │
│  Réclamation #123                                            │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ ⛔ Utilisateur banni définitivement                     │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ 🛡️ Actions de modération :                             │ │
│  │                                                          │ │
│  │  [⏱️ Bannir temporairement] [🚫 Bannir définitivement] │ │
│  │  (buttons disabled - grayed out)                        │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│  [Content details...]                                        │
└─────────────────────────────────────────────────────────────┘
```

---

### 5. Temporary Ban Dialog
```
┌─────────────────────────────────────────────────────────────┐
│  Bannir temporairement                                       │
│  Bannir l'utilisateur pour cette réclamation                │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ⏱️ L'utilisateur sera banni temporairement et ne pourra   │
│     plus publier de posts pendant la durée spécifiée.       │
│                                                              │
│  Durée (jours) : [  7  ▲▼]                                  │
│                                                              │
│  Raison du bannissement :                                    │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ Expliquez pourquoi cet utilisateur est banni...        │ │
│  │                                                          │ │
│  │                                                          │ │
│  │                                                          │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│                                          [OK]  [Cancel]      │
└─────────────────────────────────────────────────────────────┘
```

---

### 6. Permanent Ban Dialog
```
┌─────────────────────────────────────────────────────────────┐
│  Bannir définitivement                                       │
│  Bannir l'utilisateur pour cette réclamation                │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ⚠️ L'utilisateur sera banni définitivement et ne pourra   │
│     plus publier de posts.                                  │
│                                                              │
│  Raison du bannissement :                                    │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ Expliquez pourquoi cet utilisateur est banni...        │ │
│  │                                                          │ │
│  │                                                          │ │
│  │                                                          │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│                                          [OK]  [Cancel]      │
└─────────────────────────────────────────────────────────────┘
```

---

### 7. Success Confirmation
```
┌─────────────────────────────────────────────────────────────┐
│  Bannissement appliqué                                       │
│  ✅ Utilisateur banni avec succès                           │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  L'utilisateur a été banni pour 7 jour(s) et a reçu une    │
│  notification.                                               │
│                                                              │
│                                                      [OK]    │
└─────────────────────────────────────────────────────────────┘
```

---

### 8. User Notification (In-App)
```
┌─────────────────────────────────────────────────────────────┐
│  Notifications                                        [🔔 1] │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ ⚠️ BAN                                   28/04/2026     │ │
│  │                                                          │ │
│  │ Votre compte a été banni pour 7 jour(s).               │ │
│  │ Raison : Posting inappropriate content that violates   │ │
│  │ community guidelines.                                   │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## Color Scheme

### Ban Status Indicators
- **Temporary Ban**: 🟡 Yellow background (#fef3c7), brown text (#92400e)
- **Permanent Ban**: 🔴 Red background (#fee2e2), dark red text (#991b1b)

### Action Buttons
- **Temporary Ban Button**: 🟠 Orange (#f59e0b)
- **Permanent Ban Button**: 🔴 Red (#dc2626)
- **Disabled Buttons**: ⚪ Gray (when user already banned)

### Moderation Box
- **Background**: 🟡 Light yellow (#fef3c7)
- **Border**: Rounded corners (6px)
- **Padding**: 10px

---

## Button States

### Enabled (Normal)
```
[⏱️ Bannir temporairement]  ← Orange, clickable
[🚫 Bannir définitivement]  ← Red, clickable
```

### Disabled (User Already Banned)
```
[⏱️ Bannir temporairement]  ← Gray, not clickable
[🚫 Bannir définitivement]  ← Gray, not clickable
```

---

## Validation Messages

### Error (Red)
```
❌ La raison doit contenir au moins 10 caractères.
```

### Success (Green)
```
✅ Utilisateur banni avec succès
```

### Info (Blue)
```
ℹ️ L'utilisateur sera banni temporairement...
```

---

## Responsive Behavior

### Dialog Sizes
- **Detail Dialog**: 650px width
- **Ban Dialog**: 500px width
- **Text Areas**: 4-8 rows, wrap text enabled

### Spinner (Duration)
- **Min**: 1 day
- **Max**: 365 days
- **Default**: 7 days
- **Width**: 100px

---

## Accessibility

### Icons Used
- ⏱️ Temporary ban
- 🚫 Permanent ban
- ⛔ Already banned (permanent)
- ⚠️ Already banned (temporary)
- 🛡️ Moderation actions
- ✅ Success
- ❌ Error

### Tooltips
- Disabled buttons show reason for being disabled
- All icons have text labels for clarity

---

## User Flow Diagram

```
Admin Views Reclamation
         │
         ├─→ No post_id? → No ban buttons shown
         │
         └─→ Has post_id? → Show moderation actions
                   │
                   ├─→ User not banned? → Buttons enabled
                   │         │
                   │         ├─→ Click Temp Ban → Show duration dialog
                   │         │         │
                   │         │         └─→ Fill reason → Apply ban → Notify user
                   │         │
                   │         └─→ Click Perm Ban → Show reason dialog
                   │                   │
                   │                   └─→ Fill reason → Apply ban → Notify user
                   │
                   └─→ User already banned? → Buttons disabled
                             │
                             └─→ Show ban status indicator
```

---

## Notes for Developers

### CSS Classes Used
- `.admin-badge` - Status badges
- `.admin-list-row` - Reclamation list rows

### Inline Styles
All styling is done inline for simplicity and consistency with existing code.

### Font Awesome Icons
Using emoji icons (⏱️, 🚫, ⛔, ⚠️, 🛡️) for cross-platform compatibility.
