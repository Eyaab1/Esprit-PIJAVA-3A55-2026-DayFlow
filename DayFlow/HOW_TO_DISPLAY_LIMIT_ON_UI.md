# 🎨 Comment Afficher la Limite sur l'Interface

**Objectif**: Afficher "Sessions futures: X/3" sur l'interface  
**Pas de modification de code existant** - Juste expliquer comment faire  
**Date**: 5 mai 2026

---

## 📋 Résumé Rapide

Tu dois afficher 3 choses:

1. **Compteur**: "Sessions futures: 2/3"
2. **Bouton**: Vert (cliquable) ou Gris (non cliquable)
3. **Message d'erreur**: "Limite atteinte"

---

## 🎯 Étape 1: Récupérer les Données

### Où Ajouter le Code?

**Dans ton contrôleur** (exemple: `SessionReservationController.java`):

```java
// Au démarrage de la page
@Override
public void initialize(URL location, ResourceBundle resources) {
    // Récupérer l'ID de l'utilisateur
    int userId = getCurrentUserId();
    
    // Récupérer les données de la limite
    try {
        int futureSessionsCount = sessionService.countFutureSessions(userId);
        boolean canBook = sessionService.canBookSession(userId);
        int remainingSlots = sessionService.getRemainingSlots(userId);
        
        // Afficher les données
        updateUI(futureSessionsCount, canBook, remainingSlots);
    } catch (SQLException e) {
        showError("Erreur: " + e.getMessage());
    }
}
```

---

## 🎯 Étape 2: Afficher le Compteur

### Dans le Fichier FXML

**Ajouter ce code dans ton fichier `.fxml`**:

```xml
<!-- Compteur de sessions -->
<HBox spacing="10" style="-fx-padding: 10;">
    <Label text="Sessions futures:" 
           style="-fx-font-weight: bold; -fx-font-size: 14;" />
    
    <Label fx:id="sessionCountLabel" 
           text="0/3"
           style="-fx-font-size: 14; -fx-text-fill: green;" />
</HBox>
```

### Dans le Contrôleur

**Ajouter cette variable**:

```java
@FXML
private Label sessionCountLabel;

// Méthode pour mettre à jour le compteur
private void updateSessionCount(int count) {
    sessionCountLabel.setText(count + "/3");
    
    // Changer la couleur
    if (count >= 3) {
        sessionCountLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
    } else if (count >= 2) {
        sessionCountLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
    } else {
        sessionCountLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
    }
}
```

---

## 🎯 Étape 3: Afficher le Message Informatif

### Dans le Fichier FXML

```xml
<!-- Message informatif -->
<Label fx:id="remainingSlotsLabel" 
       text="Vous pouvez réserver 3 session(s)"
       style="-fx-font-size: 12; -fx-text-fill: blue;" />
```

### Dans le Contrôleur

```java
@FXML
private Label remainingSlotsLabel;

// Méthode pour mettre à jour le message
private void updateRemainingSlots(int remaining) {
    if (remaining > 0) {
        remainingSlotsLabel.setText("Vous pouvez réserver " + remaining + " session(s)");
        remainingSlotsLabel.setVisible(true);
    } else {
        remainingSlotsLabel.setText("Limite atteinte - Annulez une session pour continuer");
        remainingSlotsLabel.setStyle("-fx-text-fill: red;");
        remainingSlotsLabel.setVisible(true);
    }
}
```

---

## 🎯 Étape 4: Activer/Désactiver le Bouton

### Dans le Fichier FXML

```xml
<!-- Bouton de réservation -->
<Button fx:id="reserveButton" 
        text="Réserver"
        onAction="#handleReservation"
        style="-fx-font-size: 14; -fx-padding: 10;" />
```

### Dans le Contrôleur

```java
@FXML
private Button reserveButton;

// Méthode pour activer/désactiver le bouton
private void updateButtonState(boolean canBook) {
    reserveButton.setDisable(!canBook);
    
    if (canBook) {
        reserveButton.setStyle("-fx-text-fill: white; -fx-background-color: green;");
        reserveButton.setTooltip(new Tooltip("Cliquez pour réserver une session"));
    } else {
        reserveButton.setStyle("-fx-text-fill: gray; -fx-background-color: lightgray;");
        reserveButton.setTooltip(new Tooltip("Vous avez atteint la limite de 3 sessions"));
    }
}
```

---

## 🎯 Étape 5: Gérer l'Erreur

### Quand l'Utilisateur Clique sur "Réserver"

