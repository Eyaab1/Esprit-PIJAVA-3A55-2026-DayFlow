# Configuration Google Calendar API pour DayFlow

## Prérequis

Vous avez déjà téléchargé `client_secret.json` et l'avez placé dans `src/main/resources/`.

## Première Utilisation

### 1. Compilation du Projet

Compilez d'abord le projet pour télécharger les nouvelles dépendances Google Calendar API :

```bash
mvn clean compile
```

### 2. Premier Lancement

Lors de la première création d'un post programmé :

1. Une fenêtre de navigateur s'ouvrira automatiquement
2. Connectez-vous avec votre compte Google
3. Autorisez l'application DayFlow à accéder à votre Google Calendar
4. Le navigateur affichera "Received verification code. You may now close this window."
5. Fermez la fenêtre du navigateur

### 3. Stockage des Tokens

Les tokens d'authentification seront stockés dans le dossier `tokens/` à la racine du projet.

**Important** : Ajoutez `tokens/` à votre `.gitignore` pour ne pas committer les tokens :

```
tokens/
```

## Fonctionnement

### Création de Post Programmé

Quand un utilisateur crée un post avec le statut "Programmer" :

1. Le post est enregistré dans la base de données avec `status = SCHEDULED`
2. Un événement Google Calendar est automatiquement créé avec :
   - **Titre** : "Publication programmée – [Titre du Post]"
   - **Description** : "Votre publication DayFlow sera publiée automatiquement à la date programmée."
   - **Date/Heure** : Date et heure de publication programmée
   - **Rappel** : 30 minutes avant la publication

### Gestion des Erreurs

- Si Google Calendar échoue, le post est quand même créé (erreur non bloquante)
- Les erreurs sont loggées dans la console pour débogage
- L'utilisateur voit toujours le message de succès

## Dépannage

### Erreur "client_secret.json introuvable"

Vérifiez que le fichier est bien placé dans `src/main/resources/client_secret.json`

### Erreur d'authentification

1. Supprimez le dossier `tokens/`
2. Relancez l'application
3. Réautorisez l'accès à Google Calendar

### Port 8888 déjà utilisé

Le serveur local d'authentification utilise le port 8888. Si ce port est occupé :
- Fermez l'application qui utilise ce port
- Ou modifiez le port dans `GoogleCalendarService.java` ligne 73

## Améliorations Futures (Optionnel)

### Stocker l'Event ID dans la Base de Données

Pour pouvoir mettre à jour ou supprimer les événements Google Calendar :

1. Ajouter une colonne `google_calendar_event_id` à la table `post`
2. Stocker l'ID retourné par `createScheduledPostEvent()`
3. Utiliser `updateEvent()` ou `deleteEvent()` lors de modifications/suppressions

### Migration SQL Exemple

```sql
ALTER TABLE post ADD COLUMN google_calendar_event_id VARCHAR(255);
```

### Code Exemple

```java
// Après création de l'événement
if (eventId != null) {
    // Stocker dans la DB
    postService.updateGoogleCalendarEventId(pid, eventId);
}
```

## Sécurité

- ✅ Les tokens sont stockés localement dans `tokens/`
- ✅ Le fichier `client_secret.json` ne doit PAS être commité
- ✅ Ajoutez ces lignes à `.gitignore` :
  ```
  src/main/resources/client_secret.json
  tokens/
  ```

## Support

Pour plus d'informations sur l'API Google Calendar :
- [Documentation officielle](https://developers.google.com/calendar/api/guides/overview)
- [Guide Java Quickstart](https://developers.google.com/calendar/api/quickstart/java)
