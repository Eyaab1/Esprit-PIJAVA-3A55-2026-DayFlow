# 🎨 Guide Visuel - Affichage de la Limite

**Objectif**: Voir exactement comment afficher la limite sur l'interface  
**Format**: Diagrammes et exemples visuels  
**Date**: 5 mai 2026

---

## 📊 Vue d'Ensemble

### Où Afficher?

```
┌─────────────────────────────────────────────────────────┐
│                    INTERFACE UTILISATEUR                │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │ Réserver une Session                            │   │
│  ├─────────────────────────────────────────────────┤   │
│  │                                                 │   │
│  │ 📊 Sessions futures: 2/3 ✅                    │   │ ← COMPTEUR
│  │ ℹ️  Vous pouvez réserver 1 session             │   │ ← MESSAGE
│  │                                                 │   │
│  │ Sélectionner une date: [15 mai 2026]           │   │
│  │ Sélectionner une heure: [10:00]                │   │
│  │                                                 │   │
│  │ ┌─────────────────────────────────────────┐   │   │
│  │ │ [Réserver] (VERT - ACTIVÉ)              │   │   │ ← BOUTON
│  │ └─────────────────────────────────────────┘   │   │
│  │                                                 │   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## 🎯 État 1: 0 Sessions (Aucune réservation)

### Affichage

```
┌─────────────────────────────────────────────────────────┐
│ Réserver une Session                                    │
├─────────────────────────────────────────────────────────┤
│                                                         │
│ Sessions futures: 0/3 ✅                               │
│ Vous pouvez réserver 3 session(s)                      │
│                                                         │
│ [Sélectionner une date]                               │
│ [Sélectionner une heure]                              │
│                                                         │
│ ┌─────────────────────────────────────────────────┐   │
│ │ [Réserver]                                      │   │
│ │ Couleur: VERT                                   │   │
│ │ État: ACTIVÉ (cliquable)                        │   │
│ └─────────────────────────────────────────────────┘   │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### Couleurs

```
Compteur: 0/3
├─ Couleur: VERT (#4CAF50)
├─ Icône: ✅
└─ Signification: OK, vous pouvez réserver

Bouton:
├─ Couleur: VERT (#4CAF50)
├─ État: ACTIVÉ
└─ Texte: Blanc
```

### Code FXML

```xml
<Label text="Sessions futures: 0/3 ✅" 
       style="-fx-text-fill: green; -fx-font-weight: bold;" />

<Label text="Vous pouvez réserver 3 session(s)" 
       style="-fx-text-fill: blue;" />

<Button text="Réserver" 
        style="-fx-text-fill: white; -fx-background-color: green;"
        disable="false" />
```

---

## 🎯 État 2: 1 Session (Après 1ère réservation)

### Affichage

```
┌─────────────────────────────────────────────────────────┐
│ Réserver une Session                                    │
├─────────────────────────────────────────────────────────┤
│                                                         │
│ Sessions futures: 1/3 ✅                               │
│ Vous pouvez réserver 2 session(s)                      │
│                                                         │
│ [Sélectionner une date]                               │
│ [Sélectionner une heure]                              │
│                                                         │
│ ┌─────────────────────────────────────────────────┐   │
│ │ [Réserver]                                      │   │
│ │ Couleur: VERT                                   │   │
│ │ État: ACTIVÉ (cliquable)                        │   │
│ └─────────────────────────────────────────────────┘   │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### Changements

```
Avant: 0/3 → Après: 1/3
├─ Compteur: 0 → 1
├─ Message: "3 session(s)" → "2 session(s)"
├─ Couleur: VERT (inchangée)
└─ Bouton: VERT (inchangé)
```

---

## 🎯 État 3: 2 Sessions (Après 2ème réservation)

### Affichage

```
┌─────────────────────────────────────────────────────────┐
│ Réserver une Session                                    │
├─────────────────────────────────────────────────────────┤
│                                                         │
│ Sessions futures: 2/3 ⚠️                               │
│ Vous pouvez réserver 1 session                         │
│                                                         │
│ [Sélectionner une date]                               │
│ [Sélectionner une heure]                              │
│                                                         │
│ ┌─────────────────────────────────────────────────┐   │
│ │ [Réserver]                                      │   │
│ │ Couleur: VERT                                   │   │
│ │ État: ACTIVÉ (cliquable)                        │   │
│ └─────────────────────────────────────────────────┘   │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### Changements

```
Avant: 1/3 → Après: 2/3
├─ Compteur: 1 → 2
├─ Icône: ✅ → ⚠️
├─ Couleur: VERT → ORANGE
├─ Message: "2 session(s)" → "1 session"
└─ Bouton: VERT (inchangé)
```

### Couleurs

```
Compteur: 2/3
├─ Couleur: ORANGE (#FF9800)
├─ Icône: ⚠️
└─ Signification: Attention, presque à la limite

Bouton:
├─ Couleur: VERT (#4CAF50)
├─ État: ACTIVÉ
└─ Texte: Blanc
```

---

## 🎯 État 4: 3 Sessions (Limite atteinte)

### Affichage

