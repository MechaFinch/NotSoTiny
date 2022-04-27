package notsotiny.sim;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum of every opcode
 * 
 * @author Mechafinch
 */
public enum Opcode {    
    NOP         (0x00, Operation.NOP),
    
    MOV_I_I16   (0x04, Operation.MOV),
    MOV_J_I16   (0x05, Operation.MOV),
    MOV_SP_I32  (0x06, Operation.MOV),
    MOV_BP_I32  (0x07, Operation.MOV),
    
    MOV_A_I8    (0x08, Operation.MOV),
    MOV_B_I8    (0x09, Operation.MOV),
    MOV_C_I8    (0x0A, Operation.MOV),
    MOV_D_I8    (0x0B, Operation.MOV),
    MOV_A_I16   (0x0C, Operation.MOV),
    MOV_B_I16   (0x0D, Operation.MOV),
    MOV_C_I16   (0x0E, Operation.MOV),
    MOV_D_I16   (0x0F, Operation.MOV),
    
    MOV_RIM     (0x10, Operation.MOV),
    XCHG_RIM    (0x11, Operation.XCHG),
    
    XCHG_AH_AL  (0x12, Operation.XCHG),
    XCHG_BH_BL  (0x13, Operation.XCHG),
    XCHG_CH_CL  (0x14, Operation.XCHG),
    XCHG_DH_DL  (0x15, Operation.XCHG),
    
    MOV_A_B     (0x16, Operation.MOV),
    MOV_A_C     (0x17, Operation.MOV),
    MOV_A_D     (0x18, Operation.MOV),
    MOV_B_A     (0x19, Operation.MOV),
    MOV_B_C     (0x1A, Operation.MOV),
    MOV_B_D     (0x1B, Operation.MOV),
    MOV_C_A     (0x1C, Operation.MOV),
    MOV_C_B     (0x1D, Operation.MOV),
    MOV_C_D     (0x1E, Operation.MOV),
    MOV_D_A     (0x1F, Operation.MOV),
    MOV_D_B     (0x20, Operation.MOV),
    MOV_D_C     (0x21, Operation.MOV),

    MOV_AL_BL   (0x22, Operation.MOV),
    MOV_AL_CL   (0x23, Operation.MOV),
    MOV_AL_DL   (0x24, Operation.MOV),
    MOV_BL_AL   (0x25, Operation.MOV),
    MOV_BL_CL   (0x26, Operation.MOV),
    MOV_BL_DL   (0x27, Operation.MOV),
    MOV_CL_AL   (0x28, Operation.MOV),
    MOV_CL_BL   (0x29, Operation.MOV),
    MOV_CL_DL   (0x2A, Operation.MOV),
    MOV_DL_AL   (0x2B, Operation.MOV),
    MOV_DL_BL   (0x2C, Operation.MOV),
    MOV_DL_CL   (0x2D, Operation.MOV),
    
    XCHG_A_B    (0x2E, Operation.XCHG),
    XCHG_A_C    (0x2F, Operation.XCHG),
    XCHG_A_D    (0x30, Operation.XCHG),
    XCHG_B_C    (0x31, Operation.XCHG),
    XCHG_B_D    (0x32, Operation.XCHG),
    XCHG_C_D    (0x33, Operation.XCHG),
    
    XCHG_AL_BL  (0x34, Operation.XCHG),
    XCHG_AL_CL  (0x35, Operation.XCHG),
    XCHG_AL_DL  (0x36, Operation.XCHG),
    XCHG_BL_CL  (0x37, Operation.XCHG),
    XCHG_BL_DL  (0x38, Operation.XCHG),
    XCHG_CL_DL  (0x39, Operation.XCHG),
    
    PUSH_A      (0x3A, Operation.PUSH),
    PUSH_B      (0x3B, Operation.PUSH),
    PUSH_C      (0x3C, Operation.PUSH),
    PUSH_D      (0x3D, Operation.PUSH),
    PUSH_I      (0x3E, Operation.PUSH),
    PUSH_J      (0x3F, Operation.PUSH),
    PUSH_BP     (0x40, Operation.PUSH),
    PUSH_SP     (0x41, Operation.PUSH),
    PUSH_F      (0x42, Operation.PUSH),
    PUSH_RIM    (0x43, Operation.PUSH),
    
