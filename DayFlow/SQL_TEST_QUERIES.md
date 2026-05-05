# 🗄️ SQL Test Queries - Session Reservation Limit

## Préparation de la Base de Données

### 1. Vérifier les tables existantes

```sql
-- Vérifier que les tables existent
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'public' 
AND table_name IN ('session', 'coaching_request', 'user');
```

**Résultat attendu**:
```
session
coaching_request
user
```

---

## 2. Créer des Données de Test

### Créer un utilisateur de test

```sql
-- Créer un utilisateur de test (si n'existe pas)
INSERT INTO "user" (email, password, first_name, last_name, created_at)
VALUES ('test@example.com', 'hashed_password', 'Test', 'User', NOW())
ON CONFLICT DO NOTHING;

-- Récupérer l'ID
SELECT id FROM "user" WHERE email = 'test@example.com';
```

**Résultat attendu**: ID = 1 (ou autre)

### Créer un coach de test

```sql
-- Créer un coach de test (si n'existe pas)
INSERT INTO "user" (email, password, first_name, last_name, created_at)
VALUES ('coach@example.com', 'hashed_password', 'Coach', 'Test', NOW())
ON CONFLICT DO NOTHING;

-- Récupérer l'ID
SELECT id FROM "user" WHERE email = 'coach@example.com';
```

**Résultat attendu**: ID = 2 (ou autre)

### Créer une demande de coaching

```sql
-- Créer une coaching_request
INSERT INTO coaching_request (user_id, coach_id, status, created_at)
VALUES (1, 2, 'accepted', NOW())
RETURNING id;
```

**Résultat attendu**: ID = 1 (ou autre)

---

## 3. Tester le Comptage des Sessions Futures

### Requête de Test (Identique à celle du validateur)

```sql
-- Compter les sessions futures pour l'utilisateur 1
SELECT COUNT(*) as future_sessions_count FROM session s
INNER JOIN coaching_request cr ON cr.id = s.coaching_request_id
WHERE cr.user_id = 1
  AND s.status IN ('confirmed', 'proposed_by_user', 'proposed_by_coach')
  AND (
    CAST(COALESCE(s.scheduled_at, s.proposed_time_by_coach, s.proposed_time_by_user) AS date) > CURRENT_DATE
    OR (
      CAST(COALESCE(s.scheduled_at, s.proposed_time_by_coach, s.proposed_time_by_user) AS date) = CURRENT_DATE
      AND CAST(COALESCE(s.scheduled_at, s.proposed_time_by_coach, s.proposed_time_by_user) AS time) > CURRENT_TIME
    )
  );
```

**Résultat attendu**: `future_sessions_count = 0` (au départ)

---

## 4. Créer des Sessions de Test

### Créer 3 sessions futures

```sql
-- Session 1: Demain à 10:00
INSERT INTO session (coaching_request_id, status, scheduled_at, created_at, updated_at)
VALUES (1, 'confirmed', NOW() + INTERVAL '1 day' + INTERVAL '10 hours', NOW(), NOW());

-- Session 2: Demain à 14:00
INSERT INTO session (coaching_request_id, status, scheduled_at, created_at, updated_at)
VALUES (1, 'confirmed', NOW() + INTERVAL '1 day' + INTERVAL '14 hours', NOW(), NOW());

-- Session 3: Après-demain à 10:00
INSERT INTO session (coaching_request_id, status, scheduled_at, created_at, updated_at)
VALUES (1, 'confirmed', NOW() + INTERVAL '2 days' + INTERVAL '10 hours', NOW(), NOW());
```

**Résultat attendu**: 3 sessions créées

### Vérifier le comptage

```sql
-- Vérifier que 3 sessions sont comptées
SELECT COUNT(*) as future_sessions_count FROM session s
INNER JOIN coaching_request cr ON cr.id = s.coaching_request_id
WHERE cr.user_id = 1
  AND s.status IN ('confirmed', 'proposed_by_user', 'proposed_by_coach')
  AND (
    CAST(COALESCE(s.scheduled_at, s.proposed_time_by_coach, s.proposed_time_by_user) AS date) > CURRENT_DATE
    OR (
      CAST(COALESCE(s.scheduled_at, s.proposed_time_by_coach, s.proposed_time_by_user) AS date) = CURRENT_DATE
      AND CAST(COALESCE(s.scheduled_at, s.proposed_time_by_coach, s.proposed_time_by_user) AS time) > CURRENT_TIME
    )
  );
```

**Résultat attendu**: `future_sessions_count = 3`

---

## 5. Tester les Statuts Exclus

