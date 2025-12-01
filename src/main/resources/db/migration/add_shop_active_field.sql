-- TCG Arena Database Migration
-- Aggiunta campo 'active' alla tabella shops

-- 1. Aggiungere campo active (default false)
ALTER TABLE shops 
ADD COLUMN active BOOLEAN NOT NULL DEFAULT false;

-- 2. (Opzionale) Attivare automaticamente shop già verificati
-- Decommentare se si vuole attivare shop esistenti già verificati
-- UPDATE shops SET active = true WHERE verified = true;

-- 3. Indici per performance (opzionale ma raccomandato)
CREATE INDEX idx_shops_active ON shops(active);
CREATE INDEX idx_shops_owner_id ON shops(ownerId);

-- 4. Query di verifica
-- Controllare shop attivi
-- SELECT id, name, active, verified, ownerId FROM shops WHERE active = true;

-- Controllare shop in pending
-- SELECT id, name, active, verified, ownerId FROM shops WHERE active = false;

-- 5. Query per attivazione manuale di uno shop specifico
-- Sostituire {SHOP_ID} con l'ID dello shop da attivare
-- UPDATE shops SET active = true WHERE id = {SHOP_ID};

-- 6. Query per attivazione manuale tramite nome shop
-- Sostituire 'Nome Shop' con il nome esatto
-- UPDATE shops SET active = true WHERE name = 'Nome Shop';

-- 7. Query per disattivare uno shop
-- UPDATE shops SET active = false WHERE id = {SHOP_ID};
