alter table edu_document add column parse_status varchar(32) not null default 'PARSED';
alter table edu_document add column parse_error text null;
alter table edu_document add column indexed_at timestamp null;
alter table edu_document add column text_preview text null;

create index idx_edu_document_user_created on edu_document(user_id, created_at);
