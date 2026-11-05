/* =========================
LEADER role backfill
=========================
Teams created through the admin API before this version did not give their leader the LEADER
role, which the UI needs to unlock leader pages. Grant it to everyone who leads a team. */

INSERT IGNORE INTO user_roles (user_id, role)
SELECT DISTINCT
  user_id,
  'LEADER'
FROM
  team_members
WHERE
  leader = TRUE;
