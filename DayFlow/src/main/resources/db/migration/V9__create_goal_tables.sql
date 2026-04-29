-- Create goal table
CREATE TABLE IF NOT EXISTS goal (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    deadline DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'draft',
    priority VARCHAR(50),
    is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
    progress INTEGER NOT NULL DEFAULT 0,
    required_tasks INTEGER,
    trello_board_id VARCHAR(255),
    user_id INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_goal_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
);

-- Add constraints after table creation (NOT VALID to skip existing data)
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_goal_status') THEN
        ALTER TABLE goal ADD CONSTRAINT chk_goal_status CHECK (status IN ('draft', 'active', 'paused', 'completed', 'failed', 'archived')) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_goal_priority') THEN
        ALTER TABLE goal ADD CONSTRAINT chk_goal_priority CHECK (priority IS NULL OR priority IN ('low', 'medium', 'high')) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_goal_progress') THEN
        ALTER TABLE goal ADD CONSTRAINT chk_goal_progress CHECK (progress >= 0 AND progress <= 100) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_goal_dates') THEN
        ALTER TABLE goal ADD CONSTRAINT chk_goal_dates CHECK (end_date > start_date) NOT VALID;
    END IF;
END $$;

-- Create indexes for goal table
CREATE INDEX IF NOT EXISTS idx_goal_user_id ON goal(user_id);
CREATE INDEX IF NOT EXISTS idx_goal_status ON goal(status);
CREATE INDEX IF NOT EXISTS idx_goal_priority ON goal(priority);
CREATE INDEX IF NOT EXISTS idx_goal_start_date ON goal(start_date);
CREATE INDEX IF NOT EXISTS idx_goal_end_date ON goal(end_date);
CREATE INDEX IF NOT EXISTS idx_goal_is_favorite ON goal(is_favorite) WHERE is_favorite = TRUE;

-- Create goal_participation table
CREATE TABLE IF NOT EXISTS goal_participation (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    goal_id INTEGER NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'member',
    status VARCHAR(50) NOT NULL DEFAULT 'pending',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_goal_participation_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE,
    CONSTRAINT fk_goal_participation_goal FOREIGN KEY (goal_id) REFERENCES goal(id) ON DELETE CASCADE,
    CONSTRAINT uq_goal_participation UNIQUE (user_id, goal_id)
);

-- Add constraints after table creation (NOT VALID to skip existing data)
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_participation_role') THEN
        ALTER TABLE goal_participation ADD CONSTRAINT chk_participation_role CHECK (role IN ('owner', 'admin', 'member')) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_participation_status') THEN
        ALTER TABLE goal_participation ADD CONSTRAINT chk_participation_status CHECK (status IN ('pending', 'accepted', 'rejected')) NOT VALID;
    END IF;
END $$;

-- Create indexes for goal_participation
CREATE INDEX IF NOT EXISTS idx_goal_participation_user ON goal_participation(user_id);
CREATE INDEX IF NOT EXISTS idx_goal_participation_goal ON goal_participation(goal_id);
CREATE INDEX IF NOT EXISTS idx_goal_participation_role ON goal_participation(role);
CREATE INDEX IF NOT EXISTS idx_goal_participation_status ON goal_participation(status);

-- Create routine table
CREATE TABLE IF NOT EXISTS routine (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    frequency VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'active',
    goal_id INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_routine_goal FOREIGN KEY (goal_id) REFERENCES goal(id) ON DELETE CASCADE
);

-- Add constraints after table creation (NOT VALID to skip existing data)
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_routine_status') THEN
        ALTER TABLE routine ADD CONSTRAINT chk_routine_status CHECK (status IN ('active', 'paused', 'completed', 'cancelled')) NOT VALID;
    END IF;
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'routine' AND column_name = 'frequency'
    )
    AND NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_routine_frequency') THEN
        ALTER TABLE routine ADD CONSTRAINT chk_routine_frequency CHECK (frequency IN ('daily', 'weekly', 'monthly', 'custom')) NOT VALID;
    END IF;
END $$;

-- Create indexes for routine
CREATE INDEX IF NOT EXISTS idx_routine_goal ON routine(goal_id);
CREATE INDEX IF NOT EXISTS idx_routine_status ON routine(status);
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'routine' AND column_name = 'frequency'
    ) THEN
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_routine_frequency ON routine(frequency)';
    END IF;
END $$;

-- Create activity table
CREATE TABLE IF NOT EXISTS activity (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    scheduled_date DATE NOT NULL,
    completed_date DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'pending',
    routine_id INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_activity_routine FOREIGN KEY (routine_id) REFERENCES routine(id) ON DELETE CASCADE
);

-- Add constraint after table creation (NOT VALID to skip existing data)
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_activity_status') THEN
        ALTER TABLE activity ADD CONSTRAINT chk_activity_status CHECK (status IN ('pending', 'in_progress', 'completed', 'skipped', 'cancelled')) NOT VALID;
    END IF;
END $$;

-- Create indexes for activity
CREATE INDEX IF NOT EXISTS idx_activity_routine ON activity(routine_id);
CREATE INDEX IF NOT EXISTS idx_activity_status ON activity(status);
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'activity' AND column_name = 'scheduled_date'
    ) THEN
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_activity_scheduled_date ON activity(scheduled_date)';
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'activity' AND column_name = 'completed_date'
    ) THEN
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_activity_completed_date ON activity(completed_date)';
    END IF;
END $$;
