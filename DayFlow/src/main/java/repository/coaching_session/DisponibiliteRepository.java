package repository.coaching_session;

import model.coaching_session.Disponibilite;
import utils.DbConnexion;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for Disponibilite (Coach Availability)
 * Handles database operations for availability slots
 */
public class DisponibiliteRepository {
    private DbConnexion dbConnexion;

    public DisponibiliteRepository() {
        this.dbConnexion = DbConnexion.getInstance();
    }

    /**
     * Get all availabilities for a specific coach
     */
    public List<Disponibilite> getDisponibilitesByCoach(int coachId) {
        List<Disponibilite> disponibilites = new ArrayList<>();
        String query = "SELECT * FROM disponibilite WHERE coach_id = ? ORDER BY date ASC, heure_debut ASC";

        try {
            Connection conn = DbConnexion.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, coachId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                disponibilites.add(mapResultSetToDisponibilite(rs));
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            System.err.println("Error fetching disponibilites for coach " + coachId + ": " + e.getMessage());
        }

        return disponibilites;
    }

    /**
     * Get availabilities for a coach within a date range
     */
    public List<Disponibilite> getDisponibilitesByCoachAndDateRange(int coachId, LocalDate startDate, LocalDate endDate) {
        List<Disponibilite> disponibilites = new ArrayList<>();
        String query = "SELECT * FROM disponibilite WHERE coach_id = ? AND date BETWEEN ? AND ? ORDER BY date ASC, heure_debut ASC";

        try {
            Connection conn = DbConnexion.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, coachId);
            stmt.setDate(2, Date.valueOf(startDate));
            stmt.setDate(3, Date.valueOf(endDate));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                disponibilites.add(mapResultSetToDisponibilite(rs));
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            System.err.println("Error fetching disponibilites: " + e.getMessage());
        }

