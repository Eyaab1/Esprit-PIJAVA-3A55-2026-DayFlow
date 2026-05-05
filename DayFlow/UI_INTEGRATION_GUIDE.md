# 🎨 Guide d'Intégration UI - Limite de Réservation

**Objectif**: Afficher la limite de 3 sessions sur l'interface utilisateur  
**Statut**: Guide d'intégration (pas de modification de code)  
**Date**: 5 mai 2026

---

## 📋 Résumé

Tu dois afficher sur l'interface:
1. **Compteur**: "Sessions futures: X/3"
2. **Bouton**: ACTIVÉ (vert) ou DÉSACTIVÉ (gris)
3. **Message d'erreur**: "Vous avez atteint la limite"

---

## 🎯 Où Afficher Quoi?

### 1. Compteur de Sessions (Texte)

**Où**: Sur la page de réservation de session

**Afficher**:
```
Sessions futures: 0/3
Sessions futures: 1/3
Sessions futures: 2/3
Sessions futures: 3/3
```

**Couleur**:
- 0-2 sessions: VERT (OK)
- 3 sessions: ROUGE (Limite atteinte)

**Exemple visuel**:
```
┌─────────────────────────────────┐
│ Réserver une Session            │
├─────────────────────────────────┤
│                                 │
│ Sessions futures: 2/3 ✅        │
│ Vous pouvez réserver 1 session  │
│                                 │
│ [Sélectionner une date]         │
│ [Réserver] (ACTIVÉ - vert)      │
│                                 │
└─────────────────────────────────┘
```

---

### 2. Bouton de Réservation

**État 1: Limite NON atteinte (0-2 sessions)**
```
[Réserver] 
Couleur: VERT
État: ACTIVÉ (cliquable)
```

**État 2: Limite atteinte (3 sessions)**
```
[Réserver]
Couleur: GRIS
État: DÉSACTIVÉ (non cliquable)
Tooltip: "Vous avez atteint la limite de 3 sessions"
```

**Exemple visuel**:
```
Avant (0-2 sessions):
┌──────────────────┐
│ [Réserver] ✅    │  ← Vert, cliquable
└──────────────────┘

Après (3 sessions):
┌──────────────────┐
│ [Réserver] ❌    │  ← Gris, non cliquable
└──────────────────┘
```

---

### 3. Message d'Erreur

**Quand**: L'utilisateur clique sur "Réserver" avec 3 sessions

**Afficher**:
```
⚠️ Vous avez atteint la limite de 3 sessions futures.
Veuillez terminer ou annuler une session avant de réserver à nouveau.
```

**Exemple visuel**:
```
┌─────────────────────────────────────────────────────┐
│ ⚠️ ERREUR                                           │
├─────────────────────────────────────────────────────┤
│                                                     │
│ Vous avez atteint la limite de 3 sessions futures. │
│ Veuillez terminer ou annuler une session avant de  │
│ réserver à nouveau.                                │
│                                                     │
│ [OK]                                               │
│                                                     │
└─────────────────────────────────────────────────────┘
```

---

## 🔧 Comment Implémenter

### Étape 1: Récupérer le Nombre de Sessions

**Dans le contrôleur** (exemple):
```java
// Récupérer l'ID de l'utilisateur
int userId = getCurrentUserId();

// Compter les sessions futures
int futureSessionsCount = sessionService.countFutureSessions(userId);

// Vérifier si on peut réserver
boolean canBook = sessionService.canBookSession(userId);

// Obtenir les slots restants
int remainingSlots = sessionService.getRemainingSlots(userId);
```

### Étape 2: Passer les Données à la Vue

**Dans le contrôleur**:
```java
// Passer les données à la vue FXML
model.put("futureSessionsCount", futureSessionsCount);
model.put("canBook", canBook);
model.put("remainingSlots", remainingSlots);
model.put("maxSessions", 3);
```

### Étape 3: Afficher dans la Vue FXML

**Dans le fichier FXML**:
```xml
<!-- Compteur -->
<Label text="Sessions futures: ${futureSessionsCount}/3" 
       textFill="${futureSessionsCount >= 3 ? 'RED' : 'GREEN'}" />

<!-- Message informatif -->
<Label text="Vous pouvez réserver ${remainingSlots} session(s)" 
       visible="${remainingSlots > 0}" />

<!-- Bouton -->
<Button text="Réserver" 
        disable="${!canBook}"
        style="${canBook ? '-fx-text-fill: green;' : '-fx-text-fill: gray;'}" />
```

---

## 📍 Où Ajouter l'Affichage

### Option 1: Page de Réservation de Session

**Fichier FXML**: `calendar_coach.fxml` ou équivalent

