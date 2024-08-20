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
public class MemoryManager {
    
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
            System.out.printf("%08X: %s %s %s%n", e.getKey() & 0xFFFF_FFFFl, e.getValue().getClass().toString(), e.getValue().readRequiresPrivilege(), e.getValue().writeRequiresPrivilege());
        }
    }
    
    /**
     * Gets the start address of the segment containing the specified address
     * 
     * @param startAddress
     * @param endAddress
     * @param privilege
     * @param read
     * @return
     * @throws UnprivilegedAccessException 
     * @throws IndexOutOfBoundsException when trying to access out-of-bounds memory
     * @throws NullPointerException if no segments have been registered
     */
    protected Entry<Long, MemoryController> getSegment(long startAddress, long endAddress, boolean privilege, boolean read) throws UnprivilegedAccessException {
        startAddress &= 0xFFFF_FFFFl;
        endAddress &= 0xFFFF_FFFFl;
        
        Entry<Long, MemoryController> segment = this.segmentControllerMap.floorEntry(startAddress);
        Long start = segment.getKey(),
             end = this.endAddressMap.get(start);
        
        if(start == null || end < startAddress) {
            // floored segment ends before start
            throw new IndexOutOfBoundsException(String.format("Attempted to access non-registered address: %08X from %08X", startAddress, startAddress));
        } else if(end == null || end < endAddress) {
            // segment ends before access does
            throw new IndexOutOfBoundsException(String.format("Attempted to access non-registered address: %08X from %08X", endAddress, startAddress));
        } else if(endAddress < startAddress) {
            throw new IndexOutOfBoundsException(String.format("Multi-byte access overflowed: %08X", startAddress));
        }
        
        if(!privilege && (
           (read && segment.getValue().readRequiresPrivilege()) ||
           (!read && segment.getValue().writeRequiresPrivilege())
        )) {
            throw new UnprivilegedAccessException((int)startAddress);
        }
        
        return segment;
    }

    /**
     * Get 1 byte from an address
     * 
     * @param address
     * @param privilege
     * @return
     * @throws UnprivilegedAccessException 
     */
    public byte readByte(long address, boolean privilege) throws UnprivilegedAccessException {
        address &= 0xFFFF_FFFFl;
        if(DEBUG) System.out.printf("reading 1 byte: %08X\n", address);
        
        Entry<Long, MemoryController> seg = getSegment(address, address, privilege, true);
        
        return seg.getValue().readByte(address - seg.getKey());
    }
    
    /**
     * Get 1 byte from an address, with privilege
     * 
     * @param address
     * @return
     */
    public byte readBytePrivileged(long address) {
        try {
            return this.readByte(address, true);
        } catch(UnprivilegedAccessException e) {
            // Not possible
            return 0;
        }
    }
    
    /**
     * Get 2 little-endian bytes from an address
     * 
     * @param address
     * @param privilege
     * @return
     * @throws UnprivilegedAccessException
     */
    public short read2Bytes(long address, boolean privilege) throws UnprivilegedAccessException {
        address &= 0xFFFF_FFFFl;
        if(DEBUG) System.out.printf("reading 2 bytes: %08X\n", address);
        
        Entry<Long, MemoryController> seg = getSegment(address, address + 1, privilege, true);
        
        return seg.getValue().read2Bytes(address - seg.getKey());
    }
    
    /**
     * Get 2 little-endian bytes from an address, with privilege
     * 
     * @param address
     * @return
     */
    public short read2BytesPrivileged(long address) {
        try {
            return this.read2Bytes(address, true);
        } catch(UnprivilegedAccessException e) {
            // Not possible
            return 0;
        }
    }
    
    /**
     * Get 3 little-endian bytes from an address
     * 
     * @param address
     * @param privilege
     * @return
     * @throws UnprivilegedAccessException 
     */
    public int read3Bytes(long address, boolean privilege) throws UnprivilegedAccessException {
        address &= 0xFFFF_FFFFl;
        if(DEBUG) System.out.printf("reading 3 bytes: %08X\n", address);
        
        Entry<Long, MemoryController> seg = getSegment(address, address + 2, privilege, true);
        
        return seg.getValue().read3Bytes(address - seg.getKey());
    }
    
    /**
     * Get 3 little-endian bytes from an address, with privilege
     * 
     * @param address
     * @return
     */
    public int read3BytesPrivileged(long address) {
        try {
            return this.read3Bytes(address, true);
        } catch(UnprivilegedAccessException e) {
            // Not possible
            return 0;
        }
    }
    
    /**
     * Get 4 little-endian bytes from an address
     * 
     * @param address
     * @param privilege
     * @return
     * @throws UnprivilegedAccessException 
     */
    public int read4Bytes(long address, boolean privilege) throws UnprivilegedAccessException {
        address &= 0xFFFF_FFFFl;
        if(DEBUG) System.out.printf("reading 4 bytes: %08X\n", address);
        
        Entry<Long, MemoryController> seg = getSegment(address, address + 3, privilege, true);
        
        return seg.getValue().read4Bytes(address - seg.getKey());
    }
    
    /**
     * Get 4 little-endian bytes from an address, with privilege
     * 
     * @param address
     * @return
     */
    public int read4BytesPrivileged(long address) {
        try {
            return this.read4Bytes(address, true);
        } catch(UnprivilegedAccessException e) {
            // Not possible
            return 0;
        }
    }
    
    /**
     * Gets 2 bytes as an array
     * 
     * @param address
     * @param privilege
     * @return
     * @throws UnprivilegedAccessException 
     */
    public byte[] read2ByteArray(long address, boolean privilege) throws UnprivilegedAccessException {
        address &= 0xFFFF_FFFFl;
        if(DEBUG) System.out.printf("reading 4 bytes (array): %08X\n", address);
        
        Entry<Long, MemoryController> seg = getSegment(address, address + 1, privilege, true);
        
        return seg.getValue().read2ByteArray(address - seg.getKey());
    }
    
    /**
     * Gets 2 bytes as an array, with privilege
     * 
     * @param address
     * @return
     */
    public byte[] read2ByteArrayPrivileged(long address) {
        try {
            return this.read2ByteArray(address, true);
        } catch(UnprivilegedAccessException e) {
            // Not possible
            return null;
        }
    }
    
    /**
     * Gets 3 bytes as an array 
     * 
     * @param address
     * @param privilege
     * @return
     * @throws UnprivilegedAccessException 
     */
    public byte[] read3ByteArray(long address, boolean privilege) throws UnprivilegedAccessException {
        address &= 0xFFFF_FFFFl;
        if(DEBUG) System.out.printf("reading 4 bytes (array): %08X\n", address);
        
        Entry<Long, MemoryController> seg = getSegment(address, address + 2, privilege, true);
        
        return seg.getValue().read3ByteArray(address - seg.getKey());
    }
    
    /**
     * Gets 3 bytes as an array, with privilege
     * 
     * @param address
     * @return
     */
    public byte[] read3ByteArrayPrivileged(long address) {
        try {
            return this.read3ByteArray(address, true);
        } catch(UnprivilegedAccessException e) {
            // Not possible
            return null;
        }
    }
    
    /**
     * Gets 4 bytes as an array
     * 
     * @param address
     * @param privilege
     * @return
     * @throws UnprivilegedAccessException 
     */
    public byte[] read4ByteArray(long address, boolean privilege) throws UnprivilegedAccessException {
        address &= 0xFFFF_FFFFl;
        if(DEBUG) System.out.printf("reading 4 bytes (array): %08X\n", address);
        
        Entry<Long, MemoryController> seg = getSegment(address, address + 3, privilege, true);
        
        return seg.getValue().read4ByteArray(address - seg.getKey());
    }
    
    /**
     * Gets 4 bytes as an array, with privilege
     * 
     * @param address
     * @return
     */
    public byte[] read4ByteArrayPrivileged(long address) {
        try {
            return this.read4ByteArray(address, true);
        } catch(UnprivilegedAccessException e) {
            // Not possible
            return null;
        }
    }

    /**
     * Set 1 byte at an address
     * 
     * @param address
     * @param privilege
     * @param value
     * @throws UnprivilegedAccessException 
     */
    public void writeByte(long address, byte value, boolean privilege) throws UnprivilegedAccessException {
        address &= 0xFFFF_FFFFl;
        if(DEBUG) System.out.printf("writing 1 byte: %08X\n", address);
        
        Entry<Long, MemoryController> seg = getSegment(address, address, privilege, false);
        
        seg.getValue().writeByte(address - seg.getKey(), value);
    }
    
    /**
     * Set 1 byte at an address, with privilege
     * @param address
     * @param value
     */
    public void writeBytePrivileged(long address, byte value) {
        try {
            this.writeByte(address, value, true);
        } catch(UnprivilegedAccessException e) {
            // not possible
        }
    }
    
    /**
     * Set 2 little-endian bytes at an address
     * 
     * @param address
     * @param privilege
     * @param value
     * @throws UnprivilegedAccessException 
     */
    public void write2Bytes(long address, short value, boolean privilege) throws UnprivilegedAccessException {
        address &= 0xFFFF_FFFFl;
        if(DEBUG) System.out.printf("writing 2 bytes: %08X\n", address);
        
        Entry<Long, MemoryController> seg = getSegment(address, address + 1, privilege, false);
        
        seg.getValue().write2Bytes(address - seg.getKey(), value);
    }
    
    /**
     * Set 2 little-endian bytes at an address, with privilege
     * @param address
     * @param value
     */
    public void write2BytesPrivileged(long address, short value) {
        try {
            this.write2Bytes(address, value, true);
        } catch(UnprivilegedAccessException e) {
            // not possible
        }
    }
    
    /**
     * Set 3 little-endian bytes at an address
     * 
     * @param address
     * @param privilege
     * @param value
     * @throws UnprivilegedAccessException 
     */
    public void write3Bytes(long address, int value, boolean privilege) throws UnprivilegedAccessException {
        address &= 0xFFFF_FFFFl;
        if(DEBUG) System.out.printf("writing 3 bytes: %08X\n", address);
        
        Entry<Long, MemoryController> seg = getSegment(address, address + 2, privilege, false);
        
        seg.getValue().write3Bytes(address - seg.getKey(), value);
    }
    
    /**
     * Set 3 little-endian bytes at an address, with privilege
     * @param address
     * @param value
     */
    public void write3BytesPrivileged(long address, int value) {
        try {
            this.write3Bytes(address, value, true);
        } catch(UnprivilegedAccessException e) {
            // not possible
        }
    }
    
    /**
     * Set 4 little-endian bytes at an address
     * 
     * @param address
     * @param privilege
     * @param value
     * @throws UnprivilegedAccessException 
     */
    public void write4Bytes(long address, int value, boolean privilege) throws UnprivilegedAccessException {
        address &= 0xFFFF_FFFFl;
        if(DEBUG) System.out.printf("writing 4 bytes: %08X\n", address);
        
        Entry<Long, MemoryController> seg = getSegment(address, address + 3, privilege, false);
        
        seg.getValue().write4Bytes(address - seg.getKey(), value);
    }
    
    /**
     * Set 4 little-endian bytes at an address, with privilege
     * @param address
     * @param value
     */
    public void write4BytesPrivileged(long address, int value) {
        try {
            this.write4Bytes(address, value, true);
        } catch(UnprivilegedAccessException e) {
            // not possible
        }
    }
    
}
