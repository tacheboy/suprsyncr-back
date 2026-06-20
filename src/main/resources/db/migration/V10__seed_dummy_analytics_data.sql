-- =============================================================
-- V10: Analytics Seed Data — 3 Realistic D2C Stores
-- =============================================================
-- Store A: Fashion, Tier 1, Instagram-heavy, HIGH abandonment
-- Store B: Electronics, Tier 2, Google-heavy, SEO gap story
-- Store C: Home Decor, mixed traffic, underperforming listings
-- =============================================================

-- ─────────────────────────────────────────────────────────────
-- STORES
-- ─────────────────────────────────────────────────────────────
INSERT INTO dummy_stores (store_id, store_name, category, monthly_traffic, avg_order_value, primary_traffic_source) VALUES
('store-a', 'Meera Ethnicwear',   'fashion',     28000, 1450.00, 'instagram'),
('store-b', 'TechNest Gadgets',   'electronics', 14000,  3200.00, 'google'),
('store-c', 'HomeNook Decor',     'home',        9500,   880.00,  'direct');

-- ─────────────────────────────────────────────────────────────
-- PRODUCTS — Store A (Fashion / Instagram-heavy)
-- ─────────────────────────────────────────────────────────────
INSERT INTO dummy_products
  (product_id, store_id, name, category, price, stock,
   monthly_page_views, add_to_cart_count, checkout_count, purchase_count,
   organic_impressions, organic_clicks, avg_position, primary_traffic_source)
VALUES
('pa-001', 'store-a', 'Linen Kurta Set',         'kurtas',    1299, 85,  6200, 1860, 620, 136,  8400,  126, 12.3, 'instagram'),
('pa-002', 'store-a', 'Cotton Block-Print Saree', 'sarees',   1850, 42,  4100, 1025, 390,  78,  5600,   84, 15.1, 'instagram'),
('pa-003', 'store-a', 'Handloom Palazzo Set',     'bottoms',  1100, 120, 3200,  896, 224,  80,  3200,   48, 18.7, 'instagram'),
('pa-004', 'store-a', 'Embroidered Dupatta',      'dupattas',  499, 200, 2800,  392,  84,  56,  2100,   63, 11.2, 'instagram'),
('pa-005', 'store-a', 'Cotton Anarkali Suit',     'kurtas',   1650, 60,  1900,  380,  95,  32,  4200,   84, 14.6, 'google');

-- ─────────────────────────────────────────────────────────────
-- PRODUCTS — Store B (Electronics / Google-heavy / SEO gap)
-- ─────────────────────────────────────────────────────────────
INSERT INTO dummy_products
  (product_id, store_id, name, category, price, stock,
   monthly_page_views, add_to_cart_count, checkout_count, purchase_count,
   organic_impressions, organic_clicks, avg_position, primary_traffic_source)
VALUES
('pb-001', 'store-b', 'USB-C Hub 7-in-1',          'accessories', 2499,  50,  4800, 960, 480, 192,  9600,  144, 11.8, 'google'),
('pb-002', 'store-b', 'Wireless Charging Pad',      'accessories', 1299, 110,  3200, 544, 256,  90,  7200,   72, 14.2, 'google'),
('pb-003', 'store-b', 'Mechanical Keyboard TKL',    'peripherals', 4500,  25,  1800, 270, 108,  54,  3800,   38, 16.9, 'google'),
('pb-004', 'store-b', 'Webcam 1080p Auto-Focus',    'peripherals', 2999,  35,  2600, 390, 156,  78,  5100,   51, 13.5, 'google'),
('pb-005', 'store-b', 'Laptop Cooling Stand',       'accessories', 1799,  80,  1200, 144,  60,  26,  2800,   28, 19.4, 'direct');

-- ─────────────────────────────────────────────────────────────
-- PRODUCTS — Store C (Home Decor / mixed / underperforming)
-- ─────────────────────────────────────────────────────────────
INSERT INTO dummy_products
  (product_id, store_id, name, category, price, stock,
   monthly_page_views, add_to_cart_count, checkout_count, purchase_count,
   organic_impressions, organic_clicks, avg_position, primary_traffic_source)
VALUES
('pc-001', 'store-c', 'Macramé Wall Hanging Large',  'wall-decor', 799,  60,  2800, 504, 140,  42,  3600,   54, 13.7, 'direct'),
('pc-002', 'store-c', 'Terracotta Pot Set of 3',     'planters',   599, 120,  1900, 285,  76,  30,  2400,   36, 17.2, 'google'),
('pc-003', 'store-c', 'Jute Table Runner',           'table-decor',349, 200,  1600, 192,  48,  22,  1800,   27, 20.1, 'direct'),
('pc-004', 'store-c', 'Handpainted Ceramic Mugs x2', 'kitchen',    699,  90,  2100, 294,  84,  35,  2900,   46, 15.8, 'instagram'),
('pc-005', 'store-c', 'Wooden Candle Holder Set',    'decor',      549,  75,  1400, 154,  42,  18,  1500,   18, 22.3, 'direct');

