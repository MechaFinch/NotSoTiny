package notsotiny.asm.components;

import java.util.LinkedList;
import java.util.List;

import notsotiny.asm.Register;
import notsotiny.asm.resolution.ResolvableConstant;
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
    
    private int cachedAddressOffset = -1,
                cachedImmediateOffset = -1,
                immediateWidth = -1,
                ei8 = -1;
    
    private boolean hasFixedSize,
                    hasEI8;
    
    /**
     * Create an instruction with source and destination
     * 
     * @param op
     * @param destination
     * @param source
     * @param address
     * @param hasFixedSize true if the operand size was set by the source code
     */
    public Instruction(Opcode op, ResolvableLocationDescriptor destination, ResolvableLocationDescriptor source, boolean hasFixedSize) {
        this.op = op;
        this.destination = destination;
        this.source = source;
        this.hasFixedSize = hasFixedSize;
        this.hasEI8 = false;
    }
    
    /**
     * Create an instruction with destination only
     * 
     * @param op
     * @param location
     * @param destination
     * @param hasFixedSize true if the operand size was set by the source code
     */
    public Instruction(Opcode op, ResolvableLocationDescriptor location, boolean destination, boolean hasFixedSize) {
        this(op, destination ? location : ResolvableLocationDescriptor.NONE, destination ? ResolvableLocationDescriptor.NONE : location, hasFixedSize);
    }
    
    /**
     * Create an instruction with no parameters
     * 
     * @param op
     * @param hasFixedSize true if the operand size was set by the source code
     */
    public Instruction(Opcode op, boolean hasFixedSize) {
        this(op, ResolvableLocationDescriptor.NONE, ResolvableLocationDescriptor.NONE, hasFixedSize);
    }
    
    /**
     * Create an instruction with source, destination, and EI8
     * 
     * @param op
     * @param destination
     * @param source
     * @param ei8
     * @param hasFixedSize
     */
    public Instruction(Opcode op, ResolvableLocationDescriptor destination, ResolvableLocationDescriptor source, int ei8, boolean hasFixedSize) {
        this.op = op;
        this.destination = destination;
        this.source = source;
        this.hasFixedSize = hasFixedSize;
        this.ei8 = ei8;
        this.hasEI8 = true;
    }
    
    @Override
    public List<Byte> getObjectCode() {
        if(!hasValidOperands()) throw new IllegalArgumentException("Invalid operands: " + this);
        
        this.cachedImmediateOffset = -1;
        this.cachedAddressOffset = -1;
        List<Byte> data = new LinkedList<>();
        data.add(this.op.getOp());
        
        // what do we need to do
        switch(this.op) {
            // nothing
            case NOP, MOV_A_B, MOV_A_C, MOV_A_D, MOV_B_A, MOV_B_C, MOV_B_D, MOV_C_A, MOV_C_B, MOV_C_D,
                 MOV_D_A, MOV_D_B, MOV_D_C, 
                 PUSH_A, PUSH_B, PUSH_C, PUSH_D, PUSH_I, PUSH_J, PUSH_K, PUSH_L, PUSH_BP, BPUSH_SP,
                 PUSH_F, PUSH_PF, POP_A, POP_B, POP_C, POP_D, POP_I, POP_J, POP_K, POP_L, POP_BP, 
                 BPOP_SP, POP_F, POP_PF, NOT_F, INC_I, INC_J, INC_K, INC_L, ICC_I, ICC_J, ICC_K, ICC_L,
                 DEC_I, DEC_J, ICC_A, ICC_B, ICC_C, ICC_D, DCC_A, DCC_B, DCC_C, DCC_D,
                 DEC_K, DEC_L, DCC_I, DCC_J, DCC_K, DCC_L, RET, IRET, PUSHA, POPA, HLT:
                break;
            
            // 8 bit immediate only
            case MOVS_A_I8, MOVS_B_I8, MOVS_C_I8, MOVS_D_I8,
                 ADD_A_I8, ADD_B_I8, ADD_C_I8, ADD_D_I8, ADD_I_I8, ADD_J_I8, ADD_K_I8, ADD_L_I8,
                 SUB_A_I8, SUB_B_I8, SUB_C_I8, SUB_D_I8, SUB_I_I8, SUB_J_I8, SUB_K_I8, SUB_L_I8,
                 ADD_SP_I8, ADD_BP_I8, SUB_SP_I8, SUB_BP_I8,
                 JMP_I8, CALL_I8, INT_I8,
                 JC_I8, JNC_I8, JS_I8, JNS_I8, JO_I8, JNO_I8, JZ_I8,
                 JNZ_I8, JA_I8, JBE_I8, JG_I8, JGE_I8, JL_I8, JLE_I8:
                this.cachedImmediateOffset = 1;
                data.addAll(getImmediateData(this.source.getImmediate(), 1));
                break;
            
            // 16 bit immediate only
            case MOV_I_I16, MOV_J_I16, MOV_K_I16, MOV_L_I16, MOV_A_I16, MOV_B_I16, MOV_C_I16, MOV_D_I16,
                 JMP_I16, CALL_I16:
                this.cachedImmediateOffset = 1;
                data.addAll(getImmediateData(this.source.getImmediate(), 2));
                break;
            
            // 32 bit immediate only
            case PUSHW_I32, BPUSHW_I32, JMP_I32, JMPA_I32, CALL_I32, CALLA_I32:
                this.cachedImmediateOffset = 1;
                data.addAll(getImmediateData(this.source.getImmediate(), 4));
                break;
            
            // 32 bit address
            case MOV_A_O, MOV_B_O, MOV_C_O, MOV_D_O:
                this.cachedAddressOffset = 1;
                data.addAll(getImmediateData(this.source.getMemory().getOffset(), 4));
                break;
            
            case MOV_O_A, MOV_O_B, MOV_O_C, MOV_O_D:
                this.cachedAddressOffset = 1;
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
            case ADD_RIM_I8, ADC_RIM_I8, SUB_RIM_I8, SBB_RIM_I8, CMP_RIM_I8,
                 SHL_RIM_I8, SHR_RIM_I8, SAR_RIM_I8, ROL_RIM_I8, ROR_RIM_I8, RCL_RIM_I8, RCR_RIM_I8:
                data.addAll(getRIMData(true, false, false, false, false));
                this.cachedImmediateOffset = data.size();
                data.addAll(getImmediateData(this.hasEI8 ? new ResolvableConstant(this.ei8) : this.source.getImmediate(), 1));
                break;
            
            // RIM + 8 bit with source
            case CMOVCC_RIM:
                data.addAll(getRIMData(true, true, false, false, false));
                data.addAll(getImmediateData(this.hasEI8 ? new ResolvableConstant(this.ei8) : this.source.getImmediate(), 1));
                break;
            
            case PCMOVCC_RIMP:
                data.addAll(getRIMData(true, true, true, false, false));
                data.addAll(getImmediateData(this.hasEI8 ? new ResolvableConstant(this.ei8) : this.source.getImmediate(), 1));
                break;
                 
            // packed
            case PADD_RIMP, PADC_RIMP, PSUB_RIMP, PSBB_RIMP, PMUL_RIMP, PDIV_RIMP, PDIVS_RIMP, PCMP_RIMP, PTST_RIMP:
                data.addAll(getRIMData(true, true, true, false, false));
                break;
            
            case PMULH_RIMP, PMULSH_RIMP, PDIVM_RIMP, PDIVMS_RIMP:
                data.addAll(getRIMData(true, true, true, true, false));
                break;
            
            // source only
            case PUSH_RIM, BPUSH_RIM, AND_F_RIM, OR_F_RIM, XOR_F_RIM, MOV_F_RIM, JMP_RIM, CALL_RIM,
                 INT_RIM, JC_RIM, JNC_RIM, JS_RIM, JNS_RIM, JO_RIM, JNO_RIM, JZ_RIM, JNZ_RIM, JA_RIM,
                 JBE_RIM, JG_RIM, JGE_RIM, JL_RIM, JLE_RIM:
                data.addAll(getRIMData(false, true, false, false, false));
                break;
            
            case JMPA_RIM32, CALLA_RIM32, PUSHW_RIM, BPUSHW_RIM, MOV_PR_RIM:
                data.addAll(getRIMData(false, true, false, false, true));
                break;
            
            // destination only
            case POP_RIM, BPOP_RIM, AND_RIM_F, OR_RIM_F, XOR_RIM_F, MOV_RIM_F, INC_RIM, ICC_RIM,
                 DEC_RIM, DCC_RIM, NOT_RIM, NEG_RIM, CMP_RIM_0:
                data.addAll(getRIMData(true, false, false, false, false));
                break;
            
            case POPW_RIM, BPOPW_RIM, MOV_RIM_PR:
                data.addAll(getRIMData(true, false, false, true, false));
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
            case XCHGW_RIM:
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
                    throw new IllegalArgumentException("Invalid RIM destination: " + this.destination + " in " + this);
            }
        } else if(includeSource) {
            if(sourceType == LocationType.NULL) throw new IllegalArgumentException("Invalid RIM source: " + this.source + " in " + this);
        }
        
        // determine operand sizes
        int sourceSize = includeSource ? this.source.getSize() : -1,
            destSize = includeDestination ? this.destination.getSize() : -1;
        
        // unify 'not specified' values
        if(sourceSize == 0) sourceSize = -1;
        if(destSize == 0) destSize = -1;
        
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
                
                case MOV_PR_RIM:
                    sourceSize = this.destination.getSize();
                    break;
                
                case MOV_RIM_PR:
                    destSize = this.source.getSize();
                    break;
                
                default:
                    // if we've just an immediate, assume 16 bit. inferring properly breaks unoptimized asm
                    if(includeSource && sourceType == LocationType.IMMEDIATE) {
                        sourceSize = 2; 
                    } else {
                        throw new IllegalArgumentException("Cannot infer operand sizes: " + this + " " + this.source.getRegister().size() + " " + this.source.getSize());
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
        
        // correct inferred immediate size
        if(includeDestination && includeSource && sourceType == LocationType.IMMEDIATE) {
            if(wideDestination && !wideSource) sourceSize = destSize / 2;
            else sourceSize = destSize;
        }
        
        // size bit
        if(packed) {
            // packed size is set explicitly during parsing
            if(includeSource) {
                if(this.source.getSize() == 1) rim |= 0b10_000_000;
            } else {
                if(this.destination.getSize() == 1) rim |= 0b10_000_000;
            }
        } else {
            // this turns out simpler than i thought lol
            if(includeSource) {
                if(sourceSize == 1 || (wideSource && sourceSize == 2)) rim |= 0b10_000_000;
            } else {
                if(destSize == 1) rim |= 0b10_000_000;
            }
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
        if(includeDestination) {
            if(destType == LocationType.REGISTER) {
                rim |= switch(this.destination.getRegister()) {
                    case AL, A, DA      -> 0b00_000_000;
                    case BL, B, AB      -> 0b00_001_000;
                    case CL, C, BC      -> 0b00_010_000;
                    case DL, D, CD      -> 0b00_011_000;
                    case AH, I, JI      -> 0b00_100_000;
                    case BH, J, LK      -> 0b00_101_000;
                    case CH, K, BP      -> 0b00_110_000;
                    case DH, L, SP      -> 0b00_111_000;
                    case PF, ISP        -> 0b00_000_000;
                    case NONE, F        -> 0;
                    default             -> throw new IllegalArgumentException("Invalid destination register " + this.destination.getRegister());
                };
            } else if(includeSource && sourceType == LocationType.REGISTER) {
                rim |= switch(this.source.getRegister()) {
                    case AL, A, DA      -> 0b00_000_000;
                    case BL, B, AB      -> 0b00_001_000;
                    case CL, C, BC      -> 0b00_010_000;
                    case DL, D, CD      -> 0b00_011_000;
                    case AH, I, JI      -> 0b00_100_000;
                    case BH, J, LK      -> 0b00_101_000;
                    case CH, K, BP      -> 0b00_110_000;
                    case DH, L, SP      -> 0b00_111_000;
                    case PF, ISP        -> 0b00_000_000;
                    case NONE, F        -> 0;
                    default             -> throw new IllegalArgumentException("Invalid source register " + this.source.getRegister());
                };
            }
        }
        
        // rim
        if((rim & 0b01_000_000) == 0) { // register-register
            rim |= switch(this.source.getRegister()) {
                case AL, A, DA      -> 0b00_000_000;
                case BL, B, AB      -> 0b00_000_001;
                case CL, C, BC      -> 0b00_000_010;
                case DL, D, CD      -> 0b00_000_011;
                case AH, I, JI      -> 0b00_000_100;
                case BH, J, LK      -> 0b00_000_101;
                case CH, K, BP      -> 0b00_000_110;
                case DH, L, SP      -> 0b00_000_111;
                case NONE, F, PF    -> 0;
                default             -> throw new IllegalArgumentException("Invalid source register " + this.source.getRegister());
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
                this.cachedAddressOffset = 1; // will be incremented
            }
        }
        
        data.add(rim);
        if(bioData != null) data.addAll(bioData);
        
        if(this.cachedAddressOffset != -1) this.cachedAddressOffset++; // presumably set by BIO, factor RIM byte
        
        // immediate
        if(includeSource && sourceType == LocationType.IMMEDIATE) {
            this.cachedImmediateOffset = data.size() + 1;
            data.addAll(getImmediateData(this.source.getImmediate(), sourceSize));
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
        
        if(this.immediateWidth == 0) {
            if(rm.getOffset().isResolved() && rm.getOffset().value() != 0) {
                throw new IllegalArgumentException("Attempted to encode non-zero offset with forced width zero");
            }
            
            includeOffset = false;
            this.cachedAddressOffset = 2; // issues
        }
        
        boolean hasIndex = rm.getIndex() != Register.NONE,
                ipRelative = rm.getBase() == Register.IP;
        
        // check
        if(!hasIndex && rm.getBase() == Register.NONE) throw new IllegalArgumentException("Must have at least one register for BIO: " + rm); 
        
        // base
        if(!ipRelative) {
            bio |= switch(rm.getBase()) {
                case DA         -> 0b000_000;
                case AB         -> 0b001_000;
                case BC         -> 0b010_000;
                case CD         -> 0b011_000;
                case JI         -> 0b100_000;
                case LK         -> 0b101_000;
                case BP         -> 0b110_000;
                case SP, NONE   -> 0b111_000;
                default         -> throw new IllegalArgumentException("Invalid base: " + rm.getBase());
            };
        } else {
            // ip relative index
            bio |= switch(rm.getIndex()) {
                case I      -> 0b100_100;
                case J      -> 0b101_100;
                case K      -> 0b110_100;
                case L      -> 0b111_100;
                case NONE   -> 0b000_100;
                default     -> throw new IllegalArgumentException("Invalid IP-relative index: " + rm.getIndex());
            };
            
            // IP relative can't do scale
            if(rm.getScale() != 1) throw new IllegalArgumentException("Invalid IP-relative scale: " + rm.getScale());
        }
        
        int offsetSize = 4;
        
        // scale
        if(hasIndex && !ipRelative) { // scale scales the index
            bio |= switch(rm.getScale()) {
                case 1  -> 0b01_000_000;
                case 2  -> 0b10_000_000;
                case 4  -> 0b11_000_000;
                default -> throw new IllegalArgumentException("Invalid scale: " + rm.getScale());
            };
            
            bio |= switch(rm.getIndex()) {
                case A  -> 0b000;
                case B  -> 0b001;
                case C  -> 0b010;
                case D  -> 0b011;
                case I  -> 0b100;
                case J  -> 0b101;
                case K  -> 0b110;
                case L  -> 0b111;
                default -> throw new IllegalArgumentException("Invalid index: " + rm.getIndex());
            };
        } else if(includeOffset) {
            if(rm.getOffset().isResolved()) offsetSize = getValueWidth(rm.getOffset().value(), true, true);
            else if(this.immediateWidth != -1) offsetSize = this.immediateWidth;
            
            bio |= (offsetSize - 1) & 0b11;
        }
        
        data.add(bio);
        
        // offset
        if(includeOffset) {
            this.cachedAddressOffset = 2;
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
            
            default:
                throw new IllegalArgumentException("Invalid immediate length: " + size + " in " + this);
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
        // validate overrides
        if(this.immediateWidth != -1) {
            if((!three && this.immediateWidth == 3) || (!four && this.immediateWidth == 4)) {
                throw new IllegalArgumentException("invalid immediate width override: " + this.immediateWidth + " in " + this);
            }
            
            return this.immediateWidth;
        }
        
        // determine
        if((v & 0x0000_0000_FFFF_FF80l) == 0l || (v & 0x0000_0000_FFFF_FF80l) == 0x0000_0000_FFFF_FF80l) { // 1 byte
            return 1;
        } else if((v & 0x0000_0000_FFFF_8000l) == 0l || (v & 0x0000_0000_FFFF_8000l) == 0x0000_0000_FFFF_8000l) { // 2 bytes
            return 2;
        } else if((v & 0x0000_0000_FF80_0000l) == 0l || (v & 0x0000_0000_FF80_0000l) == 0x0000_0000_FF80_0000l) { // 3 bytes
            if(three) return 3;
            if(four) return 4;
            
            throw new IllegalArgumentException("value too large for 2 bytes: " + v);
        } else {
            if(four) return 4;
            
            throw new IllegalArgumentException("value too large for 2 bytes: " + v);
        }
    }
    
    /**
     * @return True if the operands are (likely to be) valid
     */
    public boolean hasValidOperands() {
        if(this.source.getType() == LocationType.REGISTER && this.source.getSize() == 4) {
            switch(this.op) {
                // opcodes that allow wide sources
                case MOVW_RIM, XCHGW_RIM, CALLA_RIM32, JMPA_RIM32, PUSH_BP, BPUSH_SP, PUSHW_RIM, BPUSHW_RIM:
                    break;
                
                default:
                    return false;
            }
        }
        
        if(this.destination.getType() == LocationType.REGISTER && this.destination.getSize() == 4) {
            switch(this.op) {
                // opcodes that allow wide destiantions
                case MOVW_RIM, MOVS_RIM, MOVZ_RIM, XCHGW_RIM, LEA_RIM,
                     POP_BP, BPOP_SP, POPW_RIM, BPOPW_RIM,
                     ADD_SP_I8, SUB_SP_I8, ADD_BP_I8, SUB_BP_I8,
                     MULH_RIM, MULSH_RIM, PMULH_RIMP, PMULSH_RIMP,
                     DIVM_RIM, DIVMS_RIM, PDIVM_RIMP, PDIVMS_RIMP:
                    break;
                
                default:
                    return false;
            }
        }
        
        if(this.op == Opcode.LEA_RIM) {
            if(this.source.getType() != LocationType.MEMORY ||
               this.destination.getType() != LocationType.REGISTER ||
               this.destination.getSize() != 4) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * @return The location of the immediate value relative to the start of the instruction. Will be at least 1
     */
    public int getImmediateOffset() {
        if(this.cachedImmediateOffset != -1) return this.cachedImmediateOffset;
        
        getObjectCode();
        
        if(this.cachedImmediateOffset == -1) throw new IllegalStateException("Immediate offset not set " + this);
        
        return this.cachedImmediateOffset;
    }
    
    /**
     * @return The location of the immediate address relative to the start of the instruction. Will be at least 1
     */
    public int getAddressOffset() {
        if(this.cachedAddressOffset != -1) return this.cachedAddressOffset;
        
        getObjectCode();
        
        if(this.cachedAddressOffset == -1) throw new IllegalStateException("Address offset not set " + this);
        
        return this.cachedAddressOffset;
    }
    
    @Override
    public boolean isResolved() {
        return this.source.isResolved() && this.destination.isResolved();
    }
    
    @Override
    public String toString() {
        String src = this.source.toString(),
               dst = this.destination.toString(),
               str = this.op.toString();
        
        if(this.op == Opcode.CMOVCC_RIM || this.op == Opcode.PCMOVCC_RIMP) {
            str = this.op == Opcode.CMOVCC_RIM ? "CMOV" : "PCMOV";
            
            str += Opcode.fromOp((byte) this.ei8).toString().substring(1);
        }
        
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
    
    public void setOpcode(Opcode o) { this.op = o; }
    
    public ResolvableLocationDescriptor getSourceDescriptor() { return this.source; }
    public ResolvableLocationDescriptor getDestinationDescriptor() { return this.destination; }
    public int getImmediateWidth() { return this.immediateWidth; }
    public boolean hasFixedSize() { return this.hasFixedSize; }
    public Opcode getOpcode() { return this.op; }
}