**Ajouter**:
```xml
<!-- Avant le bouton de réservation -->
<VBox spacing="10">
    <!-- Compteur -->
    <HBox spacing="5">
        <Label text="Sessions futures:" fontWeight="bold" />
        <Label text="${futureSessionsCount}/3" 
               textFill="${futureSessionsCount >= 3 ? 'RED' : 'GREEN'}" />
    </HBox>
    
    <!-- Message -->
    <Label text="Vous pouvez réserver ${remainingSlots} session(s)" 
           visible="${remainingSlots > 0}"
           textFill="BLUE" />
    
    <!-- Bouton -->
    <Button text="Réserver" 
            disable="${!canBook}"
            onAction="#handleReservation" />
</VBox>
```

### Option 2: Tableau de Sessions

**Afficher dans chaque ligne**:
```
Session 1: 15 mai 2026, 10:00 - 11:00 [Confirmée]
Session 2: 20 mai 2026, 14:00 - 15:00 [Confirmée]
Session 3: 25 mai 2026, 16:00 - 17:00 [Confirmée]

Sessions futures: 3/3 ⚠️ LIMITE ATTEINTE
```

### Option 3: Barre d'Information

**En haut de la page**:
```
┌─────────────────────────────────────────────────────┐
│ ℹ️ Sessions futures: 2/3 | Slots restants: 1       │
└─────────────────────────────────────────────────────┘
```

---

## 🎨 Styles Recommandés

### Couleurs

**Vert (OK)**:
```
- Hex: #4CAF50
- RGB: 76, 175, 80
- Utilisation: 0-2 sessions
```

**Orange (Attention)**:
```
- Hex: #FF9800
- RGB: 255, 152, 0
- Utilisation: 2-3 sessions
```

**Rouge (Limite atteinte)**:
```
- Hex: #F44336
- RGB: 244, 67, 54
- Utilisation: 3 sessions
```

### Icônes

**OK**: ✅ ou 🟢  
**Attention**: ⚠️ ou 🟠  
**Erreur**: ❌ ou 🔴  

---

## 📊 Progression Visuelle

### Étape 1: 0 sessions
```
┌──────────────────────────────────┐
│ Sessions futures: 0/3 ✅         │
│ Vous pouvez réserver 3 sessions  │
│ [Réserver] (VERT)                │
└──────────────────────────────────┘
```

### Étape 2: 1 session
```
┌──────────────────────────────────┐
│ Sessions futures: 1/3 ✅         │
│ Vous pouvez réserver 2 sessions  │
│ [Réserver] (VERT)                │
└──────────────────────────────────┘
```

### Étape 3: 2 sessions
```
┌──────────────────────────────────┐
│ Sessions futures: 2/3 ⚠️         │
│ Vous pouvez réserver 1 session   │
│ [Réserver] (VERT)                │
└──────────────────────────────────┘
```

### Étape 4: 3 sessions
```
┌──────────────────────────────────┐
│ Sessions futures: 3/3 ❌         │
│ Limite atteinte                  │
│ [Réserver] (GRIS - DÉSACTIVÉ)    │
└──────────────────────────────────┘
```

### Étape 5: Après annulation
```
┌──────────────────────────────────┐
│ Sessions futures: 2/3 ✅         │
│ Vous pouvez réserver 1 session   │
│ [Réserver] (VERT)                │
└──────────────────────────────────┘
```

---

## 🔄 Flux d'Interaction

### Scénario 1: Réservation Réussie

```
1. Utilisateur voit: "Sessions futures: 0/3"
2. Utilisateur clique: [Réserver]
3. Système crée la session
4. Interface se met à jour: "Sessions futures: 1/3"
5. Bouton reste: VERT (ACTIVÉ)
```

### Scénario 2: Limite Atteinte

```
1. Utilisateur voit: "Sessions futures: 3/3"
2. Utilisateur clique: [Réserver] (GRIS)
3. Rien ne se passe (bouton désactivé)
4. Ou affiche: "⚠️ Limite atteinte"
```

### Scénario 3: Annulation et Nouvelle Réservation

```
1. Utilisateur voit: "Sessions futures: 3/3"
2. Utilisateur annule une session
3. Interface se met à jour: "Sessions futures: 2/3"
4. Bouton devient: VERT (ACTIVÉ)
5. Utilisateur peut réserver à nouveau
```

---

## 📱 Responsive Design

### Sur Desktop
```
┌─────────────────────────────────────────┐
│ Sessions futures: 2/3 | [Réserver]     │
└─────────────────────────────────────────┘
```

