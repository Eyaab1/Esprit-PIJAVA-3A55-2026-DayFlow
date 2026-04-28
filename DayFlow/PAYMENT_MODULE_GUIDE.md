# 💳 Guide du Module de Paiement - DayFlow

## 📋 Vue d'ensemble

Ce document décrit l'architecture complète du module de paiement préparé pour l'intégration Stripe. Toute la structure est en place et prête à recevoir la clé API Stripe.

---

## 🏗️ Architecture du Module

### 1. **Structure de la Base de Données**

#### Table `payment`
```sql
CREATE TABLE payment (
    id SERIAL PRIMARY KEY,
    coaching_request_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    coach_id INTEGER NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'EUR',
    status VARCHAR(20) DEFAULT 'pending',
    stripe_payment_intent_id VARCHAR(255),
    stripe_checkout_session_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMP,
    failure_reason TEXT,
    receipt_url VARCHAR(500)
);
```

**Script de migration** : `database/migrations/create_payment_table.sql`

---

### 2. **Modèle de Données**

#### Enum `PaymentStatus`
- `PENDING` : Paiement en attente
- `PROCESSING` : En cours de traitement
- `SUCCEEDED` : Paiement réussi
- `FAILED` : Paiement échoué
- `CANCELLED` : Paiement annulé
- `REFUNDED` : Paiement remboursé

#### Classe `Payment`
Localisation : `model/payment/Payment.java`

Propriétés principales :
- Montant et devise
- Statuts de paiement
- IDs Stripe (PaymentIntent et CheckoutSession)
- Dates de création, mise à jour et paiement
- URL du reçu

---

### 3. **Services**

#### `PaymentService`
Localisation : `services/payment/PaymentService.java`

**Méthodes principales :**

```java
// Création et gestion
Payment createPaymentForRequest(CoachingRequest request, BigDecimal amount)
String initiateStripeCheckout(Payment payment)

// Mise à jour des statuts
void markPaymentAsSucceeded(int paymentId, String intentId, String receiptUrl)
void markPaymentAsFailed(int paymentId, String failureReason)
void cancelPayment(int paymentId)

// Recherche
Optional<Payment> findById(int id)
Optional<Payment> findByCoachingRequestId(int requestId)
Optional<Payment> findByStripeCheckoutSessionId(String sessionId)
List<Payment> findByUserId(int userId)
List<Payment> findByCoachId(int coachId)

// Statistiques
boolean hasSuccessfulPayment(int coachingRequestId)
BigDecimal calculateTotalEarnings(int coachId)
int countByUserAndStatus(int userId, PaymentStatus status)
```

---

### 4. **Contrôleurs**

#### `PaymentController`
Localisation : `controllers/payment/PaymentController.java`

**Fonctionnalités :**
- Affichage des informations de la demande de coaching
- Affichage du montant à payer
- Gestion du bouton "Payer la séance"
- Simulation du paiement (à remplacer par Stripe)
- Gestion de l'annulation
- Mise à jour des statuts en temps réel

#### `MesDemandesController` (modifié)
**Ajout :**
- Bouton "💳 Payer la séance"
- Activation uniquement si statut = `accepted`
- Ouverture de la fenêtre de paiement modale

---

### 5. **Interface Utilisateur**

#### Fichiers FXML et CSS
- `src/main/resources/user/payment/payment.fxml`
- `src/main/resources/user/payment/payment.css`

**Design :**
- Couleurs pastel douces (bleu ciel, lavande, rose)
- Interface claire et professionnelle
- Indicateurs de statut colorés
- Note de sécurité Stripe
- Boutons d'action bien visibles

---

## 🔄 Workflow de Paiement

### Flux Complet

```
1. Utilisateur envoie une demande de coaching
   └─> Statut: PENDING

2. Coach accepte la demande
   └─> Statut: ACCEPTED
   └─> Bouton "Payer la séance" devient actif

3. Utilisateur clique sur "Payer la séance"
   └─> Création d'un Payment (si n'existe pas)
   └─> Ouverture de la fenêtre de paiement

4. Utilisateur clique sur "💳 Payer la séance"
   └─> Appel à initiateStripeCheckout()
   └─> [À IMPLÉMENTER] Redirection vers Stripe Checkout
   └─> Statut Payment: PROCESSING

5. Stripe traite le paiement
   └─> [À IMPLÉMENTER] Webhook Stripe
   └─> Si succès: markPaymentAsSucceeded()
   └─> Statut Payment: SUCCEEDED
   └─> Statut CoachingRequest: PAID

6. Session confirmée
   └─> Statut CoachingRequest: CONFIRMED
```

