package services.reclamation_services;

import model.reclamation.Reclamation;
import model.reclamation.Response;

/**
 * Équivalent de {@code ReclamationNotificationService} Symfony (e-mail / push).
 * Implémentation no-op : branchez votre envoi réel ici.
 */
public class ReclamationNotificationService {

    public void notifyNewReclamation(Reclamation reclamation) {
        // TODO: notifier l'équipe / l'utilisateur
    }

    public void notifyReclamationResponse(Reclamation reclamation, Response response) {
        // TODO: e-mail à l'utilisateur avec la réponse admin
    }
}
