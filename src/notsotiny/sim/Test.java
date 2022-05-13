package notsotiny.sim;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import notsotiny.asm.Disassembler;

/**
 * test that sim
 * @author Mechafinch
 */
public class Test {
    
    public static void main(String[] args) throws IOException {
        byte[] mem = new byte[0x02FF];
        int[] prog = new int[] {
                       0x06, 0x00, 0x01, 0x00, 0x00,
                       0x43, 0x40, 0x34, 0x12,
                       0x43, 0x40, 0x78, 0x56,
                       0x43, 0x40, 0xBC, 0x9A,
                       0x43, 0x40, 0xF0, 0xDE,
                       0x44,
                       0x45,
                       0x46,
                       0x47,
                       0x3A,
                       0x3B,
                       0x3C,
                       0x3D,
                       0x48,
                       0x49,
                       0x3C,
                       0x3D,
                       0x4A,
                       0x4B
               };
        
        for(int i = 0; i < prog.length; i++) {
            mem[i] = (byte) prog[i];
        }
        
        int entry = 0;
        
        Disassembler dis = new Disassembler();
        
        NotSoTinySimulator sim = new NotSoTinySimulator(mem, entry);
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        
        for(int i = 0; i < 20; i++) {
            System.out.println(String.format("A    B    C    D%n%04X %04X %04X %04X%nI    J    F%n%04X %04X %04X%nip       bp       sp%n%08X %08X %08X",
                                             sim.getRegA(), sim.getRegB(), sim.getRegC(), sim.getRegD(),
                                             sim.getRegI(), sim.getRegJ(), sim.getRegF(),
                                             sim.getRegIP(), sim.getRegBP(), sim.getRegSP()));
            
            for(int j = 0x00F0; j < 0x0100; j += 2) {
                System.out.println(String.format("%08X: %02X%02X", j, mem[j + 1], mem[j]));
            }
            System.out.println(dis.disassemble(mem, sim.getRegIP()));
            
            stdin.readLine();
            
            System.out.println("\n");
            sim.step();
            System.out.println("\n");
        }
    }
}
