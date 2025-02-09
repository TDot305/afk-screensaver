import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;


public class StageManager extends Application {
    private static final Logger LOGGER = LogManager.getLogger(StageManager.class);

    @Override
    public void start(Stage stage) {
        LOGGER.info("Starting AFK-Screensaver.");
        LOGGER.info("Starting config screen.");

        try{
            URL fxmlResource = Objects.requireNonNull(getClass().getResource("fxml/StartConfig.fxml"));
            FXMLLoader fxmlLoader =new FXMLLoader(fxmlResource);
            Parent configRoot = fxmlLoader.load();
            ConfigScreenController configScreenController = fxmlLoader.getController();

            configScreenController.setMainControllerCallback(screenSaverConfiguration -> {
                var screensaver = new Screensaver(stage, screenSaverConfiguration);
                screensaver.launchScreensaver();
            });

            Scene configScene = new Scene(configRoot);
            stage.setTitle("AFK Screensaver Configuration");
            stage.setScene(configScene);
            stage.show();
        } catch (IOException ioe){
            LOGGER.error("Failed to load the FXML file file for the config screen.", ioe);
        }
    }

}
