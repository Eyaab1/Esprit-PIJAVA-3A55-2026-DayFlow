package services.goals_routines;

import model.goals_activity_management.Goal;
import model.goals_activity_management.Routine;
import model.user.User;
import services.CRUD;
import services.chatroom.ChatroomService;
import services.chatroom.MessageService;
import services.chatroom.GoalParticipationService;
import services.deadline.DeadlineEmailReminderWorkflowService;
import utils.DbConnexion;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

public class GoalService implements CRUD<Goal, Integer> {

    private Connection cnx;
    private DeadlineEmailReminderWorkflowService deadlineEmailWorkflow;

    private static final String INSERT_GOAL = """
            INSERT INTO goal (
                title, description, start_date, end_date, deadline, status, priority,
                is_favorite, progress, required_tasks, trello_board_id,
                email_reminder_enabled, email_reminder_at,
                user_id, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_GOAL = """
            UPDATE goal SET
                title=?, description=?, start_date=?, end_date=?, deadline=?, status=?, priority=?,
                is_favorite=?, progress=?, required_tasks=?, trello_board_id=?,
                email_reminder_enabled=?, email_reminder_at=?, updated_at=?
            WHERE id=?
            """;

    public GoalService() {
        cnx = DbConnexion.getInstance().getCnx();
    }

    // Lazy initialization to avoid circular dependency
    private DeadlineEmailReminderWorkflowService getDeadlineEmailWorkflow() {
        if (deadlineEmailWorkflow == null) {
            deadlineEmailWorkflow = new DeadlineEmailReminderWorkflowService();
            deadlineEmailWorkflow.setGoalService(this); // Set this instance to avoid circular dependency
        }
        return deadlineEmailWorkflow;
    }

    @Override
    public void create(Goal goal) throws SQLException {
        insert(goal);
    }

    @Override
    public void insert(Goal goal) throws SQLException {
        if (goal.getStartDate() != null && goal.getEndDate() != null && goal.getEndDate().isBefore(goal.getStartDate())) {
            throw new IllegalArgumentException("La date de fin ne peut pas être avant la date de début");
        }
        try (PreparedStatement ps = cnx.prepareStatement(INSERT_GOAL, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            ps.setString(i++, goal.getTitle());
            ps.setString(i++, goal.getDescription());
            ps.setDate(i++, goal.getStartDate() != null ? Date.valueOf(goal.getStartDate()) : null);
            ps.setDate(i++, goal.getEndDate() != null ? Date.valueOf(goal.getEndDate()) : null);
            ps.setTimestamp(i++, goal.getDeadline() != null ? Timestamp.valueOf(goal.getDeadline()) : null);
            ps.setString(i++, goal.getStatus());
            if (goal.getPriority() == null) {
                ps.setNull(i++, Types.VARCHAR);
            } else {
                ps.setString(i++, goal.getPriority());
            }
            ps.setBoolean(i++, goal.isFavorite());
            ps.setInt(i++, goal.getProgress());
            if (goal.getRequiredTasks() == null) {
                ps.setNull(i++, Types.INTEGER);
            } else {
                ps.setInt(i++, goal.getRequiredTasks());
            }
            if (goal.getTrelloBoardId() == null) {
                ps.setNull(i++, Types.VARCHAR);
            } else {
                ps.setString(i++, goal.getTrelloBoardId());
            }
            ps.setBoolean(i++, goal.isEmailReminderEnabled());
            ps.setTimestamp(i++, goal.getEmailReminderAt() != null ? Timestamp.valueOf(goal.getEmailReminderAt()) : null);
            // Add user_id
            if (goal.getUser() != null && goal.getUser().getId() != null) {
                ps.setInt(i++, goal.getUser().getId());
            } else {
                ps.setNull(i++, Types.INTEGER);
            }
            ps.setTimestamp(i++, goal.getCreatedAt() != null ? Timestamp.valueOf(goal.getCreatedAt()) : null);
            ps.setTimestamp(i++, goal.getUpdatedAt() != null ? Timestamp.valueOf(goal.getUpdatedAt()) : null);
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    goal.setId(keys.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(Goal goal) throws SQLException {
        if (goal.getStartDate() != null && goal.getEndDate() != null && goal.getEndDate().isBefore(goal.getStartDate())) {
            throw new IllegalArgumentException("La date de fin ne peut pas être avant la date de début");
        }
        Goal previousState = findById(goal.getId());
        try (PreparedStatement ps = cnx.prepareStatement(UPDATE_GOAL)) {
            int i = 1;
            ps.setString(i++, goal.getTitle());
            ps.setString(i++, goal.getDescription());
            ps.setDate(i++, goal.getStartDate() != null ? Date.valueOf(goal.getStartDate()) : null);
            ps.setDate(i++, goal.getEndDate() != null ? Date.valueOf(goal.getEndDate()) : null);
            ps.setTimestamp(i++, goal.getDeadline() != null ? Timestamp.valueOf(goal.getDeadline()) : null);
            ps.setString(i++, goal.getStatus());
            if (goal.getPriority() == null) {
                ps.setNull(i++, Types.VARCHAR);
            } else {
                ps.setString(i++, goal.getPriority());
            }
            ps.setBoolean(i++, goal.isFavorite());
            ps.setInt(i++, goal.getProgress());
            if (goal.getRequiredTasks() == null) {
                ps.setNull(i++, Types.INTEGER);
            } else {
                ps.setInt(i++, goal.getRequiredTasks());
            }
            if (goal.getTrelloBoardId() == null) {
                ps.setNull(i++, Types.VARCHAR);
            } else {
                ps.setString(i++, goal.getTrelloBoardId());
            }
            ps.setBoolean(i++, goal.isEmailReminderEnabled());
            ps.setTimestamp(i++, goal.getEmailReminderAt() != null ? Timestamp.valueOf(goal.getEmailReminderAt()) : null);
            ps.setTimestamp(i++, goal.getUpdatedAt() != null ? Timestamp.valueOf(goal.getUpdatedAt()) : null);
            ps.setInt(i, goal.getId());
            ps.executeUpdate();
        }
        if (previousState != null) {
            getDeadlineEmailWorkflow().handleGoalUpdated(previousState, goal);
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        deleteWithDependencies(id);
    }

    /**
     * Supprime un objectif et les données liées (chat, participations, routines, activités).
     */
    public void deleteWithDependencies(int goalId) throws SQLException {
        MessageService messageService = new MessageService();
        ChatroomService chatroomService = new ChatroomService();
        GoalParticipationService participationService = new GoalParticipationService();
        RoutineService routineService = new RoutineService();
        ActivityService activityService = new ActivityService();

        var chatOpt = chatroomService.findByGoalId(goalId);
        if (chatOpt.isPresent()) {
            int cid = chatOpt.get().getId();
            messageService.deleteByChatroomId(cid);
            chatroomService.delete(cid);
        }
        try (PreparedStatement ps = cnx.prepareStatement("DELETE FROM goal_participation WHERE goal_id = ?")) {
            ps.setInt(1, goalId);
            ps.executeUpdate();
        }
        List<Routine> routines = routineService.findByGoalId(goalId);
        for (Routine routine : routines) {
            activityService.deleteByRoutineId(routine.getId());
            routineService.delete(routine.getId());
        }
        String sql = "DELETE FROM goal WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, goalId);
            ps.executeUpdate();
        }
    }

    public Goal findById(int id) throws SQLException {
        String sql = """
                SELECT id, title, description, start_date, end_date, deadline, status, priority,
                       is_favorite, progress, required_tasks, trello_board_id,
                       email_reminder_enabled, email_reminder_at,
                       created_at, updated_at, user_id
                FROM goal WHERE id = ?
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapGoal(rs);
                }
            }
        }
        return null;
    }

    /**
     * Liste des goals avec propriétaire (OWNER), nombre de participants approuvés et id chatroom.
     */
    public List<GoalListRow> findAllForDashboard() throws SQLException {
        String sql = """
                SELECT g.id, g.title, g.description, g.start_date, g.end_date, g.deadline, g.status, g.priority,
                       g.is_favorite, g.progress, g.required_tasks, g.trello_board_id,
                       g.email_reminder_enabled, g.email_reminder_at,
                       g.created_at, g.updated_at,
                       g.user_id,
                       u.first_name AS owner_fn, u.last_name AS owner_ln,
                       c.id AS chatroom_id,
                       COALESCE(pc.cnt, 0) AS participant_count
                FROM goal g
                LEFT JOIN goal_participation owner_gp ON owner_gp.goal_id = g.id AND owner_gp.role = 'owner'
                LEFT JOIN "user" u ON u.id = owner_gp.user_id
                LEFT JOIN chatroom c ON c.goal_id = g.id
                LEFT JOIN (
                    SELECT goal_id, COUNT(*)::int AS cnt
                    FROM goal_participation
                    WHERE status = 'accepted'
                    GROUP BY goal_id
                ) pc ON pc.goal_id = g.id
                ORDER BY g.created_at DESC
                """;
        List<GoalListRow> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Goal g = mapGoal(rs);
                String fn = rs.getString("owner_fn");
                String ln = rs.getString("owner_ln");
                int chatId = rs.getInt("chatroom_id");
                Integer chatroomId = rs.wasNull() ? null : chatId;
                int part = rs.getInt("participant_count");
                list.add(new GoalListRow(g, fn, ln, part, chatroomId));
            }
        }
        return list;
    }

    private static Goal mapGoal(ResultSet rs) throws SQLException {
        Goal g = new Goal();
        g.setId(rs.getInt("id"));
        g.setTitle(rs.getString("title"));
        g.setDescription(rs.getString("description"));
        Date sd = rs.getDate("start_date");
        g.setStartDate(sd != null ? sd.toLocalDate() : null);
        Date ed = rs.getDate("end_date");
        g.setEndDate(ed != null ? ed.toLocalDate() : null);
        Timestamp dl = rs.getTimestamp("deadline");
        g.setDeadline(dl != null ? dl.toLocalDateTime() : null);
        g.setStatus(rs.getString("status"));
        g.setPriority(rs.getString("priority"));
        g.setFavorite(rs.getBoolean("is_favorite"));
        g.setProgress(rs.getInt("progress"));
        int rt = rs.getInt("required_tasks");
        g.setRequiredTasks(rs.wasNull() ? null : rt);
        g.setTrelloBoardId(rs.getString("trello_board_id"));
        g.setEmailReminderEnabled(rs.getBoolean("email_reminder_enabled"));
        Timestamp er = rs.getTimestamp("email_reminder_at");
        if (er != null) {
            g.setEmailReminderAt(er.toLocalDateTime());
        }
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) {
            g.setCreatedAt(ca.toLocalDateTime());
        }
        Timestamp ua = rs.getTimestamp("updated_at");
        if (ua != null) {
            g.setUpdatedAt(ua.toLocalDateTime());
        }
        int uid = rs.getInt("user_id");
        if (!rs.wasNull()) {
            User u = new User();
            u.setId(uid);
            g.setUser(u);
        }
        return g;
    }

    public record GoalListRow(Goal goal, String ownerFirstName, String ownerLastName,
                              int participantCount, Integer chatroomId) {
    }

    /**
     * Objectifs visibles dans la communauté avec la participation de l'utilisateur courant
     * (pour Rejoindre / En attente / Chatroom / Quitter).
     */
    public record GoalDiscussionRow(
            Goal goal,
            String ownerFirstName,
            String ownerLastName,
            int ownerUserId,
            int participantCount,
            Integer chatroomId,
            String myParticipationStatus,
            String myRole,
            Integer myParticipationId) {
    }

    /**
     * Tous les objectifs + participation de {@code userId} (LEFT JOIN), propriétaire et chatroom.
     */
    public List<GoalDiscussionRow> findGoalsForCommunityDiscussion(int userId) throws SQLException {
        String sql = """
                SELECT g.id, g.title, g.description, g.start_date, g.end_date, g.deadline, g.status, g.priority,
                       g.is_favorite, g.progress, g.required_tasks, g.trello_board_id,
                       g.email_reminder_enabled, g.email_reminder_at,
                       g.created_at, g.updated_at,
                       g.user_id,
                       u.first_name AS owner_fn, u.last_name AS owner_ln,
                       owner_gp.user_id AS owner_user_id,
                       c.id AS chatroom_id,
                       COALESCE(pc.cnt, 0) AS participant_count,
                       gp_me.status AS my_status,
                       gp_me.role AS my_role,
                       gp_me.id AS my_gp_id
                FROM goal g
                LEFT JOIN goal_participation owner_gp ON owner_gp.goal_id = g.id AND owner_gp.role = 'owner'
                LEFT JOIN "user" u ON u.id = owner_gp.user_id
                LEFT JOIN chatroom c ON c.goal_id = g.id
                LEFT JOIN (
                    SELECT goal_id, COUNT(*)::int AS cnt
                    FROM goal_participation
                    WHERE status = 'accepted'
                    GROUP BY goal_id
                ) pc ON pc.goal_id = g.id
                LEFT JOIN goal_participation gp_me ON gp_me.goal_id = g.id AND gp_me.user_id = ?
                ORDER BY g.created_at DESC
                """;
        List<GoalDiscussionRow> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Goal g = mapGoal(rs);
                    String fn = rs.getString("owner_fn");
                    String ln = rs.getString("owner_ln");
                    int ownerUid = rs.getInt("owner_user_id");
                    if (rs.wasNull()) {
                        ownerUid = 0;
                    }
                    int chatId = rs.getInt("chatroom_id");
                    Integer chatroomId = rs.wasNull() ? null : chatId;
                    int part = rs.getInt("participant_count");
                    String mySt = rs.getString("my_status");
                    String myRole = rs.getString("my_role");
                    int gpId = rs.getInt("my_gp_id");
                    Integer myGpId = rs.wasNull() ? null : gpId;
                    list.add(new GoalDiscussionRow(g, fn, ln, ownerUid, part, chatroomId, mySt, myRole, myGpId));
                }
            }
        }
        return list;
    }

    /**
     * Compteurs globaux pour le tableau de bord (tous les objectifs en base).
     */
    public record GoalStatusCounts(int active, int completed, int paused, int failed, int draft, int archived,
                                   int total) {
    }

    public GoalStatusCounts countGoalsByStatus() throws SQLException {
        String sql = """
                SELECT LOWER(TRIM(status)) AS st, COUNT(*)::int AS cnt
                FROM goal
                GROUP BY LOWER(TRIM(status))
                """;
        Map<String, Integer> m = new HashMap<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                m.put(rs.getString("st"), rs.getInt("cnt"));
            }
        }
        int active = m.getOrDefault("active", 0);
        int completed = m.getOrDefault("completed", 0);
        int paused = m.getOrDefault("paused", 0);
        int failed = m.getOrDefault("failed", 0);
        int draft = m.getOrDefault("draft", 0);
        int archived = m.getOrDefault("archived", 0);
        int total = active + completed + paused + failed + draft + archived;
        return new GoalStatusCounts(active, completed, paused, failed, draft, archived, total);
    }

    public List<Goal> findByUserId(int userId) throws SQLException {
        String sql = """
                SELECT id, title, description, start_date, end_date, deadline, status, priority,
                       is_favorite, progress, required_tasks, trello_board_id,
                       email_reminder_enabled, email_reminder_at,
                       created_at, updated_at, user_id
                FROM goal WHERE user_id = ?
                ORDER BY created_at DESC
                """;
        List<Goal> goals = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    goals.add(mapGoal(rs));
                }
            }
        }
        return goals;
    }

    /**
     * Recalculates goal progress from all activities of linked routines and updates status accordingly.
     * Formula: (completed activities / total activities) * 100.
     */
    public int recalculateGoalProgress(int goalId) throws SQLException {
        String statsSql = """
                SELECT
                    COUNT(a.id)::int AS total_count,
                    COALESCE(SUM(CASE WHEN LOWER(TRIM(a.status)) = 'completed' THEN 1 ELSE 0 END), 0)::int AS completed_count
                FROM routine r
                LEFT JOIN activity a ON a.routine_id = r.id
                WHERE r.goal_id = ?
                """;

        int total = 0;
        int completed = 0;
        try (PreparedStatement ps = cnx.prepareStatement(statsSql)) {
            ps.setInt(1, goalId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    total = rs.getInt("total_count");
                    completed = rs.getInt("completed_count");
                }
            }
        }

        int progress = total == 0 ? 0 : Math.round((completed * 100.0f) / total);

        LocalDateTime deadline = null;
        String currentStatus = null;
        String metaSql = "SELECT deadline, status FROM goal WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(metaSql)) {
            ps.setInt(1, goalId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp dl = rs.getTimestamp("deadline");
                    deadline = dl != null ? dl.toLocalDateTime() : null;
                    currentStatus = rs.getString("status");
                }
            }
        }

        String newStatus = currentStatus;
        boolean isOverdue = deadline != null && LocalDateTime.now().isAfter(deadline) && progress < 100;
        if (progress >= 100) {
            newStatus = "completed";
        } else if (isOverdue) {
            newStatus = "failed";
        } else if (progress > 0) {
            newStatus = "active";
        } else {
            newStatus = "draft";
        }

        String updateSql = "UPDATE goal SET progress = ?, status = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(updateSql)) {
            ps.setInt(1, progress);
            ps.setString(2, newStatus);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(4, goalId);
            ps.executeUpdate();
        }

        return progress;
    }
}
