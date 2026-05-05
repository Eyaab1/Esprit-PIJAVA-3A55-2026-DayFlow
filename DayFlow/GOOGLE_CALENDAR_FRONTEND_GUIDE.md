# 📅 Google Calendar Frontend Integration Guide

## Overview

This guide explains how to integrate the Google Calendar Frontend UI with your DayFlow application. The frontend provides a complete user interface for managing Google Calendar synchronization.

## Files Created

### 1. **UI Files**
- `src/main/resources/user/coaching_session/google_calendar_sync.fxml` - Main sync dashboard
- `src/main/resources/user/coaching_session/google_calendar_sync.css` - Dashboard styles
- `src/main/resources/user/coaching_session/calendar_sync_status.fxml` - Session sync status component
- `src/main/resources/user/coaching_session/calendar_sync_status.css` - Status component styles

### 2. **Controller Files**
- `src/main/java/controllers/GoogleCalendarSyncController.java` - Main sync dashboard controller
- `src/main/java/controllers/CalendarSyncStatusController.java` - Status component controller

## Features

### Main Dashboard (GoogleCalendarSyncController)

#### 1. **Connection Management**
- Connect/Disconnect Google Account
- Real-time connection status indicator
- OAuth flow integration ready

#### 2. **Sync Options**
- Auto-sync toggle (enabled by default)
- Notification sync toggle
- Bidirectional sync (experimental)
- Configurable sync intervals:
  - Real-time (30 seconds)
  - Every 5 minutes
  - Every 15 minutes
  - Every hour
  - Manual only

#### 3. **Sync History**
- Table showing all sync operations
- Timestamp, action, status, and details
- Keeps last 50 entries
- Sortable and filterable

#### 4. **Calendar Events Preview**
- List of upcoming synced events
- Real-time updates
- Event details display

#### 5. **Action Buttons**
- **Refresh**: Manually refresh calendar events
- **Test Sync**: Test sync with sample session
- **Clear Cache**: Clear all cached data
- **Settings**: View current sync settings

### Status Component (CalendarSyncStatusController)

#### 1. **Session-Level Sync Status**
- Visual indicator (green = synced, red = not synced)
- Status text and details
- Quick sync button

#### 2. **Options Menu**
- View in Google Calendar
- Remove from Calendar
- Sync Settings

## Integration Steps

### Step 1: Add to Main Navigation

In your `ShellController.java` or main navigation, add a menu item:

```java
// In your navigation setup
MenuItem googleCalendarItem = new MenuItem("📅 Google Calendar Sync");
googleCalendarItem.setOnAction(e -> {
    try {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/user/coaching_session/google_calendar_sync.fxml")
        );
        Parent root = loader.load();
        
        Stage stage = new Stage();
        stage.setTitle("Google Calendar Sync");
        stage.setScene(new Scene(root, 900, 700));
        stage.show();
    } catch (IOException ex) {
        ex.printStackTrace();
    }
});
```

### Step 2: Add to Session Details View

In `CoachSessionsController.java` or session detail view, add the status component:

```java
// Load the status component
FXMLLoader loader = new FXMLLoader(
    getClass().getResource("/user/coaching_session/calendar_sync_status.fxml")
);
HBox statusComponent = loader.load();
CalendarSyncStatusController statusController = loader.getController();

// Set session ID and sync status
statusController.setSessionId(sessionId);
statusController.setSyncStatus(isSynced, googleCalendarEventId);

// Add to your session details layout
sessionDetailsVBox.getChildren().add(statusComponent);
```

### Step 3: Load CSS Stylesheet

In your main application or controller:

```java
// Load the CSS stylesheet
String css = getClass().getResource("/user/coaching_session/google_calendar_sync.css").toExternalForm();
scene.getStylesheets().add(css);
```

### Step 4: Connect to Backend Services

Update the controllers to use your backend services:

```java
// In GoogleCalendarSyncController.handleConnectGoogle()
private void handleConnectGoogle() {
    // Call your CalendarService
    CalendarService calendarService = new CalendarService();
    
    new Thread(() -> {
        try {
            boolean connected = calendarService.authenticateWithGoogle();
            
            Platform.runLater(() -> {
                if (connected) {
                    isConnected = true;
                    updateConnectionStatus(true);
                    loadCalendarEvents();
                } else {
                    showMessage("Connection failed", "error");
                }
            });
        } catch (Exception e) {
            Platform.runLater(() -> showMessage("Error: " + e.getMessage(), "error"));
        }
    }).start();
}
```

### Step 5: Implement Backend Integration

Connect the frontend to your backend services:

```java
// In GoogleCalendarSyncController.performSync()
private void performSync() {
    if (!isConnected) return;
    
    new Thread(() -> {
        try {
            // Call your backend sync service
            CalendarService calendarService = new CalendarService();
            List<Session> sessions = calendarService.getSessions();
            
            for (Session session : sessions) {
                if (!session.isSyncedToGoogle()) {
                    calendarService.syncSessionToGoogle(session);
                }
            }
            
            Platform.runLater(() -> {
                loadCalendarEvents();
                addSyncHistoryEntry("Auto Sync", "Success", "Sessions synced");
            });
        } catch (Exception e) {
            Platform.runLater(() -> showMessage("Sync failed: " + e.getMessage(), "error"));
        }
    }).start();
}
```

