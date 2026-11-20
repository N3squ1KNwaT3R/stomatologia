CREATE TABLE services (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    price NUMERIC(10, 2) NOT NULL CHECK (price >= 0),
    duration_min INT NOT NULL CHECK (duration_min > 0),
    buffer_min INT NOT NULL DEFAULT 0 CHECK (buffer_min >= 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE doctors (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
    specialization VARCHAR(100),
    room VARCHAR(20)
);

CREATE TABLE doctor_services (
    doctor_id BIGINT NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    service_id BIGINT NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    PRIMARY KEY (doctor_id, service_id)
);

CREATE TABLE doctor_working_hours (
    id BIGSERIAL PRIMARY KEY,
    doctor_id BIGINT NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    weekday SMALLINT NOT NULL CHECK (weekday BETWEEN 0 AND 6),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    CONSTRAINT chk_doctor_working_hours_time CHECK (start_time < end_time)
);

CREATE INDEX idx_doctor_services_service_id
    ON doctor_services(service_id);

CREATE INDEX idx_doctor_working_hours_doctor_weekday
    ON doctor_working_hours(doctor_id, weekday);
