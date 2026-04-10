package model.user;

import enums.UserRole;
import model.goals_activity_management.Goal;
import model.goals_activity_management.GoalParticipation;
import model.interaction.Comment;
import model.interaction.Post;
import model.interaction.PostLike;
import model.reclamation.Reclamation;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Équivalent de {@code App\Entity\User} (Symfony / Doctrine) avec relations objet.
 * Colonnes JDBC usuelles : identité, profil, rôles, coach, média, badges, activité, onboarding, etc.
 */
public class User {

    public static final String TABLE = "user";

    private Integer id;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String googleId;
    private List<String> roles = new ArrayList<>();
    private String phoneNumber;
    private Integer age;
    private String status;
    private String speciality;
    private List<String> specialities;
    private String availability;
    private Double rating;
    private Integer reviewCount;
    private Double pricePerSession;
    private String bio;
    private String photoUrl;
    private String profilePictureName;
    private Integer profilePictureSize;

    private List<String> badges = new ArrayList<>();
    private Boolean respondsQuickly = false;
    private Integer totalSessions = 0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastActivityAt;

    private String preferredLanguage;

    private String archetypeShortBio;
    private String archetypeName;
    private String archetypeDescription;
    private Map<String, Object> archetypeData;
    private Map<String, Object> onboardingAnswers;
    private boolean onboarded;

    private final List<Post> posts = new ArrayList<>();
    private final List<Comment> comments = new ArrayList<>();
    private final List<PostLike> postLikes = new ArrayList<>();
    private final List<Goal> goals = new ArrayList<>();
    private final List<GoalParticipation> goalParticipations = new ArrayList<>();
    private final List<Reclamation> reclamations = new ArrayList<>();

