-- Adds created_by + last_modified_by columns to soft_deleted_records.
-- Populated via @CreatedBy + @LastModifiedBy + AuditorAware<String> bean.
ALTER TABLE soft_deleted_records ADD COLUMN IF NOT EXISTS created_by VARCHAR(64);
ALTER TABLE soft_deleted_records ADD COLUMN IF NOT EXISTS last_modified_by VARCHAR(64);
