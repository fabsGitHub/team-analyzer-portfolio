/* =========================
Deleting surveys and teams
=========================
survey_answers.question_id was ON DELETE RESTRICT, so deleting a team or survey that already had
answers failed (MySQL error 1451): the admin "delete team" endpoint returned 500, and any cleanup
of teams with responses (e.g. expired demo workspaces) stopped. Answers belong to their response
and question, so they are removed with them. */

ALTER TABLE survey_answers DROP FOREIGN KEY fk_sa_question;

ALTER TABLE survey_answers
ADD CONSTRAINT fk_sa_question FOREIGN KEY (question_id) REFERENCES survey_questions (id) ON DELETE CASCADE;
