-- =============================================
-- V4: Seed Test Data
-- Creates: 3 test accounts, categories, brands, 10 products with images
-- =============================================

-- =============================================
-- 0. ADD REGION COLUMNS TO USERS (if not exist)
-- =============================================
ALTER TABLE users ADD COLUMN IF NOT EXISTS region_code VARCHAR(2) DEFAULT 'US';
ALTER TABLE users ADD COLUMN IF NOT EXISTS preferred_currency VARCHAR(3);
ALTER TABLE users ADD COLUMN IF NOT EXISTS locale VARCHAR(10) DEFAULT 'en-US';

-- =============================================
-- 1. TEST USER ACCOUNTS
-- Password: Test@123 (BCrypt hash)
-- =============================================

-- Get role IDs
DO $$
DECLARE
    admin_role_id UUID;
    customer_role_id UUID;
    staff_role_id UUID;
BEGIN
    SELECT id INTO admin_role_id FROM roles WHERE name = 'ROLE_ADMIN' LIMIT 1;
    SELECT id INTO customer_role_id FROM roles WHERE name = 'ROLE_CUSTOMER' LIMIT 1;
    SELECT id INTO staff_role_id FROM roles WHERE name = 'ROLE_STAFF' LIMIT 1;

    -- Admin Account
    INSERT INTO users (id, role_id, email, password_hash, first_name, last_name, phone, is_email_verified, is_active, region_code, locale)
    VALUES (
        'a0000000-0000-0000-0000-000000000001',
        admin_role_id,
        'admin@foalrider.com',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGd6LXovGpKq2k0LIvXYDOQHpCYq', -- Test@123
        'Admin',
        'User',
        '+1234567890',
        TRUE,
        TRUE,
        'US',
        'en-US'
    ) ON CONFLICT (email) DO NOTHING;

    -- Customer Account
    INSERT INTO users (id, role_id, email, password_hash, first_name, last_name, phone, is_email_verified, is_active, region_code, locale)
    VALUES (
        'c0000000-0000-0000-0000-000000000001',
        customer_role_id,
        'customer@foalrider.com',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGd6LXovGpKq2k0LIvXYDOQHpCYq', -- Test@123
        'John',
        'Customer',
        '+1987654321',
        TRUE,
        TRUE,
        'US',
        'en-US'
    ) ON CONFLICT (email) DO NOTHING;

    -- Vendor/Staff Account
    INSERT INTO users (id, role_id, email, password_hash, first_name, last_name, phone, is_email_verified, is_active, region_code, locale)
    VALUES (
        'v0000000-0000-0000-0000-000000000001',
        staff_role_id,
        'vendor@foalrider.com',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGd6LXovGpKq2k0LIvXYDOQHpCYq', -- Test@123
        'Fashion',
        'Vendor',
        '+1555666777',
        TRUE,
        TRUE,
        'US',
        'en-US'
    ) ON CONFLICT (email) DO NOTHING;
END $$;

-- =============================================
-- 2. BRANDS
-- =============================================
INSERT INTO brands (id, name, slug, description, logo_url, is_active, is_featured, created_at, updated_at) VALUES
    ('b0000000-0000-0000-0000-000000000001', 'FoalRider', 'foalrider', 'Premium fashion brand with modern designs', 'https://ui-avatars.com/api/?name=FoalRider&background=000&color=fff&size=200', TRUE, TRUE, NOW(), NOW()),
    ('b0000000-0000-0000-0000-000000000002', 'UrbanStyle', 'urbanstyle', 'Modern urban fashion for the city life', 'https://ui-avatars.com/api/?name=UrbanStyle&background=2563eb&color=fff&size=200', TRUE, TRUE, NOW(), NOW()),
    ('b0000000-0000-0000-0000-000000000003', 'ClassicWear', 'classicwear', 'Timeless classic fashion pieces', 'https://ui-avatars.com/api/?name=ClassicWear&background=dc2626&color=fff&size=200', TRUE, FALSE, NOW(), NOW())
ON CONFLICT (slug) DO NOTHING;

-- =============================================
-- 3. CATEGORIES
-- =============================================
-- Root Categories
INSERT INTO categories (id, name, slug, description, image_url, parent_id, display_order, is_active, is_featured, created_at, updated_at) VALUES
    ('ca000000-0000-0000-0000-000000000001', 'Men''s Fashion', 'mens-fashion', 'Men''s clothing and accessories', 'https://images.unsplash.com/photo-1490578474895-699cd4e2cf59?w=800', NULL, 1, TRUE, TRUE, NOW(), NOW()),
    ('ca000000-0000-0000-0000-000000000002', 'Women''s Fashion', 'womens-fashion', 'Women''s clothing and accessories', 'https://images.unsplash.com/photo-1483985988355-763728e1935b?w=800', NULL, 2, TRUE, TRUE, NOW(), NOW()),
    ('ca000000-0000-0000-0000-000000000003', 'Kids'' Fashion', 'kids-fashion', 'Children''s clothing and accessories', 'https://images.unsplash.com/photo-1503919545889-aef636e10ad4?w=800', NULL, 3, TRUE, TRUE, NOW(), NOW())
ON CONFLICT (slug) DO NOTHING;

