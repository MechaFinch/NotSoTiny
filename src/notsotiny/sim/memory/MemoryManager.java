package notsotiny.sim.memory;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A MemoryController container which can register other MemoryControllers with their own address spaces 
 * 
 * @author Mechafinch
 */
public class MemoryManager implements MemoryController {
    
    // maps starting address to controller, treemap for searching
    private TreeMap<Long, MemoryController> segmentControllerMap;
    
    // maps starting address to ending address
    // long hash should be basically free
    private HashMap<Long, Long> endAddressMap;
    
    private static final boolean DEBUG = false;
    
    /**
     * Create an empty MemoryManager
     */
    public MemoryManager() {
        this.segmentControllerMap = new TreeMap<>();
        this.endAddressMap = new HashMap<>();
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
        start &= 0xFFFF_FFFFl; // stop sign extending plz
        size &= 0xFFFF_FFFFl;
        
        // check for overlap
        if(this.segmentControllerMap.size() != 0) {
            Long closestSegmentStart = this.segmentControllerMap.floorKey(start + size - 1);
            
            if(closestSegmentStart != null) {
                long closestSegmentEnd = this.endAddressMap.get(closestSegmentStart);
                
                if(closestSegmentStart > start || closestSegmentEnd > start) {
                    throw new IllegalArgumentException("Memory segments cannot overlap");
                }
            }
        }
        
        // add
        this.segmentControllerMap.put(start, mc);
        this.endAddressMap.put(start, start + size - 1);
    }
    
    /**
     * Remove a segment from the memory map
     * 
     * @param address Start address of the segment to remove
     */
    public void removeSegment(long address) {
        address &= 0xFFFF_FFFFl;
        this.segmentControllerMap.remove(address);
        this.endAddressMap.remove(address);
    }
    
    /**
     * Prints the memory mappings
     */
    public void printMap() {
        for(Entry<Long, MemoryController> e : segmentControllerMap.entrySet()) {
            System.out.printf("%08X: %s%n", e.getKey() & 0xFFFF_FFFFl, e.getValue().getClass().toString());
        }
    }
    
    /**
     * Gets the start address of the segment containing the specified address
     * 
     * @param address
     * @return
     * @throws IndexOutOfBoundsException when trying to access out-of-bounds memory
     * @throws NullPointerException if no segments have been registered
     */
    private Entry<Long, MemoryController> getSegment(long startAddress, long endAddress) {
        startAddress &= 0xFFFF_FFFFl;
        endAddress &= 0xFFFF_FFFFl;
        
        Entry<Long, MemoryController> segment = this.segmentControllerMap.floorEntry(startAddress);
        Long start = segment.getKey();
        
        if(start == null || this.endAddressMap.get(start) < startAddress) {
            throw new IndexOutOfBoundsException(String.format("Attempted to access non-registered address: %08X", startAddress));
        } else if(endAddress < startAddress) {
            throw new IndexOutOfBoundsException(String.format("Multi-byte access overflowed: %08X", startAddress));
        }
        
        return segment;
    }

    @Override
    public byte readByte(long address) {
        address &= 0xFFFF_FFFFl;
        if(DEBUG) System.out.printf("reading 1 byte: %08X\n", address);
        
        Entry<Long, MemoryController> seg = getSegment(address, address);
        
        return seg.getValue().readByte(address - seg.getKey());
    }
    
    @Override
    public short read2Bytes(long address) {
        address &= 0xFFFF_FFFFl;
        if(DEBUG) System.out.printf("reading 2 bytes: %08X\n", address);
        
        Entry<Long, MemoryController> seg = getSegment(address, address + 1);
        
        return seg.getValue().read2Bytes(address - seg.getKey());
    }
    
    @Override
    public int read3Bytes(long address) {
        address &= 0xFFFF_FFFFl;
        if(DEBUG) System.out.printf("reading 3 bytes: %08X\n", address);
        
        Entry<Long, MemoryController> seg = getSegment(address, address + 2);
        
        return seg.getValue().read3Bytes(address - seg.getKey());
    }
    
    @Override
    public int read4Bytes(long address) {
        address &= 0xFFFF_FFFFl;
        if(DEBUG) System.out.printf("reading 4 bytes: %08X\n", address);
        
        Entry<Long, MemoryController> seg = getSegment(address, address + 3);
        
        return seg.getValue().read4Bytes(address - seg.getKey());
    }
    
    @Override
    public byte[] read2ByteArray(long address) {
        address &= 0xFFFF_FFFFl;
        if(DEBUG) System.out.printf("reading 4 bytes (array): %08X\n", address);
        
        Entry<Long, MemoryController> seg = getSegment(address, address + 1);
        
        return seg.getValue().read2ByteArray(address - seg.getKey());
    }
    
    @Override
    public byte[] read3ByteArray(long address) {
        address &= 0xFFFF_FFFFl;
        if(DEBUG) System.out.printf("reading 4 bytes (array): %08X\n", address);
        
        Entry<Long, MemoryController> seg = getSegment(address, address + 2);
        
        return seg.getValue().read3ByteArray(address - seg.getKey());
    }
    
    @Override
    public byte[] read4ByteArray(long address) {
        address &= 0xFFFF_FFFFl;
        if(DEBUG) System.out.printf("reading 4 bytes (array): %08X\n", address);
        
        Entry<Long, MemoryController> seg = getSegment(address, address + 3);
        
        return seg.getValue().read4ByteArray(address - seg.getKey());
    }

    @Override
    public void writeByte(long address, byte value) {
        address &= 0xFFFF_FFFFl;
        if(DEBUG) System.out.printf("writing 1 byte: %08X\n", address);
        
        Entry<Long, MemoryController> seg = getSegment(address, address);
        
        seg.getValue().writeByte(address - seg.getKey(), value);
    }
    
    @Override
    public void write2Bytes(long address, short value) {
        address &= 0xFFFF_FFFFl;
        if(DEBUG) System.out.printf("writing 2 bytes: %08X\n", address);
        
        Entry<Long, MemoryController> seg = getSegment(address, address + 1);
        
        seg.getValue().write2Bytes(address - seg.getKey(), value);
    }
    
    @Override
    public void write3Bytes(long address, int value) {
        address &= 0xFFFF_FFFFl;
        if(DEBUG) System.out.printf("writing 3 bytes: %08X\n", address);
        
        Entry<Long, MemoryController> seg = getSegment(address, address + 2);
        
        seg.getValue().write3Bytes(address - seg.getKey(), value);
    }
    
    @Override
    public void write4Bytes(long address, int value) {
        address &= 0xFFFF_FFFFl;
        if(DEBUG) System.out.printf("writing 4 bytes: %08X\n", address);
        
        Entry<Long, MemoryController> seg = getSegment(address, address + 3);
        
        seg.getValue().write4Bytes(address - seg.getKey(), value);
    }
    
}
