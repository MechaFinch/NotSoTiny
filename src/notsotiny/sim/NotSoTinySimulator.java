package notsotiny.sim;

import notsotiny.sim.LocationDescriptor.LocationType;
import notsotiny.sim.memory.MemoryController;
import notsotiny.sim.memory.MemoryManager;
import notsotiny.sim.memory.UnprivilegedAccessException;
import notsotiny.sim.ops.Family;
import notsotiny.sim.ops.Opcode;
import notsotiny.sim.ops.Operation;

/**
 * Simulates the NotSoTiny architecture
 * 
 * @author Mechafinch
 */
public class NotSoTinySimulator {
    
    private static final byte VECTOR_DECODING_ERROR = 0x08,
                              VECTOR_GENERAL_PROTECTION_FAULT = 0x10,
                              VECTOR_MEMORY_PROTECTION_FAULT = 0x11;
    
    private MemoryManager memory;
    
    // 16 bit regs
    private short reg_a,
                  reg_b,
                  reg_c,
                  reg_d,
                  reg_i,
                  reg_j,
                  reg_k,
                  reg_l,
                  reg_f;
    
    // PF Fields 
    private boolean pf_ie,
                    pf_pv;
    
    // 32 bit regs
    private int reg_ip,
                reg_sp,
                reg_bp;
    
    // state
    private boolean halted,
                    externalInterrupt,
                    generalProtectionFault,
                    memoryProtectionFault;
    
    private byte externalInterruptVector;
    
    private int prev_ip;
    private byte[] fetchBuffer;
    
    /**
     * Start the simulator at the given address
     * 
     * @param memory
     * @param entry
     */
    public NotSoTinySimulator(MemoryManager memory, int entry) {
        this.memory = memory;
        this.reg_ip = entry;
        
        this.reg_a = 0;
        this.reg_b = 0;
        this.reg_c = 0;
        this.reg_d = 0;
        this.reg_i = 0;
        this.reg_j = 0;
        this.reg_sp = 0;
        this.reg_bp = 0;
        this.reg_f = 0;
        this.pf_ie = false;
        this.pf_pv = true;
        this.halted = false;
        this.externalInterrupt = false;
        this.externalInterruptVector = 0;
        this.generalProtectionFault = false;
        this.memoryProtectionFault = false;
        this.fetchBuffer = new byte[8];
        this.prev_ip = this.reg_ip;
    }
    
    /**
     * Start the simulator
     * 
     * @param memory
     */
    public NotSoTinySimulator(MemoryManager memory) {
        this(memory, memory.read4BytesPrivileged(0));
    }
    
    /**
     * Run a step
     * 
     * Opcodes are sent to family handlers based on their category. These family handlers then send
     * opcodes off to operation-specific methods. All for organization.
     */
    public synchronized void step() {
        
        if(this.externalInterrupt) {
            //System.out.println("External interrupt: " + this.externalInterruptVector);
            this.externalInterrupt = false;
            runInterruptPrivileged(this.externalInterruptVector);
            return;
        }
        
        // fetch
        byte[] readArr;
        int ipDiff = this.reg_ip - this.prev_ip;
        
        try {
            switch(ipDiff) {
                case 1:
                    this.fetchBuffer[0] = this.fetchBuffer[1];
                    this.fetchBuffer[1] = this.fetchBuffer[2];
                    this.fetchBuffer[2] = this.fetchBuffer[3];
                    this.fetchBuffer[3] = this.fetchBuffer[4];
                    this.fetchBuffer[4] = this.fetchBuffer[5];
                    this.fetchBuffer[5] = this.fetchBuffer[6];
                    this.fetchBuffer[6] = this.fetchBuffer[7];
                    this.fetchBuffer[7] = this.memory.readByte(this.reg_ip + 7, this.pf_pv);
                    break;
                    
                case 2:
                    this.fetchBuffer[0] = this.fetchBuffer[2];
                    this.fetchBuffer[1] = this.fetchBuffer[3];
                    this.fetchBuffer[2] = this.fetchBuffer[4];
                    this.fetchBuffer[3] = this.fetchBuffer[5];
                    this.fetchBuffer[4] = this.fetchBuffer[6];
                    this.fetchBuffer[5] = this.fetchBuffer[7];
                    
                    readArr = this.memory.read2ByteArray(this.reg_ip + 6, this.pf_pv);
                    this.fetchBuffer[6] = readArr[0];
                    this.fetchBuffer[7] = readArr[1];
                    break;
                
                case 3:
                    this.fetchBuffer[0] = this.fetchBuffer[3];
                    this.fetchBuffer[1] = this.fetchBuffer[4];
                    this.fetchBuffer[2] = this.fetchBuffer[5];
                    this.fetchBuffer[3] = this.fetchBuffer[6];
                    this.fetchBuffer[4] = this.fetchBuffer[7];
                    
                    readArr = this.memory.read3ByteArray(this.reg_ip + 5, this.pf_pv);
                    this.fetchBuffer[5] = readArr[0];
                    this.fetchBuffer[6] = readArr[1];
                    this.fetchBuffer[7] = readArr[2];
                    break;
                
                case 4:
                    this.fetchBuffer[0] = this.fetchBuffer[4];
                    this.fetchBuffer[1] = this.fetchBuffer[5];
                    this.fetchBuffer[2] = this.fetchBuffer[6];
                    this.fetchBuffer[3] = this.fetchBuffer[7];
                    
                    readArr = this.memory.read4ByteArray(this.reg_ip + 4, this.pf_pv);
                    this.fetchBuffer[4] = readArr[0];
                    this.fetchBuffer[5] = readArr[1];
                    this.fetchBuffer[6] = readArr[2];
                    this.fetchBuffer[7] = readArr[3];
                    break;
                
                case 5:
                    this.fetchBuffer[0] = this.fetchBuffer[5];
                    this.fetchBuffer[1] = this.fetchBuffer[6];
                    this.fetchBuffer[2] = this.fetchBuffer[7];
                    
                    readArr = this.memory.read4ByteArray(this.reg_ip + 3, this.pf_pv);
                    this.fetchBuffer[3] = readArr[0];
                    this.fetchBuffer[4] = readArr[1];
                    this.fetchBuffer[5] = readArr[2];
                    this.fetchBuffer[6] = readArr[3];
                    
                    this.fetchBuffer[7] = this.memory.readByte(reg_ip + 7, this.pf_pv);
                    break;
                    
                case 6:
                    this.fetchBuffer[0] = this.fetchBuffer[6];
                    this.fetchBuffer[1] = this.fetchBuffer[7];
                    
                    readArr = this.memory.read4ByteArray(this.reg_ip + 2, this.pf_pv);
                    this.fetchBuffer[2] = readArr[0];
                    this.fetchBuffer[3] = readArr[1];
                    this.fetchBuffer[4] = readArr[2];
                    this.fetchBuffer[5] = readArr[3];
                    
                    readArr = this.memory.read2ByteArray(this.reg_ip + 6, this.pf_pv);
                    this.fetchBuffer[6] = readArr[0];
                    this.fetchBuffer[7] = readArr[1];
                    break;
                    
                case 7:
                    this.fetchBuffer[0] = this.fetchBuffer[7];
                    
                    readArr = this.memory.read4ByteArray(this.reg_ip + 1, this.pf_pv);
                    this.fetchBuffer[1] = readArr[0];
                    this.fetchBuffer[2] = readArr[1];
                    this.fetchBuffer[3] = readArr[2];
                    this.fetchBuffer[4] = readArr[3];
                    
                    readArr = this.memory.read3ByteArray(this.reg_ip + 5, this.pf_pv);
                    this.fetchBuffer[5] = readArr[0];
                    this.fetchBuffer[6] = readArr[1];
                    this.fetchBuffer[7] = readArr[2];
                    break;
                
                default:
                    readArr = this.memory.read4ByteArray(this.reg_ip + 0, this.pf_pv);
                    this.fetchBuffer[0] = readArr[0];
                    this.fetchBuffer[1] = readArr[1];
                    this.fetchBuffer[2] = readArr[2];
                    this.fetchBuffer[3] = readArr[3];
                    
                    readArr = this.memory.read4ByteArray(this.reg_ip + 4, this.pf_pv);
                    this.fetchBuffer[4] = readArr[0];
                    this.fetchBuffer[5] = readArr[1];
                    this.fetchBuffer[6] = readArr[2];
                    this.fetchBuffer[7] = readArr[3];
                    
            }
            
            this.prev_ip = this.reg_ip;
            this.reg_ip++;
            
            InstructionDescriptor desc = new InstructionDescriptor();
            desc.op = Opcode.fromOp(this.fetchBuffer[0]);
            
            if(desc.op == Opcode.NOP) return; 
            
            switch(desc.op) {
                case CMP_RIM_I8, ADD_RIM_I8, ADC_RIM_I8, SUB_RIM_I8, SBB_RIM_I8,
                     SHL_RIM_I8, SHR_RIM_I8, SAR_RIM_I8, ROL_RIM_I8, ROR_RIM_I8, RCL_RIM_I8, RCR_RIM_I8:
                    desc.hasRIMI8 = true;
                    break;
                
                default:
                    break;
            }
            
            //System.out.println(desc.op.getType().getFamily()); 
            
            switch(desc.op.getType().getFamily()) {
                case INV:
                    // invalid
                    runInterruptPrivileged(VECTOR_DECODING_ERROR);
                    break;
                
                case ADDITION:
                    stepAdditionFamily(desc);
                    break;
                    
                case JUMP:
                    stepJumpFamily(desc);
                    break;
                    
                case LOGIC:
                    stepLogicFamily(desc);
                    break;
                    
                case MISC:
                    stepMiscFamily(desc);
                    break;
                    
                case MOVE:
                    stepMoveFamily(desc);
                    break;
                    
                case MULTIPLICATION:
                    stepMultiplicationFamily(desc);
                    break;
                    
                default:
                    break;
            }
            
            // jumps update themselves
            if(!(desc.op.getType().getFamily() == Family.JUMP && desc.op.getType() != Operation.CMP && desc.op.getType() != Operation.PCMP)) {
                updateIP(desc);
            }
        } catch(UnprivilegedAccessException e) {
            this.memoryProtectionFault = true;
        }
        
        if(this.generalProtectionFault) {
            this.generalProtectionFault = false;
            runInterruptPrivileged(VECTOR_GENERAL_PROTECTION_FAULT);
        }
        
        if(this.memoryProtectionFault) {
            this.memoryProtectionFault = false;
            runInterruptPrivileged(VECTOR_MEMORY_PROTECTION_FAULT);
        }
    }
    
    /**
     * Interprets MISC family instructions
     * 
     * @param op
     * @throws UnprivilegedAccessException 
     */
    private void stepMiscFamily(InstructionDescriptor desc) throws UnprivilegedAccessException {
        //System.out.println(desc.op.getType());
        
        switch(desc.op.getType()) {
            case HLT:
                this.halted = true;
                break;
            
            case LEA:
                runLEA(desc);
                break;
            
            case NOP: // just NOP
                break;
            
            default:
                throw new IllegalStateException("Sent invalid instruction to MISC Family handler: " + desc.op);
        }
    }
    
    /**
     * Interprets LOGIC family instructions
     * 
     * @param op
     * @throws UnprivilegedAccessException 
     */
    private void stepLogicFamily(InstructionDescriptor desc) throws UnprivilegedAccessException {
        //System.out.println(desc.op.getType());
        
        switch(desc.op.getType()) {
            case AND:
            case OR:
            case XOR:
            case TST:
            case PTST:
                run2Logic(desc);
                break;
            
            case NOT:
            case NEG:
                run1Logic(desc);
                break;
            
            case SHL:
            case SHR:
            case SAR:
            case ROL:
            case ROR:
            case RCL:
            case RCR:
                runRotateLogic(desc);
                break;
            
            default:
                throw new IllegalStateException("Sent invalid instruction to LOGIC Family handler: " + desc.op);
        }
    }
    
    /**
     * Executes 2-input logic instructions AND, OR, and XOR
     * 
     * @param op
     * @throws UnprivilegedAccessException 
     */
    private void run2Logic(InstructionDescriptor desc) throws UnprivilegedAccessException {
        // operands
        LocationDescriptor dst = switch(desc.op) {
            case AND_F_RIM, OR_F_RIM, XOR_F_RIM -> LocationDescriptor.REGISTER_F;
            default                             -> getNormalRIMDestinationDescriptor(desc);
        };
        
        int b = switch(desc.op) {
            case AND_RIM_F, OR_RIM_F, XOR_RIM_F -> this.reg_f;
            case PTST_RIMP                      -> getPackedRIMSource(getNormalRIMSourceDescriptor(desc), desc);
            default                             -> getNormalRIMSource(desc);
        };
        
        // operation
        int c = switch(desc.op.getType()) {
            case AND, TST   -> readLocation(dst) & b;
            case PTST       -> getPackedRIMSource(dst, desc) & b;
            case OR         -> readLocation(dst) | b;
            case XOR        -> readLocation(dst) ^ b;
            default -> 0;
        };
        
        // flags
        switch(desc.op) {
            case AND_F_RIM, AND_RIM_F, OR_F_RIM, OR_RIM_F, XOR_F_RIM, XOR_RIM_F:
                break;
            
            case PTST_RIMP:
                if(dst.size() == 1) {
                    // packed 4
                    this.reg_f = (short)(
                        get2LogicFlags(c & 0x0F, 0) |
                        (get2LogicFlags((c >> 4) & 0x0F, 0) << 4) |
                        (get2LogicFlags((c >> 8) & 0x0F, 0) << 8) |
                        (get2LogicFlags((c >> 12) & 0x0F, 0) << 12)
                    );
                } else {
                    // packed 8
                    this.reg_f = (short)(get2LogicFlags(c & 0x00FF, 1) | (get2LogicFlags((c >> 8) & 0x00FF, 1) << 8));
                }
                break;
            
            default:
                this.reg_f = get2LogicFlags(c, dst.size());
        }
        
        // write
        switch(desc.op.getType()) {
            case TST, PTST: break;
            default: writeLocation(dst, c);
        }
    }
    
    /**
     * Gets the flags from a 2logic operation
     * @param c
     */
    private short get2LogicFlags(int c, int size) {
        boolean zero = c == 0,
                overflow = false,
                sign = switch(size) {
                    case 0  -> (c & 0x08) != 0;
                    case 1  -> (c & 0x80) != 0;
                    case 2  -> (c & 0x8000) != 0;
                    case 4  -> (c & 0x8000_0000) != 0;
                    default -> false;
                },
                carry = false;
                
        return (short)((zero ? 0x08 : 0x00) | (overflow ? 0x04 : 0x00) | (sign ? 0x02 : 0x00) | (carry ? 0x01 : 0x00));
    }
    
    /**
     * Executes 1-input logic instructions NOT and NEG
     * 
     * @param op
     * @throws UnprivilegedAccessException 
     */
    private void run1Logic(InstructionDescriptor desc) throws UnprivilegedAccessException {
        if(desc.op == Opcode.NOT_F) {
            this.reg_f = (short)(~this.reg_f);
        } else {
            LocationDescriptor dst = getNormalRIMDestinationDescriptor(desc);
            int v = ~readLocation(dst);
            
            if(desc.op == Opcode.NEG_RIM) {
                v = add(v, 1, dst.size(), false, false);
            } else {
                // flags
                boolean zero = v == 0,
                        overflow = false,
                        sign = switch(dst.size()) {
                            case 1  -> (v & 0x80) != 0;
                            case 2  -> (v & 0x8000) != 0;
                            case 4  -> (v & 0x8000_0000) != 0;
                            default -> false;
                        },
                        carry = false;
                        
                this.reg_f = (short)((zero ? 0x08 : 0x00) | (overflow ? 0x04 : 0x00) | (sign ? 0x02 : 0x00) | (carry ? 0x01 : 0x00));
            }
            
            writeLocation(dst, v);
        }
    }
    
