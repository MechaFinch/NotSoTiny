package notsotiny.asm.resolution;

import notsotiny.asm.Register;

/**
 * A Resolvable that describes a location
 * 
 * @author Mechafinch
 */
public class ResolvableLocationDescriptor implements Resolvable {
    
    public static final ResolvableLocationDescriptor NONE = new ResolvableLocationDescriptor(LocationType.NULL);
    
    private LocationType type;
    
    private int size;
    
    private Register register;
    
    private ResolvableMemory memory;
    
    private ResolvableValue immediate;
    
    private Resolvable parent;
    
    public enum LocationType {
        REGISTER,
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
        this.register = Register.NONE;
        
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
        this.register = Register.NONE;
        
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
    public ResolvableLocationDescriptor(LocationType type, Register register) {
        this.type = type;
        this.register = register;
        
        this.parent = null;
        this.immediate = null;
        this.memory = null;
        
        if(this.type == LocationType.IMMEDIATE || this.type == LocationType.MEMORY) {
            throw new IllegalArgumentException("Single-argument constructor not valid for immediates or memory");
        }
        
        // set size according to register
        this.size = switch(this.register) {
            case DA, AB, BC, CD, JI, IJ, BP, SP -> 4;
            case A, B, C, D, I, J, F            -> 2;
            case AH, AL, BH, BL, CH, CL, DH, DL -> 1;
            default -> -1;
        };
    }
    
    /**
     * Create an empty location descriptor
     * 
     * @param type
     */
    public ResolvableLocationDescriptor(LocationType type) {
        this.type = type;
        
        this.parent = null;
        this.immediate = null;
        this.memory = null;
        this.register = Register.NONE;
        this.size = -1;
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
        if(this.parent != null) this.parent.resolve();
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
        
        return switch(this.type) {
            case REGISTER   -> this.register.toString();
            case IMMEDIATE  -> s + this.immediate.toString();
            case MEMORY     -> s + this.memory.toString();
            case NULL       -> "";
        };
    }
    
    public int getSize() { return this.size; }
    public void setSize(int s) { this.size = s; }
    public LocationType getType() { return this.type; }
    public Register getRegister() { return this.register; }
    public ResolvableMemory getMemory() { return this.memory; }
    public ResolvableValue getImmediate() { return this.immediate; }
}
