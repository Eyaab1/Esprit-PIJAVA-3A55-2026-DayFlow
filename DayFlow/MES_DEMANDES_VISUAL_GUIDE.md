# 🎨 Guide Visuel - Interface "Mes Demandes" Améliorée

## 🌈 Palette de Couleurs Pastel

### Fond Principal
```
┌─────────────────────────────────────┐
│  Dégradé Vertical                   │
│  ↓                                  │
│  #f0f9ff (Bleu ciel clair)         │
│  ↓                                  │
│  #fef3f9 (Rose poudré)             │
└─────────────────────────────────────┘
```

### Cartes Statistiques

```
┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   📊 Total   │  │ ⏳ En attente│  │ ✅ Acceptées │  │  ❌ Refusées │
│              │  │              │  │              │  │              │
│  Bleu ciel   │  │   Orange     │  │ Vert menthe  │  │ Rose poudré  │
│  #dbeafe     │  │   #fed7aa    │  │   #bbf7d0    │  │   #fecaca    │
│      ↓       │  │      ↓       │  │      ↓       │  │      ↓       │
│  #e0f2fe     │  │   #fef3c7    │  │   #d1fae5    │  │   #fee2e2    │
│              │  │              │  │              │  │              │
│      2       │  │      1       │  │      1       │  │      0       │
└──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘
```

### Boutons d'Action

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│  ✨ Nouvelle demande    💳 Payer    ✏️ Modifier                │
│  Vert émeraude         Cyan        Indigo                      │
│  #10b981 → #059669     #06b6d4     #6366f1 → #8b5cf6          │
│                                                                 │
│  🗑️ Supprimer          🔄 Actualiser                           │
│  Rouge corail          Bleu                                    │
│  #ef4444 → #dc2626     #3b82f6 → #2563eb                      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📐 Structure de l'Interface

```
┌─────────────────────────────────────────────────────────────────┐
│  ← Retour    [Gérer sessions]                                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│              Mes demandes de coaching                           │
│         Gérez vos demandes de coaching en cours                 │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│  ┌───────────────────────────────────────────────────────────┐ │
│  │                                                           │ │
│  │  [Total: 2]  [En attente: 1]  [Acceptées: 1]  [Refusées: 0] │
│  │                                                           │ │
│  ├───────────────────────────────────────────────────────────┤ │
│  │                                                           │ │
│  │  🔍 Rechercher...  [Statut▼] [Priorité▼] [Du] [Au] [Filtrer] │
│  │                                                           │ │
│  ├───────────────────────────────────────────────────────────┤ │
│  │                                                           │ │
│  │  ┌─────────────────────────────────────────────────────┐ │ │
│  │  │ ID │ Coach │ Message │ Priorité │ Statut │ Date    │ │ │
│  │  ├────┼───────┼─────────┼──────────┼────────┼─────────┤ │ │
│  │  │ 29 │ Marie │ Je me...│ Normal   │ En att.│ 24/04   │ │ │
│  │  │ 24 │ marye │ perdre..│ Normal   │ Accept.│ 15/04   │ │ │
│  │  └─────────────────────────────────────────────────────┘ │ │
│  │                                                           │ │
│  ├───────────────────────────────────────────────────────────┤ │
│  │                                                           │ │
│  │  [✨ Nouvelle] [💳 Payer] [✏️ Modifier] [🗑️ Supprimer] [🔄] │
│  │                                                           │ │
│  ├───────────────────────────────────────────────────────────┤ │
│  │                                                           │ │
│  │  Sélection : Aucune demande sélectionnée                 │ │
│  │                                                           │ │
│  └───────────────────────────────────────────────────────────┘ │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🎯 Zones Interactives

### Effets de Survol

```
Boutons :
┌──────────────┐         ┌──────────────┐
│   Normal     │  Hover  │   Hover      │
│              │  ────►  │   ↑ Scale    │
│  Ombre légère│         │  Ombre forte │
└──────────────┘         └──────────────┘

Lignes du Tableau :
┌──────────────────────────────────┐
│  Ligne normale (blanc/gris clair)│
│  ────────────────────────────────│
│  Ligne hover (bleu ciel)         │  ◄── Survol
│  ────────────────────────────────│
│  Ligne sélectionnée (bleu-lavande)│  ◄── Sélection
└──────────────────────────────────┘

Champs de Saisie :
┌──────────────────────┐         ┌──────────────────────┐
│  Normal              │  Focus  │  Focus               │
│  Bordure gris clair  │  ────►  │  Bordure indigo      │
│                      │         │  + Ombre indigo      │
└──────────────────────┘         └──────────────────────┘
```

---

## 📊 Hiérarchie Visuelle

### Tailles de Police

```
Titre Principal       : 36px  ████████████████
Sous-titre           : 16px  ████████
Valeurs Statistiques : 28px  ██████████████
Labels               : 14px  ███████
Texte Tableau        : 13px  ██████
```

### Poids de Police

```
Gras (Bold)      : Titres, valeurs, boutons
Semi-gras (600)  : Labels, en-têtes
Moyen (500)      : Textes secondaires
Normal (400)     : Texte standard
```

---

## 🌟 Effets Visuels

### Ombres

```
Carte Principale :
  dropshadow(gaussian, rgba(99, 102, 241, 0.08), 16, 0, 0, 4)
  └─> Ombre douce avec teinte indigo

Boutons :
  dropshadow(gaussian, rgba(couleur, 0.3), 8, 0, 0, 2)
  └─> Ombre colorée selon le bouton

Statistiques :
  dropshadow(gaussian, rgba(0, 0, 0, 0.05), 8, 0, 0, 2)
  └─> Ombre très subtile
