package notsotiny.sim.ops;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum of every opcode
 * 
 * @author Mechafinch
 */
public enum Opcode {    
    NOP         (0x00, Operation.NOP, DecodingGroup.NODECODE, ExecutionGroup.NOP),
    
    HLT         (0x01, Operation.HLT, DecodingGroup.NODECODE, ExecutionGroup.HLT),
    MOVS_RIM    (0x02, Operation.MOVS, DecodingGroup.RIM_WIDEDST_WOD, ExecutionGroup.MOVS),
    MOVZ_RIM    (0x03, Operation.MOVZ, DecodingGroup.RIM_WIDEDST_WOD, ExecutionGroup.MOVZ),
    
    MOVS_A_I8   (0x04, Operation.MOV, DecodingGroup.I8, ExecutionGroup.MOVS),
    MOVS_B_I8   (0x05, Operation.MOV, DecodingGroup.I8, ExecutionGroup.MOVS),
    MOVS_C_I8   (0x06, Operation.MOV, DecodingGroup.I8, ExecutionGroup.MOVS),
    MOVS_D_I8   (0x07, Operation.MOV, DecodingGroup.I8, ExecutionGroup.MOVS),
    
    MOV_A_I16   (0x08, Operation.MOV, DecodingGroup.I16, ExecutionGroup.MOV_SHORTCUT_SRC),
    MOV_B_I16   (0x09, Operation.MOV, DecodingGroup.I16, ExecutionGroup.MOV_SHORTCUT_SRC),
    MOV_C_I16   (0x0A, Operation.MOV, DecodingGroup.I16, ExecutionGroup.MOV_SHORTCUT_SRC),
    MOV_D_I16   (0x0B, Operation.MOV, DecodingGroup.I16, ExecutionGroup.MOV_SHORTCUT_SRC),
    MOV_I_I16   (0x0C, Operation.MOV, DecodingGroup.I16, ExecutionGroup.MOV_SHORTCUT_SRC),
    MOV_J_I16   (0x0D, Operation.MOV, DecodingGroup.I16, ExecutionGroup.MOV_SHORTCUT_SRC),
    MOV_K_I16   (0x0E, Operation.MOV, DecodingGroup.I16, ExecutionGroup.MOV_SHORTCUT_SRC),
    MOV_L_I16   (0x0F, Operation.MOV, DecodingGroup.I16, ExecutionGroup.MOV_SHORTCUT_SRC),
    
    MOV_A_BI    (0x10, Operation.MOV, DecodingGroup.BI_SRC, ExecutionGroup.MOV_SHORTCUT_SRC),
    MOV_B_BI    (0x11, Operation.MOV, DecodingGroup.BI_SRC, ExecutionGroup.MOV_SHORTCUT_SRC),
    MOV_C_BI    (0x12, Operation.MOV, DecodingGroup.BI_SRC, ExecutionGroup.MOV_SHORTCUT_SRC),
    MOV_D_BI    (0x13, Operation.MOV, DecodingGroup.BI_SRC, ExecutionGroup.MOV_SHORTCUT_SRC),
    MOV_A_BIO   (0x14, Operation.MOV, DecodingGroup.BIO_SRC, ExecutionGroup.MOV_SHORTCUT_SRC),
    MOV_B_BIO   (0x15, Operation.MOV, DecodingGroup.BIO_SRC, ExecutionGroup.MOV_SHORTCUT_SRC),
    MOV_C_BIO   (0x16, Operation.MOV, DecodingGroup.BIO_SRC, ExecutionGroup.MOV_SHORTCUT_SRC),
    MOV_D_BIO   (0x17, Operation.MOV, DecodingGroup.BIO_SRC, ExecutionGroup.MOV_SHORTCUT_SRC),
    
