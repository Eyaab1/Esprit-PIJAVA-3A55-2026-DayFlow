# 📊 Flux Complet - Du Créneau à la Demande

**Objectif**: Expliquer comment un créneau sélectionné devient une demande de coaching  
**Date**: 5 mai 2026

---

## 🎯 Vue d'Ensemble

### Flux Attendu (Correct)

```
┌─────────────────────────────────────────────────────────┐
│ 1. CALENDRIER - Sélectionner un Créneau                │
├─────────────────────────────────────────────────────────┤
│ Utilisateur voit: Calendrier avec créneaux disponibles │
│ Utilisateur clique: Sur un créneau (ex: 15 mai 10:00) │
│ Résultat: Créneau sélectionné (visuellement)           │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ 2. FORMULAIRE - Remplir les Détails                    │
├─────────────────────────────────────────────────────────┤
│ Utilisateur voit: Formulaire avec:                     │
│   - Créneau sélectionné (pré-rempli)                   │
│   - Coach sélectionné (pré-rempli)                     │
│   - Message/Objectif (à remplir)                       │
│   - Priorité (à sélectionner)                          │
│ Utilisateur remplit: Les champs vides                  │
│ Utilisateur clique: "Confirmer" ou "Réserver"          │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ 3. CRÉATION - Créer la Demande                         │
├─────────────────────────────────────────────────────────┤
│ Système crée: Objet CoachingRequest avec:              │
│   - user_id = ID de l'utilisateur                      │
│   - coach_id = ID du coach                             │
│   - message = Message de l'utilisateur                 │
│   - priority = Priorité sélectionnée                   │
│   - status = "pending" (en attente)                    │
│   - proposed_time_by_user = Créneau sélectionné        │
│ Système sauvegarde: En base de données                 │
│ Système affiche: Message de succès                     │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ 4. AFFICHAGE - Voir la Demande                         │
├─────────────────────────────────────────────────────────┤
│ Système recharge: La liste des demandes                │
│ Utilisateur voit: La nouvelle demande dans:            │
│   - "Mes demandes" (page principale)                   │
│   - Avec le statut "En attente"                        │
│   - Avec le coach sélectionné                          │
│   - Avec le créneau sélectionné                        │
└─────────────────────────────────────────────────────────┘
```

---

## 🔴 Flux Actuel (Problème)

```
┌─────────────────────────────────────────────────────────┐
│ 1. CALENDRIER - Sélectionner un Créneau                │
├─────────────────────────────────────────────────────────┤
│ Utilisateur clique: Sur un créneau                     │
│ Résultat: Créneau sélectionné (visuellement) ✅        │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ 2. ??? - Rien ne se passe                              │
├─────────────────────────────────────────────────────────┤
│ Pas de formulaire                                       │
│ Pas de bouton "Confirmer"                              │
│ Pas de message d'erreur                                │
│ Pas de message de succès                               │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ 3. RÉSULTAT - Rien n'apparaît                          │
├─────────────────────────────────────────────────────────┤
│ Pas de demande créée                                    │
│ Pas de demande en base de données                       │
│ Pas de demande dans "Mes demandes"                      │
└─────────────────────────────────────────────────────────┘
```

---

## 🔍 Où Est le Problème?

### Problème 1: Pas de Formulaire Après Sélection

**Symptôme**:
```
Tu cliques sur un créneau
↓
Rien ne se passe
↓
Pas de formulaire
```

**Cause Possible**:
- Le contrôleur n'affiche pas de formulaire après sélection
- Il n'y a pas de boîte de dialogue
- Il n'y a pas de navigation vers une autre page

**Solution**:
- Ajouter une boîte de dialogue pour remplir les détails
- Ou naviguer vers une page de confirmation
- Ou afficher un formulaire inline

---

### Problème 2: Pas de Bouton "Confirmer"

**Symptôme**:
```
Tu vois un formulaire
↓
Mais pas de bouton "Confirmer"
↓
Tu ne peux pas créer la demande
```

**Cause Possible**:
- Le bouton n'existe pas
- Le bouton est caché
- Le bouton est désactivé

**Solution**:
- Ajouter un bouton "Confirmer" ou "Réserver"
- Ajouter une action au bouton

---

### Problème 3: Pas de Création de Demande

**Symptôme**:
```
Tu cliques sur "Confirmer"
↓
Rien ne se passe
↓
Pas de demande créée
```

**Cause Possible**:
- Le bouton n'appelle pas de méthode
- La méthode n'existe pas
- La méthode ne crée pas la demande

**Solution**:
- Ajouter une méthode pour créer la demande
- Ajouter une méthode pour sauvegarder en base de données

---

### Problème 4: Pas d'Affichage de la Demande

**Symptôme**:
```
La demande est créée
↓
Mais elle n'apparaît pas dans "Mes demandes"
↓
Ou elle apparaît ailleurs
```

**Cause Possible**:
- Le contrôleur ne recharge pas la liste
- Le filtre exclut la demande
- La demande a le mauvais statut

**Solution**:
- Recharger la liste après création
- Vérifier le filtre
- Vérifier le statut

---

## 📋 Checklist - Qu'est-ce Qui Manque?

