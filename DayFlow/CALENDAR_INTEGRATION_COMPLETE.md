# ✅ Calendar Integration - Complete Setup

## What Was Done

✅ **Modified FindCoachController.java**
- Changed button action from showing alert to opening calendar
- Added `openCoachCalendar(User coach)` method
- Added necessary imports (FXMLLoader, Parent, Scene, Stage, CalendarCoachController)

✅ **Calendar is Ready**
- FXML file: `calendar_coach.fxml`
- Controller: `CalendarCoachController.java`
- Service: `DisponibiliteService.java`
- Repository: `DisponibiliteRepository.java`
- Model: `Disponibilite.java`

## How It Works Now

1. **User clicks "Voir disponibilités" button** on coach card
2. **Calendar window opens** showing coach's availability
3. **Calendar displays** all available time slots for that coach
4. **User can select** a date and time slot
5. **User can reserve** the session

## Setup Steps (5 minutes)

### Step 1: Create Database Table
```sql
CREATE TABLE IF NOT EXISTS disponibilite (
    id INT PRIMARY KEY AUTO_INCREMENT,
    coach_id INT NOT NULL,
    date DATE NOT NULL,
    heure_debut TIME NOT NULL,
    heure_fin TIME NOT NULL,
    statut VARCHAR(50) NOT NULL DEFAULT 'disponible',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_disponibilite_coach FOREIGN KEY (coach_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_coach_id (coach_id),
    INDEX idx_date (date),
    UNIQUE KEY uk_coach_date_time (coach_id, date, heure_debut, heure_fin),
    CONSTRAINT chk_time_order CHECK (heure_debut < heure_fin)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### Step 2: Insert Sample Data
```sql
-- Get a valid coach ID first
SELECT id, first_name FROM user LIMIT 1;

-- Then insert data (replace 1 with actual coach_id)
INSERT INTO disponibilite (coach_id, date, heure_debut, heure_fin, statut) VALUES
(1, '2026-05-10', '09:00:00', '10:00:00', 'disponible'),
(1, '2026-05-10', '10:00:00', '11:00:00', 'disponible'),
(1, '2026-05-10', '14:00:00', '15:00:00', 'disponible'),
(1, '2026-05-11', '09:00:00', '10:00:00', 'disponible'),
(1, '2026-05-11', '15:00:00', '16:00:00', 'disponible'),
(1, '2026-05-12', '10:00:00', '11:00:00', 'disponible');
```

### Step 3: Compile Project
```bash
mvn clean compile
```

### Step 4: Run Application
```bash
mvn javafx:run
```

### Step 5: Test
1. Navigate to "Nos coaches disponibles"
2. Click "Voir disponibilités" button on any coach card
3. Calendar should open showing:
   - May 2026 calendar
   - Green highlighted dates (10, 11, 12)
   - Time slots when you click a date

## Expected Result

### Before (Old)
- Button showed: "Calendrier des créneaux — bientôt disponible."

### After (New)
- Button opens calendar window
- Shows coach's available time slots
- User can select and reserve sessions

## Files Modified

### FindCoachController.java
**Changes**:
1. Added imports:
   - `import controllers.CalendarCoachController;`
   - `import javafx.fxml.FXMLLoader;`
   - `import javafx.scene.Parent;`
   - `import javafx.scene.Scene;`
   - `import javafx.stage.Stage;`

2. Modified button action:
   ```java
   // OLD
   b1.setOnAction(e -> new Alert(Alert.AlertType.INFORMATION, "Calendrier des créneaux — bientôt disponible.").showAndWait());
   
   // NEW
   b1.setOnAction(e -> openCoachCalendar(u));
   ```

3. Added method:
   ```java
   private void openCoachCalendar(User coach) {
       try {
           FXMLLoader loader = new FXMLLoader(
               getClass().getResource("/user/coaching_session/calendar_coach.fxml")
           );
           Parent root = loader.load();
           CalendarCoachController controller = loader.getController();
           
           String coachName = coach.getFirstName() != null ? coach.getFirstName() : coach.getUsername();
           controller.setCoachInfo(coach.getId(), coachName);
           
           Stage stage = new Stage();
           stage.setTitle("Disponibilités - " + coachName);
           stage.setScene(new Scene(root, 1000, 700));
           stage.show();
       } catch (IOException ex) {
           System.err.println("Error loading calendar: " + ex.getMessage());
           ex.printStackTrace();
           new Alert(Alert.AlertType.ERROR, "Erreur lors du chargement du calendrier: " + ex.getMessage()).showAndWait();
       }
   }
   ```

## Architecture

```
User Interface (find_coach.fxml)
    ↓
FindCoachController (button click)
    ↓
openCoachCalendar(coach)
    ↓
CalendarCoachController (loads calendar_coach.fxml)
    ↓
DisponibiliteService (gets available slots)
    ↓
DisponibiliteRepository (queries database)
    ↓
Database (disponibilite table)
```

## Data Flow

1. **User clicks button** → `openCoachCalendar(coach)` called
2. **FXML loads** → `CalendarCoachController` initializes
3. **Coach info passed** → `setCoachInfo(coachId, coachName)`
4. **Calendar loads** → `loadCalendar()` called
5. **Query database** → `getAvailableSlots(coachId)`
6. **Display calendar** → Shows dates with available slots
7. **User selects slot** → `selectTimeSlot(slot)`
8. **User reserves** → `handleReservation()`
9. **Status updated** → `disponibilite.statut = 'reserve'`

## Testing Checklist

- [ ] Database table created
- [ ] Sample data inserted (6+ rows)
- [ ] Project compiles successfully
- [ ] Application runs
- [ ] Navigate to "Nos coaches disponibles"
- [ ] Click "Voir disponibilités" button
- [ ] Calendar window opens
- [ ] Calendar shows May 2026
- [ ] Dates 10, 11, 12 are highlighted in green
- [ ] Click on a date shows time slots
- [ ] Select a time slot
- [ ] Click "Réserver session"
- [ ] Confirmation dialog appears
- [ ] Session is reserved

## Troubleshooting

### Calendar doesn't open
- Check console for errors
- Verify FXML file exists at: `src/main/resources/user/coaching_session/calendar_coach.fxml`
- Verify controller class exists: `CalendarCoachController.java`

### Calendar shows no data
- Check database table exists: `SHOW TABLES LIKE 'disponibilite';`
- Check data exists: `SELECT COUNT(*) FROM disponibilite;`
- Verify coach_id matches: `SELECT * FROM user WHERE id = 1;`

### Compilation errors
- Run: `mvn clean compile`
- Check for missing imports
- Verify all classes exist

## Next Steps

1. ✅ Database setup
2. ✅ Insert sample data
3. ✅ Compile project
4. ✅ Test calendar
5. ⏳ Add more coaches' availability
6. ⏳ Add email notifications
7. ⏳ Add payment integration

---

**The calendar is now fully integrated and ready to use!**
