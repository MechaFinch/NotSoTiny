package notsotiny.sim.memory;

import java.util.HashMap;
import java.util.TreeSet;

/**
 * A MemoryController container which can register other MemoryControllers with their own address spaces 
 * 
 * @author Mechafinch
 */
public class MemoryManager implements MemoryController {
    
    private HashMap<Integer, MemoryController> segmentControllers; // maps starting address to controller
    
    private TreeSet<Integer> startAddresses; // good old tree for finding the right memory segment fast
    
    private HashMap<Integer, Integer> endAddresses; // maps starting address to ending address
    
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
    public void registerSegment(MemoryController mc, int start, int end) {
        // check for overlap
        if(this.segmentControllers.size() != 0) {
            int closestSegmentStart = this.startAddresses.floor(end),
                closestSegmentEnd = this.endAddresses.get(closestSegmentStart);
            
            if(closestSegmentStart > start || closestSegmentEnd > start) {
                throw new IllegalArgumentException("Memory segments cannot overlap");
            }
        }
        
        // add
        this.startAddresses.add(start);
        this.endAddresses.put(start, end);
        this.segmentControllers.put(start, mc);
    }
    
    /**
     * Remove a segment from the memory map
     * 
     * @param address Start address of the segment to remove
     */
    public void removeSegment(int address) {
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
    private int getSegment(int address) {
        int start = this.startAddresses.floor(address);
        
        if(this.endAddresses.get(start) < address) {
            throw new IndexOutOfBoundsException(String.format("Attempted to access non-registered address: %04X", address));
        }
        
        return start; 
    }

    @Override
    public byte readByte(int address) {
        int seg = getSegment(address);
        MemoryController sc = this.segmentControllers.get(seg);
        
        return sc.readByte(address - seg);
    }
    
    @Override
    public short read2Bytes(int address) {
        int seg = getSegment(address);
        MemoryController sc = this.segmentControllers.get(seg);
        
        return sc.read2Bytes(address - seg);
    }
    
    @Override
    public int read3Bytes(int address) {
        int seg = getSegment(address);
        MemoryController sc = this.segmentControllers.get(seg);
        
        return sc.read3Bytes(address - seg);
    }
    
    @Override
    public int read4Bytes(int address) {
        int seg = getSegment(address);
        MemoryController sc = this.segmentControllers.get(seg);
        
        return sc.read4Bytes(address - seg);
    }

    @Override
    public void writeByte(int address, byte value) {
        int seg = getSegment(address);
        MemoryController sc = this.segmentControllers.get(seg);
        
        sc.writeByte(address - seg, value);
    }
    
    @Override
    public void write2Bytes(int address, short value) {
        int seg = getSegment(address);
        MemoryController sc = this.segmentControllers.get(seg);
        
        sc.write2Bytes(address - seg, value);
    }
    
    @Override
    public void write3Bytes(int address, int value) {
        int seg = getSegment(address);
        MemoryController sc = this.segmentControllers.get(seg);
        
        sc.write3Bytes(address - seg, value);
    }
    
    @Override
    public void write4Bytes(int address, int value) {
        int seg = getSegment(address);
        MemoryController sc = this.segmentControllers.get(seg);
        
        sc.write4Bytes(address - seg, value);
    }
    
}