    /**
     * Executes rotation instructions SHL, SHR, SAR, ROL, ROR, RCL, and RCR
     * 
     * @param op
     * @throws UnprivilegedAccessException 
     */
    private void runRotateLogic(InstructionDescriptor desc) throws UnprivilegedAccessException {
        LocationDescriptor dst = getNormalRIMDestinationDescriptor(desc);
        
        int mask = switch(dst.size()) {
            case 1  -> 0x0000_00FF;
            case 2  -> 0x0000_FFFF;
            default -> 0xFFFF_FFFF;
        };
        
        int a = readLocation(dst) & mask,
            b,
            c = 0;
        
        if(desc.hasRIMI8) {
            b = getRIMI8(desc);
        } else {
            b = getNormalRIMSource(desc) & mask;
        }
        
        boolean carry = (this.reg_f & 0x0001) != 0;
        
        // agh
        switch(desc.op) {
            case SHL_RIM:
            case SHL_RIM_I8:
                carry = ((switch(dst.size()) {
                    case 1  -> 0x80;
                    case 2  -> 0x8000;
                    case 4  -> 0x8000_000;
                    default -> 0;
                } >>> (b - 1)) & a) != 0;
                
                c = a << b;
                break;
            
            case SHR_RIM:
            case SHR_RIM_I8:
                carry = ((1 << (b - 1)) & a) != 0;
                
                c = switch(dst.size()) {
                    case 1  -> ((a << 24) >>> b) >>> 24;
                    case 2  -> ((a << 16) >>> b) >>> 16;
                    case 4  -> a >>> b;
                    default -> 0;
                };
                break;
                
            case SAR_RIM:
            case SAR_RIM_I8:
                carry = ((1 << (b - 1)) & a) != 0;
                
                c = switch(dst.size()) {
                    case 1  -> ((a << 24) >> b) >>> 24;
                    case 2  -> ((a << 16) >> b) >>> 16;
                    case 3  -> a >> b;
                    default -> 0;
                };
                break;
                
            case ROL_RIM:
            case ROL_RIM_I8:
            case RCL_RIM:
            case RCL_RIM_I8:
                long al = a;
                int rot = 0;
                for(int i = 0; i < b; i++) {
                    // use previous carry for RCL
                    if(desc.op == Opcode.RCL_RIM || desc.op == Opcode.RCL_RIM_I8) {
                        rot = carry ? 1 : 0;
                    }
                    
                    al <<= 1;
                    carry = (switch(dst.size()) {
                        case 1  -> 0x100l;
                        case 2  -> 0x1_0000l;
                        case 4  -> 0x1_0000_0000l;
                        default -> 0l;
                    } & al) != 0;
                    
                    // use current carry for ROL
                    if(desc.op == Opcode.ROL_RIM || desc.op == Opcode.ROL_RIM_I8) {
                        rot = carry ? 1 : 0;
                    }
                    
                    al |= rot;
                }
                
                c = (int) al;
                break;
                
            case ROR_RIM:
            case ROR_RIM_I8:
            case RCR_RIM:
            case RCR_RIM_I8:
                al = a;
                rot = 0;
                
                int rot_in = switch(dst.size()) {
                    case 1  -> 0x80;
                    case 2  -> 0x8000;
                    case 4  -> 0x8000_0000;
                    default -> 0;
                };
                
                for(int i = 0; i < b; i++) {
                    // use previous carry for RCR
                    if(desc.op == Opcode.RCR_RIM || desc.op == Opcode.RCR_RIM_I8) {
                        rot = carry ? 1 : 0;
                    }
                    
                    carry = (al & 1) != 0;
                    al >>>= 1;
                    
                    // use current carry for ROR
                    if(desc.op == Opcode.ROR_RIM || desc.op == Opcode.ROR_RIM_I8) {
                        rot = carry ? 1 : 0;
                    }
                    
                    al |= rot * rot_in; // rotate right means rot goes into the MSB
                }
                
                c = (int) al;
                break;
            
            default:
        }
        
        // flags
        boolean zero = c == 0,
                overflow = false,
                sign = switch(dst.size()) {
                    case 1  -> (c & 0x80) != 0;
                    case 2  -> (c & 0x8000) != 0;
                    case 4  -> (c & 0x8000_0000) != 0;
                    default -> false;
                };
                
        this.reg_f = (short)((zero ? 0x08 : 0x00) | (overflow ? 0x04 : 0x00) | (sign ? 0x02 : 0x00) | (carry ? 0x01 : 0x00));
        
        writeLocation(dst, c);
    }
    
    /**
     * Interpret a MULTIPLICATION family instruction
     * 
     * @param op
     * @throws UnprivilegedAccessException 
     */
    private void stepMultiplicationFamily(InstructionDescriptor desc) throws UnprivilegedAccessException {
        //System.out.println(desc.op.getType());
        
        switch(desc.op.getType()) {
            case MUL:
            case MULH:
            case MULSH:
                runMUL(desc);
                break;
            
            case PMUL:
            case PMULH:
            case PMULSH:
                runPMUL(desc);
                break;
            
            case DIV:
            case DIVS:
            case DIVM:
            case DIVMS:
                runDIV(desc);
                break;
            
            case PDIV:
            case PDIVS:
            case PDIVM:
            case PDIVMS:
                runPDIV(desc);
                break;
            
            default:
                throw new IllegalStateException("Sent invalid instruction to MULTIPLICATION Family handler: " + desc.op);
        }
    }
    
    /**
     * Executes MUL, MULH, and MULSH instructions
     * 
     * @param op
     * @throws UnprivilegedAccessException 
     */
    private void runMUL(InstructionDescriptor desc) throws UnprivilegedAccessException {
        LocationDescriptor thinDst = getNormalRIMDestinationDescriptor(desc);
        
        boolean high = desc.op == Opcode.MULH_RIM || desc.op == Opcode.MULSH_RIM,
                signed = desc.op == Opcode.MULSH_RIM;
        
        if(high) {
            LocationType type;
            
            if(thinDst.size() == 1) {
                type = switch(thinDst.type()) {
                    case REG_A  -> thinDst.address() == 0 ? LocationType.REG_A : LocationType.REG_I;
                    case REG_B  -> thinDst.address() == 0 ? LocationType.REG_B : LocationType.REG_J;
                    case REG_C  -> thinDst.address() == 0 ? LocationType.REG_C : LocationType.REG_K;
                    case REG_D  -> thinDst.address() == 0 ? LocationType.REG_D : LocationType.REG_L;
                    default     -> thinDst.type();
                };
                
                LocationDescriptor wideDst = new LocationDescriptor(type, 2, 0);
                int a = getNormalRIMSource(desc),
                    b = readLocation(wideDst) & 0x0000_00FF;
                
                int c = multiply(a, b, 2, high, signed);
                writeLocation(wideDst, c);
            } else {
                type = switch(thinDst.type()) {
                    case REG_J  -> LocationType.REG_K;
                    case REG_K  -> LocationType.REG_BP;
                    case REG_L  -> LocationType.REG_SP;
                    default     -> thinDst.type();
                };
                
                LocationDescriptor wideDst = new LocationDescriptor(type, 4, 0);
                int a = getNormalRIMSource(desc),
                    b = readLocation(wideDst) & 0x0000_FFFF;
                
                // A is sign extended, B is not
                if(signed) {
                    b = (int)((short) b);
                } else {
                    a &= 0xFFFF;
                }
                
                int c = multiply(a, b, 4, high, signed);
                writeLocation(wideDst, c);
            }
        } else {
            int a = getNormalRIMSource(desc),
                b = readLocation(thinDst);
            
            int c = multiply(a, b, thinDst.size(), high, signed);
            writeLocation(thinDst, c);
        }
    }
    
    /**
     * Executes PMUL, PMULH, and PMULSH instructions
     * 
     * @param op
     * @throws UnprivilegedAccessException 
     */
    private void runPMUL(InstructionDescriptor desc) throws UnprivilegedAccessException {
        LocationDescriptor thinDst = getNormalRIMDestinationDescriptor(desc);
        
        int a = getPackedRIMSource(thinDst, desc),
            b = getPackedRIMSource(getNormalRIMSourceDescriptor(desc), desc);
        
        boolean high = desc.op == Opcode.PMULH_RIMP || desc.op == Opcode.PMULSH_RIMP,
                signed = desc.op == Opcode.PMULSH_RIMP;
        
        int c = multiplyPacked(a, b, thinDst.size() != 1, high, signed);
        
        if(high) {
            putWidePackedRIMDestination(thinDst, c);
        } else {
            putPackedRIMDestination(thinDst, c);
        }
    }
    
    /**
     * Executes DIV, DIVS, DIVM, and DIVMS instructions
     * 
     * @param op
     * @throws UnprivilegedAccessException 
     */
    private void runDIV(InstructionDescriptor desc) throws UnprivilegedAccessException {
        LocationDescriptor thinDst = getNormalRIMDestinationDescriptor(desc);
        
        int size = thinDst.size();
        
        boolean mod = desc.op == Opcode.DIVM_RIM || desc.op == Opcode.DIVMS_RIM,
                signed = desc.op == Opcode.DIVS_RIM || desc.op == Opcode.DIVMS_RIM;
        
        if(mod && thinDst.size() != 4) {
            thinDst = new LocationDescriptor(thinDst.type(), thinDst.size() * 2, thinDst.address());
        }
        
        int a = readLocation(thinDst),
            b = getNormalRIMSource(desc);
        
        // {quot, rem}
        int[] res = divide(a, b, size, mod, signed);
        
        // use unmodified size
        int c = switch(size) {
            case 1  -> mod ? (((res[1] & 0xFF) << 8) | (res[0] & 0xFF)) : res[0]; 
            case 2  -> mod ? (((res[1] & 0xFFFF) << 16) | (res[0] & 0xFFFF)) : res[0];
            default -> res[0];
        };
        
        // no mod check because the destination was modified above
        writeLocation(thinDst, c);
    }
    
    /**
     * Executes PDIV, PDIVS, PDIVM, and PDIVMS instructions
     * 
     * @param op
     * @throws UnprivilegedAccessException 
     */
    private void runPDIV(InstructionDescriptor desc) throws UnprivilegedAccessException {
        LocationDescriptor thinDst = getNormalRIMDestinationDescriptor(desc),
                           src = getNormalRIMSourceDescriptor(desc);
        
        boolean mod = desc.op == Opcode.PDIVM_RIMP || desc.op == Opcode.PDIVMS_RIMP,
                signed = desc.op == Opcode.PDIVS_RIMP || desc.op == Opcode.PDIVMS_RIMP;
        
        int a = mod ? getWidePackedRIMSource(thinDst, desc) : getPackedRIMSource(thinDst, desc),
            b = getPackedRIMSource(src, desc);
        
        // immedaites are always 2 bytes for packed
        if(desc.hasImmediateValue) {
            desc.immediateWidth = 2;
        }
        
        int[] res = dividePacked(a, b, thinDst.size() != 1, mod, signed);
        
        int c = mod ? (((res[1] & 0xFFFF) << 16) | (res[0] & 0xFFFF)) : res[0];
        
        if(mod) {
            writeWideLocation(thinDst, c);
        } else {
            putPackedRIMDestination(thinDst, c);
        }
    }
    
    /**
     * Interpret an ADDITION family instruction
     * 
     * @param op
     * @throws UnprivilegedAccessException 
     */
    private void stepAdditionFamily(InstructionDescriptor desc) throws UnprivilegedAccessException {
        //System.out.println(desc.op.getType());
        
        switch(desc.op.getType()) {
            case INC:
            case ICC:
            case DEC:
            case DCC:
                runINC(desc);
                break;
            
            case PINC:
            case PICC:
            case PDEC:
            case PDCC:
                runPINC(desc);
                break;
                
            case ADD:
            case ADC:
            case SUB:
            case SBB:
                runADD(desc);
                break;
            
            case PADD:
            case PADC:
            case PSUB:
            case PSBB:
                runPADD(desc);
                break;
            
            default:
                throw new IllegalStateException("Sent invalid instruction to ADDITION Family handler: " + desc.op);
        }
    }
    
    /**
     * Executes INC, ICC, DEC, and DCC instructions
     * 
     * @param op
     * @throws UnprivilegedAccessException 
     */
    private void runINC(InstructionDescriptor desc) throws UnprivilegedAccessException {
        LocationDescriptor dst = switch(desc.op) {
            case ICC_A, DCC_A               -> LocationDescriptor.REGISTER_A;
            case ICC_B, DCC_B               -> LocationDescriptor.REGISTER_B;
            case ICC_C, DCC_C               -> LocationDescriptor.REGISTER_C;
            case ICC_D, DCC_D               -> LocationDescriptor.REGISTER_D;
            case INC_I, ICC_I, DEC_I, DCC_I -> LocationDescriptor.REGISTER_I;
            case INC_J, ICC_J, DEC_J, DCC_J -> LocationDescriptor.REGISTER_J;
            case INC_K, ICC_K, DEC_K, DCC_K -> LocationDescriptor.REGISTER_K;
            case INC_L, ICC_L, DEC_L, DCC_L -> LocationDescriptor.REGISTER_L;
            default                         -> getNormalRIMDestinationDescriptor(desc);
        };
        
        // unconditional or condition met
        int incVal = (desc.op.getType() == Operation.INC || desc.op.getType() == Operation.DEC || (this.reg_f & 0x01) != 0) ? 1 : 0;    // are we making a change
        boolean inc = desc.op.getType() == Operation.INC || desc.op.getType() == Operation.ICC;                                         // are we incrementing or decrementing
        int v = add(readLocation(dst), incVal, dst.size(), !inc, false); // needs to happen unconditionally to correctly set flags
        
        writeLocation(dst, v);
    }
    
    /**
     * Executes PINC, PICC, PDEC, and PDCC instructions
     * 
     * @param op
     * @throws UnprivilegedAccessException 
     */
    private void runPINC(InstructionDescriptor desc) throws UnprivilegedAccessException {
        LocationDescriptor dst = getNormalRIMDestinationDescriptor(desc);
        
        boolean bytes = dst.size() != 1,
                subtract = desc.op == Opcode.PDEC_RIMP || desc.op == Opcode.PDCC_RIMP;
        
        int b = 0;
        if(desc.op == Opcode.PINC_RIMP || desc.op == Opcode.PDEC_RIMP) { // unconditional
            b = bytes ? 0x0101 : 0x1111;
        } else { // conditional
            b = this.reg_f & (bytes ? 0x0101 : 0x1111);
        }
        
        int c = addPacked(getPackedRIMSource(dst, desc), b, bytes, subtract, false);
        
        putPackedRIMDestination(dst, c);
    }
    
    /**
     * Executes ADD, ADC, SUB, and SBB instructions
     * 
     * @param op
     * @throws UnprivilegedAccessException 
     */
    private void runADD(InstructionDescriptor desc) throws UnprivilegedAccessException {
        LocationDescriptor dst = switch(desc.op) {
            case ADD_A_I8, SUB_A_I8     -> LocationDescriptor.REGISTER_A;
            case ADD_B_I8, SUB_B_I8     -> LocationDescriptor.REGISTER_B;
            case ADD_C_I8, SUB_C_I8     -> LocationDescriptor.REGISTER_C;
            case ADD_D_I8, SUB_D_I8     -> LocationDescriptor.REGISTER_D;
            case ADD_I_I8, SUB_I_I8     -> LocationDescriptor.REGISTER_I;
            case ADD_J_I8, SUB_J_I8     -> LocationDescriptor.REGISTER_J;
            case ADD_K_I8, SUB_K_I8     -> LocationDescriptor.REGISTER_K;
            case ADD_L_I8, SUB_L_I8     -> LocationDescriptor.REGISTER_L;
            case ADD_SP_I8, SUB_SP_I8   -> LocationDescriptor.REGISTER_SP;
            case ADD_BP_I8, SUB_BP_I8   -> LocationDescriptor.REGISTER_BP; 
            default                     -> getNormalRIMDestinationDescriptor(desc);
        };
        
        int b = switch(desc.op) {
            case ADD_A_I8, ADD_B_I8, ADD_C_I8, ADD_D_I8, ADD_I_I8, ADD_J_I8, ADD_K_I8, ADD_L_I8,
                 SUB_A_I8, SUB_B_I8, SUB_C_I8, SUB_D_I8, SUB_I_I8, SUB_J_I8, SUB_K_I8, SUB_L_I8,
                 ADD_SP_I8, ADD_BP_I8, SUB_SP_I8, SUB_BP_I8 -> {
                     desc.hasImmediateValue = true;
                     desc.immediateWidth = 1;
                     yield this.fetchBuffer[1];
                     //yield this.memory.readByte(this.reg_ip);
                 }
                 
            case ADD_RIM_I8, ADC_RIM_I8, SUB_RIM_I8, SBB_RIM_I8 -> {
                // figure out where our immediate is
                yield getRIMI8(desc);
                //yield this.memory.readByte(this.reg_ip + offset);
            }
                 
            default -> getNormalRIMSource(desc);
        };
        
        boolean includeCarry = desc.op.getType() == Operation.ADC || desc.op.getType() == Operation.SBB;
        int c;
        
        if(desc.op.getType() == Operation.ADD || desc.op.getType() == Operation.ADC) { // add
            c = add(readLocation(dst), b, dst.size(), false, includeCarry);
        } else { // subtract
            c = add(readLocation(dst), b, dst.size(), true, includeCarry);
        }
        
        writeLocation(dst, c);
    }
    
