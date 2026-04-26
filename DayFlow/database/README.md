# 📊 Base de Données - Module de Paiement

## 📁 Structure des Fichiers

```
database/
├── migrations/
│   └── create_payment_table.sql      # Script de création de la table payment
├── test_data/
│   └── payment_test_data.sql         # Données de test pour le module
└── README.md                          # Ce fichier
```

---

## 🚀 Installation

### Prérequis

- PostgreSQL 12 ou supérieur
- Accès à la base de données DayFlow
- Droits de création de tables

### Étape 1 : Créer la Table Payment

```bash
psql -U postgres -d dayflow -f migrations/create_payment_table.sql
```

Ou depuis pgAdmin :
1. Ouvrir pgAdmin
2. Se connecter à la base `dayflow`
3. Ouvrir Query Tool
4. Copier-coller le contenu de `create_payment_table.sql`
5. Exécuter (F5)

### Étape 2 : Vérifier la Création

```sql
-- Vérifier que la table existe
SELECT table_name 
FROM information_schema.tables 
WHERE table_name = 'payment';

-- Vérifier la structure
\d payment

-- Vérifier les index
SELECT indexname, indexdef 
FROM pg_indexes 
WHERE tablename = 'payment';
```

### Étape 3 : Charger les Données de Test (Optionnel)

```bash
psql -U postgres -d dayflow -f test_data/payment_test_data.sql
```

---

## 📋 Structure de la Table Payment

### Colonnes

| Colonne | Type | Description |
|---------|------|-------------|
| `id` | SERIAL | Identifiant unique (clé primaire) |
| `coaching_request_id` | INTEGER | Référence à la demande de coaching |
| `user_id` | INTEGER | Référence à l'utilisateur qui paie |
| `coach_id` | INTEGER | Référence au coach qui reçoit |
| `amount` | NUMERIC(10,2) | Montant du paiement |
| `currency` | VARCHAR(3) | Devise (EUR, USD, etc.) |
| `status` | VARCHAR(20) | Statut du paiement |
| `stripe_payment_intent_id` | VARCHAR(255) | ID du PaymentIntent Stripe |
| `stripe_checkout_session_id` | VARCHAR(255) | ID de la session Stripe |
| `created_at` | TIMESTAMP | Date de création |
| `updated_at` | TIMESTAMP | Date de dernière mise à jour |
| `paid_at` | TIMESTAMP | Date de paiement effectif |
| `failure_reason` | TEXT | Raison de l'échec (si applicable) |
| `receipt_url` | VARCHAR(500) | URL du reçu Stripe |

### Contraintes

- **Clé primaire** : `id`
- **Clés étrangères** :
  - `coaching_request_id` → `coaching_request(id)` ON DELETE CASCADE
  - `user_id` → `user(id)` ON DELETE CASCADE
  - `coach_id` → `user(id)` ON DELETE CASCADE
- **Contrainte unique** : Un seul paiement réussi par demande
- **Check** : `amount >= 0`

### Index

- `idx_payment_coaching_request` : Sur `coaching_request_id`
- `idx_payment_user` : Sur `user_id`
- `idx_payment_coach` : Sur `coach_id`
- `idx_payment_status` : Sur `status`
- `idx_payment_stripe_checkout_session` : Sur `stripe_checkout_session_id`
- `idx_payment_created_at` : Sur `created_at DESC`

### Triggers

- `trigger_update_payment_updated_at` : Met à jour automatiquement `updated_at`

---

## 🔍 Requêtes Utiles

### Statistiques Générales

```sql
-- Nombre total de paiements par statut
SELECT status, COUNT(*) as count, SUM(amount) as total
FROM payment
GROUP BY status
ORDER BY status;

-- Paiements du jour
SELECT * FROM payment
WHERE DATE(created_at) = CURRENT_DATE
ORDER BY created_at DESC;

-- Paiements en attente
SELECT * FROM payment
WHERE status = 'pending'
ORDER BY created_at ASC;
```

### Statistiques par Coach

```sql
-- Gains totaux d'un coach
SELECT 
    coach_id,
    COUNT(*) as total_payments,
    SUM(amount) as total_earnings,
    AVG(amount) as average_payment
FROM payment
WHERE status = 'succeeded' AND coach_id = 2
GROUP BY coach_id;

-- Historique des paiements d'un coach
SELECT 
    p.*,
    u.first_name || ' ' || u.last_name as client_name,
    cr.message as request_message
FROM payment p
JOIN "user" u ON u.id = p.user_id
JOIN coaching_request cr ON cr.id = p.coaching_request_id
WHERE p.coach_id = 2
ORDER BY p.created_at DESC;
```

