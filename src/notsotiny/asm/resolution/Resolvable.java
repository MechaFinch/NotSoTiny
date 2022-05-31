package notsotiny.asm.resolution;

/**
 * An abstraction for values that may have a resolved or unresolved location or value
 * 
 * @author Mechafinch
 */
public interface Resolvable {
    
    /**
     * @return true if this Resolvable has a concrete value
     */
    public boolean isResolved();
    
    /**
     * Tells this Resolvable to attempt to resolve itself with new information. 
     * Called by children when their values are resolved
     */
    public void resolve();
    
    /**
     * @return The resolved value of this object
     */
    public int value();
}
