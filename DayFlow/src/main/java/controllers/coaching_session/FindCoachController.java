package controllers.coaching_session;

import controllers.navigation.NavigationManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import model.coaching_session.CoachingRequest;
import model.user.User;
import services.account.UserService;
import services.coaching_session.CoachSearchParams;
import services.coaching_session.CoachingRequestService;
import session.AppSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Recherche de coachs + formulaire de demande de coaching.
 */
public class FindCoachController {

    private final UserService userService = new UserService();
    private final CoachingRequestService coachingRequestService = new CoachingRequestService();

    @FXML
    private TextField searchField;
    @FXML
    private TextField minPriceField;
    @FXML
    private TextField maxPriceField;
    @FXML
    private ComboBox<String> specialityCombo;
    @FXML
    private ComboBox<String> ratingCombo;
    @FXML
    private ComboBox<String> availabilityCombo;
    @FXML
    private FlowPane coachGrid;
    @FXML
    private Label coachCountChip;
    @FXML
    private ComboBox<User> coachCombo;
    @FXML
    private TextArea messageArea;
    @FXML
    private Label charCountLabel;
    @FXML
    private RadioButton radioNormal;
    @FXML
    private RadioButton radioMedium;
    @FXML
    private RadioButton radioUrgent;
    @FXML
    private ComboBox<String> goalCombo;
    @FXML
    private ComboBox<String> levelCombo;
    @FXML
    private ComboBox<String> frequencyCombo;
    @FXML
    private TextField budgetField;

    private List<User> lastCoaches = new ArrayList<>();

    @FXML
    private void initialize() {
        try {
            specialityCombo.getItems().add("Toutes les spécialités");
            specialityCombo.getItems().addAll(userService.findAllCoachSpecialities());
            specialityCombo.setValue("Toutes les spécialités");

            ratingCombo.getItems().addAll("Toutes", "3.0 et +", "4.0 et +", "4.5 et +");
            ratingCombo.setValue("Toutes");

            availabilityCombo.getItems().add("Toutes");
            availabilityCombo.getItems().addAll(userService.findAllCoachAvailabilities());
            availabilityCombo.setValue("Toutes");

            goalCombo.getItems().addAll(
                    "Sélectionnez votre objectif",
                    "Concentration & productivité",
                    "Bien-être & stress",
                    "Sport & forme",
                    "Nutrition",
                    "Carrière & leadership"
            );
            goalCombo.setValue("Sélectionnez votre objectif");

            levelCombo.getItems().addAll(
                    "Sélectionnez votre niveau",
                    "Débutant",
                    "Intermédiaire",
                    "Avancé"
            );
            levelCombo.setValue("Sélectionnez votre niveau");

            frequencyCombo.getItems().addAll(
                    "Sélectionnez la fréquence",
                    "Hebdomadaire",
                    "Bi-mensuel",
                    "Mensuel",
                    "Ponctuel"
            );
            frequencyCombo.setValue("Sélectionnez la fréquence");

            minPriceField.setText("0");
            maxPriceField.setText("200");

            setupCoachCombo();
            messageArea.textProperty().addListener((o, old, v) -> updateCharCount());
            updateCharCount();

            loadCoaches();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Chargement : " + e.getMessage()).showAndWait();
        }
    }