### Statistiques par Utilisateur

```sql
-- Historique des paiements d'un utilisateur
SELECT 
    p.*,
    c.first_name || ' ' || c.last_name as coach_name
FROM payment p
JOIN "user" c ON c.id = p.coach_id
WHERE p.user_id = 1
ORDER BY p.created_at DESC;

-- Montant total dépensé par un utilisateur
SELECT 
    user_id,
    COUNT(*) as total_payments,
    SUM(amount) as total_spent
FROM payment
WHERE status = 'succeeded' AND user_id = 1
GROUP BY user_id;
```

### Demandes Acceptées Sans Paiement

```sql
-- Trouver les demandes acceptées qui n'ont pas encore de paiement
SELECT 
    cr.id,
    cr.user_id,
    cr.coach_id,
    cr.message,
    cr.budget,
    cr.created_at
FROM coaching_request cr
LEFT JOIN payment p ON p.coaching_request_id = cr.id
WHERE cr.status = 'accepted' AND p.id IS NULL
ORDER BY cr.created_at DESC;
```

### Paiements Échoués

```sql
-- Analyser les paiements échoués
SELECT 
    p.id,
    p.amount,
    p.failure_reason,
    p.created_at,
    u.email as user_email
FROM payment p
JOIN "user" u ON u.id = p.user_id
WHERE p.status = 'failed'
ORDER BY p.created_at DESC;
```

---

## 🧪 Tests de Validation

### Test 1 : Intégrité Référentielle

```sql
-- Vérifier que tous les paiements ont des références valides
SELECT 
    p.id,
    CASE 
        WHEN cr.id IS NULL THEN 'ERREUR: Demande inexistante'
        WHEN u.id IS NULL THEN 'ERREUR: Utilisateur inexistant'
        WHEN c.id IS NULL THEN 'ERREUR: Coach inexistant'
        ELSE 'OK'
    END as check_result
FROM payment p
LEFT JOIN coaching_request cr ON cr.id = p.coaching_request_id
LEFT JOIN "user" u ON u.id = p.user_id
LEFT JOIN "user" c ON c.id = p.coach_id;
```

### Test 2 : Contrainte Unique

```sql
-- Vérifier qu'il n'y a pas de doublons de paiements réussis
SELECT 
    coaching_request_id,
    COUNT(*) as payment_count
FROM payment
WHERE status = 'succeeded'
GROUP BY coaching_request_id
HAVING COUNT(*) > 1;
```

### Test 3 : Montants Valides

```sql
-- Vérifier que tous les montants sont positifs
SELECT * FROM payment WHERE amount < 0;

-- Vérifier que les montants correspondent aux budgets
SELECT 
    p.id,
    p.amount as payment_amount,
    cr.budget as request_budget,
    ABS(p.amount - cr.budget) as difference
FROM payment p
JOIN coaching_request cr ON cr.id = p.coaching_request_id
WHERE ABS(p.amount - cr.budget) > 0.01;
```

---

## 🔧 Maintenance

### Nettoyage des Données de Test

```sql
-- Supprimer tous les paiements de test
DELETE FROM payment 
WHERE stripe_payment_intent_id LIKE 'pi_test_%'
   OR stripe_checkout_session_id LIKE 'cs_test_%';
```

### Réinitialiser la Séquence

```sql
-- Après suppression de données, réinitialiser la séquence
SELECT setval('payment_id_seq', (SELECT MAX(id) FROM payment));
```

### Archivage des Anciens Paiements

```sql
-- Créer une table d'archive
CREATE TABLE payment_archive (LIKE payment INCLUDING ALL);

-- Archiver les paiements de plus d'un an
INSERT INTO payment_archive
SELECT * FROM payment
WHERE created_at < CURRENT_DATE - INTERVAL '1 year';

-- Supprimer les paiements archivés
DELETE FROM payment
WHERE created_at < CURRENT_DATE - INTERVAL '1 year';
```

---

## 📊 Vues Utiles

### Vue : Paiements avec Détails

