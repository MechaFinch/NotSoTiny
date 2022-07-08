package notsotiny.sim.memory;

/**
 * A MemoryController using a flat array
 * 
 * @author Mechafinch
 */
public class FlatMemoryController implements MemoryController {

    byte[] mem;
    
    public FlatMemoryController(byte[] mem) {
        this.mem = mem;
    }
    
    @Override
    public byte readByte(long address) {
        return mem[(int) address];
    }

    @Override
    public void writeByte(long address, byte value) {
        mem[(int) address] = value;
    }
    
}