    POP_A       (0x44, Operation.POP),
    POP_B       (0x45, Operation.POP),
    POP_C       (0x46, Operation.POP),
    POP_D       (0x47, Operation.POP),
    POP_I       (0x48, Operation.POP),
    POP_J       (0x49, Operation.POP),
    POP_BP      (0x4A, Operation.POP),
    POP_SP      (0x4B, Operation.POP),
    POP_F       (0x4C, Operation.POP),
    POP_RIM     (0x4D, Operation.POP),
    
    AND_F_RIM   (0x4E, Operation.AND),
    AND_RIM_F   (0x4F, Operation.AND),
    OR_F_RIM    (0x50, Operation.OR),
    OR_RIM_F    (0x51, Operation.OR),
    XOR_F_RIM   (0x52, Operation.XOR),
    XOR_RIM_F   (0x53, Operation.XOR),
    NOT_F       (0x54, Operation.NOT),
    
    MOV_F_RIM   (0x56, Operation.MOV),
    MOV_RIM_F   (0x57, Operation.MOV),
    
    INC_I       (0x58, Operation.INC),
    INC_J       (0x59, Operation.INC),
    ICC_I       (0x5A, Operation.ICC),
    ICC_J       (0x5B, Operation.ICC),
    DEC_I       (0x5C, Operation.DEC),
    DEC_J       (0x5D, Operation.DEC),
    DCC_I       (0x5E, Operation.DCC),
    DCC_J       (0x5F, Operation.DCC),
    
    ADD_A_A     (0x60, Operation.ADD),
    ADD_A_B     (0x61, Operation.ADD),
    ADD_A_C     (0x62, Operation.ADD),
    ADD_A_D     (0x63, Operation.ADD),
    ADD_B_A     (0x64, Operation.ADD),
    ADD_B_B     (0x65, Operation.ADD),
    ADD_B_C     (0x66, Operation.ADD),
    ADD_B_D     (0x67, Operation.ADD),
    ADD_C_A     (0x68, Operation.ADD),
    ADD_C_B     (0x69, Operation.ADD),
    ADD_C_C     (0x6A, Operation.ADD),
    ADD_C_D     (0x6B, Operation.ADD),
    ADD_D_A     (0x6C, Operation.ADD),
    ADD_D_B     (0x6D, Operation.ADD),
    ADD_D_C     (0x6E, Operation.ADD),
    ADD_D_D     (0x6F, Operation.ADD),
    
    ADD_A_I8    (0x70, Operation.ADD),
    ADD_B_I8    (0x71, Operation.ADD),
    ADD_C_I8    (0x72, Operation.ADD),
    ADD_D_I8    (0x73, Operation.ADD),
    ADD_A_I16   (0x74, Operation.ADD),
    ADD_B_I16   (0x75, Operation.ADD),
    ADD_C_I16   (0x76, Operation.ADD),
    ADD_D_I16   (0x77, Operation.ADD),
    
    ADC_A_I8    (0x78, Operation.ADC),
    ADC_B_I8    (0x79, Operation.ADC),
    ADC_C_I8    (0x7A, Operation.ADC),
    ADC_D_I8    (0x7B, Operation.ADC),
    ADC_A_I16   (0x7C, Operation.ADC),
    ADC_B_I16   (0x7D, Operation.ADC),
    ADC_C_I16   (0x7E, Operation.ADC),
    ADC_D_I16   (0x7F, Operation.ADC),
    
