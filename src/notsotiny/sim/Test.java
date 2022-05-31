package notsotiny.sim;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

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
        byte[] mem = new byte[0x0800];
        
        try(BufferedReader br = new BufferedReader(new FileReader(new File("calculator_assembled.txt")))) {
            int i = 0;
            
            for(String ln : br.lines().toList()) {
                if(ln.contains(":")) {
                    System.out.println(ln);
                    int a = ln.lastIndexOf(":\t") + 1,
                        b = ln.lastIndexOf('\t');
                    
                    String[] bytes;
                    
                    if(a == b) {
                        bytes = ln.substring(a).strip().split(" ");
                    } else {
                        bytes = ln.substring(a, b).strip().split(" ");
                    }
                    
                    for(int j = 0; j < bytes.length; j++) {
                        System.out.println(bytes[j]);
                        mem[i] = (byte) Integer.parseInt(bytes[j], 16);
                        
                        i++;
                    }
                }
            }
        }
        
        Halter halter = new Halter();
        
        MemoryManager mmu = new MemoryManager();
        mmu.registerSegment(new FlatMemoryController(mem), 0, mem.length);
        mmu.registerSegment(new CharacterIOMC(System.in, System.out), 0x8000, 16);
        mmu.registerSegment(halter, 0x0000_FFFE, 2);
        
        int entry = 0;
        
        NotSoTinySimulator sim = new NotSoTinySimulator(mmu, entry);
        sim.setRegSP(0x0800);
        
        try {
            //runStepped(sim, mem, 1024, halter);
            runFast(sim, mem, 2048, halter);
        } catch(Exception e) {
            e.printStackTrace();
        }
        
        System.out.println("\n");
        printState(sim, mem, new Disassembler());
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
            printState(sim, mem, dis);
            
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
    
    public static void printState(NotSoTinySimulator sim, byte[] mem, Disassembler dis) {
        for(int j = 0x07F0; j < 0x0800; j += 2) {
            System.out.println(String.format("%08X: %02X%02X", j, mem[j + 1], mem[j]));
        }
        
        System.out.println();
        
        System.out.println(String.format("A    B    C    D%n%04X %04X %04X %04X%nI    J    F%n%04X %04X %04X%nip       bp       sp%n%08X %08X %08X",
                                         sim.getRegA(), sim.getRegB(), sim.getRegC(), sim.getRegD(),
                                         sim.getRegI(), sim.getRegJ(), sim.getRegF(),
                                         sim.getRegIP(), sim.getRegBP(), sim.getRegSP()));
        
        System.out.println(dis.disassemble(mem, sim.getRegIP()));
        
        for(int j = 0; j < dis.getLastInstructionLength(); j++) {
            System.out.print(String.format("%02X ", mem[sim.getRegIP() + j]));
        }
        
        System.out.println();
    }
}
