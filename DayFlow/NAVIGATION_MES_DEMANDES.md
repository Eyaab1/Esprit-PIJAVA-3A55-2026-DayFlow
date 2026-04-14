# Navigation vers "Mes demandes" - Modifications effectuées

## Résumé
Le bouton/lien "Mes demandes" a été connecté à la page `mes_demandes.fxml` dans toute l'application.

## Fichiers modifiés

### 1. coaching_request.fxml
**Emplacement** : `DayFlow/src/main/resources/views/coaching_request.fxml`

**Modification** :
- Ajout de `onAction="#goToMesDemandes"` au bouton "Mes demandes"

```xml
<Button fx:id="mesDemandesButton" text="Mes demandes" styleClass="nav-button"
        onAction="#goToMesDemandes"
        style="-fx-background-color: transparent; -fx-text-fill: #2d3436; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 8 16 8 16;">
</Button>
```

### 2. CoachingRequestController.java
**Emplacement** : `DayFlow/src/main/java/controllers/CoachingRequestController.java`

**Modifications** :
- Ajout de l'import `javafx.event.ActionEvent`
- Ajout de l'import `javafx.scene.Node`
- Ajout de la méthode `goToMesDemandes(ActionEvent event)`

```java
@FXML
private void goToMesDemandes(ActionEvent event) {
    System.out.println("Navigation vers Mes demandes OK");
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/mes_demandes.fxml"));
        Parent root = loader.load();
        
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Mes demandes de coaching");
        stage.show();
        
    } catch (IOException e) {
        showError("Erreur de navigation", "Impossible de charger la page Mes demandes");
        e.printStackTrace();
    }
}
```

### 3. find_coach.fxml
**Emplacement** : `DayFlow/src/main/resources/views/find_coach.fxml`

**Modifications** :
- Ajout du contrôleur `fx:controller="controllers.FindCoachViewController"`
- Ajout de `fx:id="mesDemandesButton"` et `onAction="#goToMesDemandes"` au bouton

```xml
<ScrollPane xmlns="http://javafx.com/javafx"
            xmlns:fx="http://javafx.com/fxml/1"
            fx:controller="controllers.FindCoachViewController"
            fitToWidth="true">
```

```xml
<Button fx:id="mesDemandesButton" text="Mes demandes" onAction="#goToMesDemandes"/>
```

### 4. FindCoachViewController.java (NOUVEAU)
**Emplacement** : `DayFlow/src/main/java/controllers/FindCoachViewController.java`

**Création** : Nouveau contrôleur pour gérer la navigation depuis find_coach.fxml

```java
package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;

public class FindCoachViewController {

    @FXML
    private void goToMesDemandes(ActionEvent event) {
        System.out.println("Navigation vers Mes demandes OK");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/mes_demandes.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Mes demandes de coaching");
            stage.show();
            
        } catch (IOException e) {
            showError("Erreur de navigation", "Impossible de charger la page Mes demandes");
            e.printStackTrace();
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
```

### 5. NavbarController.java
**Emplacement** : `DayFlow/src/main/java/controllers/components/NavbarController.java`

**Modification** :
- Remplacement de `toastSoon("Mes demandes")` par la navigation réelle

**AVANT** :
```java
@FXML
private void onMesDemandes() {
    toastSoon("Mes demandes");
}
```

**APRÈS** :
```java
@FXML
private void onMesDemandes() {
    System.out.println("Navigation vers Mes demandes OK");
    navigate("/views/mes_demandes.fxml", "DayFlow — Mes demandes");
}
```

## Points de navigation

L'utilisateur peut maintenant accéder à "Mes demandes" depuis :

1. **coaching_request.fxml** : Bouton "Mes demandes" dans la navbar
2. **find_coach.fxml** : Bouton "Mes demandes" dans la navbar
3. **navbar.fxml** : Lien "Mes demandes" dans la navbar globale (utilisé dans toutes les pages)

## Vérifications effectuées

✅ Chemin du fichier FXML correct : `/views/mes_demandes.fxml`
✅ Nom du fichier respecte les majuscules/minuscules
✅ Contrôleur lié dans mes_demandes.fxml : `fx:controller="controllers.MesDemandesController"`
✅ Message "bientôt disponible" supprimé
✅ Message de debug ajouté : `System.out.println("Navigation vers Mes demandes OK")`

## Test

Pour tester la navigation :

1. Lancer l'application
2. Cliquer sur "Mes demandes" depuis n'importe quelle page
3. Vérifier que la page "Mes demandes de coaching" s'affiche
4. Vérifier dans la console : "Navigation vers Mes demandes OK"

## Navigation retour

Depuis la page "Mes demandes", l'utilisateur peut :
- Cliquer sur "Nouvelle demande" → Ouvre coaching_request.fxml
- Cliquer sur "Modifier" → Ouvre coaching_request.fxml avec données pré-remplies
- Utiliser la navbar pour naviguer vers d'autres sections

## Notes

- La navigation utilise `NavigationManager.show()` dans NavbarController (méthode existante)
- La navigation utilise `FXMLLoader` directement dans les autres contrôleurs
- Le titre de la fenêtre change à "Mes demandes de coaching" ou "DayFlow — Mes demandes"
- Aucune modification de la logique métier ou de la base de données
