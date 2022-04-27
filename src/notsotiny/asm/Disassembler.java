package notsotiny.asm;

import java.util.HashMap;
import java.util.Map;

import notsotiny.sim.Opcode;

/**
 * Disassembles machine code
 * 
 * @author Mechafinch
 */
public class Disassembler {
    
    private int lastInstructionLength = 0;
    
    private boolean uppercase,
                    lastIsRIM,      // we can piggyback off previous work with these
                    lastIsI8,
                    lastIsI16,
                    lastIsI32;
    
    private Map<Integer, String> labels;
    
    /**
     * Creates a disassembler
     * 
     * @param uppercase
     * @param labels
     */
    public Disassembler(boolean uppercase, Map<Integer, String> labels) {
        this.uppercase = uppercase;
        this.labels = labels;
    }
    
    /**
     * Creates a disassembler with no labels 
     * 
     * @param uppercase
     */
    public Disassembler(boolean uppercase) {
        this.uppercase = uppercase;
        
        this.labels = new HashMap<>();
    }
    
    /**
     * Default disassembler
     */
    public Disassembler() {
        this.uppercase = true;
        this.labels = new HashMap<>();
    }
    
    /**
     * Disassembles the instruction at the given address
     * 
     * @param memory
     * @param address
     * @return
     */
    public String disassemble(byte[] memory, int address) {
        String s = disassembleInternal(memory, address);
        
        if(this.uppercase) {
            return s.toUpperCase();
        } else {
            return s.toLowerCase();
        }
    }
    
    /**
     * Actual implementation. Doesn't care about uppercase/lowercase
     * 
     * @param memory
     * @param address
     * @return
     */
    private String disassembleInternal(byte[] memory, int address) {
        Opcode op = Opcode.fromOp(memory[address]);
        
        // invalid instruction
        if(op == null) {
            return String.format("INVALID: %02X", memory[address]);
        }
        
        // get length and set flags
        this.lastInstructionLength = getInstructionLength(memory, address);
        
        // no arguments = ez
        if(this.lastInstructionLength == 1) {
            return disassembleNoArgs(op);
        } else if(this.lastIsRIM) {
            return disassembleRIM(memory, address, op);
        } else if(this.lastIsI8 || this.lastIsI16 || this.lastIsI32) {
            String s = op.toString().replaceFirst("_", " ").replace("_", ", ");
            s = s.substring(0, s.indexOf(",") + 1);
            
            if(this.lastIsI8) { 
                return String.format("%s %02X", s, memory[address + 1]);
            } else if(this.lastIsI16) { 
                return String.format("%s %04X", s, read16(memory, address + 1));
            } else {
                return String.format("%s %08X", s, read32(memory, address + 1));
            }
        } else {
            return disassembleSpecial(memory, address, op);
        }
    }
    
    /**
     * Disassembles special cases
     * 
     * @param memory
     * @param address
     * @param op
     * @return
     */
    private String disassembleSpecial(byte[] memory, int address, Opcode op) {
        return ""; // TODO
    }
    
