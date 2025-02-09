import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

public class Utils {
    public static GraphicsDevice[] getGraphicsDevices() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        return ge.getScreenDevices();
    }
}
