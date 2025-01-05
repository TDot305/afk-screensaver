import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;


public class AfkScreensaver extends Application {
    private static final Logger LOGGER = LogManager.getLogger(AfkScreensaver.class);

    private final Dimension screenDimensions = Toolkit.getDefaultToolkit().getScreenSize();
    private final Dimension2D rectDimension = new Dimension2D(180, 80);
    private final Point2D startingCoordinate = new Point2D(
            screenDimensions.getWidth() / 2 - rectDimension.getWidth() / 2,
            screenDimensions.getHeight() / 2 - rectDimension.getHeight() / 2);

    private int pixelsToTraversePerSecond = 300;
    private final Rectangle boundingBox = new Rectangle(0, 0, this.screenDimensions.getWidth(), this.screenDimensions.getHeight());
    private final Rectangle rect = new Rectangle(startingCoordinate.getX(), startingCoordinate.getY(), rectDimension.getWidth(), rectDimension.getHeight());

    private Point2D lastVector;

    @Override
    public void start(Stage stage) {
        LOGGER.info("Starting AFK-Screensaver.");

        stage.setFullScreen(true);
        stage.setTitle("AFK Screensaver");

        Image img = new Image(new File("resources/afk_logo.png").toURI().toString());
        this.rect.setFill(new ImagePattern(img));
        this.rect.setStyle("-fx-stroke: green; -fx-stroke-width: 3;");
        this.rect.setSmooth(true);

        var group = new Group(rect);
        var scene = new Scene(group);
        scene.setFill(Color.BLACK);
        stage.setScene(scene);
        stage.show();

        scene.setOnKeyPressed(keyEvent -> {
            switch (keyEvent.getCode()) {
                case UP -> this.pixelsToTraversePerSecond += 20;
                case DOWN -> this.pixelsToTraversePerSecond = Math.max(10, this.pixelsToTraversePerSecond - 20);
            }
        });


        Point2D initialVector = Geometrics.getRandomVector();
        double collisionT = Geometrics.getMinimalCollisionT(rect, initialVector, boundingBox);
        Point2D collisionLTCoord = new Point2D(rect.getX(), rect.getY()).add(initialVector.multiply(collisionT));
        LOGGER.debug("Initial move: Vector = " + initialVector + " Collision-t = " + collisionT + " Collision-Coord = " + collisionLTCoord);

        var transition = new TranslateTransition(Duration.seconds(collisionLTCoord.distance(rect.getX(), rect.getY()) / pixelsToTraversePerSecond), rect);
        transition.setToX(collisionLTCoord.getX() - rect.getX());
        transition.setToY(collisionLTCoord.getY() - rect.getY());
        transition.setInterpolator(Interpolator.LINEAR);
        transition.setOnFinished(e -> {
            this.rect.setX(this.rect.getX() + transition.getToX());
            this.rect.setY(this.rect.getY() + transition.getToY());
            this.rect.setTranslateX(0);
            this.rect.setTranslateY(0);

            this.lastVector = initialVector;
            triggerReflectionTransition();
        });

        LOGGER.info("Launching initial transition.");
        transition.play();

    }

    private void triggerReflectionTransition() {
        // Determine side that has been hit.
        Point2D reflectionVector = Geometrics.getDeflectionVector(this.rect, this.lastVector, this.boundingBox);
        double collisionT = Geometrics.getMinimalCollisionT(rect, reflectionVector, boundingBox);
        Point2D collisionPoint = new Point2D(rect.getX(), rect.getY()).add(reflectionVector.multiply(collisionT));

        var transition = new TranslateTransition(Duration.seconds(collisionPoint.distance(rect.getX(), rect.getY()) / pixelsToTraversePerSecond), rect);
        transition.setToX(collisionPoint.getX() - rect.getX());
        transition.setToY(collisionPoint.getY() - rect.getY());
        transition.setInterpolator(Interpolator.LINEAR);

        transition.setOnFinished(e -> {
            this.rect.setX(this.rect.getX() + transition.getToX());
            this.rect.setY(this.rect.getY() + transition.getToY());
            this.rect.setTranslateX(0);
            this.rect.setTranslateY(0);

            this.lastVector = reflectionVector;
            triggerReflectionTransition();
        });
        transition.play();
    }


}
