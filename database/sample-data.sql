-- Non-personal sample data for local demonstrations.
USE location_tracking;

INSERT INTO vehicles (identification, type, description, active)
VALUES
    ('Circular 1', 'ONIBUS', 'Veículo de demonstração 1', TRUE),
    ('Circular 2', 'ONIBUS', 'Veículo de demonstração 2', TRUE)
ON DUPLICATE KEY UPDATE active = TRUE;

INSERT INTO schedules (vehicle_id, departure_time, direction, day_type, active)
SELECT id, '07:30:00', 'CAMPUS', 'WEEKDAY', TRUE
FROM vehicles WHERE identification = 'Circular 1'
ON DUPLICATE KEY UPDATE active = TRUE;

INSERT INTO schedules (vehicle_id, departure_time, direction, day_type, active)
SELECT id, '08:00:00', 'CENTRO', 'WEEKDAY', TRUE
FROM vehicles WHERE identification = 'Circular 2'
ON DUPLICATE KEY UPDATE active = TRUE;
