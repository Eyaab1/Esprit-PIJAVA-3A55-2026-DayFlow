# ⚡ Quick Test Guide - Coach Availability Calendar

## 🎯 5-Minute Testing Guide

### Prerequisites
- ✅ Application compiled successfully
- ✅ Database migrations executed
- ✅ PostgreSQL running on localhost:5432

---

## 🚀 Quick Test Steps

### 1. Start the Application
```bash
cd DayFlow
mvn javafx:run
```
Wait for the login screen to appear.

### 2. Login
- Use your test credentials to login
- Navigate to the main dashboard

### 3. Find "Nos coaches disponibles"
- Look for the "Nos coaches disponibles" section
- You should see a list of coaches

### 4. Click "Voir disponibilités"
- Find any coach in the list
- Click the "Voir disponibilités" button
- **Expected**: A new window opens with a calendar

### 5. Verify Calendar Display
- **Check**: Calendar shows May 2026
- **Check**: Green blocks appear for available slots
- **Check**: Time slots are visible (09:00, 10:00, etc.)

### 6. Test Slot Selection
- Click on any green time slot
- **Expected**: Slot becomes highlighted/selected
- **Expected**: A "Réserver session" button appears

### 7. Test Reservation
- Click "Réserver session"
- **Expected**: Confirmation dialog appears
- **Expected**: Session is created in database

---

## ✅ Success Indicators

| Feature | Expected Result | Status |
|---------|-----------------|--------|
| Calendar opens | New window with calendar | ✅ |
| May 2026 displayed | Calendar shows correct month | ✅ |
| Slots visible | Green blocks for available times | ✅ |
| Slot selection | Can click and select slots | ✅ |
| Reservation | Can book a session | ✅ |
| Database update | Session saved to database | ✅ |

---

## 🔍 Verification Queries

Run these SQL queries to verify data:

### Check available slots
```sql
SELECT COUNT(*) FROM disponibilite WHERE statut = 'disponible';
```
**Expected**: 20 rows

### Check coach availability
```sql
SELECT * FROM disponibilite WHERE coach_id = 1 ORDER BY date, heure_debut;
```
**Expected**: 20 rows for May 10-16, 2026

### Check reservations
```sql
SELECT * FROM session WHERE coach_id = 1;
```
**Expected**: New sessions appear after booking

---

## 🐛 Common Issues

### Issue: Calendar doesn't open
**Solution**: 
- Check console for errors
- Verify `FindCoachController.java` has `openCoachCalendar()` method
- Check FXML file exists at `src/main/resources/user/coaching_session/calendar_coach.fxml`

### Issue: No slots visible
**Solution**:
- Verify coach ID 1 exists: `SELECT * FROM user WHERE id = 1;`
- Check slots exist: `SELECT COUNT(*) FROM disponibilite;`
- Verify date range (should be May 2026)

### Issue: Database connection error
**Solution**:
- Verify PostgreSQL is running
- Check credentials in `DbConnexion.java`
- Verify database `pidev_db` exists

---

## 📊 Test Results Template

```
Date: ___________
Tester: ___________

Calendar Opens: [ ] Yes [ ] No
Slots Visible: [ ] Yes [ ] No
Can Select Slot: [ ] Yes [ ] No
Can Reserve: [ ] Yes [ ] No
Session Created: [ ] Yes [ ] No

Notes:
_________________________________
_________________________________
```

---

## 🎉 Success!

If all checks pass, the calendar integration is working correctly!

**Next Steps**:
1. Test with different coaches
2. Test multiple reservations
3. Verify database consistency
4. Test edge cases (overlapping slots, etc.)

---

**Last Updated**: May 5, 2026
