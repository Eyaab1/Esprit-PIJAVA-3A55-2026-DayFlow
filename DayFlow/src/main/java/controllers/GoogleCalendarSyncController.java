package controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.shape.Circle;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Controller for Google Calendar Sync UI
 * Manages synchronization of coaching sessions with Google Calendar
 */
public class GoogleCalendarSyncController implements Initializable {

    @FXML private Circle statusIndicator;
    @FXML private Label statusLabel;
    @FXML private Button connectButton;
    @FXML private Button refreshButton;
    @FXML private CheckBox autoSyncCheckBox;
    @FXML private CheckBox syncNotificationsCheckBox;
    @FXML private CheckBox bidirectionalCheckBox;
    @FXML private ComboBox<String> syncIntervalCombo;
    @FXML private TableView<SyncHistoryEntry> syncHistoryTable;
    @FXML private ListView<String> eventsListView;
    @FXML private Button testSyncButton;
    @FXML private Button clearCacheButton;
    @FXML private Button settingsButton;
    @FXML private Label messageLabel;

    private boolean isConnected = false;
    private ScheduledExecutorService syncScheduler;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final ObservableList<SyncHistoryEntry> syncHistory = FXCollections.observableArrayList();
    private final ObservableList<String> eventsList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUI();
        setupEventHandlers();
        loadSyncHistory();
        checkConnectionStatus();
    }

    /**
     * Setup initial UI state
     */
    private void setupUI() {
        // Setup sync history table
        syncHistoryTable.setItems(syncHistory);

        // Setup events list view
        eventsListView.setItems(eventsList);

        // Setup sync interval combo box
        syncIntervalCombo.setValue("Real-time");
        syncIntervalCombo.setOnAction(e -> handleSyncIntervalChange());

        // Initial status
        updateConnectionStatus(false);
    }

    /**
     * Setup event handlers for all buttons
     */
    private void setupEventHandlers() {
        connectButton.setOnAction(e -> handleConnectGoogle());
        refreshButton.setOnAction(e -> handleRefresh());
        testSyncButton.setOnAction(e -> handleTestSync());
        clearCacheButton.setOnAction(e -> handleClearCache());
        settingsButton.setOnAction(e -> handleSettings());

        autoSyncCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            handleAutoSyncToggle(newVal);
        });

        syncNotificationsCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            handleNotificationsToggle(newVal);
        });

        bidirectionalCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            handleBidirectionalToggle(newVal);
        });
    }

    /**
     * Handle Google Account Connection
     */
    @FXML
    private void handleConnectGoogle() {
        try {
            showMessage("Connecting to Google Account...", "info");
            connectButton.setDisable(true);

            // Simulate connection process (replace with actual OAuth flow)
            new Thread(() -> {
                try {
                    Thread.sleep(2000); // Simulate connection delay

                    Platform.runLater(() -> {
                        isConnected = true;
                        updateConnectionStatus(true);
                        showMessage("✓ Successfully connected to Google Calendar!", "success");
                        connectButton.setText("🔗 Disconnect");
                        connectButton.setDisable(false);

                        // Load events after connection
                        loadCalendarEvents();
                    });
                } catch (InterruptedException e) {
                    Platform.runLater(() -> {
                        showMessage("✗ Connection failed: " + e.getMessage(), "error");
                        connectButton.setDisable(false);
                    });
                }
            }).start();

        } catch (Exception e) {
            showMessage("✗ Error: " + e.getMessage(), "error");
            connectButton.setDisable(false);
        }
    }

    /**
     * Handle Refresh Button
     */
    @FXML
    private void handleRefresh() {
        if (!isConnected) {
            showMessage("⚠ Not connected to Google Calendar", "warning");
            return;
        }

        refreshButton.setDisable(true);
        showMessage("Refreshing calendar events...", "info");

        new Thread(() -> {
            try {
                Thread.sleep(1500); // Simulate refresh delay

                Platform.runLater(() -> {
                    loadCalendarEvents();
                    addSyncHistoryEntry("Manual Refresh", "Success", "Refreshed calendar events");
                    showMessage("✓ Calendar refreshed successfully!", "success");
                    refreshButton.setDisable(false);
                });
            } catch (InterruptedException e) {
                Platform.runLater(() -> {
                    showMessage("✗ Refresh failed", "error");
                    refreshButton.setDisable(false);
                });
            }
        }).start();
    }

    /**
     * Handle Test Sync
     */
    @FXML
    private void handleTestSync() {
        if (!isConnected) {
            showMessage("⚠ Not connected to Google Calendar", "warning");
            return;
        }

        testSyncButton.setDisable(true);
        showMessage("Testing sync with a sample session...", "info");

        new Thread(() -> {
            try {
                Thread.sleep(2000); // Simulate sync delay

                Platform.runLater(() -> {
                    addSyncHistoryEntry("Test Sync", "Success", "Sample session synced to Google Calendar");
                    eventsList.add("📅 Test Session - 2026-05-10 14:00 (synced)");
                    showMessage("✓ Test sync completed successfully!", "success");
                    testSyncButton.setDisable(false);
                });
            } catch (InterruptedException e) {
                Platform.runLater(() -> {
                    showMessage("✗ Test sync failed", "error");
                    testSyncButton.setDisable(false);
                });
            }
        }).start();
    }

    /**
     * Handle Clear Cache
     */
    @FXML
    private void handleClearCache() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Clear Cache");
        alert.setHeaderText("Clear Sync Cache?");
        alert.setContentText("This will clear all cached calendar data. Continue?");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            eventsList.clear();
            syncHistory.clear();
            addSyncHistoryEntry("Cache Clear", "Success", "Sync cache cleared");
            showMessage("✓ Cache cleared successfully!", "success");
        }
    }

    /**
     * Handle Settings Button
     */
    @FXML
    private void handleSettings() {
        Alert settingsAlert = new Alert(Alert.AlertType.INFORMATION);
        settingsAlert.setTitle("Google Calendar Settings");
        settingsAlert.setHeaderText("Sync Settings");
        settingsAlert.setContentText(
            "Auto-sync: " + autoSyncCheckBox.isSelected() + "\n" +
            "Notifications: " + syncNotificationsCheckBox.isSelected() + "\n" +
            "Bidirectional: " + bidirectionalCheckBox.isSelected() + "\n" +
            "Sync Interval: " + syncIntervalCombo.getValue()
        );
        settingsAlert.showAndWait();
    }

    /**
     * Handle Auto-sync Toggle
     */
    private void handleAutoSyncToggle(boolean enabled) {
        if (enabled) {
            startAutoSync();
            showMessage("✓ Auto-sync enabled", "success");
        } else {
            stopAutoSync();
            showMessage("Auto-sync disabled", "info");
        }
    }

    /**
     * Handle Notifications Toggle
     */
    private void handleNotificationsToggle(boolean enabled) {
        if (enabled) {
            showMessage("✓ Notifications enabled", "success");
        } else {
            showMessage("Notifications disabled", "info");
        }
    }

    /**
     * Handle Bidirectional Sync Toggle
     */
    private void handleBidirectionalToggle(boolean enabled) {
        if (enabled) {
            showMessage("⚠ Bidirectional sync enabled (experimental)", "warning");
        } else {
            showMessage("Bidirectional sync disabled", "info");
        }
    }

    /**
     * Handle Sync Interval Change
     */
    private void handleSyncIntervalChange() {
        String interval = syncIntervalCombo.getValue();
        showMessage("Sync interval changed to: " + interval, "info");

        if (autoSyncCheckBox.isSelected()) {
            stopAutoSync();
            startAutoSync();
        }
    }

    /**
     * Start Auto-sync with configured interval
     */
    private void startAutoSync() {
        if (syncScheduler != null && !syncScheduler.isShutdown()) {
            return;
        }

        syncScheduler = Executors.newScheduledThreadPool(1);
        String interval = syncIntervalCombo.getValue();

        long delay = getIntervalDelay(interval);

        if (delay > 0) {
            syncScheduler.scheduleAtFixedRate(
                this::performSync,
                delay,
                delay,
                TimeUnit.SECONDS
            );
        }
    }

    /**
     * Stop Auto-sync
     */
    private void stopAutoSync() {
        if (syncScheduler != null && !syncScheduler.isShutdown()) {
            syncScheduler.shutdown();
        }
    }

    /**
     * Get interval delay in seconds
     */
    private long getIntervalDelay(String interval) {
        return switch (interval) {
            case "Real-time" -> 30; // 30 seconds
            case "Every 5 minutes" -> 300;
            case "Every 15 minutes" -> 900;
            case "Every hour" -> 3600;
            case "Manual only" -> 0;
            default -> 0;
        };
    }

    /**
     * Perform sync operation
     */
    private void performSync() {
        if (!isConnected) {
            return;
        }

        try {
            // Simulate sync operation
            Platform.runLater(() -> {
                statusIndicator.getStyleClass().add("syncing");
                statusLabel.setText("Syncing...");
            });

            Thread.sleep(1000);

            Platform.runLater(() -> {
                statusIndicator.getStyleClass().remove("syncing");
                statusLabel.setText("Connected");
                addSyncHistoryEntry("Auto Sync", "Success", "Sessions synced to Google Calendar");
            });

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Check current connection status
     */
    private void checkConnectionStatus() {
        // Check if already connected (placeholder)
        updateConnectionStatus(false);
    }

    /**
     * Update connection status UI
     */
    private void updateConnectionStatus(boolean connected) {
        Platform.runLater(() -> {
            if (connected) {
                statusIndicator.setFill(javafx.scene.paint.Color.web("#10b981"));
                statusIndicator.getStyleClass().add("connected");
                statusLabel.setText("Connected");
                statusLabel.setStyle("-fx-text-fill: #10b981;");
                connectButton.setText("🔗 Disconnect");
                refreshButton.setDisable(false);
                testSyncButton.setDisable(false);
            } else {
                statusIndicator.setFill(javafx.scene.paint.Color.web("#ef4444"));
                statusIndicator.getStyleClass().remove("connected");
                statusLabel.setText("Not Connected");
                statusLabel.setStyle("-fx-text-fill: #ef4444;");
                connectButton.setText("🔗 Connect Google Account");
                refreshButton.setDisable(true);
                testSyncButton.setDisable(true);
            }
        });
    }

    /**
     * Load calendar events from Google Calendar
     */
    private void loadCalendarEvents() {
        eventsList.clear();
        eventsList.addAll(
            "📅 Coaching Session - 2026-05-10 14:00 (synced)",
            "📅 Follow-up Session - 2026-05-12 15:30 (synced)",
            "📅 Progress Review - 2026-05-15 10:00 (synced)",
            "📅 Goal Planning - 2026-05-17 11:00 (synced)"
        );
    }

    /**
     * Load sync history from storage
     */
    private void loadSyncHistory() {
        syncHistory.addAll(
            new SyncHistoryEntry("2026-05-05 10:30", "Initial Sync", "Success", "3 sessions synced"),
            new SyncHistoryEntry("2026-05-05 11:00", "Auto Sync", "Success", "No changes"),
            new SyncHistoryEntry("2026-05-05 11:30", "Auto Sync", "Success", "1 session updated")
        );
    }

    /**
     * Add entry to sync history
     */
    private void addSyncHistoryEntry(String action, String status, String details) {
        String timestamp = LocalDateTime.now().format(dateFormatter);
        syncHistory.add(0, new SyncHistoryEntry(timestamp, action, status, details));

        // Keep only last 50 entries
        if (syncHistory.size() > 50) {
            syncHistory.remove(syncHistory.size() - 1);
        }
    }

    /**
     * Show message to user
     */
    private void showMessage(String message, String type) {
        Platform.runLater(() -> {
            messageLabel.setText(message);
            messageLabel.getStyleClass().clear();
            messageLabel.getStyleClass().add(type);
        });
    }

    /**
     * Cleanup on close
     */
    public void cleanup() {
        stopAutoSync();
    }

    /**
     * Inner class for sync history entries
     */
    public static class SyncHistoryEntry {
        private final String timestamp;
        private final String action;
        private final String status;
        private final String details;

        public SyncHistoryEntry(String timestamp, String action, String status, String details) {
            this.timestamp = timestamp;
            this.action = action;
            this.status = status;
            this.details = details;
        }

        public String getTimestamp() { return timestamp; }
        public String getAction() { return action; }
        public String getStatus() { return status; }
        public String getDetails() { return details; }
    }
}
