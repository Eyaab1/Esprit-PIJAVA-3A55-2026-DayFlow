# 📖 Résumé d'Implémentation UI - Limite de Réservation

**Objectif**: Afficher la limite de 3 sessions sur l'interface  
**Format**: Résumé complet avec exemples  
**Date**: 5 mai 2026

---

## 🎯 Résumé Exécutif

Tu dois afficher 3 éléments sur l'interface:

1. **Compteur**: "Sessions futures: X/3"
2. **Message**: "Vous pouvez réserver Y session(s)"
3. **Bouton**: ACTIVÉ (vert) ou DÉSACTIVÉ (gris)

**Pas de modification du code existant** - Juste ajouter du nouveau code.

---

## 📋 Ce Qu'il Faut Faire

### Étape 1: Ajouter les Composants FXML

**Fichier**: `calendar_coach.fxml` (ou ton fichier FXML)

```xml
<!-- Ajouter ces lignes dans ton VBox/HBox -->

<!-- Compteur -->
<Label fx:id="sessionCountLabel" 
       text="0/3"
       style="-fx-font-size: 14; -fx-text-fill: green; -fx-font-weight: bold;" />

<!-- Message -->
<Label fx:id="remainingSlotsLabel" 
       text="Vous pouvez réserver 3 session(s)"
       style="-fx-font-size: 12; -fx-text-fill: blue;" />

<!-- Bouton (modifier le bouton existant) -->
<Button fx:id="reserveButton" 
        text="Réserver"
        onAction="#handleReservation"
        style="-fx-font-size: 14; -fx-padding: 10;" />
```

### Étape 2: Ajouter les Variables au Contrôleur

**Fichier**: `CalendarCoachController.java` (ou ton contrôleur)

```java
@FXML
private Label sessionCountLabel;

@FXML
private Label remainingSlotsLabel;

@FXML
private Button reserveButton;

private SessionService sessionService;
```

### Étape 3: Initialiser au Démarrage

```java
@Override
public void initialize(URL location, ResourceBundle resources) {
    sessionService = new SessionService();
    refreshUI();
}

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
```

### Étape 4: Ajouter les Méthodes de Mise à Jour

```java
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

private void updateRemainingSlots(int remaining) {
    if (remaining > 0) {
        remainingSlotsLabel.setText("Vous pouvez réserver " + remaining + " session(s)");
        remainingSlotsLabel.setStyle("-fx-text-fill: blue;");
    } else {
        remainingSlotsLabel.setText("Limite atteinte - Annulez une session pour continuer");
        remainingSlotsLabel.setStyle("-fx-text-fill: red;");
    }
}

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

### Étape 5: Gérer la Réservation

```java
@FXML
private void handleReservation() {
    try {
        int userId = getCurrentUserId();
        
        // Vérifier la limite
        sessionService.validateReservation(userId);
        
        // Créer la session
        Session session = new Session();
        session.setUserId(userId);
        // ... remplir les autres propriétés ...
        
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

private void showErrorDialog(String message) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle("Erreur");
    alert.setHeaderText("Impossible de réserver");
    alert.setContentText(message);
    alert.showAndWait();
}

private void showSuccess(String message) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle("Succès");
    alert.setHeaderText("Opération réussie");
    alert.setContentText(message);
    alert.showAndWait();
}

private void showError(String message) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle("Erreur");
    alert.setContentText(message);
    alert.showAndWait();
}

private int getCurrentUserId() {
    // À implémenter selon votre système d'authentification
    return 1; // Exemple
}
```

---

## 📊 Résultat Visuel

### Avant (0 sessions)
```
Sessions futures: 0/3 ✅
Vous pouvez réserver 3 session(s)
[Réserver] (VERT - ACTIVÉ)
```

### Après 1ère réservation (1 session)
```
Sessions futures: 1/3 ✅
Vous pouvez réserver 2 session(s)
[Réserver] (VERT - ACTIVÉ)
```

### Après 2ème réservation (2 sessions)
```
Sessions futures: 2/3 ⚠️
Vous pouvez réserver 1 session
[Réserver] (VERT - ACTIVÉ)
```

### Après 3ème réservation (3 sessions)
```
Sessions futures: 3/3 ❌
Limite atteinte - Annulez une session pour continuer
[Réserver] (GRIS - DÉSACTIVÉ)
```

---

## 🔧 Intégration avec le Code Existant

### Méthodes Disponibles

```java
// SessionService
sessionService.countFutureSessions(userId)      // Retourne: int
sessionService.canBookSession(userId)           // Retourne: boolean
sessionService.getRemainingSlots(userId)        // Retourne: int
sessionService.validateReservation(userId)      // Lance: ReservationLimitExceededException
sessionService.addSession(session)              // Crée la session

