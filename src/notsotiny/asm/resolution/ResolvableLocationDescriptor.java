package notsotiny.asm.resolution;

/**
 * A Resolvable that describes a location
 * 
 * @author Mechafinch
 */
public class ResolvableLocationDescriptor implements Resolvable {
    
    public static final ResolvableLocationDescriptor NONE = new ResolvableLocationDescriptor(LocationType.NULL);
    
    private LocationType type;
    
    int size;
    
    ResolvableMemory memory;
    
    ResolvableValue immediate;
    
    Resolvable parent;
    
    public enum LocationType {
        REG_A, REG_AH, REG_AL,
        REG_B, REG_BH, REG_BL,
        REG_C, REG_CH, REG_CL,
        REG_D, REG_DH, REG_DL,
        REG_I,
        REG_J,
        REG_F,
        REG_BP,
        REG_SP,
        IMMEDIATE,
        MEMORY,
        NULL
    }
    
    /**
     * Create a descriptor for memory
     * 
     * @param type
     * @param size -1 to infer, 1, 2, 4 if specified
     * @param memory
     */
    public ResolvableLocationDescriptor(LocationType type, int size, ResolvableMemory memory) {
        this.type = type;
        this.size = size;
        this.memory = memory;
        
        this.parent = null;
        this.immediate = null;
        
        this.memory.setParent(this);
        
        if(this.type != LocationType.MEMORY) {
            throw new IllegalArgumentException("Memory constructor valid for memory only");
        }
    }
    
    /**
     * Create a descriptor for an immediate
     * 
     * @param type
     * @param size -1 to infer, 1, 2, 4 if specified
     * @param immediate
     */
    public ResolvableLocationDescriptor(LocationType type, int size, ResolvableValue immediate) {
        this.type = type;
        this.size = size;
        this.immediate = immediate;
        
        this.parent = null;
        this.memory = null;
        
        this.immediate.setParent(this);
        
        if(this.type != LocationType.IMMEDIATE) {
            throw new IllegalArgumentException("Immediate constructor valid for immediates only");
        }
    }
    
    /**
     * Create a descriptor for a register
     * 
     * @param type
     */
    public ResolvableLocationDescriptor(LocationType type) {
        this.type = type;
        
        this.size = 0;
        this.parent = null;
        this.immediate = null;
        this.memory = null;
        
        if(this.type == LocationType.IMMEDIATE || this.type == LocationType.MEMORY) {
            throw new IllegalArgumentException("Single-argument constructor not valid for immediates or memory");
        }
    }

    @Override
    public boolean isResolved() {
        return switch(this.type) { 
            case MEMORY     -> this.memory.isResolved();
            case IMMEDIATE  -> this.immediate.isResolved();
            default         -> true;
        }; 
    }

    @Override
    public void resolve() {
        // pass it on
        this.parent.resolve();
    }
    
    @Override
    public void setParent(Resolvable r) {
        this.parent = r;
    }
    
    @Override
    public String toString() {
        String s = switch(this.size) {
            case 1  -> "byte ";
            case 2  -> "word ";
            case 4  -> "ptr ";
            default -> "";
        };
        
        if(this.type == LocationType.NULL) {
            return "";
        } else if(this.type == LocationType.MEMORY) {
            return s + this.memory.toString();
        } else if(this.type == LocationType.IMMEDIATE) {
            return s + this.immediate.toString();
        } else {
            return this.type.toString().substring(4); // remove "REG_"
        }
    }
    
    public int getSize() { return this.size; }
    public void setSize(int s) { this.size = s; }
    public LocationType getType() { return this.type; }
    public ResolvableMemory getMemory() { return this.memory; }
    public ResolvableValue getImmediate() { return this.immediate; }
}
