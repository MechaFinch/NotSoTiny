package notsotiny.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Greyscale bytemap of a section of memory
 * 
 * @author Alex Pickering
 */
public class Screen extends Canvas {
    
    private int screenWidth,
                screenHeight,
                pixelSize;
    
    public Screen(int screenWidth, int screenHeight, int pixelSize) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.pixelSize = pixelSize;
        
        this.setWidth(screenWidth * pixelSize);
        this.setHeight(screenHeight * pixelSize);
    }
    
    /**
     * Updates the screen
     * 
     * @param mem
     * @param startAddress
     */
    public void update(byte[] mem, int startAddress) {
        GraphicsContext g = this.getGraphicsContext2D();
        
        // clear
        g.clearRect(0, 0, screenWidth * pixelSize, screenHeight * pixelSize);
        g.setLineWidth(1);
        
        // draw
        for(int y = 0; y < this.screenHeight; y++) {
            for(int x = 0; x < this.screenWidth; x++) {
                byte b = mem[startAddress + x + (y * this.screenWidth)];
                
                int red = ((b >> 5) & 0x07) * (256 / 7),
                    green = ((b >> 2) & 0x07) * (256 / 7),
                    blue = (b & 0x03) * (256 / 3);
                
                //g.setFill(Color.grayRgb(b & 0xFF));
                g.setFill(Color.rgb(red, green, blue));
                g.fillRect(x * pixelSize, y * pixelSize, pixelSize, pixelSize);
            }
        }
    }
}