    MOV_BI_A    (0x18, Operation.MOV, DecodingGroup.BI_DST, ExecutionGroup.MOV_SHORTCUT_DST),
    MOV_BI_B    (0x19, Operation.MOV, DecodingGroup.BI_DST, ExecutionGroup.MOV_SHORTCUT_DST),
    MOV_BI_C    (0x1A, Operation.MOV, DecodingGroup.BI_DST, ExecutionGroup.MOV_SHORTCUT_DST),
    MOV_BI_D    (0x1B, Operation.MOV, DecodingGroup.BI_DST, ExecutionGroup.MOV_SHORTCUT_DST),
    MOV_BIO_A   (0x1C, Operation.MOV, DecodingGroup.BIO_DST, ExecutionGroup.MOV_SHORTCUT_DST),
    MOV_BIO_B   (0x1D, Operation.MOV, DecodingGroup.BIO_DST, ExecutionGroup.MOV_SHORTCUT_DST),
    MOV_BIO_C   (0x1E, Operation.MOV, DecodingGroup.BIO_DST, ExecutionGroup.MOV_SHORTCUT_DST),
    MOV_BIO_D   (0x1F, Operation.MOV, DecodingGroup.BIO_DST, ExecutionGroup.MOV_SHORTCUT_DST),
    
    MOV_A_O     (0x20, Operation.MOV, DecodingGroup.O_SRC, ExecutionGroup.MOV_SHORTCUT_SRC),
    MOV_B_O     (0x21, Operation.MOV, DecodingGroup.O_SRC, ExecutionGroup.MOV_SHORTCUT_SRC),
    MOV_C_O     (0x22, Operation.MOV, DecodingGroup.O_SRC, ExecutionGroup.MOV_SHORTCUT_SRC),
    MOV_D_O     (0x23, Operation.MOV, DecodingGroup.O_SRC, ExecutionGroup.MOV_SHORTCUT_SRC),
    MOV_O_A     (0x24, Operation.MOV, DecodingGroup.O_DST, ExecutionGroup.MOV_SHORTCUT_DST),
    MOV_O_B     (0x25, Operation.MOV, DecodingGroup.O_DST, ExecutionGroup.MOV_SHORTCUT_DST),
    MOV_O_C     (0x26, Operation.MOV, DecodingGroup.O_DST, ExecutionGroup.MOV_SHORTCUT_DST),
    MOV_O_D     (0x27, Operation.MOV, DecodingGroup.O_DST, ExecutionGroup.MOV_SHORTCUT_DST),
    
    XCHG_RIM    (0x28, Operation.XCHG, DecodingGroup.RIM_NORMAL, ExecutionGroup.XCHG),
    XCHGW_RIM   (0x29, Operation.XCHGW, DecodingGroup.RIM_WIDE, ExecutionGroup.XCHG),
    MOV_RIM     (0x2A, Operation.MOV, DecodingGroup.RIM_WOD, ExecutionGroup.MOV),
    MOVW_RIM    (0x2B, Operation.MOVW, DecodingGroup.RIM_WIDE_WOD, ExecutionGroup.MOV),
    
    MOV_A_B     (0x2C, Operation.MOV, DecodingGroup.NODECODE, ExecutionGroup.MOV_SHORTCUT_REG),
    MOV_A_C     (0x2D, Operation.MOV, DecodingGroup.NODECODE, ExecutionGroup.MOV_SHORTCUT_REG),
    MOV_A_D     (0x2E, Operation.MOV, DecodingGroup.NODECODE, ExecutionGroup.MOV_SHORTCUT_REG),
    MOV_B_A     (0x2F, Operation.MOV, DecodingGroup.NODECODE, ExecutionGroup.MOV_SHORTCUT_REG),
    MOV_B_C     (0x30, Operation.MOV, DecodingGroup.NODECODE, ExecutionGroup.MOV_SHORTCUT_REG),
    MOV_B_D     (0x31, Operation.MOV, DecodingGroup.NODECODE, ExecutionGroup.MOV_SHORTCUT_REG),
    MOV_C_A     (0x32, Operation.MOV, DecodingGroup.NODECODE, ExecutionGroup.MOV_SHORTCUT_REG),
    MOV_C_B     (0x33, Operation.MOV, DecodingGroup.NODECODE, ExecutionGroup.MOV_SHORTCUT_REG),
    MOV_C_D     (0x34, Operation.MOV, DecodingGroup.NODECODE, ExecutionGroup.MOV_SHORTCUT_REG),
    MOV_D_A     (0x35, Operation.MOV, DecodingGroup.NODECODE, ExecutionGroup.MOV_SHORTCUT_REG),
    MOV_D_B     (0x36, Operation.MOV, DecodingGroup.NODECODE, ExecutionGroup.MOV_SHORTCUT_REG),
    MOV_D_C     (0x37, Operation.MOV, DecodingGroup.NODECODE, ExecutionGroup.MOV_SHORTCUT_REG),
    
