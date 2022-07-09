package notsotiny.asm.resolution;

public interface ResolvableValue extends Resolvable {
    
    /**
     * @return The value
     */
    public long value();
    
    /**
     * @return A deep copy of this ResolvableValue
     */
    public ResolvableValue copy();
}
