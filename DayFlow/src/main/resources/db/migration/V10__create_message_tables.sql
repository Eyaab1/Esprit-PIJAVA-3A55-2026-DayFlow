-- Create message table
CREATE TABLE IF NOT EXISTS message (
    id SERIAL PRIMARY KEY,
    content TEXT NOT NULL,
    chatroom_id INTEGER NOT NULL,
    author_id INTEGER NOT NULL,
    reply_to_id INTEGER DEFAULT 0,
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    is_edited BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_message_chatroom FOREIGN KEY (chatroom_id) REFERENCES chatroom(id) ON DELETE CASCADE,
    CONSTRAINT fk_message_author FOREIGN KEY (author_id) REFERENCES "user"(id) ON DELETE CASCADE
);

-- Create indexes for message table
CREATE INDEX IF NOT EXISTS idx_message_chatroom ON message(chatroom_id);
CREATE INDEX IF NOT EXISTS idx_message_author ON message(author_id);
CREATE INDEX IF NOT EXISTS idx_message_reply_to ON message(reply_to_id) WHERE reply_to_id > 0;
CREATE INDEX IF NOT EXISTS idx_message_created_at ON message(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_message_is_pinned ON message(is_pinned) WHERE is_pinned = TRUE;

-- Create reaction table
CREATE TABLE IF NOT EXISTS reaction (
    id SERIAL PRIMARY KEY,
    message_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reaction_message FOREIGN KEY (message_id) REFERENCES message(id) ON DELETE CASCADE,
    CONSTRAINT fk_reaction_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE,
    CONSTRAINT uq_reaction_message_user UNIQUE (message_id, user_id)
);

-- Create indexes for reaction table
CREATE INDEX IF NOT EXISTS idx_reaction_message ON reaction(message_id);
CREATE INDEX IF NOT EXISTS idx_reaction_user ON reaction(user_id);
CREATE INDEX IF NOT EXISTS idx_reaction_type ON reaction(type);
