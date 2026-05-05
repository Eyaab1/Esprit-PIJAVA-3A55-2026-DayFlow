# 🧪 Guide de Test - Session Reservation Limit

## Vue d'ensemble

Ce guide explique comment tester la règle métier de limitation de réservation (maximum 3 sessions futures par utilisateur).

---

## 📋 Prérequis

### Base de Données
- ✅ PostgreSQL en cours d'exécution
- ✅ Base de données `pidev_db` créée
- ✅ Tables `session`, `coaching_request`, `user` existantes

### Application
- ✅ DayFlow compilée et prête à démarrer
- ✅ Utilisateur de test créé (ex: ID = 1)
- ✅ Coach de test créé (ex: ID = 2)

---

## 🧪 Scénarios de Test

### Scénario 1: Vérifier que l'utilisateur peut réserver quand il a 0 sessions

#### Étapes:
1. **Démarrer l'application**
   - Lancer DayFlow
   - Se connecter avec l'utilisateur de test

2. **Vérifier l'état initial**
   - Aller à la section "Réserver une session"
   - Vérifier que le message affiche: "Vous pouvez réserver 3 session(s) de plus"
   - Vérifier que le bouton "Réserver" est **ACTIVÉ** (vert)

3. **Réserver une session**
   - Cliquer sur "Réserver une session"
   - Sélectionner un coach
   - Sélectionner une date et heure
   - Cliquer sur "Confirmer"

4. **Vérifier le résultat**
   - ✅ Session créée avec succès
   - ✅ Message: "Session réservée!"
   - ✅ Compteur mis à jour: "Sessions futures: 1/3"
   - ✅ Créneaux restants: "2"

#### Résultat attendu:
```
✅ SUCCÈS - Session créée
Sessions futures: 1/3
Réservations restantes: 2
```

---

### Scénario 2: Réserver 2 sessions supplémentaires

#### Étapes:
1. **Réserver la 2ème session**
   - Cliquer sur "Réserver une session"
   - Sélectionner un coach différent (ou même coach, date/heure différente)
   - Confirmer

2. **Vérifier le compteur**
   - Sessions futures: 2/3
   - Réservations restantes: 1
   - Bouton "Réserver": **ACTIVÉ**

3. **Réserver la 3ème session**
   - Cliquer sur "Réserver une session"
   - Sélectionner un coach
   - Confirmer

4. **Vérifier le compteur final**
   - Sessions futures: 3/3
   - Réservations restantes: 0
   - Bouton "Réserver": **DÉSACTIVÉ** (gris)

#### Résultat attendu:
```
✅ SUCCÈS - 3 sessions créées
Sessions futures: 3/3
Réservations restantes: 0
Bouton "Réserver": DÉSACTIVÉ
```

---

### Scénario 3: Tenter de réserver une 4ème session (DOIT ÉCHOUER)

#### Étapes:
1. **Vérifier l'état**
   - Bouton "Réserver": **DÉSACTIVÉ** (gris)
   - Message: "⚠️ Limite atteinte - Aucune réservation possible"

2. **Tenter de réserver**
   - Essayer de cliquer sur le bouton "Réserver"
   - Le bouton ne doit pas réagir (désactivé)

