-- Insert Sample Disponibilite Data for Testing
-- This script inserts sample availability data for coaches

-- First, verify the table exists
-- SELECT COUNT(*) FROM disponibilite;

-- Insert sample data for coach with ID 1
-- Make sure coach with ID 1 exists in user table first
-- SELECT * FROM user WHERE id = 1;

INSERT INTO disponibilite (coach_id, date, heure_debut, heure_fin, statut) VALUES
-- May 10, 2026 (Saturday)
(1, '2026-05-10', '09:00:00', '10:00:00', 'disponible'),
(1, '2026-05-10', '10:00:00', '11:00:00', 'disponible'),
(1, '2026-05-10', '14:00:00', '15:00:00', 'disponible'),
(1, '2026-05-10', '15:00:00', '16:00:00', 'disponible'),

-- May 11, 2026 (Sunday)
(1, '2026-05-11', '09:00:00', '10:00:00', 'disponible'),
(1, '2026-05-11', '10:00:00', '11:00:00', 'disponible'),
(1, '2026-05-11', '15:00:00', '16:00:00', 'disponible'),

-- May 12, 2026 (Monday)
(1, '2026-05-12', '10:00:00', '11:00:00', 'disponible'),
(1, '2026-05-12', '11:00:00', '12:00:00', 'disponible'),
(1, '2026-05-12', '14:00:00', '15:00:00', 'disponible'),

-- May 13, 2026 (Tuesday)
(1, '2026-05-13', '09:00:00', '10:00:00', 'disponible'),
(1, '2026-05-13', '13:00:00', '14:00:00', 'disponible'),
(1, '2026-05-13', '15:00:00', '16:00:00', 'disponible'),

-- May 14, 2026 (Wednesday)
(1, '2026-05-14', '10:00:00', '11:00:00', 'disponible'),
(1, '2026-05-14', '11:00:00', '12:00:00', 'disponible'),
(1, '2026-05-14', '14:00:00', '15:00:00', 'disponible'),

-- May 15, 2026 (Thursday)
(1, '2026-05-15', '09:00:00', '10:00:00', 'disponible'),
(1, '2026-05-15', '10:00:00', '11:00:00', 'disponible'),
(1, '2026-05-15', '15:00:00', '16:00:00', 'disponible'),

-- May 16, 2026 (Friday)
(1, '2026-05-16', '11:00:00', '12:00:00', 'disponible'),
(1, '2026-05-16', '13:00:00', '14:00:00', 'disponible'),
(1, '2026-05-16', '14:00:00', '15:00:00', 'disponible');

-- Verify data was inserted
-- SELECT COUNT(*) FROM disponibilite;
-- SELECT * FROM disponibilite WHERE coach_id = 1 ORDER BY date, heure_debut;
