package notsotiny.sim;

import notsotiny.sim.memory.MemoryManager;
import notsotiny.sim.memory.UnprivilegedAccessException;
import notsotiny.sim.ops.Opcode;
import notsotiny.sim.ops.Operation;

/**
 * Simulates the NotSoTiny architecture
 * 
 * Second iteration. Very clean code, but poorer performance
 * 
 * @author Mechafinch
 */
public class NotSoTinySimulatorV2 {
    
    private class DecodingException extends Exception { private static final long serialVersionUID = 1L; }
    
    private class GPFException extends Exception { private static final long serialVersionUID = 1L; }
    
    // Vectors for interrupts fired by the processor
    private static final byte VECTOR_MEMORY_ERROR = 0x08,
                              VECTOR_DIVISION_ERROR = 0x0D,
                              VECTOR_DECODING_ERROR = 0x0F,
                              VECTOR_GENERAL_PROTECTION_FAULT = 0x10,
                              VECTOR_MEMORY_PROTECTION_FAULT = 0x11;
    
    // Vectors for interrupts allowed to be fired by unprivileged code
    private static final byte VECTOR_SYSCALL = 0x20;
    
    // MMU
    private MemoryManager memory;
    
    // 32 bit registers
    private int reg_ip,
                reg_sp,
                reg_bp,
                reg_isp;
    
    // 16 bit registers
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
    private boolean pf_ie,  // Interrupts Enabled
                    pf_pv,  // Privilege
                    pf_ii;  // In Interrupt
    
    // Other State
    private boolean halted,
                    pendingExternalInterrupt;
    
    private InstructionDescriptor cid; // Current Instruction Descriptor
    
    private byte externalInterruptVector;
    
    // Instruction Readahead Buffer
    private int previousIP;
    private byte[] fetchBuffer;
    
    /**
     * Create a simulator instance with the given starting IP
     */
    public NotSoTinySimulatorV2(MemoryManager memory, int entry) {
        this.memory = memory;
        this.reg_ip = entry;
        
        this.reg_a = 0;
        this.reg_b = 0;
        this.reg_c = 0;
        this.reg_d = 0;
        this.reg_i = 0;
        this.reg_j = 0;
        this.reg_k = 0;
        this.reg_l = 0;
        this.reg_f = 0;
        
        this.reg_sp = 0;
        this.reg_bp = 0;
        this.reg_isp = 0;
        
        this.pf_ie = false;
        this.pf_pv = true;
        this.pf_ii = true;
        
        this.halted = false;
        this.pendingExternalInterrupt = false;
        this.externalInterruptVector = 0;
        this.cid = new InstructionDescriptor();
        
        this.previousIP = this.reg_ip;
        this.fetchBuffer = new byte[8];
    }
    
    /**
     * Create a simulator instance with starting IP memory[0]
     */
    public NotSoTinySimulatorV2(MemoryManager memory) {
        this(memory, memory.read4BytesPrivileged(0));
    }
    
    /**
     * Execute 1 instruction.
     */
    public synchronized void step() {
        synchronized(this.memory) {
            // Check interrupts
            if(this.pendingExternalInterrupt) {
                this.pendingExternalInterrupt = false;
                runInterrupt(this.externalInterruptVector);
                return;
            }
            
            // Do the instruction
            try {
                runFetch();
                runDecode();
                runExecute();
            } catch(GPFException e) {
                this.reg_ip = this.previousIP;
                System.out.printf("General Protection Fault: %08X\n", this.reg_ip);
                if(e.getMessage() != null) System.out.println(e.getMessage());
                runInterrupt(VECTOR_GENERAL_PROTECTION_FAULT);
            } catch(UnprivilegedAccessException e) {
                this.reg_ip = this.previousIP;
                System.out.printf("Memory Protection Fault: %08X\n", this.reg_ip);
                if(e.getMessage() != null) System.out.println(e.getMessage());
                runInterrupt(VECTOR_MEMORY_PROTECTION_FAULT);
            } catch(DecodingException e) {
                this.reg_ip = this.previousIP;
                System.out.printf("Decoding Error: %08X\n", this.reg_ip);
                if(e.getMessage() != null) System.out.println(e.getMessage());
                runInterrupt(VECTOR_DECODING_ERROR);
            } catch(ArithmeticException e) {
                this.reg_ip = this.previousIP;
                System.out.printf("Division Error: %08X\n", this.reg_ip);
                if(e.getMessage() != null) System.out.println(e.getMessage());
                runInterrupt(VECTOR_DIVISION_ERROR);
            } catch(IndexOutOfBoundsException e) {
                this.reg_ip = this.previousIP;
                System.out.printf("Memory Error: %08X\n", this.reg_ip);
                if(e.getMessage() != null) System.out.println(e.getMessage());
                runInterrupt(VECTOR_MEMORY_ERROR);
            }
        }
    }
    
    /*
     * Execution Stages
     */
    
