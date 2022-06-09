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
import java.util.Map;
import java.util.MissingResourceException;

import asmlib.lex.Lexer;
import asmlib.lex.symbols.DirectiveSymbol;
import asmlib.lex.symbols.LabelSymbol;
import asmlib.lex.symbols.LineMarkerSymbol;
import asmlib.lex.symbols.MnemonicSymbol;
import asmlib.lex.symbols.NameSymbol;
import asmlib.lex.symbols.Symbol;
import asmlib.token.Tokenizer;
import asmlib.util.relocation.RelocatableObject;
import asmlib.util.relocation.RelocatableObject.Endianness;
import notsotiny.asm.components.Component;
import notsotiny.asm.components.Instruction;
import notsotiny.asm.resolution.ResolvableLocationDescriptor;
import notsotiny.asm.resolution.ResolvableLocationDescriptor.LocationType;
import notsotiny.sim.ops.Opcode;
import notsotiny.sim.ops.Operation;
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
            lexer = new Lexer(new File(Assembler.class.getResource("resources/reserved_words.txt").getFile()), "", true);
        } catch(IOException | NullPointerException e) {
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
        // we'll need this later
        HashMap<String, Integer> outgoingReferences = new HashMap<>(),
                                 incomingReferenceWidths = new HashMap<>(),
                                 outgoingReferenceWidths = new HashMap<>();
        HashMap<String, List<Integer>> incomingReferences = new HashMap<>();
        HashMap<File, String> libraryNames = new HashMap<>();
        
        /*
         * Assembler Passes
         * 1. Definitions pass          - Parse %define statements and apply them (handled by AssemblerLib)
         * 2. Main parse pass           - Figure out what each instruction is, record labels.
         * 3. Jump distinction pass     - Jumps to internal references are relative, jumps to external references are absolute 
         * 4. Relative jump width pass  - Determine whether relative jumps can hit their targets and update accordingly. Continues until all jumps are stable.
         * 5. Expression reduction pass - Reduce expressions to values. Error if an unresolved label is part of an expression.
         */
        
        /*
         * Relative jump width pass
         * All relative jumps are initialized to the maximum width. Jumps that can reach their target
         * with a shorter width are shortened, and this repeats until all jumps are their minimum width
         */
        
        /*
         * MAIN PARSE PASS
         */
        
        // symbols to parse
        LinkedList<Symbol> symbolQueue = new LinkedList<>(symbols);
        List<Component> allInstructions = new LinkedList<>(),         // every instruction parsed
                        unresolvedInstructions = new LinkedList<>();  // instructions with an unresolved component
        
        Map<String, Integer> labelIndexMap = new HashMap<>();   // label name -> allInstructions index. Points to the start of the Component at that address
        
        while(symbolQueue.size() > 0) {
            Symbol s = symbolQueue.poll();
            
            // newline = update current line
            if(s instanceof LineMarkerSymbol lm) {
                line = lm.lineNumber();
                continue;
            }
            
            System.out.println("Assembling from symbol: " + s);
            
            // parse
            try {
                switch(s) {
                    case DirectiveSymbol d:
                        // TODO
                        break;
                        
                        // explicit label
                        // add it to the map
                    case LabelSymbol l:
                        labelIndexMap.put(l.name(), allInstructions.size());
                        System.out.println("Added label " + l.name() + " at index " + allInstructions.size());
                        break;
                        
                        // implicit label
                    case NameSymbol n:
                        labelIndexMap.put(n.name(), allInstructions.size());
                        System.out.println("Added label " + n.name() + " at index " + allInstructions.size());
                        break;
                    
                    case MnemonicSymbol m:
                        // TODO
                        Instruction inst = parseInstruction(symbolQueue, m);
                        
                        if(inst == null) {
                            // placeholder
                        } else {
                            allInstructions.add(inst);
                            if(!inst.isResolved()) unresolvedInstructions.add(inst);
                        }
                        break;
                        
                    default: // do nothing
                        System.out.println("Unknown construct start: " + s + " on line " + line);
                }
            } catch(IndexOutOfBoundsException e) { // these let us avoid checking that needed symbols exist
                throw new IllegalArgumentException("Missing symbol after " + s + " on line " + line);
            } catch(ClassCastException e) {
                throw new IllegalArgumentException("Invalid symbol after " + s + " on line " + line);
            }
        }
        
        System.out.println(allInstructions);
        System.out.println(unresolvedInstructions);
        System.out.println(labelIndexMap);
        
        // convert object code to array
        byte[] objectCodeArray = new byte[objectCode.size()];
        
        for(int i = 0; i < objectCodeArray.length; i++) {
            objectCodeArray[i] = objectCode.get(i);
        }
        
        return new RenameableRelocatableObject(Endianness.LITTLE, libraryName, 2, incomingReferences, outgoingReferences, incomingReferenceWidths, outgoingReferenceWidths, objectCodeArray, false, libraryNames);
    }

    /**
     * Parse an instruction
     * 
     * @param symbolQueue
     * @return
     */
    private static Instruction parseInstruction(LinkedList<Symbol> symbolQueue, MnemonicSymbol m) {
        // convert to Operation for argument count
        Operation opr;
        
        try {
            opr = Operation.valueOf(m.name());
        } catch(IllegalArgumentException e) { // JCC doesn't match mnemonic
            switch(m.name()) {
                case "JC", "JNC", "JS", "JNS", "JO", "JNO", "JZ", "JNZ", "JE", "JNE",
                     "JA", "JNA", "JAE", "JNAE", "JB", "JNB", "JBE", "JNBE",
                     "JG", "JNG", "JGE", "JNGE", "JL", "JNL", "JLE", "JNLE":
                         opr = Operation.JCC;
                break;
                
                default:
                    throw new IllegalArgumentException("Invalid mnemonic " + m);
            }
        }
        
        // how many arguments does this mnemonic have
        if(!hasFirstOperand(opr)) {
            // no arguments, ez
            return switch(opr) {
                case NOP    -> new Instruction(Opcode.NOP);
                case RET    -> new Instruction(Opcode.RET);
                case IRET   -> new Instruction(Opcode.IRET);
                default     -> null; // not possible
            };
        } else {
            // parse first operand
            ResolvableLocationDescriptor firstOperand = parseOperand(true);
            
            if(!hasSecondOperand(opr)) {
                // 1 argument.
                // PUSH, JMP, JCC, CALL, and INT are all source only while the others are destination only
                Opcode opcode;
                
                // everything has its own rules
                switch(opr) {
                        // register shortcuts + F
                    case PUSH, POP:
                        // TODO
                        break;
                    
                        // I/J shortcuts
                    case INC, ICC, DEC, DCC:
                        // TODO
                        break;
                    
                        // 8, 16, 32 bit immediates
                    case JMP:
                        // TODO
                        break;
                        
                        // 16 bit immediates
                    case CALL:
                        // TODO
                        break;
                        
                        // 32 bit immediates & 32 bit rim
                    case JMPA, CALLA:
                        // TODO
                        break;
                    
                        // 8 bit immediates & aliases
                    case JCC:
                        // TODO
                        break;
                    
                        // packed rim
                    case PINC, PICC, PDEC, PDCC:
                        // TODO
                        break;
                    
                        // rim + F
                    case NOT:
                        // TODO
                        break;
                    
                        // rim only
                    case NEG:
                        // TODO
                        break;
                    
                    default: // error
                        throw new IllegalArgumentException("how did we get here? " + m);
                }
                
                // nice and generic-ish
                return switch(opr) {
                    case PUSH, JMP, JMPA, JCC, CALL, CALLA, INT -> new Instruction(opcode, firstOperand, false);
                    default                                     -> new Instruction(opcode, firstOperand, true);
                };
            } else {
                // parse second operand
                boolean canBeMemory = (firstOperand.getType() != LocationType.MEMORY) && (firstOperand.getType() != LocationType.IMMEDIATE);
                ResolvableLocationDescriptor secondOperand = parseOperand(canBeMemory);
                // TODO
            }
        }
        
        return null;
    }
    
    /**
     * Parse an operand
     * 
     * @return
     */
    private static ResolvableLocationDescriptor parseOperand(boolean canBeMemory) {
        // TODO
        return null;
    }
    
    /**
     * Determine if op has a first operand
     * 
     * @param op
     * @return true if op has a first operand
     */
    private static boolean hasFirstOperand(Operation op) {
        return switch(op) {
            case NOP, RET, IRET -> false;
            default             -> true;
        };
    }
    
    /**
     * Determine if op has a second operand. Only valid when op has a first operand.
     * 
     * @param op
     * @return true if op has a second operand
     */
    private static boolean hasSecondOperand(Operation op) {
        return switch(op) {
            case PUSH, POP, NOT, NEG, INC, ICC, PINC, PICC,
                 DEC, DCC, PDEC, PDCC, JMP, JMPA, CALL, CALLA,
                 INT, JCC  -> false;
                 
            default             -> true;
        };
    }
}