    /**
     * Executes PADD, PADC, PSUB, and PSBB instructions
     * 
     * @param op
     * @throws UnprivilegedAccessException 
     */
    private void runPADD(InstructionDescriptor desc) throws UnprivilegedAccessException {
        LocationDescriptor dst = getNormalRIMDestinationDescriptor(desc);
        
        boolean bytes = dst.size() != 1,
                subtract = desc.op == Opcode.PSUB_RIMP || desc.op == Opcode.PSBB_RIMP,
                includeCarry = desc.op == Opcode.PADC_RIMP || desc.op == Opcode.PSBB_RIMP;
        
        int a = getPackedRIMSource(dst, desc),
            b = getPackedRIMSource(getNormalRIMSourceDescriptor(desc), desc);
        
        if(desc.hasImmediateValue) {
            desc.immediateWidth = 2;
        }
        
        int c = addPacked(a, b, bytes, subtract, includeCarry);
        
        putPackedRIMDestination(dst, c);
    }
    
    /**
     * Interpret a JUMP family instruction
     * 
     * @param op
     * @throws UnprivilegedAccessException 
     */
    private void stepJumpFamily(InstructionDescriptor desc) throws UnprivilegedAccessException {
        //System.out.println(desc.op.getType());
        
        switch(desc.op.getType()) {
            case JMP:
            case JMPA:
                runJMP(desc);
                break;
            
            case CALL:
            case CALLA:
                runCALL(desc);
                break;
            
            case RET:
            case IRET:
                runRET(desc);
                break;
            
            case INT:
                runINT(desc);
                break;
            
            case CMP:
                runCMP(desc);
                break;
            
            case PCMP:
                runPCMP(desc);
                break;
            
            case JCC:
                runJCC(desc);
                break;
                
            default:
                throw new IllegalStateException("Sent invalid instruction to JUMP Family handler: " + desc.op);
        }
    }
    
    /**
     * Executes JMP and JMPA instructions
     * 
     * @param op
     * @throws UnprivilegedAccessException 
     */
    private void runJMP(InstructionDescriptor desc) throws UnprivilegedAccessException {
        // get value
        int val = 0;
        
        switch(desc.op) {
            case JMP_I8:
                desc.hasImmediateValue = true;
                desc.immediateWidth = 1;
                val = this.fetchBuffer[1];
                //val = this.memory.readByte(this.reg_ip);
                break;
            
            case JMP_I16:
                desc.hasImmediateValue = true;
                desc.immediateWidth = 2;
                val = get2FetchBytes(1);
                //val = this.memory.read2Bytes(this.reg_ip);
                break;
            
            case JMP_I32:
            case JMPA_I32:
                desc.hasImmediateValue = true;
                desc.immediateWidth = 4;
                val = get4FetchBytes(1);
                //val = this.memory.read4Bytes(this.reg_ip);
                break;
            
            case JMP_RIM:
                val = getNormalRIMSource(desc);
                break;
            
            case JMPA_RIM32:
                val = getWideRIMSource(desc);
                break;
            
            default:
        }
        
        if(desc.op.getType() == Operation.JMP) { // relative
            updateIP(desc); // relative to next inst.
            this.reg_ip += val;
        } else { // absolute
            this.reg_ip = val;
        }
    }
    
    /**
     * Executes CALL and CALLA instructions
     * 
     * @param op
     * @throws UnprivilegedAccessException 
     */
    private void runCALL(InstructionDescriptor desc) throws UnprivilegedAccessException {
        int target = 0;
        
        switch(desc.op) {
            case CALL_I8:
                desc.hasImmediateValue = true;
                desc.immediateWidth = 1;
                target = this.fetchBuffer[1];
                //target = this.memory.readByte(this.reg_ip);
                break;
                
            case CALL_I16:
                desc.hasImmediateValue = true;
                desc.immediateWidth = 2;
                target = get2FetchBytes(1);
                //target = this.memory.read2Bytes(this.reg_ip);
                break;
                
            case CALL_I32:
                desc.hasImmediateValue = true;
                desc.immediateWidth = 4;
                target = get4FetchBytes(1);
                //target = this.memory.read4Bytes(this.reg_ip);
            
            case CALLA_I32:
                desc.hasImmediateValue = true;
                desc.immediateWidth = 4;
                target = get4FetchBytes(1);
                //target = this.memory.read4Bytes(this.reg_ip);
                break;
            
            case CALL_RIM:
                target = getNormalRIMSource(desc);
                break;
            
            case CALLA_RIM32:
                target = getWideRIMSource(desc);
                break;
            
            default:
        }
        
        // push correct value
        updateIP(desc);
        
        this.reg_sp -= 4;
        this.memory.write4Bytes(this.reg_sp, this.reg_ip, this.pf_pv);
        
        // jump
        if(desc.op.getType() == Operation.CALL) { // relative
            this.reg_ip += target;
        } else { // absolute
            this.reg_ip = target;
        }
    }
    
    /**
     * Executes RET and IRET instructions
     * 
     * @param op
     * @throws UnprivilegedAccessException 
     */
    private void runRET(InstructionDescriptor desc) throws UnprivilegedAccessException {
        if(desc.op == Opcode.IRET) {
            // IRET also pops flags
            this.setRegPF(this.memory.read2Bytes(this.reg_sp, this.pf_pv));
            this.reg_f = this.memory.read2Bytes(this.reg_sp + 2, this.pf_pv);
            this.reg_sp += 4;
        }
        
        // normal RET
        this.reg_ip = this.memory.read4Bytes(this.reg_sp, this.pf_pv);
        this.reg_sp += 4;
    }
    
    /**
     * Executes INT instructions
     * 
     * @param op
     * @throws UnprivilegedAccessException 
     */
    private void runINT(InstructionDescriptor desc) throws UnprivilegedAccessException {
        byte b;
        
        // vector
        if(desc.op == Opcode.INT_I8) {
            b = this.fetchBuffer[1];
            //b = this.memory.readByte(this.reg_ip);
            desc.hasImmediateValue = true;
            desc.immediateWidth = 1;
        } else {
            b = (byte) getNormalRIMSource(desc);
        }
        
        // for proper return location
        updateIP(desc);
        
        runInterrupt(b);
    }
    
    /**
     * Executes a software interrupt
     * 
     * @param num
     * @throws UnprivilegedAccessException 
     */
    private void runInterrupt(byte num) throws UnprivilegedAccessException {
        // push IP, flags, pflags
        this.reg_sp -= 8;
        this.memory.write4Bytes(this.reg_sp + 4, this.reg_ip, this.pf_pv);
        this.memory.write2Bytes(this.reg_sp + 2, this.reg_f, this.pf_pv);
        this.memory.write2Bytes(this.reg_sp, this.getRegPF(), this.pf_pv);
        
        // get vector & jump
        this.reg_ip = this.memory.read4BytesPrivileged((num & 0x00FFl) << 2);
        this.pf_ie = false; // Disable maskable interrupts
        this.pf_pv = true;  // Set privileged
    }
    
    /**
     * Executes a privileged interrupt
     * 
     * @param num
     * @throws UnprivilegedAccessException 
     */
    private void runInterruptPrivileged(byte num) {
        // push IP, flags, pflags
        this.reg_sp -= 8;
        this.memory.write4BytesPrivileged(this.reg_sp + 4, this.reg_ip);
        this.memory.write2BytesPrivileged(this.reg_sp + 2, this.reg_f);
        this.memory.write2BytesPrivileged(this.reg_sp, this.getRegPF());
        
        // get vector & jump
        this.reg_ip = this.memory.read4BytesPrivileged((num & 0x00FFl) << 2);
        this.pf_ie = false; // Disable maskable interrupts
        this.pf_pv = true;  // Set privileged
    }
    
    /**
     * Executes LEA instructions
     * 
     * @param op
     * @throws UnprivilegedAccessException 
     */
    private void runLEA(InstructionDescriptor desc) throws UnprivilegedAccessException {
        // souper simple
        putWideRIMDestination(desc, getNormalRIMSourceDescriptor(desc).address());
    }
    
    /**
     * Executes CMP instructions
     * 
     * @param op
     * @throws UnprivilegedAccessException 
     */
    private void runCMP(InstructionDescriptor desc) throws UnprivilegedAccessException {
        LocationDescriptor dest = getNormalRIMDestinationDescriptor(desc);
        
        int a = readLocation(dest);
                
        int b = switch(desc.op) {
            case CMP_RIM    -> getNormalRIMSource(desc);
            case CMP_RIM_I8 -> {
                yield getRIMI8(desc);
                //yield this.memory.readByte(this.reg_ip + offset);
            }
            default -> 0; // also CMP_RIM_0
        };
        
        // subtract and discard
        add(a, b, dest.size(), true, false);
    }
    
    /**
     * Executes PCMP instructions
     * 
     * @param desc
     * @throws UnprivilegedAccessException 
     */
    private void runPCMP(InstructionDescriptor desc) throws UnprivilegedAccessException {
        LocationDescriptor dest = getNormalRIMDestinationDescriptor(desc);
        
        int a = getPackedRIMSource(dest, desc),
            b = getPackedRIMSource(getNormalRIMSourceDescriptor(desc), desc);
            
        
        addPacked(a, b, dest.size() != 1, true, false);
    }
    
    /**
     * Executes CMOV
     * 
     * @param desc
     * @throws UnprivilegedAccessException 
     */
    private void runCMOV(InstructionDescriptor desc) throws UnprivilegedAccessException {
        // make sure RIM is read properly
        LocationDescriptor srcDesc = getNormalRIMSourceDescriptor(desc),
                           dstDesc = getNormalRIMDestinationDescriptor(desc);
        
        // add EI8
        int offset = 2;
        if(desc.hasBIOByte) offset++;
        if(desc.hasImmediateAddress || desc.hasImmediateValue) offset += desc.immediateWidth;
        
        desc.hasImmediateValue = true;
        desc.immediateWidth++;
        
        boolean condition = conditionTrue(this.fetchBuffer[offset], 0);
        //boolean condition = conditionTrue(this.memory.readByte(this.reg_ip + offset), 0);
        
        // read is unconditional, write is conditional
        int data = readLocation(srcDesc);
        if(condition) {
            writeLocation(dstDesc, data);
        }
    }
    
    /**
     * Executes PCMOV
     * 
     * @param desc
     * @throws UnprivilegedAccessException 
     */
    private void runPCMOV(InstructionDescriptor desc) throws UnprivilegedAccessException {
        // make sure RIM is read properly
        LocationDescriptor srcDesc = getPackedRIMSourceDescriptor(getNormalRIMSourceDescriptor(desc), desc),
                           ndstDesc = getNormalRIMDestinationDescriptor(desc),
                           dstDesc = getPackedRIMSourceDescriptor(ndstDesc, desc);
        
        // add EI8
        int offset = 2;
        if(desc.hasBIOByte) offset++;
        if(desc.hasImmediateAddress || desc.hasImmediateValue) offset += desc.immediateWidth;
        
        desc.hasImmediateValue = true;
        desc.immediateWidth++;
        
        int sourceData = readLocation(srcDesc),
            destData = readLocation(dstDesc);
        
        Opcode cond = Opcode.fromOp(this.fetchBuffer[offset]);
        //Opcode cond = Opcode.fromOp(this.memory.readByte(this.reg_ip + offset));
        
        // for each condition
        if(ndstDesc.size() != 1) {
            // bytes
            // lower
            if(conditionTrue(cond, 0)) {
                destData = (destData & 0xFF00) | (sourceData & 0x00FF);
            }
            
            // upper
            if(conditionTrue(cond, 2)) {
                destData = (destData & 0x00FF) | (sourceData & 0xFF00);
            }
        } else {
            // nybbles
            for(int i = 0; i < 4; i++) {
                if(conditionTrue(cond, i)) {
                    int mask = 0x0F << (i * 4);
                    destData = (destData & ~mask) | (sourceData & mask);
                }
            }
        }
        
        putPackedRIMDestination(dstDesc, destData);
    }
    
    /**
     * Determines if a condition is met
     * 
     * @param opcode
     * @param flagsIndex
     * @return
     */
    private boolean conditionTrue(Opcode op, int flagsIndex) {
        int flags = this.reg_f >> (4 * flagsIndex);
        
        return switch(op) {
            case JC_I8, JC_RIM      -> (flags & 0x01) != 0;    // carry - carry set
            case JNC_I8, JNC_RIM    -> (flags & 0x01) == 0;    // not carry - carry clear
            case JS_I8, JS_RIM      -> (flags & 0x02) != 0;    // sign - sign set
            case JNS_I8, JNS_RIM    -> (flags & 0x02) == 0;    // not sign - sign clear
            case JO_I8, JO_RIM      -> (flags & 0x04) != 0;    // overflow - overflow set
            case JNO_I8, JNO_RIM    -> (flags & 0x04) == 0;    // not overflow - overflow clear
            case JZ_I8, JZ_RIM      -> (flags & 0x08) != 0;    // zero - zero set
            case JNZ_I8, JNZ_RIM    -> (flags & 0x08) == 0;    // not zero - zero clear
            case JA_I8, JA_RIM      -> (flags & 0x09) == 0;    // above - carry clear and zero clear 
            case JBE_I8, JBE_RIM    -> (flags & 0x09) != 0;    // below equal - carry set or zero set
            case JG_I8, JG_RIM      -> ((flags & 0x08) == 0) && (((flags & 0x04) >>> 1) == (flags & 0x02)); // greater - zero clear and sign = overflow
            case JGE_I8, JGE_RIM    -> (((flags & 0x04) >>> 1) == (flags & 0x02));                          // greater equal - sign = overflow
            case JL_I8, JL_RIM      -> (((flags & 0x04) >>> 1) != (flags & 0x02));                          // less - sign != overflow
            case JLE_I8, JLE_RIM    -> ((flags & 0x08) != 0) || (((flags & 0x04) >>> 1) != (flags & 0x02)); // less equal - zero set or sign != overflow
            default                 -> false;
        };
    }
    
    /**
     * Determines if a condition is met
     * 
     * @param opcode
     * @param flagsIndex
     * @return
     */
    private boolean conditionTrue(byte opcode, int flagsIndex) {
        return conditionTrue(Opcode.fromOp(opcode), flagsIndex);
    }
    
    /**
     * Executes JCC instructions
     * 
     * @param op
     * @throws UnprivilegedAccessException 
     */
    private void runJCC(InstructionDescriptor desc) throws UnprivilegedAccessException {
        int offset = 0;
        
        // target
        switch(desc.op) {
            // immediate
            case JC_I8: case JNC_I8:
            case JS_I8: case JNS_I8:
            case JO_I8: case JNO_I8:
            case JZ_I8: case JNZ_I8:
            case JA_I8:
            case JBE_I8:
            case JG_I8: case JGE_I8:
            case JL_I8: case JLE_I8:
                desc.hasImmediateValue = true;
                desc.immediateWidth = 1;
                offset = this.fetchBuffer[1];
                //offset = this.memory.readByte(this.reg_ip);
                break;
            
            // rim
            default:
                offset = getNormalRIMSource(desc);
        }
        
        // check condition
        boolean condition = conditionTrue(desc.op, 0);
        
        updateIP(desc);
        
        // jump
        if(condition) {
            this.reg_ip += offset;
        }
    }
    
    /**
     * Interpret a MOVE family instruction
     * 
     * @param op
     * @throws UnprivilegedAccessException 
     */
    private void stepMoveFamily(InstructionDescriptor desc) throws UnprivilegedAccessException {
        //System.out.println(desc.op.getType());
        
        switch(desc.op.getType()) {
            case MOV:
            case MOVW:
            case MOVS:
            case MOVZ:
                runMOV(desc);
                break;
            
            case CMOVCC:
                runCMOV(desc);
                break;
                
            case PCMOVCC:
                runPCMOV(desc);
                break;
            
            case XCHG:
            case XCHGW:
                runXCHG(desc);
                break;
            
            case PUSH:
            case PUSHA:
                runPUSH(desc);
                break;
            
            case POP:
            case POPA:
                runPOP(desc);
                break;
            
            default:
                throw new IllegalStateException("Sent invalid instruction to MOVE Family handler: " + desc.op);
        }
    }
    
