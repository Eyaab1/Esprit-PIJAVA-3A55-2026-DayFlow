# Ban User Feature - Implementation Checklist ✅

## Pre-Implementation ✅ (DONE)

- [x] Database migration script created
- [x] Reclamation model updated with `postId` field
- [x] ReclamationService updated for all SQL queries
- [x] NotificationService updated with `createNotification()` method
- [x] AdminReclamationsController updated with ban functionality
- [x] Documentation created

---

## Your Tasks 📋

### 1. Database Setup
- [ ] **Run the migration script**
  ```bash
  psql -U your_user -d your_database -f DayFlow/APPLY_BAN_FEATURE.sql
  ```
  Or use your database client (pgAdmin, DBeaver, etc.)

- [ ] **Verify migration succeeded**
  ```sql
  -- Check if post_id column exists
  SELECT column_name, data_type 
  FROM information_schema.columns 
  WHERE table_name = 'reclamation' AND column_name = 'post_id';
  
  -- Should return: post_id | integer
  ```

### 2. Code Compilation
- [ ] **Clean and compile**
  ```bash
  mvn clean compile
  ```

- [ ] **Check for compilation errors**
  - If errors occur, check console output
  - Most likely cause: missing imports (already added)

### 3. Testing

#### Basic Functionality
- [ ] **Test 1: View reclamation without post_id**
  - Create/view a reclamation with `post_id = NULL`
  - Verify: No ban buttons appear
  - Expected: Only translation and reply buttons visible

- [ ] **Test 2: View reclamation with post_id**
  - Create/view a reclamation with a valid `post_id`
  - Verify: Ban buttons appear in yellow box
  - Expected: "🛡️ Actions de modération" section visible

- [ ] **Test 3: Apply temporary ban**
  - Click "⏱️ Bannir temporairement"
  - Enter reason (min 10 chars)
  - Select duration (e.g., 7 days)
  - Click OK
  - Verify: Success message appears
  - Check database: `user.status = 'temp_banned'`
  - Check database: `user.banned_until` is set
  - Check notifications table: New notification created

- [ ] **Test 4: Apply permanent ban**
  - Click "🚫 Bannir définitivement"
  - Enter reason (min 10 chars)
  - Click OK
  - Verify: Success message appears
  - Check database: `user.status = 'banned'`
  - Check notifications table: New notification created

- [ ] **Test 5: View banned user reclamation**
  - View reclamation from banned user
  - Verify: Ban status indicator appears (red or yellow box)
  - Verify: Ban buttons are disabled (grayed out)

#### Validation Testing
- [ ] **Test 6: Empty reason**
  - Try to ban without entering reason
  - Expected: Error message "La raison doit contenir au moins 10 caractères"
  - Dialog should reopen

- [ ] **Test 7: Short reason**
  - Enter reason with less than 10 characters
  - Expected: Same error as Test 6

- [ ] **Test 8: Valid reason**
  - Enter reason with 10+ characters
  - Expected: Ban applied successfully

#### User Notification Testing
- [ ] **Test 9: User receives notification**
  - Ban a user
  - Login as that user
  - Check notifications
  - Verify: Notification appears with ban details

- [ ] **Test 10: Notification content**
  - Temporary ban: Should show duration and reason
  - Permanent ban: Should show "définitivement" and reason

#### Edge Cases
- [ ] **Test 11: Ban already banned user**
  - View reclamation from permanently banned user
  - Verify: Buttons are disabled
  - Try clicking: Should not open dialog

- [ ] **Test 12: Database error handling**
  - Disconnect database (or use invalid user ID)
  - Try to ban user
  - Expected: Error alert with message

- [ ] **Test 13: Cancel ban dialog**
  - Click ban button
  - Click Cancel
  - Verify: No ban applied, dialog closes

### 4. Integration Testing
- [ ] **Test 14: Post creation blocked**
  - Ban a user
  - Login as that user
  - Try to create a post
  - Expected: Should be blocked (if post creation checks user status)

- [ ] **Test 15: Multiple reclamations**
  - Create multiple reclamations for same user
  - Ban user from one reclamation
  - View other reclamations
  - Verify: All show ban status

- [ ] **Test 16: Admin audit trail**
  - Check if admin ID is recorded
  - Query: `SELECT * FROM reclamation WHERE id = [reclamation_id]`
  - Verify: Admin actions are logged

