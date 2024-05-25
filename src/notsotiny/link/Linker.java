package notsotiny.link;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.logging.Logger;

import asmlib.util.relocation.ExecLoader;
import asmlib.util.relocation.Relocator;
import notsotiny.asm.Disassembler;
import notsotiny.sim.memory.FlatMemoryController;
import notsotiny.sim.memory.MemoryManager;

/**
 * Linker for NotSoTiny
 * This program's main purpose is to go from relocatable object files to relocated data files.
 * 
 * @author Mechafinch
 */
public class Linker {
    
    private static Logger LOG = Logger.getLogger(Linker.class.getName());
    
    /**
     * Main for running standalone
     * 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        if(args.length < 1 || args.length > 7) {
            System.out.println("Usage: Linker [flags] <exec file> [<output file>]");
            System.out.println("Flags:");
            System.out.println("\t-s [size]  \tSize: Set a fixed output size in bytes (hexadecimal)");
            System.out.println("\t-o [origin]\tOrigin: Start address to be relocated to, in hexadecimal");
            System.out.println("\t-l         \tList: output a listing file");
            System.exit(0);
        }
        
        // parse flags
        int flagIndex = 0;
        
        int size = 0,
            origin = 0;
        boolean outputListing = false,
                truncate = true;
        
        out:
        while(true) {
            switch(args[flagIndex]) {
                case "-s":
                    truncate = false;
                    size = (int) Long.parseLong(args[flagIndex + 1], 16);
                    flagIndex += 2;
                    
                    LOG.fine(String.format("Buffer size set to &08X", size));
                    break;
                
                case "-o":
                    origin = (int) Long.parseLong(args[flagIndex + 1], 16);
                    flagIndex += 2;
                    
                    LOG.fine(String.format("Origin set to %08X", origin));
                    break;
                
                case "-l":
                    outputListing = true;
                    flagIndex += 1;
                    
                    LOG.fine("Listing file will be output");
                    break;
                
                default:
                    break out;
            }
        }
        
        String inputFileName = args[flagIndex];
        
        // load
        List<Object> relocatorPair = ExecLoader.loadExecFileToRelocator(new File(inputFileName));
        
        Relocator relocator = (Relocator) relocatorPair.get(0);
        String entrySymbol = (String) relocatorPair.get(1);
        
        byte[] data,
               relData = relocator.relocate(Integer.toUnsignedLong(origin));
        
        if(truncate) {
            data = relData;
        } else {
            data = new byte[size];
            System.arraycopy(relData, 0, data, 0, Math.min(data.length, relData.length));
            
            if(data.length < relData.length) { 
                LOG.warning("Output size smaller than relocated data size: %08X bytes specified, %08X bytes relocated");
            }
        }
        
        long entry = relocator.getReference(entrySymbol);
        
        LOG.finest(String.format("Entry symbol %s relocated to %08X", entrySymbol, entry));
        
        // write data to file
        String outputFileName = "";
        
        if(args.length > flagIndex + 1) {
            outputFileName = args[flagIndex + 1];
        } else {
            outputFileName = inputFileName.substring(0, inputFileName.lastIndexOf('.')) + ".dat";
        }
        
        LOG.fine("Writing to output file " + outputFileName);
        
        Files.write(new File(outputFileName).toPath(), data, StandardOpenOption.CREATE);
        
        // write listing file
        if(outputListing) {
            String listFileName = outputFileName.substring(0, outputFileName.lastIndexOf('.')) + ".lst";
            LOG.fine("Writing listing file " + listFileName);
            
            try(PrintWriter listWriter = new PrintWriter(listFileName)) {
                Disassembler dis = new Disassembler();
                MemoryManager mm = new MemoryManager();
                FlatMemoryController fmc = new FlatMemoryController(data);
                
                mm.registerSegment(fmc, 0, data.length);
                
                for(int address = 0; address < data.length;) {
                    try {
                    String dsm = dis.disassemble(mm, address);
                    int len = dis.getLastInstructionLength();
                    
                    String label = relocator.getAddressName(Integer.toUnsignedLong(address + origin));
                        
                    StringBuilder bytes = new StringBuilder();
                    
                    for(int i = 0; i < len; i++) {
                        bytes.append(String.format("%02X ", data[address + i]));
                    }
                    
                    String listString = String.format("%-32s%08X: %-24s %s", label, address + origin, bytes, dsm);
                    listWriter.println(listString);
                    LOG.finer(listString);
                    
                    address += len;
                    } catch(IndexOutOfBoundsException e) {
                        // ignore end
                    }
                }
            }
        }
    }
}
