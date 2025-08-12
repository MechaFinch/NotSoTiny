package notsotiny.sim.ops;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum of every opcode
 * 
 * @author Mechafinch
 */
public enum Opcode {
    INVALID     (0x00, Operation.INV, DecodingGroup.UNDEF, ExecutionGroup.UNDEF),
    MOVW_RIM_0  (0x01, Operation.MOV, DecodingGroup.RIM_WIDE_DO_WOD, ExecutionGroup.MOV_SHORTCUT),
    MOVS_RIM    (0x02, Operation.MOVS, DecodingGroup.RIM_WIDEDST_WOD, ExecutionGroup.MOVS),
    MOVZ_RIM    (0x03, Operation.MOVZ, DecodingGroup.RIM_WIDEDST_WOD, ExecutionGroup.MOVZ),
    MOV_RIM     (0x04, Operation.MOV, DecodingGroup.RIM_WOD, ExecutionGroup.MOV),
    MOVW_RIM    (0x05, Operation.MOV, DecodingGroup.RIM_WIDE_WOD, ExecutionGroup.MOV),
    XCHG_RIM    (0x06, Operation.XCHG, DecodingGroup.RIM_NORMAL, ExecutionGroup.XCHG),
    XCHGW_RIM   (0x07, Operation.XCHG, DecodingGroup.RIM_WIDE, ExecutionGroup.XCHG),
    CMOVCC_RIM  (0x08, Operation.CMOVCC, DecodingGroup.RIM_WOD_EI8, ExecutionGroup.CMOV),
    CMOVWCC_RIM (0x09, Operation.CMOVCC, DecodingGroup.RIM_WIDE_WOD_EI8, ExecutionGroup.CMOV),
    PCMOVCC_RIMP(0x0A, Operation.PCMOVCC, DecodingGroup.RIM_PACKED_EI8, ExecutionGroup.CMOV),
    LEA_RIM     (0x0B, Operation.LEA, DecodingGroup.RIM_LEA, ExecutionGroup.MOV),
    MOV_PR_RIM  (0x0C, Operation.MOV, DecodingGroup.RIM_WIDE, ExecutionGroup.MOV_PROTECTED),
    MOV_RIM_PR  (0x0D, Operation.MOV, DecodingGroup.RIM_WIDE, ExecutionGroup.MOV_PROTECTED),
    MOV_F_RIM   (0x0E, Operation.MOV, DecodingGroup.RIM_SO, ExecutionGroup.MOV_SHORTCUT),
    MOV_RIM_F   (0x0F, Operation.MOV, DecodingGroup.RIM_DO_WOD, ExecutionGroup.MOV_SHORTCUT),
    
    MOV_A_I16   (0x10, Operation.MOV, DecodingGroup.I16, ExecutionGroup.MOV_SHORTCUT),
    MOV_B_I16   (0x11, Operation.MOV, DecodingGroup.I16, ExecutionGroup.MOV_SHORTCUT),
    MOV_C_I16   (0x12, Operation.MOV, DecodingGroup.I16, ExecutionGroup.MOV_SHORTCUT),
    MOV_D_I16   (0x13, Operation.MOV, DecodingGroup.I16, ExecutionGroup.MOV_SHORTCUT),
    MOV_I_I16   (0x14, Operation.MOV, DecodingGroup.I16, ExecutionGroup.MOV_SHORTCUT),
    MOV_J_I16   (0x15, Operation.MOV, DecodingGroup.I16, ExecutionGroup.MOV_SHORTCUT),
    MOV_K_I16   (0x16, Operation.MOV, DecodingGroup.I16, ExecutionGroup.MOV_SHORTCUT),
    MOV_L_I16   (0x17, Operation.MOV, DecodingGroup.I16, ExecutionGroup.MOV_SHORTCUT),
    
