-- Override Game Database Schema
-- For MySQL / PostgreSQL. H2 uses JPA auto-DDL instead.

CREATE TABLE IF NOT EXISTS users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL UNIQUE,
    email           VARCHAR(120) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS player_profiles (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT       NOT NULL UNIQUE,
    display_name      VARCHAR(50)  DEFAULT 'Ayan',
    level             INT          DEFAULT 1,
    xp                INT          DEFAULT 0,
    logic_stat        INT          DEFAULT 5,
    awareness_stat    INT          DEFAULT 5,
    willpower_stat    INT          DEFAULT 5,
    combat_stat       INT          DEFAULT 5,
    empathy_stat      INT          DEFAULT 5,
    dependency_meter  INT          DEFAULT 0,
    current_chapter   INT          DEFAULT 1,
    coins             INT          DEFAULT 100,
    independent_xp    INT          DEFAULT 0,
    created_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS game_saves (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id           BIGINT       NOT NULL,
    chapter_number      INT,
    checkpoint          VARCHAR(100),
    player_hp           INT          DEFAULT 100,
    dependency_meter    INT          DEFAULT 0,
    coins               INT          DEFAULT 100,
    choices_json        TEXT,
    unlocked_skills_json TEXT,
    timestamp           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (player_id) REFERENCES player_profiles(id)
);

CREATE TABLE IF NOT EXISTS chapter_progress (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id          BIGINT  NOT NULL,
    chapter_number     INT     NOT NULL,
    completed          BOOLEAN DEFAULT FALSE,
    score              INT     DEFAULT 0,
    time_spent_seconds BIGINT  DEFAULT 0,
    ai_help_used       INT     DEFAULT 0,
    FOREIGN KEY (player_id) REFERENCES player_profiles(id),
    UNIQUE (player_id, chapter_number)
);

CREATE TABLE IF NOT EXISTS achievements (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL UNIQUE,
    title       VARCHAR(100) NOT NULL,
    description VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS leaderboard_entries (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id               BIGINT      NOT NULL,
    score                   INT,
    ending_type             VARCHAR(30),
    completion_time_seconds BIGINT,
    created_at              TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (player_id) REFERENCES player_profiles(id)
);

-- One row per mini-game (e.g. 'kernel-panic'); tracks the global best run.
CREATE TABLE IF NOT EXISTS high_scores (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    game_type             VARCHAR(50) NOT NULL UNIQUE,
    best_score            INT     DEFAULT 0,
    best_combo            INT     DEFAULT 0,
    best_wave             INT     DEFAULT 0,
    best_run_was_assisted BOOLEAN DEFAULT FALSE,
    achieved_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Single-row global best for Chapter 1's "Syntax Snake" mini-game.
CREATE TABLE IF NOT EXISTS snake_high_scores (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    best_score     INT     NOT NULL DEFAULT 0,
    best_run_date  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seed achievements
INSERT INTO achievements (code, title, description) VALUES
    ('FIRST_PUZZLE',    'First Steps',          'Solved your first puzzle without AI help'),
    ('CH1_COMPLETE',    'Silent No More',       'Completed Chapter 1: The Silent Classroom'),
    ('CH2_COMPLETE',    'Harvest Freedom',      'Completed Chapter 2: Harvest Protocol'),
    ('CH3_COMPLETE',    'Mercy Restored',       'Completed Chapter 3: Mercy Index'),
    ('CH4_COMPLETE',    'Eyes Open',            'Completed Chapter 4: Codeblind'),
    ('ALL_CHAPTERS',    'Override Complete',     'Completed all chapters and the final mission'),
    ('ZERO_DEPENDENCY', 'True Independence',    'Finished the game with 0 dependency'),
    ('SYMBIOSIS',       'Best of Both Worlds',  'Achieved the Symbiosis ending'),
    ('NO_AI_HELP',      'Human After All',      'Never used Astra help in any chapter'),
    ('SPEED_RUN',       'Quick Thinker',        'Completed the game in under 60 minutes');