```java
@FXML
private void handleReservation() {
    try {
        int userId = getCurrentUserId();
        
        // Vérifier la limite
        sessionService.validateReservation(userId);
        
        // Créer la session
        Session session = new Session();
        // ... remplir les propriétés ...
        sessionService.addSession(session);
        
        // Succès - mettre à jour l'interface
        showSuccess("Session réservée avec succès!");
        refreshUI();
        
    } catch (ReservationLimitExceededException e) {
        // Afficher le message d'erreur
        showErrorDialog(e.getUserFriendlyMessage());
        
    } catch (SQLException e) {
        showErrorDialog("Erreur base de données: " + e.getMessage());
    }
}

// Afficher une boîte de dialogue d'erreur
private void showErrorDialog(String message) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle("Erreur");
    alert.setHeaderText("Impossible de réserver");
    alert.setContentText(message);
    alert.showAndWait();
}

// Afficher un message de succès
private void showSuccess(String message) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle("Succès");
    alert.setHeaderText("Opération réussie");
    alert.setContentText(message);
    alert.showAndWait();
}
```

---

## 📊 Exemple Complet

### Structure FXML

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>

<VBox spacing="15" style="-fx-padding: 20;">
    
    <!-- Titre -->
    <Label text="Réserver une Session" 
           style="-fx-font-size: 18; -fx-font-weight: bold;" />
    
    <!-- Compteur -->
    <HBox spacing="10">
        <Label text="Sessions futures:" 
               style="-fx-font-weight: bold;" />
        <Label fx:id="sessionCountLabel" 
               text="0/3"
               style="-fx-text-fill: green;" />
    </HBox>
    
    <!-- Message informatif -->
    <Label fx:id="remainingSlotsLabel" 
           text="Vous pouvez réserver 3 session(s)"
           style="-fx-text-fill: blue;" />
    
    <!-- Sélection de date -->
    <DatePicker fx:id="datePicker" />
    
    <!-- Sélection d'heure -->
    <ComboBox fx:id="timeCombo" />
    
    <!-- Bouton -->
    <Button fx:id="reserveButton" 
            text="Réserver"
            onAction="#handleReservation"
            style="-fx-font-size: 14; -fx-padding: 10;" />
    
</VBox>
```

### Contrôleur Complet

```java
public class SessionReservationController implements Initializable {
    
    @FXML private Label sessionCountLabel;
    @FXML private Label remainingSlotsLabel;
    @FXML private Button reserveButton;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> timeCombo;
    
