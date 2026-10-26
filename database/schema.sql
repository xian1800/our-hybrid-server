CREATE DATABASE IF NOT EXISTS hstestdb;
USE hstestdb;

-- Main table used by the HybridServer project for HTML content.
CREATE TABLE IF NOT EXISTS HTML (
    uuid VARCHAR(36) PRIMARY KEY,
    content LONGTEXT NOT NULL
);

-- The project expects the application user hsdb to be able to access the schema.
-- In Docker, the application connects from outside the container, so use '%' instead of 'localhost'.
CREATE USER IF NOT EXISTS 'hsdb'@'%' IDENTIFIED BY 'hsdbpass';
GRANT ALL PRIVILEGES ON hstestdb.* TO 'hsdb'@'%';
FLUSH PRIVILEGES;
