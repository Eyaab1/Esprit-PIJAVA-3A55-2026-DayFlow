package services.coaching_session;

import model.coaching_session.Disponibilite;
import repository.coaching_session.DisponibiliteRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing coach availability (Disponibilite)
 * Handles business logic for availability slots and reservations
 */
public class DisponibiliteService {
    private DisponibiliteRepository disponibiliteRepository;

    public DisponibiliteService() {
        this.disponibiliteRepository = new DisponibiliteRepository();
    }

    /**
     * Get all availabilities for a coach
     */
    public List<Disponibilite> getDisponibilitesByCoach(int coachId) {
        return disponibiliteRepository.getDisponibilitesByCoach(coachId);
    }

    /**
     * Get availabilities for a coach within a date range
     */
    public List<Disponibilite> getDisponibilitesByCoachAndDateRange(int coachId, LocalDate startDate, LocalDate endDate) {
        return disponibiliteRepository.getDisponibilitesByCoachAndDateRange(coachId, startDate, endDate);
    }

    /**
     * Get available slots for a coach (not reserved)
     */
    public List<Disponibilite> getAvailableSlots(int coachId) {
        return disponibiliteRepository.getAvailableSlots(coachId);
    }

    /**
     * Get available slots for a coach on a specific date
     */
    public List<Disponibilite> getAvailableSlotsByDate(int coachId, LocalDate date) {
        return disponibiliteRepository.getAvailableSlotsByDate(coachId, date);
    }

    /**
     * Get available slots grouped by date
     */
    public java.util.Map<LocalDate, List<Disponibilite>> getAvailableSlotsByDateMap(int coachId) {
        List<Disponibilite> slots = getAvailableSlots(coachId);
        return slots.stream()
                .collect(Collectors.groupingBy(Disponibilite::getDate));
    }

    /**
     * Get available slots for a week starting from a specific date
     */
    public List<Disponibilite> getAvailableSlotsForWeek(int coachId, LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        return getDisponibilitesByCoachAndDateRange(coachId, weekStart, weekEnd)
                .stream()
                .filter(Disponibilite::isAvailable)
                .collect(Collectors.toList());
    }

    /**
     * Get available slots for a month starting from a specific date
     */
    public List<Disponibilite> getAvailableSlotsForMonth(int coachId, LocalDate monthStart) {
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
        return getDisponibilitesByCoachAndDateRange(coachId, monthStart, monthEnd)
                .stream()
                .filter(Disponibilite::isAvailable)
                .collect(Collectors.toList());
    }

    /**
     * Create a new availability slot
     */
    public boolean createDisponibilite(Disponibilite disponibilite) {
        // Validate
        if (!validateDisponibilite(disponibilite)) {
            return false;
        }

        // Check for overlapping slots
        if (hasOverlappingSlots(disponibilite)) {
            System.err.println("Overlapping slots detected");
            return false;
        }

        return disponibiliteRepository.createDisponibilite(disponibilite);
    }

    /**
     * Create multiple availability slots
     */
    public boolean createMultipleDisponibilites(List<Disponibilite> disponibilites) {
        for (Disponibilite disponibilite : disponibilites) {
            if (!createDisponibilite(disponibilite)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Update availability status
     */
    public boolean updateDisponibiliteStatus(int id, String newStatus) {
        // Validate status
        if (!isValidStatus(newStatus)) {
            System.err.println("Invalid status: " + newStatus);
            return false;
        }

        return disponibiliteRepository.updateDisponibiliteStatus(id, newStatus);
    }

    /**
     * Reserve a slot (mark as reserved)
     */
    public boolean reserveSlot(int disponibiliteId) {
        Disponibilite disponibilite = disponibiliteRepository.getDisponibiliteById(disponibiliteId);

        if (disponibilite == null) {
            System.err.println("Disponibilite not found");
            return false;
        }

        if (!disponibilite.isAvailable()) {
            System.err.println("Slot is not available");
            return false;
        }

        return updateDisponibiliteStatus(disponibiliteId, "reserve");
    }

    /**
     * Release a slot (mark as available again)
     */
    public boolean releaseSlot(int disponibiliteId) {
        return updateDisponibiliteStatus(disponibiliteId, "disponible");
    }

    /**
     * Check if a slot is available
     */
    public boolean isSlotAvailable(int coachId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        return disponibiliteRepository.isSlotAvailable(coachId, date, startTime, endTime);
    }

    /**
     * Get count of available slots for a coach
     */
    public int getAvailableSlotsCount(int coachId) {
        return disponibiliteRepository.getAvailableSlotsCount(coachId);
    }

    /**
     * Check if coach has any available slots
     */
    public boolean hasAvailableSlots(int coachId) {
        return getAvailableSlotsCount(coachId) > 0;
    }

    /**
     * Get next available slot for a coach
     */
    public Disponibilite getNextAvailableSlot(int coachId) {
        List<Disponibilite> slots = getAvailableSlots(coachId);
        return slots.isEmpty() ? null : slots.get(0);
    }

    /**
     * Validate disponibilite
     */
    private boolean validateDisponibilite(Disponibilite disponibilite) {
        // Check required fields
        if (disponibilite.getCoachId() <= 0) {
            System.err.println("Invalid coach ID");
            return false;
        }

        if (disponibilite.getDate() == null) {
            System.err.println("Date is required");
            return false;
        }

        if (disponibilite.getHeureDebut() == null || disponibilite.getHeureFin() == null) {
            System.err.println("Start and end times are required");
            return false;
        }

        // Check that start time is before end time
        if (!disponibilite.getHeureDebut().isBefore(disponibilite.getHeureFin())) {
            System.err.println("Start time must be before end time");
            return false;
        }

        // Check that date is not in the past
        if (disponibilite.getDate().isBefore(LocalDate.now())) {
            System.err.println("Cannot create availability for past dates");
            return false;
        }

        return true;
    }

    /**
     * Check for overlapping slots
     */
    private boolean hasOverlappingSlots(Disponibilite newSlot) {
        List<Disponibilite> existingSlots = disponibiliteRepository.getDisponibilitesByCoachAndDateRange(
                newSlot.getCoachId(),
                newSlot.getDate(),
                newSlot.getDate()
        );

        for (Disponibilite existing : existingSlots) {
            if (slotsOverlap(existing, newSlot)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if two slots overlap
     */
    private boolean slotsOverlap(Disponibilite slot1, Disponibilite slot2) {
        return slot1.getHeureDebut().isBefore(slot2.getHeureFin()) &&
               slot2.getHeureDebut().isBefore(slot1.getHeureFin());
    }

    /**
     * Validate status
     */
    private boolean isValidStatus(String status) {
        return status.equalsIgnoreCase("disponible") ||
               status.equalsIgnoreCase("reserve") ||
               status.equalsIgnoreCase("annulee");
    }

    /**
     * Get statistics for a coach
     */
    public java.util.Map<String, Integer> getCoachStatistics(int coachId) {
        java.util.Map<String, Integer> stats = new java.util.HashMap<>();
        List<Disponibilite> allSlots = getDisponibilitesByCoach(coachId);

        int totalSlots = allSlots.size();
        int availableSlots = (int) allSlots.stream().filter(Disponibilite::isAvailable).count();
        int reservedSlots = (int) allSlots.stream().filter(Disponibilite::isReserved).count();

        stats.put("total", totalSlots);
        stats.put("available", availableSlots);
        stats.put("reserved", reservedSlots);

        return stats;
    }
}
