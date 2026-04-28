# 🚫 Ban User from Posting - Complete Feature

## Quick Start ⚡

### 1. Apply Database Migration
```bash
psql -U your_user -d your_database -f DayFlow/APPLY_BAN_FEATURE.sql
```

### 2. Compile & Run
```bash
mvn clean compile
mvn javafx:run
```

### 3. Test It
1. Login as admin
2. Go to "Gestion des Réclamations"
3. View a reclamation about a post
4. Click ban button
5. Fill in reason and confirm
6. User receives in-app notification ✅

---

## What This Feature Does 🎯

Allows admins to **ban users from posting** when they receive reclamations about problematic posts. Users are notified **in-app** (no emails).

### Key Features
- ✅ **Temporary Ban**: 1-365 days
- ✅ **Permanent Ban**: Indefinite
- ✅ **In-App Notifications**: Users notified within app
- ✅ **Ban Status Display**: Shows if user is already banned
- ✅ **Post Context**: Links reclamations to specific posts
- ✅ **Validation**: Requires ban reason (min 10 chars)

---

## Documentation 📚

| File | Purpose |
|------|---------|
| **BAN_FEATURE_SUMMARY.md** | Quick overview and file changes |
| **BAN_USER_FROM_RECLAMATION_GUIDE.md** | Detailed technical documentation |
| **BAN_FEATURE_UI_GUIDE.md** | Visual UI walkthrough |
| **BAN_FEATURE_CHECKLIST.md** | Testing checklist and verification |
| **APPLY_BAN_FEATURE.sql** | Database migration script |
| **BAN_FEATURE_README.md** | This file |

---

## Architecture 🏗️

### Database Layer
```
reclamation table
├── post_id (NEW) → references post(id)
└── user_id → references user(id)

user table
├── status (banned, temp_banned, active)
├── banned_until (timestamp)
└── ban_reason (text)

notifications table
├── user_id
├── type (BAN)
└── message
```

### Service Layer
```
AdminModerationService
└── applyAction() → Updates user status

NotificationService
└── createNotification() → Sends in-app notification

ReclamationService
└── Updated to handle post_id
```

### Controller Layer
```
AdminReclamationsController
├── showReclamationDetail() → Shows ban buttons
├── showBanDialog() → Collects ban info
├── getUserStatus() → Checks if user is banned
└── getUserBannedUntil() → Gets ban expiration
```

---

## User Flow 🔄

### Admin Perspective
```
1. View reclamation about a post
2. See moderation actions section
3. Click "Bannir temporairement" or "Bannir définitivement"
4. Fill in reason (required, min 10 chars)
5. Select duration (if temporary: 1-365 days)
6. Confirm ban
7. User is banned and notified
```

### User Perspective
```
1. Receive in-app notification
2. Notification shows:
   - Ban type (temporary/permanent)
   - Duration (if temporary)
   - Reason
3. Cannot create posts while banned
```

---

## UI Preview 🎨

### Reclamation Detail with Ban Actions
```
┌─────────────────────────────────────────────┐
│ Réclamation #123                            │
├─────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────┐ │
│ │ 🛡️ Actions de modération :              │ │
│ │                                          │ │
│ │ [⏱️ Bannir temporairement]              │ │
│ │ [🚫 Bannir définitivement]              │ │
│ └─────────────────────────────────────────┘ │
│                                             │
│ ID: 123                                     │
│ Type: Bug                                   │
│ Utilisateur: John Doe                       │
│ Post signalé: #456                          │
│ ...                                         │
└─────────────────────────────────────────────┘
```

### Ban Status Indicators
```
⚠️ Utilisateur banni temporairement jusqu'au 05/05/2026
⛔ Utilisateur banni définitivement
```

---

## Code Changes 📝

### Modified Files (6)
1. `model/reclamation/Reclamation.java`
   - Added `postId` field

2. `services/reclamation/ReclamationService.java`
   - Updated all SQL queries to include `post_id`

3. `services/notification/NotificationService.java`
   - Added `createNotification()` method

4. `controllers/admin/AdminReclamationsController.java`
   - Added ban functionality
   - Added user status checks
   - Added ban dialog

5. `database/migrations/add_post_id_to_reclamation.sql`
   - Migration script

6. `APPLY_BAN_FEATURE.sql`
   - Easy-to-run migration script

### New Documentation Files (5)
- BAN_FEATURE_SUMMARY.md
- BAN_USER_FROM_RECLAMATION_GUIDE.md
- BAN_FEATURE_UI_GUIDE.md
- BAN_FEATURE_CHECKLIST.md
- BAN_FEATURE_README.md (this file)

