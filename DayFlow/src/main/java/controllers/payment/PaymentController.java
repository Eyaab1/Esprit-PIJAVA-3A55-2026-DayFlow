package controllers.payment;

import enums.PaymentStatus;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.coaching_session.CoachingRequest;
import model.payment.Payment;
import model.user.User;
import services.account.UserService;
import services.coaching_session.CoachingRequestService;
import services.payment.PaymentService;
import session.AppSession;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Contrôleur pour la page de paiement d'une séance de coaching.
 */
public class PaymentController implements Initializable {

    // Informations de la demande
    @FXML private Label requestIdLabel;
    @FXML private Label coachNameLabel;
    @FXML private Label sessionDateLabel;
    @FXML private Label sessionTypeLabel;

    // Informations du paiement
    @FXML private Label amountLabel;
    @FXML private Label currencyLabel;
    @FXML private Label statusLabel;
    @FXML private VBox paymentInfoBox;

    // Boutons d'action
    @FXML private Button payButton;
    @FXML private Button cancelButton;
    @FXML private Button closeButton;

    // Messages
    @FXML private Label messageLabel;
    @FXML private ProgressIndicator progressIndicator;

    // Services
    private final PaymentService paymentService;
    private final CoachingRequestService requestService;
    private final UserService userService;

    // Données
    private CoachingRequest coachingRequest;
    private Payment payment;
    private User currentUser;

    public PaymentController() {
        this.paymentService = new PaymentService();
        this.requestService = new CoachingRequestService();
        this.userService = new UserService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Récupérer l'utilisateur connecté
        currentUser = AppSession.getCurrentUser().orElse(null);
        
        // Masquer le progress indicator au départ
        if (progressIndicator != null) {
            progressIndicator.setVisible(false);
        }

        // Configuration initiale des boutons
        setupButtons();
    }

