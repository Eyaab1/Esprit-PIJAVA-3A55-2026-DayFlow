package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import model.coaching_session.Disponibilite;
import model.coaching_session.CoachingRequest;
import model.user.User;
import services.coaching_session.DisponibiliteService;
import services.coaching_session.CoachingRequestService;
import services.coaching_session.SessionTimeValidator;
import exceptions.PastSessionException;
import session.AppSession;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

public class CalendarCoachController implements Initializable {

    @FXML public Label coachNameLabel;
    @FXML public Label monthYearLabel;
    @FXML public Button previousMonthButton;
    @FXML public Button nextMonthButton;
    @FXML public GridPane calendarGrid;
    @FXML public VBox timeSlotContainer;
    @FXML public Label selectedDateLabel;
    @FXML public Label selectedTimeLabel;
    @FXML public Button reserveButton;
    @FXML public Label messageLabel;
    @FXML public ComboBox<String> viewModeCombo;
    @FXML public Label sessionCountLabel;
    @FXML public Label remainingSlotsLabel;

    private int coachId = -1;
    private String coachName = "";
    private YearMonth currentMonth;
    private LocalDate selectedDate;
    private Disponibilite selectedSlot;
    private DisponibiliteService service;
    private Map<LocalDate, List<Disponibilite>> slotsCache;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("\n\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║         CALENDAR CONTROLLER INITIALIZING                    ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        
        try {
            service = new DisponibiliteService();
            slotsCache = new HashMap<>();
            currentMonth = YearMonth.of(2026, 5);
            
            System.out.println("✓ Service created");
            System.out.println("✓ Cache initialized");
            System.out.println("✓ Current month set to: " + currentMonth);
            
            // Setup buttons
            if (previousMonthButton != null) {
                previousMonthButton.setOnAction(e -> previousMonth());
                System.out.println("✓ Previous button configured");
            }
            
            if (nextMonthButton != null) {
                nextMonthButton.setOnAction(e -> nextMonth());
                System.out.println("✓ Next button configured");
            }
            
            if (reserveButton != null) {
                reserveButton.setOnAction(e -> reserve());
                reserveButton.setDisable(true);
                System.out.println("✓ Reserve button configured");
            }
            
            // Setup combo
            if (viewModeCombo != null) {
                viewModeCombo.getItems().addAll("Mois", "Semaine", "Jour");
                viewModeCombo.setValue("Mois");
                System.out.println("✓ View mode combo configured");
            }
            
            // Setup calendar headers
            if (calendarGrid != null) {
                String[] days = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
                for (int i = 0; i < 7; i++) {
                    Label label = new Label(days[i]);
                    label.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");
                    label.setMaxWidth(Double.MAX_VALUE);
                    label.setAlignment(Pos.CENTER);
                    GridPane.setHgrow(label, Priority.ALWAYS);
                    calendarGrid.add(label, i, 0);
                }
                System.out.println("✓ Calendar headers configured");
            }
            
            System.out.println("\n✓ INITIALIZATION COMPLETE\n");
        } catch (Exception e) {
            System.err.println("✗ ERROR in initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setCoachInfo(int coachId, String coachName) {
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║         SETTING COACH INFO                                  ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        System.out.println("Coach ID: " + coachId);
        System.out.println("Coach Name: " + coachName);
        
        this.coachId = coachId;
        this.coachName = coachName;
        
        if (coachNameLabel != null) {
            coachNameLabel.setText("📅 Disponibilités - " + coachName);
            System.out.println("✓ Coach name label updated");
        }
        
        loadCalendar();
        updateSessionCount();
        System.out.println("\n✓ COACH INFO SET\n");
    }

    private void loadCalendar() {
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║         LOADING CALENDAR                                    ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        System.out.println("Month: " + currentMonth);
        System.out.println("Coach ID: " + coachId);
        
        if (monthYearLabel != null) {
            monthYearLabel.setText(currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        }
        
        // Pre-load all slots for this month
        slotsCache.clear();
        System.out.println("\nLoading slots from database...");
        
        int totalSlots = 0;
        for (int day = 1; day <= currentMonth.lengthOfMonth(); day++) {
            LocalDate date = currentMonth.atDay(day);
            try {
                List<Disponibilite> slots = service.getAvailableSlotsByDate(coachId, date);
                slotsCache.put(date, slots);
                if (!slots.isEmpty()) {
                    System.out.println("  ✓ " + date + ": " + slots.size() + " slots");
                    totalSlots += slots.size();
                }
            } catch (Exception e) {
                System.err.println("  ✗ Error loading slots for " + date + ": " + e.getMessage());
                slotsCache.put(date, new ArrayList<>());
            }
        }
        
        System.out.println("\nTotal slots loaded: " + totalSlots);
        
        // If no slots found, create test data
        if (totalSlots == 0) {
            System.out.println("\n⚠️  No slots found! Creating test data...");
            createTestData();
        }
        
        System.out.println("✓ CALENDAR LOADED\n");
        
        displayDays();
    }

    private void createTestData() {
        System.out.println("Creating test slots for May 2026...");
        
        // Create test slots for dates 10-16
        int[][] testSlots = {
            {10, 9, 10, 10, 11, 14, 15, 15, 16},  // day 10: 9-10, 10-11, 14-15, 15-16
            {11, 9, 10, 10, 11, 15, 16},           // day 11: 9-10, 10-11, 15-16
            {12, 10, 11, 11, 12, 14, 15},          // day 12: 10-11, 11-12, 14-15
            {13, 9, 10, 13, 14, 15, 16},           // day 13: 9-10, 13-14, 15-16
            {14, 10, 11, 11, 12, 14, 15},          // day 14: 10-11, 11-12, 14-15
            {15, 9, 10, 10, 11, 15, 16},           // day 15: 9-10, 10-11, 15-16
            {16, 11, 12, 13, 14, 14, 15}           // day 16: 11-12, 13-14, 14-15
        };
        
        for (int[] dayData : testSlots) {
            int day = dayData[0];
            LocalDate date = currentMonth.atDay(day);
            List<Disponibilite> slots = new ArrayList<>();
            
            for (int i = 1; i < dayData.length; i += 2) {
                int startHour = dayData[i];
                int endHour = dayData[i + 1];
                
                Disponibilite slot = new Disponibilite();
                slot.setCoachId(coachId);
                slot.setDate(date);
                slot.setHeureDebut(LocalTime.of(startHour, 0));
                slot.setHeureFin(LocalTime.of(endHour, 0));
                slot.setStatut("disponible");
                
                slots.add(slot);
            }
            
            slotsCache.put(date, slots);
            System.out.println("  ✓ " + date + ": " + slots.size() + " test slots created");
        }
    }

    private void displayDays() {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║         DISPLAYING CALENDAR DAYS                           ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        
        if (calendarGrid == null) {
            System.err.println("✗ ERROR: calendarGrid is NULL!");
            return;
        }
        
        // Remove old days (keep headers in row 0)
        calendarGrid.getChildren().removeIf(node -> {
            Integer row = GridPane.getRowIndex(node);
            return row != null && row > 0;
        });

        LocalDate first = currentMonth.atDay(1);
        int startDay = first.getDayOfWeek().getValue() - 1;
        int daysInMonth = currentMonth.lengthOfMonth();

        int row = 1, col = startDay;
        int greenCount = 0;
        int grayCount = 0;

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentMonth.atDay(day);
            Button btn = new Button(String.valueOf(day));
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setMaxHeight(Double.MAX_VALUE);
            btn.setPrefHeight(60);
            btn.setStyle("-fx-font-size: 14;");

            List<Disponibilite> slots = slotsCache.getOrDefault(date, new ArrayList<>());

            if (!slots.isEmpty()) {
                // GREEN - has slots
                btn.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #065f46; -fx-font-size: 14; -fx-font-weight: bold;");
                final LocalDate clickDate = date;
                btn.setOnAction(e -> {
                    System.out.println("\n>>> DATE CLICKED: " + clickDate + " <<<\n");
                    selectDate(clickDate);
                });
                greenCount++;
                System.out.println("  ✓ Day " + String.format("%2d", day) + " (GREEN): " + slots.size() + " slots");
            } else {
                // GRAY - no slots
                btn.setStyle("-fx-background-color: #f3f4f6; -fx-text-fill: #9ca3af; -fx-font-size: 14;");
                btn.setDisable(true);
                grayCount++;
            }

            if (date.equals(LocalDate.now())) {
                btn.setStyle(btn.getStyle() + "; -fx-border-color: #3b82f6; -fx-border-width: 2;");
            }

            if (date.equals(selectedDate)) {
                btn.setStyle(btn.getStyle() + "; -fx-border-color: #ef4444; -fx-border-width: 3;");
            }

            calendarGrid.add(btn, col, row);

            col++;
            if (col == 7) {
                col = 0;
                row++;
            }
        }
        
        System.out.println("\nSummary:");
        System.out.println("  ✓ GREEN dates (clickable): " + greenCount);
        System.out.println("  ✗ GRAY dates (disabled):   " + grayCount);
        System.out.println("\n✓ CALENDAR DISPLAYED\n");
    }

    private void selectDate(LocalDate date) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║         DATE SELECTED: " + date + "                          ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        
        selectedDate = date;
        selectedSlot = null;
        
        if (selectedDateLabel != null) {
            selectedDateLabel.setText("📅 " + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }
        
        if (selectedTimeLabel != null) {
            selectedTimeLabel.setText("⏰ Sélectionnez un créneau");
        }
        
        if (reserveButton != null) {
            reserveButton.setDisable(true);
        }
        
        displayDays();
        displaySlots(date);
        
        showMessage("✓ Date sélectionnée: " + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), "success");
    }

    private void displaySlots(LocalDate date) {
        System.out.println("Displaying slots for: " + date);
        
        if (timeSlotContainer == null) {
            System.err.println("✗ ERROR: timeSlotContainer is NULL!");
            return;
        }
        
        timeSlotContainer.getChildren().clear();

        List<Disponibilite> slots = slotsCache.getOrDefault(date, new ArrayList<>());
        System.out.println("Found " + slots.size() + " slots");

        if (slots.isEmpty()) {
            Label label = new Label("❌ Aucun créneau disponible");
            label.setStyle("-fx-text-fill: #ef4444;");
            timeSlotContainer.getChildren().add(label);
            return;
        }

        slots.sort((a, b) -> a.getHeureDebut().compareTo(b.getHeureDebut()));

        Label count = new Label("📍 " + slots.size() + " créneau(x) disponible(s)");
        count.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11;");
        timeSlotContainer.getChildren().add(count);

        for (Disponibilite slot : slots) {
            // ✅ Vérifier si le créneau est dans le passé
            boolean isSlotInPast = SessionTimeValidator.isSlotInPast(slot);
            
            HBox box = new HBox(10);
            box.setPadding(new Insets(10));
            box.setStyle("-fx-border-color: #e5e7eb; -fx-border-radius: 6; -fx-background-color: #f9fafb;");
            box.setAlignment(Pos.CENTER_LEFT);

            Label time = new Label("⏰ " + slot.getFormattedTimeRange());
            time.setStyle("-fx-font-size: 12; -fx-font-weight: bold;");
            time.setPrefWidth(100);

            Label duration = new Label(slot.getDurationMinutes() + " min");
            duration.setStyle("-fx-font-size: 11; -fx-text-fill: #6b7280;");

            Button select = new Button("Sélectionner");
            select.setStyle("-fx-padding: 6 12; -fx-font-size: 11;");
            
            // ✅ Si le créneau est dans le passé, le désactiver visuellement
            if (isSlotInPast) {
                box.setStyle("-fx-border-color: #d1d5db; -fx-border-radius: 6; -fx-background-color: #f3f4f6; -fx-opacity: 0.6;");
                time.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #9ca3af;");
                duration.setStyle("-fx-font-size: 11; -fx-text-fill: #d1d5db;");
                select.setDisable(true);
                select.setStyle("-fx-padding: 6 12; -fx-font-size: 11; -fx-background-color: #d1d5db; -fx-text-fill: #6b7280;");
                
                // Ajouter un badge "Passé"
                Label pastBadge = new Label("⏳ Passé");
                pastBadge.setStyle("-fx-font-size: 10; -fx-text-fill: #ef4444; -fx-font-weight: bold;");
                box.getChildren().add(pastBadge);
                
                System.out.println("[CalendarCoachController] Slot " + slot.getFormattedTimeRange() + " is in the past - disabled");
            } else {
                final Disponibilite slotRef = slot;
                select.setOnAction(e -> {
                    System.out.println(">>> SLOT SELECTED: " + slotRef.getFormattedTimeRange() + " <<<\n");
                    selectSlot(slotRef);
                });

                box.setOnMouseEntered(e -> box.setStyle("-fx-border-color: #3b82f6; -fx-background-color: #eff6ff;"));
                box.setOnMouseExited(e -> {
                    if (!slot.equals(selectedSlot)) {
                        box.setStyle("-fx-border-color: #e5e7eb; -fx-background-color: #f9fafb;");
                    }
                });
            }

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            box.getChildren().addAll(time, duration, spacer, select);
            timeSlotContainer.getChildren().add(box);
        }
        
        System.out.println("✓ Slots displayed\n");
    }

    private void selectSlot(Disponibilite slot) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║         SLOT SELECTED: " + slot.getFormattedTimeRange() + "                    ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        
        selectedSlot = slot;
        
        if (selectedTimeLabel != null) {
            selectedTimeLabel.setText("⏰ " + slot.getFormattedTimeRange());
        }
        
        if (reserveButton != null) {
            reserveButton.setDisable(false);
        }
        
        showMessage("✓ Créneau sélectionné: " + slot.getFormattedTimeRange(), "success");
    }

