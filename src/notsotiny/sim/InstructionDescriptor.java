package notsotiny.sim;

import notsotiny.sim.ops.Opcode;

/**
 * Flags for information used multiple times so it doesn't need to be recomputed
 * 
 * @author Mechafinch
 */
public class InstructionDescriptor {
    
    public Opcode op;
    
    public boolean hasRIMByte = false,          // used by all instructions. true if a RIM byte is processed
                   hasBIOByte = false,          // used by all instructions. true if a BIO byte is processed
                   hasImmediateValue = false,   // used by all instructions. true if an immediate value is read
                   hasImmediateAddress = false; // used by all instructions. true if an immediate address is read
    
    public int immediateWidth = 0,              // used by all instructions. the width of an immediate value/address
               sourceWidth = 0;                 // used by thin sources. the width of the source
}
