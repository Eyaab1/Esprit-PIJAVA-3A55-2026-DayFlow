package controllers.coach;

import controllers.navigation.NavigationManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.coaching_session.CoachingRequest;
import model.coaching_session.Session;
import model.user.User;
import services.UserServices.UserService;
import services.coaching_session_module.CoachRequestListFilters;
import services.coaching_session_module.CoachStats;
import services.coaching_session_module.CoachingRequestService;
import services.coaching_session_module.CoachingWorkflowService;
import services.coaching_session_module.SessionService;
import session.AppSession;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Tableau de bord coach — demandes reçues, filtres, accepter / refuser (aligné Symfony).
 */
public class CoachDashboardController {

    private final CoachingRequestService coachingRequests = new CoachingRequestService();
    private final SessionService sessionService = new SessionService();
    private final CoachingWorkflowService workflow = new CoachingWorkflowService();
    private final UserService userService = new UserService();

    @FXML
    private Label statPendingLabel;
    @FXML
    private Label statAcceptedLabel;
    @FXML
    private Label statSessionsTodayLabel;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> statusCombo;
    @FXML
    private ComboBox<String> priorityCombo;
    @FXML
    private DatePicker dateFromPicker;
    @FXML
    private DatePicker dateToPicker;
    @FXML
    private VBox pendingBox;
    @FXML
    private VBox allRequestsBox;
    @FXML
    private Label pendingCountBadge;
    @FXML
    private Label pendingEmptyLabel;
    @FXML
    private Label allEmptyLabel;
    @FXML
    private VBox sessionsPreviewBox;
    @FXML
    private Label sessionsEmptyLabel;

    private int coachId;

    @FXML
    private void initialize() {
        if (!AppSession.isCoach()) {
            redirectNonCoach();
            return;
        }
        Integer id = AppSession.getCurrentUser().flatMap(u -> Optional.ofNullable(u.getId())).orElse(null);
        if (id == null) {
            redirectNonCoach();
            return;
        }
        coachId = id;

        statusCombo.getItems().setAll(
                "Tous",
                "En attente",
                "Acceptée",
                "Payée",
                "Confirmée",
                "Terminée",
                "Annulée",
                "Refusée"
        );
        statusCombo.setValue("Tous");

        priorityCombo.getItems().setAll("Toutes", "Faible", "Moyenne", "Urgente");
        priorityCombo.setValue("Toutes");

        reloadAll();
    }

