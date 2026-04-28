# Règles de Validation - DayFlow

## 📋 Objectifs (Goals)

### Champs Obligatoires
- ✅ **Titre** : 3-255 caractères
- ✅ **Date de début** : Obligatoire
- ✅ **Date de fin** : Obligatoire, doit être après la date de début

### Champs Optionnels
- **Description** : Maximum 1000 caractères
- **Priorité** : low, medium, high
- **Statut** : draft, active, paused, completed, failed, archived

### Validation en Temps Réel
- ✅ Bordure rouge si invalide
- ✅ Bordure verte si valide
- ✅ Tooltip avec message d'erreur
- ✅ Bouton "Enregistrer" désactivé si formulaire invalide

---

## 🔁 Routines

### Champs Obligatoires
- ✅ **Titre** : 3-255 caractères

### Champs Optionnels
- **Description** : Maximum 1000 caractères
- **Visibilité** : public, private
- **Priorité** : low, medium, high
- **Statut** : draft, active, paused, completed, skipped
- **Deadline** : Date optionnelle

### Validation en Temps Réel
- ✅ Bordure rouge si invalide
- ✅ Bordure verte si valide
- ✅ Tooltip avec message d'erreur
- ✅ Bouton "Enregistrer" désactivé si formulaire invalide

---

## ⏱️ Activités

### Champs Obligatoires
- ✅ **Titre** : 3-255 caractères
- ✅ **Heure de début** : Format HH:mm (ex: 09:30)
- ✅ **Durée** : Nombre positif en minutes

### Champs Optionnels
- **Priorité** : low, medium, high
- **Statut** : pending, in_progress, completed, skipped, cancelled
- **Deadline** : Date optionnelle
- **Rappel** : Checkbox pour activer

### Validation en Temps Réel
- ✅ Bordure rouge si invalide
- ✅ Bordure verte si valide
- ✅ Tooltip avec message d'erreur
- ✅ Bouton "Enregistrer" désactivé si formulaire invalide
- ✅ Validation du format d'heure (HH:mm)
- ✅ Validation que la durée est un nombre positif

---

## 🎨 Indicateurs Visuels

### États de Validation
1. **Invalide** : Bordure rouge (#dc2626) + Tooltip d'erreur
2. **Valide** : Bordure verte (#059669)
3. **Par défaut** : Bordure grise (#e2e8f0)
4. **Focus** : Bordure bleue (#6366f1)

### Bouton Enregistrer
- **Actif** : Opacité 100%, cliquable
- **Désactivé** : Opacité 50%, non cliquable

---

## 🔧 Classe Utilitaire

`utils.FormValidator` fournit :
- `validateTextField()` - Validation de TextField
- `validateTextArea()` - Validation de TextArea
- `validateDatePicker()` - Validation de DatePicker
- `validateChoiceBox()` - Validation de ChoiceBox
- `Validators.*` - Prédicats de validation réutilisables

### Validateurs Disponibles
- `notEmpty()` - Vérifie que le champ n'est pas vide
- `minLength(int)` - Longueur minimale
- `maxLength(int)` - Longueur maximale
- `lengthBetween(int, int)` - Longueur entre min et max
- `notNull()` - Vérifie que la valeur n'est pas null
- `afterDate(LocalDate)` - Date après une référence
- `beforeDate(LocalDate)` - Date avant une référence
- `isNumeric()` - Vérifie que c'est un nombre
- `isPositiveNumber()` - Nombre positif
- `isTimeFormat()` - Format HH:mm

---

## ✨ Fonctionnalités

1. **Validation en temps réel** - Feedback immédiat pendant la saisie
2. **Messages d'erreur contextuels** - Tooltips explicatifs
3. **Désactivation intelligente** - Bouton désactivé si formulaire invalide
4. **Indicateurs visuels** - Couleurs pour guider l'utilisateur
5. **Validation côté modèle** - Double validation (UI + modèle)

---

## 📝 Exemples d'Utilisation

```java
// Validation d'un TextField
titleField.textProperty().addListener((obs, old, newVal) -> {
    FormValidator.validateTextField(titleField,
        FormValidator.Validators.lengthBetween(3, 255),
        "Le titre doit contenir entre 3 et 255 caractères");
});

// Validation d'un DatePicker
startDatePicker.valueProperty().addListener((obs, old, newVal) -> {
    FormValidator.validateDatePicker(startDatePicker,
        FormValidator.Validators.notNull(),
        "La date de début est obligatoire");
});
```
