-- Users
CREATE TABLE users (
    id              UUID PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    name            VARCHAR(255),
    avatar_url      VARCHAR(512),
    coins           INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Materials (uploaded files)
CREATE TABLE materials (
    id               UUID PRIMARY KEY,
    user_id          UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title            VARCHAR(255) NOT NULL,
    subject          VARCHAR(255),
    course_code      VARCHAR(64),
    tags             VARCHAR(1024), -- comma-separated for now
    storage_key      VARCHAR(512) NOT NULL,
    file_size        BIGINT,
    mime_type        VARCHAR(128),
    text_extract     TEXT,
    summary          TEXT,
    flashcards_json  TEXT,
    avg_rating       DOUBLE PRECISION NOT NULL DEFAULT 0,
    ratings_count    INT NOT NULL DEFAULT 0,
    downloads_count  INT NOT NULL DEFAULT 0,
    status           VARCHAR(32) NOT NULL,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_materials_user_id ON materials(user_id);
CREATE INDEX idx_materials_title ON materials(title);

-- Reviews
CREATE TABLE reviews (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    material_id UUID NOT NULL REFERENCES materials(id) ON DELETE CASCADE,
    rating      INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment     TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, material_id)
);
CREATE INDEX idx_reviews_material_id ON reviews(material_id);

-- Coin transactions
CREATE TABLE coin_transactions (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    delta       INT NOT NULL,
    reason      VARCHAR(64) NOT NULL,
    ref_id      UUID,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_coin_tx_user_id ON coin_transactions(user_id);

-- AI jobs
CREATE TABLE ai_jobs (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    material_id UUID NOT NULL REFERENCES materials(id) ON DELETE CASCADE,
    type        VARCHAR(32) NOT NULL,
    status      VARCHAR(32) NOT NULL,
    error       TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_ai_jobs_status ON ai_jobs(status);


