package model.reclamation;

import java.time.LocalDateTime;
import java.util.Objects;


public class Response {

    private Integer id;
    private String content;
    private LocalDateTime createdAt;
    private Reclamation reclamation;

    public Response() {
        this.createdAt = LocalDateTime.now();
    }

    public static void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("La réponse ne peut pas être vide.");
        }
        if (content.trim().length() < 5) {
            throw new IllegalArgumentException("La réponse doit contenir au moins 5 caractères.");
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        validateContent(content);
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        if (createdAt == null) {
            throw new IllegalArgumentException("La date de création est obligatoire.");
        }
        this.createdAt = createdAt;
    }

    public Reclamation getReclamation() {
        return reclamation;
    }

    public void setReclamation(Reclamation reclamation) {
        this.reclamation = reclamation;
    }

    
    public Integer getReclamationId() {
        return reclamation != null && reclamation.getId() != null ? reclamation.getId() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Response that = (Response) o;
        if (id != null && that.id != null) {
            return id.equals(that.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hashCode(id) : System.identityHashCode(this);
    }
}
