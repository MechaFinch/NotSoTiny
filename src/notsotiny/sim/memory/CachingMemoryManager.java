package notsotiny.sim.memory;

import java.util.HashMap;
import java.util.Map.Entry;

public class CachingMemoryManager extends MemoryManager {
    
    private static final boolean DEBUG = false;
    
    private static final int CACHE_SIZE = 16384,
                             BLOCK_SIZE = 32,
                             BLOCK_SHIFT = 5,   // log2(BLOCK_SIZE)
                             BLOCK_COUNT = CACHE_SIZE >> BLOCK_SHIFT,
                             BLOCK_MASK = BLOCK_COUNT - 1,
                             INDEX_MASK = BLOCK_SIZE - 1,
                             LINE_MASK = ~INDEX_MASK,
                             TAG_SHIFT = 14;    // log2(CACHE_SIZE)\
    
    private int[] tags;
    private boolean[] dirty,
                      readPrivilege,
                      writePrivilege;
    private byte[][] cache;
    
    // Map of what segments are cachable
    private HashMap<Long, Boolean> cachabilityMap;
    
    /**
     * Create an empty CachingMemoryManager
     */
    public CachingMemoryManager() {
        super();
        
        this.cachabilityMap = new HashMap<>();
        
        this.cache = new byte[BLOCK_COUNT][BLOCK_SIZE];
        this.tags = new int[BLOCK_COUNT];
        this.dirty = new boolean[BLOCK_COUNT];
        this.readPrivilege = new boolean[BLOCK_COUNT];
        this.writePrivilege = new boolean[BLOCK_COUNT];
        
        for(int i = 0; i < BLOCK_COUNT; i++) {
            this.tags[i] = -1;
            this.dirty[i] = false;
            this.readPrivilege[i] = true;
            this.writePrivilege[i] = true;
        }
    }
    
    /**
     * Adds a new segment to the memory map
     * 
     * @param mc Segment controller
     * @param start Start address
     * @param end End address
     * @param cachable True if cachable
     * @throws IllegalArgumentException if the specified range overlaps with an existing segment
     */
    public void registerSegment(MemoryController mc, long start, long size, boolean cachable) {
        super.registerSegment(mc, start, size);
        start &= 0xFFFF_FFFFl;
        //System.out.printf("%s %08X %s\n", mc, start, cachable);
        this.cachabilityMap.put(start, cachable);
    }
    
    @Override
    public void registerSegment(MemoryController mc, long start, long size) {
        registerSegment(mc, start, size, false);
    }
    
    @Override
    public void removeSegment(long address) {
        super.removeSegment(address);
        
        address &= 0xFFFF_FFFFl;
        this.cachabilityMap.remove(address);
    }
    
    /**
     * Reads a cache line from memory
     * 
     * @param tag
     * @param block
     * @param address
     * @param seg
     * @throws UnprivilegedAccessException
     */
    private void readCacheLine(int tag, int block, long address, Entry<Long, MemoryController> seg) throws UnprivilegedAccessException {
        if(this.dirty[block]) {
            if(DEBUG) System.out.printf("line was dirty ");
            long dirtyAddress = (this.tags[block] << TAG_SHIFT) | (block << BLOCK_SHIFT);
            Entry<Long, MemoryController> dirtySeg = getSegment(dirtyAddress, dirtyAddress + 1, true, false);
            writeCacheLine(this.tags[block], block, dirtyAddress, dirtySeg);
        } else {
            if(DEBUG) System.out.printf("line was clean ");
        }
        
        final long lineAddress = (address & LINE_MASK) - seg.getKey();
        this.tags[block] = tag;
        this.readPrivilege[block] = seg.getValue().readRequiresPrivilege();
        this.writePrivilege[block] = seg.getValue().writeRequiresPrivilege();
        byte[] line = this.cache[block];
        
        if(DEBUG) System.out.printf("reading block %03X for address %08X with tag %05X from segment offset %08X ", block, address, tag, lineAddress);
        
        for(int offs = 0; offs < BLOCK_SIZE; offs += 4) {
            byte[] data = seg.getValue().read4ByteArray(lineAddress + offs);
            
            line[offs + 0] = data[0];
            line[offs + 1] = data[1];
            line[offs + 2] = data[2];
            line[offs + 3] = data[3];
        }
    }
    
    /**
     * Writes a cache line to memory
     * 
     * @param tag
     * @param block
     * @param address
     * @param seg
     * @throws UnprivilegedAccessException
     */
    private void writeCacheLine(int tag, int block, long address, Entry<Long, MemoryController> seg) throws UnprivilegedAccessException {
        final long lineAddress = (address & LINE_MASK) - seg.getKey();
        this.tags[block] = tag;
        this.dirty[block] = false;
        byte[] line = this.cache[block];
        
        for(int offs = 0; offs < BLOCK_SIZE; offs += 4) {
            int val = (line[offs + 0] & 0xFF) | ((line[offs + 1] & 0xFF) << 8) | ((line[offs + 2] & 0xFF) << 16) | ((line[offs + 3] & 0xFF) << 24);
            seg.getValue().write4Bytes(lineAddress + offs, val);
        }
    }
    
