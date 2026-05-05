# ⚡ Quick Test Checklist - Session Reservation Limit

## 🚀 Test Rapide (5 minutes)

### Étape 1: Démarrer l'application
```
1. Lancer DayFlow
2. Se connecter avec un utilisateur de test
3. Aller à "Réserver une session"
```

### Étape 2: Vérifier l'état initial
```
✓ Bouton "Réserver": ACTIVÉ (vert)
✓ Message: "Vous pouvez réserver 3 session(s)"
✓ Compteur: "Sessions futures: 0/3"
```

### Étape 3: Réserver 3 sessions
```
Réservation 1:
  ✓ Cliquer "Réserver"
  ✓ Sélectionner coach + date/heure
  ✓ Confirmer
  ✓ Vérifier: "Sessions futures: 1/3"

Réservation 2:
  ✓ Cliquer "Réserver"
  ✓ Sélectionner coach + date/heure
  ✓ Confirmer
  ✓ Vérifier: "Sessions futures: 2/3"

Réservation 3:
  ✓ Cliquer "Réserver"
  ✓ Sélectionner coach + date/heure
  ✓ Confirmer
  ✓ Vérifier: "Sessions futures: 3/3"
```

### Étape 4: Tenter une 4ème réservation (DOIT ÉCHOUER)
```
✓ Bouton "Réserver": DÉSACTIVÉ (gris)
✓ Message: "⚠️ Limite atteinte"
✓ Impossible de cliquer
```

### Étape 5: Annuler une session
```
✓ Aller à "Mes sessions"
✓ Cliquer sur une session
✓ Cliquer "Annuler"
✓ Confirmer
✓ Vérifier: "Sessions futures: 2/3"
✓ Bouton "Réserver": RÉACTIVÉ (vert)
```

### Étape 6: Réserver une nouvelle session
```
✓ Cliquer "Réserver"
✓ Sélectionner coach + date/heure
✓ Confirmer
✓ Vérifier: "Sessions futures: 3/3"
✓ Bouton "Réserver": DÉSACTIVÉ (gris)
```

---

## 📊 Résultats Attendus

| Étape | Résultat | Status |
|-------|----------|--------|
| 1 | Application démarre | ✅ |
| 2 | État initial correct | ✅ |
| 3.1 | 1ère session créée | ✅ |
| 3.2 | 2ème session créée | ✅ |
| 3.3 | 3ème session créée | ✅ |
| 4 | 4ème session bloquée | ✅ |
| 5 | Limite levée après annulation | ✅ |
| 6 | Nouvelle session créée | ✅ |

---

## 🔍 Vérifications dans les Logs

### Chercher ces messages:

**Succès (Réservation 1-3)**:
```
[SessionReservationValidator] User X has Y future sessions
[SessionReservationValidator] User X can book: true (current: Y, max: 3)
[SessionService] Session created successfully with ID Z
```

**Échec (Réservation 4)**:
```
[SessionReservationValidator] User X has 3 future sessions
[SessionReservationValidator] User X can book: false (current: 3, max: 3)
[SessionReservationValidator] RESERVATION REFUSED
```

---

## ✅ Test Réussi Si:

- [ ] Réservations 1-3 créées avec succès
- [ ] Compteur mis à jour correctement (0/3 → 1/3 → 2/3 → 3/3)
- [ ] 4ème réservation bloquée
- [ ] Bouton "Réserver" désactivé à 3/3
- [ ] Limite levée après annulation
- [ ] Nouvelle réservation possible après annulation
- [ ] Logs affichent les bons messages

---

## ❌ Test Échoué Si:

- [ ] Réservation 4 créée (limite non appliquée)
- [ ] Compteur ne se met pas à jour
- [ ] Bouton "Réserver" reste activé à 3/3
- [ ] Limite non levée après annulation
- [ ] Messages d'erreur manquants
- [ ] Logs manquants ou incorrects

---

## 🎯 Temps Estimé

- Préparation: 1 min
- Test: 4 min
- **Total: 5 minutes**

---

**Status**: ✅ READY FOR QUICK TEST