3. **Vérifier les logs**
   - Ouvrir la console (F12 ou logs de l'application)
   - Chercher le message:
     ```
     [SessionReservationValidator] User 1 has 3 future sessions
     [SessionReservationValidator] User 1 can book: false (current: 3, max: 3)
     ```

#### Résultat attendu:
```
❌ BLOQUÉ - Limite atteinte
Bouton "Réserver": DÉSACTIVÉ
Message d'erreur affiché
Logs: "Reservation blocked for user 1: limit reached (3/3)"
```

---

### Scénario 4: Annuler une session et vérifier que la limite est levée

#### Étapes:
1. **Afficher les sessions**
   - Aller à "Mes sessions"
   - Vérifier que 3 sessions sont affichées

2. **Annuler une session**
   - Cliquer sur une session
   - Cliquer sur "Annuler"
   - Confirmer l'annulation

3. **Vérifier le compteur**
   - Sessions futures: 2/3
   - Réservations restantes: 1
   - Bouton "Réserver": **RÉACTIVÉ** (vert)

4. **Réserver une nouvelle session**
   - Cliquer sur "Réserver une session"
   - Sélectionner un coach
   - Confirmer

5. **Vérifier le résultat**
   - ✅ Session créée avec succès
   - Sessions futures: 3/3
   - Réservations restantes: 0

#### Résultat attendu:
```
✅ SUCCÈS - Limite levée après annulation
Sessions futures: 2/3 → 3/3
Réservations restantes: 1 → 0
Nouvelle session créée
```

---

### Scénario 5: Terminer une session et vérifier que la limite est levée

#### Étapes:
1. **Afficher les sessions**
   - Aller à "Mes sessions"
   - Vérifier que 3 sessions sont affichées

2. **Terminer une session**
   - Cliquer sur une session
   - Cliquer sur "Marquer comme terminée"
   - Confirmer

3. **Vérifier le compteur**
   - Sessions futures: 2/3 (la session terminée n'est plus comptée)
   - Réservations restantes: 1
   - Bouton "Réserver": **RÉACTIVÉ** (vert)

4. **Réserver une nouvelle session**
   - Cliquer sur "Réserver une session"
   - Confirmer

5. **Vérifier le résultat**
   - ✅ Session créée avec succès
   - Sessions futures: 3/3

#### Résultat attendu:
```
✅ SUCCÈS - Limite levée après fin de session
Sessions futures: 2/3 → 3/3
Réservations restantes: 1 → 0
Nouvelle session créée
```

---

### Scénario 6: Tester avec une session pour aujourd'hui

#### Étapes:
1. **Créer une session pour aujourd'hui**
   - Réserver une session pour aujourd'hui à 14:00
   - Confirmer

2. **Vérifier le compteur**
   - Si l'heure actuelle < 14:00: Session comptée comme future
   - Si l'heure actuelle > 14:00: Session NOT comptée (passée)

3. **Vérifier les logs**
   - Chercher:
     ```
     [SessionReservationValidator] Counting future sessions for user 1
     [SessionReservationValidator] User 1 has X future sessions
     ```

#### Résultat attendu:
```
✅ SUCCÈS - Sessions d'aujourd'hui correctement comptées
Si heure_debut > heure_actuelle: Comptée comme future
Si heure_debut < heure_actuelle: NOT comptée
```

---

## 🔍 Vérifications dans les Logs

### Où trouver les logs?

**Option 1: Console de l'IDE**
- Si vous lancez depuis IntelliJ/Eclipse
- Cherchez l'onglet "Console"

**Option 2: Fichier de logs**
- Cherchez `logs/` ou `target/logs/`
- Fichier: `application.log`

**Option 3: Sortie standard**
- Lancez l'application en ligne de commande:
  ```bash
  java -jar target/DayFlow-1.0-SNAPSHOT.jar
  ```

### Messages à chercher

#### ✅ Succès (Réservation autorisée)
```
[SessionReservationValidator] Counting future sessions for user 1
[SessionReservationValidator] User 1 has 2 future sessions
[SessionReservationValidator] User 1 can book: true (current: 2, max: 3)
[SessionService] Attempting to reserve session for user 1
[SessionService] Reservation allowed, creating session for user 1
[SessionService] Session created successfully with ID 123
```

#### ❌ Échec (Limite atteinte)
```
[SessionReservationValidator] Counting future sessions for user 1
[SessionReservationValidator] User 1 has 3 future sessions
[SessionReservationValidator] User 1 can book: false (current: 3, max: 3)
[SessionReservationValidator] Reservation blocked for user 1: limit reached (3/3)
[SessionReservationValidator] RESERVATION REFUSED - User: 1, Reason: ..., Timestamp: ...
[SessionService] Reservation blocked: Utilisateur 1 a atteint la limite de 3 sessions futures
```

---

## 📊 Checklist de Test

### Test 1: Réservation Autorisée (0 sessions)
- [ ] Bouton "Réserver" activé
- [ ] Message: "Vous pouvez réserver 3 session(s)"
- [ ] Session créée avec succès
- [ ] Compteur: 1/3
- [ ] Logs: "can book: true"

### Test 2: Réservation Autorisée (1 session)
- [ ] Bouton "Réserver" activé
- [ ] Message: "Vous pouvez réserver 2 session(s)"
- [ ] Session créée avec succès
- [ ] Compteur: 2/3
- [ ] Logs: "can book: true"

### Test 3: Réservation Autorisée (2 sessions)
- [ ] Bouton "Réserver" activé
- [ ] Message: "Vous pouvez réserver 1 session(s)"
- [ ] Session créée avec succès
- [ ] Compteur: 3/3
- [ ] Logs: "can book: true"

### Test 4: Réservation Bloquée (3 sessions)
- [ ] Bouton "Réserver" désactivé
- [ ] Message: "⚠️ Limite atteinte"
- [ ] Impossible de cliquer sur le bouton
- [ ] Compteur: 3/3
- [ ] Logs: "can book: false"

### Test 5: Limite Levée (Annulation)
- [ ] Annuler une session
- [ ] Compteur: 2/3
- [ ] Bouton "Réserver" réactivé
- [ ] Nouvelle session créée
- [ ] Compteur: 3/3

### Test 6: Limite Levée (Fin)
- [ ] Terminer une session
- [ ] Compteur: 2/3
- [ ] Bouton "Réserver" réactivé
- [ ] Nouvelle session créée
- [ ] Compteur: 3/3

### Test 7: Sessions d'Aujourd'hui
- [ ] Session créée pour aujourd'hui à 14:00
- [ ] Si heure_actuelle < 14:00: Comptée
- [ ] Si heure_actuelle > 14:00: NOT comptée

---

## 🐛 Dépannage

### Problème: Le bouton "Réserver" reste activé même avec 3 sessions

**Cause possible**: Les sessions ne sont pas comptées correctement

**Solution**:
1. Vérifier les logs pour: `User X has Y future sessions`
2. Vérifier que les sessions ont le statut `confirmed` ou `proposed_by_user`
3. Vérifier que les dates sont dans le futur

### Problème: Le message d'erreur ne s'affiche pas

**Cause possible**: L'exception n'est pas capturée dans le contrôleur

**Solution**:
1. Vérifier les logs pour: `RESERVATION REFUSED`
2. Vérifier que le contrôleur catch `ReservationLimitExceededException`
3. Vérifier que le message est affiché dans l'UI

### Problème: Les logs ne s'affichent pas

**Cause possible**: Le niveau de log n'est pas configuré

**Solution**:
1. Vérifier `logback.xml` ou `log4j.properties`
2. Ajouter: `<logger name="services.coaching_session_module" level="DEBUG"/>`
3. Redémarrer l'application

---

## 📈 Cas Limites à Tester

### Cas 1: Session à minuit
- Créer une session pour demain à 00:00
- Vérifier qu'elle est comptée comme future

### Cas 2: Session dans 1 minute
- Créer une session pour dans 1 minute
- Vérifier qu'elle est comptée comme future

### Cas 3: Session il y a 1 minute
- Créer une session pour il y a 1 minute
- Vérifier qu'elle n'est PAS comptée

### Cas 4: Plusieurs utilisateurs
- Créer 2 utilisateurs
- Utilisateur 1: 3 sessions (bloqué)
- Utilisateur 2: 0 sessions (peut réserver)
- Vérifier que chaque utilisateur a sa propre limite

### Cas 5: Statuts différents
- Créer une session avec statut `completed`
- Vérifier qu'elle n'est PAS comptée
- Créer une session avec statut `cancelled`
- Vérifier qu'elle n'est PAS comptée

---

## 📝 Rapport de Test

### Template à utiliser:

```
TEST: [Nom du test]
DATE: [Date]
UTILISATEUR: [ID]
SESSIONS AVANT: [Nombre]

ÉTAPES:
1. [Étape 1]
2. [Étape 2]
3. [Étape 3]

RÉSULTAT ATTENDU:
- [Attente 1]
- [Attente 2]

RÉSULTAT OBTENU:
- [Résultat 1]
- [Résultat 2]

STATUT: ✅ SUCCÈS / ❌ ÉCHEC

LOGS PERTINENTS:
[Copier les logs importants]

NOTES:
[Observations supplémentaires]
```

---

## 🎯 Résumé des Tests

| Test | Objectif | Résultat |
|------|----------|----------|
| 1 | Réserver avec 0 sessions | ✅ Autorisé |
| 2 | Réserver avec 1 session | ✅ Autorisé |
| 3 | Réserver avec 2 sessions | ✅ Autorisé |
| 4 | Réserver avec 3 sessions | ❌ Bloqué |
| 5 | Annuler et réserver | ✅ Autorisé |
| 6 | Terminer et réserver | ✅ Autorisé |
| 7 | Sessions d'aujourd'hui | ✅ Correct |

---

## ✅ Validation Finale

Quand tous les tests passent:

✅ La limite de 3 sessions est appliquée  
✅ Les sessions futures sont correctement comptées  
✅ Les statuts sont correctement filtrés  
✅ Les messages d'erreur sont affichés  
✅ Les logs sont corrects  
✅ Les cas limites sont gérés  

**La fonctionnalité est prête pour la production!** 🚀

---

**Date**: May 5, 2026  
**Version**: 1.0  
**Status**: ✅ READY FOR TESTING
