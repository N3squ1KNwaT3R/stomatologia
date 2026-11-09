ALTER TABLE patients
    ADD CONSTRAINT fk_patients_user
        FOREIGN KEY (user_id)
            REFERENCES users(id);