    /**
     * Executes MOV instructions
     * 
     * @param op
     * @throws UnprivilegedAccessException 
     */
    private void runMOV(InstructionDescriptor desc) throws UnprivilegedAccessException {
        //System.out.println(desc.op);
        
        // deal with register-register moves
        switch(desc.op) {
            case MOV_A_B:
                this.reg_a = this.reg_b;
                return;
            
            case MOV_A_C:
                this.reg_a = this.reg_c;
                return;
            
            case MOV_A_D:
                this.reg_a = this.reg_d;
                return;
            
            case MOV_B_A:
                this.reg_b = this.reg_a;
                return;
            
            case MOV_B_C:
                this.reg_b = this.reg_c;
                return;
                
            case MOV_B_D:
                this.reg_b = this.reg_d;
                return;
            
            case MOV_C_A:
                this.reg_c = this.reg_a;
                return;
                
            case MOV_C_B:
                this.reg_c = this.reg_b;
                return;
                
            case MOV_C_D:
                this.reg_c = this.reg_d;
                return;
                
            case MOV_D_A:
                this.reg_d = this.reg_a;
                return;
                
            case MOV_D_B:
                this.reg_d = this.reg_b;
                return;
                
            case MOV_D_C:
                this.reg_d = this.reg_c;
                return;
            
            default:
        }
        
        // get source
        int src = 0;
        
        switch(desc.op) {
            // A
            case MOV_O_A:
            case MOV_BI_A:
            case MOV_BIO_A:
                src = this.reg_a;
                break;
            
            // B
            case MOV_O_B:
            case MOV_BI_B:
            case MOV_BIO_B:
                src = this.reg_b;
                break;
            
            // C
            case MOV_O_C:
            case MOV_BI_C:
            case MOV_BIO_C:
                src = this.reg_c;
                break;
            
            // D
            case MOV_O_D:
            case MOV_BI_D:
            case MOV_BIO_D:
                src = this.reg_d;
                break;
            
            // immediate 8 moves
            case MOVS_A_I8:
            case MOVS_B_I8:
            case MOVS_C_I8:
            case MOVS_D_I8:
                desc.hasImmediateValue = true;
                desc.immediateWidth = 1;
                src = this.fetchBuffer[1];
                //src = this.memory.readByte(this.reg_ip);
                break;
            
            // immediate 16 moves
            case MOV_A_I16:
            case MOV_B_I16:
            case MOV_C_I16:
            case MOV_D_I16:
            case MOV_I_I16:
            case MOV_J_I16:
            case MOV_K_I16:
            case MOV_L_I16:
                desc.hasImmediateValue = true;
                desc.immediateWidth = 2;
                src = get2FetchBytes(1);
                //src = this.memory.read2Bytes(this.reg_ip);
                break;
            
            // offset
            case MOV_A_O:
            case MOV_B_O:
            case MOV_C_O:
            case MOV_D_O:
                desc.hasImmediateAddress = true;
                desc.immediateWidth = 4;
                src = this.memory.read2Bytes(get4FetchBytes(1), this.pf_pv);
                //src = this.memory.read2Bytes(this.memory.read4Bytes(this.reg_ip));
                break;
            
            // BIO w/o offset
            case MOV_A_BI:
            case MOV_B_BI:
            case MOV_C_BI:
            case MOV_D_BI:
                src = this.memory.read2Bytes(getBIOAddress(desc, false, false), this.pf_pv);
                break;
            
            // BIO with offset
            case MOV_A_BIO:
            case MOV_B_BIO:
            case MOV_C_BIO:
            case MOV_D_BIO:
                src = this.memory.read2Bytes(getBIOAddress(desc, false, true), this.pf_pv);
                break;
            
            // wide rim
            case MOVW_RIM:
                src = getWideRIMSource(desc);
                break;
            
            // flags
            case MOV_RIM_F:
                src = this.reg_f;
                break;
            
            case MOV_RIM_PF:
                if(this.pf_pv) {
                    src = this.getRegPF();
                } else {
                    this.generalProtectionFault = true;
                }
                break;
            
            // rim
            case MOV_RIM:
            case MOVS_RIM:
            case MOVZ_RIM:
            case MOV_F_RIM:
            case MOV_PF_RIM:
                src = getNormalRIMSource(desc);
                break;
            
            default:
        }
        
        // put source in destination
        switch(desc.op) {
            // A
            case MOVS_A_I8:
            case MOV_A_I16:
            case MOV_A_O:
            case MOV_A_BI:
            case MOV_A_BIO:
                this.reg_a = (short) src;
                return;
            
            // B
            case MOVS_B_I8:
            case MOV_B_I16:
            case MOV_B_O:
            case MOV_B_BI:
            case MOV_B_BIO:
                this.reg_b = (short) src;
                return;
            
            // C
            case MOVS_C_I8:
            case MOV_C_I16:
            case MOV_C_O:
            case MOV_C_BI:
            case MOV_C_BIO:
                this.reg_c = (short) src;
                return;
            
            // D
            case MOVS_D_I8:
            case MOV_D_I16:
            case MOV_D_O:
            case MOV_D_BI:
            case MOV_D_BIO:
                this.reg_d = (short) src;
                return;
            
            // other regs
            case MOV_I_I16:
                this.reg_i = (short) src;
                return;
            
            case MOV_J_I16:
                this.reg_j = (short) src;
                return;
                
            case MOV_K_I16:
                this.reg_k = (short) src;
                return;
            
            case MOV_L_I16:
                this.reg_l = (short) src;
                return;
            
            // offset
            case MOV_O_A:
            case MOV_O_B:
            case MOV_O_C:
            case MOV_O_D:
                desc.hasImmediateAddress = true;
                desc.immediateWidth = 4;
                this.memory.write2Bytes(get4FetchBytes(1), (short) src, this.pf_pv);
                //this.memory.write2Bytes(this.memory.read4Bytes(this.reg_ip), (short) src);
                return;
            
            // BIO w/o offset
            case MOV_BI_A:
            case MOV_BI_B:
            case MOV_BI_C:
            case MOV_BI_D:
                this.memory.write2Bytes(getBIOAddress(desc, false, false), (short) src, this.pf_pv);
                return;
                
            // BIO with offset
            case MOV_BIO_A:
            case MOV_BIO_B:
            case MOV_BIO_C:
            case MOV_BIO_D:
                this.memory.write2Bytes(getBIOAddress(desc, false, true), (short) src, this.pf_pv);
                return;
                
            // flags
            case MOV_F_RIM:
                this.reg_f = (short) src;
                break;
            
            case MOV_PF_RIM:
                if(this.pf_pv) {
                    this.setRegPF((short) src);
                } else {
                    this.generalProtectionFault = true;
                }
                break;
            
            // rim
            case MOVW_RIM:
                putWideRIMDestination(desc, src);
                return;
            
            case MOVS_RIM:
                // sign extend
                if(desc.sourceWidth == 1) { // 1 byte
                    putWideRIMDestination(desc, (int)((byte) src));
                } else { // 2 byte
                    putWideRIMDestination(desc, (int)((short) src));
                }
                return;
            
            case MOVZ_RIM:
                // zero extend
                //System.out.println("MOVZ Source Width: " + desc.sourceWidth);
                if(desc.sourceWidth == 1) { // 1 byte
                    putWideRIMDestination(desc, src & 0x0000_00FF);
                } else { // 2 byte
                    putWideRIMDestination(desc, src & 0x0000_FFFF);
                }
                return;
            
            case MOV_RIM_PF:
                if(this.generalProtectionFault) {
                    // Ensure complete decoding without actually writing
                    getNormalRIMDestinationDescriptor(desc);
                } else {
                    putNormalRIMDestination(desc, src);
                }
                return;
            
            case MOV_RIM:
            case MOV_RIM_F:
                putNormalRIMDestination(desc, src);
                return;
            
            default:
        }
    }
    
    /**
     * Executes XCHG instructions
     * 
     * @param op
     * @throws UnprivilegedAccessException 
     */
    private void runXCHG(InstructionDescriptor desc) throws UnprivilegedAccessException {
        // i love abstraction
        LocationDescriptor srcDesc = getNormalRIMSourceDescriptor(desc),
                           dstDesc = getNormalRIMDestinationDescriptor(desc);
        
        if(desc.op == Opcode.XCHG_RIM) {
            int tmp = readLocation(srcDesc);
            writeLocation(srcDesc, readLocation(dstDesc));
            writeLocation(dstDesc, tmp);
        } else {
            // XCHGW_RIM
            int tmp = readWideLocation(desc, srcDesc);
            writeWideLocation(srcDesc, readWideLocation(desc, dstDesc));
            writeWideLocation(dstDesc, tmp);
        }
    }
    
    /**
     * Executes PUSH instructions
     * 
     * @param op
     * @return
     * @throws UnprivilegedAccessException 
     */
    private void runPUSH(InstructionDescriptor desc) throws UnprivilegedAccessException {
        // deal with this separately cause of multiple registers and whatnot
        if(desc.op == Opcode.PUSHA) {
            this.reg_sp -= 20;
            this.memory.write4Bytes(this.reg_sp + 0, this.reg_bp, this.pf_pv);
            this.memory.write2Bytes(this.reg_sp + 4, this.reg_l, this.pf_pv);
            this.memory.write2Bytes(this.reg_sp + 6, this.reg_k, this.pf_pv);
            this.memory.write2Bytes(this.reg_sp + 8, this.reg_j, this.pf_pv);
            this.memory.write2Bytes(this.reg_sp + 10, this.reg_i, this.pf_pv);
            this.memory.write2Bytes(this.reg_sp + 12, this.reg_d, this.pf_pv);
            this.memory.write2Bytes(this.reg_sp + 14, this.reg_c, this.pf_pv);
            this.memory.write2Bytes(this.reg_sp + 16, this.reg_b, this.pf_pv);
            this.memory.write2Bytes(this.reg_sp + 18, this.reg_a, this.pf_pv);
            return;
        }
        
        // deal with operand size
        int opSize = switch(desc.op) {
            case PUSH_A, PUSH_B, PUSH_C, PUSH_D, PUSH_I, PUSH_J, PUSH_K, PUSH_L, PUSH_F, PUSH_PF -> 2;
            case PUSH_BP, BPUSH_SP, PUSHW_I32, BPUSHW_I32, PUSHW_RIM, BPUSHW_RIM -> 4;
            default -> getNormalRIMSourceWidth(desc);
        };
        
        // get value
        int val = switch(desc.op) {
            case PUSH_A     -> this.reg_a;
            case PUSH_B     -> this.reg_b;
            case PUSH_C     -> this.reg_c;
            case PUSH_D     -> this.reg_d;
            case PUSH_I     -> this.reg_i;
            case PUSH_J     -> this.reg_j;
            case PUSH_K     -> this.reg_k;
            case PUSH_L     -> this.reg_l;
            case PUSH_BP    -> this.reg_bp;
            case BPUSH_SP   -> this.reg_sp;
            case PUSH_F     -> this.reg_f;
            case PUSH_PF    -> {
                if(this.pf_pv) {
                    yield this.getRegPF();
                } else {
                    this.generalProtectionFault = true;
                    yield 0;
                }
            }
            case PUSHW_I32, BPUSHW_I32 -> {
                desc.hasImmediateValue = true;
                desc.immediateWidth = 4;
                yield get4FetchBytes(1);
                //yield this.memory.read4Bytes(this.reg_ip);
            }
            case PUSHW_RIM, BPUSHW_RIM -> getWideRIMSource(desc);
            default         -> getNormalRIMSource(desc); // rim
        };
        
        if(this.generalProtectionFault) {
            return;
        }
        
        switch(desc.op) { 
            case BPUSH_RIM:
            case BPUSHW_RIM:
            case BPUSHW_I32:
            case BPUSH_SP:// BP
                // update BP
                this.reg_bp -= opSize;
                
                // write
                if(opSize == 1) {
                    this.memory.writeByte(this.reg_bp, (byte) val, this.pf_pv);
                } else if(opSize == 2) {
                    this.memory.write2Bytes(this.reg_bp, (short) val, this.pf_pv);
                } else {
                    this.memory.write4Bytes(this.reg_bp, val, this.pf_pv);
                }
                break;
                
            default: // SP
                // update SP
                this.reg_sp -= opSize;
                
                // write
                if(opSize == 1) {
                    this.memory.writeByte(this.reg_sp, (byte) val, this.pf_pv);
                } else if(opSize == 2) {
                    this.memory.write2Bytes(this.reg_sp, (short) val, this.pf_pv);
                } else {
                    this.memory.write4Bytes(this.reg_sp, val, this.pf_pv);
                }
        }
    }
    
    /**
     * Executes POP instructions
     * 
     * @param op
     * @throws UnprivilegedAccessException 
     */
    private void runPOP(InstructionDescriptor desc) throws UnprivilegedAccessException {
        // deal with POPA separately
        if(desc.op == Opcode.POPA) {
            this.reg_bp = this.memory.read4Bytes(this.reg_sp + 0, this.pf_pv);
            this.reg_l = this.memory.read2Bytes(this.reg_sp + 4, this.pf_pv);
            this.reg_k = this.memory.read2Bytes(this.reg_sp + 6, this.pf_pv);
            this.reg_j = this.memory.read2Bytes(this.reg_sp + 8, this.pf_pv);
            this.reg_i = this.memory.read2Bytes(this.reg_sp + 10, this.pf_pv);
            this.reg_d = this.memory.read2Bytes(this.reg_sp + 12, this.pf_pv);
            this.reg_c = this.memory.read2Bytes(this.reg_sp + 14, this.pf_pv);
            this.reg_b = this.memory.read2Bytes(this.reg_sp + 16, this.pf_pv);
            this.reg_a = this.memory.read2Bytes(this.reg_sp + 18, this.pf_pv);
            
            this.reg_sp += 20;
            return;
        } else if(desc.op == Opcode.POP_PF && !this.pf_pv) {
            this.generalProtectionFault = true;
            return;
        }
        
        // deal with operand size
        int opSize = switch(desc.op) {
            case POP_A, POP_B, POP_C, POP_D, POP_I, POP_J, POP_K, POP_L, POP_F, POP_PF -> 2;
            case POP_BP, BPOP_SP, POPW_RIM, BPOPW_RIM -> 4;
            default -> getNormalRIMSourceWidth(desc);
        };
        
        int sourcePointer;
        
        switch(desc.op) {
            case BPOP_RIM, BPOPW_RIM, BPOP_SP:
                sourcePointer = this.reg_bp;
                this.reg_bp += opSize;
                break;
            
            default:
                sourcePointer = this.reg_sp;
                this.reg_sp += opSize;
                break;
        }
        
        int val = switch(opSize) {
            case 1  -> this.memory.readByte(sourcePointer, this.pf_pv);
            case 2  -> this.memory.read2Bytes(sourcePointer, this.pf_pv);
            case 4  -> this.memory.read4Bytes(sourcePointer, this.pf_pv);
            default -> -1; // not possible
        };
                
        // write
        switch(desc.op) {
            case POP_A:     this.reg_a = (short) val; break;
            case POP_B:     this.reg_b = (short) val; break;
            case POP_C:     this.reg_c = (short) val; break;
            case POP_D:     this.reg_d = (short) val; break;
            case POP_I:     this.reg_i = (short) val; break;
            case POP_J:     this.reg_j = (short) val; break;
            case POP_K:     this.reg_k = (short) val; break;
            case POP_L:     this.reg_l = (short) val; break;
            
            case POP_BP:    this.reg_bp = val; break;
            case BPOP_SP:   this.reg_sp = val; break;
            case POP_F:     this.reg_f = (short) val; break;
            case POP_PF:    this.setRegPF((short) val); break;
            case POPW_RIM, BPOPW_RIM:   putWideRIMDestination(desc, val); break;
            default:        putNormalRIMDestination(desc, val); // rim
        }
    }
    