    SUB_A_A     (0x80, Operation.SUB),
    SUB_A_B     (0x81, Operation.SUB),
    SUB_A_C     (0x82, Operation.SUB),
    SUB_A_D     (0x83, Operation.SUB),
    SUB_B_A     (0x84, Operation.SUB),
    SUB_B_B     (0x85, Operation.SUB),
    SUB_B_C     (0x86, Operation.SUB),
    SUB_B_D     (0x87, Operation.SUB),
    SUB_C_A     (0x88, Operation.SUB),
    SUB_C_B     (0x89, Operation.SUB),
    SUB_C_C     (0x8A, Operation.SUB),
    SUB_C_D     (0x8B, Operation.SUB),
    SUB_D_A     (0x8C, Operation.SUB),
    SUB_D_B     (0x8D, Operation.SUB),
    SUB_D_C     (0x8E, Operation.SUB),
    SUB_D_D     (0x8F, Operation.SUB),
    
    SUB_A_I8    (0x90, Operation.SUB),
    SUB_B_I8    (0x91, Operation.SUB),
    SUB_C_I8    (0x92, Operation.SUB),
    SUB_D_I8    (0x93, Operation.SUB),
    SUB_A_I16   (0x94, Operation.SUB),
    SUB_B_I16   (0x95, Operation.SUB),
    SUB_C_I16   (0x96, Operation.SUB),
    SUB_D_I16   (0x97, Operation.SUB),
    
    SBB_A_I8    (0x98, Operation.SBB),
    SBB_B_I8    (0x99, Operation.SBB),
    SBB_C_I8    (0x9A, Operation.SBB),
    SBB_D_I8    (0x9B, Operation.SBB),
    SBB_A_I16   (0x9C, Operation.SBB),
    SBB_B_I16   (0x9D, Operation.SBB),
    SBB_C_I16   (0x9E, Operation.SBB),
    SBB_D_I16   (0x9F, Operation.SBB),
    
    ADD_RIM     (0xA0, Operation.ADD),
    ADC_RIM     (0xA1, Operation.ADC),
    PADD_RIMP   (0xA2, Operation.PADD),
    PADC_RIMP   (0xA3, Operation.PADC),
    
    SUB_RIM     (0xA4, Operation.SUB),
    SBB_RIM     (0xA5, Operation.SBB),
    PSUB_RIMP   (0xA6, Operation.PSUB),
    PSBB_RIMP   (0xA7, Operation.PSBB),
    
    INC_RIM     (0xA8, Operation.INC),
    ICC_RIM     (0xA9, Operation.ICC),
    PINC_RIMP   (0xAA, Operation.PINC),
    PICC_RIMP   (0xAB, Operation.PICC),
    
    DEC_RIM     (0xAC, Operation.DEC),
    DCC_RIM     (0xAD, Operation.DCC),
    PDEC_RIMP   (0xAE, Operation.PDCC),
    PDCC_RIMP   (0xAF, Operation.PDCC),
    
    MUL_RIM     (0xB0, Operation.MUL),
    MULS_RIM    (0xB1, Operation.MULS),
    MULH_RIM    (0xB2, Operation.MULH),
    MULSH_RIM   (0xB3, Operation.MULSH),
    
    PMUL_RIMP  (0xB4, Operation.PMUL),
    PMULS_RIMP  (0xB5, Operation.PMULS),
    PMULH_RIMP  (0xB6, Operation.PMULH),
    PMULSH_RIMP (0xB7, Operation.PMULSH),
    
    DIV_RIM     (0xB8, Operation.DIV),
    DIVS_RIM    (0xB9, Operation.DIVS),
    DIVM_RIM    (0x8A, Operation.DIVM),
    DIVMS_RIM   (0x8B, Operation.DIVMS),
    
    PDIV_RIMP   (0x8C, Operation.PDIV),
    PDIVS_RIMP  (0x8D, Operation.PDIVS),
    PDIVM_RIMP  (0x8E, Operation.PDIVM),
    PDIVMS_RIMP (0x8F, Operation.PDIVMS),
    
    AND_RIM     (0xC0, Operation.AND),
    OR_RIM      (0xC1, Operation.OR),
    XOR_RIM     (0xC2, Operation.XOR),
    NOT_RIM     (0xC3, Operation.NOT),
    NEG_RIM     (0xC4, Operation.NEG),
    
