import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.DisplayMode;
import java.io.File;
import java.util.Arrays;
import java.util.Optional;

public class ConfigScreenController {
    public interface ConfigurationScreenCallback {
        void onConfigurationComplete(ScreenSaverConfiguration configuration);
    }

    public record ScreenSaverConfiguration(File backgroundImage, DisplayMode resolution) {
    }

    private static final Logger LOGGER = LogManager.getLogger(ConfigScreenController.class);

    private @FXML AnchorPane topAnchor;
    private @FXML TextField backgroundImageField;
    private @FXML ListView<DisplayMode> resolutionList;
    private @FXML Button startButton;

    private File backgroundImage = null;
    private ConfigurationScreenCallback callback = null;

    public void setMainControllerCallback(ConfigurationScreenCallback callback) {
        this.callback = callback;
    }

    @FXML
    private void initialize() {
        // Attach file chooser to backgroundImageField.
        this.backgroundImageField.setOnMouseClicked(mouseEvent -> {
            var fileChooser = new FileChooser();
            fileChooser.setTitle("Select Background for AFK Screensaver");
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("JPG", "*.jpg"),
                    new FileChooser.ExtensionFilter("PNG", "*.png")
            );

            this.backgroundImage = fileChooser.showOpenDialog(topAnchor.getScene().getWindow());

            Optional.ofNullable(this.backgroundImage).ifPresentOrElse(
                    file -> this.backgroundImageField.setText(file.getName()),
                    () -> this.backgroundImageField.clear());
        });

        // Populate resolution scroll pane.
        this.resolutionList.setItems(FXCollections.observableList(Arrays.asList(Utils.getDisplayModes())));
        this.resolutionList.getSelectionModel().selectFirst();

        this.resolutionList.getSelectionModel().selectedItemProperty().addListener(changeEvent -> {
            System.out.println("Selection changed: " + this.resolutionList.getSelectionModel().getSelectedItem());
        });

        this.startButton.setOnAction(actionEvent -> {
            if (this.callback != null) {
                this.callback.onConfigurationComplete(
                        new ScreenSaverConfiguration(
                                this.backgroundImage,
                                this.resolutionList.getSelectionModel().getSelectedItem()
                        )
                );
            } else {
                LOGGER.error("Configuration screen controller has no callback to the main application to start " +
                        "the screensaver!");
            }
        });
    }
}
