# Ban User from Posting - Reclamation Feature Guide

## Overview
This feature allows administrators to ban users from posting when they receive reclamations about problematic posts. Users receive in-app notifications instead of emails.

---

## Database Changes

### Migration File
**Location**: `DayFlow/database/migrations/add_post_id_to_reclamation.sql`

**Changes**:
- Added `post_id` column to `reclamation` table (nullable)
- Added foreign key constraint to `post` table
- Added index for performance

**To Apply**:
```sql
-- Run this SQL script on your database
psql -U your_user -d your_database -f DayFlow/database/migrations/add_post_id_to_reclamation.sql
```

---

## Model Updates

### Reclamation Model
**File**: `DayFlow/src/main/java/model/reclamation/Reclamation.java`

**Added**:
- `private Integer postId;` field
- `getPostId()` and `setPostId()` methods

### Notification Service
**File**: `DayFlow/src/main/java/services/notification/NotificationService.java`

**Added**:
- `createNotification(int userId, String type, String message)` method
- Sends in-app notifications to users

---

## Features

### 1. Admin Reclamation Detail View

When viewing a reclamation about a post, admins see:

#### User Ban Status Display
- **Permanently Banned**: Red box showing "⛔ Utilisateur banni définitivement"
- **Temporarily Banned**: Yellow box showing "⚠️ Utilisateur banni temporairement jusqu'au [date]"

#### Moderation Actions (Only for Post-Related Reclamations)
Two ban buttons appear:
1. **⏱️ Bannir temporairement** (Temporary Ban) - Orange button
2. **🚫 Bannir définitivement** (Permanent Ban) - Red button

**Note**: Buttons are disabled if user is already permanently banned.

---

### 2. Ban Dialog

#### Temporary Ban Dialog
- **Duration Selector**: Spinner to choose 1-365 days (default: 7 days)
- **Reason Field**: Text area for ban reason (minimum 10 characters)
- **Validation**: Ensures reason is provided before applying ban

#### Permanent Ban Dialog
- **Reason Field**: Text area for ban reason (minimum 10 characters)
- **Warning**: Shows clear warning about permanent nature of ban

---

### 3. Ban Application Process

When admin applies a ban:

1. **Database Update**:
   - User status changed to `banned` or `temp_banned`
   - `banned_until` timestamp set (for temporary bans)
   - `ban_reason` stored

2. **In-App Notification Sent**:
   - **Permanent Ban**: "🚫 Votre compte a été banni définitivement. Raison : [reason]"
   - **Temporary Ban**: "⚠️ Votre compte a été banni pour [X] jour(s). Raison : [reason]"
   - Notification type: `BAN`
   - User receives notification in their notification center

3. **Success Confirmation**:
   - Alert dialog confirms ban was applied
   - Reclamation list refreshes automatically

---

## User Experience

### For Admins
1. Open reclamation detail view
2. See user's current ban status (if any)
3. Click ban button (temporary or permanent)
4. Fill in reason and duration (if temporary)
5. Confirm ban
6. User is banned and notified

### For Users
1. Receive in-app notification about ban
2. Notification appears in notification center
3. Cannot create new posts while banned
4. Can see ban reason in notification

---

## Integration Points

### Services Used
- **AdminModerationService**: Handles ban logic and user status updates
- **NotificationService**: Sends in-app notifications
- **ReclamationService**: Manages reclamation data with post references

### Session Management
- Uses `AppSession.getCurrentUser()` to get admin ID for audit trail

---

## Code Structure

### AdminReclamationsController Methods

#### New Methods
```java
// Get user's current ban status
private String getUserStatus(Integer userId)

// Get user's ban expiration date
private java.time.LocalDateTime getUserBannedUntil(Integer userId)

// Show ban dialog (temporary or permanent)
private void showBanDialog(Reclamation reclamation, boolean permanent)
```

#### Updated Methods
```java
// Now shows post ID, ban status, and moderation actions
private void showReclamationDetail(int reclamationId)
```

---

## Notification Types

### BAN Notification
- **Type**: `"BAN"`
- **Purpose**: Inform user they've been banned
- **Content**: Includes ban duration (if temporary) and reason
- **Visibility**: Appears in user's notification center

---

## Security & Validation

### Input Validation
- ✅ Ban reason must be at least 10 characters
- ✅ Temporary ban duration: 1-365 days
- ✅ User ID validation before applying ban

### Database Constraints
- ✅ Foreign key constraint on `post_id`
- ✅ Nullable `post_id` (not all reclamations are about posts)
- ✅ Index on `post_id` for performance

### UI Safety
- ✅ Ban buttons disabled if user already permanently banned
- ✅ Confirmation required before applying ban
- ✅ Clear warnings about permanent bans
- ✅ Error handling with user-friendly messages

---

## Testing Checklist

### Database
- [ ] Run migration script successfully
- [ ] Verify `post_id` column exists in `reclamation` table
- [ ] Verify foreign key constraint works
- [ ] Test with NULL `post_id` values

### Functionality
- [ ] Create reclamation with `post_id`
- [ ] View reclamation detail as admin
- [ ] See moderation actions for post-related reclamations
- [ ] Apply temporary ban (7 days)
- [ ] Apply permanent ban
- [ ] Verify user receives notification
- [ ] Verify user status updated in database
- [ ] Verify ban buttons disabled after permanent ban

### Edge Cases
- [ ] Reclamation without `post_id` (no moderation actions shown)
- [ ] User already banned (buttons disabled)
- [ ] Invalid ban reason (validation error)
- [ ] Database connection error (error handling)

---

## Future Enhancements

### Potential Improvements
1. **Ban History**: Show list of all bans for a user
2. **Unban Feature**: Allow admins to lift bans early
3. **Ban Appeal**: Let users appeal their bans
4. **Bulk Actions**: Ban multiple users at once
5. **Ban Templates**: Pre-defined ban reasons
6. **Email + Notification**: Send both email and in-app notification

---

## Troubleshooting

### Common Issues

#### Ban buttons not showing
- **Cause**: Reclamation has no `post_id`
- **Solution**: Only post-related reclamations show ban buttons

#### Notification not received
- **Cause**: Notification service error or user ID invalid
- **Solution**: Check console logs for SQL errors

#### Ban not applied
- **Cause**: Database connection error or validation failure
- **Solution**: Check error alert message and console logs

#### User still can post after ban
- **Cause**: Post creation logic doesn't check user status
- **Solution**: Ensure post creation checks `user.status` field

---

## Related Files

### Modified Files
- `DayFlow/src/main/java/model/reclamation/Reclamation.java`
- `DayFlow/src/main/java/services/reclamation/ReclamationService.java`
- `DayFlow/src/main/java/services/notification/NotificationService.java`
- `DayFlow/src/main/java/controllers/admin/AdminReclamationsController.java`

### New Files
- `DayFlow/database/migrations/add_post_id_to_reclamation.sql`
- `DayFlow/BAN_USER_FROM_RECLAMATION_GUIDE.md` (this file)

### Dependencies
- `services.admin.AdminModerationService` (existing)
- `services.notification.NotificationService` (updated)
- `session.AppSession` (existing)

---

## Summary

This feature provides a complete moderation workflow:
1. ✅ User creates reclamation about a post
2. ✅ Admin views reclamation with post context
3. ✅ Admin sees user's current ban status
4. ✅ Admin can apply temporary or permanent ban
5. ✅ User receives in-app notification
6. ✅ User cannot post while banned

**Key Benefit**: Streamlined moderation with in-app notifications instead of emails, keeping users informed within the application.
