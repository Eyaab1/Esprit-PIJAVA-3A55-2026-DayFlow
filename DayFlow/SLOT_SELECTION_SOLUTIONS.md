# ✅ Solutions - Créneau Sélectionné N'Apparaît Pas

**Objectif**: Fournir les solutions pour que la demande apparaisse après sélection d'un créneau  
**Date**: 5 mai 2026

---

## 🎯 Résumé du Problème

**Symptôme**: Tu sélectionnes un créneau mais il n'apparaît pas dans "Mes demandes"

**Cause**: Une ou plusieurs étapes du flux manquent

**Solution**: Ajouter le code manquant

---

## 🔧 Solutions Possibles

### Solution 1: Ajouter une Boîte de Dialogue de Confirmation

**Problème**: Après sélection du créneau, rien ne se passe

**Solution**: Afficher une boîte de dialogue pour confirmer

**Où ajouter**: `CalendarCoachController.java`

**Code à ajouter**:

```java
// Quand l'utilisateur clique sur un créneau
private void handleSlotSelection(LocalDateTime slot) {
    System.out.println("Créneau sélectionné: " + slot);
    
    // Afficher une boîte de dialogue de confirmation
    showConfirmationDialog(slot);
}

// Afficher la boîte de dialogue
private void showConfirmationDialog(LocalDateTime slot) {
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Confirmer la réservation");
    alert.setHeaderText("Voulez-vous réserver ce créneau?");
    alert.setContentText(
        "Créneau: " + slot + "\n" +
        "Coach: " + getSelectedCoachName() + "\n" +
        "Cliquez OK pour confirmer"
    );
    
    Optional<ButtonType> result = alert.showAndWait();
    
    if (result.isPresent() && result.get() == ButtonType.OK) {
        // Créer la demande
        createRequest(slot);
    }
}

// Créer la demande
private void createRequest(LocalDateTime slot) {
    try {
        // Créer l'objet
        CoachingRequest request = new CoachingRequest();
        request.setUserId(getCurrentUserId());
        request.setCoachId(getSelectedCoachId());
        request.setMessage("Demande de coaching pour le créneau: " + slot);
        request.setPriority("normal");
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

### Solution 2: Ajouter un Formulaire Détaillé

**Problème**: Tu veux que l'utilisateur remplisse plus de détails

**Solution**: Afficher un formulaire avec plusieurs champs

**Où ajouter**: `CalendarCoachController.java`

**Code à ajouter**:

```java
private void showDetailedForm(LocalDateTime slot) {
    Dialog<CoachingRequest> dialog = new Dialog<>();
    dialog.setTitle("Créer une demande de coaching");
    dialog.setHeaderText("Remplissez les détails de votre demande");
    
    // Créer les champs
    Label slotLabel = new Label("Créneau: " + slot);
    Label coachLabel = new Label("Coach: " + getSelectedCoachName());
    
    TextArea messageArea = new TextArea();
    messageArea.setPromptText("Décrivez votre objectif...");
    messageArea.setWrapText(true);
    messageArea.setPrefRowCount(4);
    
    ComboBox<String> priorityCombo = new ComboBox<>();
    priorityCombo.getItems().addAll("Normal", "Moyen", "Urgent");
    priorityCombo.setValue("Normal");
    
    // Créer le layout
    VBox content = new VBox(10);
    content.setPadding(new Insets(10));
    content.getChildren().addAll(
        slotLabel,
        coachLabel,
        new Label("Message:"),
        messageArea,
        new Label("Priorité:"),
        priorityCombo
    );
    
    dialog.getDialogPane().setContent(content);
    
    // Ajouter les boutons
    ButtonType confirmButton = new ButtonType("Confirmer", ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(confirmButton, ButtonType.CANCEL);
    
    // Afficher et traiter le résultat
    Optional<ButtonType> result = dialog.showAndWait();
    
    if (result.isPresent() && result.get() == confirmButton) {
        createRequestWithDetails(
            slot,
            messageArea.getText(),
            priorityCombo.getValue()
        );
    }
}

private void createRequestWithDetails(LocalDateTime slot, String message, String priority) {
    try {
        // Créer l'objet
        CoachingRequest request = new CoachingRequest();
        request.setUserId(getCurrentUserId());
        request.setCoachId(getSelectedCoachId());
        request.setMessage(message);
        request.setPriority(priority.toLowerCase());
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

### Solution 3: Ajouter un Bouton "Réserver" dans le Calendrier

**Problème**: Tu veux un bouton explicite pour réserver

**Solution**: Ajouter un bouton "Réserver" qui s'active quand un créneau est sélectionné

**Où ajouter**: `calendar_coach.fxml` et `CalendarCoachController.java`

**Code FXML**:

```xml
<!-- Ajouter ce bouton dans le FXML -->
<Button fx:id="reserveButton" 
        text="Réserver le créneau sélectionné"
        onAction="#handleReserveClick"
        disable="true"
        style="-fx-font-size: 14; -fx-padding: 10;" />
```

**Code Java**:

```java
@FXML
private Button reserveButton;

private LocalDateTime selectedSlot;

// Quand l'utilisateur clique sur un créneau
private void handleSlotSelection(LocalDateTime slot) {
    selectedSlot = slot;
    reserveButton.setDisable(false);
    System.out.println("Créneau sélectionné: " + slot);
}

// Quand l'utilisateur clique sur "Réserver"
@FXML
private void handleReserveClick() {
    if (selectedSlot != null) {
        showDetailedForm(selectedSlot);
    }
}
```

---

### Solution 4: Vérifier que la Demande Est Sauvegardée

**Problème**: La demande est créée mais pas affichée

**Solution**: Vérifier que la sauvegarde fonctionne

**Code à ajouter**:

```java
private void createRequest(LocalDateTime slot) {
    try {
        // Créer l'objet
        CoachingRequest request = new CoachingRequest();
        request.setUserId(getCurrentUserId());
        request.setCoachId(getSelectedCoachId());
        request.setMessage("Demande de coaching");
        request.setPriority("normal");
        request.setStatus("pending");
        request.setProposedTimeByUser(slot);
        
        // Sauvegarder
        coachingRequestService.create(request);
        
        // ✅ VÉRIFIER: Afficher l'ID créé
        System.out.println("Demande créée avec ID: " + request.getId());
        
        // ✅ VÉRIFIER: Afficher les détails
        System.out.println("User ID: " + request.getUserId());
        System.out.println("Coach ID: " + request.getCoachId());
        System.out.println("Status: " + request.getStatus());
        System.out.println("Slot: " + request.getProposedTimeByUser());
        
        // Afficher succès
        showSuccess("Demande créée avec succès!");
        
        // Recharger la liste
        navigateToMesDemandes();
        
    } catch (Exception e) {
        System.err.println("Erreur lors de la création: " + e.getMessage());
        e.printStackTrace();
        showError("Erreur: " + e.getMessage());
    }
}
```

---

### Solution 5: Vérifier que la Liste Est Rechargée

**Problème**: La demande est créée mais n'apparaît pas dans "Mes demandes"

**Solution**: Vérifier que la liste est rechargée

**Code à ajouter**:

```java
private void navigateToMesDemandes() {
    try {
        // Charger la vue
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/views/mes_demandes.fxml")
        );
        Parent root = loader.load();
        
        // Obtenir le contrôleur
        MesDemandesController controller = loader.getController();
        
        // Recharger les données
        controller.loadRequests();
        
        // Afficher la vue
        Stage stage = (Stage) reserveButton.getScene().getWindow();
        stage.setScene(new Scene(root));
        
    } catch (IOException e) {
        System.err.println("Erreur lors de la navigation: " + e.getMessage());
        e.printStackTrace();
    }
}
```

---

### Solution 6: Vérifier le Filtre

**Problème**: La demande apparaît ailleurs (pas dans "Mes demandes")

**Solution**: Vérifier que le filtre n'exclut pas la demande

**Code à ajouter dans MesDemandesController**:

```java
private void loadRequests() {
    try {
        // Récupérer les demandes
        List<CoachingRequest> requests = coachingRequestService.getByUserId(getCurrentUserId());
        
        // ✅ VÉRIFIER: Afficher le nombre de demandes
        System.out.println("Nombre de demandes: " + requests.size());
        
        // ✅ VÉRIFIER: Afficher chaque demande
        for (CoachingRequest request : requests) {
            System.out.println("Demande ID: " + request.getId() + 
                             ", Status: " + request.getStatus() + 
                             ", Coach: " + request.getCoachId());
        }
        
        // Afficher dans le tableau
        tableView.setItems(FXCollections.observableArrayList(requests));
        
        // Appliquer les filtres
        applyFilters();
        
    } catch (SQLException e) {
        System.err.println("Erreur lors du chargement: " + e.getMessage());
        e.printStackTrace();
    }
}
```

---

## 📋 Checklist - Quelle Solution Appliquer?

### Si rien ne se passe après sélection
→ **Solution 1 ou 2**: Ajouter une boîte de dialogue

### Si tu veux un bouton explicite
→ **Solution 3**: Ajouter un bouton "Réserver"

### Si la demande n'apparaît pas
→ **Solution 4 ou 5**: Vérifier la sauvegarde et le rechargement

### Si la demande apparaît ailleurs
→ **Solution 6**: Vérifier le filtre

---

## 🔧 Étapes d'Implémentation

### Étape 1: Choisir une Solution
```
Lire les solutions ci-dessus
Choisir celle qui correspond à ton problème
```

### Étape 2: Ajouter le Code
```
Copier le code de la solution
Ajouter dans le fichier approprié
```

### Étape 3: Compiler
```
mvn clean compile
Vérifier qu'il n'y a pas d'erreurs
```

### Étape 4: Tester
```
Démarrer l'application
Sélectionner un créneau
Vérifier que la demande apparaît
```

### Étape 5: Déboguer si Nécessaire
```
Regarder les logs
Vérifier la base de données
Appliquer la solution 4 ou 6
```

---

## 📊 Tableau - Quelle Solution?

| Problème | Solution | Fichier | Complexité |
|----------|----------|---------|-----------|
| Rien ne se passe | 1 ou 2 | CalendarCoachController | Facile |
| Pas de bouton | 3 | FXML + Controller | Moyen |
| Pas d'affichage | 4 ou 5 | CalendarCoachController | Moyen |
| Affiche ailleurs | 6 | MesDemandesController | Facile |

---

## ✅ Résumé

### Le Problème
Tu sélectionnes un créneau mais il n'apparaît pas dans "Mes demandes"

### Les Solutions
1. Ajouter une boîte de dialogue de confirmation
2. Ajouter un formulaire détaillé
3. Ajouter un bouton "Réserver"
4. Vérifier que la demande est sauvegardée
5. Vérifier que la liste est rechargée
6. Vérifier que le filtre n'exclut pas la demande

### Comment Choisir
- Rien ne se passe? → Solution 1 ou 2
- Pas de bouton? → Solution 3
- Pas d'affichage? → Solution 4 ou 5
- Affiche ailleurs? → Solution 6

---

**Status**: ✅ SOLUTIONS  
**Date**: 5 mai 2026  
**Version**: 1.0

**Prochaine étape**: Choisir une solution et l'implémenter