```
┌─────────────────────────────────────────────────────────┐
│ Réserver une Session                                    │
├─────────────────────────────────────────────────────────┤
│                                                         │
│ Sessions futures: 3/3 ❌                               │
│ Limite atteinte - Annulez une session pour continuer   │
│                                                         │
│ [Sélectionner une date]                               │
│ [Sélectionner une heure]                              │
│                                                         │
│ ┌─────────────────────────────────────────────────┐   │
│ │ [Réserver]                                      │   │
│ │ Couleur: GRIS                                   │   │
│ │ État: DÉSACTIVÉ (non cliquable)                 │   │
│ │ Tooltip: "Vous avez atteint la limite"          │   │
│ └─────────────────────────────────────────────────┘   │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### Changements

```
Avant: 2/3 → Après: 3/3
├─ Compteur: 2 → 3
├─ Icône: ⚠️ → ❌
├─ Couleur: ORANGE → ROUGE
├─ Message: "1 session" → "Limite atteinte"
├─ Bouton: VERT → GRIS
└─ Bouton: ACTIVÉ → DÉSACTIVÉ
```

### Couleurs

```
Compteur: 3/3
├─ Couleur: ROUGE (#F44336)
├─ Icône: ❌
└─ Signification: Limite atteinte

Bouton:
├─ Couleur: GRIS (#CCCCCC)
├─ État: DÉSACTIVÉ
├─ Texte: Gris foncé
└─ Tooltip: "Vous avez atteint la limite de 3 sessions"
```

### Code FXML

```xml
<Label text="Sessions futures: 3/3 ❌" 
       style="-fx-text-fill: red; -fx-font-weight: bold;" />

<Label text="Limite atteinte - Annulez une session pour continuer" 
       style="-fx-text-fill: red;" />

<Button text="Réserver" 
        style="-fx-text-fill: gray; -fx-background-color: lightgray;"
        disable="true" />
```

---

## 🎯 État 5: Après Annulation (Retour à 2 sessions)

### Affichage

```
┌─────────────────────────────────────────────────────────┐
│ Réserver une Session                                    │
├─────────────────────────────────────────────────────────┤
│                                                         │
│ Sessions futures: 2/3 ⚠️                               │
│ Vous pouvez réserver 1 session                         │
│                                                         │
│ [Sélectionner une date]                               │
│ [Sélectionner une heure]                              │
│                                                         │
│ ┌─────────────────────────────────────────────────┐   │
│ │ [Réserver]                                      │   │
│ │ Couleur: VERT                                   │   │
│ │ État: ACTIVÉ (cliquable)                        │   │
│ └─────────────────────────────────────────────────┘   │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### Changements

```
Avant: 3/3 ❌ → Après: 2/3 ⚠️
├─ Compteur: 3 → 2
├─ Icône: ❌ → ⚠️
├─ Couleur: ROUGE → ORANGE
├─ Message: "Limite atteinte" → "Vous pouvez réserver 1 session"
├─ Bouton: GRIS → VERT
└─ Bouton: DÉSACTIVÉ → ACTIVÉ
```

---

## 📊 Tableau de Progression

```
┌─────────────┬──────────┬──────────┬──────────┬──────────┬──────────┐
│ État        │ 0/3      │ 1/3      │ 2/3      │ 3/3      │ 2/3*     │
├─────────────┼──────────┼──────────┼──────────┼──────────┼──────────┤
│ Compteur    │ 0/3 ✅   │ 1/3 ✅   │ 2/3 ⚠️   │ 3/3 ❌   │ 2/3 ⚠️   │
├─────────────┼──────────┼──────────┼──────────┼──────────┼──────────┤
│ Couleur     │ VERT     │ VERT     │ ORANGE   │ ROUGE    │ ORANGE   │
├─────────────┼──────────┼──────────┼──────────┼──────────┼──────────┤
│ Message     │ 3 slots  │ 2 slots  │ 1 slot   │ Limite   │ 1 slot   │
├─────────────┼──────────┼──────────┼──────────┼──────────┼──────────┤
│ Bouton      │ VERT     │ VERT     │ VERT     │ GRIS     │ VERT     │
├─────────────┼──────────┼──────────┼──────────┼──────────┼──────────┤
│ État Bouton │ ACTIVÉ   │ ACTIVÉ   │ ACTIVÉ   │ DÉSACTIVÉ│ ACTIVÉ   │
├─────────────┼──────────┼──────────┼──────────┼──────────┼──────────┤
│ Action      │ Réserver │ Réserver │ Réserver │ Annuler  │ Réserver │
└─────────────┴──────────┴──────────┴──────────┴──────────┴──────────┘

* Après annulation d'une session
```

---

## 🎨 Palette de Couleurs

### Vert (OK)
```
Hex: #4CAF50
RGB: 76, 175, 80
Utilisation: 0-2 sessions
Signification: Vous pouvez réserver
```

### Orange (Attention)
```
Hex: #FF9800
RGB: 255, 152, 0
Utilisation: 2-3 sessions
Signification: Presque à la limite
```

### Rouge (Erreur)
```
Hex: #F44336
RGB: 244, 67, 54
Utilisation: 3 sessions
Signification: Limite atteinte
```

### Gris (Désactivé)
```
Hex: #CCCCCC
RGB: 204, 204, 204
Utilisation: Bouton désactivé
Signification: Action non disponible
```

---

## 🎯 Boîte de Dialogue d'Erreur

### Quand Afficher?

Quand l'utilisateur clique sur "Réserver" avec 3 sessions

### Affichage

```
┌─────────────────────────────────────────────────────────┐
│ ⚠️ ERREUR                                               │
├─────────────────────────────────────────────────────────┤
│                                                         │
│ Impossible de réserver                                  │
│                                                         │
│ Vous avez atteint la limite de 3 sessions futures.     │
│ Veuillez terminer ou annuler une session avant de      │
│ réserver à nouveau.                                     │
│                                                         │
│                                    ┌──────────────┐    │
│                                    │ [OK]         │    │
│                                    └──────────────┘    │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### Code JavaFX

```java
Alert alert = new Alert(Alert.AlertType.ERROR);
alert.setTitle("Erreur");
alert.setHeaderText("Impossible de réserver");
alert.setContentText(
    "Vous avez atteint la limite de 3 sessions futures.\n" +
    "Veuillez terminer ou annuler une session avant de réserver à nouveau."
);
alert.showAndWait();
```

---

## ✅ Boîte de Dialogue de Succès

### Quand Afficher?

Après une réservation réussie

### Affichage

```
┌─────────────────────────────────────────────────────────┐
│ ✅ SUCCÈS                                               │
├─────────────────────────────────────────────────────────┤
│                                                         │
│ Opération réussie                                       │
│                                                         │
│ Session réservée avec succès!                           │
│ Sessions futures: 1/3                                   │
│                                                         │
│                                    ┌──────────────┐    │
│                                    │ [OK]         │    │
│                                    └──────────────┘    │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### Code JavaFX

```java
Alert alert = new Alert(Alert.AlertType.INFORMATION);
alert.setTitle("Succès");
alert.setHeaderText("Opération réussie");
alert.setContentText("Session réservée avec succès!\nSessions futures: 1/3");
alert.showAndWait();
```

---

## 📱 Responsive Design

### Sur Desktop (Large)

```
┌─────────────────────────────────────────────────────────┐
│ Sessions futures: 2/3 ⚠️ | Vous pouvez réserver 1      │
│ [Réserver]                                              │
└─────────────────────────────────────────────────────────┘
```

### Sur Tablette (Medium)

```
┌──────────────────────────────────┐
│ Sessions futures: 2/3 ⚠️         │
│ Vous pouvez réserver 1 session   │
│ [Réserver]                       │
└──────────────────────────────────┘
```

### Sur Mobile (Small)

```
┌──────────────────┐
│ Sessions: 2/3 ⚠️ │
│ [Réserver]       │
└──────────────────┘
```

---

## 🔔 Notifications Toast

### Après Réservation Réussie

```
┌──────────────────────────────────┐
│ ✅ Session réservée!             │
│ Sessions futures: 1/3            │
└──────────────────────────────────┘
(Disparaît après 3 secondes)
```

### Après Annulation

```
┌──────────────────────────────────┐
│ ✅ Session annulée               │
│ Sessions futures: 2/3            │
└──────────────────────────────────┘
(Disparaît après 3 secondes)
```

### Erreur - Limite Atteinte

```
┌──────────────────────────────────┐
│ ❌ Limite atteinte               │
│ Annulez une session pour continuer│
└──────────────────────────────────┘
(Disparaît après 5 secondes)
```

---

## 🎯 Checklist Visuelle

### Compteur
- [ ] Affiche "X/3"
- [ ] Couleur change (vert → orange → rouge)
- [ ] Icône change (✅ → ⚠️ → ❌)
- [ ] Se met à jour en temps réel

### Message
- [ ] Affiche le nombre de slots restants
- [ ] Change quand limite atteinte
- [ ] Texte clair et compréhensible
- [ ] Couleur appropriée

### Bouton
- [ ] VERT quand slots disponibles
- [ ] GRIS quand limite atteinte
- [ ] ACTIVÉ quand slots disponibles
- [ ] DÉSACTIVÉ quand limite atteinte
- [ ] Tooltip informatif

### Erreur
- [ ] Affiche quand limite atteinte
- [ ] Message clair
- [ ] Bouton OK pour fermer
- [ ] Disparaît après clic

---

## 📊 Résumé Visuel

```
PROGRESSION:

0/3 ✅ (VERT)
  ↓ [Réserver]
1/3 ✅ (VERT)
  ↓ [Réserver]
2/3 ⚠️ (ORANGE)
  ↓ [Réserver]
3/3 ❌ (ROUGE) [Réserver GRIS - DÉSACTIVÉ]
  ↓ [Annuler]
2/3 ⚠️ (ORANGE)
  ↓ [Réserver]
3/3 ❌ (ROUGE) [Réserver GRIS - DÉSACTIVÉ]
```

---

**Status**: ✅ GUIDE VISUEL  
**Date**: 5 mai 2026  
**Version**: 1.0

**Prochaine étape**: Implémenter selon ce guide visuel

