package notsotiny.asm.resolution;

public interface ResolvableValue extends Resolvable {
    
    /**
     * @return The value
     */
    public long value();
    
    /**
     * Marks named values as unresolved
     */
    public void unresolveNames();
    
    /**
     * @return A deep copy of this ResolvableValue
     */
    public ResolvableValue copy();
}
