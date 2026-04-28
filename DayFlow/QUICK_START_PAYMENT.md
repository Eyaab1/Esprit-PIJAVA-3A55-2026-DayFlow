# 🚀 Guide de Démarrage Rapide - Module de Paiement

## ⏱️ Temps estimé : 15 minutes

---

## 📋 Checklist Rapide

### ✅ Étape 1 : Base de Données (5 min)

```bash
# 1. Se connecter à PostgreSQL
psql -U postgres -d dayflow

# 2. Exécuter le script de création
\i database/migrations/create_payment_table.sql

# 3. (Optionnel) Charger les données de test
\i database/test_data/payment_test_data.sql

# 4. Vérifier
SELECT COUNT(*) FROM payment;
```

**✅ Résultat attendu** : Table `payment` créée avec succès

---

### ✅ Étape 2 : Configuration (2 min)

```bash
# 1. Copier le template de configuration
cp .env.example .env

# 2. Éditer le fichier .env
nano .env  # ou votre éditeur préféré
```

**Contenu minimal du `.env` :**
```env
# Pour l'instant, laisser vide (mode simulation)
STRIPE_SECRET_KEY=
STRIPE_PUBLISHABLE_KEY=
STRIPE_WEBHOOK_SECRET=

# Base de données
DB_HOST=localhost
DB_PORT=5432
DB_NAME=dayflow
DB_USER=postgres
DB_PASSWORD=votre_mot_de_passe
```

**✅ Résultat attendu** : Fichier `.env` créé

---

### ✅ Étape 3 : Compilation (3 min)

```bash
# 1. Nettoyer et compiler le projet
mvn clean compile

# 2. Vérifier qu'il n'y a pas d'erreurs
mvn test-compile
```

**✅ Résultat attendu** : Compilation réussie sans erreurs

---

### ✅ Étape 4 : Test de l'Interface (5 min)

```bash
# 1. Lancer l'application
mvn javafx:run

# 2. Tester le workflow :
```

**Scénario de test :**

1. **Se connecter** en tant qu'utilisateur
2. **Aller dans "Mes demandes"**
3. **Sélectionner une demande acceptée**
4. **Cliquer sur "💳 Payer la séance"**
5. **Vérifier l'ouverture de la fenêtre de paiement**
6. **Cliquer sur "Payer la séance"**
7. **Attendre 2 secondes** (simulation)
8. **Vérifier le message de succès**

**✅ Résultat attendu** : 
- Fenêtre de paiement s'ouvre
- Informations affichées correctement
- Paiement simulé avec succès
- Statut mis à jour

---

## 🎯 Vérifications Rapides

### Vérification 1 : Base de Données

```sql
-- La table existe ?
SELECT table_name FROM information_schema.tables WHERE table_name = 'payment';

-- Les index sont créés ?
SELECT indexname FROM pg_indexes WHERE tablename = 'payment';

-- Le trigger fonctionne ?
SELECT tgname FROM pg_trigger WHERE tgrelid = 'payment'::regclass;
```

### Vérification 2 : Fichiers Java

```bash
# Tous les fichiers sont présents ?
ls -la src/main/java/enums/PaymentStatus.java
ls -la src/main/java/model/payment/Payment.java
ls -la src/main/java/services/payment/PaymentService.java
ls -la src/main/java/controllers/payment/PaymentController.java
ls -la src/main/java/config/StripeConfig.java
```

### Vérification 3 : Fichiers FXML/CSS

```bash
# Interface de paiement présente ?
ls -la src/main/resources/user/payment/payment.fxml
ls -la src/main/resources/user/payment/payment.css
```

---

## 🐛 Problèmes Courants

### Problème 1 : Erreur de compilation

**Symptôme** : `cannot find symbol: class Payment`

**Solution** :
```bash
mvn clean compile
```

### Problème 2 : Table payment n'existe pas

**Symptôme** : `ERROR: relation "payment" does not exist`

**Solution** :
```bash
psql -U postgres -d dayflow -f database/migrations/create_payment_table.sql
```

### Problème 3 : Bouton "Payer" désactivé

**Symptôme** : Le bouton reste grisé

**Solution** :
- Vérifier que la demande a le statut `accepted`
- Vérifier dans la base :
```sql
SELECT id, status FROM coaching_request WHERE id = 1;
UPDATE coaching_request SET status = 'accepted' WHERE id = 1;
```

### Problème 4 : Fenêtre de paiement ne s'ouvre pas

**Symptôme** : Erreur `IOException` ou `NullPointerException`

**Solution** :
- Vérifier que le fichier FXML existe
- Vérifier les logs de la console
- Vérifier le chemin : `/user/payment/payment.fxml`

---

## 📊 Tests Manuels Rapides

### Test 1 : Création de Paiement

```java
// Dans un test ou main temporaire
PaymentService service = new PaymentService();
CoachingRequest request = /* récupérer une demande acceptée */;
Payment payment = service.createPaymentForRequest(request, new BigDecimal("50.00"));
System.out.println("Paiement créé : " + payment.getId());
```

