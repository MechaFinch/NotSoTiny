package notsotiny.asm.components;

import notsotiny.asm.resolution.Resolvable;
import notsotiny.asm.resolution.ResolvableLocationDescriptor;
import notsotiny.sim.ops.Opcode;

/**
 * Represents an instruction for assembly
 * 
 * @author Mechafinch
 */
public class Instruction implements Component {
    
    public Opcode op;
    
    public ResolvableLocationDescriptor destination, source;
    
    private int width = -1;
    
    /**
     * Create an instruction with source and destination
     * 
     * @param op
     * @param destination
     * @param source
     * @param address
     */
    public Instruction(Opcode op, ResolvableLocationDescriptor destination, ResolvableLocationDescriptor source) {
        this.op = op;
        this.destination = destination;
        this.source = source;
    }
    
    /**
     * Create an instruction with destination only
     * 
     * @param op
     * @param location
     * @param destination
     * @param address
     */
    public Instruction(Opcode op, ResolvableLocationDescriptor location, boolean destination) {
        this(op, destination ? location : ResolvableLocationDescriptor.NONE, destination ? ResolvableLocationDescriptor.NONE : location);
    }
    
    /**
     * Create an instruction with no parameters
     * 
     * @param op
     */
    public Instruction(Opcode op) {
        this(op, ResolvableLocationDescriptor.NONE, ResolvableLocationDescriptor.NONE);
    }
    
    /**
     * @return true if resolved
     */
    public boolean isResolved() {
        return this.source.isResolved() && this.destination.isResolved();
    }
    
