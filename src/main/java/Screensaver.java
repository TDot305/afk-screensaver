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
    final Puck primaryPuck;
    private @Nullable Puck secondaryPuck;

    private int pixelsToTraversePerSecond = Constants.DEFAULT_PIXELS_TO_TRAVERSE_PER_SECOND;


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
        this.primaryPuck = new Puck(this.boundingBox,
                getClass().getResource(Constants.AFK_LOGO_PATH),
                this.screenSaverConfiguration.primaryPuckSizeMultiplier());
        if (screenSaverConfiguration.secondaryPuck()) {
            try {
                this.secondaryPuck = new Puck(this.boundingBox,
                        this.screenSaverConfiguration.secondaryPuckImage().toURI().toURL(),
                        this.screenSaverConfiguration.secondaryPuckSizeMultiplier());
            } catch (MalformedURLException malformedURLException) {
                this.secondaryPuck = new Puck(this.boundingBox,
                        null,
                        null);
            }
        }
    }

    public void launchScreensaver() {
        // Basic initialization.
        var group = new Group(this.primaryPuck.getEncompassingRect());

        if (this.secondaryPuck != null) {
            group.getChildren().add(this.secondaryPuck.getEncompassingRect());
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
        }

        this.attachHandlers(scene);

        // Actual start procedure.
        var primaryThread = new Thread(() -> Screensaver.this.launchPuck(Screensaver.this.primaryPuck));
        primaryThread.start();

        // TODO Start second thread for secondary puck if necessary.
        if(this.secondaryPuck != null) {
            var secondaryThread = new Thread(() -> Screensaver.this.launchPuck(Screensaver.this.secondaryPuck));
            secondaryThread.start();
        }
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
                case F10 -> this.primaryPuck.toggleDebugMode();
                case F11 -> stage.setFullScreen(!stage.isFullScreen());
            }
        });
    }

    // TODO Make more general such that it can be used to start either the primary or the secondary puck.
    private void launchPuck(@NotNull Puck puck) {
        Point2D initialVector = Geometrics.getRandomVector();
        puck.setLastVector(initialVector);

        double collisionT = Geometrics.getMinimalCollisionT(puck.getEncompassingRect(), initialVector, this.boundingBox);
        var collisionLTCoord = new Point2D(
                puck.getEncompassingRect().getX(),
                puck.getEncompassingRect().getY()).add(initialVector.multiply(collisionT));
        LOGGER.debug("Initial move: Vector = {} Collision-t = {} Collision-Coord = {}",
                initialVector, collisionT, collisionLTCoord);

        LOGGER.info("Launching initial transition.");
        this.launchNewTransition(puck, collisionLTCoord);
    }

    private void launchNewTransition(@NotNull Puck puck, @NotNull Point2D collisionLTCoord) {
        var transition = new TranslateTransition(Duration.seconds(
                collisionLTCoord.distance(puck.getEncompassingRect().getX(), puck.getEncompassingRect().getY()) / pixelsToTraversePerSecond), puck.getEncompassingRect());

        transition.setToX(collisionLTCoord.getX() - puck.getEncompassingRect().getX());
        transition.setToY(collisionLTCoord.getY() - puck.getEncompassingRect().getY());
        transition.setInterpolator(Interpolator.LINEAR);
        transition.setOnFinished(e -> {
            // Move to new position.
            puck.getEncompassingRect().setX(puck.getEncompassingRect().getX() + transition.getToX());
            puck.getEncompassingRect().setY(puck.getEncompassingRect().getY() + transition.getToY());
            puck.getEncompassingRect().setTranslateX(0);
            puck.getEncompassingRect().setTranslateY(0);

            launchReflectionTransition(puck);
        });

        transition.play();
    }

    private void launchReflectionTransition(@NotNull Puck puck) {
        // Determine side that has been hit.
        Point2D reflectionVector = Geometrics.getDeflectionVector(
                puck.getEncompassingRect(),
                puck.getLastVector(),
                this.boundingBox);
        puck.setLastVector(reflectionVector);

        double collisionT = Geometrics.getMinimalCollisionT(
                puck.getEncompassingRect(),
                reflectionVector,
                this.boundingBox);
        Point2D collisionLTCoord = new Point2D(
                puck.getEncompassingRect().getX(),
                puck.getEncompassingRect().getY()).add(reflectionVector.multiply(collisionT));

        LOGGER.debug("Collision Point Calculation: rect[{}, {}] + {} * {} = {}",
                puck.getEncompassingRect().getX(),
                puck.getEncompassingRect().getY(), collisionT, reflectionVector, collisionLTCoord);

        this.launchNewTransition(puck, collisionLTCoord);
    }
}
