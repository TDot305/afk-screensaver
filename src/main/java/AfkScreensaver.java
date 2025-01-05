import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.net.URISyntaxException;
import java.net.URL;


public class AfkScreensaver extends Application {
    private static final Logger LOGGER = LogManager.getLogger(AfkScreensaver.class);

    private static final String AFK_LOGO_NAME = "afk_logo.png";

    private final Dimension screenDimensions = Toolkit.getDefaultToolkit().getScreenSize();
    private final Rectangle boundingBox = new Rectangle(0, 0, this.screenDimensions.getWidth(), this.screenDimensions.getHeight());

    /**
     * Default dimensions of AFK logo puck.
     */
    private final Dimension2D defaultRectDimension = new Dimension2D(400, 130);
    private final Point2D defaultStartingCoordinate = new Point2D(
            screenDimensions.getWidth() / 2 - defaultRectDimension.getWidth() / 2,
            screenDimensions.getHeight() / 2 - defaultRectDimension.getHeight() / 2);
    private final Rectangle rect = new Rectangle(defaultStartingCoordinate.getX(), defaultStartingCoordinate.getY(), defaultRectDimension.getWidth(), defaultRectDimension.getHeight());

    private int pixelsToTraversePerSecond = 300;
    private Point2D lastVector;
    private boolean debugMode = false;

    @Override
    public void start(Stage stage) {
        LOGGER.info("Starting AFK-Screensaver.");

        stage.setFullScreen(true);
        stage.setTitle("AFK Screensaver");

        // Configure puck.
        this.rect.setSmooth(true);

        URL afkImageResource = getClass().getResource(AfkScreensaver.AFK_LOGO_NAME);
        if (afkImageResource != null) {
            try {
                Image afkImage = new Image(afkImageResource.toURI().toString());

                System.out.println("Image dimensions: " + afkImage.getWidth() + " | " + afkImage.getHeight());
                // Set puck dimensions according to screen resolution.
                double afkLogoAspectRatio = afkImage.getHeight() / afkImage.getWidth();
                this.rect.setWidth(0.15 * afkImage.getWidth());
                this.rect.setHeight(afkLogoAspectRatio * this.rect.getWidth());

                this.rect.setFill(new ImagePattern(afkImage));
                LOGGER.info("Successfully loaded AFK logo for the puck.");
            } catch (URISyntaxException use){
                LOGGER.error("Caught a URISyntaxException when trying to turn " + afkImageResource + " into a URI.");
                this.rect.setFill(Color.PINK);
            }
        } else {
            LOGGER.warn("Could not find resource for AFK logo with name \"" + AfkScreensaver.AFK_LOGO_NAME + "\"");
            this.rect.setFill(Color.PINK);
        }

        // Placing puck at random position.
        double randomXPos = Math.random() * (this.boundingBox.getWidth() - this.rect.getWidth());
        double randomYPos = Math.random() * (this.boundingBox.getHeight() - this.rect.getHeight());
        this.rect.setX(randomXPos);
        this.rect.setY(randomYPos);

        var group = new Group(rect);
        var scene = new Scene(group);
        scene.setFill(Color.BLACK);
        stage.setScene(scene);
        stage.show();

        // Attach handlers.
        stage.addEventFilter(KeyEvent.KEY_PRESSED, keyEvent -> {
            switch (keyEvent.getCode()) {
                case UP -> this.pixelsToTraversePerSecond += 20; // TODO Small information overlay.
                case DOWN -> this.pixelsToTraversePerSecond = Math.max(10, this.pixelsToTraversePerSecond - 20);
                case F1 -> {
                    // TODO Background picker.
                }
                case F11 -> stage.setFullScreen(!stage.isFullScreen());
                case F10 -> {
                    if(!this.debugMode){
                        // Turn on debug mode.
                        this.rect.setStyle("-fx-stroke: green; -fx-stroke-width: 3;");
                    } else {
                        // Turn off debug mode.
                        this.rect.setStyle("");
                    }

                    this.debugMode = !this.debugMode;
                }
            }
        });


        Point2D initialVector = Geometrics.getRandomVector();
        this.lastVector = initialVector;

        double collisionT = Geometrics.getMinimalCollisionT(rect, initialVector, boundingBox);
        Point2D collisionLTCoord = new Point2D(rect.getX(), rect.getY()).add(initialVector.multiply(collisionT));
        LOGGER.debug("Initial move: Vector = " + initialVector + " Collision-t = " + collisionT + " Collision-Coord = " + collisionLTCoord);

        LOGGER.info("Launching initial transition.");
        this.launchNewTransition(collisionLTCoord);
    }

    private void launchReflectionTransition() {
        // Determine side that has been hit.
        Point2D reflectionVector = Geometrics.getDeflectionVector(this.rect, this.lastVector, this.boundingBox);
        this.lastVector = reflectionVector;
        double collisionT = Geometrics.getMinimalCollisionT(rect, reflectionVector, boundingBox);
        Point2D collisionLTCoord = new Point2D(rect.getX(), rect.getY()).add(reflectionVector.multiply(collisionT));

        LOGGER.debug("Collision Point Calculation: rect[" + rect.getX() + ", " + rect.getY() + "] + " + collisionT +
                " * " + reflectionVector + " = " + collisionLTCoord);

        this.launchNewTransition(collisionLTCoord);
    }

    private void launchNewTransition(Point2D collisionLTCoord){
        var transition = new TranslateTransition(Duration.seconds(
                collisionLTCoord.distance(rect.getX(), rect.getY()) / pixelsToTraversePerSecond), rect);

        transition.setToX(collisionLTCoord.getX() - rect.getX());
        transition.setToY(collisionLTCoord.getY() - rect.getY());
        transition.setInterpolator(Interpolator.LINEAR);
        transition.setOnFinished(e -> {
            // Move to new position.
            this.rect.setX(this.rect.getX() + transition.getToX());
            this.rect.setY(this.rect.getY() + transition.getToY());
            this.rect.setTranslateX(0);
            this.rect.setTranslateY(0);

            launchReflectionTransition();
        });

        transition.play();
    }

}
