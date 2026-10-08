-- cat-maintain 精简演示数据，版本 demo-v2。
-- 默认仅补充缺失的基础数据，不恢复库存或覆盖账号密码。
-- 显式设置 @reset_demo_data = 1 时，清空 9 张业务表并重建基础数据。
-- 重置保留表结构和自增序列，旧会话不会因 ID 复用获得新账号权限。
-- 调用前在同一会话设置 @demo_password_hash，推荐使用 deploy/seed-demo.sh。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET SESSION time_zone = '+08:00';

DROP PROCEDURE IF EXISTS cm_seed_demo_v2;
DELIMITER $$
CREATE PROCEDURE cm_seed_demo_v2()
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    IF DATABASE() IS NULL OR DATABASE() <> 'cat_maintain' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Select cat_maintain before importing demo data';
    END IF;
    IF @demo_password_hash IS NULL
       OR @demo_password_hash NOT REGEXP '^[$]2[aby][$][0-9]{2}[$][./A-Za-z0-9]{53}$' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Provide a valid BCrypt hash in @demo_password_hash';
    END IF;
    IF COALESCE(@reset_demo_data, 0) NOT IN (0, 1) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '@reset_demo_data must be 0 or 1';
    END IF;

    START TRANSACTION;

    IF COALESCE(@reset_demo_data, 0) = 1 THEN
        -- 按外键依赖顺序删除；任一步失败都回滚，禁止关闭外键检查。
        DELETE FROM product_reviews;
        DELETE FROM reviews;
        DELETE FROM maintenance_records;
        DELETE FROM appointments;
        DELETE FROM order_items;
        DELETE FROM orders;
        DELETE FROM merchant_stores;
        DELETE FROM accounts;
        DELETE FROM products;
    END IF;

    IF EXISTS (
        SELECT 1 FROM accounts
        WHERE (username = 'admin' AND role <> 'ADMIN')
           OR (username = 'car_owner' AND role <> 'USER')
           OR (username = 'car_store' AND role <> 'STORE')
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Demo username conflicts with an existing account role';
    END IF;

    INSERT INTO accounts (username, password_hash, phone, role, status)
    SELECT 'admin', @demo_password_hash, '19900000100', 'ADMIN', 'ACTIVE'
    WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE username = 'admin');
    INSERT INTO accounts (username, password_hash, phone, role, status)
    SELECT 'car_owner', @demo_password_hash, '19900000001', 'USER', 'ACTIVE'
    WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE username = 'car_owner');
    INSERT INTO accounts (username, password_hash, phone, role, status)
    SELECT 'car_store', @demo_password_hash, '19900000101', 'STORE', 'ACTIVE'
    WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE username = 'car_store');

    INSERT INTO merchant_stores (
        account_id, store_name, contact_name, phone, address, review_remark, reviewed_at
    )
    SELECT a.id, '城东汽车养护店', '陈师傅', a.phone, '汽车服务路 100 号', '资料审核通过。', NOW()
    FROM accounts a
    WHERE a.username = 'car_store'
      AND NOT EXISTS (SELECT 1 FROM merchant_stores s WHERE s.account_id = a.id);

    INSERT INTO products (sku, product_name, brand, description, price, labor_fee, stock, status)
    SELECT 'CM-OIL-001', '5W-30 全合成机油 4L', '养护精选', '发动机日常保养用机油，容量 4L。', 268, 60, 20, 'ACTIVE'
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'CM-OIL-001');
    INSERT INTO products (sku, product_name, brand, description, price, labor_fee, stock, status)
    SELECT 'CM-FILTER-001', '机油滤清器', '养护精选', '可搭配机油更换的保养配件。', 45, 15, 40, 'ACTIVE'
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'CM-FILTER-001');
    INSERT INTO products (sku, product_name, brand, description, price, labor_fee, stock, status)
    SELECT 'CM-AC-001', '活性炭空调滤芯', '养护精选', '用于车内空调系统的滤芯更换。', 88, 25, 30, 'ACTIVE'
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'CM-AC-001');

    COMMIT;
    SELECT 'accounts' AS data_table, COUNT(*) AS total_rows FROM accounts
    UNION ALL SELECT 'merchant_stores', COUNT(*) FROM merchant_stores
    UNION ALL SELECT 'products', COUNT(*) FROM products
    UNION ALL SELECT 'orders', COUNT(*) FROM orders
    UNION ALL SELECT 'order_items', COUNT(*) FROM order_items
    UNION ALL SELECT 'appointments', COUNT(*) FROM appointments
    UNION ALL SELECT 'maintenance_records', COUNT(*) FROM maintenance_records
    UNION ALL SELECT 'reviews', COUNT(*) FROM reviews
    UNION ALL SELECT 'product_reviews', COUNT(*) FROM product_reviews;
END$$
DELIMITER ;
CALL cm_seed_demo_v2();
DROP PROCEDURE cm_seed_demo_v2;

-- 同一连接再次导入时默认回到补充模式，避免沿用第一次的重置标志。
SET @reset_demo_data = 0;