    private void reserve() {
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║         RESERVATION REQUESTED                              ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        
        if (selectedSlot == null || selectedDate == null) {
            System.err.println("✗ ERROR: selectedSlot or selectedDate is null");
            showMessage("❌ Sélectionnez une date et un créneau", "error");
            return;
        }

        System.out.println("Coach: " + coachName);
        System.out.println("Date: " + selectedDate);
        System.out.println("Time: " + selectedSlot.getFormattedTimeRange());
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmer");
        alert.setHeaderText("Réserver une session");
        alert.setContentText("Coach: " + coachName + "\nDate: " + selectedDate + "\nHeure: " + selectedSlot.getFormattedTimeRange());

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            System.out.println("✓ User confirmed reservation");
            
            try {
                // Créer une demande de coaching avec le créneau sélectionné
                createCoachingRequest();
                
                System.out.println("✓ Reservation successful!");
                showMessage("✓ Session réservée!", "success");
                selectedDate = null;
                selectedSlot = null;
                if (selectedDateLabel != null) {
                    selectedDateLabel.setText("Sélectionnez une date");
                }
                if (selectedTimeLabel != null) {
                    selectedTimeLabel.setText("Sélectionnez un créneau");
                }
                if (reserveButton != null) {
                    reserveButton.setDisable(true);
                }
                loadCalendar();
                updateSessionCount();
            } catch (PastSessionException e) {
                System.err.println("✗ Past session error: " + e.getMessage());
                showMessage("❌ " + e.getMessage(), "error");
            } catch (Exception e) {
                System.err.println("✗ Error creating coaching request: " + e.getMessage());
                e.printStackTrace();
                showMessage("❌ Erreur lors de la création de la demande: " + e.getMessage(), "error");
            }
        } else {
            System.out.println("✗ User cancelled reservation");
        }
    }
    
    /**
     * Crée une demande de coaching avec le créneau sélectionné.
     * Valide que le créneau n'est pas dans le passé avant de créer la demande.
     */
    private void createCoachingRequest() throws SQLException, PastSessionException {
        // Récupérer l'utilisateur actuel
        Optional<User> currentUser = AppSession.getCurrentUser();
        if (!currentUser.isPresent()) {
            throw new IllegalArgumentException("Utilisateur non connecté");
        }
        
        User user = currentUser.get();
        int userId = user.getId();
        
        System.out.println("[CalendarCoachController] Creating coaching request for user: " + userId);
        System.out.println("[CalendarCoachController] Coach ID: " + coachId);
        System.out.println("[CalendarCoachController] Selected date: " + selectedDate);
        System.out.println("[CalendarCoachController] Selected slot: " + selectedSlot.getFormattedTimeRange());
        
        // ✅ VÉRIFICATION 1: Vérifier que le créneau n'est pas dans le passé
        try {
            SessionTimeValidator.validateSlotNotInPast(selectedSlot);
            System.out.println("[CalendarCoachController] Slot time validation passed");
        } catch (PastSessionException e) {
            System.err.println("[CalendarCoachController] Slot time validation failed: " + e.getMessage());
            throw e;
        }
        
        // ✅ VÉRIFICATION 2: Vérifier la limite de 3 sessions
        CoachingRequestService requestService = new CoachingRequestService();
        int futureRequestsCount = requestService.countFutureRequests(userId);
        
        System.out.println("[CalendarCoachController] User has " + futureRequestsCount + " future requests");
        
        if (futureRequestsCount >= 3) {
            System.err.println("[CalendarCoachController] User has reached the limit of 3 future sessions");
            throw new IllegalArgumentException(
                "Vous avez atteint la limite de 3 sessions futures. " +
                "Veuillez terminer ou annuler une session avant de réserver à nouveau."
            );
        }
        
        // Créer l'objet CoachingRequest
        CoachingRequest request = new CoachingRequest();
        request.setUserId(userId);
        request.setCoachId(coachId);
        request.setMessage("Demande de session de coaching pour le créneau: " + selectedSlot.getFormattedTimeRange());
        request.setPriority(CoachingRequest.PRIORITY_NORMAL);
        request.setStatus(CoachingRequest.STATUS_PENDING);
        
        System.out.println("[CalendarCoachController] Proposed time: " + selectedDate + " " + selectedSlot.getFormattedTimeRange());
        
        // Sauvegarder la demande
        requestService.create(request);
        
        System.out.println("[CalendarCoachController] Coaching request created successfully!");
        System.out.println("[CalendarCoachController] Request ID: " + request.getId());
    }

    private void previousMonth() {
        System.out.println("\n>>> PREVIOUS MONTH <<<\n");
        currentMonth = currentMonth.minusMonths(1);
        selectedDate = null;
        selectedSlot = null;
        if (selectedDateLabel != null) {
            selectedDateLabel.setText("Sélectionnez une date");
        }
        if (selectedTimeLabel != null) {
            selectedTimeLabel.setText("Sélectionnez un créneau");
        }
        if (reserveButton != null) {
            reserveButton.setDisable(true);
        }
        loadCalendar();
    }

    private void nextMonth() {
        System.out.println("\n>>> NEXT MONTH <<<\n");
        currentMonth = currentMonth.plusMonths(1);
        selectedDate = null;
        selectedSlot = null;
        if (selectedDateLabel != null) {
            selectedDateLabel.setText("Sélectionnez une date");
        }
        if (selectedTimeLabel != null) {
            selectedTimeLabel.setText("Sélectionnez un créneau");
        }
        if (reserveButton != null) {
            reserveButton.setDisable(true);
        }
        loadCalendar();
    }

    private void showMessage(String msg, String type) {
        System.out.println("MESSAGE: " + msg + " (" + type + ")");
        
        if (messageLabel != null) {
            messageLabel.setText(msg);
            if ("success".equals(type)) {
                messageLabel.setStyle("-fx-text-fill: #10b981;");
            } else {
                messageLabel.setStyle("-fx-text-fill: #ef4444;");
            }
        }
    }

    /**
     * Met à jour l'affichage du compteur de sessions futures.
     * Affiche le nombre de sessions réservées et change la couleur en fonction du statut.
     */
    private void updateSessionCount() {
        try {
            Optional<User> currentUser = AppSession.getCurrentUser();
            if (!currentUser.isPresent()) {
                return;
            }
            
            CoachingRequestService requestService = new CoachingRequestService();
            int futureCount = requestService.countFutureRequests(currentUser.get().getId());
            int remaining = requestService.getRemainingSlots(currentUser.get().getId());
            
            if (sessionCountLabel != null) {
                sessionCountLabel.setText(futureCount + "/3");
                
                // Changer la couleur en fonction du nombre de sessions
                if (futureCount == 0) {
                    sessionCountLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #10b981;"); // Vert
                } else if (futureCount < 3) {
                    sessionCountLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #f59e0b;"); // Orange
                } else {
                    sessionCountLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #ef4444;"); // Rouge
                }
            }
            
            if (remainingSlotsLabel != null) {
                if (remaining > 0) {
                    remainingSlotsLabel.setText("Vous pouvez réserver " + remaining + " session(s)");
                    remainingSlotsLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #059669; -fx-padding: 0 0 0 10;");
                } else {
                    remainingSlotsLabel.setText("Limite atteinte - Vous ne pouvez plus réserver");
                    remainingSlotsLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #dc2626; -fx-padding: 0 0 0 10;");
                }
            }
            
            // Mettre à jour l'état du bouton
            updateButtonState(remaining > 0);
            
        } catch (SQLException e) {
            System.err.println("[CalendarCoachController] Error updating session count: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Met à jour l'état du bouton "Réserver" en fonction de la limite.
     */
    private void updateButtonState(boolean canBook) {
        if (reserveButton != null) {
            if (canBook) {
                reserveButton.setDisable(false);
                reserveButton.setStyle("-fx-padding: 10 20; -fx-font-size: 12; -fx-background-color: #10b981; -fx-text-fill: white;");
            } else {
                reserveButton.setDisable(true);
                reserveButton.setStyle("-fx-padding: 10 20; -fx-font-size: 12; -fx-background-color: #d1d5db; -fx-text-fill: #6b7280;");
            }
        }
    }
}