    /*
    @Override
    public byte readByte(long address, boolean privilege) throws UnprivilegedAccessException {
        if(DEBUG) System.out.printf("reading byte: %08X ", address);
        byte b = readByteInternal(address, privilege);
        if(DEBUG) System.out.printf("got %02X\n", b);
        return b;
    }*/
    
    @Override
    public byte readByte(long address, boolean privilege) throws UnprivilegedAccessException {
        address &= 0xFFFF_FFFFl;
        
        int tag = (int)address >> TAG_SHIFT;
        int block = ((int)address >> BLOCK_SHIFT) & BLOCK_MASK;
        int index = (int)address & INDEX_MASK;
        
        if(this.tags[block] == tag) {
            // value in cache
            if(DEBUG) System.out.printf("from cache block %02X index %02X ", block, (int)address & INDEX_MASK);
            if(this.readPrivilege[block]) throw new UnprivilegedAccessException((int)address);
            return this.cache[block][index]; 
        } else {
            // not in cache, retrieve
            if(DEBUG) System.out.printf("from memory ");
            Entry<Long, MemoryController> seg = getSegment(address, address, privilege, true);
            
            // if cachable, cache
            if(this.cachabilityMap.get(seg.getKey())) {
                readCacheLine(tag, block, address, seg);
                return this.cache[block][index];
            } else {
                return seg.getValue().readByte(address - seg.getKey());
            }
        }
    }
    
    /*
    @Override
    public short read2Bytes(long address, boolean privilege) throws UnprivilegedAccessException {
        address &= 0xFFFF_FFFFl;
        
        int tag = (int)address >> TAG_SHIFT;
        int block = ((int)address >> BLOCK_SHIFT) & BLOCK_MASK;
        int index = (int)address & INDEX_MASK;
        
        if(this.tags[block] == tag) {
            // value in cache
            if(DEBUG) System.out.printf("from cache block %02X index %02X ", block, (int)address & INDEX_MASK);
            if(this.readPrivilege[block]) throw new UnprivilegedAccessException();
            
            // when crossing cache lines, break things up
            if(index >= BLOCK_SIZE - 1) {
                return (short)((this.readByte(address, privilege) & 0xFF) | (this.readByte(address + 1, privilege) << 8));
            } else {
                return (short)((this.cache[block][index + 0] & 0xFF) | ((this.cache[block][index + 1] & 0xFF) << 8));
            }
        } else {
            // not in cache, retrieve
            if(DEBUG) System.out.printf("from memory ");
            Entry<Long, MemoryController> seg = getSegment(address, address + 1, privilege, true);
            
            // if cachable, cache
            if(this.cachabilityMap.get(seg.getKey())) {
                if(index >= BLOCK_SIZE - 1) {
                    // 2 lins
                    readCacheLine(tag, block, address, seg);
                    readCacheLine(tag, block + 1, address + 32, seg);
                } else {
                    // 1 line
                    readCacheLine(tag, block, address, seg);
                    return (short)((this.cache[block][index + 0] & 0xFF) | ((this.cache[block][index + 1] & 0xFF) << 8));
                }
            } else {
                return seg.getValue().read2Bytes(address - seg.getKey());
            }
        }
        
        return (short)((this.readByte(address, privilege) & 0xFF) | (this.readByte(address + 1, privilege) << 8));
    }
    */
    
    @Override
    public short read2Bytes(long address, boolean privilege) throws UnprivilegedAccessException {
        return (short)((this.readByte(address, privilege) & 0xFF) | ((this.readByte(address + 1, privilege) & 0xFF) << 8));
    }
    
    @Override
    public int read3Bytes(long address, boolean privilege) throws UnprivilegedAccessException {
        return (this.readByte(address, privilege) & 0xFF) | ((this.readByte(address + 1, privilege) & 0xFF) << 8) | ((this.readByte(address + 2, privilege) & 0xFF) << 16);
    }
    
    @Override
    public int read4Bytes(long address, boolean privilege) throws UnprivilegedAccessException {
        return (this.readByte(address, privilege) & 0xFF) | ((this.readByte(address + 1, privilege) & 0xFF) << 8) | ((this.readByte(address + 2, privilege) & 0xFF) << 16) | ((this.readByte(address + 3, privilege) & 0xFF) << 24);
    }
    
