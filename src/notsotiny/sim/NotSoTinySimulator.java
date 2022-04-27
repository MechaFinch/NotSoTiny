package notsotiny.sim;

/**
 * Simulates the NotSoTiny architecture
 * 
 * @author Mechafinch
 */
public class NotSoTinySimulator {
    
    private byte[] memory;
    
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
    public NotSoTinySimulator(byte[] memory, int entry) {
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
        Opcode op = Opcode.fromOp(this.memory[this.reg_ip++]);
        
        if(op == Opcode.NOP) return; 
        
        System.out.println(op.getType().getFamily());
        
        switch(op.getType().getFamily()) {
            case ADDITION:
                stepAdditionFamily(op);
                break;
                
            case JUMP:
                stepJumpFamily(op);
                break;
                
            case LOGIC:
                stepLogicFamily(op);
                break;
                
            case MISC:
                stepMiscFamily(op);
                break;
                
            case MOVE:
                stepMoveFamily(op);
                break;
                
            case MULTIPLICATION:
                stepMultiplicationFamily(op);
                break;
                
            default:
                break;
        }
        
        updateIP(op);
    }
    
    /**
     * Interprets MISC family instructions
     * 
     * @param op
     */
    private void stepMiscFamily(Opcode op) {
        System.out.println(op.getType());
        
        switch(op.getType()) {
            case NOP: // just NOP
                break;
            
            default:
                throw new IllegalStateException("Sent invalid instruction to MISC Family handler: " + op);
        }
    }
    
    /**
     * Interprets LOGIC family instructions
     * 
     * @param op
     */
    private void stepLogicFamily(Opcode op) {
        System.out.println(op.getType());
        
        switch(op.getType()) {
            case AND:
            case OR:
            case XOR:
                run2Logic(op);
                break;
            
            case NOT:
            case NEG:
                run1Logic(op);
                break;
            
            case SHL:
            case SHR:
            case SAR:
            case ROL:
            case ROR:
            case RCL:
            case RCR:
                runRotateLogic(op);
                break;
            
            default:
                throw new IllegalStateException("Sent invalid instruction to LOGIC Family handler: " + op);
        }
    }
    
    /**
     * Executes 2-input logic instructions AND, OR, and XOR
     * 
     * @param op
     */
    private void run2Logic(Opcode op) {
        // TODO
    }
    
    /**
     * Executes 1-input logic instructions NOT and NEG
     * 
     * @param op
     */
    private void run1Logic(Opcode op) {
        // TODO
    }
    
    /**
     * Executes rotation instructions SHL, SHR, SAR, ROL, ROR, RCL, and RCR
     * 
     * @param op
     */
    private void runRotateLogic(Opcode op) {
        // TODO
    }
    
    /**
     * Interpret a MULTIPLICATION family instruction
     * 
     * @param op
     */
    private void stepMultiplicationFamily(Opcode op) {
        System.out.println(op.getType());
        
        switch(op.getType()) {
            case MUL:
            case MULS:
            case MULH:
            case MULSH:
                runMUL(op);
                break;
            
            case PMUL:
            case PMULS:
            case PMULH:
            case PMULSH:
                runPMUL(op);
                break;
            
            case DIV:
            case DIVS:
            case DIVM:
            case DIVMS:
                runDIV(op);
                break;
            
            case PDIV:
            case PDIVS:
            case PDIVM:
            case PDIVMS:
                runPDIV(op);
                break;
            
            default:
                throw new IllegalStateException("Sent invalid instruction to MULTIPLICATION Family handler: " + op);
        }
    }
    
    /**
     * Executes MUL, MULS, MULH, and MULSH instructions
     * 
     * @param op
     */
    private void runMUL(Opcode op) {
        // TODO
    }
    
    /**
     * Executes PMUL, PMULS, PMULH, and PMULSH instructions
     * 
     * @param op
     */
    private void runPMUL(Opcode op) {
        // TODO
    }
    
    /**
     * Executes DIV, DIVS, DIVM, and DIVMS instructions
     * 
     * @param op
     */
    private void runDIV(Opcode op) {
        // TODO
    }
    
    /**
     * Executes PDIV, PDIVS, PDIVM, and PDIVMS instructions
     * 
     * @param op
     */
    private void runPDIV(Opcode op) {
        // TODO
    }
    
