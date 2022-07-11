package notsotiny.sim.memory;

/**
 * Indicates that a program should halt when its address is read or written to
 * 
 * @author Mechafinch
 */
public class Halter implements MemoryController {
    
    private boolean halted;
    
    public Halter() {
        this.halted = false;
    }
    
    public boolean halted() { return this.halted; }
    
    public void clear() { this.halted = false; }

    @Override
    public byte readByte(long address) {
        this.halted = true;
        return 0;
    }

    @Override
    public void writeByte(long address, byte value) {
        this.halted = true;
    }
}
