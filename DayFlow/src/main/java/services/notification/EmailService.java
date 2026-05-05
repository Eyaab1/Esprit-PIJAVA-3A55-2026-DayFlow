package services.notification;

import config.AppConfig;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import model.goals_activity_management.Goal;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Properties;

/**
 * Centralized email service for business notifications.
 * Uses SMTP (Brevo-compatible) through Jakarta Mail.
 */
public class EmailService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.FRENCH);

    public enum DeadlineReminderType {
        DEADLINE_7D("7 days"),
        DEADLINE_3D("3 days"),
        DEADLINE_24H("24 hours"),
        CUSTOM_EXACT("custom"),
        OVERDUE("overdue"),
        COMPLETION_CONGRATS("completion");

        private final String label;

        DeadlineReminderType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public boolean sendDeadlineReminder(String toEmail, String userName, Goal goal, String thresholdLabel) {
        if (goal == null) {
            return false;
        }
        String subject = "Goal Deadline Reminder - DayFlow";
        String body = """
                <html><body style="font-family:Arial,sans-serif;color:#1f2937;line-height:1.5;">
                <h2>Your goal deadline is approaching</h2>
                <p>Hello %s,</p>
                <p>Your goal "<strong>%s</strong>" is approaching its deadline.</p>
                <ul>
                    <li><strong>Reminder:</strong> %s before deadline</li>
                    <li><strong>Deadline:</strong> %s</li>
                    <li><strong>Current Progress:</strong> %d%%</li>
                </ul>
                <p>You still have limited time to complete your goal.</p>
                <p>Stay consistent and keep progressing.</p>
                <p>- DayFlow Smart Reminder System</p>
                </body></html>
                """.formatted(
                safe(userName, "User"),
                escapeHtml(goal.getTitle()),
                escapeHtml(thresholdLabel),
                goal.getDeadline() != null ? goal.getDeadline().format(DATE_TIME_FORMATTER) : "N/A",
                goal.getProgress()
        );
        return sendHtmlEmail(toEmail, subject, body);
    }

    public boolean sendOverdueReminder(String toEmail, String userName, Goal goal) {
        if (goal == null) {
            return false;
        }
        String subject = "Goal Deadline Passed - DayFlow";
        String body = """
                <html><body style="font-family:Arial,sans-serif;color:#1f2937;line-height:1.5;">
                <h2>Your goal deadline has passed</h2>
                <p>Hello %s,</p>
                <p>Your goal "<strong>%s</strong>" has passed its deadline.</p>
                <ul>
                    <li><strong>Deadline:</strong> %s</li>
                    <li><strong>Current Progress:</strong> %d%%</li>
                </ul>
                <p>Review your goal plan and update your next milestones.</p>
                <p>- DayFlow Smart Reminder System</p>
                </body></html>
                """.formatted(
                safe(userName, "User"),
                escapeHtml(goal.getTitle()),
                goal.getDeadline() != null ? goal.getDeadline().format(DATE_TIME_FORMATTER) : "N/A",
                goal.getProgress()
        );
        return sendHtmlEmail(toEmail, subject, body);
    }

    public boolean sendCompletionCongratulations(String toEmail, String userName, Goal goal) {
        if (goal == null) {
            return false;
        }
        String subject = "Congratulations! Goal Completed - DayFlow";
        String body = """
                <html><body style="font-family:Arial,sans-serif;color:#1f2937;line-height:1.5;">
                <h2>Congratulations on completing your goal!</h2>
                <p>Hello %s,</p>
                <p>Great work! Your goal "<strong>%s</strong>" is now completed.</p>
                <ul>
                    <li><strong>Completion:</strong> 100%%</li>
                    <li><strong>Deadline:</strong> %s</li>
                </ul>
                <p>Keep this momentum for your next objectives.</p>
                <p>- DayFlow Smart Reminder System</p>
                </body></html>
                """.formatted(
                safe(userName, "User"),
                escapeHtml(goal.getTitle()),
                goal.getDeadline() != null ? goal.getDeadline().format(DATE_TIME_FORMATTER) : "N/A"
        );
        return sendHtmlEmail(toEmail, subject, body);
    }

    public boolean sendHtmlEmail(String toEmail, String subject, String htmlBody) {
        if (toEmail == null || toEmail.isBlank()) {
            return false;
        }
        boolean enabled = Boolean.parseBoolean(AppConfig.get("app.mail.enabled", "false"));
        if (!enabled) {
            return false;
        }

        String host = AppConfig.get("app.mail.smtp.host", "");
        String port = AppConfig.get("app.mail.smtp.port", "587");
        String username = AppConfig.get("app.mail.smtp.username", "");
        String password = AppConfig.get("app.mail.smtp.password", "");
        String from = AppConfig.get("app.mail.from", username);
        if (from == null || from.isBlank() || "your_email".equalsIgnoreCase(from.trim())) {
            from = username;
        }
        String fromName = AppConfig.get("app.mail.fromName", "DayFlow");
        boolean startTls = Boolean.parseBoolean(AppConfig.get("app.mail.smtp.starttls", "true"));
        boolean ssl = Boolean.parseBoolean(AppConfig.get("app.mail.smtp.ssl", "false"));

        if (host.isBlank() || from == null || from.isBlank()) {
            System.err.println("[EmailService] Missing SMTP host/from configuration.");
            return false;
        }
        if (username.isBlank() || password.isBlank()) {
            System.err.println("[EmailService] Missing SMTP credentials (username/password).");
            return false;
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", (!username.isBlank() && !password.isBlank()) ? "true" : "false");
        props.put("mail.smtp.starttls.enable", Boolean.toString(startTls));
        props.put("mail.smtp.ssl.enable", Boolean.toString(ssl));
        props.put("mail.smtp.connectiontimeout", AppConfig.get("app.mail.smtp.connectionTimeoutMs", "10000"));
        props.put("mail.smtp.timeout", AppConfig.get("app.mail.smtp.timeoutMs", "10000"));
        props.put("mail.smtp.writetimeout", AppConfig.get("app.mail.smtp.writeTimeoutMs", "10000"));

        Session session;
        if (!username.isBlank() && !password.isBlank()) {
            session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
        } else {
            session = Session.getInstance(props);
        }

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from, fromName));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setContent(htmlBody, "text/html; charset=utf-8");
            Transport.send(message);
            return true;
        } catch (Exception e) {
            System.err.println("[EmailService] SMTP send failed: " + e.getMessage());
            return false;
        }
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : escapeHtml(value);
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
