# 📅 Calendar Integration - Complete Steps

## Problem: Calendar Shows Nothing

**Cause**: You haven't:
1. Created the database table
2. Inserted sample data
3. Added the button to open the calendar
4. Passed the coach information

## ✅ Complete Solution (10 minutes)

### Step 1: Create Database Table (2 minutes)

Execute this SQL:
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
    INDEX idx_coach_date (coach_id, date),
    UNIQUE KEY uk_coach_date_time (coach_id, date, heure_debut, heure_fin),
    CONSTRAINT chk_time_order CHECK (heure_debut < heure_fin)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Verify**:
```sql
SHOW TABLES LIKE 'disponibilite';
-- Should show: disponibilite
```

### Step 2: Insert Sample Data (2 minutes)

Execute this SQL:
```sql
INSERT INTO disponibilite (coach_id, date, heure_debut, heure_fin, statut) VALUES
(1, '2026-05-10', '09:00:00', '10:00:00', 'disponible'),
(1, '2026-05-10', '10:00:00', '11:00:00', 'disponible'),
(1, '2026-05-10', '14:00:00', '15:00:00', 'disponible'),
(1, '2026-05-11', '09:00:00', '10:00:00', 'disponible'),
(1, '2026-05-11', '15:00:00', '16:00:00', 'disponible'),
(1, '2026-05-12', '10:00:00', '11:00:00', 'disponible');
```

**Verify**:
```sql
SELECT COUNT(*) FROM disponibilite;
-- Should show: 6
```

### Step 3: Find Where to Add Button (2 minutes)

Find the controller that displays coaches. Common locations:
- `FindCoachViewController.java`
- `CoachCardController.java`
- `CoachingRequestController.java`
- Any view that shows coach cards/list

Look for code like:
```java
// Coach display code
Label coachName = new Label(coach.getNom());
Button bookButton = new Button("Réserver");
```

### Step 4: Add Button Code (2 minutes)

Add this code where coaches are displayed:

```java
// Import statements (add at top of file)
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import controllers.CalendarCoachController;
import java.io.IOException;

// In your coach display method/loop
Button viewAvailabilityButton = new Button("👁️ Voir disponibilités");
viewAvailabilityButton.setStyle("-fx-padding: 8 15; -fx-font-size: 11;");
viewAvailabilityButton.setOnAction(e -> openCalendarView(coachId, coachName));

// Add button to your layout
coachCardVBox.getChildren().add(viewAvailabilityButton);

// Add this method to your controller class
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
        System.err.println("Error loading calendar: " + ex.getMessage());
        ex.printStackTrace();
    }
}
```

### Step 5: Test (2 minutes)

1. Restart application
2. Navigate to coach view
3. Click "👁️ Voir disponibilités" button
4. Calendar should open and display:
   - May 2026 calendar
   - Green highlighted dates (May 10, 11, 12)
   - Time slots when you click a date

## 🎯 Expected Result

When you click the button:
1. ✅ New window opens
2. ✅ Shows "📅 Disponibilités - Coach Name"
3. ✅ Calendar displays May 2026
4. ✅ Dates with slots are highlighted in green
5. ✅ Click a date to see time slots
6. ✅ Select a time slot
7. ✅ Click "✓ Réserver session"

## 🐛 If Still Not Working

### Check 1: Database Table
```sql
SHOW TABLES LIKE 'disponibilite';
```
**Expected**: Table should exist

### Check 2: Data in Table
```sql
SELECT COUNT(*) FROM disponibilite;
```
**Expected**: Should return > 0

### Check 3: Coach Exists
```sql
SELECT * FROM user WHERE id = 1;
```
**Expected**: Should return a coach record

### Check 4: Console Errors
Look for error messages like:
```
Error loading calendar: ...
Error fetching disponibilites: ...
```

### Check 5: FXML File
Verify file exists at:
```
src/main/resources/user/coaching_session/calendar_coach.fxml
```

## 📋 Complete Checklist

- [ ] Database table `disponibilite` created
- [ ] Sample data inserted (6+ rows)
- [ ] Button added to coach view
- [ ] Button click handler implemented
- [ ] `openCalendarView()` method added
- [ ] FXML file exists
- [ ] Controller class exists
- [ ] Application restarted
- [ ] Button visible on coach view
- [ ] Calendar opens when button clicked
- [ ] Calendar displays dates with data
- [ ] Time slots appear when date clicked

## 🚀 Quick Copy-Paste Solution

### 1. SQL to Create Table
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

### 2. SQL to Insert Data
```sql
INSERT INTO disponibilite (coach_id, date, heure_debut, heure_fin, statut) VALUES
(1, '2026-05-10', '09:00:00', '10:00:00', 'disponible'),
(1, '2026-05-10', '10:00:00', '11:00:00', 'disponible'),
(1, '2026-05-10', '14:00:00', '15:00:00', 'disponible'),
(1, '2026-05-11', '09:00:00', '10:00:00', 'disponible'),
(1, '2026-05-11', '15:00:00', '16:00:00', 'disponible'),
(1, '2026-05-12', '10:00:00', '11:00:00', 'disponible');
```

### 3. Java Code to Add Button
```java
// Add to your coach display controller
Button viewAvailabilityButton = new Button("👁️ Voir disponibilités");
viewAvailabilityButton.setOnAction(e -> openCalendarView(coachId, coachName));
coachCardVBox.getChildren().add(viewAvailabilityButton);

private void openCalendarView(int coachId, String coachName) {
    try {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/user/coaching_session/calendar_coach.fxml")
        );
        Parent root = loader.load();
        CalendarCoachController controller = loader.getController();
        controller.setCoachInfo(coachId, coachName);
        
        Stage stage = new Stage();
        stage.setTitle("Disponibilités - " + coachName);
        stage.setScene(new Scene(root, 1000, 700));
        stage.show();
    } catch (IOException ex) {
        ex.printStackTrace();
    }
}
```

---

**Follow these steps and the calendar will work!**
