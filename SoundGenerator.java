import javax.sound.sampled.*;

public class SoundGenerator {

    public static void playSineWave(double frequency, int durationMs) {
        try {
            // Paramètres du son
            float sampleRate = 44100; // Échantillonnage audio standard (44.1 kHz)
            int bufferSize = (int) (durationMs * sampleRate / 1000); // Taille du buffer en fonction de la durée
            byte[] buffer = new byte[bufferSize];

            // Génération d'une onde sinusoïdale
            for (int i = 0; i < buffer.length; i++) {
                double angle = 2.0 * Math.PI * i / (sampleRate / frequency);
                buffer[i] = (byte) (Math.sin(angle) * 127);
            }

            // Configurer le format audio
            AudioFormat audioFormat = new AudioFormat(sampleRate, 8, 1, true, true);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);

            // Ouvrir et démarrer la ligne audio
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(audioFormat);
            line.start();

            // Écrire les données audio dans la ligne (lecture du son)
            line.write(buffer, 0, buffer.length);

            // Finir et fermer la ligne audio
            line.drain();
            line.close();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }
}
