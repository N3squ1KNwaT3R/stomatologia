ALTER TABLE patients
ALTER COLUMN created_at TYPE TIMESTAMPTZ
USING created_at AT TIME ZONE 'Europe/Warsaw';