    /**
     * Interpret an ADDITION family instruction
     * 
     * @param op
     */
    private void stepAdditionFamily(Opcode op) {
        System.out.println(op.getType());
        
        switch(op.getType()) {
            case INC:
            case ICC:
            case DEC:
            case DCC:
                runINC(op);
                break;
            
            case PINC:
            case PICC:
            case PDEC:
            case PDCC:
                runPINC(op);
                break;
                
            case ADD:
            case ADC:
            case SUB:
            case SBB:
                runADD(op);
                break;
            
            case PADD:
            case PADC:
            case PSUB:
            case PSBB:
                runPADD(op);
                break;
            
            default:
                throw new IllegalStateException("Sent invalid instruction to ADDITION Family handler: " + op);
        }
    }
    
    /**
     * Executes INC, ICC, DEC, and DCC instructions
     * 
     * @param op
     */
    private void runINC(Opcode op) {
        // TODO
    }
    
    /**
     * Executes PINC, PICC, PDEC, and PDCC instructions
     * 
     * @param op
     */
    private void runPINC(Opcode op) {
       // TODO 
    }
    
    /**
     * Executes ADD, ADC, SUB, and SBB instructions
     * 
     * @param op
     */
    private void runADD(Opcode op) {
        // TODO
    }
    
    /**
     * Executes PADD, PADC, PSUB, and PSBB instructions
     * 
     * @param op
     */
    private void runPADD(Opcode op) {
        // TODO
    }
    
    /**
     * Interpret a JUMP family instruction
     * 
     * @param op
     */
    private void stepJumpFamily(Opcode op) {
        System.out.println(op.getType());
        
        switch(op.getType()) {
            case JMP:
            case JMPA:
                runJMP(op);
                break;
            
            case CALL:
            case CALLA:
                runCALL(op);
                break;
            
            case RET:
            case IRET:
                runRET(op);
                break;
            
            case INT:
                runINT(op);
                break;
            
            case LEA:
                runLEA(op);
                break;
            
            case CMP:
                runCMP(op);
                break;
            
            case JCC:
                runJCC(op);
                
            default:
                throw new IllegalStateException("Sent invalid instruction to JUMP Family handler: " + op);
        }
    }
    
    /**
     * Executes JMP and JMPA instructions
     * 
     * @param op
     */
    private void runJMP(Opcode op) {
        // TODO
    }
    
    /**
     * Executes CALL and CALLA instructions
     * 
     * @param op
     */
    private void runCALL(Opcode op) {
        // TODO
    }
    
    /**
     * Executes RET and IRET instructions
     * 
     * @param op
     */
    private void runRET(Opcode op) {
        // TODO
    }
    
    /**
     * Executes INT instructions
     * 
     * @param op
     */
    private void runINT(Opcode op) {
        // TODO
    }
    
    /**
     * Executes LEA instructions
     * 
     * @param op
     */
    private void runLEA(Opcode op) {
        // TODO
    }
    
    /**
     * Executes CMP instructions
     * 
     * @param op
     */
    private void runCMP(Opcode op) {
        // TODO
    }
    
    /**
     * Executes JCC instructions
     * 
     * @param op
     */
    private void runJCC(Opcode op) {
        // TODO
    }
    
    /**
     * Interpret a MOVE family instruction
     * 
     * @param op
     */
    private void stepMoveFamily(Opcode op) {
        System.out.println(op.getType());
        
        switch(op.getType()) {
            case MOV:
                runMOV(op);
                break;
            
            case XCHG:
                runXCHG(op);
                break;
            
            case PUSH:
                runPUSH(op);
                break;
            
            case POP:
                runPOP(op);
                break;
            
            default:
                throw new IllegalStateException("Sent invalid instruction to MOVE Family handler: " + op);
        }
    }
    
