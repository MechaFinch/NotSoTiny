package notsotiny.sim;

import notsotiny.sim.LocationDescriptor.LocationType;
import notsotiny.sim.memory.MemoryController;
import notsotiny.sim.ops.Family;
import notsotiny.sim.ops.Opcode;
import notsotiny.sim.ops.Operation;

/**
 * Simulates the NotSoTiny architecture
 * 
 * @author Mechafinch
 */
public class NotSoTinySimulator {
    
    private MemoryController memory;
    
    // 16 bit
    private short reg_a,
                  reg_b,
                  reg_c,
                  reg_d,
                  reg_i,
                  reg_j,
                  reg_k,
                  reg_l,
                  reg_f,
                  reg_pf;
    
    // 32 bit
    private int reg_ip,
                reg_sp,
                reg_bp;
    
    private boolean halted,
                    externalInterrupt;
    
    private byte externalInterruptVector;
    
    /**
     * Start the simulator at the given address
     * 
     * @param memory
     * @param entry
     */
    public NotSoTinySimulator(MemoryController memory, int entry) {
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
        this.reg_pf = 1; // interrupts enabled by default
        this.halted = false;
        this.externalInterrupt = false;
        this.externalInterruptVector = 0;
    }
    
    /**
     * Start the simulator
     * 
     * @param memory
     */
    public NotSoTinySimulator(MemoryController memory) {
        this(memory, memory.read4Bytes(0));
    }
    
