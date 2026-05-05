-- Create Disponibilite (Coach Availability) Table
-- This table stores available time slots for coaches
-- PostgreSQL version

CREATE TABLE IF NOT EXISTS disponibilite (
    id SERIAL PRIMARY KEY,
    coach_id INT NOT NULL,
    date DATE NOT NULL,
    heure_debut TIME NOT NULL,
    heure_fin TIME NOT NULL,
    statut VARCHAR(50) NOT NULL DEFAULT 'disponible',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraint
    CONSTRAINT fk_disponibilite_coach FOREIGN KEY (coach_id) REFERENCES "user"(id) ON DELETE CASCADE,
    
    -- Unique constraint to prevent duplicate slots
    CONSTRAINT uk_coach_date_time UNIQUE (coach_id, date, heure_debut, heure_fin),
    
    -- Check constraint to ensure start time is before end time
    CONSTRAINT chk_time_order CHECK (heure_debut < heure_fin)
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_coach_id ON disponibilite(coach_id);
CREATE INDEX IF NOT EXISTS idx_date ON disponibilite(date);
CREATE INDEX IF NOT EXISTS idx_coach_date ON disponibilite(coach_id, date);
CREATE INDEX IF NOT EXISTS idx_statut ON disponibilite(statut);
CREATE INDEX IF NOT EXISTS idx_coach_statut ON disponibilite(coach_id, statut);
CREATE INDEX IF NOT EXISTS idx_disponible_slots ON disponibilite(coach_id, date, statut) WHERE statut = 'disponible';

-- Add table comment
COMMENT ON TABLE disponibilite IS 'Stores available time slots for coaches';

-- Add column comments
COMMENT ON COLUMN disponibilite.id IS 'Unique identifier';
COMMENT ON COLUMN disponibilite.coach_id IS 'Reference to coach (user)';
COMMENT ON COLUMN disponibilite.date IS 'Date of availability';
COMMENT ON COLUMN disponibilite.heure_debut IS 'Start time of availability';
COMMENT ON COLUMN disponibilite.heure_fin IS 'End time of availability';
COMMENT ON COLUMN disponibilite.statut IS 'Status: disponible, reserve, annulea';
COMMENT ON COLUMN disponibilite.created_at IS 'Creation timestamp';
COMMENT ON COLUMN disponibilite.updated_at IS 'Last update timestamp';
