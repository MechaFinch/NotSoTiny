package notsotiny.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import notsotiny.sim.memory.MemoryController;

/**
 * black & white bitmap of a section of memory
 * 
 * @author Alex Pickering
 */
public class Screen extends Canvas {
    
    private int screenWidth,
                screenHeight,
                pixelSize,
                widthBytes;
    
    /**
     * Creates a blank screen of a given size
     * 
     * @param screenWidth
     * @param screenHeight
     * @param pixelSize
     */
    public Screen(int screenWidth, int screenHeight, int pixelSize) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.pixelSize = pixelSize;
        
        this.widthBytes = this.screenWidth / 8;
        
        this.setWidth(screenWidth * pixelSize);
        this.setHeight(screenHeight * pixelSize);
    }
    
    /**
     * Updates the screen
     * 
     * @param mem
     * @param startAddress
     */
    public void update(MemoryController mem, int startAddress) {
        GraphicsContext g = this.getGraphicsContext2D();
        
        // clear
        g.clearRect(0, 0, screenWidth * pixelSize, screenHeight * pixelSize);
        g.setLineWidth(1);
        
        // draw
        for(int y = 0; y < this.screenHeight; y++) {
            for(int x = 0; x < this.widthBytes; x++) {
                byte b = mem.readByte(startAddress + x + (y * this.widthBytes));
                
                for(int i = 7; i >= 0; i--) {
                    if(((b >> i) & 0x01) == 1) {
                        g.setFill(Color.WHITE);
                    } else {
                        g.setFill(Color.BLACK);
                    }
                    
                    g.fillRect(((x * 8) + 7 - i) * pixelSize, y * pixelSize, pixelSize, pixelSize);
                }
            }
        }
    }
}
