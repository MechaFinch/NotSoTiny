package notsotiny.sim.ops;

/**
 * Groups opcodes by their decode properties
 */
public enum DecodingGroup {
    NODECODE            (),
    RIM_NORMAL          (),
    RIM_PACKED          (),
    RIM_WOD             (),
    RIM_WOD_EI8         (),
    RIM_WIDEDST         (),
    RIM_WIDEDST_WOD     (),
    RIM_PACKED_EI8      (),
    RIM_PACKED_WIDEDST  (),
    RIM_WIDE            (),
    RIM_WIDE_WOD        (),
    RIM_SO              (),
    RIM_WIDE_SO         (),
    RIM_DO              (),
    RIM_PACKED_DO       (),
    RIM_DO_EI8          (),
    RIM_DO_WOD          (),
    RIM_WIDEDST_DO_WOD  (),
    RIM_LEA             (),
    BI_SRC              (),
    BI_DST              (),
    BIO_SRC             (),
    BIO_DST             (),
    O_SRC               (),
    O_DST               (),
    I8                  (),
    I16                 (),
    I32                 (),
    UNDEF               (),
    ;
    
    private DecodingGroup() {
    }
}
