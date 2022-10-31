package notsotiny.sim.memory;

import java.util.Random;

/**
 * A MemoryController which gives random numbers on read
 * 
 * @author Mechafinch
 */
public class RandomController implements MemoryController {
    
    private Random rand;
    
    public RandomController() {
        this.rand = new Random();
    }

    @Override
    public byte readByte(long address) {
        return (byte) this.rand.nextInt();
    }

    @Override
    public void writeByte(long address, byte value) {
        // n/a
    }
    
}
