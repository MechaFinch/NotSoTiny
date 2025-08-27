package notsotiny.asm;

import java.util.HashMap;
import java.util.Map;

import notsotiny.sim.Register;
import notsotiny.sim.memory.MemoryManager;
import notsotiny.sim.ops.Opcode;

/**
 * Disassembles machine code
 * 
 * @author Mechafinch
 */
public class Disassembler {
    
    private long startAddress = 0,
                 lastAddress = 0;
    
    private boolean uppercase;
    
    public Map<Opcode, Integer> instructionStatisticsMap;

    /**
     * Creates a disassembler
     * 
     * @param uppercase
     */
    public Disassembler(boolean uppercase) {
        this.uppercase = uppercase;
        this.instructionStatisticsMap = new HashMap<>();
    }
    
    /**
     * Default disassembler
     */
    public Disassembler() {
        this(true);
    }
    
    /**
     * Disassembles the instruction at the given address
     * 
     * @param memory
     * @param address
     * @return
     */
    public String disassemble(MemoryManager memory, long address) {
        this.startAddress = address;
        this.lastAddress = address;
        
        Opcode op = Opcode.fromOp((byte) readSize(memory, 1));
        this.instructionStatisticsMap.put(op, this.instructionStatisticsMap.getOrDefault(op, 0) + 1);
        
        String s = getMnemonic(op),
               os = op.toString();
        
        switch(op) {
            // no args, just mnemonic
            case NOP, RET, IRET, PUSHA, POPA, HLT:
                break;
            
            // register only shortcuts - convert underscores
            case PUSH_F, PUSH_PF, POP_F, POP_PF, NOT_F, 
                 PUSH_A, PUSH_B, PUSH_C, PUSH_D, PUSH_I, PUSH_J, PUSH_K, PUSH_L,
                 PUSHW_DA, PUSHW_BC, PUSHW_JI, PUSHW_LK, PUSHW_XP, PUSHW_YP, PUSHW_BP,
                 POP_A, POP_B, POP_C, POP_D, POP_I, POP_J, POP_K, POP_L,
                 POPW_DA, POPW_BC, POPW_JI, POPW_LK, POPW_XP, POPW_YP, POPW_BP:
                s += " " + os.substring(os.indexOf('_') + 1);
                s = s.replace("_", ", ");
                break;
            
            // 8 bit immediates
            case MOVS_A_I8, MOVS_B_I8, MOVS_C_I8, MOVS_D_I8, MOVS_I_I8, MOVS_J_I8, MOVS_K_I8, MOVS_L_I8,
                 ADD_A_I8, ADD_B_I8, ADD_C_I8, ADD_D_I8, ADD_I_I8, ADD_J_I8, ADD_K_I8, ADD_L_I8,
                 ADDW_DA_I8, ADDW_BC_I8, ADDW_JI_I8, ADDW_LK_I8, ADDW_XP_I8, ADDW_YP_I8, ADDW_BP_I8, ADDW_SP_I8,
                 SUB_A_I8, SUB_B_I8, SUB_C_I8, SUB_D_I8, SUB_I_I8, SUB_J_I8, SUB_K_I8, SUB_L_I8,
                 SUBW_DA_I8, SUBW_BC_I8, SUBW_JI_I8, SUBW_LK_I8, SUBW_XP_I8, SUBW_YP_I8, SUBW_BP_I8, SUBW_SP_I8,
                 JMP_I8, JC_I8, JNC_I8, JS_I8, JNS_I8, JO_I8, JNO_I8, JZ_I8, JNZ_I8, JA_I8, JBE_I8, JL_I8, JLE_I8, JG_I8, JGE_I8:
                s += disassembleImmediateShortcut(memory, op, 1);
                break;
            
            // 8 bit immediate + EI8
            case JCC_I8:
                String imm = disassembleImmediateShortcut(memory, op, 1);
                int cmp = readSize(memory, 1);
                String cmps = getMnemonic(Opcode.fromOp((byte)(cmp | 0xF0)));
                s = s.substring(0, s.length() - 5) + cmps.substring(1) + " " + imm;
                break;
            
            // 16 bit immediates
            case MOV_A_I16, MOV_B_I16, MOV_C_I16, MOV_D_I16, MOV_I_I16, MOV_J_I16, MOV_K_I16, MOV_L_I16,
                 JMP_I16, CALL_I16:
                s += disassembleImmediateShortcut(memory, op, 2);
                break;
            
            // 32 bit immediate
            case JMP_I32, JMPA_I32, CALL_I32, CALLA_I32:
                s += disassembleImmediateShortcut(memory, op, 4);
                break;
            
            // RIM + I8
            case MOV_RIM_BP, CMP_RIM_I8, ADD_RIM_I8, ADC_RIM_I8, SUB_RIM_I8, SBB_RIM_I8,
                 SHL_RIM_I8, SHR_RIM_I8, SAR_RIM_I8, ROL_RIM_I8, ROR_RIM_I8, RCL_RIM_I8, RCR_RIM_I8:
                s += disassembleRIM(memory, true, false, false, false, false) + ", " + readHex(memory, 1);
                break;
            
            case CMOVCC_RIM:
            	s = s.substring(0, s.length() - 2);
            	String params = disassembleRIM(memory, true, true, false, false, false);
            	cmp = readSize(memory, 1);
            	cmps = getMnemonic(Opcode.fromOp((byte)(cmp | 0xF0)));
            	
            	s += cmps.substring(1) + " " + params;
            	break;
            
            case MOVW_RIM_BP, CMPW_RIM_I8, ADDW_RIM_I8, ADCW_RIM_I8, SUBW_RIM_I8, SBBW_RIM_I8:
                s += disassembleRIM(memory, true, false, false, true, false) + ", " + readHex(memory, 1);
                break;
            
            // packed
            case PADD_RIMP, PADC_RIMP, PSUB_RIMP, PSBB_RIMP, PMUL_RIMP, PDIV_RIMP, PDIVS_RIMP, PCMP_RIMP, PTST_RIMP,
                 PAND_RIMP, POR_RIMP, PXOR_RIMP:
                s += disassembleRIM(memory, true, true, true, false, false);
                break;
            
            case PMULH_RIMP, PMULSH_RIMP, PDIVM_RIMP, PDIVMS_RIMP:
                s += disassembleRIM(memory, true, true, true, true, false);
                break;
            
            // source only
            case PUSH_RIM, JMP_RIM, CALL_RIM, INT_RIM, JC_RIM, JNC_RIM, JS_RIM, JNS_RIM, JO_RIM,
                 JNO_RIM, JZ_RIM, JNZ_RIM, JA_RIM, JBE_RIM, JG_RIM, JGE_RIM, JL_RIM, JLE_RIM:
                s += disassembleRIM(memory, false, true, false, false, false);
                break;
            
            case AND_F_RIM, OR_F_RIM, XOR_F_RIM, MOV_F_RIM:
                s += " F," + disassembleRIM(memory, false, true, false, false, false);
                break;
            
            case MOV_PR_RIM:
                s += " PR," + disassembleRIM(memory, false, true, false, true, true);
                break;
            
            case JMPA_RIM32, CALLA_RIM32, PUSHW_RIM:
                s += disassembleRIM(memory, false, true, false, false, true);
                break;
            
            // destination only
            case POP_RIM, INC_RIM, ICC_RIM, DEC_RIM, DCC_RIM, NOT_RIM, NEG_RIM:
                s += disassembleRIM(memory, true, false, false, false, false);
                break;
            
            case POPW_RIM:
                s += disassembleRIM(memory, true, false, false, true, false);
                break;
            
            case AND_RIM_F, OR_RIM_F, XOR_RIM_F, MOV_RIM_F:
                s += disassembleRIM(memory, true, false, false, false, false) + ", F";
                break;
            
            case MOV_RIM_PR:
                s += disassembleRIM(memory, true, false, false, true, true) + ", PR";
                break;
            
            case PINC_RIMP, PICC_RIMP, PDEC_RIMP, PDCC_RIMP:
                s += disassembleRIM(memory, true, false, true, false, false);
                break;
            
            case CMP_RIM_0:
                s += disassembleRIM(memory, true, false, false, false, false) + ", 0";
                break;
            
            // wide destination
            case MOVS_RIM, MOVZ_RIM, MULH_RIM, MULSH_RIM, DIVM_RIM, DIVMS_RIM, LEA_RIM:
                s += disassembleRIM(memory, true, true, false, true, false);
                break;
                
            // wide
            case MOVW_RIM:
                s += disassembleRIM(memory, true, true, false, true, true);
                break;
                
            // normal
            default:
                s += disassembleRIM(memory, true, true, false, false, false);
                break;
        }
        
        if(this.uppercase) {
            return s.toUpperCase();
        } else {
            return s.toLowerCase();
        }
    }
    
    
    
