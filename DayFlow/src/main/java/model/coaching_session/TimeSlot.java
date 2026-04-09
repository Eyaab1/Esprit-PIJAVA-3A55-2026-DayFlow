package model.coaching_session;

/**
 * Créneau horaire (ManyToOne depuis {@link CoachingRequest} côté Symfony).
 * Étendre avec start/end, coach, etc. selon votre schéma BD.
 */
public class TimeSlot {

    private int id;

    public TimeSlot() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        if (id < 0) {
            throw new IllegalArgumentException("ID invalide");
        }
        this.id = id;
    }
}
