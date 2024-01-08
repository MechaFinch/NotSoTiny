package notsotiny.sim.memory;

/**
 * optionally buffers the screen
 * 
 * @author Mechafinch
 */
public class ScreenBuffer implements MemoryController {
    private byte[] screenBuffer,
                   screenArray;
    
    private boolean buffered = false;
    
    public ScreenBuffer(byte[] screenArray) {
        this.screenBuffer = new byte[screenArray.length];
        this.screenArray = screenArray;
    }

    @Override
    public byte readByte(long address) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void writeByte(long address, byte value) {
        this.buffered = value != 0;
        //System.out.println(this.buffered);
        
        if(this.buffered) {
            System.arraycopy(screenArray, 0, screenBuffer, 0, screenArray.length);
        }
    }
    
    public boolean isBuffered() { return this.buffered; }
    
    public byte[] getScreen() {
        return buffered ? this.screenBuffer : this.screenArray;
    }
}