    private void redirectNonCoach() {
        try {
            NavigationManager.show("/user/account/user_dashboard.fxml", "DayFlow — Accueil");
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onBackToUserHome() {
        try {
            NavigationManager.show("/user/account/user_dashboard.fxml", "DayFlow — Accueil");
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onApplyFilters() {
        reloadAll();
    }

    @FXML
    private void onResetFilters() {
        searchField.clear();
        statusCombo.setValue("Tous");
        priorityCombo.setValue("Toutes");
        dateFromPicker.setValue(null);
        dateToPicker.setValue(null);
        reloadAll();
    }

    private void reloadAll() {
        try {
            System.out.println("[CoachDashboardController] reloadAll coachId=" + coachId);
            CoachStats stats = coachingRequests.buildCoachStats(coachId, sessionService);
            statPendingLabel.setText(String.valueOf(stats.pending()));
            statAcceptedLabel.setText(String.valueOf(stats.accepted()));
            statSessionsTodayLabel.setText(String.valueOf(stats.sessionsToday()));

            CoachRequestListFilters filters = currentFilters();
            List<CoachingRequest> all = coachingRequests.findForCoachWithFilters(coachId, filters);
            System.out.println("[CoachDashboardController] demandes chargées=" + all.size());

            List<CoachingRequest> pending = all.stream()
                    .filter(r -> CoachingRequest.STATUS_PENDING.equals(r.getStatus()))
                    .collect(Collectors.toList());
            List<CoachingRequest> accepted = all.stream()
                    .filter(r -> CoachingRequest.STATUS_ACCEPTED.equals(r.getStatus()))
                    .collect(Collectors.toList());

            pendingBox.getChildren().clear();
            pendingCountBadge.setText(String.valueOf(pending.size()));
            pendingEmptyLabel.setVisible(pending.isEmpty());
            for (CoachingRequest cr : pending) {
                pendingBox.getChildren().add(buildRequestCard(cr, true));
            }

            allRequestsBox.getChildren().clear();
            allEmptyLabel.setVisible(accepted.isEmpty());
            for (CoachingRequest cr : accepted) {
                allRequestsBox.getChildren().add(buildRequestCard(cr, false));
            }

            refreshSessionsPreview();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur base de données : " + e.getMessage()).showAndWait();
        }
    }

    private void refreshSessionsPreview() throws SQLException {
        List<Session> sessions = sessionService.getSessionsByCoach(coachId);
        System.out.println("[CoachDashboardController] sessions chargées=" + sessions.size());
        sessionsPreviewBox.getChildren().clear();
        sessionsEmptyLabel.setVisible(sessions.isEmpty());
        int max = Math.min(5, sessions.size());
        for (int i = 0; i < max; i++) {
            sessionsPreviewBox.getChildren().add(buildSessionCard(sessions.get(i)));
        }
    }

    private CoachRequestListFilters currentFilters() {
        String search = searchField.getText() != null ? searchField.getText().trim() : "";
        String status = mapStatusToParam(statusCombo.getValue());
        String priority = mapPriorityToParam(priorityCombo.getValue());
        String from = dateFromPicker.getValue() != null ? dateFromPicker.getValue().toString() : "";
        String to = dateToPicker.getValue() != null ? dateToPicker.getValue().toString() : "";
        return new CoachRequestListFilters(search, status, from, to, priority);
    }

    private static String mapStatusToParam(String display) {
        if (display == null || "Tous".equals(display)) {
            return "";
        }
        return switch (display) {
            case "En attente" -> CoachingRequest.STATUS_PENDING;
            case "Acceptée" -> CoachingRequest.STATUS_ACCEPTED;
            case "Payée" -> CoachingRequest.STATUS_PAID;
            case "Confirmée" -> CoachingRequest.STATUS_CONFIRMED;
            case "Terminée" -> CoachingRequest.STATUS_COMPLETED;
            case "Annulée" -> CoachingRequest.STATUS_CANCELLED;
            case "Refusée" -> CoachingRequest.STATUS_DECLINED;
            default -> "";
        };
    }

    private static String mapPriorityToParam(String display) {
        if (display == null || "Toutes".equals(display)) {
            return "";
        }
        return switch (display) {
            case "Faible" -> CoachingRequest.PRIORITY_NORMAL;
            case "Moyenne" -> CoachingRequest.PRIORITY_MEDIUM;
            case "Urgente" -> CoachingRequest.PRIORITY_URGENT;
            default -> "";
        };
    }

    private VBox buildRequestCard(CoachingRequest cr, boolean showActions) throws SQLException {
        VBox card = new VBox(12);
        card.getStyleClass().add("coach-request-card");
        card.setPadding(new Insets(4, 0, 4, 0));

        Optional<User> clientOpt = userService.findById(cr.getUserId());
        User client = clientOpt.orElse(null);
        String name = client != null
                ? (client.getFirstName() + " " + client.getLastName()).trim()
                : "Utilisateur #" + cr.getUserId();
        String email = client != null && client.getEmail() != null ? client.getEmail() : "—";
        String initials = initials(client);

        HBox top = new HBox(16);
        top.setAlignment(Pos.TOP_LEFT);

        Label avatar = new Label(initials);
        avatar.getStyleClass().add("coach-avatar");

        VBox leftText = new VBox(4);
        Label nm = new Label(name);
        nm.getStyleClass().add("coach-name");
        String dateStr = formatFrench(cr.getCreatedAt());
        Label meta = new Label(email + " · " + dateStr);
        meta.getStyleClass().add("coach-meta");
        leftText.getChildren().addAll(nm, meta);

        HBox badges = new HBox(8);
        badges.setAlignment(Pos.CENTER_LEFT);
        badges.getChildren().add(statusBadge(cr.getStatus()));
        badges.getChildren().add(priorityBadge(cr.getPriority()));
        addSlaBadges(cr, badges);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox right = new HBox(8);
        right.setAlignment(Pos.CENTER_RIGHT);
        if (showActions && CoachingRequest.STATUS_PENDING.equals(cr.getStatus())) {
            Button accept = new Button("✓ Accepter");
            accept.getStyleClass().add("btn-accept");
            accept.setOnAction(e -> onAccept(cr));
            Button decline = new Button("✕ Refuser");
            decline.getStyleClass().add("btn-decline");
            decline.setOnAction(e -> onDecline(cr));
            right.getChildren().addAll(accept, decline);
        }

        top.getChildren().addAll(avatar, leftText, spacer, badges);
        if (!right.getChildren().isEmpty()) {
            top.getChildren().add(right);
        }

        Label msg = new Label("\"" + cr.getMessage() + "\"");
        msg.getStyleClass().add("coach-msg-box");
        msg.setWrapText(true);
        msg.setMaxWidth(Double.MAX_VALUE);

        card.getChildren().add(top);
        card.getChildren().add(msg);
        return card;
    }

    private void addSlaBadges(CoachingRequest cr, HBox badges) {
        if (cr.getCreatedAt() == null) {
            return;
        }
        Instant created = cr.getCreatedAt().toInstant();
        long hours = Duration.between(created, Instant.now()).toHours();
        long days = Duration.between(created, Instant.now()).toDays();
        if (CoachingRequest.STATUS_PENDING.equals(cr.getStatus()) && hours > 48) {
            Label sla = new Label("> 48h");
            sla.getStyleClass().add("badge-sla");
            badges.getChildren().add(sla);
        }
        Label d = new Label(days + "j");
        d.getStyleClass().add("badge-days");
        badges.getChildren().add(d);
    }

    private Label statusBadge(String status) {
        Label l = new Label(formatStatusFr(status));
        if (CoachingRequest.STATUS_PENDING.equals(status)) {
            l.getStyleClass().add("badge-status-pending");
        } else {
            l.getStyleClass().add("badge-priority-low");
        }
        return l;
    }

    private Label priorityBadge(String priority) {
        Label l = new Label(formatPriorityFr(priority));
        String p = priority != null ? priority : CoachingRequest.PRIORITY_NORMAL;
        l.getStyleClass().add(switch (p) {
            case CoachingRequest.PRIORITY_URGENT -> "badge-priority-high";
            case CoachingRequest.PRIORITY_MEDIUM -> "badge-priority-mid";
            default -> "badge-priority-low";
        });
        return l;
    }

    private static String formatStatusFr(String status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case CoachingRequest.STATUS_PENDING -> "En attente";
            case CoachingRequest.STATUS_ACCEPTED -> "Acceptée";
            case CoachingRequest.STATUS_PAID -> "Payée";
            case CoachingRequest.STATUS_CONFIRMED -> "Confirmée";
            case CoachingRequest.STATUS_COMPLETED -> "Terminée";
            case CoachingRequest.STATUS_CANCELLED -> "Annulée";
            case CoachingRequest.STATUS_DECLINED -> "Refusée";
            default -> status;
        };
    }

    private static String formatPriorityFr(String priority) {
        if (priority == null) {
            return "—";
        }
        return switch (priority) {
            case CoachingRequest.PRIORITY_NORMAL -> "Faible";
            case CoachingRequest.PRIORITY_MEDIUM -> "Moyenne";
            case CoachingRequest.PRIORITY_URGENT -> "Urgente";
            default -> priority;
        };
    }

    private static String initials(User u) {
        if (u == null) {
            return "?";
        }
        String a = u.getFirstName() != null && !u.getFirstName().isBlank()
                ? u.getFirstName().substring(0, 1).toUpperCase(Locale.ROOT) : "";
        String b = u.getLastName() != null && !u.getLastName().isBlank()
                ? u.getLastName().substring(0, 1).toUpperCase(Locale.ROOT) : "";
        String s = a + b;
        return s.isEmpty() ? "?" : s;
    }

    private static String formatFrench(Date d) {
        if (d == null) {
            return "—";
        }
        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy 'à' HH:mm", Locale.FRENCH);
        return fmt.format(d);
    }

    private void onAccept(CoachingRequest cr) {
        try {
            System.out.println("[CoachDashboardController] accept requestId=" + cr.getId() + ", coachId=" + coachId);
            int sessionId = workflow.acceptCoachingRequest(cr.getId(), coachId);
            Session existing = sessionService.findByCoachingRequestId(cr.getId());
            if (existing == null) {
                Session session = new Session();
                session.setCoachingRequestId(cr.getId());
                session.setScheduledAt(new Date());
                session.setDuration(60);
                session.setObjective("Coaching session");
                session.setStatus(Session.STATUS_CONFIRMED);
                sessionService.addSession(session);
                coachingRequests.updateStatus(cr.getId(), CoachingRequest.STATUS_ACCEPTED);
                System.out.println("Session created for coach: " + coachId);
            }
            System.out.println("[CoachDashboardController] session created/linked sessionId=" + sessionId);
            new Alert(Alert.AlertType.INFORMATION,
                    "Demande acceptée. Session #" + sessionId + " créée. Vous pourrez proposer un créneau ensuite.").showAndWait();
            reloadAll();
            openAddSessionForm(cr);
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        } catch (IllegalStateException | IllegalArgumentException ex) {
            new Alert(Alert.AlertType.WARNING, ex.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onGoToSessions() {
        try {
            NavigationManager.show("/user/coaching_session/mes_sessions.fxml", "DayFlow — Mes sessions");
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private VBox buildSessionCard(Session s) throws SQLException {
        VBox card = new VBox(8);
        card.getStyleClass().add("coach-request-card");
        card.setPadding(new Insets(4, 0, 4, 0));

        Optional<CoachingRequest> crOpt = coachingRequests.findById(s.getCoachingRequestId());
        String clientName = "Utilisateur inconnu";
        if (crOpt.isPresent()) {
            Optional<User> user = userService.findById(crOpt.get().getUserId());
            if (user.isPresent()) {
                clientName = (user.get().getFirstName() + " " + user.get().getLastName()).trim();
            } else {
                clientName = "Utilisateur #" + crOpt.get().getUserId();
            }
        }

        Label title = new Label("Session #" + s.getId() + " — " + clientName);
        title.getStyleClass().add("coach-name");
        String when = s.getDisplayTime() == null ? "Date non planifiée" : formatFrench(s.getDisplayTime());
        Label meta = new Label(when + " · " + (s.getDuration() == null ? "-" : s.getDuration() + " min"));
        meta.getStyleClass().add("coach-meta");
        String objective = s.getObjective() == null || s.getObjective().isBlank() ? "Sans description" : s.getObjective();
        Label description = new Label(objective);
        description.getStyleClass().add("coach-msg-box");
        description.setWrapText(true);
        card.getChildren().addAll(title, meta, description);
        return card;
    }

    private void openAddSessionForm(CoachingRequest request) {
        try {
            URL formUrl = getClass().getResource("/user/coaching_session/add_session.fxml");
            if (formUrl == null) {
                new Alert(Alert.AlertType.ERROR,
                        "Fichier FXML introuvable sur le classpath : /user/coaching_session/add_session.fxml\n"
                                + "Recompilez le projet (mvn compile) ou faites Build > Rebuild.").showAndWait();
                return;
            }
            FXMLLoader loader = new FXMLLoader(formUrl);
            Parent root = loader.load();
            AddSessionController controller = loader.getController();
            controller.setRequest(request);
            controller.setOnSaved(this::reloadAll);
            Stage stage = new Stage();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Créer une session");
            stage.setMinWidth(400);
            stage.setMinHeight(480);
            stage.sizeToScene();
            stage.show();
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, formatFxmlLoadFailure(e)).showAndWait();
            e.printStackTrace();
        } catch (RuntimeException e) {
            new Alert(Alert.AlertType.ERROR,
                    "Erreur lors de l'ouverture du formulaire session :\n" + e.getClass().getSimpleName() + ": " + e.getMessage()).showAndWait();
            e.printStackTrace();
        }
    }

    /** Utilisé aussi par {@link CoachRequestsController} pour un message d'erreur FXML lisible. */
    public static String formatFxmlLoadFailure(IOException e) {
        StringBuilder sb = new StringBuilder("Impossible de charger le formulaire (FXML).\n\n");
        sb.append(e.getClass().getSimpleName()).append(": ").append(e.getMessage());
        Throwable c = e.getCause();
        if (c != null) {
            sb.append("\n\nCause : ").append(c.getClass().getSimpleName()).append(": ").append(c.getMessage());
        }
        sb.append("\n\nSi le message mentionne une ligne dans add_session.fxml, corrigez ce fichier ou faites mvn clean compile.");
        return sb.toString();
    }

    private void onDecline(CoachingRequest cr) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText("Refuser cette demande ?");
        confirm.setContentText("Le client sera notifié côté application web (Java : pas de notification envoyée).");
        Optional<javafx.scene.control.ButtonType> r = confirm.showAndWait();
        if (r.isEmpty() || r.get() != javafx.scene.control.ButtonType.OK) {
            return;
        }
        try {
            workflow.declineCoachingRequest(cr.getId(), coachId);
            reloadAll();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        } catch (IllegalStateException | IllegalArgumentException ex) {
            new Alert(Alert.AlertType.WARNING, ex.getMessage()).showAndWait();
        }
    }
}
