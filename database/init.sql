-- Run from the project root in the MySQL client: SOURCE database/init.sql;
-- For a fresh database. Existing databases use the documented optional migrations.
SET NAMES utf8mb4;
SOURCE database/initial-schema.sql;
SOURCE database/create-prescription-tables.sql;
SOURCE database/create-medicine-inbound-tables.sql;
SOURCE database/create-medical-order.sql;
SOURCE database/create-medical-record.sql;
SOURCE database/migrations/upgrade-medicine-safe.sql;
-- Start the staff portal next; it applies the remaining business migrations.
