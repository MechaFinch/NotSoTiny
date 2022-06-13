package notsotiny.asm.resolution;

/**
 * An offset between two labels
 * 
 * @author Mechafinch
 */
public class ResolvableOffset implements ResolvableValue {
    
    private int val;
    
    private boolean resolved;
    
    private String originName,
                   targetName;
    
    private Resolvable parent;
    
    /**
     * Resolved constructor
     * 
     * @param val
     */
    public ResolvableOffset(int val) {
        this.val = val;
        
        this.resolved = true;
        this.originName = null;
        this.targetName = null;
        this.parent = null;
    }
    
    /**
     * Unresolved constructor
     * 
     * @param originName
     * @param targetName
     */
    public ResolvableOffset(String originName, String targetName) {
        this.originName = originName;
        this.targetName = targetName;
        
        this.val = -1;
        this.resolved = false;
        this.parent = null;
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
    
    @Override
    public void setParent(Resolvable r) {
        this.parent = r;
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
    
    @Override
    public String toString() {
        if(this.resolved) {
            return Integer.toString(this.val);
        } else {
            return this.originName + "->" + this.targetName;
        }
    }
    
    public String getOriginName() { return this.originName; }
    public String getTargetName() { return this.targetName; }
}
