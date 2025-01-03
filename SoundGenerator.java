import javax.sound.sampled.*;

class SoundGenerator {

    // Joue un tableau de données audio
    public static void playSound(byte[] soundData, float sampleRate) {
        try {
            // Configurer le format audio
            AudioFormat audioFormat = new AudioFormat(sampleRate, 8, 1, true, true);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);

            // Ouvrir et démarrer la ligne audio
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(audioFormat);
            line.start();

            // Écrire les données audio dans la ligne
            line.write(soundData, 0, soundData.length);

            // Finaliser et fermer la ligne audio
            line.drain();
            line.close();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }
}
