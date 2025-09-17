package notsotiny.sim.ops;

/**
 * Groups opcodes by their decode properties
 */
public enum DecodingGroup {
    NODECODE                (false, false, false, false, false, false, false),
    RIM_NORMAL              (true, false, true, false, true, false, false),
    RIM_PACKED              (true, false, true, false, true, false, true),
    RIM_WOD                 (true, false, true, false, true, false, false),
    RIM_WOD_EI8             (true, false, true, false, true, true, false),
    RIM_WIDEDST             (true, true, true, false, true, false, false),
    RIM_WIDEDST_WOD         (true, true, true, false, true, false, false),
    RIM_PACKED_EI8          (true, false, true, false, true, true, true),
    RIM_PACKED_WIDEDST      (true, true, true, false, true, false, true),
    RIM_WIDE                (true, true, true, true, true, false, false),
    RIM_WIDE_WOD            (true, true, true, true, true, false, false),
    RIM_WIDE_WOD_EI8        (true, true, true, true, true, true, false),
    RIM_WIDE_DO_WOD         (true, true, false, false, true, false, false),
    RIM_SO                  (false, false, true, false, true, false, false),
    RIM_SO_EI8              (false, false, true, false, true, true, false),
    RIM_WIDE_SO             (false, false, true, true, true, false, false),
    RIM_DO                  (true, false, false, false, true, false, false),
    RIM_WIDE_DO             (true, true, false, false, true, false, false),
    RIM_WIDE_DO_EI8         (true, true, false, false, true, true, false),
    RIM_PACKED_DO           (true, false, false, false, true, false, true),
    RIM_DO_EI8              (true, false, false, false, true, true, false),
    RIM_DO_WOD              (true, false, false, false, true, false, false),
    RIM_LEA                 (true, true, true, false, true, false, false),
    RIM_RS_SO_EI8           (false, false, true, false, true, true, false),
    RIM_R32S_WOD            (true, false, true, true, true, false, false),
    RIM_WIDE_RS_SO_EI8      (false, false, true, true, true, true, false),
    RIM_WIDE_R32S_WOD       (true, true, true, true, true, false, false),
    RIM_RD_DO_WOD_EI8       (true, false, false, false, true, true, false),
    RIM_R32D                (true, true, true, false, true, false, false),
    RIM_WIDE_RD_DO_WOD_EI8  (true, true, false, false, true, true, false),
    RIM_WIDE_R32D           (true, true, true, true, true, false, false),
    I8                      (false, false, true, false, false, false, false),
    I8_EI8                  (false, false, true, false, false, true, false),
    I16                     (false, false, true, false, false, false, false),
    I32                     (false, false, true, true, false, false, false),
    UNDEF                   (false, false, false, false, false, false, false),
    ;
    
    public final boolean hasDestination, destIsWide, hasSource, sourceIsWide, hasRIM, hasEI8, isPacked;
    
    private DecodingGroup(boolean hasDestination, boolean destIsWide, boolean hasSource, boolean sourceIsWide, boolean hasRIM, boolean hasEI8, boolean isPacked) {
        this.hasDestination = hasDestination;
        this.destIsWide = destIsWide;
        this.hasSource = hasSource;
        this.sourceIsWide = sourceIsWide;
        this.hasRIM = hasRIM;
        this.hasEI8 = hasEI8;
        this.isPacked = isPacked;
    }
}
