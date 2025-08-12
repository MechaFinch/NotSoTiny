package notsotiny.sim.ops;

/**
 * Groups opcodes by their decode properties
 */
public enum DecodingGroup {
    NODECODE                (),
    RIM_NORMAL              (),
    RIM_PACKED              (),
    RIM_WOD                 (),
    RIM_WOD_EI8             (),
    RIM_WIDEDST             (),
    RIM_WIDEDST_WOD         (),
    RIM_PACKED_EI8          (),
    RIM_PACKED_WIDEDST      (),
    RIM_WIDE                (),
    RIM_WIDE_WOD            (),
    RIM_WIDE_WOD_EI8        (),
    RIM_WIDE_DO_WOD         (),
    RIM_SO                  (),
    RIM_SO_EI8              (),
    RIM_WIDE_SO             (),
    RIM_DO                  (),
    RIM_WIDE_DO             (),
    RIM_WIDE_DO_EI8         (),
    RIM_PACKED_DO           (),
    RIM_DO_EI8              (),
    RIM_DO_WOD              (),
    RIM_LEA                 (),
    RIM_RS_SO_EI8           (),
    RIM_R32S_WOD            (),
    RIM_WIDE_RS_SO_EI8      (),
    RIM_WIDE_R32S_WOD       (),
    RIM_RD_DO_WOD_EI8       (),
    RIM_R32D                (),
    RIM_WIDE_RD_DO_WOD_EI8  (),
    RIM_WIDE_R32D           (),
    I8                      (),
    I8_EI8                  (),
    I16                     (),
    I32                     (),
    UNDEF                   (),
    ;
    
    private DecodingGroup() {
    }
}
