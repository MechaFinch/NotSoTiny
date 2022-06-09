package notsotiny.asm.resolution;

/**
 * A Resolvable that describes a location
 * 
 * @author Mechafinch
 */
public class ResolvableLocationDescriptor implements Resolvable {
    
    public static final ResolvableLocationDescriptor NONE = new ResolvableLocationDescriptor(LocationType.REG_A, 0, null);
    
    private LocationType type;
    
    int size;
    
    ResolvableMemory memory;
    
    ResolvableValue immediate;
    
    Resolvable parent;
    
    public enum LocationType {
        REG_A,
        REG_B,
        REG_C,
        REG_D,
        REG_I,
        REG_J,
        REG_F,
        REG_BP,
        REG_SP,
        IMMEDIATE,
        MEMORY
    }
    
    /**
     * Create a descriptor that includes memory
     * 
     * @param type
     * @param size
     * @param memory
     * @param parent
     */
    public ResolvableLocationDescriptor(LocationType type, int size, ResolvableMemory memory, Resolvable parent) {
        this.type = type;
        this.size = size;
        this.memory = memory;
        this.parent = parent;
        
        this.immediate = null;
    }
    
    public ResolvableLocationDescriptor(LocationType type, int size, ResolvableValue immediate, Resolvable parent) {
        this.type = type;
        this.size = size;
        this.immediate = immediate;
        this.parent = parent;
        
        this.memory = null;
    }
    
    /**
     * Create a descriptor that doesn't include memory
     * 
     * @param type
     * @param size
     * @param parent
     */
    public ResolvableLocationDescriptor(LocationType type, int size, Resolvable parent) {
        this.type = type;
        this.size = size;
        this.parent = parent;
        
        this.immediate = null;
        this.memory = null;
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
    public String toString() {
        if(this.size == 0) {
            return "";
        } else if(this.type == LocationType.MEMORY) {
            return this.memory.toString();
        } else {
            return this.type.toString();
        }
    }
    
    public LocationType getType() { return this.type; }
    public ResolvableMemory getMemory() { return this.memory; }
    public ResolvableValue getImmediate() { return this.immediate; }
}
