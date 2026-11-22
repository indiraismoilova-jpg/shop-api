BEGIN;

INSERT INTO product_images (
    image_url,
    thumbnail_url,
    alt_text,
    is_primary,
    sort_order,
    product_id
)
SELECT
    '/images/demo-product.svg',
    '/images/demo-product.svg',
    p.name,
    NOT EXISTS (
        SELECT 1
        FROM product_images existing
        WHERE existing.product_id = p.id
          AND existing.is_primary = TRUE
    ),
    0,
    p.id
FROM products p
JOIN generate_series(1, 20) AS n(number)
    ON p.sku = 'DEMO-P' || lpad(n.number::text, 2, '0')
WHERE NOT EXISTS (
    SELECT 1
    FROM product_images existing
    WHERE existing.product_id = p.id
      AND existing.image_url = '/images/demo-product.svg'
);

INSERT INTO product_specifications (
    name,
    value,
    group_name,
    sort_order,
    product_id
)
SELECT
    details.name,
    details.value,
    'General',
    details.sort_order,
    p.id
FROM products p
JOIN generate_series(1, 20) AS n(number)
    ON p.sku = 'DEMO-P' || lpad(n.number::text, 2, '0')
LEFT JOIN brands b ON b.id = p.brand_id
CROSS JOIN LATERAL (
    VALUES
        ('Model', p.name, 0),
        ('Brand', COALESCE(b.name, 'Demo'), 1)
) AS details(name, value, sort_order)
WHERE NOT EXISTS (
    SELECT 1
    FROM product_specifications existing
    WHERE existing.product_id = p.id
      AND existing.name = details.name
      AND existing.group_name = 'General'
);

COMMIT;