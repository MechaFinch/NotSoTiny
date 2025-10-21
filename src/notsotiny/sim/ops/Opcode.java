package notsotiny.sim.ops;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum of every opcode
 * 
 * @author Mechafinch
 */
public enum Opcode {
    INVALID     (0x00, DecodingGroup.UNDEF, ExecutionGroup.UNDEF),
    MOVW_RIM_0  (0x01, DecodingGroup.RIM_WIDE_DO_WOD, ExecutionGroup.MOV_SHORTCUT),
    MOVS_RIM    (0x02, DecodingGroup.RIM_WIDEDST_WOD, ExecutionGroup.MOVS),
    MOVZ_RIM    (0x03, DecodingGroup.RIM_WIDEDST_WOD, ExecutionGroup.MOVZ),
    MOV_RIM     (0x04, DecodingGroup.RIM_WOD, ExecutionGroup.MOV),
    MOVW_RIM    (0x05, DecodingGroup.RIM_WIDE_WOD, ExecutionGroup.MOV),
    XCHG_RIM    (0x06, DecodingGroup.RIM_NORMAL, ExecutionGroup.XCHG),
    XCHGW_RIM   (0x07, DecodingGroup.RIM_WIDE, ExecutionGroup.XCHG),
    CMOVCC_RIM  (0x08, DecodingGroup.RIM_WOD_EI8, ExecutionGroup.CMOV),
    CMOVWCC_RIM (0x09, DecodingGroup.RIM_WIDE_WOD_EI8, ExecutionGroup.CMOV),
    PCMOVCC_RIMP(0x0A, DecodingGroup.RIM_PACKED_EI8, ExecutionGroup.CMOV),
    LEA_RIM     (0x0B, DecodingGroup.RIM_LEA, ExecutionGroup.MOV),
    MOV_PR_RIM  (0x0C, DecodingGroup.RIM_WIDE, ExecutionGroup.MOV_PROTECTED),
    MOV_RIM_PR  (0x0D, DecodingGroup.RIM_WIDE, ExecutionGroup.MOV_PROTECTED),
    MOV_F_RIM   (0x0E, DecodingGroup.RIM_SO, ExecutionGroup.MOV_SHORTCUT),
    MOV_RIM_F   (0x0F, DecodingGroup.RIM_DO_WOD, ExecutionGroup.MOV_SHORTCUT),
    
    MOV_A_I16   (0x10, DecodingGroup.I16, ExecutionGroup.MOV_SHORTCUT),
    MOV_B_I16   (0x11, DecodingGroup.I16, ExecutionGroup.MOV_SHORTCUT),
    MOV_C_I16   (0x12, DecodingGroup.I16, ExecutionGroup.MOV_SHORTCUT),
    MOV_D_I16   (0x13, DecodingGroup.I16, ExecutionGroup.MOV_SHORTCUT),
    MOV_I_I16   (0x14, DecodingGroup.I16, ExecutionGroup.MOV_SHORTCUT),
    MOV_J_I16   (0x15, DecodingGroup.I16, ExecutionGroup.MOV_SHORTCUT),
    MOV_K_I16   (0x16, DecodingGroup.I16, ExecutionGroup.MOV_SHORTCUT),
    MOV_L_I16   (0x17, DecodingGroup.I16, ExecutionGroup.MOV_SHORTCUT),
    
    MOVS_A_I8   (0x18, DecodingGroup.I8, ExecutionGroup.MOVS),
    MOVS_B_I8   (0x19, DecodingGroup.I8, ExecutionGroup.MOVS),
    MOVS_C_I8   (0x1A, DecodingGroup.I8, ExecutionGroup.MOVS),
    MOVS_D_I8   (0x1B, DecodingGroup.I8, ExecutionGroup.MOVS),
    MOVS_I_I8   (0x1C, DecodingGroup.I8, ExecutionGroup.MOVS),
    MOVS_J_I8   (0x1D, DecodingGroup.I8, ExecutionGroup.MOVS),
    MOVS_K_I8   (0x1E, DecodingGroup.I8, ExecutionGroup.MOVS),
    MOVS_L_I8   (0x1F, DecodingGroup.I8, ExecutionGroup.MOVS),
    
