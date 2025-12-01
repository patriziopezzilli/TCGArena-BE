-- TCG Arena - SQL Queries per Gestione Shop
-- Istruzioni per Admin

-- ========================================
-- 1. VISUALIZZARE TUTTI GLI SHOP
-- ========================================

-- Tutti gli shop con info base
SELECT 
    id,
    name,
    address,
    phoneNumber as phone,
    active,
    isVerified as verified,
    ownerId,
    type
FROM shops
ORDER BY id DESC;

-- ========================================
-- 2. SHOP IN ATTESA DI APPROVAZIONE
-- ========================================

-- Mostra solo shop NON ancora attivi
SELECT 
    s.id,
    s.name,
    s.address,
    s.phoneNumber as phone,
    s.active,
    s.isVerified as verified,
    s.ownerId,
    u.username as owner_username,
    u.email as owner_email,
    u.displayName as owner_name
FROM shops s
LEFT JOIN users u ON s.ownerId = u.id
WHERE s.active = false
ORDER BY s.id DESC;

-- ========================================
-- 3. SHOP ATTIVI
-- ========================================

-- Mostra solo shop attivi (visibili nell'app)
SELECT 
    s.id,
    s.name,
    s.address,
    s.phoneNumber as phone,
    s.active,
    s.isVerified as verified,
    s.ownerId,
    u.username as owner_username
FROM shops s
LEFT JOIN users u ON s.ownerId = u.id
WHERE s.active = true
ORDER BY s.id DESC;

-- ========================================
-- 4. ATTIVARE UNO SHOP
-- ========================================

-- Attivare shop per ID
UPDATE shops 
SET active = true 
WHERE id = 1;  -- Sostituire con l'ID corretto

-- Attivare shop per nome
UPDATE shops 
SET active = true 
WHERE name = 'Nome Shop Esatto';

-- Attivare shop per owner username
UPDATE shops 
SET active = true 
WHERE ownerId = (SELECT id FROM users WHERE username = 'merchant_username');

-- Attivare E verificare shop contemporaneamente
UPDATE shops 
SET active = true, isVerified = true 
WHERE id = 1;

-- ========================================
-- 5. DISATTIVARE UNO SHOP
-- ========================================

-- Disattivare shop (rimuovere dalla visibilità pubblica)
UPDATE shops 
SET active = false 
WHERE id = 1;

-- Disattivare E rimuovere verifica
UPDATE shops 
SET active = false, isVerified = false 
WHERE id = 1;

-- ========================================
-- 6. INFO DETTAGLIATE SU UNO SHOP
-- ========================================

-- Dettagli completi shop + owner
SELECT 
    s.id as shop_id,
    s.name as shop_name,
    s.description,
    s.address,
    s.phoneNumber,
    s.websiteUrl,
    s.openingHours,
    s.openingDays,
    s.type,
    s.active,
    s.isVerified,
    u.id as owner_id,
    u.username,
    u.email,
    u.displayName,
    u.dateJoined as owner_registration_date
FROM shops s
LEFT JOIN users u ON s.ownerId = u.id
WHERE s.id = 1;  -- Sostituire con l'ID corretto

-- ========================================
-- 7. STATISTICHE SHOP
-- ========================================

-- Conteggio shop per status
SELECT 
    COUNT(*) as total,
    SUM(CASE WHEN active = true THEN 1 ELSE 0 END) as active_shops,
    SUM(CASE WHEN active = false THEN 1 ELSE 0 END) as pending_shops,
    SUM(CASE WHEN isVerified = true THEN 1 ELSE 0 END) as verified_shops
FROM shops;

-- ========================================
-- 8. SHOP SENZA OWNER
-- ========================================

-- Trova shop con ownerId non valido (data integrity check)
SELECT 
    s.id,
    s.name,
    s.ownerId
FROM shops s
LEFT JOIN users u ON s.ownerId = u.id
WHERE u.id IS NULL;

-- ========================================
-- 9. MERCHANT SENZA SHOP
-- ========================================

-- Trova utenti merchant senza shop associato
SELECT 
    u.id,
    u.username,
    u.email,
    u.displayName,
    u.isMerchant,
    u.shopId
FROM users u
WHERE u.isMerchant = true 
AND u.shopId IS NULL;

-- ========================================
-- 10. ATTIVAZIONE BATCH
-- ========================================

-- Attivare tutti gli shop verificati
UPDATE shops 
SET active = true 
WHERE isVerified = true AND active = false;

-- Attivare tutti gli shop con email verificata
UPDATE shops s
SET s.active = true 
WHERE s.ownerId IN (
    SELECT id FROM users WHERE email LIKE '%@verified-domain.com'
);

-- ========================================
-- 11. SHOP PER TIPO
-- ========================================

-- Contare shop per tipo
SELECT 
    type,
    COUNT(*) as count,
    SUM(CASE WHEN active = true THEN 1 ELSE 0 END) as active_count
FROM shops
GROUP BY type;

-- ========================================
-- 12. VERIFICARE CONSISTENZA DATI
-- ========================================

-- Shops con active=true ma owner non è merchant
SELECT 
    s.id,
    s.name,
    u.username,
    u.isMerchant
FROM shops s
LEFT JOIN users u ON s.ownerId = u.id
WHERE s.active = true AND u.isMerchant = false;

-- Users merchant con shopId ma shop non esiste
SELECT 
    u.id,
    u.username,
    u.shopId
FROM users u
LEFT JOIN shops s ON u.shopId = s.id
WHERE u.isMerchant = true 
AND u.shopId IS NOT NULL 
AND s.id IS NULL;

-- ========================================
-- 13. ESEMPIO: WORKFLOW COMPLETO APPROVAZIONE
-- ========================================

-- Step 1: Visualizzare shop in pending
SELECT id, name, address, phoneNumber, ownerId 
FROM shops 
WHERE active = false;

-- Step 2: Verificare dati owner
SELECT u.username, u.email, u.displayName 
FROM users u 
WHERE u.id = 1;  -- ownerId dello shop

-- Step 3: Attivare shop
UPDATE shops SET active = true, isVerified = true WHERE id = 1;

-- Step 4: Verifica
SELECT id, name, active, isVerified FROM shops WHERE id = 1;

-- ========================================
-- 14. BACKUP PRIMA DI MODIFICHE
-- ========================================

-- Creare backup tabella shops prima di modifiche massive
CREATE TABLE shops_backup AS SELECT * FROM shops;

-- Ripristino da backup (se necessario)
-- DELETE FROM shops;
-- INSERT INTO shops SELECT * FROM shops_backup;

-- ========================================
-- 15. RICERCHE UTILI
-- ========================================

-- Shop registrati oggi
SELECT * FROM shops 
WHERE DATE(createdAt) = CURDATE();

-- Shop registrati ultima settimana
SELECT * FROM shops 
WHERE createdAt >= DATE_SUB(NOW(), INTERVAL 7 DAY);

-- Shop per città
SELECT 
    SUBSTRING_INDEX(address, ',', -1) as city,
    COUNT(*) as count
FROM shops
WHERE active = true
GROUP BY city;
