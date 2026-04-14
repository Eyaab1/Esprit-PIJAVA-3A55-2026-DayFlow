# Guide complet - Workflow de gestion des sessions de coaching

## Vue d'ensemble

Ce guide documente l'implémentation complète du workflow de gestion des sessions de coaching côté coach, avec des données dynamiques provenant de la base de données.

## Architecture

### Services

#### SessionService.java
**Emplacement** : `DayFlow/src/main/java/services/coaching_session_module/SessionService.java`

**Méthodes ajoutées** :
- `getSessionsByCoach(int coachId)` : Récupère toutes les sessions d'un coach
- `getSessionsByUser(int userId)` : Récupère toutes les sessions d'un utilisateur
- `addSession(Session session)` : Ajoute une nouvelle session
- `updateSession(Session session)` : Met à jour une session existante
- `deleteSession(int sessionId)` : Supprime une session

#### CoachingRequestService.java
**Emplacement** : `DayFlow/src/main/java/services/coaching_session_module/CoachingRequestService.java`

**Méthodes ajoutées** :
- `getRequestsByCoach(int coachId)` : Récupère toutes les demandes pour un coach
- `updateStatus(int requestId, String newStatus)` : Met à jour le statut d'une demande

## Interfaces FXML

### 1. coach_requests.fxml - Gestion des demandes

**Emplacement** : `DayFlow/src/main/resources/views/coach_requests.fxml`

**Fonctionnalités** :
- Affichage des demandes de coaching reçues
- Statistiques (en attente, acceptées, refusées)
- Filtrage par statut
- TableView avec colonnes : ID, Client, Message, Priorité, Statut, Date, Actions
- Boutons d'action dans chaque ligne :
  - **Accepter** : Change le statut à "ACCEPTEE"
  - **Refuser** : Change le statut à "REFUSEE"
  - **Créer session** : Disponible après acceptation

**Contrôleur** : `controllers.coach.CoachRequestsController`

**Données dynamiques** :
- Chargement depuis `CoachingRequestService.getRequestsByCoach()`
- Mise à jour en temps réel après chaque action
- Récupération des informations client depuis `UserService`

### 2. add_session.fxml - Créer une session

**Emplacement** : `DayFlow/src/main/resources/views/add_session.fxml`

**Fonctionnalités** :
- Affichage des informations du client
- Formulaire de création :
  - Date (DatePicker)
  - Heure (TextField format HH:MM)
  - Durée (ComboBox : 30, 45, 60, 90, 120 minutes)
  - Objectif (TextField)
  - Description (TextArea)
  - Prix (TextField)
- Boutons : Annuler, Créer la session

**Contrôleur** : `controllers.coach.AddSessionController`

**Données dynamiques** :
- Reçoit la `CoachingRequest` depuis l'écran précédent
- Charge les informations du client depuis la base de données
- Sauvegarde la session avec `SessionService.addSession()`
- Retour automatique à la liste des demandes

### 3. coach_sessions.fxml - Mes sessions

**Emplacement** : `DayFlow/src/main/resources/views/coach_sessions.fxml`

**Fonctionnalités** :
- Affichage de toutes les sessions du coach
- Statistiques (total, planifiées, terminées)
- TableView avec colonnes : ID, Client, Date, Heure, Durée, Objectif, Statut, Prix
- Boutons d'action :
  - **Modifier** : Ouvre le formulaire de modification
  - **Supprimer** : Supprime la session (avec confirmation)
  - **Marquer comme terminée** : Change le statut à "COMPLETED"

**Contrôleur** : `controllers.coach.CoachSessionsController`

**Données dynamiques** :
- Chargement depuis `SessionService.getSessionsByCoach()`
- Récupération des informations client via `CoachingRequestService` et `UserService`
- Mise à jour en temps réel après chaque action

### 4. edit_session.fxml - Modifier une session

**Emplacement** : `DayFlow/src/main/resources/views/edit_session.fxml`

**Fonctionnalités** :
- Formulaire pré-rempli avec les données existantes
- Champs modifiables :
  - Date
  - Heure
  - Durée
  - Objectif
  - Prix
  - Statut
- Boutons : Annuler, Enregistrer

**Contrôleur** : `controllers.coach.EditSessionController`

**Données dynamiques** :
- Reçoit la `Session` depuis l'écran précédent
- Charge les données existantes dans le formulaire
- Sauvegarde avec `SessionService.updateSession()`
- Retour automatique à la liste des sessions

## Contrôleurs

### CoachRequestsController.java

**Responsabilités** :
- Charger les demandes du coach connecté
- Afficher les statistiques
- Gérer les actions (accepter, refuser)
- Navigation vers la création de session

**Méthodes principales** :
- `initialize()` : Initialisation et chargement des données
- `loadRequests()` : Charge les demandes depuis la BD
- `handleAccept(CoachingRequest)` : Accepte une demande
- `handleRefuse(CoachingRequest)` : Refuse une demande
- `handleCreateSession(CoachingRequest)` : Navigation vers création de session
- `updateStatistics()` : Met à jour les compteurs

### AddSessionController.java

