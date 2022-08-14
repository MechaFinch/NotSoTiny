# NotSoTiny
Simulator and toolchain for my NotSoTiny architecture. 

This project uses the AssemblerLib library of my own creation. It can be found [here](https://github.com/MechaFinch/AssemblerLib)

The name comes from why it was originally created, the idea was to implement it on a TinyFPGA (unfortunately that got delayed from shipping in March to September) and be designed with advanced stuff like out-of-order execution in mind, hence it's capabilities are not-so-tiny. Hopefully.

## Project Structure
This repository contains three largely independent elements: The assembler, found in the `notsotiny.asm` package, the simulator, found in the `notsotiny.sim` package, and the UI, found in the `notsotiny.ui` package. The assembler and UI each contain `main` methods to be run as programs, while the simulator is used by the UI to run code. 

### The Docs
The /docs/ folder contains the documentation of the NST architecture. It's quite bare-bones but it's all the necessary information. The isa file describes the registers, instruction encoding, and notes about behaviors that might be ambiguous. The instruction listing file contains a listing of every instruction by opcode, including some notes such as aliases and whether single-argument instructions use the source or destination of the RIM encoding.

### The Assembler
The assembler will take an assembly source file and assemble it and its `%include` dependencies into .obj relocatable object files. This is a custom format which boils down to a bunch of metadata for where symbols are in the object code followed by said object code, and are loaded via AssemblerLib's relocator. Aside from enabling position independency the object files allow the debug tools in the UI to convert source file symbol names into addresses, which is very convenient.

### The Simulator
The simulator runs NST machine code, and that's it. It uses a memory management system that allows non-continuous addresses which makes for convenient simulation of MMIO and the like. The most imporant public methods `step` the simulator by having it execute a single instruction, and fire interrupts which tell the next step to run an interrupt instruction.

I plan to re-write the simulator in Rust mostly to see how the performance compares and to get better at Rust, and integrate via JNI, but if this can get fast enough it has the potential for testing code meant for real hardware (if the FPGA ever ships). The current Java implementation can run at upwards of 10 MHz, which is already quite good.

### The UI
The user interface has two primary purposes, a screen and debugging tools. It has systems for running the simulation at a fixed rate or in halted bursts (running as fast as possible until MMIO calls for a halt (this might change to a halt instruction who knows)), and the debug tools include processor state, memwatch, and breakpoints. Also included is interrupt-driven keyboard input and timer.

## Plans for the Architecture
This section will be for rambling about potential changes to make the ISA better. They're most likely to be implemented with the Rust simulator re-write.

NST is an ISA that has undergone considerable evolution during the development of these tools, and it's gone in a strange direction that mixes everything from 4 to 32 bit operations. It was supposed to be a fairly pure 16-bit architecture and the fancy packed operations had 8 bit slices for more 'practical' math and 4 bit slices primarily for BCD operations. The original product for this to be used in would've been a custom built nspire-esque calculator, and those packed-4s are useful for decimal arithmetic. As development has gone on, things have gotten slowly more 32-bit. The original address space was 16 bit, but that felt too limited for something that calls itself not-so-tiny so the addresses were extended to 32 bits. Thanks to RIM being designed with everything being 16 bits, address registers BP and SP are included in general operations and thus those would need 32 bit support. The various exceptions that come from this disjoint register size are both a pain to deal with and quite unelegant, so here are some plans to change them. The general idea is to make general RIM purely 8/16 bit to greatly simplify implementation and make the special-purpose registers BP and SP properly special-purpose.

### Changes
* Remove BP and SP from normal RIM, and add 2 index registers in their place. Names TBD, here will use K & L
* Replace the I:J pair with K:L
* Allow packed operations to use index registers - there wasn't much reason to exclude them beyond BP/SP also having the MSB set in the encoding.

Currently, the distinction between "general purpose" registers A B C D and "index" registers I J is that general purpose registers can be broken into H/L bytes and used in packed operations. With these changes, the packed distinction is replaced with less flexible pairs.

### Things to figure out
* What to do about BIO - the idea of adding 2 index registers is that it cleanly replaces BP and SP: 2 registers for the 2 replaced, 1 pair for when there's only 1 spot for a new pair.  However, the BIO encoding includes a special case for SP in order to allow having only a base or only an index. Having 2 more index registers means BP/SP can no longer be used as indices, leaving us without an empty index.
  * The first thing that comes to mind is an index already part of the base is ignored, but this doesn't allow for BP/SP without an index.
  * This is most likely a place that will disrupt the otherwise increased orthogonality

## Projects using this
This section talks about things made using this suite.

### 500 Dice
500 Dice in the Vacuum of Space is a tiny game made for the GMTK Game Jam 2022. Written entirely in NST assembly in 48 hours, it can be downloaded from its [itch page](https://mechafinch.itch.io/500-dice) and its source code is in [this repo](https://github.com/MechaFinch/gmtk-jam-2022).
