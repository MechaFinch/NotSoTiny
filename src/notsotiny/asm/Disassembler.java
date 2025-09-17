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
        
        String mnemonic = getMnemonic(op),
               args = "";
        
        // Main decoding
        switch(op.dgroup) {
            case NODECODE, UNDEF:
                break;
                
            case I8:
                args = disassembleImmediateShortcut(memory, op, 1);
                break;
                
            case I8_EI8:
                args = disassembleImmediateShortcut(memory, op, 1) + ", " + disassembleImmediateShortcut(memory, op, 1);
                break;
                
            case I16:
                args = disassembleImmediateShortcut(memory, op, 2);
                break;
                
            case I32:
                args = disassembleImmediateShortcut(memory, op, 4);
                break;
            
            default:
                // various RIMs
                args = disassembleRIM(memory, op.dgroup.hasDestination, op.dgroup.hasSource, op.dgroup.isPacked, op.dgroup.destIsWide, op.dgroup.sourceIsWide);
                if(op.dgroup.hasEI8) {
                    args += ", " + readHex(memory, 1);
                }
        }
        
        // Handle additional opcode-specific things
        switch(op) {
            case CMOVCC_RIM, CMOVWCC_RIM, PCMOVCC_RIMP, JCC_RIM, JCC_I8:
                // Get EI8 and remove from args string
                int condVal = Integer.parseInt(args.substring(args.length() - 2), 16);
                args = args.substring(0, args.length() - 4);
                
                // Put condition in mnemonic
                String baseCondition = switch(condVal & 0x0F) {
                    case 0x02   -> "C";
                    case 0x03   -> "NC";
                    case 0x04   -> "S";
                    case 0x05   -> "NS";
                    case 0x06   -> "O";
                    case 0x07   -> "NO";
                    case 0x08   -> "Z";
                    case 0x09   -> "NZ";
                    case 0x0A   -> "A";
                    case 0x0B   -> "BE";
                    case 0x0C   -> "G";
                    case 0x0D   -> "GE";
                    case 0x0E   -> "L";
                    case 0x0F   -> "LE";
                    default     -> "";
                };
                
                String conditionSuffix = switch(condVal & 0x70) {
                    case 0x40   -> ".A8";
                    case 0x50   -> ".E8";
                    case 0x60   -> ".A4";
                    case 0x70   -> ".E8";
                    default     -> "";
                };
                
                mnemonic = mnemonic.substring(0, mnemonic.length() - 2) + baseCondition + conditionSuffix;
                break;
                
            case MOV_RIM_BP, MOVW_RIM_BP:
                // EI8 is offset for [BP + ei8] source
                args = args.substring(0, args.length() - 4) + ", [BP + " + args.substring(args.length() - 2) + "]";
                break;
            
            case MOV_BP_RIM, MOVW_BP_RIM:
                // EI8 is offset for [BP + ei8] destination
                args = " [BP + " + args.substring(args.length() - 2) + "], " + args.substring(0, args.length() - 4); 
                break;
                
                // register-specific shortcuts not covered by immediate shortcuts                
            case PUSH_A, PUSH_B, PUSH_C, PUSH_D, PUSH_I, PUSH_J, PUSH_K, PUSH_L,
                 PUSHW_DA, PUSHW_BC, PUSHW_JI, PUSHW_LK, PUSHW_XP, PUSHW_YP, PUSHW_BP,
                 POP_A, POP_B, POP_C, POP_D, POP_I, POP_J, POP_K, POP_L,
                 POPW_DA, POPW_BC, POPW_JI, POPW_LK, POPW_XP, POPW_YP, POPW_BP:
                mnemonic = op.toString().replace("_", " ");
                break;
                
            case MOVW_RIM_0, CMP_RIM_0, CMPW_RIM_0:
                args += ", 0";
                break;
                
            case SHL_RIM_1, SHR_RIM_1, SAR_RIM_1, ROL_RIM_1, ROR_RIM_1, RCL_RIM_1, RCR_RIM_1:
                args += ", 1";
                break;
            
            default:
        }
        
        if(this.uppercase) {
            return (mnemonic + args).toUpperCase();
        } else {
            return (mnemonic + args).toLowerCase();
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
