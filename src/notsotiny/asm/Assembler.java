package notsotiny.asm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.MissingResourceException;

import asmlib.lex.Lexer;
import asmlib.lex.symbols.Directive;
import asmlib.lex.symbols.Label;
import asmlib.lex.symbols.LineMarker;
import asmlib.lex.symbols.Mnemonic;
import asmlib.lex.symbols.Symbol;
import asmlib.token.Tokenizer;
import asmlib.util.relocation.RelocatableObject;
import asmlib.util.relocation.RelocatableObject.Endianness;
import asmlib.util.relocation.RenameableRelocatableObject;

/**
 * Assembler for NotSoTiny
 * 
 * @author Mechafinch
 */
public class Assembler {
    
    private static Lexer lexer;
    
    static {
        try {
            lexer = new Lexer(new File(Assembler.class.getResource("resources/reserved-words.txt").getFile()), "", true);
        } catch(IOException e) {
            throw new MissingResourceException(e.getMessage(), Assembler.class.getName(), "lexer reserved word file");
        }
    }
    
    /**
     * Main for running the assembler standalone
     * 
     * @param args
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
        if(args.length != 3 && args.length != 1) {
            System.out.println("Usage: Assembler <input file> [<executable file> <executable entry point>]");
            System.exit(0);
        }
        
        // from the VeryTinyAssembler
        List<RelocatableObject> objects = assemble(new File(args[0]));
        
        // write object files
        String directory = new File(args[0]).getAbsolutePath();
        directory = directory.substring(0, directory.lastIndexOf("\\")) + "\\";
        
        List<String> execContents = new ArrayList<>();
        
        for(RelocatableObject obj : objects) {
            String filename = obj.getName() + ".obj";
            execContents.add(filename);
            
            try(FileOutputStream fos = new FileOutputStream(directory + filename)){
                fos.write(obj.asObjectFile());
            }
        }
        
        // exec file
        if(args.length == 3) {
            try(PrintWriter execWriter = new PrintWriter(directory + args[1])) {
                // entry point
                execWriter.println("#entry " + args[2]);
                
                // object files
                execContents.forEach(execWriter::println);
            }
        }
    }
    
    /**
     * Assembles a file and its dependencies
     * 
     * @param f
     * @return
     * @throws IOException
     */
    public static List<RelocatableObject> assemble(File f) throws IOException {
        ArrayList<RenameableRelocatableObject> objects = new ArrayList<>();
        ArrayList<String> files = new ArrayList<>(); // dependencies
        files.add(f.getAbsolutePath());
        
        HashMap<String, File> libraryMap = new HashMap<>();
        
        String directory = f.getAbsolutePath();
        directory = directory.substring(0, directory.lastIndexOf("\\")) + "\\";
        
        // until all dependencies are dealt with
        for(int i = 0; i < files.size(); i++) {
            File file = new File(files.get(i));
            
            if(!file.isAbsolute()) {
                file = new File(directory + files.get(i));
            }
            
            // load or assemble
            RenameableRelocatableObject obj;
            
            if(file.getPath().endsWith(".obj")) {
                // load
                obj = new RenameableRelocatableObject(file, null);
            } else {
                // assemble
                List<Symbol> symbols;
                
                try(BufferedReader br = new BufferedReader(new FileReader(file))) {
                    symbols = lexer.lex(Tokenizer.tokenize(br.lines().toList()));
                }
                
                obj = assembleObject(symbols, files, file);
            }
            
            libraryMap.put(obj.getName(), file);
            objects.add(obj);
        }
        
        // unify library names
        for(String name : libraryMap.keySet()) {
            File file = libraryMap.get(name);
            
            for(RenameableRelocatableObject obj : objects) {
                obj.rename(file, name);
            }
        }
        
        return new ArrayList<RelocatableObject>(objects);
    }
    
    /**
     * Assembles a file while adding dependencies to the list
     * 
     * @param symbols
     * @param includedFiles
     * @param file
     * @return
     * @throws IOException
     */
    private static RenameableRelocatableObject assembleObject(List<Symbol> symbols, List<String> includedFiles, File file) throws IOException {
        int line = 0,       // line in file
            address = 0;    // current address
        
        String workingDirectory = file.getAbsolutePath();
        int libNameIndex = workingDirectory.lastIndexOf("\\");
        
        String libraryName = workingDirectory.substring(libNameIndex + 1, workingDirectory.lastIndexOf('.'));
        workingDirectory = workingDirectory.substring(0, libNameIndex) + "\\";
        
        ArrayList<Byte> objectCode = new ArrayList<>();
        
        // relocation info
        HashMap<String, Integer> outgoingReferences = new HashMap<>(),
                                 incomingReferenceWidths = new HashMap<>(),
                                 outgoingReferenceWidths = new HashMap<>();
        HashMap<String, List<Integer>> incomingReferences = new HashMap<>();
        HashMap<File, String> libraryNames = new HashMap<>();
        
        /*
         * Assembler Passes
         * 1. Initial parse pass        - Figure out what each instruction is, record labels.
         * 2. Relative jump width pass  - Determine whether relative jumps can hit their targets and update accordingly
         * 3. Label resolution pass     - Resolve the values of labels (excluding externals).
         * 4. Expression reduction pass - Reduce expressions to values. Error if an unresolved label is part of an expression.
         */
        
        // symbols to parse
        LinkedList<Symbol> symbolQueue = new LinkedList<>(symbols);
        
        while(symbolQueue.size() > 0) {
            Symbol s = symbolQueue.poll();
            
            // newline = update current line
            if(s instanceof LineMarker lm) {
                line = lm.lineNumber();
                continue;
            }
            
            // parse
            try {
                switch(s) {
                    case Directive d:
                        // TODO
                        break;
                        
                    case Label l:
                        // TODO
                        break;
                    
                    case Mnemonic m:
                        // TODO
                        break;
                        
                    default: // do nothing
                }
            } catch(IndexOutOfBoundsException e) { // these let us avoid checking that needed symbols exist
                throw new IllegalArgumentException("Missing symbol after " + s + " on line " + line);
            } catch(ClassCastException e) {
                throw new IllegalArgumentException("Invalid symbol after " + s + " on line " + line);
            }
        }
        
        // convert object code to array
        byte[] objectCodeArray = new byte[objectCode.size()];
        
        for(int i = 0; i < objectCodeArray.length; i++) {
            objectCodeArray[i] = objectCode.get(i);
        }
        
        return new RenameableRelocatableObject(Endianness.LITTLE, libraryName, 2, incomingReferences, outgoingReferences, incomingReferenceWidths, outgoingReferenceWidths, objectCodeArray, false, libraryNames);
    }
}