-- Men's Subcategories
INSERT INTO categories (id, name, slug, description, image_url, parent_id, display_order, is_active, is_featured, created_at, updated_at) VALUES
    ('ca000000-0000-0000-0001-000000000001', 'Shirts', 'mens-shirts', 'Men''s shirts and tops', 'https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=800', 'ca000000-0000-0000-0000-000000000001', 1, TRUE, FALSE, NOW(), NOW()),
    ('ca000000-0000-0000-0001-000000000002', 'Pants', 'mens-pants', 'Men''s pants and trousers', 'https://images.unsplash.com/photo-1624378439575-d8705ad7ae80?w=800', 'ca000000-0000-0000-0000-000000000001', 2, TRUE, FALSE, NOW(), NOW()),
    ('ca000000-0000-0000-0001-000000000003', 'Jackets', 'mens-jackets', 'Men''s jackets and outerwear', 'https://images.unsplash.com/photo-1551028719-00167b16eac5?w=800', 'ca000000-0000-0000-0000-000000000001', 3, TRUE, FALSE, NOW(), NOW())
ON CONFLICT (slug) DO NOTHING;

-- Women's Subcategories
INSERT INTO categories (id, name, slug, description, image_url, parent_id, display_order, is_active, is_featured, created_at, updated_at) VALUES
    ('ca000000-0000-0000-0002-000000000001', 'Dresses', 'womens-dresses', 'Women''s dresses for all occasions', 'https://images.unsplash.com/photo-1595777457583-95e059d581b8?w=800', 'ca000000-0000-0000-0000-000000000002', 1, TRUE, FALSE, NOW(), NOW()),
    ('ca000000-0000-0000-0002-000000000002', 'Tops', 'womens-tops', 'Women''s tops and blouses', 'https://images.unsplash.com/photo-1564257631407-4deb1f99d992?w=800', 'ca000000-0000-0000-0000-000000000002', 2, TRUE, FALSE, NOW(), NOW()),
    ('ca000000-0000-0000-0002-000000000003', 'Skirts', 'womens-skirts', 'Women''s skirts', 'https://images.unsplash.com/photo-1592301933927-35b597393c0a?w=800', 'ca000000-0000-0000-0000-000000000002', 3, TRUE, FALSE, NOW(), NOW())
ON CONFLICT (slug) DO NOTHING;

-- Kids' Subcategories
INSERT INTO categories (id, name, slug, description, image_url, parent_id, display_order, is_active, is_featured, created_at, updated_at) VALUES
    ('ca000000-0000-0000-0003-000000000001', 'Boys', 'kids-boys', 'Boys'' clothing', 'https://images.unsplash.com/photo-1519238263530-99bdd11df2ea?w=800', 'ca000000-0000-0000-0000-000000000003', 1, TRUE, FALSE, NOW(), NOW()),
    ('ca000000-0000-0000-0003-000000000002', 'Girls', 'kids-girls', 'Girls'' clothing', 'https://images.unsplash.com/photo-1518831959646-742c3a14ebf7?w=800', 'ca000000-0000-0000-0000-000000000003', 2, TRUE, FALSE, NOW(), NOW())
ON CONFLICT (slug) DO NOTHING;

-- =============================================
-- 4. PRODUCTS (10 Products)
-- =============================================

-- Product 1: Men's Classic White Shirt
INSERT INTO products (id, name, slug, sku, short_description, description, base_price, sale_price, category_id, brand_id, tags, is_active, is_featured, is_new, weight, weight_unit, meta_title, meta_description, view_count, sold_count, avg_rating, review_count, created_at, updated_at) VALUES
('p0000000-0000-0000-0000-000000000001', 'Classic White Oxford Shirt', 'classic-white-oxford-shirt', 'MENS-SHIRT-001', 
'A timeless white Oxford shirt perfect for any occasion', 
'Premium cotton Oxford shirt with button-down collar. Features a regular fit, single chest pocket, and rounded hem. Perfect for both casual and formal occasions. Made from 100% Egyptian cotton for ultimate comfort and durability.',
79.99, 59.99, 'ca000000-0000-0000-0001-000000000001', 'b0000000-0000-0000-0000-000000000003',
'["formal", "casual", "office", "cotton"]', TRUE, TRUE, TRUE, 0.30, 'kg', 
'Classic White Oxford Shirt | FoalRider', 'Premium cotton Oxford shirt perfect for any occasion', 0, 0, 0, 0, NOW(), NOW())
ON CONFLICT (sku) DO NOTHING;

-- Product 2: Men's Slim Fit Chinos
INSERT INTO products (id, name, slug, sku, short_description, description, base_price, sale_price, category_id, brand_id, tags, is_active, is_featured, is_new, weight, weight_unit, meta_title, meta_description, view_count, sold_count, avg_rating, review_count, created_at, updated_at) VALUES
('p0000000-0000-0000-0000-000000000002', 'Slim Fit Chino Pants', 'slim-fit-chino-pants', 'MENS-PANTS-001',
'Comfortable slim-fit chinos for everyday wear',
'These versatile chinos feature a slim fit with stretch fabric for comfort. Made from premium cotton-elastane blend with a soft hand feel. Features front slant pockets and rear welt pockets. Perfect for office or weekend wear.',
89.99, NULL, 'ca000000-0000-0000-0001-000000000002', 'b0000000-0000-0000-0000-000000000002',
'["casual", "office", "everyday", "stretch"]', TRUE, TRUE, FALSE, 0.45, 'kg',
'Slim Fit Chino Pants | FoalRider', 'Comfortable slim-fit chinos for everyday wear', 0, 0, 0, 0, NOW(), NOW())
ON CONFLICT (sku) DO NOTHING;

