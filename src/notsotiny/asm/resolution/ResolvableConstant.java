package notsotiny.asm.resolution;


/**
 * A value
 * 
 * @author Mechafinch
 */
public class ResolvableConstant implements ResolvableValue {
    
    private int val;
    
    private boolean resolved;
    
    private Resolvable parent;
    
    /**
     * Resolved constructor
     * 
     * @param val
     */
    public ResolvableConstant(Resolvable parent, int val) {
        this.val = val;
        this.parent = parent;
        
        this.resolved = true;
    }
    
    /**
     * Unresolved constructor
     * 
     * @param e
     */
    public ResolvableConstant(Resolvable parent) {
        this.parent = parent;
        
        this.val = -1;
        this.resolved = false;
    }

    @Override
    public boolean isResolved() {
        return this.resolved;
    }

    @Override
    public void resolve() {
        this.parent.resolve();
    }
    
    @Override
    public int value() {
        return this.val;
    }
    
    /**
     * Set the value and resolve
     * 
     * @param val
     */
    public void setValue(int val) {
        this.val = val;
        this.resolved = true;
        
        resolve();
    }
}
