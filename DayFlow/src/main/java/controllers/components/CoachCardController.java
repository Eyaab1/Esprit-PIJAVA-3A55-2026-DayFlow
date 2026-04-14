package controllers.components;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import model.user.User;

import java.util.List;

/**
 * Carte coach (pastel) — données issues du modèle {@link User}.
 */
public class CoachCardController {

    @FXML
    private Label initialsLabel;
    @FXML
    private Label nameLabel;
    @FXML
    private Label specialityLabel;
    @FXML
    private Label ratingLabel;
    @FXML
    private Label priceLabel;
    @FXML
    private Label emailLabel;

    public void setData(User u) {
        if (u == null) {
            initialsLabel.setText("—");
            nameLabel.setText("—");
            specialityLabel.setText("—");
            ratingLabel.setText("—");
            priceLabel.setText("—");
            emailLabel.setText("—");
            return;
        }

        String fn = u.getFirstName() != null ? u.getFirstName() : "";
        String ln = u.getLastName() != null ? u.getLastName() : "";
        String full = (fn + " " + ln).trim();
        nameLabel.setText(full.isEmpty() ? "Coach" : full);

        initialsLabel.setText(computeInitials(fn, ln));
        specialityLabel.setText(resolveSpeciality(u));

        Double rating = u.getRating();
        if (rating == null || Double.isNaN(rating)) {
            ratingLabel.setText("Pas de note");
        } else {
            ratingLabel.setText(String.format("★ %.1f", rating));
        }

        Double price = u.getPricePerSession();
        if (price == null || Double.isNaN(price)) {
            priceLabel.setText("Tarif sur demande");
        } else {
            priceLabel.setText(String.format("%.0f € / séance", price));
        }

        String em = u.getEmail() != null ? u.getEmail() : "—";
        emailLabel.setText(em);
    }

    private static String computeInitials(String firstName, String lastName) {
        StringBuilder sb = new StringBuilder();
        if (!firstName.isEmpty()) {
            sb.append(Character.toUpperCase(firstName.charAt(0)));
        }
        if (!lastName.isEmpty()) {
            sb.append(Character.toUpperCase(lastName.charAt(0)));
        }
        return sb.length() > 0 ? sb.toString() : "?";
    }

    private static String resolveSpeciality(User u) {
        if (u.getSpeciality() != null && !u.getSpeciality().isBlank()) {
            return u.getSpeciality();
        }
        List<String> specs = u.getSpecialities();
        if (specs != null && !specs.isEmpty()) {
            return String.join(" · ", specs);
        }
        return "Coaching général";
    }
}
