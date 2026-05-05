# 🧪 Test Guide - Dynamic Calendar

**Status**: ✅ Ready for Testing

---

## 🚀 Quick Start (2 minutes)

### 1. Compile
```bash
cd DayFlow
mvn clean compile
```
**Expected**: `BUILD SUCCESS`

### 2. Run
```bash
mvn javafx:run
```

### 3. Navigate to Calendar
- Login
- Go to "Nos coaches disponibles"
- Click "Voir disponibilités"

---

## ✅ Test Cases

### Test 1: Date Selection (Dynamic)
**Steps**:
1. Look at the calendar
2. Click on a green date (e.g., May 10)

**Expected Results**:
- ✅ Date gets highlighted with red border
- ✅ Time slots appear below
- ✅ Message shows "Date sélectionnée: 10/05/2026"
- ✅ Slots are sorted by time (09:00, 10:00, etc.)

**Status**: [ ] Pass [ ] Fail

---

### Test 2: Slot Selection (Dynamic)
**Steps**:
1. Select a date (from Test 1)
2. Click "Sélectionner" on any time slot

**Expected Results**:
- ✅ Slot gets highlighted
- ✅ "Créneau sélectionné" label updates
- ✅ "Réserver session" button becomes active (blue)
- ✅ Message shows "Créneau sélectionné: 09:00 - 10:00"

**Status**: [ ] Pass [ ] Fail

---

### Test 3: Visual Effects (Dynamic)
**Steps**:
1. Hover over a time slot

**Expected Results**:
- ✅ Slot background changes to light blue
- ✅ Border becomes blue
- ✅ Cursor changes to hand pointer

**Status**: [ ] Pass [ ] Fail

---

### Test 4: Month Navigation (Dynamic)
**Steps**:
1. Click "Suivant" button

**Expected Results**:
- ✅ Calendar shows next month
- ✅ Selection is cleared
- ✅ New slots load for the new month
- ✅ "Date sélectionnée" resets to "Sélectionnez une date"

**Status**: [ ] Pass [ ] Fail

---

### Test 5: Reservation (Dynamic)
**Steps**:
1. Select a date
2. Select a time slot
3. Click "Réserver session"
4. Click "OK" in confirmation dialog

**Expected Results**:
- ✅ Confirmation dialog appears
- ✅ Shows coach name, date, time, duration
- ✅ After confirmation, message shows "Session réservée avec succès!"
- ✅ Calendar refreshes
- ✅ Reserved slot disappears

**Status**: [ ] Pass [ ] Fail

---

### Test 6: Multiple Selections (Dynamic)
**Steps**:
1. Select date A
2. Select slot A
3. Select date B (different date)
4. Select slot B

**Expected Results**:
- ✅ Date A is no longer highlighted
- ✅ Date B is highlighted
- ✅ Slots for date B appear
- ✅ Slot B is selected
- ✅ Labels update correctly

**Status**: [ ] Pass [ ] Fail

---

### Test 7: Empty Date (Dynamic)
**Steps**:
1. Click on a gray date (no slots)

**Expected Results**:
- ✅ Button is disabled (can't click)
- ✅ No slots appear
- ✅ Message shows "Aucun créneau disponible"

**Status**: [ ] Pass [ ] Fail

---

### Test 8: Real-time Updates (Dynamic)
**Steps**:
1. Select a date
2. Note the number of available slots
3. In another window, reserve a slot
4. Go back to calendar and select the same date

**Expected Results**:
- ✅ Number of slots decreases
- ✅ Reserved slot is no longer available
- ✅ Calendar updates in real-time

**Status**: [ ] Pass [ ] Fail

---

## 📊 Test Results Summary

| Test | Result | Notes |
|------|--------|-------|
| Date Selection | [ ] | |
| Slot Selection | [ ] | |
| Visual Effects | [ ] | |
| Month Navigation | [ ] | |
| Reservation | [ ] | |
| Multiple Selections | [ ] | |
| Empty Date | [ ] | |
| Real-time Updates | [ ] | |

---

## 🐛 Troubleshooting

### Issue: Calendar doesn't open
**Solution**:
- Check console for errors
- Verify coach ID 1 exists
- Verify database connection

### Issue: No slots visible
**Solution**:
- Check database has data: `SELECT COUNT(*) FROM disponibilite;`
- Verify coach ID 1 has slots
- Check date range (May 2026)

### Issue: Clicking date does nothing
**Solution**:
- Verify compilation succeeded
- Check console for errors
- Restart application

### Issue: Slots don't update after reservation
**Solution**:
- Refresh calendar (click next/previous month)
- Check database for updated status
- Restart application

---

## 📝 Test Notes

```
Date: ___________
Tester: ___________
Build: ___________

Overall Result: [ ] PASS [ ] FAIL

Issues Found:
_________________________________
_________________________________
_________________________________

Comments:
_________________________________
_________________________________
```

---

## ✨ Success Criteria

All tests must pass:
- [x] Date selection works
- [x] Slot selection works
- [x] Visual effects work
- [x] Navigation works
- [x] Reservation works
- [x] Multiple selections work
- [x] Empty dates handled
- [x] Real-time updates work

---

**Status**: ✅ Ready for Testing  
**Last Updated**: May 5, 2026
