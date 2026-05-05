# ✅ TASK 5: COMPLETE IMPLEMENTATION - Session Reservation Limit & Slot Selection

## STATUS: ✅ COMPLETE

All modifications have been successfully implemented and compiled. The application now:
1. ✅ Prevents users from booking more than 3 future sessions
2. ✅ Displays the session counter on the calendar interface
3. ✅ Shows remaining slots message
4. ✅ Disables the "Réserver" button when limit is reached
5. ✅ Creates coaching requests when slots are selected

---

## WHAT WAS IMPLEMENTED

### 1. Backend: Session Reservation Limit Logic

#### File: `CoachingRequestService.java`
**Added two new methods:**

```java
/**
 * Compte le nombre de demandes futures pour un utilisateur.
 * Les demandes futures sont celles avec le statut 'pending' ou 'accepted'.
 */
public int countFutureRequests(int userId) throws SQLException {
    String sql = """
            SELECT COUNT(*) as count
            FROM coaching_request
            WHERE user_id = ? AND status IN ('pending', 'accepted')
            """;
    try (PreparedStatement ps = cnx.prepareStatement(sql)) {
        ps.setInt(1, userId);
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("count");
            }
        }
    }
    return 0;
}

/**
 * Récupère le nombre de créneaux restants pour un utilisateur.
 * La limite est de 3 sessions futures.
 */
public int getRemainingSlots(int userId) throws SQLException {
    int futureCount = countFutureRequests(userId);
    int remaining = 3 - futureCount;
    return Math.max(0, remaining);
}
```

**Location:** Before the closing brace of `CoachingRequestService` class

---

### 2. Backend: Slot Selection Fix

#### File: `CalendarCoachController.java`
**Modified `reserve()` method:**
- Now calls `createCoachingRequest()` to create a coaching request in the database
- Calls `updateSessionCount()` after successful creation to refresh the UI counter

**Added `createCoachingRequest()` method:**
- Retrieves current user from `AppSession`
- Checks the 3-session limit using `CoachingRequestService.countFutureRequests()`
- Throws `IllegalArgumentException` if limit is reached
- Creates a `CoachingRequest` object with proper fields
- Saves to database using `requestService.create(request)`

---

### 3. UI: Session Counter Display

#### File: `calendar_coach.fxml`
**Added new UI components in the header:**

```xml
<!-- Session Counter -->
<HBox spacing="15" alignment="CENTER_LEFT" style="-fx-padding: 10; -fx-background-color: #eff6ff; -fx-border-color: #bfdbfe; -fx-border-radius: 6;">
    <Label text="Sessions futures:" style="-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #1e40af;"/>
    <Label fx:id="sessionCountLabel" text="0/3" 
           style="-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #10b981;"/>
    <Label fx:id="remainingSlotsLabel" text="Vous pouvez réserver 3 session(s)" 
           style="-fx-font-size: 11; -fx-text-fill: #059669; -fx-padding: 0 0 0 10;"/>
</HBox>
```

**Features:**
- Displays "X/3" format (e.g., "2/3")
- Color changes based on status:
  - 🟢 Green (0 sessions): User can book
  - 🟠 Orange (1-2 sessions): User can still book
  - 🔴 Red (3 sessions): Limit reached
- Shows remaining slots message
- Updates dynamically after each reservation

---

### 4. UI: Button State Management

#### File: `CalendarCoachController.java`
**Added new methods:**

```java
/**
 * Met à jour l'affichage du compteur de sessions futures.
 */
private void updateSessionCount() {
    // Retrieves current user
    // Counts future requests
    // Updates sessionCountLabel with color changes
    // Updates remainingSlotsLabel with message
    // Calls updateButtonState()
}

/**
 * Met à jour l'état du bouton "Réserver" en fonction de la limite.
 */
private void updateButtonState(boolean canBook) {
    if (canBook) {
        reserveButton.setDisable(false);
        // Green style
    } else {
        reserveButton.setDisable(true);
        // Gray style
    }
}
```

**Behavior:**
- Button is **ENABLED** (green) when user has slots remaining
- Button is **DISABLED** (gray) when user has reached the 3-session limit
- Button state updates automatically after each reservation

---

### 5. UI: FXML Fields

#### File: `CalendarCoachController.java`
**Added new FXML fields:**

```java
@FXML public Label sessionCountLabel;
@FXML public Label remainingSlotsLabel;
```

These fields are bound to the FXML components and updated dynamically.

---

## HOW IT WORKS - COMPLETE FLOW

### User Journey:

1. **User opens calendar** → `setCoachInfo()` is called
   - Loads calendar
   - Calls `updateSessionCount()` to display current counter

2. **User selects a date and slot** → `selectSlot()` is called
   - Slot is highlighted
   - "Réserver" button becomes enabled (if limit not reached)

3. **User clicks "Réserver"** → `reserve()` is called
   - Shows confirmation dialog
   - If confirmed, calls `createCoachingRequest()`

4. **`createCoachingRequest()` executes:**
   - Gets current user from `AppSession`
   - Calls `countFutureRequests()` to check limit
   - If count >= 3: throws `IllegalArgumentException` with French message
   - If count < 3: creates `CoachingRequest` and saves to database

5. **After successful creation:**
   - Calls `updateSessionCount()` to refresh counter
   - Counter updates with new count
   - Button state updates based on remaining slots
   - Success message displays

