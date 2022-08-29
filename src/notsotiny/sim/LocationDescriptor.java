package notsotiny.sim;

/**
 * Describes a location as it pertains to RIM. Records are pretty neat.
 * 
 * @param type The type of location
 * @param size Number of bytes
 * @param address Memory address. For H/L registers, 0 = L
 * @author Mechafinch
 */
public record LocationDescriptor(LocationType type, int size, int address) {
    
    public static final LocationDescriptor REGISTER_A = new LocationDescriptor(LocationType.REG_A, 2, 0),
                                           REGISTER_AL = new LocationDescriptor(LocationType.REG_A, 1, 0),
                                           REGISTER_AH = new LocationDescriptor(LocationType.REG_A, 1, 1),
                                           REGISTER_DA = new LocationDescriptor(LocationType.REG_A, 4, 0),
                                           REGISTER_B = new LocationDescriptor(LocationType.REG_B, 2, 0),
                                           REGISTER_BL = new LocationDescriptor(LocationType.REG_B, 1, 0),
                                           REGISTER_BH = new LocationDescriptor(LocationType.REG_B, 1, 1),
                                           REGISTER_AB = new LocationDescriptor(LocationType.REG_B, 4, 0),
                                           REGISTER_C = new LocationDescriptor(LocationType.REG_C, 2, 0),
                                           REGISTER_CL = new LocationDescriptor(LocationType.REG_C, 1, 0),
                                           REGISTER_CH = new LocationDescriptor(LocationType.REG_C, 1, 1),
                                           REGISTER_BC = new LocationDescriptor(LocationType.REG_C, 4, 0),
                                           REGISTER_D = new LocationDescriptor(LocationType.REG_D, 2, 0),
                                           REGISTER_DL = new LocationDescriptor(LocationType.REG_D, 1, 0),
                                           REGISTER_DH = new LocationDescriptor(LocationType.REG_D, 1, 1),
                                           REGISTER_CD = new LocationDescriptor(LocationType.REG_D, 4, 0),
                                           REGISTER_I = new LocationDescriptor(LocationType.REG_I, 2, 0),
                                           REGISTER_JI = new LocationDescriptor(LocationType.REG_I, 4, 0),
                                           REGISTER_J = new LocationDescriptor(LocationType.REG_J, 2, 0),
                                           REGISTER_K = new LocationDescriptor(LocationType.REG_K, 2, 0),
                                           REGISTER_LK = new LocationDescriptor(LocationType.REG_K, 4, 0),
                                           REGISTER_L = new LocationDescriptor(LocationType.REG_L, 2, 0),
                                           REGISTER_BP = new LocationDescriptor(LocationType.REG_BP, 4, 0),
                                           REGISTER_SP = new LocationDescriptor(LocationType.REG_SP, 4, 0),
                                           REGISTER_F = new LocationDescriptor(LocationType.REG_F, 2, 0),
                                           REGISTER_PF = new LocationDescriptor(LocationType.REG_PF, 2, 0);
    
    public enum LocationType {
        REG_A,
        REG_B,
        REG_C,
        REG_D,
        REG_I,
        REG_J,
        REG_K,
        REG_L,
        REG_BP,
        REG_SP,
        MEMORY,
        REG_F,
        REG_PF
    }
}
