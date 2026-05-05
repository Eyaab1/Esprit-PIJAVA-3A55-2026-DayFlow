# 📅 Coach Availability Calendar - Quick Start (5 minutes)

## What Was Created

✅ **Backend (3 Files)**:
- `Disponibilite.java` - Model for availability slots
- `DisponibiliteRepository.java` - Database access layer
- `DisponibiliteService.java` - Business logic

✅ **Frontend (2 Files)**:
- `calendar_coach.fxml` - Calendar UI interface
- `CalendarCoachController.java` - Calendar controller

✅ **Database (1 File)**:
- `create_disponibilite_table.sql` - Database migration

## 3-Step Integration

### Step 1: Run Database Migration (1 minute)

Execute the SQL migration:
```sql
-- Run: database/migrations/create_disponibilite_table.sql
```

### Step 2: Add "Voir disponibilités" Button (2 minutes)

In your coach card or coach list view, add a button:

```java
Button viewAvailabilityButton = new Button("👁️ Voir disponibilités");
viewAvailabilityButton.setOnAction(e -> {
    try {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/user/coaching_session/calendar_coach.fxml")
        );
        Parent root = loader.load();
        CalendarCoachController controller = loader.getController();
        
        // Pass coach information
        controller.setCoachInfo(coachId, coachName);
        
        Stage stage = new Stage();
        stage.setTitle("Disponibilités - " + coachName);
        stage.setScene(new Scene(root, 1000, 700));
        stage.show();
    } catch (IOException ex) {
        ex.printStackTrace();
    }
});
```

### Step 3: Add Sample Data (2 minutes)

Insert sample availability data:

```sql
-- Insert sample availabilities for a coach
INSERT INTO disponibilite (coach_id, date, heure_debut, heure_fin, statut) VALUES
(1, '2026-05-10', '09:00:00', '10:00:00', 'disponible'),
(1, '2026-05-10', '10:00:00', '11:00:00', 'disponible'),
(1, '2026-05-10', '14:00:00', '15:00:00', 'disponible'),
(1, '2026-05-11', '09:00:00', '10:00:00', 'disponible'),
(1, '2026-05-11', '15:00:00', '16:00:00', 'disponible');
```

## Features Available Now

✅ **Calendar Display**
- Monthly calendar view
- Available slots highlighted in green
- Navigate between months

✅ **Slot Selection**
- Click on date to see available time slots
- Select a specific time slot
- View selected date and time

✅ **Reservation**
- Click "Réserver session" to book
- Confirmation dialog
- Automatic status update

✅ **User Feedback**
- Success/error messages
- Slot availability indicators
- Real-time updates

## File Locations

```
DayFlow/
├── src/main/java/model/coaching_session/
│   └── Disponibilite.java
├── src/main/java/repository/coaching_session/
│   └── DisponibiliteRepository.java
├── src/main/java/services/coaching_session_module/
│   └── DisponibiliteService.java
├── src/main/java/controllers/
│   └── CalendarCoachController.java
├── src/main/resources/user/coaching_session/
│   └── calendar_coach.fxml
└── database/migrations/
    └── create_disponibilite_table.sql
```

## Next Steps

1. ✅ Run database migration
2. ✅ Add button to coach view
3. ✅ Insert sample data
4. ✅ Test the calendar
5. ⏳ Customize styling
6. ⏳ Add more features

## Testing

1. Click "Voir disponibilités" button
2. Calendar should display
3. Click on a date with available slots
4. Select a time slot
5. Click "Réserver session"
6. Confirm reservation

---

**Ready to integrate?** Follow the 3 steps above!
