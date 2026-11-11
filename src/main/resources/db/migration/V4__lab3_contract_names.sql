-- Deploy only after all application instances use first_name/last_name.
DROP TRIGGER customer_names ON customers;
DROP FUNCTION sync_customer_names();
ALTER TABLE customers ALTER COLUMN first_name SET NOT NULL, ALTER COLUMN last_name SET NOT NULL;
ALTER TABLE customers DROP COLUMN full_name;
