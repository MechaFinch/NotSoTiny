package notsotiny.sim.ops;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum of every opcode
 * 
 * @author Mechafinch
 */
public enum Opcode {    
    NOP         (0x00, Operation.NOP),
    
    MOVW_RIM    (0x01, Operation.MOVW),
    MOVS_RIM    (0x02, Operation.MOVS),
    MOVZ_RIM    (0x03, Operation.MOVZ),
    
    MOVS_A_I8   (0x04, Operation.MOV),
    MOVS_B_I8   (0x05, Operation.MOV),
    MOVS_C_I8   (0x06, Operation.MOV),
    MOVS_D_I8   (0x07, Operation.MOV),
    
    MOV_A_I16   (0x08, Operation.MOV),
    MOV_B_I16   (0x09, Operation.MOV),
    MOV_C_I16   (0x0A, Operation.MOV),
    MOV_D_I16   (0x0B, Operation.MOV),
    MOV_I_I16   (0x0C, Operation.MOV),
    MOV_J_I16   (0x0D, Operation.MOV),
    MOV_K_I16   (0x0E, Operation.MOV),
    MOV_L_I16   (0x0F, Operation.MOV),
    
    MOV_A_BI    (0x10, Operation.MOV),
    MOV_B_BI    (0x11, Operation.MOV),
    MOV_C_BI    (0x12, Operation.MOV),
    MOV_D_BI    (0x13, Operation.MOV),
    MOV_A_BIO   (0x14, Operation.MOV),
    MOV_B_BIO   (0x15, Operation.MOV),
    MOV_C_BIO   (0x16, Operation.MOV),
    MOV_D_BIO   (0x17, Operation.MOV),
    
    MOV_BI_A    (0x18, Operation.MOV),
    MOV_BI_B    (0x19, Operation.MOV),
    MOV_BI_C    (0x1A, Operation.MOV),
    MOV_BI_D    (0x1B, Operation.MOV),
    MOV_BIO_A   (0x1C, Operation.MOV),
    MOV_BIO_B   (0x1D, Operation.MOV),
    MOV_BIO_C   (0x1E, Operation.MOV),
    MOV_BIO_D   (0x1F, Operation.MOV),
    
    MOV_A_O     (0x20, Operation.MOV),
    MOV_B_O     (0x21, Operation.MOV),
    MOV_C_O     (0x22, Operation.MOV),
    MOV_D_O     (0x23, Operation.MOV),
    MOV_O_A     (0x24, Operation.MOV),
    MOV_O_B     (0x25, Operation.MOV),
    MOV_O_C     (0x26, Operation.MOV),
    MOV_O_D     (0x27, Operation.MOV),
    
    MOV_SP_I32  (0x28, Operation.MOV),
    MOV_BP_I32  (0x29, Operation.MOV),
    MOV_RIM     (0x2A, Operation.MOV),
    XCHG_RIM    (0x2B, Operation.XCHG),
    
    MOV_A_B     (0x2C, Operation.MOV),
    MOV_A_C     (0x2D, Operation.MOV),
    MOV_A_D     (0x2E, Operation.MOV),
    MOV_B_A     (0x2F, Operation.MOV),
    MOV_B_C     (0x30, Operation.MOV),
    MOV_B_D     (0x31, Operation.MOV),
    MOV_C_A     (0x32, Operation.MOV),
    MOV_C_B     (0x33, Operation.MOV),
    MOV_C_D     (0x34, Operation.MOV),
    MOV_D_A     (0x35, Operation.MOV),
    MOV_D_B     (0x36, Operation.MOV),
    MOV_D_C     (0x37, Operation.MOV),

    MOV_AL_BL   (0x38, Operation.MOV),
    MOV_AL_CL   (0x39, Operation.MOV),
    MOV_AL_DL   (0x3A, Operation.MOV),
    MOV_BL_AL   (0x3B, Operation.MOV),
    MOV_BL_CL   (0x3C, Operation.MOV),
    MOV_BL_DL   (0x3D, Operation.MOV),
    MOV_CL_AL   (0x3E, Operation.MOV),
    MOV_CL_BL   (0x3F, Operation.MOV),
    MOV_CL_DL   (0x40, Operation.MOV),
    MOV_DL_AL   (0x41, Operation.MOV),
    MOV_DL_BL   (0x42, Operation.MOV),
    MOV_DL_CL   (0x43, Operation.MOV),
    
