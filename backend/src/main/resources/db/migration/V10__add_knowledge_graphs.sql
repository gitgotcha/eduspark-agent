create table edu_knowledge_graph (
  id varchar(64) primary key,
  user_id varchar(64) not null,
  task_id varchar(64) null,
  title varchar(255) not null,
  document_ids_json longtext not null,
  graph_json longtext not null,
  status varchar(32) not null,
  created_at datetime not null,
  updated_at datetime not null,
  index idx_graph_user_created (user_id, created_at),
  index idx_graph_user_status (user_id, status),
  index idx_graph_user_task (user_id, task_id)
);
