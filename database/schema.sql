-- =====================================================
-- LOCATION TRACKING SYSTEM
-- Initial database schema
-- MySQL 8.0+
-- =====================================================

CREATE DATABASE IF NOT EXISTS location_tracking
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE location_tracking;

-- =====================================================
-- TABLE: users
-- Stores system users and drivers
-- =====================================================

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    current_status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    current_vehicle_id BIGINT NULL UNIQUE,
    waiting_location VARCHAR(20) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_users_role
        CHECK (role IN ('DRIVER', 'ADMIN')),
    CONSTRAINT chk_users_current_status
        CHECK (current_status IN ('AVAILABLE', 'WAITING', 'ON_TRIP')),
    CONSTRAINT chk_users_waiting_location
        CHECK ((current_status = 'WAITING' AND waiting_location IS NOT NULL
                AND waiting_location IN ('IMT', 'ESTACAO'))
            OR (current_status <> 'WAITING' AND waiting_location IS NULL)),
    CONSTRAINT chk_users_state_vehicle
        CHECK ((current_status = 'AVAILABLE' AND current_vehicle_id IS NULL)
            OR (current_status IN ('WAITING', 'ON_TRIP') AND current_vehicle_id IS NOT NULL)),
    INDEX idx_users_waiting (current_status, waiting_location)
) ENGINE=InnoDB;

-- =====================================================
-- TABLE: vehicles
-- Stores the circular transport vehicles
-- =====================================================

CREATE TABLE IF NOT EXISTS vehicles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    identification VARCHAR(50) NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_vehicles_type
        CHECK (type IN ('MICRO_ONIBUS', 'ONIBUS', 'VAN')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Added after vehicles exists so fresh installations can create the FK.
SET @fk_users_vehicle_exists = (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'users'
      AND CONSTRAINT_NAME = 'fk_users_current_vehicle'
);
SET @add_users_vehicle_fk = IF(@fk_users_vehicle_exists = 0,
    'ALTER TABLE users ADD CONSTRAINT fk_users_current_vehicle FOREIGN KEY (current_vehicle_id) REFERENCES vehicles(id)',
    'SELECT 1');
PREPARE add_users_vehicle_fk_stmt FROM @add_users_vehicle_fk;
EXECUTE add_users_vehicle_fk_stmt;
DEALLOCATE PREPARE add_users_vehicle_fk_stmt;

-- =====================================================
-- TABLE: trips
-- Stores vehicle trips
-- =====================================================

CREATE TABLE IF NOT EXISTS trips (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    driver_id BIGINT NOT NULL,
    vehicle_id BIGINT NOT NULL,
    start_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    direction VARCHAR(50) NOT NULL,
    active_driver_id BIGINT GENERATED ALWAYS AS
        (CASE WHEN status = 'ACTIVE' THEN driver_id ELSE NULL END) STORED,
    active_vehicle_id BIGINT GENERATED ALWAYS AS
        (CASE WHEN status = 'ACTIVE' THEN vehicle_id ELSE NULL END) STORED,

    CONSTRAINT fk_trips_driver
        FOREIGN KEY (driver_id)
        REFERENCES users(id),

    CONSTRAINT fk_trips_vehicle
        FOREIGN KEY (vehicle_id)
        REFERENCES vehicles(id),

    CONSTRAINT chk_trips_status
        CHECK (status IN ('ACTIVE', 'FINISHED', 'CANCELLED')),

    INDEX idx_trips_status (status),
    INDEX idx_trips_driver (driver_id),
    INDEX idx_trips_vehicle (vehicle_id),
    UNIQUE KEY uq_trips_one_active_driver (active_driver_id),
    UNIQUE KEY uq_trips_one_active_vehicle (active_vehicle_id)
) ENGINE=InnoDB;

-- =====================================================
-- TABLE: locations
-- Stores geographic positions recorded during trips
-- =====================================================

CREATE TABLE IF NOT EXISTS locations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id BIGINT NOT NULL,
    latitude DECIMAL(10, 7) NOT NULL,
    longitude DECIMAL(10, 7) NOT NULL,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_locations_trip
        FOREIGN KEY (trip_id)
        REFERENCES trips(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_locations_latitude
        CHECK (latitude BETWEEN -90 AND 90),

    CONSTRAINT chk_locations_longitude
        CHECK (longitude BETWEEN -180 AND 180),

    INDEX idx_locations_trip_time (trip_id, recorded_at)
) ENGINE=InnoDB;

-- =====================================================
-- TABLE: schedules
-- Stores the transport timetable
-- =====================================================

CREATE TABLE IF NOT EXISTS schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id BIGINT NOT NULL,
    departure_time TIME NOT NULL,
    direction VARCHAR(50) NOT NULL,
    day_type VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_schedules_vehicle
        FOREIGN KEY (vehicle_id)
        REFERENCES vehicles(id),

    CONSTRAINT chk_schedules_day_type
        CHECK (day_type IN ('WEEKDAY', 'SATURDAY', 'SUNDAY', 'HOLIDAY')),

    INDEX idx_schedules_search (direction, day_type, departure_time),
    UNIQUE KEY uq_schedules_vehicle_departure_direction_day
        (vehicle_id, departure_time, direction, day_type)
) ENGINE=InnoDB;
