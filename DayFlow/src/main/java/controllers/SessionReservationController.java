package controllers;

import exceptions.ReservationLimitExceededException;
import exceptions.PastSessionException;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import model.coaching_session.Session;
import services.coaching_session_module.SessionService;
import session.AppSession;

import java.sql.SQLException;

/**
 * Contrôleur pour la réservation de sessions avec vérification de limite.
 * 
 * Gère:
 * - Affichage du nombre de sessions futures
 * - Affichage du nombre de réservations restantes
 * - Validation avant réservation
 * - Gestion des erreurs de limite dépassée
 */
public class SessionReservationController {

    @FXML private Label futureSessionsLabel;
    @FXML private Label remainingSlotsLabel;
    @FXML private Button reserveButton;

    private SessionService sessionService;
    private int currentUserId;

    @FXML
    public void initialize() {
        sessionService = new SessionService();
        
        // Récupérer l'ID de l'utilisateur courant
        AppSession.getCurrentUser().ifPresent(user -> {
            currentUserId = user.getId();
            updateReservationInfo();
        });
    }

    /**
     * Met à jour l'affichage des informations de réservation.
     */
    private void updateReservationInfo() {
        try {
            int futureCount = sessionService.countFutureSessions(currentUserId);
            int remaining = sessionService.getRemainingSlots(currentUserId);
            int maxLimit = sessionService.getMaxFutureSessions();

            // Afficher le nombre de sessions futures
            futureSessionsLabel.setText(
                String.format("Sessions futures: %d/%d", futureCount, maxLimit)
            );

            // Afficher le nombre de réservations restantes
            if (remaining == 0) {
                remainingSlotsLabel.setText("⚠️ Limite atteinte - Aucune réservation possible");
                remainingSlotsLabel.setStyle("-fx-text-fill: #ef4444;");
                reserveButton.setDisable(true);
            } else {
                remainingSlotsLabel.setText(
                    String.format("Réservations restantes: %d", remaining)
                );
                remainingSlotsLabel.setStyle("-fx-text-fill: #10b981;");
                reserveButton.setDisable(false);
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des informations: " + e.getMessage());
            showError("Erreur", "Impossible de récupérer les informations de réservation");
        }
    }

    /**
     * Gère la réservation d'une session.
     */
    @FXML
    public void handleReserveSession() {
        try {
            // Créer la session
            Session session = new Session();
            session.setStatus(Session.STATUS_CONFIRMED);
            // ... définir les autres propriétés ...

            // Réserver avec vérification de limite
            sessionService.reserveSession(session, currentUserId);

            // Succès
            showSuccess("Réservation confirmée", "Votre session a été réservée avec succès!");
            updateReservationInfo();

        } catch (PastSessionException e) {
            // Session dans le passé
            System.err.println("Réservation refusée - Session dans le passé: " + e.getMessage());
            showWarning(
                "Session dans le passé",
                "Impossible de réserver une session dans le passé.\n\n" + e.getMessage()
            );

        } catch (ReservationLimitExceededException e) {
            // Limite dépassée
            System.err.println("Réservation refusée: " + e.getMessage());
            showWarning(
                "Limite de réservation atteinte",
                e.getUserFriendlyMessage() + "\n\n" +
                "Sessions futures: " + e.getCurrentCount() + "/" + e.getMaxLimit()
            );

        } catch (SQLException e) {
            // Erreur base de données
            System.err.println("Erreur base de données: " + e.getMessage());
            showError("Erreur", "Une erreur est survenue lors de la réservation");
        }
    }

    /**
     * Affiche un message de succès.
     */
    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Affiche un message d'avertissement.
     */
    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Affiche un message d'erreur.
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Rafraîchit les informations de réservation.
     */
    public void refresh() {
        updateReservationInfo();
    }
}
