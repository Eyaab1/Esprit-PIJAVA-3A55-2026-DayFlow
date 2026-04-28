package services.profile;

import enums.PostStatus;
import model.profile.ProfileAnalysisHistoryItem;
import model.profile.ProfileAnalysisResult;
import model.profile.ProfileSnapshot;
import model.user.User;
import services.account.UserService;
import services.interaction.PostService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ProfileAnalyzerService {

    private final ProfileScoreService scoreService;
    private final ProfileAnalysisRepository profileAnalysisRepository;
    private final UserService userService;
    private final PostService postService;

    public ProfileAnalyzerService() {
        this(new ProfileScoreService(), new ProfileAnalysisRepository(), new UserService(), new PostService());
    }

    public ProfileAnalyzerService(ProfileScoreService scoreService,
                                  ProfileAnalysisRepository profileAnalysisRepository,
                                  UserService userService,
                                  PostService postService) {
        this.scoreService = scoreService;
        this.profileAnalysisRepository = profileAnalysisRepository;
        this.userService = userService;
        this.postService = postService;
    }

    public ProfileAnalysisResult analyzeAndSave(int userId, ProfileSnapshot snapshot) throws SQLException {
        ProfileAnalysisResult result = scoreService.analyze(snapshot);
        profileAnalysisRepository.saveAnalysis(userId, result);
        return result;
    }

    public List<String> readRecommendationsJson(String recommendationsJson) {
        return profileAnalysisRepository.parseRecommendations(recommendationsJson);
    }

    public ProfileAnalysisResult analyzeCurrentUserProfile(int userId) throws SQLException {
        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        User u = userOpt.get();
        int activityCount = postService.countPostsByAuthorAndStatus(userId, PostStatus.PUBLISHED);
        List<String> skills = listFromProfile(u.getSpecialities(), u.getSpeciality());
        List<String> preferences = listFromProfile(List.of(), u.getAvailability());
        ProfileSnapshot snapshot = new ProfileSnapshot(u.getBio(), skills, preferences, activityCount);
        return analyzeAndSave(userId, snapshot);
    }

    public List<ProfileAnalysisHistoryItem> history(int userId, Integer minScore, LocalDate fromDate, String sortBy)
            throws SQLException {
        return profileAnalysisRepository.findByUserIdFiltered(userId, minScore, fromDate, sortBy);
    }

    private static List<String> listFromProfile(List<String> list, String fallbackCsv) {
        if (list != null && !list.isEmpty()) {
            return list;
        }
        if (fallbackCsv == null || fallbackCsv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(fallbackCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }
}
