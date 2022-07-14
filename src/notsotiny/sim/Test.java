package notsotiny.sim;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import asmlib.util.relocation.ExecLoader;
import asmlib.util.relocation.RelocatableObject;
import asmlib.util.relocation.Relocator;
import notsotiny.asm.Assembler;
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
        byte[] mem = new byte[0x1000];
        
        /*
        try(BufferedReader br = new BufferedReader(new FileReader(new File("wtf.txt")))) {
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
        */
        
        //String filename = "C:\\Users\\wetca\\Desktop\\silly  code\\architecture\\NotSoTiny\\programming\\snake\\snake.asm";
        //String filename = "calculator.asm";
        String filename = "primes_mincraf.asm";
        
        List<RelocatableObject> objects = Assembler.assemble(new File(filename), false);
        
        Relocator rel = new Relocator();
        objects.forEach(rel::add);
        
        int entry = ExecLoader.loadRelocator(rel, "primes_mincraf.main", mem, 1024, 1024);
        
        Halter halter = new Halter();
        
        MemoryManager mmu = new MemoryManager();
        mmu.registerSegment(new FlatMemoryController(mem), 0, mem.length);
        mmu.registerSegment(new CharacterIOMC(System.in, System.out), 0x8000, 16);
        //mmu.registerSegment(new FlatMemoryController(new byte[16]), 0x8000, 16);
        mmu.registerSegment(halter, 0x0000_FFFE, 2);
        
        // entry vector
        mmu.write4Bytes(0, entry);
        
        NotSoTinySimulator sim = new NotSoTinySimulator(mmu);
        sim.setRegSP(0x1000);
        
        try {
            runFast(sim, mem, 100_000_000, halter);
            //halter.clear();
            //runStepped(sim, mem, 1024, halter);
        } catch(Exception e) {
            e.printStackTrace();
        }
        
        System.out.println("\n");
        printState(sim, mem, new Disassembler());
    }
    
    public static double runFast(NotSoTinySimulator sim, byte[] mem, int maxInstructions, Halter halter) throws IOException {
        int executed = 0;
        long startTime = System.nanoTime();
        
        for(int i = 0; i < maxInstructions; i++) {
            sim.step();
            executed++;
            
            if(halter.halted()) {
                System.out.println("halted");
                break;
            }
            
            /*
            try {
                Thread.sleep(1);
            } catch(Exception e) {}
            */
        }
        
        long time = System.nanoTime() - startTime;
        double e = executed, t = time;
        
        double khz = e / (t / 1_000_000);
        
        System.out.println("Executed " + executed + " instructions in " + (time / 1_000_000) + "ms");
        System.out.printf("Average %.3f khz\n", khz);
        return khz;
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
        for(int j = 0x0FE0; j < 0x1000; j += 2) {
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
