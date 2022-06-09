package notsotiny.asm.components;

/**
 * A Component representing some amount of initialized data.
 * 
 * @author Mechafinch
 */
public record InitializedData(byte[] data) implements Component {

    @Override
    public int getSize() {
        return this.data.length;
    }
}