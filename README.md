# NotSoTiny
Simulator and toolchain for my NotSoTiny architecture. 

This project uses the AssemblerLib library of my own creation. It can be found [here](https://github.com/MechaFinch/AssemblerLib)

The name comes from why it was originally created, the idea was to implement it on a TinyFPGA (unfortunately that got delayed from shipping in March to September) and be designed for advanted techniques like out-of-order execution - this is one of the motivators behind the ---- design - hence it's capabilities are not-so-tiny. Hopefully.

## Project Structure
This repository contains three largely independent elements: The assembler, found in the `notsotiny.asm` package, the simulator, found in the `notsotiny.sim` package, and the UI, found in the `notsotiny.ui` package. The assembler and UI each contain `main` methods to be run as programs, while the simulator is used by the UI to run code. 
 
### The Assembler
The assembler will take an assembly source file and assemble it and its `%include` dependencies into .obj relocatable object files. This is a custom format which boils down to a bunch of metadata for where symbols are in the object code followed by said object code, and are loaded via AssemblerLib's relocator. Aside from enabling position independency the object files allow the debug tools in the UI to convert source file symbol names into addresses, which is very convenient.

### The Simulator
The simulator runs NST machine code, and that's it. It uses a memory management system that allows non-continuous addresses which makes for convenient simulation of MMIO and the like. The most imporant public methods `step` the simulator by having it execute a single instruction, and fire interrupts which tell the next step to run an interrupt instruction.

I plan to re-write the simulator in Rust mostly to see how the performance compares and to get better at Rust, and integrate via JNI, but if this can get fast enough it has the potential for testing code meant for real hardware (if the FPGA ever ships). The current Java implementation can run at upwards of 10 MHz, which is already quite good.

### The UI
The user interface has two primary purposes, a screen and debugging tools. It has systems for running the simulation at a fixed rate or in halted bursts (running as fast as possible until MMIO calls for a halt (this might change to a halt instruction who knows)), and the debug tools include processor state, memwatch, and breakpoints. Also included is interrupt-driven keyboard input and timer.

## Projects using this
This section talks about things made using this suite.

### 500 Dice
500 Dice in the Vacuum of Space is a tiny game made for the GMTK Game Jam 2022. Written entirely in NST assembly in 48 hours, it can be downloaded from its [itch page](https://mechafinch.itch.io/500-dice) and its source code is in [this repo](https://github.com/MechaFinch/gmtk-jam-2022).