    PUSH_A      (0x44, Operation.PUSH),
    PUSH_B      (0x45, Operation.PUSH),
    PUSH_C      (0x46, Operation.PUSH),
    PUSH_D      (0x47, Operation.PUSH),
    PUSH_I      (0x48, Operation.PUSH),
    PUSH_J      (0x49, Operation.PUSH),
    PUSH_K      (0x4A, Operation.PUSH),
    PUSH_L      (0x4B, Operation.PUSH),
    PUSH_BP     (0x4C, Operation.PUSH),
    PUSH_SP     (0x4D, Operation.PUSH),
    PUSH_F      (0x4E, Operation.PUSH),
    PUSH_RIM    (0x4F, Operation.PUSH),
    
    PUSH_I32    (0x50, Operation.PUSH),
    PUSHA       (0x51, Operation.PUSHA),
    POPA        (0x52, Operation.POPA),
    
    POP_A       (0x54, Operation.POP),
    POP_B       (0x55, Operation.POP),
    POP_C       (0x56, Operation.POP),
    POP_D       (0x57, Operation.POP),
    POP_I       (0x58, Operation.POP),
    POP_J       (0x59, Operation.POP),
    POP_K       (0x5A, Operation.POP),
    POP_L       (0x5B, Operation.POP),
    POP_BP      (0x5C, Operation.POP),
    POP_SP      (0x5D, Operation.POP),
    POP_F       (0x5E, Operation.POP),
    POP_RIM     (0x5F, Operation.POP),
    
    AND_F_RIM   (0x60, Operation.AND),
    AND_RIM_F   (0x61, Operation.AND),
    OR_F_RIM    (0x62, Operation.OR),
    OR_RIM_F    (0x63, Operation.OR),
    XOR_F_RIM   (0x64, Operation.XOR),
    XOR_RIM_F   (0x65, Operation.XOR),
    NOT_F       (0x66, Operation.NOT),
    MOV_F_RIM   (0x67, Operation.MOV),
    MOV_RIM_F   (0x68, Operation.MOV),
    MOV_PF_RIM  (0x69, Operation.MOV),
    MOV_RIM_PF  (0x6A, Operation.MOV),
    
    LEA_RIM     (0x6B, Operation.LEA),
    CMP_RIM     (0x6C, Operation.CMP),
    CMP_RIM_I8  (0x6D, Operation.CMP),
    CMP_RIM_0   (0x6E, Operation.CMP),
    HLT         (0x6F, Operation.HLT),
    
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
    
    SUB_A_I8    (0x80, Operation.SUB),
    SUB_B_I8    (0x81, Operation.SUB),
    SUB_C_I8    (0x82, Operation.SUB),
    SUB_D_I8    (0x83, Operation.SUB),
    SUB_A_I16   (0x84, Operation.SUB),
    SUB_B_I16   (0x85, Operation.SUB),
    SUB_C_I16   (0x86, Operation.SUB),
    SUB_D_I16   (0x87, Operation.SUB),
    
    SBB_A_I8    (0x88, Operation.SBB),
    SBB_B_I8    (0x89, Operation.SBB),
    SBB_C_I8    (0x8A, Operation.SBB),
    SBB_D_I8    (0x8B, Operation.SBB),
    SBB_A_I16   (0x8C, Operation.SBB),
    SBB_B_I16   (0x8D, Operation.SBB),
    SBB_C_I16   (0x8E, Operation.SBB),
    SBB_D_I16   (0x8F, Operation.SBB),
    
    ADD_RIM     (0x90, Operation.ADD),
    ADD_RIM_I8  (0x91, Operation.ADD),
    ADC_RIM     (0x92, Operation.ADC),
    ADC_RIM_I8  (0x93, Operation.ADC),
    PADD_RIMP   (0x94, Operation.PADD),
    PADC_RIMP   (0x95, Operation.PADC),
    ADD_SP_I8   (0x96, Operation.ADD),
    
    SUB_RIM     (0x98, Operation.SUB),
    SUB_RIM_I8  (0x99, Operation.SUB),
    SBB_RIM     (0x9A, Operation.SBB),
    SBB_RIM_I8  (0x9B, Operation.SBB),
    PSUB_RIMP   (0x9C, Operation.PSUB),
    PSBB_RIMP   (0x9D, Operation.PSBB),
    SUB_SP_I8   (0x9E, Operation.SUB),
    
    MUL_RIM     (0xA0, Operation.MUL),
    MULH_RIM    (0xA2, Operation.MULH),
    MULSH_RIM   (0xA3, Operation.MULSH),
    PMUL_RIMP   (0xA4, Operation.PMUL),
    PMULH_RIMP  (0xA6, Operation.PMULH),
    PMULSH_RIMP (0xA7, Operation.PMULSH),
    
