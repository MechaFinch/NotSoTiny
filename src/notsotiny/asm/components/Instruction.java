package notsotiny.asm.components;

import java.util.LinkedList;
import java.util.List;

import notsotiny.asm.Register;
import notsotiny.asm.resolution.Resolvable;
import notsotiny.asm.resolution.ResolvableLocationDescriptor;
import notsotiny.asm.resolution.ResolvableLocationDescriptor.LocationType;
import notsotiny.asm.resolution.ResolvableMemory;
import notsotiny.asm.resolution.ResolvableValue;
import notsotiny.sim.ops.Opcode;

/**
 * Represents an instruction for assembly
 * 
 * @author Mechafinch
 */
public class Instruction implements Component {
    
    private Opcode op;
    
    private ResolvableLocationDescriptor destination, source;
    
    private int cachedWidth = -1,
                immediateWidth = -1;
    
    boolean overrideImmediateWidth = false;
    
    /**
     * Create an instruction with source and destination
     * 
     * @param op
     * @param destination
     * @param source
     * @param address
     */
    public Instruction(Opcode op, ResolvableLocationDescriptor destination, ResolvableLocationDescriptor source) {
        this.op = op;
        this.destination = destination;
        this.source = source;
    }
    
    /**
     * Create an instruction with destination only
     * 
     * @param op
     * @param location
     * @param destination
     * @param address
     */
    public Instruction(Opcode op, ResolvableLocationDescriptor location, boolean destination) {
        this(op, destination ? location : ResolvableLocationDescriptor.NONE, destination ? ResolvableLocationDescriptor.NONE : location);
    }
    
    /**
     * Create an instruction with no parameters
     * 
     * @param op
     */
    public Instruction(Opcode op) {
        this(op, ResolvableLocationDescriptor.NONE, ResolvableLocationDescriptor.NONE);
    }
    
