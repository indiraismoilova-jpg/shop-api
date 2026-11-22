BEGIN;

INSERT INTO addresses (
    title, country, city, street, building, postal_code,
    recipient_name, recipient_phone, is_default, user_id
)
SELECT
    'Demo Home', 'Kazakhstan', 'Almaty', 'Demo Street',
    '1', '050000', 'Demo Customer', '+77000000000', TRUE, u.id
FROM users u
WHERE u.email = 'customer@shop.local'
  AND NOT EXISTS (
    SELECT 1 FROM addresses a
    WHERE a.user_id = u.id AND a.title = 'Demo Home'
);

DO $$
DECLARE
demo_user_id BIGINT;
    delivery delivery_options%ROWTYPE;
    product products%ROWTYPE;
    entry RECORD;
    new_order_id BIGINT;
    shipping NUMERIC(12,2);
    created TIMESTAMP;
    is_paid BOOLEAN;
BEGIN
SELECT id INTO demo_user_id
FROM users WHERE email = 'customer@shop.local';

SELECT * INTO delivery
FROM delivery_options WHERE code = 'COURIER';

IF demo_user_id IS NULL OR delivery.id IS NULL THEN
        RAISE EXCEPTION 'Run reference and delivery seeds first';
END IF;

FOR entry IN
SELECT * FROM (
                  VALUES
                      (1, 'PENDING', 'DEMO-P01'),
                      (2, 'CONFIRMED', 'DEMO-P02'),
                      (3, 'PAID', 'DEMO-P03'),
                      (4, 'SHIPPED', 'DEMO-P04'),
                      (5, 'DELIVERED', 'DEMO-P05')
              ) AS entries(number, status, sku)
    LOOP
        IF EXISTS (
            SELECT 1 FROM orders
            WHERE order_number = 'DEMO-ORDER-00' || entry.number
        ) THEN
            CONTINUE;
END IF;

SELECT * INTO product
FROM products WHERE sku = entry.sku
    FOR UPDATE;

IF product.id IS NULL THEN
            RAISE EXCEPTION 'Missing seed product: %', entry.sku;
END IF;

        IF product.stock_quantity - product.reserved_quantity < 1 THEN
            RAISE EXCEPTION 'Insufficient seed stock: %', entry.sku;
END IF;

        is_paid := entry.status IN ('PAID', 'SHIPPED', 'DELIVERED');

        created := CASE WHEN is_paid
            THEN CURRENT_TIMESTAMP - INTERVAL '7 days'
            ELSE CURRENT_TIMESTAMP
END;

        shipping := CASE
            WHEN delivery.free_threshold IS NOT NULL
             AND product.base_price >= delivery.free_threshold
            THEN 0
            ELSE delivery.base_price
END;

INSERT INTO orders (
    order_number, status,
    shipping_country, shipping_city, shipping_street,
    shipping_building, shipping_postal_code,
    shipping_recipient_name, shipping_recipient_phone,
    delivery_method, delivery_tracking_number,
    estimated_delivery_date, actual_delivery_date,
    subtotal, shipping_cost, discount,
    bonus_points_used, bonus_points_earned, total_amount,
    customer_note, created_at, updated_at,
    paid_at, shipped_at, delivered_at, user_id
)
VALUES (
           'DEMO-ORDER-00' || entry.number, entry.status,
           'Kazakhstan', 'Almaty', 'Demo Street',
           '1', '050000', 'Demo Customer', '+77000000000',
           'COURIER',
           CASE WHEN entry.status IN ('SHIPPED', 'DELIVERED')
                    THEN 'TRK-DEMO-00' || entry.number ELSE NULL END,
           created + INTERVAL '5 days',
           CASE WHEN entry.status = 'DELIVERED'
                    THEN created + INTERVAL '4 days' ELSE NULL END,
           product.base_price, shipping, 0,
           0, 0, product.base_price + shipping,
           'Demo seed order', created, CURRENT_TIMESTAMP,
           CASE WHEN is_paid
                    THEN created + INTERVAL '1 minute' ELSE NULL END,
           CASE WHEN entry.status IN ('SHIPPED', 'DELIVERED')
                    THEN created + INTERVAL '2 days' ELSE NULL END,
           CASE WHEN entry.status = 'DELIVERED'
                    THEN created + INTERVAL '4 days' ELSE NULL END,
           demo_user_id
       )
    RETURNING id INTO new_order_id;

INSERT INTO order_items (
    quantity, price_at_purchase, product_snapshot,
    order_id, product_id
)
VALUES (
           1, product.base_price,
           jsonb_build_object(
                   'id', product.id,
                   'name', product.name,
                   'sku', product.sku
           ),
           new_order_id, product.id
       );

IF is_paid THEN
UPDATE products
SET stock_quantity = stock_quantity - 1,
    sales_count = sales_count + 1,
    updated_at = CURRENT_TIMESTAMP
WHERE id = product.id;

INSERT INTO payments (
    transaction_id, payment_method, status,
    amount, currency, bonus_points_used,
    card_last_four_digits, paid_at,
    refunded_amount, metadata, order_id
)
VALUES (
           'SIM-DEMO-00' || entry.number,
           'CARD', 'COMPLETED',
           product.base_price + shipping, 'USD', 0,
           '1111', created + INTERVAL '1 minute',
           0, '{"simulated": true}'::jsonb, new_order_id
       );
ELSE
UPDATE products
SET reserved_quantity = reserved_quantity + 1,
    updated_at = CURRENT_TIMESTAMP
WHERE id = product.id;
END IF;
END LOOP;
END;
$$;

COMMIT;