### Test 2 : Simulation de Paiement Réussi

```java
PaymentService service = new PaymentService();
service.markPaymentAsSucceeded(1, "pi_test_123", "https://stripe.com/receipt");
System.out.println("Paiement marqué comme réussi");
```

### Test 3 : Vérification SQL

```sql
-- Créer un paiement de test
INSERT INTO payment (coaching_request_id, user_id, coach_id, amount, currency, status)
VALUES (1, 1, 2, 50.00, 'EUR', 'pending');

-- Vérifier
SELECT * FROM payment ORDER BY id DESC LIMIT 1;

-- Mettre à jour le statut
UPDATE payment SET status = 'succeeded', paid_at = NOW() WHERE id = 1;

-- Vérifier la mise à jour automatique de updated_at
SELECT id, status, created_at, updated_at, paid_at FROM payment WHERE id = 1;
```

---

## 🎨 Aperçu de l'Interface

### Fenêtre de Paiement

```
┌─────────────────────────────────────────────┐
│  💳 Paiement de la séance                   │
│  Confirmez votre présence en réglant        │
├─────────────────────────────────────────────┤
│  📋 Informations de la séance               │
│  Demande : Demande #1                       │
│  Coach : Jean Dupont                        │
│  Date : 15/01/2024 à 14:30                  │
│  Type : Coaching individuel                 │
├─────────────────────────────────────────────┤
│  💰 Détails du paiement                     │
│  Montant : 50.00 EUR                        │
│  Statut : En attente                        │
├─────────────────────────────────────────────┤
│  [💳 Payer la séance]  [Annuler]  [Fermer] │
├─────────────────────────────────────────────┤
│  🔒 Paiement sécurisé par Stripe            │
└─────────────────────────────────────────────┘
```

### Bouton dans "Mes Demandes"

```
┌─────────────────────────────────────────────┐
│  Mes demandes de coaching                   │
├─────────────────────────────────────────────┤
│  [Tableau des demandes]                     │
│  ID | Coach | Message | Statut | Date       │
│  1  | Jean  | ...     | Acceptée | 15/01    │
├─────────────────────────────────────────────┤
│  [💳 Payer] [Modifier] [Supprimer] [↻]     │
└─────────────────────────────────────────────┘
```

---

## 🔄 Workflow Complet

```
1. Utilisateur envoie demande
   └─> Status: PENDING
   
2. Coach accepte
   └─> Status: ACCEPTED
   └─> Bouton "Payer" activé ✅
   
3. Utilisateur clique "Payer"
   └─> Fenêtre de paiement s'ouvre
   └─> Payment créé (status: PENDING)
   
4. Utilisateur clique "💳 Payer la séance"
   └─> [SIMULATION] Attente 2 secondes
   └─> Payment status: SUCCEEDED
   └─> Request status: PAID
   
5. Confirmation
   └─> Message de succès
   └─> Fenêtre se ferme
   └─> Liste rafraîchie
```

---

## 📚 Documentation Complète

Pour plus de détails, consultez :

- **PAYMENT_MODULE_GUIDE.md** : Guide complet d'intégration Stripe
- **PAYMENT_IMPLEMENTATION_SUMMARY.md** : Résumé de l'implémentation
- **database/README.md** : Documentation de la base de données

---

## 🎯 Prochaines Étapes

### Pour Activer Stripe (Optionnel)

1. **Créer un compte Stripe** : https://stripe.com
2. **Récupérer les clés API** (mode test)
3. **Ajouter dans `.env`** :
   ```env
   STRIPE_SECRET_KEY=sk_test_votre_cle
   STRIPE_PUBLISHABLE_KEY=pk_test_votre_cle
   ```
4. **Ajouter la dépendance** dans `pom.xml` :
   ```xml
   <dependency>
       <groupId>com.stripe</groupId>
       <artifactId>stripe-java</artifactId>
       <version>24.0.0</version>
   </dependency>
   ```
5. **Implémenter le checkout réel** (voir PAYMENT_MODULE_GUIDE.md)

---

## ✅ Validation Finale

Cochez chaque élément :

- [ ] Table `payment` créée dans PostgreSQL
- [ ] Fichier `.env` configuré
- [ ] Projet compile sans erreurs
- [ ] Application démarre correctement
- [ ] Bouton "Payer" visible dans "Mes demandes"
- [ ] Fenêtre de paiement s'ouvre
- [ ] Simulation de paiement fonctionne
- [ ] Statut mis à jour après paiement
- [ ] Données visibles dans la base

---

## 🎉 Félicitations !

**Le module de paiement est opérationnel !**

Vous pouvez maintenant :
- ✅ Tester le workflow complet en mode simulation
- ✅ Développer d'autres fonctionnalités
- ✅ Intégrer Stripe quand vous êtes prêt

---

## 📞 Besoin d'Aide ?

- Consultez les logs de l'application
- Vérifiez les logs PostgreSQL
- Relisez la documentation complète
- Testez avec les données de test

**Bon développement ! 🚀**
