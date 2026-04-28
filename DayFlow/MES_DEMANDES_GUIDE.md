# Guide - Interface Mes Demandes

## Vue d'ensemble

L'interface `mes_demandes.fxml` est une page dédiée à la gestion des demandes de coaching avec les fonctionnalités CRUD complètes (Create, Read, Update, Delete).

## Structure de l'interface

### 1. En-tête
- Titre : "Mes demandes de coaching"
- Sous-titre descriptif

### 2. Statistiques rapides
Quatre cartes colorées affichant :
- **Total** : Nombre total de demandes (bleu)
- **En attente** : Demandes en statut "pending" (orange)
- **Acceptées** : Demandes acceptées (vert)
- **Refusées** : Demandes refusées (rouge)

### 3. Barre de recherche et filtres
- **Champ de recherche** : Recherche en temps réel dans les messages
- **Filtre Statut** : Filtrer par statut (pending, accepted, declined, etc.)
- **Filtre Priorité** : Filtrer par priorité (normal, medium, urgent)
- **Bouton Filtrer** : Appliquer les filtres

### 4. TableView
Affiche toutes les demandes avec les colonnes :
- **ID** : Identifiant de la demande
- **Coach** : Nom complet du coach
- **Message** : Aperçu du message (tronqué à 50 caractères)
- **Priorité** : Normal, Moyen ou Urgent
- **Statut** : État actuel de la demande
- **Date** : Date de création (format dd/MM/yyyy HH:mm)

**Style des lignes** :
- Fond rouge clair pour les demandes urgentes
- Fond orange clair pour les demandes moyennes
- Fond blanc pour les demandes normales

### 5. Boutons d'action
- **Nouvelle demande** (vert) : Créer une nouvelle demande
- **Modifier** (violet) : Modifier la demande sélectionnée
- **Supprimer** (rouge) : Supprimer la demande sélectionnée
- **Actualiser** (bleu) : Recharger les données

### 6. Barre d'information
Affiche les détails de la demande sélectionnée

## Fonctionnalités

### READ (Lire)
- Les demandes sont chargées automatiquement au démarrage
- Filtrées par l'utilisateur connecté
- Affichées dans le TableView
- Statistiques mises à jour automatiquement

### CREATE (Créer)
1. Cliquer sur "Nouvelle demande"
2. Navigation vers `coaching_request.fxml`
3. Remplir le formulaire
4. Envoyer la demande

### UPDATE (Modifier)
1. Sélectionner une demande dans le tableau
2. Cliquer sur "Modifier"
3. Navigation vers `coaching_request.fxml` avec données pré-remplies
4. Le bouton "Envoyer" devient "Mettre à jour"
5. Modifier les champs souhaités
6. Cliquer sur "Mettre à jour"
7. Retour automatique à la liste

### DELETE (Supprimer)
1. Sélectionner une demande dans le tableau
2. Cliquer sur "Supprimer"
3. Confirmer la suppression dans la boîte de dialogue
4. La demande est supprimée de la base de données
5. Le tableau et les statistiques sont mis à jour

### Recherche et filtres
- **Recherche** : Tape dans le champ de recherche pour filtrer en temps réel
- **Filtres** : Sélectionner un statut et/ou une priorité
- **Appliquer** : Cliquer sur "Filtrer" pour appliquer les filtres combinés
- **Réinitialiser** : Sélectionner "Tous" / "Toutes" pour voir toutes les demandes

## Contrôleur Java

### MesDemandesController.java

#### Méthodes principales

- `initialize()` : Initialise l'interface, charge les données
- `setupFilters()` : Configure les filtres et la recherche
- `setupTableView()` : Configure le TableView et les colonnes
- `setupButtons()` : Configure les actions des boutons
- `loadRequests()` : Charge les demandes depuis la base de données
- `applyFilters()` : Applique les filtres de recherche
- `updateStatistics()` : Met à jour les statistiques affichées
- `handleNewRequest()` : Navigation vers la création
- `handleUpdate()` : Navigation vers la modification
- `handleDelete()` : Suppression avec confirmation

#### Services utilisés

- `CoachingRequestService` : Gestion des demandes
- `UserService` : Récupération des informations des coachs

## Navigation

### Depuis mes_demandes.fxml

- **Nouvelle demande** → `coaching_request.fxml` (mode création)
- **Modifier** → `coaching_request.fxml` (mode modification)

### Vers mes_demandes.fxml

- Depuis `coaching_request.fxml` après mise à jour
- Depuis la navbar (bouton "Mes demandes")

## Intégration dans la navbar

Pour ajouter un lien vers cette page dans la navbar de `coaching_request.fxml` :

```java
@FXML private Button mesDemandesButton;

mesDemandesButton.setOnAction(event -> {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/mes_demandes.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) mesDemandesButton.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Mes demandes");
    } catch (IOException e) {
        e.printStackTrace();
    }
});
```

## Personnalisation

### Modifier l'utilisateur connecté

Dans `MesDemandesController.java`, ligne 67 :
```java
private int currentUserId = 1; // TODO: Récupérer l'utilisateur connecté
```

### Ajouter des colonnes

1. Ajouter la colonne dans le FXML avec fx:id
2. Déclarer dans le contrôleur avec @FXML
3. Configurer dans setupTableView()

### Modifier les couleurs

Les couleurs sont définies inline dans le FXML :
- Statistiques : background-color dans les VBox
- Boutons : background-color dans les Button
- Lignes du tableau : setStyle() dans setRowFactory()

## Validation

- Sélection obligatoire pour modifier/supprimer
- Confirmation avant suppression
- Messages d'erreur clairs
- Retour automatique après modification

## Compatibilité

- ✅ JavaFX 17+
- ✅ Scene Builder compatible
- ✅ Design moderne et responsive
- ✅ Aucun ScrollPane imbriqué
- ✅ Structure VBox simple

## Avantages de cette approche

1. **Séparation des responsabilités** : Une page pour créer, une pour gérer
2. **Interface claire** : Tableau dédié avec statistiques
3. **Recherche et filtres** : Fonctionnalités avancées
4. **Navigation fluide** : Transitions entre les pages
5. **Compatible Scene Builder** : Facile à modifier visuellement
6. **Code propre** : Contrôleurs séparés et maintenables

## Notes importantes

- Les statistiques se mettent à jour automatiquement après chaque action
- La recherche fonctionne en temps réel (pas besoin de cliquer sur Filtrer)
- Les filtres sont cumulatifs (recherche + statut + priorité)
- Le style des lignes change selon la priorité pour une meilleure visibilité
- La confirmation de suppression évite les erreurs
