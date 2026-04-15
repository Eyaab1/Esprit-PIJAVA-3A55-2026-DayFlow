package controllers.admin;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import services.admin.AdminGoalService;
import services.admin.AdminGoalService.AdminGoalRow;
import services.admin.AdminGoalService.SortOrder;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Administration des objectifs (liste filtrable).
 */
public class AdminGoalsController {

    private static final int LIMIT = 300;

    private final AdminGoalService goalService = new AdminGoalService();

    @FXML
    private TextField ownerNameField;
    @FXML
    private TextField ownerEmailField;
    @FXML
    private ComboBox<StatusOption> statusCombo;
    @FXML
    private ComboBox<SortOption> sortCombo;
    @FXML
    private VBox goalsRowsBox;

    public record StatusOption(String dbValue, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    public record SortOption(SortOrder order, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    @FXML
    private void initialize() {
        statusCombo.setItems(FXCollections.observableArrayList(
                new StatusOption("", "Tous les statuts"),
                new StatusOption("draft", "Brouillon"),
                new StatusOption("active", "Actif"),
                new StatusOption("paused", "En pause"),
                new StatusOption("completed", "Terminé"),
                new StatusOption("failed", "Échoué"),
                new StatusOption("archived", "Archivé")
        ));
        statusCombo.getSelectionModel().selectFirst();

        sortCombo.setItems(FXCollections.observableArrayList(
                new SortOption(SortOrder.NEWEST, "Plus récents"),
                new SortOption(SortOrder.OLDEST, "Plus anciens")
        ));
        sortCombo.getSelectionModel().selectFirst();

        onFilter();
    }

    @FXML
    private void onFilter() {
        String name = ownerNameField != null ? ownerNameField.getText() : "";
        String email = ownerEmailField != null ? ownerEmailField.getText() : "";
        StatusOption st = statusCombo != null && statusCombo.getValue() != null
                ? statusCombo.getValue()
                : new StatusOption("", "");
        SortOrder sort = sortCombo != null && sortCombo.getValue() != null
                ? sortCombo.getValue().order()
                : SortOrder.NEWEST;
        try {
            List<AdminGoalRow> rows = goalService.searchGoals(name, email, st.dbValue(), sort, LIMIT);
            render(rows);
        } catch (SQLException ex) {
            new Alert(Alert.AlertType.ERROR, "Objectifs : " + ex.getMessage()).showAndWait();
        }
    }

    private void render(List<AdminGoalRow> rows) {
        goalsRowsBox.getChildren().clear();
        DateTimeFormatter df = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH);
        for (AdminGoalRow r : rows) {
            goalsRowsBox.getChildren().add(buildRow(r, df));
        }
    }

    private HBox buildRow(AdminGoalRow r, DateTimeFormatter df) {
        VBox titleCol = new VBox(4);
        titleCol.setMinWidth(240);
        titleCol.setPrefWidth(280);
        HBox.setHgrow(titleCol, Priority.SOMETIMES);
        Label title = new Label("🎯 " + (r.title().isBlank() ? "Sans titre" : r.title()));
        title.setStyle("-fx-font-weight:bold;-fx-font-size:14px;");
        title.setWrapText(true);
        String snip = r.descriptionSnippet().isBlank() ? "—" : r.descriptionSnippet();
        Label snippet = new Label(snip);
        snippet.setStyle("-fx-text-fill:#64748b;-fx-font-size:11px;");
        snippet.setWrapText(true);
        titleCol.getChildren().addAll(title, snippet);

        VBox ownerCol = new VBox(6);
        ownerCol.setMinWidth(140);
        ownerCol.setPrefWidth(170);
        String ini = initials(r.ownerFirstName(), r.ownerLastName());
        StackPane av = new StackPane(new Label(ini.isBlank() ? "?" : ini));
        av.getStyleClass().add("admin-avatar");
        av.setMaxSize(36, 36);
        Label on = new Label(r.ownerFullName().isBlank() ? "—" : r.ownerFullName());
        on.setStyle("-fx-font-size:13px;");
        Label em = new Label(r.ownerEmail() == null || r.ownerEmail().isBlank() ? "" : r.ownerEmail());
        em.setStyle("-fx-text-fill:#64748b;-fx-font-size:11px;");
        em.setWrapText(true);
        ownerCol.getChildren().addAll(av, on, em);

        String period = formatPeriod(r, df);
        Label periodLbl = new Label(period);
        periodLbl.setMinWidth(100);
        periodLbl.setWrapText(true);
        periodLbl.setStyle("-fx-font-size:12px;");

        Label prog = new Label(r.progress() + " %");
        prog.setMinWidth(72);

        Label rc = new Label(String.valueOf(r.routineCount()));
        rc.setMinWidth(56);

        Label st = new Label(statusFr(r.status()));
        st.setMinWidth(96);
        st.getStyleClass().addAll("admin-badge", goalStatusBadge(r.status()));

        Button more = new Button("⋯");
        more.setStyle("-fx-background-color:#f1f5f9;-fx-background-radius:8;-fx-cursor:hand;");
        more.setOnAction(e -> showDetail(r));

        HBox row = new HBox(12, titleCol, ownerCol, periodLbl, prog, rc, st, more);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 10, 12, 10));
        row.getStyleClass().add("admin-post-row");
        return row;
    }

    private static String formatPeriod(AdminGoalRow r, DateTimeFormatter df) {
        if (r.startDate() != null && r.endDate() != null) {
            return df.format(r.startDate()) + " →\n" + df.format(r.endDate());
        }
        if (r.startDate() != null) {
            return "Depuis " + df.format(r.startDate());
        }
        return "—";
    }

    private static String statusFr(String raw) {
        if (raw == null || raw.isBlank()) {
            return "—";
        }
        return switch (raw.toLowerCase(Locale.ROOT).trim()) {
            case "draft" -> "Brouillon";
            case "active" -> "Actif";
            case "paused" -> "En pause";
            case "completed" -> "Terminé";
            case "failed" -> "Échoué";
            case "archived" -> "Archivé";
            default -> raw.replace('_', ' ');
        };
    }

    private static String goalStatusBadge(String raw) {
        if (raw == null) {
            return "badge-default";
        }
        return switch (raw.toLowerCase(Locale.ROOT).trim()) {
            case "active" -> "badge-accepted";
            case "completed" -> "badge-published";
            case "draft" -> "badge-draft-post";
            case "paused" -> "badge-pending";
            case "failed" -> "badge-proposed";
            case "archived" -> "badge-default";
            default -> "badge-default";
        };
    }

    private static void showDetail(AdminGoalRow r) {
        String dl = r.deadline() != null ? r.deadline().toString() : "—";
        String pr = r.priority() == null || r.priority().isBlank() ? "—" : r.priority();
        String cr = r.createdAt() != null ? r.createdAt().toString() : "—";
        new Alert(Alert.AlertType.INFORMATION,
                "Objectif #" + r.id()
                        + "\nPriorité : " + pr
                        + "\nÉchéance : " + dl
                        + "\nCréé : " + cr
        ).showAndWait();
    }

    private static String initials(String first, String last) {
        String a = first != null && !first.isBlank() ? first.substring(0, 1) : "";
        String b = last != null && !last.isBlank() ? last.substring(0, 1) : "";
        return (a + b).toUpperCase(Locale.ROOT);
    }
}
