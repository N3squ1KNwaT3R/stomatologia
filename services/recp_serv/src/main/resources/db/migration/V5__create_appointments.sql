CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE appointments (
                              id BIGSERIAL PRIMARY KEY,

                              patient_id BIGINT NOT NULL REFERENCES patients(id),
                              doctor_id BIGINT NOT NULL REFERENCES doctors(id),
                              service_id BIGINT NOT NULL REFERENCES services(id),

                              start_at TIMESTAMPTZ NOT NULL,

                              duration_min INT NOT NULL CHECK (duration_min > 0),
                              buffer_min INT NOT NULL DEFAULT 0 CHECK (buffer_min >= 0),

                              end_at TIMESTAMPTZ NOT NULL,
                              blocked_until TIMESTAMPTZ NOT NULL,

                              status VARCHAR(20) NOT NULL
                                  CHECK (status IN ('PENDING', 'SCHEDULED', 'COMPLETED', 'CANCELLED')),

                              source VARCHAR(20) NOT NULL
                                  CHECK (source IN ('RECEPTION', 'WEBSITE')),

                              room VARCHAR(20),
                              comment TEXT,

                              created_by BIGINT NOT NULL REFERENCES users(id),
                              created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                              cancelled_at TIMESTAMPTZ,

                              CONSTRAINT chk_appointment_end_at
                                  CHECK (end_at = start_at + duration_min * INTERVAL '1 minute'),

    CONSTRAINT chk_appointment_blocked_until
        CHECK (blocked_until = end_at + buffer_min * INTERVAL '1 minute'),

    CONSTRAINT chk_appointment_cancelled_at
        CHECK (
            (status = 'CANCELLED' AND cancelled_at IS NOT NULL)
            OR
            (status <> 'CANCELLED' AND cancelled_at IS NULL)
        )
);

CREATE INDEX idx_appointments_doctor_start
    ON appointments (doctor_id, start_at);

CREATE INDEX idx_appointments_patient_start
    ON appointments (patient_id, start_at);

ALTER TABLE appointments
    ADD CONSTRAINT no_doctor_overlap
    EXCLUDE USING gist (
        doctor_id WITH =,
        tstzrange(start_at, blocked_until, '[)') WITH &&
    )
    WHERE (status IN ('PENDING', 'SCHEDULED'));