        return disponibilites;
    }

    /**
     * Get available slots for a coach (not reserved)
     */
    public List<Disponibilite> getAvailableSlots(int coachId) {
        List<Disponibilite> slots = new ArrayList<>();
        String query = "SELECT * FROM disponibilite WHERE coach_id = ? AND statut = 'disponible' ORDER BY date ASC, heure_debut ASC";

        try {
            Connection conn = DbConnexion.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, coachId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                slots.add(mapResultSetToDisponibilite(rs));
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            System.err.println("Error fetching available slots: " + e.getMessage());
        }

        return slots;
    }

    /**
     * Get available slots for a coach on a specific date
     */
    public List<Disponibilite> getAvailableSlotsByDate(int coachId, LocalDate date) {
        List<Disponibilite> slots = new ArrayList<>();
        String query = "SELECT * FROM disponibilite WHERE coach_id = ? AND date = ? AND statut = 'disponible' ORDER BY heure_debut ASC";

        try {
            Connection conn = DbConnexion.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, coachId);
            stmt.setDate(2, Date.valueOf(date));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                slots.add(mapResultSetToDisponibilite(rs));
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            System.err.println("Error fetching available slots for date: " + e.getMessage());
        }

        return slots;
    }

    /**
     * Get a specific disponibilite by ID
     */
    public Disponibilite getDisponibiliteById(int id) {
        String query = "SELECT * FROM disponibilite WHERE id = ?";

        try {
            Connection conn = DbConnexion.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Disponibilite result = mapResultSetToDisponibilite(rs);
                rs.close();
                stmt.close();
                return result;
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            System.err.println("Error fetching disponibilite by ID: " + e.getMessage());
        }

        return null;
    }

    /**
     * Create a new disponibilite
     */
    public boolean createDisponibilite(Disponibilite disponibilite) {
        String query = "INSERT INTO disponibilite (coach_id, date, heure_debut, heure_fin, statut, created_at, updated_at) VALUES (?, ?, ?, ?, ?, NOW(), NOW())";

        try {
            Connection conn = DbConnexion.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, disponibilite.getCoachId());
            stmt.setDate(2, Date.valueOf(disponibilite.getDate()));
            stmt.setTime(3, Time.valueOf(disponibilite.getHeureDebut()));
            stmt.setTime(4, Time.valueOf(disponibilite.getHeureFin()));
            stmt.setString(5, disponibilite.getStatut());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    disponibilite.setId(generatedKeys.getInt(1));
                    generatedKeys.close();
                    stmt.close();
                    return true;
                }
                generatedKeys.close();
            }
            stmt.close();
        } catch (SQLException e) {
            System.err.println("Error creating disponibilite: " + e.getMessage());
        }

        return false;
    }

    /**
     * Update disponibilite status
     */
    public boolean updateDisponibiliteStatus(int id, String newStatus) {
        String query = "UPDATE disponibilite SET statut = ?, updated_at = NOW() WHERE id = ?";

        try {
            Connection conn = DbConnexion.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, newStatus);
            stmt.setInt(2, id);

            boolean result = stmt.executeUpdate() > 0;
            stmt.close();
            return result;
        } catch (SQLException e) {
            System.err.println("Error updating disponibilite status: " + e.getMessage());
        }

        return false;
    }

    /**
     * Update entire disponibilite
     */
    public boolean updateDisponibilite(Disponibilite disponibilite) {
        String query = "UPDATE disponibilite SET coach_id = ?, date = ?, heure_debut = ?, heure_fin = ?, statut = ?, updated_at = NOW() WHERE id = ?";

        try {
            Connection conn = DbConnexion.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, disponibilite.getCoachId());
            stmt.setDate(2, Date.valueOf(disponibilite.getDate()));
            stmt.setTime(3, Time.valueOf(disponibilite.getHeureDebut()));
            stmt.setTime(4, Time.valueOf(disponibilite.getHeureFin()));
            stmt.setString(5, disponibilite.getStatut());
            stmt.setInt(6, disponibilite.getId());

            boolean result = stmt.executeUpdate() > 0;
            stmt.close();
            return result;
        } catch (SQLException e) {
            System.err.println("Error updating disponibilite: " + e.getMessage());
        }

        return false;
    }

    /**
     * Delete a disponibilite
     */
    public boolean deleteDisponibilite(int id) {
        String query = "DELETE FROM disponibilite WHERE id = ?";

        try {
            Connection conn = DbConnexion.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, id);
            boolean result = stmt.executeUpdate() > 0;
            stmt.close();
            return result;
        } catch (SQLException e) {
            System.err.println("Error deleting disponibilite: " + e.getMessage());
        }

        return false;
    }

    /**
     * Check if a slot is available (not reserved)
     */
    public boolean isSlotAvailable(int coachId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        String query = "SELECT COUNT(*) FROM disponibilite WHERE coach_id = ? AND date = ? AND heure_debut = ? AND heure_fin = ? AND statut = 'disponible'";

        try {
            Connection conn = DbConnexion.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, coachId);
            stmt.setDate(2, Date.valueOf(date));
            stmt.setTime(3, Time.valueOf(startTime));
            stmt.setTime(4, Time.valueOf(endTime));

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                boolean result = rs.getInt(1) > 0;
                rs.close();
                stmt.close();
                return result;
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            System.err.println("Error checking slot availability: " + e.getMessage());
        }

        return false;
    }

    /**
     * Get count of available slots for a coach
     */
    public int getAvailableSlotsCount(int coachId) {
        String query = "SELECT COUNT(*) FROM disponibilite WHERE coach_id = ? AND statut = 'disponible'";

        try {
            Connection conn = DbConnexion.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, coachId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int result = rs.getInt(1);
                rs.close();
                stmt.close();
                return result;
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            System.err.println("Error counting available slots: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Map ResultSet to Disponibilite object
     */
    private Disponibilite mapResultSetToDisponibilite(ResultSet rs) throws SQLException {
        Disponibilite disponibilite = new Disponibilite();
        disponibilite.setId(rs.getInt("id"));
        disponibilite.setCoachId(rs.getInt("coach_id"));
        disponibilite.setDate(rs.getDate("date").toLocalDate());
        disponibilite.setHeureDebut(rs.getTime("heure_debut").toLocalTime());
        disponibilite.setHeureFin(rs.getTime("heure_fin").toLocalTime());
        disponibilite.setStatut(rs.getString("statut"));
        disponibilite.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        disponibilite.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return disponibilite;
    }
}
