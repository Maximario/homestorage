CREATE TABLE containers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    group_id UUID NULL REFERENCES groups(id) ON DELETE CASCADE,
    access_level VARCHAR(50) NOT NULL CHECK (access_level IN ('PRIVATE', 'GROUP_READ', 'GROUP_WRITE')) DEFAULT 'PRIVATE',
    name VARCHAR(255) NOT NULL,
    description TEXT,
    parent_id UUID REFERENCES containers(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL CHECK (type IN ('BUILDING', 'ROOM', 'FURNITURE', 'SHELF', 'BOX', 'DRAWER')),
    qr_code TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_containers_user_id ON containers(user_id);
CREATE INDEX idx_containers_group_id ON containers(group_id);
CREATE INDEX idx_containers_parent_id ON containers(parent_id);
CREATE INDEX idx_containers_type ON containers(type);

COMMENT ON COLUMN containers.type IS
'Тип контейнера:
BUILDING (здание) → ROOM
ROOM (комната) → FURNITURE, SHELF, BOX, DRAWER
FURNITURE (мебель) → SHELF, BOX, DRAWER
SHELF (полка) → BOX, DRAWER
BOX (коробка) → НИЧЕГО (только вещи)
DRAWER (ящик) → BOX';