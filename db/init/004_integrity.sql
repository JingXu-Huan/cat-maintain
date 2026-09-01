SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = DATABASE() AND table_name = 'maintenance_records'
       AND index_name = 'uk_maintenance_records_appointment') = 0,
    'ALTER TABLE maintenance_records ADD UNIQUE KEY uk_maintenance_records_appointment (appointment_id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