-- Product 3: Men's Leather Bomber Jacket
INSERT INTO products (id, name, slug, sku, short_description, description, base_price, sale_price, category_id, brand_id, tags, is_active, is_featured, is_new, weight, weight_unit, meta_title, meta_description, view_count, sold_count, avg_rating, review_count, created_at, updated_at) VALUES
('p0000000-0000-0000-0000-000000000003', 'Premium Leather Bomber Jacket', 'premium-leather-bomber-jacket', 'MENS-JACKET-001',
'Stylish leather bomber jacket with classic design',
'Genuine leather bomber jacket with ribbed cuffs and hem. Features a front zipper closure, two side pockets, and one interior pocket. Lined with soft polyester for comfort. A timeless piece that gets better with age.',
299.99, 249.99, 'ca000000-0000-0000-0001-000000000003', 'b0000000-0000-0000-0000-000000000001',
'["premium", "leather", "winter", "classic"]', TRUE, TRUE, TRUE, 1.20, 'kg',
'Premium Leather Bomber Jacket | FoalRider', 'Genuine leather bomber jacket with classic design', 0, 0, 0, 0, NOW(), NOW())
ON CONFLICT (sku) DO NOTHING;

-- Product 4: Women's Floral Summer Dress
INSERT INTO products (id, name, slug, sku, short_description, description, base_price, sale_price, category_id, brand_id, tags, is_active, is_featured, is_new, weight, weight_unit, meta_title, meta_description, view_count, sold_count, avg_rating, review_count, created_at, updated_at) VALUES
('p0000000-0000-0000-0000-000000000004', 'Floral Print Summer Dress', 'floral-print-summer-dress', 'WOMENS-DRESS-001',
'Beautiful floral dress perfect for summer days',
'Lightweight summer dress with beautiful floral print. Features a V-neckline, adjustable spaghetti straps, and a flowing A-line silhouette. Made from breathable viscose fabric. Perfect for beach days, garden parties, or casual outings.',
129.99, 89.99, 'ca000000-0000-0000-0002-000000000001', 'b0000000-0000-0000-0000-000000000001',
'["summer", "floral", "casual", "beach"]', TRUE, TRUE, TRUE, 0.25, 'kg',
'Floral Print Summer Dress | FoalRider', 'Beautiful floral dress perfect for summer days', 0, 0, 0, 0, NOW(), NOW())
ON CONFLICT (sku) DO NOTHING;

-- Product 5: Women's Silk Blouse
INSERT INTO products (id, name, slug, sku, short_description, description, base_price, sale_price, category_id, brand_id, tags, is_active, is_featured, is_new, weight, weight_unit, meta_title, meta_description, view_count, sold_count, avg_rating, review_count, created_at, updated_at) VALUES
('p0000000-0000-0000-0000-000000000005', 'Elegant Silk Blouse', 'elegant-silk-blouse', 'WOMENS-TOP-001',
'Luxurious silk blouse for sophisticated style',
'100% mulberry silk blouse with a relaxed fit. Features a classic collar, button-front closure, and French cuffs. The natural silk fabric provides a beautiful drape and soft feel. Dry clean recommended.',
159.99, NULL, 'ca000000-0000-0000-0002-000000000002', 'b0000000-0000-0000-0000-000000000003',
'["formal", "elegant", "silk", "luxury"]', TRUE, TRUE, FALSE, 0.15, 'kg',
'Elegant Silk Blouse | FoalRider', 'Luxurious silk blouse for sophisticated style', 0, 0, 0, 0, NOW(), NOW())
ON CONFLICT (sku) DO NOTHING;

-- Product 6: Women's Pleated Midi Skirt
INSERT INTO products (id, name, slug, sku, short_description, description, base_price, sale_price, category_id, brand_id, tags, is_active, is_featured, is_new, weight, weight_unit, meta_title, meta_description, view_count, sold_count, avg_rating, review_count, created_at, updated_at) VALUES
('p0000000-0000-0000-0000-000000000006', 'Pleated Midi Skirt', 'pleated-midi-skirt', 'WOMENS-SKIRT-001',
'Elegant pleated skirt for a feminine look',
'Classic pleated midi skirt with an elastic waistband. Made from lightweight polyester with a beautiful drape. Perfect for both casual and dressy occasions. Pairs beautifully with blouses, sweaters, or fitted tops.',
79.99, 64.99, 'ca000000-0000-0000-0002-000000000003', 'b0000000-0000-0000-0000-000000000002',
'["elegant", "midi", "pleated", "versatile"]', TRUE, FALSE, TRUE, 0.30, 'kg',
'Pleated Midi Skirt | FoalRider', 'Elegant pleated skirt for a feminine look', 0, 0, 0, 0, NOW(), NOW())
ON CONFLICT (sku) DO NOTHING;

-- Product 7: Boys' Graphic T-Shirt
INSERT INTO products (id, name, slug, sku, short_description, description, base_price, sale_price, category_id, brand_id, tags, is_active, is_featured, is_new, weight, weight_unit, meta_title, meta_description, view_count, sold_count, avg_rating, review_count, created_at, updated_at) VALUES
('p0000000-0000-0000-0000-000000000007', 'Boys Cool Graphic Tee', 'boys-cool-graphic-tee', 'KIDS-BOYS-001',
'Fun graphic t-shirt for active boys',
'Comfortable cotton t-shirt featuring a cool dinosaur graphic. Made from 100% organic cotton for softness and breathability. Machine washable and durable for everyday adventures. Available in multiple sizes.',
29.99, 24.99, 'ca000000-0000-0000-0003-000000000001', 'b0000000-0000-0000-0000-000000000001',
'["casual", "graphic", "cotton", "kids"]', TRUE, FALSE, TRUE, 0.15, 'kg',
'Boys Cool Graphic Tee | FoalRider', 'Fun graphic t-shirt for active boys', 0, 0, 0, 0, NOW(), NOW())
ON CONFLICT (sku) DO NOTHING;

