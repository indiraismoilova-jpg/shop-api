BEGIN;

INSERT INTO products (
    name, slug, description, short_description, sku,
    base_price, stock_quantity, reserved_quantity,
    low_stock_threshold,
    is_active, is_featured, is_new_arrival,
    average_rating, review_count, view_count, sales_count,
    created_at, updated_at, category_id, brand_id
)
SELECT
    v.name,
    v.slug,
    'Demo product for the shop assignment',
    v.name,
    v.sku,
    v.price,
    50,
    0,
    5,
    TRUE,
    v.featured,
    v.new_arrival,
    0,
    0,
    0,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    c.id,
    b.id
FROM (
         VALUES
             ('Nova Phone Lite', 'demo-phone-lite', 'DEMO-P01', 120.00, 'demo-phones', 'demo-novatech', TRUE, FALSE),
             ('Nova Phone Plus', 'demo-phone-plus', 'DEMO-P02', 240.00, 'demo-phones', 'demo-novatech', TRUE, TRUE),
             ('Orbit Phone Mini', 'demo-phone-mini', 'DEMO-P03', 150.00, 'demo-phones', 'demo-orbit', FALSE, FALSE),
             ('Orbit Phone Pro', 'demo-phone-pro', 'DEMO-P04', 450.00, 'demo-phones', 'demo-orbit', TRUE, TRUE),

             ('Nova Laptop Air', 'demo-laptop-air', 'DEMO-P05', 650.00, 'demo-laptops', 'demo-novatech', TRUE, FALSE),
             ('Nova Laptop Pro', 'demo-laptop-pro', 'DEMO-P06', 950.00, 'demo-laptops', 'demo-novatech', TRUE, TRUE),
             ('Orbit Laptop Go', 'demo-laptop-go', 'DEMO-P07', 550.00, 'demo-laptops', 'demo-orbit', FALSE, FALSE),
             ('Orbit Laptop Max', 'demo-laptop-max', 'DEMO-P08', 1100.00, 'demo-laptops', 'demo-orbit', FALSE, TRUE),

             ('Nova Tablet Mini', 'demo-tablet-mini', 'DEMO-P09', 180.00, 'demo-tablets', 'demo-novatech', FALSE, FALSE),
             ('Nova Tablet Plus', 'demo-tablet-plus', 'DEMO-P10', 320.00, 'demo-tablets', 'demo-novatech', TRUE, TRUE),
             ('Orbit Tablet Go', 'demo-tablet-go', 'DEMO-P11', 210.00, 'demo-tablets', 'demo-orbit', FALSE, FALSE),
             ('Orbit Tablet Pro', 'demo-tablet-pro', 'DEMO-P12', 420.00, 'demo-tablets', 'demo-orbit', FALSE, TRUE),

             ('Pulse Earbuds', 'demo-earbuds', 'DEMO-P13', 45.00, 'demo-audio', 'demo-pulse', TRUE, FALSE),
             ('Pulse Headphones', 'demo-headphones', 'DEMO-P14', 85.00, 'demo-audio', 'demo-pulse', TRUE, TRUE),
             ('Pulse Speaker Mini', 'demo-speaker-mini', 'DEMO-P15', 35.00, 'demo-audio', 'demo-pulse', FALSE, FALSE),
             ('Pulse Speaker Max', 'demo-speaker-max', 'DEMO-P16', 125.00, 'demo-audio', 'demo-pulse', FALSE, TRUE),

             ('Nova Charger', 'demo-charger', 'DEMO-P17', 20.00, 'demo-accessories', 'demo-novatech', FALSE, FALSE),
             ('Orbit Power Bank', 'demo-power-bank', 'DEMO-P18', 40.00, 'demo-accessories', 'demo-orbit', TRUE, FALSE),
             ('Orbit Keyboard', 'demo-keyboard', 'DEMO-P19', 55.00, 'demo-accessories', 'demo-orbit', FALSE, TRUE),
             ('Pulse Microphone', 'demo-microphone', 'DEMO-P20', 70.00, 'demo-accessories', 'demo-pulse', FALSE, TRUE)
     ) AS v (
             name, slug, sku, price,
             category_slug, brand_slug, featured, new_arrival
    )
         JOIN categories c ON c.slug = v.category_slug
         JOIN brands b ON b.slug = v.brand_slug
    ON CONFLICT (slug) DO NOTHING;

COMMIT;