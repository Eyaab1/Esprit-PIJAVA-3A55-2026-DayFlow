# 📚 Google Calendar Frontend - Integration Examples

## Example 1: Add to Coach Dashboard

### Scenario
Add a "Google Calendar Sync" button to the coach dashboard that opens the sync interface.

### Implementation

**In CoachDashboardController.java:**

```java
package controllers.coach;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import controllers.GoogleCalendarSyncController;
import java.io.IOException;

public class CoachDashboardController {
    
    @FXML
    private Button googleCalendarButton;
    
    @FXML
    public void initialize() {
        googleCalendarButton.setOnAction(e -> openGoogleCalendarSync());
    }
    
    @FXML
    private void openGoogleCalendarSync() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/user/coaching_session/google_calendar_sync.fxml")
            );
            Parent root = loader.load();
            GoogleCalendarSyncController controller = loader.getController();
            
            Stage stage = new Stage();
            stage.setTitle("Google Calendar Sync");
            stage.setScene(new Scene(root, 900, 700));
            stage.setOnCloseRequest(e -> controller.cleanup());
            stage.show();
            
        } catch (IOException e) {
            System.err.println("Error loading Google Calendar Sync: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

**In coach_dashboard.fxml:**

```xml
<Button fx:id="googleCalendarButton" 
        text="📅 Google Calendar Sync" 
        style="-fx-padding: 10 20; -fx-font-size: 14;"/>
```

---

## Example 2: Add Status Component to Session List

### Scenario
Display sync status for each session in a list view with quick sync buttons.

### Implementation

**In CoachSessionsController.java:**

```java
package controllers.coach;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import controllers.CalendarSyncStatusController;
import model.coaching_session.Session;
import java.io.IOException;

public class CoachSessionsController {
    
    @FXML
    private ListView<Session> sessionsListView;
    
    @FXML
    public void initialize() {
        setupSessionsList();
    }
    
    private void setupSessionsList() {
        sessionsListView.setCellFactory(param -> new SessionListCell());
    }
    
    /**
     * Custom ListCell that includes sync status component
     */
    private class SessionListCell extends ListCell<Session> {
        private VBox container;
        private HBox syncStatusComponent;
        private CalendarSyncStatusController syncController;
        
        @Override
        protected void updateItem(Session session, boolean empty) {
            super.updateItem(session, empty);
            
            if (empty || session == null) {
                setGraphic(null);
                return;
            }
            
            try {
                // Create session details
                container = new VBox(10);
                container.setStyle("-fx-padding: 10; -fx-border-color: #e5e7eb; -fx-border-radius: 6;");
                
                // Session info
                Label sessionInfo = new Label(
                    "Coach: " + session.getCoachName() + 
                    " | Date: " + session.getDate() + 
                    " | Time: " + session.getStartTime()
                );
                sessionInfo.setStyle("-fx-font-size: 12; -fx-font-weight: bold;");
                
                // Load sync status component
                FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/user/coaching_session/calendar_sync_status.fxml")
                );
                syncStatusComponent = loader.load();
                syncController = loader.getController();
                
                // Configure sync status
                syncController.setSessionId(session.getId());
                syncController.setSyncStatus(
                    session.getGoogleCalendarEventId() != null,
                    session.getGoogleCalendarEventId()
                );
                
                // Add to container
                container.getChildren().addAll(sessionInfo, syncStatusComponent);
                setGraphic(container);
                
            } catch (IOException e) {
                System.err.println("Error loading sync status: " + e.getMessage());
            }
        }
    }
}
```

---

## Example 3: Auto-sync on Session Creation

### Scenario
Automatically sync a new session to Google Calendar when it's created.

### Implementation

**In AddSessionController.java:**

```java
package controllers.coach;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Alert;
import services.calendar.CalendarService;
import model.coaching_session.Session;

public class AddSessionController {
    
    @FXML
    private CheckBox autoSyncCheckBox;
    
    @FXML
    private void handleConfirmSession() {
        try {
            // Create session in database
            Session session = createSessionFromForm();
            sessionService.saveSession(session);
            
            // Auto-sync to Google Calendar if enabled
            if (autoSyncCheckBox.isSelected()) {
                syncSessionToGoogle(session);
            }
            
            showSuccess("✓ Session created successfully!");
            closeDialog();
            
        } catch (Exception e) {
            showError("Error creating session: " + e.getMessage());
        }
    }
    
