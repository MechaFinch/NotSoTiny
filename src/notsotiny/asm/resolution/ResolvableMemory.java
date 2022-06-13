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
    
    private Resolvable parent;
    
    private ResolvableValue rOffset;
    
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
        
        this.parent = null;
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
    public ResolvableMemory(Register base, Register index, int scale, ResolvableValue offset) {
        this.base = base;
        this.index = index;
        this.scale = scale;
        this.rOffset = offset;
        
        this.parent = null;
        this.offset = -1;
        this.resolved = false;
        
        this.rOffset.setParent(this);
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
            this.parent.resolve();
        }
    }
    
    @Override
    public void setParent(Resolvable r) {
        this.parent = r;
    }
    
    public Register getBase() { return this.base; }
    public Register getIndex() { return this.index; }
    public int getScale() { return this.scale; }
    public int getOffset() { return this.offset; }
    public Resolvable getUnresolvedOffset() { return this.rOffset; }
    
}
