package notsotiny.asm.components;

/**
 * A Component representing some amount of uninitialized data.
 * 
 * @author Mechafinch
 */
public record UninitializedData(int size) implements Component {

    @Override
    public int getSize() {
        return this.size;
    }
}