    private void setupCoachCombo() {
        coachCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                if (empty || u == null) {
                    setText(null);
                } else {
                    setText(formatCoachName(u));
                }
            }
        });
        coachCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                if (empty || u == null) {
                    setText(null);
                } else {
                    setText(formatCoachName(u));
                }
            }
        });
    }

    private static String formatCoachName(User u) {
        String fn = u.getFirstName() != null ? u.getFirstName() : "";
        String ln = u.getLastName() != null ? u.getLastName() : "";
        return (fn + " " + ln).trim();
    }

    private void updateCharCount() {
        int n = messageArea.getText() != null ? messageArea.getText().length() : 0;
        charCountLabel.setText(n + " / 1000");
    }

    @FXML
    private void onBackDashboard() {
        try {
            NavigationManager.show("/user/account/user_dashboard.fxml", "DayFlow — Accueil");
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onSearch() {
        loadCoaches();
    }

    @FXML
    private void onApplyFilters() {
        loadCoaches();
    }

    @FXML
    private void onResetFilters() {
        searchField.clear();
        specialityCombo.setValue("Toutes les spécialités");
        minPriceField.setText("0");
        maxPriceField.setText("200");
        ratingCombo.setValue("Toutes");
        availabilityCombo.setValue("Toutes");
        loadCoaches();
    }

    private void loadCoaches() {
        try {
            CoachSearchParams params = buildParams();
            lastCoaches = userService.searchCoaches(params);
            coachGrid.getChildren().clear();
            for (User u : lastCoaches) {
                coachGrid.getChildren().add(buildCoachCard(u));
            }
            coachCombo.getItems().setAll(lastCoaches);
            if (!lastCoaches.isEmpty() && coachCombo.getValue() == null) {
                coachCombo.setValue(lastCoaches.get(0));
            }
            int n = lastCoaches.size();
            coachCountChip.setText(n + " coach" + (n > 1 ? "s" : "") + " disponible" + (n > 1 ? "s" : ""));
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Recherche : " + e.getMessage()).showAndWait();
        }
    }

    private CoachSearchParams buildParams() {
        String q = searchField.getText() != null ? searchField.getText().trim() : "";
        String spec = specialityCombo.getValue();
        if (spec != null && spec.startsWith("Toutes")) {
            spec = "";
        } else if (spec == null) {
            spec = "";
        }
        Double minP = parseDoubleOrNull(minPriceField.getText());
        Double maxP = parseDoubleOrNull(maxPriceField.getText());
        Double minR = null;
        String rat = ratingCombo.getValue();
        if (rat != null && !rat.startsWith("Toutes")) {
            String num = rat.replace(" et +", "").trim();
            minR = Double.parseDouble(num);
        }
        String avail = availabilityCombo.getValue();
        if (avail != null && "Toutes".equals(avail)) {
            avail = "";
        } else if (avail == null) {
            avail = "";
        }
        return new CoachSearchParams(q, spec, minP, maxP, minR, avail, "", "rating", "desc");
    }

    private static Double parseDoubleOrNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(s.replace(',', '.').trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private VBox buildCoachCard(User u) {
        VBox card = new VBox(8);
        card.getStyleClass().add("coach-card");

        HBox head = new HBox(10);
        Label av = new Label(initials(u));
        av.getStyleClass().add("coach-avatar");
        av.setMinSize(40, 40);
        av.setAlignment(javafx.geometry.Pos.CENTER);
        av.setStyle("-fx-background-color: #6366f1; -fx-background-radius: 20; -fx-text-fill: white; -fx-font-weight: bold;");

        VBox nameBlock = new VBox(2);
        Label name = new Label(formatCoachName(u));
        name.getStyleClass().add("coach-name");
        String em = u.getEmail() != null ? u.getEmail() : "—";
        Label email = new Label("✉ " + em);
        email.getStyleClass().add("coach-email");
        nameBlock.getChildren().addAll(name, email);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label online = new Label(u.isOnline() ? "En ligne" : "Hors ligne");
        online.getStyleClass().add(u.isOnline() ? "badge-online" : "badge-offline");

        head.getChildren().addAll(av, nameBlock, sp, online);

        String spec = u.getSpeciality() != null ? u.getSpeciality() : "Coach";
        Label specBadge = new Label("★ " + spec);
        specBadge.getStyleClass().add("badge-spec");

        Double rating = u.getRating();
        String stars = rating != null ? String.format(Locale.FRENCH, "%.1f/5", rating) : "—/5";
        Label rate = new Label("⭐ " + stars + " · Voir les avis");
        rate.getStyleClass().add("coach-email");

        Double price = u.getPricePerSession();
        Label priceL = new Label(price != null ? String.format(Locale.FRENCH, "%.0f € / session", price) : "Prix sur demande");
        priceL.getStyleClass().add("coach-name");

        String ava = u.getAvailability() != null ? u.getAvailability() : "—";
        Label avL = new Label("📅 " + ava);
        avL.setWrapText(true);
        avL.getStyleClass().add("coach-email");

        Button b1 = new Button("📅 Voir disponibilités");
        b1.getStyleClass().add("btn-card");
        b1.setOnAction(e -> new Alert(Alert.AlertType.INFORMATION, "Calendrier des créneaux — bientôt disponible.").showAndWait());

        Button b2 = new Button("✈ Demande sans créneau");
        b2.getStyleClass().add("btn-card-outline");
        b2.setMaxWidth(Double.MAX_VALUE);
        b2.setOnAction(e -> {
            coachCombo.setValue(u);
            createRequestWithoutSlot(u);
        });

        card.getChildren().addAll(head, specBadge, rate, priceL, avL, b1, b2);
        return card;
    }

    private static String initials(User u) {
        String a = u.getFirstName() != null && !u.getFirstName().isBlank()
                ? u.getFirstName().substring(0, 1).toUpperCase(Locale.ROOT) : "";
        String b = u.getLastName() != null && !u.getLastName().isBlank()
                ? u.getLastName().substring(0, 1).toUpperCase(Locale.ROOT) : "";
        String s = a + b;
        return s.isEmpty() ? "?" : s;
    }

    @FXML
    private void onSubmitRequest() {
        Optional<User> me = AppSession.getCurrentUser();
        if (me.isEmpty() || me.get().getId() == null) {
            new Alert(Alert.AlertType.WARNING, "Session expirée. Reconnectez-vous.").showAndWait();
            return;
        }
        User coach = coachCombo.getValue();
        if (coach == null || coach.getId() == null) {
            new Alert(Alert.AlertType.WARNING, "Sélectionnez un coach.").showAndWait();
            return;
        }
        String msg = messageArea.getText() != null ? messageArea.getText().trim() : "";
        if (msg.length() < 10) {
            new Alert(Alert.AlertType.WARNING, "Le message doit contenir au moins 10 caractères.").showAndWait();
            return;
        }
        if (msg.length() > 1000) {
            new Alert(Alert.AlertType.WARNING, "Le message ne peut pas dépasser 1000 caractères.").showAndWait();
            return;
        }

        try {
            coachingRequestService.createRequest(coach, me.get(), msg, CoachingRequest.STATUS_PENDING);
            new Alert(Alert.AlertType.INFORMATION, "Votre demande a été envoyée avec succès !").showAndWait();
            messageArea.clear();
            updateCharCount();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        } catch (IllegalArgumentException ex) {
            new Alert(Alert.AlertType.WARNING, ex.getMessage()).showAndWait();
        }
    }

    private void createRequestWithoutSlot(User coach) {
        Optional<User> me = AppSession.getCurrentUser();
        if (me.isEmpty() || me.get().getId() == null) {
            new Alert(Alert.AlertType.WARNING, "Session expirée. Reconnectez-vous.").showAndWait();
            return;
        }
        if (coach == null || coach.getId() == null) {
            new Alert(Alert.AlertType.WARNING, "Coach invalide.").showAndWait();
            return;
        }

        String msg = messageArea.getText() != null ? messageArea.getText().trim() : "";
        try {
            coachingRequestService.createRequest(coach, me.get(), msg, CoachingRequest.STATUS_PENDING);
            new Alert(Alert.AlertType.INFORMATION, "Demande sans créneau créée avec succès.").showAndWait();
            messageArea.clear();
            updateCharCount();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        } catch (IllegalArgumentException ex) {
            new Alert(Alert.AlertType.WARNING, ex.getMessage()).showAndWait();
        }
    }
}