    /**
     * Executes MOV instructions
     * 
     * @param op
     */
    private void runMOV(Opcode op) {
        System.out.println(op);
        
        // deal with register-register moves
        switch(op) {
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
        
        switch(op) {
            // immediate 8 moves
            case MOV_A_I8:
            case MOV_B_I8:
            case MOV_C_I8:
            case MOV_D_I8:
                src = this.memory[this.reg_ip];
                break;
            
            // immediate 16 moves
            case MOV_I_I16:
            case MOV_J_I16:
            case MOV_A_I16:
            case MOV_B_I16:
            case MOV_C_I16:
            case MOV_D_I16:
                src = read16(this.reg_ip);
                break;
            
            // immediate 32 moves
            case MOV_SP_I32:
            case MOV_BP_I32:
                src = read32(this.reg_ip);
                break;
            
            // rim
            case MOV_RIM:
                src = getNormalRIMSource();
            
            default:
        }
        
        // TODO: BP/SP SPECIAL CASES
        System.out.println(String.format("source value: %08X", src));
        
        // put source in destination
        switch(op) {
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
            
            case MOV_RIM:
                putNormalRIMDestination(src);
                return;
            
            default:
        }
    }
    
    /**
     * Executes XCHG instructions
     * 
     * @param op
     */
    private void runXCHG(Opcode op) {
        // TODO
    }
    
    /**
     * Executes PUSH instructions
     * 
     * @param op
     * @return
     */
    private void runPUSH(Opcode op) {
        // deal with operand size
        int opSize = switch(op) {
            case PUSH_A, PUSH_B, PUSH_C, PUSH_D, PUSH_I, PUSH_J, PUSH_F -> 2;
            case PUSH_BP, PUSH_SP -> 4;
            default -> getNormalRIMSourceWidth();
        };
        
        this.reg_sp -= opSize;
        
        // get value
        int val = switch(op) {
            case PUSH_A     -> this.reg_a;
            case PUSH_B     -> this.reg_b;
            case PUSH_C     -> this.reg_c;
            case PUSH_D     -> this.reg_d;
            case PUSH_I     -> this.reg_i;
            case PUSH_J     -> this.reg_j;
            case PUSH_F     -> this.reg_f;
            default         -> getNormalRIMSource();
        };
        
        // write
        if(opSize == 1) {
            this.memory[this.reg_sp] = (byte) val;
        } else if(opSize == 2) {
            write16(this.reg_sp, val);
        } else {
            write32(this.reg_sp, val);
        }
    }
    
    /**
     * Executes POP instructions
     * 
     * @param op
     */
    private void runPOP(Opcode op) {
        // TODO
    }
    
    /**
     * Puts the result of a RIM in its destination
     * 
     * @param val
     */
    private void putNormalRIMDestination(int val) {
        byte rim = this.memory[this.reg_ip];
        boolean size = (rim & 0x80) == 0;
        
        System.out.println(String.format("rim, size: %02X, %s", rim, size));
        
        if((rim & 0x40) == 0 || (rim & 0x04) == 0) {
            System.out.println(String.format("register destination %s", (rim & 0x38) >> 3));
            
            // register destination
            if(size) { // 16 bit
                switch((rim & 0x38) >> 3) {
                    case 0: this.reg_a = (short) val; break;
                    case 1: this.reg_b = (short) val; break;
                    case 2: this.reg_c = (short) val; break;
                    case 3: this.reg_d = (short) val; break;
                    case 4: this.reg_i = (short) val; break;
                    case 5: this.reg_j = (short) val; break;
                    case 6: this.reg_bp = val; break;
                    case 7: this.reg_sp = val; break;
                    default: // not possible
                }
            } else { // 8 bit
                switch((rim & 0x38) >> 3) {
                    case 0: this.reg_a = (short)((this.reg_a & 0xFF00) | val); break;           // AL
                    case 1: this.reg_b = (short)((this.reg_b & 0xFF00) | val); break;           // BL
                    case 2: this.reg_c = (short)((this.reg_c & 0xFF00) | val); break;           // CL
                    case 3: this.reg_d = (short)((this.reg_d & 0xFF00) | val); break;           // DL
                    case 4: this.reg_a = (short)((val << 8) | (this.reg_a & 0x00FF)); break;    // AH
                    case 5: this.reg_b = (short)((val << 8) | (this.reg_b & 0x00FF)); break;    // BH
                    case 6: this.reg_c = (short)((val << 8) | (this.reg_c & 0x00FF)); break;    // CH
                    case 7: this.reg_d = (short)((val << 8) | (this.reg_d & 0x00FF)); break;    // DH
                    default: // not possible
                }
            }
            
            return;
        } else {
            // immediate address
            int addr = 0;
            
            if((rim & 0x02) == 0) {
                addr = read32(this.reg_ip + 1);
            } else {
                addr = getBIOAddress();
            }
            
            System.out.println(String.format("immediate address destination: %08X   ", addr));
            
            if(size) { // 16 bit
                // if the source is SP or BP, 32 bit
                if((rim & 0x38) > 0x28) { 
                    write32(addr, val);
                } else {
                    write16(addr, val);
                }
            } else { // 8 bit
                this.memory[addr] = (byte) val;
            }
        }
    }
    
    /**
     * Returns the width in bytes of a RIM source
     * 
     * @return
     */
    private int getNormalRIMSourceWidth() {
        byte rim = this.memory[this.reg_ip];
        
        if((rim & 0x80) == 0) { // 16, unless BP/SP
            // if the source is a register, the target is that register. otherwise, it's the destination register
            int target = ((rim & 0x40) == 0) ? (rim & 0x07) : ((rim & 0x38) >> 3);
            
            // if the target is BP/SP, return 4. otherwise 2
            if(target > 5) return 4;
            else return 2;
        } else {
            return 1;
        }
    }
    
    /**
     * Gets the source of a RIM
     * 
     * @return
     */
    private int getNormalRIMSource() {
        byte rim = this.memory[this.reg_ip];
        boolean size = (rim & 0x80) == 0;
        
        System.out.println(String.format("rim, size: %02X, %s", rim, size));
        
        // rim or mem
        if((rim & 0x40) == 0 || (rim & 0x04) != 0) {
            int sizeMask = size ? 0xFFFF : 0xFF00;
            
            // if memory is a destination, source comes from d reg
            int src = 0;
            if((rim & 0x40) == 0) { // reg source
                src = rim & 0x07;
            } else { // mem destination
                src = (rim & 0x38) >> 3;
            }
            
            System.out.println(String.format("register source %s", src));
            
            // register source
            return switch(src) {
                case 0  -> this.reg_a & sizeMask;                               // A/AL
                case 1  -> this.reg_b & sizeMask;                               // B/BL
                case 2  -> this.reg_c & sizeMask;                               // C/CL
                case 3  -> this.reg_d & sizeMask;                               // D/DL
                case 4  -> size ? this.reg_i : (this.reg_a >> 8);               // I/AH
                case 5  -> size ? this.reg_j : (this.reg_b >> 8);               // J/BH
                case 6  -> size ? this.reg_bp : (this.reg_c >> 8);              // BP/CH
                case 7  -> size ? this.reg_sp : (this.reg_d >> 8);              // SP/CH
                default -> -1; // not possible
            };
        } else if((rim & 0x07) == 0) {
            System.out.println("immediate value");
            
            // immediate value
            if(size) { // 16 bit
                // BP/SP?
                if((rim & 0x38) > 0x28) {
                    return read32(this.reg_ip + 1);
                } else {
                    return read16(this.reg_ip + 1);
                }
            } else { // 8 bit
                return this.memory[this.reg_ip + 1];
            }
        } else if((rim & 0x07) == 1) {
            System.out.println("immediate address source");
            
            // immediate address
            int addr = read32(this.reg_ip + 1),
                val = 0;
            
            if(size) { // 16 bit
                // BP/SP?
                if((rim & 0x38) > 0x28) {
                    val = read32(addr); 
                } else {
                    val = read16(addr);
                }
            } else { // 8 bit
                val = this.memory[addr];
            }
            
            return val;
        } else {
            System.out.println("bio address source");
            
            // BIO memory source
            int addr = getBIOAddress(),
                val = 0;
            
            if(size) { // 16 bit
                // BP/SP?
                if((rim & 0x38) > 0x28) {
                    val = read32(addr); 
                } else {
                    val = read16(addr);
                }
            } else { // 8 bit
                val = this.memory[addr];
            }
            
            return val;
        }
    }
    
    /**
     * Writes 4 bytes to the address
     * 
     * @param address
     * @param value
     */
    private void write32(int address, int value) {
        this.memory[address] = (byte) value;
        this.memory[address + 1] = (byte)(value >> 8);
        this.memory[address + 2] = (byte)(value >> 16);
        this.memory[address + 3] = (byte)(value >> 24);
    }
    
    /**
     * Writes 2 bytes to the address
     * 
     * @param address
     * @param value
     */
    private void write16(int address, int value) {
        this.memory[address] = (byte) value;
        this.memory[address + 1] = (byte)(value >> 8);
    }
    
    /**
     * Reads 4 bytes from the address
     * 
     * @param address
     * @return
     */
    private int read32(int address) {
        return this.memory[address] | (this.memory[address + 2] << 8) | (this.memory[address + 3] << 16) | (this.memory[address + 4] << 24);
    }
    
    /**
     * Reads 2 bytes from the address
     * 
     * @param address
     * @return
     */
    private short read16(int address) {
        return (short)(this.memory[address] | (this.memory[address + 1] << 8));
    }
    
    /**
     * Gets the address from BIO
     * 
     * @return
     */
    private int getBIOAddress() {
        byte rim = this.memory[this.reg_ip],
             bio = this.memory[this.reg_ip + 1],
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
            offsetSize = ++scale;
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
            int imm = 0;
            
            for(int i = 0; i < offsetSize; i++) {
                imm |= this.memory[this.reg_ip + 2 + i] << (i * 8);
            }
            
            addr += imm;
        }
        
        System.out.println(String.format("final address: %08X", addr));
        
        return addr;
    }
    