-- Product 8: Boys' Denim Jeans
INSERT INTO products (id, name, slug, sku, short_description, description, base_price, sale_price, category_id, brand_id, tags, is_active, is_featured, is_new, weight, weight_unit, meta_title, meta_description, view_count, sold_count, avg_rating, review_count, created_at, updated_at) VALUES
('p0000000-0000-0000-0000-000000000008', 'Boys Classic Denim Jeans', 'boys-classic-denim-jeans', 'KIDS-BOYS-002',
'Durable denim jeans for everyday wear',
'Classic fit denim jeans with adjustable waistband. Features reinforced knees for extra durability. Made from soft stretch denim for comfort and ease of movement. Perfect for school, play, or any occasion.',
49.99, NULL, 'ca000000-0000-0000-0003-000000000001', 'b0000000-0000-0000-0000-000000000002',
'["denim", "casual", "durable", "kids"]', TRUE, FALSE, FALSE, 0.35, 'kg',
'Boys Classic Denim Jeans | FoalRider', 'Durable denim jeans for everyday wear', 0, 0, 0, 0, NOW(), NOW())
ON CONFLICT (sku) DO NOTHING;

-- Product 9: Girls' Princess Dress
INSERT INTO products (id, name, slug, sku, short_description, description, base_price, sale_price, category_id, brand_id, tags, is_active, is_featured, is_new, weight, weight_unit, meta_title, meta_description, view_count, sold_count, avg_rating, review_count, created_at, updated_at) VALUES
('p0000000-0000-0000-0000-000000000009', 'Girls Sparkle Princess Dress', 'girls-sparkle-princess-dress', 'KIDS-GIRLS-001',
'Magical princess dress for special occasions',
'Beautiful princess dress with sparkle tulle overlay. Features a satin bodice with sequin details and a full tulle skirt. Perfect for parties, holidays, and special occasions. Hand wash recommended.',
69.99, 54.99, 'ca000000-0000-0000-0003-000000000002', 'b0000000-0000-0000-0000-000000000001',
'["party", "princess", "sparkle", "special"]', TRUE, TRUE, TRUE, 0.40, 'kg',
'Girls Sparkle Princess Dress | FoalRider', 'Magical princess dress for special occasions', 0, 0, 0, 0, NOW(), NOW())
ON CONFLICT (sku) DO NOTHING;

-- Product 10: Girls' Floral Leggings Set
INSERT INTO products (id, name, slug, sku, short_description, description, base_price, sale_price, category_id, brand_id, tags, is_active, is_featured, is_new, weight, weight_unit, meta_title, meta_description, view_count, sold_count, avg_rating, review_count, created_at, updated_at) VALUES
('p0000000-0000-0000-0000-000000000010', 'Girls Floral Leggings Set', 'girls-floral-leggings-set', 'KIDS-GIRLS-002',
'Cute matching top and leggings set',
'Adorable two-piece set featuring a floral print top and matching solid leggings. Made from soft cotton-spandex blend for comfort and stretch. Easy to mix and match with other pieces. Machine washable.',
39.99, 34.99, 'ca000000-0000-0000-0003-000000000002', 'b0000000-0000-0000-0000-000000000002',
'["set", "floral", "comfortable", "kids"]', TRUE, FALSE, TRUE, 0.25, 'kg',
'Girls Floral Leggings Set | FoalRider', 'Cute matching top and leggings set', 0, 0, 0, 0, NOW(), NOW())
ON CONFLICT (sku) DO NOTHING;

-- =============================================
-- 5. PRODUCT IMAGES (3 images per product)
-- =============================================

-- Product 1 Images: Men's Classic White Shirt
INSERT INTO product_images (id, product_id, url, alt_text, display_order, is_primary, created_at, updated_at) VALUES
('pi000000-0001-0000-0000-000000000001', 'p0000000-0000-0000-0000-000000000001', 'https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=800', 'Classic White Oxford Shirt - Front View', 0, TRUE, NOW(), NOW()),
('pi000000-0001-0000-0000-000000000002', 'p0000000-0000-0000-0000-000000000001', 'https://images.unsplash.com/photo-1598033129183-c4f50c736f10?w=800', 'Classic White Oxford Shirt - Side View', 1, FALSE, NOW(), NOW()),
('pi000000-0001-0000-0000-000000000003', 'p0000000-0000-0000-0000-000000000001', 'https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?w=800', 'Classic White Oxford Shirt - Detail', 2, FALSE, NOW(), NOW())
ON CONFLICT DO NOTHING;

-- Product 2 Images: Men's Slim Fit Chinos
INSERT INTO product_images (id, product_id, url, alt_text, display_order, is_primary, created_at, updated_at) VALUES
('pi000000-0002-0000-0000-000000000001', 'p0000000-0000-0000-0000-000000000002', 'https://images.unsplash.com/photo-1624378439575-d8705ad7ae80?w=800', 'Slim Fit Chino Pants - Front View', 0, TRUE, NOW(), NOW()),
('pi000000-0002-0000-0000-000000000002', 'p0000000-0000-0000-0000-000000000002', 'https://images.unsplash.com/photo-1473966968600-fa801b869a1a?w=800', 'Slim Fit Chino Pants - Style View', 1, FALSE, NOW(), NOW()),
('pi000000-0002-0000-0000-000000000003', 'p0000000-0000-0000-0000-000000000002', 'https://images.unsplash.com/photo-1542272604-787c3835535d?w=800', 'Slim Fit Chino Pants - Detail', 2, FALSE, NOW(), NOW())
ON CONFLICT DO NOTHING;

