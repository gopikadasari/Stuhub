-- Questions table for AskHub
CREATE TABLE questions (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title           VARCHAR(500) NOT NULL,
    description     TEXT NOT NULL,
    course_code     VARCHAR(64),
    subject         VARCHAR(255),
    tags            VARCHAR(1024), -- comma-separated
    image_url       VARCHAR(512), -- URL to uploaded image
    storage_key     VARCHAR(512), -- Storage key for the question image
    status          VARCHAR(32) NOT NULL DEFAULT 'OPEN', -- OPEN, CLOSED, RESOLVED
    answers_count   INT NOT NULL DEFAULT 0,
    views_count     INT NOT NULL DEFAULT 0,
    priority        VARCHAR(32) DEFAULT 'NORMAL', -- LOW, NORMAL, HIGH, URGENT
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_questions_user_id ON questions(user_id);
CREATE INDEX idx_questions_course_code ON questions(course_code);
CREATE INDEX idx_questions_subject ON questions(subject);
CREATE INDEX idx_questions_status ON questions(status);
CREATE INDEX idx_questions_priority ON questions(priority);
CREATE INDEX idx_questions_created_at ON questions(created_at);

-- Answers table for AskHub
CREATE TABLE answers (
    id              UUID PRIMARY KEY,
    question_id     UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content         TEXT NOT NULL,
    image_url       VARCHAR(512), -- URL to uploaded image
    storage_key     VARCHAR(512), -- Storage key for the answer image
    is_accepted     BOOLEAN NOT NULL DEFAULT FALSE,
    votes_up        INT NOT NULL DEFAULT 0,
    votes_down      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_answers_question_id ON answers(question_id);
CREATE INDEX idx_answers_user_id ON answers(user_id);
CREATE INDEX idx_answers_is_accepted ON answers(is_accepted);
CREATE INDEX idx_answers_votes_up ON answers(votes_up);
CREATE INDEX idx_answers_created_at ON answers(created_at);