### 5. UI/UX Testing
- [ ] **Test 17: Button styling**
  - Temporary ban button: Orange (#f59e0b)
  - Permanent ban button: Red (#dc2626)
  - Disabled buttons: Gray

- [ ] **Test 18: Status indicators**
  - Temporary ban: Yellow box with ⚠️
  - Permanent ban: Red box with ⛔

- [ ] **Test 19: Dialog responsiveness**
  - Open ban dialog
  - Resize window
  - Verify: Dialog remains centered and readable

- [ ] **Test 20: Spinner functionality**
  - Test duration spinner (1-365 days)
  - Try typing values
  - Try using up/down arrows

---

## Verification Queries 🔍

### Check Migration
```sql
-- Verify post_id column exists
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'reclamation' AND column_name = 'post_id';

-- Verify foreign key constraint
SELECT constraint_name, table_name, constraint_type
FROM information_schema.table_constraints
WHERE table_name = 'reclamation' AND constraint_name = 'fk_reclamation_post';

-- Verify index
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'reclamation' AND indexname = 'idx_reclamation_post_id';
```

### Check Ban Applied
```sql
-- Check user status after ban
SELECT id, email, status, banned_until, ban_reason
FROM "user"
WHERE id = [user_id];

-- Check notification created
SELECT id, user_id, type, message, created_at
FROM notifications
WHERE user_id = [user_id] AND type = 'BAN'
ORDER BY created_at DESC
LIMIT 1;
```

### Check Reclamation Data
```sql
-- View reclamations with post references
SELECT r.id, r.content, r.post_id, r.user_id, u.status as user_status
FROM reclamation r
LEFT JOIN "user" u ON u.id = r.user_id
WHERE r.post_id IS NOT NULL
ORDER BY r.created_at DESC;
```

---

## Troubleshooting 🔧

### Issue: Migration fails
**Solution**: 
- Check if `post` table exists
- Check if you have ALTER TABLE permissions
- Try running commands one by one

### Issue: Compilation errors
**Solution**:
- Check all imports are present
- Verify AdminModerationService exists
- Verify NotificationService exists
- Run `mvn clean` first

### Issue: Ban buttons not showing
**Solution**:
- Check if `post_id` is set in reclamation
- Check console for SQL errors
- Verify ReclamationService is loading `post_id`

### Issue: Notification not created
**Solution**:
- Check `notifications` table exists
- Check NotificationService SQL syntax
- Check user_id is valid
- Check console for SQLException

### Issue: User can still post after ban
**Solution**:
- Check user status in database
- Verify post creation logic checks `user.status`
- Add status check if missing:
  ```java
  if (user.getStatus().equals("banned") || user.getStatus().equals("temp_banned")) {
      // Block post creation
  }
  ```

---

## Success Criteria ✅

Your implementation is successful when:

- [x] Database migration runs without errors
- [x] Code compiles without errors
- [x] Ban buttons appear for post-related reclamations
- [x] Ban buttons don't appear for non-post reclamations
- [x] Temporary ban works (user status updated, notification sent)
- [x] Permanent ban works (user status updated, notification sent)
- [x] Ban status indicators show correctly
- [x] Validation prevents empty/short reasons
- [x] Disabled buttons for already-banned users
- [x] User receives in-app notification
- [x] No console errors during normal operation

---

## Documentation Reference 📚

- **Quick Start**: `BAN_FEATURE_SUMMARY.md`
- **Detailed Guide**: `BAN_USER_FROM_RECLAMATION_GUIDE.md`
- **UI Walkthrough**: `BAN_FEATURE_UI_GUIDE.md`
- **This Checklist**: `BAN_FEATURE_CHECKLIST.md`
- **Migration Script**: `APPLY_BAN_FEATURE.sql`

---

## Next Steps After Testing 🚀

Once all tests pass:

1. **Commit your changes**
   ```bash
   git add .
   git commit -m "feat: Add ban user functionality for post reclamations with in-app notifications"
   ```

2. **Push to your branch**
   ```bash
   git push origin ReclamationV2-admin-ai
   ```

3. **Optional enhancements** (future work):
   - Add unban feature
   - Add ban history view
   - Add ban appeal system
   - Add email notifications (in addition to in-app)
   - Add bulk ban actions

---

## Support 💬

If you encounter issues:
1. Check console logs for errors
2. Verify database migration succeeded
3. Check all imports are present
4. Review the troubleshooting section above
5. Check the detailed guide for specific scenarios

---

**Good luck with testing! 🎉**
