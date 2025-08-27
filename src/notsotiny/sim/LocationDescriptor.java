package notsotiny.sim;

public class LocationDescriptor {
    
    // Common locations
    public static final LocationDescriptor NULL = new LocationDescriptor(),
                                           REG_DA = new LocationDescriptor(Register.DA),
                                           REG_A = new LocationDescriptor(Register.A),
                                           REG_AH = new LocationDescriptor(Register.AH),
                                           REG_AL = new LocationDescriptor(Register.AL),
                                           REG_B = new LocationDescriptor(Register.B),
                                           REG_BH = new LocationDescriptor(Register.BH),
                                           REG_BL = new LocationDescriptor(Register.BL),
                                           REG_BC = new LocationDescriptor(Register.BC),
                                           REG_C = new LocationDescriptor(Register.C),
                                           REG_CH = new LocationDescriptor(Register.CH),
                                           REG_CL = new LocationDescriptor(Register.CL),
                                           REG_D = new LocationDescriptor(Register.D),
                                           REG_DH = new LocationDescriptor(Register.DH),
                                           REG_DL = new LocationDescriptor(Register.DL),
                                           REG_JI = new LocationDescriptor(Register.JI),
                                           REG_I = new LocationDescriptor(Register.I),
                                           REG_J = new LocationDescriptor(Register.J),
                                           REG_LK = new LocationDescriptor(Register.LK),
                                           REG_K = new LocationDescriptor(Register.K),
                                           REG_L = new LocationDescriptor(Register.L),
                                           REG_XP = new LocationDescriptor(Register.XP),
                                           REG_YP = new LocationDescriptor(Register.YP),
                                           REG_BP = new LocationDescriptor(Register.BP),
                                           REG_SP = new LocationDescriptor(Register.SP),
                                           REG_IP = new LocationDescriptor(Register.IP);
    
    public LocationType type;
    public Register register;
    public LocationSize size;
    public int address;

    /**
     * Blank constructor
     */
    public LocationDescriptor() {
        this.address = 0;
        this.type = LocationType.NULL;
        this.register = Register.NONE;
        this.size = LocationSize.NULL;
    }
    
    /**
     * Register constructor
     * @param type
     * @param register
     */
    public LocationDescriptor(Register register) {
        this.register = register;
        
        this.type = LocationType.REGISTER;
        this.size = register.lsize();
        this.address = 0;
    }
    
    /**
     * Memory/Immediate constructor
     * @param address
     */
    public LocationDescriptor(LocationType type, int address, LocationSize size) {
        this.type = type;
        this.address = address;
        this.size = size;
        
        this.register = Register.NONE;
    }
    
    @Override
    public String toString() {
        String str = "";
        
        switch(this.type) {
            case MEMORY:
                str = this.size.toString() + " MEMORY: " + String.format("%08X", this.address);
                break;
            
            case REGISTER:
                str = "REGISTER: " + this.register.toString();
                break;
            
            case IMMEDIATE:
                str = this.size.toString() + " IMMEDIATE: " + this.address;
                break;
            
            case NULL:
                str = "NULL";
                break;
        }
        
        return str;
    }
}
