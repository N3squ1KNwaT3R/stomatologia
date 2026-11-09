
CREATE TABLE patients (
                          id BIGSERIAL PRIMARY KEY,

                          first_name VARCHAR(100) NOT NULL,
                          last_name VARCHAR(100) NOT NULL,

                          pesel VARCHAR(11) UNIQUE,
                          date_of_birth DATE,

                          phone VARCHAR(20),
                          email VARCHAR(255),

                          notes TEXT,

                          user_id BIGINT UNIQUE,

                          is_verified BOOLEAN NOT NULL DEFAULT FALSE,

                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
