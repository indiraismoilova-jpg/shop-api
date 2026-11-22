BEGIN;

INSERT INTO reviews (
    rating, title, comment, images,
    is_verified_purchase, helpful_count, not_helpful_count,
    status, created_at, updated_at, product_id, user_id
)
SELECT
    3 + (n.number % 3),
    'Demo review ' || n.number,
    'Demo customer feedback for ' || p.name,
    '[]'::jsonb,
    EXISTS (
        SELECT 1
        FROM orders o
                 JOIN order_items oi ON oi.order_id = o.id
        WHERE o.user_id = u.id
          AND oi.product_id = p.id
          AND o.status = 'DELIVERED'
    ),
    0,
    0,
    'APPROVED',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    p.id,
    u.id
FROM generate_series(1, 10) AS n(number)
         JOIN products p
              ON p.sku = 'DEMO-P' || lpad(n.number::text, 2, '0')
         CROSS JOIN users u
WHERE u.email = 'customer@shop.local'
  AND NOT EXISTS (
    SELECT 1
    FROM reviews r
    WHERE r.product_id = p.id AND r.user_id = u.id
);

UPDATE products p
SET
    review_count = (
        SELECT COUNT(*)
        FROM reviews r
        WHERE r.product_id = p.id
          AND r.status = 'APPROVED'
    ),
    average_rating = COALESCE((
                                  SELECT ROUND(AVG(r.rating), 2)
                                  FROM reviews r
                                  WHERE r.product_id = p.id
                                    AND r.status = 'APPROVED'
                              ), 0),
    updated_at = CURRENT_TIMESTAMP
WHERE p.sku IN (
    SELECT 'DEMO-P' || lpad(n::text, 2, '0')
    FROM generate_series(1, 20) AS n
);

INSERT INTO promo_codes (
    code, description, discount_type, discount_value,
    min_order_amount, max_discount_amount,
    max_usage_count, current_usage_count, max_usage_per_user,
    valid_from, valid_to, is_active, created_by_id
)
SELECT
    v.code,
    v.description,
    v.discount_type,
    v.discount_value,
    v.min_amount,
    v.max_discount,
    1000,
    0,
    3,
    CURRENT_TIMESTAMP - INTERVAL '1 day',
    CURRENT_TIMESTAMP + INTERVAL '3650 days',
    TRUE,
    u.id
FROM (
    VALUES
    (
    'DEMO10',
    'Ten percent off, capped at 100',
    'PERCENTAGE',
    10.00, 50.00, 100.00
    ),
    (
    'DEMO20',
    'Twenty off orders of at least 100',
    'FIXED_AMOUNT',
    20.00, 100.00, 20.00
    ),
    (
    'DEMOSHIP',
    'Free shipping on orders of at least 100',
    'FREE_SHIPPING',
    0.00, 100.00, NULL
    )
    ) AS v (
    code, description, discount_type,
    discount_value, min_amount, max_discount
    )
    CROSS JOIN users u
WHERE u.email = 'admin@shop.local'
ON CONFLICT (code) DO NOTHING;

COMMIT;