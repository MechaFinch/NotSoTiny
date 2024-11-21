package notsotiny.sim;

public enum LocationSize {
    DWORD   (4),
    WORD    (2),
    BYTE    (1),
    NULL    (0);
    
    public int bytes;
    
    private LocationSize(int bytes) {
        this.bytes = bytes;
    }
}