    /**
     * Adds two numbers and sets flags accordingly
     * 
     * @param a
     * @param b
     * @param size
     * @param subtract determines if B is complemented and how CF is treated. if true: b, carry in, and carry out are complemented.
     * @param includeCarry true to use flags for cin
     * @return a + b
     */
    private int add(int a, int b, int size, boolean subtract, boolean includeCarry) {
        int carryIn = includeCarry ? (this.reg_f & 0x01) : 0;
        
        if(subtract) {
            carryIn ^= 1;
            b = ~b;
        }
        
        // stop sign extending goddammit
        long sizeMask = switch(size) {
            case 1  -> 0xFFl;
            case 2  -> 0xFFFFl;
            case 4  -> 0xFFFF_FFFFl;
            default -> 0l;
        };
        
        long c = (((long) a) & sizeMask) + (((long) b) & sizeMask) + carryIn;
        
        boolean zero = false,
                overflow = false,
                sign = false,
                carry = false;
        
        if(size == 1) { // 8 bit
            zero = (c & 0xFF) == 0;
            overflow = ((a & 0x80) == (b & 0x80)) && ((a & 0x80) != (c & 0x80));
            sign = (c & 0x80) != 0;
            carry = (c & 0x100) != 0;
            
            c &= 0xFF;
        } else if(size == 2) { // 16 bit
            zero = (c & 0xFFFF) == 0;
            overflow = ((a & 0x8000) == (b & 0x8000)) && ((a & 0x8000) != (c & 0x8000));
            sign = (c & 0x8000) != 0;
            carry = (c & 0x1_0000) != 0;
            
            c &= 0xFFFF;
        } else { // 32 bit
            zero = c == 0;
            overflow = ((a & 0x8000_0000l) == (b & 0x8000_0000l)) && ((a & 0x8000_0000l) != (c & 0x8000_0000l));
            sign = (c & 0x8000_0000l) != 0;
            carry = (c & 0x1_0000_0000l) != 0;
        }
        
        this.reg_f = (short)((zero ? 0x08 : 0x00) | (overflow ? 0x04 : 0x00) | (sign ? 0x02 : 0x00) | ((carry != subtract) ? 0x01 : 0x00));
        
        return (int) c;
    }
    
    /**
     * Adds packed numbers and sets flags accordingly
     * 
     * @param a
     * @param b
     * @param bytes
     * @param subtract
     * @param includeCarry
     * @return a + b
     */
    private short addPacked(int a, int b, boolean bytes, boolean subtract, boolean includeCarry) {
        
        // split things up
        long al, bl, carryIn;
        
        if(subtract) {
            b = ~b;
        }
        
        // calc
        if(bytes) {
            al = ((a << 8) & 0xFF_0000) | (a & 0xFF);
            bl = ((b << 8) & 0xFF_0000) | (b & 0xFF);
            carryIn = includeCarry ? (((this.reg_f << 8) & 0x01_0000) | (this.reg_f & 0x01)) : 0;
            if(subtract) carryIn ^= 0x01_0001;
        } else {
            al = ((a << 12) & 0x0F00_0000) | ((a << 8) & 0x000F_0000) | ((a << 4) & 0x0000_0F00) | (a & 0x0000_000F);
            bl = ((b << 12) & 0x0F00_0000) | ((b << 8) & 0x000F_0000) | ((b << 4) & 0x0000_0F00) | (b & 0x0000_000F);
            carryIn = includeCarry ? (((this.reg_f << 12) & 0x0100_0000) | ((this.reg_f << 8) & 0x0001_0000) | ((this.reg_f << 4) & 0x0000_0100) | (this.reg_f & 0x0000_0001)) : 0;
            if(subtract) carryIn ^= 0x0101_0101;
        }
        
        long c = al + bl + carryIn;
        int f = 0;
        
        // flags
        if(bytes) {
            // zero
            f |= ((c & 0xFF_0000) == 0 ? 0x0800 : 0) | ((c & 0xFF) == 0 ? 0x08 : 0);
            
            // overflow
            f |= (((a & 0x8000) == (b & 0x8000) && ((a & 0x8000) != ((c & 0x80_0000) >> 8))) ? 0x0400 : 0) | (((a & 0x80) == (b & 0x80) && ((a & 0x80) != (c & 0x80))) ? 0x04 : 0); 
            
            // sign
            f |= ((c & 0x80_0000) != 0 ? 0x0200 : 0) | ((c & 0x80) != 0 ? 0x02 : 0);
            
            // carry
            f |= ((c & 0x0100_0000) != 0 ? 0x0100 : 0) | ((c & 0x0100) != 0 ? 0x01 : 0);
        } else {
            // zero
            f |= ((c & 0x0F00_0000) == 0 ? 0x8000 : 0) | ((c & 0x0F_0000) == 0 ? 0x0800 : 0) | ((c & 0x0F00) == 0 ? 0x80 : 0) | ((c & 0x0F) == 0 ? 0x08 : 0);
            
            // overflow
            f |= (((a & 0x8000) == (b & 0x8000) && ((a & 0x8000) != ((c & 0x0800_0000) >> 12))) ? 0x4000 : 0) |
                 (((a & 0x0800) == (b & 0x0800) && ((a & 0x0800) != ((c & 0x0008_0000) >> 8))) ? 0x0400 : 0) |
                 (((a & 0x0080) == (b & 0x0080) && ((a & 0x0080) != ((c & 0x0000_0800) >> 4))) ? 0x0040 : 0) |
                 (((a & 0x0008) == (b & 0x0008) && ((a & 0x0008) != (c & 0x0000_0008))) ? 0x0004 : 0);
            
            // sign
            f |= ((c & 0x0800_0000) != 0 ? 0x2000 : 0) | ((c & 0x08_0000) != 0 ? 0x0200 : 0) | ((c & 0x0800) != 0 ? 0x20 : 0) | ((c & 0x08) != 0 ? 0x02 : 0);
            
            // carry
            f |= ((c & 0x1000_0000) != 0 ? 0x1000 : 0) | ((c & 0x10_0000) != 0 ? 0x0100 : 0) | ((c & 0x1000) != 0 ? 0x10 : 0) | ((c & 0x10) != 0 ? 0x01 : 0);
        }
        
        this.reg_f = (short) f;
        
        short v = 0;
        
        // put things back together
        if(bytes) {
            v = (short) (((c >> 8) & 0xFF00) | (c & 0xFF));
        } else {
            v = (short) (((c >> 12) & 0xF000) | ((c >> 8) & 0x0F00) | ((c >> 4) & 0x00F0) | (c & 0x000F));
        }
        
        return v;
    }
    
    /**
     * Multiplies two numbers and sets flags accordingly
     * 
     * @param a
     * @param b
     * @param size
     * @param high
     * @param signed
     * @return a * b {lower, upper}
     */
    private int multiply(int a, int b, int size, boolean high, boolean signed) {
        // calculate
        long res;
        
        long al = 0,
             bl = 0;
        
        if(signed) {
            switch(size) {
                case 0:
                    // shift sign to correct bit, extend, shift back w/ sign-extension
                    al = (long)(((byte)(a << 4)) >> 4);
                    bl = (long)(((byte)(b << 4)) >> 4);
                    break;
                    
                case 1:
                    al = (long)((byte) a);
                    bl = (long)((byte) b);
                    break;
                
                case 2:
                    al = (long)((short) a);
                    bl = (long)((short) b);
                    break;
                
                case 4:
                    al = (long) a;
                    bl = (long) b;
                    break;
                
                default:
            }
        } else {
            long sizeMask = switch(size) {
                case 0  -> 0x0Fl;
                case 1  -> high ? 0x0F : 0xFFl;
                case 2  -> high ? 0xFF : 0xFFFFl;
                case 4  -> high ? 0xFFFF : 0xFFFF_FFFFl;
                default -> 0l;
            };
            
            //System.out.printf("%s %08X %08X %08X ", size, sizeMask, a, b);
            
            al = ((long) a) & sizeMask;
            bl = ((long) b) & sizeMask;
        }
        
        res = al * bl;
        //if(!signed) System.out.printf("%08X * %08X = %08X%n", al, bl, res);
        
        // flags time
        boolean zero = false,
                overflow = false,
                sign = false,
                carry = false;
        
        if(size == 0) { // 4 bit operands (for packed)
            overflow = signed ? !((res & 0xF8l) == 0l || (res & 0xF8l) == 0xF8l) : ((res & 0xF0l) != 0l);
            carry = overflow;
            
            if(high) { // 8 bit result
                zero = (res & 0xFFl) == 0l;
                sign = (res & 0x80l) != 0l;
                
                res &= 0xFFl;
            } else { // 4 bit result
                zero = (res & 0x0Fl) == 0l;
                sign = (res & 0x08l) != 0l;
                
                res &= 0x0Fl;
            }
        } else if(size == 1) { // 8 bit operands
            // if the top half matches the lower's sign bit, overflow/carry are cleared
            overflow = signed ? !((res & 0xFF80l) == 0l || (res & 0xFF80l) == 0xFF80l) : ((res & 0xFF00l) != 0l);
            carry = overflow;
            
            if(high) { // 16 bit result
                zero = (res & 0xFFFFl) == 0l;
                sign = (res & 0x8000l) != 0l;
                
                res &= 0xFFFFl;
            } else { // 8 bit result
                zero = (res & 0xFFl) == 0l;
                sign = (res & 0x80l) != 0l;
                
                res &= 0xFFl;
            }
        } else if(size == 2) { // 16 bit operands
            // as above
            overflow = signed ? !((res & 0xFFFF_8000l) == 0l || (res & 0xFFFF_8000l) == 0xFFFF_8000l) : ((res & 0xFFFF_0000) != 0l);
            carry = overflow;
            
            if(high) { // 32 bit result
                zero = (res & 0xFFFF_FFFFl) == 0l;
                sign = (res & 0x8000_0000l) != 0l;
                
                res &= 0xFFFF_FFFFl;
            } else { // 16 bit result
                zero = (res & 0xFFFFl) == 0l;
                sign = (res & 0x8000l) != 0l;
                
                res &= 0xFFFFl;
            }
        } else { // 32 bit operands, 32 bit result
            zero = (res & 0xFFFF_FFFFl) == 0;
            sign = (res & 0x8000_0000l) != 0;
            overflow = signed ? !((res & 0xFFFF_FFFF_8000_0000l) == 0l || (res & 0xFFFF_FFFF_8000_0000l) == 0xFFFF_FFFF_8000_0000l) : ((res & 0xFFFF_FFFF_0000_0000l) != 0l);
            carry = overflow;
            
            res &= 0xFFFF_FFFFl;
        }
        
        this.reg_f = (short)((zero ? 0x08 : 0x00) | (overflow ? 0x04 : 0x00) | (sign ? 0x02 : 0x00) | (carry ? 0x01 : 0x00));
        
        return (int) res;
    }
    
    /**
     * Multiplies packed numbers and sets flags accordingly.
     * 
     * @param a
     * @param b
     * @param bytes
     * @param high
     * @param signed
     * @return a * b {lower, upper}
     */
    private int multiplyPacked(int a, int b, boolean bytes, boolean high, boolean signed) {
        // multiplication isn't local like additon, so we have to do things separately
        if(bytes) {
            int r1 = multiply(a >> 8, b >> 8, 1, high, signed);
            short r1f = this.reg_f;
            int r2 = multiply(a, b, 1, high, signed);
            
            this.reg_f |= (r1f << 8);
            
            if(high) {
                return ((r1 << 16) & 0xFFFF_0000) | (r2 & 0x0000_FFFF); 
            } else {
                return ((r1 << 8) & 0xFF00) | (r2 & 0x00FF);
            } 
        } else {
            int r1 = multiply(a >> 12, b >> 12, 0, high, signed);
            short r1f = this.reg_f;
            
            int r2 = multiply(a >> 8, b >> 8, 0, high, signed);
            short r2f = this.reg_f;
            
            int r3 = multiply(a >> 4, b >> 4, 0, high, signed);
            short r3f = this.reg_f;
            
            int r4 = multiply(a, b, 0, high, signed);
            
            this.reg_f |= (r1f << 12) | (r2f << 8) | (r3f << 4);
            
            if(high) {
                return ((r1 << 24) & 0xFF00_0000) | ((r2 << 16) & 0x00FF_0000) | ((r3 << 8) & 0x0000_FF00) | (r4 & 0x0000_00FF);
            } else {
                return ((r1 << 12) & 0xF000) | ((r2 << 8) & 0x0F00) | ((r3 << 4) & 0x00F0) | (r4 & 0x000F); 
            }
        }
    }
    
    /**
     * Divides & modulos two numbers and sets flags accordingly
     * 
     * @param a
     * @param b
     * @param size
     * @param mod
     * @param signed
     * @return a / b {quotient, remainder}
     */
    private int[] divide(int a, int b, int size, boolean mod, boolean signed) {
        long quot,
             rem;
        
        // calculate
        long al = 0,
             bl = 0;
        
        // A and B's sizes differ for DIVM
        if(signed) {
            al = switch(size + (mod ? 1 : 0)) {
                case 0      -> (long)(((byte)(a << 4)) >> 4);
                case 1      -> (long)((byte) a);
                case 2      -> (long)((short) a);
                case 3, 4   -> (long) a;
                default     -> 0;
            };
            
            bl = switch(size) {
                case 0      -> (long)(((byte)(b << 4)) >> 4); //shift sign to correct bit, extend, shift back w/ sign-extension
                case 1      -> (long)((byte) b);
                case 2      -> (long)((short) b);
                case 3, 4   -> (long) b;
                default     -> 0;
            };
        } else {
            long sizeMaskA = switch(size + (mod ? 1 : 0)) {
                case 0      -> 0x0Fl;
                case 1      -> 0xFFl;
                case 2      -> 0xFFFFl;
                case 3, 4   -> 0xFFFF_FFFFl;
                default     -> 0l;
            };
            
            long sizeMaskB = switch(size) {
                case 0      -> 0x0Fl;
                case 1      -> 0xFFl;
                case 2      -> 0xFFFFl;
                case 3, 4   -> 0xFFFF_FFFFl;
                default     -> 0l;
            };
            
            al = ((long) a) & sizeMaskA;
            bl = ((long) b) & sizeMaskB;
        }
        
        quot = al / bl;
        rem = al % bl;
        
        boolean zero = false,
                overflow = false,
                sign = false,
                carry = false;
        
        // flags
        if(size == 0) {
            zero = (quot & 0x0F) == 0 && (mod ? (rem & 0x0F) == 0 : true);
            sign = (quot & 0x08) != 0;
            overflow = quot > 7 || quot < -8;
            carry = quot > 15;
            
            quot &= 0x0Fl;
            rem &= 0x0Fl;
        } else if(size == 1) {
            zero = (quot & 0xFF) == 0 && (mod ? (rem & 0xFF) == 0 : true);
            sign = (quot & 0x80) != 0;
            overflow = quot > 127 || quot < -128;
            carry = quot > 255;
            
            quot &= 0xFFl;
            rem &= 0xFFl;
        } else if(size == 2) {
            zero = (quot & 0xFFFF) == 0 && (mod ? (rem & 0xFFFF) == 0 : true);
            sign = (quot & 0x8000) != 0;
            overflow = quot > 32767 || quot < -32768;
            carry = quot > 65535;
            
            quot &= 0xFFFFl;
            rem &= 0xFFFFl;
        } else {
            zero = (quot & 0xFFFF_FFFF) == 0 && (mod ? (rem & 0xFFFF_FFFF) == 0 : true);
            sign = (quot & 0x8000_0000) != 0;
            overflow = quot > 2147483647l || quot < -2147483648l;
            carry = quot > 4294967295l;
        }
        
        this.reg_f = (short)((zero ? 0x08 : 0x00) | (overflow ? 0x04 : 0x00) | (sign ? 0x02 : 0x00) | (carry ? 0x01 : 0x00));
        
        return new int[] {(int) quot, (int) rem};
    }
    
    /**
     * Divides & modulos packed numbers and sets flags accordingly
     * 
     * @param a
     * @param b
     * @param mod
     * @param signed
     * @param bytes
     * @return a / b {quotient, remainder}
     */
    private int[] dividePacked(int a, int b, boolean mod, boolean signed, boolean bytes) {
        // division isn't local like additon, so we have to do things separately
        if(bytes) {
            // because DIVM uses the full value of a, we need to combine properly
            // might as well do b here too
            int a1 = (a >> 8) & 0xFF,
                b1 = (b >> 8) & 0xFF,
                a2 = a & 0xFF,
                b2 = b & 0xFF;
            
            if(mod) {
                a1 |= (a >> 16) & 0xFF00;
                a2 |= (a >> 8) & 0xFF00;
            }
            
            // divide in parts
            int[] r1 = divide(a1, b1, 1, mod, signed);
            short rf1 = this.reg_f;
            
            int[] r2 = divide(a2, b2, 1, mod, signed);
            
            this.reg_f |= (rf1 << 8);
            
            return new int[] {
                    ((r1[0] << 8) & 0xFF00) | (r2[0] & 0xFF),
                    ((r1[1] << 8) & 0xFF00) | (r2[1] & 0xFF)
            };
        } else {
            // above but more
            int a1 = (a >> 12) & 0x0F,
                b1 = (b >> 12) & 0x0F,
                a2 = (a >> 8) & 0x0F,
                b2 = (b >> 8) & 0x0F,
                a3 = (a >> 4) & 0x0F,
                b3 = (b >> 4) & 0x0F,
                a4 = a & 0x0F,
                b4 = b & 0x0F;
            
            if(mod) {
                a1 |= (a >> 24) & 0xF0;
                a2 |= (a >> 20) & 0xF0;
                a3 |= (a >> 16) & 0xF0;
                a4 |= (a >> 12) & 0xF0;
            }
            
            // divide
            int[] r1 = divide(a1, b1, 0, mod, signed);
            short r1f = this.reg_f;
            
            int[] r2 = divide(a2, b2, 0, mod, signed);
            short r2f = this.reg_f;
            
            int[] r3 = divide(a3, b3, 0, mod, signed);
            short r3f = this.reg_f;
            
            int[] r4 = divide(a4, b4, 0, mod, signed);
            
            this.reg_f |= (r1f << 12) | (r2f << 8) | (r3f << 4);
            
            return new int[] {
                    ((r1[0] << 12) & 0xF000) | ((r2[0] << 8) & 0x0F00) | ((r3[0] << 4) & 0xF0) | (r4[0] & 0x0F),
                    ((r1[1] << 12) & 0xF000) | ((r2[1] << 8) & 0x0F00) | ((r3[1] << 4) & 0xF0) | (r4[1] & 0x0F)
            };
        }
    }
    
