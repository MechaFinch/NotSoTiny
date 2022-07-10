package notsotiny.asm.components;

import java.util.List;

import notsotiny.asm.resolution.Resolvable;

/**
 * Type abstraction for parts of an assembled file. Instructions, labels, and data
 * 
 * @author Mechafinch
 */
public interface Component extends Resolvable {
    
    /**
     * Returns the current or maximum size of the component.
     * 
     * @return
     */
    public int getSize();
    
    /**
     * @return machine/object code. Unresolved values are zeroed.
     */
    public List<Byte> getObjectCode();
}
