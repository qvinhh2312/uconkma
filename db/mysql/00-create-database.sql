-- UCONKMA MySQL bootstrap script.
-- Run this once with a MySQL account that can create databases and users.

CREATE DATABASE IF NOT EXISTS ucon_kma
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'ucon_app'@'localhost'
  IDENTIFIED BY 'ucon_app_123';

GRANT ALL PRIVILEGES ON ucon_kma.* TO 'ucon_app'@'localhost';

FLUSH PRIVILEGES;