    /**
     * Charge les informations de paiement pour une demande de coaching.
     *
     * @param request la demande de coaching
     */
    public void loadPaymentForRequest(CoachingRequest request) {
        this.coachingRequest = request;

        try {
            // Charger ou créer le paiement
            Optional<Payment> existingPayment = paymentService.findByCoachingRequestId(request.getId());
            
            if (existingPayment.isPresent()) {
                this.payment = existingPayment.get();
            } else {
                // Créer un nouveau paiement avec le montant du budget de la demande
                BigDecimal amount = request.getBudget() != null 
                    ? BigDecimal.valueOf(request.getBudget()) 
                    : BigDecimal.valueOf(50.00); // Montant par défaut
                
                this.payment = paymentService.createPaymentForRequest(request, amount);
            }

            // Afficher les informations
            displayRequestInfo();
            displayPaymentInfo();
            updateButtonStates();

        } catch (SQLException e) {
            showError("Erreur de chargement", "Impossible de charger les informations de paiement: " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalStateException e) {
            showError("Erreur", e.getMessage());
        }
    }

    /**
     * Affiche les informations de la demande de coaching.
     */
    private void displayRequestInfo() {
        if (coachingRequest == null) return;

        requestIdLabel.setText("Demande #" + coachingRequest.getId());

        // Charger les informations du coach
        try {
            Optional<User> coach = userService.findById(coachingRequest.getCoachId());
            if (coach.isPresent()) {
                User c = coach.get();
                coachNameLabel.setText(c.getFirstName() + " " + c.getLastName());
            } else {
                coachNameLabel.setText("Coach #" + coachingRequest.getCoachId());
            }
        } catch (SQLException e) {
            coachNameLabel.setText("Coach #" + coachingRequest.getCoachId());
        }

        // Date de création de la demande
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy à HH:mm");
        sessionDateLabel.setText(sdf.format(coachingRequest.getCreatedAt()));

        // Type de coaching
        String type = coachingRequest.getCoachingType() != null 
            ? coachingRequest.getCoachingType() 
            : "Non spécifié";
        sessionTypeLabel.setText(type);
    }

    /**
     * Affiche les informations du paiement.
     */
    private void displayPaymentInfo() {
        if (payment == null) return;

        amountLabel.setText(String.format("%.2f", payment.getAmount()));
        currencyLabel.setText(payment.getCurrency());
        statusLabel.setText(payment.getStatus().getDisplayName());

        // Appliquer un style selon le statut
        statusLabel.getStyleClass().removeAll("status-pending", "status-processing", 
                                              "status-succeeded", "status-failed", "status-cancelled");
        
        switch (payment.getStatus()) {
            case PENDING -> statusLabel.getStyleClass().add("status-pending");
            case PROCESSING -> statusLabel.getStyleClass().add("status-processing");
            case SUCCEEDED -> statusLabel.getStyleClass().add("status-succeeded");
            case FAILED, CANCELLED -> statusLabel.getStyleClass().add("status-failed");
        }
    }

    /**
     * Configure les boutons.
     */
    private void setupButtons() {
        payButton.setOnAction(event -> handlePayment());
        cancelButton.setOnAction(event -> handleCancel());
        closeButton.setOnAction(event -> handleClose());
    }

    /**
     * Met à jour l'état des boutons selon le statut du paiement.
     */
    private void updateButtonStates() {
        if (payment == null) {
            payButton.setDisable(true);
            cancelButton.setDisable(true);
            return;
        }

        PaymentStatus status = payment.getStatus();
        
        // Bouton Payer : actif seulement si pending ou failed
        payButton.setDisable(status != PaymentStatus.PENDING && status != PaymentStatus.FAILED);
        
        // Bouton Annuler : actif seulement si pending ou processing
        cancelButton.setDisable(!payment.canBeCancelled());

        // Si le paiement est réussi, afficher un message de succès
        if (status == PaymentStatus.SUCCEEDED) {
            showMessage("Paiement réussi ! Votre séance est confirmée.", "success");
        }
    }

    /**
     * Gère le clic sur le bouton Payer.
     */
    @FXML
    private void handlePayment() {
        if (payment == null) return;

        try {
            // Afficher le progress indicator
            progressIndicator.setVisible(true);
            payButton.setDisable(true);
            showMessage("Redirection vers Stripe...", "info");

            // Initier le paiement Stripe
            String checkoutUrl = paymentService.initiateStripeCheckout(payment);

            openCheckoutInBrowser(checkoutUrl);
            showMessage("Finalisez le paiement dans Stripe, puis revenez ici. Vérification en cours...", "info");
            pollPaymentStatusUntilPaid();

        } catch (SQLException | IllegalStateException e) {
            progressIndicator.setVisible(false);
            payButton.setDisable(false);
            showError("Erreur de paiement", "Impossible d'initier le paiement: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Ouvre Stripe Checkout dans le navigateur système.
     */
    private void openCheckoutInBrowser(String checkoutUrl) {
        if (checkoutUrl == null || checkoutUrl.isBlank()) {
            throw new IllegalStateException("URL Stripe Checkout invalide.");
        }
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(checkoutUrl));
            } else {
                throw new IllegalStateException("Ouverture automatique du navigateur non supportee.");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Impossible d'ouvrir Stripe Checkout dans le navigateur.", e);
        }
    }

    /**
     * Vérifie périodiquement le statut de la session Checkout jusqu'au paiement.
     */
    private void pollPaymentStatusUntilPaid() {
        new Thread(() -> {
            try {
                int maxAttempts = 120; // ~10 minutes (120 * 5s)
                int attempts = 0;
                while (attempts < maxAttempts) {
                    attempts++;
                    Thread.sleep(5000);
                    Optional<PaymentService.StripeCheckoutStatus> checkoutStatus =
                            paymentService.fetchCheckoutStatus(payment.getStripeCheckoutSessionId());
                    if (checkoutStatus.isPresent()) {
                        PaymentService.StripeCheckoutStatus status = checkoutStatus.get();
                        String paymentIntentId = status.paymentIntentId() != null
                                ? status.paymentIntentId()
                                : "pi_unknown_" + System.currentTimeMillis();
                        paymentService.markPaymentAsSucceeded(payment.getId(), paymentIntentId, status.receiptUrl());
                        javafx.application.Platform.runLater(() -> refreshUiAfterSuccessfulPayment());
                        return;
                    }
                }
                javafx.application.Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    payButton.setDisable(false);
                    showMessage("Paiement non confirme pour le moment. Vous pouvez reessayer ou fermer cette fenetre.", "info");
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                javafx.application.Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    payButton.setDisable(false);
                    showError("Interruption", "La verification du paiement a ete interrompue.");
                });
            } catch (SQLException e) {
                javafx.application.Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    payButton.setDisable(false);
                    showError("Erreur", "Impossible de confirmer le paiement: " + e.getMessage());
                });
            }
        }).start();
    }

    private void refreshUiAfterSuccessfulPayment() {
        try {
            Optional<Payment> updated = paymentService.findById(payment.getId());
            if (updated.isPresent()) {
                payment = updated.get();
                displayPaymentInfo();
                updateButtonStates();
            }
            progressIndicator.setVisible(false);
            showSuccess("Paiement réussi !", "Votre séance de coaching est maintenant confirmée.");
        } catch (SQLException e) {
            progressIndicator.setVisible(false);
            payButton.setDisable(false);
            showError("Erreur", "Paiement reçu mais impossible de recharger les données: " + e.getMessage());
        }
    }

    /**
     * Gère le clic sur le bouton Annuler.
     */
    @FXML
    private void handleCancel() {
        if (payment == null) return;

        // Confirmation
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Annuler le paiement");
        alert.setContentText("Êtes-vous sûr de vouloir annuler ce paiement ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                paymentService.cancelPayment(payment.getId());
                
                // Recharger le paiement
                Optional<Payment> updated = paymentService.findById(payment.getId());
                if (updated.isPresent()) {
                    payment = updated.get();
                    displayPaymentInfo();
                    updateButtonStates();
                }

                showMessage("Paiement annulé", "info");

            } catch (SQLException e) {
                showError("Erreur", "Impossible d'annuler le paiement: " + e.getMessage());
            } catch (IllegalStateException e) {
                showError("Erreur", e.getMessage());
            }
        }
    }

    /**
     * Gère le clic sur le bouton Fermer.
     */
    @FXML
    private void handleClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    // Méthodes utilitaires pour les messages

    private void showMessage(String message, String type) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.getStyleClass().removeAll("message-success", "message-error", "message-info");
            messageLabel.getStyleClass().add("message-" + type);
            messageLabel.setVisible(true);
        }
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
