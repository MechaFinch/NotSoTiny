package notsotiny.link;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import asmlib.util.relocation.ExecLoader;
import asmlib.util.relocation.Relocator;

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
        if(args.length < 1 || args.length > 6) {
            System.out.println("Usage: Linker [flags] <relocator buffer size (hex)> <exec file> [<output file>]");
            System.out.println("Flags:");
            System.out.println("\t-o\t\tOrigin: Start address to be relocated to, in hexadecimal");
            System.out.println("\t-s [names]\tSymbols: Comma-separated list of symbols to output to a listing file");
            System.exit(0);
        }
        
        // parse flags
        int flagIndex = 0;
        
        int origin = 0;
        List<String> symbols = new ArrayList<>();
        
        out:
        while(true) {
            switch(args[flagIndex]) {
                case "-o":
                    origin = (int) Long.parseLong(args[flagIndex + 1], 16);
                    flagIndex += 2;
                    break;
                
                case "-s":
                    symbols.addAll(Arrays.asList(args[flagIndex + 1].split(",")));
                    flagIndex += 2;
                    break;
                
                default:
                    break out;
            }
        }
        
        byte[] data = new byte[Integer.parseInt(args[flagIndex + 0], 16)];
        String inputFileName = args[flagIndex + 1];
        
        // load
        List<Object> relocatorPair = ExecLoader.loadExecFileToRelocator(new File(inputFileName));
        
        Relocator relocator = (Relocator) relocatorPair.get(0);
        String entrySymbol = (String) relocatorPair.get(1);
        
        int entry = ExecLoader.loadRelocator(relocator, entrySymbol, data, (int) origin, 0);
        
        // write data to file
        String outputFileName = "";
        
        if(args.length > flagIndex + 2) {
            outputFileName = args[flagIndex + 2];
        } else {
            outputFileName = inputFileName.substring(0, inputFileName.lastIndexOf('.')) + ".dat";
        }
        
        Files.write(new File(outputFileName).toPath(), data, StandardOpenOption.CREATE);
        
        // write requested symbols
        if(symbols.size() > 0) {
            String listFileName = outputFileName.substring(0, outputFileName.lastIndexOf('.')) + ".lst";
            
            try(PrintWriter listWriter = new PrintWriter(listFileName)) {
                listWriter.println("SYMBOL LISTING");
                
                int maxSymbolLength = Collections.max(symbols, (a, b) -> (a.length() - b.length())).length();
                
                listWriter.println(String.format("%" + maxSymbolLength + "s  %08X", "ENTRY", entry));
                
                for(String symbol : symbols) {
                    listWriter.println(String.format("%" + maxSymbolLength + "s  %08X", symbol, relocator.getReference(symbol)));
                }
            }
        }
    }
}