    DIV_RIM     (0xA8, Operation.DIV),
    DIVS_RIM    (0xA9, Operation.DIVS),
    DIVM_RIM    (0xAA, Operation.DIVM),
    DIVMS_RIM   (0xAB, Operation.DIVMS),
    PDIV_RIMP   (0xAC, Operation.PDIV),
    PDIVS_RIMP  (0xAD, Operation.PDIVS),
    PDIVM_RIMP  (0xAE, Operation.PDIVM),
    PDIVMS_RIMP (0xAF, Operation.PDIVMS),
    
    INC_I       (0xB0, Operation.INC),
    INC_J       (0xB1, Operation.INC),
    INC_K       (0xB2, Operation.INC),
    INC_L       (0xB3, Operation.INC),
    ICC_I       (0xB4, Operation.ICC),
    ICC_J       (0xB5, Operation.ICC),
    ICC_K       (0xB6, Operation.ICC),
    ICC_L       (0xB7, Operation.ICC),
    DEC_I       (0xB8, Operation.DEC),
    DEC_J       (0xB9, Operation.DEC),
    DEC_K       (0xBA, Operation.DEC),
    DEC_L       (0xBB, Operation.DEC),
    DCC_I       (0xBC, Operation.DCC),
    DCC_J       (0xBD, Operation.DCC),
    DCC_K       (0xBE, Operation.DCC),
    DCC_L       (0xBF, Operation.DCC),
    
    INC_RIM     (0xC0, Operation.INC),
    ICC_RIM     (0xC1, Operation.ICC),
    PINC_RIMP   (0xC2, Operation.PINC),
    PICC_RIMP   (0xC3, Operation.PICC),
    DEC_RIM     (0xC4, Operation.DEC),
    DCC_RIM     (0xC5, Operation.DCC),
    PDEC_RIMP   (0xC6, Operation.PDCC),
    PDCC_RIMP   (0xC7, Operation.PDCC),
    
    SHL_RIM     (0xC8, Operation.SHL),
    SHR_RIM     (0xC9, Operation.SHR),
    SAR_RIM     (0xCA, Operation.SAR),
    ROL_RIM     (0xCB, Operation.ROL),
    ROR_RIM     (0xCC, Operation.ROR),
    RCL_RIM     (0xCD, Operation.RCL),
    RCR_RIM     (0xCE, Operation.RCR),
    NEG_RIM     (0xCF, Operation.NEG),
    
    AND_RIM     (0xD0, Operation.AND),
    OR_RIM      (0xD1, Operation.OR),
    XOR_RIM     (0xD2, Operation.XOR),
    NOT_RIM     (0xD3, Operation.NOT),
    
    CALL_I16    (0xD4, Operation.CALL),
    CALL_RIM    (0xD5, Operation.CALL),
    CALLA_I32   (0xD6, Operation.CALLA),
    CALLA_RIM32 (0xD7, Operation.CALLA),
    
    JMP_I8      (0xD8, Operation.JMP),
    JMP_I16     (0xD9, Operation.JMP),
    JMP_I32     (0xDA, Operation.JMP),
    JMP_RIM     (0xDB, Operation.JMP),
    JMPA_I32    (0xDC, Operation.JMPA),
    JMPA_RIM32  (0xDD, Operation.JMPA),
    
    RET         (0xE0, Operation.RET),
    IRET        (0xE1, Operation.IRET),
    INT_I8      (0xE2, Operation.INT),
    INT_RIM     (0xE3, Operation.INT),
    
    JC_I8       (0xE4, Operation.JCC),
    JC_RIM      (0xE5, Operation.JCC),
    JNC_I8      (0xE6, Operation.JCC),
    JNC_RIM     (0xE7, Operation.JCC),
    
    JS_I8       (0xE8, Operation.JCC),
    JS_RIM      (0xE9, Operation.JCC),
    JNS_I8      (0xEA, Operation.JCC),
    JNS_RIM     (0xEB, Operation.JCC),
    
    JO_I8       (0xEC, Operation.JCC),
    JO_RIM      (0xED, Operation.JCC),
    JNO_I8      (0xEE, Operation.JCC),
    JNO_RIM     (0xEF, Operation.JCC),
    
    JZ_I8       (0xF0, Operation.JCC),
    JZ_RIM      (0xF1, Operation.JCC),
    JNZ_I8      (0xF2, Operation.JCC),
    JNZ_RIM     (0xF3, Operation.JCC),
    
    JA_I8       (0xF4, Operation.JCC),
    JA_RIM      (0xF5, Operation.JCC),
    
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
            opMap.put(op.op, op);
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
