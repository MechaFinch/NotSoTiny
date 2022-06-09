package notsotiny.asm.components;

/**
 * Type abstraction for parts of an assembled file. Instructions, labels, and data
 * 
 * @author Mechafinch
 */
public interface Component {
    
    /**
     * Returns the current or maximum size of the component.
     * 
     * @return
     */
    public int getSize();
}
