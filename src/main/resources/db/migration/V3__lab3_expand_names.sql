ALTER TABLE customers ADD COLUMN first_name VARCHAR(255), ADD COLUMN last_name VARCHAR(255);
CREATE FUNCTION sync_customer_names() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP = 'INSERT' THEN
  IF NEW.full_name IS NOT NULL THEN
   NEW.first_name := split_part(NEW.full_name, ' ', 1);
   NEW.last_name := CASE WHEN strpos(NEW.full_name, ' ') > 0
    THEN substr(NEW.full_name, strpos(NEW.full_name, ' ') + 1) ELSE '' END;
  ELSE
   NEW.full_name := concat_ws(' ', NEW.first_name, nullif(NEW.last_name, ''));
  END IF;
 ELSIF NEW.full_name IS DISTINCT FROM OLD.full_name THEN
  NEW.first_name := split_part(NEW.full_name, ' ', 1);
  NEW.last_name := CASE WHEN strpos(NEW.full_name, ' ') > 0
   THEN substr(NEW.full_name, strpos(NEW.full_name, ' ') + 1) ELSE '' END;
 ELSE
  NEW.full_name := concat_ws(' ', NEW.first_name, nullif(NEW.last_name, ''));
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER customer_names BEFORE INSERT OR UPDATE ON customers
 FOR EACH ROW EXECUTE FUNCTION sync_customer_names();
UPDATE customers SET first_name=split_part(full_name, ' ', 1),
 last_name=CASE WHEN strpos(full_name, ' ') > 0
 THEN substr(full_name, strpos(full_name, ' ') + 1) ELSE '' END;
