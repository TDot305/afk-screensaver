import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.*;
import java.security.InvalidParameterException;


public class AfkScreensaver extends Application {
    private Dimension screenDimensions = Toolkit.getDefaultToolkit().getScreenSize();
    private Point2D startingCoordinate = new Point2D(screenDimensions.getWidth() / 2, screenDimensions.getHeight() / 2);
    private int circleRadius = 15;

    @Override
    public void start(Stage stage) throws Exception {


        stage.setFullScreen(true);
        stage.setTitle("AFK Screensaver");


        var circle = new Circle(startingCoordinate.getX(), startingCoordinate.getY(), 2 * circleRadius);
        var group = new Group(circle);

        var scene = new Scene(group);
        stage.setScene(scene);
        stage.show();

        var transition = new TranslateTransition(Duration.seconds(5), circle);
        transition.setToX(800);
        transition.setToY(800);
        transition.setInterpolator(Interpolator.LINEAR);

        //transition.onfin;
        circle.setFill(Color.GREEN);


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

    private Point2D determineCollisionPoint(Point2D startingPoint, Point2D vector) {
        var boundingBoxLT = new Point2D(0,0);
        var boundingBoxRB = new Point2D(this.screenDimensions.getWidth(),this.screenDimensions.getHeight());

        // Sanity check.
        if(startingPoint.getX() >= boundingBoxLT.getX() && startingPoint.getX() <= boundingBoxRB.getX()
                && startingPoint.getY() >= boundingBoxLT.getY() && startingPoint.getY() <= boundingBoxRB.getY()) {
            throw new IllegalArgumentException("Starting point not situated within the bounding box.");
        }

        // Top border.
        // Test whether collision with top is even possible.
        if(Math.abs(vector.getY()) > 0 || startingPoint.getY() == boundingBoxLT.getY()){
            double t = (boundingBoxLT.getY() - startingPoint.getY()) / vector.getY();

            // Discard if vector would need to be traversed backwards.
            if(t >= 0){
                var potentialCollisionPoint = startingPoint.add(vector.multiply(t));

                // Horizontal boundary check
                if(potentialCollisionPoint.getX() >= boundingBoxLT.getX()
                        && potentialCollisionPoint.getX() <= boundingBoxRB.getX()){
                    return potentialCollisionPoint;
                }
            }
        }

        // TODO
        throw new InvalidParameterException();
    }
}
