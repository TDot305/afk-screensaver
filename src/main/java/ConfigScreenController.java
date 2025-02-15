import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.GraphicsDevice;
import java.io.File;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ConfigScreenController {
    public interface ConfigurationScreenCallback {
        void onConfigurationComplete(ScreenSaverConfiguration configuration);
    }

    private static final Logger LOGGER = LogManager.getLogger(ConfigScreenController.class);

    private @FXML AnchorPane topAnchor;
    private @FXML TextField backgroundImageField;
    private @FXML ListView<GraphicsDevice> resolutionList;
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
                    new FileChooser.ExtensionFilter("JPG", "*.jpg", "*.JPG"),
                    new FileChooser.ExtensionFilter("PNG", "*.png", "*.PNG")
            );

            this.backgroundImage = fileChooser.showOpenDialog(topAnchor.getScene().getWindow());

            Optional.ofNullable(this.backgroundImage).ifPresentOrElse(
                    file -> this.backgroundImageField.setText(file.getName()),
                    () -> this.backgroundImageField.clear());
        });

        // Populate screen resolution scroll pane.
        this.resolutionList.setItems(FXCollections.observableList(Arrays.asList(Utils.getGraphicsDevices())));
        this.resolutionList.getSelectionModel().selectFirst();
        // Custom display of GraphicsDevices in the ListView
        this.resolutionList.setCellFactory(graphicsDeviceListView -> new ListCell<>() {
            @Override
            protected void updateItem(GraphicsDevice graphicsDevice, boolean empty) {
                super.updateItem(graphicsDevice, empty);

                if (empty || graphicsDevice == null) {
                    this.setText(null);
                } else {
                    String graphicsDeviceFormattedId = graphicsDevice.getIDstring().trim();
                    // Remove leading backslash
                    if (graphicsDeviceFormattedId.startsWith("\\")) {
                        graphicsDeviceFormattedId = graphicsDeviceFormattedId.substring(1);
                    }

                    // Separate actual id and associated number (e.g., Display1 --> Display 1).
                    Pattern pattern = Pattern.compile("(\\D+)(\\d+)");
                    Matcher matcher = pattern.matcher(graphicsDeviceFormattedId);
                    if (matcher.find()) {
                        graphicsDeviceFormattedId = matcher.group(1) + " " + matcher.group(2);
                    }

                    this.setText(graphicsDeviceFormattedId + " (" + graphicsDevice.getDisplayMode().toString() + ")");
                }
            }
        });

        // Equip start button with handler.
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
