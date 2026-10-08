CREATE TABLE course_bookmarks (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    course_code VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE (user_id, course_code)
);