    /**
     * Gets the descriptor of a RIM destination
     * 
     * @return
     */
    private LocationDescriptor getNormalRIMDestinationDescriptor(InstructionDescriptor desc) {
        byte rim;
        
        if(desc.hasRIMByte) {
            rim = desc.rimByte;
        } else {
            rim = this.fetchBuffer[1];
            //rim = this.memory.readByte(this.reg_ip);
            desc.rimByte = rim;
            desc.hasRIMByte = true;
        }
        
        boolean size = (rim & 0x80) == 0,   // true = large
                regreg = (rim & 0x40) == 0; // true = register-register
        
        byte reg = (byte)((rim >> 3) & 0x07),
             rimem = (byte)(rim & 0x07);
        
        // is the destination a register or memory
        if(regreg || rimem <= 3) {
            // destination is the reg register
            if(size) {
                // 16 bit
                return switch(reg) {
                    case 0  -> LocationDescriptor.REGISTER_A;
                    case 1  -> LocationDescriptor.REGISTER_B;
                    case 2  -> LocationDescriptor.REGISTER_C;
                    case 3  -> LocationDescriptor.REGISTER_D;
                    case 4  -> LocationDescriptor.REGISTER_I;
                    case 5  -> LocationDescriptor.REGISTER_J;
                    case 6  -> LocationDescriptor.REGISTER_K;
                    case 7  -> LocationDescriptor.REGISTER_L;
                    default -> null;
                };
            } else {
                // 8 bit
                return switch(reg) {
                    case 0  -> LocationDescriptor.REGISTER_AL;
                    case 1  -> LocationDescriptor.REGISTER_BL;
                    case 2  -> LocationDescriptor.REGISTER_CL;
                    case 3  -> LocationDescriptor.REGISTER_DL;
                    case 4  -> LocationDescriptor.REGISTER_AH;
                    case 5  -> LocationDescriptor.REGISTER_BH;
                    case 6  -> LocationDescriptor.REGISTER_CH;
                    case 7  -> LocationDescriptor.REGISTER_DH;
                    default -> null;
                };
            }
        } else {
            // destination is memory
            int addr;
            
            if(rimem == 5) { // immediate address
                desc.hasImmediateAddress = true;
                desc.immediateWidth = 4;
                
                addr = get4FetchBytes(2);
                //addr = this.memory.read4Bytes(this.reg_ip + 1);
            } else { // bio address
                addr = getBIOAddress(desc, true, (rim & 0x01) == 1);
            }
            
            return new LocationDescriptor(LocationType.MEMORY, size ? 2 : 1, addr);
        }
    }
    
    /**
     * Puts a value at a location
     * 
     * @param val
     * @throws UnprivilegedAccessException 
     */
    private void writeLocation(LocationDescriptor desc, int val) throws UnprivilegedAccessException {
        //System.out.println("writing location " + desc + " with " + val);
        
        // what we dealin with
        if(desc.type() == LocationType.MEMORY) {
            switch(desc.size()) {
                case 1: this.memory.writeByte(desc.address(), (byte) val, this.pf_pv);      break;
                case 2: this.memory.write2Bytes(desc.address(), (short) val, this.pf_pv);   break;
                case 3: this.memory.write3Bytes(desc.address(), val, this.pf_pv);           break;
                case 4: this.memory.write4Bytes(desc.address(), val, this.pf_pv);           break;
            }
        } else {
            // registers. handle by size
            switch(desc.size()) {
                case 1: // 8 bit
                    switch(desc.type()) {
                        case REG_A:
                            if(desc.address() == 0) { // AL
                                this.reg_a = (short)((this.reg_a & 0xFF00) | (val & 0xFF));
                            } else { // AH
                                this.reg_a = (short)(((val << 8) & 0xFF00) | (this.reg_a & 0xFF));
                            }
                            break;
                            
                        case REG_B:
                            if(desc.address() == 0) { // BL
                                this.reg_b = (short)((this.reg_b & 0xFF00) | (val & 0xFF));
                            } else { // BH
                                this.reg_b = (short)(((val << 8) & 0xFF00) | (this.reg_b & 0xFF));
                            }
                            break;
                            
                        case REG_C:
                            if(desc.address() == 0) { // CL
                                this.reg_c = (short)((this.reg_c & 0xFF00) | (val & 0xFF));
                            } else { // CH
                                this.reg_c = (short)(((val << 8) & 0xFF00) | (this.reg_c & 0xFF));
                            }
                            break;
                            
                        case REG_D:
                            if(desc.address() == 0) { // DL
                                this.reg_d = (short)((this.reg_d & 0xFF00) | (val & 0xFF));
                            } else { // DH
                                this.reg_d = (short)(((val << 8) & 0xFF00) | (this.reg_d & 0xFF));
                            }
                            break;
                            
                        default:
                            throw new IllegalArgumentException("Invalid location descriptor: " + desc);
                    }
                    break;
                
                case 2: // 16 bit
                    switch(desc.type()) {
                        case REG_A:
                            this.reg_a = (short) val;
                            break;
                        
                        case REG_B:
                            this.reg_b = (short) val;
                            break;
                            
                        case REG_C:
                            this.reg_c = (short) val;
                            break;
                        
                        case REG_D:
                            this.reg_d = (short) val;
                            break;
                            
                        case REG_I:
                            this.reg_i = (short) val;
                            break;
                        
                        case REG_J:
                            this.reg_j = (short) val;
                            break;
                            
                        case REG_K:
                            this.reg_k = (short) val;
                            break;
                            
                        case REG_L:
                            this.reg_l = (short) val;
                            break;
                        
                        case REG_F:
                            this.reg_f = (short) val;
                            break;
                            
                        case REG_PF:
                            if(this.pf_pv) {
                                this.setRegPF((short) val);
                            } else {
                                this.generalProtectionFault = true;
                            }
                            break;
                            
                        default:
                            throw new IllegalArgumentException("Invalid location descriptor: " + desc);
                    }
                    break;
                    
                case 4: // 32 bit
                    switch(desc.type()) {
                        case REG_A: // DA
                            this.reg_a = (short) val;
                            this.reg_d = (short) (val >>> 16);
                            break;
                        
                        case REG_B: // AB
                            this.reg_b = (short) val;
                            this.reg_a = (short) (val >>> 16);
                            break;
                            
                        case REG_C: // BC
                            this.reg_c = (short) val;
                            this.reg_b = (short) (val >>> 16);
                            break;
                        
                        case REG_D: // CD
                            this.reg_d = (short) val;
                            this.reg_c = (short) (val >>> 16);
                            break;
                            
                        case REG_I: // JI
                            this.reg_i = (short) val;
                            this.reg_j = (short) (val >>> 16);
                            break;
                        
                        case REG_K: // LK 
                            this.reg_k = (short) val;
                            this.reg_l = (short) (val >>> 16);
                            break;
                        
                        case REG_BP:
                            this.reg_bp = val;
                            break;
                            
                        case REG_SP:
                            this.reg_sp = val;
                            break;
                        
                        default:
                            throw new IllegalArgumentException("Invalid location descriptor: " + desc);
                    }
                    break;
            }
        }
    }
    
    /**
     * Puts the result of a RIM in its destination
     * 
     * @param val
     * @throws UnprivilegedAccessException 
     */
    private void putNormalRIMDestination(InstructionDescriptor desc, int val) throws UnprivilegedAccessException {
        writeLocation(getNormalRIMDestinationDescriptor(desc), val);
    }
    
    /**
     * Puts the result of a wide RIM in its destination (double normal width)
     * 
     * @param val
     * @throws UnprivilegedAccessException 
     */
    private void putWideRIMDestination(InstructionDescriptor desc, int val) throws UnprivilegedAccessException {
        LocationDescriptor normalDesc = getNormalRIMDestinationDescriptor(desc);
        
        writeWideLocation(normalDesc, val);
    }
    
    /**
     * Puts the result of a wide RIM in its destination (double normal width)
     * Takes the LocationDescriptor to avoid recalculation 
     * 
     * @param normalDesc
     * @param val
     * @throws UnprivilegedAccessException 
     */
    private void writeWideLocation(LocationDescriptor normalDesc, int val) throws UnprivilegedAccessException {
        int size = normalDesc.size();
        
        if(size < 4) {
            // correct r16 -> pair by rim
            LocationType type = switch(normalDesc.type()) {
                case REG_A  -> normalDesc.address() == 1 ? LocationType.REG_I : LocationType.REG_A;
                case REG_B  -> normalDesc.address() == 1 ? LocationType.REG_J : LocationType.REG_B;
                case REG_C  -> normalDesc.address() == 1 ? LocationType.REG_K : LocationType.REG_C;
                case REG_D  -> normalDesc.address() == 1 ? LocationType.REG_L : LocationType.REG_D;
                case REG_I  -> LocationType.REG_I;
                case REG_J  -> LocationType.REG_K;
                case REG_K  -> LocationType.REG_BP;
                case REG_L  -> LocationType.REG_SP;
                default     -> normalDesc.type();
            };
            
            // size < 4, double it
            writeLocation(new LocationDescriptor(type, size * 2, normalDesc.address()), val);
        } else {
            // size is 4 write as normal
            writeLocation(normalDesc, val);
        }
    }
    
    /**
     * Puts the result of a packed RIM in its destination
     * 
     * @param normalDesc
     * @param val
     * @throws UnprivilegedAccessException 
     */
    private void putPackedRIMDestination(LocationDescriptor normalDesc, int val) throws UnprivilegedAccessException {
        writeLocation(new LocationDescriptor(normalDesc.type(), 2, normalDesc.address()), val);
    }
    
    /**
     * Puts the result of a wide packed RIM in its destination
     * 
     * @param normalDesc
     * @param val
     * @throws UnprivilegedAccessException 
     */
    private void putWidePackedRIMDestination(LocationDescriptor normalDesc, int val) throws UnprivilegedAccessException {
        writeLocation(new LocationDescriptor(normalDesc.type(), 4, normalDesc.address()), val);
    }
    
    /**
     * Returns the width in bytes of a RIM source
     * 
     * @return
     */
    private int getNormalRIMSourceWidth(InstructionDescriptor desc) {
        byte rim;
        
        // use cache
        if(desc.hasRIMByte) {
            rim = desc.rimByte;
        } else {
            rim = this.fetchBuffer[1];
            //rim = this.memory.readByte(this.reg_ip);
            desc.rimByte = rim;
            desc.hasRIMByte = true;
        }
        
        // simple now that rim doesn't have bp/sp
        return ((rim & 0x80) == 0) ? 2 : 1;
    }
    
    /**
     * Gets the descriptor of a RIM source
     * 
     * @return
     */
    private LocationDescriptor getNormalRIMSourceDescriptor(InstructionDescriptor desc) {
        byte rim;
        
        // use cache
        if(desc.hasRIMByte) {
            rim = desc.rimByte;
        } else {
            rim = this.fetchBuffer[1];
            //rim = this.memory.readByte(this.reg_ip);
            desc.rimByte = rim;
            desc.hasRIMByte = true;
        }
        
        boolean size = (rim & 0x80) == 0,   // true = large
                regreg = (rim & 0x40) == 0; // true = register-register
        
        byte reg = (byte)((rim >> 3) & 0x07),
             rimem = (byte)(rim & 0x07);
        
        // is the source a register or memory
        if(regreg || rimem > 3) {
            // source is a register
            // is reg or rimem the source
            int src = regreg ? rimem : reg;
            
            return switch(src) {
                case 0  -> size ? LocationDescriptor.REGISTER_A : LocationDescriptor.REGISTER_AL;
                case 1  -> size ? LocationDescriptor.REGISTER_B : LocationDescriptor.REGISTER_BL;
                case 2  -> size ? LocationDescriptor.REGISTER_C : LocationDescriptor.REGISTER_CL;
                case 3  -> size ? LocationDescriptor.REGISTER_D : LocationDescriptor.REGISTER_DL;
                case 4  -> size ? LocationDescriptor.REGISTER_I : LocationDescriptor.REGISTER_AH;
                case 5  -> size ? LocationDescriptor.REGISTER_J : LocationDescriptor.REGISTER_BH;
                case 6  -> size ? LocationDescriptor.REGISTER_K : LocationDescriptor.REGISTER_CH;
                case 7  -> size ? LocationDescriptor.REGISTER_L : LocationDescriptor.REGISTER_DH;
                default -> null;
            };
        } else if(rimem == 0) {
            // source is an immediate
            desc.hasImmediateValue = true;
            desc.immediateWidth = size ? 2 : 1;
            
            return new LocationDescriptor(LocationType.MEMORY, size ? 2 : 1, this.reg_ip + 1);
        } else if(rimem == 1) {
            // source is an immediate address
            desc.hasImmediateAddress = true;
            desc.immediateWidth = 4;
            
            int addr = get4FetchBytes(2);
            //int addr = this.memory.read4Bytes(this.reg_ip + 1);
            
            return new LocationDescriptor(LocationType.MEMORY, size ? 2 : 1, addr);
        } else {
            // source is a BIO
            int addr = getBIOAddress(desc, true, (rim & 1) == 1);
            
            return new LocationDescriptor(LocationType.MEMORY, size ? 2 : 1, addr);
        }
    }
    
    /**
     * Get the value from a LocationDescriptor
     * 
     * @param loc
     * @return
     * @throws UnprivilegedAccessException 
     */
    private int readLocation(LocationDescriptor desc) throws UnprivilegedAccessException {
        //System.out.println("reading location " + desc);
        
        // what we workin with
        if(desc.type() == LocationType.MEMORY) {
            return switch(desc.size()) {
                case 1  -> this.memory.readByte(desc.address(), this.pf_pv);
                case 2  -> this.memory.read2Bytes(desc.address(), this.pf_pv);
                case 4  -> this.memory.read4Bytes(desc.address(), this.pf_pv);
                default -> 0;
            };
        } else {
            // initial value
            int rawVal = switch(desc.type()) {
                case REG_A  -> this.reg_a;
                case REG_B  -> this.reg_b;
                case REG_C  -> this.reg_c;
                case REG_D  -> this.reg_d;
                case REG_I  -> this.reg_i;
                case REG_J  -> this.reg_j;
                case REG_K  -> this.reg_k;
                case REG_L  -> this.reg_l;
                case REG_BP -> this.reg_bp;
                case REG_SP -> this.reg_sp;
                case REG_F  -> this.reg_f;
                case REG_PF -> this.getRegPF();
                default     -> 0;
            };
            
            // trim to size
            if(desc.size() == 4) {
                // 32 bit
                if(desc.type() == LocationType.REG_BP || desc.type() == LocationType.REG_SP) {
                    // already 32 bit
                    return rawVal;
                }
                
                // pairs
                int v = switch(desc.type()) {
                    case REG_A  -> this.reg_d;
                    case REG_B  -> this.reg_a;
                    case REG_C  -> this.reg_b;
                    case REG_D  -> this.reg_c;
                    case REG_I  -> this.reg_j;
                    case REG_K  -> this.reg_l;
                    default     -> 0;
                };
                
                return (v << 16) | (rawVal & 0xFFFF);
            } else if(desc.size() == 1) {
                // 8 bit
                if(desc.address() == 0) { // lower byte
                    return rawVal & 0xFF;
                } else { // upper byte
                    return (rawVal >>> 8) & 0xFF;
                }
            } else {
                // 16 bit
                return rawVal;
            }
        }
    }
    