---

## 🔧 Intégration Stripe - Étapes Restantes

### 1. **Ajouter la Dépendance Stripe**

Dans `pom.xml` :
```xml
<dependency>
    <groupId>com.stripe</groupId>
    <artifactId>stripe-java</artifactId>
    <version>24.0.0</version>
</dependency>
```

### 2. **Configuration de la Clé API**

Créer un fichier de configuration :
```java
// config/StripeConfig.java
public class StripeConfig {
    private static final String SECRET_KEY = "sk_test_..."; // Votre clé secrète
    
    static {
        Stripe.apiKey = SECRET_KEY;
    }
}
```

**⚠️ IMPORTANT** : Ne jamais commiter la clé secrète !
- Utiliser des variables d'environnement
- Ou un fichier `.env` (à ajouter dans `.gitignore`)

### 3. **Implémenter `initiateStripeCheckout()`**

Remplacer la simulation dans `PaymentService.java` :

```java
public String initiateStripeCheckout(Payment payment) throws SQLException {
    try {
        // Créer une session Stripe Checkout
        SessionCreateParams params = SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.PAYMENT)
            .setSuccessUrl("http://localhost:8080/payment/success?session_id={CHECKOUT_SESSION_ID}")
            .setCancelUrl("http://localhost:8080/payment/cancel")
            .addLineItem(
                SessionCreateParams.LineItem.builder()
                    .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency(payment.getCurrency().toLowerCase())
                            .setUnitAmount(payment.getAmount().multiply(new BigDecimal(100)).longValue())
                            .setProductData(
                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName("Séance de coaching")
                                    .setDescription("Demande #" + payment.getCoachingRequestId())
                                    .build()
                            )
                            .build()
                    )
                    .setQuantity(1L)
                    .build()
            )
            .putMetadata("payment_id", String.valueOf(payment.getId()))
            .putMetadata("coaching_request_id", String.valueOf(payment.getCoachingRequestId()))
            .build();

        Session session = Session.create(params);
        
        // Sauvegarder l'ID de session
        payment.setStripeCheckoutSessionId(session.getId());
        payment.setStatus(PaymentStatus.PROCESSING);
        update(payment);

        return session.getUrl();
        
    } catch (StripeException e) {
        throw new SQLException("Erreur Stripe: " + e.getMessage(), e);
    }
}
```

### 4. **Ouvrir l'URL Stripe dans le Navigateur**

Dans `PaymentController.java`, remplacer `simulatePaymentSuccess()` :

```java
private void handlePayment() {
    try {
        String checkoutUrl = paymentService.initiateStripeCheckout(payment);
        
        // Ouvrir dans le navigateur par défaut
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(new URI(checkoutUrl));
        }
        
        showMessage("Redirection vers Stripe...", "info");
        
    } catch (Exception e) {
        showError("Erreur", e.getMessage());
    }
}
```

### 5. **Créer un Webhook Stripe**

Créer un endpoint pour recevoir les événements Stripe :

```java
// controllers/payment/StripeWebhookController.java
@RestController
@RequestMapping("/api/stripe/webhook")
public class StripeWebhookController {
    
    @PostMapping
    public ResponseEntity<String> handleWebhook(
        @RequestBody String payload,
        @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        Event event = Webhook.constructEvent(
            payload, sigHeader, WEBHOOK_SECRET
        );
        
        if ("checkout.session.completed".equals(event.getType())) {
            Session session = (Session) event.getDataObjectDeserializer()
                .getObject().orElse(null);
            
            if (session != null) {
                handleSuccessfulPayment(session);
            }
        }
        
        return ResponseEntity.ok("Success");
    }
    
    private void handleSuccessfulPayment(Session session) {
        String paymentId = session.getMetadata().get("payment_id");
        String paymentIntentId = session.getPaymentIntent();
        
        paymentService.markPaymentAsSucceeded(
            Integer.parseInt(paymentId),
            paymentIntentId,
            session.getUrl()
        );
    }
}
```

