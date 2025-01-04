import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class Screensaver extends JPanel implements ActionListener {
    private BufferedImage image;
    private BufferedImage scaledImage;
    private int x = 0; // Initial x position
    private int y = 100; // y position (fixed)

    public Screensaver() {
        // Load the image
        try {
            image = ImageIO.read(new File("resources/afk_logo.png")); // Update with your image path
            // Scale the image to a smaller size (e.g., 200x200)
            scaledImage = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = scaledImage.createGraphics();
            g2d.drawImage(image, 0, 0, 200, 200, null);
            g2d.dispose();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Set up a timer to move the image
        Timer timer = new Timer(30, this); // Update every 30 milliseconds
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(scaledImage, x, y, this); // Draw the scaled image at (x, y)
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Update the x position to move the image
        x += 5; // Move 5 pixels to the right
        if (x > getWidth()) {
            x = -scaledImage.getWidth(); // Reset to the left side if it goes off screen
        }
        repaint(); // Repaint the panel
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Moving Image Example");
        Screensaver movingImagePanel = new Screensaver();
        frame.add(movingImagePanel);
        frame.setSize(800, 600); // Set the size of the JFrame
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
