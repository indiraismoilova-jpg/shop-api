INSERT INTO delivery_options (
    name,
    code,
    description,
    base_price,
    free_threshold,
    estimated_days_min,
    estimated_days_max,
    is_active
)
VALUES
    (
        'Courier',
        'COURIER',
        'Standard courier delivery',
        10.00,
        500.00,
        2,
        5,
        TRUE
    ),
    (
        'Pickup point',
        'PICKUP_POINT',
        'Collect at a pickup point',
        5.00,
        200.00,
        1,
        3,
        TRUE
    ),
    (
        'Post office',
        'POST_OFFICE',
        'Postal delivery',
        7.00,
        300.00,
        3,
        10,
        TRUE
    ),
    (
        'Express',
        'EXPRESS',
        'Express courier delivery',
        20.00,
        NULL,
        1,
        2,
        TRUE
    )
    ON CONFLICT (code) DO NOTHING;