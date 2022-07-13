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
    
    MOV_RIM     (0x28, Operation.MOV),
    XCHG_RIM    (0x29, Operation.XCHG),
    
    XCHG_AH_AL  (0x2A, Operation.XCHG),
    XCHG_BH_BL  (0x2B, Operation.XCHG),
    XCHG_CH_CL  (0x2C, Operation.XCHG),
    XCHG_DH_DL  (0x2D, Operation.XCHG),
    
    MOV_A_B     (0x2E, Operation.MOV),
    MOV_A_C     (0x2F, Operation.MOV),
    MOV_A_D     (0x30, Operation.MOV),
    MOV_B_A     (0x31, Operation.MOV),
    MOV_B_C     (0x32, Operation.MOV),
    MOV_B_D     (0x33, Operation.MOV),
    MOV_C_A     (0x34, Operation.MOV),
    MOV_C_B     (0x35, Operation.MOV),
    MOV_C_D     (0x36, Operation.MOV),
    MOV_D_A     (0x37, Operation.MOV),
    MOV_D_B     (0x38, Operation.MOV),
    MOV_D_C     (0x39, Operation.MOV),

    MOV_AL_BL   (0x3A, Operation.MOV),
    MOV_AL_CL   (0x3B, Operation.MOV),
    MOV_AL_DL   (0x3C, Operation.MOV),
    MOV_BL_AL   (0x3D, Operation.MOV),
    MOV_BL_CL   (0x3E, Operation.MOV),
    MOV_BL_DL   (0x3F, Operation.MOV),
    MOV_CL_AL   (0x40, Operation.MOV),
    MOV_CL_BL   (0x41, Operation.MOV),
    MOV_CL_DL   (0x42, Operation.MOV),
    MOV_DL_AL   (0x43, Operation.MOV),
    MOV_DL_BL   (0x44, Operation.MOV),
    MOV_DL_CL   (0x45, Operation.MOV),
    
    XCHG_A_B    (0x46, Operation.XCHG),
    XCHG_A_C    (0x47, Operation.XCHG),
    XCHG_A_D    (0x48, Operation.XCHG),
    XCHG_B_C    (0x49, Operation.XCHG),
    XCHG_B_D    (0x4A, Operation.XCHG),
    XCHG_C_D    (0x4B, Operation.XCHG),
    
    XCHG_AL_BL  (0x4C, Operation.XCHG),
    XCHG_AL_CL  (0x4D, Operation.XCHG),
    XCHG_AL_DL  (0x4E, Operation.XCHG),
    XCHG_BL_CL  (0x4F, Operation.XCHG),
    XCHG_BL_DL  (0x50, Operation.XCHG),
    XCHG_CL_DL  (0x51, Operation.XCHG),
    
    PUSH_A      (0x52, Operation.PUSH),
    PUSH_B      (0x53, Operation.PUSH),
    PUSH_C      (0x54, Operation.PUSH),
    PUSH_D      (0x55, Operation.PUSH),
    PUSH_I      (0x56, Operation.PUSH),
    PUSH_J      (0x57, Operation.PUSH),
    PUSH_BP     (0x58, Operation.PUSH),
    PUSH_SP     (0x59, Operation.PUSH),
    PUSH_F      (0x5A, Operation.PUSH),
    PUSH_RIM    (0x5B, Operation.PUSH),
    
    POP_A       (0x5C, Operation.POP),
    POP_B       (0x5D, Operation.POP),
    POP_C       (0x5E, Operation.POP),
    POP_D       (0x5F, Operation.POP),
    POP_I       (0x60, Operation.POP),
    POP_J       (0x61, Operation.POP),
    POP_BP      (0x62, Operation.POP),
    POP_SP      (0x63, Operation.POP),
    POP_F       (0x64, Operation.POP),
    POP_RIM     (0x65, Operation.POP),
    
    PUSH_I32    (0x66, Operation.PUSH),
    
    AND_F_RIM   (0x67, Operation.AND),
    AND_RIM_F   (0x68, Operation.AND),
    OR_F_RIM    (0x69, Operation.OR),
    OR_RIM_F    (0x6A, Operation.OR),
    XOR_F_RIM   (0x6B, Operation.XOR),
    XOR_RIM_F   (0x6C, Operation.XOR),
    NOT_F       (0x6D, Operation.NOT),
    
    MOV_F_RIM   (0x6E, Operation.MOV),
    MOV_RIM_F   (0x6F, Operation.MOV),
    
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
    ADC_RIM     (0x91, Operation.ADC),
    PADD_RIMP   (0x92, Operation.PADD),
    PADC_RIMP   (0x93, Operation.PADC),
    
    SUB_RIM     (0x94, Operation.SUB),
    SBB_RIM     (0x95, Operation.SBB),
    PSUB_RIMP   (0x96, Operation.PSUB),
    PSBB_RIMP   (0x97, Operation.PSBB),
    
    ADD_RIM_I8  (0x98, Operation.ADD),
    ADC_RIM_I8  (0x99, Operation.ADC),
    SUB_RIM_I8  (0x9A, Operation.SUB),
    SBB_RIM_I8  (0x9B, Operation.SBB),
    
    INC_RIM     (0xA0, Operation.INC),
    ICC_RIM     (0xA1, Operation.ICC),
    PINC_RIMP   (0xA2, Operation.PINC),
    PICC_RIMP   (0xA3, Operation.PICC),
    
    DEC_RIM     (0xA4, Operation.DEC),
    DCC_RIM     (0xA5, Operation.DCC),
    PDEC_RIMP   (0xA6, Operation.PDCC),
    PDCC_RIMP   (0xA7, Operation.PDCC),
    
    INC_I       (0xA8, Operation.INC),
    INC_J       (0xA9, Operation.INC),
    ICC_I       (0xAA, Operation.ICC),
    ICC_J       (0xAB, Operation.ICC),
    DEC_I       (0xAC, Operation.DEC),
    DEC_J       (0xAD, Operation.DEC),
    DCC_I       (0xAE, Operation.DCC),
    DCC_J       (0xAF, Operation.DCC),
    
    MUL_RIM     (0xB0, Operation.MUL),
    MULH_RIM    (0xB2, Operation.MULH),
    MULSH_RIM   (0xB3, Operation.MULSH),
    
    PMUL_RIMP   (0xB4, Operation.PMUL),
    PMULH_RIMP  (0xB6, Operation.PMULH),
    PMULSH_RIMP (0xB7, Operation.PMULSH),
    
    DIV_RIM     (0xB8, Operation.DIV),
    DIVS_RIM    (0xB9, Operation.DIVS),
    DIVM_RIM    (0xBA, Operation.DIVM),
    DIVMS_RIM   (0xBB, Operation.DIVMS),
    
    PDIV_RIMP   (0xBC, Operation.PDIV),
    PDIVS_RIMP  (0xBD, Operation.PDIVS),
    PDIVM_RIMP  (0xBE, Operation.PDIVM),
    PDIVMS_RIMP (0xBF, Operation.PDIVMS),
    
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
    INT_I8      (0xD8, Operation.INT),
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
