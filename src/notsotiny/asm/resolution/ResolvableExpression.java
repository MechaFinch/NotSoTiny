package notsotiny.asm.resolution;

import notsotiny.asm.Operator;

/**
 * An expression constructed with Resolvables separated by Operators
 * Each ResolvableExpression contains two Resolvable values separated by one Operator, and a Resolvable parent
 * 
 * @author Mechafinch
 */
public class ResolvableExpression implements ResolvableValue {
    
    private Resolvable parent;  // container Resolvable
    
    private ResolvableValue left,    // left side
                            right;   // right side
    
    private Operator operation;

    /**
     * Constructor
     * 
     * @param parent
     * @param left
     * @param right
     * @param operation
     */
    public ResolvableExpression(ResolvableValue left, ResolvableValue right, Operator operation) {
        this.left = left;
        this.right = right;
        this.operation = operation;
        
        this.parent = null;
        
        this.left.setParent(this);
        this.right.setParent(this);
    }
    
    @Override
    public boolean isResolved() {
        return left.isResolved() && right.isResolved();
    }

    @Override
    public void resolve() {
        if(this.isResolved()) parent.resolve();
    }
    
    /**
     * Gets the value of this expression
     * 
     * @return left [operation] right
     */
    @Override
    public int value() {
        int a = this.left.value(),
            b = this.right.value();
        
        return switch(this.operation) {
            case ADD        -> a + b;
            case SUBTRACT   -> a - b;
            case MULTIPLY   -> a * b;
            case DIVIDE     -> a / b;
        };
    }
    
    @Override
    public void setParent(Resolvable r) {
        this.parent = r;
    }
    
    public Resolvable getLeft() { return this.left; }
    public Resolvable getRight() { return this.right; }
    public Operator getOperation() { return this.operation; }
}
