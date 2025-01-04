import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.io.File;
import java.security.InvalidParameterException;


public class AfkScreensaver extends Application {
    private Dimension screenDimensions = Toolkit.getDefaultToolkit().getScreenSize();
    private Dimension2D rectDimension = new Dimension2D(181, 80);
    private Point2D startingCoordinate = new Point2D(
            screenDimensions.getWidth() / 2 - rectDimension.getWidth() / 2,
            screenDimensions.getHeight() / 2 - rectDimension.getHeight() / 2);
    private int pixelsToTraversePerSecond = 300;


    private Rectangle boundingBox = new Rectangle(0, 0, this.screenDimensions.getWidth(), this.screenDimensions.getHeight());
    private Rectangle rect = new Rectangle(startingCoordinate.getX(), startingCoordinate.getY(), rectDimension.getWidth(), rectDimension.getHeight());

    private Point2D currentPosition;
    private Point2D lastVector;

    @Override
    public void start(Stage stage) {
        stage.setFullScreen(true);
        stage.setTitle("AFK Screensaver");

        //this.rect.setFill(Color.WHITE);
        Image img = new Image(new File("resources/afk_logo.png").toURI().toString());
        this.rect.setFill(new ImagePattern(img));
        this.rect.setStyle("-fx-stroke: green; -fx-stroke-width: 5;");

        var group = new Group(rect);
        var scene = new Scene(group);
        scene.setFill(Color.BLACK);
        stage.setScene(scene);
        stage.show();

        scene.setOnScroll((ScrollEvent se) -> {
            this.pixelsToTraversePerSecond = (int) Math.max(10, this.pixelsToTraversePerSecond + se.getDeltaY());
        });

        Point2D initialVector = getRandomVector();
        double collisionT = getMinimalCollisionT(rect, initialVector, boundingBox);
        Point2D collisionPoint = new Point2D(rect.getX(), rect.getY()).add(initialVector.multiply(collisionT));

        var transition = new TranslateTransition(Duration.seconds(collisionPoint.distance(rect.getX(), rect.getY()) / pixelsToTraversePerSecond), rect);
        transition.setToX(collisionPoint.getX() - rect.getX());
        transition.setToY(collisionPoint.getY() - rect.getY());
        transition.setInterpolator(Interpolator.LINEAR);

        transition.setOnFinished(e -> {
            System.out.println("Initial Finished");

            this.rect.setX(this.rect.getX() + transition.getToX());
            this.rect.setY(this.rect.getY() + transition.getToY());
            this.rect.setTranslateX(0);
            this.rect.setTranslateY(0);

            System.out.println("New coords: " + this.rect.getX() + " | " + this.rect.getY());
            this.currentPosition = new Point2D(rect.getX(), rect.getY());
            this.lastVector = initialVector;
            triggerReflectionTransition();
        });
        transition.play();

    }

    private void triggerReflectionTransition() {
        // Determine side that has been hit.
        Direction hitDirection = null;
        if (this.rect.getY() == this.boundingBox.getY()) {
            hitDirection = Direction.NORTH;
        } else if (this.rect.getX() + this.rect.getWidth() == this.boundingBox.getX() + this.boundingBox.getWidth()) {
            hitDirection = Direction.EAST;
        } else if (this.rect.getY() + this.rect.getHeight() == this.boundingBox.getY() + this.boundingBox.getHeight()) {
            hitDirection = Direction.SOUTH;
        } else if (this.rect.getX() == this.boundingBox.getX()) {
            hitDirection = Direction.WEST;
        }
        System.out.println("Pos: " + this.rect + " Dir: " + hitDirection + " Vector: " + this.lastVector);
        assert hitDirection != null;

        Point2D reflectionVector = getDeflectionVector(this.lastVector, hitDirection);
        double collisionT = getMinimalCollisionT(rect, reflectionVector, boundingBox);
        Point2D collisionPoint = new Point2D(rect.getX(), rect.getY()).add(reflectionVector.multiply(collisionT));

        System.out.println("Reflection vector: " + reflectionVector + " Collision t: " + collisionT + " Collision point: " + collisionPoint);

        var transition = new TranslateTransition(Duration.seconds(collisionPoint.distance(rect.getX(), rect.getY()) / pixelsToTraversePerSecond), rect);
        transition.setToX(collisionPoint.getX() - rect.getX());
        transition.setToY(collisionPoint.getY() - rect.getY());
        transition.setInterpolator(Interpolator.LINEAR);

        transition.setOnFinished(e -> {
            System.out.println("Finished");
            this.rect.setX(this.rect.getX() + transition.getToX());
            this.rect.setY(this.rect.getY() + transition.getToY());
            this.rect.setTranslateX(0);
            this.rect.setTranslateY(0);

            this.currentPosition = new Point2D(rect.getX(), rect.getY());
            this.lastVector = reflectionVector;
            triggerReflectionTransition();
        });
        transition.play();
    }