### Créer une session avec statut "completed"

```sql
-- Session complétée (ne doit PAS être comptée)
INSERT INTO session (coaching_request_id, status, scheduled_at, created_at, updated_at)
VALUES (1, 'completed', NOW() + INTERVAL '1 day' + INTERVAL '18 hours', NOW(), NOW());

-- Vérifier que le comptage reste 3
SELECT COUNT(*) as future_sessions_count FROM session s
INNER JOIN coaching_request cr ON cr.id = s.coaching_request_id
WHERE cr.user_id = 1
  AND s.status IN ('confirmed', 'proposed_by_user', 'proposed_by_coach')
  AND (
    CAST(COALESCE(s.scheduled_at, s.proposed_time_by_coach, s.proposed_time_by_user) AS date) > CURRENT_DATE
    OR (
      CAST(COALESCE(s.scheduled_at, s.proposed_time_by_coach, s.proposed_time_by_user) AS date) = CURRENT_DATE
      AND CAST(COALESCE(s.scheduled_at, s.proposed_time_by_coach, s.proposed_time_by_user) AS time) > CURRENT_TIME
    )
  );
```

**Résultat attendu**: `future_sessions_count = 3` (inchangé)

### Créer une session avec statut "cancelled"

```sql
-- Session annulée (ne doit PAS être comptée)
INSERT INTO session (coaching_request_id, status, scheduled_at, created_at, updated_at)
VALUES (1, 'cancelled', NOW() + INTERVAL '3 days' + INTERVAL '10 hours', NOW(), NOW());

-- Vérifier que le comptage reste 3
SELECT COUNT(*) as future_sessions_count FROM session s
INNER JOIN coaching_request cr ON cr.id = s.coaching_request_id
WHERE cr.user_id = 1
  AND s.status IN ('confirmed', 'proposed_by_user', 'proposed_by_coach')
  AND (
    CAST(COALESCE(s.scheduled_at, s.proposed_time_by_coach, s.proposed_time_by_user) AS date) > CURRENT_DATE
    OR (
      CAST(COALESCE(s.scheduled_at, s.proposed_time_by_coach, s.proposed_time_by_user) AS date) = CURRENT_DATE
      AND CAST(COALESCE(s.scheduled_at, s.proposed_time_by_coach, s.proposed_time_by_user) AS time) > CURRENT_TIME
    )
  );
```

**Résultat attendu**: `future_sessions_count = 3` (inchangé)

---

## 6. Tester les Sessions d'Aujourd'hui

### Créer une session pour aujourd'hui (futur)

```sql
-- Session aujourd'hui à 18:00 (si heure actuelle < 18:00)
INSERT INTO session (coaching_request_id, status, scheduled_at, created_at, updated_at)
VALUES (1, 'confirmed', NOW() + INTERVAL '8 hours', NOW(), NOW());

-- Vérifier le comptage
SELECT COUNT(*) as future_sessions_count FROM session s
INNER JOIN coaching_request cr ON cr.id = s.coaching_request_id
WHERE cr.user_id = 1
  AND s.status IN ('confirmed', 'proposed_by_user', 'proposed_by_coach')
  AND (
    CAST(COALESCE(s.scheduled_at, s.proposed_time_by_coach, s.proposed_time_by_user) AS date) > CURRENT_DATE
    OR (
      CAST(COALESCE(s.scheduled_at, s.proposed_time_by_coach, s.proposed_time_by_user) AS date) = CURRENT_DATE
      AND CAST(COALESCE(s.scheduled_at, s.proposed_time_by_coach, s.proposed_time_by_user) AS time) > CURRENT_TIME
    )
  );
```

**Résultat attendu**: `future_sessions_count = 4` (si heure_debut > heure_actuelle)

### Créer une session pour aujourd'hui (passé)

```sql
-- Session aujourd'hui à 08:00 (si heure actuelle > 08:00)
INSERT INTO session (coaching_request_id, status, scheduled_at, created_at, updated_at)
VALUES (1, 'confirmed', NOW() - INTERVAL '2 hours', NOW(), NOW());

-- Vérifier que le comptage ne change pas
SELECT COUNT(*) as future_sessions_count FROM session s
INNER JOIN coaching_request cr ON cr.id = s.coaching_request_id
WHERE cr.user_id = 1
  AND s.status IN ('confirmed', 'proposed_by_user', 'proposed_by_coach')
  AND (
    CAST(COALESCE(s.scheduled_at, s.proposed_time_by_coach, s.proposed_time_by_user) AS date) > CURRENT_DATE
    OR (
      CAST(COALESCE(s.scheduled_at, s.proposed_time_by_coach, s.proposed_time_by_user) AS date) = CURRENT_DATE
      AND CAST(COALESCE(s.scheduled_at, s.proposed_time_by_coach, s.proposed_time_by_user) AS time) > CURRENT_TIME
    )
  );
```

