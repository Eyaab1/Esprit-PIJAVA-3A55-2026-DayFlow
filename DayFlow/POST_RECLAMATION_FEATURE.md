# 📝 Post Reclamation Feature - Implementation Plan

## Overview

Users can report inappropriate posts, and admins can:
1. View post reclamations
2. See the reported post details
3. Ban the post author from posting
4. Send warning notifications to the user

---

## Database Changes Needed

### 1. Add `post_id` Column to Reclamation Table

```sql
-- Add post_id column to reclamation table
ALTER TABLE reclamation 
ADD COLUMN post_id INTEGER REFERENCES post(id) ON DELETE CASCADE;

-- Add index for performance
CREATE INDEX idx_reclamation_post_id ON reclamation(post_id);

-- Check existing reclamations
SELECT id, type, status, user_id, post_id 
FROM reclamation 
WHERE post_id IS NOT NULL;
```

### 2. Add `banned_from_posting` Column to User Table

```sql
-- Add banned_from_posting column to user table
ALTER TABLE "user" 
ADD COLUMN banned_from_posting BOOLEAN DEFAULT FALSE;

-- Add banned_at timestamp
ALTER TABLE "user"
ADD COLUMN banned_at TIMESTAMP;

-- Check banned users
SELECT id, first_name, last_name, email, banned_from_posting, banned_at
FROM "user"
WHERE banned_from_posting = TRUE;
```

---

## Implementation Steps

### Step 1: Update Reclamation Model

Add `postId` field to `Reclamation.java`:

```java
private Integer postId;

public Integer getPostId() {
    return postId;
}

public void setPostId(Integer postId) {
    this.postId = postId;
}
```

### Step 2: Update ReclamationService

Add methods to handle post reclamations:

```java
// Find reclamations for a specific post
public List<Reclamation> findByPostId(int postId) throws SQLException;

// Find all post reclamations for admin
public List<Reclamation> findPostReclamationsForAdmin(
    ReclamationStatus status, int limit, int offset) throws SQLException;

// Count post reclamations
public int countPostReclamations(ReclamationStatus status) throws SQLException;
```

### Step 3: Create NotificationService

```java
public class NotificationService {
    public void sendWarningNotification(int userId, String message);
    public void sendBanNotification(int userId);
}
```

### Step 4: Create UserBanService

```java
public class UserBanService {
    public void banUserFromPosting(int userId, String reason);
    public void unbanUser(int userId);
    public boolean isUserBanned(int userId);
}
```

### Step 5: Create Admin Post Reclamations Controller

```java
public class AdminPostReclamationsController {
    // View post reclamations
    // Show post details
    // Ban user action
    // Send warning action
}
```

### Step 6: Create FXML for Admin Post Reclamations

```xml
<!-- admin_post_reclamations.fxml -->
- List of post reclamations
- Filters (status, date)
- Actions: View Post, Ban User, Send Warning
```

---

## User Flow

### User Reports a Post:

```
1. User sees inappropriate post
2. User clicks "🚩 Signaler ce post"
3. User selects reason:
   - Contenu inapproprié
   - Spam
   - Harcèlement
   - Fausses informations
   - Autre
4. User adds description (optional)
5. Reclamation created with post_id
```

### Admin Handles Report:

```
1. Admin goes to "Réclamations de Posts"
2. Admin sees list of reported posts
3. Admin clicks "Voir le post" to see details
4. Admin reviews post content
5. Admin takes action:
   
   Option A: Ban User
   - Click "🚫 Bannir l'utilisateur"
   - User can no longer create posts
   - User receives ban notification
   
   Option B: Send Warning
   - Click "⚠️ Envoyer un avertissement"
   - User receives warning notification
   - Post remains visible
   
   Option C: Dismiss
   - Click "✓ Marquer comme résolu"
   - No action taken
   - Reclamation marked as resolved
```

---

## UI Mockups

### Admin Post Reclamations List:

```
┌─────────────────────────────────────────────────────────┐
│ Réclamations de Posts                                   │
├─────────────────────────────────────────────────────────┤
│ Filtres: [Tous les statuts▼] [🔍 Filtrer]              │
│ Total: 15 réclamations                                  │
├─────────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────────┐ │
│ │ 🚩 Post #123 - "Titre du post..."                   │ │
│ │ 👤 Signalé par: John Doe                            │ │
│ │ 📝 Auteur du post: Jane Smith                       │ │
│ │ 📅 28/04/2026 à 14:30                               │ │
│ │ Raison: Contenu inapproprié                         │ │
│ │                                                     │ │
│ │ [👁 Voir Post] [🚫 Bannir] [⚠️ Avertir] [✓ Résolu] │ │
│ └─────────────────────────────────────────────────────┘ │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ 🚩 Post #122 - "Autre post..."                      │ │
│ │ ...                                                 │ │
│ └─────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

### Post Detail Dialog:

```
┌─────────────────────────────────────────────────────────┐
│ Détail du post signalé                                  │
├─────────────────────────────────────────────────────────┤
│ ═══ POST ═══                                            │
│ ID: 123                                                 │
│ Titre: "Titre du post"                                  │
│ Auteur: Jane Smith (jane@example.com)                   │
│ Date: 28/04/2026 à 14:30                                │
│                                                         │
│ Contenu:                                                │
│ Lorem ipsum dolor sit amet...                           │
│                                                         │
│ ═══ RÉCLAMATION ═══                                     │
│ Signalé par: John Doe (john@example.com)                │
│ Raison: Contenu inapproprié                             │
│ Description: Ce post contient des propos offensants...  │
│                                                         │
│                                    [Fermer]             │
└─────────────────────────────────────────────────────────┘
```

### Ban User Dialog:

```
┌─────────────────────────────────────────────────────────┐
│ Bannir l'utilisateur                                    │
├─────────────────────────────────────────────────────────┤
│ Utilisateur: Jane Smith (jane@example.com)              │
│                                                         │
│ ⚠️ ATTENTION: Cette action empêchera l'utilisateur de   │
│ créer de nouveaux posts.                                │
│                                                         │
│ Raison du bannissement:                                 │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ Violation des règles de la communauté              │ │
│ └─────────────────────────────────────────────────────┘ │
│                                                         │
│ ☑ Envoyer une notification à l'utilisateur              │
│                                                         │
│                              [Annuler]  [Confirmer]     │
└─────────────────────────────────────────────────────────┘
```

### Warning Dialog:

```
┌─────────────────────────────────────────────────────────┐
│ Envoyer un avertissement                                │
├─────────────────────────────────────────────────────────┤
│ Utilisateur: Jane Smith (jane@example.com)              │
│                                                         │
│ Message d'avertissement:                                │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ Votre post a été signalé pour contenu inapproprié. │ │
│ │ Veuillez respecter les règles de la communauté.    │ │
│ │ Un prochain signalement pourrait entraîner un       │ │
│ │ bannissement.                                       │ │
│ └─────────────────────────────────────────────────────┘ │
│                                                         │
│                              [Annuler]  [Envoyer]       │
└─────────────────────────────────────────────────────────┘
```

---

## Notification Types

### Ban Notification:
```
Type: "BAN"
Message: "Votre compte a été restreint. Vous ne pouvez plus créer de posts 
suite à des violations répétées des règles de la communauté."
```

### Warning Notification:
```
Type: "WARNING"
Message: "Votre post a été signalé pour [raison]. Veuillez respecter les 
règles de la communauté. Un prochain signalement pourrait entraîner des 
restrictions sur votre compte."
```

### Reclamation Resolved Notification:
```
Type: "RECLAMATION_RESOLVED"
Message: "Votre signalement concernant le post #[id] a été traité. Merci 
pour votre contribution à maintenir une communauté saine."
```

---

## Security Considerations

### Prevent Abuse:
1. ✅ Limit reclamations per user per day (e.g., max 10)
2. ✅ Prevent duplicate reclamations (same user, same post)
3. ✅ Log all admin actions (ban, warning)
4. ✅ Require admin authentication for actions

### Privacy:
1. ✅ Don't show reporter identity to post author
2. ✅ Only admins can see reclamation details
3. ✅ Log access to sensitive data

---

## Testing Checklist

### User Side:
- [ ] User can report a post
- [ ] User can select report reason
- [ ] User can add description
- [ ] User receives confirmation
- [ ] User cannot report same post twice
- [ ] User receives notification when reclamation is resolved

### Admin Side:
- [ ] Admin can see all post reclamations
- [ ] Admin can filter by status
- [ ] Admin can view post details
- [ ] Admin can ban user from posting
- [ ] Admin can send warning notification
- [ ] Admin can mark as resolved
- [ ] Banned user cannot create posts
- [ ] Notifications are sent correctly

---

## Next Steps

1. **Run database migrations** (add columns)
2. **Update Reclamation model** (add postId field)
3. **Update ReclamationService** (add post reclamation methods)
4. **Create NotificationService** (send notifications)
5. **Create UserBanService** (ban/unban users)
6. **Create AdminPostReclamationsController** (admin UI)
7. **Create admin_post_reclamations.fxml** (UI layout)
8. **Add menu item** in admin shell
9. **Test all features**
10. **Document for team**

---

## Estimated Time

- Database migrations: 30 min
- Model updates: 1 hour
- Service layer: 2 hours
- Controller + UI: 3 hours
- Testing: 1 hour
- Documentation: 30 min

**Total: ~8 hours**

---

## Questions to Clarify

1. Should banned users be able to comment on posts?
2. Should there be a temporary ban option (e.g., 7 days)?
3. Should admins be able to delete the reported post?
4. Should there be an appeal process for banned users?
5. Should we track ban history?

---

Would you like me to proceed with implementing this feature?
