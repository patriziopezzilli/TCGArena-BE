-- Create waiting_list table
CREATE TABLE IF NOT EXISTS waiting_list (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    city VARCHAR(255) NOT NULL,
    user_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    contacted BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_email (email),
    INDEX idx_contacted (contacted),
    INDEX idx_user_type (user_type)
);