    /**
     * Disassembles an immediate shortcut instruction, excluding the mnemonic
     * @param memory
     * @param op
     * @param size
     * @return
     */
    private String disassembleImmediateShortcut(MemoryManager memory, Opcode op, int size) {
        String s = " ",
               ops = op.toString(),
               args = ops.substring(ops.indexOf('_') + 1);
        
        // two underscores = register + I8
        if(args.contains("_")) {
            s += args.substring(0, args.indexOf('_')) + ", ";
        }
        
        s += readHex(memory, size);
        
        return s;
    }
    
    /**
     * Disassembles a RIM sequence
     * 
     * @param memory
     * @param address
     * @return
     */
    private String disassembleRIM(MemoryManager memory, boolean includeDestination, boolean includeSource, boolean packed, boolean wideDestination, boolean wideSource) {
        String s = " ";
        
        // byte
        byte rimByte = (byte) readSize(memory, 1);
        
        boolean small = packed ? false : (rimByte & 0x80) != 0,
                isIM = (rimByte & 0x40) != 0;
        
        int reg = (rimByte >> 3) & 0x07,
            rim = rimByte & 0x07;
        
        int sourceSize = packed ? 2 : (wideSource ? (small ? 2 : 4) : (small ? 1 : 2)),
            destSize = packed ? 2 : (wideDestination ? (small ? 2 : 4) : (small ? 1 : 2));
        
        // build
        if(isIM) {
            String sourceString = "",
                   destString = "";
            
            // rim is immediate/memory
            switch(rim) {
                case 0: // reg dest, immediate source
                    destString = convertRegister(reg, destSize).toString();
                    sourceString = readHex(memory, sourceSize);
                    break;
                    
                case 1: // reg dest, immediate addr source
                    destString = convertRegister(reg, destSize).toString();
                    sourceString = "[" + readHex(memory, 4) + "]";
                    break;
                    
                case 2: // BIO with no offset source
                    destString = convertRegister(reg, destSize).toString(); 
                    sourceString = disassembleBIO(memory, false);
                    break;
                    
                case 3: // BIO with offset source
                    destString = convertRegister(reg, destSize).toString(); 
                    sourceString = disassembleBIO(memory, true);
                    break;
                    
                case 4: // invalid
                    destString = "INVALID4";
                    sourceString = convertRegister(reg, sourceSize).toString();
                    break;
                    
                case 5: // immediate addr dest
                    destString = "[" + readHex(memory, 4) + "]";
                    sourceString = convertRegister(reg, sourceSize).toString();
                    break;
                    
                case 6: // BIO with no offset dest
                    destString = disassembleBIO(memory, false);
                    sourceString = convertRegister(reg, sourceSize).toString();
                    break;
                    
                case 7: // BIO with offset dest
                    destString = disassembleBIO(memory, true);
                    sourceString = convertRegister(reg, sourceSize).toString();
                    break;
            }
            
            // build
            if(includeDestination) {
                s += destString;
                
                if(includeSource) {
                    s += ", ";
                }
            }
            
            if(includeSource) {
                s += sourceString;
            }
            
            return s;
        } else {
            // rim is src reg
            if(includeDestination) {
                s += convertRegister(reg, destSize);
                
                if(includeSource) {
                    s += ", ";
                }
            }
            
            if(includeSource) {
                s += convertRegister(rim, sourceSize);
            }
            
            return s;
        }
    }
    