    /**
     * Gets the source of a RIM
     * 
     * @return
     * @throws UnprivilegedAccessException 
     */
    private int getNormalRIMSource(InstructionDescriptor desc) throws UnprivilegedAccessException {
        LocationDescriptor locDesc = getNormalRIMSourceDescriptor(desc); 
        desc.sourceWidth = getNormalRIMSourceWidth(desc);
        return readLocation(locDesc);
    }
    
    /**
     * Gets the source of a wide RIM. Forces sources to be 4 bytes.
     * 
     * @return
     * @throws UnprivilegedAccessException 
     */
    private int getWideRIMSource(InstructionDescriptor desc) throws UnprivilegedAccessException {
        LocationDescriptor normalDesc = getNormalRIMSourceDescriptor(desc);
        desc.sourceWidth = 4;
        
        return readWideLocation(desc, normalDesc);
    }
    
    /**
     * Reads a location (wide) 
     * @param idesc
     * @param normalDesc
     * @return
     * @throws UnprivilegedAccessException 
     */
    private int readWideLocation(InstructionDescriptor idesc, LocationDescriptor normalDesc) throws UnprivilegedAccessException {
        if(idesc.hasImmediateValue) {
            idesc.immediateWidth = 4;
        }
        
        // convert to pairs properly
        LocationType type = switch(normalDesc.type()) {
            case REG_J  -> LocationType.REG_K;
            case REG_K  -> LocationType.REG_BP;
            case REG_L  -> LocationType.REG_SP;
            default     -> normalDesc.type();
        };
        
        // change the size to 4 bytes
        return readLocation(new LocationDescriptor(type, 4, normalDesc.address()));
    }
    
    /**
     * Gets the descriptor for the source of a packed RIM
     * 
     * @param normalDesc
     * @param idesc
     * @return
     */
    private LocationDescriptor getPackedRIMSourceDescriptor(LocationDescriptor normalDesc, InstructionDescriptor idesc) {
        // override immediate size
        if(idesc.hasImmediateValue) idesc.immediateWidth = 2;
        idesc.sourceWidth = 2;
        
        return new LocationDescriptor(normalDesc.type(), 2, normalDesc.address());
    }
    
    /**
     * Gets the source of a packed RIM
     * 
     * @param desc
     * @return
     * @throws UnprivilegedAccessException 
     */
    private int getPackedRIMSource(LocationDescriptor normalDesc, InstructionDescriptor idesc) throws UnprivilegedAccessException {
        return readLocation(getPackedRIMSourceDescriptor(normalDesc, idesc));
    }
    
    /**
     * Gets the source of a wide packed RIM
     * 
     * @param normalDesc
     * @return
     * @throws UnprivilegedAccessException 
     */
    private int getWidePackedRIMSource(LocationDescriptor normalDesc, InstructionDescriptor idesc) throws UnprivilegedAccessException {
        if(idesc.hasImmediateValue) idesc.immediateWidth = 4;
        idesc.sourceWidth = 4;
        return readLocation(new LocationDescriptor(normalDesc.type(), 4, normalDesc.address()));
    }
    
    /**
     * Gets the address from BIO (switch table version)
     * 
     * 64 mdbt iterations test
     * Old: 54 seconds
     * New: 53 seconds
     * 
     * @param desc
     * @param hasRIM
     * @param hasOffset
     * @return
     */
    private int getBIOAddress(InstructionDescriptor desc, boolean hasRIM, boolean hasOffset) {
        byte bio = this.fetchBuffer[hasRIM ? 2 : 1];
        //byte bio = this.memory.readByte(this.reg_ip + (hasRIM ? 1 : 0));
        
        desc.hasBIOByte = true;
        
        int address = 0,
            immediateWidth = 4,
            ipOffset = -1;  // account for ip autoincrement
        
        if(desc.hasRIMByte) {
            if(desc.hasRIMI8) { 
                ipOffset = 2;
            } else {
                ipOffset = 1;
            }
        } else {
            if(desc.hasRIMI8) { 
                ipOffset = 1;
            } else {
                ipOffset = 0;
            }
        }
        
        //System.out.printf("%08X   ", (int) bio);
            
        // base, index, scale
        switch(bio & 0xFF) { 
            case 0x00, 0x01, 0x02, 0x03: // D:A
                immediateWidth = bio + 1;
                address = (this.reg_d << 16) | (this.reg_a & 0xFFFF);
                break;
            
            case 0x04, 0x05, 0x06, 0x07: // IP
                immediateWidth = bio - 0x03;
                address = this.reg_ip + ipOffset + immediateWidth;
                break;
            
            case 0x08, 0x09, 0x0A, 0x0B: // A:B
                immediateWidth = bio - 0x07;
                address = (this.reg_a << 16) | (this.reg_b & 0xFFFF);
                break;
            
            case 0x0C, 0x0D, 0x0E, 0x0F: // IP
                immediateWidth = bio - 0x0B;
                address = this.reg_ip + ipOffset + immediateWidth;
                break;
            
            case 0x10, 0x11, 0x12, 0x13: // B:C
                immediateWidth = bio - 0x0F;
                address = (this.reg_b << 16) | (this.reg_c & 0xFFFF);
                break;
            
            case 0x14, 0x15, 0x16, 0x17: // IP
                immediateWidth = bio - 0x13;
                address = this.reg_ip + ipOffset + immediateWidth;
                break;
            
            case 0x18, 0x19, 0x1A, 0x1B: // C:D
                immediateWidth = bio - 0x17;
                address = (this.reg_c << 16) | (this.reg_d & 0xFFFF);
                break;
            
            case 0x1C, 0x1D, 0x1E, 0x1F: // IP
                immediateWidth = bio - 0x1B;
                address = this.reg_ip + ipOffset + immediateWidth;
                break;
            
            case 0x20, 0x21, 0x22, 0x23: // J:I
                immediateWidth = bio - 0x1F;
                address = (this.reg_j << 16) | (this.reg_i & 0xFFFF);
                break;
            
            case 0x24, 0x25, 0x26, 0x27: // IP + I
                immediateWidth = bio - 0x23;
                address = this.reg_ip + this.reg_i + ipOffset + immediateWidth;
                break;
            
            case 0x28, 0x29, 0x2A, 0x2B: // L:K
                immediateWidth = bio - 0x27;
                address = (this.reg_l << 16) | (this.reg_k & 0xFFFF);
                break;
            
            case 0x2C, 0x2D, 0x2E, 0x2F: // IP + J
                immediateWidth = bio - 0x2B;
                address = this.reg_ip + this.reg_j + ipOffset + immediateWidth;
                break;
            
            case 0x30, 0x31, 0x32, 0x33: // BP
                immediateWidth = bio - 0x2F;
                address = this.reg_bp;
                break;
            
            case 0x34, 0x35, 0x36, 0x37: // IP + K
                immediateWidth = bio - 0x33;
                address = this.reg_ip + this.reg_k + ipOffset + immediateWidth;
                break;
            
            case 0x38, 0x39, 0x3A, 0x3B: // SP
                immediateWidth = bio - 0x37;
                address = this.reg_sp;
                break;
            
            case 0x3C, 0x3D, 0x3E, 0x3F: // IP + L
                immediateWidth = bio - 0x3B;
                address = this.reg_ip + this.reg_l + ipOffset + immediateWidth;
                break;
            
            case 0x40: // D:A + A
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + this.reg_a;
                break;
            
            case 0x41: // D:A + B
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + this.reg_b;
                break;
            
            case 0x42: // D:A + C
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + this.reg_c;
                break;
            
            case 0x43: // D:A + D
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + this.reg_d;
                break;
            
            case 0x44: // D:A + I
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + this.reg_i;
                break;
            
            case 0x45: // D:A + J
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + this.reg_j;
                break;
            
            case 0x46: // D:A + K
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + this.reg_k;
                break;
            
            case 0x47: // D:A + L
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + this.reg_l;
                break;
            
            case 0x48: // A:B + A
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + this.reg_a;
                break;
            
            case 0x49: // A:B + B
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + this.reg_b;
                break;
            
            case 0x4A: // A:B + C
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + this.reg_c;
                break;
            
            case 0x4B: // A:B + D
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + this.reg_d;
                break;
            
            case 0x4C: // A:B + I
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + this.reg_i;
                break;
            
            case 0x4D: // A:B + J
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + this.reg_j;
                break;
            
            case 0x4E: // A:B + K
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + this.reg_k;
                break;
            
            case 0x4F: // A:B + L
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + this.reg_l;
                break;
            
            case 0x50: // B:C + A
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + this.reg_a;
                break;
            
            case 0x51: // B:C + B
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + this.reg_b;
                break;
            
            case 0x52: // B:C + C
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + this.reg_c;
                break;
            
            case 0x53: // B:C + D
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + this.reg_d;
                break;
            
            case 0x54: // B:C + I
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + this.reg_i;
                break;
            
            case 0x55: // B:C + J
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + this.reg_j;
                break;
            
            case 0x56: // B:C + K
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + this.reg_k;
                break;
            
            case 0x57: // B:C + L
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + this.reg_l;
                break;
            
            case 0x58: // C:D + A
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + this.reg_a;
                break;
            
            case 0x59: // C:D + B
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + this.reg_b;
                break;
            
            case 0x5A: // C:D + C
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + this.reg_c;
                break;
            
            case 0x5B: // C:D + D
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + this.reg_d;
                break;
            
            case 0x5C: // C:D + I
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + this.reg_i;
                break;
            
            case 0x5D: // C:D + J
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + this.reg_j;
                break;
            
            case 0x5E: // C:D + K
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + this.reg_k;
                break;
            
            case 0x5F: // C:D + L
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + this.reg_l;
                break;
            
            case 0x60: // J:I + A
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + this.reg_a;
                break;
            
            case 0x61: // J:I + B
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + this.reg_b;
                break;
            
            case 0x62: // J:I + C
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + this.reg_c;
                break;
            
            case 0x63: // J:I + D
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + this.reg_d;
                break;
            
            case 0x64: // J:I + I
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + this.reg_i;
                break;
            
            case 0x65: // J:I + J
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + this.reg_j;
                break;
            
            case 0x66: // J:I + K
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + this.reg_k;
                break;
            
            case 0x67: // J:I + L
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + this.reg_l;
                break;
            
            case 0x68: // L:K + A
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + this.reg_a;
                break;
            
            case 0x69: // L:K + B
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + this.reg_b;
                break;
            
            case 0x6A: // L:K + C
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + this.reg_c;
                break;
            
            case 0x6B: // L:K + D
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + this.reg_d;
                break;
            
            case 0x6C: // L:K + I
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + this.reg_i;
                break;
            
            case 0x6D: // L:K + J
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + this.reg_j;
                break;
            
            case 0x6E: // L:K + K
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + this.reg_k;
                break;
            
            case 0x6F: // L:K + L
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + this.reg_l;
                break;
            
            case 0x70: // BP + A
                address = this.reg_bp + this.reg_a;
                break;
                
            case 0x71: // BP + B
                address = this.reg_bp + this.reg_b;
                break;
            
            case 0x72: // BP + C
                address = this.reg_bp + this.reg_c;
                break;
                
            case 0x73: // BP + D
                address = this.reg_bp + this.reg_d;
                break;
                
            case 0x74: // BP + I
                address = this.reg_bp + this.reg_i;
                break;
                
            case 0x75: // BP + J
                address = this.reg_bp + this.reg_j;
                break;
            
            case 0x76: // BP + K
                address = this.reg_bp + this.reg_k;
                break;
                
            case 0x77: // BP + L
                address = this.reg_bp + this.reg_l;
                break;
            
            case 0x78: // A
                address = this.reg_a;
                break;
            
            case 0x79: // B
                address = this.reg_b;
                break;
            
            case 0x7A: // C
                address = this.reg_c;
                break;
            
            case 0x7B: // D
                address = this.reg_d;
                break;
            
            case 0x7C: // I
                address = this.reg_i;
                break;
            
            case 0x7D: // J
                address = this.reg_j;
                break;
            
            case 0x7E: // K
                address = this.reg_k;
                break;
            
            case 0x7F: // L
                address = this.reg_l;
                break;
            
            case 0x80: // D:A + 2*A
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + (this.reg_a << 1);
                break;
            
            case 0x81: // D:A + 2*B
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + (this.reg_b << 1);
                break;
            
            case 0x82: // D:A + 2*C
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + (this.reg_c << 1);
                break;
            
            case 0x83: // D:A + 2*D
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + (this.reg_d << 1);
                break;
            
            case 0x84: // D:A + 2*I
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + (this.reg_i << 1);
                break;
            
            case 0x85: // D:A + 2*J
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + (this.reg_j << 1);
                break;
                
            case 0x86: // D:A + 2*K
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + (this.reg_k << 1);
                break;
            
            case 0x87: // D:A + 2*L
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + (this.reg_l << 1);
                break;
            
            case 0x88: // A:B + 2*A
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + (this.reg_a << 1);
                break;
            
            case 0x89: // A:B + 2*B
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + (this.reg_b << 1);
                break;
            
            case 0x8A: // A:B + 2*C
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + (this.reg_c << 1);
                break;
            
            case 0x8B: // A:B + 2*D
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + (this.reg_d << 1);
                break;
            
            case 0x8C: // A:B + 2*I
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + (this.reg_i << 1);
                break;
            
            case 0x8D: // A:B + 2*J
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + (this.reg_j << 1);
                break;
                
            case 0x8E: // A:B + 2*K
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + (this.reg_k << 1);
                break;
            
            case 0x8F: // A:B + 2*L
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + (this.reg_l << 1);
                break;
            
            case 0x90: // B:C + 2*A
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + (this.reg_a << 1);
                break;
            
            case 0x91: // B:C + 2*B
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + (this.reg_b << 1);
                break;
            
            case 0x92: // B:C + 2*C
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + (this.reg_c << 1);
                break;
            
            case 0x93: // B:C + 2*D
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + (this.reg_d << 1);
                break;
            
            case 0x94: // B:C + 2*I
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + (this.reg_i << 1);
                break;
            
            case 0x95: // B:C + 2*J
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + (this.reg_j << 1);
                break;
                
            case 0x96: // B:C + 2*K
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + (this.reg_k << 1);
                break;
            
            case 0x97: // B:C + 2*L
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + (this.reg_l << 1);
                break;
            
            case 0x98: // C:D + 2*A
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + (this.reg_a << 1);
                break;
            
            case 0x99: // C:D + 2*B
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + (this.reg_b << 1);
                break;
            
            case 0x9A: // C:D + 2*C
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + (this.reg_c << 1);
                break;
            
            case 0x9B: // C:D + 2*D
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + (this.reg_d << 1);
                break;
            
            case 0x9C: // C:D + 2*I
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + (this.reg_i << 1);
                break;
            
            case 0x9D: // C:D + 2*J
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + (this.reg_j << 1);
                break;
                
            case 0x9E: // C:D + 2*K
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + (this.reg_k << 1);
                break;
            
            case 0x9F: // C:D + 2*L
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + (this.reg_l << 1);
                break;
            
            case 0xA0: // J:I + 2*A
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + (this.reg_a << 1);
                break;
            
            case 0xA1: // J:I + 2*B
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + (this.reg_b << 1);
                break;
            
            case 0xA2: // J:I + 2*C
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + (this.reg_c << 1);
                break;
            
            case 0xA3: // J:I + 2*D
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + (this.reg_d << 1);
                break;
            
            case 0xA4: // J:I + 2*I
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + (this.reg_i << 1);
                break;
            
            case 0xA5: // J:I + 2*J
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + (this.reg_j << 1);
                break;
                
            case 0xA6: // J:I + 2*K
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + (this.reg_k << 1);
                break;
            
            case 0xA7: // J:I + 2*L
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + (this.reg_l << 1);
                break;
            
            case 0xA8: // L:K + 2*A
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + (this.reg_a << 1);
                break;
            
            case 0xA9: // L:K + 2*B
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + (this.reg_b << 1);
                break;
            
            case 0xAA: // L:K + 2*C
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + (this.reg_c << 1);
                break;
            
            case 0xAB: // L:K + 2*D
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + (this.reg_d << 1);
                break;
            
            case 0xAC: // L:K + 2*I
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + (this.reg_i << 1);
                break;
            
            case 0xAD: // L:K + 2*J
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + (this.reg_j << 1);
                break;
                
            case 0xAE: // L:K + 2*K
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + (this.reg_k << 1);
                break;
            
            case 0xAF: // L:K + 2*L
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + (this.reg_l << 1);
                break;
            
            case 0xB0: // BP + 2*A
                address = this.reg_bp + (this.reg_a << 1);
                break;
            
            case 0xB1: // BP + 2*B
                address = this.reg_bp + (this.reg_b << 1);
                break;
                
            case 0xB2: // BP + 2*C
                address = this.reg_bp + (this.reg_c << 1);
                break;
                
            case 0xB3: // BP + 2*D
                address = this.reg_bp + (this.reg_d << 1);
                break;
                
            case 0xB4: // BP + 2*I
                address = this.reg_bp + (this.reg_i << 1);
                break;
            
            case 0xB5: // BP + 2*J
                address = this.reg_bp + (this.reg_j << 1);
                break;
                
            case 0xB6: // BP + 2*K
                address = this.reg_bp + (this.reg_k << 1);
                break;
                
            case 0xB7: // BP + 2*L
                address = this.reg_bp + (this.reg_l << 1);
                break;
            
            case 0xB8: // 2*A
                address = this.reg_a << 1;
                break;
            
            case 0xB9: // 2*B
                address = this.reg_b << 1;
                break;
                
            case 0xBA: // 2*C
                address = this.reg_c << 1;
                break;
                
            case 0xBB: // 2*D
                address = this.reg_d << 1;
                break;
                
            case 0xBC: // 2*I
                address = this.reg_i << 1;
                break;
                
            case 0xBD: // 2*J
                address = this.reg_j << 1;
                break;
                
            case 0xBE: // 2*K
                address = this.reg_k << 1;
                break;
                
            case 0xBF: // 2*L
                address = this.reg_l << 1;
                break;
            
            case 0xC0: // D:A + 4*A
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + (this.reg_a << 2);
                break;
            
            case 0xC1: // D:A + 4*B
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + (this.reg_b << 2);
                break;
            
            case 0xC2: // D:A + 4*C
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + (this.reg_c << 2);
                break;
            
            case 0xC3: // D:A + 4*D
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + (this.reg_d << 2);
                break;
            
            case 0xC4: // D:A + 4*I
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + (this.reg_i << 2);
                break;
            
            case 0xC5: // D:A + 4*J
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + (this.reg_j << 2);
                break;
                
            case 0xC6: // D:A + 4*K
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + (this.reg_k << 2);
                break;
            
            case 0xC7: // D:A + 4*L
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + (this.reg_l << 2);
                break;
            
            case 0xC8: // A:B + 4*A
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + (this.reg_a << 2);
                break;
            
            case 0xC9: // A:B + 4*B
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + (this.reg_b << 2);
                break;
            
            case 0xCA: // A:B + 4*C
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + (this.reg_c << 2);
                break;
            
            case 0xCB: // A:B + 4*D
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + (this.reg_d << 2);
                break;
            
            case 0xCC: // A:B + 4*I
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + (this.reg_i << 2);
                break;
            
            case 0xCD: // A:B + 4*J
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + (this.reg_j << 2);
                break;
                
            case 0xCE: // A:B + 4*K
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + (this.reg_k << 2);
                break;
            
            case 0xCF: // A:B + 4*L
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + (this.reg_l << 2);
                break;
            
            case 0xD0: // B:C + 4*A
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + (this.reg_a << 2);
                break;
            
            case 0xD1: // B:C + 4*B
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + (this.reg_b << 2);
                break;
            
            case 0xD2: // B:C + 4*C
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + (this.reg_c << 2);
                break;
            
            case 0xD3: // B:C + 4*D
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + (this.reg_d << 2);
                break;
            
            case 0xD4: // B:C + 4*I
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + (this.reg_i << 2);
                break;
            
            case 0xD5: // B:C + 4*J
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + (this.reg_j << 2);
                break;
                
            case 0xD6: // B:C + 4*K
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + (this.reg_k << 2);
                break;
            
            case 0xD7: // B:C + 4*L
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + (this.reg_l << 2);
                break;
            
            case 0xD8: // C:D + 4*A
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + (this.reg_a << 2);
                break;
            
            case 0xD9: // C:D + 4*B
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + (this.reg_b << 2);
                break;
            
            case 0xDA: // C:D + 4*C
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + (this.reg_c << 2);
                break;
            
            case 0xDB: // C:D + 4*D
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + (this.reg_d << 2);
                break;
                
            case 0xDC: // C:D + 4*I
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + (this.reg_i << 2);
                break;
            
            case 0xDD: // C:D + 4*J
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + (this.reg_j << 2);
                break;
                
            case 0xDE: // C:D + 4*K
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + (this.reg_k << 2);
                break;
            
            case 0xDF: // C:D + 4*L
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + (this.reg_l << 2);
                break;
            
            case 0xE0: // J:I + 4*A
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + (this.reg_a << 2);
                break;
            
            case 0xE1: // J:I + 4*B
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + (this.reg_b << 2);
                break;
            
            case 0xE2: // J:I + 4*C
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + (this.reg_c << 2);
                break;
            
            case 0xE3: // J:I + 4*D
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + (this.reg_d << 2);
                break;
            
            case 0xE4: // J:I + 4*I
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + (this.reg_i << 2);
                break;
            
            case 0xE5: // J:I + 4*J
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + (this.reg_j << 2);
                break;
                
            case 0xE6: // J:I + 4*K
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + (this.reg_k << 2);
                break;
            
            case 0xE7: // J:I + 4*L
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + (this.reg_l << 2);
                break;
            
            case 0xE8: // L:K + 4*A
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + (this.reg_a << 2);
                break;
            
            case 0xE9: // L:K + 4*B
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + (this.reg_b << 2);
                break;
            
            case 0xEA: // L:K + 4*C
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + (this.reg_c << 2);
                break;
            
            case 0xEB: // L:K + 4*D
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + (this.reg_d << 2);
                break;
            
            case 0xEC: // L:K + 4*I
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + (this.reg_i << 2);
                break;
            
            case 0xED: // L:K + 4*J
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + (this.reg_j << 2);
                break;
                
            case 0xEE: // L:K + 4*K
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + (this.reg_k << 2);
                break;
            
            case 0xEF: // L:K + 4*L
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + (this.reg_l << 2);
                break;
            
            case 0xF0: // BP + 4*A
                address = this.reg_bp + (this.reg_a << 2);
                break;
            
            case 0xF1: // BP + 4*B
                address = this.reg_bp + (this.reg_b << 2);
                break;
                
            case 0xF2: // BP + 4*C
                address = this.reg_bp + (this.reg_c << 2);
                break;
                
            case 0xF3: // BP + 4*D
                address = this.reg_bp + (this.reg_d << 2);
                break;
                
            case 0xF4: // BP + 4*I
                address = this.reg_bp + (this.reg_i << 2);
                break;
            
            case 0xF5: // BP + 4*J
                address = this.reg_bp + (this.reg_j << 2);
                break;
                
            case 0xF6: // BP + 4*K
                address = this.reg_bp + (this.reg_k << 2);
                break;
                
            case 0xF7: // BP + 4*L
                address = this.reg_bp + (this.reg_l << 2);
                break;
            
            case 0xF8: // 4*A
                address = this.reg_a << 2;
                break;
            
            case 0xF9: // 4*B
                address = this.reg_b << 2;
                break;
                
            case 0xFA: // 4*C
                address = this.reg_c << 2;
                break;
                
            case 0xFB: // 4*D
                address = this.reg_d << 2;
                break;
                
            case 0xFC: // 4*I
                address = this.reg_i << 2;
                break;
                
            case 0xFD: // 4*J
                address = this.reg_j << 2;
                break;
                
            case 0xFE: // 4*K
                address = this.reg_k << 2;
                break;
                
            case 0xFF: // 4*L
                address = this.reg_l << 2;
                break;
        }
        
        // offset
        if(hasOffset) {
            int immOffs = hasRIM ? 3 : 2;
            
            address += switch(immediateWidth) {
                case 1  -> this.fetchBuffer[immOffs];
                case 2  -> get2FetchBytes(immOffs);
                case 3  -> get3FetchBytes(immOffs);
                case 4  -> get4FetchBytes(immOffs);
                default -> 0;
            };
            
            /*
            long immAddr = this.reg_ip + (hasRIM ? 2 : 1);
            
            address += switch(immediateWidth) {
                case 1  -> this.memory.readByte(immAddr);
                case 2  -> this.memory.read2Bytes(immAddr);
                case 3  -> this.memory.read3Bytes(immAddr);
                case 4  -> this.memory.read4Bytes(immAddr);
                default -> 0;
            };
            */
            
            desc.hasImmediateAddress = true;
            desc.immediateWidth = immediateWidth;
        }
        
        //System.out.printf("%08X%n", address);
        return address;
    }
    
