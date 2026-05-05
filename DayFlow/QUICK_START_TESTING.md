# 🚀 QUICK START - Testing the 3-Session Limit Feature

## ⚡ 5-Minute Quick Test

### Step 1: Restart the Application
1. Close the DayFlow application completely
2. Reopen it (the new code is compiled and ready)

### Step 2: Login and Navigate to Calendar
1. Login as a user
2. Navigate to "Calendrier Coach" or click on a coach's profile
3. You should see the calendar with the new **session counter** at the top

### Step 3: Verify Initial State
Look for this in the header:
```
Sessions futures: 0/3
Vous pouvez réserver 3 session(s)
```
- Counter should be **GREEN** (0/3)
- "Réserver" button should be **ENABLED** (green)

### Step 4: Make First Reservation
1. Click on a date in the calendar
2. Select a time slot
3. Click "✓ Réserver session"
4. Confirm in the dialog

**Expected result:**
- Counter changes to **1/3** (orange)
- Message: "Vous pouvez réserver 2 session(s)"
- Button still enabled

### Step 5: Make Second Reservation
1. Repeat Step 4 with a different date/slot
2. Counter should show **2/3** (orange)
3. Message: "Vous pouvez réserver 1 session(s)"

### Step 6: Make Third Reservation
1. Repeat Step 4 with another date/slot
2. Counter should show **3/3** (RED)
3. Message: "Limite atteinte - Vous ne pouvez plus réserver"
4. Button should be **DISABLED** (gray)

### Step 7: Try to Book When Limit Reached
1. Try to select another slot
2. Button should be disabled (can't click)
3. If you try to force it, error message appears

---

## 🔍 Verification in Database

### Check Created Requests
Open your database tool and run:

```sql
SELECT id, user_id, coach_id, status, created_at 
FROM coaching_request 
WHERE user_id = [YOUR_USER_ID]
ORDER BY created_at DESC
LIMIT 5;
```

You should see your 3 new requests with status `pending`.

### Check Counter Logic
```sql
SELECT COUNT(*) as future_count
FROM coaching_request
WHERE user_id = [YOUR_USER_ID] 
AND status IN ('pending', 'accepted');
```

Should return: **3**

---

## ✅ Verification Checklist

- [ ] Counter displays "0/3" initially (green)
- [ ] Counter updates to "1/3" after first reservation (orange)
- [ ] Counter updates to "2/3" after second reservation (orange)
- [ ] Counter updates to "3/3" after third reservation (red)
- [ ] Button is enabled when counter < 3
- [ ] Button is disabled when counter = 3
- [ ] Message updates correctly for remaining slots
- [ ] Reservations appear in "Mes Demandes" list
- [ ] Database shows 3 pending requests

---

## 🐛 Troubleshooting

### Counter not showing?
- Make sure you restarted the application
- Check browser console for errors (F12)
- Verify FXML file was updated correctly

### Button not disabling?
- Check that `updateButtonState()` is being called
- Look at console logs for any errors
- Verify `getRemainingSlots()` returns 0

### Reservations not appearing in "Mes Demandes"?
- Check database for coaching_request entries
- Verify user_id is correct
- Check that status is 'pending' or 'accepted'

### Error message when trying to book?
- This is expected when limit is reached
- Message should be in French
- Check console for full error details

---

## 📝 Console Logs to Look For

When making a reservation, you should see logs like:

```
[CalendarCoachController] Creating coaching request for user: 5
[CalendarCoachController] Coach ID: 3
[CalendarCoachController] Selected date: 2026-05-15
[CalendarCoachController] Selected slot: 09:00 - 10:00
[CalendarCoachController] User has 2 future requests
[CalendarCoachController] Coaching request created successfully!
[CalendarCoachController] Request ID: 42
```

---

## 🎯 Expected Behavior Summary

| Counter | Button State | Message | Color |
|---------|--------------|---------|-------|
| 0/3 | Enabled | Vous pouvez réserver 3 session(s) | 🟢 Green |
| 1/3 | Enabled | Vous pouvez réserver 2 session(s) | 🟠 Orange |
| 2/3 | Enabled | Vous pouvez réserver 1 session(s) | 🟠 Orange |
| 3/3 | Disabled | Limite atteinte - Vous ne pouvez plus réserver | 🔴 Red |

---

## 💡 Tips

1. **Test with multiple users** to ensure each user has their own limit
2. **Test cancelling a request** to verify the counter decreases
3. **Test with different coaches** to ensure coach_id is correct
4. **Check "Mes Demandes"** to verify requests are created properly

---

## 🎉 Success!

If all checks pass, the feature is working correctly! 

The user can now:
- ✅ See how many sessions they have booked
- ✅ Know how many more they can book
- ✅ Be prevented from booking more than 3 sessions
- ✅ See their reservations in "Mes Demandes"
