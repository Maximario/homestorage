CREATE TABLE items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL CHECK (category IN ('CLOTHES', 'TOOLS', 'BOOKS', 'DOCUMENTS', 'ELECTRONICS', 'FOOD', 'MEDICINES', 'SPORTS', 'OTHER')),
    description TEXT,
    container_id UUID NOT NULL REFERENCES containers(id) ON DELETE CASCADE,
    quantity INTEGER DEFAULT 1,
    photo_url TEXT,
    photo_thumbnail_url TEXT,
    reminder_date DATE,
    reminder_note TEXT,
    reminder_completed BOOLEAN DEFAULT FALSE,
    reminder_completed_at TIMESTAMP,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_items_user_id ON items(user_id);
CREATE INDEX idx_items_container_id ON items(container_id);
CREATE INDEX idx_items_name ON items(name);
CREATE INDEX idx_items_category ON items(category);
CREATE INDEX idx_items_reminder_date ON items(reminder_date) WHERE reminder_completed = FALSE;