    /**
     * Update the Instruction Readahead Buffer
     * @throws UnprivilegedAccessException
     */
    private void runFetch() throws UnprivilegedAccessException {
        byte[] readArr;
        int delta = this.reg_ip - this.previousIP;
        this.previousIP = this.reg_ip;
        
        // Move fetch buffer to match current IP, and fill gaps
        switch(delta) {
            case -7:
                this.fetchBuffer[7] = this.fetchBuffer[0];
                
                readArr = this.memory.read4ByteArray(this.reg_ip + 0, this.pf_pv);
                this.fetchBuffer[0] = readArr[0];
                this.fetchBuffer[1] = readArr[1];
                this.fetchBuffer[2] = readArr[2];
                this.fetchBuffer[3] = readArr[3];
                
                readArr = this.memory.read3ByteArray(this.reg_ip + 4, this.pf_pv);
                this.fetchBuffer[4] = readArr[0];
                this.fetchBuffer[5] = readArr[1];
                this.fetchBuffer[6] = readArr[2];
                break;
            
            case -6:
                this.fetchBuffer[7] = this.fetchBuffer[1];
                this.fetchBuffer[6] = this.fetchBuffer[0];
                
                readArr = this.memory.read4ByteArray(this.reg_ip + 0, this.pf_pv);
                this.fetchBuffer[0] = readArr[0];
                this.fetchBuffer[1] = readArr[1];
                this.fetchBuffer[2] = readArr[2];
                this.fetchBuffer[3] = readArr[3];
                
                readArr = this.memory.read2ByteArray(this.reg_ip + 4, this.pf_pv);
                this.fetchBuffer[4] = readArr[0];
                this.fetchBuffer[5] = readArr[1];
                break;
                
            case -5:
                this.fetchBuffer[7] = this.fetchBuffer[2];
                this.fetchBuffer[6] = this.fetchBuffer[1];
                this.fetchBuffer[5] = this.fetchBuffer[0];
                
                readArr = this.memory.read4ByteArray(this.reg_ip + 0, this.pf_pv);
                this.fetchBuffer[0] = readArr[0];
                this.fetchBuffer[1] = readArr[1];
                this.fetchBuffer[2] = readArr[2];
                this.fetchBuffer[3] = readArr[3];
                
                this.fetchBuffer[4] = this.memory.readByte(reg_ip + 4, this.pf_pv);
                break;
                
            case -4:
                this.fetchBuffer[7] = this.fetchBuffer[3];
                this.fetchBuffer[6] = this.fetchBuffer[2];
                this.fetchBuffer[5] = this.fetchBuffer[1];
                this.fetchBuffer[4] = this.fetchBuffer[0];
                
                readArr = this.memory.read4ByteArray(this.reg_ip + 0, this.pf_pv);
                this.fetchBuffer[0] = readArr[0];
                this.fetchBuffer[1] = readArr[1];
                this.fetchBuffer[2] = readArr[2];
                this.fetchBuffer[3] = readArr[3];
                break;
                
            case -3:
                this.fetchBuffer[7] = this.fetchBuffer[4];
                this.fetchBuffer[6] = this.fetchBuffer[3];
                this.fetchBuffer[5] = this.fetchBuffer[2];
                this.fetchBuffer[4] = this.fetchBuffer[1];
                this.fetchBuffer[3] = this.fetchBuffer[0];
                
                readArr = this.memory.read3ByteArray(this.reg_ip + 0, this.pf_pv);
                this.fetchBuffer[0] = readArr[0];
                this.fetchBuffer[1] = readArr[1];
                this.fetchBuffer[2] = readArr[2];
                break;
            
            case -2:
                this.fetchBuffer[7] = this.fetchBuffer[5];
                this.fetchBuffer[6] = this.fetchBuffer[4];
                this.fetchBuffer[5] = this.fetchBuffer[3];
                this.fetchBuffer[4] = this.fetchBuffer[2];
                this.fetchBuffer[3] = this.fetchBuffer[1];
                this.fetchBuffer[2] = this.fetchBuffer[0];
                
                readArr = this.memory.read2ByteArray(this.reg_ip + 0, this.pf_pv);
                this.fetchBuffer[0] = readArr[0];
                this.fetchBuffer[1] = readArr[1];
                break;
                
            case -1:
                this.fetchBuffer[7] = this.fetchBuffer[6];
                this.fetchBuffer[6] = this.fetchBuffer[5];
                this.fetchBuffer[5] = this.fetchBuffer[4];
                this.fetchBuffer[4] = this.fetchBuffer[3];
                this.fetchBuffer[3] = this.fetchBuffer[2];
                this.fetchBuffer[2] = this.fetchBuffer[1];
                this.fetchBuffer[1] = this.fetchBuffer[0];
                this.fetchBuffer[0] = this.memory.readByte(this.reg_ip + 0, this.pf_pv);
                break;
            
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
    }
    
    /**
     * Does instruction decode
     * 
     * @throws UnprivilegedAccessException
     * @throws DecodingException
     */
    private void runDecode() throws UnprivilegedAccessException, DecodingException {
        // Get opcode
        this.cid.reset(Opcode.fromOp(this.fetchBuffer[0]));
        
        // Decode
        switch(this.cid.opcode.dgroup) {
            case NODECODE:
                break;
                
            case RIM_NORMAL:
                // Normal RIM
                decodeRIM();
                break;
            
            case RIM_PACKED:
                // Packed RIM
                this.cid.isPacked = true;
                decodePackedRIM();
                break;
            
            case RIM_WOD:
                // Write-only destination RIM
                decodeRIMNoDestRead();
                break;
            
            case RIM_WIDEDST_WOD:
                // Wide write-only destination RIM
                decodeRIMWideDestNoRead();
                break;
            
            case RIM_WOD_EI8:
                // Write-only destination RIM with EI8
                this.cid.hasEI8 = true;
                decodeRIMNoDestReadEI8();
                break;
            
            case RIM_PACKED_EI8:
                // Packed RIM with EI8
                this.cid.hasEI8 = true;
                this.cid.isPacked = true;
                decodePackedRIMEI8();
                break;
            
            case RIM_WIDEDST:
                // Wide destination RIM
                decodeRIMWideDest();
                break;
                
            case RIM_PACKED_WIDEDST:
                // Packed wide destination RIM
                this.cid.isPacked = true;
                decodePackedRIMWideDest();
                break;
            
            case RIM_WIDE:
                // Wide source & wide destination RIM
                decodeWideRIM();
                break;
                
            case RIM_WIDE_WOD:
                // Wide source & wide write-only destination RIM
                decodeWideRIMNoDestRead();
                break;
            
            case RIM_SO:
                // Source only RIM
                decodeRIMSourceOnly();
                break;
            
            case RIM_WIDE_SO:
                // Wide source only RIM
                decodeRIMWideSourceOnly();
                break;
            
            case RIM_DO:
                // Destination only RIM
                decodeRIMDestOnly();
                break;
                
            case RIM_PACKED_DO:
                // Packed destination only RIM
                this.cid.isPacked = true;
                decodePackedRIMDestOnly();
                break;
                
            case RIM_DO_EI8:
                // Destination only RIM with EI8 source
                this.cid.hasEI8 = true;
                decodeRIMDestOnlyEI8();
                break;
            
            case RIM_DO_WOD:
                // Write-only destination only RIM
                decodeRIMDestOnlyNoRead();
                break;
                
            case RIM_WIDEDST_DO_WOD:
                // Wide write-only destination only RIM
                decodeRIMWideDestOnlyNoRead();
                break;
            
            case RIM_LEA:
                // kinda does its own thing
                decodeLEA();
                break;
            
            case BI_SRC:
                // word BIO source
                this.cid.sourceValue = this.memory.read2Bytes(decodeBIO(), this.pf_pv);
                break;
                
            case BI_DST:
                // destination BIO
                this.cid.destinationAddress = decodeBIO();
                break;
            
            case BIO_SRC:
                // word BIO-offset source
                this.cid.hasOffset = true;
                this.cid.sourceValue = this.memory.read2Bytes(decodeBIO(), this.pf_pv);
                break;
                
            case BIO_DST:
                // destination BIO-offset
                this.cid.hasOffset = true;
                this.cid.destinationAddress = decodeBIO();
                break;
                
            case O_SRC:
                // Source offset
                this.cid.sourceValue = this.memory.read2Bytes(decodeOffset(4), this.pf_pv);
                break;
                
            case O_DST:
                // Destination offset
                this.cid.destinationAddress = decodeOffset(4);
                break;
            
            case I8:
                // i8 source only
                this.cid.sourceValue = decodeOffset(1);
                this.cid.sourceDescriptor = new LocationDescriptor(LocationType.IMMEDIATE, this.cid.sourceValue, LocationSize.BYTE);
                break;
            
            case I16:
                // i16 source only
                this.cid.sourceValue = decodeOffset(2);
                this.cid.sourceDescriptor = new LocationDescriptor(LocationType.IMMEDIATE, this.cid.sourceValue, LocationSize.WORD);
                break;
                
            case I32:
                // i32 source only
                this.cid.sourceValue = decodeOffset(4);
                this.cid.sourceDescriptor = new LocationDescriptor(LocationType.IMMEDIATE, this.cid.sourceValue, LocationSize.DWORD);
                break;
            
            case UNDEF:
            default:
                // Undefined opcode
                throw new DecodingException();
        }
        
        //System.out.printf("IP: %08X\n", this.reg_ip);
        //this.cid.print();
        this.reg_ip += this.cid.instructionSize;
    }
    
    /**
     * Decode normal RIM
     * @throws UnprivilegedAccessException 
     * @throws DecodingException 
     */
    private void decodeRIM() throws UnprivilegedAccessException, DecodingException {
        decodeRIMNoDestRead();
        this.cid.destinationValue = readLocation(this.cid.destinationDescriptor);
    }
    
    /**
     * Decode normal RIM without reading the destination
     * @throws DecodingException 
     * @throws UnprivilegedAccessException 
     */
    private void decodeRIMNoDestRead() throws DecodingException, UnprivilegedAccessException {
        byte rimByte = this.fetchBuffer[this.cid.instructionSize++];
        
        // Get parts
        boolean s = (rimByte & 0x80) != 0;
        boolean r = (rimByte & 0x40) != 0;
        int reg = ((rimByte >> 3) & 0x07);
        int rim = (rimByte & 0x07);
        
        if(r) {
            // rim is memory
            if(rim >= 0x04) {
                // rim is destination
                if(s) {
                    this.cid.sourceDescriptor = decodeByteRegField(reg);
                    this.cid.destinationDescriptor = decodeRimField(LocationSize.BYTE, rim);                    
                } else {
                    this.cid.sourceDescriptor = decodeWordRegField(reg);
                    this.cid.destinationDescriptor = decodeRimField(LocationSize.WORD, rim);                    
                }
            } else {
                // rim is source
                if(s) {
                    this.cid.sourceDescriptor = decodeRimField(LocationSize.BYTE, rim);
                    this.cid.destinationDescriptor = decodeByteRegField(reg);
                } else {
                    this.cid.sourceDescriptor = decodeRimField(LocationSize.WORD, rim);
                    this.cid.destinationDescriptor = decodeWordRegField(reg);
                }
            }
        } else {
            // rim is register source
            if(s) {
                this.cid.sourceDescriptor = decodeByteRegField(rim);
                this.cid.destinationDescriptor = decodeByteRegField(reg);
            } else {
                this.cid.sourceDescriptor = decodeWordRegField(rim);
                this.cid.destinationDescriptor = decodeWordRegField(reg);
            }
        }
        
        this.cid.sourceValue = readLocation(this.cid.sourceDescriptor);
    }
    
    /**
     * Decode normal RIM without reading the destination. Read EI8.
     * @throws DecodingException 
     * @throws UnprivilegedAccessException 
     */
    private void decodeRIMNoDestReadEI8() throws DecodingException, UnprivilegedAccessException {
        decodeRIMNoDestRead();
        this.cid.i8Byte = this.fetchBuffer[this.cid.instructionSize++];
    }
    
    /**
     * Decode RIM with wide destination
     * @throws UnprivilegedAccessException 
     * @throws DecodingException 
     */
    private void decodeRIMWideDest() throws UnprivilegedAccessException, DecodingException {
        decodeRIMWideDestNoRead();
        this.cid.destinationValue = readLocation(this.cid.destinationDescriptor);
    }
    
    /**
     * Decode wide RIM destination without reading it
     * @throws UnprivilegedAccessException 
     * @throws DecodingException 
     */
    private void decodeRIMWideDestNoRead() throws UnprivilegedAccessException, DecodingException {
        byte rimByte = this.fetchBuffer[this.cid.instructionSize++];
        
        // Get parts
        boolean s = (rimByte & 0x80) != 0;
        boolean r = (rimByte & 0x40) != 0;
        int reg = ((rimByte >> 3) & 0x07);
        int rim = (rimByte & 0x07);
        
        if(r) {
            // rim is memory
            if(rim >= 0x04) {
                // rim is destination
                if(s) {
                    this.cid.sourceDescriptor = decodeByteRegField(reg);
                    this.cid.destinationDescriptor = decodeRimField(LocationSize.WORD, rim);                    
                } else {
                    this.cid.sourceDescriptor = decodeWordRegField(reg);
                    this.cid.destinationDescriptor = decodeRimField(LocationSize.DWORD, rim);                    
                }
            } else {
                // rim is source
                if(s) {
                    this.cid.sourceDescriptor = decodeRimField(LocationSize.BYTE, rim);
                    this.cid.destinationDescriptor = decodeWordRegField(reg);
                } else {
                    this.cid.sourceDescriptor = decodeRimField(LocationSize.WORD, rim);
                    this.cid.destinationDescriptor = decodeDwordRegField(reg);
                }
            }
        } else {
            // rim is register source
            if(s) {
                this.cid.sourceDescriptor = decodeByteRegField(rim);
                this.cid.destinationDescriptor = decodeWordRegField(reg);
            } else {
                this.cid.sourceDescriptor = decodeWordRegField(rim);
                this.cid.destinationDescriptor = decodeDwordRegField(reg);
            }
        }
        
        this.cid.sourceValue = readLocation(this.cid.sourceDescriptor);
    }
    
    /**
     * Decode RIM with wide source & wide destination
     * @throws UnprivilegedAccessException 
     * @throws DecodingException 
     */
    private void decodeWideRIM() throws UnprivilegedAccessException, DecodingException {
        decodeWideRIMNoDestRead();
        this.cid.destinationValue = readLocation(this.cid.destinationDescriptor);
    }
    
    /**
     * Decode wide RIM, don't read destination
     * @throws UnprivilegedAccessException 
     * @throws DecodingException 
     */
    private void decodeWideRIMNoDestRead() throws UnprivilegedAccessException, DecodingException {
        byte rimByte = this.fetchBuffer[this.cid.instructionSize++];
        
        // Get parts
        boolean s = (rimByte & 0x80) != 0;
        boolean r = (rimByte & 0x40) != 0;
        int reg = ((rimByte >> 3) & 0x07);
        int rim = (rimByte & 0x07);
        
        if(r) {
            // rim is memory
            if(rim >= 0x04) {
                // rim is destination
                if(s) {
                    this.cid.sourceDescriptor = decodeWordRegField(reg);
                    this.cid.destinationDescriptor = decodeRimField(LocationSize.WORD, rim);                    
                } else {
                    this.cid.sourceDescriptor = decodeDwordRegField(reg);
                    this.cid.destinationDescriptor = decodeRimField(LocationSize.DWORD, rim);                    
                }
            } else {
                // rim is source
                if(s) {
                    this.cid.sourceDescriptor = decodeRimField(LocationSize.WORD, rim);
                    this.cid.destinationDescriptor = decodeWordRegField(reg);
                } else {
                    this.cid.sourceDescriptor = decodeRimField(LocationSize.DWORD, rim);
                    this.cid.destinationDescriptor = decodeDwordRegField(reg);
                }
            }
        } else {
            // rim is register source
            if(s) {
                this.cid.sourceDescriptor = decodeWordRegField(rim);
                this.cid.destinationDescriptor = decodeWordRegField(reg);
            } else {
                this.cid.sourceDescriptor = decodeDwordRegField(rim);
                this.cid.destinationDescriptor = decodeDwordRegField(reg);
            }
        }
        
        this.cid.sourceValue = readLocation(this.cid.sourceDescriptor);
    }
    
    /**
     * Decode RIM source
     * @throws UnprivilegedAccessException 
     * @throws DecodingException 
     */
    private void decodeRIMSourceOnly() throws UnprivilegedAccessException, DecodingException {
        byte rimByte = this.fetchBuffer[this.cid.instructionSize++];
        
        // Get parts
        boolean s = (rimByte & 0x80) != 0;
        boolean r = (rimByte & 0x40) != 0;
        int reg = ((rimByte >> 3) & 0x07);
        int rim = (rimByte & 0x07);
        
        if(r) {
            // rim is memory
            if(rim >= 0x04) {
                // rim is destination
                if(s) {
                    this.cid.sourceDescriptor = decodeByteRegField(reg);                    
                } else {
                    this.cid.sourceDescriptor = decodeWordRegField(reg);                    
                }
            } else {
                // rim is source
                if(s) {
                    this.cid.sourceDescriptor = decodeRimField(LocationSize.BYTE, rim);
                } else {
                    this.cid.sourceDescriptor = decodeRimField(LocationSize.WORD, rim);
                }
            }
        } else {
            // rim is register source
            if(s) {
                this.cid.sourceDescriptor = decodeByteRegField(rim);
            } else {
                this.cid.sourceDescriptor = decodeWordRegField(rim);
            }
        }
        
        this.cid.sourceValue = readLocation(this.cid.sourceDescriptor);
    }
    
    /**
     * Decode wide RIM source
     * @throws UnprivilegedAccessException 
     * @throws DecodingException 
     */
    private void decodeRIMWideSourceOnly() throws UnprivilegedAccessException, DecodingException {
        byte rimByte = this.fetchBuffer[this.cid.instructionSize++];
        
        // Get parts
        boolean s = (rimByte & 0x80) != 0;
        boolean r = (rimByte & 0x40) != 0;
        int reg = ((rimByte >> 3) & 0x07);
        int rim = (rimByte & 0x07);
        
        if(r) {
            // rim is memory
            if(rim >= 0x04) {
                // rim is destination
                if(s) {
                    this.cid.sourceDescriptor = decodeWordRegField(reg);                    
                } else {
                    this.cid.sourceDescriptor = decodeDwordRegField(reg);                    
                }
            } else {
                // rim is source
                if(s) {
                    this.cid.sourceDescriptor = decodeRimField(LocationSize.WORD, rim);
                } else {
                    this.cid.sourceDescriptor = decodeRimField(LocationSize.DWORD, rim);
                }
            }
        } else {
            // rim is register source
            if(s) {
                this.cid.sourceDescriptor = decodeWordRegField(rim);
            } else {
                this.cid.sourceDescriptor = decodeDwordRegField(rim);
            }
        }
        
        this.cid.sourceValue = readLocation(this.cid.sourceDescriptor);
    }
    
    /**
     * Decode RIM destination
     * @throws UnprivilegedAccessException 
     * @throws DecodingException 
     */
    private void decodeRIMDestOnly() throws UnprivilegedAccessException, DecodingException {
        decodeRIMDestOnlyNoRead();
        this.cid.destinationValue = readLocation(this.cid.destinationDescriptor);
    }
    
    /**
     * Decode RIM destination. EI8 is source
     * @throws UnprivilegedAccessException 
     * @throws DecodingException 
     */
    private void decodeRIMDestOnlyEI8() throws UnprivilegedAccessException, DecodingException {
        decodeRIMDestOnly();
        this.cid.sourceValue = this.fetchBuffer[this.cid.instructionSize++];
    }
    
    /**
     * Decode RIM destination without reading it
     * @throws DecodingException 
     */
    private void decodeRIMDestOnlyNoRead() throws DecodingException {
        byte rimByte = this.fetchBuffer[this.cid.instructionSize++];
        
        // Get parts
        boolean s = (rimByte & 0x80) != 0;
        boolean r = (rimByte & 0x40) != 0;
        int reg = ((rimByte >> 3) & 0x07);
        int rim = (rimByte & 0x07);
        
        if(r) {
            // rim is memory
            if(rim >= 0x04) {
                // rim is destination
                if(s) {
                    this.cid.destinationDescriptor = decodeRimField(LocationSize.BYTE, rim);                    
                } else {
                    this.cid.destinationDescriptor = decodeRimField(LocationSize.WORD, rim);                    
                }
            } else {
                // rim is source
                if(s) {
                    this.cid.destinationDescriptor = decodeByteRegField(reg);
                } else {
                    this.cid.destinationDescriptor = decodeWordRegField(reg);
                }
            }
        } else {
            // rim is register source
            if(s) {
                this.cid.destinationDescriptor = decodeByteRegField(reg);
            } else {
                this.cid.destinationDescriptor = decodeWordRegField(reg);
            }
        }
    }
    
    /**
     * Decode wide RIM destination without reading it
     * @throws DecodingException 
     */
    private void decodeRIMWideDestOnlyNoRead() throws DecodingException {
        byte rimByte = this.fetchBuffer[this.cid.instructionSize++];
        
        // Get parts
        boolean s = (rimByte & 0x80) != 0;
        boolean r = (rimByte & 0x40) != 0;
        int reg = ((rimByte >> 3) & 0x07);
        int rim = (rimByte & 0x07);
        
        if(r) {
            // rim is memory
            if(rim >= 0x04) {
                // rim is destination
                if(s) {
                    this.cid.destinationDescriptor = decodeRimField(LocationSize.WORD, rim);                    
                } else {
                    this.cid.destinationDescriptor = decodeRimField(LocationSize.DWORD, rim);                    
                }
            } else {
                // rim is source
                if(s) {
                    this.cid.destinationDescriptor = decodeWordRegField(reg);
                } else {
                    this.cid.destinationDescriptor = decodeDwordRegField(reg);
                }
            }
        } else {
            // rim is register source
            if(s) {
                this.cid.destinationDescriptor = decodeWordRegField(reg);
            } else {
                this.cid.destinationDescriptor = decodeDwordRegField(reg);
            }
        }
    }
    
    /**
     * Decode RIM. Soruce value is source address. Destination should be a register.
     * @throws DecodingException 
     */
    private void decodeLEA() throws DecodingException {
        byte rimByte = this.fetchBuffer[this.cid.instructionSize++];
        
        // Get parts
        boolean s = (rimByte & 0x80) != 0;
        boolean r = (rimByte & 0x40) != 0;
        int reg = ((rimByte >> 3) & 0x07);
        int rim = (rimByte & 0x07);
        
        if(r && rim < 0x04 && !s) {
            // rim is memory source
            this.cid.sourceDescriptor = decodeRimField(LocationSize.WORD, rim);
            this.cid.sourceValue = this.cid.sourceDescriptor.address;
            this.cid.destinationDescriptor = decodeDwordRegField(reg);
        } else {
            // invalid
            throw new DecodingException();
        }
    }
    
    /**
     * Decode packed RIM
     * @throws UnprivilegedAccessException 
     * @throws DecodingException 
     */
    private void decodePackedRIM() throws UnprivilegedAccessException, DecodingException {
        byte rimByte = this.fetchBuffer[this.cid.instructionSize++];
        
        // Get parts
        boolean s = (rimByte & 0x80) != 0;
        boolean r = (rimByte & 0x40) != 0;
        int reg = ((rimByte >> 3) & 0x07);
        int rim = (rimByte & 0x07);
        
        if(s) {
            this.cid.packedIs4s = true;                    
        }
        
        if(r) {
            // rim is memory
            if(rim >= 0x04) {
                // rim is destination
                this.cid.sourceDescriptor = decodeWordRegField(reg);
                this.cid.destinationDescriptor = decodeRimField(LocationSize.WORD, rim);
            } else {
                // rim is source
                this.cid.sourceDescriptor = decodeRimField(LocationSize.WORD, rim);
                this.cid.destinationDescriptor = decodeWordRegField(reg);
            }
        } else {
            // rim is register source
            this.cid.sourceDescriptor = decodeWordRegField(rim);
            this.cid.destinationDescriptor = decodeWordRegField(reg);
        }
        
        this.cid.sourceValue = readLocation(this.cid.sourceDescriptor);
        this.cid.destinationValue = readLocation(this.cid.destinationDescriptor);
    }
    
    /**
     * Decode packed RIM. Read EI8
     * @throws UnprivilegedAccessException 
     * @throws DecodingException 
     */
    private void decodePackedRIMEI8() throws UnprivilegedAccessException, DecodingException {
        byte rimByte = this.fetchBuffer[this.cid.instructionSize++];
        
        // Get parts
        boolean s = (rimByte & 0x80) != 0;
        boolean r = (rimByte & 0x40) != 0;
        int reg = ((rimByte >> 3) & 0x07);
        int rim = (rimByte & 0x07);
        
        if(s) {
            this.cid.packedIs4s = true;                    
        }
        
        if(r) {
            // rim is memory
            if(rim >= 0x04) {
                // rim is destination
                this.cid.sourceDescriptor = decodeWordRegField(reg);
                this.cid.destinationDescriptor = decodeRimField(LocationSize.WORD, rim);
            } else {
                // rim is source
                this.cid.sourceDescriptor = decodeRimField(LocationSize.WORD, rim);
                this.cid.destinationDescriptor = decodeWordRegField(reg);
            }
        } else {
            // rim is register source
            this.cid.sourceDescriptor = decodeWordRegField(rim);
            this.cid.destinationDescriptor = decodeWordRegField(reg);
        }
        
        this.cid.sourceValue = readLocation(this.cid.sourceDescriptor);
        this.cid.destinationValue = readLocation(this.cid.destinationDescriptor);
        this.cid.i8Byte = this.fetchBuffer[this.cid.instructionSize++];
    }
    
    /**
     * Decode packed RIM with wide destination
     * @throws UnprivilegedAccessException 
     * @throws DecodingException 
     */
    private void decodePackedRIMWideDest() throws UnprivilegedAccessException, DecodingException {
        byte rimByte = this.fetchBuffer[this.cid.instructionSize++];
        
        // Get parts
        boolean s = (rimByte & 0x80) != 0;
        boolean r = (rimByte & 0x40) != 0;
        int reg = ((rimByte >> 3) & 0x07);
        int rim = (rimByte & 0x07);
        
        if(s) {
            this.cid.packedIs4s = true;                    
        }
        
        if(r) {
            // rim is memory
            if(rim >= 0x04) {
                // rim is destination
                this.cid.sourceDescriptor = decodeWordRegField(reg);
                this.cid.destinationDescriptor = decodeRimField(LocationSize.DWORD, rim);
            } else {
                // rim is source
                this.cid.sourceDescriptor = decodeRimField(LocationSize.WORD, rim);
                this.cid.destinationDescriptor = decodeDwordRegField(reg);
            }
        } else {
            // rim is register source
            this.cid.sourceDescriptor = decodeWordRegField(rim);
            this.cid.destinationDescriptor = decodeDwordRegField(reg);
        }
        
        this.cid.sourceValue = readLocation(this.cid.sourceDescriptor);
        this.cid.destinationValue = readLocation(this.cid.destinationDescriptor);
    }
    
    /**
     * Decode packed RIM destination
     * @throws UnprivilegedAccessException 
     * @throws DecodingException 
     */
    private void decodePackedRIMDestOnly() throws UnprivilegedAccessException, DecodingException {
        byte rimByte = this.fetchBuffer[this.cid.instructionSize++];
        
        // Get parts
        boolean s = (rimByte & 0x80) != 0;
        boolean r = (rimByte & 0x40) != 0;
        int reg = ((rimByte >> 3) & 0x07);
        int rim = (rimByte & 0x07);
        
        if(s) {
            this.cid.packedIs4s = true;                    
        }
        
        if(r) {
            // rim is memory
            if(rim >= 0x04) {
                // rim is destination
                this.cid.destinationDescriptor = decodeRimField(LocationSize.WORD, rim);
            } else {
                // rim is source
                this.cid.destinationDescriptor = decodeWordRegField(reg);
            }
        } else {
            // rim is register source
            this.cid.destinationDescriptor = decodeWordRegField(reg);
        }
        
        this.cid.destinationValue = readLocation(this.cid.destinationDescriptor);
    }
    
    /**
     * Decode a RIM field
     * @return
     * @throws DecodingException 
     */
    private LocationDescriptor decodeRimField(LocationSize size, int field) throws DecodingException {
        return switch(field & 0x03) {
            // immediate
            case 0  -> new LocationDescriptor(LocationType.IMMEDIATE, decodeOffset(size.bytes), size);
            // immediate address
            case 1  -> new LocationDescriptor(LocationType.MEMORY, decodeOffset(4), size);
            // base index
            case 2  -> new LocationDescriptor(LocationType.MEMORY, decodeBIO(), size);
            // base index offset
            case 3  -> {
                this.cid.hasOffset = true;
                yield new LocationDescriptor(LocationType.MEMORY, decodeBIO(), size);
            }
            default -> throw new DecodingException();
        };
    }
    
    /**
     * Decode an 8-bit reg field
     * @param field
     * @return
     * @throws DecodingException 
     */
    private LocationDescriptor decodeByteRegField(int field) throws DecodingException {
        return switch(field) {
            case 0  -> LocationDescriptor.REG_AL;
            case 1  -> LocationDescriptor.REG_BL;
            case 2  -> LocationDescriptor.REG_CL;
            case 3  -> LocationDescriptor.REG_DL;
            case 4  -> LocationDescriptor.REG_AH;
            case 5  -> LocationDescriptor.REG_BH;
            case 6  -> LocationDescriptor.REG_CH;
            case 7  -> LocationDescriptor.REG_DH;
            default -> throw new DecodingException();
        };
    }
    
    /**
     * Decode a 16-bit reg field
     * @param field
     * @return
     * @throws DecodingException 
     */
    private LocationDescriptor decodeWordRegField(int field) throws DecodingException {
        return switch(field) {
            case 0  -> LocationDescriptor.REG_A;
            case 1  -> LocationDescriptor.REG_B;
            case 2  -> LocationDescriptor.REG_C;
            case 3  -> LocationDescriptor.REG_D;
            case 4  -> LocationDescriptor.REG_I;
            case 5  -> LocationDescriptor.REG_J;
            case 6  -> LocationDescriptor.REG_K;
            case 7  -> LocationDescriptor.REG_L;
            default -> throw new DecodingException();
        };
    }
    
    /**
     * Decode a 32-bit reg field
     * @param field
     * @return
     * @throws DecodingException 
     */
    private LocationDescriptor decodeDwordRegField(int field) throws DecodingException {
        return switch(field) {
            case 0  -> LocationDescriptor.REG_DA;
            case 1  -> LocationDescriptor.REG_AB;
            case 2  -> LocationDescriptor.REG_BC;
            case 3  -> LocationDescriptor.REG_CD;
            case 4  -> LocationDescriptor.REG_JI;
            case 5  -> LocationDescriptor.REG_LK;
            case 6  -> LocationDescriptor.REG_BP;
            case 7  -> LocationDescriptor.REG_SP;
            default -> throw new DecodingException();
        };
    }
    
    /**
     * Decodes BIO
     * 
     * @param irbOffset Index of bytes after BIO
     * @param bio
     * @return Address
     * @throws UnprivilegedAccessException
     * @throws DecodingException 
     */
    private int decodeBIO() throws DecodingException {
        byte bio = this.fetchBuffer[this.cid.instructionSize++];
        
        int offsetSize = 4;
        int address = 0;
        
        boolean isIPRelative = false; // flag to correct for instruction size later
        
        // Get base, index, and offset size
        switch(bio & 0xFF) { 
            case 0x00, 0x01, 0x02, 0x03: // D:A
                offsetSize = bio + 1;
                address = (this.reg_d << 16) | (this.reg_a & 0xFFFF);
                break;
            
            case 0x04, 0x05, 0x06, 0x07: // IP
                isIPRelative = true;
                offsetSize = bio - 0x03;
                address = this.reg_ip;
                break;
            
            case 0x08, 0x09, 0x0A, 0x0B: // A:B
                offsetSize = bio - 0x07;
                address = (this.reg_a << 16) | (this.reg_b & 0xFFFF);
                break;
            
            case 0x0C, 0x0D, 0x0E, 0x0F: // IP
                isIPRelative = true;
                offsetSize = bio - 0x0B;
                address = this.reg_ip;
                break;
            
            case 0x10, 0x11, 0x12, 0x13: // B:C
                offsetSize = bio - 0x0F;
                address = (this.reg_b << 16) | (this.reg_c & 0xFFFF);
                break;
            
            case 0x14, 0x15, 0x16, 0x17: // IP
                isIPRelative = true;
                offsetSize = bio - 0x13;
                address = this.reg_ip;
                break;
            
            case 0x18, 0x19, 0x1A, 0x1B: // C:D
                offsetSize = bio - 0x17;
                address = (this.reg_c << 16) | (this.reg_d & 0xFFFF);
                break;
            
            case 0x1C, 0x1D, 0x1E, 0x1F: // IP
                isIPRelative = true;
                offsetSize = bio - 0x1B;
                address = this.reg_ip;
                break;
            
            case 0x20, 0x21, 0x22, 0x23: // J:I
                offsetSize = bio - 0x1F;
                address = (this.reg_j << 16) | (this.reg_i & 0xFFFF);
                break;
            
            case 0x24, 0x25, 0x26, 0x27: // IP + I
                isIPRelative = true;
                offsetSize = bio - 0x23;
                address = this.reg_ip + (this.reg_i & 0xFFFF);
                break;
            
            case 0x28, 0x29, 0x2A, 0x2B: // L:K
                offsetSize = bio - 0x27;
                address = (this.reg_l << 16) | (this.reg_k & 0xFFFF);
                break;
            
            case 0x2C, 0x2D, 0x2E, 0x2F: // IP + J
                isIPRelative = true;
                offsetSize = bio - 0x2B;
                address = this.reg_ip + (this.reg_j & 0xFFFF);
                break;
            
            case 0x30, 0x31, 0x32, 0x33: // BP
                offsetSize = bio - 0x2F;
                address = this.reg_bp;
                break;
            
            case 0x34, 0x35, 0x36, 0x37: // IP + K
                isIPRelative = true;
                offsetSize = bio - 0x33;
                address = this.reg_ip + (this.reg_k & 0xFFFF);
                break;
            
            case 0x38, 0x39, 0x3A, 0x3B: // SP
                offsetSize = bio - 0x37;
                address = this.reg_sp;
                break;
            
            case 0x3C, 0x3D, 0x3E, 0x3F: // IP + L
                isIPRelative = true;
                offsetSize = bio - 0x3B;
                address = this.reg_ip + (this.reg_l & 0xFFFF);
                break;
            
            case 0x40: // D:A + A
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + (this.reg_a & 0xFFFF);
                break;
            
            case 0x41: // D:A + B
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + (this.reg_b & 0xFFFF);
                break;
            
            case 0x42: // D:A + C
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + (this.reg_c & 0xFFFF);
                break;
            
            case 0x43: // D:A + D
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + (this.reg_d & 0xFFFF);
                break;
            
            case 0x44: // D:A + I
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + (this.reg_i & 0xFFFF);
                break;
            
            case 0x45: // D:A + J
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + (this.reg_j & 0xFFFF);
                break;
            
            case 0x46: // D:A + K
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + (this.reg_k & 0xFFFF);
                break;
            
            case 0x47: // D:A + L
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + (this.reg_l & 0xFFFF);
                break;
            
            case 0x48: // A:B + A
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + (this.reg_a & 0xFFFF);
                break;
            
            case 0x49: // A:B + B
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + (this.reg_b & 0xFFFF);
                break;
            
            case 0x4A: // A:B + C
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + (this.reg_c & 0xFFFF);
                break;
            
            case 0x4B: // A:B + D
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + (this.reg_d & 0xFFFF);
                break;
            
            case 0x4C: // A:B + I
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + (this.reg_i & 0xFFFF);
                break;
            
            case 0x4D: // A:B + J
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + (this.reg_j & 0xFFFF);
                break;
            
            case 0x4E: // A:B + K
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + (this.reg_k & 0xFFFF);
                break;
            
            case 0x4F: // A:B + L
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + (this.reg_l & 0xFFFF);
                break;
            
            case 0x50: // B:C + A
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + (this.reg_a & 0xFFFF);
                break;
            
            case 0x51: // B:C + B
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + (this.reg_b & 0xFFFF);
                break;
            
            case 0x52: // B:C + C
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + (this.reg_c & 0xFFFF);
                break;
            
            case 0x53: // B:C + D
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + (this.reg_d & 0xFFFF);
                break;
            
            case 0x54: // B:C + I
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + (this.reg_i & 0xFFFF);
                break;
            
            case 0x55: // B:C + J
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + (this.reg_j & 0xFFFF);
                break;
            
            case 0x56: // B:C + K
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + (this.reg_k & 0xFFFF);
                break;
            
            case 0x57: // B:C + L
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + (this.reg_l & 0xFFFF);
                break;
            
            case 0x58: // C:D + A
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + (this.reg_a & 0xFFFF);
                break;
            
            case 0x59: // C:D + B
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + (this.reg_b & 0xFFFF);
                break;
            
            case 0x5A: // C:D + C
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + (this.reg_c & 0xFFFF);
                break;
            
            case 0x5B: // C:D + D
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + (this.reg_d & 0xFFFF);
                break;
            
            case 0x5C: // C:D + I
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + (this.reg_i & 0xFFFF);
                break;
            
            case 0x5D: // C:D + J
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + (this.reg_j & 0xFFFF);
                break;
            
            case 0x5E: // C:D + K
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + (this.reg_k & 0xFFFF);
                break;
            
            case 0x5F: // C:D + L
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + (this.reg_l & 0xFFFF);
                break;
            
            case 0x60: // J:I + A
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + (this.reg_a & 0xFFFF);
                break;
            
            case 0x61: // J:I + B
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + (this.reg_b & 0xFFFF);
                break;
            
            case 0x62: // J:I + C
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + (this.reg_c & 0xFFFF);
                break;
            
            case 0x63: // J:I + D
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + (this.reg_d & 0xFFFF);
                break;
            
            case 0x64: // J:I + I
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + (this.reg_i & 0xFFFF);
                break;
            
            case 0x65: // J:I + J
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + (this.reg_j & 0xFFFF);
                break;
            
            case 0x66: // J:I + K
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + (this.reg_k & 0xFFFF);
                break;
            
            case 0x67: // J:I + L
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + (this.reg_l & 0xFFFF);
                break;
            
            case 0x68: // L:K + A
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + (this.reg_a & 0xFFFF);
                break;
            
            case 0x69: // L:K + B
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + (this.reg_b & 0xFFFF);
                break;
            
            case 0x6A: // L:K + C
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + (this.reg_c & 0xFFFF);
                break;
            
            case 0x6B: // L:K + D
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + (this.reg_d & 0xFFFF);
                break;
            
            case 0x6C: // L:K + I
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + (this.reg_i & 0xFFFF);
                break;
            
            case 0x6D: // L:K + J
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + (this.reg_j & 0xFFFF);
                break;
            
            case 0x6E: // L:K + K
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + (this.reg_k & 0xFFFF);
                break;
            
            case 0x6F: // L:K + L
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + (this.reg_l & 0xFFFF);
                break;
            
            case 0x70: // BP + A
                address = this.reg_bp + (this.reg_a & 0xFFFF);
                break;
                
            case 0x71: // BP + B
                address = this.reg_bp + (this.reg_b & 0xFFFF);
                break;
            
            case 0x72: // BP + C
                address = this.reg_bp + (this.reg_c & 0xFFFF);
                break;
                
            case 0x73: // BP + D
                address = this.reg_bp + (this.reg_d & 0xFFFF);
                break;
                
            case 0x74: // BP + I
                address = this.reg_bp + (this.reg_i & 0xFFFF);
                break;
                
            case 0x75: // BP + J
                address = this.reg_bp + (this.reg_j & 0xFFFF);
                break;
            
            case 0x76: // BP + K
                address = this.reg_bp + (this.reg_k & 0xFFFF);
                break;
                
            case 0x77: // BP + L
                address = this.reg_bp + (this.reg_l & 0xFFFF);
                break;
            
            case 0x78: // A
                address = this.reg_a & 0xFFFF;
                break;
            
            case 0x79: // B
                address = this.reg_b & 0xFFFF;
                break;
            
            case 0x7A: // C
                address = this.reg_c & 0xFFFF;
                break;
            
            case 0x7B: // D
                address = this.reg_d & 0xFFFF;
                break;
            
            case 0x7C: // I
                address = this.reg_i & 0xFFFF;
                break;
            
            case 0x7D: // J
                address = this.reg_j & 0xFFFF;
                break;
            
            case 0x7E: // K
                address = this.reg_k & 0xFFFF;
                break;
            
            case 0x7F: // L
                address = this.reg_l;
                break;
            
            case 0x80: // D:A + 2*A
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + ((this.reg_a & 0xFFFF) << 1);
                break;
            
            case 0x81: // D:A + 2*B
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + ((this.reg_b & 0xFFFF) << 1);
                break;
            
            case 0x82: // D:A + 2*C
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + ((this.reg_c & 0xFFFF) << 1);
                break;
            
            case 0x83: // D:A + 2*D
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + ((this.reg_d & 0xFFFF) << 1);
                break;
            
            case 0x84: // D:A + 2*I
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + ((this.reg_i & 0xFFFF) << 1);
                break;
            
            case 0x85: // D:A + 2*J
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + ((this.reg_j & 0xFFFF) << 1);
                break;
                
            case 0x86: // D:A + 2*K
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + ((this.reg_k & 0xFFFF) << 1);
                break;
            
            case 0x87: // D:A + 2*L
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + ((this.reg_l & 0xFFFF) << 1);
                break;
            
            case 0x88: // A:B + 2*A
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + ((this.reg_a & 0xFFFF) << 1);
                break;
            
            case 0x89: // A:B + 2*B
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + ((this.reg_b & 0xFFFF) << 1);
                break;
            
            case 0x8A: // A:B + 2*C
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + ((this.reg_c & 0xFFFF) << 1);
                break;
            
            case 0x8B: // A:B + 2*D
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + ((this.reg_d & 0xFFFF) << 1);
                break;
            
            case 0x8C: // A:B + 2*I
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + ((this.reg_i & 0xFFFF) << 1);
                break;
            
            case 0x8D: // A:B + 2*J
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + ((this.reg_j & 0xFFFF) << 1);
                break;
                
            case 0x8E: // A:B + 2*K
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + ((this.reg_k & 0xFFFF) << 1);
                break;
            
            case 0x8F: // A:B + 2*L
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + ((this.reg_l & 0xFFFF) << 1);
                break;
            
            case 0x90: // B:C + 2*A
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + ((this.reg_a & 0xFFFF) << 1);
                break;
            
            case 0x91: // B:C + 2*B
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + ((this.reg_b & 0xFFFF) << 1);
                break;
            
            case 0x92: // B:C + 2*C
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + ((this.reg_c & 0xFFFF) << 1);
                break;
            
            case 0x93: // B:C + 2*D
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + ((this.reg_d & 0xFFFF) << 1);
                break;
            
            case 0x94: // B:C + 2*I
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + ((this.reg_i & 0xFFFF) << 1);
                break;
            
            case 0x95: // B:C + 2*J
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + ((this.reg_j & 0xFFFF) << 1);
                break;
                
            case 0x96: // B:C + 2*K
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + ((this.reg_k & 0xFFFF) << 1);
                break;
            
            case 0x97: // B:C + 2*L
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + ((this.reg_l & 0xFFFF) << 1);
                break;
            
            case 0x98: // C:D + 2*A
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + ((this.reg_a & 0xFFFF) << 1);
                break;
            
            case 0x99: // C:D + 2*B
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + ((this.reg_b & 0xFFFF) << 1);
                break;
            
            case 0x9A: // C:D + 2*C
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + ((this.reg_c & 0xFFFF) << 1);
                break;
            
            case 0x9B: // C:D + 2*D
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + ((this.reg_d & 0xFFFF) << 1);
                break;
            
            case 0x9C: // C:D + 2*I
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + ((this.reg_i & 0xFFFF) << 1);
                break;
            
            case 0x9D: // C:D + 2*J
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + ((this.reg_j & 0xFFFF) << 1);
                break;
                
            case 0x9E: // C:D + 2*K
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + ((this.reg_k & 0xFFFF) << 1);
                break;
            
            case 0x9F: // C:D + 2*L
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + ((this.reg_l & 0xFFFF) << 1);
                break;
            
            case 0xA0: // J:I + 2*A
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + ((this.reg_a & 0xFFFF) << 1);
                break;
            
            case 0xA1: // J:I + 2*B
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + ((this.reg_b & 0xFFFF) << 1);
                break;
            
            case 0xA2: // J:I + 2*C
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + ((this.reg_c & 0xFFFF) << 1);
                break;
            
            case 0xA3: // J:I + 2*D
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + ((this.reg_d & 0xFFFF) << 1);
                break;
            
            case 0xA4: // J:I + 2*I
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + ((this.reg_i & 0xFFFF) << 1);
                break;
            
            case 0xA5: // J:I + 2*J
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + ((this.reg_j & 0xFFFF) << 1);
                break;
                
            case 0xA6: // J:I + 2*K
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + ((this.reg_k & 0xFFFF) << 1);
                break;
            
            case 0xA7: // J:I + 2*L
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + ((this.reg_l & 0xFFFF) << 1);
                break;
            
            case 0xA8: // L:K + 2*A
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + ((this.reg_a & 0xFFFF) << 1);
                break;
            
            case 0xA9: // L:K + 2*B
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + ((this.reg_b & 0xFFFF) << 1);
                break;
            
            case 0xAA: // L:K + 2*C
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + ((this.reg_c & 0xFFFF) << 1);
                break;
            
            case 0xAB: // L:K + 2*D
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + ((this.reg_d & 0xFFFF) << 1);
                break;
            
            case 0xAC: // L:K + 2*I
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + ((this.reg_i & 0xFFFF) << 1);
                break;
            
            case 0xAD: // L:K + 2*J
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + ((this.reg_j & 0xFFFF) << 1);
                break;
                
            case 0xAE: // L:K + 2*K
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + ((this.reg_k & 0xFFFF) << 1);
                break;
            
            case 0xAF: // L:K + 2*L
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + ((this.reg_l & 0xFFFF) << 1);
                break;
            
            case 0xB0: // BP + 2*A
                address = this.reg_bp + ((this.reg_a & 0xFFFF) << 1);
                break;
            
            case 0xB1: // BP + 2*B
                address = this.reg_bp + ((this.reg_b & 0xFFFF) << 1);
                break;
                
            case 0xB2: // BP + 2*C
                address = this.reg_bp + ((this.reg_c & 0xFFFF) << 1);
                break;
                
            case 0xB3: // BP + 2*D
                address = this.reg_bp + ((this.reg_d & 0xFFFF) << 1);
                break;
                
            case 0xB4: // BP + 2*I
                address = this.reg_bp + ((this.reg_i & 0xFFFF) << 1);
                break;
            
            case 0xB5: // BP + 2*J
                address = this.reg_bp + ((this.reg_j & 0xFFFF) << 1);
                break;
                
            case 0xB6: // BP + 2*K
                address = this.reg_bp + ((this.reg_k & 0xFFFF) << 1);
                break;
                
            case 0xB7: // BP + 2*L
                address = this.reg_bp + ((this.reg_l & 0xFFFF) << 1);
                break;
            
            case 0xB8: // 2*A
                address = (this.reg_a & 0xFFFF) << 1;
                break;
            
            case 0xB9: // 2*B
                address = (this.reg_b & 0xFFFF) << 1;
                break;
                
            case 0xBA: // 2*C
                address = (this.reg_c & 0xFFFF) << 1;
                break;
                
            case 0xBB: // 2*D
                address = (this.reg_d & 0xFFFF) << 1;
                break;
                
            case 0xBC: // 2*I
                address = (this.reg_i & 0xFFFF) << 1;
                break;
                
            case 0xBD: // 2*J
                address = (this.reg_j & 0xFFFF) << 1;
                break;
                
            case 0xBE: // 2*K
                address = (this.reg_k & 0xFFFF) << 1;
                break;
                
            case 0xBF: // 2*L
                address = (this.reg_l & 0xFFFF) << 1;
                break;
            
            case 0xC0: // D:A + 4*A
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + ((this.reg_a & 0xFFFF) << 2);
                break;
            
            case 0xC1: // D:A + 4*B
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + ((this.reg_b & 0xFFFF) << 2);
                break;
            
            case 0xC2: // D:A + 4*C
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + ((this.reg_c & 0xFFFF) << 2);
                break;
            
            case 0xC3: // D:A + 4*D
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + ((this.reg_d & 0xFFFF) << 2);
                break;
            
            case 0xC4: // D:A + 4*I
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + ((this.reg_i & 0xFFFF) << 2);
                break;
            
            case 0xC5: // D:A + 4*J
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + ((this.reg_j & 0xFFFF) << 2);
                break;
                
            case 0xC6: // D:A + 4*K
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + ((this.reg_k & 0xFFFF) << 2);
                break;
            
            case 0xC7: // D:A + 4*L
                address = ((this.reg_d << 16) | (this.reg_a & 0xFFFF)) + ((this.reg_l & 0xFFFF) << 2);
                break;
            
            case 0xC8: // A:B + 4*A
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + ((this.reg_a & 0xFFFF) << 2);
                break;
            
            case 0xC9: // A:B + 4*B
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + ((this.reg_b & 0xFFFF) << 2);
                break;
            
            case 0xCA: // A:B + 4*C
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + ((this.reg_c & 0xFFFF) << 2);
                break;
            
            case 0xCB: // A:B + 4*D
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + ((this.reg_d & 0xFFFF) << 2);
                break;
            
            case 0xCC: // A:B + 4*I
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + ((this.reg_i & 0xFFFF) << 2);
                break;
            
            case 0xCD: // A:B + 4*J
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + ((this.reg_j & 0xFFFF) << 2);
                break;
                
            case 0xCE: // A:B + 4*K
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + ((this.reg_k & 0xFFFF) << 2);
                break;
            
            case 0xCF: // A:B + 4*L
                address = ((this.reg_a << 16) | (this.reg_b & 0xFFFF)) + ((this.reg_l & 0xFFFF) << 2);
                break;
            
            case 0xD0: // B:C + 4*A
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + ((this.reg_a & 0xFFFF) << 2);
                break;
            
            case 0xD1: // B:C + 4*B
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + ((this.reg_b & 0xFFFF) << 2);
                break;
            
            case 0xD2: // B:C + 4*C
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + ((this.reg_c & 0xFFFF) << 2);
                break;
            
            case 0xD3: // B:C + 4*D
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + ((this.reg_d & 0xFFFF) << 2);
                break;
            
            case 0xD4: // B:C + 4*I
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + ((this.reg_i & 0xFFFF) << 2);
                break;
            
            case 0xD5: // B:C + 4*J
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + ((this.reg_j & 0xFFFF) << 2);
                break;
                
            case 0xD6: // B:C + 4*K
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + ((this.reg_k & 0xFFFF) << 2);
                break;
            
            case 0xD7: // B:C + 4*L
                address = ((this.reg_b << 16) | (this.reg_c & 0xFFFF)) + ((this.reg_l & 0xFFFF) << 2);
                break;
            
            case 0xD8: // C:D + 4*A
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + ((this.reg_a & 0xFFFF) << 2);
                break;
            
            case 0xD9: // C:D + 4*B
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + ((this.reg_b & 0xFFFF) << 2);
                break;
            
            case 0xDA: // C:D + 4*C
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + ((this.reg_c & 0xFFFF) << 2);
                break;
            
            case 0xDB: // C:D + 4*D
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + ((this.reg_d & 0xFFFF) << 2);
                break;
                
            case 0xDC: // C:D + 4*I
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + ((this.reg_i & 0xFFFF) << 2);
                break;
            
            case 0xDD: // C:D + 4*J
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + ((this.reg_j & 0xFFFF) << 2);
                break;
                
            case 0xDE: // C:D + 4*K
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + ((this.reg_k & 0xFFFF) << 2);
                break;
            
            case 0xDF: // C:D + 4*L
                address = ((this.reg_c << 16) | (this.reg_d & 0xFFFF)) + ((this.reg_l & 0xFFFF) << 2);
                break;
            
            case 0xE0: // J:I + 4*A
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + ((this.reg_a & 0xFFFF) << 2);
                break;
            
            case 0xE1: // J:I + 4*B
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + ((this.reg_b & 0xFFFF) << 2);
                break;
            
            case 0xE2: // J:I + 4*C
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + ((this.reg_c & 0xFFFF) << 2);
                break;
            
            case 0xE3: // J:I + 4*D
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + ((this.reg_d & 0xFFFF) << 2);
                break;
            
            case 0xE4: // J:I + 4*I
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + ((this.reg_i & 0xFFFF) << 2);
                break;
            
            case 0xE5: // J:I + 4*J
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + ((this.reg_j & 0xFFFF) << 2);
                break;
                
            case 0xE6: // J:I + 4*K
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + ((this.reg_k & 0xFFFF) << 2);
                break;
            
            case 0xE7: // J:I + 4*L
                address = ((this.reg_j << 16) | (this.reg_i & 0xFFFF)) + ((this.reg_l & 0xFFFF) << 2);
                break;
            
            case 0xE8: // L:K + 4*A
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + ((this.reg_a & 0xFFFF) << 2);
                break;
            
            case 0xE9: // L:K + 4*B
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + ((this.reg_b & 0xFFFF) << 2);
                break;
            
            case 0xEA: // L:K + 4*C
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + ((this.reg_c & 0xFFFF) << 2);
                break;
            
            case 0xEB: // L:K + 4*D
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + ((this.reg_d & 0xFFFF) << 2);
                break;
            
            case 0xEC: // L:K + 4*I
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + ((this.reg_i & 0xFFFF) << 2);
                break;
            
            case 0xED: // L:K + 4*J
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + ((this.reg_j & 0xFFFF) << 2);
                break;
                
            case 0xEE: // L:K + 4*K
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + ((this.reg_k & 0xFFFF) << 2);
                break;
            
            case 0xEF: // L:K + 4*L
                address = ((this.reg_l << 16) | (this.reg_k & 0xFFFF)) + ((this.reg_l & 0xFFFF) << 2);
                break;
            
            case 0xF0: // BP + 4*A
                address = this.reg_bp + ((this.reg_a & 0xFFFF) << 2);
                break;
            
            case 0xF1: // BP + 4*B
                address = this.reg_bp + ((this.reg_b & 0xFFFF) << 2);
                break;
                
            case 0xF2: // BP + 4*C
                address = this.reg_bp + ((this.reg_c & 0xFFFF) << 2);
                break;
                
            case 0xF3: // BP + 4*D
                address = this.reg_bp + ((this.reg_d & 0xFFFF) << 2);
                break;
                
            case 0xF4: // BP + 4*I
                address = this.reg_bp + ((this.reg_i & 0xFFFF) << 2);
                break;
            
            case 0xF5: // BP + 4*J
                address = this.reg_bp + ((this.reg_j & 0xFFFF) << 2);
                break;
                
            case 0xF6: // BP + 4*K
                address = this.reg_bp + ((this.reg_k & 0xFFFF) << 2);
                break;
                
            case 0xF7: // BP + 4*L
                address = this.reg_bp + ((this.reg_l & 0xFFFF) << 2);
                break;
            
            case 0xF8: // 4*A
                address = (this.reg_a & 0xFFFF) << 2;
                break;
            
            case 0xF9: // 4*B
                address = (this.reg_b & 0xFFFF) << 2;
                break;
                
            case 0xFA: // 4*C
                address = (this.reg_c & 0xFFFF) << 2;
                break;
                
            case 0xFB: // 4*D
                address = (this.reg_d & 0xFFFF) << 2;
                break;
                
            case 0xFC: // 4*I
                address = (this.reg_i & 0xFFFF) << 2;
                break;
                
            case 0xFD: // 4*J
                address = (this.reg_j & 0xFFFF) << 2;
                break;
                
            case 0xFE: // 4*K
                address = (this.reg_k & 0xFFFF) << 2;
                break;
                
            case 0xFF: // 4*L
                address = (this.reg_l & 0xFFFF) << 2;
                break;
        }
        
        // Account for instruction size
        if(isIPRelative) {
            // instructionSize accounts for opcode, rim, bio
            address += this.cid.instructionSize + (this.cid.hasOffset ? offsetSize : 0) + (this.cid.hasEI8 ? 1 : 0);
        }
        
        // Get offset if applicable
        if(this.cid.hasOffset) {
            address += decodeOffset(offsetSize);
        }
        
        return address;
    }
    
    /**
     * Decodes an offset (sign-extended read from fetch buffer)
     * @param irbOffset
     * @return offset
     */
    private int decodeOffset(int bytes) throws DecodingException {
        int v = switch(bytes) {
            case 1  -> this.fetchBuffer[this.cid.instructionSize];
            case 2  -> (this.fetchBuffer[this.cid.instructionSize + 1] << 8) | (this.fetchBuffer[this.cid.instructionSize] & 0xFF);
            case 3  -> (this.fetchBuffer[this.cid.instructionSize + 2] << 16) | ((this.fetchBuffer[this.cid.instructionSize + 1] & 0xFF) << 8) | (this.fetchBuffer[this.cid.instructionSize] & 0xFF);
            case 4  -> (this.fetchBuffer[this.cid.instructionSize + 3] << 24) | ((this.fetchBuffer[this.cid.instructionSize + 2] & 0xFF) << 16) | ((this.fetchBuffer[this.cid.instructionSize + 1] & 0xFF) << 8) | (this.fetchBuffer[this.cid.instructionSize] & 0xFF);
            default -> throw new DecodingException();
        };
        
        this.cid.instructionSize += bytes;
        
        return v;
    }
    
    /**
     * Does instruction execution
     * 
     * @throws UnprivilegedAccessException
     * @throws DecodingException 
     */
    private void runExecute() throws UnprivilegedAccessException, GPFException, DecodingException {
            switch(this.cid.opcode.egroup) {
                case NOP:
                    return;
                
                case HLT:
                    this.halted = true;
                    return;
                
                case MOV:
                    writeLocation(this.cid.destinationDescriptor, this.cid.sourceValue);
                    return;
                    
                case XCHG:
                    writeLocation(this.cid.destinationDescriptor, this.cid.sourceValue);
                    writeLocation(this.cid.sourceDescriptor, this.cid.destinationValue);
                    return;
                
                case JMP:
                    this.reg_ip += this.cid.sourceValue;
                    return;
                
                case JMPA:
                    this.reg_ip = this.cid.sourceValue;
                    return;
                    
                case MOVS:
                    runMOVS();
                    return;
                
                case MOVZ:
                    runMOVZ();
                    return;
                
                case MOV_SHORTCUT_SRC:
                    runShortcutSourceMOV();
                    return;
                
                case MOV_SHORTCUT_DST:
                    runShortcutDestMOV();
                    return;
                
                case MOV_SHORTCUT_REG:
                    runShortcutMOV();
                    return;
                    
                case MOV_PROTECTED:
                    runProtectedMOV();
                    return;
                
                case CMOV:
                    runCMOV();
                    return;
                    
                case PUSH_SHORTCUT:
                    runShortcutPUSH();
                    return;
                
                case POP_SHORTCUT:
                    runShortcutPOP();
                    return;
                
                case BPP_SHORTCUT:
                    runShortcutBPP();
                    return;
                
                case PUSH:
                    runPUSH();
                    return;
                
                case BPUSH:
                    runBPUSH();
                    return;
                
                case POP:
                    runPOP();
                    return;
                
                case BPOP:
                    runBPOP();
                    return;
                
                case PUSHA:
                    runPUSHA();
                    return;
                
                case POPA:
                    runPOPA();
                    return;
                    
                case ADD_SHORTCUT:
                    runShortcutADD();
                    return;
                    
                case ADD:
                    runADD();
                    return;
                
                case ADC:
                    runADC();
                    return;
                
                case PADD:
                    runPADD();
                    return;
                
                case INC_SHORTCUT:
                    runShortcutINC();
                    return;
                
                case INC:
                    runINC();
                    return;
                
                case PINC:
                    runPINC();
                    return;
                    
                case NEG:
                    runNEG();
                    return;
                    
                case SUB_SHORTCUT:
                    runShortcutSUB();
                    return;
                
                case SUB:
                    runSUB();
                    return;
                
                case SBB:
                    runSBB();
                    return;
                
                case PSUB:
                    runPSUB();
                    return;
                    
                case MUL:
                    runMUL();
                    return;
                
                case DIV:
                    runDIV();
                    return;
                    
                case LOGIC:
                    runLogic();
                    return;
            
                case SHIFT:
                    runShifts();
                    return;
                    
                case TST:
                    runTST();
                    return;
                    
                case CMP:
                    runCMP();
                    return;
                
                case PCMP:
                    runPCMP();
                    return;
                    
                case F_OPS:
                    runFOps();
                    return;
                    
                case JCC:
                    runJCC();
                    return;
                    
                case CALL:
                    runCALL();
                    return;
                
                case CALLA:
                    runCALLA();
                    return;
                
                case RET:
                    runRET();
                    return;
                
                case IRET:
                    runIRET();
                    return;
                
                case INT:
                    runINT();
                    return;
                    
                default:
                    throw new DecodingException();
        }
    }
    
    /*
     * Instructions
     */
    
    /**
     * Sign-extended move
     * @throws UnprivilegedAccessException 
     */
    private void runMOVS() throws UnprivilegedAccessException {
        LocationDescriptor dst = switch(this.cid.opcode) {
            case MOVS_A_I8  -> LocationDescriptor.REG_A;
            case MOVS_B_I8  -> LocationDescriptor.REG_B;
            case MOVS_C_I8  -> LocationDescriptor.REG_C;
            case MOVS_D_I8  -> LocationDescriptor.REG_D;
            default         -> this.cid.destinationDescriptor;
        };
        
        int v = this.cid.sourceValue;
        
        if(this.cid.sourceDescriptor.size == LocationSize.BYTE) {
            v = (int)((byte) v);
        } else {
            v = (int)((short) v);
        }
        
        writeLocation(dst, v);
    }
    
    /**
     * Zero-extended move
     * @throws UnprivilegedAccessException
     */
    private void runMOVZ() throws UnprivilegedAccessException {
        int v = this.cid.sourceValue;
        
        if(this.cid.sourceDescriptor.size == LocationSize.BYTE) {
            v = v & 0xFF;
        } else {
            v = v & 0xFFFF;
        }
        
        writeLocation(this.cid.destinationDescriptor, v);
    }
    
    /**
     * MOV x, BI/BIO/O/I
     * @throws GPFException 
     * @throws UnprivilegedAccessException
     */
    @SuppressWarnings("incomplete-switch")
    private void runShortcutSourceMOV() throws GPFException {
        switch(this.cid.opcode) {
            case MOV_A_I16, MOV_A_BI, MOV_A_BIO, MOV_A_O:   this.reg_a = (short) this.cid.sourceValue; break;
            case MOV_B_I16, MOV_B_BI, MOV_B_BIO, MOV_B_O:   this.reg_b = (short) this.cid.sourceValue; break;
            case MOV_C_I16, MOV_C_BI, MOV_C_BIO, MOV_C_O:   this.reg_c = (short) this.cid.sourceValue; break;
            case MOV_D_I16, MOV_D_BI, MOV_D_BIO, MOV_D_O:   this.reg_d = (short) this.cid.sourceValue; break;
            case MOV_I_I16:     this.reg_i = (short) this.cid.sourceValue; break;
            case MOV_J_I16:     this.reg_j = (short) this.cid.sourceValue; break;
            case MOV_K_I16:     this.reg_k = (short) this.cid.sourceValue; break;
            case MOV_L_I16:     this.reg_l = (short) this.cid.sourceValue; break;
            case MOV_F_RIM:     this.reg_f = (short) this.cid.sourceValue; break;
        }
    }
    
    /**
     * MOV BI/BIO/O, x
     * @throws UnprivilegedAccessException
     * @throws GPFException 
     */
    @SuppressWarnings("incomplete-switch")
    private void runShortcutDestMOV() throws UnprivilegedAccessException, GPFException {
        switch(this.cid.opcode) {
            case MOV_BI_A, MOV_BIO_A, MOV_O_A:  this.memory.write2Bytes(this.cid.destinationAddress, this.reg_a, this.pf_pv); break;
            case MOV_BI_B, MOV_BIO_B, MOV_O_B:  this.memory.write2Bytes(this.cid.destinationAddress, this.reg_b, this.pf_pv); break;
            case MOV_BI_C, MOV_BIO_C, MOV_O_C:  this.memory.write2Bytes(this.cid.destinationAddress, this.reg_c, this.pf_pv); break;
            case MOV_BI_D, MOV_BIO_D, MOV_O_D:  this.memory.write2Bytes(this.cid.destinationAddress, this.reg_d, this.pf_pv); break;
            case MOV_RIM_F:                     writeLocation(this.cid.destinationDescriptor, this.reg_f); break;
        }
    }
    
    /**
     * MOV x, x
     */
    @SuppressWarnings("incomplete-switch")
    private void runShortcutMOV() {
        switch(this.cid.opcode) {
            case MOV_A_B:   this.reg_a = this.reg_b; break;
            case MOV_A_C:   this.reg_a = this.reg_c; break;
            case MOV_A_D:   this.reg_a = this.reg_d; break;
            case MOV_B_A:   this.reg_b = this.reg_a; break;
            case MOV_B_C:   this.reg_b = this.reg_c; break;
            case MOV_B_D:   this.reg_b = this.reg_d; break;
            case MOV_C_A:   this.reg_c = this.reg_a; break;
            case MOV_C_B:   this.reg_c = this.reg_b; break;
            case MOV_C_D:   this.reg_c = this.reg_d; break;
            case MOV_D_A:   this.reg_d = this.reg_a; break;
            case MOV_D_B:   this.reg_d = this.reg_b; break;
            case MOV_D_C:   this.reg_d = this.reg_c; break;
        }
    }
    
    /**
     * MOV RIM, PR
     * MOV PR, RIM
     * @throws UnprivilegedAccessException
     * @throws GPFException
     * @throws DecodingException 
     */
    private void runProtectedMOV() throws UnprivilegedAccessException, GPFException, DecodingException {
        if(!this.pf_pv) {
            throw new GPFException();
        }
        
        // Decoding is done as if it's a normal wide RIM
        if(this.cid.opcode == Opcode.MOV_PR_RIM) {
            // Write to PR
            if(this.cid.destinationDescriptor.type != LocationType.REGISTER) {
                throw new DecodingException();
            }
            
            int val = readLocation(this.cid.sourceDescriptor);
            
            switch(this.cid.destinationDescriptor.register) {
                case DA:
                    this.reg_isp = val;
                    break;
                
                case A:
                    this.setRegPF((short) val);
                    break;
                
                default:
                    // Not possible
            }
        } else {
            // Read from PR
            if(this.cid.sourceDescriptor.type != LocationType.REGISTER) {
                throw new DecodingException();
            }
            
            int val = switch(this.cid.sourceDescriptor.register) {
                case DA -> this.reg_isp;
                case A  -> this.getRegPF();
                default -> 0; // Not possible
            };
            
            writeLocation(this.cid.destinationDescriptor, val);
        }
    }
    
    /**
     * Shortcut PUSH
     * @throws UnprivilegedAccessException
     * @throws GPFException
     */
    private void runShortcutPUSH() throws UnprivilegedAccessException, GPFException {
        // Write happens before SP update so MPFs don't have side effects
        if(this.cid.opcode == Opcode.PUSH_BP) {
            this.memory.write4Bytes(this.reg_sp - 4, this.reg_bp, this.pf_pv);
            this.reg_sp -= 4;
        } else {
            short v = switch(this.cid.opcode) {
                case PUSH_A     -> this.reg_a;
                case PUSH_B     -> this.reg_b;
                case PUSH_C     -> this.reg_c;
                case PUSH_D     -> this.reg_d;
                case PUSH_I     -> this.reg_i;
                case PUSH_J     -> this.reg_j;
                case PUSH_K     -> this.reg_k;
                case PUSH_L     -> this.reg_l;
                case PUSH_F     -> this.reg_f;
                case PUSH_PF    -> getRegPFChecked();
                default -> 0;
            };
            
            this.memory.write2Bytes(this.reg_sp - 2, v, this.pf_pv);
            this.reg_sp -= 2;
        }
    }
    
    /**
     * Shortcut POP
     * @throws UnprivilegedAccessException 
     * @throws GPFException 
     */
    @SuppressWarnings("incomplete-switch")
    private void runShortcutPOP() throws UnprivilegedAccessException, GPFException {
        if(this.cid.opcode == Opcode.POP_BP) {
            this.reg_bp = this.memory.read4Bytes(this.reg_sp, this.pf_pv);
            this.reg_sp += 4;
        } else {
            short v = this.memory.read2Bytes(this.reg_sp, this.pf_pv);
            
            switch(this.cid.opcode) {
                case POP_A:     this.reg_a = v; break;
                case POP_B:     this.reg_b = v; break;
                case POP_C:     this.reg_c = v; break;
                case POP_D:     this.reg_d = v; break;
                case POP_I:     this.reg_i = v; break;
                case POP_J:     this.reg_j = v; break;
                case POP_K:     this.reg_k = v; break;
                case POP_L:     this.reg_l = v; break;
                case POP_F:     this.reg_f = v; break;
                case POP_PF:    this.setRegPFChecked(v); break;
            }
            
            this.reg_sp += 2;
        }
    }
    
    /**
     * BPUSH SP, BPOP SP
     * @throws UnprivilegedAccessException 
     */
    private void runShortcutBPP() throws UnprivilegedAccessException {
        if(this.cid.opcode == Opcode.BPUSH_SP) {
            this.memory.write4Bytes(this.reg_bp - 4, this.reg_sp, this.pf_pv);
            this.reg_bp -= 4;
        } else {
            this.reg_sp = this.memory.read4Bytes(this.reg_bp, this.pf_pv);
            this.reg_bp += 4;
        }
    }
    
    /**
     * PUSH
     * @throws UnprivilegedAccessException 
     */
    private void runPUSH() throws UnprivilegedAccessException {
        LocationSize size = this.cid.sourceDescriptor.size;
        writeMemory(size, this.reg_sp - size.bytes, this.cid.sourceValue);
        this.reg_sp -= size.bytes;
    }
    
    /**
     * BPUSH
     * @throws UnprivilegedAccessException 
     */
    private void runBPUSH() throws UnprivilegedAccessException {
        LocationSize size = this.cid.sourceDescriptor.size;
        writeMemory(size, this.reg_bp - size.bytes, this.cid.sourceValue);
        this.reg_bp -= size.bytes;
    }
    
    /**
     * POP
     * @throws UnprivilegedAccessException 
     */
    private void runPOP() throws UnprivilegedAccessException {
        LocationSize size = this.cid.destinationDescriptor.size;
        int v = readMemory(size, this.reg_sp);
        writeLocation(this.cid.destinationDescriptor, v);
        this.reg_sp += size.bytes;
    }
    
    /**
     * BPOP
     * @throws UnprivilegedAccessException 
     */
    private void runBPOP() throws UnprivilegedAccessException {
        LocationSize size = this.cid.destinationDescriptor.size;
        int v = readMemory(size, this.reg_bp);
        writeLocation(this.cid.destinationDescriptor, v);
        this.reg_bp += size.bytes;
    }
    
    /**
     * PUSHA
     * @throws UnprivilegedAccessException 
     */
    private void runPUSHA() throws UnprivilegedAccessException {
        this.memory.write2Bytes(this.reg_sp - 16, this.reg_l, this.pf_pv);
        this.memory.write2Bytes(this.reg_sp - 14, this.reg_k, this.pf_pv);
        this.memory.write2Bytes(this.reg_sp - 12, this.reg_j, this.pf_pv);
        this.memory.write2Bytes(this.reg_sp - 10, this.reg_i, this.pf_pv);
        this.memory.write2Bytes(this.reg_sp - 8, this.reg_d, this.pf_pv);
        this.memory.write2Bytes(this.reg_sp - 6, this.reg_c, this.pf_pv);
        this.memory.write2Bytes(this.reg_sp - 4, this.reg_b, this.pf_pv);
        this.memory.write2Bytes(this.reg_sp - 2, this.reg_a, this.pf_pv);
        
        this.reg_sp -= 16;
    }
    
    /**
     * POPA
     * @throws UnprivilegedAccessException 
     */
    private void runPOPA() throws UnprivilegedAccessException {
        this.reg_l = this.memory.read2Bytes(this.reg_sp + 0, this.pf_pv);
        this.reg_k = this.memory.read2Bytes(this.reg_sp + 2, this.pf_pv);
        this.reg_j = this.memory.read2Bytes(this.reg_sp + 4, this.pf_pv);
        this.reg_i = this.memory.read2Bytes(this.reg_sp + 6, this.pf_pv);
        this.reg_d = this.memory.read2Bytes(this.reg_sp + 8, this.pf_pv);
        this.reg_c = this.memory.read2Bytes(this.reg_sp + 10, this.pf_pv);
        this.reg_b = this.memory.read2Bytes(this.reg_sp + 12, this.pf_pv);
        this.reg_a = this.memory.read2Bytes(this.reg_sp + 14, this.pf_pv);
        
        this.reg_sp += 16;
    }
    
    /**
     * TST
     * @throws UnprivilegedAccessException 
     */
    private void runTST() {
        // value
        int v = this.cid.destinationValue & this.cid.sourceValue;
        int f;
        
        // flags
        if(this.cid.isPacked) {
            if(this.cid.packedIs4s) {
                f = (getLogicFlags((v >> 12) & 0x0F, 0) << 12) |
                    (getLogicFlags((v >> 8) & 0x0F, 0) << 8) |
                    (getLogicFlags((v >> 4) & 0x0F, 0) << 4) |
                    getLogicFlags(v & 0x0F, 0);
            } else {
                f = (getLogicFlags((v >> 8) & 0x00FF, 1) << 8) | getLogicFlags(v & 0x00FF, 1);
            }
        } else {
            f = getLogicFlags(v, this.cid.destinationDescriptor.size.bytes);
        }
        
        // write
        this.reg_f = (short) f;
    }
    
    /**
     * Operations on the F register
     * @throws UnprivilegedAccessException 
     */
    @SuppressWarnings("incomplete-switch")
    private void runFOps() throws UnprivilegedAccessException {
        switch(this.cid.opcode) {
            case AND_F_RIM: this.reg_f &= this.cid.sourceValue; break;
            case OR_F_RIM:  this.reg_f |= this.cid.sourceValue; break;
            case XOR_F_RIM: this.reg_f ^= this.cid.sourceValue; break;
            case AND_RIM_F: writeLocation(this.cid.destinationDescriptor, this.reg_f & this.cid.sourceValue); break;
            case OR_RIM_F:  writeLocation(this.cid.destinationDescriptor, this.reg_f | this.cid.sourceValue); break;
            case XOR_RIM_F: writeLocation(this.cid.destinationDescriptor, this.reg_f ^ this.cid.sourceValue); break;
            case NOT_F:     this.reg_f = (short) ~this.reg_f; break;
        }
    }
    
    /**
     * CMP
     */
    private void runCMP() {
        int[] res = add(this.cid.destinationValue, this.cid.sourceValue, this.cid.destinationDescriptor.size.bytes, true, false);
        this.reg_f = (short) res[1];
    }
    
    /**
     * PCMP
     */
    private void runPCMP() {
        short[] res = addPacked(this.cid.destinationValue, this.cid.sourceValue, !this.cid.packedIs4s, true, false);
        this.reg_f = res[1];
    }
    
    /**
     * ADD gpr, i8
     */
    private void runShortcutADD() {
        int v = switch(this.cid.opcode) {
            case ADD_A_I8   -> this.reg_a;
            case ADD_B_I8   -> this.reg_b;
            case ADD_C_I8   -> this.reg_c;
            case ADD_D_I8   -> this.reg_d;
            case ADD_I_I8   -> this.reg_i;
            case ADD_J_I8   -> this.reg_j;
            case ADD_K_I8   -> this.reg_k;
            case ADD_L_I8   -> this.reg_l;
            case ADD_SP_I8  -> this.reg_sp;
            case ADD_BP_I8  -> this.reg_bp;
            default         -> 0;
        };
        
        int size = (this.cid.opcode == Opcode.ADD_SP_I8 || this.cid.opcode == Opcode.ADD_BP_I8) ? 4 : 2;
        
        int[] res = add(v, this.cid.sourceValue, size, false, false);
        
        switch(this.cid.opcode) {
            case ADD_A_I8:  this.reg_a = (short) res[0]; break;
            case ADD_B_I8:  this.reg_b = (short) res[0]; break;
            case ADD_C_I8:  this.reg_c = (short) res[0]; break;
            case ADD_D_I8:  this.reg_d = (short) res[0]; break;
            case ADD_I_I8:  this.reg_i = (short) res[0]; break;
            case ADD_J_I8:  this.reg_j = (short) res[0]; break;
            case ADD_K_I8:  this.reg_k = (short) res[0]; break;
            case ADD_L_I8:  this.reg_l = (short) res[0]; break;
            case ADD_SP_I8: this.reg_sp = res[0]; break;
            case ADD_BP_I8: this.reg_bp = res[0]; break;
            default:
        }
        
        this.reg_f = (short) res[1];
    }
    
    /**
     * ADD
     * @throws UnprivilegedAccessException 
     */
    private void runADD() throws UnprivilegedAccessException {
        int[] res = add(this.cid.destinationValue, this.cid.sourceValue, this.cid.destinationDescriptor.size.bytes, false, false);
        writeLocation(this.cid.destinationDescriptor, res[0]);
        this.reg_f = (short) res[1];
    }
    
    /**
     * SUB gpr, I8
     */
    private void runShortcutSUB() {
        int v = switch(this.cid.opcode) {
            case SUB_A_I8   -> this.reg_a;
            case SUB_B_I8   -> this.reg_b;
            case SUB_C_I8   -> this.reg_c;
            case SUB_D_I8   -> this.reg_d;
            case SUB_I_I8   -> this.reg_i;
            case SUB_J_I8   -> this.reg_j;
            case SUB_K_I8   -> this.reg_k;
            case SUB_L_I8   -> this.reg_l;
            case SUB_SP_I8  -> this.reg_sp;
            case SUB_BP_I8  -> this.reg_bp;
            default         -> 0;
        };
        
        int size = (this.cid.opcode == Opcode.SUB_SP_I8 || this.cid.opcode == Opcode.SUB_BP_I8) ? 4 : 2;
        
        int[] res = add(v, this.cid.sourceValue, size, true, false);
        
        switch(this.cid.opcode) {
            case SUB_A_I8:  this.reg_a = (short) res[0]; break;
            case SUB_B_I8:  this.reg_b = (short) res[0]; break;
            case SUB_C_I8:  this.reg_c = (short) res[0]; break;
            case SUB_D_I8:  this.reg_d = (short) res[0]; break;
            case SUB_I_I8:  this.reg_i = (short) res[0]; break;
            case SUB_J_I8:  this.reg_j = (short) res[0]; break;
            case SUB_K_I8:  this.reg_k = (short) res[0]; break;
            case SUB_L_I8:  this.reg_l = (short) res[0]; break;
            case SUB_SP_I8: this.reg_sp = res[0]; break;
            case SUB_BP_I8: this.reg_bp = res[0]; break;
            default:
        }
        
        this.reg_f = (short) res[1];
    }
    
    /**
     * SUB
     * @throws UnprivilegedAccessException 
     */
    private void runSUB() throws UnprivilegedAccessException {
        int[] res = add(this.cid.destinationValue, this.cid.sourceValue, this.cid.destinationDescriptor.size.bytes, true, false);
        writeLocation(this.cid.destinationDescriptor, res[0]);
        this.reg_f = (short) res[1];
    }
    
    /**
     * ADC
     * @throws UnprivilegedAccessException
     */
    private void runADC() throws UnprivilegedAccessException {
        int[] res = add(this.cid.destinationValue, this.cid.sourceValue, this.cid.destinationDescriptor.size.bytes, false, true);
        writeLocation(this.cid.destinationDescriptor, res[0]);
        this.reg_f = (short) res[1];
    }
    
    /**
     * SBB
     * @throws UnprivilegedAccessException
     */
    private void runSBB() throws UnprivilegedAccessException {
        int[] res = add(this.cid.destinationValue, this.cid.sourceValue, this.cid.destinationDescriptor.size.bytes, true, true);
        writeLocation(this.cid.destinationDescriptor, res[0]);
        this.reg_f = (short) res[1];
    }
    
    /**
     * PADD, PADC
     * @throws UnprivilegedAccessException
     */
    private void runPADD() throws UnprivilegedAccessException {
        short[] res = addPacked(this.cid.destinationValue, this.cid.sourceValue, !this.cid.packedIs4s, false, this.cid.opcode == Opcode.PADC_RIMP);
        writeLocation(this.cid.destinationDescriptor, res[0]);
        this.reg_f = res[1];
    }
    
    /**
     * PSUB, PSBB
     * @throws UnprivilegedAccessException
     */
    private void runPSUB() throws UnprivilegedAccessException {
        short[] res = addPacked(this.cid.destinationValue, this.cid.sourceValue, !this.cid.packedIs4s, true, this.cid.opcode == Opcode.PSBB_RIMP);
        writeLocation(this.cid.destinationDescriptor, res[0]);
        this.reg_f = res[1];
    }
    
    /**
     * INC/ICC/DEC/DCC gpr
     */
    private void runShortcutINC() {
        int v = switch(this.cid.opcode) {
            case ICC_A, DCC_A               -> this.reg_a;
            case ICC_B, DCC_B               -> this.reg_b;
            case ICC_C, DCC_C               -> this.reg_c;
            case ICC_D, DCC_D               -> this.reg_d;
            case INC_I, ICC_I, DEC_I, DCC_I -> this.reg_i;
            case INC_J, ICC_J, DEC_J, DCC_J -> this.reg_j;
            case INC_K, ICC_K, DEC_K, DCC_K -> this.reg_k;
            case INC_L, ICC_L, DEC_L, DCC_L -> this.reg_l;
            default                         -> this.cid.destinationValue;
        };
        
        int i = (this.cid.opcode.getType() == Operation.INC || this.cid.opcode.getType() == Operation.DEC || (this.reg_f & 0x01) != 0) ? 1 : 0;
        boolean sub = this.cid.opcode.getType() == Operation.DEC || this.cid.opcode.getType() == Operation.DCC;
        
        int[] res = add(v, i, 2, sub, false);
        
        switch(this.cid.opcode) {
            case ICC_A, DCC_A:                  this.reg_a = (short) res[0]; break;
            case ICC_B, DCC_B:                  this.reg_b = (short) res[0]; break;
            case ICC_C, DCC_C:                  this.reg_c = (short) res[0]; break;
            case ICC_D, DCC_D:                  this.reg_d = (short) res[0]; break;
            case INC_I, ICC_I, DEC_I, DCC_I:    this.reg_i = (short) res[0]; break;
            case INC_J, ICC_J, DEC_J, DCC_J:    this.reg_j = (short) res[0]; break;
            case INC_K, ICC_K, DEC_K, DCC_K:    this.reg_k = (short) res[0]; break;
            case INC_L, ICC_L, DEC_L, DCC_L:    this.reg_l = (short) res[0]; break;
            default:
        }
        
        this.reg_f = (short) res[1];
    }
    
    /**
     * INC/DEC/ICC/DCC
     * @throws UnprivilegedAccessException 
     */
    private void runINC() throws UnprivilegedAccessException {
        int i = (this.cid.opcode == Opcode.INC_RIM || this.cid.opcode == Opcode.DEC_RIM || (this.reg_f & 0x01) != 0) ? 1 : 0;
        boolean sub = this.cid.opcode == Opcode.DEC_RIM || this.cid.opcode == Opcode.DCC_RIM;
        
        int[] res = add(this.cid.destinationValue, i, this.cid.destinationDescriptor.size.bytes, sub, false);
        writeLocation(this.cid.destinationDescriptor, res[0]);
        this.reg_f = (short) res[1];
    }
    
    /**
     * PINC/PDEC/PICC/PDCC
     * @throws UnprivilegedAccessException 
     */
    private void runPINC() throws UnprivilegedAccessException {
        boolean sub = this.cid.opcode == Opcode.PDEC_RIMP || this.cid.opcode == Opcode.PDCC_RIMP;
        int i;
        
        if(this.cid.opcode == Opcode.PINC_RIMP || this.cid.opcode == Opcode.PDEC_RIMP) {
            // unconditional
            i = this.cid.packedIs4s ? 0x1111 : 0x0101;
        } else {
            i = this.reg_f & (this.cid.packedIs4s ? 0x0101 : 0x1111);
        }
        
        short[] res = addPacked(this.cid.destinationValue, i, !this.cid.packedIs4s, sub, false);
        writeLocation(this.cid.destinationDescriptor, res[0]);
        this.reg_f = res[1];
    }
    
    /**
     * MUL & co
     * @throws UnprivilegedAccessException 
     */
    private void runMUL() throws UnprivilegedAccessException {
        boolean high = this.cid.opcode == Opcode.MULSH_RIM || this.cid.opcode == Opcode.PMULSH_RIMP || this.cid.opcode == Opcode.MULH_RIM || this.cid.opcode == Opcode.PMULH_RIMP;
        boolean signed = this.cid.opcode == Opcode.MULSH_RIM || this.cid.opcode == Opcode.PMULSH_RIMP;
        int[] res;
        
        if(this.cid.isPacked) {
            res = multiplyPacked(this.cid.destinationValue, this.cid.sourceValue, !this.cid.packedIs4s, high, signed);
        } else {
            res = multiply(this.cid.destinationValue, this.cid.sourceValue, this.cid.destinationDescriptor.size.bytes, high, signed);
            
        }
        
        writeLocation(this.cid.destinationDescriptor, res[0]);
        this.reg_f = (short) res[1];
    }
    
    /**
     * DIV & co
     */
    private void runDIV() throws UnprivilegedAccessException {
        boolean mod = this.cid.opcode == Opcode.DIVM_RIM || this.cid.opcode == Opcode.PDIVM_RIMP || this.cid.opcode == Opcode.DIVMS_RIM || this.cid.opcode == Opcode.PDIVMS_RIMP;
        boolean signed = this.cid.opcode == Opcode.DIVS_RIM || this.cid.opcode == Opcode.PDIVS_RIMP || this.cid.opcode == Opcode.DIVMS_RIM || this.cid.opcode == Opcode.PDIVMS_RIMP;
        int[] res;
        
        if(this.cid.isPacked) {
            res = dividePacked(this.cid.destinationValue, this.cid.sourceValue, !this.cid.packedIs4s, mod, signed);
        } else {
            res = divide(this.cid.destinationValue, this.cid.sourceValue, this.cid.sourceDescriptor.size.bytes, mod, signed);
        }
        
        //System.out.printf("Divsion result: Quotient %08X, Remainder %08X, Flags %04X\n", res[0], res[1], res[2]);
        
        int r = switch(this.cid.destinationDescriptor.size) {
            case BYTE   -> mod ? (((res[1] & 0x0F) << 4) | (res[0] & 0x0F)) : res[0];
            case WORD   -> mod ? (((res[1] & 0xFF) << 8) | (res[0] & 0xFF)) : res[0];
            case DWORD  -> mod ? (((res[1] & 0xFFFF) << 16) | (res[0] & 0xFFFF)) : res[0];
            default     -> 0; // NULL
        };
        
        writeLocation(this.cid.destinationDescriptor, r);
        this.reg_f = (short) res[2];
    }
    
    /**
     * Shifts
     * @throws UnprivilegedAccessException
     */
    private void runShifts() throws UnprivilegedAccessException {
        int mask = switch(this.cid.destinationDescriptor.size) {
            case BYTE   -> 0x0000_00FF;
            case WORD   -> 0x0000_FFFF;
            case DWORD  -> 0xFFFF_FFFF;
            default     -> 0;
        };
        
        int a = this.cid.destinationValue & mask;
        int b = this.cid.sourceValue & mask;
        int c = 0;
        boolean carry = (this.reg_f & 0x0001) != 0;
        
        // agh
        switch(this.cid.opcode) {
            case SHL_RIM:
            case SHL_RIM_I8:
                carry = ((switch(this.cid.destinationDescriptor.size) {
                    case BYTE   -> 0x80;
                    case WORD   -> 0x8000;
                    case DWORD  -> 0x8000_000;
                    default     -> 0;
                } >>> (b - 1)) & a) != 0;
                
                c = a << b;
                break;
            
            case SHR_RIM:
            case SHR_RIM_I8:
                carry = ((1 << (b - 1)) & a) != 0;
                
                c = switch(this.cid.destinationDescriptor.size) {
                    case BYTE   -> ((a << 24) >>> b) >>> 24;
                    case WORD   -> ((a << 16) >>> b) >>> 16;
                    case DWORD  -> a >>> b;
                    default     -> 0;
                };
                break;
                
            case SAR_RIM:
            case SAR_RIM_I8:
                carry = ((1 << (b - 1)) & a) != 0;
                
                c = switch(this.cid.destinationDescriptor.size) {
                    case BYTE   -> ((a << 24) >> b) >>> 24;
                    case WORD   -> ((a << 16) >> b) >>> 16;
                    case DWORD  -> a >> b;
                    default     -> 0;
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
                    if(this.cid.opcode == Opcode.RCL_RIM || this.cid.opcode == Opcode.RCL_RIM_I8) {
                        rot = carry ? 1 : 0;
                    }
                    
                    al <<= 1;
                    carry = (switch(this.cid.destinationDescriptor.size) {
                        case BYTE   -> 0x100l;
                        case WORD   -> 0x1_0000l;
                        case DWORD  -> 0x1_0000_0000l;
                        default     -> 0l;
                    } & al) != 0;
                    
                    // use current carry for ROL
                    if(this.cid.opcode == Opcode.ROL_RIM || this.cid.opcode == Opcode.ROL_RIM_I8) {
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
                
                int rot_in = switch(this.cid.destinationDescriptor.size) {
                    case BYTE   -> 0x80;
                    case WORD   -> 0x8000;
                    case DWORD  -> 0x8000_0000;
                    default     -> 0;
                };
                
                for(int i = 0; i < b; i++) {
                    // use previous carry for RCR
                    if(this.cid.opcode == Opcode.RCR_RIM || this.cid.opcode == Opcode.RCR_RIM_I8) {
                        rot = carry ? 1 : 0;
                    }
                    
                    carry = (al & 1) != 0;
                    al >>>= 1;
                    
                    // use current carry for ROR
                    if(this.cid.opcode == Opcode.ROR_RIM || this.cid.opcode == Opcode.ROR_RIM_I8) {
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
                sign = switch(this.cid.destinationDescriptor.size) {
                    case BYTE   -> (c & 0x80) != 0;
                    case WORD   -> (c & 0x8000) != 0;
                    case DWORD  -> (c & 0x8000_0000) != 0;
                    default     -> false;
                };
        
        short f = (short)((zero ? 0x08 : 0x00) | (overflow ? 0x04 : 0x00) | (sign ? 0x02 : 0x00) | (carry ? 0x01 : 0x00));
        
        writeLocation(this.cid.destinationDescriptor, c);
        this.reg_f = f;
    }
    
    /**
     * Bitwise logic (and negate)
     * @throws UnprivilegedAccessException 
     */
    private void runLogic() throws UnprivilegedAccessException {
        int res = switch(this.cid.opcode) {
            case AND_RIM    -> this.cid.destinationValue & this.cid.sourceValue;
            case OR_RIM     -> this.cid.destinationValue | this.cid.sourceValue;
            case XOR_RIM    -> this.cid.destinationValue ^ this.cid.sourceValue;
            case NOT_RIM    -> ~this.cid.destinationValue;
            default         -> 0;
        };
        
        writeLocation(this.cid.destinationDescriptor, res);
        this.reg_f = (short) getLogicFlags(res, this.cid.destinationDescriptor.size.bytes);
    }
    
    /**
     * Conditional relative branch
     */
    private void runJCC() {
        if(conditionTrue(this.cid.opcode, 0)) {
            this.reg_ip += this.cid.sourceValue;
        }
    }
    
    /**
     * Conditional move
     * @throws UnprivilegedAccessException 
     */
    private void runCMOV() throws UnprivilegedAccessException {
        Opcode cond = Opcode.fromOp(this.cid.i8Byte);
        
        if(this.cid.isPacked) {
            int v = this.cid.destinationValue;
            int s = this.cid.sourceValue;
            
            // If condition true, replace original with new
            if(this.cid.packedIs4s) {
                // nybbles
                if(conditionTrue(cond, 0)) {
                    //System.out.println("nybble 0");
                    v = (v & 0xFFF0) | (s & 0x000F);
                }
                
                if(conditionTrue(cond, 1)) {
                    //System.out.println("nybble 1");
                    v = (v & 0xFF0F) | (s & 0x00F0);
                }
                
                if(conditionTrue(cond, 2)) {
                    //System.out.println("nybble 2");
                    v = (v & 0xF0FF) | (s & 0x0F00);
                }
                
                if(conditionTrue(cond, 3)) {
                    //System.out.println("nybble 3");
                    v = (v & 0x0FFF) | (s & 0xF000);
                }
            } else {
                // lower
                if(conditionTrue(cond, 0)) {
                    //System.out.println("byte 0");
                    v = (v & 0xFF00) | (s & 0x00FF);
                }
                
                // upper
                if(conditionTrue(cond, 2)) {
                    //System.out.println("byte 1");
                    v = (v & 0x00FF) | (s & 0xFF00);
                }
            }
            
            writeLocation(this.cid.destinationDescriptor, v);
        } else {
            // If condition true, write new
            if(conditionTrue(cond, 0)) {
                writeLocation(this.cid.destinationDescriptor, this.cid.sourceValue);
            }
        }
    }
    
    /**
     * Relative function call
     * @throws UnprivilegedAccessException 
     */
    private void runCALL() throws UnprivilegedAccessException {
        // Push IP
        this.memory.write4Bytes(this.reg_sp - 4, this.reg_ip, this.pf_pv);
        this.reg_sp -= 4;
        
        // Branch
        this.reg_ip += this.cid.sourceValue;
    }
    
    /**
     * Absolute function call
     * @throws UnprivilegedAccessException 
     */
    private void runCALLA() throws UnprivilegedAccessException {
        // Push IP
        this.memory.write4Bytes(this.reg_sp - 4, this.reg_ip, this.pf_pv);
        this.reg_sp -= 4;
        
        // Branch
        this.reg_ip = this.cid.sourceValue;
    }
    
    /**
     * Function return
     * @throws UnprivilegedAccessException 
     */
    private void runRET() throws UnprivilegedAccessException {
        // Pop IP
        this.reg_ip = this.memory.read4Bytes(this.reg_sp, this.pf_pv);
        this.reg_sp += 4;
    }
    
    /**
     * Interrupt return
     * @throws UnprivilegedAccessException 
     * @throws GPFException 
     */
    private void runIRET() throws UnprivilegedAccessException, GPFException {
        // Check privilege
        if(!this.pf_pv) {
            throw new GPFException();
        }
        
        // Pop PF, F, SP, BP, IP
        short tmpPF = this.memory.read2Bytes(this.reg_sp + 0, this.pf_pv);
        short tmpF = this.memory.read2Bytes(this.reg_sp + 2, this.pf_pv);
        int tmpSP = this.memory.read4Bytes(this.reg_sp + 4, this.pf_pv);
        int tmpBP = this.memory.read4Bytes(this.reg_sp + 8, this.pf_pv);
        int tmpIP = this.memory.read4Bytes(this.reg_sp + 12, this.pf_pv);
        
        // Write flags now that all exception causers are done
        this.reg_f = tmpF;
        this.setRegPF(tmpPF);
        this.reg_sp = tmpSP;
        this.reg_bp = tmpBP;
        this.reg_ip = tmpIP;
    }
    
    /**
     * Software interrupt
     * @throws UnprivilegedAccessException
     * @throws GPFException
     */
    private void runINT() throws GPFException {
        byte vector = (byte) this.cid.sourceValue;
        
        // TODO
        if(!this.pf_pv) {
            // Only select software interrupts are allowed when unprivileged
            if(vector != VECTOR_SYSCALL) {
                throw new GPFException();
            }
        }
        
        runInterrupt(vector);
    }
    
    /**
     * Negate
     * @throws UnprivilegedAccessException
     */
    private void runNEG() throws UnprivilegedAccessException {
        int[] res = add(~this.cid.destinationValue, 1, this.cid.destinationDescriptor.size.bytes, false, false);
        
        writeLocation(this.cid.destinationDescriptor, res[0]);
        this.reg_f = (short) res[1];
    }
    
    /*
     * Helpers
     */
    
    /**
     * Read a location
     * 
     * @param ld
     * @return
     * @throws UnprivilegedAccessException 
     */
    private int readLocation(LocationDescriptor ld) throws UnprivilegedAccessException {
        switch(ld.type) {
            case IMMEDIATE:
                return ld.address;
                
            case MEMORY:
                return switch(ld.size) {
                    case DWORD  -> this.memory.read4Bytes(ld.address, this.pf_pv);
                    case WORD   -> this.memory.read2Bytes(ld.address, this.pf_pv);
                    case BYTE   -> this.memory.readByte(ld.address, this.pf_pv);
                    default     -> 0;
                };
                
            case REGISTER:
                return switch(ld.register) {
                    case NONE   -> 0;
                    case DA     -> (this.reg_d << 16) | (this.reg_a & 0xFFFF);
                    case AB     -> (this.reg_a << 16) | (this.reg_b & 0xFFFF);
                    case BC     -> (this.reg_b << 16) | (this.reg_c & 0xFFFF);
                    case CD     -> (this.reg_c << 16) | (this.reg_d & 0xFFFF);
                    case JI     -> (this.reg_j << 16) | (this.reg_i & 0xFFFF);
                    case LK     -> (this.reg_l << 16) | (this.reg_k & 0xFFFF);
                    case BP     -> this.reg_bp;
                    case SP     -> this.reg_sp;
                    case IP     -> this.reg_ip;
                    case A      -> this.reg_a;
                    case B      -> this.reg_b;
                    case C      -> this.reg_c;
                    case D      -> this.reg_d;
                    case I      -> this.reg_i;
                    case J      -> this.reg_j;
                    case K      -> this.reg_k;
                    case L      -> this.reg_l;
                    case AH     -> (byte)(this.reg_a >> 8);
                    case BH     -> (byte)(this.reg_b >> 8);
                    case CH     -> (byte)(this.reg_c >> 8);
                    case DH     -> (byte)(this.reg_d >> 8);
                    case AL     -> (byte) this.reg_a;
                    case BL     -> (byte) this.reg_b;
                    case CL     -> (byte) this.reg_c;
                    case DL     -> (byte) this.reg_d;
                };
                
            default: //case NULL:
                return 0;
        }
    }
    
    /**
     * Write a location
     * 
     * @param ld
     * @param value
     * @throws UnprivilegedAccessException 
     * @throws GPFException 
     */
    private void writeLocation(LocationDescriptor ld, int value) throws UnprivilegedAccessException {
        switch(ld.type) {
            case MEMORY:
                switch(ld.size) {
                    case DWORD: this.memory.write4Bytes(ld.address, value, this.pf_pv); break;
                    case WORD:  this.memory.write2Bytes(ld.address, (short) value, this.pf_pv); break;
                    case BYTE:  this.memory.writeByte(ld.address, (byte) value, this.pf_pv); break;
                    default: // NULL
                }
                break;
                
            case REGISTER:
                switch(ld.register) {
                    case DA:    this.reg_d = (short)(value >> 16); this.reg_a = (short) value; break;
                    case AB:    this.reg_a = (short)(value >> 16); this.reg_b = (short) value; break;
                    case BC:    this.reg_b = (short)(value >> 16); this.reg_c = (short) value; break;
                    case CD:    this.reg_c = (short)(value >> 16); this.reg_d = (short) value; break;
                    case JI:    this.reg_j = (short)(value >> 16); this.reg_i = (short) value; break;
                    case LK:    this.reg_l = (short)(value >> 16); this.reg_k = (short) value; break;
                    case BP:    this.reg_bp = value; break;
                    case SP:    this.reg_sp = value; break;
                    case IP:    this.reg_ip = value; break;
                    case A:     this.reg_a = (short) value; break;
                    case B:     this.reg_b = (short) value; break;
                    case C:     this.reg_c = (short) value; break;
                    case D:     this.reg_d = (short) value; break;
                    case I:     this.reg_i = (short) value; break;
                    case J:     this.reg_j = (short) value; break;
                    case K:     this.reg_k = (short) value; break;
                    case L:     this.reg_l = (short) value; break;
                    case AH:    this.reg_a = (short)((value << 8) | (this.reg_a & 0xFF)); break;
                    case BH:    this.reg_b = (short)((value << 8) | (this.reg_b & 0xFF)); break;
                    case CH:    this.reg_c = (short)((value << 8) | (this.reg_c & 0xFF)); break;
                    case DH:    this.reg_d = (short)((value << 8) | (this.reg_d & 0xFF)); break;
                    case AL:    this.reg_a = (short)((this.reg_a & 0xFF00) | (value & 0xFF)); break;
                    case BL:    this.reg_b = (short)((this.reg_b & 0xFF00) | (value & 0xFF)); break;
                    case CL:    this.reg_c = (short)((this.reg_c & 0xFF00) | (value & 0xFF)); break;
                    case DL:    this.reg_d = (short)((this.reg_d & 0xFF00) | (value & 0xFF)); break;
                    default: // NONE
                }
                break;
                
            default:
                // NULL, IMMEDAITE
        }
    }
    
    /**
     * Writes to variable-size memory location
     * @param size
     * @param value
     * @throws UnprivilegedAccessException
     */
    private void writeMemory(LocationSize size, int address, int value) throws UnprivilegedAccessException {
        switch(size) {
            case DWORD: this.memory.write4Bytes(address, value, this.pf_pv); break;
            case WORD:  this.memory.write2Bytes(address, (short) value, this.pf_pv); break;
            case BYTE:  this.memory.writeByte(address, (byte) value, this.pf_pv); break;
            default: // NULL
        }
    }
    
    /**
     * Reads from variable-size memory location
     * @param size
     * @param address
     * @return
     * @throws UnprivilegedAccessException 
     */
    private int readMemory(LocationSize size, int address) throws UnprivilegedAccessException {
        return switch(size) {
            case DWORD  -> this.memory.read4Bytes(address, this.pf_pv);
            case WORD   -> this.memory.read2Bytes(address, this.pf_pv);
            case BYTE   -> this.memory.readByte(address, this.pf_pv);
            default     -> 0;
        };
    }
    
    /**
     * Gets the flags from a logic operation
     * @param v
     * @param size
     * @return
     */
    private int getLogicFlags(int v, int size) {
        boolean zero = v == 0,
                overflow = false,
                sign = switch(size) {
                    case 0  -> (v & 0x08) != 0;
                    case 1  -> (v & 0x80) != 0;
                    case 2  -> (v & 0x8000) != 0;
                    case 4  -> (v & 0x8000_0000) != 0;
                    default -> false;
                },
                carry = false;
                
        return (zero ? 0x08 : 0x00) | (overflow ? 0x04 : 0x00) | (sign ? 0x02 : 0x00) | (carry ? 0x01 : 0x00);
    }
    
    /**
     * Adds two numbers and sets flags accordingly
     * 
     * @param a
     * @param b
     * @param size
     * @param subtract determines if B is complemented and how CF is treated. if true: b, carry in, and carry out are complemented.
     * @param includeCarry true to use flags for cin
     * @return {a+b, flags}
     */
    private int[] add(int a, int b, int size, boolean subtract, boolean includeCarry) {
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
        
        int flags = (zero ? 0x08 : 0x00) | (overflow ? 0x04 : 0x00) | (sign ? 0x02 : 0x00) | ((carry != subtract) ? 0x01 : 0x00);
        
        return new int[] { (int) c, flags };
    }
    
    /**
     * Adds packed numbers and sets flags accordingly
     * 
     * @param a
     * @param b
     * @param bytes
     * @param subtract
     * @param includeCarry
     * @return {a+b, flags}
     */
    private short[] addPacked(int a, int b, boolean bytes, boolean subtract, boolean includeCarry) {
        
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
        
        short v = 0;
        
        // put things back together
        if(bytes) {
            v = (short) (((c >> 8) & 0xFF00) | (c & 0xFF));
        } else {
            v = (short) (((c >> 12) & 0xF000) | ((c >> 8) & 0x0F00) | ((c >> 4) & 0x00F0) | (c & 0x000F));
        }
        
        return new short[] { v, (short) f };
    }
    
    /**
     * Multiplies two numbers and sets flags accordingly
     * 
     * @param a
     * @param b
     * @param size
     * @param high
     * @param signed
     * @return {a * b, flags}
     */
    private int[] multiply(int a, int b, int size, boolean high, boolean signed) {
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
                    al = (long)(high ? ((byte)(a << 4)) >> 4 : (byte) a);
                    bl = (long)(high ? ((byte)(b << 4)) >> 4 : (byte) b);
                    break;
                
                case 2:
                    al = (long)(high ? (byte) a : (short) a);
                    bl = (long)(high ? (byte) a : (short) a);
                    break;
                
                case 4:
                    al = (long)(high ? (short) a : a);
                    bl = (long)(high ? (short) b : b);
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
        
        int f = (zero ? 0x08 : 0x00) | (overflow ? 0x04 : 0x00) | (sign ? 0x02 : 0x00) | (carry ? 0x01 : 0x00);
        
        return new int[] { (int) res, f };
    }
    
    /**
     * Multiplies packed numbers and sets flags accordingly.
     * 
     * @param a
     * @param b
     * @param bytes
     * @param high
     * @param signed
     * @return {a*b, flags}
     */
    private int[] multiplyPacked(int a, int b, boolean bytes, boolean high, boolean signed) {
        // multiplication isn't local like additon, so we have to do things separately
        int r, f;
        if(bytes) {
            int[] res1 = multiply(a >> 8, b >> 8, 1, high, signed);
            int[] res2 = multiply(a, b, 1, high, signed);
            
            f = (res1[1] << 8) | res2[1];
            
            if(high) {
                r = ((res1[0] << 16) & 0xFFFF_0000) | (res2[0] & 0x0000_FFFF); 
            } else {
                r = ((res1[0] << 8) & 0xFF00) | (res2[0] & 0x00FF);
            } 
        } else {
            int[] res1 = multiply(a >> 12, b >> 12, 0, high, signed);
            int[] res2 = multiply(a >> 8, b >> 8, 0, high, signed);
            int[] res3 = multiply(a >> 4, b >> 4, 0, high, signed);
            int[] res4 = multiply(a, b, 0, high, signed);
            
            f = (res1[1] << 12) | (res2[1] << 8) | (res3[1] << 4) | res4[1];
            
            if(high) {
                r = ((res1[0] << 24) & 0xFF00_0000) | ((res2[0] << 16) & 0x00FF_0000) | ((res3[0] << 8) & 0x0000_FF00) | (res4[0] & 0x0000_00FF);
            } else {
                r = ((res1[0] << 12) & 0xF000) | ((res2[0] << 8) & 0x0F00) | ((res3[0] << 4) & 0x00F0) | (res4[0] & 0x000F); 
            }
        }
        
        return new int[] { r, f };
    }
    
    /**
     * Divides & modulos two numbers and sets flags accordingly
     * 
     * @param a
     * @param b
     * @param size
     * @param mod
     * @param signed
     * @return a / b {quotient, remainder, flags}
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
        
        //System.out.println(a);
        //System.out.println(b);
        //System.out.println(al);
        //System.out.println(bl);
        //System.out.println(quot);
        //System.out.println(rem);
        
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
        
        int f = (short)((zero ? 0x08 : 0x00) | (overflow ? 0x04 : 0x00) | (sign ? 0x02 : 0x00) | (carry ? 0x01 : 0x00));
        
        return new int[] {(int) quot, (int) rem, f};
    }
    
    /**
     * Divides & modulos packed numbers and sets flags accordingly
     * 
     * @param a
     * @param b
     * @param mod
     * @param signed
     * @param bytes
     * @return a / b {quotient, remainder, flags}
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
            
            int[] r2 = divide(a2, b2, 1, mod, signed);
            
            return new int[] {
                    ((r1[0] << 8) & 0xFF00) | (r2[0] & 0xFF),
                    ((r1[1] << 8) & 0xFF00) | (r2[1] & 0xFF),
                    (r1[2] << 8) | r2[2]
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
                    ((r1[1] << 12) & 0xF000) | ((r2[1] << 8) & 0x0F00) | ((r3[1] << 4) & 0xF0) | (r4[1] & 0x0F),
                    (r1[2] << 12) | (r2[2] << 8) | (r3[2] << 4) | r4[2]
            };
        }
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
    
    /*
     * Interrupts
     */
    
    /**
     * Execute an interrupt
     * 
     * @param vector
     * @throws UnprivilegedAccessException
     */
    private void runInterrupt(byte vector) {
        // What pointer are we using
        // In interrupt -> SP, ISP otherwise
        int pointer = (this.pf_ii) ? this.reg_sp : this.reg_isp;
        
        this.memory.write4BytesPrivileged(pointer - 4, this.reg_ip);
        this.memory.write4BytesPrivileged(pointer - 8, this.reg_bp);
        this.memory.write4BytesPrivileged(pointer - 12, this.reg_sp);
        this.memory.write4BytesPrivileged(pointer - 16, (this.reg_f << 16) | this.getRegPF());
        this.reg_sp = pointer - 16;
        
        // Get vector & jump
        this.reg_ip = this.memory.read4BytesPrivileged((vector & 0x00FFl) << 2);
        this.pf_ie = false; // Discourage interrupt nesting
        this.pf_pv = true;  // Set privilege
        this.pf_ii = true;  // We're in an interrupt now
    }
    
    /*
     * Interfacing Functions
     */
    
    /**
     * Attempts to fire an interrupt with the given vector.
     * The interrupt will be ignored if interrupts are not enabled.
     * 
     * @param vector
     * @return true if the interrupt was fired
     */
    public boolean fireMaskableInterrupt(byte vector) {
        if(this.pf_ie) {
            this.halted = false;
            this.pendingExternalInterrupt = true;
            this.externalInterruptVector = vector;
            
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Fires an interrupt with the given vector, regardless of whether interrupts are enabled.
     * 
     * @param vector
     */
    public void fireNonMaskableInterrupt(byte vector) {
        this.halted = false;
        this.pendingExternalInterrupt = true;
        this.externalInterruptVector = vector;
    }
    
    /*
     * Getters
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
    public int getRegISP() { return this.reg_isp; }
    public boolean getHalted() { return this.halted; }
    public boolean hasPendingInterrupt() { return this.pendingExternalInterrupt; }
    public byte getPendingInterruptVector() { return this.externalInterruptVector; }
    
    private short getRegPFChecked() throws GPFException {
        if(this.pf_pv) {
            return getRegPF();
        } else {
            throw new GPFException();
        }
    }
    
    public short getRegPF() {
        return (short)(
            (this.pf_ie ? 0x01 : 0x00) |
            (this.pf_pv ? 0x02 : 0x00) |
            (this.pf_ii ? 0x04 : 0x00)
        );
    }
    
    /*
     * Setters
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
    public void setRegISP(int isp) { this.reg_isp = isp; } 
    public void setHalted(boolean h) { this.halted = h; }
    
    private void setRegPFChecked(short pf) throws GPFException {
        if(this.pf_pv) {
            setRegPF(pf);
        } else {
            throw new GPFException();
        }
    }
    
    public void setRegPF(short pf) {
        this.pf_ie = (pf & 0x01) != 0;
        this.pf_pv = (pf & 0x02) != 0;
        this.pf_ii = (pf & 0x04) != 0;
    }
}
