package controllers.goals_routines;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.ScrollPane;
import model.goals_activity_management.Goal;
import model.user.User;
import services.goals_routines.GoalService;
import session.AppSession;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class GoalsCalendarController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRENCH);
    private static final DateTimeFormatter MONTH_YEAR_FMT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);

    private final GoalService goalService = new GoalService();
    private List<Goal> userGoals = new ArrayList<>();
    private YearMonth currentMonth = YearMonth.now();
    private LocalDate selectedDate = LocalDate.now();

    @FXML private Button prevMonthBtn;
    @FXML private Button nextMonthBtn;
    @FXML private Label monthYearLabel;
    @FXML private GridPane weekHeaderGrid;
    @FXML private GridPane calendarGrid;
    @FXML private Label selectedDateTitle;
    @FXML private VBox eventsListBox;
    @FXML private VBox upcomingListBox;

    @FXML
    private void initialize() {
        try {
            loadGoals();
            updateCalendar();
            renderForDate(selectedDate);
            renderUpcoming();
        } catch (Exception e) {
            // Fallback initialization
            try {
                if (monthYearLabel != null) {
                    monthYearLabel.setText("Mai 2026");
                }
                if (selectedDateTitle != null) {
                    selectedDateTitle.setText("📋 Calendrier des objectifs");
                }
                if (eventsListBox != null) {
                    eventsListBox.getChildren().clear();
                    Label errorLabel = new Label("Erreur lors du chargement des objectifs");
                    errorLabel.getStyleClass().add("empty-state");
                    eventsListBox.getChildren().add(errorLabel);
                }
                if (upcomingListBox != null) {
                    upcomingListBox.getChildren().clear();
                    Label errorLabel = new Label("Erreur lors du chargement des deadlines");
                    errorLabel.getStyleClass().add("empty-state");
                    upcomingListBox.getChildren().add(errorLabel);
                }
            } catch (Exception fallbackError) {
                // ignore fallback failures
            }
        }
    }

    @FXML
    private void onRefresh() {
        loadGoals();
        updateCalendar();
        renderForDate(selectedDate);
        renderUpcoming();
    }

    @FXML
    private void onPreviousMonth() {
        currentMonth = currentMonth.minusMonths(1);
        updateCalendar();
    }

    @FXML
    private void onNextMonth() {
        currentMonth = currentMonth.plusMonths(1);
        updateCalendar();
    }

    @FXML
    private void onToday() {
        currentMonth = YearMonth.now();
        selectedDate = LocalDate.now();
        updateCalendar();
        renderForDate(selectedDate);
    }

    private void loadGoals() {
        try {
            Integer userId = AppSession.getCurrentUser().map(User::getId).orElse(null);
            if (userId == null) {
                userGoals = List.of();
                return;
            }
            userGoals = goalService.findByUserId(userId);
        } catch (SQLException e) {
            userGoals = List.of();
        } catch (Exception e) {
            userGoals = List.of();
        }
    }

    private void updateCalendar() {
        try {
            // Update month/year label
            if (monthYearLabel != null) {
                monthYearLabel.setText(MONTH_YEAR_FMT.format(currentMonth));
            }
            
            // Clear existing calendar
            if (calendarGrid != null) {
                calendarGrid.getChildren().clear();
                
                // Get first day of month and calculate starting position
                LocalDate firstOfMonth = currentMonth.atDay(1);
                int startDayOfWeek = firstOfMonth.getDayOfWeek().getValue() - 1; // Monday = 0
                
                // Calculate dates to show (including previous/next month dates)
                LocalDate startDate = firstOfMonth.minusDays(startDayOfWeek);
                
                // Create calendar cells
                for (int week = 0; week < 6; week++) {
                    for (int day = 0; day < 7; day++) {
                        LocalDate cellDate = startDate.plusDays(week * 7 + day);
                        VBox dayCell = createDayCell(cellDate);
                        calendarGrid.add(dayCell, day, week);
                    }
                }
            }
        } catch (Exception e) {
            // keep page responsive even if a single refresh fails
        }
    }

    private VBox createDayCell(LocalDate date) {
        VBox cell = new VBox(4);
        cell.setAlignment(Pos.TOP_LEFT);
        cell.setPadding(new Insets(12));
        cell.setPrefHeight(120);
        cell.getStyleClass().add("day-cell");
        
        // Add click handler
        cell.setOnMouseClicked(e -> {
            selectedDate = date;
            updateCalendar(); // Refresh to update selection
            renderForDate(date);
            
            // Show popup with day details
            showDayDetailsPopup(date, e.getScreenX(), e.getScreenY());
        });
        
        // Create header with day number (top right alignment)
        HBox header = new HBox();
        header.setAlignment(Pos.TOP_RIGHT);
        
        Label dayNumber = new Label(String.valueOf(date.getDayOfMonth()));
        dayNumber.getStyleClass().add("day-number");
        header.getChildren().add(dayNumber);
        
        // Apply styles based on date properties
        boolean isCurrentMonth = date.getMonth() == currentMonth.getMonth() && date.getYear() == currentMonth.getYear();
        boolean isToday = date.equals(LocalDate.now());
        boolean isSelected = date.equals(selectedDate);
        
        if (!isCurrentMonth) {
            cell.getStyleClass().add("other-month");
            dayNumber.getStyleClass().add("other-month");
        }
        
        if (isToday) {
            cell.getStyleClass().add("today");
            dayNumber.getStyleClass().add("today");
        }
        
        if (isSelected) {
            cell.getStyleClass().add("selected");
        }
        
        // Get goals for this date
        List<Goal> dayGoals = getGoalsForDate(date);
        
        // Create events container
        VBox eventsContainer = new VBox(3);
        eventsContainer.setAlignment(Pos.TOP_LEFT);
        
        if (!dayGoals.isEmpty()) {
            // Add visual indicator that this day is clickable
            cell.getStyleClass().add("has-events-clickable");
            
            // Sort goals by priority: Deadlines > Goals > Normal events
            List<Goal> sortedGoals = dayGoals.stream()
                    .sorted((g1, g2) -> {
                        // Priority: Deadlines first, then goals, then normal events
                        boolean g1IsDeadline = g1.getDeadline() != null && date.equals(g1.getDeadline().toLocalDate());
                        boolean g2IsDeadline = g2.getDeadline() != null && date.equals(g2.getDeadline().toLocalDate());
                        
                        if (g1IsDeadline && !g2IsDeadline) return -1;
                        if (!g1IsDeadline && g2IsDeadline) return 1;
                        
                        // Both are same type, sort by time if deadline, otherwise by title
                        if (g1IsDeadline && g2IsDeadline) {
                            return g1.getDeadline().compareTo(g2.getDeadline());
                        }
                        
                        return safeTitle(g1).compareToIgnoreCase(safeTitle(g2));
                    })
                    .toList();
            
            // Show up to 3 events
            List<Goal> displayGoals = sortedGoals.stream().limit(3).toList();
            
            for (Goal goal : displayGoals) {
                Label eventLabel = createEventLabel(goal, date);
                eventsContainer.getChildren().add(eventLabel);
            }
            
            // Show "+X more" if there are additional goals
            if (sortedGoals.size() > 3) {
                Label moreLabel = new Label("+" + (sortedGoals.size() - 3) + " more");
                moreLabel.getStyleClass().add("more-events");
                eventsContainer.getChildren().add(moreLabel);
            }
            
            // Create comprehensive tooltip
            String tooltipText = sortedGoals.stream()
                    .map(g -> {
                        if (g.getDeadline() != null && date.equals(g.getDeadline().toLocalDate())) {
                            return "🔴 " + safeTitle(g) + " (Deadline: " + DATE_TIME_FMT.format(g.getDeadline()) + ")";
                        } else {
                            return "🟢 " + safeTitle(g) + " (Goal)";
                        }
                    })
                    .collect(Collectors.joining("\n"));
            Tooltip.install(cell, new Tooltip(tooltipText));
        }
        
        cell.getChildren().addAll(header, eventsContainer);
        return cell;
    }
    
    /**
     * Creates an event label based on the goal type and priority
     */
    private Label createEventLabel(Goal goal, LocalDate date) {
        boolean isDeadline = goal.getDeadline() != null && date.equals(goal.getDeadline().toLocalDate());
        
        String displayText;
        
        if (isDeadline) {
            // 🔴 DEADLINE (Highest Priority)
            displayText = goal.getDeadline().format(DateTimeFormatter.ofPattern("HH:mm")) + " " + safeTitle(goal);
            if (displayText.length() > 16) {
                displayText = displayText.substring(0, 13) + "...";
            }
            Label eventLabel = new Label(displayText);
            eventLabel.getStyleClass().add("event-deadline");
            return eventLabel;
            
        } else if (isGoalActiveOnDate(goal, date)) {
            // 🟢 GOAL (Medium Priority)
            displayText = safeTitle(goal);
            if (displayText.length() > 14) {
                displayText = displayText.substring(0, 11) + "...";
            }
            Label eventLabel = new Label(displayText);
            eventLabel.getStyleClass().add("event-goal");
            
            // Add progress indicator border
            if (goal.getProgress() >= 70) {
                eventLabel.getStyleClass().add("goal-progress-good");
            } else if (goal.getProgress() >= 40) {
                eventLabel.getStyleClass().add("goal-progress-medium");
            } else if (goal.isOverdue()) {
                eventLabel.getStyleClass().add("goal-progress-late");
            }
            
            return eventLabel;
            
        } else {
            // 🔵 NORMAL EVENT (Lowest Priority) - return as simple text with dot
            displayText = "• " + safeTitle(goal);
            if (displayText.length() > 16) {
                displayText = displayText.substring(0, 13) + "...";
            }
            
            Label eventLabel = new Label(displayText);
            eventLabel.getStyleClass().add("event-normal");
            return eventLabel;
        }
    }

    private List<Goal> getGoalsForDate(LocalDate date) {
        return userGoals.stream()
                .filter(g -> isGoalOnDate(g, date))
                .toList();
    }

    private void renderForDate(LocalDate date) {
        try {
            selectedDateTitle.setText("📋 Objectifs du " + DATE_FMT.format(date));
            eventsListBox.getChildren().clear();

            List<Goal> events = new ArrayList<>(getGoalsForDate(date)); // Create mutable list

            if (events.isEmpty()) {
                Label emptyLabel = new Label("Aucun objectif prévu pour cette date");
                emptyLabel.getStyleClass().add("empty-state");
                eventsListBox.getChildren().add(emptyLabel);
                return;
            }

            events.sort(Comparator.comparing(Goal::getTitle, Comparator.nullsLast(String::compareToIgnoreCase)));

            for (Goal goal : events) {
                VBox card = createEventCard(goal, date);
                eventsListBox.getChildren().add(card);
            }
        } catch (Exception e) {
            // Fallback display
            eventsListBox.getChildren().clear();
            Label errorLabel = new Label("Erreur lors du chargement des objectifs pour cette date");
            errorLabel.getStyleClass().add("empty-state");
            eventsListBox.getChildren().add(errorLabel);
        }
    }

    private VBox createEventCard(Goal goal, LocalDate date) {
        VBox card = new VBox(8);
        card.getStyleClass().add("calendar-event-card");
        
        // Title
        Label title = new Label(goal.getTitle() != null ? goal.getTitle() : "Objectif sans titre");
        title.getStyleClass().add("calendar-event-title");
        
        // Status
        Label status = new Label(getStatusText(goal));
        status.getStyleClass().addAll("event-status", getStatusStyleClass(goal));
        
        // Duration info
        Label duration = new Label("📅 " + formatDuration(goal));
        duration.getStyleClass().add("event-detail");
        
        // Deadline info
        Label deadline = new Label("⏰ " + formatDeadline(goal));
        deadline.getStyleClass().add("event-detail");
        
        // Progress info
        Label progress = new Label("📊 Progression: " + goal.getProgress() + "%");
        progress.getStyleClass().add("event-detail");
        
        HBox statusRow = new HBox(10);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        statusRow.getChildren().addAll(status, progress);
        
        card.getChildren().addAll(title, statusRow, duration, deadline);
        return card;
    }

    private String getStatusText(Goal goal) {
        if (goal.getStatus() == null) return "Non défini";
        return switch (goal.getStatus().toLowerCase()) {
            case "completed" -> "✅ Terminé";
            case "active" -> "🔄 En cours";
            case "paused" -> "⏸️ En pause";
            case "draft" -> "📝 Brouillon";
            default -> goal.getStatus();
        };
    }

    private String getStatusStyleClass(Goal goal) {
        if (goal.getStatus() == null) return "active";
        return switch (goal.getStatus().toLowerCase()) {
            case "completed" -> "completed";
            case "active" -> "active";
            default -> {
                // Check if overdue
                if (goal.getDeadline() != null && goal.getDeadline().isBefore(LocalDate.now().atStartOfDay()) && goal.getProgress() < 100) {
                    yield "overdue";
                }
                yield "active";
            }
        };
    }

    private void renderUpcoming() {
        try {
            upcomingListBox.getChildren().clear();
            
            List<Goal> withDeadlines = userGoals.stream()
                    .filter(g -> g.getDeadline() != null && g.getDeadline().isAfter(LocalDate.now().atStartOfDay()))
                    .sorted(Comparator.comparing(Goal::getDeadline))
                    .limit(10)
                    .collect(Collectors.toList()); // Use collect to create mutable list
                    
            if (withDeadlines.isEmpty()) {
                Label emptyLabel = new Label("Aucune deadline à venir");
                emptyLabel.getStyleClass().add("empty-state");
                upcomingListBox.getChildren().add(emptyLabel);
                return;
            }
            
            for (Goal goal : withDeadlines) {
                VBox item = createUpcomingItem(goal);
                upcomingListBox.getChildren().add(item);
            }
        } catch (Exception e) {
            // Fallback display
            upcomingListBox.getChildren().clear();
            Label errorLabel = new Label("Erreur lors du chargement des deadlines");
            errorLabel.getStyleClass().add("empty-state");
            upcomingListBox.getChildren().add(errorLabel);
        }
    }

    private VBox createUpcomingItem(Goal goal) {
        VBox item = new VBox(4);
        item.getStyleClass().add("upcoming-item");
        
        // Check if urgent (within 3 days)
        long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), goal.getDeadline().toLocalDate());
        if (daysUntil <= 3) {
            item.getStyleClass().add("urgent");
        }
        
        Label title = new Label(safeTitle(goal));
        title.getStyleClass().add("upcoming-title");
        
        Label dateLabel = new Label(DATE_TIME_FMT.format(goal.getDeadline()) + 
                                  " (" + daysUntil + " jour" + (daysUntil > 1 ? "s" : "") + ")");
        dateLabel.getStyleClass().add("upcoming-date");
        if (daysUntil <= 3) {
            dateLabel.getStyleClass().add("urgent");
        }
        
        Label progressLabel = new Label("Progression: " + goal.getProgress() + "%");
        progressLabel.getStyleClass().add("upcoming-date");
        
        item.getChildren().addAll(title, dateLabel, progressLabel);
        return item;
    }

    /**
     * Shows a popup with detailed information about goals and deadlines for the selected day
     */
    private void showDayDetailsPopup(LocalDate date, double screenX, double screenY) {
        List<Goal> dayGoals = getGoalsForDate(date);
        
        if (dayGoals.isEmpty()) {
            return; // Don't show popup if no goals
        }
        
        // Create popup window
        Stage popupStage = new Stage();
        popupStage.initOwner(calendarGrid.getScene().getWindow());
        popupStage.setResizable(false);
        popupStage.setTitle("Objectifs du " + DATE_FMT.format(date));
        
        // Create popup content
        VBox popupContent = createPopupContent(date, dayGoals);
        
        // Wrap in scroll pane for long lists
        ScrollPane scrollPane = new ScrollPane(popupContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPrefViewportHeight(Math.min(400, popupContent.getPrefHeight()));
        scrollPane.getStyleClass().add("popup-scroll");
        
        // Create scene
        Scene popupScene = new Scene(scrollPane, 320, Math.min(450, 150 + dayGoals.size() * 60));
        
        // Add CSS styling
        popupScene.getStylesheets().add(getClass().getResource("/user/goals_routines/goals_calendar.css").toExternalForm());
        
        popupStage.setScene(popupScene);
        
        // Position popup near click location but ensure it's visible
        double popupX = Math.max(50, Math.min(screenX - 160, 
            calendarGrid.getScene().getWindow().getX() + calendarGrid.getScene().getWindow().getWidth() - 370));
        double popupY = Math.max(50, Math.min(screenY - 100, 
            calendarGrid.getScene().getWindow().getY() + calendarGrid.getScene().getWindow().getHeight() - 500));
        
        popupStage.setX(popupX);
        popupStage.setY(popupY);
        
        // Show popup
        popupStage.show();
        
        // Focus the popup for keyboard interaction
        popupStage.requestFocus();
        
        // Add keyboard support (ESC to close)
        popupScene.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode().toString().equals("ESCAPE")) {
                popupStage.close();
            }
        });
        
        // Auto-close after losing focus
        popupStage.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                // Delay closing to allow for clicking within popup
                javafx.application.Platform.runLater(() -> {
                    try {
                        Thread.sleep(100);
                        if (!popupStage.isFocused()) {
                            popupStage.close();
                        }
                    } catch (InterruptedException e) {
                        popupStage.close();
                    }
                });
            }
        });
    }
    
    /**
     * Creates the content for the day details popup
     */
    private VBox createPopupContent(LocalDate date, List<Goal> dayGoals) {
        VBox content = new VBox(16);
        content.getStyleClass().add("popup-content");
        content.setPadding(new Insets(20));
        
        // Header
        Label headerLabel = new Label("📅 " + date.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH)));
        headerLabel.getStyleClass().add("popup-header");
        content.getChildren().add(headerLabel);
        
        // Separate goals by type
        List<Goal> deadlineGoals = dayGoals.stream()
                .filter(g -> g.getDeadline() != null && date.equals(g.getDeadline().toLocalDate()))
                .sorted(Comparator.comparing(Goal::getDeadline))
                .toList();
        
        List<Goal> activeGoals = dayGoals.stream()
                .filter(g -> isGoalActiveOnDate(g, date) && 
                           !(g.getDeadline() != null && date.equals(g.getDeadline().toLocalDate())))
                .sorted(Comparator.comparing(Goal::getTitle, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
        
        // Deadlines section
        if (!deadlineGoals.isEmpty()) {
            Label deadlineHeader = new Label("🔴 Deadlines (" + deadlineGoals.size() + ")");
            deadlineHeader.getStyleClass().add("popup-section-header");
            content.getChildren().add(deadlineHeader);
            
            for (Goal goal : deadlineGoals) {
                VBox goalCard = createPopupGoalCard(goal, true);
                content.getChildren().add(goalCard);
            }
        }
        
        // Active goals section
        if (!activeGoals.isEmpty()) {
            Label goalsHeader = new Label("🟢 Objectifs actifs (" + activeGoals.size() + ")");
            goalsHeader.getStyleClass().add("popup-section-header");
            content.getChildren().add(goalsHeader);
            
            for (Goal goal : activeGoals) {
                VBox goalCard = createPopupGoalCard(goal, false);
                content.getChildren().add(goalCard);
            }
        }
        
        // Close button
        Button closeButton = new Button("Fermer");
        closeButton.getStyleClass().add("popup-close-button");
        closeButton.setOnAction(e -> ((Stage) closeButton.getScene().getWindow()).close());
        
        HBox buttonContainer = new HBox();
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.getChildren().add(closeButton);
        content.getChildren().add(buttonContainer);
        
        return content;
    }
    
    /**
     * Creates a goal card for the popup
     */
    private VBox createPopupGoalCard(Goal goal, boolean isDeadline) {
        VBox card = new VBox(8);
        card.getStyleClass().add("popup-goal-card");
        
        // Title
        Label titleLabel = new Label(safeTitle(goal));
        titleLabel.getStyleClass().add("popup-goal-title");
        
        // Time and status info
        VBox infoContainer = new VBox(4);
        
        if (isDeadline && goal.getDeadline() != null) {
            Label timeLabel = new Label("⏰ " + goal.getDeadline().format(DateTimeFormatter.ofPattern("HH:mm")));
            timeLabel.getStyleClass().add("popup-goal-time");
            infoContainer.getChildren().add(timeLabel);
        }
        
        // Progress
        Label progressLabel = new Label("📊 Progression: " + goal.getProgress() + "%");
        progressLabel.getStyleClass().add("popup-goal-progress");
        infoContainer.getChildren().add(progressLabel);
        
        // Status
        String statusText = getStatusText(goal);
        Label statusLabel = new Label(statusText);
        statusLabel.getStyleClass().addAll("popup-goal-status", getStatusStyleClass(goal));
        infoContainer.getChildren().add(statusLabel);
        
        // Description (if available)
        if (goal.getDescription() != null && !goal.getDescription().trim().isEmpty()) {
            Label descLabel = new Label(goal.getDescription().length() > 100 ? 
                goal.getDescription().substring(0, 97) + "..." : goal.getDescription());
            descLabel.getStyleClass().add("popup-goal-description");
            descLabel.setWrapText(true);
            infoContainer.getChildren().add(descLabel);
        }
        
        card.getChildren().addAll(titleLabel, infoContainer);
        
        // Add priority styling
        if (isDeadline) {
            card.getStyleClass().add("deadline-card");
        } else {
            card.getStyleClass().add("goal-card");
        }
        
        return card;
    }

    private static String safeTitle(Goal goal) {
        return goal.getTitle() != null ? goal.getTitle() : "Objectif sans titre";
    }

    private static String formatDuration(Goal goal) {
        if (goal.getStartDate() == null || goal.getEndDate() == null) {
            return "Durée non définie";
        }
        return DATE_FMT.format(goal.getStartDate()) + " → " + DATE_FMT.format(goal.getEndDate());
    }

    private static String formatDeadline(Goal goal) {
        if (goal.getDeadline() == null) {
            return "Deadline non définie";
        }
        return DATE_TIME_FMT.format(goal.getDeadline());
    }

    private static boolean isGoalOnDate(Goal goal, LocalDate date) {
        boolean inDuration = isGoalActiveOnDate(goal, date);
        boolean isDeadline = goal.getDeadline() != null && date.equals(goal.getDeadline().toLocalDate());
        return inDuration || isDeadline;
    }

    private static boolean isGoalActiveOnDate(Goal goal, LocalDate date) {
        return goal.getStartDate() != null
                && goal.getEndDate() != null
                && !date.isBefore(goal.getStartDate())
                && !date.isAfter(goal.getEndDate());
    }
}