    MOVS_A_I8   (0x18, Operation.MOV, DecodingGroup.I8, ExecutionGroup.MOVS),
    MOVS_B_I8   (0x19, Operation.MOV, DecodingGroup.I8, ExecutionGroup.MOVS),
    MOVS_C_I8   (0x1A, Operation.MOV, DecodingGroup.I8, ExecutionGroup.MOVS),
    MOVS_D_I8   (0x1B, Operation.MOV, DecodingGroup.I8, ExecutionGroup.MOVS),
    MOVS_I_I8   (0x1C, Operation.MOV, DecodingGroup.I8, ExecutionGroup.MOVS),
    MOVS_J_I8   (0x1D, Operation.MOV, DecodingGroup.I8, ExecutionGroup.MOVS),
    MOVS_K_I8   (0x1E, Operation.MOV, DecodingGroup.I8, ExecutionGroup.MOVS),
    MOVS_L_I8   (0x1F, Operation.MOV, DecodingGroup.I8, ExecutionGroup.MOVS),
    
    MOV_RIM_BP  (0x20, Operation.MOV, DecodingGroup.RIM_RD_DO_WOD_EI8, ExecutionGroup.MOV_SHORTCUT),
    MOVW_RIM_BP (0x21, Operation.MOV, DecodingGroup.RIM_WIDE_RD_DO_WOD_EI8, ExecutionGroup.MOV_SHORTCUT),
    MOV_BP_RIM  (0x22, Operation.MOV, DecodingGroup.RIM_RS_SO_EI8, ExecutionGroup.MOV_SHORTCUT),
    MOVW_BP_RIM (0x23, Operation.MOV, DecodingGroup.RIM_WIDE_RS_SO_EI8, ExecutionGroup.MOV_SHORTCUT),
    
