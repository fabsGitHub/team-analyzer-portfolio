/* =========================
USERS & ROLES
========================= */
CREATE TABLE
  users (
    id BINARY(16) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    email_verified_at TIMESTAMP NULL,
    reset_token VARCHAR(100) NULL,
    reset_token_created DATETIME (3) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
  );

CREATE INDEX ix_users_reset_token ON users (reset_token);

CREATE INDEX ix_users_created_at ON users (created_at);

/* Optional – falls gewünscht zusätzlich zum UNIQUE (MySQL legt für UNIQUE intern bereits einen Index an):
CREATE INDEX ix_users_email ON users (email);
 */
CREATE TABLE
  user_roles (
    user_id BINARY(16) NOT NULL,
    role VARCHAR(64) NOT NULL,
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_user_roles UNIQUE (user_id, role)
  );

CREATE INDEX ix_user_roles_role ON user_roles (role);

/* =========================
REFRESH TOKENS
========================= */
CREATE TABLE
  refresh_tokens (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    token_hash BINARY(32) NOT NULL, -- SHA-256 (binär)
    expires_at TIMESTAMP NOT NULL,
    user_agent VARCHAR(512) NULL,
    ip VARCHAR(45) NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
  );

CREATE INDEX ix_rt_user_revoked ON refresh_tokens (user_id, revoked);

CREATE INDEX ix_rt_expires_at ON refresh_tokens (expires_at);

CREATE INDEX ix_rt_revoked_expires ON refresh_tokens (revoked, expires_at);

/* =========================
TEAMS & TEAM MEMBERS
========================= */
CREATE TABLE
  teams (
    id BINARY(16) NOT NULL,
    name VARCHAR(200) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_teams PRIMARY KEY (id)
    -- Optional: CONSTRAINT uq_team_name UNIQUE (name)
  );

CREATE TABLE
  team_members (
    team_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    leader BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_team_members PRIMARY KEY (team_id, user_id),
    CONSTRAINT fk_tm_team FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE CASCADE,
    CONSTRAINT fk_tm_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
  );

CREATE INDEX ix_tm_user ON team_members (user_id);

CREATE INDEX ix_tm_leader ON team_members (team_id, leader);

/* =========================
SURVEYS & QUESTIONS
========================= */
CREATE TABLE
  surveys (
    id BINARY(16) NOT NULL,
    team_id BINARY(16) NOT NULL,
    title VARCHAR(300) NOT NULL,
    created_by BINARY(16) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_surveys PRIMARY KEY (id),
    CONSTRAINT fk_survey_team FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE CASCADE,
    CONSTRAINT fk_survey_creator FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE RESTRICT
  );

CREATE INDEX ix_survey_team ON surveys (team_id);

CREATE INDEX ix_survey_created ON surveys (created_at);

CREATE TABLE
  survey_questions (
    id BINARY(16) NOT NULL,
    survey_id BINARY(16) NOT NULL,
    idx SMALLINT UNSIGNED NOT NULL, -- 1..n
    text VARCHAR(1000) NOT NULL,
    -- aus späteren Migrationen direkt integriert:
    created_at DATETIME (6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME (6) NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_survey_questions PRIMARY KEY (id),
    CONSTRAINT uq_sq_idx UNIQUE (survey_id, idx),
    CONSTRAINT fk_sq_survey FOREIGN KEY (survey_id) REFERENCES surveys (id) ON DELETE CASCADE
  );

/* =========================
SURVEY TOKENS
========================= */
CREATE TABLE
  survey_tokens (
    id BINARY(16) NOT NULL,
    survey_id BINARY(16) NOT NULL,
    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    issued_to_email VARCHAR(254) NULL,
    issued_to_user_id BINARY(16) NULL,
    token_hash BINARY(32) NOT NULL, -- SHA-256 (binär)
    redeemed BOOLEAN NOT NULL DEFAULT FALSE,
    redeemed_at TIMESTAMP NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at DATETIME (6) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_survey_tokens PRIMARY KEY (id),
    CONSTRAINT uq_survey_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_st_survey FOREIGN KEY (survey_id) REFERENCES surveys (id) ON DELETE CASCADE,
    CONSTRAINT fk_st_user FOREIGN KEY (issued_to_user_id) REFERENCES users (id) ON DELETE SET NULL
  );

CREATE INDEX ix_token_survey_redeemed ON survey_tokens (survey_id, redeemed);

CREATE INDEX ix_st_redeemed ON survey_tokens (redeemed);

CREATE INDEX ix_st_revoked ON survey_tokens (revoked);

CREATE INDEX ix_st_issued_at ON survey_tokens (issued_at);

CREATE INDEX ix_token_survey_user ON survey_tokens (survey_id, issued_to_user_id);

/* =========================
SURVEY RESPONSES & ANSWERS
========================= */
CREATE TABLE
  survey_responses (
    id BINARY(16) NOT NULL,
    survey_id BINARY(16) NOT NULL,
    token_id BINARY(16) NULL, -- nullable, ON DELETE SET NULL
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_survey_responses PRIMARY KEY (id),
    CONSTRAINT fk_sr_survey FOREIGN KEY (survey_id) REFERENCES surveys (id) ON DELETE CASCADE,
    CONSTRAINT fk_sr_token FOREIGN KEY (token_id) REFERENCES survey_tokens (id) ON DELETE SET NULL,
    CONSTRAINT uq_sr_token UNIQUE (token_id) -- erlaubt mehrere NULLs (MySQL), aber nur 1× gleiche Token-ID
  );

CREATE INDEX ix_sr_survey ON survey_responses (survey_id, created_at);

CREATE TABLE
  survey_answers (
    id BINARY(16) NOT NULL,
    response_id BINARY(16) NOT NULL,
    question_id BINARY(16) NOT NULL,
    value SMALLINT UNSIGNED NOT NULL,
    -- finaler Zustand direkt in CREATE:
    answer_order INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_survey_answers PRIMARY KEY (id),
    CONSTRAINT uq_response_question UNIQUE (response_id, question_id),
    CONSTRAINT uq_response_answer_order UNIQUE (response_id, answer_order),
    CONSTRAINT fk_sa_response FOREIGN KEY (response_id) REFERENCES survey_responses (id) ON DELETE CASCADE,
    CONSTRAINT fk_sa_question FOREIGN KEY (question_id) REFERENCES survey_questions (id) ON DELETE RESTRICT,
    CONSTRAINT ck_sa_value CHECK (value BETWEEN 1 AND 5)
  );

CREATE INDEX ix_sa_response ON survey_answers (response_id);

CREATE INDEX ix_sa_question ON survey_answers (question_id);

/* =========================
VIEW
========================= */
CREATE
OR REPLACE VIEW v_active_refresh_tokens AS
SELECT
  *
FROM
  refresh_tokens
WHERE
  revoked = FALSE
  AND expires_at > CURRENT_TIMESTAMP();