    MOV_RIM_BP  (0x20, DecodingGroup.RIM_RD_DO_WOD_EI8, ExecutionGroup.MOV_SHORTCUT),
    MOVW_RIM_BP (0x21, DecodingGroup.RIM_WIDE_RD_DO_WOD_EI8, ExecutionGroup.MOV_SHORTCUT),
    MOV_BP_RIM  (0x22, DecodingGroup.RIM_RS_SO_EI8, ExecutionGroup.MOV_SHORTCUT),
    MOVW_BP_RIM (0x23, DecodingGroup.RIM_WIDE_RS_SO_EI8, ExecutionGroup.MOV_SHORTCUT),
    
    LDI_RIM     (0x24, DecodingGroup.RIM_R32S_WOD, ExecutionGroup.MVI),
    LDIW_RIM    (0x25, DecodingGroup.RIM_WIDE_R32S_WOD, ExecutionGroup.MVI),
    DLD_RIM     (0x26, DecodingGroup.RIM_R32S_WOD, ExecutionGroup.DMV),
    DLDW_RIM    (0x27, DecodingGroup.RIM_WIDE_R32S_WOD, ExecutionGroup.DMV),
    STI_RIM     (0x28, DecodingGroup.RIM_R32D, ExecutionGroup.MVI),
    STIW_RIM    (0x29, DecodingGroup.RIM_WIDE_R32D, ExecutionGroup.MVI),
    DST_RIM     (0x2A, DecodingGroup.RIM_R32D, ExecutionGroup.DMV),
    DSTW_RIM    (0x2B, DecodingGroup.RIM_WIDE_R32D, ExecutionGroup.DMV),
    
