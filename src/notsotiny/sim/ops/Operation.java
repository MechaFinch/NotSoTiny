package notsotiny.sim.ops;

/**
 * Groups opcodes by mnemonic
 * 
 * @author Mechafinch
 */
public enum Operation {
    INV     (Family.INV),
    
    NOP     (Family.MISC),
    HLT     (Family.MISC),
    LEA     (Family.MISC),
    
    MOV     (Family.MOVE),
    MOVS    (Family.MOVE),
    MOVZ    (Family.MOVE),
    CMOVCC  (Family.MOVE),
    PCMOVCC (Family.MOVE),
    XCHG    (Family.MOVE),
    PUSH    (Family.MOVE),
    RPUSH   (Family.MOVE),
    PUSHA   (Family.MOVE),
    POP     (Family.MOVE),
    RPOP    (Family.MOVE),
    POPA    (Family.MOVE),
    
    AND     (Family.LOGIC),
    PAND    (Family.LOGIC),
    OR      (Family.LOGIC),
    POR     (Family.LOGIC),
    XOR     (Family.LOGIC),
    PXOR    (Family.LOGIC),
    NOT     (Family.LOGIC),
    PNOT    (Family.LOGIC),
    NEG     (Family.LOGIC),
    PNEG    (Family.LOGIC),
    TST     (Family.LOGIC),
    PTST    (Family.LOGIC),
    
    INC     (Family.ADDITION),
    ICC     (Family.ADDITION),
    PINC    (Family.ADDITION),
    PICC    (Family.ADDITION),
    
    DEC     (Family.ADDITION),
    DCC     (Family.ADDITION),
    PDEC    (Family.ADDITION),
    PDCC    (Family.ADDITION),
    
    ADD     (Family.ADDITION),
    ADC     (Family.ADDITION),
    PADD    (Family.ADDITION),
    PADC    (Family.ADDITION),
    AADJ    (Family.ADDITION),
    
    SUB     (Family.ADDITION),
    SBB     (Family.ADDITION),
    PSUB    (Family.ADDITION),
    PSBB    (Family.ADDITION),
    SADJ    (Family.ADDITION),
    
    MUL     (Family.MULTIPLICATION),
    MULH    (Family.MULTIPLICATION),
    MULSH   (Family.MULTIPLICATION),
    PMUL    (Family.MULTIPLICATION),
    PMULH   (Family.MULTIPLICATION),
    PMULSH  (Family.MULTIPLICATION),
    
    DIV     (Family.MULTIPLICATION),
    DIVS    (Family.MULTIPLICATION),
    DIVM    (Family.MULTIPLICATION),
    DIVMS   (Family.MULTIPLICATION),
    PDIV    (Family.MULTIPLICATION),
    PDIVS   (Family.MULTIPLICATION),
    PDIVM   (Family.MULTIPLICATION),
    PDIVMS  (Family.MULTIPLICATION),
    
    SHL     (Family.LOGIC),
    SHR     (Family.LOGIC),
    SAR     (Family.LOGIC),
    ROL     (Family.LOGIC),
    ROR     (Family.LOGIC),
    RCL     (Family.LOGIC),
    RCR     (Family.LOGIC),
    
    JMP     (Family.JUMP),
    JMPA    (Family.JUMP),
    CALL    (Family.JUMP),
    CALLA   (Family.JUMP),
    RET     (Family.JUMP),
    IRET    (Family.JUMP),
    INT     (Family.JUMP),
    
    CMP     (Family.JUMP),
    PCMP	(Family.JUMP),
    JCC     (Family.JUMP),
    ;
    
    private Family fam;
    
    /**
     * Constructor
     * @param fam
     */
    private Operation(Family fam) {
        this.fam = fam;
    }
    
    /**
     * Converts a mnemonic to an Operation
     * 
     * @param n
     * @return
     */
    public static Operation fromMnemonic(String n) {
        try {
            // easy ones
            return Operation.valueOf(n);
        } catch(IllegalArgumentException e) {
            // packed stuff
        	if(n.contains("4") || n.contains("8")) {
        		if(n.endsWith("4") || n.endsWith("8")) {
        			// normal packed stuff
                    return Operation.valueOf(n.substring(0, n.length() - 1));
                } else {
                	// only pcmov has a second suffix
                	return Operation.PCMOVCC;
                }
        	}
        	
        	if(n.endsWith("W")) {
        	    return fromMnemonic(n.substring(0, n.length() - 1));
        	}
            
            // JCC & aliases
            switch(n) {
                // JCC
                case "JC", "JNC", "JS", "JNS", "JO", "JNO", "JZ", "JNZ", "JE", "JNE",
                     "JA", "JNA", "JAE", "JNAE", "JB", "JNB", "JBE", "JNBE",
                     "JG", "JNG", "JGE", "JNGE", "JL", "JNL", "JLE", "JNLE":
                         return Operation.JCC;
                
                // CMOVCC
                case "CMOVC", "CMOVNC", "CMOVS", "CMOVNS", "CMOVO", "CMOVNO", "CMOVZ", "CMOVNZ", "CMOVE", "CMOVNE",
                     "CMOVA", "CMOVNA", "CMOVAE", "CMOVNAE", "CMOVB", "CMOVNB", "CMOVBE", "CMOVNBE",
                     "CMOVG", "CMOVNG", "CMOVGE", "CMOVNGE", "CMOVL", "CMOVNL", "CMOVLE", "CMOVNLE":
                         return Operation.CMOVCC;
                
                // alias MULS to MUL cause they're the same
                case "MULS":
                    return Operation.MUL;
                
                case "PMULS":
                    return Operation.PMUL;
                
                default:
                    throw new IllegalArgumentException("Invalid mnemonic " + n);
            }
            
            
        }
    }
    
    public Family getFamily() { return this.fam; }
}
