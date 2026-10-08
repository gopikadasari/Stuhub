CREATE TABLE material_bookmarks (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    material_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (material_id) REFERENCES materials(id),
    UNIQUE (user_id, material_id)
);
