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
            message.setText(buildBody(token, validityMinutes));
            Transport.send(message);
            return true;
        } catch (MessagingException e) {
            return false;
        }
    }

    private static String buildBody(String token, int validityMinutes) {
        return "Hello,\n\n"
                + "You requested a password reset for your DayFlow account.\n\n"
                + "Your reset token is:\n"
                + token + "\n\n"
                + "This token is valid for " + validityMinutes + " minutes.\n\n"
                + "If you did not request this, you can ignore this email.\n\n"
                + "DayFlow Team";
    }
}
