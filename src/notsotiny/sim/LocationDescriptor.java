package notsotiny.sim;

public class LocationDescriptor {
    
    // Common locations
    public static final LocationDescriptor NULL = new LocationDescriptor(),
                                           REG_DA = new LocationDescriptor(Registers.DA),
                                           REG_A = new LocationDescriptor(Registers.A),
                                           REG_AH = new LocationDescriptor(Registers.AH),
                                           REG_AL = new LocationDescriptor(Registers.AL),
                                           REG_AB = new LocationDescriptor(Registers.AB),
                                           REG_B = new LocationDescriptor(Registers.B),
                                           REG_BH = new LocationDescriptor(Registers.BH),
                                           REG_BL = new LocationDescriptor(Registers.BL),
                                           REG_BC = new LocationDescriptor(Registers.BC),
                                           REG_C = new LocationDescriptor(Registers.C),
                                           REG_CH = new LocationDescriptor(Registers.CH),
                                           REG_CL = new LocationDescriptor(Registers.CL),
                                           REG_CD = new LocationDescriptor(Registers.CD),
                                           REG_D = new LocationDescriptor(Registers.D),
                                           REG_DH = new LocationDescriptor(Registers.DH),
                                           REG_DL = new LocationDescriptor(Registers.DL),
                                           REG_JI = new LocationDescriptor(Registers.JI),
                                           REG_I = new LocationDescriptor(Registers.I),
                                           REG_J = new LocationDescriptor(Registers.J),
                                           REG_LK = new LocationDescriptor(Registers.LK),
                                           REG_K = new LocationDescriptor(Registers.K),
                                           REG_L = new LocationDescriptor(Registers.L),
                                           REG_IP = new LocationDescriptor(Registers.IP),
                                           REG_BP = new LocationDescriptor(Registers.BP),
                                           REG_SP = new LocationDescriptor(Registers.SP);
    
    public LocationType type;
    public Registers register;
    public LocationSize size;
    public int address;

    /**
     * Blank constructor
     */
    public LocationDescriptor() {
        this.address = 0;
        this.type = LocationType.NULL;
        this.register = Registers.NONE;
        this.size = LocationSize.NULL;
    }
    
    /**
     * Register constructor
     * @param type
     * @param register
     */
    public LocationDescriptor(Registers register) {
        this.register = register;
        
        this.type = LocationType.REGISTER;
        this.size = register.size;
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
        
        this.register = Registers.NONE;
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