### Après Sélection du Créneau
- [ ] Un formulaire s'affiche?
- [ ] Ou une boîte de dialogue s'affiche?
- [ ] Ou une page de confirmation s'affiche?

### Dans le Formulaire
- [ ] Le créneau est pré-rempli?
- [ ] Le coach est pré-rempli?
- [ ] Y a-t-il un champ "Message"?
- [ ] Y a-t-il un champ "Priorité"?
- [ ] Y a-t-il un bouton "Confirmer"?

### Après Clic sur "Confirmer"
- [ ] Un message de succès s'affiche?
- [ ] La page se recharge?
- [ ] La demande apparaît dans "Mes demandes"?

---

## 🔧 Code Manquant - Exemple

### Étape 1: Sélectionner un Créneau

**Fichier**: `CalendarCoachController.java`

```java
// Quand l'utilisateur clique sur un créneau
private void handleSlotSelection(LocalDateTime slot) {
    System.out.println("Créneau sélectionné: " + slot);
    
    // ✅ À FAIRE: Afficher un formulaire ou une boîte de dialogue
    // Actuellement: Rien ne se passe
}
```

### Étape 2: Afficher un Formulaire

**À ajouter**:
```java
private void showConfirmationDialog(LocalDateTime slot) {
    // Créer une boîte de dialogue
    Dialog<CoachingRequest> dialog = new Dialog<>();
    dialog.setTitle("Confirmer la réservation");
    
    // Ajouter les champs
    TextField messageField = new TextField();
    ComboBox<String> priorityCombo = new ComboBox<>();
    priorityCombo.getItems().addAll("Normal", "Moyen", "Urgent");
    
    // Ajouter les boutons
    ButtonType confirmButton = new ButtonType("Confirmer", ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(confirmButton, ButtonType.CANCEL);
    
    // Afficher la boîte de dialogue
    Optional<CoachingRequest> result = dialog.showAndWait();
    
    if (result.isPresent()) {
        // Créer la demande
        createRequest(slot, messageField.getText(), priorityCombo.getValue());
    }
}
```

### Étape 3: Créer la Demande

**À ajouter**:
```java
private void createRequest(LocalDateTime slot, String message, String priority) {
    try {
        // Créer l'objet
        CoachingRequest request = new CoachingRequest();
        request.setUserId(getCurrentUserId());
        request.setCoachId(getSelectedCoachId());
        request.setMessage(message);
        request.setPriority(priority);
        request.setStatus("pending");
        request.setProposedTimeByUser(slot);
        
        // Sauvegarder
        coachingRequestService.create(request);
        
        // Afficher succès
        showSuccess("Demande créée avec succès!");
        
        // Recharger la liste
        navigateToMesDemandes();
        
    } catch (Exception e) {
        showError("Erreur: " + e.getMessage());
    }
}
```

---

## 📊 Tableau - Flux Complet

| Étape | Action | Résultat | Code |
|-------|--------|---------|------|
| 1 | Cliquer sur créneau | Créneau sélectionné | `handleSlotSelection()` |
| 2 | Afficher formulaire | Boîte de dialogue | `showConfirmationDialog()` |
| 3 | Remplir détails | Données saisies | Utilisateur remplit |
| 4 | Cliquer "Confirmer" | Demande créée | `createRequest()` |
| 5 | Sauvegarder | En base de données | `coachingRequestService.create()` |
| 6 | Recharger liste | Demande affichée | `navigateToMesDemandes()` |

---

## 🎯 Prochaines Étapes

### Étape 1: Identifier le Problème Exact
```
1. Clique sur un créneau
2. Regarde ce qui se passe
3. Y a-t-il un formulaire?
4. Y a-t-il un message d'erreur?
```

### Étape 2: Lire le Code
```
1. Ouvre: CalendarCoachController.java
2. Cherche: handleSlotSelection() ou équivalent
3. Regarde: Qu'est-ce qui se passe après?
```

### Étape 3: Ajouter le Code Manquant
```
1. Si pas de formulaire: Ajouter showConfirmationDialog()
2. Si pas de création: Ajouter createRequest()
3. Si pas d'affichage: Ajouter navigateToMesDemandes()
```

### Étape 4: Tester
```
1. Compiler
2. Démarrer l'application
3. Sélectionner un créneau
4. Vérifier que la demande apparaît
```

---

## 📝 Fichiers à Vérifier

### Contrôleurs
- `CalendarCoachController.java` - Sélection de créneau
- `MesDemandesController.java` - Affichage des demandes

### Services
- `CoachingRequestService.java` - Création de demande

### Vues FXML
- `calendar_coach.fxml` - Calendrier

---

## ✅ Résumé

### Le Flux Correct
1. Sélectionner un créneau
2. Afficher un formulaire
3. Remplir les détails
4. Créer la demande
5. Sauvegarder en base de données
6. Afficher dans "Mes demandes"

### Le Problème
Une ou plusieurs étapes manquent

### La Solution
Identifier quelle étape manque et ajouter le code

---

**Status**: 📊 FLUX COMPLET  
**Date**: 5 mai 2026  
**Version**: 1.0

**Prochaine étape**: Lire CalendarCoachController.java et identifier où le flux s'arrête

