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
    
    private ResolvableValue offset;
    
    private int scale;
    
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
        this.offset = new ResolvableConstant(offset);
        
        this.parent = null;
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
        this.offset = offset;
        
        this.parent = null;
        
        this.offset.setParent(this);
    }

    @Override
    public boolean isResolved() {
        return this.offset.isResolved();
    }

    @Override
    public void resolve() {
        if(this.isResolved()) this.parent.resolve();
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
        
        boolean resolved = this.isResolved();
        
        if(this.base != Register.NONE) {
            sb = this.base.toString();
            
            if(this.index != Register.NONE) {
                sb += " + ";
                si = this.index.toString();
                
                if(!resolved || (resolved && this.offset.value() != 0)) {
                    si += " + ";
                    so = this.offset.toString();
                }
            } else {
                if(!resolved || (resolved && this.offset.value() != 0)) {
                    sb += " + ";
                    so = this.offset.toString();
                }
            }
        } else {
            if(this.index != Register.NONE) {
                si = this.index.toString();
                
                if(!resolved || (resolved && this.offset.value() != 0)) {
                    si += " + ";
                    so = this.offset.toString();
                }
            } else {
                if(!resolved || (resolved && this.offset.value() != 0)) {
                    so = this.offset.toString();
                }
            }
        }
        
        return "[" + sb + si + so + "]";
    }
    
    public Register getBase() { return this.base; }
    public Register getIndex() { return this.index; }
    public int getScale() { return this.scale; }
    public ResolvableValue getOffset() { return this.offset; }
    
}