    /**
     * Disassembles instructions with generic RIM arguments
     * 
     * @param memory
     * @param address
     * @param op
     * @return
     */
    private String disassembleRIM(byte[] memory, int address, Opcode op) {
        String mnemonic = getMnemonic(op),
               source = "",
               destination = "";
        
        byte rim = memory[address + 1];
        boolean size = (rim & 0x80) == 0;
        
        int srcCode = rim & 0x07,
            dstCode = (rim & 0x38) >> 3;
        
        // register source
        if((rim & 0x40) == 0 || (rim & 0x04) != 0) {
            int src = ((rim & 0x04) == 0) ? srcCode : dstCode;
            
            // register register
            source = switch(src) {
                case 0  -> size ? "A" : "AL";
                case 1  -> size ? "B" : "BL";
                case 2  -> size ? "C" : "CL";
                case 3  -> size ? "D" : "DL";
                case 4  -> size ? "I" : "AH";
                case 5  -> size ? "J" : "BH";
                case 6  -> size ? "BP" : "CH";
                case 7  -> size ? "SP" : "DH";
                default -> "INVALID";
            };
        } else if(srcCode == 0) { // immediate value source
            if(size) { // 16 bit
                // BP/SP
                if(dstCode > 5) { // 32
                    source = String.format("%08X", read32(memory, address + 2));
                } else { // 16
                    source = String.format("%04X", read16(memory, address + 2));
                }
            } else { // 8 bit
                source = String.format("%02X", memory[address + 2]);
            }
        } else if(srcCode == 1) { // immediate address source
            source = String.format("[%08X]", read32(memory, address + 2));
        } else { // bio
            source = disassembleBIO(memory, address, srcCode == 3);
        }
        
        // register destination
        if((rim & 0x40) == 0 || (rim & 0x04) == 0) {
            destination = switch((rim & 0x38) >> 3) {
                case 0  -> size ? "A" : "AL";
                case 1  -> size ? "B" : "BL";
                case 2  -> size ? "C" : "CL";
                case 3  -> size ? "D" : "DL";
                case 4  -> size ? "I" : "AH";
                case 5  -> size ? "J" : "BH";
                case 6  -> size ? "BP" : "CH";
                case 7  -> size ? "SP" : "DH";
                default -> "INVALID";
            };
        } else if(srcCode == 4) { // immediate value destination, invalid
            destination = "INVALID";
        } else if(srcCode == 5) { // immediate address destination
            destination = String.format("[%08X]", read32(memory, address + 2));
        } else { // bio
            destination = disassembleBIO(memory, address, srcCode == 7);
        }
        
        return mnemonic + " " + destination + ", " + source;
    }
    
    /**
     * Disassembles BIO
     * 
     * @param memory
     * @param address
     * @return
     */
    private String disassembleBIO(byte[] memory, int address, boolean hasOffset) {
        String source = "[";
        
        byte bio = memory[address + 2];
        
        int base = (bio & 0x38) >> 3,
            index = bio & 0x07,
            scale = bio >> 6;
            
        boolean hasIndex = index != 7,
                hasBase = (base != 7) || !hasIndex;
        
        if(hasBase) {
            source += switch(base){
                case 0  -> "D:A";
                case 1  -> "A:B";
                case 2  -> "B:C";
                case 3  -> "C:D";
                case 4  -> "J:I";
                case 5  -> "I:J";
                case 6  -> "BP";
                case 7  -> "SP";
                default -> "INVALID";
            };
            
            if(hasIndex) source += " + ";
        }
        
        if(hasIndex) {
            if(scale != 0) source += "(";
            
            source += switch(index) {
                case 0  -> "A";
                case 1  -> "B";
                case 2  -> "C";
                case 3  -> "D";
                case 4  -> "I";
                case 5  -> "J";
                case 6  -> "BP";
                default -> "INVALID";
            };
            
            if(scale != 0) source += " * " + (1 << scale) + ")";
        }
        
        if(hasOffset) { // has offset
            if(hasIndex || scale == 3) { // 4 byte
                source = String.format("%s + %08X", source, read32(memory, address + 3));
            } else if(scale == 2) { // 3 byte
                source = String.format("%s + %06X", source, read24(memory, address + 3));
            } else if(scale == 1) { // 2 byte
                source = String.format("%s + %04X", source, read16(memory, address + 3));
            } else { // 1 byte
                source = String.format("%s + %02X", source, memory[address + 3]);
            }
        }
        
        return source + "]";
    }
    
    /**
     * Disassembles instructions with no arguments.
     * A giant switch, really
     * 
     * @param op
     * @return
     */
    private String disassembleNoArgs(Opcode op) {
        return switch(op) {
            case NOP, RET, IRET -> op.toString();
            
            default -> op.toString().replaceFirst("_", " ").replace("_", ", ");
        };
    }
    
    /**
     * Gets the mnemonic of an Opcode
     * 
     * @param op
     * @return
     */
    private String getMnemonic(Opcode op) {
        String s = op.toString();
        return s.substring(0, s.indexOf("_"));
    }
    
    /**
     * Reads 4 bytes from the address
     * 
     * @param address
     * @return
     */
    private int read32(byte[] memory, int address) {
        return memory[address] | (memory[address + 1] << 8) | (memory[address + 2] << 16) | (memory[address + 3] << 24);
    }
    