### Sur Mobile
```
┌──────────────────┐
│ Sessions: 2/3    │
│ [Réserver]       │
└──────────────────┘
```

---

## 🔔 Notifications

### Toast (Notification Temporaire)

**Après réservation réussie**:
```
✅ Session réservée avec succès!
Sessions futures: 1/3
```

**Après annulation**:
```
✅ Session annulée
Sessions futures: 2/3
```

**Erreur - Limite atteinte**:
```
❌ Vous avez atteint la limite de 3 sessions
Veuillez annuler une session avant de réserver
```

---

## 📝 Textes à Afficher

### Compteur
```
"Sessions futures: X/3"
```

### Messages Informatifs
```
"Vous pouvez réserver Y session(s)"
"Limite atteinte"
"Vous avez atteint la limite de 3 sessions"
```

### Messages d'Erreur
```
"Vous avez atteint la limite de 3 sessions futures.
Veuillez terminer ou annuler une session avant de réserver à nouveau."
```

### Tooltips (Au survol du bouton)
```
Bouton ACTIVÉ: "Cliquez pour réserver une session"
Bouton DÉSACTIVÉ: "Vous avez atteint la limite de 3 sessions"
```

---

## 🎯 Checklist d'Implémentation

### Affichage
- [ ] Compteur "Sessions futures: X/3" visible
- [ ] Couleur change selon le nombre (vert/orange/rouge)
- [ ] Message informatif affiche les slots restants
- [ ] Bouton change d'état (activé/désactivé)

### Interaction
- [ ] Bouton ACTIVÉ quand sessions < 3
- [ ] Bouton DÉSACTIVÉ quand sessions = 3
- [ ] Message d'erreur affiche quand limite atteinte
- [ ] Interface se met à jour après réservation

### Mise à Jour
- [ ] Compteur se met à jour après réservation
- [ ] Compteur se met à jour après annulation
- [ ] Compteur se met à jour après fin de session
- [ ] Bouton change d'état automatiquement

### Textes
- [ ] Tous les textes en français
- [ ] Messages clairs et compréhensibles
- [ ] Pas de jargon technique
- [ ] Icônes appropriées

---

## 💡 Conseils d'UX

### 1. Clarté
```
✅ "Sessions futures: 2/3"
❌ "Vous avez 2 sessions"
```

### 2. Feedback Immédiat
```
✅ Compteur se met à jour immédiatement
❌ Compteur se met à jour après rechargement
```

### 3. Prévention d'Erreur
```
✅ Bouton désactivé quand limite atteinte
❌ Afficher erreur après clic
```

### 4. Aide Contextuelle
```
✅ Tooltip: "Vous avez atteint la limite"
❌ Rien du tout
```

---

## 🔗 Intégration avec le Code Existant

### Appels de Service

```java
// Dans le contrôleur
SessionService sessionService = new SessionService();

// Récupérer les données
int count = sessionService.countFutureSessions(userId);
boolean canBook = sessionService.canBookSession(userId);
int remaining = sessionService.getRemainingSlots(userId);

// Passer à la vue
model.put("futureSessionsCount", count);
model.put("canBook", canBook);
model.put("remainingSlots", remaining);
```

### Gestion d'Erreur

```java
try {
    sessionService.reserveSession(session, userId);
    // Succès - mettre à jour l'interface
    updateUI();
} catch (ReservationLimitExceededException e) {
    // Afficher le message d'erreur
    showErrorDialog(e.getUserFriendlyMessage());
}
```

---

## 📚 Fichiers à Consulter

### Pour Comprendre le Code
- `SessionService.java` - Méthodes disponibles
- `SessionReservationValidator.java` - Logique de validation
- `ReservationLimitExceededException.java` - Messages d'erreur

### Pour Voir des Exemples
- `SessionReservationController.java` - Exemple d'intégration
- `TESTING_GUIDE_SESSION_RESERVATION_LIMIT.md` - Scénarios de test

---

## ✅ Résumé

### À Afficher
1. **Compteur**: "Sessions futures: X/3"
2. **Bouton**: ACTIVÉ (vert) ou DÉSACTIVÉ (gris)
3. **Message**: "Vous pouvez réserver Y session(s)"
4. **Erreur**: "Vous avez atteint la limite"

### Où Afficher
- Page de réservation de session
- Tableau de sessions
- Barre d'information

### Comment Afficher
- Utiliser les données du service
- Mettre à jour en temps réel
- Afficher des messages clairs
- Utiliser des couleurs appropriées

---

**Status**: ✅ GUIDE D'INTÉGRATION UI  
**Date**: 5 mai 2026  
**Version**: 1.0

**Prochaine étape**: Implémenter l'affichage dans votre contrôleur FXML

