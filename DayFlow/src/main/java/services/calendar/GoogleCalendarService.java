package services.calendar;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Service pour intégrer Google Calendar avec les posts programmés
 */
public class GoogleCalendarService {

    private static final String APPLICATION_NAME = "DayFlow";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);
    private static final String CREDENTIALS_FILE_PATH = "/client_secret.json";

    private Calendar calendarService;

    /**
     * Crée une instance autorisée de l'API Google Calendar
     */
    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Charger le fichier client_secret.json
        InputStream in = GoogleCalendarService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            // Essayer de charger depuis le système de fichiers si pas trouvé dans les resources
            File credFile = new File("src/main/resources/client_secret.json");
            if (credFile.exists()) {
                in = new FileInputStream(credFile);
            } else {
                throw new IOException("Fichier client_secret.json introuvable. Veuillez le placer dans src/main/resources/");
            }
        }
        
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Construire le flux d'autorisation et déclencher la demande d'autorisation utilisateur
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    /**
     * Initialise le service Google Calendar
     */
    private void initializeService() throws IOException, GeneralSecurityException {
        if (calendarService == null) {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            Credential credential = getCredentials(HTTP_TRANSPORT);
            calendarService = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        }
    }

    /**
     * Crée un événement Google Calendar pour un post programmé
     * 
     * @param postTitle Titre du post
     * @param scheduledDateTime Date et heure de publication programmée
     * @return ID de l'événement créé, ou null en cas d'erreur
     */
    public String createScheduledPostEvent(String postTitle, LocalDateTime scheduledDateTime) {
        try {
            initializeService();

            // Créer l'événement
            Event event = new Event()
                    .setSummary("Publication programmée – " + postTitle)
                    .setDescription("Votre publication DayFlow sera publiée automatiquement à la date programmée.");

            // Convertir LocalDateTime en DateTime Google
            Date startDate = Date.from(scheduledDateTime.atZone(ZoneId.systemDefault()).toInstant());
            DateTime startDateTime = new DateTime(startDate);
            EventDateTime start = new EventDateTime()
                    .setDateTime(startDateTime)
                    .setTimeZone(ZoneId.systemDefault().getId());
            event.setStart(start);

            // Fin de l'événement (même heure, durée 0)
            EventDateTime end = new EventDateTime()
                    .setDateTime(startDateTime)
                    .setTimeZone(ZoneId.systemDefault().getId());
            event.setEnd(end);

            // Ajouter un rappel 30 minutes avant
            EventReminder[] reminderOverrides = new EventReminder[] {
                new EventReminder().setMethod("popup").setMinutes(30)
            };
            Event.Reminders reminders = new Event.Reminders()
                    .setUseDefault(false)
                    .setOverrides(List.of(reminderOverrides));
            event.setReminders(reminders);

            // Insérer l'événement dans le calendrier principal
            String calendarId = "primary";
            event = calendarService.events().insert(calendarId, event).execute();
            
            System.out.println("Événement Google Calendar créé avec succès: " + event.getHtmlLink());
            return event.getId();

        } catch (IOException | GeneralSecurityException e) {
            System.err.println("Erreur lors de la création de l'événement Google Calendar: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Supprime un événement Google Calendar
     * 
     * @param eventId ID de l'événement à supprimer
     * @return true si supprimé avec succès, false sinon
     */
    public boolean deleteEvent(String eventId) {
        try {
            initializeService();
            calendarService.events().delete("primary", eventId).execute();
            System.out.println("Événement Google Calendar supprimé: " + eventId);
            return true;
        } catch (IOException | GeneralSecurityException e) {
            System.err.println("Erreur lors de la suppression de l'événement Google Calendar: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Met à jour un événement Google Calendar existant
     * 
     * @param eventId ID de l'événement à mettre à jour
     * @param newTitle Nouveau titre du post
     * @param newScheduledDateTime Nouvelle date/heure programmée
     * @return true si mis à jour avec succès, false sinon
     */
    public boolean updateEvent(String eventId, String newTitle, LocalDateTime newScheduledDateTime) {
        try {
            initializeService();

            // Récupérer l'événement existant
            Event event = calendarService.events().get("primary", eventId).execute();

            // Mettre à jour le titre et la description
            event.setSummary("Publication programmée – " + newTitle);
            event.setDescription("Votre publication DayFlow sera publiée automatiquement à la date programmée.");

            // Mettre à jour la date/heure
            Date startDate = Date.from(newScheduledDateTime.atZone(ZoneId.systemDefault()).toInstant());
            DateTime startDateTime = new DateTime(startDate);
            EventDateTime start = new EventDateTime()
                    .setDateTime(startDateTime)
                    .setTimeZone(ZoneId.systemDefault().getId());
            event.setStart(start);
            event.setEnd(start);

            // Mettre à jour l'événement
            calendarService.events().update("primary", eventId, event).execute();
            System.out.println("Événement Google Calendar mis à jour: " + eventId);
            return true;

        } catch (IOException | GeneralSecurityException e) {
            System.err.println("Erreur lors de la mise à jour de l'événement Google Calendar: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
