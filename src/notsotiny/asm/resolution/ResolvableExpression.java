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
     * Note that when using the NOT operator, the left value is not used by the {@link #value()}
     * method but is still part of the resolved check. Use a constant zero or something
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
    
    /**
     * Tells the expression to minimize itself
     * Attempts to resolve each side, then applies the operator if successful. Sub-expressions are also minimized.
     */
    public ResolvableValue minimize() {
        // minimize sides
        if(this.left instanceof ResolvableExpression rel) {
            this.left = rel.minimize();
        }
        
        if(this.right instanceof ResolvableExpression rer) {
            this.right = rer.minimize();
        }
        
        // resolve if possible
        if(this.left.isResolved() && this.right.isResolved()) {
            return new ResolvableConstant(this.value());
        } else {
            return this;
        }
    }
    
    /**
     * @return true if the only operators in this expression and all sub-expressions are ADD or SUBTRACT
     */
    public boolean isSum() {
        if(this.operation != Operator.ADD && this.operation != Operator.SUBTRACT) return false;
        
        boolean l = !(this.left instanceof ResolvableExpression rel && !rel.isSum()),
                r = !(this.right instanceof ResolvableExpression rer && !rer.isSum());
        
        return l && r;
    }
    
    @Override
    public boolean isResolved() {
        return this.left.isResolved() && this.right.isResolved();
    }

    @Override
    public void resolve() {
        if(this.isResolved() && this.parent != null) this.parent.resolve();
    }
    
    /**
     * Gets the value of this expression
     * 
     * @return left [operation] right
     */
    @Override
    public long value() {
        long a = this.left.value(),
             b = this.right.value();
        
        return switch(this.operation) {
            case ADD        -> a + b;
            case SUBTRACT   -> a - b;
            case MULTIPLY   -> a * b;
            case DIVIDE     -> a / b;
            case AND        -> a & b;
            case OR         -> a | b;
            case XOR        -> a ^ b;
            case NOT        -> ~b;
            default -> throw new IllegalArgumentException("Unexpected value: " + this.operation); // auto generated
        };
    }
    
    @Override
    public ResolvableValue copy() {
        return new ResolvableExpression(this.left.copy(), this.right.copy(), this.operation);
    }
    
    @Override
    public void setParent(Resolvable r) {
        this.parent = r;
    }
    
    @Override
    public String toString() {
        if(this.operation == Operator.NOT) { // not
            return "~" + this.right;
        } else if(this.operation == Operator.SUBTRACT && this.left.isResolved() && this.left.value() == 0) { // negate
            return "-" + this.right;
        } else { // normal 2 argument things
            return "(" + this.left + " " + this.operation.character() + " " + this.right + ")";
        }
    }
    
    public ResolvableValue getLeft() { return this.left; }
    public ResolvableValue getRight() { return this.right; }
    public Operator getOperation() { return this.operation; }
}