-- ─────────────────────────────────────────────────────────────
-- SEO KEYWORDS — Store A
-- ─────────────────────────────────────────────────────────────
INSERT INTO dummy_product_keywords (product_id, query, impressions, clicks, position, ctr) VALUES
('pa-001', 'handmade linen kurta set',          1200,  18, 11.4, 0.015),
('pa-001', 'linen women kurta india',            840,   6, 14.2, 0.007),
('pa-001', 'ethnic linen kurta online',          620,   4, 16.8, 0.006),
('pa-002', 'cotton block print saree buy',       980,  12, 12.6, 0.012),
('pa-002', 'handblock printed sarees women',     740,   7, 15.3, 0.009),
('pa-003', 'palazzo set women ethnic',          1100,   9, 13.1, 0.008),
('pa-004', 'embroidered dupatta online',         880,  14, 10.9, 0.016),
('pa-005', 'anarkali suit cotton buy',           960,   8, 14.7, 0.008);

-- ─────────────────────────────────────────────────────────────
-- SEO KEYWORDS — Store B
-- ─────────────────────────────────────────────────────────────
INSERT INTO dummy_product_keywords (product_id, query, impressions, clicks, position, ctr) VALUES
('pb-001', 'usb c hub multiport india',         2100,  21, 11.8, 0.010),
('pb-001', '7 in 1 usb hub laptop',             1800,  14, 13.4, 0.008),
('pb-001', 'usb hub 4k hdmi buy',               1400,   7, 16.2, 0.005),
('pb-002', 'wireless charging pad fast',         1600,  12, 14.2, 0.008),
('pb-002', 'qi wireless charger india',          1200,   6, 17.9, 0.005),
('pb-003', 'mechanical keyboard tkl budget',     1900,  19, 11.3, 0.010),
('pb-003', 'tenkeyless keyboard under 5000',    1600,  10, 14.8, 0.006),
('pb-004', '1080p webcam autofocus buy',         2200,  22, 10.9, 0.010),
('pb-004', 'best webcam work from home india',  1800,  14, 13.1, 0.008);

-- ─────────────────────────────────────────────────────────────
-- SEO KEYWORDS — Store C
-- ─────────────────────────────────────────────────────────────
INSERT INTO dummy_product_keywords (product_id, query, impressions, clicks, position, ctr) VALUES
('pc-001', 'macrame wall hanging large buy',      820,  6, 13.7, 0.007),
('pc-001', 'boho wall decor handmade india',      640,  4, 16.9, 0.006),
('pc-002', 'terracotta pots set india',           740,  7, 17.2, 0.009),
('pc-002', 'handmade clay pots for plants',       560,  3, 20.5, 0.005),
('pc-004', 'handpainted ceramic mugs gift india', 980, 10, 15.8, 0.010),
('pc-004', 'ceramic coffee mug set handmade',     760,  5, 18.2, 0.007);

-- ─────────────────────────────────────────────────────────────
-- SESSION EVENTS — 30 days, Store A (Instagram-heavy, high abandon)
-- ─────────────────────────────────────────────────────────────
INSERT INTO dummy_session_events
  (event_date, store_id, source, sessions, add_to_cart_sessions, checkout_init_sessions, purchased_sessions)
SELECT
    CURRENT_DATE - (gs.day || ' days')::INTERVAL,
    'store-a',
    src.source,
    src.sessions,
    ROUND(src.sessions * src.atc_rate),
    ROUND(src.sessions * src.atc_rate * src.checkout_rate),
    ROUND(src.sessions * src.atc_rate * src.checkout_rate * src.purchase_rate)
FROM generate_series(0, 29) AS gs(day)
CROSS JOIN (VALUES
    ('instagram', 580, 0.32, 0.34, 0.22),
    ('google',    180, 0.24, 0.48, 0.41),
    ('direct',     80, 0.28, 0.52, 0.38),
    ('referral',   40, 0.20, 0.30, 0.25)
) AS src(source, sessions, atc_rate, checkout_rate, purchase_rate);

-- ─────────────────────────────────────────────────────────────
-- SESSION EVENTS — 30 days, Store B (Google-heavy)
-- ─────────────────────────────────────────────────────────────
INSERT INTO dummy_session_events
  (event_date, store_id, source, sessions, add_to_cart_sessions, checkout_init_sessions, purchased_sessions)
SELECT
    CURRENT_DATE - (gs.day || ' days')::INTERVAL,
    'store-b',
    src.source,
    src.sessions,
    ROUND(src.sessions * src.atc_rate),
    ROUND(src.sessions * src.atc_rate * src.checkout_rate),
    ROUND(src.sessions * src.atc_rate * src.checkout_rate * src.purchase_rate)
FROM generate_series(0, 29) AS gs(day)
CROSS JOIN (VALUES
    ('google',    320, 0.28, 0.52, 0.44),
    ('instagram',  80, 0.18, 0.28, 0.18),
    ('direct',     50, 0.30, 0.56, 0.46),
    ('referral',   20, 0.22, 0.38, 0.30)
) AS src(source, sessions, atc_rate, checkout_rate, purchase_rate);

