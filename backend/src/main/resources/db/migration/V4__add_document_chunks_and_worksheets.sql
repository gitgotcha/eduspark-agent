create table edu_document_chunk (
  id varchar(36) primary key,
  user_id varchar(36) not null,
  document_id varchar(36) not null,
  chunk_index int not null,
  content longtext not null,
  embedding_json longtext not null,
  token_count int null,
  created_at datetime not null
);

create index idx_chunk_user_document on edu_document_chunk(user_id, document_id, chunk_index);
create index idx_chunk_user_created on edu_document_chunk(user_id, created_at);

create table edu_worksheet (
  id varchar(36) primary key,
  user_id varchar(36) not null,
  task_id varchar(36) null,
  title varchar(255) not null,
  document_ids longtext not null,
  config_json longtext not null,
  questions_json longtext null,
  status varchar(32) not null,
  failure_reason text null,
  created_at datetime not null,
  updated_at datetime not null
);

create index idx_worksheet_user_created on edu_worksheet(user_id, created_at);

create table edu_worksheet_export (
  id varchar(36) primary key,
  user_id varchar(36) not null,
  worksheet_id varchar(36) not null,
  file_name varchar(255) not null,
  storage_path varchar(512) not null,
  created_at datetime not null
);

create index idx_export_user_worksheet on edu_worksheet_export(user_id, worksheet_id);
