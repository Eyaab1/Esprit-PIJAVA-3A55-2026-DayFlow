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
 * Grille / liste des coachs uniquement.
 */
public class AdminCoachesController {

    private static final int LIMIT = 200;

    private final AdminStatsService statsService = new AdminStatsService();

    @FXML
    private TextField searchField;
    @FXML
    private ToggleButton gridToggleBtn;
    @FXML
    private ToggleButton listToggleBtn;
    @FXML
    private ToggleGroup viewModeGroup;
    @FXML
    private FlowPane coachesFlowPane;
    @FXML
    private VBox coachesListPane;

    @FXML
    private void initialize() {
        if (viewModeGroup != null && gridToggleBtn != null) {
            viewModeGroup.selectToggle(gridToggleBtn);
        }
        applyViewMode();
        onSearch();
    }

    @FXML
    private void onSearch() {
        String q = searchField != null ? searchField.getText() : "";
        try {
            List<UserCardRow> rows = statsService.searchCoaches(q, LIMIT);
            render(rows);
        } catch (SQLException ex) {
            new Alert(Alert.AlertType.ERROR, "Coachs : " + ex.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onViewModeChanged() {
        applyViewMode();
    }

    private void applyViewMode() {
        boolean grid = gridToggleBtn == null || gridToggleBtn.isSelected();
        coachesFlowPane.setVisible(grid);
        coachesFlowPane.setManaged(grid);
        coachesListPane.setVisible(!grid);
        coachesListPane.setManaged(!grid);
    }

    private void render(List<UserCardRow> rows) {
        coachesFlowPane.getChildren().clear();
        coachesListPane.getChildren().clear();
        for (UserCardRow r : rows) {
            coachesFlowPane.getChildren().add(buildCoachCard(r));
            coachesListPane.getChildren().add(buildCoachListRow(r));
        }
    }

    private VBox buildCoachCard(UserCardRow r) {
        VBox card = new VBox(10);
        card.getStyleClass().add("admin-user-card");
        card.setPadding(new Insets(4, 0, 4, 0));

        String fullName = (r.firstName() + " " + r.lastName()).trim();
        Label name = new Label(fullName.isBlank() ? "Sans nom" : fullName);
        name.setStyle("-fx-font-weight:bold;-fx-font-size:15px;");
        String spec = r.speciality() == null || r.speciality().isBlank() ? "" : r.speciality();
        Label role = new Label(spec.isBlank() ? "Coach" : "Coach : " + spec);
        role.setStyle("-fx-text-fill:#7c3aed;-fx-font-size:12px;");

        VBox stats = new VBox(6);
        stats.getChildren().add(new Label("Sessions coaching : " + r.coachSessionsCount()));
        stats.getChildren().add(new Label("Demandes : " + r.coachRequestsCount()));
        if (r.rating() != null && r.rating() > 0) {
            stats.getChildren().add(new Label(starsLabel(r.rating())));
        }

        Region grow = new Region();
        VBox.setVgrow(grow, Priority.ALWAYS);
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        Label mail = new Label("✉");
        mail.setStyle("-fx-text-fill:#7c3aed;-fx-font-size:18px;");
        footer.getChildren().add(mail);

        card.getChildren().addAll(name, role, stats, grow, footer);
        return card;
    }

    private HBox buildCoachListRow(UserCardRow r) {
        String fullName = (r.firstName() + " " + r.lastName()).trim();
        Label name = new Label(fullName.isBlank() ? "Sans nom" : fullName);
        name.setStyle("-fx-font-weight:bold;");
        Label email = new Label(r.email());
        email.setStyle("-fx-text-fill:#64748b;-fx-font-size:12px;");
        VBox left = new VBox(2, name, email);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label meta = new Label(r.coachSessionsCount() + " sess. · " + r.coachRequestsCount() + " demandes");
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