**Résultat attendu**: `future_sessions_count = 4` (inchangé, session passée non comptée)

---

## 7. Tester l'Annulation

### Annuler une session

```sql
-- Annuler la première session
UPDATE session SET status = 'cancelled' 
WHERE coaching_request_id = 1 
AND status = 'confirmed'
LIMIT 1;

-- Vérifier que le comptage diminue
SELECT COUNT(*) as future_sessions_count FROM session s
INNER JOIN coaching_request cr ON cr.id = s.coaching_request_id
WHERE cr.user_id = 1
  AND s.status IN ('confirmed', 'proposed_by_user', 'proposed_by_coach')
  AND (
    CAST(COALESCE(s.scheduled_at, s.proposed_time_by_coach, s.proposed_time_by_user) AS date) > CURRENT_DATE
    OR (
      CAST(COALESCE(s.scheduled_at, s.proposed_time_by_coach, s.proposed_time_by_user) AS date) = CURRENT_DATE
      AND CAST(COALESCE(s.scheduled_at, s.proposed_time_by_coach, s.proposed_time_by_user) AS time) > CURRENT_TIME
    )
  );
```

**Résultat attendu**: `future_sessions_count = 2` (diminué de 1)

---

## 8. Afficher Toutes les Sessions de l'Utilisateur

```sql
-- Afficher toutes les sessions de l'utilisateur 1
SELECT 
  s.id,
  s.status,
  s.scheduled_at,
  CASE 
    WHEN CAST(COALESCE(s.scheduled_at, s.proposed_time_by_coach, s.proposed_time_by_user) AS date) > CURRENT_DATE THEN 'FUTURE'
    WHEN CAST(COALESCE(s.scheduled_at, s.proposed_time_by_coach, s.proposed_time_by_user) AS date) = CURRENT_DATE 
      AND CAST(COALESCE(s.scheduled_at, s.proposed_time_by_coach, s.proposed_time_by_user) AS time) > CURRENT_TIME THEN 'FUTURE'
    ELSE 'PAST'
  END as is_future
FROM session s
INNER JOIN coaching_request cr ON cr.id = s.coaching_request_id
WHERE cr.user_id = 1
ORDER BY s.scheduled_at DESC;
```

**Résultat attendu**: Liste de toutes les sessions avec leur statut (FUTURE/PAST)

---

## 9. Nettoyer les Données de Test

### Supprimer toutes les sessions de test

```sql
-- Supprimer les sessions de test
DELETE FROM session 
WHERE coaching_request_id IN (
  SELECT id FROM coaching_request WHERE user_id = 1
);

-- Supprimer la coaching_request de test
DELETE FROM coaching_request WHERE user_id = 1;

-- Supprimer les utilisateurs de test
DELETE FROM "user" WHERE email IN ('test@example.com', 'coach@example.com');
```

---

## 📊 Résumé des Tests SQL

| Test | Requête | Résultat Attendu |
|------|---------|------------------|
| 1 | Vérifier tables | 3 tables existent |
| 2 | Créer utilisateurs | 2 utilisateurs créés |
| 3 | Créer coaching_request | 1 request créée |
| 4 | Compter sessions (0) | 0 sessions |
| 5 | Créer 3 sessions | 3 sessions créées |
| 6 | Compter sessions (3) | 3 sessions |
| 7 | Créer session completed | Comptage = 3 (inchangé) |
| 8 | Créer session cancelled | Comptage = 3 (inchangé) |
| 9 | Session aujourd'hui (futur) | Comptage = 4 |
| 10 | Session aujourd'hui (passé) | Comptage = 4 (inchangé) |
| 11 | Annuler une session | Comptage = 2 |
| 12 | Afficher toutes sessions | Liste complète |

---

## ✅ Validation Finale

Quand tous les tests SQL passent:

✅ La requête SQL est correcte  
✅ Les statuts sont correctement filtrés  
✅ Les dates futures sont correctement identifiées  
✅ Les sessions d'aujourd'hui sont correctement gérées  
✅ L'annulation fonctionne correctement  

**La logique de base de données est validée!** 🚀

---

**Date**: May 5, 2026  
**Version**: 1.0  
**Status**: ✅ READY FOR SQL TESTING
