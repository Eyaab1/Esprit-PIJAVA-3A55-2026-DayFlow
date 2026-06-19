package services;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service d'envoi d'emails via Gmail SMTP.
 * Les emails sont envoyés en arrière-plan (thread séparé).
 *
 * Configuration : modifier FROM_EMAIL et APP_PASSWORD.
 * Pour Gmail : activer "Mots de passe d'application" dans les paramètres Google.
 */
public final class EmailService {

    // ── Configuration ─────────────────────────────────────────────────────
    private static final String SMTP_HOST    = "smtp.gmail.com";
    private static final int    SMTP_PORT    = 587;
    private static final String FROM_EMAIL   = "dayflow.esprit2026@gmail.com"; // ← ton Gmail
    private static final String APP_PASSWORD = "xxxx xxxx xxxx xxxx";          // ← mot de passe d'application
    private static final String FROM_NAME    = "DayFlow";

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "email-sender");
        t.setDaemon(true);
        return t;
    });

    private EmailService() {}

    // ── API publique ──────────────────────────────────────────────────────

    /**
     * Envoie un email de confirmation d'acceptation de participation.
     * Envoi asynchrone — ne bloque pas l'UI.
     */
    public static void sendParticipationAccepted(String toEmail, String userName, String goalTitle) {
        String subject = "✅ Participation acceptée — " + goalTitle;
        String body = buildHtml(
            userName,
            "Votre demande a été <strong style='color:#48bb78;'>acceptée</strong> !",
            "Vous pouvez maintenant accéder au chatroom de l'objectif <strong>\"" + goalTitle + "\"</strong> " +
            "et communiquer avec les autres membres.",
            "#48bb78",
            "Accéder au chatroom"
        );
        sendAsync(toEmail, subject, body);
    }

    /**
     * Envoie un email de notification de refus de participation.
     */
    public static void sendParticipationRejected(String toEmail, String userName, String goalTitle) {
        String subject = "❌ Participation refusée — " + goalTitle;
        String body = buildHtml(
            userName,
            "Votre demande a été <strong style='color:#ef4444;'>refusée</strong>.",
            "Votre demande de participation à l'objectif <strong>\"" + goalTitle + "\"</strong> " +
            "n'a pas été acceptée par l'administrateur. " +
            "Vous pouvez contacter l'administrateur pour plus d'informations.",
            "#ef4444",
            null
        );
        sendAsync(toEmail, subject, body);
    }

    /**
     * Envoie un email de bienvenue après inscription.
     */
    public static void sendWelcome(String toEmail, String firstName) {
        String subject = "🎉 Bienvenue sur DayFlow, " + firstName + " !";
        String body = buildHtml(
            firstName,
            "Bienvenue sur <strong>DayFlow</strong> ! 🎉",
            "Votre compte a été créé avec succès. " +
            "Commencez dès maintenant à créer vos objectifs, " +
            "rejoindre des groupes et suivre votre progression.",
            "#6c63ff",
            "Commencer"
        );
        sendAsync(toEmail, subject, body);
    }

    /**
     * Envoie un email de notification de nouveau message.
     */
    public static void sendNewMessageNotification(String toEmail, String userName,
                                                   String senderName, String goalTitle,
                                                   String messagePreview) {
        String subject = "💬 Nouveau message dans " + goalTitle;
        String body = buildHtml(
            userName,
            "Nouveau message de <strong>" + senderName + "</strong>",
            "Dans le chatroom <strong>\"" + goalTitle + "\"</strong> :<br><br>" +
            "<em style='color:#6c63ff;'>\"" + messagePreview + "\"</em>",
            "#6c63ff",
            "Voir le message"
        );
        sendAsync(toEmail, subject, body);
    }

    // ── Envoi ─────────────────────────────────────────────────────────────

    private static void sendAsync(String to, String subject, String htmlBody) {
        EXECUTOR.submit(() -> {
            try {
                send(to, subject, htmlBody);
                System.out.println("[EmailService] ✅ Email envoyé à " + to);
            } catch (Exception e) {
                System.err.println("[EmailService] ❌ Échec envoi à " + to + " : " + e.getMessage());
            }
        });
    }

    private static void send(String to, String subject, String htmlBody) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            String.valueOf(SMTP_PORT));
        props.put("mail.smtp.ssl.trust",       SMTP_HOST);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
            }
        });

        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(FROM_EMAIL));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        msg.setSubject(subject, "UTF-8");
        msg.setContent(htmlBody, "text/html; charset=UTF-8");
        Transport.send(msg);
    }

    // ── Template HTML ─────────────────────────────────────────────────────

    private static String buildHtml(String name, String headline, String body,
                                     String accentColor, String ctaText) {
        String cta = ctaText != null
            ? "<a href='#' style='display:inline-block;margin-top:20px;padding:12px 28px;" +
              "background:" + accentColor + ";color:white;text-decoration:none;" +
              "border-radius:8px;font-weight:bold;'>" + ctaText + "</a>"
            : "";

        return "<!DOCTYPE html><html><body style='margin:0;padding:0;" +
               "background:#f5f6fa;font-family:Segoe UI,Arial,sans-serif;'>" +
               "<div style='max-width:560px;margin:40px auto;background:white;" +
               "border-radius:16px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>" +
               // Header
               "<div style='background:linear-gradient(135deg,#6c63ff,#764ba2);" +
               "padding:32px 40px;text-align:center;'>" +
               "<h1 style='color:white;margin:0;font-size:28px;'>DayFlow</h1>" +
               "<p style='color:rgba(255,255,255,0.8);margin:6px 0 0;font-size:14px;'>" +
               "Votre plateforme d'objectifs</p></div>" +
               // Body
               "<div style='padding:36px 40px;'>" +
               "<p style='color:#374151;font-size:16px;margin:0 0 8px;'>Bonjour <strong>" +
               name + "</strong>,</p>" +
               "<h2 style='color:#1a1a2e;font-size:20px;margin:16px 0 12px;'>" +
               headline + "</h2>" +
               "<p style='color:#6b7280;font-size:15px;line-height:1.6;margin:0;'>" +
               body + "</p>" +
               cta +
               "</div>" +
               // Footer
               "<div style='background:#f9fafb;padding:20px 40px;text-align:center;" +
               "border-top:1px solid #e5e7eb;'>" +
               "<p style='color:#9ca3af;font-size:12px;margin:0;'>" +
               "© 2026 DayFlow — Cet email a été envoyé automatiquement, ne pas répondre.</p>" +
               "</div></div></body></html>";
    }
}
