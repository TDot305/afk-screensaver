import java.awt.GraphicsDevice;
import java.io.File;

public record ScreenSaverConfiguration(File backgroundImage,
                                       GraphicsDevice graphicsDevice,
                                       double primaryPuckSizeMultiplier,
                                       boolean secondaryPuck,
                                       File secondaryPuckImage,
                                       double secondaryPuckSizeMultiplier) {
}
