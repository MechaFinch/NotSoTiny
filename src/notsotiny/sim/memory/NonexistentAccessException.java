package notsotiny.sim.memory;

/**
 * Exception thrown accessing memory that does not exist
 */
public class NonexistentAccessException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    private final int address;
    
    public NonexistentAccessException(int address) {
        this.address = address;
    }
    
    public int getAddress() { return this.address; }
    
}
