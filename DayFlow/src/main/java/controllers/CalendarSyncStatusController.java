package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for Calendar Sync Status Component
 * Displays sync status for individual sessions
 */
public class CalendarSyncStatusController implements Initializable {

    @FXML private Circle syncIndicator;
    @FXML private Label statusTitle;
    @FXML private Label statusDetails;
    @FXML private Button syncButton;
    @FXML private MenuButton optionsMenu;

    private String sessionId;
    private boolean isSynced = false;
    private String googleCalendarEventId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupEventHandlers();
        updateSyncStatus(false);
    }

    /**
     * Setup event handlers
     */
    private void setupEventHandlers() {
        syncButton.setOnAction(e -> handleSync());
    }

    /**
     * Set session ID for this component
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Set initial sync status
     */
    public void setSyncStatus(boolean synced, String googleEventId) {
        this.isSynced = synced;
        this.googleCalendarEventId = googleEventId;
        updateSyncStatus(synced);
    }

    /**
     * Handle sync button click
     */
    @FXML
    private void handleSync() {
        if (isSynced) {
            showConfirmation("Remove from Google Calendar?", "This session will be removed from your Google Calendar.");
            return;
        }

        syncButton.setDisable(true);
        statusTitle.setText("Syncing...");

        new Thread(() -> {
            try {
                Thread.sleep(1500); // Simulate sync delay

                Platform.runLater(() -> {
                    isSynced = true;
                    googleCalendarEventId = "evt_" + System.currentTimeMillis();
                    updateSyncStatus(true);
                    showNotification("✓ Session synced to Google Calendar!");
                    syncButton.setDisable(false);
                });
            } catch (InterruptedException e) {
                Platform.runLater(() -> {
                    showNotification("✗ Sync failed");
                    syncButton.setDisable(false);
                });
            }
        }).start();
    }

    /**
     * Handle view in Google Calendar
     */
    @FXML
    private void handleViewInCalendar() {
        if (!isSynced || googleCalendarEventId == null) {
            showNotification("⚠ Session not synced yet");
            return;
        }

        try {
            // Open Google Calendar in browser
            String url = "https://calendar.google.com/calendar/u/0/r/eventedit/" + googleCalendarEventId;
            openBrowser(url);
            showNotification("✓ Opening Google Calendar...");
        } catch (Exception e) {
            showNotification("✗ Could not open Google Calendar");
        }
    }

    /**
     * Handle remove from Google Calendar
     */
    @FXML
    private void handleRemoveFromCalendar() {
        if (!isSynced) {
            showNotification("⚠ Session not synced");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Remove from Calendar");
        alert.setHeaderText("Remove Session from Google Calendar?");
        alert.setContentText("This action cannot be undone.");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            syncButton.setDisable(true);
            statusTitle.setText("Removing...");

            new Thread(() -> {
                try {
                    Thread.sleep(1000);

                    Platform.runLater(() -> {
                        isSynced = false;
                        googleCalendarEventId = null;
                        updateSyncStatus(false);
                        showNotification("✓ Session removed from Google Calendar");
                        syncButton.setDisable(false);
                    });
                } catch (InterruptedException e) {
                    Platform.runLater(() -> {
                        showNotification("✗ Failed to remove session");
                        syncButton.setDisable(false);
                    });
                }
            }).start();
        }
    }

    /**
     * Handle sync settings
     */
    @FXML
    private void handleSyncSettings() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Sync Settings");
        alert.setHeaderText("Session Sync Settings");
        alert.setContentText(
            "Session ID: " + sessionId + "\n" +
            "Sync Status: " + (isSynced ? "Synced" : "Not Synced") + "\n" +
            "Google Event ID: " + (googleCalendarEventId != null ? googleCalendarEventId : "N/A") + "\n\n" +
            "This session will be automatically synced to your Google Calendar when you confirm the booking."
        );
        alert.showAndWait();
    }

    /**
     * Update sync status UI
     */
    private void updateSyncStatus(boolean synced) {
        Platform.runLater(() -> {
            if (synced) {
                syncIndicator.setFill(Color.web("#10b981"));
                statusTitle.setText("✓ Synced");
                statusTitle.setStyle("-fx-text-fill: #10b981;");
                statusDetails.setText("Synced to Google Calendar");
                statusDetails.setStyle("-fx-text-fill: #059669;");
                syncButton.setText("📅 Remove");
                syncButton.setStyle("-fx-background-color: #ef4444;");
            } else {
                syncIndicator.setFill(Color.web("#ef4444"));
                statusTitle.setText("Not Synced");
                statusTitle.setStyle("-fx-text-fill: #ef4444;");
                statusDetails.setText("Click to sync with Google Calendar");
                statusDetails.setStyle("-fx-text-fill: #6b7280;");
                syncButton.setText("📅 Sync");
                syncButton.setStyle("-fx-background-color: #3b82f6;");
            }
        });
    }

    /**
     * Show notification
     */
    private void showNotification(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Notification");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Show confirmation dialog
     */
    private void showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Open URL in browser
     */
    private void openBrowser(String url) {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                Runtime.getRuntime().exec("cmd /c start " + url);
            } else if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                Runtime.getRuntime().exec("open " + url);
            } else {
                Runtime.getRuntime().exec("xdg-open " + url);
            }
        } catch (Exception e) {
            System.err.println("Could not open browser: " + e.getMessage());
        }
    }
}