-- Product 3 Images: Men's Leather Bomber Jacket
INSERT INTO product_images (id, product_id, url, alt_text, display_order, is_primary, created_at, updated_at) VALUES
('pi000000-0003-0000-0000-000000000001', 'p0000000-0000-0000-0000-000000000003', 'https://images.unsplash.com/photo-1551028719-00167b16eac5?w=800', 'Premium Leather Bomber Jacket - Front', 0, TRUE, NOW(), NOW()),
('pi000000-0003-0000-0000-000000000002', 'p0000000-0000-0000-0000-000000000003', 'https://images.unsplash.com/photo-1520975954732-35dd22299614?w=800', 'Premium Leather Bomber Jacket - Back', 1, FALSE, NOW(), NOW()),
('pi000000-0003-0000-0000-000000000003', 'p0000000-0000-0000-0000-000000000003', 'https://images.unsplash.com/photo-1559551409-dadc959f76b8?w=800', 'Premium Leather Bomber Jacket - Detail', 2, FALSE, NOW(), NOW())
ON CONFLICT DO NOTHING;

-- Product 4 Images: Women's Floral Summer Dress
INSERT INTO product_images (id, product_id, url, alt_text, display_order, is_primary, created_at, updated_at) VALUES
('pi000000-0004-0000-0000-000000000001', 'p0000000-0000-0000-0000-000000000004', 'https://images.unsplash.com/photo-1595777457583-95e059d581b8?w=800', 'Floral Print Summer Dress - Front', 0, TRUE, NOW(), NOW()),
('pi000000-0004-0000-0000-000000000002', 'p0000000-0000-0000-0000-000000000004', 'https://images.unsplash.com/photo-1572804013309-59a88b7e92f1?w=800', 'Floral Print Summer Dress - Side', 1, FALSE, NOW(), NOW()),
('pi000000-0004-0000-0000-000000000003', 'p0000000-0000-0000-0000-000000000004', 'https://images.unsplash.com/photo-1515372039744-b8f02a3ae446?w=800', 'Floral Print Summer Dress - Back', 2, FALSE, NOW(), NOW())
ON CONFLICT DO NOTHING;

-- Product 5 Images: Women's Silk Blouse
INSERT INTO product_images (id, product_id, url, alt_text, display_order, is_primary, created_at, updated_at) VALUES
('pi000000-0005-0000-0000-000000000001', 'p0000000-0000-0000-0000-000000000005', 'https://images.unsplash.com/photo-1564257631407-4deb1f99d992?w=800', 'Elegant Silk Blouse - Front', 0, TRUE, NOW(), NOW()),
('pi000000-0005-0000-0000-000000000002', 'p0000000-0000-0000-0000-000000000005', 'https://images.unsplash.com/photo-1551163943-3f6a855d1153?w=800', 'Elegant Silk Blouse - Style', 1, FALSE, NOW(), NOW()),
('pi000000-0005-0000-0000-000000000003', 'p0000000-0000-0000-0000-000000000005', 'https://images.unsplash.com/photo-1485462537746-965f33f7f6a7?w=800', 'Elegant Silk Blouse - Detail', 2, FALSE, NOW(), NOW())
ON CONFLICT DO NOTHING;

-- Product 6 Images: Women's Pleated Midi Skirt
INSERT INTO product_images (id, product_id, url, alt_text, display_order, is_primary, created_at, updated_at) VALUES
('pi000000-0006-0000-0000-000000000001', 'p0000000-0000-0000-0000-000000000006', 'https://images.unsplash.com/photo-1592301933927-35b597393c0a?w=800', 'Pleated Midi Skirt - Front', 0, TRUE, NOW(), NOW()),
('pi000000-0006-0000-0000-000000000002', 'p0000000-0000-0000-0000-000000000006', 'https://images.unsplash.com/photo-1583496661160-fb5886a0aaae?w=800', 'Pleated Midi Skirt - Side', 1, FALSE, NOW(), NOW()),
('pi000000-0006-0000-0000-000000000003', 'p0000000-0000-0000-0000-000000000006', 'https://images.unsplash.com/photo-1577900232427-18219b9166a0?w=800', 'Pleated Midi Skirt - Detail', 2, FALSE, NOW(), NOW())
ON CONFLICT DO NOTHING;

-- Product 7 Images: Boys' Graphic T-Shirt
INSERT INTO product_images (id, product_id, url, alt_text, display_order, is_primary, created_at, updated_at) VALUES
('pi000000-0007-0000-0000-000000000001', 'p0000000-0000-0000-0000-000000000007', 'https://images.unsplash.com/photo-1519238263530-99bdd11df2ea?w=800', 'Boys Cool Graphic Tee - Front', 0, TRUE, NOW(), NOW()),
('pi000000-0007-0000-0000-000000000002', 'p0000000-0000-0000-0000-000000000007', 'https://images.unsplash.com/photo-1503919889273-c4e74afd0df8?w=800', 'Boys Cool Graphic Tee - Style', 1, FALSE, NOW(), NOW()),
('pi000000-0007-0000-0000-000000000003', 'p0000000-0000-0000-0000-000000000007', 'https://images.unsplash.com/photo-1471286174890-9c112ffca5b4?w=800', 'Boys Cool Graphic Tee - Back', 2, FALSE, NOW(), NOW())
ON CONFLICT DO NOTHING;

