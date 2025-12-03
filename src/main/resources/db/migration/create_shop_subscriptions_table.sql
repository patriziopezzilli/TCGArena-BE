-- TCG Arena Database Migration
-- Creazione tabella shop_subscriptions per gestire le iscrizioni utente-negozio

-- 1. Creazione tabella shop_subscriptions
CREATE TABLE shop_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    shop_id BIGINT NOT NULL,
    subscribed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true,

    -- Chiavi esterne
    CONSTRAINT fk_shop_subscriptions_user_id
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_shop_subscriptions_shop_id
        FOREIGN KEY (shop_id) REFERENCES shops(id) ON DELETE CASCADE,

    -- Vincolo di unicità per evitare duplicati attivi
    CONSTRAINT uk_user_shop_active
        UNIQUE (user_id, shop_id, is_active)
);

-- 2. Indici per performance
CREATE INDEX idx_shop_subscriptions_user_id ON shop_subscriptions(user_id);
CREATE INDEX idx_shop_subscriptions_shop_id ON shop_subscriptions(shop_id);
CREATE INDEX idx_shop_subscriptions_active ON shop_subscriptions(is_active);
CREATE INDEX idx_shop_subscriptions_user_active ON shop_subscriptions(user_id, is_active);
CREATE INDEX idx_shop_subscriptions_shop_active ON shop_subscriptions(shop_id, is_active);

-- 3. Commenti sulla tabella
COMMENT ON TABLE shop_subscriptions IS 'Tabella che gestisce le iscrizioni degli utenti ai negozi per ricevere notifiche';
COMMENT ON COLUMN shop_subscriptions.user_id IS 'ID dell''utente iscritto';
COMMENT ON COLUMN shop_subscriptions.shop_id IS 'ID del negozio a cui l''utente è iscritto';
COMMENT ON COLUMN shop_subscriptions.subscribed_at IS 'Data e ora dell''iscrizione';
COMMENT ON COLUMN shop_subscriptions.is_active IS 'Flag che indica se l''iscrizione è attiva (true) o disattivata (false)';

-- 4. Query di verifica
-- Controllare numero di iscritti per negozio
-- SELECT shop_id, COUNT(*) as subscriber_count FROM shop_subscriptions WHERE is_active = true GROUP BY shop_id ORDER BY subscriber_count DESC;

-- Controllare iscrizioni di un utente specifico
-- SELECT ss.*, s.name as shop_name FROM shop_subscriptions ss JOIN shops s ON ss.shop_id = s.id WHERE ss.user_id = {USER_ID} AND ss.is_active = true;

-- Controllare iscritti di un negozio specifico
-- SELECT ss.*, u.username, u.display_name FROM shop_subscriptions ss JOIN users u ON ss.user_id = u.id WHERE ss.shop_id = {SHOP_ID} AND ss.is_active = true;