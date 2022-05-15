package notsotiny.sim;

/**
 * An interface for memory, to enable wide address spaces that don't take up tons of physical memory
 * 
 * @author Mechafinch
 */
public interface MemoryController {
    
    /**
     * Get 1 byte from an address
     * 
     * @param address
     * @return
     */
    public byte readByte(int address);
    
    /**
     * Get 2 little-endian bytes from an address
     * 
     * @param address
     * @return
     */
    public default short read2Bytes(int address) {
        return (short)((this.readByte(address) & 0xFF) | (this.readByte(address + 1) << 8)); 
    }
    
    /**
     * Get 3 little-endian bytes from an address
     * 
     * @param address
     * @return
     */
    public default int read3Bytes(int address) {
        return (this.readByte(address) & 0xFF) | ((this.readByte(address + 1) & 0xFF) << 8) | ((this.readByte(address + 2) & 0xFF) << 16);
    }
    
    /**
     * Get 4 little-endian bytes from an address
     * 
     * @param address
     * @return
     */
    public default int read4Bytes(int address) {
        return (this.readByte(address) & 0xFF) | ((this.readByte(address + 1) & 0xFF) << 8) | ((this.readByte(address + 2) & 0xFF) << 16) | ((this.readByte(address + 3) & 0xFF) << 24);
    }
    
    /**
     * Set 1 byte at an address
     * 
     * @param address
     * @param value
     */
    public void writeByte(int address, byte value);
    
    /**
     * Set 2 little-endian bytes at an address
     * 
     * @param address
     * @param value
     */
    public default void write2Bytes(int address, short value) {
        this.writeByte(address + 0, (byte) value);
        this.writeByte(address + 1, (byte) (value >> 8));
    }
    
    /**
     * Set 3 little-endian bytes at an address
     * 
     * @param address
     * @param value
     */
    public default void write3Bytes(int address, int value) {
        this.writeByte(address + 0, (byte) value);
        this.writeByte(address + 1, (byte) (value >> 8));
        this.writeByte(address + 2, (byte) (value >> 16));
    }
    
    /**
     * Set 4 little-endian bytes at an address
     * 
     * @param address
     * @param value
     */
    public default void write4Bytes(int address, int value) {
        this.writeByte(address + 0, (byte) value);
        this.writeByte(address + 1, (byte) (value >> 8));
        this.writeByte(address + 2, (byte) (value >> 16));
        this.writeByte(address + 3, (byte) (value >> 24));
    }
}