    /**
     * Sync session to Google Calendar asynchronously
     */
    private void syncSessionToGoogle(Session session) {
        new Thread(() -> {
            try {
                CalendarService calendarService = new CalendarService();
                
                // Check if connected to Google Calendar
                if (!calendarService.isAuthenticated()) {
                    Platform.runLater(() -> {
                        showWarning("Google Calendar not connected. Session created but not synced.");
                    });
                    return;
                }
                
                // Sync session
                String googleEventId = calendarService.syncSessionToGoogle(session);
                
                // Update session with Google event ID
                session.setGoogleCalendarEventId(googleEventId);
                sessionService.updateSession(session);
                
                Platform.runLater(() -> {
                    showSuccess("✓ Session synced to Google Calendar!");
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showWarning("Session created but sync failed: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private Session createSessionFromForm() {
        // Implementation to create session from form fields
        return new Session();
    }
    
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void closeDialog() {
        // Close the dialog
    }
}
```

**In add_session.fxml:**

```xml
<CheckBox fx:id="autoSyncCheckBox" 
          text="✓ Auto-sync to Google Calendar" 
          selected="true"
          style="-fx-font-size: 12;"/>
```

---

## Example 4: Sync on Session Confirmation

### Scenario
When a user confirms a coaching session booking, automatically sync it to their Google Calendar.

### Implementation

**In MesDemandesController.java:**

```java
package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import services.calendar.CalendarService;
import services.coaching_session_module.SessionService;
import model.coaching_session.Session;

public class MesDemandesController {
    
    private CalendarService calendarService;
    private SessionService sessionService;
    
    @FXML
    public void initialize() {
        calendarService = new CalendarService();
        sessionService = new SessionService();
    }
    
    /**
     * Handle session confirmation with Google Calendar sync
     */
    @FXML
    private void handleConfirmSession(Session session) {
        try {
            // Update session status
            session.setStatut("confirmée");
            sessionService.updateSession(session);
            
            // Sync to Google Calendar
            syncToGoogleCalendar(session);
            
            showSuccess("✓ Session confirmed!");
            refreshSessionsList();
            
        } catch (Exception e) {
            showError("Error confirming session: " + e.getMessage());
        }
    }
    
    /**
     * Sync session to Google Calendar with error handling
     */
    private void syncToGoogleCalendar(Session session) {
        new Thread(() -> {
            try {
                // Check authentication
                if (!calendarService.isAuthenticated()) {
                    Platform.runLater(() -> {
                        showInfo("Session confirmed. Connect Google Calendar to sync.");
                    });
                    return;
                }
                
                // Perform sync
                String googleEventId = calendarService.syncSessionToGoogle(session);
                
                // Update session
                session.setGoogleCalendarEventId(googleEventId);
                sessionService.updateSession(session);
                
                Platform.runLater(() -> {
                    showSuccess("✓ Session synced to Google Calendar!");
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    System.err.println("Sync error: " + e.getMessage());
                    // Don't show error to user - session is already confirmed
                });
            }
        }).start();
    }
    
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void refreshSessionsList() {
        // Refresh the sessions list
    }
}
```

---

## Example 5: Batch Sync Multiple Sessions

### Scenario
Sync all pending sessions to Google Calendar at once.

### Implementation

**In GoogleCalendarSyncController.java:**

```java
/**
 * Batch sync all pending sessions
 */
@FXML
private void handleBatchSync() {
    if (!isConnected) {
        showMessage("⚠ Not connected to Google Calendar", "warning");
        return;
    }
    
    testSyncButton.setDisable(true);
    showMessage("Syncing all pending sessions...", "info");
    
    new Thread(() -> {
        try {
            CalendarService calendarService = new CalendarService();
            SessionService sessionService = new SessionService();
            
            // Get all sessions not yet synced
            List<Session> pendingSessions = sessionService.getSessionsNotSyncedToGoogle();
            
            int successCount = 0;
            int failureCount = 0;
            
            for (Session session : pendingSessions) {
                try {
                    String googleEventId = calendarService.syncSessionToGoogle(session);
                    session.setGoogleCalendarEventId(googleEventId);
                    sessionService.updateSession(session);
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    System.err.println("Failed to sync session " + session.getId() + ": " + e.getMessage());
                }
            }
            
            Platform.runLater(() -> {
                String message = String.format(
                    "✓ Batch sync completed: %d synced, %d failed",
                    successCount, failureCount
                );
                showMessage(message, successCount > 0 ? "success" : "warning");
                addSyncHistoryEntry("Batch Sync", "Success", message);
                loadCalendarEvents();
                testSyncButton.setDisable(false);
            });
            
        } catch (Exception e) {
            Platform.runLater(() -> {
                showMessage("✗ Batch sync failed: " + e.getMessage(), "error");
                testSyncButton.setDisable(false);
            });
        }
    }).start();
}
```

---

## Example 6: Handle Sync Conflicts

### Scenario
When a session is modified in both DayFlow and Google Calendar, resolve the conflict.

### Implementation

**In CalendarSyncStatusController.java:**

```java
/**
 * Handle sync conflict resolution
 */
private void handleSyncConflict(Session dayflowSession, Event googleEvent) {
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Sync Conflict");
    alert.setHeaderText("Session modified in both DayFlow and Google Calendar");
    alert.setContentText(
        "DayFlow version: " + dayflowSession.getDate() + " " + dayflowSession.getStartTime() + "\n" +
        "Google Calendar version: " + googleEvent.getStart() + "\n\n" +
        "Which version should be used?"
    );
    
    ButtonType keepDayFlow = new ButtonType("Keep DayFlow Version");
    ButtonType keepGoogle = new ButtonType("Keep Google Calendar Version");
    ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
    
    alert.getButtonTypes().setAll(keepDayFlow, keepGoogle, cancel);
    
    alert.showAndWait().ifPresent(result -> {
        if (result == keepDayFlow) {
            // Update Google Calendar with DayFlow version
            updateGoogleCalendarEvent(dayflowSession);
        } else if (result == keepGoogle) {
            // Update DayFlow with Google Calendar version
            updateDayFlowSession(googleEvent);
        }
    });
}
```

---

## Example 7: Display Sync Statistics

### Scenario
Show sync statistics in the dashboard (total synced, pending, failed).

### Implementation

**In GoogleCalendarSyncController.java:**

```java
/**
 * Load and display sync statistics
 */
private void loadSyncStatistics() {
    new Thread(() -> {
        try {
            SessionService sessionService = new SessionService();
            
            int totalSessions = sessionService.getTotalSessions();
            int syncedSessions = sessionService.getSyncedSessionsCount();
            int pendingSessions = totalSessions - syncedSessions;
            double syncPercentage = (syncedSessions * 100.0) / totalSessions;
            
            Platform.runLater(() -> {
                Label statsLabel = new Label(
                    String.format(
                        "Total: %d | Synced: %d | Pending: %d | Progress: %.1f%%",
                        totalSessions, syncedSessions, pendingSessions, syncPercentage
                    )
                );
                statsLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #6b7280;");
                
                // Add to UI
                mainVBox.getChildren().add(0, statsLabel);
            });
            
        } catch (Exception e) {
            System.err.println("Error loading statistics: " + e.getMessage());
        }
    }).start();
}
```

---

## Example 8: Notification on Sync Completion

### Scenario
Send a notification to the user when sync completes.

### Implementation

**In GoogleCalendarSyncController.java:**

```java
/**
 * Send notification on sync completion
 */
private void notifyOnSyncCompletion(int syncedCount) {
    if (syncNotificationsCheckBox.isSelected()) {
        // Desktop notification
        showDesktopNotification(
            "Google Calendar Sync",
            syncedCount + " sessions synced to Google Calendar"
        );
        
        // In-app notification
        showMessage(
            "✓ " + syncedCount + " sessions synced to Google Calendar!",
            "success"
        );
    }
}

/**
 * Show desktop notification (Windows/Mac/Linux)
 */
private void showDesktopNotification(String title, String message) {
    try {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            // Windows notification
            Runtime.getRuntime().exec(new String[]{
                "powershell", "-Command",
                "[Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType = WindowsRuntime] > $null\n" +
                "[Windows.UI.Notifications.ToastNotification, Windows.UI.Notifications, ContentType = WindowsRuntime] > $null\n" +
                "[Windows.Data.Xml.Dom.XmlDocument, Windows.Data.Xml.Dom.XmlDocument, ContentType = WindowsRuntime] > $null\n" +
                "$APP_ID = 'DayFlow'\n" +
                "$template = @\"\n" +
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<toast>\n" +
                "    <visual>\n" +
                "        <binding template=\"ToastText02\">\n" +
                "            <text id=\"1\">" + title + "</text>\n" +
                "            <text id=\"2\">" + message + "</text>\n" +
                "        </binding>\n" +
                "    </visual>\n" +
                "</toast>\n" +
                "\"@\n" +
                "$xml = New-Object Windows.Data.Xml.Dom.XmlDocument\n" +
                "$xml.LoadXml($template)\n" +
                "$toast = New-Object Windows.UI.Notifications.ToastNotification $xml\n" +
                "[Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier($APP_ID).Show($toast)"
            });
        }
    } catch (Exception e) {
        System.err.println("Could not show desktop notification: " + e.getMessage());
    }
}
```

---

## Summary

These examples show how to:
- ✅ Add sync dashboard to navigation
- ✅ Display sync status in session lists
- ✅ Auto-sync on session creation
- ✅ Sync on session confirmation
- ✅ Batch sync multiple sessions
- ✅ Handle sync conflicts
- ✅ Display sync statistics
- ✅ Send notifications

All examples follow best practices for:
- Thread safety (using Platform.runLater)
- Error handling
- User feedback
- Performance optimization

---

**Last Updated**: May 5, 2026
**Version**: 1.0.0
