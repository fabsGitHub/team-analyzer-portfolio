/* =========================
Anonymous survey responses
=========================
Participation tokens record which member they were issued to. Responses therefore must not
reference the token, and must not carry precise timestamps that could be matched with the
token's redemption time. Only the submission date (UTC) is kept on responses and answers. */

ALTER TABLE survey_responses DROP FOREIGN KEY fk_sr_token;

ALTER TABLE survey_responses DROP INDEX uq_sr_token;

ALTER TABLE survey_responses DROP COLUMN token_id;

/* MySQL 8.0.29+ drops columns INSTANTly, leaving the old token_id bytes inside existing rows.
Rebuild the table so they are physically removed (TiDB treats this as a no-op). */
ALTER TABLE survey_responses ENGINE = InnoDB;

UPDATE survey_responses
SET
  created_at = DATE(created_at),
  updated_at = DATE(created_at);

UPDATE survey_answers
SET
  created_at = DATE(created_at),
  updated_at = DATE(created_at);
