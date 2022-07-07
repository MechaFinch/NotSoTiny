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
        
        this.rOffset.setParent(this);
        
        if(this.rOffset.isResolved()) {
            this.resolved = true;
            this.offset = this.rOffset.value();
        } else {
            this.resolved = false;
            this.offset = -1;
        }
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
    
    @Override
    public String toString() {
        String sb = "", // base
               si = "", // index
               so = ""; // offset
        
        if(this.base != Register.NONE) {
            sb = this.base.toString();
            
            if(this.index != Register.NONE) {
                sb += " + ";
                si = this.index.toString();
                
                if(this.resolved) {
                    if(this.offset != 0) {
                        si += " + ";
                        so = Integer.toString(this.offset);
                    }
                } else {
                    si += " + ";
                    so = this.rOffset.toString();
                }
            } else {
                if(this.resolved) {
                    if(this.offset != 0) {
                        sb += " + ";
                        so = Integer.toString(this.offset);
                    }
                } else {
                    sb += " + ";
                    so = this.rOffset.toString();
                }
            }
        } else {
            if(this.index != Register.NONE) {
                si = this.index.toString();
                
                if(this.resolved) {
                    if(this.offset != 0) {
                        si += " + ";
                        so = Integer.toString(this.offset);
                    }
                } else {
                    si += " + ";
                    so = this.rOffset.toString();
                }
            } else {
                if(this.resolved) {
                    if(this.offset != 0) {
                        so = Integer.toString(this.offset);
                    }
                } else {
                    so = this.rOffset.toString();
                }
            }
        }
        
        return "[" + sb + si + so + "]";
    }
    
    public Register getBase() { return this.base; }
    public Register getIndex() { return this.index; }
    public int getScale() { return this.scale; }
    public int getOffset() { return this.offset; }
    public Resolvable getUnresolvedOffset() { return this.rOffset; }
    
}
