package notsotiny.asm.resolution;

import notsotiny.asm.Register;

/**
 * Wrapper for Registers
 * 
 * @author Mechafinch
 */
public record ResolvableRegister(Register reg) implements Resolvable {

    @Override
    public boolean isResolved() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void resolve() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int value() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return null;
    }
    
}