package notsotiny.sim.memory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * A MemoryController that implements character IO.
 * Values go in address 0-3
 * Read from address 4 to read a character into address 0
 * Read from address 5 to read an integer into address 0
 * Write to address 4 to write the character in address 0
 * Write to address 5 to write the integer in address 0
 * 
 * Upon an error during read/write, address 0-3 is set to -1
 * 
 * @author Mechafinch
 */
public class CharacterIOMC implements MemoryController {
    
    private int val;
    
    private BufferedReader reader;
    
    private PrintStream writer;
    
    public CharacterIOMC(InputStream in, OutputStream out) {
        this.val = 0;
        
        this.reader = new BufferedReader(new InputStreamReader(in));
        this.writer = new PrintStream(out);
    }

    @Override
    public byte readByte(int address) {
        // read in
        try {
            if(address == 4) { // char
                this.val = this.reader.read();
            } else if(address == 5) { // int
                String s = "";
                int c = '_';
                
                while(!Character.isWhitespace(c) && c != -1) {
                    c = this.reader.read();
                    s += (char) c;
                }
                
                this.val = Integer.parseInt(s);
            }
        } catch(IOException e) {
            this.val = -1;
        }
        
        // val reads
        return switch(address) {
            case 0  -> (byte) val;
            case 1  -> (byte) (val >> 8);
            case 2  -> (byte) (val >> 16);
            case 3  -> (byte) (val >> 24);
            default -> 0;
        };
    }

    @Override
    public void writeByte(int address, byte value) {
        // write
        if(address == 4) { // char
            //System.out.println("IOMC Value: " + Integer.toHexString(this.val));
            this.writer.print((char) this.val);
        } else if(address == 5) { // int
            this.writer.println(this.val);
        } else {
            // val writes
            this.val = switch(address) {
                case 0  -> (this.val & 0xFFFF_FF00) | (value & 0xFF);
                case 1  -> (this.val & 0xFFFF_00FF) | ((value & 0xFF) << 8);
                case 2  -> (this.val & 0xFF00_FFFF) | ((value & 0xFF) << 16);
                case 3  -> (this.val & 0x00FF_FFFF) | ((value & 0xFF) << 24);
                default -> -1;
            };
        }
    }
}
