package notsotiny.sim.ops;

/**
 * Groups opcode by their execution function
 */
public enum ExecutionGroup {
    NOP                 (),
    HLT                 (),
    MOVS                (),
    MOVZ                (),
    MOV_SHORTCUT        (),
    MOV_PROTECTED       (),
    MOV                 (),
    CMOV                (),
    XCHG                (),
    PUSH_SHORTCUT       (),
    POP_SHORTCUT        (),
    PUSH                (),
    RPUSH               (),
    POP                 (),
    RPOP                (),
    PUSHA               (),
    POPA                (),
    TST                 (),
    F_OPS               (),
    CMP                 (),
    PCMP                (),
    ADD_SHORTCUT        (),
    ADD                 (),
    ADC                 (),
    PADD                (),
    SUB_SHORTCUT        (),
    SUB                 (),
    SBB                 (),
    PSUB                (),
    ADJ                 (),
    INC                 (),
    PINC                (),
    MUL                 (),
    DIV                 (),
    SHIFT               (),
    LOGIC               (),
    NEG                 (),
    CALL                (),
    CALLA               (),
    JMP                 (),
    JMPA                (),
    RET                 (),
    IRET                (),
    INT                 (),
    JCC                 (),
    UNDEF               (),
    ;
    
    private ExecutionGroup() {
    }
}
