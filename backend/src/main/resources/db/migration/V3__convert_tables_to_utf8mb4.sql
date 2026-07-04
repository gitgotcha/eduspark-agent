alter table edu_task_log drop foreign key fk_task_log_task;
alter table edu_document drop foreign key fk_document_task;
alter table edu_task_artifact drop foreign key fk_artifact_task;

alter table edu_user convert to character set utf8mb4 collate utf8mb4_unicode_ci;
alter table edu_task convert to character set utf8mb4 collate utf8mb4_unicode_ci;
alter table edu_task_log convert to character set utf8mb4 collate utf8mb4_unicode_ci;
alter table edu_document convert to character set utf8mb4 collate utf8mb4_unicode_ci;
alter table edu_task_artifact convert to character set utf8mb4 collate utf8mb4_unicode_ci;

alter table edu_task_log add constraint fk_task_log_task foreign key (task_id) references edu_task(id);
alter table edu_document add constraint fk_document_task foreign key (task_id) references edu_task(id);
alter table edu_task_artifact add constraint fk_artifact_task foreign key (task_id) references edu_task(id);
