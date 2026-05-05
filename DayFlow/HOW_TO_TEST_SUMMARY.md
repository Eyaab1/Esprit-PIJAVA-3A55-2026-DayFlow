# 📖 How to Test - Session Reservation Limit (Summary)

## 🎯 Objectif

Tester la règle métier: **Limite de 3 sessions futures par utilisateur**

---

## 3 Façons de Tester

### 1️⃣ Test Rapide (5 minutes) - RECOMMANDÉ

**Fichier**: `QUICK_TEST_CHECKLIST.md`

**Étapes**:
1. Démarrer l'application
2. Réserver 3 sessions
3. Vérifier que la 4ème est bloquée
4. Annuler une session
5. Vérifier que la limite est levée

**Résultat**: ✅ ou ❌

---

### 2️⃣ Test Complet (30 minutes)

**Fichier**: `TESTING_GUIDE_SESSION_RESERVATION_LIMIT.md`

**Scénarios**:
- Réservation avec 0 sessions
- Réservation avec 1 session
- Réservation avec 2 sessions
- Réservation avec 3 sessions (bloquée)
- Annulation et nouvelle réservation
- Fin de session et nouvelle réservation
- Sessions d'aujourd'hui

**Résultat**: Rapport détaillé

---

### 3️⃣ Test SQL (15 minutes)

**Fichier**: `SQL_TEST_QUERIES.md`

**Étapes**:
1. Créer données de test
2. Exécuter requête de comptage
3. Vérifier les statuts exclus
4. Tester les sessions d'aujourd'hui
5. Tester l'annulation

**Résultat**: Vérification de la logique SQL

---

## 🚀 Démarrage Rapide

### Étape 1: Lire le guide approprié

```
Pour test rapide (5 min):
  → Lire: QUICK_TEST_CHECKLIST.md

Pour test complet (30 min):
  → Lire: TESTING_GUIDE_SESSION_RESERVATION_LIMIT.md

Pour test SQL (15 min):
  → Lire: SQL_TEST_QUERIES.md
```

### Étape 2: Préparer l'environnement

```
✓ PostgreSQL en cours d'exécution
✓ Base de données pidev_db créée
✓ Tables session, coaching_request, user existantes
✓ DayFlow compilée
```

### Étape 3: Exécuter les tests

```
Suivre les étapes du guide choisi
Vérifier les résultats attendus
Documenter les résultats
```

### Étape 4: Valider

```
✓ Tous les tests passent
✓ Logs affichent les bons messages
✓ UI affiche les bons messages
✓ Limite est appliquée correctement
```

---

## 📋 Checklist Rapide

### Avant de tester:
- [ ] PostgreSQL en cours d'exécution
- [ ] Base de données créée
- [ ] DayFlow compilée
- [ ] Utilisateur de test créé

### Pendant le test:
- [ ] Réserver 3 sessions
- [ ] Vérifier compteur: 0/3 → 1/3 → 2/3 → 3/3
- [ ] Vérifier bouton: ACTIVÉ → ACTIVÉ → ACTIVÉ → DÉSACTIVÉ
- [ ] Tenter 4ème réservation: BLOQUÉE
- [ ] Annuler une session: Compteur → 2/3, Bouton → ACTIVÉ
- [ ] Réserver nouvelle session: Compteur → 3/3, Bouton → DÉSACTIVÉ

### Après le test:
- [ ] Tous les tests passent
- [ ] Logs corrects
- [ ] Messages d'erreur affichés
- [ ] Limite appliquée correctement

---

## 🔍 Où Chercher les Preuves

### UI (Interface Utilisateur)
```
Compteur: "Sessions futures: X/3"
Message: "Vous pouvez réserver Y session(s)"
Bouton: ACTIVÉ (vert) ou DÉSACTIVÉ (gris)
Erreur: "⚠️ Limite atteinte"
```

### Logs (Console)
```
[SessionReservationValidator] User X has Y future sessions
[SessionReservationValidator] User X can book: true/false
[SessionService] Session created successfully
[SessionReservationValidator] RESERVATION REFUSED
```

### Base de Données
```
SELECT COUNT(*) FROM session WHERE user_id = X AND status IN (...)
```

---

## ✅ Résultats Attendus

### Test Réussi:
```
✅ Réservations 1-3 créées
✅ Compteur: 0/3 → 1/3 → 2/3 → 3/3
✅ Bouton: ACTIVÉ → ACTIVÉ → ACTIVÉ → DÉSACTIVÉ
✅ 4ème réservation bloquée
✅ Limite levée après annulation
✅ Logs corrects
```

### Test Échoué:
```
❌ 4ème réservation créée (limite non appliquée)
❌ Compteur ne se met pas à jour
❌ Bouton reste activé à 3/3
❌ Limite non levée après annulation
❌ Messages d'erreur manquants
❌ Logs manquants
```

---

## 📊 Temps Estimé

| Test | Temps |
|------|-------|
| Rapide | 5 min |
| Complet | 30 min |
| SQL | 15 min |
| **Total** | **50 min** |

---

## 🎯 Prochaines Étapes

### Si tous les tests passent:
```
✅ Fonctionnalité validée
✅ Prête pour la production
✅ Documenter les résultats
✅ Archiver les logs
```

### Si des tests échouent:
```
❌ Identifier le problème
❌ Vérifier les logs
❌ Vérifier la base de données
❌ Contacter le développeur
```

---

## 📞 Support

### Questions sur le test?
- Lire le guide complet: `TESTING_GUIDE_SESSION_RESERVATION_LIMIT.md`
- Vérifier les logs
- Exécuter les requêtes SQL

### Problèmes?
- Vérifier les prérequis
- Vérifier la base de données
- Vérifier les logs de l'application

---

## 📝 Documentation Disponible

1. **QUICK_TEST_CHECKLIST.md** - Test rapide (5 min)
2. **TESTING_GUIDE_SESSION_RESERVATION_LIMIT.md** - Test complet (30 min)
3. **SQL_TEST_QUERIES.md** - Test SQL (15 min)
4. **SESSION_RESERVATION_LIMIT_GUIDE.md** - Guide technique
5. **SESSION_RESERVATION_IMPLEMENTATION_SUMMARY.md** - Résumé implémentation
6. **IMPLEMENTATION_COMPLETE.md** - Vue d'ensemble complète

---

## ✨ Résumé

| Aspect | Détail |
|--------|--------|
| **Objectif** | Tester limite de 3 sessions |
| **Durée** | 5-50 min selon le test |
| **Prérequis** | PostgreSQL, DayFlow compilée |
| **Résultat** | ✅ ou ❌ |
| **Documentation** | 6 fichiers disponibles |

---

**Status**: ✅ READY FOR TESTING  
**Date**: May 5, 2026  
**Version**: 1.0