    /**
     * Disassembles a BIO sequence
     * 
     * @param memroy
     * @param includeOffset
     * @return
     */
    private String disassembleBIO(MemoryManager memory, boolean includeOffset) {
        String s = "[";
        
        // byte
        byte bio = (byte) readSize(memory, 1);
        
        // fields
        int scale = (bio >> 6) & 0x03,
            base = (bio >> 3) & 0x07,
            index = bio & 0x07;
        
        // determine values
        Register baseRegister = Register.NONE,
                 indexRegister = Register.NONE;
        
        int offset = 0,
            offsetSize = 0;
        
        if(scale == 0) {
            // no index or IP base, variable width offset
            offsetSize = (index & 0x03) + 1;
            
            if((index & 0x04) != 0) {
                // IP base, index from base field
                baseRegister = Register.IP;
                
                if((base & 0x04) == 1) {
                    indexRegister = convertRegister(base, 2);
                }
            } else {
                // normal base
                baseRegister = convertRegister(base, 4);
            }
            
            if(includeOffset) offset = readSize(memory, offsetSize);
        } else {
            // normal base/index, 4 byte offset        
            if(base == 7) {
                baseRegister = Register.NONE;
            } else {
                baseRegister = convertRegister(base, 4);
            }
            
            indexRegister = convertRegister(index, 2);
            
            if(includeOffset) {
                offset = readSize(memory, 4);
                offsetSize = 4;
            }
        }
        
        // build string in base-index-offset order
        if(baseRegister != Register.NONE) {
            s += baseRegister + " + ";
        }
        
        if(indexRegister != Register.NONE) {
            if(scale > 1) {
                s += (scale == 3 ? "4*" : "2*");
            }
            
            s += indexRegister + " + ";
        }
        
        if(includeOffset) {
            s += toHexString(offset, offsetSize);
        }
        
        if(s.endsWith(" + ")) {
            s = s.substring(0, s.length() - 3);
        }
        
        return s + "]";
    }
    
