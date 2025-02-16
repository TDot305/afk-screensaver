import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public class Screensaver {
    private @NotNull
    static final Logger LOGGER = LogManager.getLogger(Screensaver.class);

    private @NotNull
    final Stage stage;
    private @NotNull
    final ScreenSaverConfiguration screenSaverConfiguration;
    private @NotNull
    final Rectangle boundingBox;
    private @NotNull
    final Rectangle primaryPuck;
    private @Nullable Rectangle secondaryPuck;

    private boolean debugMode = false;
    private int pixelsToTraversePerSecond = Constants.DEFAULT_PIXELS_TO_TRAVERSE_PER_SECOND;
    private @Nullable Point2D lastPrimaryPuckVector;
    private @Nullable Point2D lastSecondaryPuckVector;


    public Screensaver(@NotNull Stage stage, @NotNull ScreenSaverConfiguration screenSaverConfiguration) {
        this.stage = stage;
        this.screenSaverConfiguration = screenSaverConfiguration;
        java.awt.Rectangle graphicsBounds = this.screenSaverConfiguration.graphicsDevice().getDefaultConfiguration().getBounds();

        // Basic stage configuration.
        stage.setX(graphicsBounds.getX());
        stage.setY(graphicsBounds.getY());
        stage.setTitle("AFK Screensaver");

        // Initialize bounding box and puck(s).
        this.boundingBox = new Rectangle(
                0,
                0,
                graphicsBounds.getWidth(),
                graphicsBounds.getHeight());
        this.primaryPuck = this.createPuck(
                getClass().getResource(Constants.AFK_LOGO_PATH),
                this.screenSaverConfiguration.primaryPuckSizeMultiplier());
        if (screenSaverConfiguration.secondaryPuck()) {
            try {
                this.secondaryPuck = this.createPuck(
                        this.screenSaverConfiguration.secondaryPuckImage().toURI().toURL(),
                        this.screenSaverConfiguration.secondaryPuckSizeMultiplier());
            } catch (MalformedURLException malformedURLException) {
                this.secondaryPuck = this.createPuck(
                        null,
                        this.screenSaverConfiguration.secondaryPuckSizeMultiplier());
            }
        }
    }

    private @NotNull Rectangle createPuck(@Nullable URL imageUrl, double sizeMultiplier) {
        var puck = new Rectangle();

        // Set puck fill.
        if (imageUrl != null) {
            try {
                var puckImage = new Image(imageUrl.toURI().toString());

                // Set puck dimensions according to screen resolution.
                double imageAspectRatio = puckImage.getHeight() / puckImage.getWidth();
                puck.setWidth(Math.min(this.boundingBox.getWidth() * 0.95,
                        puckImage.getWidth()
                                * Constants.AFK_LOGO_DEFAULT_SIZE_MULTIPLIER
                                * sizeMultiplier));
                puck.setHeight(imageAspectRatio * puck.getWidth());

                puck.setFill(new ImagePattern(puckImage));
                LOGGER.info("Successfully loaded image \"{}\" for the puck.", imageUrl);
            } catch (URISyntaxException use) {
                LOGGER.error("Caught a URISyntaxException when trying to turn {} into a URI.", imageUrl);
                puck.setFill(Color.PINK);
            }
        } else {
            LOGGER.warn("Could not find resource for image specified for the puck as the associated URL was null.");
            puck.setFill(Color.PINK);
            puck.setWidth(200);
            puck.setHeight(200);
        }

        // Set random position.
        double randomXPos = Math.random() * (this.boundingBox.getWidth() - puck.getWidth());
        double randomYPos = Math.random() * (this.boundingBox.getHeight() - puck.getHeight());
        puck.setX(randomXPos);
        puck.setY(randomYPos);

        puck.setSmooth(true);

        return puck;
    }

    public void launchScreensaver() {
        // Basic initialization.
        var group = new Group(this.primaryPuck);

        if (this.screenSaverConfiguration.secondaryPuck()) {
            group.getChildren().add(this.secondaryPuck);
        }

        var scene = new Scene(group);
        scene.setFill(Constants.DEFAULT_SCREENSAVER_BACKGROUND_COLOR);

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

        this.attachHandlers(scene);

        // Actual start procedure.
        var primaryThread = new Thread(this::launchPuck);
        primaryThread.start();
        // TODO Start second thread for secondary puck if necessary.
    }

    private void attachHandlers(@NotNull Scene scene) {
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
                        this.primaryPuck.setStyle("-fx-stroke: green; -fx-stroke-width: 3;");
                    } else {
                        // Turn off debug mode.
                        this.primaryPuck.setStyle("");
                    }

                    this.debugMode = !this.debugMode;
                }
                case F11 -> stage.setFullScreen(!stage.isFullScreen());
            }
        });
    }

    // TODO Make more general such that it can be used to start either the primary or the secondary puck.
    private void launchPuck() {
        Point2D initialVector = Geometrics.getRandomVector();
        this.lastPrimaryPuckVector = initialVector;

        double collisionT = Geometrics.getMinimalCollisionT(primaryPuck, initialVector, boundingBox);
        Point2D collisionLTCoord = new Point2D(primaryPuck.getX(), primaryPuck.getY()).add(initialVector.multiply(collisionT));
        LOGGER.debug("Initial move: Vector = {} Collision-t = {} Collision-Coord = {}", initialVector, collisionT, collisionLTCoord);

        LOGGER.info("Launching initial transition.");
        this.launchNewTransition(collisionLTCoord);
    }

    private void launchNewTransition(Point2D collisionLTCoord) {
        var transition = new TranslateTransition(Duration.seconds(
                collisionLTCoord.distance(primaryPuck.getX(), primaryPuck.getY()) / pixelsToTraversePerSecond), primaryPuck);

        transition.setToX(collisionLTCoord.getX() - primaryPuck.getX());
        transition.setToY(collisionLTCoord.getY() - primaryPuck.getY());
        transition.setInterpolator(Interpolator.LINEAR);
        transition.setOnFinished(e -> {
            // Move to new position.
            this.primaryPuck.setX(this.primaryPuck.getX() + transition.getToX());
            this.primaryPuck.setY(this.primaryPuck.getY() + transition.getToY());
            this.primaryPuck.setTranslateX(0);
            this.primaryPuck.setTranslateY(0);

            launchReflectionTransition();
        });

        transition.play();
    }

    private void launchReflectionTransition() {
        // Determine side that has been hit.
        Point2D reflectionVector = Geometrics.getDeflectionVector(this.primaryPuck, this.lastPrimaryPuckVector, this.boundingBox);
        this.lastPrimaryPuckVector = reflectionVector;
        double collisionT = Geometrics.getMinimalCollisionT(primaryPuck, reflectionVector, boundingBox);
        Point2D collisionLTCoord = new Point2D(primaryPuck.getX(), primaryPuck.getY()).add(reflectionVector.multiply(collisionT));

        LOGGER.debug("Collision Point Calculation: rect[{}, {}] + {} * {} = {}",
                primaryPuck.getX(), primaryPuck.getY(), collisionT, reflectionVector, collisionLTCoord);

        this.launchNewTransition(collisionLTCoord);
    }
}
