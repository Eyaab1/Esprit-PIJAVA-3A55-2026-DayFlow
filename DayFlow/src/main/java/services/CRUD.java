package services;

import java.sql.SQLException;

/**
 * Contrat CRUD générique pour les services (création, insertion, mise à jour, suppression).
 *
 * @param <T> type de l’entité
 * @param <K> type de la clé primaire (ex. {@link Integer})
 */
public interface CRUD<T, K> {

    /** Crée l’entité en persistance (souvent équivalent à {@link #insert}). */
    void create(T entity) throws SQLException;

    /** Insère une nouvelle ligne (SQL INSERT). */
    void insert(T entity) throws SQLException;

    /** Met à jour une ligne existante (SQL UPDATE) ; l’entité doit porter la clé. */
    void update(T entity) throws SQLException;

    /** Supprime la ligne identifiée par {@code id} (SQL DELETE). */
    void delete(K id) throws SQLException;
}