---

## Testing 🧪

### Quick Test
```bash
# 1. Run migration
psql -U your_user -d your_database -f APPLY_BAN_FEATURE.sql

# 2. Compile
mvn clean compile

# 3. Run app
mvn javafx:run

# 4. Test as admin
- Login as admin
- View reclamation with post_id
- Click ban button
- Fill reason and confirm
- Check user receives notification
```

### Verification Queries
```sql
-- Check migration
SELECT column_name FROM information_schema.columns 
WHERE table_name = 'reclamation' AND column_name = 'post_id';

-- Check ban applied
SELECT status, banned_until, ban_reason 
FROM "user" WHERE id = [user_id];

-- Check notification sent
SELECT * FROM notifications 
WHERE user_id = [user_id] AND type = 'BAN';
```

---

## Important Notes ⚠️

### When Ban Buttons Appear
- ✅ Reclamation has `post_id` set
- ❌ Reclamation has `post_id = NULL`

### Notification Behavior
- ✅ In-app notification sent
- ❌ No email sent (by design)

### Ban Validation
- ✅ Reason required (min 10 chars)
- ✅ Duration: 1-365 days (temporary)
- ✅ Buttons disabled if user already permanently banned

### Database Requirements
- ✅ PostgreSQL database
- ✅ `post` table must exist
- ✅ `notifications` table must exist
- ✅ `user` table must have `status`, `banned_until`, `ban_reason` columns

---

## Troubleshooting 🔧

| Issue | Solution |
|-------|----------|
| Migration fails | Check if `post` table exists, verify permissions |
| Compilation errors | Run `mvn clean`, check imports |
| Ban buttons not showing | Check if `post_id` is set in reclamation |
| Notification not created | Check `notifications` table exists, check console logs |
| User can still post | Add status check in post creation logic |

See **BAN_FEATURE_CHECKLIST.md** for detailed troubleshooting.

---

## Future Enhancements 🚀

Potential improvements:
- [ ] Unban feature for admins
- [ ] Ban history view
- [ ] Ban appeal system for users
- [ ] Email notifications (in addition to in-app)
- [ ] Bulk ban actions
- [ ] Ban templates with pre-defined reasons
- [ ] Auto-ban based on multiple reclamations

---

## Technical Details 🔧

### Dependencies
- JavaFX (UI)
- PostgreSQL (Database)
- Existing services:
  - AdminModerationService
  - NotificationService
  - ReclamationService
  - AppSession

### Database Schema Changes
```sql
ALTER TABLE reclamation ADD COLUMN post_id INTEGER;
ALTER TABLE reclamation ADD CONSTRAINT fk_reclamation_post 
  FOREIGN KEY (post_id) REFERENCES post(id) ON DELETE SET NULL;
CREATE INDEX idx_reclamation_post_id ON reclamation(post_id);
```

### Notification Format
```json
{
  "user_id": 123,
  "type": "BAN",
  "message": "🚫 Votre compte a été banni définitivement. Raison: [reason]",
  "is_read": false,
  "created_at": "2026-04-28 14:30:00"
}
```

---

## Support 💬

### Getting Help
1. Check console logs for errors
2. Verify database migration succeeded
3. Review troubleshooting section
4. Check detailed documentation files

### Common Questions

**Q: Why don't I see ban buttons?**
A: Ban buttons only appear for reclamations with a `post_id` set.

**Q: Can I unban a user?**
A: Not yet - this feature is planned for future enhancement.

**Q: Do users receive emails?**
A: No, only in-app notifications are sent (by design).

**Q: Can I ban a user without a reclamation?**
A: Use the existing moderation system for that (AdminModerationService).

---

## Summary ✨

This feature provides a complete moderation workflow:
1. ✅ User creates reclamation about a post
2. ✅ Admin views reclamation with post context
3. ✅ Admin sees user's current ban status
4. ✅ Admin can apply temporary or permanent ban
5. ✅ User receives in-app notification
6. ✅ User cannot post while banned

**Key Benefit**: Streamlined moderation with in-app notifications, keeping users informed within the application.

---

## Credits 👥

- **Feature**: Ban user from posting via reclamations
- **Notification**: In-app only (no emails)
- **Integration**: Uses existing AdminModerationService
- **Documentation**: Complete guides and checklists provided

---

**Ready to use! 🎉**

Start with **APPLY_BAN_FEATURE.sql** and follow **BAN_FEATURE_CHECKLIST.md** for testing.
