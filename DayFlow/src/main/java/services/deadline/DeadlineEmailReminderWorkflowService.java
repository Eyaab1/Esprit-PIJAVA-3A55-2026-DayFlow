package services.deadline;

import config.AppConfig;
import model.goals_activity_management.Goal;
import model.notification.Notification;
import services.goals_routines.GoalService;
import services.notification.EmailService;
import services.notification.GoalEmailHistoryService;
import services.notification.NotificationService;
import utils.DbConnexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Advanced workflow for goal deadline reminders by email.
 * Includes automatic detection, duplicate prevention, and in-app synchronization.
 */
public class DeadlineEmailReminderWorkflowService {

    private final Connection cnx;
    private GoalService goalService;
    private final EmailService emailService;
    private final GoalEmailHistoryService historyService;
    private final NotificationService notificationService;

    public DeadlineEmailReminderWorkflowService() {
        this.cnx = DbConnexion.getConnection();
        this.goalService = null; // Will be set later to avoid circular dependency
        this.emailService = new EmailService();
        this.historyService = new GoalEmailHistoryService();
        this.notificationService = new NotificationService();
    }

    // Constructor with GoalService parameter to avoid circular dependency
    public DeadlineEmailReminderWorkflowService(GoalService goalService) {
        this.cnx = DbConnexion.getConnection();
        this.goalService = goalService;
        this.emailService = new EmailService();
        this.historyService = new GoalEmailHistoryService();
        this.notificationService = new NotificationService();
    }

    // Setter to inject GoalService and avoid circular dependency
    public void setGoalService(GoalService goalService) {
        this.goalService = goalService;
    }