    @Override
    public void writeByte(long address, byte value, boolean privilege) throws UnprivilegedAccessException {
        address &= 0xFFFF_FFFFl;
        
        if(DEBUG) System.out.printf("writing byte: %02X to %08X\n", value, address);
        
        int tag = (int)address >> TAG_SHIFT;
        int block = ((int)address >> BLOCK_SHIFT) & BLOCK_MASK;
        int index = (int)address & INDEX_MASK;
        
        if(this.tags[block] == tag) {
            // value in cache
            if(this.writePrivilege[block]) throw new UnprivilegedAccessException((int) address);
            this.cache[block][index] = value;
            this.dirty[block] = true;
        } else {
            // not in cache
            Entry<Long, MemoryController> seg = getSegment(address, address, privilege, true);
            
            // if cachable, cache
            if(this.cachabilityMap.get(seg.getKey())) {
                readCacheLine(tag, block, address, seg);
                
                this.cache[block][index] = value;
                this.dirty[block] = true;
            } else {
                seg.getValue().writeByte(address - seg.getKey(), value);
            }
        }
    }
    
    @Override
    public void write2Bytes(long address, short value, boolean privilege) throws UnprivilegedAccessException {
        this.writeByte(address + 0, (byte) value, privilege);
        this.writeByte(address + 1, (byte) (value >> 8), privilege);
    }
    
    @Override
    public void write3Bytes(long address, int value, boolean privilege) throws UnprivilegedAccessException {
        this.writeByte(address + 0, (byte) value, privilege);
        this.writeByte(address + 1, (byte) (value >> 8), privilege);
        this.writeByte(address + 2, (byte) (value >> 16), privilege);
    }
    
    @Override
    public void write4Bytes(long address, int value, boolean privilege) throws UnprivilegedAccessException {
        this.writeByte(address + 0, (byte) value, privilege);
        this.writeByte(address + 1, (byte) (value >> 8), privilege);
        this.writeByte(address + 2, (byte) (value >> 16), privilege);
        this.writeByte(address + 3, (byte) (value >> 24), privilege);
    }
    
    // Instruction fetch uses these, so they get special attention
    @Override
    public byte[] read2ByteArray(long address, boolean privilege) throws UnprivilegedAccessException {
        address &= 0xFFFF_FFFFl;
        
        int tag = (int)address >> TAG_SHIFT;
        int block = ((int)address >> BLOCK_SHIFT) & BLOCK_MASK;
        int index = (int)address & INDEX_MASK;
        
        if(this.tags[block] == tag && index < BLOCK_SIZE - 1) {
            // value in cache & doesn't cross lines
            if(DEBUG) System.out.printf("from cache block %02X index %02X ", block, (int)address & INDEX_MASK);
            if(this.readPrivilege[block]) throw new UnprivilegedAccessException((int)address);
            
            return new byte[] {
                this.cache[block][index + 0],
                this.cache[block][index + 1]
            };
        } else {
            // defer
            return new byte[] {
                this.readByte(address + 0, privilege),
                this.readByte(address + 1, privilege)
            };
        }
    }
    
    @Override
    public byte[] read3ByteArray(long address, boolean privilege) throws UnprivilegedAccessException {
        address &= 0xFFFF_FFFFl;
        
        int tag = (int)address >> TAG_SHIFT;
        int block = ((int)address >> BLOCK_SHIFT) & BLOCK_MASK;
        int index = (int)address & INDEX_MASK;
        
        if(this.tags[block] == tag && index < BLOCK_SIZE - 2) {
            // value in cache & doesn't cross lines
            if(DEBUG) System.out.printf("from cache block %02X index %02X ", block, (int)address & INDEX_MASK);
            if(this.readPrivilege[block]) throw new UnprivilegedAccessException((int)address);
            
            return new byte[] {
                this.cache[block][index + 0],
                this.cache[block][index + 1],
                this.cache[block][index + 2]
            };
        } else {
            // defer
            return new byte[] {
                this.readByte(address + 0, privilege),
                this.readByte(address + 1, privilege),
                this.readByte(address + 2, privilege)
            };
        }
    }
    
    @Override
    public byte[] read4ByteArray(long address, boolean privilege) throws UnprivilegedAccessException {
        address &= 0xFFFF_FFFFl;
        
        int tag = (int)address >> TAG_SHIFT;
        int block = ((int)address >> BLOCK_SHIFT) & BLOCK_MASK;
        int index = (int)address & INDEX_MASK;
        
        if(this.tags[block] == tag && index < BLOCK_SIZE - 3) {
            // value in cache & doesn't cross lines
            if(DEBUG) System.out.printf("from cache block %02X index %02X ", block, (int)address & INDEX_MASK);
            if(this.readPrivilege[block]) throw new UnprivilegedAccessException((int)address);
            
            return new byte[] {
                this.cache[block][index + 0],
                this.cache[block][index + 1],
                this.cache[block][index + 2],
                this.cache[block][index + 3]
            };
        } else {
            // defer
            return new byte[] {
                this.readByte(address + 0, privilege),
                this.readByte(address + 1, privilege),
                this.readByte(address + 2, privilege),
                this.readByte(address + 3, privilege)
            };
        }
    }
}
