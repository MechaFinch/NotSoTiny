module NotSoTiny {
    requires transitive javafx.controls;
    requires transitive AssemblerLib;
    requires javafx.graphics;
    requires java.logging;
    requires java.desktop;
    
    exports notsotiny.asm;
    exports notsotiny.sim;
    exports notsotiny.sim.memory;
    exports notsotiny.sim.ops;
    exports notsotiny.ui;
}