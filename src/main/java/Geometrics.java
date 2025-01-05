import javafx.geometry.Point2D;
import javafx.scene.shape.Rectangle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.InvalidParameterException;

public class Geometrics {
    private static final Logger LOGGER = LogManager.getLogger(Geometrics.class);

    public static Point2D getRandomVector() {
        double randomAngleRadians = Math.random() * (2 * Math.PI);
        double x = Math.sin(randomAngleRadians);
        double y = Math.cos(randomAngleRadians);
        Point2D randomVector = (new Point2D(x, y)).normalize();

        LOGGER.debug("Produced random vector for " + randomAngleRadians + " rads: " + randomVector);

        return randomVector;
    }

    public static Point2D getDeflectionVector(Rectangle rect, Point2D incidentVector, Rectangle boundingBox) {
        LOGGER.debug("Determining reflection vector for rect " + rect + "with incident vector " + incidentVector +
                " in bounding box " + boundingBox);
        Direction hitDirection = Geometrics.determineHitDirection(rect, boundingBox);
        Point2D reflectionVector;

        switch (hitDirection) {
            case NORTH, SOUTH -> reflectionVector = new Point2D(incidentVector.getX(), -incidentVector.getY());
            case EAST, WEST -> reflectionVector = new Point2D(-incidentVector.getX(), incidentVector.getY());
            default -> {
                LOGGER.error("could not determine reflection vector for unknown direction \"" + hitDirection + "\"");
                throw new IllegalArgumentException("Unknown direction \"" + hitDirection + "\"");
            }
        }

        LOGGER.debug("Reflection vector is: " + reflectionVector);
        return reflectionVector;
    }

    private static Direction determineHitDirection(Rectangle rect, Rectangle boundingBox){
        LOGGER.debug("Determine hit direction for rect " + rect + " in bounding box " + boundingBox);

        Direction hitDirection = null;
        if (rect.getY() == boundingBox.getY()) {
            hitDirection = Direction.NORTH;
        } else if (rect.getX() + rect.getWidth() == boundingBox.getX() + boundingBox.getWidth()) {
            hitDirection = Direction.EAST;
        } else if (rect.getY() + rect.getHeight() == boundingBox.getY() + boundingBox.getHeight()) {
            hitDirection = Direction.SOUTH;
        } else if (rect.getX() == boundingBox.getX()) {
            hitDirection = Direction.WEST;
        }

        // Sanity check.
        if(hitDirection == null){
            LOGGER.error("Rect " + rect + " does not interfere with any of the sides of the bounding box " + boundingBox);
            throw new IllegalArgumentException("Rectangle does not seem to have hit one of the sides of the surrounding bounding box.");
        }

        return hitDirection;
    }

    public static double getMinimalCollisionT(Rectangle rect, Point2D vector, Rectangle boundingBox) {
        double lt_t = determineCollisionT(new Point2D(rect.getX(), rect.getY()), vector, boundingBox);
        double rt_t = determineCollisionT(new Point2D(rect.getX() + rect.getWidth(), rect.getY()), vector, boundingBox);
        double rb_t = determineCollisionT(new Point2D(rect.getX() + rect.getWidth(), rect.getY() + rect.getHeight()), vector, boundingBox);
        double lb_t = determineCollisionT(new Point2D(rect.getX(), rect.getY() + rect.getHeight()), vector, boundingBox);
        double minT = Math.min(Math.min(lt_t, rt_t), Math.min(rb_t, lb_t));

        LOGGER.debug("Out of the t's " + lt_t + " (LT), " + rt_t + " (RT), " + rb_t + " (RB), and " + lb_t + " (LB) " +
                "determined minimum t as: " + minT);

        return minT;
    }


    private static double determineCollisionT(Point2D startingPoint, Point2D vector, Rectangle boundingBox) {
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

        throw new InvalidParameterException("Could not determine collision t for vector " + vector + " from initial" +
                "starting point " + startingPoint + " with bounding box " + boundingBox);
    }
}
