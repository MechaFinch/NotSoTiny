package notsotiny.sim;

public enum Registers {
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
    ;
    
    public LocationSize size;
    
    private Registers(LocationSize size) {
        this.size = size;
    }
}
