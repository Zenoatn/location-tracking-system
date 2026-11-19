-- Read-only preflight for migration_001_operational_states.sql.
-- All three queries must return zero rows before applying the migration.
USE location_tracking;

-- One active trip per driver:
SELECT driver_id, COUNT(*) AS active_trip_count
FROM trips WHERE status = 'ACTIVE'
GROUP BY driver_id HAVING COUNT(*) > 1;

-- One active trip per vehicle:
SELECT vehicle_id, COUNT(*) AS active_trip_count
FROM trips WHERE status = 'ACTIVE'
GROUP BY vehicle_id HAVING COUNT(*) > 1;

-- Types the migration cannot safely infer. Map these using verified fleet data.
SELECT id, identification, type
FROM vehicles
WHERE type IS NULL OR UPPER(type) NOT IN
    ('BUS', 'ÔNIBUS', 'ONIBUS', 'MICROBUS', 'MICRO-ÔNIBUS', 'MICRO_ONIBUS', 'VAN');
