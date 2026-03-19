-- Company-wide holidays (read-only list for employees; manage via DB or future admin UI)
CREATE TABLE holidays (
    id            BIGSERIAL PRIMARY KEY,
    holiday_date  DATE         NOT NULL UNIQUE,
    name          VARCHAR(200) NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Sample holidays (adjust for your region)
INSERT INTO holidays (holiday_date, name) VALUES
    ('2025-01-01', 'New Year''s Day'),
    ('2025-01-26', 'Republic Day'),
    ('2025-03-14', 'Holi'),
    ('2025-08-15', 'Independence Day'),
    ('2025-10-02', 'Gandhi Jayanti'),
    ('2025-10-20', 'Dussehra'),
    ('2025-11-01', 'Diwali'),
    ('2025-12-25', 'Christmas'),
    ('2026-01-01', 'New Year''s Day'),
    ('2026-01-26', 'Republic Day'),
    ('2026-03-03', 'Holi'),
    ('2026-08-15', 'Independence Day'),
    ('2026-10-02', 'Gandhi Jayanti'),
    ('2026-10-12', 'Dussehra'),
    ('2026-11-08', 'Diwali'),
    ('2026-12-25', 'Christmas');
