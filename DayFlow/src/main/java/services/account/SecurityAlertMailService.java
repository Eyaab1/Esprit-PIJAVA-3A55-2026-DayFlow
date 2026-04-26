package services.account;

import config.AppConfig;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

public class SecurityAlertMailService {

    public boolean sendSuspiciousLoginAlert(String toEmail,
                                            String displayName,
                                            String reason,
                                            String deviceLabel,
                                            String ipAddress,
                                            String locationLabel) {
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
        boolean startTls = Boolean.parseBoolean(AppConfig.get("app.mail.smtp.starttls", "true"));
        boolean ssl = Boolean.parseBoolean(AppConfig.get("app.mail.smtp.ssl", "false"));

        if (host.isBlank() || from.isBlank()) {
            return false;
        }

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

        String safeName = (displayName == null || displayName.isBlank()) ? "User" : displayName;
        String safeReason = (reason == null || reason.isBlank()) ? "Suspicious login detected" : reason;
        String safeDevice = (deviceLabel == null || deviceLabel.isBlank()) ? "Unknown device" : deviceLabel;
        String safeIp = (ipAddress == null || ipAddress.isBlank()) ? "Unknown IP" : ipAddress;
        String safeLocation = (locationLabel == null || locationLabel.isBlank()) ? "Unknown location" : locationLabel;

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("DayFlow - Security alert");
            message.setContent(buildBodyHtml(safeName, safeReason, safeDevice, safeIp, safeLocation), "text/html; charset=utf-8");
            Transport.send(message);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String buildBodyHtml(String displayName,
                                        String reason,
                                        String deviceLabel,
                                        String ipAddress,
                                        String locationLabel) {
        return "<html><body style=\"font-family:Arial,sans-serif;color:#1f2937;line-height:1.5;\">"
                + "<h2 style=\"margin-bottom:12px;\">Suspicious login detected</h2>"
                + "<p>Hello " + escapeHtml(displayName) + ",</p>"
                + "<p>We detected a potentially risky login on your DayFlow account.</p>"
                + "<ul>"
                + "<li><strong>Reason:</strong> " + escapeHtml(reason) + "</li>"
                + "<li><strong>Device:</strong> " + escapeHtml(deviceLabel) + "</li>"
                + "<li><strong>IP:</strong> " + escapeHtml(ipAddress) + "</li>"
                + "<li><strong>Location:</strong> " + escapeHtml(locationLabel) + "</li>"
                + "</ul>"
                + "<p>If this was you, you can ignore this message.</p>"
                + "<p>If not, please change your password and logout all active sessions from your profile security tab.</p>"
                + "<p>DayFlow Team</p>"
                + "</body></html>";
    }

    private static String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
