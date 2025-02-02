import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;

public class Utils {
    public static DisplayMode[] getDisplayModes() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();

        return Arrays.stream(gs).map(GraphicsDevice::getDisplayMode).toArray(DisplayMode[]::new);
    }
}
