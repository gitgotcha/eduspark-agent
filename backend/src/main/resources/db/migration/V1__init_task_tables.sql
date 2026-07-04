create table edu_task (
  id varchar(36) primary key,
  user_instruction text not null,
  status varchar(32) not null,
  plan_json longtext null,
  final_answer longtext null,
  failure_reason text null,
  retry_count int not null default 0,
  created_at datetime not null,
  updated_at datetime not null
);

create index idx_task_status_created on edu_task(status, created_at);

create table edu_task_log (
  id varchar(36) primary key,
  task_id varchar(36) not null,
  stage varchar(64) not null,
  message text not null,
  payload_json longtext null,
  created_at datetime not null,
  constraint fk_task_log_task foreign key (task_id) references edu_task(id)
);

create index idx_log_task_created on edu_task_log(task_id, created_at);

create table edu_document (
  id varchar(36) primary key,
  task_id varchar(36) null,
  file_name varchar(255) not null,
  mime_type varchar(128) not null,
  storage_path varchar(512) not null,
  extracted_text longtext null,
  created_at datetime not null,
  constraint fk_document_task foreign key (task_id) references edu_task(id)
);

create table edu_task_artifact (
  id varchar(36) primary key,
  task_id varchar(36) not null,
  artifact_type varchar(64) not null,
  content_json longtext not null,
  created_at datetime not null,
  constraint fk_artifact_task foreign key (task_id) references edu_task(id)
);
