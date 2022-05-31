package notsotiny.asm.resolution;

import notsotiny.asm.Register;

/**
 * A memory address
 * 
 * @author Alex
 *
 */
public class ResolvableMemory implements Resolvable {
    
    private Register base,
                     index;
    
    private Resolvable rOffset;
    
    private int offset,
                scale;
    
    private boolean resolved;
    
    /**
     * Resolved constructor
     * 
     * @param base
     * @param index
     * @param scale
     * @param offset
     */
    public ResolvableMemory(Register base, Register index, int scale, int offset) {
        this.base = base;
        this.index = index;
        this.scale = scale;
        this.offset = offset;
        
        this.rOffset = null;
        this.resolved = true;
    }
    
    /**
     * Unresolved constructor
     * 
     * @param base
     * @param index
     * @param scale
     * @param offset
     */
    public ResolvableMemory(Register base, Register index, int scale, Resolvable offset) {
        this.base = base;
        this.index = index;
        this.scale = scale;
        this.rOffset = offset;
        
        this.offset = -1;
        this.resolved = false;
    }

    @Override
    public boolean isResolved() {
        return this.resolved;
    }

    @Override
    public void resolve() {
        this.resolved = this.rOffset.isResolved();
        
        if(this.resolved) { 
            this.offset = this.rOffset.value();
        }
    }

    @Override
    public int value() {
        return -1;
    }
    
    public Register getBase() { return this.base; }
    public Register getIndex() { return this.index; }
    public int getScale() { return this.scale; }
    public int getOffset() { return this.offset; }
    public Resolvable getUnresolvedOffset() { return this.rOffset; }
    
}
