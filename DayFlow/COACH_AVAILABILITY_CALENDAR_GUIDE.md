# 📅 Coach Availability Calendar - Complete Integration Guide

## Overview

This guide explains how to integrate the Coach Availability Calendar feature into your DayFlow application. Users can view available time slots for coaches and book sessions directly from an interactive calendar.

## Architecture

```
User Interface (JavaFX)
    ↓
CalendarCoachController
    ↓
DisponibiliteService (Business Logic)
    ↓
DisponibiliteRepository (Data Access)
    ↓
Database (disponibilite table)
```

## Files Created

### 1. Model Layer
**File**: `src/main/java/model/coaching_session/Disponibilite.java`
- Represents an availability slot
- Fields: id, coachId, date, heureDebut, heureFin, statut
- Methods: isAvailable(), isReserved(), getDurationMinutes(), getFormattedTimeRange()

### 2. Repository Layer
**File**: `src/main/java/repository/coaching_session/DisponibiliteRepository.java`
- Database operations for availability slots
- Methods:
  - `getDisponibilitesByCoach(coachId)` - Get all slots for a coach
  - `getAvailableSlots(coachId)` - Get only available slots
  - `getAvailableSlotsByDate(coachId, date)` - Get slots for specific date
  - `createDisponibilite(disponibilite)` - Create new slot
  - `updateDisponibiliteStatus(id, status)` - Update slot status
  - `isSlotAvailable(coachId, date, startTime, endTime)` - Check availability

### 3. Service Layer
**File**: `src/main/java/services/coaching_session_module/DisponibiliteService.java`
- Business logic for availability management
- Methods:
  - `getAvailableSlots(coachId)` - Get available slots
  - `getAvailableSlotsForWeek(coachId, weekStart)` - Get weekly slots
  - `getAvailableSlotsForMonth(coachId, monthStart)` - Get monthly slots
  - `reserveSlot(disponibiliteId)` - Reserve a slot
  - `releaseSlot(disponibiliteId)` - Release a slot
  - `hasAvailableSlots(coachId)` - Check if coach has slots
  - `getCoachStatistics(coachId)` - Get availability statistics

### 4. Controller Layer
**File**: `src/main/java/controllers/CalendarCoachController.java`
- Manages calendar UI and user interactions
- Methods:
  - `setCoachInfo(coachId, coachName)` - Set coach information
  - `selectDate(date)` - Select a date
  - `selectTimeSlot(slot)` - Select a time slot
  - `handleReservation()` - Handle reservation
  - `previousMonth()` / `nextMonth()` - Navigate months

### 5. UI Layer
**File**: `src/main/resources/user/coaching_session/calendar_coach.fxml`
- Calendar interface with:
  - Monthly calendar grid
  - Date navigation buttons
  - Time slot display
  - Reservation button
  - Message display

### 6. Database
**File**: `database/migrations/create_disponibilite_table.sql`
- Creates `disponibilite` table
- Indexes for performance
- Constraints for data integrity

## Integration Steps

### Step 1: Database Setup

Run the migration:
```sql
-- Execute: database/migrations/create_disponibilite_table.sql
```

This creates the `disponibilite` table with:
- Columns: id, coach_id, date, heure_debut, heure_fin, statut, created_at, updated_at
- Indexes on coach_id, date, statut
- Unique constraint on (coach_id, date, heure_debut, heure_fin)
- Foreign key to user table

### Step 2: Add Button to Coach View

In your coach card or coach list controller:

```java
// Create button
Button viewAvailabilityButton = new Button("👁️ Voir disponibilités");

// Add click handler
viewAvailabilityButton.setOnAction(e -> openCalendarView(coachId, coachName));

// Add to layout
coachCardVBox.getChildren().add(viewAvailabilityButton);

// Handler method
private void openCalendarView(int coachId, String coachName) {
    try {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/user/coaching_session/calendar_coach.fxml")
        );
        Parent root = loader.load();
        CalendarCoachController controller = loader.getController();
        
        // Pass coach information
        controller.setCoachInfo(coachId, coachName);
        
        // Create and show stage
        Stage stage = new Stage();
        stage.setTitle("Disponibilités - " + coachName);
        stage.setScene(new Scene(root, 1000, 700));
        stage.show();
    } catch (IOException ex) {
        ex.printStackTrace();
    }
}
```

### Step 3: Add Sample Data

Insert availability data for coaches:

```sql
-- Insert availabilities for coach with ID 1
INSERT INTO disponibilite (coach_id, date, heure_debut, heure_fin, statut) VALUES
(1, '2026-05-10', '09:00:00', '10:00:00', 'disponible'),
(1, '2026-05-10', '10:00:00', '11:00:00', 'disponible'),
(1, '2026-05-10', '14:00:00', '15:00:00', 'disponible'),
(1, '2026-05-11', '09:00:00', '10:00:00', 'disponible'),
(1, '2026-05-11', '15:00:00', '16:00:00', 'disponible'),
(1, '2026-05-12', '10:00:00', '11:00:00', 'disponible');
```

