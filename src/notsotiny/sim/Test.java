package notsotiny.sim;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import notsotiny.asm.Disassembler;
import notsotiny.sim.memory.CharacterIOMC;
import notsotiny.sim.memory.FlatMemoryController;
import notsotiny.sim.memory.Halter;
import notsotiny.sim.memory.MemoryManager;

/**
 * test that sim
 * @author Mechafinch
 */
public class Test {
    
    public static void main(String[] args) throws IOException {
        byte[] mem = new byte[0x02FF];
        int[] prog = new int[] {
                       0x01, 0x58, 0x22, 0x00, 0x00, 0x00,
                       0x03, 0x70, 0x00, 0x80,
                       0x04, 0x00, 0x00,
                       0x10, 0xC2, 0x1C,
                       0x58,
                       0x10, 0xC6, 0x37,
                       0x10, 0xCF, 0x37, 0x04,
                       0xDD, 0x00,
                       0xEE, 0xF1,
                       0x10, 0xCD, 0xFF, 0xFF, 0x00, 0x00,
                       0x48, 0x65, 0x6C, 0x6C, 0x6F, 0x2C, 0x20, 0x57, 0x6F, 0x72, 0x6C, 0x64, 0x21, 0x00
               };
        
        for(int i = 0; i < prog.length; i++) {
            mem[i] = (byte) prog[i];
        }
        
        Halter halter = new Halter();
        
        MemoryManager mmu = new MemoryManager();
        mmu.registerSegment(new FlatMemoryController(mem), 0, mem.length);
        mmu.registerSegment(new CharacterIOMC(System.in, System.out), 0x8000, 16);
        mmu.registerSegment(halter, 0x0000_FFFF, 1);
        
        int entry = 0;
        
        NotSoTinySimulator sim = new NotSoTinySimulator(mmu, entry);
        
        runStepped(sim, mem, 20, halter);
        //runFast(sim, mem, 128, halter);
    }
    
    public static void runFast(NotSoTinySimulator sim, byte[] mem, int maxInstructions, Halter halter) throws IOException {
        for(int i = 0; i < maxInstructions; i++) {
            sim.step();
            
            if(halter.halted()) break;
        }
    }
    
    public static void runStepped(NotSoTinySimulator sim, byte[] mem, int maxInstructions, Halter halter) throws IOException {
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        Disassembler dis = new Disassembler();
        
        for(int i = 0; i < maxInstructions; i++) {
            System.out.println(String.format("A    B    C    D%n%04X %04X %04X %04X%nI    J    F%n%04X %04X %04X%nip       bp       sp%n%08X %08X %08X",
                                             sim.getRegA(), sim.getRegB(), sim.getRegC(), sim.getRegD(),
                                             sim.getRegI(), sim.getRegJ(), sim.getRegF(),
                                             sim.getRegIP(), sim.getRegBP(), sim.getRegSP()));
            
            /*
            for(int j = 0x00F0; j < 0x0100; j += 2) {
                System.out.println(String.format("%08X: %02X%02X", j, mem[j + 1], mem[j]));
            }
            */
            
            System.out.println(dis.disassemble(mem, sim.getRegIP()));
            
            stdin.readLine();
            
            System.out.println("\n");
            sim.step();
            System.out.println("\n");
            
            if(halter.halted()) {
                System.out.println("halted");
                break;
            }
        }
    }
}
