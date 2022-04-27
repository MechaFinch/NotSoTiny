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
        byte[] mem = new byte[0x02FF],
               prog = new byte[] {
                       0x0C, 0x69, 0x69,
                       0x0D, 0x20, 0x04,
                       0x1C,
                       0x20,
                       0x04, 0x00, 0x01,
                       0x05, 0x00, 0x02,
                       0x10, 0x46, 0x3C,
                       0x10, 0x5E, 0x3D,
                       0x10, 0x53, 0x3C, 0x00, 0x01, 0x00, 0x00,
                       0x10, 0x4A, 0x3D
               };
        
        for(int i = 0; i < prog.length; i++) {
            mem[i] = prog[i];
        }
        
        int entry = 0;
        
        Disassembler dis = new Disassembler();
        
        NotSoTinySimulator sim = new NotSoTinySimulator(mem, entry);
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        
        for(int i = 0; i < 12; i++) {
            System.out.println(String.format("A    B    C    D%n%04X %04X %04X %04X%nI    J    F%n%04X %04X %04X%nip       bp       sp%n%08X %08X %08X",
                                             sim.getRegA(), sim.getRegB(), sim.getRegC(), sim.getRegD(),
                                             sim.getRegI(), sim.getRegJ(), sim.getRegF(),
                                             sim.getRegIP(), sim.getRegBP(), sim.getRegSP()));
            
            System.out.println(String.format("00000100: %02X%02X%n00000200: %02X%02X", mem[0x0101], mem[0x0100], mem[0x0201], mem[0x0200]));
            System.out.println(dis.disassemble(mem, sim.getRegIP()));
            
            stdin.readLine();
            
            System.out.println("\n");
            sim.step();
            System.out.println("\n");
        }
    }
}
