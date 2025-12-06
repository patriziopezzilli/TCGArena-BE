-- TCG Arena Database Migration
-- Aggiunta campo 'reservation_duration_minutes' alla tabella shops

-- 1. Aggiungere campo reservation_duration_minutes (default 30 minuti)
ALTER TABLE shops
ADD COLUMN reservation_duration_minutes INTEGER NOT NULL DEFAULT 30;

-- 2. Aggiungere constraint per valori validi (1-1440 minuti = 24 ore)
ALTER TABLE shops
ADD CONSTRAINT chk_reservation_duration_minutes
CHECK (reservation_duration_minutes >= 1 AND reservation_duration_minutes <= 1440);

-- 3. Indice per performance (opzionale)
CREATE INDEX idx_shops_reservation_duration ON shops(reservation_duration_minutes);

-- 4. Query di verifica
-- Controllare la durata delle prenotazioni per tutti gli shop
-- SELECT id, name, reservation_duration_minutes FROM shops ORDER BY reservation_duration_minutes;

-- 5. Query per aggiornare la durata di uno shop specifico
-- Sostituire {SHOP_ID} con l'ID dello shop e {DURATION} con i minuti desiderati
-- UPDATE shops SET reservation_duration_minutes = {DURATION} WHERE id = {SHOP_ID};

-- 6. Query per vedere shop con durata default (30 minuti)
-- SELECT id, name, reservation_duration_minutes FROM shops WHERE reservation_duration_minutes = 30;

-- 7. Query per vedere shop con durata personalizzata
-- SELECT id, name, reservation_duration_minutes FROM shops WHERE reservation_duration_minutes != 30;