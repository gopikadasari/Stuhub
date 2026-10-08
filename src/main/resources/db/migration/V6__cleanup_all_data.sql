-- Cleanup script to clear all data while keeping table structure
-- This will give a fresh start while maintaining all functionality

-- Clear all data tables (in dependency order to avoid foreign key issues)
TRUNCATE TABLE answers CASCADE;
TRUNCATE TABLE questions CASCADE;
TRUNCATE TABLE reviews CASCADE;
TRUNCATE TABLE material_bookmarks CASCADE;
TRUNCATE TABLE course_bookmarks CASCADE;
TRUNCATE TABLE ai_jobs CASCADE;
TRUNCATE TABLE coin_transactions CASCADE;
TRUNCATE TABLE materials CASCADE;

-- Reset any sequences if they exist
-- PostgreSQL will automatically reset sequences when truncating

-- Update user statistics to 0 (only if columns exist)
DO $$
BEGIN
    -- Check if columns exist before updating
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'coins') THEN
        UPDATE users SET coins = 0;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'uploads_count') THEN
        UPDATE users SET uploads_count = 0;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'reviews_count') THEN
        UPDATE users SET reviews_count = 0;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'questions_count') THEN
        UPDATE users SET questions_count = 0;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'answers_count') THEN
        UPDATE users SET answers_count = 0;
    END IF;
END $$;
