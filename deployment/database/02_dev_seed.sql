-- Dev-only seed data. Do not run against production.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO roles (name) VALUES ('ADMIN');

INSERT INTO accounts (id, identifier, password_hash, account_type)
VALUES ('00000000-0000-0000-0000-000000000001', 'admin@blossom.dev', crypt('admin123', gen_salt('bf', 10)), 'EMPLOYEE');

INSERT INTO employees (account_id, role_id, first_name, last_name)
VALUES ('00000000-0000-0000-0000-000000000001', (SELECT id FROM roles WHERE name = 'ADMIN'), 'Admin', 'Blossom');

INSERT INTO categories (name) VALUES ('Shirts'), ('Pants'), ('Dresses'), ('Shoes'), ('Accessories');