    /**
     * Reads 3 bytes from the address
     * 
     * @param address
     * @return
     */
    private int read24(byte[] memory, int address) {
        return memory[address] | (memory[address + 1] << 8) | (memory[address + 2] << 16);
    }
    
    /**
     * Reads 2 bytes from the address
     * 
     * @param address
     * @return
     */
    private short read16(byte[] memory, int address) {
        return (short)(memory[address] | (memory[address + 1] << 8));
    }
    
    /**
     * Returns the length in bytes of the instruction at the given address
     * 
     * @param address
     * @return
     */
    public int getInstructionLength(byte[] memory, int address) {
        Opcode op = Opcode.fromOp(memory[address]);
        
        // invalid instruction?
        if(op == null) {
            return 1;
        }
        
        // copied from NotSoTinySimulator's IP updater
        // big ole switch
        switch(op) {
            // no arguments, do nothing
            case NOP:
            case XCHG_AH_AL:    case XCHG_BH_BL:    case XCHG_CH_CL:    case XCHG_DH_DL:
            case MOV_A_B:       case MOV_A_C:       case MOV_A_D:       case MOV_B_A:       case MOV_B_C:
            case MOV_B_D:       case MOV_C_A:       case MOV_C_B:       case MOV_C_D:       case MOV_D_A:
            case MOV_D_B:       case MOV_D_C:       case MOV_AL_BL:     case MOV_AL_CL:     case MOV_AL_DL:
            case MOV_BL_AL:     case MOV_BL_CL:     case MOV_BL_DL:     case MOV_CL_AL:     case MOV_CL_BL:
            case MOV_CL_DL:     case MOV_DL_AL:     case MOV_DL_BL:     case MOV_DL_CL:
            case XCHG_A_B:      case XCHG_A_C:      case XCHG_A_D:      case XCHG_B_C:      case XCHG_B_D:
            case XCHG_C_D:      case XCHG_AL_BL:    case XCHG_AL_CL:    case XCHG_AL_DL:    case XCHG_BL_CL:
            case XCHG_BL_DL:    case XCHG_CL_DL:
            case PUSH_A:        case PUSH_B:        case PUSH_C:        case PUSH_D:        case PUSH_I:
            case PUSH_J:        case PUSH_BP:       case PUSH_SP:       case PUSH_F:
            case POP_A:         case POP_B:         case POP_C:         case POP_D:         case POP_I:
            case POP_J:         case POP_BP:        case POP_SP:        case POP_F:
            case NOT_F:
            case INC_I:         case INC_J:         case ICC_I:         case ICC_J:         case DEC_I:
            case DEC_J:         case DCC_I:         case DCC_J:
            case ADD_A_A:       case ADD_A_B:       case ADD_A_C:       case ADD_A_D:       case ADD_B_A:
            case ADD_B_B:       case ADD_B_C:       case ADD_B_D:       case ADD_C_A:       case ADD_C_B:
            case ADD_C_C:       case ADD_C_D:       case ADD_D_A:       case ADD_D_B:       case ADD_D_C:
            case ADD_D_D:
            case SUB_A_A:       case SUB_A_B:       case SUB_A_C:       case SUB_A_D:       case SUB_B_A:
            case SUB_B_B:       case SUB_B_C:       case SUB_B_D:       case SUB_C_A:       case SUB_C_B:
            case SUB_C_C:       case SUB_C_D:       case SUB_D_A:       case SUB_D_B:       case SUB_D_C:
            case SUB_D_D:
            case RET:           case IRET:
                lastIsRIM = false;
                lastIsI8 = false;
                lastIsI16 = false;
                lastIsI32 = false;
                return 1;
            
            // 1 byte
            case MOV_A_I8:      case MOV_B_I8:      case MOV_C_I8:      case MOV_D_I8:
            case ADD_A_I8:      case ADD_B_I8:      case ADD_C_I8:      case ADD_D_I8:      case ADC_A_I8:
            case ADC_B_I8:      case ADC_C_I8:      case ADC_D_I8:
            case SUB_A_I8:      case SUB_B_I8:      case SUB_C_I8:      case SUB_D_I8:      case SBB_A_I8:
            case SBB_B_I8:      case SBB_C_I8:      case SBB_D_I8:
            case JMP_I8:        case JC_I8:         case JNC_I8:        case JS_I8:         case JNS_I8:
            case JO_I8:         case JNO_I8:        case JZ_I8:         case JNZ_I8:        case JA_I8:
            case JAE_I8:        case JB_I8:         case JBE_I8:        case JG_I8:         case JGE_I8:
            case JL_I8:         case JLE_I8:
                lastIsRIM = false;
                lastIsI8 = true;
                lastIsI16 = false;
                lastIsI32 = false;
                return 2;
            
            // 2 bytes
            case MOV_I_I16:     case MOV_J_I16:     case MOV_A_I16:     case MOV_B_I16:     case MOV_C_I16:
            case MOV_D_I16:
            case ADD_A_I16:     case ADD_B_I16:     case ADD_C_I16:     case ADD_D_I16:     case ADC_A_I16:
            case ADC_B_I16:     case ADC_C_I16:     case ADC_D_I16:
            case SUB_A_I16:     case SUB_B_I16:     case SUB_C_I16:     case SUB_D_I16:     case SBB_A_I16:
            case SBB_B_I16:     case SBB_C_I16:     case SBB_D_I16:
            case JMP_I16:       case CALL_I16:      case INT_I16:
                lastIsRIM = false;
                lastIsI8 = false;
                lastIsI16 = true;
                lastIsI32 = false;
                return 3;
            
            // 4 bytes
            case MOV_BP_I32:    case MOV_SP_I32:
            case JMP_I32:       case JMPA_I32:      case CALLA_I32:
                lastIsRIM = false;
                lastIsI8 = false;
                lastIsI16 = false;
                lastIsI32 = true;
                return 4;
            
            // special cases
            case PADD_RIMP:     case PADC_RIMP:     case PSUB_RIMP:     case PSBB_RIMP:
            case PINC_RIMP:     case PICC_RIMP:     case PDEC_RIMP:     case PDCC_RIMP:
            case MULH_RIM:      case MULSH_RIM:     case PMUL_RIMP:     case PMULS_RIMP:
            case PMULH_RIMP:    case PMULSH_RIMP:
            case DIVM_RIM:      case DIVMS_RIM:     case PDIV_RIMP:     case PDIVS_RIMP:
            case PDIVM_RIMP:    case PDIVMS_RIMP:
            case CMP_RIM_I8:
                lastIsRIM = false;
                lastIsI8 = false;
                lastIsI16 = false;
                lastIsI32 = false;
                return getInstructionLengthSpecialCases(memory, address, op);
            
            // RIM
            default:
                lastIsRIM = true;
                lastIsI8 = false;
                lastIsI16 = false;
                lastIsI32 = false;
        }
        
        // General RIM
        byte rim = memory[address + 1];
        
        // register register? 1 byte
        if((rim & 0x40) == 0) {
            return 2;
        } else {
            // what are we dealing with
            int diff = switch(rim & 0x07) {
                case 0, 4   -> ((rim & 0x80) == 0) ? 3 : 2; // immediate value
                case 1, 5   -> 5; // immediate address
                case 2, 6   -> 2; // bio
                case 3, 7   -> {  // bio + offset
                    // if index != 111, offset is 4 bytes. Otherwise offset is scale + 1 bytes
                    byte bio = memory[address + 2];
                    
                    if((bio & 0x07) == 0x07) {
                        yield (bio >> 6) + 3;
                    } else {
                        yield 6;
                    }
                }
                
                default     -> 0; // not possible
            };
            
            return diff + 1;
        }
    }
    
    /**
     * Special cases for getInstructionLength
     * 
     * @param memory
     * @param address
     * @param op
     * @return
     */
    private int getInstructionLengthSpecialCases(byte[] memory, int address, Opcode op) {
        return -1; // TODO
    }
    
    /**
     * Gets the length in bytes of the most recently disassembled instruction
     * 
     * @return
     */
    public int getLastInstructionLength() {
        return this.lastInstructionLength;
    }
    
    public void setCase(boolean upper) { this.uppercase = upper; }
    
    public void addLabel(int address, String name) { 
        this.labels.put(address, name);
    }
}
