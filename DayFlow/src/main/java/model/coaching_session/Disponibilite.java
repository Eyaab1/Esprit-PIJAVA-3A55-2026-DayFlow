package model.coaching_session;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * Model for Coach Availability (Disponibilite)
 * Represents available time slots for coaches
 */
public class Disponibilite {
    private int id;
    private int coachId;
    private LocalDate date;
    private LocalTime heureDebut;
    private LocalTime heureFin;
    private String statut; // "disponible", "reserve", "annulee"
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Default constructor
     */
    public Disponibilite() {
    }

    /**
     * Constructor with main fields
     */
    public Disponibilite(int coachId, LocalDate date, LocalTime heureDebut, LocalTime heureFin) {
        this.coachId = coachId;
        this.date = date;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
        this.statut = "disponible";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Constructor with all fields
     */
    public Disponibilite(int id, int coachId, LocalDate date, LocalTime heureDebut, 
                        LocalTime heureFin, String statut) {
        this.id = id;
        this.coachId = coachId;
        this.date = date;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
        this.statut = statut;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCoachId() {
        return coachId;
    }

    public void setCoachId(int coachId) {
        this.coachId = coachId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getHeureDebut() {
        return heureDebut;
    }

    public void setHeureDebut(LocalTime heureDebut) {
        this.heureDebut = heureDebut;
    }

    public LocalTime getHeureFin() {
        return heureFin;
    }

    public void setHeureFin(LocalTime heureFin) {
        this.heureFin = heureFin;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Check if this slot is available (not reserved)
     */
    public boolean isAvailable() {
        return "disponible".equalsIgnoreCase(statut);
    }

    /**
     * Check if this slot is reserved
     */
    public boolean isReserved() {
        return "reserve".equalsIgnoreCase(statut);
    }

    /**
     * Get duration in minutes
     */
    public int getDurationMinutes() {
        return (int) java.time.temporal.ChronoUnit.MINUTES.between(heureDebut, heureFin);
    }

    /**
     * Get formatted time range (e.g., "09:00 - 10:00")
     */
    public String getFormattedTimeRange() {
        return String.format("%02d:%02d - %02d:%02d", 
            heureDebut.getHour(), heureDebut.getMinute(),
            heureFin.getHour(), heureFin.getMinute());
    }

    /**
     * Get formatted date and time
     */
    public String getFormattedDateTime() {
        return String.format("%s %s", date, getFormattedTimeRange());
    }

    @Override
    public String toString() {
        return "Disponibilite{" +
                "id=" + id +
                ", coachId=" + coachId +
                ", date=" + date +
                ", heureDebut=" + heureDebut +
                ", heureFin=" + heureFin +
                ", statut='" + statut + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Disponibilite that = (Disponibilite) o;

        if (id != that.id) return false;
        if (coachId != that.coachId) return false;
        if (!date.equals(that.date)) return false;
        if (!heureDebut.equals(that.heureDebut)) return false;
        if (!heureFin.equals(that.heureFin)) return false;
        return statut.equals(that.statut);
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + coachId;
        result = 31 * result + date.hashCode();
        result = 31 * result + heureDebut.hashCode();
        result = 31 * result + heureFin.hashCode();
        result = 31 * result + statut.hashCode();
        return result;
    }
}
