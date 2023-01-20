# NotSoTiny
Simulator and toolchain for my NotSoTiny architecture. 

This project uses the AssemblerLib library of my own creation. It can be found [here](https://github.com/MechaFinch/AssemblerLib)

The name comes from why it was originally created, the idea was to implement it on a TinyFPGA (unfortunately that never shipped) and be used to foray into advanced architectural techniques, hence it's capabilities are not-so-tiny. Hopefully.

## Project Structure
This repository contains three largely independent elements: The assembler, found in the `notsotiny.asm` package, the simulator, found in the `notsotiny.sim` package, and the UI, found in the `notsotiny.ui` package. The assembler and UI each contain `main` methods to be run as programs, while the simulator is used by the UI to run code. 

### The Docs
The /docs/ folder contains the documentation of the NST architecture. It's quite bare-bones but it's all the necessary information. The isa file describes the registers, instruction encoding, and notes about behaviors that might be ambiguous. The instruction listing file contains a listing of every instruction by opcode, including some notes such as aliases and whether single-argument instructions use the source or destination of the RIM encoding.

### The Assembler
The assembler will take an assembly source file and assemble it and its `%include` dependencies into .obj relocatable object files. This is a custom format which boils down to a bunch of metadata for where symbols are in the object code followed by said object code, and are loaded via AssemblerLib's relocator. Aside from enabling position independency the object files allow the debug tools in the UI to convert symbol names into addresses, which is very convenient.

If enabled, the assembler will attempt to encode instructions as compactly as possible. The algorithm is about as simple as it gets, "make things shorter until that doesn't let anything else get shorter." The parts affected by other instructions are usually relative jumps. This is done rather inefficiently as all instructions are re-encoded for every iteration, and can be disabled in an individual file with the `%nlo` (no length optimization) directive.

### The Simulator
The simulator runs NST machine code, and that's it. It uses a memory management system that allows non-continuous addresses which makes for convenient simulation of MMIO and the like. The most imporant public methods `step` the simulator by having it execute a single instruction, and fire interrupts which tell the next step to run an interrupt instruction.

I plan to re-write the simulator in Rust mostly to see how the performance compares and to get better at Rust, and integrate via JNI, but if this can get fast enough it has the potential for testing code meant for real hardware (if the FPGA ever ships). The current Java implementation can run at upwards of 10 MHz, which is already more than good enough though.

### The UI
The user interface has two primary purposes, a screen and debugging tools. It can run the simulation constantly and/or until halted, and the debug tools include processor state, memwatch, and breakpoints. Also included is interrupt-driven keyboard input and a timer.

## Plans for the Architecture
This section will be for rambling about potential changes to make the ISA better.

In implementing the plans that used to be here, the ISA has been revised to add index registers K and L, which take the places of BP and SP in the r16 RIM set, and their pair L:K replaces I:J in the r32 set. BIO was modified such that the `scale` field is consistent and includes a scale of 0 to exclude the index. This loses the `index << 3` scale, but seeing as it never saw use and is unlikely to, it's not much of a loss. Instead of the `scale` field indicating the offset width in the absense of an index, the `index` field is used for that and other purposes when `scale` is 00.

## Projects using this
This section talks about things made using this suite.

### 500 Dice
500 Dice in the Vacuum of Space is a tiny game made for the GMTK Game Jam 2022. Written entirely in NST assembly in 48 hours, it can be downloaded from its [itch page](https://mechafinch.itch.io/500-dice) and its source code is in [this repo](https://github.com/MechaFinch/gmtk-jam-2022).