-- ─────────────────────────────────────────────────────────────
-- SESSION EVENTS — 30 days, Store C (mixed)
-- ─────────────────────────────────────────────────────────────
INSERT INTO dummy_session_events
  (event_date, store_id, source, sessions, add_to_cart_sessions, checkout_init_sessions, purchased_sessions)
SELECT
    CURRENT_DATE - (gs.day || ' days')::INTERVAL,
    'store-c',
    src.source,
    src.sessions,
    ROUND(src.sessions * src.atc_rate),
    ROUND(src.sessions * src.atc_rate * src.checkout_rate),
    ROUND(src.sessions * src.atc_rate * src.checkout_rate * src.purchase_rate)
FROM generate_series(0, 29) AS gs(day)
CROSS JOIN (VALUES
    ('direct',    120, 0.22, 0.40, 0.32),
    ('google',    100, 0.20, 0.42, 0.34),
    ('instagram',  60, 0.16, 0.26, 0.18),
    ('referral',   30, 0.18, 0.34, 0.28)
) AS src(source, sessions, atc_rate, checkout_rate, purchase_rate);

-- ─────────────────────────────────────────────────────────────
-- DAILY ORDERS — Store A (last 30 days, realistic city/gender/age spread)
-- ─────────────────────────────────────────────────────────────
INSERT INTO dummy_daily_orders (order_id, store_id, product_id, revenue, order_date, channel, customer_city, customer_gender, age_group)
SELECT
    'ord-a-' || gs.day || '-' || p.rn,
    'store-a',
    p.pid,
    p.price,
    CURRENT_DATE - (gs.day || ' days')::INTERVAL,
    p.ch,
    p.city,
    p.gender,
    p.age
FROM generate_series(0, 29) AS gs(day)
CROSS JOIN (VALUES
    (1, 'pa-001', 1299.00, 'instagram', 'Mumbai',    'female', '25-34'),
    (2, 'pa-001', 1299.00, 'instagram', 'Delhi',     'female', '18-24'),
    (3, 'pa-002', 1850.00, 'instagram', 'Bangalore', 'female', '25-34'),
    (4, 'pa-002', 1850.00, 'google',    'Pune',      'female', '35-44'),
    (5, 'pa-003', 1100.00, 'instagram', 'Chennai',   'female', '18-24'),
    (6, 'pa-004', 499.00,  'instagram', 'Hyderabad', 'female', '25-34'),
    (7, 'pa-005', 1650.00, 'google',    'Kolkata',   'female', '25-34')
) AS p(rn, pid, price, ch, city, gender, age);

-- ─────────────────────────────────────────────────────────────
-- DAILY ORDERS — Store B
-- ─────────────────────────────────────────────────────────────
INSERT INTO dummy_daily_orders (order_id, store_id, product_id, revenue, order_date, channel, customer_city, customer_gender, age_group)
SELECT
    'ord-b-' || gs.day || '-' || p.rn,
    'store-b',
    p.pid,
    p.price,
    CURRENT_DATE - (gs.day || ' days')::INTERVAL,
    p.ch,
    p.city,
    p.gender,
    p.age
FROM generate_series(0, 29) AS gs(day)
CROSS JOIN (VALUES
    (1, 'pb-001', 2499.00, 'google',   'Bangalore', 'male', '25-34'),
    (2, 'pb-001', 2499.00, 'google',   'Delhi',     'male', '18-24'),
    (3, 'pb-002', 1299.00, 'google',   'Mumbai',    'male', '25-34'),
    (4, 'pb-003', 4500.00, 'google',   'Pune',      'male', '25-34'),
    (5, 'pb-004', 2999.00, 'direct',   'Hyderabad', 'male', '25-34'),
    (6, 'pb-005', 1799.00, 'referral', 'Chennai',   'male', '18-24')
) AS p(rn, pid, price, ch, city, gender, age);

-- ─────────────────────────────────────────────────────────────
-- DAILY ORDERS — Store C
-- ─────────────────────────────────────────────────────────────
INSERT INTO dummy_daily_orders (order_id, store_id, product_id, revenue, order_date, channel, customer_city, customer_gender, age_group)
SELECT
    'ord-c-' || gs.day || '-' || p.rn,
    'store-c',
    p.pid,
    p.price,
    CURRENT_DATE - (gs.day || ' days')::INTERVAL,
    p.ch,
    p.city,
    p.gender,
    p.age
FROM generate_series(0, 29) AS gs(day)
CROSS JOIN (VALUES
    (1, 'pc-001', 799.00,  'direct',    'Delhi',     'female', '25-34'),
    (2, 'pc-001', 799.00,  'google',    'Mumbai',    'female', '35-44'),
    (3, 'pc-002', 599.00,  'direct',    'Bangalore', 'female', '25-34'),
    (4, 'pc-004', 699.00,  'instagram', 'Pune',      'female', '25-34'),
    (5, 'pc-005', 549.00,  'direct',    'Chennai',   'female', '35-44')
) AS p(rn, pid, price, ch, city, gender, age);