// ReservationLimitExceededException
exception.getUserFriendlyMessage()               // Retourne: String
exception.getCurrentCount()                     // Retourne: int
exception.getMaxLimit()                         // Retourne: int
exception.getRemainingSlots()                   // Retourne: int
```

### Imports Nécessaires

```java
import services.coaching_session_module.SessionService;
import exceptions.ReservationLimitExceededException;
import model.coaching_session.Session;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.SQLException;
```

---

## 📋 Checklist d'Implémentation

### FXML
- [ ] Label pour compteur ajouté
- [ ] Label pour message ajouté
- [ ] Bouton de réservation modifié
- [ ] fx:id assignés correctement

### Contrôleur
- [ ] Variables @FXML déclarées
- [ ] SessionService initialisé
- [ ] Méthode initialize() implémentée
- [ ] Méthode refreshUI() implémentée
- [ ] Méthode updateSessionCount() implémentée
- [ ] Méthode updateRemainingSlots() implémentée
- [ ] Méthode updateButtonState() implémentée
- [ ] Méthode handleReservation() implémentée
- [ ] Gestion d'erreur implémentée

### Test
- [ ] Compteur affiche 0/3 au démarrage
- [ ] Compteur passe à 1/3 après réservation
- [ ] Compteur passe à 2/3 après 2ème réservation
- [ ] Compteur passe à 3/3 après 3ème réservation
- [ ] Bouton devient gris à 3/3
- [ ] Message d'erreur affiche quand limite atteinte
- [ ] Compteur revient à 2/3 après annulation

---

## 🎨 Styles CSS

### Couleurs Recommandées

```css
/* Vert (OK) */
-fx-text-fill: green;
-fx-background-color: #4CAF50;

/* Orange (Attention) */
-fx-text-fill: orange;
-fx-background-color: #FF9800;

/* Rouge (Erreur) */
-fx-text-fill: red;
-fx-background-color: #F44336;

/* Gris (Désactivé) */
-fx-text-fill: gray;
-fx-background-color: lightgray;
```

### Styles Complets

```css
/* Compteur - Vert */
-fx-font-size: 14; -fx-text-fill: green; -fx-font-weight: bold;

/* Compteur - Orange */
-fx-font-size: 14; -fx-text-fill: orange; -fx-font-weight: bold;

/* Compteur - Rouge */
-fx-font-size: 14; -fx-text-fill: red; -fx-font-weight: bold;

/* Message */
-fx-font-size: 12; -fx-text-fill: blue;

/* Bouton - Activé */
-fx-text-fill: white; -fx-background-color: green; -fx-font-size: 14; -fx-padding: 10;

/* Bouton - Désactivé */
-fx-text-fill: gray; -fx-background-color: lightgray; -fx-font-size: 14; -fx-padding: 10;
```

---

## 📚 Documentation Disponible

### Pour Comprendre
- `UI_INTEGRATION_GUIDE.md` - Guide complet d'intégration
- `HOW_TO_DISPLAY_LIMIT_ON_UI.md` - Comment afficher la limite
- `UI_VISUAL_GUIDE.md` - Guide visuel avec diagrammes

### Pour Tester
- `QUICK_TEST_CHECKLIST.md` - Test rapide (5 min)
- `TESTING_GUIDE_SESSION_RESERVATION_LIMIT.md` - Test complet (30 min)

### Pour Comprendre le Code
- `SESSION_RESERVATION_LIMIT_GUIDE.md` - Guide technique
- `SessionReservationValidator.java` - Logique de validation
- `SessionService.java` - Service layer

---

## 🔗 Fichiers à Modifier

### À Modifier
1. **Ton fichier FXML** (ex: `calendar_coach.fxml`)
   - Ajouter les Labels et Button

2. **Ton contrôleur** (ex: `CalendarCoachController.java`)
   - Ajouter les variables @FXML
   - Ajouter les méthodes

### À NE PAS Modifier
- `SessionService.java` - Déjà implémenté
- `SessionReservationValidator.java` - Déjà implémenté
- `ReservationLimitExceededException.java` - Déjà implémenté

---

## ✅ Résumé

### À Faire
1. Ajouter 3 composants au FXML (Label, Label, Button)
2. Ajouter 3 variables @FXML au contrôleur
3. Ajouter 5 méthodes au contrôleur
4. Initialiser au démarrage
5. Tester

### Pas de Modification de Code Existant
- ✅ Juste ajouter du nouveau code
- ✅ Utiliser les méthodes existantes
- ✅ Pas de changement dans SessionService
- ✅ Pas de changement dans SessionReservationValidator

### Résultat
- ✅ Compteur affiche "X/3"
- ✅ Couleur change (vert → orange → rouge)
- ✅ Bouton change d'état (activé → désactivé)
- ✅ Message d'erreur affiche quand limite atteinte

---

## 🎯 Prochaines Étapes

1. **Lire** les guides d'intégration
2. **Ajouter** les composants FXML
3. **Ajouter** les variables et méthodes au contrôleur
4. **Compiler** et tester
5. **Valider** que tout fonctionne

---

**Status**: ✅ RÉSUMÉ D'IMPLÉMENTATION UI  
**Date**: 5 mai 2026  
**Version**: 1.0

**Prochaine étape**: Implémenter selon ce résumé