```sql
CREATE OR REPLACE VIEW v_payment_details AS
SELECT 
    p.id,
    p.amount,
    p.currency,
    p.status,
    p.created_at,
    p.paid_at,
    cr.id as request_id,
    cr.message as request_message,
    u.id as user_id,
    u.first_name || ' ' || u.last_name as user_name,
    u.email as user_email,
    c.id as coach_id,
    c.first_name || ' ' || c.last_name as coach_name,
    c.email as coach_email
FROM payment p
JOIN coaching_request cr ON cr.id = p.coaching_request_id
JOIN "user" u ON u.id = p.user_id
JOIN "user" c ON c.id = p.coach_id;

-- Utilisation
SELECT * FROM v_payment_details WHERE status = 'succeeded';
```

### Vue : Statistiques par Coach

```sql
CREATE OR REPLACE VIEW v_coach_payment_stats AS
SELECT 
    coach_id,
    COUNT(*) as total_payments,
    COUNT(*) FILTER (WHERE status = 'succeeded') as successful_payments,
    COUNT(*) FILTER (WHERE status = 'failed') as failed_payments,
    COUNT(*) FILTER (WHERE status = 'pending') as pending_payments,
    SUM(amount) FILTER (WHERE status = 'succeeded') as total_earnings,
    AVG(amount) FILTER (WHERE status = 'succeeded') as average_payment
FROM payment
GROUP BY coach_id;

-- Utilisation
SELECT * FROM v_coach_payment_stats ORDER BY total_earnings DESC;
```

---

## 🔐 Sécurité

### Permissions Recommandées

```sql
-- Créer un rôle pour l'application
CREATE ROLE dayflow_app WITH LOGIN PASSWORD 'votre_mot_de_passe';

-- Donner les permissions nécessaires
GRANT SELECT, INSERT, UPDATE ON payment TO dayflow_app;
GRANT USAGE, SELECT ON SEQUENCE payment_id_seq TO dayflow_app;

-- Interdire la suppression directe (utiliser une fonction)
REVOKE DELETE ON payment FROM dayflow_app;
```

### Audit Trail

```sql
-- Créer une table d'audit
CREATE TABLE payment_audit (
    id SERIAL PRIMARY KEY,
    payment_id INTEGER,
    action VARCHAR(10),
    old_status VARCHAR(20),
    new_status VARCHAR(20),
    changed_by VARCHAR(100),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Créer un trigger d'audit
CREATE OR REPLACE FUNCTION audit_payment_changes()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' AND OLD.status != NEW.status THEN
        INSERT INTO payment_audit (payment_id, action, old_status, new_status)
        VALUES (NEW.id, 'UPDATE', OLD.status, NEW.status);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER payment_audit_trigger
AFTER UPDATE ON payment
FOR EACH ROW
EXECUTE FUNCTION audit_payment_changes();
```

---

## 📈 Performance

### Optimisation des Index

```sql
-- Analyser l'utilisation des index
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
WHERE tablename = 'payment'
ORDER BY idx_scan DESC;

-- Analyser les requêtes lentes
EXPLAIN ANALYZE
SELECT * FROM payment WHERE status = 'pending';
```

### Vacuum et Analyze

```sql
-- Nettoyer et analyser la table
VACUUM ANALYZE payment;

-- Statistiques de la table
SELECT 
    n_tup_ins as inserts,
    n_tup_upd as updates,
    n_tup_del as deletes,
    n_live_tup as live_rows,
    n_dead_tup as dead_rows
FROM pg_stat_user_tables
WHERE tablename = 'payment';
```

---

## 🆘 Dépannage

### Problème : La table existe déjà

```sql
-- Supprimer la table (ATTENTION : perte de données)
DROP TABLE IF EXISTS payment CASCADE;

-- Puis réexécuter le script de création
```

### Problème : Erreur de clé étrangère

```sql
-- Vérifier que les tables référencées existent
SELECT table_name FROM information_schema.tables 
WHERE table_name IN ('coaching_request', 'user');

-- Vérifier les IDs invalides
SELECT p.* FROM payment p
LEFT JOIN coaching_request cr ON cr.id = p.coaching_request_id
WHERE cr.id IS NULL;
```

### Problème : Séquence désynchronisée

```sql
-- Réinitialiser la séquence
SELECT setval('payment_id_seq', (SELECT MAX(id) FROM payment));
```

---

## 📞 Support

Pour toute question sur la base de données :
- Consulter la documentation PostgreSQL : https://www.postgresql.org/docs/
- Vérifier les logs PostgreSQL
- Contacter l'équipe de développement

---

**✅ La base de données est prête pour le module de paiement !**