    /**
     * Updates the instruction pointer based on the instruction
     * 
     * @param op
     */
    private void updateIP(Opcode op) {
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
                System.out.println("zero arguments ip += 1");
                return;
            
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
                System.out.println("one byte argument ip += 2");
                this.reg_ip += 1;
                return;
            
            // 2 bytes
            case MOV_I_I16:     case MOV_J_I16:     case MOV_A_I16:     case MOV_B_I16:     case MOV_C_I16:
            case MOV_D_I16:
            case ADD_A_I16:     case ADD_B_I16:     case ADD_C_I16:     case ADD_D_I16:     case ADC_A_I16:
            case ADC_B_I16:     case ADC_C_I16:     case ADC_D_I16:
            case SUB_A_I16:     case SUB_B_I16:     case SUB_C_I16:     case SUB_D_I16:     case SBB_A_I16:
            case SBB_B_I16:     case SBB_C_I16:     case SBB_D_I16:
            case JMP_I16:       case CALL_I16:      case INT_I16:
                System.out.println("two byte argument ip += 3");
                this.reg_ip += 2;
                return;
            
            // 4 bytes
            case MOV_BP_I32:    case MOV_SP_I32:
            case JMP_I32:       case JMPA_I32:      case CALLA_I32:
                System.out.println("four byte argument ip += 5");
                this.reg_ip += 4;
                return;
            
            // special cases
            case PADD_RIMP:     case PADC_RIMP:     case PSUB_RIMP:     case PSBB_RIMP:
            case PINC_RIMP:     case PICC_RIMP:     case PDEC_RIMP:     case PDCC_RIMP:
            case MULH_RIM:      case MULSH_RIM:     case PMUL_RIMP:     case PMULS_RIMP:
            case PMULH_RIMP:    case PMULSH_RIMP:
            case DIVM_RIM:      case DIVMS_RIM:     case PDIV_RIMP:     case PDIVS_RIMP:
            case PDIVM_RIMP:    case PDIVMS_RIMP:
            case CMP_RIM_I8:
                updateIPSpecialCases(op);
                return;
            
            // RIM
            default:
        }
        
        // General RIM
        byte rim = this.memory[this.reg_ip];
        
        // register register? 1 byte
        if((rim & 0x40) == 0) {
            System.out.println("register-register rim ip += 2");
            this.reg_ip += 1;
            return;
        } else {
            // what are we dealing with
            int diff = switch(rim & 0x07) {
                case 0, 4   -> ((rim & 0x80) == 0) ? 3 : 2; // immediate value
                case 1, 5   -> 5; // immediate address
                case 2, 6   -> 2; // bio
                case 3, 7   -> {  // bio + offset
                    // if index != 111, offset is 4 bytes. Otherwise offset is scale + 1 bytes
                    byte bio = this.memory[this.reg_ip + 1];
                    
                    if((bio & 0x07) == 0x07) {
                        yield (bio >> 6) + 3;
                    } else {
                        yield 6;
                    }
                }
                
                default     -> 0; // not possible
            };
            
            System.out.println("register-memory rim ip += " + (diff + 1));
            
            this.reg_ip += diff;
        }
    }
    
    /**
     * Deals with special cases for updating the IP
     * 
     * @param op
     */
    private void updateIPSpecialCases(Opcode op) {
        throw new IllegalStateException("Update IP Special Cases not implemented");
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
