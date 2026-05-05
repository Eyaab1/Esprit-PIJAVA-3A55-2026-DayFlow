# 🔍 Diagnostic - Problème de Sélection de Créneau

**Problème**: Quand tu sélectionnes un créneau (slot), il n'apparaît pas dans la liste de demandes  
**Date**: 5 mai 2026  
**Statut**: Diagnostic et solution

---

## 📋 Résumé du Problème

Tu fais ceci:
1. ✅ Tu cliques sur un créneau (slot)
2. ✅ Le créneau est bien sélectionné (visuellement)
3. ❌ Mais il n'apparaît PAS dans la liste de demandes pour l'utilisateur

---

## 🔍 Causes Possibles

### Cause 1: La Demande N'est Pas Créée en Base de Données
```
Flux attendu:
1. Sélectionner un créneau
2. Créer une demande de coaching
3. Sauvegarder en base de données
4. Afficher dans la liste

Flux actuel (problème):
1. Sélectionner un créneau
2. ??? (Rien ne se passe)
3. Pas de sauvegarde
4. Rien n'apparaît
```

### Cause 2: La Demande Est Créée Mais Pas Affichée
```
Possible si:
- La demande est créée en base de données
- Mais le contrôleur ne recharge pas la liste
- Ou le filtre cache la demande
```

### Cause 3: Mauvais Statut ou Utilisateur
```
Possible si:
- La demande est créée avec le mauvais utilisateur
- Ou avec le mauvais statut
- Et donc ne s'affiche pas dans le filtre
```

---

## 🔧 Flux Correct - Comment Ça Devrait Fonctionner

### Étape 1: Sélectionner un Créneau
```
Utilisateur clique sur un créneau disponible
↓
Le créneau est sélectionné (visuellement)
↓
Un formulaire s'affiche (ou une boîte de dialogue)
```

### Étape 2: Créer la Demande
```
Utilisateur remplit les détails:
- Message/Objectif
- Priorité
- Autres informations
↓
Utilisateur clique "Confirmer" ou "Réserver"
```

### Étape 3: Sauvegarder en Base de Données
```
Le contrôleur crée un objet CoachingRequest:
- user_id = ID de l'utilisateur
- coach_id = ID du coach
- message = Message de l'utilisateur
- priority = Priorité sélectionnée
- status = "pending" (en attente)
- proposed_time_by_user = Créneau sélectionné
↓
Sauvegarde en base de données
```

### Étape 4: Afficher dans la Liste
```
Le contrôleur recharge la liste des demandes
↓
La nouvelle demande apparaît dans "Mes demandes"
```

---

## 🎯 Où Est le Problème?

### Scénario 1: Pas de Création de Demande
**Symptôme**: Rien ne se passe quand tu cliques sur un créneau

**Solution**: 
- Vérifier qu'il y a un bouton "Confirmer" ou "Réserver"
- Vérifier que ce bouton appelle une méthode pour créer la demande
- Vérifier que la méthode sauvegarde en base de données

### Scénario 2: Création Mais Pas d'Affichage
**Symptôme**: La demande est créée mais n'apparaît pas

**Solution**:
- Vérifier que le contrôleur recharge la liste après création
- Vérifier que le filtre n'exclut pas la demande
- Vérifier que l'utilisateur est correct

### Scénario 3: Mauvais Statut
**Symptôme**: La demande apparaît ailleurs (pas dans "Mes demandes")

**Solution**:
- Vérifier le statut de la demande en base de données
- Vérifier que le statut est "pending" ou équivalent
- Vérifier que le filtre affiche ce statut

---

## 📊 Checklist de Diagnostic

### Avant de Cliquer sur un Créneau
- [ ] Y a-t-il un bouton "Confirmer" ou "Réserver"?
- [ ] Y a-t-il un formulaire pour remplir les détails?
- [ ] Y a-t-il un message d'erreur?

### Après avoir Cliqué sur un Créneau
- [ ] Un message de succès s'affiche-t-il?
- [ ] La page se recharge-t-elle?
- [ ] Reviens-tu à la liste des demandes?

### Dans la Base de Données
- [ ] La demande est-elle créée?
- [ ] Quel est le statut?
- [ ] Quel est l'utilisateur?
- [ ] Quel est le coach?

### Dans la Liste des Demandes
- [ ] La demande apparaît-elle?
- [ ] Avec quel statut?
- [ ] Avec quel utilisateur?