    /**
     * Convernience function for toHexString(readSize(memory, size), size);
     * 
     * @param memory
     * @param size
     * @return
     */
    private String readHex(MemoryManager memory, int size) {
        return toHexString(readSize(memory, size), size);
    }
    
    /**
     * Reads a value of the given size from memory
     * 
     * @param memory
     * @param address
     * @param size
     * @return
     */
    private int readSize(MemoryManager memory, int size) {
        try {
            int i = switch(size) {
                case 1          -> memory.readBytePrivileged(this.lastAddress);
                case 2          -> memory.read2BytesPrivileged(this.lastAddress);
                case 3          -> memory.read3BytesPrivileged(this.lastAddress);
                default    		-> memory.read4BytesPrivileged(this.lastAddress);
            };
            
            this.lastAddress += size;
            return i;
        } catch(IndexOutOfBoundsException e) {
            this.lastAddress += size;
            return 0;
        }
    }
    
    /**
     * Converts a normal register field (default 16 bit)
     * @param field
     * @return
     */
    private Register convertRegister(int field, int size) {
        switch(size) {
            case 1:
                return switch(field) {
                    case 0          -> Register.AL;
                    case 1          -> Register.BL;
                    case 2          -> Register.CL;
                    case 3          -> Register.DL;
                    case 4          -> Register.AH;
                    case 5          -> Register.BH;
                    case 6          -> Register.CH;
                    case 7          -> Register.DH;
                    default    		-> Register.NONE;
                };
                
            case 4:
                return switch(field) {
                    case 0          -> Register.DA;
                    case 1          -> Register.BC;
                    case 2          -> Register.JI;
                    case 3          -> Register.LK;
                    case 4          -> Register.XP;
                    case 5          -> Register.YP;
                    case 6          -> Register.BP;
                    case 7          -> Register.SP; 
                    default    		-> Register.NONE;
                };
            
            default:
                return switch(field) {
                    case 0          -> Register.A;
                    case 1          -> Register.B;
                    case 2          -> Register.C;
                    case 3          -> Register.D;
                    case 4          -> Register.I;
                    case 5          -> Register.J;
                    case 6          -> Register.K;
                    case 7          -> Register.L;
                    default    		-> Register.NONE;
                };
        }
    }
    
    /**
     * Converts an integer i of l bytes to a hex string
     * 
     * @param i
     * @param l
     * @return
     */
    private String toHexString(int i, int l) {
        return String.format("%08X", i).substring(8 - (l * 2));
    }
    
    /**
     * Gets the mnemonic of an Opcode
     * 
     * @param op
     * @return
     */
    private String getMnemonic(Opcode op) {
        try {
            String s = op.toString();
            
            if(s.contains("_")) {
                return s.substring(0, s.indexOf("_"));
            }
            
            return s;
        } catch(NullPointerException e) {
            return "INV";
        }
    }
    
    /**
     * Gets the length in bytes of the most recently disassembled instruction
     * 
     * @return
     */
    public int getLastInstructionLength() {
        return (int)(this.lastAddress - this.startAddress);
    }
    
    public void setCase(boolean upper) { this.uppercase = upper; }
}
