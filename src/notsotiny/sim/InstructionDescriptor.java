package notsotiny.sim;

import notsotiny.sim.ops.Opcode;

/**
 * Holds information about a decoded instruction
 */
public class InstructionDescriptor {
    public Opcode opcode;
    
    // Physical instruction decode
    public byte i8Byte = 0;
    
    public int instructionSize = 1;
    
    // Dynamic information
    public LocationDescriptor sourceDescriptor,
                              destinationDescriptor;
    
    public int sourceValue = 0,
               destinationValue = 0,
               destinationAddress;
    
    public boolean hasOffset = false,
                   hasEI8 = false,
                   isPacked = false,
                   packedIs4s = false;
    
    public void reset(Opcode opcode) {
        this.opcode = opcode;
        this.i8Byte = 0;
        this.instructionSize = 1;
        this.sourceDescriptor = null;
        this.destinationDescriptor = null;
        this.sourceValue = 0;
        this.destinationValue = 0;
        this.destinationAddress = 0;
        this.hasOffset = false;
        this.hasEI8 = false;
        this.isPacked = false;
        this.packedIs4s = false;
    }
}