    public User() {
        this.roles.add(UserRole.USER.getValue());
        // Aligné sur {@code UserService#signUp} / colonne {@code status} (souvent en minuscules en BD).
        this.status = "active";
        this.reviewCount = 0;
        this.totalSessions = 0;
        this.respondsQuickly = false;
        this.onboarded = false;
        this.createdAt = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    /**
     * Rôles uniques (équivalent {@code array_unique} côté Symfony).
     */
    public List<String> getRoles() {
        return new ArrayList<>(new LinkedHashSet<>(roles != null ? roles : List.of()));
    }

    public void setRole(UserRole role) {
        this.roles = new ArrayList<>();
        if (role != null) {
            this.roles.add(role.getValue());
        }
    }

    public void setRoles(List<String> roles) {
        if (roles == null) {
            this.roles = new ArrayList<>();
            return;
        }
        this.roles = new ArrayList<>(new LinkedHashSet<>(roles));
    }

    public boolean hasRole(UserRole role) {
        return role != null && roles != null && roles.contains(role.getValue());
    }

    public String getUserIdentifier() {
        return email != null ? email : "";
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSpeciality() {
        return speciality;
    }

    public void setSpeciality(String speciality) {
        this.speciality = speciality;
    }

    public List<String> getSpecialities() {
        return specialities != null ? specialities : new ArrayList<>();
    }

    public void setSpecialities(List<String> specialities) {
        this.specialities = specialities;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    public Double getPricePerSession() {
        return pricePerSession;
    }

    public void setPricePerSession(Double pricePerSession) {
        this.pricePerSession = pricePerSession;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getProfilePictureName() {
        return profilePictureName;
    }

    public void setProfilePictureName(String profilePictureName) {
        this.profilePictureName = profilePictureName;
    }

    public Integer getProfilePictureSize() {
        return profilePictureSize;
    }

    public void setProfilePictureSize(Integer profilePictureSize) {
        this.profilePictureSize = profilePictureSize;
    }

    public String getFormattedProfilePictureSize() {
        if (profilePictureSize == null || profilePictureSize <= 0) {
            return "0 B";
        }
        String[] units = {"B", "KB", "MB", "GB"};
        double size = profilePictureSize;
        int unitIndex = 0;
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        return Math.round(size * 100.0) / 100.0 + " " + units[unitIndex];
    }

    public boolean hasProfilePicture() {
        return profilePictureName != null && !profilePictureName.isBlank();
    }

    public List<String> getBadges() {
        return badges != null ? badges : new ArrayList<>();
    }

    public void setBadges(List<String> badges) {
        this.badges = badges != null ? new ArrayList<>(badges) : new ArrayList<>();
    }

    public void addBadge(String badge) {
        if (badge == null || badge.isBlank()) {
            return;
        }
        if (badges == null) {
            badges = new ArrayList<>();
        }
        if (!badges.contains(badge)) {
            badges.add(badge);
        }
    }

    public Boolean getRespondsQuickly() {
        return respondsQuickly;
    }

    public void setRespondsQuickly(Boolean respondsQuickly) {
        this.respondsQuickly = respondsQuickly != null ? respondsQuickly : false;
    }

    public Integer getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(Integer totalSessions) {
        this.totalSessions = totalSessions != null ? totalSessions : 0;
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

    public LocalDateTime getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(LocalDateTime lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public User updateLastActivity() {
        this.lastActivityAt = LocalDateTime.now();
        return this;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public String getArchetypeShortBio() {
        return archetypeShortBio;
    }

    public void setArchetypeShortBio(String archetypeShortBio) {
        this.archetypeShortBio = archetypeShortBio;
    }

    public String getArchetypeName() {
        return archetypeName;
    }

    public void setArchetypeName(String archetypeName) {
        this.archetypeName = archetypeName;
    }

    public String getArchetypeDescription() {
        return archetypeDescription;
    }

    public void setArchetypeDescription(String archetypeDescription) {
        this.archetypeDescription = archetypeDescription;
    }

    public Map<String, Object> getArchetypeData() {
        return archetypeData;
    }

    public void setArchetypeData(Map<String, Object> archetypeData) {
        this.archetypeData = archetypeData;
    }

    public Map<String, Object> getOnboardingAnswers() {
        return onboardingAnswers;
    }

    public void setOnboardingAnswers(Map<String, Object> onboardingAnswers) {
        this.onboardingAnswers = onboardingAnswers;
    }

    public boolean isOnboarded() {
        return onboarded;
    }

    public void setOnboarded(boolean onboarded) {
        this.onboarded = onboarded;
    }

    public boolean isCoach() {
        return hasRole(UserRole.COACH);
    }

    public boolean isOnline() {
        if (lastActivityAt == null) {
            return false;
        }
        return ChronoUnit.SECONDS.between(lastActivityAt, LocalDateTime.now()) < 300;
    }

    public String getOnlineStatus() {
        if (lastActivityAt == null) {
            return "offline";
        }
        long diffSec = ChronoUnit.SECONDS.between(lastActivityAt, LocalDateTime.now());
        if (diffSec < 300) {
            return "online";
        }
        if (diffSec < 3600) {
            return "away";
        }
        return "offline";
    }

    // —— Posts ——

    public List<Post> getPosts() {
        return posts;
    }

    public void addPost(Post post) {
        if (post == null || posts.contains(post)) {
            return;
        }
        posts.add(post);
        post.setCreatedBy(this);
    }

    public void removePost(Post post) {
        if (post == null) {
            return;
        }
        if (posts.remove(post) && post.getCreatedBy() == this) {
            post.setCreatedBy(null);
        }
    }

    // —— Comments ——

    public List<Comment> getComments() {
        return comments;
    }

    public void addComment(Comment comment) {
        if (comment == null || comments.contains(comment)) {
            return;
        }
        comments.add(comment);
        comment.setCommenter(this);
    }

    public void removeComment(Comment comment) {
        if (comment == null) {
            return;
        }
        if (comments.remove(comment) && comment.getCommenter() == this) {
            comment.setCommenter(null);
        }
    }

    // —— Post likes ——

    public List<PostLike> getPostLikes() {
        return postLikes;
    }

    public void addPostLike(PostLike postLike) {
        if (postLike == null || postLikes.contains(postLike)) {
            return;
        }
        postLikes.add(postLike);
        postLike.setLiker(this);
    }

    public void removePostLike(PostLike postLike) {
        if (postLike == null) {
            return;
        }
        if (postLikes.remove(postLike) && postLike.getLiker() == this) {
            postLike.setLiker(null);
        }
    }

    // —— Goals ——

    public List<Goal> getGoals() {
        return goals;
    }

    public void addGoal(Goal goal) {
        if (goal == null || goals.contains(goal)) {
            return;
        }
        goals.add(goal);
        goal.setUser(this);
    }

    public void removeGoal(Goal goal) {
        if (goal == null) {
            return;
        }
        if (goals.remove(goal) && goal.getUser() == this) {
            goal.setUser(null);
        }
    }

    // —— Goal participations ——

    public List<GoalParticipation> getGoalParticipations() {
        return goalParticipations;
    }

    public void addGoalParticipation(GoalParticipation goalParticipation) {
        if (goalParticipation == null || goalParticipations.contains(goalParticipation)) {
            return;
        }
        goalParticipations.add(goalParticipation);
        goalParticipation.setUser(this);
    }

    public void removeGoalParticipation(GoalParticipation goalParticipation) {
        if (goalParticipation == null) {
            return;
        }
        if (goalParticipations.remove(goalParticipation) && goalParticipation.getUser() == this) {
            goalParticipation.setUser(null);
        }
    }

    // —— Réclamations ——

    public List<Reclamation> getReclamations() {
        return reclamations;
    }

    public void addReclamation(Reclamation reclamation) {
        if (reclamation == null || reclamations.contains(reclamation)) {
            return;
        }
        reclamations.add(reclamation);
        reclamation.setUser(this);
    }

    public void removeReclamation(Reclamation reclamation) {
        if (reclamation == null) {
            return;
        }
        if (reclamations.remove(reclamation) && reclamation.getUser() == this) {
            reclamation.setUser(null);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
