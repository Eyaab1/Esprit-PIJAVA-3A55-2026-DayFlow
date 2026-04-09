package model.user;

import java.util.ArrayList;
import java.util.List;

/**
 * Représente la table {@code "user"} (Symfony / Doctrine).
 * Colonnes JDBC attendues : id, first_name, last_name, email, password, google_id, roles,
 * phone_number, age, status, speciality, specialities, availability, rating, review_count,
 * price_per_session, bio, photo_url, profile_picture_name, profile_picture_size.
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

    public User() {
    }

    public User(Integer id, String firstName, String lastName, String email, String password, String googleId,
                List<String> roles, String phoneNumber, Integer age, String status, String speciality,
                List<String> specialities, String availability, Double rating, Integer reviewCount,
                Double pricePerSession, String bio, String photoUrl, String profilePictureName,
                Integer profilePictureSize) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.googleId = googleId;
        this.roles = roles != null ? new ArrayList<>(roles) : new ArrayList<>();
        this.phoneNumber = phoneNumber;
        this.age = age;
        this.status = status;
        this.speciality = speciality;
        this.specialities = specialities;
        this.availability = availability;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.pricePerSession = pricePerSession;
        this.bio = bio;
        this.photoUrl = photoUrl;
        this.profilePictureName = profilePictureName;
        this.profilePictureSize = profilePictureSize;
    }

    public User(String firstName, String lastName, String email, String password, String googleId,
                List<String> roles, String phoneNumber, Integer age, String status, String speciality,
                List<String> specialities, String availability, Double rating, Integer reviewCount,
                Double pricePerSession, String bio, String photoUrl, String profilePictureName,
                Integer profilePictureSize) {
        this(null, firstName, lastName, email, password, googleId, roles, phoneNumber, age, status, speciality,
                specialities, availability, rating, reviewCount, pricePerSession, bio, photoUrl,
                profilePictureName, profilePictureSize);
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

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles != null ? roles : new ArrayList<>();
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
        return specialities;
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
}