-- Product 8 Images: Boys' Denim Jeans
INSERT INTO product_images (id, product_id, url, alt_text, display_order, is_primary, created_at, updated_at) VALUES
('pi000000-0008-0000-0000-000000000001', 'p0000000-0000-0000-0000-000000000008', 'https://images.unsplash.com/photo-1473966968600-fa801b869a1a?w=800', 'Boys Classic Denim Jeans - Front', 0, TRUE, NOW(), NOW()),
('pi000000-0008-0000-0000-000000000002', 'p0000000-0000-0000-0000-000000000008', 'https://images.unsplash.com/photo-1565084888279-aca607ecce0c?w=800', 'Boys Classic Denim Jeans - Style', 1, FALSE, NOW(), NOW()),
('pi000000-0008-0000-0000-000000000003', 'p0000000-0000-0000-0000-000000000008', 'https://images.unsplash.com/photo-1604176354204-9268737828e4?w=800', 'Boys Classic Denim Jeans - Detail', 2, FALSE, NOW(), NOW())
ON CONFLICT DO NOTHING;

-- Product 9 Images: Girls' Princess Dress
INSERT INTO product_images (id, product_id, url, alt_text, display_order, is_primary, created_at, updated_at) VALUES
('pi000000-0009-0000-0000-000000000001', 'p0000000-0000-0000-0000-000000000009', 'https://images.unsplash.com/photo-1518831959646-742c3a14ebf7?w=800', 'Girls Sparkle Princess Dress - Front', 0, TRUE, NOW(), NOW()),
('pi000000-0009-0000-0000-000000000002', 'p0000000-0000-0000-0000-000000000009', 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=800', 'Girls Sparkle Princess Dress - Style', 1, FALSE, NOW(), NOW()),
('pi000000-0009-0000-0000-000000000003', 'p0000000-0000-0000-0000-000000000009', 'https://images.unsplash.com/photo-1494578379344-d6c710782a3d?w=800', 'Girls Sparkle Princess Dress - Detail', 2, FALSE, NOW(), NOW())
ON CONFLICT DO NOTHING;

-- Product 10 Images: Girls' Floral Leggings Set
INSERT INTO product_images (id, product_id, url, alt_text, display_order, is_primary, created_at, updated_at) VALUES
('pi000000-0010-0000-0000-000000000001', 'p0000000-0000-0000-0000-000000000010', 'https://images.unsplash.com/photo-1476234251651-f353703a034d?w=800', 'Girls Floral Leggings Set - Front', 0, TRUE, NOW(), NOW()),
('pi000000-0010-0000-0000-000000000002', 'p0000000-0000-0000-0000-000000000010', 'https://images.unsplash.com/photo-1519457431-44ccd64a579b?w=800', 'Girls Floral Leggings Set - Style', 1, FALSE, NOW(), NOW()),
('pi000000-0010-0000-0000-000000000003', 'p0000000-0000-0000-0000-000000000010', 'https://images.unsplash.com/photo-1522771739844-6a9f6d5f14af?w=800', 'Girls Floral Leggings Set - Detail', 2, FALSE, NOW(), NOW())
ON CONFLICT DO NOTHING;

-- =============================================
-- 6. PRODUCT VARIANTS (Sizes for each product)
-- =============================================

-- Product 1 Variants: Men's Shirt (S, M, L, XL)
INSERT INTO product_variants (id, product_id, sku, name, attributes, price_adjustment, stock_quantity, low_stock_threshold, is_active, created_at, updated_at) VALUES
('pv000000-0001-0000-0000-000000000001', 'p0000000-0000-0000-0000-000000000001', 'MENS-SHIRT-001-S', 'Small', '{"size": "S"}', 0, 50, 10, TRUE, NOW(), NOW()),
('pv000000-0001-0000-0000-000000000002', 'p0000000-0000-0000-0000-000000000001', 'MENS-SHIRT-001-M', 'Medium', '{"size": "M"}', 0, 75, 10, TRUE, NOW(), NOW()),
('pv000000-0001-0000-0000-000000000003', 'p0000000-0000-0000-0000-000000000001', 'MENS-SHIRT-001-L', 'Large', '{"size": "L"}', 0, 60, 10, TRUE, NOW(), NOW()),
('pv000000-0001-0000-0000-000000000004', 'p0000000-0000-0000-0000-000000000001', 'MENS-SHIRT-001-XL', 'Extra Large', '{"size": "XL"}', 5.00, 40, 10, TRUE, NOW(), NOW())
ON CONFLICT (sku) DO NOTHING;

-- Product 2 Variants: Men's Chinos (30, 32, 34, 36)
INSERT INTO product_variants (id, product_id, sku, name, attributes, price_adjustment, stock_quantity, low_stock_threshold, is_active, created_at, updated_at) VALUES
('pv000000-0002-0000-0000-000000000001', 'p0000000-0000-0000-0000-000000000002', 'MENS-PANTS-001-30', 'Waist 30', '{"size": "30"}', 0, 45, 10, TRUE, NOW(), NOW()),
('pv000000-0002-0000-0000-000000000002', 'p0000000-0000-0000-0000-000000000002', 'MENS-PANTS-001-32', 'Waist 32', '{"size": "32"}', 0, 80, 10, TRUE, NOW(), NOW()),
('pv000000-0002-0000-0000-000000000003', 'p0000000-0000-0000-0000-000000000002', 'MENS-PANTS-001-34', 'Waist 34', '{"size": "34"}', 0, 70, 10, TRUE, NOW(), NOW()),
('pv000000-0002-0000-0000-000000000004', 'p0000000-0000-0000-0000-000000000002', 'MENS-PANTS-001-36', 'Waist 36', '{"size": "36"}', 0, 35, 10, TRUE, NOW(), NOW())
ON CONFLICT (sku) DO NOTHING;

-- Product 3 Variants: Men's Jacket (S, M, L, XL)
INSERT INTO product_variants (id, product_id, sku, name, attributes, price_adjustment, stock_quantity, low_stock_threshold, is_active, created_at, updated_at) VALUES
('pv000000-0003-0000-0000-000000000001', 'p0000000-0000-0000-0000-000000000003', 'MENS-JACKET-001-S', 'Small', '{"size": "S"}', 0, 25, 5, TRUE, NOW(), NOW()),
('pv000000-0003-0000-0000-000000000002', 'p0000000-0000-0000-0000-000000000003', 'MENS-JACKET-001-M', 'Medium', '{"size": "M"}', 0, 40, 5, TRUE, NOW(), NOW()),
('pv000000-0003-0000-0000-000000000003', 'p0000000-0000-0000-0000-000000000003', 'MENS-JACKET-001-L', 'Large', '{"size": "L"}', 0, 35, 5, TRUE, NOW(), NOW()),
('pv000000-0003-0000-0000-000000000004', 'p0000000-0000-0000-0000-000000000003', 'MENS-JACKET-001-XL', 'Extra Large', '{"size": "XL"}', 10.00, 20, 5, TRUE, NOW(), NOW())
ON CONFLICT (sku) DO NOTHING;

-- Product 4 Variants: Women's Dress (XS, S, M, L)
INSERT INTO product_variants (id, product_id, sku, name, attributes, price_adjustment, stock_quantity, low_stock_threshold, is_active, created_at, updated_at) VALUES
('pv000000-0004-0000-0000-000000000001', 'p0000000-0000-0000-0000-000000000004', 'WOMENS-DRESS-001-XS', 'Extra Small', '{"size": "XS"}', 0, 30, 10, TRUE, NOW(), NOW()),
('pv000000-0004-0000-0000-000000000002', 'p0000000-0000-0000-0000-000000000004', 'WOMENS-DRESS-001-S', 'Small', '{"size": "S"}', 0, 55, 10, TRUE, NOW(), NOW()),
('pv000000-0004-0000-0000-000000000003', 'p0000000-0000-0000-0000-000000000004', 'WOMENS-DRESS-001-M', 'Medium', '{"size": "M"}', 0, 65, 10, TRUE, NOW(), NOW()),
('pv000000-0004-0000-0000-000000000004', 'p0000000-0000-0000-0000-000000000004', 'WOMENS-DRESS-001-L', 'Large', '{"size": "L"}', 0, 40, 10, TRUE, NOW(), NOW())
ON CONFLICT (sku) DO NOTHING;

-- Product 5 Variants: Women's Blouse (XS, S, M, L)
INSERT INTO product_variants (id, product_id, sku, name, attributes, price_adjustment, stock_quantity, low_stock_threshold, is_active, created_at, updated_at) VALUES
('pv000000-0005-0000-0000-000000000001', 'p0000000-0000-0000-0000-000000000005', 'WOMENS-TOP-001-XS', 'Extra Small', '{"size": "XS"}', 0, 25, 10, TRUE, NOW(), NOW()),
('pv000000-0005-0000-0000-000000000002', 'p0000000-0000-0000-0000-000000000005', 'WOMENS-TOP-001-S', 'Small', '{"size": "S"}', 0, 45, 10, TRUE, NOW(), NOW()),
('pv000000-0005-0000-0000-000000000003', 'p0000000-0000-0000-0000-000000000005', 'WOMENS-TOP-001-M', 'Medium', '{"size": "M"}', 0, 50, 10, TRUE, NOW(), NOW()),
('pv000000-0005-0000-0000-000000000004', 'p0000000-0000-0000-0000-000000000005', 'WOMENS-TOP-001-L', 'Large', '{"size": "L"}', 0, 35, 10, TRUE, NOW(), NOW())
ON CONFLICT (sku) DO NOTHING;

-- Product 6 Variants: Women's Skirt (XS, S, M, L)
INSERT INTO product_variants (id, product_id, sku, name, attributes, price_adjustment, stock_quantity, low_stock_threshold, is_active, created_at, updated_at) VALUES
('pv000000-0006-0000-0000-000000000001', 'p0000000-0000-0000-0000-000000000006', 'WOMENS-SKIRT-001-XS', 'Extra Small', '{"size": "XS"}', 0, 30, 10, TRUE, NOW(), NOW()),
('pv000000-0006-0000-0000-000000000002', 'p0000000-0000-0000-0000-000000000006', 'WOMENS-SKIRT-001-S', 'Small', '{"size": "S"}', 0, 50, 10, TRUE, NOW(), NOW()),
('pv000000-0006-0000-0000-000000000003', 'p0000000-0000-0000-0000-000000000006', 'WOMENS-SKIRT-001-M', 'Medium', '{"size": "M"}', 0, 55, 10, TRUE, NOW(), NOW()),
('pv000000-0006-0000-0000-000000000004', 'p0000000-0000-0000-0000-000000000006', 'WOMENS-SKIRT-001-L', 'Large', '{"size": "L"}', 0, 40, 10, TRUE, NOW(), NOW())
ON CONFLICT (sku) DO NOTHING;

-- Product 7 Variants: Boys' T-Shirt (4-5Y, 6-7Y, 8-9Y, 10-11Y)
INSERT INTO product_variants (id, product_id, sku, name, attributes, price_adjustment, stock_quantity, low_stock_threshold, is_active, created_at, updated_at) VALUES
('pv000000-0007-0000-0000-000000000001', 'p0000000-0000-0000-0000-000000000007', 'KIDS-BOYS-001-4-5Y', '4-5 Years', '{"size": "4-5Y"}', 0, 60, 15, TRUE, NOW(), NOW()),
('pv000000-0007-0000-0000-000000000002', 'p0000000-0000-0000-0000-000000000007', 'KIDS-BOYS-001-6-7Y', '6-7 Years', '{"size": "6-7Y"}', 0, 70, 15, TRUE, NOW(), NOW()),
('pv000000-0007-0000-0000-000000000003', 'p0000000-0000-0000-0000-000000000007', 'KIDS-BOYS-001-8-9Y', '8-9 Years', '{"size": "8-9Y"}', 0, 55, 15, TRUE, NOW(), NOW()),
('pv000000-0007-0000-0000-000000000004', 'p0000000-0000-0000-0000-000000000007', 'KIDS-BOYS-001-10-11Y', '10-11 Years', '{"size": "10-11Y"}', 2.00, 45, 15, TRUE, NOW(), NOW())
ON CONFLICT (sku) DO NOTHING;

-- Product 8 Variants: Boys' Jeans (4-5Y, 6-7Y, 8-9Y, 10-11Y)
INSERT INTO product_variants (id, product_id, sku, name, attributes, price_adjustment, stock_quantity, low_stock_threshold, is_active, created_at, updated_at) VALUES
('pv000000-0008-0000-0000-000000000001', 'p0000000-0000-0000-0000-000000000008', 'KIDS-BOYS-002-4-5Y', '4-5 Years', '{"size": "4-5Y"}', 0, 50, 15, TRUE, NOW(), NOW()),
('pv000000-0008-0000-0000-000000000002', 'p0000000-0000-0000-0000-000000000008', 'KIDS-BOYS-002-6-7Y', '6-7 Years', '{"size": "6-7Y"}', 0, 65, 15, TRUE, NOW(), NOW()),
('pv000000-0008-0000-0000-000000000003', 'p0000000-0000-0000-0000-000000000008', 'KIDS-BOYS-002-8-9Y', '8-9 Years', '{"size": "8-9Y"}', 0, 55, 15, TRUE, NOW(), NOW()),
('pv000000-0008-0000-0000-000000000004', 'p0000000-0000-0000-0000-000000000008', 'KIDS-BOYS-002-10-11Y', '10-11 Years', '{"size": "10-11Y"}', 5.00, 40, 15, TRUE, NOW(), NOW())
ON CONFLICT (sku) DO NOTHING;

-- Product 9 Variants: Girls' Dress (4-5Y, 6-7Y, 8-9Y, 10-11Y)
INSERT INTO product_variants (id, product_id, sku, name, attributes, price_adjustment, stock_quantity, low_stock_threshold, is_active, created_at, updated_at) VALUES
('pv000000-0009-0000-0000-000000000001', 'p0000000-0000-0000-0000-000000000009', 'KIDS-GIRLS-001-4-5Y', '4-5 Years', '{"size": "4-5Y"}', 0, 45, 15, TRUE, NOW(), NOW()),
('pv000000-0009-0000-0000-000000000002', 'p0000000-0000-0000-0000-000000000009', 'KIDS-GIRLS-001-6-7Y', '6-7 Years', '{"size": "6-7Y"}', 0, 60, 15, TRUE, NOW(), NOW()),
('pv000000-0009-0000-0000-000000000003', 'p0000000-0000-0000-0000-000000000009', 'KIDS-GIRLS-001-8-9Y', '8-9 Years', '{"size": "8-9Y"}', 0, 50, 15, TRUE, NOW(), NOW()),
('pv000000-0009-0000-0000-000000000004', 'p0000000-0000-0000-0000-000000000009', 'KIDS-GIRLS-001-10-11Y', '10-11 Years', '{"size": "10-11Y"}', 5.00, 35, 15, TRUE, NOW(), NOW())
ON CONFLICT (sku) DO NOTHING;

-- Product 10 Variants: Girls' Leggings Set (4-5Y, 6-7Y, 8-9Y, 10-11Y)
INSERT INTO product_variants (id, product_id, sku, name, attributes, price_adjustment, stock_quantity, low_stock_threshold, is_active, created_at, updated_at) VALUES
('pv000000-0010-0000-0000-000000000001', 'p0000000-0000-0000-0000-000000000010', 'KIDS-GIRLS-002-4-5Y', '4-5 Years', '{"size": "4-5Y"}', 0, 55, 15, TRUE, NOW(), NOW()),
('pv000000-0010-0000-0000-000000000002', 'p0000000-0000-0000-0000-000000000010', 'KIDS-GIRLS-002-6-7Y', '6-7 Years', '{"size": "6-7Y"}', 0, 65, 15, TRUE, NOW(), NOW()),
('pv000000-0010-0000-0000-000000000003', 'p0000000-0000-0000-0000-000000000010', 'KIDS-GIRLS-002-8-9Y', '8-9 Years', '{"size": "8-9Y"}', 0, 50, 15, TRUE, NOW(), NOW()),
('pv000000-0010-0000-0000-000000000004', 'p0000000-0000-0000-0000-000000000010', 'KIDS-GIRLS-002-10-11Y', '10-11 Years', '{"size": "10-11Y"}', 3.00, 40, 15, TRUE, NOW(), NOW())
ON CONFLICT (sku) DO NOTHING;

-- =============================================
-- SUMMARY
-- =============================================
-- Test Accounts Created:
--   1. admin@foalrider.com (ADMIN) - Password: Test@123
--   2. customer@foalrider.com (CUSTOMER) - Password: Test@123
--   3. vendor@foalrider.com (STAFF) - Password: Test@123
--
-- Categories: 3 root + 8 sub = 11 total
-- Brands: 3
-- Products: 10 (Men's: 3, Women's: 3, Kids: 4)
-- Product Images: 30 (3 per product)
-- Product Variants: 40 (4 sizes per product)
-- =============================================