    public void processGoalDeadlineReminders() {
        try {
            List<Goal> goals = findGoalsEligibleForDeadlineChecks();
            for (Goal goal : goals) {
                if (goal.getDeadline() == null) {
                    continue;
                }
                dispatchGoalReminderIfDue(goal, EmailService.DeadlineReminderType.DEADLINE_7D, Duration.ofDays(7));
                dispatchGoalReminderIfDue(goal, EmailService.DeadlineReminderType.DEADLINE_3D, Duration.ofDays(3));
                dispatchGoalReminderIfDue(goal, EmailService.DeadlineReminderType.DEADLINE_24H, Duration.ofHours(24));
                dispatchCustomExactReminderIfDue(goal);
                dispatchOverdueIfDue(goal);
            }
        } catch (Exception e) {
            System.err.println("[DeadlineEmailReminderWorkflowService] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void handleGoalUpdated(Goal previousGoal, Goal updatedGoal) throws SQLException {
        if (previousGoal == null || updatedGoal == null) {
            return;
        }

        boolean completedNow = !isCompletedStatus(previousGoal.getStatus()) && isCompletedStatus(updatedGoal.getStatus());
        if (completedNow) {
            cancelPendingReminderEmails(updatedGoal.getId());
            sendCompletionCongratulations(updatedGoal);
        }
    }

    public void cancelPendingReminderEmails(int goalId) throws SQLException {
        historyService.deletePendingByGoal(goalId);
    }

    private void dispatchGoalReminderIfDue(Goal goal, EmailService.DeadlineReminderType reminderType,
                                           Duration threshold) throws SQLException {
        if (!isGoalEligibleForReminder(goal)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Duration remaining = Duration.between(now, goal.getDeadline());
        if (remaining.isNegative() || remaining.isZero() || remaining.compareTo(threshold) > 0) {
            return;
        }

        List<UserContact> recipients = getEmailRecipients(goal.getId());
        for (UserContact recipient : recipients) {
            if (historyService.hasEmailBeenSent(goal.getId(), recipient.userId, reminderType, goal.getDeadline())) {
                continue;
            }
            boolean sent = emailService.sendDeadlineReminder(
                    recipient.email,
                    recipient.fullName(),
                    goal,
                    reminderType.getLabel()
            );
            historyService.logEmailAttempt(goal.getId(), recipient.userId, reminderType, goal.getDeadline(),
                    sent, sent ? null : "SMTP send failed");
            if (sent) {
                createInAppNotification(goal, recipient.userId, reminderType);
            } else {
                System.err.println("[DeadlineEmailReminderWorkflowService] Failed to send " + reminderType
                        + " email to user " + recipient.userId + " for goal " + goal.getId());
            }
        }
    }

    private void dispatchOverdueIfDue(Goal goal) throws SQLException {
        if (goal.getDeadline() == null || LocalDateTime.now().isBefore(goal.getDeadline())) {
            return;
        }
        if (isCompletedStatus(goal.getStatus())) {
            return;
        }
        List<UserContact> recipients = getEmailRecipients(goal.getId());
        for (UserContact recipient : recipients) {
            EmailService.DeadlineReminderType type = EmailService.DeadlineReminderType.OVERDUE;
            if (historyService.hasEmailBeenSent(goal.getId(), recipient.userId, type, goal.getDeadline())) {
                continue;
            }
            boolean sent = emailService.sendOverdueReminder(recipient.email, recipient.fullName(), goal);
            historyService.logEmailAttempt(goal.getId(), recipient.userId, type, goal.getDeadline(),
                    sent, sent ? null : "SMTP send failed");
            if (sent) {
                createInAppNotification(goal, recipient.userId, type);
            } else {
                System.err.println("[DeadlineEmailReminderWorkflowService] Failed to send OVERDUE email to user "
                        + recipient.userId + " for goal " + goal.getId());
            }
        }
    }

    private void dispatchCustomExactReminderIfDue(Goal goal) throws SQLException {
        if (!goal.isEmailReminderEnabled() || goal.getEmailReminderAt() == null) {
            return;
        }
        if (!isGoalEligibleForReminder(goal)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(goal.getEmailReminderAt())) {
            return;
        }
        List<UserContact> recipients = getEmailRecipients(goal.getId());
        for (UserContact recipient : recipients) {
            EmailService.DeadlineReminderType type = EmailService.DeadlineReminderType.CUSTOM_EXACT;
            LocalDateTime snapshot = goal.getEmailReminderAt();
            if (historyService.hasEmailBeenSent(goal.getId(), recipient.userId, type, snapshot)) {
                continue;
            }
            boolean sent = emailService.sendDeadlineReminder(
                    recipient.email,
                    recipient.fullName(),
                    goal,
                    "custom time"
            );
            historyService.logEmailAttempt(goal.getId(), recipient.userId, type, snapshot,
                    sent, sent ? null : "SMTP send failed");
            if (sent) {
                createInAppNotification(goal, recipient.userId, type);
            } else {
                System.err.println("[DeadlineEmailReminderWorkflowService] Failed to send completion email to user "
                        + recipient.userId + " for goal " + goal.getId());
            }
        }
    }

    private void sendCompletionCongratulations(Goal goal) throws SQLException {
        List<UserContact> recipients = getEmailRecipients(goal.getId());
        for (UserContact recipient : recipients) {
            EmailService.DeadlineReminderType type = EmailService.DeadlineReminderType.COMPLETION_CONGRATS;
            LocalDateTime snapshot = goal.getDeadline() != null ? goal.getDeadline() : LocalDateTime.now();
            if (historyService.hasEmailBeenSent(goal.getId(), recipient.userId, type, snapshot)) {
                continue;
            }
            boolean sent = emailService.sendCompletionCongratulations(recipient.email, recipient.fullName(), goal);
            historyService.logEmailAttempt(goal.getId(), recipient.userId, type, snapshot,
                    sent, sent ? null : "SMTP send failed");
            if (sent) {
                createInAppNotification(goal, recipient.userId, type);
            } else {
                System.err.println("[DeadlineEmailReminderWorkflowService] Failed to send CUSTOM_EXACT email to user "
                        + recipient.userId + " for goal " + goal.getId());
            }
        }
    }

    private void createInAppNotification(Goal goal, int userId, EmailService.DeadlineReminderType reminderType) {
        try {
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setEntityType(Notification.EntityType.GOAL);
            notification.setEntityId(goal.getId());
            notification.setRead(false);
            notification.setCreatedAt(LocalDateTime.now());
            notification.setActionUrl("/user/goals_routines/goal_detail.fxml?id=" + goal.getId());

            switch (reminderType) {
                case DEADLINE_7D -> {
                    notification.setType(Notification.NotificationType.DEADLINE_24H);
                    notification.setTitle("Goal Deadline Reminder - 7 days");
                    notification.setMessage("Your goal '" + goal.getTitle() + "' deadline is approaching in 7 days.");
                }
                case DEADLINE_3D -> {
                    notification.setType(Notification.NotificationType.DEADLINE_24H);
                    notification.setTitle("Goal Deadline Reminder - 3 days");
                    notification.setMessage("Your goal '" + goal.getTitle() + "' deadline is approaching in 3 days.");
                }
                case DEADLINE_24H -> {
                    notification.setType(Notification.NotificationType.DEADLINE_24H);
                    notification.setTitle("Goal Deadline Reminder - 24h");
                    notification.setMessage("Your goal '" + goal.getTitle() + "' deadline is in 24 hours.");
                }
                case OVERDUE -> {
                    notification.setType(Notification.NotificationType.DEADLINE_MISSED);
                    notification.setTitle("Goal Deadline Missed");
                    notification.setMessage("Your goal '" + goal.getTitle() + "' deadline has passed.");
                }
                case CUSTOM_EXACT -> {
                    notification.setType(Notification.NotificationType.DEADLINE_24H);
                    notification.setTitle("Goal Reminder");
                    notification.setMessage("Reminder for your selected goal '" + goal.getTitle() + "'.");
                }
                case COMPLETION_CONGRATS -> {
                    notification.setType(Notification.NotificationType.STATUS_CHANGED);
                    notification.setTitle("Goal Completed");
                    notification.setMessage("Congratulations! Goal '" + goal.getTitle() + "' is completed.");
                }
            }
            notificationService.insert(notification);
        } catch (Exception ignored) {
        }
    }

    private List<Goal> findGoalsEligibleForDeadlineChecks() throws SQLException {
        List<Goal> result = new ArrayList<>();
        for (GoalService.GoalListRow row : goalService.findAllForDashboard()) {
            Goal goal = row.goal();
            if (goal != null && goal.getDeadline() != null) {
                result.add(goal);
            }
        }
        return result;
    }

    private boolean isGoalEligibleForReminder(Goal goal) {
        if (goal == null || goal.getDeadline() == null) {
            return false;
        }
        String st = goal.getStatus() != null ? goal.getStatus().trim().toLowerCase() : "";
        return !st.equals("completed") && !st.equals("cancelled") && !st.equals("archived");
    }

    private boolean isCompletedStatus(String status) {
        if (status == null) {
            return false;
        }
        String st = status.trim().toLowerCase();
        return st.equals("completed");
    }

    private List<UserContact> getEmailRecipients(int goalId) throws SQLException {
        Set<Integer> userIds = new HashSet<>();
        List<UserContact> recipients = new ArrayList<>();

        Integer ownerId = findGoalOwnerId(goalId);
        if (ownerId != null) {
            userIds.add(ownerId);
        }

        boolean notifyParticipants = Boolean.parseBoolean(AppConfig.get("app.goalReminder.notifyParticipants", "true"));
        if (notifyParticipants) {
            for (Integer participantId : findAcceptedParticipantIds(goalId)) {
                userIds.add(participantId);
            }
        }

        for (Integer userId : userIds) {
            UserContact contact = findUserContact(userId);
            if (contact != null && contact.email != null && !contact.email.isBlank()) {
                recipients.add(contact);
            }
        }
        return recipients;
    }

    private Integer findGoalOwnerId(int goalId) throws SQLException {
        String sql = "SELECT user_id FROM goal WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, goalId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int userId = rs.getInt("user_id");
                    return rs.wasNull() ? null : userId;
                }
            }
        }
        return null;
    }

    private List<Integer> findAcceptedParticipantIds(int goalId) throws SQLException {
        String sql = """
                SELECT user_id
                FROM goal_participation
                WHERE goal_id = ?
                  AND status = 'accepted'
                """;
        List<Integer> userIds = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, goalId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    userIds.add(rs.getInt("user_id"));
                }
            }
        }
        return userIds;
    }

    private UserContact findUserContact(int userId) throws SQLException {
        String sql = """
                SELECT id, first_name, last_name, email
                FROM "user"
                WHERE id = ?
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new UserContact(
                            rs.getInt("id"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getString("email")
                    );
                }
            }
        }
        return null;
    }

    private record UserContact(int userId, String firstName, String lastName, String email) {
        String fullName() {
            String fn = firstName != null ? firstName.trim() : "";
            String ln = lastName != null ? lastName.trim() : "";
            String full = (fn + " " + ln).trim();
            return full.isBlank() ? "User" : full;
        }
    }
}