6. **If limit reached:**
   - Error message displays: "Vous avez atteint la limite de 3 sessions futures..."
   - Button becomes disabled
   - Counter shows "3/3" in red

---

## DATABASE QUERIES

### Count Future Requests:
```sql
SELECT COUNT(*) as count
FROM coaching_request
WHERE user_id = ? AND status IN ('pending', 'accepted')
```

### Statuses Counted:
- `pending` - Demande en attente
- `accepted` - Demande acceptée

### Statuses NOT Counted:
- `completed` - Session terminée
- `cancelled` - Demande annulée
- `scheduling` - En cours de planification

---

## FILES MODIFIED

1. ✅ `src/main/java/services/coaching_session_module/CoachingRequestService.java`
   - Added `countFutureRequests(userId)` method
   - Added `getRemainingSlots(userId)` method

2. ✅ `src/main/java/controllers/CalendarCoachController.java`
   - Added FXML fields: `sessionCountLabel`, `remainingSlotsLabel`
   - Added `updateSessionCount()` method
   - Added `updateButtonState(canBook)` method
   - Modified `setCoachInfo()` to call `updateSessionCount()`
   - Modified `reserve()` to call `updateSessionCount()` after creation

3. ✅ `src/main/resources/user/coaching_session/calendar_coach.fxml`
   - Added session counter HBox with labels
   - Added styling for counter display

---

## BUILD STATUS

✅ **BUILD SUCCESSFUL**
- All 131 source files compiled without errors
- No compilation errors or warnings related to new code
- Build time: ~27 seconds

---

## TESTING CHECKLIST

### Test 1: Initial Load
- [ ] Open calendar
- [ ] Verify counter shows "0/3" in green
- [ ] Verify message shows "Vous pouvez réserver 3 session(s)"
- [ ] Verify "Réserver" button is enabled

### Test 2: After First Reservation
- [ ] Select a date and slot
- [ ] Click "Réserver"
- [ ] Confirm reservation
- [ ] Verify counter shows "1/3" in orange
- [ ] Verify message shows "Vous pouvez réserver 2 session(s)"
- [ ] Verify "Réserver" button is still enabled

### Test 3: After Second Reservation
- [ ] Select another date and slot
- [ ] Click "Réserver"
- [ ] Confirm reservation
- [ ] Verify counter shows "2/3" in orange
- [ ] Verify message shows "Vous pouvez réserver 1 session(s)"
- [ ] Verify "Réserver" button is still enabled

### Test 4: After Third Reservation
- [ ] Select another date and slot
- [ ] Click "Réserver"
- [ ] Confirm reservation
- [ ] Verify counter shows "3/3" in red
- [ ] Verify message shows "Limite atteinte - Vous ne pouvez plus réserver"
- [ ] Verify "Réserver" button is DISABLED (gray)

### Test 5: Limit Reached
- [ ] Try to select a slot when limit is reached
- [ ] Verify "Réserver" button is disabled
- [ ] Try to click disabled button (should not work)
- [ ] Verify error message displays if attempting to reserve

### Test 6: Verify in "Mes Demandes"
- [ ] Navigate to "Mes Demandes"
- [ ] Verify all 3 reservations appear in the list
- [ ] Verify each shows correct date, time, and coach

---

## NEXT STEPS FOR USER

1. **Restart the application** to load the new code
2. **Test the complete flow** using the checklist above
3. **Verify in database** that coaching requests are created:
   ```sql
   SELECT id, user_id, coach_id, status, created_at 
   FROM coaching_request 
   WHERE user_id = ? 
   ORDER BY created_at DESC;
   ```

---

## FRENCH MESSAGES

### Success Messages:
- "✓ Session réservée!" - Reservation successful
- "Vous pouvez réserver X session(s)" - Remaining slots message

### Error Messages:
- "❌ Sélectionnez une date et un créneau" - Select date and slot
- "❌ Erreur lors de la création de la demande: ..." - Error creating request
- "Vous avez atteint la limite de 3 sessions futures. Veuillez terminer ou annuler une session avant de réserver à nouveau." - Limit reached

### UI Labels:
- "Sessions futures:" - Future sessions label
- "Limite atteinte - Vous ne pouvez plus réserver" - Limit reached message

---

## TECHNICAL DETAILS

### Color Scheme:
- **Green (#10b981)**: 0 sessions, can book
- **Orange (#f59e0b)**: 1-2 sessions, can book
- **Red (#ef4444)**: 3 sessions, cannot book
- **Gray (#d1d5db)**: Button disabled

### Button States:
- **Enabled**: `-fx-background-color: #10b981; -fx-text-fill: white;`
- **Disabled**: `-fx-background-color: #d1d5db; -fx-text-fill: #6b7280;`

### Counter Box Styling:
- Background: Light blue (#eff6ff)
- Border: Blue (#bfdbfe)
- Border radius: 6px
- Padding: 10px

---

## SUMMARY

✅ **All requirements completed:**
1. ✅ Session reservation limit (max 3 future sessions)
2. ✅ Limit check before creating coaching request
3. ✅ UI counter display with color changes
4. ✅ Remaining slots message
5. ✅ Button state management (enable/disable)
6. ✅ Error handling with French messages
7. ✅ Database integration
8. ✅ Code compiles without errors

**The application is ready for testing!**
