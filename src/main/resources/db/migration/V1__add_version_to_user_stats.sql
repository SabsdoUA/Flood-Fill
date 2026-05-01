DO $$
BEGIN
    IF to_regclass('public.user_stats') IS NOT NULL THEN
        ALTER TABLE user_stats
            ADD COLUMN IF NOT EXISTS version BIGINT;

        UPDATE user_stats
        SET version = 0
        WHERE version IS NULL;

        ALTER TABLE user_stats
            ALTER COLUMN version SET DEFAULT 0;

        ALTER TABLE user_stats
            ALTER COLUMN version SET NOT NULL;
    END IF;
END $$;
