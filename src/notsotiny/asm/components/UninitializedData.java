package notsotiny.asm.components;

import java.util.LinkedList;
import java.util.List;

import notsotiny.asm.resolution.Resolvable;

/**
 * A Component representing some amount of uninitialized data.
 * 
 * @author Mechafinch
 */
public class UninitializedData implements Component {
    
    private int sizeWords,
                wordSize;
    
    /**
     * Constructor
     * 
     * @param sizeWords
     * @param wordSize
     */
    public UninitializedData(int sizeWords, int wordSize) {
        this.sizeWords = sizeWords;
        this.wordSize = wordSize;
    }
    
    @Override
    public List<Byte> getObjectCode() {
        List<Byte> data = new LinkedList<>();
        
        for(int i = 0; i < (this.sizeWords * this.wordSize); i++) data.add((byte) 0);
        
        return data;
    }

    @Override
    public boolean isResolved() {
        return true;
    }

    @Override
    public void resolve() {
        // not applicible
    }

    @Override
    public void setParent(Resolvable r) {
        // no parent
    }

    @Override
    public int getSize() {
        return this.sizeWords * this.wordSize;
    }
    
    @Override
    public String toString() {
        return this.getSize() + " bytes of uninitialized data";
    }
    
    public int getSizeWords() { return this.sizeWords; }
    public int getWordSize() { return this.wordSize; }
    
}