    private Point2D getRandomVector() {
        double randomAngleRadians = Math.random() * (2 * Math.PI);

        double x = Math.sin(randomAngleRadians);
        double y = Math.cos(randomAngleRadians);

        return (new Point2D(x, y)).normalize();
    }

    private Point2D getDeflectionVector(Point2D incidentVector, Direction direction) {
        switch (direction) {
            case NORTH:
            case SOUTH:
                return new Point2D(incidentVector.getX(), -incidentVector.getY());
            case EAST:
            case WEST:
                return new Point2D(-incidentVector.getX(), incidentVector.getY());
            default:
                throw new IllegalArgumentException("Unknown direction \"" + direction + "\"");
        }
    }

    private double getMinimalCollisionT(Rectangle rect, Point2D vector, Rectangle boundingBox) {
        double lt_t = determineCollisionT(new Point2D(rect.getX(), rect.getY()), vector, boundingBox);
        double rt_t = determineCollisionT(new Point2D(rect.getX() + rect.getWidth(), rect.getY()), vector, boundingBox);
        double rb_t = determineCollisionT(new Point2D(rect.getX() + rect.getWidth(), rect.getY() + rect.getHeight()), vector, boundingBox);
        double lb_t = determineCollisionT(new Point2D(rect.getX(), rect.getY() + rect.getHeight()), vector, boundingBox);

        return Math.min(Math.min(lt_t, rt_t), Math.min(rb_t, lb_t));
    }

    private double determineCollisionT(Point2D startingPoint, Point2D vector, Rectangle boundingBox) {
        var boundingBoxLT = new Point2D(boundingBox.getX(), boundingBox.getY());
        var boundingBoxRB = new Point2D(boundingBox.getX() + boundingBox.getWidth(), boundingBox.getY() + boundingBox.getHeight());


        // Sanity check.
        if (startingPoint.getX() < boundingBoxLT.getX() || startingPoint.getX() > boundingBoxRB.getX()
                || startingPoint.getY() < boundingBoxLT.getY() || startingPoint.getY() > boundingBoxRB.getY()) {
            throw new IllegalArgumentException("Starting point not situated within the bounding box.");
        }

        // North border.
        // Test whether collision with top is even possible.
        if (Math.abs(vector.getY()) > 0 || startingPoint.getY() == boundingBoxLT.getY()) {
            double t = (boundingBoxLT.getY() - startingPoint.getY()) / vector.getY();

            // Discard if vector would need to be traversed backwards.
            if (t > 0) {
                var potentialCollisionPoint = startingPoint.add(vector.multiply(t));

                // Horizontal boundary check
                if (potentialCollisionPoint.getX() >= boundingBoxLT.getX()
                        && potentialCollisionPoint.getX() <= boundingBoxRB.getX()) {
                    return t;
                }
            }
        }

        // East border.
        // Test whether collision with right side is even possible.
        if (Math.abs(vector.getX()) > 0 || startingPoint.getX() == boundingBoxRB.getX()) {
            double t = (boundingBoxRB.getX() - startingPoint.getX()) / vector.getX();

            // Discard if vector would need to be traversed backwards.
            if (t > 0) {
                var potentialCollisionPoint = startingPoint.add(vector.multiply(t));

                // Vertical boundary check
                if (potentialCollisionPoint.getY() >= boundingBoxLT.getY()
                        && potentialCollisionPoint.getY() <= boundingBoxRB.getY()) {
                    return t;
                }
            }
        }

        // South border.
        // Test whether collision with bottom is even possible.
        if (Math.abs(vector.getY()) > 0 || startingPoint.getY() == boundingBoxRB.getY()) {
            double t = (boundingBoxRB.getY() - startingPoint.getY()) / vector.getY();

            // Discard if vector would need to be traversed backwards.
            if (t > 0) {
                var potentialCollisionPoint = startingPoint.add(vector.multiply(t));

                // Horizontal boundary check
                if (potentialCollisionPoint.getX() >= boundingBoxLT.getX()
                        && potentialCollisionPoint.getX() <= boundingBoxRB.getX()) {
                    return t;
                }
            }
        }

        // West border.
        // Test whether collision with left side is even possible.
        if (Math.abs(vector.getX()) > 0 || startingPoint.getX() == boundingBoxLT.getX()) {
            double t = (boundingBoxLT.getX() - startingPoint.getX()) / vector.getX();

            // Discard if vector would need to be traversed backwards.
            if (t > 0) {
                var potentialCollisionPoint = startingPoint.add(vector.multiply(t));

                // Vertical boundary check
                if (potentialCollisionPoint.getY() >= boundingBoxLT.getY()
                        && potentialCollisionPoint.getY() <= boundingBoxRB.getY()) {
                    return t;
                }
            }
        }

        // TODO
        throw new InvalidParameterException();
    }
}
