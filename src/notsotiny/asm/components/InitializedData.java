package notsotiny.asm.components;

import java.util.List;
import java.util.stream.Collectors;

import notsotiny.asm.resolution.Resolvable;
import notsotiny.asm.resolution.ResolvableValue;

/**
 * A Component representing some amount of initialized data.
 * 
 * @author Mechafinch
 */
public class InitializedData implements Component {
    
    private List<ResolvableValue> data;
    
    private int wordSize;
    
    /**
     * Constructor
     * 
     * @param data
     * @param wordSize bytes per word
     */
    public InitializedData(List<ResolvableValue> data, int wordSize) {
        this.data = data;
        this.wordSize = wordSize;
    }
    
    /**
     * @return Any unresolved values in this data set
     */
    public List<ResolvableValue> getUnresolvedData() {
        return this.data.stream().filter(rv -> !rv.isResolved()).collect(Collectors.toList());
    }

    @Override
    public int getSize() {
        return data.size() * wordSize;
    }

    @Override
    public boolean isResolved() {
        return data.stream().allMatch(rv -> rv.isResolved());
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
    public String toString() {
        String s = getSize() + " bytes of data: [";
        
        for(ResolvableValue rv : this.data) {
            s += rv + ", ";
        }
        
        return s.substring(0, s.length() - 2) + "]";
    }
    
    public List<ResolvableValue> getData() { return this.data; }
    public int getWordSize() { return this.wordSize; }
}