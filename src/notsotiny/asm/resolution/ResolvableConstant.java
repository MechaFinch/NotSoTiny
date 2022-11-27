package notsotiny.asm.resolution;


/**
 * A value. Has a name if unresolved
 * 
 * @author Mechafinch
 */
public class ResolvableConstant implements ResolvableValue {
    
    private long val;
    
    private boolean resolved;
    
    private String name;
    
    /**
     * Resolved constructor
     * 
     * @param val
     */
    public ResolvableConstant(long val) {
        this.val = val;
        
        this.resolved = true;
        this.name = null;
    }
    
    /**
     * Unresolved constructor
     * 
     * @param name
     */
    public ResolvableConstant(String name) {
        this.name = name;
        
        this.val = -1;
        this.resolved = false;
    }

    @Override
    public boolean isResolved() {
        return this.resolved;
    }
    
    @Override
    public long value() {
        return this.val;
    }
    
    @Override
    public void unresolveNames() {
        if(this.name != null) {
            this.resolved = false;
        }
    }
    
    @Override
    public ResolvableValue copy() {
        if(this.resolved) {
            return new ResolvableConstant(this.val);
        } else {
            return new ResolvableConstant(this.name);
        }
    }
    
    /**
     * Set the value and resolve
     * 
     * @param val
     */
    public void setValue(int val) {
        this.val = val;
        this.resolved = true;
    }
    
    @Override
    public String toString() {
        if(this.resolved) {
            return Long.toString(this.val);
        } else {
            return this.name;
        }
    }
    
    public String getName() { return this.name; }
}