    @Override
    public List<Byte> getObjectCode() {
        List<Byte> data = new LinkedList<>();
        data.add(this.op.getOp());
        
        // what do we need to do
        switch(this.op) {
            // nothing
            case NOP, XCHG_AH_AL, XCHG_BH_BL, XCHG_CH_CL, XCHG_DH_DL, MOV_A_B, MOV_A_C, MOV_A_D, MOV_B_A,
                 MOV_B_C, MOV_B_D, MOV_C_A, MOV_C_B, MOV_C_D, MOV_D_A, MOV_D_B, MOV_D_C, MOV_AL_BL,
                 MOV_AL_CL, MOV_AL_DL, MOV_BL_AL, MOV_BL_CL, MOV_BL_DL, MOV_CL_AL, MOV_CL_BL, MOV_CL_DL,
                 MOV_DL_AL, MOV_DL_BL, MOV_DL_CL, XCHG_A_B, XCHG_A_C, XCHG_A_D, XCHG_B_C, XCHG_B_D,
                 XCHG_C_D, XCHG_AL_BL, XCHG_AL_CL, XCHG_AL_DL, XCHG_BL_CL, XCHG_BL_DL, XCHG_CL_DL,
                 PUSH_A, PUSH_B, PUSH_C, PUSH_D, PUSH_I, PUSH_J, PUSH_BP, PUSH_SP, PUSH_F, POP_A, POP_B,
                 POP_C, POP_D, POP_I, POP_J, POP_BP, POP_SP, POP_F, NOT_F, INC_I, INC_J, ICC_I, ICC_J,
                 DEC_I, DEC_J, DCC_I, DCC_J, RET, IRET:
                break;
            
            // 8 bit immediate only
            case MOV_A_I8, MOV_B_I8, MOV_C_I8, MOV_D_I8, ADD_A_I8, ADD_B_I8, ADD_C_I8, ADD_D_I8, ADC_A_I8,
                 ADC_B_I8, ADC_C_I8, ADC_D_I8, SUB_A_I8, SUB_B_I8, SUB_C_I8, SUB_D_I8, SBB_A_I8, SBB_B_I8,
                 SBB_C_I8, SBB_D_I8, JMP_I8, JC_I8, JNC_I8, JS_I8, JNS_I8, JO_I8, JNO_I8, JZ_I8, JNZ_I8,
                 JA_I8, JBE_I8, JG_I8, JGE_I8, JL_I8, JLE_I8:
                data.addAll(getImmediateData(this.source.getImmediate(), 1));
                break;
            
            // 16 bit immediate only
            case MOV_I_I16, MOV_J_I16, MOV_A_I16, MOV_B_I16, MOV_C_I16, MOV_D_I16, ADD_A_I16, ADD_B_I16,
                 ADD_C_I16, ADD_D_I16, ADC_A_I16, ADC_B_I16, ADC_C_I16, ADC_D_I16, SUB_A_I16, SUB_B_I16,
                 SUB_C_I16, SUB_D_I16, SBB_A_I16, SBB_B_I16, SBB_C_I16, SBB_D_I16, JMP_I16, CALL_I16,
                 INT_I16:
                data.addAll(getImmediateData(this.source.getImmediate(), 2));
                break;
            
            // 32 bit immediate only
            case MOV_SP_I32, MOV_BP_I32, PUSH_I32, JMP_I32, JMPA_I32, CALLA_I32:
                data.addAll(getImmediateData(this.source.getImmediate(), 4));
                break;
            
            // 32 bit immediate from memory
            case MOV_A_O, MOV_B_O, MOV_C_O, MOV_D_O:
                data.addAll(getImmediateData(this.source.getMemory().getOffset(), 4));
                break;
            
            case MOV_O_A, MOV_O_B, MOV_O_C, MOV_O_D:
                data.addAll(getImmediateData(this.destination.getMemory().getOffset(), 4));
                break;
            
            // BIO only
            case MOV_A_BI, MOV_B_BI, MOV_C_BI, MOV_D_BI:
                data.addAll(getBIOData(this.source.getMemory(), false));
                break;
            
            case MOV_BI_A, MOV_BI_B, MOV_BI_C, MOV_BI_D:
                data.addAll(getBIOData(this.destination.getMemory(), false));
                break;
            
            case MOV_A_BIO, MOV_B_BIO, MOV_C_BIO, MOV_D_BIO:
                data.addAll(getBIOData(this.source.getMemory(), true));
                break;
                
            case MOV_BIO_A, MOV_BIO_B, MOV_BIO_C, MOV_BIO_D:
                data.addAll(getBIOData(this.destination.getMemory(), true));
                break;
            
            // RIM special cases
            // RIM + 8 bit immediate
            case ADD_RIM_I8, ADC_RIM_I8, SUB_RIM_I8, SBB_RIM_I8, CMP_RIM_I8:
                data.addAll(getRIMData(true, false, false, false, false));
                data.addAll(getImmediateData(this.source.getImmediate(), 1));
                break;
                 
            // packed
            case PADD_RIMP, PADC_RIMP, PSUB_RIMP, PSBB_RIMP, PMUL_RIMP, PDIV_RIMP, PDIVS_RIMP:
                data.addAll(getRIMData(true, true, true, false, false));
                break;
            
            case PMULH_RIMP, PMULSH_RIMP, PDIVM_RIMP, PDIVMS_RIMP:
                data.addAll(getRIMData(true, true, true, true, false));
                break;
            
            // source only
            case PUSH_RIM, AND_F_RIM, OR_F_RIM, XOR_F_RIM, MOV_F_RIM, JMP_RIM, CALL_RIM, INT_RIM, JC_RIM,
                 JNC_RIM, JS_RIM, JNS_RIM, JO_RIM, JNO_RIM, JZ_RIM, JNZ_RIM, JA_RIM, JBE_RIM, JG_RIM,
                 JGE_RIM, JL_RIM, JLE_RIM:
                data.addAll(getRIMData(false, true, false, false, false));
                break;
            
            case JMPA_RIM32, CALLA_RIM32:
                data.addAll(getRIMData(false, true, false, false, true));
                break;
            
            // destination only
            case POP_RIM, AND_RIM_F, OR_RIM_F, XOR_RIM_F, MOV_RIM_F, INC_RIM, ICC_RIM, DEC_RIM, DCC_RIM,
                 NOT_RIM, NEG_RIM, CMP_RIM, CMP_RIM_0:
                data.addAll(getRIMData(true, false, false, false, false));
                break;
            
            case PINC_RIMP, PICC_RIMP, PDEC_RIMP, PDCC_RIMP:
                data.addAll(getRIMData(true, false, true, false, false));
                break;
            
            // wide destination
            case MOVS_RIM, MOVZ_RIM, MULH_RIM, MULSH_RIM, DIVM_RIM, DIVMS_RIM:
                data.addAll(getRIMData(true, true, false, true, false));
                break;
            
            // w i d e
            case MOVW_RIM:
                data.addAll(getRIMData(true, true, false, true, true));
                break;
            
            // pure RIM
            default:
                data.addAll(getRIMData(true, true, false, false, false));
        }
        
        return data;
    }
    
