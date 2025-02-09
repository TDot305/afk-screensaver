import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

public class Screensaver {
    private static final Logger LOGGER = LogManager.getLogger(Screensaver.class);

    private final Stage stage;
    private final ScreenSaverConfiguration screenSaverConfiguration;
    private final Rectangle boundingBox;
    private final Rectangle puck;

    private int pixelsToTraversePerSecond = 300;
    private Point2D lastVector;
    private boolean debugMode = false;


    public Screensaver(Stage stage, ScreenSaverConfiguration screenSaverConfiguration) {
        this.stage = stage;
        this.screenSaverConfiguration = screenSaverConfiguration;
        java.awt.Rectangle graphicsBounds = this.screenSaverConfiguration.graphicsDevice().getDefaultConfiguration().getBounds();

        // Basic stage configuration.
        stage.setX(graphicsBounds.getX());
        stage.setY(graphicsBounds.getY());
        stage.setTitle("AFK Screensaver");

        // Initialize bounding box and puck.
        this.boundingBox = new Rectangle(
                0,
                0,
                graphicsBounds.getWidth(),
                graphicsBounds.getHeight());
        var defaultPuckDimension = new Dimension2D(400, 130); // Default dimensions of the AFK puck.
        var defaultStartingCoordinate = new Point2D(
                graphicsBounds.getWidth() / 2 - defaultPuckDimension.getWidth() / 2,
                graphicsBounds.getHeight() / 2 - defaultPuckDimension.getHeight() / 2);
        this.puck = new Rectangle(
                defaultStartingCoordinate.getX(),
                defaultStartingCoordinate.getY(),
                defaultPuckDimension.getWidth(),
                defaultPuckDimension.getHeight());
    }

    public void launchScreensaver() {
        // Basic initialization.
        var group = new Group(puck);
        var scene = new Scene(group);
        scene.setFill(Color.BLACK);

        // Configure stage.
        stage.setScene(scene);
        stage.show();
        stage.setFullScreen(true);

        // Configure background.
        if (screenSaverConfiguration.backgroundImage() != null) {
            LOGGER.info("Selected background image file: {}", screenSaverConfiguration.backgroundImage().getAbsolutePath());
            scene.setFill(new ImagePattern(new Image(screenSaverConfiguration.backgroundImage().toURI().toString())));
        } else {
            LOGGER.info("No background image selected. Clearing the background.");
            scene.setFill(Color.BLACK);
        }

        URL afkImageResource = getClass().getResource(Constants.AFK_LOGO_PATH);
        if (afkImageResource != null) {
            try {
                Image afkImage = new Image(afkImageResource.toURI().toString());

                System.out.println("Image dimensions: " + afkImage.getWidth() + " | " + afkImage.getHeight());
                // Set puck dimensions according to screen resolution.
                double afkLogoAspectRatio = afkImage.getHeight() / afkImage.getWidth();
                this.puck.setWidth(0.15 * afkImage.getWidth());
                this.puck.setHeight(afkLogoAspectRatio * this.puck.getWidth());

                this.puck.setFill(new ImagePattern(afkImage));
                LOGGER.info("Successfully loaded AFK logo for the puck.");
            } catch (URISyntaxException use) {
                LOGGER.error("Caught a URISyntaxException when trying to turn {} into a URI.", afkImageResource);
                this.puck.setFill(Color.PINK);
            }
        } else {
            LOGGER.warn("Could not find resource for AFK logo with name \"" + Constants.AFK_LOGO_PATH + "\"");
            this.puck.setFill(Color.PINK);
        }

        // Attach handlers to stage.
        stage.addEventFilter(KeyEvent.KEY_PRESSED, keyEvent -> {
            switch (keyEvent.getCode()) {
                case UP -> this.pixelsToTraversePerSecond += 20; // TODO Small information overlay.
                case DOWN -> this.pixelsToTraversePerSecond = Math.max(10, this.pixelsToTraversePerSecond - 20);
                case F1 -> {
                    var fileChooser = new FileChooser();
                    fileChooser.setTitle("Select Background for AFK Screensaver");
                    fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
                    fileChooser.getExtensionFilters().addAll(
                            new FileChooser.ExtensionFilter("JPG", "*.jpg"),
                            new FileChooser.ExtensionFilter("PNG", "*.png")
                    );

                    File selectedBackgroundFile = fileChooser.showOpenDialog(stage);

                    if (selectedBackgroundFile != null) {
                        LOGGER.info("Selected background image file: {}", selectedBackgroundFile.getAbsolutePath());
                        scene.setFill(new ImagePattern(new Image(selectedBackgroundFile.toURI().toString())));
                    } else {
                        LOGGER.info("No background image selected. Clearing the background.");
                        scene.setFill(Color.BLACK);
                    }
                }
                case F10 -> {
                    if (!this.debugMode) {
                        // Turn on debug mode.
                        this.puck.setStyle("-fx-stroke: green; -fx-stroke-width: 3;");
                    } else {
                        // Turn off debug mode.
                        this.puck.setStyle("");
                    }

                    this.debugMode = !this.debugMode;
                }
                case F11 -> stage.setFullScreen(!stage.isFullScreen());
            }
        });

        // // Configure puck.
        double randomXPos = Math.random() * (this.boundingBox.getWidth() - this.puck.getWidth());
        double randomYPos = Math.random() * (this.boundingBox.getHeight() - this.puck.getHeight());
        this.puck.setX(randomXPos);
        this.puck.setY(randomYPos);
        this.puck.setSmooth(true);

        Point2D initialVector = Geometrics.getRandomVector();
        this.lastVector = initialVector;

        double collisionT = Geometrics.getMinimalCollisionT(puck, initialVector, boundingBox);
        Point2D collisionLTCoord = new Point2D(puck.getX(), puck.getY()).add(initialVector.multiply(collisionT));
        LOGGER.debug("Initial move: Vector = {} Collision-t = {} Collision-Coord = {}", initialVector, collisionT, collisionLTCoord);

        LOGGER.info("Launching initial transition.");
        this.launchNewTransition(collisionLTCoord);
    }

    private void launchReflectionTransition() {
        // Determine side that has been hit.
        Point2D reflectionVector = Geometrics.getDeflectionVector(this.puck, this.lastVector, this.boundingBox);
        this.lastVector = reflectionVector;
        double collisionT = Geometrics.getMinimalCollisionT(puck, reflectionVector, boundingBox);
        Point2D collisionLTCoord = new Point2D(puck.getX(), puck.getY()).add(reflectionVector.multiply(collisionT));

        LOGGER.debug("Collision Point Calculation: rect[{}, {}] + {} * {} = {}",
                puck.getX(), puck.getY(), collisionT, reflectionVector, collisionLTCoord);

        this.launchNewTransition(collisionLTCoord);
    }

    private void launchNewTransition(Point2D collisionLTCoord) {
        var transition = new TranslateTransition(Duration.seconds(
                collisionLTCoord.distance(puck.getX(), puck.getY()) / pixelsToTraversePerSecond), puck);

        transition.setToX(collisionLTCoord.getX() - puck.getX());
        transition.setToY(collisionLTCoord.getY() - puck.getY());
        transition.setInterpolator(Interpolator.LINEAR);
        transition.setOnFinished(e -> {
            // Move to new position.
            this.puck.setX(this.puck.getX() + transition.getToX());
            this.puck.setY(this.puck.getY() + transition.getToY());
            this.puck.setTranslateX(0);
            this.puck.setTranslateY(0);

            launchReflectionTransition();
        });

        transition.play();
    }
}
