package notsotiny.sim;

/**
 * Names for groups of opcodes. Mnemonics, basically
 * 
 * @author Mechafinch
 */
public enum Operation {
    NOP     (Family.MISC),
    
    MOV     (Family.MOVE),
    XCHG    (Family.MOVE),
    PUSH    (Family.MOVE),
    POP     (Family.MOVE),
    
    AND     (Family.LOGIC),
    OR      (Family.LOGIC),
    XOR     (Family.LOGIC),
    NOT     (Family.LOGIC),
    NEG     (Family.LOGIC),
    
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
    
    SUB     (Family.ADDITION),
    SBB     (Family.ADDITION),
    PSUB    (Family.ADDITION),
    PSBB    (Family.ADDITION),
    
    MUL     (Family.MULTIPLICATION),
    MULS    (Family.MULTIPLICATION),
    MULH    (Family.MULTIPLICATION),
    MULSH   (Family.MULTIPLICATION),
    PMUL    (Family.MULTIPLICATION),
    PMULS   (Family.MULTIPLICATION),
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
    LEA     (Family.JUMP),
    
    CMP     (Family.JUMP),
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
    
    public Family getFamily() { return this.fam; }
}
