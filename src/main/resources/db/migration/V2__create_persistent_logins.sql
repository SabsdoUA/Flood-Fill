CREATE TABLE IF NOT EXISTS persistent_logins (
    username VARCHAR(255) NOT NULL,
    series VARCHAR(64) PRIMARY KEY,
    token VARCHAR(64) NOT NULL,
    last_used TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_persistent_logins_username
    ON persistent_logins (username);
