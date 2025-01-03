import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import javax.sound.sampled.*;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public class ImageToSound extends Application {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private final AtomicBoolean isPaused = new AtomicBoolean(true);
    private VideoCapture capture;
    private boolean isVideo = false;
    private Thread soundThread;
    private Thread videoThread;

    @Override
    public void start(Stage primaryStage) {
        ImageView imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(600);
        imageView.setFitHeight(400);

        ChoiceBox<String> fileTypeChoice = new ChoiceBox<>();
        fileTypeChoice.getItems().addAll("Image", "Vidéo");
        fileTypeChoice.setValue("Image");

        TextArea frequencyLog = new TextArea();
        frequencyLog.setEditable(false);
        frequencyLog.setPrefHeight(150);

        Button selectFileButton = new Button("Sélectionner un fichier");
        selectFileButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            if (fileTypeChoice.getValue().equals("Image")) {
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
            } else {
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Vidéos", "*.mp4", "*.avi", "*.m4v"));
            }
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile != null) {
                stopThreads();
                if (fileTypeChoice.getValue().equals("Image")) {
                    isVideo = false;
                    processImage(selectedFile.getAbsolutePath(), imageView, frequencyLog);
                } else {
                    isVideo = true;
                    processVideo(selectedFile.getAbsolutePath(), imageView, frequencyLog);
                }
            }
        });

        Button playPauseButton = new Button("Lire / Pause");
        playPauseButton.setOnAction(e -> isPaused.set(!isPaused.get()));

        HBox controls = new HBox(10, playPauseButton);
        VBox vbox = new VBox(10, fileTypeChoice, selectFileButton, controls, imageView, frequencyLog);
        BorderPane root = new BorderPane();
        root.setCenter(vbox);

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("Traitement d'image et son synchronisé");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void processVideo(String filePath, ImageView imageView, TextArea frequencyLog) {
        try {
            capture = new VideoCapture(filePath);
            if (!capture.isOpened()) {
                System.err.println("Impossible d'ouvrir la vidéo.");
                return;
            }

            videoThread = new Thread(() -> {
                Mat frame = new Mat();
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        if (!isPaused.get() && capture.read(frame)) {
                            Mat processedFrame = processFrame(frame);
                            Image imageToShow = matToWritableImage(processedFrame);
                            javafx.application.Platform.runLater(() -> imageView.setImage(imageToShow));
                            playColumnsInOneSecond(processedFrame, frequencyLog);
                            playMouseClickSound();
                        } else {
                            Thread.sleep(100);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Erreur pendant le traitement de la vidéo : " + e.getMessage());
                } finally {
                    frame.release();
                    capture.release();
                }
            });
            videoThread.start();
        } catch (Exception e) {
            System.err.println("Erreur lors de la configuration du traitement vidéo : " + e.getMessage());
        }
    }

    private void processImage(String filePath, ImageView imageView, TextArea frequencyLog) {
        try {
            Mat image = Imgcodecs.imread(filePath);
            if (image.empty()) {
                System.err.println("Erreur lors du chargement de l'image.");
                return;
            }

            Mat processedImage = processFrame(image);
            Image imageToShow = matToWritableImage(processedImage);
            javafx.application.Platform.runLater(() -> imageView.setImage(imageToShow));

            soundThread = new Thread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        if (!isPaused.get()) {
                            playColumnsInOneSecond(processedImage, frequencyLog);
                            playMouseClickSound();
                        }
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    System.err.println("Erreur pendant le traitement de l'image : " + e.getMessage());
                }
            });
            soundThread.start();
        } catch (Exception e) {
            System.err.println("Erreur lors de la configuration du traitement d'image : " + e.getMessage());
        }
    }

    private Mat processFrame(Mat frame) {
        Mat luminanceMat = new Mat(frame.rows(), frame.cols(), CvType.CV_8UC1);
        try {
            Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2RGB);
            for (int y = 0; y < frame.rows(); y++) {
                for (int x = 0; x < frame.cols(); x++) {
                    double[] rgb = frame.get(y, x);
                    double luminance = 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2];
                    luminanceMat.put(y, x, luminance);
                }
            }

            Imgproc.resize(luminanceMat, luminanceMat, new Size(64, 64));

            for (int y = 0; y < luminanceMat.rows(); y++) {
                for (int x = 0; x < luminanceMat.cols(); x++) {
                    double[] pixel = luminanceMat.get(y, x);
                    int grayLevel = (int) pixel[0];
                    int quantizedGray = (grayLevel / 16) * 16;
                    luminanceMat.put(y, x, quantizedGray);
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du traitement de la trame : " + e.getMessage());
        }
        return luminanceMat;
    }

    private static Image matToWritableImage(Mat mat) {
        int width = mat.width();
        int height = mat.height();
        byte[] buffer = new byte[width * height];
        try {
            mat.get(0, 0, buffer);
        } catch (Exception e) {
            System.err.println("Erreur lors de la conversion de la matrice en image : " + e.getMessage());
        }

        javafx.scene.image.WritableImage writableImage = new javafx.scene.image.WritableImage(width, height);
        javafx.scene.image.PixelWriter pixelWriter = writableImage.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int gray = buffer[y * width + x] & 0xFF;
                pixelWriter.setArgb(x, y, (0xFF << 24) | (gray << 16) | (gray << 8) | gray);
            }
        }

        return writableImage;
    }

    private static void playColumnsInOneSecond(Mat image, TextArea frequencyLog) {
        int columns = 64;
        float totalDurationSeconds = 1.0f;
        float columnDurationSeconds = totalDurationSeconds / columns;
        float sampleRate = 44100;

        byte[] combinedSound = new byte[(int) (sampleRate * totalDurationSeconds)];
        int samplesPerColumn = (int) (sampleRate * columnDurationSeconds);

        StringBuilder frequencyOutput = new StringBuilder();

        try {
            for (int col = 0; col < columns; col++) {
                double columnFrequency = calculateFrequencyForColumn(image, col);

                frequencyOutput.append("Colonne ").append(col).append(" : ").append(columnFrequency).append(" Hz\n");

                byte[] columnSound = generateColumnSound(columnFrequency, columnDurationSeconds, sampleRate);

                int offset = col * samplesPerColumn;
                for (int i = 0; i < columnSound.length; i++) {
                    if (offset + i < combinedSound.length) {
                        combinedSound[offset + i] += columnSound[i];
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la génération des sons des colonnes : " + e.getMessage());
        }

        javafx.application.Platform.runLater(() -> frequencyLog.setText(frequencyOutput.toString()));
        SoundGenerator.playSound(combinedSound, sampleRate);
    }

    private static double calculateFrequencyForColumn(Mat image, int columnIndex) {
        double sumGrayLevels = 0;
        try {
            for (int row = 0; row < 64; row++) {
                int grayLevel = (int) image.get(row, columnIndex)[0];
                sumGrayLevels += grayLevel;
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du calcul des fréquences des colonnes : " + e.getMessage());
        }
        double avgGrayLevel = sumGrayLevels / 64.0;
        return 20 + (avgGrayLevel * 30);
    }

    private static byte[] generateColumnSound(double frequency, float durationSeconds, float sampleRate) {
        int totalSamples = (int) (sampleRate * durationSeconds);
        byte[] buffer = new byte[totalSamples];

        try {
            for (int i = 0; i < totalSamples; i++) {
                double angle = 2.0 * Math.PI * frequency * i / sampleRate;
                buffer[i] = (byte) (Math.sin(angle) * 127);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la génération du son de colonne : " + e.getMessage());
        }
        return buffer;
    }

    private static void playMouseClickSound() {
        try {
            Clip clip = AudioSystem.getClip();
            AudioInputStream inputStream = AudioSystem.getAudioInputStream(
                    ImageToSound.class.getResourceAsStream("souris_clic.wav")
            );
            clip.open(inputStream);
            clip.start();
        } catch (Exception e) {
            System.err.println("Erreur lors de la lecture du son de clic : " + e.getMessage());
        }
    }

    private void stopThreads() {
        try {
            if (soundThread != null && soundThread.isAlive()) {
                soundThread.interrupt();
                soundThread.join();
            }
            if (videoThread != null && videoThread.isAlive()) {
                videoThread.interrupt();
                videoThread.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("Erreur lors de l'arrêt des threads : " + e.getMessage());
        }
    }
}