    PUSH_F      (0x2C, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSH_PF     (0x2D, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    POP_F       (0x2E, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POP_PF      (0x2F, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    
    PUSH_A      (0x30, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSH_B      (0x31, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSH_C      (0x32, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSH_D      (0x33, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSH_I      (0x34, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSH_J      (0x35, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSH_K      (0x36, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSH_L      (0x37, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSHW_DA    (0x38, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSHW_BC    (0x39, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSHW_JI    (0x3A, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSHW_LK    (0x3B, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSHW_XP    (0x3C, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSHW_YP    (0x3D, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSHW_BP    (0x3E, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSHA       (0x3F, DecodingGroup.NODECODE, ExecutionGroup.PUSHA),
    
    POP_A       (0x40, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POP_B       (0x41, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POP_C       (0x42, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POP_D       (0x43, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POP_I       (0x44, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POP_J       (0x45, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POP_K       (0x46, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POP_L       (0x47, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POPW_DA     (0x48, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POPW_BC     (0x49, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POPW_JI     (0x4A, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POPW_LK     (0x4B, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POPW_XP     (0x4C, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POPW_YP     (0x4D, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POPW_BP     (0x4E, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POPA        (0x4F, DecodingGroup.NODECODE, ExecutionGroup.POPA),
    
    CMP_RIM_0   (0x50, DecodingGroup.RIM_DO, ExecutionGroup.CMP),
    CMPW_RIM_0  (0x51, DecodingGroup.RIM_WIDE_DO, ExecutionGroup.CMP),
    CMP_RIM_I8  (0x52, DecodingGroup.RIM_DO_EI8, ExecutionGroup.CMP),
    CMPW_RIM_I8 (0x53, DecodingGroup.RIM_WIDE_DO_EI8, ExecutionGroup.CMP),
    CMP_RIM     (0x54, DecodingGroup.RIM_NORMAL, ExecutionGroup.CMP),
    CMPW_RIM    (0x55, DecodingGroup.RIM_WIDE, ExecutionGroup.CMP),
    PCMP_RIMP   (0x56, DecodingGroup.RIM_PACKED, ExecutionGroup.PCMP),
    NOT_F       (0x57, DecodingGroup.NODECODE, ExecutionGroup.F_OPS),
    AND_F_RIM   (0x58, DecodingGroup.RIM_SO, ExecutionGroup.F_OPS),
    AND_RIM_F   (0x59, DecodingGroup.RIM_DO, ExecutionGroup.F_OPS),
    OR_F_RIM    (0x5A, DecodingGroup.RIM_SO, ExecutionGroup.F_OPS),
    OR_RIM_F    (0x5B, DecodingGroup.RIM_DO, ExecutionGroup.F_OPS),
    XOR_F_RIM   (0x5C, DecodingGroup.RIM_SO, ExecutionGroup.F_OPS),
    XOR_RIM_F   (0x5D, DecodingGroup.RIM_DO, ExecutionGroup.F_OPS),
    TST_RIM     (0x5E, DecodingGroup.RIM_NORMAL, ExecutionGroup.TST),
    PTST_RIMP   (0x5F, DecodingGroup.RIM_PACKED, ExecutionGroup.TST),
    
    ADD_A_I8    (0x60, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADD_B_I8    (0x61, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADD_C_I8    (0x62, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADD_D_I8    (0x63, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADD_I_I8    (0x64, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADD_J_I8    (0x65, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADD_K_I8    (0x66, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADD_L_I8    (0x67, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADDW_DA_I8  (0x68, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADDW_BC_I8  (0x69, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADDW_JI_I8  (0x6A, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADDW_LK_I8  (0x6B, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADDW_XP_I8  (0x6C, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADDW_YP_I8  (0x6D, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADDW_BP_I8  (0x6E, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADDW_SP_I8  (0x6F, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    
    SUB_A_I8    (0x70, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUB_B_I8    (0x71, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUB_C_I8    (0x72, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUB_D_I8    (0x73, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUB_I_I8    (0x74, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUB_J_I8    (0x75, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUB_K_I8    (0x76, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUB_L_I8    (0x77, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUBW_DA_I8  (0x78, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUBW_BC_I8  (0x79, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUBW_JI_I8  (0x7A, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUBW_LK_I8  (0x7B, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUBW_XP_I8  (0x7C, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUBW_YP_I8  (0x7D, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUBW_BP_I8  (0x7E, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUBW_SP_I8  (0x7F, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    
    ADD_RIM_I8  (0x80, DecodingGroup.RIM_DO_EI8, ExecutionGroup.ADD),
    ADC_RIM_I8  (0x81, DecodingGroup.RIM_DO_EI8, ExecutionGroup.ADC),
    ADDW_RIM_I8 (0x82, DecodingGroup.RIM_WIDE_DO_EI8, ExecutionGroup.ADD),
    ADCW_RIM_I8 (0x83, DecodingGroup.RIM_WIDE_DO_EI8, ExecutionGroup.ADC),
    ADD_RIM     (0x84, DecodingGroup.RIM_NORMAL, ExecutionGroup.ADD),
    ADC_RIM     (0x85, DecodingGroup.RIM_NORMAL, ExecutionGroup.ADC),
    ADDW_RIM    (0x86, DecodingGroup.RIM_WIDE, ExecutionGroup.ADD),
    ADCW_RIM    (0x87, DecodingGroup.RIM_WIDE, ExecutionGroup.ADC),
    
    SUB_RIM_I8  (0x88, DecodingGroup.RIM_DO_EI8, ExecutionGroup.SUB),
    SBB_RIM_I8  (0x89, DecodingGroup.RIM_DO_EI8, ExecutionGroup.SBB),
    SUBW_RIM_I8 (0x8A, DecodingGroup.RIM_WIDE_DO_EI8, ExecutionGroup.SUB),
    SBBW_RIM_I8 (0x8B, DecodingGroup.RIM_WIDE_DO_EI8, ExecutionGroup.SBB),
    SUB_RIM     (0x8C, DecodingGroup.RIM_NORMAL, ExecutionGroup.SUB),
    SBB_RIM     (0x8D, DecodingGroup.RIM_NORMAL, ExecutionGroup.SBB),
    SUBW_RIM    (0x8E, DecodingGroup.RIM_WIDE, ExecutionGroup.SUB),
    SBBW_RIM    (0x8F, DecodingGroup.RIM_WIDE, ExecutionGroup.SBB),
    
    INC_RIM     (0x90, DecodingGroup.RIM_DO, ExecutionGroup.INC),
    ICC_RIM     (0x91, DecodingGroup.RIM_DO, ExecutionGroup.INC),
    INCW_RIM    (0x92, DecodingGroup.RIM_WIDE_DO, ExecutionGroup.INC),
    ICCW_RIM    (0x93, DecodingGroup.RIM_WIDE_DO, ExecutionGroup.INC),
    DEC_RIM     (0x94, DecodingGroup.RIM_DO, ExecutionGroup.INC),
    DCC_RIM     (0x95, DecodingGroup.RIM_DO, ExecutionGroup.INC),
    DECW_RIM    (0x96, DecodingGroup.RIM_WIDE_DO, ExecutionGroup.INC),
    DCCW_RIM    (0x97, DecodingGroup.RIM_WIDE_DO, ExecutionGroup.INC),
    
    PINC_RIMP   (0x98, DecodingGroup.RIM_PACKED_DO, ExecutionGroup.PINC),
    PICC_RIMP   (0x99, DecodingGroup.RIM_PACKED_DO, ExecutionGroup.PINC),
    PDEC_RIMP   (0x9A, DecodingGroup.RIM_PACKED_DO, ExecutionGroup.PINC),
    PDCC_RIMP   (0x9B, DecodingGroup.RIM_PACKED_DO, ExecutionGroup.PINC),
    PADD_RIMP   (0x9C, DecodingGroup.RIM_PACKED, ExecutionGroup.PADD),
    PADC_RIMP   (0x9D, DecodingGroup.RIM_PACKED, ExecutionGroup.PADD),
    PSUB_RIMP   (0x9E, DecodingGroup.RIM_PACKED, ExecutionGroup.PSUB),
    PSBB_RIMP   (0x9F, DecodingGroup.RIM_PACKED, ExecutionGroup.PSUB),
    
    AND_RIM     (0xA0, DecodingGroup.RIM_NORMAL, ExecutionGroup.LOGIC),
    OR_RIM      (0xA1, DecodingGroup.RIM_NORMAL, ExecutionGroup.LOGIC),
    XOR_RIM     (0xA2, DecodingGroup.RIM_NORMAL, ExecutionGroup.LOGIC),
    NOT_RIM     (0xA3, DecodingGroup.RIM_DO, ExecutionGroup.LOGIC),
    PAND_RIMP   (0xA4, DecodingGroup.RIM_PACKED, ExecutionGroup.LOGIC),
    POR_RIMP    (0xA5, DecodingGroup.RIM_PACKED, ExecutionGroup.LOGIC),
    PXOR_RIMP   (0xA6, DecodingGroup.RIM_PACKED, ExecutionGroup.LOGIC),
    PNOT_RIMP   (0xA7, DecodingGroup.RIM_PACKED_DO, ExecutionGroup.LOGIC),
    
    SHL_RIM_1   (0xA8, DecodingGroup.RIM_DO, ExecutionGroup.SHIFT),
    SHR_RIM_1   (0xA9, DecodingGroup.RIM_DO, ExecutionGroup.SHIFT),
    SAR_RIM_1   (0xAA, DecodingGroup.RIM_DO, ExecutionGroup.SHIFT),
    ROL_RIM_1   (0xAB, DecodingGroup.RIM_DO, ExecutionGroup.SHIFT),
    ROR_RIM_1   (0xAC, DecodingGroup.RIM_DO, ExecutionGroup.SHIFT),
    RCL_RIM_1   (0xAD, DecodingGroup.RIM_DO, ExecutionGroup.SHIFT),
    RCR_RIM_1   (0xAE, DecodingGroup.RIM_DO, ExecutionGroup.SHIFT),
    NOP         (0xAF, DecodingGroup.NODECODE, ExecutionGroup.NOP),
    
    SHL_RIM_I8  (0xB0, DecodingGroup.RIM_DO_EI8, ExecutionGroup.SHIFT),
    SHR_RIM_I8  (0xB1, DecodingGroup.RIM_DO_EI8, ExecutionGroup.SHIFT),
    SAR_RIM_I8  (0xB2, DecodingGroup.RIM_DO_EI8, ExecutionGroup.SHIFT),
    ROL_RIM_I8  (0xB3, DecodingGroup.RIM_DO_EI8, ExecutionGroup.SHIFT),
    ROR_RIM_I8  (0xB4, DecodingGroup.RIM_DO_EI8, ExecutionGroup.SHIFT),
    RCL_RIM_I8  (0xB5, DecodingGroup.RIM_DO_EI8, ExecutionGroup.SHIFT),
    RCR_RIM_I8  (0xB6, DecodingGroup.RIM_DO_EI8, ExecutionGroup.SHIFT),
    NEG_RIM     (0xB7, DecodingGroup.RIM_DO, ExecutionGroup.NEG),
    
    SHL_RIM     (0xB8, DecodingGroup.RIM_NORMAL, ExecutionGroup.SHIFT),
    SHR_RIM     (0xB9, DecodingGroup.RIM_NORMAL, ExecutionGroup.SHIFT),
    SAR_RIM     (0xBA, DecodingGroup.RIM_NORMAL, ExecutionGroup.SHIFT),
    ROL_RIM     (0xBB, DecodingGroup.RIM_NORMAL, ExecutionGroup.SHIFT),
    ROR_RIM     (0xBC, DecodingGroup.RIM_NORMAL, ExecutionGroup.SHIFT),
    RCL_RIM     (0xBD, DecodingGroup.RIM_NORMAL, ExecutionGroup.SHIFT),
    RCR_RIM     (0xBE, DecodingGroup.RIM_NORMAL, ExecutionGroup.SHIFT),
    PNEG_RIMP   (0xBF, DecodingGroup.RIM_PACKED_DO, ExecutionGroup.NEG),
    
    MUL_RIM     (0xC0, DecodingGroup.RIM_NORMAL, ExecutionGroup.MUL),
    AADJ_RIMP   (0xC1, DecodingGroup.RIM_PACKED_DO, ExecutionGroup.ADJ),
    MULH_RIM    (0xC2, DecodingGroup.RIM_WIDEDST, ExecutionGroup.MUL),
    MULSH_RIM   (0xC3, DecodingGroup.RIM_WIDEDST, ExecutionGroup.MUL),
    PMUL_RIMP   (0xC4, DecodingGroup.RIM_PACKED, ExecutionGroup.MUL),
    SADJ_RIMP   (0xC5, DecodingGroup.RIM_PACKED_DO, ExecutionGroup.ADJ),
    PMULH_RIMP  (0xC6, DecodingGroup.RIM_PACKED_WIDEDST, ExecutionGroup.MUL),
    PMULSH_RIMP (0xC7, DecodingGroup.RIM_PACKED_WIDEDST, ExecutionGroup.MUL),
    
    DIV_RIM     (0xC8, DecodingGroup.RIM_NORMAL, ExecutionGroup.DIV),
    DIVS_RIM    (0xC9, DecodingGroup.RIM_NORMAL, ExecutionGroup.DIV),
    DIVM_RIM    (0xCA, DecodingGroup.RIM_WIDEDST, ExecutionGroup.DIV),
    DIVMS_RIM   (0xCB, DecodingGroup.RIM_WIDEDST, ExecutionGroup.DIV),
    PDIV_RIMP   (0xCC, DecodingGroup.RIM_PACKED, ExecutionGroup.DIV),
    PDIVS_RIMP  (0xCD, DecodingGroup.RIM_PACKED, ExecutionGroup.DIV),
    PDIVM_RIMP  (0xCE, DecodingGroup.RIM_PACKED_WIDEDST, ExecutionGroup.DIV),
    PDIVMS_RIMP (0xCF, DecodingGroup.RIM_PACKED_WIDEDST, ExecutionGroup.DIV),
    
    CALL_I8     (0xD0, DecodingGroup.I8, ExecutionGroup.CALL),
    CALL_I16    (0xD1, DecodingGroup.I16, ExecutionGroup.CALL),
    CALL_I32    (0xD2, DecodingGroup.I32, ExecutionGroup.CALL),
    CALL_RIM    (0xD3, DecodingGroup.RIM_SO, ExecutionGroup.CALL),
    CALLA_I32   (0xD4, DecodingGroup.I32, ExecutionGroup.CALLA),
    CALLA_RIM32 (0xD5, DecodingGroup.RIM_WIDE_SO, ExecutionGroup.CALLA),
    RET         (0xD6, DecodingGroup.NODECODE, ExecutionGroup.RET),
    IRET        (0xD7, DecodingGroup.NODECODE, ExecutionGroup.IRET),
    
    JMP_I16     (0xD8, DecodingGroup.I16, ExecutionGroup.JMP),
    JMP_I32     (0xD9, DecodingGroup.I32, ExecutionGroup.JMP),
    JMPA_I32    (0xDA, DecodingGroup.I32, ExecutionGroup.JMPA),
    JMPA_RIM32  (0xDB, DecodingGroup.RIM_WIDE_SO, ExecutionGroup.JMPA),
    
    INT_I8      (0xDC, DecodingGroup.I8, ExecutionGroup.INT),
    INT_RIM     (0xDD, DecodingGroup.RIM_SO, ExecutionGroup.INT),
    UNDEF_DE    (0xDE, DecodingGroup.UNDEF, ExecutionGroup.UNDEF),
    HLT         (0xDF, DecodingGroup.NODECODE, ExecutionGroup.HLT),
    
    JMP_I8      (0xE0, DecodingGroup.I8, ExecutionGroup.JMP),
    JCC_I8      (0xE1, DecodingGroup.I8_EI8, ExecutionGroup.JCC),
    JC_I8       (0xE2, DecodingGroup.I8, ExecutionGroup.JCC),
    JNC_I8      (0xE3, DecodingGroup.I8, ExecutionGroup.JCC),
    JS_I8       (0xE4, DecodingGroup.I8, ExecutionGroup.JCC),
    JNS_I8      (0xE5, DecodingGroup.I8, ExecutionGroup.JCC),
    JO_I8       (0xE6, DecodingGroup.I8, ExecutionGroup.JCC),
    JNO_I8      (0xE7, DecodingGroup.I8, ExecutionGroup.JCC),
    JZ_I8       (0xE8, DecodingGroup.I8, ExecutionGroup.JCC),
    JNZ_I8      (0xE9, DecodingGroup.I8, ExecutionGroup.JCC),
    JA_I8       (0xEA, DecodingGroup.I8, ExecutionGroup.JCC),
    JBE_I8      (0xEB, DecodingGroup.I8, ExecutionGroup.JCC),
    JG_I8       (0xEC, DecodingGroup.I8, ExecutionGroup.JCC),
    JGE_I8      (0xED, DecodingGroup.I8, ExecutionGroup.JCC),
    JL_I8       (0xEE, DecodingGroup.I8, ExecutionGroup.JCC),
    JLE_I8      (0xEF, DecodingGroup.I8, ExecutionGroup.JCC),
    
    JMP_RIM     (0xF0, DecodingGroup.RIM_SO, ExecutionGroup.JMP),
    JCC_RIM     (0xF1, DecodingGroup.RIM_SO_EI8, ExecutionGroup.JCC),
    JC_RIM      (0xF2, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    JNC_RIM     (0xF3, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    JS_RIM      (0xF4, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    JNS_RIM     (0xF5, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    JO_RIM      (0xF6, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    JNO_RIM     (0xF7, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    JZ_RIM      (0xF8, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    JNZ_RIM     (0xF9, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    JA_RIM      (0xFA, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    JBE_RIM     (0xFB, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    JG_RIM      (0xFC, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    JGE_RIM     (0xFD, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    JL_RIM      (0xFE, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    JLE_RIM     (0xFF, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    ;
    
    
    // opcode
    public final byte op;
    
    // meta
    public final DecodingGroup dgroup;
    public final ExecutionGroup egroup;
    
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
    private Opcode(int op, DecodingGroup dgroup, ExecutionGroup egroup) {
        this.op = (byte) op;
        this.dgroup = dgroup;
        this.egroup = egroup;
    }
    
    public static Opcode fromOp(byte op) {
        return opMap.get(op);
    }
    
    public byte getOp() { return this.op; }
}