    SHL_RIM     (0xC5, Operation.SHL),
    SHR_RIM     (0xC6, Operation.SHR),
    SAR_RIM     (0xC7, Operation.SAR),
    ROL_RIM     (0xC8, Operation.ROL),
    ROR_RIM     (0xC9, Operation.ROR),
    RCL_RIM     (0xCA, Operation.RCL),
    RCR_RIM     (0xCB, Operation.RCR),
    
    JMP_I8      (0xCC, Operation.JMP),
    JMP_I16     (0xCD, Operation.JMP),
    JMP_I32     (0xCE, Operation.JMP),
    JMP_RIM     (0xCF, Operation.JMP),
    JMPA_I32    (0xD0, Operation.JMPA),
    JMPA_RIM32  (0xD1, Operation.JMPA),
    
    CALL_I16    (0xD2, Operation.CALL),
    CALL_RIM    (0xD3, Operation.CALL),
    CALLA_I32   (0xD4, Operation.CALLA),
    CALLA_RIM32 (0xD5, Operation.CALLA),
    
    RET         (0xD6, Operation.RET),
    IRET        (0xD7, Operation.IRET),
    INT_I16     (0xD8, Operation.INT),
    INT_RIM     (0xD9, Operation.INT),
    
    LEA_RIM     (0xDA, Operation.LEA),
    CMP_RIM     (0xDB, Operation.CMP),
    CMP_RIM_I8  (0xDC, Operation.CMP),
    CMP_RIM_0   (0xDD, Operation.CMP),
    
    JC_I8       (0xE0, Operation.JCC),
    JC_RIM      (0xE1, Operation.JCC),
    JNC_I8      (0xE2, Operation.JCC),
    JNC_RIM     (0xE3, Operation.JCC),
    
    JS_I8       (0xE4, Operation.JCC),
    JS_RIM      (0xE5, Operation.JCC),
    JNS_I8      (0xE6, Operation.JCC),
    JNS_RIM     (0xE7, Operation.JCC),
    
    JO_I8       (0xE8, Operation.JCC),
    JO_RIM      (0xE9, Operation.JCC),
    JNO_I8      (0xEA, Operation.JCC),
    JNO_RIM     (0xEB, Operation.JCC),
    
    JZ_I8       (0xEC, Operation.JCC),
    JZ_RIM      (0xED, Operation.JCC),
    JNZ_I8      (0xEE, Operation.JCC),
    JNZ_RIM     (0xEF, Operation.JCC),
    
    JA_I8       (0xF0, Operation.JCC),
    JA_RIM      (0xF1, Operation.JCC),
    JAE_I8      (0xF2, Operation.JCC),
    JAE_RIM     (0xF3, Operation.JCC),
    
    JB_I8       (0xF4, Operation.JCC),
    JB_RIM      (0xF5, Operation.JCC),
    JBE_I8      (0xF6, Operation.JCC),
    JBE_RIM     (0xF7, Operation.JCC),
    
    JG_I8       (0xF8, Operation.JCC),
    JG_RIM      (0xF9, Operation.JCC),
    JGE_I8      (0xFA, Operation.JCC),
    JGE_RIM     (0xFB, Operation.JCC),
    
    JL_I8       (0xFC, Operation.JCC),
    JL_RIM      (0xFD, Operation.JCC),
    JLE_I8      (0xFE, Operation.JCC),
    JLE_RIM     (0xFF, Operation.JCC),
    ;
    
    
    // opcode
    private byte op;
    
    // mnemonic
    private Operation type;
    
    // value -> enum
    private static final Map<Byte, Opcode> opMap = new HashMap<>();
    
    static {
        for(Opcode op : values()) {
            opMap.put(op.getOp(), op);
        }
    }
    
    /**
     * Constructor
     * @param op
     * @param type
     */
    private Opcode(int op, Operation type) {
        this.op = (byte) op; 
        this.type = type;
    }
    
    public static Opcode fromOp(byte op) {
        return opMap.get(op);
    }
    
    public byte getOp() { return this.op; }
    public Operation getType() { return this.type; }
}
