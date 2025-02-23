import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URISyntaxException;
import java.net.URL;

public class Puck {
    private @NotNull static final Logger LOGGER = LogManager.getLogger(Puck.class);

    private @NotNull final Rectangle encompassingRect;
    private @Nullable Point2D lastVector;
    private boolean debugMode = false;

    public Puck(@NotNull Rectangle boundingBox, @Nullable URL imageUrl, @Nullable Double sizeMultiplier) {
        this.encompassingRect = new Rectangle();

        // Set puck fill.
        if (imageUrl != null) {
            sizeMultiplier = (sizeMultiplier == null) ? 1.0 : sizeMultiplier;

            try {
                var puckImage = new Image(imageUrl.toURI().toString());

                // Set puck dimensions according to screen resolution.
                double imageAspectRatio = puckImage.getHeight() / puckImage.getWidth();
                this.encompassingRect.setWidth(Math.min(boundingBox.getWidth() * 0.95,
                        puckImage.getWidth()
                                * Constants.AFK_LOGO_DEFAULT_SIZE_MULTIPLIER
                                * sizeMultiplier));
                this.encompassingRect.setHeight(imageAspectRatio * this.encompassingRect.getWidth());

                this.encompassingRect.setFill(new ImagePattern(puckImage));
                LOGGER.info("Successfully loaded image \"{}\" for the puck.", imageUrl);
            } catch (URISyntaxException use) {
                LOGGER.error("Caught a URISyntaxException when trying to turn {} into a URI.", imageUrl);
                this.encompassingRect.setFill(Color.PINK);
            }
        } else {
            LOGGER.warn("Could not find resource for image specified for the puck as the associated URL was null.");
            this.encompassingRect.setFill(Color.PINK);
            this.encompassingRect.setWidth(200);
            this.encompassingRect.setHeight(200);
        }

        // Set random position.
        double randomXPos = Math.random() * (boundingBox.getWidth() - this.encompassingRect.getWidth());
        double randomYPos = Math.random() * (boundingBox.getHeight() - this.encompassingRect.getHeight());

        this.encompassingRect.setX(randomXPos);
        this.encompassingRect.setY(randomYPos);
        this.encompassingRect.setSmooth(true);
    }

    public void toggleDebugMode(){
        this.debugMode = !this.debugMode;

        if (this.debugMode) {
            // Turn on debug mode.
            this.encompassingRect.setStyle("-fx-stroke: green; -fx-stroke-width: 3;");
        } else {
            // Turn off debug mode.
            this.encompassingRect.setStyle("");
        }
    }

    public @NotNull Rectangle getEncompassingRect() {
        return encompassingRect;
    }

    public @Nullable Point2D getLastVector() {
        return lastVector;
    }

    public void setLastVector(@NotNull Point2D lastVector){
        this.lastVector = lastVector;
    }
}