    /**
     * Constructs RIM bytes
     * 
     * @param includeDestination
     * @param includeSource
     * @param packed
     * @param wideDestination
     * @param wideSource
     * @return
     */
    private List<Byte> getRIMData(boolean includeDestination, boolean includeSource, boolean packed, boolean wideDestination, boolean wideSource) {
        List<Byte> data = new LinkedList<>();
        byte rim = 0;
        
        LocationType sourceType = this.source.getType(),
                     destType = this.destination.getType();
        
        // validate operand types
        if(includeDestination) {
            switch(destType) {
                case MEMORY:
                    if(includeSource) {
                        if(sourceType != LocationType.REGISTER) throw new IllegalArgumentException("Invalid RIM source for memory: " + this.source);
                    }
                    break;
                    
                case REGISTER:
                    if(includeSource) {
                        if(sourceType == LocationType.NULL) throw new IllegalArgumentException("Invalid RIM source: " + this.source);
                    }
                    break;
                    
                default:
                    throw new IllegalArgumentException("Invalid RIM destination: " + this.destination);
            }
        } else if(includeSource) {
            if(sourceType == LocationType.NULL) throw new IllegalArgumentException("Invalid RIM source: " + this.source);
        }
        
        // determine operand sizes
        int sourceSize = includeSource ? this.source.getSize() : -1,
            destSize = includeDestination ? this.destination.getSize() : -1;
        
        if(sourceSize == -1 && destSize == -1) {
            // this is only valid in special cases
            switch(this.op) {
                // source only 32 bit
                case JMPA_RIM32, CALLA_RIM32:
                    sourceSize = 4;
                    break;
                
                // destination only 16 bit
                case AND_RIM_F, OR_RIM_F, XOR_RIM_F, MOV_RIM_F:
                    destSize = 2;
                    break;
                
                // source only 16 bit
                case AND_F_RIM, OR_F_RIM, XOR_F_RIM, MOV_F_RIM, INT_RIM:
                    sourceSize = 2;
                    break;
                
                default:
                    // if we've just an immediate, assume 16 bit?
                    if(includeSource && sourceType == LocationType.IMMEDIATE) {
                        ResolvableValue rv = this.source.getImmediate();
                        
                        sourceSize = rv.isResolved() ? getValueWidth(rv.value(), false, false) : 2;
                    } else {
                        throw new IllegalArgumentException("Cannot infer operand sizes: " + this);
                    }
            }
        }
        
        if(includeSource && sourceSize == -1) {
            // dest size is known. do we copy or halve it?
            if(wideDestination && !wideSource) { // halve
                if(destSize == 1) throw new IllegalArgumentException("Destination too small: " + this);
                
                sourceSize = destSize / 2;
            } else { // copy
                sourceSize = destSize;
            }
        } else if(includeDestination && destSize == -1) {
            // source size is known. do we copy or double it?
            if(wideDestination && !wideSource) {
                if(sourceSize == 4) throw new IllegalArgumentException("Source too large: " + this);
                
                destSize = sourceSize * 2;
            } else {
                destSize = sourceSize;
            }
        }
        
        // size bit
        // this turns out simpler than i thought lol
        if(includeSource) {
            if(sourceSize == 1) rim |= 0b10_000_000;
        } else {
            if(destSize == 1) rim |= 0b10_000_000;
        }
        
        // type bit
        if(includeSource && includeDestination) {
            // 0 for register-register
            if(sourceType != LocationType.REGISTER || destType != LocationType.REGISTER) {
                rim |= 0b01_000_000;
            }
        } else if(includeSource) {
            // source only. 0 for register
            if(sourceType != LocationType.REGISTER) rim |= 0b01_000_000;
        } else {
            // destination only. 0 for register dest
            if(destType != LocationType.REGISTER) rim |= 0b01_000_000;
        }
        
        List<Byte> bioData = null;
        
        // reg
        // this is the destination register unless dest is memory
        if(includeDestination && destType == LocationType.REGISTER) {
            rim |= switch(this.destination.getRegister()) {
                case AL, A, DA  -> 0b00_000_000;
                case BL, B, AB  -> 0b00_001_000;
                case CL, C, BC  -> 0b00_010_000;
                case DL, D, CD  -> 0b00_011_000;
                case AH, I, JI  -> 0b00_100_000;
                case BH, J, IJ  -> 0b00_101_000;
                case CH, BP     -> 0b00_110_000;
                case DH, SP     -> 0b00_111_000;
                default         -> 0; // handled earlier
            };
        } else if(includeSource && sourceType == LocationType.REGISTER) {
            rim |= switch(this.source.getRegister()) {
                case AL, A, DA  -> 0b00_000_000;
                case BL, B, AB  -> 0b00_001_000;
                case CL, C, BC  -> 0b00_010_000;
                case DL, D, CD  -> 0b00_011_000;
                case AH, I, JI  -> 0b00_100_000;
                case BH, J, IJ  -> 0b00_101_000;
                case CH, BP     -> 0b00_110_000;
                case DH, SP     -> 0b00_111_000;
                default         -> 0; // handled earlier
            };
        }
        
        // rim
        if((rim & 0b01_000_000) != 0) { // register-register
            rim |= switch(this.source.getRegister()) {
                case AL, A, DA  -> 0b00_000_000;
                case BL, B, AB  -> 0b00_000_001;
                case CL, C, BC  -> 0b00_000_010;
                case DL, D, CD  -> 0b00_000_011;
                case AH, I, JI  -> 0b00_000_100;
                case BH, J, IJ  -> 0b00_000_101;
                case CH, BP     -> 0b00_000_110;
                case DH, SP     -> 0b00_000_111;
                default         -> 0; // handled earlier
            };
        //} else if(includeSource && sourceType == LocationType.IMMEDIATE) { // do nothing
            
        } else if((includeSource && sourceType == LocationType.MEMORY) || (includeDestination && destType == LocationType.MEMORY)) {
            // bio nonsense
            boolean src = (includeSource && sourceType == LocationType.MEMORY);
            ResolvableMemory rm = src ? this.source.getMemory() : this.destination.getMemory();
            ResolvableValue offs = rm.getOffset();
            
            boolean includeOffset = !offs.isResolved() || offs.value() != 0;
            
            if(rm.getBase() != Register.NONE || rm.getIndex() != Register.NONE) {
                rim |= includeOffset ? (src ? 0b00_000_011 : 0b00_000_111) : (src ? 0b00_000_010 : 0b00_000_110);
                bioData = getBIOData(rm, includeOffset);
            } else {
                rim |= src ? 0b00_000_001 : 0b00_000_101;
                bioData = getImmediateData(offs, 4);
            }
        }
        
        data.add(rim);
        if(bioData != null) data.addAll(bioData);
        
        // immediate
        if(includeSource && sourceType == LocationType.IMMEDIATE) {
            data.addAll(getImmediateData(this.source.getImmediate(), this.source.getSize()));
        }
        
        return data;
    }
    
