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
}
