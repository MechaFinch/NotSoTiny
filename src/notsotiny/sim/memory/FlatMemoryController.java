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
    public byte readByte(int address) {
        return mem[address];
    }

    @Override
    public void writeByte(int address, byte value) {
        mem[address] = value;
    }
    
}
