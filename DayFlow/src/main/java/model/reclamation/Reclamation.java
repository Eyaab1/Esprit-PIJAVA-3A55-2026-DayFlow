package model.reclamation;

import enums.ReclamationType;
import enums.ReclamationStatus;
import model.user.User;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;


public class Reclamation {

    private Integer id;
    private String content;
    private ReclamationType type;
    private ReclamationStatus status;
    private LocalDateTime createdAt;
    private String photoPath;
    
    private Integer userId;
    
    private User user;
    private final List<Response> responses = new ArrayList<>();

    public Reclamation() {
        this.createdAt = LocalDateTime.now();
        this.status = ReclamationStatus.PENDING;
    }

    public static void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Le contenu ne peut pas être vide.");
        }
        if (content.trim().length() < 10) {
            throw new IllegalArgumentException("La réclamation doit contenir au moins 10 caractères.");
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

    public ReclamationType getType() {
        return type;
    }

    public void setType(ReclamationType type) {
        if (type == null) {
            throw new IllegalArgumentException("Le type de réclamation est obligatoire.");
        }
        this.type = type;
    }

    public ReclamationStatus getStatus() {
        return status;
    }

    public void setStatus(ReclamationStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Le statut est obligatoire.");
        }
        this.status = status;
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

    public String getPhotoPath() {
        return photoPath;
    }
    

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        if (userId == null) {
            this.userId = null;
            this.user = null;
            return;
        }
        if (userId <= 0) {
            throw new IllegalArgumentException("L'utilisateur est obligatoire.");
        }
        this.userId = userId;
        if (this.user != null && this.user.getId() != null && !this.user.getId().equals(userId)) {
            this.user = null;
        }
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        if (this.user == user) {
            return;
        }
        if (this.user != null) {
            this.user.getReclamations().remove(this);
        }
        this.user = user;
        if (user != null) {
            if (user.getId() != null) {
                this.userId = user.getId();
            }
            if (!user.getReclamations().contains(this)) {
                user.getReclamations().add(this);
            }
        } else {
            this.userId = null;
        }
    }

    public List<Response> getResponses() {
        return Collections.unmodifiableList(responses);
    }

    public void addResponse(Response response) {
        if (response == null) {
            return;
        }
        if (!responses.contains(response)) {
            responses.add(response);
            response.setReclamation(this);
        }
    }

    public void removeResponse(Response response) {
        if (response == null) {
            return;
        }
        if (responses.remove(response) && response.getReclamation() == this) {
            response.setReclamation(null);
        }
    }
}