---

## 🔧 Solutions Possibles

### Solution 1: Ajouter un Bouton "Confirmer"
**Si le problème**: Pas de bouton pour confirmer la sélection

**À faire**:
1. Ajouter un bouton "Confirmer la réservation"
2. Ajouter une méthode pour créer la demande
3. Ajouter une méthode pour sauvegarder en base de données
4. Ajouter une méthode pour recharger la liste

### Solution 2: Recharger la Liste Après Création
**Si le problème**: La demande est créée mais n'apparaît pas

**À faire**:
1. Après création, appeler `loadRequests()` ou équivalent
2. Ou naviguer vers la page "Mes demandes"
3. Ou afficher un message de succès avec la nouvelle demande

### Solution 3: Vérifier le Filtre
**Si le problème**: La demande apparaît ailleurs

**À faire**:
1. Vérifier le statut de la demande
2. Vérifier le filtre appliqué
3. Réinitialiser les filtres si nécessaire

---

## 📝 Code à Vérifier

### Fichiers à Lire
1. **CalendarCoachController.java** (ou équivalent)
   - Méthode pour sélectionner un créneau
   - Méthode pour créer la demande
   - Méthode pour sauvegarder en base de données

2. **MesDemandesController.java**
   - Méthode `loadRequests()`
   - Méthode `applyFilters()`
   - Vérifier que la nouvelle demande est affichée

3. **CoachingRequestService.java**
   - Méthode pour créer une demande
   - Vérifier que la demande est sauvegardée correctement

---

## 🎯 Prochaines Étapes

### Étape 1: Identifier le Problème
```
1. Clique sur un créneau
2. Regarde s'il y a un bouton "Confirmer"
3. Regarde s'il y a un message d'erreur
4. Regarde s'il y a un message de succès
```

### Étape 2: Vérifier la Base de Données
```
1. Ouvre un client SQL (pgAdmin, DBeaver, etc.)
2. Exécute: SELECT * FROM coaching_request WHERE user_id = ?
3. Regarde si la nouvelle demande est là
4. Regarde le statut et les détails
```

### Étape 3: Vérifier la Liste des Demandes
```
1. Va à "Mes demandes"
2. Regarde si la demande apparaît
3. Regarde le statut et les filtres appliqués
4. Réinitialise les filtres si nécessaire
```

### Étape 4: Lire le Code
```
1. Lire CalendarCoachController.java
2. Lire MesDemandesController.java
3. Lire CoachingRequestService.java
4. Identifier où le problème se produit
```

---

## 📊 Tableau de Diagnostic

| Symptôme | Cause Probable | Solution |
|----------|---|---|
| Rien ne se passe | Pas de bouton "Confirmer" | Ajouter le bouton |
| Message d'erreur | Erreur lors de la création | Vérifier le message d'erreur |
| Succès mais pas d'affichage | Pas de rechargement de la liste | Ajouter `loadRequests()` |
| Affiche ailleurs | Mauvais statut ou filtre | Vérifier le statut et le filtre |
| Pas de demande en BD | Pas de sauvegarde | Vérifier la méthode de sauvegarde |

---

## 🔗 Fichiers Pertinents

### Contrôleurs
- `CalendarCoachController.java` - Sélection de créneau
- `MesDemandesController.java` - Affichage des demandes
- `CoachingRequestController.java` - Création de demande

### Services
- `CoachingRequestService.java` - Gestion des demandes
- `SessionService.java` - Gestion des sessions

### Vues FXML
- `calendar_coach.fxml` - Calendrier et sélection de créneau
- `mes_demandes.fxml` - Liste des demandes

---

## ✅ Résumé

### Le Problème
Tu sélectionnes un créneau mais il n'apparaît pas dans la liste de demandes

### Les Causes Possibles
1. Pas de bouton "Confirmer" pour créer la demande
2. La demande est créée mais pas affichée
3. La demande a le mauvais statut ou utilisateur

### La Solution
1. Identifier où le problème se produit
2. Vérifier la base de données
3. Vérifier le code du contrôleur
4. Ajouter le code manquant

---

**Status**: 🔍 DIAGNOSTIC  
**Date**: 5 mai 2026  
**Version**: 1.0

**Prochaine étape**: Lire les fichiers pertinents et identifier le problème exact