## Usage Examples

### Example 1: Add Sync Dashboard to Coach Dashboard

```java
// In CoachDashboardController.java
@FXML
private void handleGoogleCalendarSync() {
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
        e.printStackTrace();
    }
}
```

### Example 2: Add Status Component to Session Card

```java
// In a session card or list item
private void loadSessionWithCalendarStatus(Session session) {
    try {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/user/coaching_session/calendar_sync_status.fxml")
        );
        HBox statusComponent = loader.load();
        CalendarSyncStatusController controller = loader.getController();
        
        controller.setSessionId(session.getId());
        controller.setSyncStatus(
            session.getGoogleCalendarEventId() != null,
            session.getGoogleCalendarEventId()
        );
        
        sessionDetailsVBox.getChildren().add(statusComponent);
    } catch (IOException e) {
        e.printStackTrace();
    }
}
```

### Example 3: Auto-sync on Session Creation

```java
// In AddSessionController.java
@FXML
private void handleConfirmSession() {
    // Create session
    Session session = createSession();
    
    // Auto-sync to Google Calendar if enabled
    if (autoSyncEnabled) {
        new Thread(() -> {
            try {
                CalendarService calendarService = new CalendarService();
                calendarService.syncSessionToGoogle(session);
                
                Platform.runLater(() -> {
                    showMessage("✓ Session created and synced to Google Calendar!", "success");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showMessage("Session created but sync failed: " + e.getMessage(), "warning");
                });
            }
        }).start();
    }
}
```

## Configuration

### Environment Variables

Add to your `.env` file:

```env
# Google Calendar Configuration
GOOGLE_CALENDAR_ID=primary
GOOGLE_CREDENTIALS_PATH=src/main/resources/credentials.json
GOOGLE_CALENDAR_SYNC_ENABLED=true
GOOGLE_CALENDAR_AUTO_SYNC=true
GOOGLE_CALENDAR_SYNC_INTERVAL=30
```

### Application Properties

Add to `config.properties`:

```properties
# Google Calendar Settings
google.calendar.enabled=true
google.calendar.auto.sync=true
google.calendar.sync.interval=30
google.calendar.bidirectional=false
google.calendar.notifications=true
```

## Styling Customization

### Change Primary Color

In `google_calendar_sync.css`:

```css
Button {
    -fx-background-color: #your-color-here;
}

Button:hover {
    -fx-background-color: #your-hover-color;
}
```

### Change Status Indicator Colors

```css
Circle.connected {
    -fx-fill: #your-success-color;
}

Circle.syncing {
    -fx-fill: #your-warning-color;
}
```

## Error Handling

The controllers include comprehensive error handling:

- Connection failures
- Sync failures
- Network timeouts
- Invalid credentials
- Rate limiting

All errors are displayed to the user with appropriate messages.

## Performance Considerations

1. **Auto-sync Scheduling**: Uses `ScheduledExecutorService` for efficient scheduling
2. **Thread Management**: All long-running operations run on separate threads
3. **UI Updates**: All UI updates use `Platform.runLater()` for thread safety
4. **Memory Management**: Sync history limited to 50 entries
5. **Resource Cleanup**: Call `cleanup()` when closing the sync dashboard

## Testing

### Test Sync Functionality

1. Click "Test Sync" button
2. Verify sync history entry is created
3. Check that event appears in events list
4. Verify Google Calendar is updated

### Test Connection

1. Click "Connect Google Account"
2. Verify status indicator changes to green
3. Verify buttons become enabled
4. Verify events load

### Test Auto-sync

1. Enable auto-sync
2. Set interval to "Every 5 minutes"
3. Wait for automatic sync to occur
4. Verify sync history updates

## Troubleshooting

### Connection Issues

**Problem**: Cannot connect to Google Account
**Solution**: 
- Verify credentials.json file exists
- Check Google API credentials are valid
- Ensure internet connection is active

### Sync Failures

**Problem**: Sync operation fails
**Solution**:
- Check backend CalendarService is running
- Verify Google Calendar API is enabled
- Check rate limiting hasn't been exceeded

### UI Not Displaying

**Problem**: FXML file not loading
**Solution**:
- Verify file path is correct
- Check CSS stylesheet is loaded
- Verify controller class name matches FXML

## Next Steps

1. **Implement OAuth Flow**: Add proper Google OAuth authentication
2. **Add Bidirectional Sync**: Sync changes from Google Calendar back to DayFlow
3. **Add Notifications**: Send notifications when events are synced
4. **Add Conflict Resolution**: Handle conflicts between DayFlow and Google Calendar
5. **Add Batch Operations**: Sync multiple sessions at once

## Support

For issues or questions:
1. Check the troubleshooting section
2. Review the integration examples
3. Check backend CalendarService logs
4. Verify Google Calendar API configuration

---

**Last Updated**: May 5, 2026
**Version**: 1.0.0