    PUSH_A      (0x38, Operation.PUSH, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSH_B      (0x39, Operation.PUSH, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSH_C      (0x3A, Operation.PUSH, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSH_D      (0x3B, Operation.PUSH, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSH_I      (0x3C, Operation.PUSH, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSH_J      (0x3D, Operation.PUSH, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSH_K      (0x3E, Operation.PUSH, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSH_L      (0x3F, Operation.PUSH, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSH_BP     (0x40, Operation.PUSH, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    BPUSH_SP    (0x41, Operation.PUSH, DecodingGroup.NODECODE, ExecutionGroup.BPP_SHORTCUT),
    PUSH_F      (0x42, Operation.PUSH, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    PUSH_PF     (0x43, Operation.PUSH, DecodingGroup.NODECODE, ExecutionGroup.PUSH_SHORTCUT),
    
    POP_A       (0x44, Operation.POP, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POP_B       (0x45, Operation.POP, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POP_C       (0x46, Operation.POP, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POP_D       (0x47, Operation.POP, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POP_I       (0x48, Operation.POP, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POP_J       (0x49, Operation.POP, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POP_K       (0x4A, Operation.POP, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POP_L       (0x4B, Operation.POP, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POP_BP      (0x4C, Operation.POP, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    BPOP_SP     (0x4D, Operation.POP, DecodingGroup.NODECODE, ExecutionGroup.BPP_SHORTCUT),
    POP_F       (0x4E, Operation.POP, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    POP_PF      (0x4F, Operation.POP, DecodingGroup.NODECODE, ExecutionGroup.POP_SHORTCUT),
    
    PUSH_RIM    (0x50, Operation.PUSH, DecodingGroup.RIM_SO, ExecutionGroup.PUSH),
    PUSHW_RIM   (0x51, Operation.PUSH, DecodingGroup.RIM_WIDE_SO, ExecutionGroup.PUSH),
    BPUSH_RIM   (0x52, Operation.PUSH, DecodingGroup.RIM_SO, ExecutionGroup.BPUSH),
    BPUSHW_RIM  (0x53, Operation.PUSH, DecodingGroup.RIM_WIDE_SO, ExecutionGroup.BPUSH),
    POP_RIM     (0x54, Operation.POP, DecodingGroup.RIM_DO, ExecutionGroup.POP),
    POPW_RIM    (0x55, Operation.POP, DecodingGroup.RIM_WIDEDST_DO_WOD, ExecutionGroup.POP),
    BPOP_RIM    (0x56, Operation.POP, DecodingGroup.RIM_DO, ExecutionGroup.BPOP),
    BPOPW_RIM   (0x57, Operation.POP, DecodingGroup.RIM_WIDEDST_DO_WOD, ExecutionGroup.BPOP),
    PUSHW_I32   (0x58, Operation.PUSH, DecodingGroup.I32, ExecutionGroup.PUSH),
    BPUSHW_I32  (0x59, Operation.PUSH, DecodingGroup.I32, ExecutionGroup.BPUSH),
    PUSHA       (0x5A, Operation.PUSHA, DecodingGroup.NODECODE, ExecutionGroup.PUSHA),
    POPA        (0x5B, Operation.POPA, DecodingGroup.NODECODE, ExecutionGroup.POPA),
    TST_RIM     (0x5C, Operation.TST, DecodingGroup.RIM_NORMAL, ExecutionGroup.TST),
    PTST_RIMP   (0x5D, Operation.PTST, DecodingGroup.RIM_PACKED, ExecutionGroup.TST),
    UNDEF_5E    (0x5E, Operation.INV, DecodingGroup.UNDEF, ExecutionGroup.UNDEF),
    UNDEF_5F    (0x5F, Operation.INV, DecodingGroup.UNDEF, ExecutionGroup.UNDEF),
    
    AND_F_RIM   (0x60, Operation.AND, DecodingGroup.RIM_SO, ExecutionGroup.F_OPS),
    AND_RIM_F   (0x61, Operation.AND, DecodingGroup.RIM_DO, ExecutionGroup.F_OPS),
    OR_F_RIM    (0x62, Operation.OR, DecodingGroup.RIM_SO, ExecutionGroup.F_OPS),
    OR_RIM_F    (0x63, Operation.OR, DecodingGroup.RIM_DO, ExecutionGroup.F_OPS),
    XOR_F_RIM   (0x64, Operation.XOR, DecodingGroup.RIM_SO, ExecutionGroup.F_OPS),
    XOR_RIM_F   (0x65, Operation.XOR, DecodingGroup.RIM_DO, ExecutionGroup.F_OPS),
    NOT_F       (0x66, Operation.NOT, DecodingGroup.NODECODE, ExecutionGroup.F_OPS),
    MOV_F_RIM   (0x67, Operation.MOV, DecodingGroup.RIM_SO, ExecutionGroup.MOV_SHORTCUT_SRC),
    MOV_RIM_F   (0x68, Operation.MOV, DecodingGroup.RIM_DO_WOD, ExecutionGroup.MOV_SHORTCUT_DST),
    MOV_PR_RIM  (0x69, Operation.MOV, DecodingGroup.RIM_WIDE, ExecutionGroup.MOV_PROTECTED),
    MOV_RIM_PR  (0x6A, Operation.MOV, DecodingGroup.RIM_WIDE, ExecutionGroup.MOV_PROTECTED),
    
    LEA_RIM     (0x6B, Operation.LEA, DecodingGroup.RIM_LEA, ExecutionGroup.MOV),
    CMP_RIM     (0x6C, Operation.CMP, DecodingGroup.RIM_NORMAL, ExecutionGroup.CMP),
    CMP_RIM_I8  (0x6D, Operation.CMP, DecodingGroup.RIM_DO_EI8, ExecutionGroup.CMP),
    CMP_RIM_0   (0x6E, Operation.CMP, DecodingGroup.RIM_DO, ExecutionGroup.CMP),
    PCMP_RIMP   (0x6F, Operation.PCMP, DecodingGroup.RIM_PACKED, ExecutionGroup.PCMP),
    
    ADD_A_I8    (0x70, Operation.ADD, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADD_B_I8    (0x71, Operation.ADD, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADD_C_I8    (0x72, Operation.ADD, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADD_D_I8    (0x73, Operation.ADD, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADD_I_I8    (0x74, Operation.ADD, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADD_J_I8    (0x75, Operation.ADD, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADD_K_I8    (0x76, Operation.ADD, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADD_L_I8    (0x77, Operation.ADD, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    
    ICC_A       (0x78, Operation.ICC, DecodingGroup.NODECODE, ExecutionGroup.INC_SHORTCUT),
    ICC_B       (0x79, Operation.ICC, DecodingGroup.NODECODE, ExecutionGroup.INC_SHORTCUT),
    ICC_C       (0x7A, Operation.ICC, DecodingGroup.NODECODE, ExecutionGroup.INC_SHORTCUT),
    ICC_D       (0x7B, Operation.ICC, DecodingGroup.NODECODE, ExecutionGroup.INC_SHORTCUT),
    ICC_I       (0x7C, Operation.ICC, DecodingGroup.NODECODE, ExecutionGroup.INC_SHORTCUT),
    ICC_J       (0x7D, Operation.ICC, DecodingGroup.NODECODE, ExecutionGroup.INC_SHORTCUT),
    ICC_K       (0x7E, Operation.ICC, DecodingGroup.NODECODE, ExecutionGroup.INC_SHORTCUT),
    ICC_L       (0x7F, Operation.ICC, DecodingGroup.NODECODE, ExecutionGroup.INC_SHORTCUT),
    
    SUB_A_I8    (0x80, Operation.SUB, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUB_B_I8    (0x81, Operation.SUB, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUB_C_I8    (0x82, Operation.SUB, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUB_D_I8    (0x83, Operation.SUB, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUB_I_I8    (0x84, Operation.SUB, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUB_J_I8    (0x85, Operation.SUB, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUB_K_I8    (0x86, Operation.SUB, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUB_L_I8    (0x87, Operation.SUB, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    
    DCC_A       (0x88, Operation.DCC, DecodingGroup.NODECODE, ExecutionGroup.INC_SHORTCUT),
    DCC_B       (0x89, Operation.DCC, DecodingGroup.NODECODE, ExecutionGroup.INC_SHORTCUT),
    DCC_C       (0x8A, Operation.DCC, DecodingGroup.NODECODE, ExecutionGroup.INC_SHORTCUT),
    DCC_D       (0x8B, Operation.DCC, DecodingGroup.NODECODE, ExecutionGroup.INC_SHORTCUT),
    DCC_I       (0x8C, Operation.DCC, DecodingGroup.NODECODE, ExecutionGroup.INC_SHORTCUT),
    DCC_J       (0x8D, Operation.DCC, DecodingGroup.NODECODE, ExecutionGroup.INC_SHORTCUT),
    DCC_K       (0x8E, Operation.DCC, DecodingGroup.NODECODE, ExecutionGroup.INC_SHORTCUT),
    DCC_L       (0x8F, Operation.DCC, DecodingGroup.NODECODE, ExecutionGroup.INC_SHORTCUT),
    
    ADD_RIM     (0x90, Operation.ADD, DecodingGroup.RIM_NORMAL, ExecutionGroup.ADD),
    ADD_RIM_I8  (0x91, Operation.ADD, DecodingGroup.RIM_DO_EI8, ExecutionGroup.ADD),
    ADC_RIM     (0x92, Operation.ADC, DecodingGroup.RIM_NORMAL, ExecutionGroup.ADC),
    ADC_RIM_I8  (0x93, Operation.ADC, DecodingGroup.RIM_DO_EI8, ExecutionGroup.ADC),
    PADD_RIMP   (0x94, Operation.PADD, DecodingGroup.RIM_PACKED, ExecutionGroup.PADD),
    PADC_RIMP   (0x95, Operation.PADC, DecodingGroup.RIM_PACKED, ExecutionGroup.PADD),
    ADD_SP_I8   (0x96, Operation.ADD, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    ADD_BP_I8   (0x97, Operation.ADD, DecodingGroup.I8, ExecutionGroup.ADD_SHORTCUT),
    
    SUB_RIM     (0x98, Operation.SUB, DecodingGroup.RIM_NORMAL, ExecutionGroup.SUB),
    SUB_RIM_I8  (0x99, Operation.SUB, DecodingGroup.RIM_DO_EI8, ExecutionGroup.SUB),
    SBB_RIM     (0x9A, Operation.SBB, DecodingGroup.RIM_NORMAL, ExecutionGroup.SBB),
    SBB_RIM_I8  (0x9B, Operation.SBB, DecodingGroup.RIM_DO_EI8, ExecutionGroup.SBB),
    PSUB_RIMP   (0x9C, Operation.PSUB, DecodingGroup.RIM_PACKED, ExecutionGroup.PSUB),
    PSBB_RIMP   (0x9D, Operation.PSBB, DecodingGroup.RIM_PACKED, ExecutionGroup.PSUB),
    SUB_SP_I8   (0x9E, Operation.SUB, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    SUB_BP_I8	(0x9F, Operation.SUB, DecodingGroup.I8, ExecutionGroup.SUB_SHORTCUT),
    
    MUL_RIM     (0xA0, Operation.MUL, DecodingGroup.RIM_NORMAL, ExecutionGroup.MUL),
    MULH_RIM    (0xA1, Operation.MULH, DecodingGroup.RIM_WIDEDST, ExecutionGroup.MUL),
    MULSH_RIM   (0xA2, Operation.MULSH, DecodingGroup.RIM_WIDEDST, ExecutionGroup.MUL),
    CMOVCC_RIM  (0xA3, Operation.CMOVCC, DecodingGroup.RIM_WOD_EI8, ExecutionGroup.CMOV),
    PMUL_RIMP   (0xA4, Operation.PMUL, DecodingGroup.RIM_PACKED, ExecutionGroup.MUL),
    PMULH_RIMP  (0xA5, Operation.PMULH, DecodingGroup.RIM_PACKED_WIDEDST, ExecutionGroup.MUL),
    PMULSH_RIMP (0xA6, Operation.PMULSH, DecodingGroup.RIM_PACKED_WIDEDST, ExecutionGroup.MUL),
    PCMOVCC_RIMP(0xA7, Operation.PCMOVCC, DecodingGroup.RIM_PACKED_EI8, ExecutionGroup.CMOV),
    
    DIV_RIM     (0xA8, Operation.DIV, DecodingGroup.RIM_NORMAL, ExecutionGroup.DIV),
    DIVS_RIM    (0xA9, Operation.DIVS, DecodingGroup.RIM_NORMAL, ExecutionGroup.DIV),
    DIVM_RIM    (0xAA, Operation.DIVM, DecodingGroup.RIM_WIDEDST, ExecutionGroup.DIV),
    DIVMS_RIM   (0xAB, Operation.DIVMS, DecodingGroup.RIM_WIDEDST, ExecutionGroup.DIV),
    PDIV_RIMP   (0xAC, Operation.PDIV, DecodingGroup.RIM_PACKED, ExecutionGroup.DIV),
    PDIVS_RIMP  (0xAD, Operation.PDIVS, DecodingGroup.RIM_PACKED, ExecutionGroup.DIV),
    PDIVM_RIMP  (0xAE, Operation.PDIVM, DecodingGroup.RIM_PACKED_WIDEDST, ExecutionGroup.DIV),
    PDIVMS_RIMP (0xAF, Operation.PDIVMS, DecodingGroup.RIM_PACKED_WIDEDST, ExecutionGroup.DIV),
    
    INC_I       (0xB0, Operation.INC, DecodingGroup.NODECODE, ExecutionGroup.INC_SHORTCUT),
    INC_J       (0xB1, Operation.INC, DecodingGroup.NODECODE, ExecutionGroup.INC_SHORTCUT),
    INC_K       (0xB2, Operation.INC, DecodingGroup.NODECODE, ExecutionGroup.INC_SHORTCUT),
    INC_L       (0xB3, Operation.INC, DecodingGroup.NODECODE, ExecutionGroup.INC_SHORTCUT),
    DEC_I       (0xB4, Operation.DEC, DecodingGroup.NODECODE, ExecutionGroup.INC_SHORTCUT),
    DEC_J       (0xB5, Operation.DEC, DecodingGroup.NODECODE, ExecutionGroup.INC_SHORTCUT),
    DEC_K       (0xB6, Operation.DEC, DecodingGroup.NODECODE, ExecutionGroup.INC_SHORTCUT),
    DEC_L       (0xB7, Operation.DEC, DecodingGroup.NODECODE, ExecutionGroup.INC_SHORTCUT),
    
    INC_RIM     (0xB8, Operation.INC, DecodingGroup.RIM_DO, ExecutionGroup.INC),
    ICC_RIM     (0xB9, Operation.ICC, DecodingGroup.RIM_DO, ExecutionGroup.INC),
    PINC_RIMP   (0xBA, Operation.PINC, DecodingGroup.RIM_PACKED_DO, ExecutionGroup.PINC),
    PICC_RIMP   (0xBB, Operation.PICC, DecodingGroup.RIM_PACKED_DO, ExecutionGroup.PINC),
    DEC_RIM     (0xBC, Operation.DEC, DecodingGroup.RIM_DO, ExecutionGroup.INC),
    DCC_RIM     (0xBD, Operation.DCC, DecodingGroup.RIM_DO, ExecutionGroup.INC),
    PDEC_RIMP   (0xBE, Operation.PDCC, DecodingGroup.RIM_PACKED_DO, ExecutionGroup.PINC),
    PDCC_RIMP   (0xBF, Operation.PDCC, DecodingGroup.RIM_PACKED_DO, ExecutionGroup.PINC),
    
    SHL_RIM_I8  (0xC0, Operation.SHL, DecodingGroup.RIM_DO_EI8, ExecutionGroup.SHIFT),
    SHR_RIM_I8  (0xC1, Operation.SHR, DecodingGroup.RIM_DO_EI8, ExecutionGroup.SHIFT),
    SAR_RIM_I8  (0xC2, Operation.SAR, DecodingGroup.RIM_DO_EI8, ExecutionGroup.SHIFT),
    ROL_RIM_I8  (0xC3, Operation.ROL, DecodingGroup.RIM_DO_EI8, ExecutionGroup.SHIFT),
    ROR_RIM_I8  (0xC4, Operation.ROR, DecodingGroup.RIM_DO_EI8, ExecutionGroup.SHIFT),
    RCL_RIM_I8  (0xC5, Operation.RCL, DecodingGroup.RIM_DO_EI8, ExecutionGroup.SHIFT),
    RCR_RIM_I8  (0xC6, Operation.RCR, DecodingGroup.RIM_DO_EI8, ExecutionGroup.SHIFT),
    
    UNDEF_C7    (0xC7, Operation.INV, DecodingGroup.UNDEF, ExecutionGroup.UNDEF),
    
    SHL_RIM     (0xC8, Operation.SHL, DecodingGroup.RIM_NORMAL, ExecutionGroup.SHIFT),
    SHR_RIM     (0xC9, Operation.SHR, DecodingGroup.RIM_NORMAL, ExecutionGroup.SHIFT),
    SAR_RIM     (0xCA, Operation.SAR, DecodingGroup.RIM_NORMAL, ExecutionGroup.SHIFT),
    ROL_RIM     (0xCB, Operation.ROL, DecodingGroup.RIM_NORMAL, ExecutionGroup.SHIFT),
    ROR_RIM     (0xCC, Operation.ROR, DecodingGroup.RIM_NORMAL, ExecutionGroup.SHIFT),
    RCL_RIM     (0xCD, Operation.RCL, DecodingGroup.RIM_NORMAL, ExecutionGroup.SHIFT),
    RCR_RIM     (0xCE, Operation.RCR, DecodingGroup.RIM_NORMAL, ExecutionGroup.SHIFT),
    NEG_RIM     (0xCF, Operation.NEG, DecodingGroup.RIM_DO, ExecutionGroup.NEG),
    
    AND_RIM     (0xD0, Operation.AND, DecodingGroup.RIM_NORMAL, ExecutionGroup.LOGIC),
    OR_RIM      (0xD1, Operation.OR, DecodingGroup.RIM_NORMAL, ExecutionGroup.LOGIC),
    XOR_RIM     (0xD2, Operation.XOR, DecodingGroup.RIM_NORMAL, ExecutionGroup.LOGIC),
    NOT_RIM     (0xD3, Operation.NOT, DecodingGroup.RIM_DO, ExecutionGroup.LOGIC),
    
    CALL_I8     (0xD4, Operation.CALL, DecodingGroup.I8, ExecutionGroup.CALL),
    CALL_I16    (0xD5, Operation.CALL, DecodingGroup.I16, ExecutionGroup.CALL),
    CALL_I32    (0xD6, Operation.CALL, DecodingGroup.I32, ExecutionGroup.CALL),
    CALL_RIM    (0xD7, Operation.CALL, DecodingGroup.RIM_SO, ExecutionGroup.CALL),
    CALLA_I32   (0xD8, Operation.CALLA, DecodingGroup.I32, ExecutionGroup.CALLA),
    CALLA_RIM32 (0xD9, Operation.CALLA, DecodingGroup.RIM_WIDE_SO, ExecutionGroup.CALLA),
    
    JMP_I8      (0xDA, Operation.JMP, DecodingGroup.I8, ExecutionGroup.JMP),
    JMP_I16     (0xDB, Operation.JMP, DecodingGroup.I16, ExecutionGroup.JMP),
    JMP_I32     (0xDC, Operation.JMP, DecodingGroup.I32, ExecutionGroup.JMP),
    JMP_RIM     (0xDD, Operation.JMP, DecodingGroup.RIM_SO, ExecutionGroup.JMP),
    JMPA_I32    (0xDE, Operation.JMPA, DecodingGroup.I32, ExecutionGroup.JMPA),
    JMPA_RIM32  (0xDF, Operation.JMPA, DecodingGroup.RIM_WIDE_SO, ExecutionGroup.JMPA),
    
    RET         (0xE0, Operation.RET, DecodingGroup.NODECODE, ExecutionGroup.RET),
    IRET        (0xE1, Operation.IRET, DecodingGroup.NODECODE, ExecutionGroup.IRET),
    INT_I8      (0xE2, Operation.INT, DecodingGroup.I8, ExecutionGroup.INT),
    INT_RIM     (0xE3, Operation.INT, DecodingGroup.RIM_SO, ExecutionGroup.INT),
    
    JC_I8       (0xE4, Operation.JCC, DecodingGroup.I8, ExecutionGroup.JCC),
    JC_RIM      (0xE5, Operation.JCC, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    JNC_I8      (0xE6, Operation.JCC, DecodingGroup.I8, ExecutionGroup.JCC),
    JNC_RIM     (0xE7, Operation.JCC, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    
    JS_I8       (0xE8, Operation.JCC, DecodingGroup.I8, ExecutionGroup.JCC),
    JS_RIM      (0xE9, Operation.JCC, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    JNS_I8      (0xEA, Operation.JCC, DecodingGroup.I8, ExecutionGroup.JCC),
    JNS_RIM     (0xEB, Operation.JCC, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    
    JO_I8       (0xEC, Operation.JCC, DecodingGroup.I8, ExecutionGroup.JCC),
    JO_RIM      (0xED, Operation.JCC, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    JNO_I8      (0xEE, Operation.JCC, DecodingGroup.I8, ExecutionGroup.JCC),
    JNO_RIM     (0xEF, Operation.JCC, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    
    JZ_I8       (0xF0, Operation.JCC, DecodingGroup.I8, ExecutionGroup.JCC),
    JZ_RIM      (0xF1, Operation.JCC, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    JNZ_I8      (0xF2, Operation.JCC, DecodingGroup.I8, ExecutionGroup.JCC),
    JNZ_RIM     (0xF3, Operation.JCC, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    
    JA_I8       (0xF4, Operation.JCC, DecodingGroup.I8, ExecutionGroup.JCC),
    JA_RIM      (0xF5, Operation.JCC, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    
    JBE_I8      (0xF6, Operation.JCC, DecodingGroup.I8, ExecutionGroup.JCC),
    JBE_RIM     (0xF7, Operation.JCC, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    
    JG_I8       (0xF8, Operation.JCC, DecodingGroup.I8, ExecutionGroup.JCC),
    JG_RIM      (0xF9, Operation.JCC, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    JGE_I8      (0xFA, Operation.JCC, DecodingGroup.I8, ExecutionGroup.JCC),
    JGE_RIM     (0xFB, Operation.JCC, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    
    JL_I8       (0xFC, Operation.JCC, DecodingGroup.I8, ExecutionGroup.JCC),
    JL_RIM      (0xFD, Operation.JCC, DecodingGroup.RIM_SO, ExecutionGroup.JCC),
    JLE_I8      (0xFE, Operation.JCC, DecodingGroup.I8, ExecutionGroup.JCC),
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