### 6. **Configurer le Webhook dans Stripe Dashboard**

1. Aller sur https://dashboard.stripe.com/webhooks
2. Créer un nouveau endpoint
3. URL : `https://votre-domaine.com/api/stripe/webhook`
4. Événements à écouter :
   - `checkout.session.completed`
   - `payment_intent.succeeded`
   - `payment_intent.payment_failed`

---

## 🎨 Interface Utilisateur

### Couleurs Pastel Utilisées

- **Bleu ciel** : `#e0f2fe`, `#6366f1`
- **Lavande** : `#ddd6fe`, `#8b5cf6`
- **Rose poudré** : `#fce7f3`, `#f472b6`
- **Vert menthe** : `#d1fae5`, `#10b981`

### Statuts Visuels

- 🟡 **Pending** : Orange (`#f59e0b`)
- 🔵 **Processing** : Bleu (`#3b82f6`)
- 🟢 **Succeeded** : Vert (`#10b981`)
- 🔴 **Failed** : Rouge (`#ef4444`)

---

## 🧪 Tests

### Tests Manuels à Effectuer

1. **Création de paiement**
   - Accepter une demande de coaching
   - Vérifier que le bouton "Payer" s'active
   - Cliquer et vérifier l'ouverture de la fenêtre

2. **Simulation de paiement**
   - Actuellement : simulation automatique après 2 secondes
   - Après intégration : tester avec les cartes de test Stripe

3. **Gestion des erreurs**
   - Tester l'annulation
   - Tester un paiement échoué

### Cartes de Test Stripe

```
Succès : 4242 4242 4242 4242
Échec : 4000 0000 0000 0002
3D Secure : 4000 0027 6000 3184
```

---

## 📊 Statistiques et Rapports

### Méthodes Disponibles

```java
// Gains totaux d'un coach
BigDecimal earnings = paymentService.calculateTotalEarnings(coachId);

// Nombre de paiements réussis
int count = paymentService.countByUserAndStatus(userId, PaymentStatus.SUCCEEDED);

// Vérifier si une demande est payée
boolean isPaid = paymentService.hasSuccessfulPayment(requestId);
```

---

## 🔒 Sécurité

### Bonnes Pratiques

1. **Clés API**
   - Ne jamais exposer la clé secrète côté client
   - Utiliser des variables d'environnement
   - Rotation régulière des clés

2. **Validation**
   - Toujours vérifier les webhooks avec la signature
   - Valider les montants côté serveur
   - Vérifier l'état de la demande avant paiement

3. **Logs**
   - Logger tous les événements de paiement
   - Conserver les IDs Stripe pour le support
   - Monitorer les échecs

---

## 📝 Checklist d'Intégration

- [ ] Exécuter le script SQL `create_payment_table.sql`
- [ ] Ajouter la dépendance Stripe dans `pom.xml`
- [ ] Créer un compte Stripe (mode test)
- [ ] Récupérer les clés API (Publishable et Secret)
- [ ] Créer `StripeConfig.java` avec la clé secrète
- [ ] Implémenter `initiateStripeCheckout()` réel
- [ ] Ajouter l'ouverture du navigateur dans `PaymentController`
- [ ] Créer le webhook controller
- [ ] Configurer le webhook dans Stripe Dashboard
- [ ] Tester avec les cartes de test
- [ ] Gérer les cas d'erreur
- [ ] Ajouter des logs
- [ ] Documenter pour l'équipe

---

## 🚀 Prochaines Étapes

1. **Phase 1 : Tests en Mode Développement**
   - Utiliser les clés de test Stripe
   - Tester tous les scénarios
   - Valider le workflow complet

2. **Phase 2 : Améliorations**
   - Ajouter des notifications email
   - Historique des paiements
   - Génération de factures PDF
   - Remboursements

3. **Phase 3 : Production**
   - Passer aux clés de production
   - Configurer le webhook en production
   - Monitoring et alertes
   - Support client

---

## 📞 Support

Pour toute question sur l'intégration Stripe :
- Documentation : https://stripe.com/docs
- Support Stripe : https://support.stripe.com
- Exemples Java : https://github.com/stripe/stripe-java

---

**✅ Le module est prêt ! Il ne reste plus qu'à ajouter votre clé Stripe et finaliser l'intégration.**