    /**
     * Run a step
     * 
     * Opcodes are sent to family handlers based on their category. These family handlers then send
     * opcodes off to operation-specific methods. All for organization.
     */
    public synchronized void step() {
        
        if(this.externalInterrupt) {
            this.externalInterrupt = false;
            runInterrupt(this.externalInterruptVector);
            return;
        }
        
        // TODO
        // memory manager may be slow. consider an instruction cache
        
        InstructionDescriptor desc = new InstructionDescriptor();
        desc.op = Opcode.fromOp(this.memory.readByte(this.reg_ip++));
        
        if(desc.op == Opcode.NOP) return; 
        
        switch(desc.op) {
            case CMP_RIM_I8, ADD_RIM_I8, ADC_RIM_I8, SUB_RIM_I8, SBB_RIM_I8:
                desc.hasRIMI8 = true;
                break;
            
            default:
                break;
        }
        
        //System.out.println(desc.op.getType().getFamily()); 
        
        switch(desc.op.getType().getFamily()) {
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
        if(!(desc.op.getType().getFamily() == Family.JUMP && desc.op.getType() != Operation.CMP)) {
            updateIP(desc);
        }
    }
    
    /**
     * Interprets MISC family instructions
     * 
     * @param op
     */
    private void stepMiscFamily(InstructionDescriptor desc) {
        //System.out.println(desc.op.getType());
        
        switch(desc.op.getType()) {
            case HLT:
                this.halted = true;
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
     */
    private void stepLogicFamily(InstructionDescriptor desc) {
        //System.out.println(desc.op.getType());
        
        switch(desc.op.getType()) {
            case AND:
            case OR:
            case XOR:
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
     */
    private void run2Logic(InstructionDescriptor desc) {
        // operands
        LocationDescriptor dst = switch(desc.op) {
            case AND_F_RIM, OR_F_RIM, XOR_F_RIM -> LocationDescriptor.REGISTER_F;
            default                             -> getNormalRIMDestinationDescriptor(desc);
        };
        
        int b = switch(desc.op) {
            case AND_RIM_F, OR_RIM_F, XOR_RIM_F -> this.reg_f;
            default                             -> getNormalRIMSource(desc);
        };
        
        // operation
        int c = switch(desc.op.getType()) {
            case AND    -> readLocation(dst) & b;
            case OR     -> readLocation(dst) | b;
            case XOR    -> readLocation(dst) ^ b;
            default -> 0;
        };
        
        // flags
        switch(desc.op) {
            case AND_F_RIM, AND_RIM_F, OR_F_RIM, OR_RIM_F, XOR_F_RIM, XOR_RIM_F:
                break;
            
            default:
                boolean zero = c == 0,
                        overflow = false,
                        sign = switch(dst.size()) {
                            case 1  -> (c & 0x80) != 0;
                            case 2  -> (c & 0x8000) != 0;
                            case 4  -> (c & 0x8000_0000) != 0;
                            default -> false;
                        },
                        carry = false;
                        
                this.reg_f = (short)((zero ? 0x08 : 0x00) | (overflow ? 0x04 : 0x00) | (sign ? 0x02 : 0x00) | (carry ? 0x01 : 0x00));
        }
        
        // write
        writeLocation(dst, c);
    }
    
    /**
     * Executes 1-input logic instructions NOT and NEG
     * 
     * @param op
     */
    private void run1Logic(InstructionDescriptor desc) {
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
     */
    private void runRotateLogic(InstructionDescriptor desc) {
        LocationDescriptor dst = getNormalRIMDestinationDescriptor(desc);
        
        int a = readLocation(dst),
            b = getNormalRIMSource(desc),
            c = 0;
        
        boolean carry = (this.reg_f & 0x01) != 0;
        
        // agh
        switch(desc.op) {
            case SHL_RIM:
                carry = ((switch(dst.size()) {
                    case 1  -> 0x80;
                    case 2  -> 0x8000;
                    case 4  -> 0x8000_000;
                    default -> 0;
                } >>> (b - 1)) & a) != 0;
                
                c = a << b;
                break;
            
            case SHR_RIM:
                carry = ((1 << (b - 1)) & a) != 0;
                
                c = a >>> b;
                break;
                
            case SAR_RIM:
                carry = ((1 << (b - 1)) & a) != 0;
                
                c = a >> b;
                break;
                
            case ROL_RIM:
            case RCL_RIM:
                long al = a;
                int rot = 0;
                for(int i = 0; i < b; i++) {
                    // use previous carry for RCL
                    if(desc.op == Opcode.RCL_RIM) rot = carry ? 1 : 0;
                    
                    al <<= 1;
                    carry = (switch(dst.size()) {
                        case 1  -> 0x100l;
                        case 2  -> 0x1_0000l;
                        case 4  -> 0x1_0000_0000l;
                        default -> 0l;
                    } & al) != 0;
                    
                    // use current carry for ROL
                    if(desc.op == Opcode.ROL_RIM) rot = carry ? 1 : 0;
                    
                    al |= rot;
                }
                
                c = (int) al;
                break;
                
            case ROR_RIM:
            case RCR_RIM:
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
                    if(desc.op == Opcode.RCR_RIM) rot = carry ? 1 : 0;
                    
                    carry = (al & 1) != 0;
                    al >>>= 1;
                    
                    // use current carry for ROR
                    if(desc.op == Opcode.ROR_RIM) rot = carry ? 1 : 0;
                    
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
     */
    private void stepMultiplicationFamily(InstructionDescriptor desc) {
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
     */
    private void runMUL(InstructionDescriptor desc) {
        LocationDescriptor thinDst = getNormalRIMDestinationDescriptor(desc);
        
        int a = getNormalRIMSource(desc),
            b = readLocation(thinDst);
        
        boolean high = desc.op == Opcode.MULH_RIM || desc.op == Opcode.MULSH_RIM,
                signed = desc.op == Opcode.MULSH_RIM;
        
        int c = multiply(a, b, thinDst.size(), high, signed);
        
        if(high) {
            putWideRIMDestination(thinDst, c);
        } else {
            writeLocation(thinDst, c);
        }
    }
    
    /**
     * Executes PMUL, PMULH, and PMULSH instructions
     * 
     * @param op
     */
    private void runPMUL(InstructionDescriptor desc) {
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
     */
    private void runDIV(InstructionDescriptor desc) {
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
     */
    private void runPDIV(InstructionDescriptor desc) {
        LocationDescriptor thinDst = getNormalRIMDestinationDescriptor(desc),
                           src = getNormalRIMSourceDescriptor(desc);
        
        boolean mod = desc.op == Opcode.PDIVM_RIMP || desc.op == Opcode.PDIVMS_RIMP,
                signed = desc.op == Opcode.PDIVS_RIMP || desc.op == Opcode.PDIVMS_RIMP;
        
        int a = mod ? getWidePackedRIMSource(thinDst) : getPackedRIMSource(thinDst, desc),
            b = getPackedRIMSource(src, desc);
        
        // immedaites are always 2 bytes for packed
        if(desc.hasImmediateValue) {
            desc.immediateWidth = 2;
        }
        
        int[] res = dividePacked(a, b, thinDst.size() != 1, mod, signed);
        
        int c = mod ? (((res[1] & 0xFFFF) << 16) | (res[0] & 0xFFFF)) : res[0];
        
        if(mod) {
            putWideRIMDestination(thinDst, c);
        } else {
            putPackedRIMDestination(thinDst, c);
        }
    }
    
    /**
     * Interpret an ADDITION family instruction
     * 
     * @param op
     */
    private void stepAdditionFamily(InstructionDescriptor desc) {
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
     */
    private void runINC(InstructionDescriptor desc) {
        LocationDescriptor dst = switch(desc.op) {
            case INC_I, ICC_I, DEC_I, DCC_I -> LocationDescriptor.REGISTER_I;
            case INC_J, ICC_J, DEC_J, DCC_J -> LocationDescriptor.REGISTER_J;
            case INC_K, ICC_K, DEC_K, DCC_K -> LocationDescriptor.REGISTER_K;
            case INC_L, ICC_L, DEC_L, DCC_L -> LocationDescriptor.REGISTER_L;
            default                         -> getNormalRIMDestinationDescriptor(desc);
        };
        
        // unconditional or condition met
        if(desc.op.getType() == Operation.INC || desc.op.getType() == Operation.DEC || (this.reg_f & 0x01) != 0) {
            boolean inc = desc.op.getType() == Operation.INC || desc.op.getType() == Operation.ICC;
            int v = add(readLocation(dst), 1, dst.size(), !inc, false);
            
            writeLocation(dst, v);
        }
    }
    
    /**
     * Executes PINC, PICC, PDEC, and PDCC instructions
     * 
     * @param op
     */
    private void runPINC(InstructionDescriptor desc) {
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
     */
    private void runADD(InstructionDescriptor desc) {
        LocationDescriptor dst = switch(desc.op) {
            case ADD_A_I8, ADC_A_I8, ADD_A_I16, ADC_A_I16, SUB_A_I8, SBB_A_I8, SUB_A_I16, SBB_A_I16 -> LocationDescriptor.REGISTER_A;
            case ADD_B_I8, ADC_B_I8, ADD_B_I16, ADC_B_I16, SUB_B_I8, SBB_B_I8, SUB_B_I16, SBB_B_I16 -> LocationDescriptor.REGISTER_B;
            case ADD_C_I8, ADC_C_I8, ADD_C_I16, ADC_C_I16, SUB_C_I8, SBB_C_I8, SUB_C_I16, SBB_C_I16 -> LocationDescriptor.REGISTER_C;
            case ADD_D_I8, ADC_D_I8, ADD_D_I16, ADC_D_I16, SUB_D_I8, SBB_D_I8, SUB_D_I16, SBB_D_I16 -> LocationDescriptor.REGISTER_D;
            case ADD_SP_I8, SUB_SP_I8                                                               -> LocationDescriptor.REGISTER_SP;
            default                                                                                 -> getNormalRIMDestinationDescriptor(desc);
        };
        
        int b = switch(desc.op) {
            case ADD_A_I8, ADD_B_I8, ADD_C_I8, ADD_D_I8, ADC_A_I8, ADC_B_I8, ADC_C_I8, ADC_D_I8,
                 SUB_A_I8, SUB_B_I8, SUB_C_I8, SUB_D_I8, SBB_A_I8, SBB_B_I8, SBB_C_I8, SBB_D_I8,
                 ADD_SP_I8, SUB_SP_I8 -> {
                     desc.hasImmediateValue = true;
                     desc.immediateWidth = 1;
                     yield this.memory.readByte(this.reg_ip);
                 }
                 
            case ADD_RIM_I8, ADC_RIM_I8, SUB_RIM_I8, SBB_RIM_I8 -> {
                // figure out where our immediate is
                int offset = 1;                 // rim
                if(desc.hasBIOByte) offset++;   // bio
                if(desc.hasImmediateAddress || desc.hasImmediateValue) offset += desc.immediateWidth; // immediate
                
                desc.hasImmediateValue = true;
                desc.immediateWidth++; // our immediate
                
                yield this.memory.readByte(this.reg_ip + offset);
            }
                 
            case ADD_A_I16, ADD_B_I16, ADD_C_I16, ADD_D_I16, ADC_A_I16, ADC_B_I16, ADC_C_I16, ADC_D_I16,
                 SUB_A_I16, SUB_B_I16, SUB_C_I16, SUB_D_I16, SBB_A_I16, SBB_B_I16, SBB_C_I16, SBB_D_I16 -> {
                     desc.hasImmediateValue = true;
                     desc.immediateWidth = 2;
                     yield this.memory.read2Bytes(this.reg_ip);
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
     */
    private void runPADD(InstructionDescriptor desc) {
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
     */
    private void stepJumpFamily(InstructionDescriptor desc) {
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
            
            case LEA:
                runLEA(desc);
                break;
            
            case CMP:
                runCMP(desc);
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
     */
    private void runJMP(InstructionDescriptor desc) {
        // get value
        int val = 0;
        
        switch(desc.op) {
            case JMP_I8:
                desc.hasImmediateValue = true;
                desc.immediateWidth = 1;
                val = this.memory.readByte(this.reg_ip);
                break;
            
            case JMP_I16:
                desc.hasImmediateValue = true;
                desc.immediateWidth = 2;
                val = this.memory.read2Bytes(this.reg_ip);
                break;
            
            case JMP_I32:
            case JMPA_I32:
                desc.hasImmediateValue = true;
                desc.immediateWidth = 4;
                val = this.memory.read4Bytes(this.reg_ip);
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
     */
    private void runCALL(InstructionDescriptor desc) {
        int target = 0;
        
        switch(desc.op) {
            case CALL_I16:
                desc.hasImmediateValue = true;
                desc.immediateWidth = 2;
                target = this.memory.read2Bytes(this.reg_ip);
                break;
            
            case CALLA_I32:
                desc.hasImmediateValue = true;
                desc.immediateWidth = 4;
                target = this.memory.read4Bytes(this.reg_ip);
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
        this.memory.write4Bytes(this.reg_sp, this.reg_ip);
        
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
     */
    private void runRET(InstructionDescriptor desc) {
        if(desc.op == Opcode.IRET) {
            // IRET also pops flags
            this.reg_pf = this.memory.read2Bytes(this.reg_sp);
            this.reg_f = this.memory.read2Bytes(this.reg_sp + 2);
            this.reg_sp += 4;
        }
        
        // normal RET
        this.reg_ip = this.memory.read4Bytes(this.reg_sp);
        this.reg_sp += 4;
    }
    
    /**
     * Executes INT instructions
     * 
     * @param op
     */
    private void runINT(InstructionDescriptor desc) {
        byte b;
        
        // vector
        if(desc.op == Opcode.INT_I8) {
            b = this.memory.readByte(this.reg_ip);
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
     * Executes an interrupt, whether internal or external
     * 
     * @param num
     */
    private void runInterrupt(byte num) {
        // push IP, flags, pflags
        this.reg_sp -= 8;
        this.memory.write4Bytes(this.reg_sp + 4, this.reg_ip);
        this.memory.write2Bytes(this.reg_sp + 2, this.reg_f);
        this.memory.write2Bytes(this.reg_sp, this.reg_pf);
        
        // get vector & jump
        this.reg_ip = this.memory.read4Bytes(((long) num) << 2);
    }
    
    /**
     * Executes LEA instructions
     * 
     * @param op
     */
    private void runLEA(InstructionDescriptor desc) {
        // souper simple
        putNormalRIMDestination(desc, getNormalRIMSourceDescriptor(desc).address());
    }
    
    /**
     * Executes CMP instructions
     * 
     * @param op
     */
    private void runCMP(InstructionDescriptor desc) {
        LocationDescriptor dest = getNormalRIMDestinationDescriptor(desc);
        
        int a = readLocation(dest);
                
        int b = switch(desc.op) {
            case CMP_RIM    -> getNormalRIMSource(desc);
            case CMP_RIM_I8 -> {
                // figure out where our immediate is
                int offset = 1;                 // rim
                if(desc.hasBIOByte) offset++;   // bio
                if(desc.hasImmediateAddress || desc.hasImmediateValue) offset += desc.immediateWidth; // immediate
                
                desc.hasImmediateValue = true;
                desc.immediateWidth++; // our immediate
                
                yield this.memory.readByte(this.reg_ip + offset);
            }
            default -> 0; // also CMP_RIM_0
        };
        
        // subtract and discard
        add(a, b, dest.size(), true, false);
    }
    
    /**
     * Executes JCC instructions
     * 
     * @param op
     */
    private void runJCC(InstructionDescriptor desc) {
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
                offset = this.memory.readByte(this.reg_ip);
                break;
            
            // rim
            default:
                offset = getNormalRIMSource(desc);
        }
        
        // check condition
        boolean condition = switch(desc.op) {
            case JC_I8, JC_RIM      -> (this.reg_f & 0x01) != 0;    // carry - carry set
            case JNC_I8, JNC_RIM    -> (this.reg_f & 0x01) == 0;    // not carry - carry clear
            case JS_I8, JS_RIM      -> (this.reg_f & 0x02) != 0;    // sign - sign set
            case JNS_I8, JNS_RIM    -> (this.reg_f & 0x02) == 0;    // not sign - sign clear
            case JO_I8, JO_RIM      -> (this.reg_f & 0x04) != 0;    // overflow - overflow set
            case JNO_I8, JNO_RIM    -> (this.reg_f & 0x04) == 0;    // not overflow - overflow clear
            case JZ_I8, JZ_RIM      -> (this.reg_f & 0x08) != 0;    // zero - zero set
            case JNZ_I8, JNZ_RIM    -> (this.reg_f & 0x08) == 0;    // not zero - zero clear
            case JA_I8, JA_RIM      -> (this.reg_f & 0x09) == 0;    // above - carry clear and zero clear 
            case JBE_I8, JBE_RIM    -> (this.reg_f & 0x09) != 0;    // below equal - carry set or zero set
            case JG_I8, JG_RIM      -> ((this.reg_f & 0x08) == 0) && (((this.reg_f & 0x04) >>> 1) == (this.reg_f & 0x02));   // greater - zero clear and sign = overflow
            case JGE_I8, JGE_RIM    -> (((this.reg_f & 0x04) >>> 1) == (this.reg_f & 0x02));                                 // greater equal - sign = overflow
            case JL_I8, JL_RIM      -> (((this.reg_f & 0x04) >>> 1) != (this.reg_f & 0x02));                                 // less - sign != overflow
            case JLE_I8, JLE_RIM    -> ((this.reg_f & 0x08) != 0) || (((this.reg_f & 0x04) >>> 1) != (this.reg_f & 0x02));   // less equal - zero set or sign != overflow
            default                 -> false;
        };
        
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
     */
    private void stepMoveFamily(InstructionDescriptor desc) {
        //System.out.println(desc.op.getType());
        
        switch(desc.op.getType()) {
            case MOV:
            case MOVW:
            case MOVS:
            case MOVZ:
                runMOV(desc);
                break;
            
            case XCHG:
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
     */
    private void runMOV(InstructionDescriptor desc) {
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
                
            case MOV_AL_BL:
                this.reg_a = (short)((this.reg_a & 0xFF00) | (this.reg_b & 0x00FF));
                return;
                
            case MOV_AL_CL:
                this.reg_a = (short)((this.reg_a & 0xFF00) | (this.reg_c & 0x00FF));
                return;
                
            case MOV_AL_DL:
                this.reg_a = (short)((this.reg_a & 0xFF00) | (this.reg_d & 0x00FF));
                return;
                
            case MOV_BL_AL:
                this.reg_b = (short)((this.reg_b & 0xFF00) | (this.reg_a & 0x00FF));
                return;
                
            case MOV_BL_CL:
                this.reg_b = (short)((this.reg_b & 0xFF00) | (this.reg_c & 0x00FF));
                return;
                
            case MOV_BL_DL:
                this.reg_b = (short)((this.reg_b & 0xFF00) | (this.reg_d & 0x00FF));
                return;
                
            case MOV_CL_AL:
                this.reg_c = (short)((this.reg_c & 0xFF00) | (this.reg_a & 0x00FF));
                return;
                
            case MOV_CL_BL:
                this.reg_c = (short)((this.reg_c & 0xFF00) | (this.reg_b & 0x00FF));
                return;
                
            case MOV_CL_DL:
                this.reg_c = (short)((this.reg_c & 0xFF00) | (this.reg_d & 0x00FF));
                return;
                
            case MOV_DL_AL:
                this.reg_d = (short)((this.reg_d & 0xFF00) | (this.reg_a & 0x00FF));
                return;
                
            case MOV_DL_BL:
                this.reg_d = (short)((this.reg_d & 0xFF00) | (this.reg_b & 0x00FF));
                return;
                
            case MOV_DL_CL:
                this.reg_d = (short)((this.reg_d & 0xFF00) | (this.reg_c & 0x00FF));
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
                src = this.memory.readByte(this.reg_ip);
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
                src = this.memory.read2Bytes(this.reg_ip);
                break;
            
            // immediate 32 moves
            case MOV_SP_I32:
            case MOV_BP_I32:
                desc.hasImmediateValue = true;
                desc.immediateWidth = 4;
                src = this.memory.read4Bytes(this.reg_ip);
                break;
            
            // offset
            case MOV_A_O:
            case MOV_B_O:
            case MOV_C_O:
            case MOV_D_O:
                desc.hasImmediateAddress = true;
                desc.immediateWidth = 4;
                src = this.memory.read2Bytes(this.memory.read4Bytes(this.reg_ip));
                break;
            
            // BIO w/o offset
            case MOV_A_BI:
            case MOV_B_BI:
            case MOV_C_BI:
            case MOV_D_BI:
                src = this.memory.read2Bytes(getBIOAddress(desc, false, false));
                break;
            
            // BIO with offset
            case MOV_A_BIO:
            case MOV_B_BIO:
            case MOV_C_BIO:
            case MOV_D_BIO:
                src = this.memory.read2Bytes(getBIOAddress(desc, false, true));
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
                src = this.reg_pf;
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
            
            case MOV_BP_I32:
                this.reg_bp = src;
                return;
            
            case MOV_SP_I32:
                this.reg_sp = src;
                return;
            
            // offset
            case MOV_O_A:
            case MOV_O_B:
            case MOV_O_C:
            case MOV_O_D:
                desc.hasImmediateAddress = true;
                desc.immediateWidth = 4;
                this.memory.write2Bytes(this.memory.read4Bytes(this.reg_ip), (short) src);
                return;
            
            // BIO w/o offset
            case MOV_BI_A:
            case MOV_BI_B:
            case MOV_BI_C:
            case MOV_BI_D:
                this.memory.write2Bytes(getBIOAddress(desc, false, false), (short) src);
                return;
                
            // BIO with offset
            case MOV_BIO_A:
            case MOV_BIO_B:
            case MOV_BIO_C:
            case MOV_BIO_D:
                this.memory.write2Bytes(getBIOAddress(desc, false, true), (short) src);
                return;
                
            // flags
            case MOV_F_RIM:
                this.reg_f = (short) src;
                break;
            
            case MOV_PF_RIM:
                this.reg_pf = (short) src;
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
                if(desc.sourceWidth == 1) { // 1 byte
                    putWideRIMDestination(desc, src & 0x0000_00FF);
                } else { // 2 byte
                    putWideRIMDestination(desc, src & 0x0000_FFFF);
                }
                return;
            
            case MOV_RIM:
            case MOV_RIM_F:
            case MOV_RIM_PF:
                putNormalRIMDestination(desc, src);
                return;
            
            default:
        }
    }
    
    /**
     * Executes XCHG instructions
     * 
     * @param op
     */
    private void runXCHG(InstructionDescriptor desc) {
        // i love abstraction
        LocationDescriptor srcDesc = getNormalRIMSourceDescriptor(desc),
                           dstDesc = getNormalRIMDestinationDescriptor(desc);
        
        int tmp = readLocation(srcDesc);
        writeLocation(srcDesc, readLocation(dstDesc));
        writeLocation(dstDesc, tmp);
    }
    
    /**
     * Executes PUSH instructions
     * 
     * @param op
     * @return
     */
    private void runPUSH(InstructionDescriptor desc) {
        // deal with this separately cause of multiple registers and whatnot
        if(desc.op == Opcode.PUSHA) {
            this.reg_sp -= 20;
            this.memory.write4Bytes(this.reg_sp + 0, this.reg_bp);
            this.memory.write2Bytes(this.reg_sp + 4, this.reg_l);
            this.memory.write2Bytes(this.reg_sp + 6, this.reg_k);
            this.memory.write2Bytes(this.reg_sp + 8, this.reg_j);
            this.memory.write2Bytes(this.reg_sp + 10, this.reg_i);
            this.memory.write2Bytes(this.reg_sp + 12, this.reg_d);
            this.memory.write2Bytes(this.reg_sp + 14, this.reg_c);
            this.memory.write2Bytes(this.reg_sp + 16, this.reg_b);
            this.memory.write2Bytes(this.reg_sp + 18, this.reg_a);
            return;
        }
        
        // deal with operand size
        int opSize = switch(desc.op) {
            case PUSH_A, PUSH_B, PUSH_C, PUSH_D, PUSH_I, PUSH_J, PUSH_K, PUSH_L, PUSH_F -> 2;
            case PUSH_BP, PUSH_SP, PUSH_I32 -> 4;
            default -> getNormalRIMSourceWidth(desc);
        };
        
        this.reg_sp -= opSize;
        
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
            case PUSH_F     -> this.reg_f;
            case PUSH_BP    -> this.reg_bp;
            case PUSH_SP    -> this.reg_sp;
            case PUSH_I32   -> {
                desc.hasImmediateValue = true;
                desc.immediateWidth = 4;
                yield this.memory.read4Bytes(this.reg_ip);
            }
            default         -> getNormalRIMSource(desc); // rim
        };
        
        // write
        if(opSize == 1) {
            this.memory.writeByte(this.reg_sp, (byte) val);
        } else if(opSize == 2) {
            this.memory.write2Bytes(this.reg_sp, (short) val);
        } else {
            this.memory.write4Bytes(this.reg_sp, val);
        }
    }
    
    /**
     * Executes POP instructions
     * 
     * @param op
     */
    private void runPOP(InstructionDescriptor desc) {
        // deal with POPA separately
        if(desc.op == Opcode.POPA) {
            this.reg_bp = this.memory.read4Bytes(this.reg_sp + 0);
            this.reg_l = this.memory.read2Bytes(this.reg_sp + 4);
            this.reg_k = this.memory.read2Bytes(this.reg_sp + 6);
            this.reg_j = this.memory.read2Bytes(this.reg_sp + 8);
            this.reg_i = this.memory.read2Bytes(this.reg_sp + 10);
            this.reg_d = this.memory.read2Bytes(this.reg_sp + 12);
            this.reg_c = this.memory.read2Bytes(this.reg_sp + 14);
            this.reg_b = this.memory.read2Bytes(this.reg_sp + 16);
            this.reg_a = this.memory.read2Bytes(this.reg_sp + 18);
            
            this.reg_sp += 20;
            return;
        }
        
        // deal with operand size
        int opSize = switch(desc.op) {
            case POP_A, POP_B, POP_C, POP_D, POP_I, POP_J, POP_K, POP_L, POP_F -> 2;
            case POP_BP, POP_SP -> 4;
            default -> getNormalRIMSourceWidth(desc);
        };
        
        int val = switch(opSize) {
            case 1  -> this.memory.readByte(this.reg_sp);
            case 2  -> this.memory.read2Bytes(this.reg_sp);
            case 4  -> this.memory.read4Bytes(this.reg_sp);
            default -> -1; // not possible
        };
        
        //System.out.println(String.format("source value: %08X", val));
        
        this.reg_sp += opSize;
        
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
            
            case POP_F:     this.reg_f = (short) val; break;
            case POP_BP:    this.reg_bp = val; break;
            case POP_SP:    this.reg_sp = val; break; // must happen after sp is incremented
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
                case 1  -> 0xFFl;
                case 2  -> 0xFFFFl;
                case 4  -> 0xFFFF_FFFFl;
                default -> 0l;
            };
            
            al = ((long) a) & sizeMask;
            bl = ((long) b) & sizeMask;
        }
        
        res = al * bl;
        
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
            rim = this.memory.readByte(this.reg_ip);
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
                
                addr = this.memory.read4Bytes(this.reg_ip + 1);
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
     */
    private void writeLocation(LocationDescriptor desc, int val) {
        //System.out.println("writing location " + desc + " with " + val);
        
        // what we dealin with
        if(desc.type() == LocationType.MEMORY) {
            switch(desc.size()) {
                case 1: this.memory.writeByte(desc.address(), (byte) val);     break;
                case 2: this.memory.write2Bytes(desc.address(), (short) val);   break;
                case 3: this.memory.write3Bytes(desc.address(), val);           break;
                case 4: this.memory.write4Bytes(desc.address(), val);           break;
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
                            this.reg_pf = (short) val;
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
     */
    private void putNormalRIMDestination(InstructionDescriptor desc, int val) {
        writeLocation(getNormalRIMDestinationDescriptor(desc), val);
    }
    
    /**
     * Puts the result of a wide RIM in its destination (double normal width)
     * 
     * @param val
     */
    private void putWideRIMDestination(InstructionDescriptor desc, int val) {
        LocationDescriptor normalDesc = getNormalRIMDestinationDescriptor(desc);
        
        putWideRIMDestination(normalDesc, val);
    }
    
    /**
     * Puts the result of a wide RIM in its destination (double normal width)
     * Takes the LocationDescriptor to avoid recalculation 
     * 
     * @param normalDesc
     * @param val
     */
    private void putWideRIMDestination(LocationDescriptor normalDesc, int val) {
        if(normalDesc.size() < 4) {
            // correct r16 -> pair by rim
            LocationType type = switch(normalDesc.type()) {
                case REG_J  -> LocationType.REG_K;
                case REG_K  -> LocationType.REG_BP;
                case REG_L  -> LocationType.REG_SP;
                default     -> normalDesc.type();
            };
            
            // size < 4, double it
            writeLocation(new LocationDescriptor(type, normalDesc.size() * 2, normalDesc.address()), val);
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
     */
    private void putPackedRIMDestination(LocationDescriptor normalDesc, int val) {
        writeLocation(new LocationDescriptor(normalDesc.type(), 2, normalDesc.address()), val);
    }
    
    /**
     * Puts the result of a wide packed RIM in its destination
     * 
     * @param normalDesc
     * @param val
     */
    private void putWidePackedRIMDestination(LocationDescriptor normalDesc, int val) {
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
            rim = this.memory.readByte(this.reg_ip);
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
            rim = this.memory.readByte(this.reg_ip);
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
            
            int addr = this.memory.read4Bytes(this.reg_ip + 1);
            
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
     */
    private int readLocation(LocationDescriptor desc) {
        //System.out.println("reading location " + desc);
        
        // what we workin with
        if(desc.type() == LocationType.MEMORY) {
            return switch(desc.size()) {
                case 1  -> this.memory.readByte(desc.address());
                case 2  -> this.memory.read2Bytes(desc.address());
                case 4  -> this.memory.read4Bytes(desc.address());
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
                case REG_PF -> this.reg_pf;
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
     */
    private int getNormalRIMSource(InstructionDescriptor desc) {
        return readLocation(getNormalRIMSourceDescriptor(desc));
    }
    
    /**
     * Gets the source of a wide RIM. Forces sources to be 4 bytes.
     * 
     * @return
     */
    private int getWideRIMSource(InstructionDescriptor desc) {
        LocationDescriptor normalDesc = getNormalRIMSourceDescriptor(desc);
        
        if(desc.hasImmediateValue) {
            desc.immediateWidth = 4;
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
     * Gets the source of a packed RIM
     * 
     * @param desc
     * @return
     */
    private int getPackedRIMSource(LocationDescriptor normalDesc, InstructionDescriptor idesc) {
        // override immediate size
        if(idesc.hasImmediateValue) idesc.immediateWidth = 2; 
        
        return readLocation(new LocationDescriptor(normalDesc.type(), 2, normalDesc.address()));
    }
    
    /**
     * Gets the source of a wide packed RIM
     * 
     * @param normalDesc
     * @return
     */
    private int getWidePackedRIMSource(LocationDescriptor normalDesc) {
        return readLocation(new LocationDescriptor(normalDesc.type(), 4, normalDesc.address()));
    }
    
    /**
     * Gets the address from BIO
     * 
     * @param desc
     * @param hasRIM
     * @param hasOffset
     * @return
     */
    private int getBIOAddress(InstructionDescriptor desc, boolean hasRIM, boolean hasOffset) {
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
                if(desc.hasImmediateAddress || desc.hasImmediateValue) addr += desc.immediateWidth; // immediate
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
     */
    public void fireMaskableInterrupt(byte vector) {
        if((this.reg_pf & 1) != 0) {
            this.halted = false;
            this.externalInterrupt = true;
            this.externalInterruptVector = vector;
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
    public short getRegPF() { return this.reg_pf; }
    public int getRegIP() { return this.reg_ip; }
    public int getRegBP() { return this.reg_bp; }
    public int getRegSP() { return this.reg_sp; }
    public boolean getHalted() { return this.halted; }
    
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
    public void setRegPF(short pf) { this.reg_pf = pf; }
    public void setRegIP(int ip) { this.reg_ip = ip; }
    public void setRegBP(int bp) { this.reg_bp = bp; }
    public void setRegSP(int sp) { this.reg_sp = sp; }
    public void setHalted(boolean h) { this.halted = h; }
}
