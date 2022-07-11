package notsotiny.sim.memory;

import java.util.HashMap;
import java.util.TreeSet;

/**
 * A MemoryController container which can register other MemoryControllers with their own address spaces 
 * 
 * @author Mechafinch
 */
public class MemoryManager implements MemoryController {
    
    private HashMap<Long, MemoryController> segmentControllers; // maps starting address to controller
    
    private TreeSet<Long> startAddresses; // good old tree for finding the right memory segment fast
    
    private HashMap<Long, Long> endAddresses; // maps starting address to ending address
    
    /**
     * Create an empty MemoryManager
     */
    public MemoryManager() {
        this.segmentControllers = new HashMap<>();
        this.startAddresses = new TreeSet<>();
        this.endAddresses = new HashMap<>();
    }
    
    /**
     * Adds a new segment to the memory map
     * 
     * @param mc Segment controller
     * @param start Start address
     * @param end End address
     * @throws IllegalArgumentException if the specified range overlaps with an existing segment
     */
    public void registerSegment(MemoryController mc, long start, long size) {
        // check for overlap
        if(this.segmentControllers.size() != 0) {
            long closestSegmentStart = this.startAddresses.floor(start + size - 1),
                 closestSegmentEnd = this.endAddresses.get(closestSegmentStart);
            
            if(closestSegmentStart > start || closestSegmentEnd > start) {
                throw new IllegalArgumentException("Memory segments cannot overlap");
            }
        }
        
        // add
        this.startAddresses.add(start);
        this.endAddresses.put(start, start + size - 1);
        this.segmentControllers.put(start, mc);
    }
    
    /**
     * Remove a segment from the memory map
     * 
     * @param address Start address of the segment to remove
     */
    public void removeSegment(long address) {
        this.startAddresses.remove(address);
        this.endAddresses.remove(address);
        this.segmentControllers.remove(address);
    }
    
    /**
     * Gets the start address of the segment containing the specified address
     * 
     * @param address
     * @return
     * @throws IndexOutOfBoundsException when trying to access out-of-bounds memory
     * @throws NullPointerException if no segments have been registered
     */
    private long getSegment(long address) {
        Long start = this.startAddresses.floor(address);
        
        if(start == null || this.endAddresses.get(start) < address) {
            throw new IndexOutOfBoundsException(String.format("Attempted to access non-registered address: %04X", address));
        }
        
        return start; 
    }

    @Override
    public byte readByte(long address) {
        //System.out.printf("reading 1 byte: %08X\n", address);
        
        long seg = getSegment(address);
        MemoryController sc = this.segmentControllers.get(seg);
        
        return sc.readByte(address - seg);
    }
    
    @Override
    public short read2Bytes(long address) {
        //System.out.printf("reading 2 bytes: %08X\n", address);
        
        long seg = getSegment(address);
        MemoryController sc = this.segmentControllers.get(seg);
        
        return sc.read2Bytes(address - seg);
    }
    
    @Override
    public int read3Bytes(long address) {
        //System.out.printf("reading 3 bytes: %08X\n", address);
        
        long seg = getSegment(address);
        MemoryController sc = this.segmentControllers.get(seg);
        
        return sc.read3Bytes(address - seg);
    }
    
    @Override
    public int read4Bytes(long address) {
        //System.out.printf("reading 4 bytes: %08X\n", address);
        
        long seg = getSegment(address);
        MemoryController sc = this.segmentControllers.get(seg);
        
        return sc.read4Bytes(address - seg);
    }

    @Override
    public void writeByte(long address, byte value) {
        //System.out.printf("writing 1 byte: %08X\n", address);
        
        long seg = getSegment(address);
        MemoryController sc = this.segmentControllers.get(seg);
        
        sc.writeByte(address - seg, value);
    }
    
    @Override
    public void write2Bytes(long address, short value) {
        //System.out.printf("writing 2 bytes: %08X\n", address);
        
        long seg = getSegment(address);
        MemoryController sc = this.segmentControllers.get(seg);
        
        sc.write2Bytes(address - seg, value);
    }
    
    @Override
    public void write3Bytes(long address, int value) {
        //System.out.printf("writing 3 bytes: %08X\n", address);
        
        long seg = getSegment(address);
        MemoryController sc = this.segmentControllers.get(seg);
        
        sc.write3Bytes(address - seg, value);
    }
    
    @Override
    public void write4Bytes(long address, int value) {
        //System.out.printf("writing 4 bytes: %08X\n", address);
        
        long seg = getSegment(address);
        MemoryController sc = this.segmentControllers.get(seg);
        
        sc.write4Bytes(address - seg, value);
    }
    
}