```

### Dégradés

```
Fond Principal :
  linear-gradient(to bottom, #f0f9ff 0%, #fef3f9 100%)
  └─> Vertical, bleu vers rose

Boutons :
  linear-gradient(135deg, couleur1 0%, couleur2 100%)
  └─> Diagonal, effet de profondeur

Statistiques :
  linear-gradient(to bottom, couleur1 0%, couleur2 100%)
  └─> Vertical, subtil
```

### Coins Arrondis

```
Carte Principale    : 20px  ╭────────╮
Statistiques        : 12px  ╭──────╮
Boutons             : 10px  ╭────╮
Champs de Saisie    : 10px  ╭────╮
Scrollbar           :  8px  ╭──╮
```

---

## 🎨 États des Éléments

### Boutons

```
État Normal :
  ┌─────────────────┐
  │  ✨ Nouvelle    │  Dégradé + Ombre légère
  └─────────────────┘

État Hover :
  ┌─────────────────┐
  │  ✨ Nouvelle    │  Dégradé foncé + Ombre forte + Scale 1.02
  └─────────────────┘

État Disabled :
  ┌─────────────────┐
  │  💳 Payer       │  Gris + Pas d'ombre + Curseur normal
  └─────────────────┘
```

### Lignes du Tableau

```
Normale (paire) :
  ┌────────────────────────────────┐
  │  Fond blanc                    │
  └────────────────────────────────┘

Normale (impaire) :
  ┌────────────────────────────────┐
  │  Fond gris très clair          │
  └────────────────────────────────┘

Hover :
  ┌────────────────────────────────┐
  │  Fond bleu ciel très clair     │
  └────────────────────────────────┘

Sélectionnée :
  ┌────────────────────────────────┐
  │  Dégradé bleu-lavande          │
  └────────────────────────────────┘
```

---

## 📏 Espacements

### Système d'Espacement (multiples de 4)

```
Padding Principal    : 40-44px
Entre Sections       : 28px
Dans la Carte        : 24-32px
Entre Éléments       : 16px
Entre Petits Éléments: 12px
Dans les Cartes Stat : 8px
```

### Marges Visuelles

```
┌─────────────────────────────────────┐
│  44px                               │  ← Padding gauche/droite
│  ┌───────────────────────────────┐ │
│  │                               │ │
│  │  28px ↕                       │ │  ← Espacement entre sections
│  │                               │ │
│  │  ┌─────────────────────────┐ │ │
│  │  │  32px padding           │ │ │  ← Padding dans la carte
│  │  │                         │ │ │
│  │  │  24px ↕                 │ │ │  ← Espacement dans la carte
│  │  │                         │ │ │
│  │  └─────────────────────────┘ │ │
│  │                               │ │
│  └───────────────────────────────┘ │
│                                     │
└─────────────────────────────────────┘
```

---

## 🔤 Typographie

### Police Principale
```
Font Family: "Segoe UI", "Inter", "Arial", sans-serif
```

### Hiérarchie

```
H1 (Titre Principal)
  ├─ Taille: 36px
  ├─ Poids: Bold
  ├─ Couleur: #1e293b
  └─ Ombre: Subtile

H2 (Sous-titre)
  ├─ Taille: 16px
  ├─ Poids: Medium (500)
  └─ Couleur: #64748b

Valeurs (Statistiques)
  ├─ Taille: 28px
  ├─ Poids: Bold
  └─ Couleur: Selon le contexte

Labels
  ├─ Taille: 14px
  ├─ Poids: Semi-bold (600)
  └─ Couleur: #64748b

Texte Standard
  ├─ Taille: 13-14px
  ├─ Poids: Normal/Medium
  └─ Couleur: #1e293b
```

---

## 🎯 Points Clés

### ✅ Lisibilité Optimale
- Contraste texte/fond : ≥ 4.5:1
- Tailles de police adaptées
- Espacement généreux

### ✅ Cohérence Visuelle
- Palette harmonieuse
- Styles uniformes
- Espacements réguliers

### ✅ Interactivité Claire
- Effets de survol visibles
- États distincts
- Feedback immédiat

### ✅ Modernité
- Couleurs pastel tendance
- Dégradés subtils
- Ombres douces

---

## 📱 Responsive (Futur)

### Breakpoints Suggérés

```
Desktop Large  : > 1400px  ████████████████
Desktop        : > 1200px  ██████████████
Tablet         : > 768px   ██████████
Mobile         : < 768px   ██████
```

---

## 🎨 Résumé Visuel

```
┌─────────────────────────────────────────────────────────┐
│                                                         │
│  🎨 PALETTE PASTEL                                      │
│  ├─ Bleu ciel, Rose poudré, Vert menthe                │
│  ├─ Indigo, Cyan, Orange, Rouge corail                 │
│  └─ Dégradés doux et harmonieux                        │
│                                                         │
│  📐 STRUCTURE                                           │
│  ├─ Hiérarchie claire                                  │
│  ├─ Espacements généreux                               │
│  └─ Alignements cohérents                              │
│                                                         │
│  ✨ EFFETS                                              │
│  ├─ Ombres subtiles                                    │
│  ├─ Dégradés modernes                                  │
│  ├─ Coins arrondis                                     │
│  └─ Animations de survol                               │
│                                                         │
│  🎯 RÉSULTAT                                            │
│  └─ Interface moderne, lisible et agréable             │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

**🎨 Interface "Mes Demandes" - Version 2.0**  
**Design moderne avec couleurs pastel douces**  
**✅ Prêt à l'emploi !**
