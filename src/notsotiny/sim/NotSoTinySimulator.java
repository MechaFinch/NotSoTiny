package notsotiny.sim;

import notsotiny.sim.LocationDescriptor.LocationType;
import notsotiny.sim.memory.MemoryController;
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
                  reg_f;
    
    // 32 bit
    private int reg_ip,
                reg_sp,
                reg_bp;
    
    /**
     * Create the simulator
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
    }
    
    /**
     * Run a step
     * 
     * Opcodes are sent to family handlers based on their category. These family handlers then send
     * opcodes off to operation-specific methods. All for organization.
     */
    public void step() {
        InstructionDescriptor desc = new InstructionDescriptor();
        desc.op = Opcode.fromOp(this.memory.readByte(this.reg_ip++));
        
        if(desc.op == Opcode.NOP) return; 
        
        System.out.println(desc.op.getType().getFamily()); 
        
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
        
        // don't update after absolute jumps
        if(desc.op.getType() != Operation.JMPA) {
            updateIP(desc);
        }
    }
    
    /**
     * Interprets MISC family instructions
     * 
     * @param op
     */
    private void stepMiscFamily(InstructionDescriptor desc) {
        System.out.println(desc.op.getType());
        
        switch(desc.op.getType()) {
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
        System.out.println(desc.op.getType());
        
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
        // TODO
    }
    
    /**
     * Executes 1-input logic instructions NOT and NEG
     * 
     * @param op
     */
    private void run1Logic(InstructionDescriptor desc) {
        // TODO
    }
    
    /**
     * Executes rotation instructions SHL, SHR, SAR, ROL, ROR, RCL, and RCR
     * 
     * @param op
     */
    private void runRotateLogic(InstructionDescriptor desc) {
        // TODO
    }
    
    /**
     * Interpret a MULTIPLICATION family instruction
     * 
     * @param op
     */
    private void stepMultiplicationFamily(InstructionDescriptor desc) {
        System.out.println(desc.op.getType());
        
        switch(desc.op.getType()) {
            case MUL:
            case MULS:
            case MULH:
            case MULSH:
                runMUL(desc);
                break;
            
            case PMUL:
            case PMULS:
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
     * Executes MUL, MULS, MULH, and MULSH instructions
     * 
     * @param op
     */
    private void runMUL(InstructionDescriptor desc) {
        // TODO
    }
    
    /**
     * Executes PMUL, PMULS, PMULH, and PMULSH instructions
     * 
     * @param op
     */
    private void runPMUL(InstructionDescriptor desc) {
        // TODO
    }
    
    /**
     * Executes DIV, DIVS, DIVM, and DIVMS instructions
     * 
     * @param op
     */
    private void runDIV(InstructionDescriptor desc) {
        // TODO
    }
    
    /**
     * Executes PDIV, PDIVS, PDIVM, and PDIVMS instructions
     * 
     * @param op
     */
    private void runPDIV(InstructionDescriptor desc) {
        // TODO
    }
    
    /**
     * Interpret an ADDITION family instruction
     * 
     * @param op
     */
    private void stepAdditionFamily(InstructionDescriptor desc) {
        System.out.println(desc.op.getType());
        
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
        // TODO
    }
    
    /**
     * Executes PINC, PICC, PDEC, and PDCC instructions
     * 
     * @param op
     */
    private void runPINC(InstructionDescriptor desc) {
       // TODO 
    }
    
    /**
     * Executes ADD, ADC, SUB, and SBB instructions
     * 
     * @param op
     */
    private void runADD(InstructionDescriptor desc) {
        // TODO
    }
    
    /**
     * Executes PADD, PADC, PSUB, and PSBB instructions
     * 
     * @param op
     */
    private void runPADD(InstructionDescriptor desc) {
        // TODO
    }
    
    /**
     * Interpret a JUMP family instruction
     * 
     * @param op
     */
    private void stepJumpFamily(InstructionDescriptor desc) {
        System.out.println(desc.op.getType());
        
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
        // TODO
    }
    
    /**
     * Executes RET and IRET instructions
     * 
     * @param op
     */
    private void runRET(InstructionDescriptor desc) {
        // TODO
    }
    
    /**
     * Executes INT instructions
     * 
     * @param op
     */
    private void runINT(InstructionDescriptor desc) {
        // TODO
    }
    
    /**
     * Executes LEA instructions
     * 
     * @param op
     */
    private void runLEA(InstructionDescriptor desc) {
        // TODO
    }
    
    /**
     * Executes CMP instructions
     * 
     * @param op
     */
    private void runCMP(InstructionDescriptor desc) {
        // TODO
    }
    
    /**
     * Executes JCC instructions
     * 
     * @param op
     */
    private void runJCC(InstructionDescriptor desc) {
        // TODO
    }
    
    /**
     * Interpret a MOVE family instruction
     * 
     * @param op
     */
    private void stepMoveFamily(InstructionDescriptor desc) {
        System.out.println(desc.op.getType());
        
        switch(desc.op.getType()) {
            case MOV:
                runMOV(desc);
                break;
            
            case XCHG:
                runXCHG(desc);
                break;
            
            case PUSH:
                runPUSH(desc);
                break;
            
            case POP:
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
        System.out.println(desc.op);
        
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
            // immediate 8 moves
            case MOV_A_I8:
            case MOV_B_I8:
            case MOV_C_I8:
            case MOV_D_I8:
                desc.hasImmediateValue = true;
                desc.immediateWidth = 1;
                src = this.memory.readByte(this.reg_ip);
                break;
            
            // immediate 16 moves
            case MOV_I_I16:
            case MOV_J_I16:
            case MOV_A_I16:
            case MOV_B_I16:
            case MOV_C_I16:
            case MOV_D_I16:
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
            
            // wide rim
            case MOVW_RIM:
                src = getWideRIMSource(desc);
                break;
            
            // thin rim
            case MOVS_RIM:
            case MOVZ_RIM:
                src = getThinRIMSource(desc);
                break;
            
            // rim
            case MOV_RIM:
                src = getNormalRIMSource(desc);
                break;
            
            default:
        }
        
        System.out.println(String.format("source value: %08X", src));
        
        // put source in destination
        switch(desc.op) {
            // A
            case MOV_A_I8:
            case MOV_A_I16:
                this.reg_a = (short) src;
                return;
            
            // B
            case MOV_B_I8:
            case MOV_B_I16:
                this.reg_b = (short) src;
                return;
            
            // C
            case MOV_C_I8:
            case MOV_C_I16:
                this.reg_c = (short) src;
                return;
            
            // D
            case MOV_D_I8:
            case MOV_D_I16:
                this.reg_d = (short) src;
                return;
            
            // others
            case MOV_I_I16:
                this.reg_i = (short) src;
                return;
            
            case MOV_J_I16:
                this.reg_j = (short) src;
                return;
            
            case MOV_BP_I32:
                this.reg_bp = src;
                return;
            
            case MOV_SP_I32:
                this.reg_sp = src;
                return;
            
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
        int tmp;
        
        // ez cases
        switch(desc.op) {
            // internal byte swaps
            case XCHG_AH_AL:
                this.reg_a = (short)((this.reg_a << 8) | (this.reg_a >>> 8));
                return;
            
            case XCHG_BH_BL:
                this.reg_b = (short)((this.reg_b << 8) | (this.reg_b >>> 8));
                return;
            
            case XCHG_CH_CL:
                this.reg_c = (short)((this.reg_c << 8) | (this.reg_c >>> 8));
                return;
            
            case XCHG_DH_DL:
                this.reg_d = (short)((this.reg_d << 8) | (this.reg_d >>> 8));
                return;
            
            // lower byte swaps
            case XCHG_AL_BL:
                tmp = this.reg_a;
                this.reg_a = (short)((this.reg_a & 0xFF00) | (this.reg_b & 0x00FF));
                this.reg_b = (short)((this.reg_b & 0xFF00) | (tmp & 0x00FF));
                return;
            
            case XCHG_AL_CL:
                tmp = this.reg_a;
                this.reg_a = (short)((this.reg_a & 0xFF00) | (this.reg_c & 0x00FF));
                this.reg_c = (short)((this.reg_c & 0xFF00) | (tmp & 0x00FF));
                return;
                
            case XCHG_AL_DL:
                tmp = this.reg_a;
                this.reg_a = (short)((this.reg_a & 0xFF00) | (this.reg_d & 0x00FF));
                this.reg_d = (short)((this.reg_d & 0xFF00) | (tmp & 0x00FF));
                return;
                
            case XCHG_BL_CL:
                tmp = this.reg_b;
                this.reg_b = (short)((this.reg_b & 0xFF00) | (this.reg_c & 0x00FF));
                this.reg_c = (short)((this.reg_c & 0xFF00) | (tmp & 0x00FF));
                return;
                
            case XCHG_BL_DL:
                tmp = this.reg_b;
                this.reg_b = (short)((this.reg_b & 0xFF00) | (this.reg_d & 0x00FF));
                this.reg_d = (short)((this.reg_d & 0xFF00) | (tmp & 0x00FF));
                return;
                
            case XCHG_CL_DL:
                tmp = this.reg_c;
                this.reg_c = (short)((this.reg_c & 0xFF00) | (this.reg_d & 0x00FF));
                this.reg_d = (short)((this.reg_d & 0xFF00) | (tmp & 0x00FF));
                return;
            
            // full register swaps
            case XCHG_A_B:
                tmp = this.reg_a;
                this.reg_a = this.reg_b;
                this.reg_b = (short) tmp;
                return;
            
            case XCHG_A_C:
                tmp = this.reg_a;
                this.reg_a = this.reg_c;
                this.reg_c = (short) tmp;
                return;
            
            case XCHG_A_D:
                tmp = this.reg_a;
                this.reg_a = this.reg_d;
                this.reg_d = (short) tmp;
                return;
                
            case XCHG_B_C:
                tmp = this.reg_b;
                this.reg_b = this.reg_c;
                this.reg_c = (short) tmp;
                return;
                
            case XCHG_B_D:
                tmp = this.reg_b;
                this.reg_b = this.reg_d;
                this.reg_d = (short) tmp;
                return;
            
            case XCHG_C_D:
                tmp = this.reg_c;
                this.reg_c = this.reg_d;
                this.reg_d = (short) tmp;
                return;
            
            default: // rim
        }
        
        // i love abstraction
        LocationDescriptor srcDesc = getNormalRIMSourceDescriptor(desc),
                           dstDesc = getNormalRIMDestinationDescriptor(desc);
        
        tmp = readLocation(srcDesc);
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
        // deal with operand size
        int opSize = switch(desc.op) {
            case PUSH_A, PUSH_B, PUSH_C, PUSH_D, PUSH_I, PUSH_J, PUSH_F -> 2;
            case PUSH_BP, PUSH_SP -> 4;
            default -> getNormalRIMSourceWidth();
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
            case PUSH_F     -> this.reg_f;
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
        // deal with operand size
        int opSize = switch(desc.op) {
            case POP_A, POP_B, POP_C, POP_D, POP_I, POP_J, POP_F -> 2;
            case POP_BP, POP_SP -> 4;
            default -> getNormalRIMSourceWidth();
        };
        
        int val = switch(opSize) {
            case 1  -> this.memory.readByte(this.reg_sp);
            case 2  -> this.memory.read2Bytes(this.reg_sp);
            case 4  -> this.memory.read4Bytes(this.reg_sp);
            default -> -1; // not possible
        };
        
        System.out.println(String.format("source value: %08X", val));
        
        this.reg_sp += opSize;
        
        // write
        switch(desc.op) {
            case POP_A:     this.reg_a = (short) val; break;
            case POP_B:     this.reg_b = (short) val; break;
            case POP_C:     this.reg_c = (short) val; break;
            case POP_D:     this.reg_d = (short) val; break;
            case POP_I:     this.reg_i = (short) val; break;
            case POP_J:     this.reg_j = (short) val; break;
            case POP_F:     this.reg_f = (short) val; break;
            case POP_BP:    this.reg_bp = val; break;
            case POP_SP:    this.reg_sp = val; break; // must happen after sp is incremented
            default:        putNormalRIMDestination(desc, val); // rim
        }
    }
    
    /**
     * Gets the descriptor of a RIM destination
     * 
     * @return
     */
    private LocationDescriptor getNormalRIMDestinationDescriptor(InstructionDescriptor desc) {
        desc.hasRIMByte = true;
        
        byte rim = this.memory.readByte(this.reg_ip);
        boolean size = (rim & 0x80) == 0;
        
        System.out.println(String.format("rim, size: %02X, %s", rim, size));
        
        if((rim & 0x40) == 0 || (rim & 0x04) == 0) {
            System.out.println(String.format("register destination %s", (rim & 0x38) >>> 3));
            
            // register destination
            if(size) { // 16 bit
                return switch((rim & 0x38) >> 3) {
                    case 0  -> LocationDescriptor.REGISTER_A;
                    case 1  -> LocationDescriptor.REGISTER_B;
                    case 2  -> LocationDescriptor.REGISTER_C;
                    case 3  -> LocationDescriptor.REGISTER_D;
                    case 4  -> LocationDescriptor.REGISTER_I;
                    case 5  -> LocationDescriptor.REGISTER_J;
                    case 6  -> LocationDescriptor.REGISTER_BP;
                    case 7  -> LocationDescriptor.REGISTER_SP;
                    default -> null;
                };
            } else { // 8 bit
                return switch((rim & 0x38) >> 3) {
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
            // memory destination
            int addr = 0;
            
            if((rim & 0x02) == 0) { // immediate address
                desc.hasImmediateAddress = true;
                desc.immediateWidth = 4;
                
                addr = this.memory.read4Bytes(this.reg_ip + 1);
            } else { // bio address
                addr = getBIOAddress(desc);
            }
            
            System.out.println(String.format("immediate address destination: %08X   ", addr));
            
            if(size) { // 16 bit
                // if the source is SP or BP, 32 bit
                if((rim & 0x38) > 0x28) {
                    return new LocationDescriptor(LocationType.MEMORY, 4, addr);
                } else {
                    return new LocationDescriptor(LocationType.MEMORY, 2, addr);
                }
            } else { // 8 bit
                return new LocationDescriptor(LocationType.MEMORY, 1, addr);
            }
        }
    }
    
    /**
     * Puts a value at a location
     * 
     * @param val
     */
    private void writeLocation(LocationDescriptor desc, int val) {
        // handle memory
        if(desc.type() == LocationType.MEMORY) {
            switch(desc.size()) {
                case 1: this.memory.writeByte(desc.address(), (byte) val); break;
                case 2: this.memory.write2Bytes(desc.address(), (short) val); break;
                case 3: this.memory.write3Bytes(desc.address(), val); break;
                case 4: this.memory.write4Bytes(desc.address(), val); break;
            }
        } else {
            // 8 bit
            if(desc.size() == 1) {
                switch(desc.type()) {
                    case REG_A:
                        if(desc.address() == 0) { // AL
                            this.reg_a = (short)((this.reg_a & 0xFF00) | ((byte) val));
                        } else { // AH
                            this.reg_a = (short)(((val << 8) & 0xFF00) | (this.reg_a & 0xFF));
                        }
                        break;
                        
                    case REG_B:
                        if(desc.address() == 0) { // BL
                            this.reg_b = (short)((this.reg_b & 0xFF00) | ((byte) val));
                        } else { // BH
                            this.reg_b = (short)(((val << 8) & 0xFF00) | (this.reg_b & 0xFF));
                        }
                        break;
                        
                    case REG_C:
                        if(desc.address() == 0) { // CL
                            this.reg_c = (short)((this.reg_c & 0xFF00) | ((byte) val));
                        } else { // CH
                            this.reg_c = (short)(((val << 8) & 0xFF00) | (this.reg_c & 0xFF));
                        }
                        break;
                        
                    case REG_D:
                        if(desc.address() == 0) { // DL
                            this.reg_d = (short)((this.reg_d & 0xFF00) | ((byte) val));
                        } else { // DH
                            this.reg_d = (short)(((val << 8) & 0xFF00) | (this.reg_d & 0xFF));
                        }
                        break;
                        
                    default:
                        throw new IllegalArgumentException("Invalid location descriptor: " + desc);
                }
            } else if(desc.size() == 2) { // 16 bit
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
                    
                    default:
                        throw new IllegalArgumentException("Invalid location descriptor: " + desc);
                }
            } else if(desc.size() == 4) { // 32 bit
                switch(desc.type()) {
                    case REG_A: // DA
                        this.reg_a = (short) val;
                        this.reg_d = (short) (val >> 16);
                        break;
                    
                    case REG_B: // AB
                        this.reg_b = (short) val;
                        this.reg_a = (short) (val >> 16);
                        break;
                        
                    case REG_C: // BC
                        this.reg_c = (short) val;
                        this.reg_b = (short) (val >> 16);
                        break;
                    
                    case REG_D: // CD
                        this.reg_d = (short) val;
                        this.reg_c = (short) (val >> 16);
                        break;
                        
                    case REG_I: // JI
                        this.reg_i = (short) val;
                        this.reg_j = (short) (val >> 16);
                        break;
                    
                    case REG_J: // IJ
                        this.reg_j = (short) val;
                        this.reg_i = (short) (val >> 16);
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
     * Puts the result of a wide RIM in its destination
     * 
     * @param val
     */
    private void putWideRIMDestination(InstructionDescriptor desc, int val) {
        LocationDescriptor normalDesc = getNormalRIMDestinationDescriptor(desc);
        
        // set size to 4
        writeLocation(new LocationDescriptor(normalDesc.type(), 4, normalDesc.address()), val);
    }
    
    /**
     * Returns the width in bytes of a RIM source
     * 
     * @return
     */
    private int getNormalRIMSourceWidth() {
        byte rim = this.memory.readByte(this.reg_ip);
        
        if((rim & 0x80) == 0) { // 16, unless BP/SP
            // if the source is a register, the target is that register. otherwise, it's the destination register
            int target = ((rim & 0x40) == 0) ? (rim & 0x07) : ((rim & 0x38) >>> 3);
            
            // if the target is BP/SP, return 4. otherwise 2
            if(target > 5) return 4;
            else return 2;
        } else {
            return 1;
        }
    }
    
    /**
     * Gets the descriptor of a RIM source
     * 
     * @return
     */
    private LocationDescriptor getNormalRIMSourceDescriptor(InstructionDescriptor desc) {
        desc.hasRIMByte = true;
        
        byte rim = this.memory.readByte(this.reg_ip);
        boolean size = (rim & 0x80) == 0;
        
        System.out.println(String.format("rim, size: %02X, %s", rim, size));
        
        // rim or mem
        if((rim & 0x40) == 0 || (rim & 0x04) != 0) {
            // if memory is a destination, source comes from d reg
            int src = 0;
            if((rim & 0x40) == 0) { // reg source
                src = rim & 0x07;
            } else { // mem destination
                src = (rim & 0x38) >> 3;
            }
            
            System.out.println(String.format("register source %s", src));
            
            // wide register source for BP/SP destination
            if(size && (rim & 0x38) > 0x28) {
                return switch(src) {
                    case 0  -> LocationDescriptor.REGISTER_DA;
                    case 1  -> LocationDescriptor.REGISTER_AB;
                    case 2  -> LocationDescriptor.REGISTER_BC;
                    case 3  -> LocationDescriptor.REGISTER_CD;
                    case 4  -> LocationDescriptor.REGISTER_JI;
                    case 5  -> LocationDescriptor.REGISTER_IJ;
                    case 6  -> LocationDescriptor.REGISTER_BP;
                    case 7  -> LocationDescriptor.REGISTER_SP;
                    default -> null;
                };
            }
            
            // register source
            return switch(src) {
                case 0  -> size ? LocationDescriptor.REGISTER_A : LocationDescriptor.REGISTER_AL;
                case 1  -> size ? LocationDescriptor.REGISTER_B : LocationDescriptor.REGISTER_BL;
                case 2  -> size ? LocationDescriptor.REGISTER_C : LocationDescriptor.REGISTER_CL;
                case 3  -> size ? LocationDescriptor.REGISTER_D : LocationDescriptor.REGISTER_DL;
                case 4  -> size ? LocationDescriptor.REGISTER_I : LocationDescriptor.REGISTER_AH;
                case 5  -> size ? LocationDescriptor.REGISTER_J : LocationDescriptor.REGISTER_BH;
                case 6  -> size ? LocationDescriptor.REGISTER_BP : LocationDescriptor.REGISTER_CH;
                case 7  -> size ? LocationDescriptor.REGISTER_SP : LocationDescriptor.REGISTER_DH;
                default -> null;
            };
        } else if((rim & 0x07) == 0) {
            System.out.println("immediate value");
            
            desc.hasImmediateValue = true;
            
            // immediate value
            if(size) { // 16 bit
                // BP/SP?
                if((rim & 0x38) > 0x28) {
                    desc.immediateWidth = 4;
                    return new LocationDescriptor(LocationType.MEMORY, 4, this.reg_ip + 1);
                } else {
                    desc.immediateWidth = 2;
                    return new LocationDescriptor(LocationType.MEMORY, 2, this.reg_ip + 1);
                }
            } else { // 8 bit
                desc.immediateWidth = 1;
                return new LocationDescriptor(LocationType.MEMORY, 1, this.reg_ip + 1);
            }
        } else if((rim & 0x07) == 1) {
            System.out.println("immediate address source");
            
            desc.hasImmediateAddress = true;
            desc.immediateWidth = 4;
            
            // immediate address
            int addr = this.memory.read4Bytes(this.reg_ip + 1);
            
            if(size) { // 16 bit
                // BP/SP?
                if((rim & 0x38) > 0x28) {
                    return new LocationDescriptor(LocationType.MEMORY, 4, addr); 
                } else {
                    return new LocationDescriptor(LocationType.MEMORY, 2, addr);
                }
            } else { // 8 bit
                return new LocationDescriptor(LocationType.MEMORY, 1, addr);
            }
        } else {
            System.out.println("bio address source");
            
            // BIO memory source
            int addr = getBIOAddress(desc);
            
            if(size) { // 16 bit
                // BP/SP?
                if((rim & 0x38) > 0x28) {
                    return new LocationDescriptor(LocationType.MEMORY, 4, addr);
                } else {
                    return new LocationDescriptor(LocationType.MEMORY, 2, addr);
                }
            } else { // 8 bit
                return new LocationDescriptor(LocationType.MEMORY, 1, addr);
            }
        }
    }
    
    /**
     * Get the value from a LocationDescriptor
     * 
     * @param loc
     * @return
     */
    private int readLocation(LocationDescriptor desc) {
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
                case REG_BP -> this.reg_bp;
                case REG_SP -> this.reg_sp;
                default     -> 0;
            };
            
            // handle size by size
            if(desc.size() == 4) { // 32 bit
                if(desc.type() == LocationType.REG_BP || desc.type() == LocationType.REG_SP) { 
                    return rawVal;
                }
                
                int v = switch(desc.type()) {
                    case REG_A  -> this.reg_d;
                    case REG_B  -> this.reg_a;
                    case REG_C  -> this.reg_b;
                    case REG_D  -> this.reg_c;
                    case REG_I  -> this.reg_j;
                    case REG_J  -> this.reg_i;
                    default     -> 0;
                };
                
                return rawVal | (v << 16);
            } else if(desc.size() == 1) { // 8 bit
                if(desc.address() == 0) { // lower
                    return rawVal & 0xFF;
                } else { // upper
                    return (rawVal >>> 8) & 0xFF;
                }
            } else { // 16 bit
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
        
        // change the size to 4 bytes
        return readLocation(new LocationDescriptor(normalDesc.type(), 4, normalDesc.address()));
    }
    
    /**
     * Gets the source of a thin RIM. Makes sure BP/SP isn't a special case
     * 
     * @param desc
     * @return
     */
    private int getThinRIMSource(InstructionDescriptor desc) {
        LocationDescriptor normalDesc = getNormalRIMSourceDescriptor(desc);
        
        // if the destination is BP or SP, set size according to the size bit
        if(normalDesc.size() == 4) {
            byte rim = this.memory.readByte(this.reg_ip);
            boolean size = (rim & 0x80) == 0,
                    regreg = (rim & 0x40) == 0;
            
            if(desc.hasImmediateValue) {
                desc.immediateWidth = size ? 2 : 1;
            }
            
            // handle half registers correctly
            if(!size && regreg) {
                LocationType type = null;
                int addr = 0;
                
                switch(rim & 0x07) {
                    case 4: // AH
                        addr = 1;
                    case 0: // AL
                        type = LocationType.REG_A;
                        break;
                        
                    case 5: // BH
                        addr = 1;
                    case 1: // BL
                        type = LocationType.REG_B;
                        break;
                        
                    case 6: // CH
                        addr = 1;
                    case 2: // CL
                        type = LocationType.REG_C;
                        break;
                        
                    case 7: // DH
                        addr = 1;
                    case 3: // DL
                        type = LocationType.REG_D;
                        break;
                }
                
                desc.sourceWidth = 1;
                return readLocation(new LocationDescriptor(type, 1, addr));
            } else {
                desc.sourceWidth = size ? 2 : 1;
                return readLocation(new LocationDescriptor(normalDesc.type(), size ? 2 : 1, normalDesc.address()));
            }
        } else {
            return readLocation(normalDesc);
        }
    }
    
    /**
     * Gets the address from BIO
     * 
     * @return
     */
    private int getBIOAddress(InstructionDescriptor desc) {
        desc.hasBIOByte = true;
        
        byte rim = this.memory.readByte(this.reg_ip),
             bio = this.memory.readByte(this.reg_ip + 1),
             scale = (byte)(bio >> 6),
             offsetSize = 4;
        
        boolean hasIndex = (bio & 0x07) != 0x07;
       
        int addr = 0;
        
        System.out.println(String.format("rim, bio, scale, index: %02X, %02X, %s, %s", rim, bio, scale, hasIndex));
        
        // index
        if(hasIndex) {
            addr += switch(bio & 0x07) {
                case 0  -> this.reg_a;
                case 1  -> this.reg_b;
                case 2  -> this.reg_c;
                case 3  -> this.reg_d;
                case 4  -> this.reg_i;
                case 5  -> this.reg_j;
                case 6  -> this.reg_bp;
                case 7  -> this.reg_sp;
                default -> -1; // not possible
            };
            
            addr <<= scale;
        } else {
            offsetSize = ++scale; // increment to avoid casting
        }
        
        System.out.println(String.format("addr from index: %08X", addr));
        
        // base
        addr += switch((bio & 0x38) >> 3) {
            case 0  -> (this.reg_d << 16) | this.reg_a; // D:A
            case 1  -> (this.reg_a << 16) | this.reg_b; // A:B
            case 2  -> (this.reg_b << 16) | this.reg_c; // B:C
            case 3  -> (this.reg_c << 16) | this.reg_d; // C:D
            case 4  -> (this.reg_j << 16) | this.reg_i; // J:I
            case 5  -> (this.reg_i << 16) | this.reg_j; // I:J
            case 6  -> this.reg_bp;
            case 7  -> hasIndex ? 0 : this.reg_sp;
            default -> -1; // not possible
        };
        
        System.out.println(String.format("addr from base: %08X", addr));
        
        // offset
        if((rim & 0x01) == 1) {
            desc.hasImmediateAddress = true;
            desc.immediateWidth = offsetSize;
            
            int imm = 0;
            
            for(int i = 0; i < offsetSize; i++) {
                imm |= (this.memory.readByte(this.reg_ip + 2 + i) & 0xFF) << (i * 8);
            }
            
            addr += imm;
        }
        
        System.out.println(String.format("final address: %08X", addr));
        
        return addr;
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
    
    /*
     * getty bois
     */
    public short getRegA() { return this.reg_a; }
    public short getRegB() { return this.reg_b; }
    public short getRegC() { return this.reg_c; }
    public short getRegD() { return this.reg_d; }
    public short getRegI() { return this.reg_i; }
    public short getRegJ() { return this.reg_j; }
    public short getRegF() { return this.reg_f; }
    public int getRegIP() { return this.reg_ip; }
    public int getRegBP() { return this.reg_bp; }
    public int getRegSP() { return this.reg_sp; }
}