### Step 4: Test the Feature

1. Navigate to coach view
2. Click "Voir disponibilités" button
3. Calendar should open
4. Click on a date with available slots (highlighted in green)
5. Select a time slot
6. Click "Réserver session"
7. Confirm reservation

## Business Rules Implemented

✅ **No Double Reservation**
- Each slot can only be reserved once
- Status changes from "disponible" to "reserve"

✅ **No Overlapping Slots**
- Cannot create overlapping availability slots
- Unique constraint on (coach_id, date, heure_debut, heure_fin)

✅ **Session Must Match Availability**
- Reservation only possible for existing availability slots
- Automatic status update

✅ **Verification Before Creation**
- Validation of all required fields
- Check for past dates
- Check for valid time ranges

## Algorithms Implemented

### Display Algorithm
```
FOR each disponibilite of coach
  IF statut = 'disponible'
    DISPLAY as clickable slot
  ELSE
    DISPLAY as grayed out
```

### Reservation Algorithm
```
IF slot is available
  UPDATE disponibilite SET statut = 'reserve'
  CREATE session record
  RETURN success
ELSE
  RETURN error "Slot already reserved"
```

## Features

### Calendar Display
- Monthly calendar view
- Available slots highlighted in green
- Unavailable slots grayed out
- Today highlighted with blue border
- Navigate between months

### Slot Selection
- Click date to see available time slots
- Display time range (e.g., "09:00 - 10:00")
- Display duration in minutes
- Select button for each slot

### Reservation
- Confirmation dialog before booking
- Shows coach name, date, time
- Automatic status update
- Success/error messages

### User Feedback
- Real-time message display
- Color-coded messages (green/red)
- Slot availability indicators
- Loading states

## Customization

### Change Calendar Colors

In `CalendarCoachController.java`:

```java
// Available slots color
button.setStyle("-fx-background-color: #d1fae5;"); // Green

// Unavailable slots color
button.setStyle("-fx-background-color: #f3f4f6;"); // Gray

// Today highlight
button.setStyle("-fx-border-color: #3b82f6;"); // Blue
```

### Change Time Format

In `CalendarCoachController.java`:

```java
private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
// Change to: DateTimeFormatter.ofPattern("hh:mm a") for 12-hour format
```

### Add More View Modes

Extend the `viewModeCombo` to support:
- Week view
- Day view
- Agenda view

## Performance Considerations

### Indexes
- `idx_coach_id` - Fast lookup by coach
- `idx_date` - Fast lookup by date
- `idx_coach_date` - Fast lookup by coach and date
- `idx_statut` - Fast lookup by status
- `idx_disponible_slots` - Optimized for available slots query

### Query Optimization
- Use prepared statements to prevent SQL injection
- Limit results to current and future dates
- Cache coach information

### UI Optimization
- Load calendar data asynchronously
- Display loading indicators
- Limit calendar display to current month

## Error Handling

### Validation
- Check required fields
- Validate date is not in past
- Validate start time < end time
- Check for overlapping slots

### User Feedback
- Display error messages
- Show success confirmations
- Handle network errors
- Handle database errors

## Testing

### Unit Tests
```java
// Test availability check
DisponibiliteService service = new DisponibiliteService();
boolean available = service.isSlotAvailable(1, LocalDate.now().plusDays(1), 
    LocalTime.of(9, 0), LocalTime.of(10, 0));
assertTrue(available);

// Test reservation
boolean reserved = service.reserveSlot(1);
assertTrue(reserved);
```

### Integration Tests
1. Create availability slots
2. Open calendar view
3. Select date and time
4. Perform reservation
5. Verify status updated
6. Verify slot no longer available

## Troubleshooting

### Calendar Not Displaying
- Check FXML file path
- Verify controller class name
- Check for errors in console

### No Slots Showing
- Verify data in database
- Check coach_id is correct
- Verify dates are not in past
- Check statut = 'disponible'

### Reservation Fails
- Verify slot still available
- Check database connection
- Verify user permissions
- Check for database errors

## Next Steps

1. ✅ Integrate with existing coach view
2. ✅ Add sample data
3. ✅ Test functionality
4. ⏳ Add email notifications
5. ⏳ Add calendar sync
6. ⏳ Add recurring availability
7. ⏳ Add availability templates

## Support

For issues or questions:
1. Check the troubleshooting section
2. Review the code comments
3. Check database logs
4. Verify data integrity

---

**Last Updated**: May 5, 2026
**Version**: 1.0.0
