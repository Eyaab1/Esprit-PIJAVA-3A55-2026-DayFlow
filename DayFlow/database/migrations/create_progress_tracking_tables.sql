-- Migration Progress Tracking & Evaluation (PostgreSQL)

CREATE TABLE IF NOT EXISTS session_feedback (
    id SERIAL PRIMARY KEY,
    session_id INTEGER NOT NULL UNIQUE,
    coaching_request_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    coach_id INTEGER NOT NULL,
    coach_rating INTEGER CHECK (coach_rating BETWEEN 1 AND 5),
    user_feedback TEXT,
    user_comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_feedback_session FOREIGN KEY (session_id) REFERENCES session(id) ON DELETE CASCADE,
    CONSTRAINT fk_feedback_request FOREIGN KEY (coaching_request_id) REFERENCES coaching_request(id) ON DELETE CASCADE,
    CONSTRAINT fk_feedback_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE,
    CONSTRAINT fk_feedback_coach FOREIGN KEY (coach_id) REFERENCES "user"(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS session_evaluation (
    id SERIAL PRIMARY KEY,
    session_id INTEGER NOT NULL UNIQUE,
    coaching_request_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    coach_id INTEGER NOT NULL,
    progress_delta INTEGER CHECK (progress_delta BETWEEN -30 AND 30),
    discipline_score INTEGER CHECK (discipline_score BETWEEN 0 AND 100),
    goal_achievement_score INTEGER CHECK (goal_achievement_score BETWEEN 0 AND 100),
    evolution_score INTEGER CHECK (evolution_score BETWEEN 0 AND 100),
    coach_feedback_score INTEGER CHECK (coach_feedback_score BETWEEN 0 AND 100),
    coach_remarks TEXT,
    recommendations TEXT,
    next_action TEXT,
    program_adjustment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_eval_session FOREIGN KEY (session_id) REFERENCES session(id) ON DELETE CASCADE,
    CONSTRAINT fk_eval_request FOREIGN KEY (coaching_request_id) REFERENCES coaching_request(id) ON DELETE CASCADE,
    CONSTRAINT fk_eval_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE,
    CONSTRAINT fk_eval_coach FOREIGN KEY (coach_id) REFERENCES "user"(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS progress_tracking (
    id SERIAL PRIMARY KEY,
    coaching_request_id INTEGER NOT NULL UNIQUE,
    user_id INTEGER NOT NULL,
    coach_id INTEGER NOT NULL,
    current_score INTEGER NOT NULL DEFAULT 0 CHECK (current_score BETWEEN 0 AND 100),
    previous_score INTEGER NOT NULL DEFAULT 0 CHECK (previous_score BETWEEN 0 AND 100),
    score_change INTEGER NOT NULL DEFAULT 0,
    last_session_id INTEGER,
    last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_progress_request FOREIGN KEY (coaching_request_id) REFERENCES coaching_request(id) ON DELETE CASCADE,
    CONSTRAINT fk_progress_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE,
    CONSTRAINT fk_progress_coach FOREIGN KEY (coach_id) REFERENCES "user"(id) ON DELETE CASCADE,
    CONSTRAINT fk_progress_last_session FOREIGN KEY (last_session_id) REFERENCES session(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS progress_history (
    id SERIAL PRIMARY KEY,
    progress_tracking_id INTEGER NOT NULL,
    session_id INTEGER NOT NULL UNIQUE,
    score_before INTEGER NOT NULL CHECK (score_before BETWEEN 0 AND 100),
    score_after INTEGER NOT NULL CHECK (score_after BETWEEN 0 AND 100),
    score_change INTEGER NOT NULL,
    summary TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_progress_history_tracking FOREIGN KEY (progress_tracking_id) REFERENCES progress_tracking(id) ON DELETE CASCADE,
    CONSTRAINT fk_progress_history_session FOREIGN KEY (session_id) REFERENCES session(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_feedback_session ON session_feedback(session_id);
CREATE INDEX IF NOT EXISTS idx_eval_session ON session_evaluation(session_id);
CREATE INDEX IF NOT EXISTS idx_progress_request ON progress_tracking(coaching_request_id);
CREATE INDEX IF NOT EXISTS idx_progress_history_session ON progress_history(session_id);
