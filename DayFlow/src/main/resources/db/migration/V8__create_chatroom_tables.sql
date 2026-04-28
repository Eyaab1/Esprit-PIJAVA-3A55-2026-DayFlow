-- Create chatroom table
CREATE TABLE IF NOT EXISTS chatroom (
    id SERIAL PRIMARY KEY,
    goal_id INTEGER NOT NULL UNIQUE,
    state VARCHAR(50) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chatroom_goal FOREIGN KEY (goal_id) REFERENCES goal(id) ON DELETE CASCADE
);

-- Create index for better query performance
CREATE INDEX IF NOT EXISTS idx_chatroom_goal_id ON chatroom(goal_id);
CREATE INDEX IF NOT EXISTS idx_chatroom_state ON chatroom(state);

-- Create chatroom_participant table (for managing chatroom members)
CREATE TABLE IF NOT EXISTS chatroom_participant (
    id SERIAL PRIMARY KEY,
    chatroom_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'member',
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_participant_chatroom FOREIGN KEY (chatroom_id) REFERENCES chatroom(id) ON DELETE CASCADE,
    CONSTRAINT fk_participant_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE,
    CONSTRAINT uq_chatroom_user UNIQUE (chatroom_id, user_id)
);

-- Create indexes for chatroom_participant
CREATE INDEX IF NOT EXISTS idx_participant_chatroom ON chatroom_participant(chatroom_id);
CREATE INDEX IF NOT EXISTS idx_participant_user ON chatroom_participant(user_id);
CREATE INDEX IF NOT EXISTS idx_participant_role ON chatroom_participant(role);
