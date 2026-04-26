-- Script de données de test pour le module de paiement
-- À exécuter APRÈS create_payment_table.sql
-- Ces données sont pour les tests uniquement

-- ============================================
-- IMPORTANT: Adapter les IDs selon votre base
-- ============================================

-- Supposons que vous avez :
-- - User ID 1 : Un utilisateur client
-- - User ID 2 : Un coach
-- - CoachingRequest ID 1 : Une demande acceptée

-- ============================================
-- 1. Créer une demande de coaching de test
-- ============================================

-- Vérifier si la demande existe déjà
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM coaching_request WHERE id = 1) THEN
        INSERT INTO coaching_request (
            id, user_id, coach_id, message, status, 
            goal, level, frequency, budget, coaching_type, priority,
            created_at
        ) VALUES (
            1, 1, 2, 
            'Je souhaite améliorer ma gestion du temps et ma productivité.',
            'accepted',
            'Productivité',
            'Intermédiaire',
            'Hebdomadaire',
            75.00,
            'Individuel',
            'normal',
            CURRENT_TIMESTAMP - INTERVAL '2 days'
        );
        
        -- Réinitialiser la séquence
        PERFORM setval('coaching_request_id_seq', (SELECT MAX(id) FROM coaching_request));
    END IF;
END $$;

-- ============================================
-- 2. Créer des paiements de test
-- ============================================

-- Paiement en attente (PENDING)
INSERT INTO payment (
    coaching_request_id, user_id, coach_id, 
    amount, currency, status,
    created_at, updated_at
) VALUES (
    1, 1, 2,
    75.00, 'EUR', 'pending',
    CURRENT_TIMESTAMP - INTERVAL '1 hour',
    CURRENT_TIMESTAMP - INTERVAL '1 hour'
) ON CONFLICT DO NOTHING;

-- Paiement réussi (SUCCEEDED) - pour une autre demande
-- Note: Créer d'abord une autre demande si nécessaire
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM coaching_request WHERE id = 2) THEN
        INSERT INTO payment (
            coaching_request_id, user_id, coach_id,
            amount, currency, status,
            stripe_payment_intent_id, stripe_checkout_session_id,
            created_at, updated_at, paid_at,
            receipt_url
        ) VALUES (
            2, 1, 2,
            50.00, 'EUR', 'succeeded',
            'pi_test_1234567890',
            'cs_test_1234567890',
            CURRENT_TIMESTAMP - INTERVAL '3 days',
            CURRENT_TIMESTAMP - INTERVAL '3 days',
            CURRENT_TIMESTAMP - INTERVAL '3 days',
            'https://stripe.com/receipt/test'
        ) ON CONFLICT DO NOTHING;
    END IF;
END $$;

-- Paiement échoué (FAILED)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM coaching_request WHERE id = 3) THEN
        INSERT INTO payment (
            coaching_request_id, user_id, coach_id,
            amount, currency, status,
            stripe_payment_intent_id,
            created_at, updated_at,
            failure_reason
        ) VALUES (
            3, 1, 2,
            60.00, 'EUR', 'failed',
            'pi_test_failed_123',
            CURRENT_TIMESTAMP - INTERVAL '5 days',
            CURRENT_TIMESTAMP - INTERVAL '5 days',
            'Carte refusée - Fonds insuffisants'
        ) ON CONFLICT DO NOTHING;
    END IF;
END $$;

-- ============================================
-- 3. Vérifier les données insérées
-- ============================================

-- Afficher tous les paiements
SELECT 
    p.id,
    p.coaching_request_id,
    p.amount,
    p.currency,
    p.status,
    p.created_at,
    cr.status as request_status
FROM payment p
LEFT JOIN coaching_request cr ON cr.id = p.coaching_request_id
ORDER BY p.created_at DESC;

-- Statistiques
SELECT 
    status,
    COUNT(*) as count,
    SUM(amount) as total_amount
FROM payment
GROUP BY status
ORDER BY status;

-- ============================================
-- 4. Requêtes utiles pour les tests
-- ============================================

-- Trouver tous les paiements d'un utilisateur
-- SELECT * FROM payment WHERE user_id = 1 ORDER BY created_at DESC;

-- Trouver tous les paiements d'un coach
-- SELECT * FROM payment WHERE coach_id = 2 ORDER BY created_at DESC;

-- Trouver le paiement d'une demande spécifique
-- SELECT * FROM payment WHERE coaching_request_id = 1;

-- Calculer les gains totaux d'un coach
-- SELECT 
--     coach_id,
--     SUM(amount) as total_earnings,
--     COUNT(*) as successful_payments
-- FROM payment 
-- WHERE status = 'succeeded' AND coach_id = 2
-- GROUP BY coach_id;

-- Vérifier les demandes acceptées sans paiement
-- SELECT cr.* 
-- FROM coaching_request cr
-- LEFT JOIN payment p ON p.coaching_request_id = cr.id
-- WHERE cr.status = 'accepted' AND p.id IS NULL;

-- ============================================
-- 5. Nettoyage (si nécessaire)
-- ============================================

-- Pour supprimer toutes les données de test :
-- DELETE FROM payment WHERE coaching_request_id IN (1, 2, 3);
-- DELETE FROM coaching_request WHERE id IN (1, 2, 3);

-- ============================================
-- 6. Scénarios de test
-- ============================================

-- Scénario 1 : Paiement réussi
-- 1. Créer une demande avec status 'accepted'
-- 2. Créer un paiement avec status 'pending'
-- 3. Mettre à jour le paiement : status = 'succeeded', paid_at = NOW()
-- 4. Mettre à jour la demande : status = 'paid'

-- Scénario 2 : Paiement échoué
-- 1. Créer une demande avec status 'accepted'
-- 2. Créer un paiement avec status 'pending'
-- 3. Mettre à jour le paiement : status = 'failed', failure_reason = '...'

-- Scénario 3 : Annulation de paiement
-- 1. Créer une demande avec status 'accepted'
-- 2. Créer un paiement avec status 'pending'
-- 3. Mettre à jour le paiement : status = 'cancelled'

-- ============================================
-- 7. Vérification de l'intégrité
-- ============================================

-- Vérifier qu'il n'y a pas de doublons de paiements réussis
SELECT 
    coaching_request_id,
    COUNT(*) as payment_count
FROM payment
WHERE status = 'succeeded'
GROUP BY coaching_request_id
HAVING COUNT(*) > 1;

-- Vérifier que tous les paiements ont des références valides
SELECT 
    p.id,
    p.coaching_request_id,
    p.user_id,
    p.coach_id,
    CASE 
        WHEN cr.id IS NULL THEN 'ERREUR: Demande inexistante'
        WHEN u.id IS NULL THEN 'ERREUR: Utilisateur inexistant'
        WHEN c.id IS NULL THEN 'ERREUR: Coach inexistant'
        ELSE 'OK'
    END as integrity_check
FROM payment p
LEFT JOIN coaching_request cr ON cr.id = p.coaching_request_id
LEFT JOIN "user" u ON u.id = p.user_id
LEFT JOIN "user" c ON c.id = p.coach_id
WHERE cr.id IS NULL OR u.id IS NULL OR c.id IS NULL;

-- ============================================
-- FIN DU SCRIPT
-- ============================================

-- Note: Ce script est conçu pour être idempotent
-- Vous pouvez l'exécuter plusieurs fois sans créer de doublons
