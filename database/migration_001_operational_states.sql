-- One-time migration from the original project schema to operational states.
-- Back up the database before applying. Legacy schedule directions are retained.
USE location_tracking;

ALTER TABLE users
    ADD COLUMN current_status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    ADD COLUMN current_vehicle_id BIGINT NULL,
    ADD COLUMN waiting_location VARCHAR(20) NULL,
    ADD UNIQUE KEY uq_users_current_vehicle (current_vehicle_id),
    ADD KEY idx_users_waiting (current_status, waiting_location);

-- Preserve existing trip and schedule records. Existing active trips regain
-- their vehicle reservation when there is no conflicting active trip.
CREATE TEMPORARY TABLE migration_active_reservations AS
SELECT t.driver_id, MAX(t.id) AS trip_id
FROM trips t WHERE t.status = 'ACTIVE' GROUP BY t.driver_id;
UPDATE users u JOIN migration_active_reservations r ON r.driver_id = u.id
JOIN trips t ON t.id = r.trip_id
SET u.current_status = 'ON_TRIP', u.current_vehicle_id = t.vehicle_id
WHERE u.current_status = 'AVAILABLE';
DROP TEMPORARY TABLE migration_active_reservations;

ALTER TABLE users
    ADD CONSTRAINT chk_users_current_status
        CHECK (current_status IN ('AVAILABLE', 'WAITING', 'ON_TRIP')),
    ADD CONSTRAINT chk_users_waiting_location
        CHECK ((current_status = 'WAITING' AND waiting_location IS NOT NULL
                AND waiting_location IN ('IMT', 'ESTACAO'))
            OR (current_status <> 'WAITING' AND waiting_location IS NULL)),
    ADD CONSTRAINT chk_users_state_vehicle
        CHECK ((current_status = 'AVAILABLE' AND current_vehicle_id IS NULL)
            OR (current_status IN ('WAITING', 'ON_TRIP') AND current_vehicle_id IS NOT NULL)),
    ADD CONSTRAINT fk_users_current_vehicle
        FOREIGN KEY (current_vehicle_id) REFERENCES vehicles(id);

ALTER TABLE trips
    ADD COLUMN active_driver_id BIGINT GENERATED ALWAYS AS
        (CASE WHEN status = 'ACTIVE' THEN driver_id ELSE NULL END) STORED,
    ADD COLUMN active_vehicle_id BIGINT GENERATED ALWAYS AS
        (CASE WHEN status = 'ACTIVE' THEN vehicle_id ELSE NULL END) STORED,
    ADD UNIQUE KEY uq_trips_one_active_driver (active_driver_id),
    ADD UNIQUE KEY uq_trips_one_active_vehicle (active_vehicle_id);

-- Known legacy vehicle labels can be normalized without changing schedule codes.
UPDATE vehicles SET type = 'ONIBUS' WHERE UPPER(type) IN ('BUS', 'ÔNIBUS', 'ONIBUS');
UPDATE vehicles SET type = 'MICRO_ONIBUS' WHERE UPPER(type) IN ('MICROBUS', 'MICRO-ÔNIBUS', 'MICRO_ONIBUS');
ALTER TABLE vehicles MODIFY COLUMN type VARCHAR(50) NOT NULL,
    ADD CONSTRAINT chk_vehicles_type CHECK (type IN ('MICRO_ONIBUS', 'ONIBUS', 'VAN'));

