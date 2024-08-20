package notsotiny.sim.memory;

public class UnprivilegedAccessException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private final int address;
    
    public UnprivilegedAccessException(int address) {
        this.address = address;
    }
    
    public int getAddress() { return this.address; }
    
}
