create table edu_worksheet_attempt (
  id varchar(64) primary key,
  user_id varchar(64) not null,
  worksheet_id varchar(64) not null,
  answers_json longtext not null,
  grading_result_json longtext not null,
  score decimal(5,2) not null,
  created_at timestamp not null,
  index idx_attempt_user_worksheet (user_id, worksheet_id),
  index idx_attempt_user_created (user_id, created_at)
);
