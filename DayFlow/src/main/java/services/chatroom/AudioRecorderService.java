package services.chatroom_module;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Enregistrement audio via le microphone — javax.sound.sampled (JDK standard).
 * Format : WAV 16kHz mono 16-bit.
 */
public class AudioRecorderService {

    private static final AudioFormat FORMAT = new AudioFormat(
            16000f,  // sample rate
            16,      // bits
            1,       // mono
            true,    // signed
            false    // little-endian
    );

    private TargetDataLine line;
    private Thread recordThread;
    private File outputFile;
    private long startTime;
    private volatile boolean recording = false;

    /** Vérifie si un microphone est disponible. */
    public static boolean isMicAvailable() {
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
            return AudioSystem.isLineSupported(info);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Démarre l'enregistrement.
     * @return chemin du fichier WAV en cours d'enregistrement
     */
    public String startRecording() throws LineUnavailableException, IOException {
        if (recording) throw new IllegalStateException("Enregistrement déjà en cours.");

        DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
        if (!AudioSystem.isLineSupported(info))
            throw new LineUnavailableException("Microphone non disponible.");

        // Créer le dossier uploads/audio/
        File dir = new File("uploads/audio");
        dir.mkdirs();
        String filename = "audio_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".wav";
        outputFile = new File(dir, filename);

        line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(FORMAT);
        line.start();
        recording = true;
        startTime = System.currentTimeMillis();

        // Thread d'enregistrement
        final File dest = outputFile;
        recordThread = new Thread(() -> {
            try (AudioInputStream ais = new AudioInputStream(line)) {
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, dest);
            } catch (IOException e) {
                // Ignoré à l'arrêt normal
            }
        });
        recordThread.setDaemon(true);
        recordThread.start();

        return outputFile.getAbsolutePath();
    }

    /**
     * Arrête l'enregistrement.
     * @return durée en secondes
     */
    public int stopRecording() {
        if (!recording) return 0;
        recording = false;
        int duration = (int) ((System.currentTimeMillis() - startTime) / 1000);
        if (line != null) {
            line.stop();
            line.close();
        }
        return Math.max(1, duration);
    }

    public boolean isRecording() { return recording; }

    public File getOutputFile() { return outputFile; }
}
