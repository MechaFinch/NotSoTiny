package notsotiny.sim;

public enum Register {
    NONE(LocationSize.NULL),
    DA  (LocationSize.DWORD),
    A   (LocationSize.WORD),
    AH  (LocationSize.BYTE),
    AL  (LocationSize.BYTE),
    B   (LocationSize.WORD),
    BH  (LocationSize.BYTE),
    BL  (LocationSize.BYTE),
    BC  (LocationSize.DWORD),
    C   (LocationSize.WORD),
    CH  (LocationSize.BYTE),
    CL  (LocationSize.BYTE),
    D   (LocationSize.WORD),
    DH  (LocationSize.BYTE),
    DL  (LocationSize.BYTE),
    JI  (LocationSize.DWORD),
    I   (LocationSize.WORD),
    J   (LocationSize.WORD),
    LK  (LocationSize.DWORD),
    K   (LocationSize.WORD),
    L   (LocationSize.WORD),
    XP  (LocationSize.DWORD),
    YP  (LocationSize.DWORD),
    BP  (LocationSize.DWORD),
    SP  (LocationSize.DWORD),
    IP  (LocationSize.DWORD),
    F   (LocationSize.WORD),
    PF  (LocationSize.WORD),
    ISP (LocationSize.DWORD),
    ;
    
    private final LocationSize size;
    private final int bytes;
    
    private Register(LocationSize size) {
        this.size = size;
        this.bytes = this.size.bytes;
    }
    
    public int size() { return this.bytes; }
    public LocationSize lsize() { return this.size; }
    
    /**
     * String -> register
     * @param s
     * @return
     */
    public static Register fromString(String s) {
        String su = s.toUpperCase();
        
        return switch(su) {
            case "D:A"  -> Register.DA;
            case "B:C"  -> Register.BC;
            case "J:I"  -> Register.JI;
            case "L:K"  -> Register.LK;
            default     -> Register.valueOf(su);
        };
    }
    
    @Override
    public String toString() {
        return switch(this) {
            case DA         -> "D:A";
            case BC         -> "B:C";
            case JI         -> "J:I";
            case LK         -> "L:K";
            default         -> super.toString();
        };
    }
}
