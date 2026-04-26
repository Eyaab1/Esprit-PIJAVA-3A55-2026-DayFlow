package services.account;

import config.AppConfig;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class ResetMailSenderService {

    public boolean sendPasswordResetEmail(String toEmail, String token, int validityMinutes) {
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

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("DayFlow - Password reset");
            message.setContent(buildResetBodyHtml(token, validityMinutes), "text/html; charset=utf-8");
            Transport.send(message);
            return true;
        } catch (MessagingException e) {
            return false;
        }
    }

    public boolean sendPasswordChangedConfirmationEmail(String toEmail) {
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

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("DayFlow - Password changed");
            message.setContent(buildPasswordChangedBodyHtml(), "text/html; charset=utf-8");
            Transport.send(message);
            return true;
        } catch (MessagingException e) {
            return false;
        }
    }

    private static String buildResetBodyHtml(String token, int validityMinutes) {
        String resetPageBaseUrl = AppConfig.get("app.mail.reset.pageUrl", "");
        String resetDeepLinkBaseUrl = AppConfig.get("app.mail.reset.deepLink", "dayflow://reset-password");
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        String resetWebLink = resetPageBaseUrl.isBlank() ? "" : resetPageBaseUrl + (resetPageBaseUrl.contains("?") ? "&" : "?") + "token=" + encodedToken;
        String resetDeepLink = resetDeepLinkBaseUrl.isBlank() ? "" : resetDeepLinkBaseUrl + (resetDeepLinkBaseUrl.contains("?") ? "&" : "?") + "token=" + encodedToken;

        StringBuilder html = new StringBuilder();
        html.append("<html><body style=\"font-family:Arial,sans-serif;color:#1f2937;line-height:1.5;\">")
                .append("<h2 style=\"margin-bottom:12px;\">Password reset request</h2>")
                .append("<p>You requested a password reset for your DayFlow account.</p>");
        if (!resetWebLink.isBlank()) {
            html.append("<p style=\"margin:20px 0;\">")
                    .append("<a href=\"").append(resetWebLink).append("\" ")
                    .append("style=\"background:#2563eb;color:#ffffff;padding:10px 16px;text-decoration:none;border-radius:6px;display:inline-block;\">")
                    .append("Reset password")
                    .append("</a>")
                    .append("</p>")
                    .append("<p style=\"font-size:12px;color:#6b7280;\">If the button does not open your app page, copy this link:</p>")
                    .append("<p style=\"font-size:12px;word-break:break-all;\">").append(resetWebLink).append("</p>");
        } else if (!resetDeepLink.isBlank()) {
            html.append("<p style=\"margin:20px 0;\">")
                    .append("<a href=\"").append(resetDeepLink).append("\" ")
                    .append("style=\"background:#2563eb;color:#ffffff;padding:10px 16px;text-decoration:none;border-radius:6px;display:inline-block;\">")
                    .append("Open DayFlow app")
                    .append("</a>")
                    .append("</p>")
                    .append("<p style=\"font-size:12px;color:#6b7280;\">If the button does not open the app, copy this deep link:</p>")
                    .append("<p style=\"font-size:12px;word-break:break-all;\">").append(resetDeepLink).append("</p>");
        }
        html.append("<p><strong>Your 6-digit reset code:</strong><br/>")
                .append(token)
                .append("</p>")
                .append("<p style=\"font-size:12px;color:#6b7280;\">You can paste this code manually in the DayFlow reset password screen.</p>")
                .append("<p>This code is valid for ").append(validityMinutes).append(" minutes.</p>")
                .append("<p>If you did not request this, you can ignore this email.</p>")
                .append("<p>DayFlow Team</p>")
                .append("</body></html>");
        return html.toString();
    }

    private static String buildPasswordChangedBodyHtml() {
        return "<html><body style=\"font-family:Arial,sans-serif;color:#1f2937;line-height:1.5;\">"
                + "<h2 style=\"margin-bottom:12px;\">Your password was changed</h2>"
                + "<p>This is a confirmation that your DayFlow account password was updated successfully.</p>"
                + "<p>If you did not perform this change, reset your password immediately and contact support.</p>"
                + "<p>DayFlow Team</p>"
                + "</body></html>";
    }
}