    private SessionService sessionService;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        sessionService = new SessionService();
        refreshUI();
    }
    
    // Mettre à jour l'interface
    private void refreshUI() {
        try {
            int userId = getCurrentUserId();
            
            // Récupérer les données
            int count = sessionService.countFutureSessions(userId);
            boolean canBook = sessionService.canBookSession(userId);
            int remaining = sessionService.getRemainingSlots(userId);
            
            // Mettre à jour l'affichage
            updateSessionCount(count);
            updateRemainingSlots(remaining);
            updateButtonState(canBook);
            
        } catch (SQLException e) {
            showError("Erreur: " + e.getMessage());
        }
    }
    
    // Mettre à jour le compteur
    private void updateSessionCount(int count) {
        sessionCountLabel.setText(count + "/3");
        
        if (count >= 3) {
            sessionCountLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        } else if (count >= 2) {
            sessionCountLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
        } else {
            sessionCountLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        }
    }
    
    // Mettre à jour le message
    private void updateRemainingSlots(int remaining) {
        if (remaining > 0) {
            remainingSlotsLabel.setText("Vous pouvez réserver " + remaining + " session(s)");
            remainingSlotsLabel.setStyle("-fx-text-fill: blue;");
        } else {
            remainingSlotsLabel.setText("Limite atteinte - Annulez une session pour continuer");
            remainingSlotsLabel.setStyle("-fx-text-fill: red;");
        }
    }
    
    // Mettre à jour le bouton
    private void updateButtonState(boolean canBook) {
        reserveButton.setDisable(!canBook);
        
        if (canBook) {
            reserveButton.setStyle("-fx-text-fill: white; -fx-background-color: green;");
            reserveButton.setTooltip(new Tooltip("Cliquez pour réserver"));
        } else {
            reserveButton.setStyle("-fx-text-fill: gray; -fx-background-color: lightgray;");
            reserveButton.setTooltip(new Tooltip("Limite atteinte"));
        }
    }
    
    // Gérer la réservation
    @FXML
    private void handleReservation() {
        try {
            int userId = getCurrentUserId();
            
            // Vérifier la limite
            sessionService.validateReservation(userId);
            
            // Créer la session
            Session session = new Session();
            session.setUserId(userId);
            session.setScheduledAt(datePicker.getValue());
            // ... autres propriétés ...
            
            sessionService.addSession(session);
            
            // Succès
            showSuccess("Session réservée avec succès!");
            refreshUI();
            
        } catch (ReservationLimitExceededException e) {
            showErrorDialog(e.getUserFriendlyMessage());
        } catch (SQLException e) {
            showErrorDialog("Erreur: " + e.getMessage());
        }
    }
    
    // Afficher erreur
    private void showErrorDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText("Impossible de réserver");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Afficher succès
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText("Opération réussie");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Afficher erreur simple
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Récupérer l'ID de l'utilisateur
    private int getCurrentUserId() {
        // À implémenter selon votre système d'authentification
        return 1; // Exemple
    }
}
```

---

## 🎨 Résultat Visuel

### Avant (0 sessions)
```
┌─────────────────────────────────────┐
│ Réserver une Session                │
├─────────────────────────────────────┤
│                                     │
│ Sessions futures: 0/3 ✅            │
│ Vous pouvez réserver 3 session(s)   │
│                                     │
│ [Sélectionner une date]             │
│ [Sélectionner une heure]            │
│                                     │
│ [Réserver] (VERT - ACTIVÉ)          │
│                                     │
└─────────────────────────────────────┘
```

### Après 1ère réservation (1 session)
```
┌─────────────────────────────────────┐
│ Réserver une Session                │
├─────────────────────────────────────┤
│                                     │
│ Sessions futures: 1/3 ✅            │
│ Vous pouvez réserver 2 session(s)   │
│                                     │
│ [Sélectionner une date]             │
│ [Sélectionner une heure]            │
│                                     │
│ [Réserver] (VERT - ACTIVÉ)          │
│                                     │
└─────────────────────────────────────┘
```

### Après 3 réservations (3 sessions)
```
┌─────────────────────────────────────┐
│ Réserver une Session                │
├─────────────────────────────────────┤
│                                     │
│ Sessions futures: 3/3 ❌            │
│ Limite atteinte - Annulez une       │
│ session pour continuer              │
│                                     │
│ [Sélectionner une date]             │
│ [Sélectionner une heure]            │
│                                     │
│ [Réserver] (GRIS - DÉSACTIVÉ)       │
│                                     │
└─────────────────────────────────────┘
```

---

## 📝 Checklist

### À Ajouter au FXML
- [ ] Label pour le compteur
- [ ] Label pour le message informatif
- [ ] Bouton de réservation

### À Ajouter au Contrôleur
- [ ] Variables @FXML pour les composants
- [ ] Méthode `refreshUI()`
- [ ] Méthode `updateSessionCount()`
- [ ] Méthode `updateRemainingSlots()`
- [ ] Méthode `updateButtonState()`
- [ ] Méthode `handleReservation()`
- [ ] Méthode `showErrorDialog()`
- [ ] Méthode `showSuccess()`

### À Tester
- [ ] Compteur affiche 0/3 au démarrage
- [ ] Compteur passe à 1/3 après réservation
- [ ] Compteur passe à 2/3 après 2ème réservation
- [ ] Compteur passe à 3/3 après 3ème réservation
- [ ] Bouton devient gris à 3/3
- [ ] Message d'erreur affiche quand limite atteinte
- [ ] Compteur revient à 2/3 après annulation

---

## 🔗 Méthodes Disponibles

### SessionService

```java
// Compter les sessions futures
int countFutureSessions(int userId) throws SQLException

// Vérifier si on peut réserver
boolean canBookSession(int userId) throws SQLException

// Obtenir les slots restants
int getRemainingSlots(int userId) throws SQLException

// Valider avant réservation
void validateReservation(int userId) throws ReservationLimitExceededException, SQLException

// Créer une session
void addSession(Session session) throws SQLException
```

### Exceptions

```java
// Exception levée quand limite atteinte
ReservationLimitExceededException
├─ getUserFriendlyMessage() → Message à afficher
├─ getCurrentCount() → Nombre actuel
├─ getMaxLimit() → Limite maximale
└─ getRemainingSlots() → Slots restants
```

---

## ✅ Résumé

### À Faire

1. **Ajouter au FXML**:
   - Label pour compteur
   - Label pour message
   - Bouton de réservation

2. **Ajouter au Contrôleur**:
   - Variables @FXML
   - Méthode refreshUI()
   - Méthodes de mise à jour
   - Gestion d'erreur

3. **Tester**:
   - Compteur se met à jour
   - Bouton change d'état
   - Message d'erreur affiche

### Pas de Modification de Code Existant
- ✅ Juste ajouter du nouveau code
- ✅ Utiliser les méthodes existantes
- ✅ Pas de changement dans SessionService
- ✅ Pas de changement dans SessionReservationValidator

---

**Status**: ✅ GUIDE D'AFFICHAGE UI  
**Date**: 5 mai 2026  
**Version**: 1.0

**Prochaine étape**: Implémenter dans votre contrôleur

