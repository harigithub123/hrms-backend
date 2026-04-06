-- HR/Admin flag: when set, onboarding tasks are created for experience and relieving letters.
ALTER TABLE employees
    ADD COLUMN existed_firm BOOLEAN NOT NULL DEFAULT FALSE;
