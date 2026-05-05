# 🔧 Calendar Not Displaying - Troubleshooting Guide

## ❌ Problem: Calendar Shows Nothing

When you click "Voir disponibilités", the calendar opens but displays no data.

## 🔍 Root Causes

### 1. Table `disponibilite` Doesn't Exist
**Check**: Run this SQL query
```sql
SHOW TABLES LIKE 'disponibilite';
```
**Expected**: Table should appear in results

**If not found**:
```sql
-- Run the migration
-- Execute: database/migrations/create_disponibilite_table.sql
```

### 2. Table is Empty (No Data)
**Check**: Run this SQL query
```sql
SELECT COUNT(*) FROM disponibilite;
```
**Expected**: Should return > 0

**If returns 0**:
```sql
-- Insert sample data
INSERT INTO disponibilite (coach_id, date, heure_debut, heure_fin, statut) VALUES
(1, '2026-05-10', '09:00:00', '10:00:00', 'disponible'),
(1, '2026-05-10', '10:00:00', '11:00:00', 'disponible'),
(1, '2026-05-10', '14:00:00', '15:00:00', 'disponible'),
(1, '2026-05-11', '09:00:00', '10:00:00', 'disponible'),
(1, '2026-05-11', '15:00:00', '16:00:00', 'disponible'),
(1, '2026-05-12', '10:00:00', '11:00:00', 'disponible');
```

### 3. Coach ID Doesn't Match
**Check**: Verify coach exists
```sql
SELECT id, nom FROM user WHERE id = 1;
```
**Expected**: Should return a coach record

**If not found**: Use correct coach_id in sample data

### 4. Database Connection Failed
**Check**: Look for error messages in console
```
Error fetching disponibilites for coach 1: ...
```

**Solution**: Verify database connection in `utils/DbConnexion.java`
```java
private static final String URL = "jdbc:postgresql://localhost:5432/pidev_db";
private static final String USER = "postgres";
private static final String PASSWORD = "admin";
```

## ✅ Complete Setup Steps

### Step 1: Verify Database Connection
```bash
# Test connection
psql -h localhost -U postgres -d pidev_db -c "SELECT 1;"
```

### Step 2: Create Table
```sql
-- Execute the migration
-- File: database/migrations/create_disponibilite_table.sql

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
    INDEX idx_statut (statut),
    UNIQUE KEY uk_coach_date_time (coach_id, date, heure_debut, heure_fin),
    CONSTRAINT chk_time_order CHECK (heure_debut < heure_fin)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### Step 3: Insert Sample Data
```sql
-- Get a valid coach ID first
SELECT id, nom FROM user LIMIT 1;

-- Then insert data (replace 1 with actual coach_id)
INSERT INTO disponibilite (coach_id, date, heure_debut, heure_fin, statut) VALUES
(1, '2026-05-10', '09:00:00', '10:00:00', 'disponible'),
(1, '2026-05-10', '10:00:00', '11:00:00', 'disponible'),
(1, '2026-05-10', '14:00:00', '15:00:00', 'disponible'),
(1, '2026-05-11', '09:00:00', '10:00:00', 'disponible'),
(1, '2026-05-11', '15:00:00', '16:00:00', 'disponible'),
(1, '2026-05-12', '10:00:00', '11:00:00', 'disponible'),
(1, '2026-05-13', '11:00:00', '12:00:00', 'disponible'),
(1, '2026-05-14', '13:00:00', '14:00:00', 'disponible');
```

### Step 4: Verify Data
```sql
-- Check data was inserted
SELECT * FROM disponibilite WHERE coach_id = 1;

-- Should return 8 rows
```

### Step 5: Test Calendar
1. Restart application
2. Click "Voir disponibilités" button
3. Calendar should now display with green highlighted dates

## 🐛 Debug Steps

### Enable Debug Logging
Add this to `CalendarCoachController.java`:

```java
@Override
public void initialize(URL location, ResourceBundle resources) {
    disponibiliteService = new DisponibiliteService();
    currentMonth = YearMonth.now();
    
    // DEBUG
    System.out.println("CalendarCoachController initialized");
    
    setupUI();
    setupEventHandlers();
}

public void setCoachInfo(int coachId, String coachName) {
    this.coachId = coachId;
    this.coachName = coachName;
    
    // DEBUG
    System.out.println("Setting coach info: " + coachId + " - " + coachName);
    
    coachNameLabel.setText("📅 Disponibilités - " + coachName);
    loadCalendar();
}

private void loadCalendar() {
    // DEBUG
    System.out.println("Loading calendar for coach: " + coachId);
    
    List<Disponibilite> slots = disponibiliteService.getAvailableSlots(coachId);
    System.out.println("Found " + slots.size() + " available slots");
    
    monthYearLabel.setText(currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
    displayCalendarDays();
}
```

### Check Console Output
Look for messages like:
```
CalendarCoachController initialized
Setting coach info: 1 - Coach Name
Loading calendar for coach: 1
Found 8 available slots
```

If you see "Found 0 available slots", the data is missing.

## 📋 Checklist

- [ ] Database connection works
- [ ] `disponibilite` table exists
- [ ] Table has data (COUNT > 0)
- [ ] Coach ID exists in `user` table
- [ ] Data has correct coach_id
- [ ] Dates are not in the past
- [ ] Status is 'disponible'
- [ ] Calendar displays with data

## 🚀 Quick Fix (Copy-Paste)

### 1. Create Table
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

### 2. Insert Data
```sql
-- First, find a valid coach ID
SELECT id FROM user LIMIT 1;

-- Then insert (replace 1 with actual coach_id)
INSERT INTO disponibilite (coach_id, date, heure_debut, heure_fin, statut) VALUES
(1, '2026-05-10', '09:00:00', '10:00:00', 'disponible'),
(1, '2026-05-10', '10:00:00', '11:00:00', 'disponible'),
(1, '2026-05-10', '14:00:00', '15:00:00', 'disponible'),
(1, '2026-05-11', '09:00:00', '10:00:00', 'disponible'),
(1, '2026-05-11', '15:00:00', '16:00:00', 'disponible'),
(1, '2026-05-12', '10:00:00', '11:00:00', 'disponible');
```

### 3. Verify
```sql
SELECT COUNT(*) FROM disponibilite;
-- Should return 6
```

### 4. Test
- Restart application
- Click "Voir disponibilités"
- Calendar should display

## 📞 Still Not Working?

1. Check console for error messages
2. Verify database connection
3. Verify table exists: `SHOW TABLES;`
4. Verify data exists: `SELECT * FROM disponibilite;`
5. Check coach_id matches: `SELECT * FROM user;`

---

**Most Common Issue**: Table exists but is empty. Just insert sample data!
