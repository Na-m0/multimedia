import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.opencv.core.Core;

import java.io.IOException;


public class GrabDemo extends Application {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    @Override
    public void start(Stage primaryStage) {

        try {
            // load the FXML resource
            FXMLLoader loader = new FXMLLoader(getClass().getResource("GrabDemo.fxml"));
            // store the root element so that the controllers can use it
            BorderPane rootElement = (BorderPane) loader.load();

            Scene scene = new Scene(rootElement);

            primaryStage.setTitle("Capture vidéo avec OpenCV");
            primaryStage.setScene(scene);
            primaryStage.show();

            // set the proper behavior on closing the application
            GrabDemoController controller = loader.getController();
            controller.setRootElement(rootElement);
            controller.setStage(primaryStage);
            rootElement.requestFocus();
            primaryStage.setOnCloseRequest((new EventHandler<WindowEvent>() {
                public void handle(WindowEvent we) {
                    controller.setClosed();
                }
            }));
            controller.startCamera();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

}

