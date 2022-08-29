package notsotiny.asm;

/**
 * Registers involved in assembly
 * 
 * @author Mechafinch
 */
public enum Register {
    DA(4), AB(4), BC(4), CD(4), JI(4), LK(4), BP(4), SP(4),
    A(2), B(2), C(2), D(2), I(2), J(2), K(2), L(2), F(2), PF(2),
    AH(1), AL(1), BH(1), BL(1), CH(1), CL(1), DH(1), DL(1),
    NONE(0);
    
    private int size;
    
    private Register(int size) {
        this.size = size;
    }
    
    /**
     * @return size in bytes
     */
    public int size() { return this.size; }
}