**Responsabilités** :
- Afficher les informations du client
- Valider le formulaire
- Créer la session dans la BD

**Méthodes principales** :
- `setCoachingRequest(CoachingRequest)` : Reçoit la demande
- `loadClientInfo()` : Charge les infos du client
- `handleCreate()` : Valide et crée la session
- `handleCancel()` : Annule et retourne

### CoachSessionsController.java

**Responsabilités** :
- Afficher toutes les sessions du coach
- Gérer les actions (modifier, supprimer, terminer)
- Afficher les statistiques

**Méthodes principales** :
- `loadSessions()` : Charge les sessions depuis la BD
- `handleModify()` : Navigation vers modification
- `handleDelete()` : Supprime une session
- `handleComplete()` : Marque comme terminée
- `updateStatistics()` : Met à jour les compteurs

### EditSessionController.java

**Responsabilités** :
- Charger les données existantes
- Valider les modifications
- Sauvegarder dans la BD

**Méthodes principales** :
- `setSession(Session)` : Reçoit la session à modifier
- `loadSessionData()` : Remplit le formulaire
- `handleSave()` : Valide et sauvegarde

## Workflow complet

### 1. Réception d'une demande

```
Client → Envoie demande
↓
Coach → Voit dans coach_requests.fxml
↓
Coach → Clique "Accepter" ou "Refuser"
↓
Statut mis à jour dans BD
```

### 2. Création de session

```
Coach → Clique "Créer session" (demande acceptée)
↓
Navigation vers add_session.fxml
↓
Formulaire pré-rempli avec infos client
↓
Coach → Remplit date, heure, durée, etc.
↓
Clique "Créer la session"
↓
Session sauvegardée dans BD
↓
Retour à coach_requests.fxml
```

### 3. Gestion des sessions

```
Coach → Accède à coach_sessions.fxml
↓
Voit toutes ses sessions
↓
Sélectionne une session
↓
Options:
  - Modifier → edit_session.fxml
  - Supprimer → Confirmation → Suppression BD
  - Marquer terminée → Statut mis à jour
```

### 4. Modification de session

```
Coach → Clique "Modifier"
↓
Navigation vers edit_session.fxml
↓
Formulaire pré-rempli
↓
Coach → Modifie les champs
↓
Clique "Enregistrer"
↓
Session mise à jour dans BD
↓
Retour à coach_sessions.fxml
```

## Intégration avec AppSession

Toutes les interfaces utilisent `AppSession.getCurrentUser()` pour récupérer l'ID du coach connecté :

```java
currentCoachId = AppSession.getCurrentUser()
        .map(User::getId)
        .orElse(1); // Valeur par défaut pour les tests
```

## Navigation

### Depuis la navbar

Pour ajouter les liens dans la navbar :

```java
// Dans NavbarController.java
@FXML
private void onMesDemandesCoach() {
    navigate("/views/coach_requests.fxml", "DayFlow — Mes demandes");
}

@FXML
private void onMesSessions() {
    navigate("/views/coach_sessions.fxml", "DayFlow — Mes sessions");
}
```

## Validation des données

### Formulaire de création de session

- Date obligatoire
- Heure obligatoire (format HH:MM)
- Durée obligatoire
- Prix optionnel (doit être un nombre valide)
- Objectif optionnel

### Formulaire de modification

- Mêmes validations que la création
- Statut doit être valide

## Gestion des erreurs

Toutes les opérations BD sont entourées de try-catch :

```java
try {
    sessionService.addSession(session);
    showSuccess("Session créée avec succès");
} catch (SQLException e) {
    showError("Erreur lors de la création", e.getMessage());
}
```

## Statuts

### Demandes de coaching
- `pending` : En attente
- `accepted` : Acceptée
- `declined` : Refusée

### Sessions
- `scheduled` : Planifiée
- `confirmed` : Confirmée
- `completed` : Terminée
- `cancelled` : Annulée

## Base de données

### Tables utilisées

1. **coaching_request**
   - Stocke les demandes de coaching
   - Lien entre user (client) et coach

2. **session**
   - Stocke les sessions planifiées
   - Référence coaching_request_id

3. **user**
   - Informations des utilisateurs (clients et coachs)

## Tests

Pour tester le workflow complet :

1. Créer un utilisateur coach dans la BD
2. Créer des demandes de coaching pour ce coach
3. Se connecter en tant que coach
4. Accéder à "Mes demandes"
5. Accepter une demande
6. Créer une session
7. Accéder à "Mes sessions"
8. Modifier/Supprimer une session

## Points importants

✅ Toutes les données viennent de la base de données (pas de données statiques)
✅ Utilisation de JDBC avec PreparedStatement
✅ ObservableList pour les TableView
✅ Navigation fluide entre les pages
✅ Validation des formulaires
✅ Confirmation avant suppression
✅ Messages de succès/erreur
✅ Compatible Scene Builder
✅ Design moderne et cohérent

## Améliorations possibles

- Ajouter des notifications en temps réel
- Système de rappel pour les sessions
- Historique des modifications
- Export des sessions en PDF
- Calendrier visuel
- Chat intégré coach-client
