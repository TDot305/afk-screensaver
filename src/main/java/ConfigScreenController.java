import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.GraphicsDevice;
import java.io.File;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigScreenController {
    public interface ConfigurationScreenCallback {
        void onConfigurationComplete(ScreenSaverConfiguration configuration);
    }

    private static final @NotNull Logger LOGGER = LogManager.getLogger(ConfigScreenController.class);

    private @FXML AnchorPane topAnchor;
    private @FXML ImageView afkImageView;
    private @FXML TextField backgroundImageField;
    private @FXML Slider primaryPuckSizeSlider;
    private @FXML CheckBox secondaryPuckCheckBox;
    private @FXML HBox secondaryPuckSizeHBox;
    private @FXML Slider secondaryPuckSizeSlider;
    private @FXML TextField secondaryPuckImageField;
    private @FXML ImageView secondaryPuckImageView;
    private @FXML ListView<GraphicsDevice> resolutionList;
    private @FXML Button startButton;

    private @Nullable File backgroundImage = null;
    private @Nullable File secondaryPuckImage = null;
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

        this.afkImageView.setFitWidth(this.afkImageView.getImage().getWidth() * Constants.AFK_LOGO_DEFAULT_SIZE_MULTIPLIER);
        this.afkImageView.setFitHeight(this.afkImageView.getImage().getHeight() * Constants.AFK_LOGO_DEFAULT_SIZE_MULTIPLIER);
        this.primaryPuckSizeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            this.afkImageView.setScaleX(newValue.doubleValue());
            this.afkImageView.setScaleY(newValue.doubleValue());
        });

        this.secondaryPuckImageField.setOnMouseClicked(mouseEvent -> {
            var fileChooser = new FileChooser();
            fileChooser.setTitle("Select Background for Secondary Puck");
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("JPG", "*.jpg", "*.JPG"),
                    new FileChooser.ExtensionFilter("PNG", "*.png", "*.PNG")
            );

            this.secondaryPuckImage = fileChooser.showOpenDialog(topAnchor.getScene().getWindow());

            Optional.ofNullable(this.secondaryPuckImage).ifPresentOrElse(
                    file -> {
                        this.secondaryPuckImageField.setText(file.getName());
                        this.secondaryPuckImageView.setImage(new Image(file.toURI().toString()));
                    },
                    () -> this.secondaryPuckImageField.clear());
        });

        this.secondaryPuckCheckBox.selectedProperty().addListener(((observableValue, oldValue, newValue) -> {
            if (newValue){
                this.secondaryPuckImageField.setDisable(false);
                this.secondaryPuckSizeHBox.setDisable(false);
                this.secondaryPuckImageView.setDisable(false);
            } else {
                this.secondaryPuckImageField.setDisable(true);
                this.secondaryPuckSizeHBox.setDisable(true);
                this.secondaryPuckImageView.setDisable(true);
            }
        }));

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
                                this.resolutionList.getSelectionModel().getSelectedItem(),
                                this.primaryPuckSizeSlider.getValue(),
                                this.secondaryPuckCheckBox.isSelected(),
                                this.secondaryPuckImage,
                                this.secondaryPuckSizeSlider.getValue()
                        )
                );
            } else {
                LOGGER.error("Configuration screen controller has no callback to the main application to start " +
                        "the screensaver!");
            }
        });
    }
}