    /**
     * Constucts BIO bytes.
     * 
     * @param rm
     * @param includeOffset
     * @return
     */
    private List<Byte> getBIOData(ResolvableMemory rm, boolean includeOffset) {
        List<Byte> data = new LinkedList<>();
        byte bio = 0;
        
        boolean hasIndex = rm.getIndex() == Register.NONE;
        
        // index
        bio |= switch(rm.getIndex()) {
            case A      -> 0b000;
            case B      -> 0b001;
            case C      -> 0b010;
            case D      -> 0b011;
            case I      -> 0b100;
            case J      -> 0b101;
            case BP     -> 0b110;
            case NONE   -> 0b111;
            default -> throw new IllegalArgumentException("Invalid index: " + rm.getIndex());
        };
        
        // check
        if(hasIndex && rm.getBase() == Register.NONE) throw new IllegalArgumentException("Must have at least one register for BIO: " + rm); 
        
        // base
        bio |= switch(rm.getBase()) {
            case DA         -> 0b000_000;
            case AB         -> 0b001_000;
            case BC         -> 0b010_000;
            case CD         -> 0b011_000;
            case JI         -> 0b100_000;
            case IJ         -> 0b101_000;
            case BP         -> 0b110_000;
            case SP, NONE   -> 0b111_000;
            default         -> throw new IllegalArgumentException("Invalid base: " + rm.getBase());
        };
        
        int offsetSize = 4;
        
        // scale
        if(hasIndex) { // scale scales the index
            bio |= (rm.getScale() & 0b11) << 6;
        } else if(includeOffset && rm.getOffset().isResolved()) { // scale scales the offset
            long offset = rm.getOffset().value();
            
            offsetSize = getValueWidth(offset, true, true);
        }
        
        data.add(bio);
        
        // offset
        if(includeOffset) {
            data.addAll(getImmediateData(rm.getOffset(), offsetSize));
        }
        
        return data;
    }
    
