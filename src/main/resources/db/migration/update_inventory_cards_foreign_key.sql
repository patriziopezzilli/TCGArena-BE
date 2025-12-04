-- Update foreign key for inventory_cards to reference card_templates instead of cards
ALTER TABLE inventory_cards DROP CONSTRAINT fkmdwgp5xi15wk165metdbyty25;
ALTER TABLE inventory_cards ADD CONSTRAINT fk_inventory_cards_card_template_id FOREIGN KEY (card_template_id) REFERENCES card_templates(id);