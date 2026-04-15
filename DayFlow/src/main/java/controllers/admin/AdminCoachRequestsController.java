package controllers.admin;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.coaching_session.CoachingRequest;
import services.coaching_session.CoachingRequestService;
import services.coaching_session.CoachingRequestService.AdminCoachRequestRow;

import java.sql.SQLException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/**
 * Liste admin des demandes coaching (accepter / refuser / détail).
 */
public class AdminCoachRequestsController {

    private final CoachingRequestService requestService = new CoachingRequestService();

    @FXML
    private VBox requestsRowsBox;

    @FXML
    private void initialize() {
        reload();
    }

    private void reload() {
        try {
            List<AdminCoachRequestRow> rows = requestService.findAllForAdmin();
            requestsRowsBox.getChildren().clear();
            DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            for (AdminCoachRequestRow r : rows) {
                requestsRowsBox.getChildren().add(buildRow(r, df));
            }
        } catch (SQLException ex) {
            new Alert(Alert.AlertType.ERROR, "Demandes : " + ex.getMessage()).showAndWait();
        }
    }

    private HBox buildRow(AdminCoachRequestRow r, DateTimeFormatter df) {
        HBox row = new HBox(12);
        row.setPadding(new Insets(12, 10, 12, 10));
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("admin-request-row");

        String st = r.status() == null ? "" : r.status().toLowerCase().trim();
        if (CoachingRequest.STATUS_PENDING.equalsIgnoreCase(st)) {
            row.setStyle("-fx-background-color:#faf5ff;");
        }

        VBox clientCol = new VBox(8);
        clientCol.setMinWidth(240);
        clientCol.setPrefWidth(260);
        StackPane cAv = avatar(initials(r.clientFirstName(), r.clientLastName()));
        Label cName = new Label(r.clientFullName());
        cName.setStyle("-fx-font-weight:bold;");
        Button voir = new Button("Voir");
        voir.setStyle("-fx-background-color:transparent;-fx-border-color:#93c5fd;-fx-border-radius:8;-fx-text-fill:#2563eb;-fx-cursor:hand;");
        voir.setOnAction(e -> showMessage(r));
        HBox clientLine = new HBox(10, cAv, cName, voir);
        clientLine.setAlignment(Pos.CENTER_LEFT);
        clientCol.getChildren().add(clientLine);

        VBox coachCol = new VBox(8);
        coachCol.setMinWidth(240);
        coachCol.setPrefWidth(260);
        HBox coachTop = new HBox(10);
        coachTop.setAlignment(Pos.CENTER_LEFT);
        coachTop.getChildren().addAll(
                avatar(initials(r.coachFirstName(), r.coachLastName())),
                new VBox(2,
                        styled(r.coachFullName(), "-fx-font-weight:bold;"),
                        styled(specLine(r.coachSpeciality()), "-fx-text-fill:#64748b;-fx-font-size:11px;")
                )
        );
        coachCol.getChildren().add(coachTop);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actions = buildActions(r, df);
        actions.setMinWidth(180);

        row.getChildren().addAll(clientCol, coachCol, spacer, actions);
        return row;
    }

    private static Label styled(String t, String style) {
        Label l = new Label(t);
        l.setStyle(style);
        return l;
    }

    private static String specLine(String spec) {
        if (spec == null || spec.isBlank()) {
            return "—";
        }
        return spec;
    }

    private HBox buildActions(AdminCoachRequestRow r, DateTimeFormatter df) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        String st = r.status() == null ? "" : r.status().toLowerCase().trim();

        if (CoachingRequest.STATUS_PENDING.equalsIgnoreCase(st)) {
            Button accept = new Button("Accepter");
            accept.setStyle("-fx-background-color:#22c55e;-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:6 14;-fx-cursor:hand;");
            accept.setOnAction(e -> updateStatus(r.id(), CoachingRequest.STATUS_ACCEPTED));
            Button reject = new Button("Refuser");
            reject.setStyle("-fx-background-color:#e2e8f0;-fx-text-fill:#334155;-fx-background-radius:8;-fx-padding:6 14;-fx-cursor:hand;");
            reject.setOnAction(ev -> updateStatus(r.id(), CoachingRequest.STATUS_DECLINED));
            box.getChildren().addAll(accept, reject);
            return box;
        }

        Label badge = new Label(labelForStatus(st));
        badge.getStyleClass().addAll("admin-badge", badgeClass(st));
        Label when = new Label(formatDate(r.createdAt(), df));
        when.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:11px;");
        VBox v = new VBox(6, badge, when);
        box.getChildren().add(v);
        return box;
    }

    private void updateStatus(int id, String newStatus) {
        try {
            requestService.updateStatus(id, newStatus);
            reload();
        } catch (SQLException ex) {
            new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
        }
    }

    private static void showMessage(AdminCoachRequestRow r) {
        String msg = r.message() == null || r.message().isBlank() ? "(aucun message)" : r.message();
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }

    private static String labelForStatus(String st) {
        if (CoachingRequest.STATUS_ACCEPTED.equalsIgnoreCase(st)) {
            return "Acceptée";
        }
        if (CoachingRequest.STATUS_DECLINED.equalsIgnoreCase(st)) {
            return "Refusée";
        }
        if (CoachingRequest.STATUS_PAID.equalsIgnoreCase(st)) {
            return "Payée";
        }
        if (CoachingRequest.STATUS_COMPLETED.equalsIgnoreCase(st)) {
            return "Terminée";
        }
        if (CoachingRequest.STATUS_CANCELLED.equalsIgnoreCase(st)) {
            return "Annulée";
        }
        return st == null || st.isBlank() ? "—" : st.replace('_', ' ');
    }

    private static String badgeClass(String st) {
        if (CoachingRequest.STATUS_ACCEPTED.equalsIgnoreCase(st)) {
            return "badge-accepted";
        }
        if (CoachingRequest.STATUS_DECLINED.equalsIgnoreCase(st) || CoachingRequest.STATUS_CANCELLED.equalsIgnoreCase(st)) {
            return "badge-default";
        }
        if (CoachingRequest.STATUS_PENDING.equalsIgnoreCase(st)) {
            return "badge-pending";
        }
        return "badge-default";
    }

    private static StackPane avatar(String initials) {
        String t = initials.isBlank() ? "?" : initials;
        StackPane p = new StackPane(new Label(t));
        p.getStyleClass().add("admin-avatar");
        p.setMinSize(36, 36);
        p.setMaxSize(36, 36);
        return p;
    }

    private static String initials(String first, String last) {
        String a = first != null && !first.isBlank() ? first.substring(0, 1) : "";
        String b = last != null && !last.isBlank() ? last.substring(0, 1) : "";
        return (a + b).toUpperCase();
    }

    private static String formatDate(Date d, DateTimeFormatter df) {
        if (d == null) {
            return "";
        }
        return df.format(d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
    }
}