    @Override
    public int getSize() {
        if(this.width != -1) return this.width;
        
        // thank u assign & use value construction
        return this.width = switch(op) {
            /*
             * Single byte instructions
             */
            case NOP, XCHG_AH_AL, XCHG_BH_BL, XCHG_CH_CL, XCHG_DH_DL,
                 MOV_A_B, MOV_A_C, MOV_A_D, MOV_B_A, MOV_B_C, MOV_B_D,
                 MOV_C_A, MOV_C_B, MOV_C_D, MOV_D_A, MOV_D_B, MOV_D_C,
                 MOV_AL_BL, MOV_AL_CL, MOV_AL_DL, MOV_BL_AL, MOV_BL_CL, MOV_BL_DL,
                 MOV_CL_AL, MOV_CL_BL, MOV_CL_DL, MOV_DL_AL, MOV_DL_BL, MOV_DL_CL,
                 XCHG_A_B, XCHG_A_C, XCHG_A_D, XCHG_B_C, XCHG_B_D, XCHG_C_D,
                 XCHG_AL_BL, XCHG_AL_CL, XCHG_AL_DL, XCHG_BL_CL, XCHG_BL_DL, XCHG_CL_DL,
                 PUSH_A, PUSH_B, PUSH_C, PUSH_D, PUSH_I, PUSH_J, PUSH_BP, PUSH_SP, PUSH_F,
                 POP_A, POP_B, POP_C, POP_D, POP_I, POP_J, POP_BP, POP_SP, POP_F,
                 NOT_F, INC_I, INC_J, ICC_I, ICC_J, DEC_I, DEC_J, DCC_I, DCC_J,
                 RET, IRET -> 1;
            
            /*
             * Fixed Immediate Instructions
             */
            case MOV_A_I8, MOV_B_I8, MOV_C_I8, MOV_D_I8,
                 MOV_A_BI, MOV_B_BI, MOV_C_BI, MOV_D_BI,
                 MOV_BI_A, MOV_BI_B, MOV_BI_C, MOV_BI_D,
                 ADD_A_I8, ADD_B_I8, ADD_C_I8, ADD_D_I8,
                 ADC_A_I8, ADC_B_I8, ADC_C_I8, ADC_D_I8,
                 SUB_A_I8, SUB_B_I8, SUB_C_I8, SUB_D_I8,
                 SBB_A_I8, SBB_B_I8, SBB_C_I8, SBB_D_I8,
                 JMP_I8, JC_I8, JNC_I8, JS_I8, JNS_I8, JO_I8, JNO_I8, JZ_I8, JNZ_I8,
                 JA_I8, JBE_I8, JG_I8, JGE_I8, JL_I8, JLE_I8 -> 2;
                 
            case MOV_I_I16, MOV_J_I16, MOV_A_I16, MOV_B_I16, MOV_C_I16, MOV_D_I16,
                 ADD_A_I16, ADD_B_I16, ADD_C_I16, ADD_D_I16,
                 ADC_A_I16, ADC_B_I16, ADC_C_I16, ADC_D_I16,
                 SUB_A_I16, SUB_B_I16, SUB_C_I16, SUB_D_I16,
                 SBB_A_I16, SBB_B_I16, SBB_C_I16, SBB_D_I16,
                 JMP_I16, CALL_I16, INT_I16 -> 3;
                 
            case MOV_SP_I32, MOV_BP_I32,
                 MOV_A_O, MOV_B_O, MOV_C_O, MOV_D_O, MOV_O_A, MOV_O_B, MOV_O_C, MOV_O_D,
                 PUSH_I32, JMP_I32, JMPA_I32, CALLA_I32 -> 5;
            
            /*
             * RIM Instructions
             */
            case MOVW_RIM, MOVS_RIM, MOVZ_RIM, MOV_RIM, XCHG_RIM, 
                 PUSH_RIM, POP_RIM,
                 AND_F_RIM, AND_RIM_F, OR_F_RIM, OR_RIM_F, XOR_F_RIM, XOR_RIM_F, MOV_F_RIM, MOV_RIM_F,
                 ADD_RIM, ADC_RIM, PADD_RIMP, PADC_RIMP,
                 SUB_RIM, SBB_RIM, PSUB_RIMP, PSBB_RIMP,
                 INC_RIM, ICC_RIM, PINC_RIMP, PICC_RIMP, DEC_RIM, DCC_RIM, PDEC_RIMP, PDCC_RIMP,
                 MUL_RIM, MULH_RIM, MULSH_RIM, PMUL_RIMP, PMULH_RIMP, PMULSH_RIMP,
                 DIV_RIM, DIVS_RIM, DIVM_RIM, DIVMS_RIM, PDIV_RIMP, PDIVS_RIMP, PDIVM_RIMP, PDIVMS_RIMP,
                 AND_RIM, OR_RIM, XOR_RIM, NOT_RIM, NEG_RIM,
                 SHL_RIM, SHR_RIM, SAR_RIM, ROL_RIM, ROR_RIM, RCL_RIM, RCR_RIM,
                 JMP_RIM, JMPA_RIM32, CALL_RIM, CALLA_RIM32,
                 INT_RIM, LEA_RIM, CMP_RIM, CMP_RIM_0,
                 JC_RIM, JNC_RIM, JS_RIM, JNS_RIM, JO_RIM, JNO_RIM, JZ_RIM, JNZ_RIM,
                 JA_RIM, JBE_RIM, JG_RIM, JGE_RIM, JL_RIM, JLE_RIM -> getRIMWidth() + 1;
            
            /*
             * RIM + Immediate Instructions
             */
            case ADD_RIM_I8, ADC_RIM_I8, SUB_RIM_I8, SBB_RIM_I8, CMP_RIM_I8 -> getRIMWidth() + 2;
            
            /*
             * BIO w/ Offset Instructions
             */
            case MOV_A_BIO, MOV_B_BIO, MOV_C_BIO, MOV_D_BIO -> getBIOWidth(source) + 1;
            
            case MOV_BIO_A, MOV_BIO_B, MOV_BIO_C, MOV_BIO_D -> getBIOWidth(destination) + 1;
        };
    }
    
    private int getRIMWidth() {
        return -1;
    }
    
    private int getBIOWidth(Resolvable location) {
        return -1;
    }
    
    @Override
    public String toString() {
        return this.op.toString() + " " + this.destination.toString() + " " + this.source.toString();
    }
}
