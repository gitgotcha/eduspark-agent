create table edu_wrong_question (
  id varchar(64) primary key,
  user_id varchar(64) not null,
  worksheet_id varchar(64) not null,
  attempt_id varchar(64) not null,
  question_id varchar(128) not null,
  question_json longtext not null,
  submitted_answer text null,
  correct_answer text null,
  explanation text null,
  weakness_tag varchar(128) not null,
  retry_worksheet_id varchar(64) null,
  resolved tinyint not null default 0,
  created_at datetime not null,
  updated_at datetime not null,
  index idx_wrong_user_created (user_id, created_at),
  index idx_wrong_user_resolved (user_id, resolved),
  index idx_wrong_worksheet (user_id, worksheet_id)
);
