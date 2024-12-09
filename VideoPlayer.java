import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import java.io.File;

public class VideoPlayer extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Chemin de la vidéo à lire
        String videoFilePath = "zwift_conv.m4v"; // Remplacer par le chemin de votre fichier vidéo

        // Création de l'objet Media pour la vidéo
        Media media = new Media(new File(videoFilePath).toURI().toString());

        // Création d'un lecteur vidéo
        MediaPlayer mediaPlayer = new MediaPlayer(media);
        MediaView mediaView = new MediaView(mediaPlayer);

        StackPane root = new StackPane(mediaView);

        // Configurer la fenêtre
        Scene scene = new Scene(root, 1220, 667); // Taille de la vidéo

        // Démarrer la lecture de la vidéo
        mediaPlayer.play();

        // Afficher la fenêtre de la vidéo
        primaryStage.setScene(scene);
        primaryStage.setTitle("Lecteur Vidéo avec Son");
        primaryStage.show();

        // Ajouter un événement pour synchroniser les sons lorsque la vidéo démarre
        mediaPlayer.setOnPlaying(() -> {
            System.out.println("La vidéo commence. Jouer les sons synchronisés.");
            new Thread(() -> playGeneratedSound()).start();
        });

        mediaPlayer.setOnEndOfMedia(() -> {
            System.out.println("La vidéo est terminée.");
        });
    }

    public static void main(String[] args) {
        launch(args);
    }

    // Méthode pour générer et jouer des sons synchronisés avec la vidéo
    public static void playGeneratedSound() {
        // Générer et jouer le son ici (expliqué ci-dessous)
        SoundGenerator.playSineWave(440, 2000); // Exemple : jouer un son à 440 Hz pendant 1 seconde
    }
}
