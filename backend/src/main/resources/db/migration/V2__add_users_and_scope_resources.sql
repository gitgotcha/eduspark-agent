create table edu_user (
  id varchar(36) primary key,
  username varchar(64) not null,
  password_hash varchar(255) not null,
  created_at datetime not null
);

create unique index uk_user_username on edu_user(username);

alter table edu_task add column user_id varchar(36) null;
alter table edu_document add column user_id varchar(36) null;

create index idx_task_user_status_created on edu_task(user_id, status, created_at);
create index idx_document_user_created on edu_document(user_id, created_at);
