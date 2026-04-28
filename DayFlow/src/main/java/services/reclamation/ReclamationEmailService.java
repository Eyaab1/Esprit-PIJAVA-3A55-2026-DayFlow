package services.reclamation;

import config.AppConfig;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import model.reclamation.Reclamation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Service for sending email notifications related to reclamations.
 */
public class ReclamationEmailService {

    /**
     * Sends an email notification to the user when admin responds to their reclamation.
     *
     * @param reclamation The reclamation that was responded to
     * @param responseContent The admin's response content
     * @return true if email was sent successfully, false otherwise
     */
    public boolean sendResponseNotification(Reclamation reclamation, String responseContent) {
        // Check if email is enabled
        boolean enabled = Boolean.parseBoolean(AppConfig.get("app.mail.enabled", "false"));
        if (!enabled) {
            System.out.println("Email notifications are disabled");
            return false;
        }

        // Get user email
        String userEmail = getUserEmail(reclamation.getUserId());
        if (userEmail == null || userEmail.isBlank()) {
            System.err.println("Cannot send email: user email not found for user ID " + reclamation.getUserId());
            return false;
        }

        // Get email configuration
        String host = AppConfig.get("app.mail.smtp.host", "");
        String port = AppConfig.get("app.mail.smtp.port", "587");
        String username = AppConfig.get("app.mail.smtp.username", "");
        String password = AppConfig.get("app.mail.smtp.password", "");
        String from = AppConfig.get("app.mail.from", username);
        boolean startTls = Boolean.parseBoolean(AppConfig.get("app.mail.smtp.starttls", "true"));
        boolean ssl = Boolean.parseBoolean(AppConfig.get("app.mail.smtp.ssl", "false"));

        if (host.isBlank() || from.isBlank()) {
            System.err.println("Email configuration incomplete");
            return false;
        }

        // Configure mail session
        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", (!username.isBlank() && !password.isBlank()) ? "true" : "false");
        props.put("mail.smtp.starttls.enable", Boolean.toString(startTls));
        props.put("mail.smtp.ssl.enable", Boolean.toString(ssl));

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

        // Send email
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(userEmail));
            message.setSubject("DayFlow - Réponse à votre réclamation #" + reclamation.getId());
            message.setContent(buildResponseEmailHtml(reclamation, responseContent), "text/html; charset=utf-8");
            Transport.send(message);
            System.out.println("Email sent successfully to " + userEmail);
            return true;
        } catch (MessagingException e) {
            System.err.println("Failed to send email: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets the user's email address from the database.
     */
    private String getUserEmail(Integer userId) {
        if (userId == null) {
            return null;
        }

        String sql = "SELECT email FROM \"user\" WHERE id = ?";
        try (Connection conn = utils.DbConnexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("email");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching user email: " + e.getMessage());
        }
        return null;
    }

    /**
     * Builds the HTML email body for reclamation response notification.
     */
    private String buildResponseEmailHtml(Reclamation reclamation, String responseContent) {
        String reclamationType = reclamation.getType() != null ? getTypeLabelFr(reclamation.getType().value) : "—";
        String reclamationContent = stripHtml(reclamation.getContent());
        String cleanResponse = stripHtml(responseContent);

        StringBuilder html = new StringBuilder();
        html.append("<html><body style=\"font-family:Arial,sans-serif;color:#1f2937;line-height:1.6;max-width:600px;margin:0 auto;\">")
                .append("<div style=\"background:#7c3aed;color:white;padding:20px;border-radius:8px 8px 0 0;\">")
                .append("<h2 style=\"margin:0;\">✉️ Réponse à votre réclamation</h2>")
                .append("</div>")
                .append("<div style=\"background:#f9fafb;padding:20px;border:1px solid #e5e7eb;border-top:none;border-radius:0 0 8px 8px;\">")
                
                .append("<div style=\"background:white;padding:16px;border-radius:8px;margin-bottom:16px;border-left:4px solid #7c3aed;\">")
                .append("<h3 style=\"margin:0 0 8px 0;color:#7c3aed;\">📋 Votre réclamation</h3>")
                .append("<p style=\"margin:4px 0;color:#6b7280;font-size:14px;\"><strong>Type :</strong> ").append(reclamationType).append("</p>")
                .append("<p style=\"margin:4px 0;color:#6b7280;font-size:14px;\"><strong>Numéro :</strong> #").append(reclamation.getId()).append("</p>")
                .append("<p style=\"margin:12px 0 0 0;color:#374151;\">").append(truncate(reclamationContent, 200)).append("</p>")
                .append("</div>")
                
                .append("<div style=\"background:white;padding:16px;border-radius:8px;margin-bottom:16px;border-left:4px solid #16a34a;\">")
                .append("<h3 style=\"margin:0 0 8px 0;color:#16a34a;\">💬 Réponse de notre équipe</h3>")
                .append("<p style=\"margin:0;color:#374151;white-space:pre-wrap;\">").append(cleanResponse).append("</p>")
                .append("</div>")
                
                .append("<div style=\"background:#dbeafe;padding:12px;border-radius:8px;margin-bottom:16px;\">")
                .append("<p style=\"margin:0;color:#1e40af;font-size:14px;\">")
                .append("💡 <strong>Astuce :</strong> Vous pouvez consulter toutes vos réclamations et leurs réponses dans votre espace \"Mes Réclamations\".")
                .append("</p>")
                .append("</div>")
                
                .append("<div style=\"text-align:center;margin:20px 0;\">")
                .append("<a href=\"#\" style=\"background:#7c3aed;color:white;padding:12px 24px;text-decoration:none;border-radius:8px;display:inline-block;font-weight:bold;\">")
                .append("Voir mes réclamations")
                .append("</a>")
                .append("</div>")
                
                .append("<div style=\"border-top:1px solid #e5e7eb;padding-top:16px;margin-top:16px;\">")
                .append("<p style=\"margin:0;color:#6b7280;font-size:12px;text-align:center;\">")
                .append("Merci d'utiliser DayFlow<br/>")
                .append("Si vous avez d'autres questions, n'hésitez pas à créer une nouvelle réclamation.")
                .append("</p>")
                .append("</div>")
                
                .append("</div>")
                .append("</body></html>");

        return html.toString();
    }

    /**
     * Strips HTML tags from content.
     */
    private String stripHtml(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String s = raw
                .replaceAll("(?i)<\\s*br\\s*/?>", "\n")
                .replaceAll("(?i)</p>\\s*", "\n")
                .replaceAll("(?i)<\\s*p[^>]*>", "");
        s = s.replaceAll("<[^>]+>", "");
        s = s.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"");
        return s.trim();
    }

    /**
     * Truncates text to specified length.
     */
    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    /**
     * Gets French label for reclamation type.
     */
    private String getTypeLabelFr(String type) {
        if (type == null) {
            return "—";
        }
        return switch (type.toUpperCase()) {
            case "ACCOUNT" -> "Compte";
            case "BUG" -> "Bug";
            case "COACHING" -> "Coaching";
            case "PAYMENT" -> "Paiement";
            case "OTHER" -> "Autre";
            default -> type;
        };
    }
}
