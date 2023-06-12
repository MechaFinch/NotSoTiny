package notsotiny.asm;

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
    
    /**
     * Creates a disassembler
     * 
     * @param uppercase
     */
    public Disassembler(boolean uppercase) {
        this.uppercase = uppercase;
    }
    
    /**
     * Default disassembler
     */
    public Disassembler() {
        this.uppercase = true;
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
        
        String s = getMnemonic(op),
               os = op.toString();
        
        switch(op) {
            // no args, just mnemonic
            case NOP, RET, IRET, PUSHA, POPA, HLT:
                break;
            
            // register only shortcuts - convert underscores
            case MOV_A_B, MOV_A_C, MOV_A_D, MOV_B_A, MOV_B_C, MOV_B_D, MOV_C_A, MOV_C_B, MOV_C_D,
                 MOV_D_A, MOV_D_B, MOV_D_C, MOV_AL_BL, MOV_AL_CL, MOV_AL_DL, MOV_BL_AL, MOV_BL_CL,
                 MOV_BL_DL, MOV_CL_AL, MOV_CL_BL, MOV_CL_DL, MOV_DL_AL, MOV_DL_BL, MOV_DL_CL, 
                 PUSH_A, PUSH_B, PUSH_C, PUSH_D, PUSH_I, PUSH_J, PUSH_K, PUSH_L, PUSH_BP, PUSH_F,
                 PUSH_PF, POP_A, POP_B, POP_C, POP_D, POP_I, POP_J, POP_K, POP_L, POP_BP, POP_F,
                 POP_PF, NOT_F, INC_I, INC_J, INC_K, INC_L, ICC_I, ICC_J, ICC_K, ICC_L, DEC_I, DEC_J,
                 DEC_K, DEC_L, DCC_I, DCC_J, DCC_K, DCC_L:
                s += " " + os.substring(os.indexOf('_') + 1);
                s = s.replace("_", ", ");
                break;
            
            // 8 bit immediates
            case MOVS_A_I8, MOVS_B_I8, MOVS_C_I8, MOVS_D_I8, ADD_A_I8, ADD_B_I8, ADD_C_I8, ADD_D_I8, ADC_A_I8,
                 ADC_B_I8, ADC_C_I8, ADC_D_I8, SUB_A_I8, SUB_B_I8, SUB_C_I8, SUB_D_I8, SBB_A_I8, SBB_B_I8,
                 SBB_C_I8, SBB_D_I8, ADD_SP_I8, SUB_SP_I8, JMP_I8, CALL_I8, INT_I8, JC_I8, JNC_I8, JS_I8, JNS_I8,
                 JO_I8, JNO_I8, JZ_I8, JNZ_I8, JA_I8, JBE_I8, JG_I8, JGE_I8, JL_I8, JLE_I8:
                     s += disassembleImmediateShortcut(memory, op, 1);
                break;
            
            // 16 bit immediates
            case MOV_I_I16, MOV_J_I16, MOV_K_I16, MOV_L_I16, MOV_A_I16, MOV_B_I16, MOV_C_I16, MOV_D_I16,
                 ADD_A_I16, ADD_B_I16, ADD_C_I16, ADD_D_I16, ADC_A_I16, ADC_B_I16, ADC_C_I16, ADC_D_I16,
                 SUB_A_I16, SUB_B_I16, SUB_C_I16, SUB_D_I16, SBB_A_I16, SBB_B_I16, SBB_C_I16, SBB_D_I16,
                 JMP_I16, CALL_I16:
                s += disassembleImmediateShortcut(memory, op, 2);
                break;
            
            // 32 bit immediate
            case MOV_SP_I32, MOV_BP_I32, PUSH_I32, JMP_I32, JMPA_I32, CALL_I32, CALLA_I32:
                s += disassembleImmediateShortcut(memory, op, 4);
                break;
            
            // immediate address
            case MOV_A_O, MOV_B_O, MOV_C_O, MOV_D_O:
                s += " " + os.charAt(4) + ", [" + readHex(memory, 4) + "]";
                break;
            
            case MOV_O_A, MOV_O_B, MOV_O_C, MOV_O_D:
                s += " [" + readHex(memory, 4) + "], " + os.charAt(6);
                break;
            
            // BIO
            case MOV_A_BI, MOV_B_BI, MOV_C_BI, MOV_D_BI, MOV_A_BIO, MOV_B_BIO, MOV_C_BIO, MOV_D_BIO:
                s += " " + os.charAt(4) + ", " + disassembleBIO(memory, os.contains("BIO"));
                break;
            
            case MOV_BI_A, MOV_BI_B, MOV_BI_C, MOV_BI_D, MOV_BIO_A, MOV_BIO_B, MOV_BIO_C, MOV_BIO_D:
                boolean offset = os.contains("BIO");
                s += " " + disassembleBIO(memory, offset) + ", " + os.charAt(offset ? 8 : 7);
                break;
            
            // RIM + I8
            case ADD_RIM_I8, ADC_RIM_I8, SUB_RIM_I8, SBB_RIM_I8, CMP_RIM_I8:
                s += disassembleRIM(memory, true, false, false, false, false) + ", " + readHex(memory, 1);
                break;
            
            // packed
            case PADD_RIMP, PADC_RIMP, PSUB_RIMP, PSBB_RIMP, PMUL_RIMP, PDIV_RIMP, PDIVS_RIMP:
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
            
            case MOV_PF_RIM:
                s += " PF," + disassembleRIM(memory, false, true, false, false, false);
                break;
            
            case JMPA_RIM32, CALLA_RIM32:
                s += disassembleRIM(memory, false, true, false, false, true);
                break;
            
            // destination only
            case POP_RIM, INC_RIM, ICC_RIM, DEC_RIM, DCC_RIM, NOT_RIM, NEG_RIM:
                s += disassembleRIM(memory, true, false, false, false, false);
                break;
            
            case AND_RIM_F, OR_RIM_F, XOR_RIM_F, MOV_RIM_F:
                s += disassembleRIM(memory, true, false, false, false, false) + ", F";
                break;
            
            case MOV_RIM_PF:
                s += disassembleRIM(memory, true, false, false, false, false) + ", PF";
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
        int i = switch(size) {
            case 1          -> memory.readByte(this.lastAddress);
            case 2          -> memory.read2Bytes(this.lastAddress);
            case 3          -> memory.read3Bytes(this.lastAddress);
            case default    -> memory.read4Bytes(this.lastAddress);
        };
        
        this.lastAddress += size;
        return i;
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
                    case default    -> Register.NONE;
                };
                
            case 4:
                return switch(field) {
                    case 0          -> Register.DA;
                    case 1          -> Register.AB;
                    case 2          -> Register.BC;
                    case 3          -> Register.CD;
                    case 4          -> Register.JI;
                    case 5          -> Register.LK;
                    case 6          -> Register.BP;
                    case 7          -> Register.SP; 
                    case default    -> Register.NONE;
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
                    case default    -> Register.NONE;
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
        String s = op.toString();
        
        if(s.contains("_")) {
            return s.substring(0, s.indexOf("_"));
        }
        
        return s;
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
