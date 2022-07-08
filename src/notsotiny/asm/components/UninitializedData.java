package notsotiny.asm.components;

import notsotiny.asm.resolution.Resolvable;
import notsotiny.asm.resolution.ResolvableValue;

/**
 * A Component representing some amount of uninitialized data.
 * 
 * @author Mechafinch
 */
public class UninitializedData implements Component {
    
    private ResolvableValue sizeWords;
    
    private int wordSize;
    
    /**
     * Constructor
     * 
     * @param sizeWords
     * @param wordSize
     */
    public UninitializedData(ResolvableValue sizeWords, int wordSize) {
        this.sizeWords = sizeWords;
        this.wordSize = wordSize;
    }

    @Override
    public boolean isResolved() {
        return this.sizeWords.isResolved();
    }

    @Override
    public void resolve() {
        // TODO Auto-generated method stub
    }

    @Override
    public void setParent(Resolvable r) {
        // no parent
    }

    @Override
    public int getSize() {
        return (int) this.sizeWords.value() * this.wordSize;
    }
    
    @Override
    public String toString() {
        return this.getSize() + " bytes of uninitialized data";
    }
    
    public ResolvableValue getSizeWords() { return this.sizeWords; }
    public int getWordSize() { return this.wordSize; }
    
}
