package controllers.admin;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import services.admin.AdminStatsService;
import services.admin.AdminStatsService.UserCardRow;

import java.sql.SQLException;
import java.util.List;

/**
 * Liste / grille des utilisateurs avec filtres nom et e-mail.
 */
public class AdminUsersController {

    private static final int SEARCH_LIMIT = 200;

    private final AdminStatsService statsService = new AdminStatsService();
    @FXML
    private TextField nameFilterField;
    @FXML
    private TextField emailFilterField;
    @FXML
    private ToggleButton gridToggleBtn;
    @FXML
    private ToggleButton listToggleBtn;
    @FXML
    private ToggleGroup viewModeGroup;
    @FXML
    private FlowPane usersFlowPane;
    @FXML
    private VBox usersListPane;

    @FXML
    private void initialize() {
        if (viewModeGroup != null && gridToggleBtn != null) {
            viewModeGroup.selectToggle(gridToggleBtn);
        }
        applyViewMode();
        onFilter();
    }

    @FXML
    private void onFilter() {
        String n = nameFilterField != null ? nameFilterField.getText() : "";
        String e = emailFilterField != null ? emailFilterField.getText() : "";
        try {
            List<UserCardRow> rows = statsService.searchUsers(n, e, SEARCH_LIMIT);
            renderUsers(rows);
        } catch (SQLException ex) {
            new Alert(Alert.AlertType.ERROR, "Recherche utilisateurs : " + ex.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onViewModeChanged() {
        applyViewMode();
    }

    private void applyViewMode() {
        boolean grid = gridToggleBtn == null || gridToggleBtn.isSelected();
        usersFlowPane.setVisible(grid);
        usersFlowPane.setManaged(grid);
        usersListPane.setVisible(!grid);
        usersListPane.setManaged(!grid);
    }

    private void renderUsers(List<UserCardRow> rows) {
        usersFlowPane.getChildren().clear();
        usersListPane.getChildren().clear();
        for (UserCardRow r : rows) {
            usersFlowPane.getChildren().add(buildUserCard(r));
            usersListPane.getChildren().add(buildUserListRow(r));
        }
    }

    private VBox buildUserCard(UserCardRow r) {
        VBox card = new VBox(10);
        card.getStyleClass().add("admin-user-card");
        card.setPadding(new Insets(4, 0, 4, 0));

        String fullName = (r.firstName() + " " + r.lastName()).trim();
        Label name = new Label(fullName.isBlank() ? "Sans nom" : fullName);
        name.setStyle("-fx-font-weight:bold;-fx-font-size:15px;");

        String roleLine = r.coach()
                ? ("Coach" + (r.speciality() != null && !r.speciality().isBlank() ? " : " + r.speciality() : ""))
                : "Utilisateur : actif";
        Label role = new Label(roleLine);
        role.setStyle("-fx-text-fill:#7c3aed;-fx-font-size:12px;");

        VBox stats = new VBox(6);
        if (r.coach()) {
            stats.getChildren().add(new Label("Sessions coaching : " + r.coachSessionsCount()));
            stats.getChildren().add(new Label("Demandes (côté coach) : " + r.coachRequestsCount()));
            if (r.rating() != null && r.rating() > 0) {
                stats.getChildren().add(new Label(starsLabel(r.rating())));
            }
        } else {
            stats.getChildren().add(new Label("Jours de série : —"));
            stats.getChildren().add(new Label("Routines : " + r.clientRoutinesCount()));
        }

        Region grow = new Region();
        VBox.setVgrow(grow, Priority.ALWAYS);

        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        Label mail = new Label("✉");
        mail.setStyle("-fx-text-fill:#7c3aed;-fx-font-size:18px;cursor:default;");
        footer.getChildren().add(mail);

        card.getChildren().addAll(name, role, stats, grow, footer);
        return card;
    }

    private HBox buildUserListRow(UserCardRow r) {
        String fullName = (r.firstName() + " " + r.lastName()).trim();
        Label name = new Label(fullName.isBlank() ? "Sans nom" : fullName);
        name.setStyle("-fx-font-weight:bold;");
        Label email = new Label(r.email());
        email.setStyle("-fx-text-fill:#64748b;-fx-font-size:12px;");
        VBox left = new VBox(2, name, email);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        String sub = r.coach()
                ? ("Coach · " + r.coachSessionsCount() + " sess. · " + r.coachRequestsCount() + " demandes")
                : ("Routines : " + r.clientRoutinesCount());
        Label meta = new Label(sub);
        meta.setStyle("-fx-font-size:12px;");
        HBox row = new HBox(16, left, spacer, meta);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 8, 10, 8));
        row.getStyleClass().add("admin-list-row");
        return row;
    }

    private static String starsLabel(double rating) {
        int filled = (int) Math.round(Math.clamp(rating, 0, 5));
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            sb.append(i <= filled ? "★" : "☆");
        }
        sb.append("  (").append(String.format("%.1f", rating)).append(")");
        return sb.toString();
    }
}
