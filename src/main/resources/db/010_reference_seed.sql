BEGIN;

INSERT INTO users (
    email, password, first_name, last_name,
    role, enabled, email_verified, created_at, updated_at
)
VALUES
    (
        'admin@shop.local',
        '$2b$12$Co6W7ViIREUQ7C1W882SEOLc6jpKs5jjSBXObsnzsw2BWnjMTvIIe',
        'Demo', 'Admin',
        'ADMIN', TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    ),
    (
        'customer@shop.local',
        '$2b$12$Co6W7ViIREUQ7C1W882SEOLc6jpKs5jjSBXObsnzsw2BWnjMTvIIe',
        'Demo', 'Customer',
        'CUSTOMER', TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    )
    ON CONFLICT (email) DO NOTHING;

INSERT INTO categories (
    name, slug, description, sort_order, is_active
)
VALUES
    ('Phones', 'demo-phones', 'Demo smartphones', 1, TRUE),
    ('Laptops', 'demo-laptops', 'Demo laptops', 2, TRUE),
    ('Tablets', 'demo-tablets', 'Demo tablets', 3, TRUE),
    ('Audio', 'demo-audio', 'Demo audio devices', 4, TRUE),
    ('Accessories', 'demo-accessories', 'Demo accessories', 5, TRUE)
    ON CONFLICT (slug) DO NOTHING;

INSERT INTO brands (
    name, slug, description, country, is_active
)
VALUES
    ('NovaTech', 'demo-novatech', 'Demo electronics brand', 'Kazakhstan', TRUE),
    ('Orbit', 'demo-orbit', 'Demo device brand', 'Kazakhstan', TRUE),
    ('Pulse', 'demo-pulse', 'Demo audio brand', 'Kazakhstan', TRUE)
    ON CONFLICT (slug) DO NOTHING;

COMMIT;