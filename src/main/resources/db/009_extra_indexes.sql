BEGIN;

CREATE INDEX IF NOT EXISTS idx_category_name
    ON categories(name);

CREATE INDEX IF NOT EXISTS idx_variant_name
    ON product_variants(name);

CREATE INDEX IF NOT EXISTS idx_variant_price
    ON product_variants(price);

CREATE INDEX IF NOT EXISTS idx_variant_active
    ON product_variants(is_active);

CREATE INDEX IF NOT EXISTS idx_product_discount_price
    ON products(discount_price);

CREATE INDEX IF NOT EXISTS idx_product_discount_start
    ON products(discount_start_date);

CREATE INDEX IF NOT EXISTS idx_product_discount_end
    ON products(discount_end_date);

CREATE INDEX IF NOT EXISTS idx_cart_item_quantity
    ON cart_items(quantity);

CREATE INDEX IF NOT EXISTS idx_cart_item_price
    ON cart_items(price_at_add);

CREATE INDEX IF NOT EXISTS idx_wishlist_price
    ON wishlists(price_at_add);

CREATE INDEX IF NOT EXISTS idx_delivery_name
    ON delivery_options(name);

CREATE INDEX IF NOT EXISTS idx_delivery_price
    ON delivery_options(base_price);

CREATE INDEX IF NOT EXISTS idx_order_total
    ON orders(total_amount);

CREATE INDEX IF NOT EXISTS idx_promo_code_lower
    ON promo_codes(lower(code));

CREATE INDEX IF NOT EXISTS idx_review_product_status_created
    ON reviews(product_id, status, created_at DESC);

COMMIT;