    /**
     * Converts a value to its bytes
     * 
     * @param val
     * @param size
     * @return
     */
    private List<Byte> getImmediateData(ResolvableValue rv, int size) {
        long val = rv.isResolved() ? rv.value() : 0;
        List<Byte> data = new LinkedList<>();
        
        switch(size) {
            case 1:
                data.add((byte)(val & 0xFF));
                break;
            
            case 2:
                data.add((byte)(val & 0xFF));
                data.add((byte)((val >> 8) & 0xFF));
                break;
            
            case 3:
                data.add((byte)(val & 0xFF));
                data.add((byte)((val >> 8) & 0xFF));
                data.add((byte)((val >> 16) & 0xFF));
                break;
            
            case 4:
                data.add((byte)(val & 0xFF));
                data.add((byte)((val >> 8) & 0xFF));
                data.add((byte)((val >> 16) & 0xFF));
                data.add((byte)((val >> 24) & 0xFF));
                break;
        }
        
        return data;
    }
    
    @Override
    public int getSize() {
        // slower but guaranteed to work
        return getObjectCode().size();
    }
    
    /**
     * Gets the width of a value in bytes
     * 
     * @param v
     * @param three true if 3 is allowed
     * @return
     */
    private int getValueWidth(long v, boolean three, boolean four) {
        if(v >= -128 && v <= 127) { // 1 byte
            return 1;
        } else if(v >= -32768 && v <= 32767) { // 2 byte
            return 2;
        } else if(v >= -8388608 && v <= 8388607) { // 3 byte
            if(three) return 3;
            if(four) return 4;
            
            throw new IllegalArgumentException("value too large for 2 bytes: " + v);
        } else { // 4 byte
            if(four) return 4;
            
            throw new IllegalArgumentException("value too large for 2 bytes: " + v);
        }
    }
    
    /**
     * @return The location of the immediate relative to the start of the instruction. Will be at least 1
     */
    public int getImmediateOffset() {
        return -1; // TODO
    }
    
    @Override
    public boolean isResolved() {
        return this.source.isResolved() && this.destination.isResolved();
    }
    
    @Override
    public void resolve() {
        // TODO Auto-generated method stub
    }

    @Override
    public void setParent(Resolvable r) {
        // Instructions don't have parents
    }
    
    @Override
    public String toString() {
        String src = this.source.toString(),
               dst = this.destination.toString(),
               str = this.op.toString();
        
        if(!dst.equals("")) {
            str += " " + dst;
            
            if(!src.equals("")) {
                str += ", " + src;
            }
        } else if(!src.equals("")) {
            str += " " + src;
        }
        
        return str;
    }
    
    /**
     * Sets the value to override immediate width with. Set to zero for a value of 0 (affects BIO offsets only)
     * @param w
     */
    public void setImmediateWidth(int w) { this.immediateWidth = w; }
    
    /**
     * Sets whether or not to override immediate width in size calculation
     * @param o
     */
    public void setOverrideImmediateWidth(boolean o) { this.overrideImmediateWidth = o; }
    
    public void setOpcode(Opcode o) { this.op = o; }
    
    public ResolvableLocationDescriptor getSourceDescriptor() { return this.source; }
    public ResolvableLocationDescriptor getDestinationDescriptor() { return this.destination; }
    public Opcode getOpcode() { return this.op; }
}
