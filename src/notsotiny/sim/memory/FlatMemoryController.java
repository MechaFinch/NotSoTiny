package notsotiny.sim.memory;

/**
 * A MemoryController using a flat array
 * 
 * @author Mechafinch
 */
public class FlatMemoryController implements MemoryController {

    byte[] mem;
    boolean readPrivileged, writePrivileged;
    
    public FlatMemoryController(byte[] mem, boolean readPrivileged, boolean writePrivileged) {
        this.mem = mem;
        this.readPrivileged = readPrivileged;
        this.writePrivileged = writePrivileged;
    }
    
    @Override
    public boolean readRequiresPrivilege() {
        return this.readPrivileged;
    }
    
    @Override
    public boolean writeRequiresPrivilege() {
        return this.writePrivileged;
    }
    
    @Override
    public byte readByte(long address) {
        return mem[(int) address];
    }
    
    @Override
    public short read2Bytes(long address) {
        int addr = (int) address;
        
        return (short)((mem[addr] & 0xFF) | (mem[addr + 1] << 8)); 
    }
    
    @Override
    public int read3Bytes(long address) {
        int addr = (int) address;
        
        return (mem[addr] & 0xFF) | ((mem[addr + 1] & 0xFF) << 8) | (mem[addr + 2] << 16);
    }
    
    @Override
    public int read4Bytes(long address) {
        int addr = (int) address;
        
        return (mem[addr] & 0xFF) | ((mem[addr + 1] & 0xFF) << 8) | ((mem[addr + 2] & 0xFF) << 16) | (mem[addr + 3] << 24);
    }
    
    @Override
    public byte[] read2ByteArray(long address) {
        int addr = (int) address;
        
        return new byte[] {
            mem[addr + 0],
            mem[addr + 1]
        };
    }
    
    @Override
    public byte[] read3ByteArray(long address) {
        int addr = (int) address;
        
        return new byte[] {
            mem[addr + 0],
            mem[addr + 1],
            mem[addr + 2]
        };
    }
    
    @Override
    public byte[] read4ByteArray(long address) {
        int addr = (int) address;
        
        return new byte[] {
            mem[addr + 0],
            mem[addr + 1],
            mem[addr + 2],
            mem[addr + 3]
        };
    }

    @Override
    public void writeByte(long address, byte value) {
        mem[(int) address] = value;
    }
    
    @Override
    public void write2Bytes(long address, short value) {
        int addr = (int) address;
        
        mem[addr + 0] = (byte) value;
        mem[addr + 1] = (byte)(value >> 8);
    }
    
    @Override
    public void write3Bytes(long address, int value) {
        int addr = (int) address;
        
        mem[addr + 0] = (byte) value;
        mem[addr + 1] = (byte)(value >> 8);
        mem[addr + 2] = (byte)(value >> 16);
    }
    
    @Override
    public void write4Bytes(long address, int value) {
        int addr = (int) address;
        
        mem[addr + 0] = (byte) value;
        mem[addr + 1] = (byte)(value >> 8);
        mem[addr + 2] = (byte)(value >> 16);
        mem[addr + 3] = (byte)(value >> 24);
    }
}
