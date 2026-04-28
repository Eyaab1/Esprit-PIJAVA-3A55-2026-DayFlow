package controllers.admin;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import model.user.User;
import services.account.UserService;
import services.admin.AdminStatsService;
import services.admin.AdminStatsService.UserCardRow;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class AdminUsersController {

    private static final int SEARCH_LIMIT = 200;

    private final AdminStatsService statsService = new AdminStatsService();
    private final UserService userService = new UserService();

    @FXML private TextField nameFilterField;
    @FXML private TextField emailFilterField;
    @FXML private ToggleButton gridToggleBtn;
    @FXML private ToggleButton listToggleBtn;
    @FXML private ToggleGroup viewModeGroup;
    @FXML private FlowPane usersFlowPane;
    @FXML private VBox usersListPane;

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

    // ── Card (grid view) ──────────────────────────────────────────────────────

    private VBox buildUserCard(UserCardRow r) {
        VBox card = new VBox(8);
        card.getStyleClass().add("admin-user-card");

        // Avatar + name row
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label avatar = new Label(initials(r.firstName(), r.lastName()));
        avatar.getStyleClass().add("admin-avatar");
        avatar.setMinSize(38, 38);
        avatar.setMaxSize(38, 38);
        avatar.setAlignment(Pos.CENTER);

        VBox nameBox = new VBox(2);
        String fullName = (r.firstName() + " " + r.lastName()).trim();
        Label name = new Label(fullName.isBlank() ? "Sans nom" : fullName);
        name.setStyle("-fx-font-weight:bold;-fx-font-size:14px;");
        Label emailLbl = new Label(r.email());
        emailLbl.setStyle("-fx-text-fill:#6b7280;-fx-font-size:11px;");
        nameBox.getChildren().addAll(name, emailLbl);
        topRow.getChildren().addAll(avatar, nameBox);

        // Role badge + status badge
        HBox badges = new HBox(6);
        badges.setAlignment(Pos.CENTER_LEFT);
        Label roleBadge = new Label(r.coach() ? "Coach" : "Utilisateur");
        roleBadge.getStyleClass().addAll("admin-badge", r.coach() ? "badge-coach" : "badge-user");
        Label statusBadge = buildStatusBadge(r.status());
        badges.getChildren().addAll(roleBadge, statusBadge);

        // Stats
        VBox stats = new VBox(4);
        stats.setStyle("-fx-font-size:12px;-fx-text-fill:#374151;");
        if (r.coach()) {
            stats.getChildren().add(new Label("📅 Sessions : " + r.coachSessionsCount()));
            stats.getChildren().add(new Label("📋 Demandes : " + r.coachRequestsCount()));
            if (r.rating() != null && r.rating() > 0) {
                stats.getChildren().add(new Label(starsLabel(r.rating())));
            }
        } else {
            stats.getChildren().add(new Label("📋 Routines : " + r.clientRoutinesCount()));
        }

        Region grow = new Region();
        VBox.setVgrow(grow, Priority.ALWAYS);

        // Action buttons
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button editBtn = new Button("✏ Modifier");
        editBtn.getStyleClass().add("admin-action-btn-edit");
        editBtn.setOnAction(e -> openEditDialog(r));

        boolean blocked = isBlocked(r.status());
        Button blockBtn = new Button(blocked ? "🔓 Débloquer" : "🚫 Bloquer");
        blockBtn.getStyleClass().add(blocked ? "admin-action-btn-unblock" : "admin-action-btn-block");
        blockBtn.setOnAction(e -> toggleBlock(r, blocked));

        actions.getChildren().addAll(editBtn, blockBtn);
        card.getChildren().addAll(topRow, badges, stats, grow, actions);
        return card;
    }

    // ── List row (list view) ──────────────────────────────────────────────────

    private HBox buildUserListRow(UserCardRow r) {
        // Avatar
        Label avatar = new Label(initials(r.firstName(), r.lastName()));
        avatar.getStyleClass().add("admin-avatar");
        avatar.setMinSize(34, 34);
        avatar.setMaxSize(34, 34);
        avatar.setAlignment(Pos.CENTER);

        // Name + email
        String fullName = (r.firstName() + " " + r.lastName()).trim();
        Label name = new Label(fullName.isBlank() ? "Sans nom" : fullName);
        name.setStyle("-fx-font-weight:bold;-fx-font-size:13px;");
        Label email = new Label(r.email());
        email.setStyle("-fx-text-fill:#64748b;-fx-font-size:11px;");
        VBox left = new VBox(2, name, email);

        // Role + status
        Label roleBadge = new Label(r.coach() ? "Coach" : "Utilisateur");
        roleBadge.getStyleClass().addAll("admin-badge", r.coach() ? "badge-coach" : "badge-user");
        Label statusBadge = buildStatusBadge(r.status());
        VBox middle = new VBox(4, roleBadge, statusBadge);
        middle.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Actions
        Button editBtn = new Button("✏ Modifier");
        editBtn.getStyleClass().add("admin-action-btn-edit");
        editBtn.setOnAction(e -> openEditDialog(r));

        boolean blocked = isBlocked(r.status());
        Button blockBtn = new Button(blocked ? "🔓 Débloquer" : "🚫 Bloquer");
        blockBtn.getStyleClass().add(blocked ? "admin-action-btn-unblock" : "admin-action-btn-block");
        blockBtn.setOnAction(e -> toggleBlock(r, blocked));

        HBox row = new HBox(12, avatar, left, spacer, middle, editBtn, blockBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 12, 10, 12));
        row.getStyleClass().add("admin-list-row");
        return row;
    }

    // ── Edit dialog ───────────────────────────────────────────────────────────

    private void openEditDialog(UserCardRow r) {
        try {
            Optional<User> opt = userService.findById(r.id());
            if (opt.isEmpty()) {
                new Alert(Alert.AlertType.ERROR, "Utilisateur introuvable.").showAndWait();
                return;
            }
            User user = opt.get();

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Modifier l'utilisateur");
            dialog.setHeaderText((r.firstName() + " " + r.lastName()).trim());

            ButtonType saveType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

            GridPane grid = new GridPane();
            grid.setHgap(12);
            grid.setVgap(10);
            grid.setPadding(new Insets(20));

            TextField firstNameField = new TextField(safe(user.getFirstName()));
            TextField lastNameField  = new TextField(safe(user.getLastName()));
            TextField emailField     = new TextField(safe(user.getEmail()));
            TextField phoneField     = new TextField(safe(user.getPhoneNumber()));
            TextField bioField       = new TextField(safe(user.getBio()));

            grid.add(new Label("Prénom :"),    0, 0); grid.add(firstNameField, 1, 0);
            grid.add(new Label("Nom :"),       0, 1); grid.add(lastNameField,  1, 1);
            grid.add(new Label("Email :"),     0, 2); grid.add(emailField,     1, 2);
            grid.add(new Label("Téléphone :"), 0, 3); grid.add(phoneField,     1, 3);
            grid.add(new Label("Bio :"),       0, 4); grid.add(bioField,       1, 4);

            // Make fields wider
            firstNameField.setPrefWidth(260);
            lastNameField.setPrefWidth(260);
            emailField.setPrefWidth(260);
            phoneField.setPrefWidth(260);
            bioField.setPrefWidth(260);

            dialog.getDialogPane().setContent(grid);
            dialog.getDialogPane().setPrefWidth(460);

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == saveType) {
                user.setFirstName(firstNameField.getText().trim());
                user.setLastName(lastNameField.getText().trim());
                user.setEmail(emailField.getText().trim());
                user.setPhoneNumber(phoneField.getText().trim());
                user.setBio(bioField.getText().trim());
                userService.update(user);
                onFilter(); // refresh list
                new Alert(Alert.AlertType.INFORMATION, "Utilisateur mis à jour avec succès.").showAndWait();
            }
        } catch (SQLException ex) {
            new Alert(Alert.AlertType.ERROR, "Erreur : " + ex.getMessage()).showAndWait();
        }
    }

    // ── Block / Unblock ───────────────────────────────────────────────────────

    private void toggleBlock(UserCardRow r, boolean currentlyBlocked) {
        String action = currentlyBlocked ? "débloquer" : "bloquer";
        String fullName = (r.firstName() + " " + r.lastName()).trim();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Voulez-vous vraiment " + action + " l'utilisateur " + fullName + " ?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.YES) return;

        try {
            String newStatus = currentlyBlocked ? "active" : "banned";
            userService.updateModerationStatus(r.id(), newStatus, null, currentlyBlocked ? null : "Bloqué par l'administrateur");
            onFilter();
        } catch (SQLException ex) {
            new Alert(Alert.AlertType.ERROR, "Erreur : " + ex.getMessage()).showAndWait();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static boolean isBlocked(String status) {
        if (status == null) return false;
        String s = status.toLowerCase();
        return s.equals("banned") || s.equals("permanent_banned") || s.equals("temp_banned");
    }

    private static Label buildStatusBadge(String status) {
        String s = status == null ? "" : status.toLowerCase();
        String text;
        String style;
        switch (s) {
            case "active"           -> { text = "● Actif";    style = "badge-status-active"; }
            case "banned",
                 "permanent_banned" -> { text = "⛔ Banni";   style = "badge-status-banned"; }
            case "temp_banned"      -> { text = "⏸ Suspendu"; style = "badge-status-temp"; }
            default                 -> { text = "○ " + (s.isBlank() ? "Inconnu" : status); style = "badge-default"; }
        }
        Label badge = new Label(text);
        badge.getStyleClass().addAll("admin-badge", style);
        return badge;
    }

    private static String initials(String first, String last) {
        String a = (first != null && !first.isBlank()) ? first.substring(0, 1).toUpperCase() : "";
        String b = (last  != null && !last.isBlank())  ? last.substring(0, 1).toUpperCase()  : "";
        String s = a + b;
        return s.isBlank() ? "?" : s;
    }

    private static String starsLabel(double rating) {
        int filled = (int) Math.round(Math.clamp(rating, 0, 5));
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) sb.append(i <= filled ? "★" : "☆");
        sb.append("  (").append(String.format("%.1f", rating)).append(")");
        return sb.toString();
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
