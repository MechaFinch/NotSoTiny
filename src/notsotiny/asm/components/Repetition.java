package notsotiny.asm.components;

import java.util.ArrayList;
import java.util.List;

import notsotiny.asm.resolution.ResolvableValue;

/**
 * A Component representing the repetition of another Component
 * 
 * @author Mechafinch
 */
public class Repetition implements Component {
    
    private Component data;
    
    private ResolvableValue repetitions;
    
    /**
     * Constructor 
     * 
     * @param data Data to repeat
     * @param repetitions Number of times to repeat
     */
    public Repetition(Component data, ResolvableValue repetitions) {
        this.data = data;
        this.repetitions = repetitions;
    }
    
    public Component getData() { return this.data; }
    public ResolvableValue getReps() { return this.repetitions; }

    @Override
    public boolean isResolved() {
        return this.data.isResolved() && this.repetitions.isResolved();
    }

    @Override
    public int getSize() {
        return this.data.getSize() * (int) this.repetitions.value();
    }

    @Override
    public List<Byte> getObjectCode() {
        int reps = (int) this.repetitions.value();
        List<Byte> singleCode = this.data.getObjectCode();
        List<Byte> repeatedCode = new ArrayList<>(singleCode.size() * reps);
        
        for(int i = 0; i < reps; i++) {
            repeatedCode.addAll(singleCode);
        }
        
        return repeatedCode;
    }
    
    @Override
    public String toString() {
        return this.repetitions + " repetitions of " + this.data;
    }
    
}