    /**
     * Gets the address from BIO (old computed version)
     * 
     * @param desc
     * @param hasRIM
     * @param hasOffset
     * @return
     */
    /*
    private int getBIOAddressOld(InstructionDescriptor desc, boolean hasRIM, boolean hasOffset) {
        byte bio = this.memory.readByte(this.reg_ip + (hasRIM ? 1 : 0)),
             base = (byte)((bio >> 3) & 0x07),
             index = (byte)(bio & 0x07),
             scale = (byte)((bio >> 6) & 0x03),
             offsetSize = 4;
        
        boolean hasIndex = scale != 0,
                ipRelative = false;
        
        long addr = 0;
        
        desc.hasBIOByte = true;
        
        // index
        if(hasIndex) {
            // this sign extends btw
            addr += switch(index) {
                case 0  -> this.reg_a;
                case 1  -> this.reg_b;
                case 2  -> this.reg_c;
                case 3  -> this.reg_d;
                case 4  -> this.reg_i;
                case 5  -> this.reg_j;
                case 6  -> this.reg_k;
                case 7  -> this.reg_l;
                default -> -1;
            };
            
            addr <<= (scale - 1);
        } else {
            offsetSize = (byte)((index & 0x03) + 1);
            
            // IP relative
            if((index & 4) != 0) {
                ipRelative = true;
                addr += this.reg_ip;
                
                // locate next instruction
                if(desc.hasRIMByte) addr++;     // rim
                addr++; // bio
                if(hasOffset) addr += offsetSize;
                if(desc.hasRIMI8) addr++; // instructions add to immediateWidth later
                
                // base as index
                addr += switch(base) {
                    case 0, 1, 2, 3 -> 0;
                    case 4          -> this.reg_i;
                    case 5          -> this.reg_j;
                    case 6          -> this.reg_k;
                    case 7          -> this.reg_l;
                    default         -> -1;
                };
            }
        }
        
        // base
        if(!ipRelative) {
            addr += switch(base) {
                case 0  -> (this.reg_d << 16) | (this.reg_a & 0xFFFF);  // D:A
                case 1  -> (this.reg_a << 16) | (this.reg_b & 0xFFFF);  // A:B
                case 2  -> (this.reg_b << 16) | (this.reg_c & 0xFFFF);  // B:C
                case 3  -> (this.reg_c << 16) | (this.reg_d & 0xFFFF);  // C:D
                case 4  -> (this.reg_j << 16) | (this.reg_i & 0xFFFF);  // J:I
                case 5  -> (this.reg_l << 16) | (this.reg_k & 0xFFFF);  // L:K
                case 6  -> this.reg_bp;
                case 7  -> hasIndex ? 0 : this.reg_sp;
                default -> -1;
            };
        }
        
        // offset
        if(hasOffset) {
            long immAddr = this.reg_ip + (hasRIM ? 2 : 1);
            
            addr += switch(offsetSize) {
                case 1  -> this.memory.readByte(immAddr);
                case 2  -> this.memory.read2Bytes(immAddr);
                case 3  -> this.memory.read3Bytes(immAddr);
                case 4  -> this.memory.read4Bytes(immAddr);
                default -> 0;
            };
            
            desc.hasImmediateAddress = true;
            desc.immediateWidth = offsetSize;
        }
        
        return (int) addr;
    }*/
    
    /**
     * Gets a 16 bit value from the fetch buffer
     * 
     * @param offset
     * @return
     */
    private short get2FetchBytes(int offset) {
        return (short)
               ((this.fetchBuffer[offset] & 0xFF) |
               (this.fetchBuffer[offset + 1] << 8));
    }
    
    /**
     * Gets a 24 bit value from the fetch buffer
     * 
     * @param offset
     * @return
     */
    private int get3FetchBytes(int offset) {
        return (this.fetchBuffer[offset] & 0xFF) |
               ((this.fetchBuffer[offset + 1] & 0xFF) << 8) |
               (this.fetchBuffer[offset + 2] << 16);
    }
    
    /**
     * Gets a 32 bit value from the fetch buffer
     * 
     * @param offset
     * @return
     */
    private int get4FetchBytes(int offset) {
        return (this.fetchBuffer[offset] & 0xFF) |
               ((this.fetchBuffer[offset + 1] & 0xFF) << 8) |
               ((this.fetchBuffer[offset + 2] & 0xFF) << 16) |
               (this.fetchBuffer[offset + 3] << 24);
    }
    
    /**
     * Gets the I8 from a _RIM_I8 instruction
     * @param desc
     * @return
     */
    private byte getRIMI8(InstructionDescriptor desc) {
        // figure out where our immediate is
        int offset = 2;                 // rim
        if(desc.hasBIOByte) offset++;   // bio
        if(desc.hasImmediateAddress || desc.hasImmediateValue) offset += desc.immediateWidth; // immediate
        
        desc.hasImmediateValue = true;
        desc.immediateWidth++; // our immediate
        
        return this.fetchBuffer[offset];
    }
    
    /**
     * Updates the instruction pointer based on the InstructionDescriptor
     * 
     * @param desc
     */
    private void updateIP(InstructionDescriptor desc) {
        // RIM byte
        if(desc.hasRIMByte) {
            this.reg_ip += 1;
        }
        
        // BIO byte
        if(desc.hasBIOByte) {
            this.reg_ip += 1;
        }
        
        // immediates
        if(desc.hasImmediateAddress || desc.hasImmediateValue) {
            this.reg_ip += desc.immediateWidth;
        }
    }
    
    /**
     * Fires an interrupt with the given vector. May be ignored.
     * 
     * @param vector
     * @return true if the interrupt was not masked
     */
    public boolean fireMaskableInterrupt(byte vector) {
        if(this.pf_ie) {
            this.halted = false;
            this.externalInterrupt = true;
            this.externalInterruptVector = vector;
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Fires an interrupt with the given vector. Cannot be ignored.
     * 
     * @param vector
     */
    public void fireNonMaskableInterrupt(byte vector) {
        this.halted = false;
        this.externalInterrupt = true;
        this.externalInterruptVector = vector;
    }
    
    /*
     * getty bois
     */
    public short getRegA() { return this.reg_a; }
    public short getRegB() { return this.reg_b; }
    public short getRegC() { return this.reg_c; }
    public short getRegD() { return this.reg_d; }
    public short getRegI() { return this.reg_i; }
    public short getRegJ() { return this.reg_j; }
    public short getRegK() { return this.reg_k; }
    public short getRegL() { return this.reg_l; }
    public short getRegF() { return this.reg_f; }
    public int getRegIP() { return this.reg_ip; }
    public int getRegBP() { return this.reg_bp; }
    public int getRegSP() { return this.reg_sp; }
    public boolean getHalted() { return this.halted; }
    public boolean hasPendingInterrupt() { return this.externalInterrupt; }
    public byte getPendingInterruptVector() { return this.externalInterruptVector; }
    
    public short getRegPF() {
        return (short)(
            (this.pf_ie ? 0x01 : 0x00) |
            (this.pf_pv ? 0x02 : 0x00)
        );
    }
    
    /*
     * setty bois
     */
    public void setRegA(short a) { this.reg_a = a; }
    public void setRegB(short b) { this.reg_b = b; }
    public void setRegC(short c) { this.reg_c = c; }
    public void setRegD(short d) { this.reg_d = d; }
    public void setRegI(short i) { this.reg_i = i; }
    public void setRegJ(short j) { this.reg_j = j; }
    public void setRegK(short k) { this.reg_k = k; }
    public void setRegL(short l) { this.reg_l = l; }
    public void setRegF(short f) { this.reg_f = f; }
    public void setRegIP(int ip) { this.reg_ip = ip; }
    public void setRegBP(int bp) { this.reg_bp = bp; }
    public void setRegSP(int sp) { this.reg_sp = sp; }
    public void setHalted(boolean h) { this.halted = h; }
    
    public void setRegPF(short pf) {
        this.pf_ie = (pf & 0x01) != 0;
        this.pf_pv = (pf & 0x02) != 0;
    }
}
