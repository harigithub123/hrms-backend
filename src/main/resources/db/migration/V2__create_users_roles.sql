-- Roles table
CREATE TABLE roles (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- Users table
CREATE TABLE users (
    id         BIGSERIAL PRIMARY KEY,
    username   VARCHAR(100) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    email      VARCHAR(255),
    enabled    BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- User-Role join table
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);

-- Refresh tokens for JWT refresh
CREATE TABLE refresh_tokens (
    id         BIGSERIAL PRIMARY KEY,
    token      VARCHAR(500) NOT NULL UNIQUE,
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked    BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- Default roles (Spring Security expects ROLE_ prefix in authority checks; we store with prefix)
INSERT INTO roles (name) VALUES ('ROLE_ADMIN'), ('ROLE_HR'), ('ROLE_USER');

-- Default admin user: username=admin, password=password (BCrypt hash)
INSERT INTO users (username, password, email, enabled)
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqYQXkHOQj.5/zYVVBvWZ.lbQK.By', 'admin@hrms.local', true);

-- Assign ADMIN and USER to admin user (role ids 1=ROLE_ADMIN, 2=ROLE_HR, 3=ROLE_USER)
INSERT INTO user_roles (user_id, role_id) VALUES (1, 1), (1, 3);
