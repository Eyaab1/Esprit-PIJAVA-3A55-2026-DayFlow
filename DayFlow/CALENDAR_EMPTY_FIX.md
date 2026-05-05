# ✅ Calendar Empty - Quick Fix

## Problem
Calendar opens but shows nothing (no dates, no time slots)

## Root Cause
**Missing data in database** - The `disponibilite` table is empty or doesn't exist

## Solution (5 minutes)

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
    UNIQUE KEY uk_coach_date_time (coach_id, date, heure_debut, heure_fin),
    CONSTRAINT chk_time_order CHECK (heure_debut < heure_fin)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 2. Insert Data
```sql
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
-- Should return: 6
```

### 4. Restart App
- Close application
- Restart application
- Click "Voir disponibilités"
- **Calendar should now display data!**

## ✅ Expected Result

Calendar will show:
- ✅ May 2026 calendar
- ✅ Green highlighted dates (10, 11, 12)
- ✅ Time slots when you click a date
- ✅ Ability to select and reserve

## 🎯 That's It!

The calendar was working all along - it just needed data!

---

**Most Common Issue**: Empty database table
**Solution**: Insert sample data above
**Time to Fix**: 2 minutes
