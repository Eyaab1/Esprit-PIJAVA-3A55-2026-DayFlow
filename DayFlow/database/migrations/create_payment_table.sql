-- Migration pour créer la table payment
-- À exécuter sur PostgreSQL

-- Création de la table payment
CREATE TABLE IF NOT EXISTS payment (
    id SERIAL PRIMARY KEY,
    coaching_request_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    coach_id INTEGER NOT NULL,
    amount NUMERIC(10, 2) NOT NULL CHECK (amount >= 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    stripe_payment_intent_id VARCHAR(255),
    stripe_checkout_session_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMP,
    failure_reason TEXT,
    receipt_url VARCHAR(500),
    
    -- Contraintes de clés étrangères
    CONSTRAINT fk_payment_coaching_request 
        FOREIGN KEY (coaching_request_id) 
        REFERENCES coaching_request(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_payment_user 
        FOREIGN KEY (user_id) 
        REFERENCES "user"(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_payment_coach 
        FOREIGN KEY (coach_id) 
        REFERENCES "user"(id) 
        ON DELETE CASCADE,
    
    -- Contrainte pour s'assurer qu'il n'y a qu'un seul paiement réussi par demande
    CONSTRAINT unique_successful_payment_per_request 
        UNIQUE (coaching_request_id, status) 
        WHERE status = 'succeeded'
);

-- Index pour améliorer les performances
CREATE INDEX idx_payment_coaching_request ON payment(coaching_request_id);
CREATE INDEX idx_payment_user ON payment(user_id);
CREATE INDEX idx_payment_coach ON payment(coach_id);
CREATE INDEX idx_payment_status ON payment(status);
CREATE INDEX idx_payment_stripe_checkout_session ON payment(stripe_checkout_session_id);
CREATE INDEX idx_payment_created_at ON payment(created_at DESC);

-- Commentaires pour la documentation
COMMENT ON TABLE payment IS 'Table des paiements pour les séances de coaching';
COMMENT ON COLUMN payment.id IS 'Identifiant unique du paiement';
COMMENT ON COLUMN payment.coaching_request_id IS 'Référence à la demande de coaching';
COMMENT ON COLUMN payment.user_id IS 'Référence à l''utilisateur qui paie';
COMMENT ON COLUMN payment.coach_id IS 'Référence au coach qui reçoit le paiement';
COMMENT ON COLUMN payment.amount IS 'Montant du paiement';
COMMENT ON COLUMN payment.currency IS 'Devise du paiement (code ISO 4217)';
COMMENT ON COLUMN payment.status IS 'Statut du paiement: pending, processing, succeeded, failed, cancelled, refunded';
COMMENT ON COLUMN payment.stripe_payment_intent_id IS 'ID du PaymentIntent Stripe';
COMMENT ON COLUMN payment.stripe_checkout_session_id IS 'ID de la session Stripe Checkout';
COMMENT ON COLUMN payment.created_at IS 'Date de création du paiement';
COMMENT ON COLUMN payment.updated_at IS 'Date de dernière mise à jour';
COMMENT ON COLUMN payment.paid_at IS 'Date de paiement effectif';
COMMENT ON COLUMN payment.failure_reason IS 'Raison de l''échec du paiement';
COMMENT ON COLUMN payment.receipt_url IS 'URL du reçu Stripe';

-- Fonction pour mettre à jour automatiquement updated_at
CREATE OR REPLACE FUNCTION update_payment_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger pour mettre à jour updated_at automatiquement
CREATE TRIGGER trigger_update_payment_updated_at
    BEFORE UPDATE ON payment
    FOR EACH ROW
    EXECUTE FUNCTION update_payment_updated_at();

-- Données de test (optionnel - à commenter en production)
-- INSERT INTO payment (coaching_request_id, user_id, coach_id, amount, currency, status)
-- VALUES (1, 2, 3, 50.00, 'EUR', 'pending');