    PUSH_F      (0x24, Operation.PUSH, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSH_PF     (0x25, Operation.PUSH, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    POP_F       (0x26, Operation.POP, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POP_PF      (0x27, Operation.POP, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    PUSH_RIM    (0x28, Operation.PUSH, DecodingGroup.RIM_SO, ExecutionGroup.PUSH),
    RPUSH_RIM   (0x29, Operation.RPUSH, DecodingGroup.RIM_R32D, ExecutionGroup.RPUSH),
    PUSHW_RIM   (0x2A, Operation.PUSH, DecodingGroup.RIM_WIDE_SO, ExecutionGroup.PUSH),
    RPUSHW_RIM  (0x2B, Operation.RPUSH, DecodingGroup.RIM_WIDE_R32D, ExecutionGroup.RPUSH),
    POP_RIM     (0x2C, Operation.POP, DecodingGroup.RIM_DO_WOD, ExecutionGroup.POP),
    RPOP_RIM    (0x2D, Operation.RPOP, DecodingGroup.RIM_R32S_WOD, ExecutionGroup.RPOP),
    POPW_RIM    (0x2E, Operation.POP, DecodingGroup.RIM_WIDE_DO_WOD, ExecutionGroup.POP),
    RPOPW_RIM   (0x2F, Operation.RPOP, DecodingGroup.RIM_WIDE_R32S_WOD, ExecutionGroup.RPOP),
    
    PUSH_A      (0x30, Operation.PUSH, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSH_B      (0x31, Operation.PUSH, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSH_C      (0x32, Operation.PUSH, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSH_D      (0x33, Operation.PUSH, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSH_I      (0x34, Operation.PUSH, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSH_J      (0x35, Operation.PUSH, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSH_K      (0x36, Operation.PUSH, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSH_L      (0x37, Operation.PUSH, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSHW_DA    (0x38, Operation.PUSH, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSHW_BC    (0x39, Operation.PUSH, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSHW_JI    (0x3A, Operation.PUSH, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSHW_LK    (0x3B, Operation.PUSH, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSHW_XP    (0x3C, Operation.PUSH, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSHW_YP    (0x3D, Operation.PUSH, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSHW_BP    (0x3E, Operation.PUSH, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSHA       (0x3F, Operation.PUSHA, DecodingGroup.NODECODE, ExecutionGroup.PUSHA),
    
    POP_A       (0x40, Operation.POP, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POP_B       (0x41, Operation.POP, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POP_C       (0x42, Operation.POP, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POP_D       (0x43, Operation.POP, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POP_I       (0x44, Operation.POP, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POP_J       (0x45, Operation.POP, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POP_K       (0x46, Operation.POP, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POP_L       (0x47, Operation.POP, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POPW_DA     (0x48, Operation.POP, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POPW_BC     (0x49, Operation.POP, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POPW_JI     (0x4A, Operation.POP, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POPW_LK     (0x4B, Operation.POP, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POPW_XP     (0x4C, Operation.POP, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POPW_YP     (0x4D, Operation.POP, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POPW_BP     (0x4E, Operation.POP, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POPA        (0x4F, Operation.POPA, DecodingGroup.NODECODE, ExecutionGroup.POPA),
    
    CMP_RIM_0   (0x50, Operation.CMP, DecodingGroup.RIM_DO, ExecutionGroup.CMP),
    CMPW_RIM_0  (0x51, Operation.CMP, DecodingGroup.RIM_WIDE_DO, ExecutionGroup.CMP),
    CMP_RIM_I8  (0x52, Operation.CMP, DecodingGroup.RIM_DO_EI8, ExecutionGroup.CMP),
    CMPW_RIM_I8 (0x53, Operation.CMP, DecodingGroup.RIM_WIDE_DO_EI8, ExecutionGroup.CMP),
    CMP_RIM     (0x54, Operation.CMP, DecodingGroup.RIM_NORMAL, ExecutionGroup.CMP),
    CMPW_RIM    (0x55, Operation.CMP, DecodingGroup.RIM_WIDE, ExecutionGroup.CMP),
    PCMP_RIMP   (0x56, Operation.PCMP, DecodingGroup.RIM_PACKED, ExecutionGroup.PCMP),
    NOT_F       (0x57, Operation.NOT, DecodingGroup.NODECODE, ExecutionGroup.F_OPS),
    AND_F_RIM   (0x58, Operation.AND, DecodingGroup.RIM_SO, ExecutionGroup.F_OPS),
    AND_RIM_F   (0x59, Operation.AND, DecodingGroup.RIM_DO, ExecutionGroup.F_OPS),
    OR_F_RIM    (0x5A, Operation.OR, DecodingGroup.RIM_SO, ExecutionGroup.F_OPS),
    OR_RIM_F    (0x5B, Operation.OR, DecodingGroup.RIM_DO, ExecutionGroup.F_OPS),
    XOR_F_RIM   (0x5C, Operation.XOR, DecodingGroup.RIM_SO, ExecutionGroup.F_OPS),
    XOR_RIM_F   (0x5D, Operation.XOR, DecodingGroup.RIM_DO, ExecutionGroup.F_OPS),
    TST_RIM     (0x5E, Operation.TST, DecodingGroup.RIM_NORMAL, ExecutionGroup.TST),
    PTST_RIMP   (0x5F, Operation.PTST, DecodingGroup.RIM_PACKED, ExecutionGroup.TST),
    
    ADD_A_I8    (0x60, Operation.ADD, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADD_B_I8    (0x61, Operation.ADD, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADD_C_I8    (0x62, Operation.ADD, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADD_D_I8    (0x63, Operation.ADD, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADD_I_I8    (0x64, Operation.ADD, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADD_J_I8    (0x65, Operation.ADD, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADD_K_I8    (0x66, Operation.ADD, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADD_L_I8    (0x67, Operation.ADD, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADDW_DA_I8  (0x68, Operation.ADD, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADDW_BC_I8  (0x69, Operation.ADD, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADDW_JI_I8  (0x6A, Operation.ADD, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADDW_LK_I8  (0x6B, Operation.ADD, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADDW_XP_I8  (0x6C, Operation.ADD, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADDW_YP_I8  (0x6D, Operation.ADD, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADDW_BP_I8  (0x6E, Operation.ADD, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADDW_SP_I8  (0x6F, Operation.ADD, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    
    SUB_A_I8    (0x70, Operation.SUB, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUB_B_I8    (0x71, Operation.SUB, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUB_C_I8    (0x72, Operation.SUB, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUB_D_I8    (0x73, Operation.SUB, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUB_I_I8    (0x74, Operation.SUB, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUB_J_I8    (0x75, Operation.SUB, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUB_K_I8    (0x76, Operation.SUB, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUB_L_I8    (0x77, Operation.SUB, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUBW_DA_I8  (0x78, Operation.SUB, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUBW_BC_I8  (0x79, Operation.SUB, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUBW_JI_I8  (0x7A, Operation.SUB, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUBW_LK_I8  (0x7B, Operation.SUB, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUBW_XP_I8  (0x7C, Operation.SUB, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUBW_YP_I8  (0x7D, Operation.SUB, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUBW_BP_I8  (0x7E, Operation.SUB, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUBW_SP_I8  (0x7F, Operation.SUB, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    
    ADD_RIM_I8  (0x80, Operation.ADD, DecodingGroup.RIM_DO_EI8, ExecutionGroup.ADD),
    ADC_RIM_I8  (0x81, Operation.ADC, DecodingGroup.RIM_DO_EI8, ExecutionGroup.ADC),
    ADDW_RIM_I8 (0x82, Operation.ADD, DecodingGroup.RIM_WIDE_DO_EI8, ExecutionGroup.ADD),
    ADCW_RIM_I8 (0x83, Operation.ADC, DecodingGroup.RIM_WIDE_DO_EI8, ExecutionGroup.ADC),
    ADD_RIM     (0x84, Operation.ADD, DecodingGroup.RIM_NORMAL, ExecutionGroup.ADD),
    ADC_RIM     (0x85, Operation.ADC, DecodingGroup.RIM_NORMAL, ExecutionGroup.ADC),
    ADDW_RIM    (0x86, Operation.ADD, DecodingGroup.RIM_WIDE, ExecutionGroup.ADD),
    ADCW_RIM    (0x87, Operation.ADC, DecodingGroup.RIM_WIDE, ExecutionGroup.ADC),
    
    SUB_RIM_I8  (0x88, Operation.SUB, DecodingGroup.RIM_DO_EI8, ExecutionGroup.SUB),
    SBB_RIM_I8  (0x89, Operation.SBB, DecodingGroup.RIM_DO_EI8, ExecutionGroup.SBB),
    SUBW_RIM_I8 (0x8A, Operation.SUB, DecodingGroup.RIM_WIDE_DO_EI8, ExecutionGroup.SUB),
    SBBW_RIM_I8 (0x8B, Operation.SBB, DecodingGroup.RIM_WIDE_DO_EI8, ExecutionGroup.SBB),
    SUB_RIM     (0x8C, Operation.SUB, DecodingGroup.RIM_NORMAL, ExecutionGroup.SUB),
    SBB_RIM     (0x8D, Operation.SBB, DecodingGroup.RIM_NORMAL, ExecutionGroup.SBB),
    SUBW_RIM    (0x8E, Operation.SUB, DecodingGroup.RIM_WIDE, ExecutionGroup.SUB),
    SBBW_RIM    (0x8F, Operation.SBB, DecodingGroup.RIM_WIDE, ExecutionGroup.SBB),
    
    INC_RIM     (0x90, Operation.INC, DecodingGroup.RIM_DO, ExecutionGroup.INC),
    ICC_RIM     (0x91, Operation.ICC, DecodingGroup.RIM_DO, ExecutionGroup.INC),
    INCW_RIM    (0x92, Operation.INC, DecodingGroup.RIM_WIDE_DO, ExecutionGroup.INC),
    ICCW_RIM    (0x93, Operation.ICC, DecodingGroup.RIM_WIDE_DO, ExecutionGroup.INC),
    DEC_RIM     (0x94, Operation.DEC, DecodingGroup.RIM_DO, ExecutionGroup.INC),
    DCC_RIM     (0x95, Operation.DCC, DecodingGroup.RIM_DO, ExecutionGroup.INC),
    DECW_RIM    (0x96, Operation.DEC, DecodingGroup.RIM_WIDE_DO, ExecutionGroup.INC),
    DCCW_RIM    (0x97, Operation.DCC, DecodingGroup.RIM_WIDE_DO, ExecutionGroup.INC),
    
    PINC_RIMP   (0x98, Operation.PINC, DecodingGroup.RIM_PACKED_DO, ExecutionGroup.PINC),
    PICC_RIMP   (0x99, Operation.PICC, DecodingGroup.RIM_PACKED_DO, ExecutionGroup.PINC),
    PDEC_RIMP   (0x9A, Operation.PDCC, DecodingGroup.RIM_PACKED_DO, ExecutionGroup.PINC),
    PDCC_RIMP   (0x9B, Operation.PDCC, DecodingGroup.RIM_PACKED_DO, ExecutionGroup.PINC),
    PADD_RIMP   (0x9C, Operation.PADD, DecodingGroup.RIM_PACKED, ExecutionGroup.PADD),
    PADC_RIMP   (0x9D, Operation.PADC, DecodingGroup.RIM_PACKED, ExecutionGroup.PADD),
    PSUB_RIMP   (0x9E, Operation.PSUB, DecodingGroup.RIM_PACKED, ExecutionGroup.PSUB),
    PSBB_RIMP   (0x9F, Operation.PSBB, DecodingGroup.RIM_PACKED, ExecutionGroup.PSUB),
    
    AND_RIM     (0xA0, Operation.AND, DecodingGroup.RIM_NORMAL, ExecutionGroup.LOGIC),
    OR_RIM      (0xA1, Operation.OR, DecodingGroup.RIM_NORMAL, ExecutionGroup.LOGIC),
    XOR_RIM     (0xA2, Operation.XOR, DecodingGroup.RIM_NORMAL, ExecutionGroup.LOGIC),
    NOT_RIM     (0xA3, Operation.NOT, DecodingGroup.RIM_DO, ExecutionGroup.LOGIC),
    PAND_RIMP   (0xA4, Operation.PAND, DecodingGroup.RIM_PACKED, ExecutionGroup.LOGIC),
    POR_RIMP    (0xA5, Operation.POR, DecodingGroup.RIM_PACKED, ExecutionGroup.LOGIC),
    PXOR_RIMP   (0xA6, Operation.PXOR, DecodingGroup.RIM_PACKED, ExecutionGroup.LOGIC),
    PNOT_RIMP   (0xA7, Operation.PNOT, DecodingGroup.RIM_PACKED_DO, ExecutionGroup.LOGIC),
    
    SHL_RIM_1   (0xA8, Operation.SHL, DecodingGroup.RIM_DO, ExecutionGroup.SHIFT),
    SHR_RIM_1   (0xA9, Operation.SHR, DecodingGroup.RIM_DO, ExecutionGroup.SHIFT),
    SAR_RIM_1   (0xAA, Operation.SAR, DecodingGroup.RIM_DO, ExecutionGroup.SHIFT),
    ROL_RIM_1   (0xAB, Operation.ROL, DecodingGroup.RIM_DO, ExecutionGroup.SHIFT),
    ROR_RIM_1   (0xAC, Operation.ROR, DecodingGroup.RIM_DO, ExecutionGroup.SHIFT),
    RCL_RIM_1   (0xAD, Operation.RCL, DecodingGroup.RIM_DO, ExecutionGroup.SHIFT),
    RCR_RIM_1   (0xAE, Operation.RCR, DecodingGroup.RIM_DO, ExecutionGroup.SHIFT),
    NOP         (0xAF, Operation.NOP, DecodingGroup.NODECODE, ExecutionGroup.NOP),
    
    SHL_RIM_I8  (0xB0, Operation.SHL, DecodingGroup.RIM_DO_EI8, ExecutionGroup.SHIFT),
    SHR_RIM_I8  (0xB1, Operation.SHR, DecodingGroup.RIM_DO_EI8, ExecutionGroup.SHIFT),
    SAR_RIM_I8  (0xB2, Operation.SAR, DecodingGroup.RIM_DO_EI8, ExecutionGroup.SHIFT),
    ROL_RIM_I8  (0xB3, Operation.ROL, DecodingGroup.RIM_DO_EI8, ExecutionGroup.SHIFT),
    ROR_RIM_I8  (0xB4, Operation.ROR, DecodingGroup.RIM_DO_EI8, ExecutionGroup.SHIFT),
    RCL_RIM_I8  (0xB5, Operation.RCL, DecodingGroup.RIM_DO_EI8, ExecutionGroup.SHIFT),
    RCR_RIM_I8  (0xB6, Operation.RCR, DecodingGroup.RIM_DO_EI8, ExecutionGroup.SHIFT),
    NEG_RIM     (0xB7, Operation.NEG, DecodingGroup.RIM_DO, ExecutionGroup.NEG),
    
    SHL_RIM     (0xB8, Operation.SHL, DecodingGroup.RIM_NORMAL, ExecutionGroup.SHIFT),
    SHR_RIM     (0xB9, Operation.SHR, DecodingGroup.RIM_NORMAL, ExecutionGroup.SHIFT),
    SAR_RIM     (0xBA, Operation.SAR, DecodingGroup.RIM_NORMAL, ExecutionGroup.SHIFT),
    ROL_RIM     (0xBB, Operation.ROL, DecodingGroup.RIM_NORMAL, ExecutionGroup.SHIFT),
    ROR_RIM     (0xBC, Operation.ROR, DecodingGroup.RIM_NORMAL, ExecutionGroup.SHIFT),
    RCL_RIM     (0xBD, Operation.RCL, DecodingGroup.RIM_NORMAL, ExecutionGroup.SHIFT),
    RCR_RIM     (0xBE, Operation.RCR, DecodingGroup.RIM_NORMAL, ExecutionGroup.SHIFT),
    PNEG_RIMP   (0xBF, Operation.PNEG, DecodingGroup.RIM_PACKED_DO, ExecutionGroup.NEG),
    
    MUL_RIM     (0xC0, Operation.MUL, DecodingGroup.RIM_NORMAL, ExecutionGroup.MUL),
    AADJ        (0xC1, Operation.AADJ, DecodingGroup.RIM_PACKED_DO, ExecutionGroup.ADJ),
    MULH_RIM    (0xC2, Operation.MULH, DecodingGroup.RIM_WIDEDST, ExecutionGroup.MUL),
    MULSH_RIM   (0xC3, Operation.MULSH, DecodingGroup.RIM_WIDEDST, ExecutionGroup.MUL),
    PMUL_RIMP   (0xC4, Operation.PMUL, DecodingGroup.RIM_PACKED, ExecutionGroup.MUL),
    SADJ        (0xC5, Operation.SADJ, DecodingGroup.RIM_PACKED_DO, ExecutionGroup.ADJ),
    PMULH_RIMP  (0xC6, Operation.PMULH, DecodingGroup.RIM_PACKED_WIDEDST, ExecutionGroup.MUL),
    PMULSH_RIMP (0xC7, Operation.PMULSH, DecodingGroup.RIM_PACKED_WIDEDST, ExecutionGroup.MUL),
    
    DIV_RIM     (0xC8, Operation.DIV, DecodingGroup.RIM_NORMAL, ExecutionGroup.DIV),
    DIVS_RIM    (0xC9, Operation.DIVS, DecodingGroup.RIM_NORMAL, ExecutionGroup.DIV),
    DIVM_RIM    (0xCA, Operation.DIVM, DecodingGroup.RIM_WIDEDST, ExecutionGroup.DIV),
    DIVMS_RIM   (0xCB, Operation.DIVMS, DecodingGroup.RIM_WIDEDST, ExecutionGroup.DIV),
    PDIV_RIMP   (0xCC, Operation.PDIV, DecodingGroup.RIM_PACKED, ExecutionGroup.DIV),
    PDIVS_RIMP  (0xCD, Operation.PDIVS, DecodingGroup.RIM_PACKED, ExecutionGroup.DIV),
    PDIVM_RIMP  (0xCE, Operation.PDIVM, DecodingGroup.RIM_PACKED_WIDEDST, ExecutionGroup.DIV),
    PDIVMS_RIMP (0xCF, Operation.PDIVMS, DecodingGroup.RIM_PACKED_WIDEDST, ExecutionGroup.DIV),
    
    CALL_I8     (0xD0, Operation.CALL, DecodingGroup.I8, ExecutionGroup.CALL),
    CALL_I16    (0xD1, Operation.CALL, DecodingGroup.I16, ExecutionGroup.CALL),
    CALL_I32    (0xD2, Operation.CALL, DecodingGroup.I32, ExecutionGroup.CALL),
    CALL_RIM    (0xD3, Operation.CALL, DecodingGroup.RIM_SO, ExecutionGroup.CALL),
    CALLA_I32   (0xD4, Operation.CALLA, DecodingGroup.I32, ExecutionGroup.CALLA),
    CALLA_RIM32 (0xD5, Operation.CALLA, DecodingGroup.RIM_WIDE_SO, ExecutionGroup.CALLA),
    RET         (0xD6, Operation.RET, DecodingGroup.NODECODE, ExecutionGroup.RET),
    IRET        (0xD7, Operation.IRET, DecodingGroup.NODECODE, ExecutionGroup.IRET),
    
    JMP_I16     (0xD8, Operation.JMP, DecodingGroup.I16, ExecutionGroup.JMP),
    JMP_I32     (0xD9, Operation.JMP, DecodingGroup.I32, ExecutionGroup.JMP),
    JMPA_I32    (0xDA, Operation.JMPA, DecodingGroup.I32, ExecutionGroup.JMPA),
    JMPA_RIM32  (0xDB, Operation.JMPA, DecodingGroup.RIM_WIDE_SO, ExecutionGroup.JMPA),
    
    INT_I8      (0xDC, Operation.INT, DecodingGroup.I8, ExecutionGroup.INT),
    INT_RIM     (0xDD, Operation.INT, DecodingGroup.RIM_SO, ExecutionGroup.INT),
    UNDEF_DE    (0xDE, Operation.INV, DecodingGroup.UNDEF, ExecutionGroup.UNDEF),
    HLT         (0xDF, Operation.HLT, DecodingGroup.NODECODE, ExecutionGroup.HLT),
    
    JMP_I8      (0xE0, Operation.JMP, DecodingGroup.I8, ExecutionGroup.JMP),
    JCC_I8      (0xE1, Operation.JCC, DecodingGroup.I8_EI8, ExecutionGroup.JCC),
    JC_I8       (0xE2, Operation.JCC, DecodingGroup.I8, ExecutionGroup.JCC),
    JNC_I8      (0xE3, Operation.JCC, DecodingGroup.I8, ExecutionGroup.JCC),
    JS_I8       (0xE4, Operation.JCC, DecodingGroup.I8, ExecutionGroup.JCC),
    JNS_I8      (0xE5, Operation.JCC, DecodingGroup.I8, ExecutionGroup.JCC),
    JO_I8       (0xE6, Operation.JCC, DecodingGroup.I8, ExecutionGroup.JCC),
    JNO_I8      (0xE7, Operation.JCC, DecodingGroup.I8, ExecutionGroup.JCC),
    JZ_I8       (0xE8, Operation.JCC, DecodingGroup.I8, ExecutionGroup.JCC),
    JNZ_I8      (0xE9, Operation.JCC, DecodingGroup.I8, ExecutionGroup.JCC),
    JA_I8       (0xEA, Operation.JCC, DecodingGroup.I8, ExecutionGroup.JCC),
    JBE_I8      (0xEB, Operation.JCC, DecodingGroup.I8, ExecutionGroup.JCC),
    JG_I8       (0xEC, Operation.JCC, DecodingGroup.I8, ExecutionGroup.JCC),
    JGE_I8      (0xED, Operation.JCC, DecodingGroup.I8, ExecutionGroup.JCC),
    JL_I8       (0xEE, Operation.JCC, DecodingGroup.I8, ExecutionGroup.JCC),
    JLE_I8      (0xEF, Operation.JCC, DecodingGroup.I8, ExecutionGroup.JCC),
    
    JMP_RIM     (0xF0, Operation.JMP, DecodingGroup.RIM_SO, ExecutionGroup.JMP),
    JCC_RIM     (0xF1, Operation.JCC, DecodingGroup.RIM_SO_EI8, ExecutionGroup.JCC),
    JC_RIM      (0xF2, Operation.JCC, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    JNC_RIM     (0xF3, Operation.JCC, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    JS_RIM      (0xF4, Operation.JCC, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    JNS_RIM     (0xF5, Operation.JCC, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    JO_RIM      (0xF6, Operation.JCC, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    JNO_RIM     (0xF7, Operation.JCC, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    JZ_RIM      (0xF8, Operation.JCC, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    JNZ_RIM     (0xF9, Operation.JCC, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    JA_RIM      (0xFA, Operation.JCC, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    JBE_RIM     (0xFB, Operation.JCC, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    JG_RIM      (0xFC, Operation.JCC, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    JGE_RIM     (0xFD, Operation.JCC, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    JL_RIM      (0xFE, Operation.JCC, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    JLE_RIM     (0xFF, Operation.JCC, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    ;
    
    
    // opcode
    public final byte op;
    
    // mnemonic
    public final Operation type;
    
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
    private Opcode(int op, Operation type, DecodingGroup dgroup, ExecutionGroup egroup) {
        this.op = (byte) op; 
        this.type = type;
        this.dgroup = dgroup;
        this.egroup = egroup;
    }
    
    public static Opcode fromOp(byte op) {
        return opMap.get(op);
    }
    
    public byte getOp() { return this.op; }
    public Operation getType() { return this.type; }
}
