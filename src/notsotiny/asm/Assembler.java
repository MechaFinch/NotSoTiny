package notsotiny.asm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import asmlib.lex.Lexer;
import asmlib.lex.symbols.ConstantSymbol;
import asmlib.lex.symbols.DirectiveSymbol;
import asmlib.lex.symbols.ExpressionSymbol;
import asmlib.lex.symbols.LabelSymbol;
import asmlib.lex.symbols.LineMarkerSymbol;
import asmlib.lex.symbols.MemorySymbol;
import asmlib.lex.symbols.MnemonicSymbol;
import asmlib.lex.symbols.NameSymbol;
import asmlib.lex.symbols.RegisterSymbol;
import asmlib.lex.symbols.SeparatorSymbol;
import asmlib.lex.symbols.SizeSymbol;
import asmlib.lex.symbols.SpecialCharacterSymbol;
import asmlib.lex.symbols.StringSymbol;
import asmlib.lex.symbols.Symbol;
import asmlib.token.Tokenizer;
import asmlib.util.FileLocator;
import asmlib.util.relocation.RelocatableObject;
import asmlib.util.relocation.RelocatableObject.Endianness;
import notsotiny.asm.components.Component;
import notsotiny.asm.components.InitializedData;
import notsotiny.asm.components.Instruction;
import notsotiny.asm.components.Repetition;
import notsotiny.asm.components.UninitializedData;
import notsotiny.asm.resolution.ResolvableConstant;
import notsotiny.asm.resolution.ResolvableExpression;
import notsotiny.asm.resolution.ResolvableLocationDescriptor;
import notsotiny.asm.resolution.ResolvableLocationDescriptor.LocationType;
import notsotiny.asm.resolution.ResolvableMemory;
import notsotiny.asm.resolution.ResolvableValue;
import notsotiny.sim.ops.Opcode;
import notsotiny.sim.ops.Operation;
import asmlib.util.relocation.RenameableRelocatableObject;

/**
 * Assembler for NotSoTiny
 * 
 * @author Mechafinch
 */
public class Assembler {
    
    private static Logger LOG = Logger.getLogger(Assembler.class.getName());
    
    public static final Lexer lexer;
    
    static {
        try {
            lexer = new Lexer(Assembler.class.getResourceAsStream("resources/reserved_words.txt"), "", true);
        } catch(IOException | NullPointerException e) {
            throw new MissingResourceException(e.getMessage(), Assembler.class.getName(), "lexer reserved word file");
        }
    }
    
    /**
     * Mutable string
     */
    private static class StringContainer {
        public String s;
        
        public StringContainer(String s) {
            this.s = s;
        }
    }
    
    /**
     * A record with all the information needed to assemble an object
     * 
     * @param allInstructions List<Component>
     * @param labelIndexMap Map<String, Integer>
     * @param libraryName String
     * @param libraryFilesMap HashMap<File, String>
     * @param incomingReferences HahsMap<String, List<Integer>>
     * @param outgoingReferences HashMap<String, Integer>
     * @param incomingReferenceWidths HashMap<String, Integer>
     * @param outgoingReferenceWidths HashMap<String, Integer>
     * @author Mechafinch
     */
    public record AssemblyObject(List<Component> allInstructions, Map<String, Integer> labelIndexMap, String libraryName, HashMap<File, String> libraryFilesMap, HashMap<String, List<Integer>> incomingReferences, HashMap<String, Integer> outgoingReferences, HashMap<String, Integer> incomingReferenceWidths, HashMap<String, Integer> outgoingReferenceWidths) {
        
        private class Printer {
            
            // I should just refactor the printer util from the compiler but uhhh fuck you
            
            private OutputStream stream;
            
            /**
             * @param stream
             */
            Printer(OutputStream stream) {
                this.stream = stream;
            }
            
            public void print(String s) throws IOException {
                this.stream.write(s.getBytes());
            }
            
            public void println(String s) throws IOException {
                this.print(s);
                this.stream.write((byte) '\n');
            }
            
        }
        
        public void print(OutputStream stream) throws IOException {
            Printer p = new Printer(stream);
            
            for(int i = 0; i < this.allInstructions.size(); i++) {
                // If this index is labeled, print relevant labels
                boolean hasLabel =false;
                
                for(String lbl : labelIndexMap.keySet()) {
                    if(labelIndexMap.get(lbl) == i) {
                        if(!hasLabel) {
                            p.println("");
                            hasLabel = true;
                        }
                        
                        p.println(lbl + ":");
                    }
                }
                
                // Print component
                switch(this.allInstructions.get(i)) {
                    case InitializedData id: {
                        p.print(switch(id.getWordSize()) {
                            case 1  -> "\tdb ";
                            case 2  -> "\tdw ";
                            case 4  -> "\tdp ";
                            default -> "\td" + (id.getWordSize() * 8) + " ";
                        });
                        
                        boolean first = true;
                        for(ResolvableValue rd : id.getData()) {
                            if(!first) {
                                p.print(", ");
                            } else {
                                first = false;
                            }
                            
                            p.print(rd + "");
                        }
                        
                        p.println("");
                        break;
                    }
                    
                    case Instruction inst: {
                        p.println("\t" + inst.toString(true));
                        break;
                    }
                    
                    case Repetition rep: {
                        p.println("\t" + rep);
                        break;
                    }
                    
                    case UninitializedData ud: {
                        if(ud.getSize() != 0) {
                            p.println("\tresb " + ud.getSize());
                        }
                        break;
                    }
                    
                    case Object o:
                        throw new IllegalArgumentException("Attempt to print unknown component class " + o.getClass());
                }
                
            }
        }
        
    }
    
    /**
     * Main for running the assembler standalone
     * 
     * @param args
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
        if(args.length < 1 || args.length > 5) {
            System.out.println("Usage: Assembler [flags] <input file> [<executable file> <executable entry point>]");
            System.out.println("Flags:");
            System.out.println("\t-o\tOptimize instruction width. If enabled, immediate widths are minimized. This restricts some expressions.");
            System.out.println("\t-d\tEnable debug-friendly object files. If enabled, object files contain the full names of labels, leading to much larger files.");
            System.exit(0);
        }
        
        // flags    
        int flagCount = 0;
        
        boolean optimize = false,
                debug = false;
        
        out:
        while(true) {
            switch(args[flagCount]) {
                case "-o":
                    flagCount++;
                    optimize = true;
                    break;
                
                case "-d":
                    flagCount++;
                    debug = true;
                    break;
                
                default:
                    break out;
            }
        }
        
        // entry symbol
        StringContainer entrySymbolContainer = new StringContainer((args.length > flagCount + 2) ? args[flagCount + 2] : "");
        
        // assemble
        List<RenameableRelocatableObject> objects = assemble(Paths.get(args[flagCount]), optimize, debug, entrySymbolContainer);
        
        // write object files
        LOG.info("Writing object files...");
        String directory = new File(args[flagCount]).getAbsolutePath();
        directory = directory.substring(0, directory.lastIndexOf("\\")) + "\\";
        
        Set<String> execContents = new HashSet<>();
        
        for(RelocatableObject obj : objects) {
            String filename = "obj\\" + obj.getName() + ".obj";
            execContents.add(filename);
            
            LOG.fine("Writing object file " + directory + filename);
            
            // make obj directory if it doesn't exist
            if(!new File(directory + "obj\\").exists()) {
                new File(directory + "obj\\").mkdir();
            }
            
            try(FileOutputStream fos = new FileOutputStream(directory + filename)){
                fos.write(obj.asObjectFile());
            }
        }
        
        // exec file
        if(args.length == (flagCount + 3)) {
            String fn = directory + args[flagCount + 1];
            
            LOG.fine("Writing exec file " + fn);
            
            try(PrintWriter execWriter = new PrintWriter(fn)) {
                // entry point
                execWriter.println("#entry " + entrySymbolContainer.s);
                
                // object files
                execContents.forEach(execWriter::println);
            }
        }
        
        LOG.info("Done.");
    }
    
    /**
     * Assemble from a list of AssemblyObjects (objects made from components).
     * This function does not unify library names or otherwise modify relocation symbol names
     * 
     * @param objects
     * @param optimizeInstructionWidth
     * @param debugFriendlyOutput
     * @param entrySymbolName
     * @return
     * @throws IOException
     */
    public static List<RenameableRelocatableObject> assemble(List<AssemblyObject> assemblyObjects, boolean optimizeInstructionWidth) throws IOException {
        LOG.info("Assembling from object list");
        
        ArrayList<RenameableRelocatableObject> objects = new ArrayList<>();
        
        for(AssemblyObject ao : assemblyObjects) {
            List<Component> unresolved = new ArrayList<>(ao.allInstructions);
            unresolved.removeIf(c -> c.isResolved());
            
            objects.add(assembleObjectFromComponents(ao.libraryName, ao.libraryFilesMap, ao.incomingReferences, ao.outgoingReferences, ao.incomingReferenceWidths, ao.outgoingReferenceWidths, ao.allInstructions, unresolved, ao.labelIndexMap, optimizeInstructionWidth));
        }
        
        return objects;
    }
    
    /**
     * Assembles a file and its dependencies
     * 
     * @param f
     * @return
     * @throws IOException
     */
    public static List<RenameableRelocatableObject> assemble(Path file, boolean optimizeInstructionWidth, boolean debugFriendlyOutput, String entrySymbolName) throws IOException {
        return assemble(file, optimizeInstructionWidth, debugFriendlyOutput, new StringContainer(entrySymbolName));
    }
    
    /**
     * Assembles a file and its dependencies
     * 
     * @param f
     * @return
     * @throws IOException
     */
    public static List<RenameableRelocatableObject> assemble(Path file, boolean optimizeInstructionWidth, boolean debugFriendlyOutput, StringContainer entrySymbolName) throws IOException {
        LOG.info("Assembling from main file: " + file + "...");
        
        ArrayList<RenameableRelocatableObject> objects = new ArrayList<>();
        Path standardLibPath = Paths.get("C:\\Users\\wetca\\data\\silly  code\\architecture\\NotSoTiny\\programming\\standard library\\");
        FileLocator locator = new FileLocator(file.toAbsolutePath().getParent(), standardLibPath, List.of(), List.of());
        locator.addFile(file);
        
        HashMap<String, Path> libraryMap = new HashMap<>();
        
        while(locator.hasUnconsumed()) {
            Path workingFile = locator.consume();
            
            RenameableRelocatableObject obj;
            
            if(workingFile.toString().endsWith(".obj")) {
                // object file, load it
                obj = new RenameableRelocatableObject(workingFile.toFile(), null);
            } else {
                // assembly file, assemble it
                List<Symbol> symbols;
                
                try(BufferedReader br = Files.newBufferedReader(workingFile)) {
                    symbols = lexer.lex(Tokenizer.tokenize(br.lines().toList()));
                }
                
                locator.setWorkingDirectory(workingFile);
                obj = assembleObjectFromSource(symbols, workingFile, locator, optimizeInstructionWidth);
            }
            
            libraryMap.put(obj.getName(), workingFile);
            objects.add(obj);
        }
        
        // unify library names
        LOG.fine("Unifying library names");
        for(String name : libraryMap.keySet()) {
            Path p = libraryMap.get(name);
            
            LOG.finest("Mapping " + p + " to " + name);
            
            for(RenameableRelocatableObject obj : objects) {
                obj.renameLibraryFile(p.toFile(), name);
            }
        }
        
        // compactify library names
        if(!debugFriendlyOutput) {
            Map<String, String> nameIDMap = new HashMap<>(),
                                libraryIDMap = new HashMap<>();
            int id = 0,
                lid = 0;
            
            // Generate names
            for(RenameableRelocatableObject obj : objects) {
                libraryIDMap.put(obj.getName(), Integer.toString(lid++, Character.MAX_RADIX));
                
                for(String s : obj.getOutgoingReferenceNames()) {
                    String n = obj.getName() + ".";
                    
                    if(s.equals("ORIGIN")) {
                        continue;
                    }
                    
                    String ids = Integer.toString(id++, Character.MAX_RADIX);
                    
                    if((n + s).equals(entrySymbolName.s)) {
                        // Update entry symbol
                        entrySymbolName.s = libraryIDMap.get(obj.getName()) + "." + ids;
                    }
                    
                    nameIDMap.put(n + s, n + ids);
                }
            }
            
            // Rename symbols
            for(RenameableRelocatableObject obj : objects) {
                for(String old : nameIDMap.keySet()) {
                    obj.renameGlobal(old, nameIDMap.get(old));
                }
            }
            
            // Rename libraries
            for(RenameableRelocatableObject obj : objects) {
                for(String old : libraryIDMap.keySet()) {
                    obj.renameLibrary(old, libraryIDMap.get(old));
                }
            }
        }
        
        LOG.info("Done.");
        return new ArrayList<RenameableRelocatableObject>(objects);
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
    public static RenameableRelocatableObject assembleObjectFromSource(List<Symbol> symbols, Path file, FileLocator locator, boolean optimizeInstructionWidth) throws IOException {
        LOG.info("Assembling file: " + file);
        
        int line = 0; // line in file
        
        // these variables are from the verytiny implementation
        String workingDirectory = file.toAbsolutePath().toString();
        int libNameIndex = workingDirectory.lastIndexOf("\\");
        
        String libraryName = workingDirectory.substring(libNameIndex + 1, workingDirectory.lastIndexOf('.'));
        workingDirectory = workingDirectory.substring(0, libNameIndex) + "\\";
        
        // relocation info
        // we'll need this later
        HashMap<String, Integer> outgoingReferences = new HashMap<>(),
                                 incomingReferenceWidths = new HashMap<>(),
                                 outgoingReferenceWidths = new HashMap<>();
        HashMap<String, List<Integer>> incomingReferences = new HashMap<>();
        HashMap<File, String> libraryNamesMap = new HashMap<>();
        
        /*
         * Assembler Passes
         * 1. Definitions pass          - Parse %define statements and apply them (handled by AssemblerLib) DONE
         * 2. Main parse pass           - Figure out what each instruction is, record labels. DONE
         * 3. Jump distinction pass     - Jumps to internal references are relative, jumps to external references are absolute.
         * 4. Constant width pass       - Determine whether relative jumps can hit their targets and update accordingly. Continues until all jumps are stable. Applies to all relative constants
         * 5. Constant resolution pass  - Resolve constants to their final values
         */
        
        /*
         * Relative jump width pass
         * All relative jumps are initialized to the maximum width. Jumps that can reach their target
         * with a shorter width are shortened, and this repeats until all jumps are their minimum width
         */
        
        /*
         * MAIN PARSE
         * Parse symbol queue to get instructions, labels, initialized data, and unintialized data
         */
        
        // symbols to parse
        LinkedList<Symbol> symbolQueue = new LinkedList<>(symbols);
        List<Component> allInstructions = new LinkedList<>(),         // every instruction parsed
                        unresolvedInstructions = new LinkedList<>();  // instructions with an unresolved component
        
        Map<String, Integer> labelIndexMap = new HashMap<>();   // label name -> allInstructions index. Points to the start of the Component at that address
        
        boolean encounteredError = false;
        while(symbolQueue.size() > 0) {
            Symbol s = symbolQueue.poll();
            
            // newline = update current line
            if(s instanceof LineMarkerSymbol lm) {
                line = lm.lineNumber();
                continue;
            }
            
            LOG.finest("Processing symbol: " + s);
            
            // parse
            try {
                // handle directives that need more info than normal
                boolean handled = false;
                if(s instanceof DirectiveSymbol d) {
                    switch(d.name()) {
                        // import another file
                        case "%INCLUDE":
                        case "IMPORT":
                            String fileName = "",
                                   libName = ""; // imported file's library name
                            
                            // we should have an Expression with <filename> as <name>
                            if(symbolQueue.poll() instanceof ExpressionSymbol es) {
                                fileName = switch(es.symbols().get(0)) {
                                    case NameSymbol ns      -> ns.name();
                                    case StringSymbol ss    -> ss.value();
                                    default -> throw new IllegalArgumentException("Invalid library description");
                                };
                                
                                // as
                                if(!(es.symbols().get(1) instanceof DirectiveSymbol ds && ds.name().equals("AS")))
                                    throw new IllegalArgumentException("Invalid library description");
                                
                                // value
                                if(es.symbols().get(2) instanceof NameSymbol ns) {
                                    libName = ns.name();
                                } else throw new IllegalArgumentException("Invalid library description " + es.symbols().get(2));
                            } else throw new IllegalArgumentException("Invalid library description");
                            
                            // add library
                            Path p = Paths.get(fileName);
                            if(!locator.addFile(p)) throw new IllegalArgumentException("Could not find library file " + p);
                            Path fp = locator.getSourceFile(p);
                            
                            LOG.finer("Added file " + fileName + " as " + libName);
                            
                            libraryNamesMap.put(fp.toFile(), libName);
                            handled = true;
                            break;
                            
                        // set library name
                        case "%LIBNAME":
                            // expecting a name
                            libraryName = switch(symbolQueue.poll()) {
                                case NameSymbol ns      -> ns.name();
                                case StringSymbol ss    -> ss.value();
                                default -> throw new IllegalArgumentException("Invalid library name");
                            };
                            
                            handled = true;
                            break;
                        
                        // set library origin
                        case "%ORG":
                            // expecting a constant
                            long origin = switch(symbolQueue.poll()) {
                                case ConstantSymbol cs -> cs.value();
                                default -> throw new IllegalArgumentException("Invalid origin");
                            };
                            
                            outgoingReferences.put("ORIGIN", (int) origin);
                            outgoingReferenceWidths.put("ORIGIN", 4);
                            
                            handled = true;
                            break;
                        
                        // flag as privilaged
                        case "%PRIVILAGED":
                            outgoingReferences.put("PRIVILAGED", 1);
                            outgoingReferenceWidths.put("PRIVILAGED", 4);
                            handled = true;
                            break;
                        
                        // disable instruction length optimizations
                        case "%NLO":
                            optimizeInstructionWidth = false;
                            handled = true;
                            break;
                        
                        // code directives
                        default:
                    }
                }
                
                if(!handled) {
                    if(s instanceof LabelSymbol l) {
                        if(labelIndexMap.containsKey(l.name())) {
                            LOG.warning("Duplicate label: " + l.name() + " on line " + line);
                            encounteredError = true;
                        }
                        
                        labelIndexMap.put(l.name(), allInstructions.size());
                        LOG.finer("Added label " + l.name() + " at index " + allInstructions.size());
                    } else {                
                        Component c = parseLine(s, symbolQueue, workingDirectory, line);
                        
                        if(c == null) {
                            encounteredError = true;
                        } else {
                            //LOG.finest(c.toString());
                            allInstructions.add(c);
                            
                            if(!c.isResolved()) unresolvedInstructions.add(c);
                        }
                    }
                }
            } catch(IndexOutOfBoundsException e) { // these let us avoid checking that needed symbols exist
                throw new IllegalArgumentException("Missing symbol after " + s + " on line " + line);
            } catch(IllegalArgumentException e) { // add line info to parser errors
                LOG.warning("ILLEGAL ARGUMENT EXCEPTION: " + e.getMessage() + " on line " + line);
                encounteredError = true;
            } catch(ClassCastException e) {
                throw new IllegalArgumentException("Invalid symbol after " + s + " on line " + line);
            }
        }
        
        LOG.finest("-- MAIN PARSE RESULTS --");
        LOG.finest(allInstructions.toString());
        LOG.finest(unresolvedInstructions.toString());
        LOG.finest(labelIndexMap.toString());
        
        if(encounteredError) {
            throw new IllegalStateException("Encountered error(s)");
        }
        
        // pass information to the component assembler
        return assembleObjectFromComponents(libraryName, libraryNamesMap, incomingReferences, outgoingReferences, incomingReferenceWidths, outgoingReferenceWidths, allInstructions, unresolvedInstructions, labelIndexMap, optimizeInstructionWidth);
    }
    
    /**
     * Assembles a file parsed Components. This performs instruction width optimization, constant resolution, and final relocatable object creation
     * 
     * @param libraryName
     * @param libraryFilesMap
     * @param incomingReferences
     * @param outgoingReferences
     * @param incomingReferenceWidths
     * @param outgoingReferenceWidths
     * @param allInstructions
     * @param unresolvedInstructions
     * @param labelIndexMap
     * @param optimizeInstructionWidth
     * @return
     */
    private static RenameableRelocatableObject assembleObjectFromComponents(String libraryName, HashMap<File, String> libraryFilesMap, HashMap<String, List<Integer>> incomingReferences, HashMap<String, Integer> outgoingReferences, HashMap<String, Integer> incomingReferenceWidths, HashMap<String, Integer> outgoingReferenceWidths,
                                                                            List<Component> allInstructions, List<Component> unresolvedInstructions, Map<String, Integer> labelIndexMap, boolean optimizeInstructionWidth) {    
        LOG.fine("Assembling from components");
        ArrayList<Byte> objectCode = new ArrayList<>();
        
        /*
         * CONSTANT RESOLUTION
         */
        
        NavigableMap<String, Integer> labelAddressMap = new TreeMap<>();        // label -> address
        NavigableMap<Integer, Integer> instructionAddressMap = new TreeMap<>(); // index in allInstructions -> address
        Set<String> libNames = new HashSet<>(libraryFilesMap.values());
        int addr = 0, lastInstructionAddr = -1;
        
        // build initial address map
        LOG.finer("Building label address map");
        buildAddressMaps(labelAddressMap, labelIndexMap, instructionAddressMap, allInstructions, libNames);
        
        LOG.finest("-- CONSTANT RESOLUTION FIRST PASS RESULTS --");
        LOG.finest(labelAddressMap.toString());
        
        // attempt to minimize parameter sizes (if enabled)
        if(optimizeInstructionWidth) {
            LOG.fine("Running instruction length optimization...");
            
            int optimizationPassNumber = 0;
            
            boolean changedValueSizes;
            
            do {
                LOG.fine("Running optimization pass " + optimizationPassNumber++);
                
                // resolve values
                addr = 0;
                lastInstructionAddr = -1;
                for(int i = 0; i < allInstructions.size(); i++) {
                    Component c = allInstructions.get(i);
                    
                    if(!c.isResolved()) {
                        String before = c.toString();
                        
                        resolveComponent(c, labelAddressMap, libNames, incomingReferences, libraryName, addr, lastInstructionAddr, true);
                        
                        LOG.finest(before + " resolved to " + c);
                    }
                    
                    addr += c.getSize();
                    
                    if(c instanceof Instruction) {
                        lastInstructionAddr = addr;
                    }
                }
                
                changedValueSizes = false;
                
                /*
                 * The following optimizations can be made to instruction length
                 * MOVW -> MOVZ
                 * MOVW -> MOVS
                 * MOV [A, B, C, D], i16 -> MOVS [A, B, C, D], i8
                 * MOV rim -> MOVZ
                 * MOV rim -> MOVS
                 * [ADD, ADC, SUB, SBB] [A, B, C, D], i16 -> [ADD, ADC, SUB, SBB] [A, B, C, D], i8
                 * [ADD, ADC, SUB, SBB] rim -> [ADD, ADC, SUB, SBB] [A, B, C, D], [i16, i8]
                 * [ADD, ADC, SUB, SBB] rim -> [ADD, ADC, SUB, SBB] rim, i8
                 * [ADD, SUB] rim, 1 -> [INC, DEC] rim
                 * [ADC, SBB] rim, 0 -> [ICC, DCC] rim
                 * [INC, ICC, DEC, DCC] rim -> [INC, ICC, DEC, DCC] [I, J, K, L]
                 * JMP [i32, i16] -> JMP [i8, i16]
                 * JMP rim -> JMP [i32, i16, i8]
                 * JMPA rim -> JMPA i32
                 * CALL [i32, i16] -> CALL [i32, i16, i8]
                 * CALL rim -> CALL i16
                 * CALLA rim -> CALLA i32
                 * CMP rim16 -> CMP rim, i8
                 * CMP rim -> CMP rim, 0
                 * Jcc rim -> Jcc i8
                 * INT rim -> INT i8
                 * PUSH rim -> PUSH [A, B, C, D, I, J, K, L]
                 * POP rim -> POP [A, B, C, D, I, J, K, L]
                 */
                
                // change value sizes if possible
                for(int i = 0; i < allInstructions.size(); i++) {
                    Component c = allInstructions.get(i);
                    
                    // is this eligible for optimization
                    if(c instanceof Instruction inst) {
                        ResolvableLocationDescriptor src = inst.getSourceDescriptor(),
                                                     dst = inst.getDestinationDescriptor();
                        
                        // PUSH/POP optimization
                        if(inst.getOpcode() == Opcode.PUSH_RIM && src.getType() == LocationType.REGISTER && src.getSize() == 2) {
                            inst.setOpcode(switch(src.getRegister()) {
                                case A  -> Opcode.PUSH_A;
                                case B  -> Opcode.PUSH_B;
                                case C  -> Opcode.PUSH_C;
                                case D  -> Opcode.PUSH_D;
                                case I  -> Opcode.PUSH_I;
                                case J  -> Opcode.PUSH_J;
                                case K  -> Opcode.PUSH_K;
                                case L  -> Opcode.PUSH_L;
                                default -> Opcode.PUSH_RIM;
                            });
                            
                            changedValueSizes = true;
                        }
                        
                        else if(inst.getOpcode() == Opcode.PUSH_RIM && dst.getType() == LocationType.REGISTER && dst.getSize() == 2) {
                            inst.setOpcode(switch(dst.getRegister()) {
                                case A  -> Opcode.POP_A;
                                case B  -> Opcode.POP_B;
                                case C  -> Opcode.POP_C;
                                case D  -> Opcode.POP_D;
                                case I  -> Opcode.POP_I;
                                case J  -> Opcode.POP_J;
                                case K  -> Opcode.POP_K;
                                case L  -> Opcode.POP_L;
                                default -> Opcode.POP_RIM;
                            });
                            
                            changedValueSizes = true;
                        }
                        
                        // Optimize source immediates
                        else if(src.getType() == LocationType.IMMEDIATE && src.isResolved() && !inst.hasFixedSize()) {
                            long val = src.getImmediate().value();
                            int width = getValueWidth(val, true, true),
                                dstWidth = dst.getSize();
                            
                            // hijack changedValueSizes for logging
                            boolean changedValueSizesBefore = changedValueSizes;
                            changedValueSizes = false;
                            Opcode before = inst.getOpcode();
                            
                            LOG.finest("optimization candidate " + inst + " size " + width);
                            
                            switch(inst.getOpcode()) {
                                // MOVW -> MOVZ
                                // MOVW -> MOVS
                                case MOVW_RIM:
                                    if(width <= (dstWidth / 2)) {
                                        inst.setOpcode(Opcode.MOVS_RIM);
                                        changedValueSizes = true;
                                    } else if(canZeroExtend(val, dstWidth / 2)) {
                                        inst.setOpcode(Opcode.MOVZ_RIM);
                                        changedValueSizes = true;
                                    }
                                    break;
                                
                                // MOV [A, B, C, D], I16 -> MOVS [A, B, C, D], I8
                                case MOV_A_I16:
                                    if(width == 1) {
                                        inst.setOpcode(Opcode.MOVS_A_I8);
                                        changedValueSizes = true;
                                    }
                                    break;
                                    
                                case MOV_B_I16:
                                    if(width == 1) {
                                        inst.setOpcode(Opcode.MOVS_B_I8);
                                        changedValueSizes = true;
                                    }
                                    break;
                                    
                                case MOV_C_I16:
                                    if(width == 1) {
                                        inst.setOpcode(Opcode.MOVS_C_I8);
                                        changedValueSizes = true;
                                    }
                                    break;
                                    
                                case MOV_D_I16:
                                    if(width == 1) {
                                        inst.setOpcode(Opcode.MOVS_D_I8);
                                        changedValueSizes = true;
                                    }
                                    break;
                                
                                // MOVS rim[A, B, C, D], I8 -> MOVS [A, B, C, D], I8
                                case MOVS_RIM:
                                    if(dstWidth == 2 && width == 1) {
                                        switch(dst.getRegister()) {
                                            case A: inst.setOpcode(Opcode.MOVS_A_I8); changedValueSizes = true; break;
                                            case B: inst.setOpcode(Opcode.MOVS_B_I8); changedValueSizes = true; break;
                                            case C: inst.setOpcode(Opcode.MOVS_C_I8); changedValueSizes = true; break;
                                            case D: inst.setOpcode(Opcode.MOVS_D_I8); changedValueSizes = true; break;
                                            default:
                                        }
                                    }
                                    break;
                                    
                                // MOV RIM -> MOVZ RIM
                                // MOV RIM -> MOVS RIM
                                case MOV_RIM:
                                    if(dstWidth == 2) {
                                        if(width == 1) {
                                            inst.setOpcode(Opcode.MOVS_RIM);
                                            changedValueSizes = true;
                                        } else if(canZeroExtend(val, 1)) {
                                            inst.setOpcode(Opcode.MOVZ_RIM);
                                            changedValueSizes = true;
                                        }
                                    }
                                    break;
                                    
                                // ADD RIM -> ADD [A, B, C, D, I, J, K, L], I8
                                // ADD RIM -> ADD RIM, I8
                                case ADD_RIM:
                                    if(dst.getType() == LocationType.REGISTER && width == 1) {
                                        switch(dst.getRegister()) {
                                            case A:     inst.setOpcode(Opcode.ADD_A_I8); changedValueSizes = true; break;
                                            case B:     inst.setOpcode(Opcode.ADD_B_I8); changedValueSizes = true; break;
                                            case C:     inst.setOpcode(Opcode.ADD_C_I8); changedValueSizes = true; break;
                                            case D:     inst.setOpcode(Opcode.ADD_D_I8); changedValueSizes = true; break;
                                            case I:     inst.setOpcode(Opcode.ADD_I_I8); changedValueSizes = true; break;
                                            case J:     inst.setOpcode(Opcode.ADD_J_I8); changedValueSizes = true; break;
                                            case K:     inst.setOpcode(Opcode.ADD_K_I8); changedValueSizes = true; break;
                                            case L:     inst.setOpcode(Opcode.ADD_L_I8); changedValueSizes = true; break;
                                            default:    if(width == 1) { inst.setOpcode(Opcode.ADD_RIM_I8); changedValueSizes = true; } break;
                                        }
                                    } else if(dst.getSize() != 1 && width == 1) {
                                        inst.setOpcode(Opcode.ADD_RIM_I8);
                                        changedValueSizes = true;
                                    }
                                    break;
                                
                                // ADD RIM, 1 -> INC RIM
                                case ADD_RIM_I8:
                                    if(val == 1) {
                                        inst.setOpcode(Opcode.INC_RIM);
                                        changedValueSizes = true;
                                    }
                                    break;
                                    
                                // ADC RIM -> ADC [A, B, C, D], I8
                                // ADC RIM -> ADC RIM, I8
                                case ADC_RIM:
                                    if(dst.getType() == LocationType.REGISTER && width == 1) {
                                        inst.setOpcode(Opcode.ADC_RIM_I8); changedValueSizes = true;
                                    } else if(dst.getSize() != 1 && width == 1) {
                                        inst.setOpcode(Opcode.ADC_RIM_I8);
                                        changedValueSizes = true;
                                    }
                                    break;
                                
                                // ADC RIM, 0 -> ICC RIM
                                case ADC_RIM_I8:
                                    if(val == 0) {
                                        inst.setOpcode(Opcode.ICC_RIM);
                                        changedValueSizes = true;
                                    }
                                    break;
                                
                                // SUB RIM -> SUB [A, B, C, D, I, J, K, L], I8
                                // SUB RIM -> SUB RIM, I8
                                case SUB_RIM:
                                    if(dst.getType() == LocationType.REGISTER && width == 1) {
                                        switch(dst.getRegister()) {
                                            case A:     inst.setOpcode(Opcode.SUB_A_I8); changedValueSizes = true; break;
                                            case B:     inst.setOpcode(Opcode.SUB_B_I8); changedValueSizes = true; break;
                                            case C:     inst.setOpcode(Opcode.SUB_C_I8); changedValueSizes = true; break;
                                            case D:     inst.setOpcode(Opcode.SUB_D_I8); changedValueSizes = true; break;
                                            case I:     inst.setOpcode(Opcode.SUB_I_I8); changedValueSizes = true; break;
                                            case J:     inst.setOpcode(Opcode.SUB_J_I8); changedValueSizes = true; break;
                                            case K:     inst.setOpcode(Opcode.SUB_K_I8); changedValueSizes = true; break;
                                            case L:     inst.setOpcode(Opcode.SUB_L_I8); changedValueSizes = true; break;
                                            default:    if(width == 1) { inst.setOpcode(Opcode.SUB_RIM_I8); changedValueSizes = true; } break;
                                        }
                                    } else if(dst.getSize() != 1 && width == 1) {
                                        inst.setOpcode(Opcode.SUB_RIM_I8);
                                        changedValueSizes = true;
                                    }
                                    break;
                                
                                // SUB RIM, 1 -> DEC RIM
                                case SUB_RIM_I8:
                                    if(val == 1) {
                                        inst.setOpcode(Opcode.DEC_RIM);
                                        changedValueSizes = true;
                                    }
                                    break;
                                
                                // SBB RIM -> SBB [A, B, C, D], I8
                                // SBB RIM -> SBB RIM, I8
                                case SBB_RIM:
                                    if(dst.getType() == LocationType.REGISTER) {
                                        inst.setOpcode(Opcode.SBB_RIM_I8); changedValueSizes = true; 
                                    } else if(dst.getSize() != 1 && width == 1) {
                                        inst.setOpcode(Opcode.SBB_RIM_I8);
                                        changedValueSizes = true;
                                    }
                                    break;
                                
                                // SBB RIM, 0 -> DCC RIM
                                case SBB_RIM_I8:
                                    if(val == 0) {
                                        inst.setOpcode(Opcode.DCC_RIM);
                                        changedValueSizes = true;
                                    }
                                    break;
                                
                                // SHL RIM -> SHL RIM I8
                                case SHL_RIM:
                                    if(dst.getType() == LocationType.IMMEDIATE && width == 1) {
                                        inst.setOpcode(Opcode.SHL_RIM_I8);
                                    }
                                    break;
                                
                                // SHR RIM -> SHR RIM I8
                                case SHR_RIM:
                                    if(dst.getType() == LocationType.IMMEDIATE && width == 1) {
                                        inst.setOpcode(Opcode.SHR_RIM_I8);
                                    }
                                    break;
                                
                                // SAR RIM -> SAR RIM I8
                                case SAR_RIM:
                                    if(dst.getType() == LocationType.IMMEDIATE && width == 1) {
                                        inst.setOpcode(Opcode.SAR_RIM_I8);
                                    }
                                    break;
                                
                                // ROL RIM -> ROL RIM I8
                                case ROL_RIM:
                                    if(dst.getType() == LocationType.IMMEDIATE && width == 1) {
                                        inst.setOpcode(Opcode.ROL_RIM_I8);
                                    }
                                    break;
                                
                                // ROR RIM -> ROR RIM I8
                                case ROR_RIM:
                                    if(dst.getType() == LocationType.IMMEDIATE && width == 1) {
                                        inst.setOpcode(Opcode.ROR_RIM_I8);
                                    }
                                    break;
                                
                                // RCL RIM -> RCL RIM I8
                                case RCL_RIM:
                                    if(dst.getType() == LocationType.IMMEDIATE && width == 1) {
                                        inst.setOpcode(Opcode.RCL_RIM_I8);
                                    }
                                    break;
                                
                                // RCR RIM -> RCR RIM I8
                                case RCR_RIM:
                                    if(dst.getType() == LocationType.IMMEDIATE && width == 1) {
                                        inst.setOpcode(Opcode.RCR_RIM_I8);
                                    }
                                    break;
                                
                                // JMP I16 -> JMP I8
                                case JMP_I16:
                                    if(width == 1) {
                                        inst.setOpcode(Opcode.JMP_I8);
                                        changedValueSizes = true;
                                    }
                                    break;
                                    
                                // JMP I32 -> JMP [I16, I8]
                                case JMP_I32:
                                    if(width == 1) {
                                        inst.setOpcode(Opcode.JMP_I8);
                                        changedValueSizes = true;
                                    } else if(width == 2) {
                                        inst.setOpcode(Opcode.JMP_I16);
                                        changedValueSizes = true;
                                    }
                                    break;
                                    
                                // JMP rim -> JMP [i16, i8]
                                case JMP_RIM:
                                    if(width == 1) {
                                        inst.setOpcode(Opcode.JMP_I8);
                                        changedValueSizes = true;
                                    } else {
                                        inst.setOpcode(Opcode.JMP_I16);
                                        changedValueSizes = true;
                                    }
                                    break;
                                
                                // JMPA rim -> JMPA i32
                                case JMPA_RIM32:
                                    inst.setOpcode(Opcode.JMPA_I32);
                                    changedValueSizes = true;
                                    break;
                                
                                // CALL I16 -> CALL I8
                                case CALL_I16:
                                    if(width == 1) {
                                        inst.setOpcode(Opcode.CALL_I8);
                                        changedValueSizes = true;
                                    }
                                    break;
                                    
                                // JMP I32 -> JMP [I16, I8]
                                case CALL_I32:
                                    if(width == 1) {
                                        inst.setOpcode(Opcode.CALL_I8);
                                        changedValueSizes = true;
                                    } else if(width == 2) {
                                        inst.setOpcode(Opcode.CALL_I16);
                                        changedValueSizes = true;
                                    }
                                    break;
                                
                                // CALL rim -> CALL [i16, i8]
                                case CALL_RIM:
                                    if(width == 1) {
                                        inst.setOpcode(Opcode.CALL_I8);
                                        changedValueSizes = true;
                                    } else {
                                        inst.setOpcode(Opcode.CALL_I16);
                                        changedValueSizes = true;
                                    }
                                    break;
                                
                                // CALLA rim -> CALLA i32
                                case CALLA_RIM32:
                                    inst.setOpcode(Opcode.CALLA_I32);
                                    changedValueSizes = true;
                                    break;
                                
                                /*
                                 * There is a very important assumption being made here that
                                 * a value will never go from zero to nonzero
                                 */
                                // CMP rim, i8 -> CMP rim, 0
                                case CMP_RIM_I8:
                                    if(val == 0) {
                                        inst.setOpcode(Opcode.CMP_RIM_0);
                                        changedValueSizes = true;
                                    }
                                    break;
                                
                                // CMP rim -> CMP rim, i8
                                // CMP rim -> CMP rim, 0
                                case CMP_RIM:
                                    if(val == 0) { 
                                        inst.setOpcode(Opcode.CMP_RIM_0);
                                        changedValueSizes = true;
                                    } else if(dst.getSize() != 1 && width == 1) {
                                        inst.setOpcode(Opcode.CMP_RIM_I8);
                                        changedValueSizes = true;
                                    }
                                    break;
                                
                                // Jcc rim -> Jcc i8
                                case JC_RIM:
                                    if(width == 1) {
                                        inst.setOpcode(Opcode.JC_I8);
                                        changedValueSizes = true;
                                    }
                                    break;
                                
                                case JNC_RIM:
                                    if(width == 1) {
                                        inst.setOpcode(Opcode.JNC_I8);
                                        changedValueSizes = true;
                                    }
                                    break;
                                
                                case JS_RIM:
                                    if(width == 1) {
                                        inst.setOpcode(Opcode.JS_I8);
                                        changedValueSizes = true;
                                    }
                                    break;
                                
                                case JNS_RIM:
                                    if(width == 1) {
                                        inst.setOpcode(Opcode.JNS_I8);
                                        changedValueSizes = true;
                                    }
                                    break;
                                
                                case JO_RIM:
                                    if(width == 1) {
                                        inst.setOpcode(Opcode.JO_I8);
                                        changedValueSizes = true;
                                    }
                                    break;
                                
                                case JNO_RIM:
                                    if(width == 1) {
                                        inst.setOpcode(Opcode.JNO_I8);
                                        changedValueSizes = true;
                                    }
                                    break;
                                
                                case JZ_RIM:
                                    if(width == 1) {
                                        inst.setOpcode(Opcode.JZ_I8);
                                        changedValueSizes = true;
                                    }
                                    break;
                                
                                case JNZ_RIM:
                                    if(width == 1) {
                                        inst.setOpcode(Opcode.JNZ_I8);
                                        changedValueSizes = true;
                                    }
                                    break;
                                
                                case JA_RIM:
                                    if(width == 1) {
                                        inst.setOpcode(Opcode.JA_I8);
                                        changedValueSizes = true;
                                    }
                                    break;
                                
                                case JBE_RIM:
                                    if(width == 1) {
                                        inst.setOpcode(Opcode.JBE_I8);
                                        changedValueSizes = true;
                                    }
                                    break;
                                
                                case JG_RIM:
                                    if(width == 1) {
                                        inst.setOpcode(Opcode.JG_I8);
                                        changedValueSizes = true;
                                    }
                                    break;
                                
                                case JGE_RIM:
                                    if(width == 1) {
                                        inst.setOpcode(Opcode.JGE_I8);
                                        changedValueSizes = true;
                                    }
                                    break;
                                
                                case JL_RIM:
                                    if(width == 1) {
                                        inst.setOpcode(Opcode.JL_I8);
                                        changedValueSizes = true;
                                    }
                                    break;
                                
                                case JLE_RIM:
                                    if(width == 1) {
                                        inst.setOpcode(Opcode.JLE_I8);
                                        changedValueSizes = true;
                                    }
                                    break;
                                
                                case INT_RIM:
                                    if(width == 1) {
                                        inst.setOpcode(Opcode.INT_I8);
                                        changedValueSizes = true;
                                    }
                                    break;
                                
                                default:
                            }
                            
                            if(changedValueSizes) {
                                LOG.finer("Optimized " + before + " to " + inst);
                            }
                            
                            changedValueSizes |= changedValueSizesBefore;
                        }
                        
                        // Optimize other aliases
                        switch(inst.getOpcode()) {
                            // INC RIM -> INC [I, J, K, L]
                            case INC_RIM:
                                if(dst.getType() == LocationType.REGISTER) {
                                    switch(dst.getRegister()) {
                                        case I:     inst.setOpcode(Opcode.INC_I); changedValueSizes = true; break;
                                        case J:     inst.setOpcode(Opcode.INC_J); changedValueSizes = true; break;
                                        case K:     inst.setOpcode(Opcode.INC_K); changedValueSizes = true; break;
                                        case L:     inst.setOpcode(Opcode.INC_L); changedValueSizes = true; break;
                                        default:    break;    
                                    }
                                }
                                break;
                                
                            // ICC RIM -> ICC [A, B, C, D, I, J, K, L]
                            case ICC_RIM:
                                if(dst.getType() == LocationType.REGISTER) {
                                    switch(dst.getRegister()) {
                                        case A:     inst.setOpcode(Opcode.ICC_A); changedValueSizes = true; break;
                                        case B:     inst.setOpcode(Opcode.ICC_B); changedValueSizes = true; break;
                                        case C:     inst.setOpcode(Opcode.ICC_C); changedValueSizes = true; break;
                                        case D:     inst.setOpcode(Opcode.ICC_D); changedValueSizes = true; break;
                                        case I:     inst.setOpcode(Opcode.ICC_I); changedValueSizes = true; break;
                                        case J:     inst.setOpcode(Opcode.ICC_J); changedValueSizes = true; break;
                                        case K:     inst.setOpcode(Opcode.ICC_K); changedValueSizes = true; break;
                                        case L:     inst.setOpcode(Opcode.ICC_L); changedValueSizes = true; break;
                                        default:    break;    
                                    }
                                }
                                break;
                                
                            // DEC RIM -> DEC [I, J, K, L]
                            case DEC_RIM:
                                if(dst.getType() == LocationType.REGISTER) {
                                    switch(dst.getRegister()) {
                                        case I:     inst.setOpcode(Opcode.DEC_I); changedValueSizes = true; break;
                                        case J:     inst.setOpcode(Opcode.DEC_J); changedValueSizes = true; break;
                                        case K:     inst.setOpcode(Opcode.DEC_K); changedValueSizes = true; break;
                                        case L:     inst.setOpcode(Opcode.DEC_L); changedValueSizes = true; break;
                                        default:    break;    
                                    }
                                }
                                break;
                                
                            // DCC RIM -> DCC [A, B, C, D, I, J, K, L]
                            case DCC_RIM:
                                if(dst.getType() == LocationType.REGISTER) {
                                    switch(dst.getRegister()) {
                                        case A:     inst.setOpcode(Opcode.DCC_A); changedValueSizes = true; break;
                                        case B:     inst.setOpcode(Opcode.DCC_B); changedValueSizes = true; break;
                                        case C:     inst.setOpcode(Opcode.DCC_C); changedValueSizes = true; break;
                                        case D:     inst.setOpcode(Opcode.DCC_D); changedValueSizes = true; break;
                                        case I:     inst.setOpcode(Opcode.DCC_I); changedValueSizes = true; break;
                                        case J:     inst.setOpcode(Opcode.DCC_J); changedValueSizes = true; break;
                                        case K:     inst.setOpcode(Opcode.DCC_K); changedValueSizes = true; break;
                                        case L:     inst.setOpcode(Opcode.DCC_L); changedValueSizes = true; break;
                                        default:    break;    
                                    }
                                }
                                break;
                            
                            default:
                        }
                        
                        // Optimize memory offsets
                        if(src.getType() == LocationType.MEMORY || dst.getType() == LocationType.MEMORY) {
                            // get the value to see if we can optimize
                            ResolvableValue rOffset;
                            
                            if(src.getType() == LocationType.MEMORY) {
                                rOffset = src.getMemory().getOffset();
                            } else {
                                rOffset = dst.getMemory().getOffset();
                            }
                            
                            // we need a resolved nonzero value
                            if(rOffset.isResolved()) {
                                long offset = rOffset.value();
                                
                                int oldImmSize = inst.getImmediateWidth();
                                
                                int newImmSize = (offset != 0) ? getValueWidth(offset, true, true) : 0;
                                
                                //LOG.finest(c + "  old: " + oldImmSize + " new: " + newImmSize);
                                
                                if(newImmSize != oldImmSize) {
                                    inst.setImmediateWidth(newImmSize);
                                    changedValueSizes = true;
                                }
                            }
                        }
                    }
                }
                
                // if we changed anything, rebuild the label map
                if(changedValueSizes) {
                    LOG.finer("Rebuilding label address map");
                    buildAddressMaps(labelAddressMap, labelIndexMap, instructionAddressMap, allInstructions, libNames);
                }
                
                // unresolve values
                incomingReferences.clear();
                for(Component c : allInstructions) {
                    unresolveComponent(c);
                }
                
                // if nothing changed we're done
            } while(changedValueSizes);
            
        }
        
        LOG.finer("PERFORMING FINAL RESOLUTION PASS");
        
        // perform final resolution
        addr = 0;
        lastInstructionAddr = -1;
        for(int i = 0; i < allInstructions.size(); i++) {
            Component c = allInstructions.get(i);
            int len = c.getSize();
            
            if(!c.isResolved()) {
                String before = c.toString();
                
                boolean relocated = resolveComponent(c, labelAddressMap, libNames, incomingReferences, libraryName, addr, lastInstructionAddr, true);
                
                LOG.finest(before + " resolved to " + c);
                
                if(!c.isResolved() && !relocated) {
                    throw new IllegalStateException("Unable to resolve component: " + c);
                }
            }
            
            int s = c.getSize();
            
            if(s != len) {
                // check if this affected any labels
                for(String lbl : labelIndexMap.keySet()) {
                    if(labelIndexMap.get(lbl) > i) {
                        throw new IllegalArgumentException("Length changed after resolution: " + len + " -> " + s + " for component " + c);
                    }
                }
            }
            
            // update addresses
            addr += s;
            
            if(c instanceof Instruction) {
                lastInstructionAddr = addr;
            }
        }
        
        LOG.finest("-- CONSTANT RESOLUTION FINAL PASS RESULTS --");
        LOG.finest(allInstructions.toString());
        
        // collect object code
        for(int i = 0; i < allInstructions.size(); i++) {
            Component c = allInstructions.get(i);
            List<Byte> objCode = c.getObjectCode();
            int l = objectCode.size();
            
            StringBuilder sb = new StringBuilder(String.format("%04X_%04X:", l >> 16, l & 0xFFFF));
            
            if(objCode.size() <= 8) {
                objCode.forEach(b -> sb.append(String.format(" %02X", b)));
            }
            
            objectCode.addAll(objCode);
            
            LOG.finest(String.format("%-34s %-48s %d bytes", sb, c, objectCode.size()));
        }
        
        // convert object code to array
        byte[] objectCodeArray = new byte[objectCode.size()];
        
        for(int i = 0; i < objectCodeArray.length; i++) {
            objectCodeArray[i] = objectCode.get(i);
        }
        
        // leftover relocation stuff
        for(String lbl : labelAddressMap.keySet()) {
            int address = labelAddressMap.get(lbl);
            
            outgoingReferences.put(lbl, address);
            outgoingReferenceWidths.put(lbl, 4);
        }
        
        for(String ref : incomingReferences.keySet()) {
            incomingReferenceWidths.put(ref, 4);
        }
        
        LOG.finest("-- FINAL RELOCATION INFO --");
        LOG.finest("OUTGOING REFERENCES: " + outgoingReferences);
        LOG.finest("INCOMING REFERENCES: " + incomingReferences);
        
        return new RenameableRelocatableObject(Endianness.LITTLE, libraryName, 4, incomingReferences, outgoingReferences, incomingReferenceWidths, outgoingReferenceWidths, objectCodeArray, false, libraryFilesMap);
    }
    
    /**
     * Builds the label address map and instruction address map
     * 
     * @param labelAddressMap
     * @param labelIndexMap
     * @param instructionAddressMap
     * @param allInstructions
     * @param libNames
     */
    private static void buildAddressMaps(Map<String, Integer> labelAddressMap, Map<String, Integer> labelIndexMap, Map<Integer, Integer> instructionAddressMap, List<Component> allInstructions, Set<String> libNames) {
        int addr = 0;
        for(int i = 0; i < allInstructions.size(); i++) {
            instructionAddressMap.put(i, addr);
            
            Component c = allInstructions.get(i);
            
            // check for invalid library references
            // we can't do math on a value we cannot access by definition
            if(!c.isResolved()) {
                validateLibraryReferences(c, libNames, labelIndexMap);
            }
            
            // getSize is set up to give the worst-case immediate width isn't overridden
            addr += c.getSize();
        }
        
        // for trailing label
        instructionAddressMap.put(allInstructions.size(), addr);
        
        // update labels as well
        for(String lbl : labelIndexMap.keySet()) {
            int i = labelIndexMap.get(lbl);
            labelAddressMap.put(lbl, instructionAddressMap.get(i));
        }
    }
    
    /**
     * Unresolves a component
     * 
     * @param c
     */
    private static void unresolveComponent(Component c) {
        switch(c) {
            case Instruction inst:
                ResolvableLocationDescriptor source = inst.getSourceDescriptor(),
                                             dest = inst.getDestinationDescriptor();
                
                // unresolve source
                switch(source.getType()) {
                    case IMMEDIATE:
                        source.getImmediate().unresolveNames();
                        break;
                        
                    case MEMORY:
                        source.getMemory().getOffset().unresolveNames();
                        break;
                        
                    default:
                }
                
                // unresolve destination
                switch(dest.getType()) {
                    case MEMORY:
                        dest.getMemory().getOffset().unresolveNames();
                        break;
                    
                    default:
                }
                
                break;
                
            case InitializedData init:
                for(ResolvableValue rv : init.getData()) {
                    rv.unresolveNames();
                }
                break;
                
            case UninitializedData uninit:
                break;
                
            case Repetition rep:
                rep.getReps().unresolveNames();
                unresolveComponent(rep.getData());
                break;
                
            default:
        }
    }
    
    /**
     * Resolves a component
     * 
     * @param c
     * @return
     */
    private static boolean resolveComponent(Component c, Map<String, Integer> labelAddressMap, Set<String> libNames, HashMap<String, List<Integer>> incomingReferences, String libraryName, int addr, int lastInstructionAddress, boolean firstAttempt) {
        boolean relocated = false;
        
        int size = c.getSize();
        
        switch(c) {
            case Instruction inst:
                ResolvableLocationDescriptor source = inst.getSourceDescriptor(),
                                             dest = inst.getDestinationDescriptor();
                
                // resolve source
                if(!source.isResolved()) {
                    switch(source.getType()) {
                        case IMMEDIATE:
                            // jumps are relative
                            Operation type = inst.getOpcode().getType();
                            boolean relative = false;
                            if(type == Operation.JMP || type == Operation.JCC || type == Operation.CALL) relative = true;
                            
                            relocated |= resolveValue(source.getImmediate(), labelAddressMap, libNames, incomingReferences, libraryName, addr, inst.getImmediateOffset(), size, relative, false, lastInstructionAddress);
                            
                            if(relative && !source.isResolved()) throw new IllegalArgumentException("Could not resolve relative value: " + source + " in " + inst);
                            
                            // infer size if not done already
                            if(source.isResolved() && source.getSize() == -1) source.setSize(getValueWidth(source.getImmediate().value(), false, false), false);
                            break;
                            
                        case MEMORY:
                            relocated |= resolveValue(source.getMemory().getOffset(), labelAddressMap, libNames, incomingReferences, libraryName, addr, inst.getAddressOffset(), size, false, false, lastInstructionAddress);
                            break;
                            
                        default:
                    }
                }
                
                // resolve destination
                if(!dest.isResolved()) {
                    switch(dest.getType()) {
                        /* this shouldn't be possible idk why i wrote it
                        case IMMEDIATE:
                            resolveValue(dest.getImmediate(), labelAddressMap, false, 0);
                            if(dest.isResolved() && dest.getSize() == -1) dest.setSize(getValueWidth(dest.getImmediate().value(), false));
                            break; */
                            
                        case MEMORY:
                            relocated |= resolveValue(dest.getMemory().getOffset(), labelAddressMap, libNames, incomingReferences, libraryName, addr, inst.getAddressOffset(), size, false, false, lastInstructionAddress);
                            break;
                            
                        default:
                    }
                }
                break;
                
            case InitializedData init:
                for(ResolvableValue rv : init.getUnresolvedData()) {
                    relocated = resolveValue(rv, labelAddressMap, libNames, incomingReferences, libraryName, addr, 0, size, false, false, lastInstructionAddress);
                    
                    if(init.getWordSize() != 4) relocated = false;
                }
                break;
                
            case UninitializedData uninit:
                break;
            
            case Repetition rep:
                relocated = resolveValue(rep.getReps(), labelAddressMap, libNames, incomingReferences, libraryName, addr, 0, size, false, false, lastInstructionAddress);
                
                if(relocated) throw new IllegalArgumentException("Repetition count cannot be an external value");
                
                for(int i = 0; i < rep.getReps().value(); i++) {
                    relocated |= resolveComponent(rep.getData(), labelAddressMap, libNames, incomingReferences, libraryName, addr, lastInstructionAddress, firstAttempt);
                    addr += rep.getData().getSize();
                }
                break;
            
            default:
        }
        
        return relocated;
    }
    
    /**
     * Validates library references contained in the given component
     * 
     * @param c
     */
    private static void validateLibraryReferences(Component c, Set<String> libNames, Map<String, Integer> labelIndexMap) {
        switch(c) {
            case Instruction inst:
                ResolvableLocationDescriptor source = inst.getSourceDescriptor(),
                                             dest = inst.getDestinationDescriptor();
                
                // check source
                if(!source.isResolved()) {
                    ResolvableValue rv = switch(source.getType()) {
                        case IMMEDIATE  -> source.getImmediate();
                        case MEMORY     -> source.getMemory().getOffset();
                        default         -> null; // shouldn't happen
                    };
                    
                    if(rv instanceof ResolvableExpression re) checkExpressionValidity(re, labelIndexMap);
                    else if(rv instanceof ResolvableConstant rc && isLibraryReference(rc.getName(), libNames)) {
                        // make sure jumps to libraries are absolute
                        Operation opType = inst.getOpcode().getType();
                        
                        if(opType == Operation.JCC) throw new IllegalArgumentException("Cannot make conditional jump absolute: " + inst);
                        else if(opType == Operation.JMP) { // convert to JMPA 
                            inst.setOpcode(Opcode.JMPA_I32);
                        } else if(opType == Operation.CALL) { // convert to CALLA
                            inst.setOpcode(Opcode.CALLA_I32);
                        }
                    }
                }
                
                // check destination
                if(!dest.isResolved()) {
                    ResolvableValue rv = switch(dest.getType()) {
                        case IMMEDIATE  -> dest.getImmediate();
                        case MEMORY     -> dest.getMemory().getOffset();
                        default         -> null; // shouldn't happen
                    };
                    
                    if(rv instanceof ResolvableExpression re) checkExpressionValidity(re, labelIndexMap);
                }
                break;
            
            case InitializedData init:
                for(ResolvableValue rv : init.getUnresolvedData()) {
                    if(rv instanceof ResolvableExpression rei) {
                        checkExpressionValidity(rei, labelIndexMap);
                    }
                }
                break;
            
            case UninitializedData uninit:
                break;
            
            case Repetition rep:
                validateLibraryReferences(rep.getData(), libNames, labelIndexMap);
                
                if(rep.getReps() instanceof ResolvableExpression re) checkExpressionValidity(re, labelIndexMap);
                break;
            
            default: // not possible
        }
    }
    
    /**
     * Parses a line
     * 
     * @param s
     * @param symbolQueue
     * @return
     */
    private static Component parseLine(Symbol s, LinkedList<Symbol> symbolQueue, String workingDirectory, int line) {
        switch(s) {
            case DirectiveSymbol d:
                Component c = parseDirective(symbolQueue, d, workingDirectory);
                
                LOG.finer("Directive resulted in: " + c);
                
                if(c != null) {
                    return c;
                }
                break;
            
            case MnemonicSymbol m:
                Instruction inst = parseInstruction(symbolQueue, m);
                
                if(inst == null) {
                    // placeholder
                    LOG.warning("PARSE INSTRUCTION RETURNED NULL");
                } else {
                    LOG.finer("Parsed instruction: " + inst);
                    return inst;
                }
                break;
                
            default: // do nothing
                LOG.warning("Unknown construct start: " + s + " on line " + line);
        }
        
        return null;
    }

    /**
     * Parse an instruction
     * 
     * @param symbolQueue
     * @return
     */
    private static Instruction parseInstruction(LinkedList<Symbol> symbolQueue, MnemonicSymbol m) {
        // convert to Operation for argument count
        Operation opr = Operation.fromMnemonic(m.name());
        
        int packedSize = 0;
        
        if(m.name().contains("4")) packedSize = 1;
        else if(m.name().contains("8")) packedSize = 2;
        
        // how many arguments does this mnemonic have
        if(!hasFirstOperand(opr)) {
            // no arguments, ez
            return switch(opr) {
                case NOP    -> new Instruction(Opcode.NOP, false);
                case HLT    -> new Instruction(Opcode.HLT, false);
                case RET    -> new Instruction(Opcode.RET, false);
                case IRET   -> new Instruction(Opcode.IRET, false);
                case PUSHA  -> new Instruction(Opcode.PUSHA, false);
                case POPA   -> new Instruction(Opcode.POPA, false);
                default     -> null; // not possible
            };
        } else {
            // parse first operand
            ResolvableLocationDescriptor firstOperand = parseOperand(symbolQueue, true);
            Opcode opcode = null;
            
            if(firstOperand == null) return null;
            
            // operands that aren't registers and have a size are set by the source code
            boolean hasFixedOperandSize = firstOperand.getType() != LocationType.REGISTER &&  firstOperand.getSize() != -1;
            
            if(!hasSecondOperand(opr)) {
                // 1 argument.
                // PUSH, JMP, JCC, CALL, and INT are all source only while the others are destination only
                boolean isImmediate = firstOperand.getType() == LocationType.IMMEDIATE;
                LocationType type = firstOperand.getType();
                Register register = firstOperand.getRegister();
                
                // everything has its own rules
                switch(opr) {
                        // register shortcuts + F
                    case PUSH:
                        if(m.name().startsWith("B")) {
                            // BP
                            if(firstOperand.getSize() == 4) {
                                // Wide
                                opcode = (type == LocationType.IMMEDIATE) ? Opcode.BPUSHW_I32 : Opcode.BPUSHW_RIM;
                            } else {
                                // Normal
                                opcode = switch(type) {
                                    case REGISTER   -> switch(register) {
                                        case SP -> Opcode.BPUSH_SP;
                                        default -> Opcode.BPUSH_RIM;
                                    };
                                    
                                    case IMMEDIATE  -> {
                                        // assuming the immediate size of a push is ill advised
                                        ResolvableValue imm = firstOperand.getImmediate();
                                        int immediateSize = firstOperand.getSize();
                                        
                                        if(immediateSize == -1) throw new IllegalArgumentException("Cannot infer push size for immedate " + imm);
                                        
                                        yield immediateSize == 4 ? Opcode.BPUSHW_I32 : Opcode.BPUSH_RIM;
                                    }
                                    
                                    default -> Opcode.BPUSH_RIM;
                                };
                            }
                        } else {
                            // SP
                            if(firstOperand.getSize() == 4) {
                                // Wide
                                opcode = switch(type) {
                                    case REGISTER   -> switch(register) {
                                        case BP -> Opcode.PUSH_BP;
                                        default -> Opcode.PUSHW_RIM;
                                    };
                                    
                                    case IMMEDIATE  -> Opcode.PUSHW_I32;
                                    
                                    default         -> Opcode.PUSHW_RIM;
                                };
                            } else {
                                // Normal
                                opcode = switch(type) {
                                    case REGISTER   -> switch(register) {
                                        case A  -> Opcode.PUSH_A;
                                        case B  -> Opcode.PUSH_B;
                                        case C  -> Opcode.PUSH_C;
                                        case D  -> Opcode.PUSH_D;
                                        case I  -> Opcode.PUSH_I;
                                        case J  -> Opcode.PUSH_J;
                                        case K  -> Opcode.PUSH_K;
                                        case L  -> Opcode.PUSH_L;
                                        case BP -> Opcode.PUSH_BP;
                                        case F  -> Opcode.PUSH_F;
                                        case PF -> Opcode.PUSH_PF;
                                        default -> Opcode.PUSH_RIM;
                                    };
                                    
                                    case IMMEDIATE  -> {
                                        // assuming the immediate size of a push is ill advised
                                        ResolvableValue imm = firstOperand.getImmediate();
                                        int immediateSize = firstOperand.getSize();
                                        
                                        if(immediateSize == -1) throw new IllegalArgumentException("Cannot infer push size for immedate " + imm);
                                        
                                        yield immediateSize == 4 ? Opcode.PUSHW_I32 : Opcode.PUSH_RIM;
                                    }
                                    
                                    default         -> Opcode.PUSH_RIM;
                                };
                            }
                        }
                        break;
                    
                    case POP:
                        if(m.name().startsWith("B")) {
                            // BP
                            if(firstOperand.getSize() == 4) {
                                opcode = switch(type) {
                                    case REGISTER   -> switch(register) {
                                        case SP -> Opcode.BPOP_SP;
                                        default -> Opcode.BPOPW_RIM;
                                    };
                                    
                                    default     -> Opcode.BPOPW_RIM;
                                };
                            } else {
                                opcode = Opcode.BPOP_RIM;
                            }
                        } else {
                            // SP
                            if(firstOperand.getSize() == 4) {
                                opcode = switch(type) {
                                    case REGISTER   -> switch(register) {
                                        case BP -> Opcode.POP_BP;
                                        default -> Opcode.POPW_RIM;
                                    };
                                    
                                    default     -> Opcode.POPW_RIM;
                                };
                            } else {
                                opcode = switch(type) {
                                    case REGISTER   -> switch(register) {
                                        case A  -> Opcode.POP_A;
                                        case B  -> Opcode.POP_B;
                                        case C  -> Opcode.POP_C;
                                        case D  -> Opcode.POP_D;
                                        case I  -> Opcode.POP_I;
                                        case J  -> Opcode.POP_J;
                                        case K  -> Opcode.POP_K;
                                        case L  -> Opcode.POP_L;
                                        case BP -> Opcode.POP_BP;
                                        case F  -> Opcode.POP_F;
                                        case PF -> Opcode.POP_PF;
                                        default -> Opcode.POP_RIM;
                                    };
                                    
                                    default         -> Opcode.POP_RIM;
                                };
                            }
                        }
                        break;
                    
                        // index register shortcuts
                    case INC:
                        opcode = switch(type) {
                            case REGISTER   -> switch(register) {
                                case I  -> Opcode.INC_I;
                                case J  -> Opcode.INC_J;
                                case K  -> Opcode.INC_K;
                                case L  -> Opcode.INC_L;
                                default -> Opcode.INC_RIM;
                            };
                            
                            default     -> Opcode.INC_RIM;
                        };
                        break;
                        
                    case ICC:
                        opcode = switch(type) {
                            case REGISTER   -> switch(register) {
                                case A  -> Opcode.ICC_A;
                                case B  -> Opcode.ICC_B;
                                case C  -> Opcode.ICC_C;
                                case D  -> Opcode.ICC_D;
                                case I  -> Opcode.ICC_I;
                                case J  -> Opcode.ICC_J;
                                case K  -> Opcode.ICC_K;
                                case L  -> Opcode.ICC_L;
                                default -> Opcode.ICC_RIM;
                            };
                            
                            default     -> Opcode.ICC_RIM;
                        };
                        break;
                        
                    case DEC:
                        opcode = switch(type) {
                            case REGISTER   -> switch(register) {
                                case I  -> Opcode.DEC_I;
                                case J  -> Opcode.DEC_J;
                                case K  -> Opcode.DEC_K;
                                case L  -> Opcode.DEC_L;
                                default -> Opcode.DEC_RIM;
                            };
                            
                            default     -> Opcode.DEC_RIM;
                        };
                        break;
                        
                    case DCC:
                        opcode = switch(type) {
                            case REGISTER   -> switch(register) {
                                case A  -> Opcode.DCC_A;
                                case B  -> Opcode.DCC_B;
                                case C  -> Opcode.DCC_C;
                                case D  -> Opcode.DCC_D;
                                case I  -> Opcode.DCC_I;
                                case J  -> Opcode.DCC_J;
                                case K  -> Opcode.DCC_K;
                                case L  -> Opcode.DCC_L;
                                default -> Opcode.DCC_RIM;
                            };
                            
                            default     -> Opcode.DCC_RIM;
                        };
                        break;
                    
                        // 8, 16, 32 bit immediates
                    case JMP:
                        if(isImmediate) {
                            opcode = switch(firstOperand.getSize()) {
                                case 1  -> Opcode.JMP_I8;
                                case 2  -> Opcode.JMP_I16;
                                default -> Opcode.JMP_I32;
                            };
                        } else {
                            opcode = Opcode.JMP_RIM;
                        }
                        break;
                    
                        // 8 bit immediates
                    case INT:
                        if(isImmediate && firstOperand.getSize() != -1 && firstOperand.getSize() < 2) {
                            opcode = Opcode.INT_I8;
                        } else {
                            opcode = Opcode.INT_RIM;
                        }
                        break;
                        
                        // 16 bit immediates
                    case CALL:
                        if(isImmediate) {
                            opcode = switch(firstOperand.getSize()) {
                                case 1  -> Opcode.CALL_I8;
                                case 2  -> Opcode.CALL_I16;
                                default -> Opcode.CALL_I32;
                            };
                        } else {
                            opcode = Opcode.CALL_RIM;
                        }
                        break;
                        
                        // 32 bit immediates & 32 bit rim
                    case JMPA:
                        opcode = isImmediate ? Opcode.JMPA_I32 : Opcode.JMPA_RIM32;
                        break;
                        
                    case CALLA:
                        opcode = isImmediate ? Opcode.CALLA_I32 : Opcode.CALLA_RIM32;
                        break;
                    
                        // 8 bit immediates & aliases
                    case JCC:
                        boolean isByteImmediate = isImmediate && firstOperand.getSize() == 1;
                        
                        opcode = switch(m.name()) {
                            case "JC", "JB", "JNAE"     -> isByteImmediate ? Opcode.JC_I8 : Opcode.JC_RIM;
                            case "JNC", "JAE", "JNB"    -> isByteImmediate ? Opcode.JNC_I8 : Opcode.JNC_RIM;
                            case "JS"                   -> isByteImmediate ? Opcode.JS_I8 : Opcode.JS_RIM;
                            case "JNS"                  -> isByteImmediate ? Opcode.JNS_I8 : Opcode.JNS_RIM;
                            case "JO"                   -> isByteImmediate ? Opcode.JO_I8 : Opcode.JO_RIM;
                            case "JNO"                  -> isByteImmediate ? Opcode.JNO_I8 : Opcode.JNO_RIM;
                            case "JZ", "JE"             -> isByteImmediate ? Opcode.JZ_I8 : Opcode.JZ_RIM;
                            case "JNZ", "JNE"           -> isByteImmediate ? Opcode.JNZ_I8 : Opcode.JNZ_RIM;
                            case "JA", "JNBE"           -> isByteImmediate ? Opcode.JA_I8 : Opcode.JA_RIM;
                            case "JBE", "JNA"           -> isByteImmediate ? Opcode.JBE_I8 : Opcode.JBE_RIM;
                            case "JG", "JNLE"           -> isByteImmediate ? Opcode.JG_I8 : Opcode.JG_RIM;
                            case "JGE", "JNL"           -> isByteImmediate ? Opcode.JGE_I8 : Opcode.JGE_RIM;
                            case "JL", "JNGE"           -> isByteImmediate ? Opcode.JL_I8 : Opcode.JL_RIM;
                            case "JLE", "JNG"           -> isByteImmediate ? Opcode.JLE_I8 : Opcode.JLE_RIM;
                            default -> throw new IllegalArgumentException("Invalid conditional jump mnemonic " + m.name());
                        };
                        break;
                    
                        // packed rim
                    case PINC:
                        opcode = Opcode.PINC_RIMP;
                        firstOperand.setSize(packedSize, true);
                        break;
                        
                    case PICC:
                        opcode = Opcode.PICC_RIMP;
                        firstOperand.setSize(packedSize, true);
                        break;
                        
                    case PDEC:
                        opcode = Opcode.PDEC_RIMP;
                        firstOperand.setSize(packedSize, true);
                        break;
                        
                    case PDCC:
                        opcode = Opcode.PDCC_RIMP;
                        firstOperand.setSize(packedSize, true);
                        break;
                    
                        // rim + F
                    case NOT:
                        opcode = (type == LocationType.REGISTER && register == Register.F) ? Opcode.NOT_F : Opcode.NOT_RIM; 
                        break;
                    
                        // rim only
                    case NEG:
                        opcode = Opcode.NEG_RIM;
                        break;
                    
                    default: // error
                        throw new IllegalArgumentException("how did we get here? " + m);
                }
                
                // nice and generic-ish
                return switch(opr) {
                    case PUSH, JMP, JMPA, JCC, CALL, CALLA, INT -> new Instruction(opcode, firstOperand, false, hasFixedOperandSize);
                    default                                     -> new Instruction(opcode, firstOperand, true, hasFixedOperandSize);
                };
            } else {
                // we expect a separator, skip it if present
                if(symbolQueue.peek() instanceof SeparatorSymbol) {
                    symbolQueue.poll();
                }
                
                // parse second operand
                boolean canBeMemory = (firstOperand.getType() != LocationType.MEMORY) && (firstOperand.getType() != LocationType.IMMEDIATE);
                ResolvableLocationDescriptor secondOperand = parseOperand(symbolQueue, canBeMemory);
                
                if(secondOperand == null) return null;
                
                hasFixedOperandSize |= secondOperand.getType() != LocationType.REGISTER && secondOperand.getSize() != -1;
                
                // useful values
                LocationType firstType = firstOperand.getType(),
                             secondType = secondOperand.getType();
                
                Register firstRegister = firstOperand.getRegister(),
                         secondRegister = secondOperand.getRegister();
                
                // this section was first written when LocationType included specific registers
                // Because the ResolvableLocationDescriptor's register field is Register.NONE when the type is not a register, there shouldn't be issues with nulls
                
                boolean isImmediate = secondOperand.getType() == LocationType.IMMEDIATE,
                        firstIsABCD = firstRegister == Register.A || firstRegister == Register.B || firstRegister == Register.C || firstRegister == Register.D,
                        secondIsABCD = secondRegister == Register.A || secondRegister == Register.B || secondRegister == Register.C || secondRegister == Register.D,
                        firstIsLByte = firstRegister == Register.AL || firstRegister == Register.BL || firstRegister == Register.CL || firstRegister == Register.DL,
                        //firstIsHByte = firstRegister == Register.AH || firstRegister == Register.BH || firstRegister == Register.CH || firstRegister == Register.DH,
                        secondIsLByte = secondRegister == Register.AL || secondRegister == Register.BL || secondRegister == Register.CL || secondRegister == Register.DL,
                        //secondIsHByte = secondRegister == Register.AH || secondRegister == Register.BH || secondRegister == Register.CH || secondRegister == Register.DH,
                        firstIsFlags = firstRegister == Register.F,
                        firstIsProtected = firstRegister == Register.PF || firstRegister == Register.ISP,
                        secondIsFlags = secondRegister == Register.F,
                        secondIsProtected = secondRegister == Register.PF || secondRegister == Register.ISP;
                
                int firstSize = firstOperand.getSize(),
                    secondSize = secondOperand.getSize(),
                    immediateSize = -1;
                
                // immediate width
                if(isImmediate) {
                    // if it's resolved we know the size, if it's not use the widest and shorten later
                    ResolvableValue imm = secondOperand.getImmediate();
                    secondOperand.getSize();
                    
                    if(imm.isResolved()) {
                        if(secondOperand.getSize() == -1) {
                            immediateSize = getValueWidth(imm.value(), true, false);
                        } else {
                            immediateSize = secondOperand.getSize();
                        }
                    } else {
                        immediateSize = 4;
                    }
                }
                
                // figure out opcode
                switch(opr) {
                        // tons of shortcuts
                    case MOV:
                        // F
                        if(firstIsFlags) {
                            opcode = Opcode.MOV_F_RIM;
                            break;
                        } else if(secondIsFlags) {
                            opcode = Opcode.MOV_RIM_F;
                            break;
                        }
                        
                        // PR
                        if(firstIsProtected) {
                            opcode = Opcode.MOV_PR_RIM;
                            break;
                        } else if(secondIsProtected) {
                            opcode = Opcode.MOV_RIM_PR;
                            break;
                        }
                        
                        // MOVW stuff
                        if(firstSize == 4) {
                            opcode = Opcode.MOVW_RIM;
                            break;
                        } else if(secondSize == 4) {
                            // wide -> memory
                            // BP & SP use normal RIM for this
                            if(secondRegister != Register.BP && secondRegister != Register.SP) {
                                opcode = Opcode.MOVW_RIM;
                            } else {
                                opcode = Opcode.MOV_RIM;
                            }
                            
                            break;
                        }
                        
                        // immediate shortcuts
                        if(isImmediate) {
                            if(firstIsABCD) {
                                // trim to register size
                                if(immediateSize > firstSize) immediateSize = firstSize;
                                
                                // 8/16
                                if(immediateSize == 1) { // 8
                                    opcode = switch(firstRegister) {
                                        case A  -> Opcode.MOVS_A_I8;
                                        case B  -> Opcode.MOVS_B_I8;
                                        case C  -> Opcode.MOVS_C_I8;
                                        case D  -> Opcode.MOVS_D_I8;
                                        default -> Opcode.NOP; // not possible
                                    };
                                } else { // 16/unknown, can be shortened
                                    opcode = switch(firstRegister) {
                                        case A  -> Opcode.MOV_A_I16;
                                        case B  -> Opcode.MOV_B_I16;
                                        case C  -> Opcode.MOV_C_I16;
                                        case D  -> Opcode.MOV_D_I16;
                                        default -> Opcode.NOP; // not possible
                                    };
                                }
                                
                                break;
                            } else if(firstRegister == Register.I) {
                                opcode = Opcode.MOV_I_I16;
                                break;
                            } else if(firstRegister == Register.J) {
                                opcode = Opcode.MOV_J_I16;
                                break;
                            } else if(firstRegister == Register.K) {
                                opcode = Opcode.MOV_K_I16;
                                break;
                            } else if(firstRegister == Register.L) {
                                opcode = Opcode.MOV_L_I16;
                                break;
                            }
                        }
                        
                        // memory shortcuts
                        if(firstIsABCD && secondType == LocationType.MEMORY) {
                            ResolvableMemory mem = secondOperand.getMemory();
                            
                            // base & index?
                            if(mem.getBase() != Register.NONE || mem.getIndex() != Register.NONE) {
                                // offset?
                                if(mem.isResolved() && mem.getOffset().value() == 0) { // definitely not
                                    opcode = switch(firstRegister) {
                                        case A  -> Opcode.MOV_A_BI;
                                        case B  -> Opcode.MOV_B_BI;
                                        case C  -> Opcode.MOV_C_BI;
                                        case D  -> Opcode.MOV_D_BI;
                                        default -> Opcode.NOP; // not possible
                                    };
                                } else { // yes or maybe
                                    opcode = switch(firstRegister) {
                                        case A  -> Opcode.MOV_A_BIO;
                                        case B  -> Opcode.MOV_B_BIO;
                                        case C  -> Opcode.MOV_C_BIO;
                                        case D  -> Opcode.MOV_D_BIO;
                                        default -> Opcode.NOP; // not possible
                                    };
                                }
                            } else {
                                // just offset
                                opcode = switch(firstRegister) {
                                    case A  -> Opcode.MOV_A_O;
                                    case B  -> Opcode.MOV_B_O;
                                    case C  -> Opcode.MOV_C_O;
                                    case D  -> Opcode.MOV_D_O;
                                    default -> Opcode.NOP; // not possible
                                };
                            }
                            
                            break;
                        } else if(firstType == LocationType.MEMORY && secondIsABCD) {
                            ResolvableMemory mem = firstOperand.getMemory();
                            
                            // base & index?
                            if(mem.getBase() != Register.NONE || mem.getIndex() != Register.NONE) {
                                // offset?
                                if(mem.isResolved() && mem.getOffset().value() == 0) { // definitely not
                                    opcode = switch(secondRegister) {
                                        case A  -> Opcode.MOV_BI_A;
                                        case B  -> Opcode.MOV_BI_B;
                                        case C  -> Opcode.MOV_BI_C;
                                        case D  -> Opcode.MOV_BI_D;
                                        default -> Opcode.NOP; // not possible
                                    };
                                } else { // yes or maybe
                                    opcode = switch(secondRegister) {
                                        case A  -> Opcode.MOV_BIO_A;
                                        case B  -> Opcode.MOV_BIO_B;
                                        case C  -> Opcode.MOV_BIO_C;
                                        case D  -> Opcode.MOV_BIO_D;
                                        default -> Opcode.NOP; // not possible
                                    };
                                }
                            } else {
                                // just offset
                                opcode = switch(secondRegister) {
                                    case A  -> Opcode.MOV_O_A;
                                    case B  -> Opcode.MOV_O_B;
                                    case C  -> Opcode.MOV_O_C;
                                    case D  -> Opcode.MOV_O_D;
                                    default -> Opcode.NOP; // not possible
                                };
                            }
                            
                            break;
                        }
                        
                        // register-register shortcuts
                        if(firstType == secondType && ((firstIsABCD && secondIsABCD) || (firstIsLByte && secondIsLByte))) {
                            // nested switch expression because funny
                            opcode = switch(firstRegister) {
                                case A  -> switch(secondRegister) {
                                    case B  -> Opcode.MOV_A_B;
                                    case C  -> Opcode.MOV_A_C;
                                    case D  -> Opcode.MOV_A_D;
                                    default -> Opcode.MOV_RIM;
                                };
                                
                                case B  -> switch(secondRegister) {
                                    case A  -> Opcode.MOV_B_A;
                                    case C  -> Opcode.MOV_B_C;
                                    case D  -> Opcode.MOV_B_D;
                                    default -> Opcode.MOV_RIM;
                                };
                                
                                case C  -> switch(secondRegister) {
                                    case A  -> Opcode.MOV_C_A;
                                    case B  -> Opcode.MOV_C_B;
                                    case D  -> Opcode.MOV_C_D;
                                    default -> Opcode.MOV_RIM;
                                };
                                
                                case D  -> switch(secondRegister) {
                                    case A  -> Opcode.MOV_D_A;
                                    case B  -> Opcode.MOV_D_B;
                                    case C  -> Opcode.MOV_D_C;
                                    default -> Opcode.MOV_RIM;
                                };
                                
                                default -> Opcode.MOV_RIM; // fallback
                            };
                            
                            break;
                        }
                        
                        opcode = Opcode.MOV_RIM;
                        break;
                    
                    case MOVW:
                        // PR
                        if(firstIsProtected) {
                            opcode = Opcode.MOV_PR_RIM;
                            break;
                        } else if(secondIsProtected) {
                            opcode = Opcode.MOV_RIM_PR;
                            break;
                        }
                        
                        opcode = Opcode.MOVW_RIM;
                        break;
                    
                    case MOVS:
                        // shortcuts for ABCD immediate
                        if(isImmediate && firstIsABCD) {
                            opcode = switch(firstRegister) {
                                case A  -> Opcode.MOVS_A_I8;
                                case B  -> Opcode.MOVS_B_I8;
                                case C  -> Opcode.MOVS_C_I8;
                                case D  -> Opcode.MOVS_D_I8;
                                default -> Opcode.NOP; // not possible
                            };
                            
                            break;
                        }
                        
                        opcode = Opcode.MOVS_RIM;
                        break;
                        
                    case XCHG:
                        if(firstOperand.getSize() == 4 || secondOperand.getSize() == 4) {
                            opcode = Opcode.XCHGW_RIM;
                        } else {
                            opcode = Opcode.XCHG_RIM;
                        }
                        break;
                    
                    case XCHGW:
                        opcode = Opcode.XCHGW_RIM;
                        break;
                        
                        // register-immediate shortcuts
                    case ADD:
                        if(isImmediate) {
                            opcode = switch(firstRegister) {
                                case A  -> (immediateSize == 1) ? Opcode.ADD_A_I8 : Opcode.ADD_RIM;
                                case B  -> (immediateSize == 1) ? Opcode.ADD_B_I8 : Opcode.ADD_RIM;
                                case C  -> (immediateSize == 1) ? Opcode.ADD_C_I8 : Opcode.ADD_RIM;
                                case D  -> (immediateSize == 1) ? Opcode.ADD_D_I8 : Opcode.ADD_RIM;
                                case I  -> (immediateSize == 1) ? Opcode.ADD_I_I8 : Opcode.ADD_RIM;
                                case J  -> (immediateSize == 1) ? Opcode.ADD_J_I8 : Opcode.ADD_RIM;
                                case K  -> (immediateSize == 1) ? Opcode.ADD_K_I8 : Opcode.ADD_RIM;
                                case L  -> (immediateSize == 1) ? Opcode.ADD_L_I8 : Opcode.ADD_RIM;
                                case SP -> Opcode.ADD_SP_I8;
                                case BP -> Opcode.ADD_BP_I8;
                                default -> (immediateSize == 1) ? Opcode.ADD_RIM_I8 : Opcode.ADD_RIM;
                            };
                        } else {
                            opcode = Opcode.ADD_RIM;
                        }
                        break;
                        
                    case ADC:
                        if(isImmediate) {
                            opcode = (immediateSize == 1) ? Opcode.ADC_RIM_I8 : Opcode.ADC_RIM;
                        } else {
                            opcode = Opcode.ADC_RIM;
                        }
                        break;
                    
                    case SUB:
                        if(isImmediate) {
                            opcode = switch(firstRegister) {
                                case A  -> (immediateSize == 1) ? Opcode.SUB_A_I8 : Opcode.SUB_RIM;
                                case B  -> (immediateSize == 1) ? Opcode.SUB_B_I8 : Opcode.SUB_RIM;
                                case C  -> (immediateSize == 1) ? Opcode.SUB_C_I8 : Opcode.SUB_RIM;
                                case D  -> (immediateSize == 1) ? Opcode.SUB_D_I8 : Opcode.SUB_RIM;
                                case I  -> (immediateSize == 1) ? Opcode.SUB_I_I8 : Opcode.SUB_RIM;
                                case J  -> (immediateSize == 1) ? Opcode.SUB_J_I8 : Opcode.SUB_RIM;
                                case K  -> (immediateSize == 1) ? Opcode.SUB_K_I8 : Opcode.SUB_RIM;
                                case L  -> (immediateSize == 1) ? Opcode.SUB_L_I8 : Opcode.SUB_RIM;
                                case SP -> Opcode.SUB_SP_I8;
                                case BP -> Opcode.SUB_BP_I8;
                                default -> (immediateSize == 1) ? Opcode.SUB_RIM_I8 : Opcode.SUB_RIM;
                            };
                        } else {
                            opcode = Opcode.SUB_RIM;
                        }
                        break;
                        
                    case SBB:
                        if(isImmediate) {
                            opcode = (immediateSize == 1) ? Opcode.SBB_RIM_I8 : Opcode.SBB_RIM;
                        } else {
                            opcode = Opcode.SBB_RIM;
                        }
                        break;
                        
                        // RIM-immediate shortcuts
                    case CMP:
                        if(isImmediate) {
                            ResolvableValue imm = secondOperand.getImmediate();
                            
                            if(imm.isResolved() && imm.value() == 0) {
                                opcode = Opcode.CMP_RIM_0;
                            } else if(immediateSize == 1) {
                                opcode = Opcode.CMP_RIM_I8;
                            } else {
                                opcode = Opcode.CMP_RIM;
                            }
                        } else {
                            opcode = Opcode.CMP_RIM;
                        }
                        break;
                        
                        // RIM + F
                    case AND:
                        if(firstIsFlags) {
                            opcode = Opcode.AND_F_RIM;
                        } else if(secondIsFlags) {
                            opcode = Opcode.AND_RIM_F;
                        } else {
                            opcode = Opcode.AND_RIM;
                        }
                        break;
                        
                    case OR:
                        if(firstIsFlags) {
                            opcode = Opcode.OR_F_RIM;
                        } else if(secondIsFlags) {
                            opcode = Opcode.OR_RIM_F;
                        } else {
                            opcode = Opcode.OR_RIM;
                        }
                        break;
                        
                    case XOR:
                        if(firstIsFlags) {
                            opcode = Opcode.XOR_F_RIM;
                        } else if(secondIsFlags) {
                            opcode = Opcode.XOR_RIM_F;
                        } else {
                            opcode = Opcode.XOR_RIM;
                        }
                        break;
                    
                    case SHL:
                        if(isImmediate & immediateSize == 1) {
                            opcode = Opcode.SHL_RIM_I8;
                        } else {
                            opcode = Opcode.SHL_RIM;
                        }
                        break;
                    
                    case SHR:
                        if(isImmediate & immediateSize == 1) {
                            opcode = Opcode.SHR_RIM_I8;
                        } else {
                            opcode = Opcode.SHR_RIM;
                        }
                        break;
                    
                    case SAR:
                        if(isImmediate & immediateSize == 1) {
                            opcode = Opcode.SAR_RIM_I8;
                        } else {
                            opcode = Opcode.SAR_RIM;
                        }
                        break;
                    
                    case ROL:
                        if(isImmediate & immediateSize == 1) {
                            opcode = Opcode.ROL_RIM_I8;
                        } else {
                            opcode = Opcode.ROL_RIM;
                        }
                        break;
                        
                    case ROR:
                        if(isImmediate & immediateSize == 1) {
                            opcode = Opcode.ROR_RIM_I8;
                        } else {
                            opcode = Opcode.ROR_RIM;
                        }
                        break;
                    
                    case RCL:
                        if(isImmediate & immediateSize == 1) {
                            opcode = Opcode.RCL_RIM_I8;
                        } else {
                            opcode = Opcode.RCL_RIM;
                        }
                        break;
                        
                    case RCR:
                        if(isImmediate & immediateSize == 1) {
                            opcode = Opcode.RCR_RIM_I8;
                        } else {
                            opcode = Opcode.RCR_RIM;
                        }
                        break;
                        
                        // just RIM? convert directly
                    default:
                        String oprn = opr.toString();
                        
                        if(oprn.startsWith("P")) { // packed
                            opcode = Opcode.valueOf(oprn + "_RIMP");
                            secondOperand.setSize(packedSize, true);
                            
                            if(firstOperand.getType() == LocationType.REGISTER) {
                                switch(firstOperand.getRegister()) {
                                    case A, B, C, D, I, J, K, L, DA, AB, BC, CD, JI, LK: break;
                                    default: throw new IllegalArgumentException("Invalid packed register: " + firstOperand);
                                }
                            }
                            
                            if(secondOperand.getType() == LocationType.REGISTER) {
                                switch(secondOperand.getRegister()) {
                                    case A, B, C, D, I, J, K, L, DA, AB, BC, CD, JI, LK: break;
                                    default: throw new IllegalArgumentException("Invalid packed register: " + secondOperand);
                                }
                            }
                        } else { // not packed
                            opcode = Opcode.valueOf(oprn + "_RIM");
                        }
                        
                        break;
                }
                
                // CMOVCC uses its own constructor
                if(opcode == Opcode.CMOVCC_RIM || opcode == Opcode.PCMOVCC_RIMP) {
                    String condition = m.name().substring(m.name().startsWith("C") ? 4 : 6);
                    
                    Opcode conditionOp = switch(condition) {
                        case "C", "B", "NAE"	-> Opcode.JC_RIM;
                        case "NC", "AE", "NB"	-> Opcode.JNC_RIM;
                        case "S"                -> Opcode.JS_RIM;
                        case "NS"               -> Opcode.JNS_RIM;
                        case "O"                -> Opcode.JO_RIM;
                        case "NO"               -> Opcode.JNO_RIM;
                        case "Z", "E"           -> Opcode.JZ_RIM;
                        case "NZ", "NE"         -> Opcode.JNZ_RIM;
                        case "A", "NBE"         -> Opcode.JA_RIM;
                        case "BE", "NA"         -> Opcode.JBE_RIM;
                        case "G", "NLE"         -> Opcode.JG_RIM;
                        case "GE", "NL"         -> Opcode.JGE_RIM;
                        case "L", "NGE"         -> Opcode.JL_RIM;
                        case "LE", "NG"         -> Opcode.JLE_RIM;
                        default -> throw new IllegalArgumentException("Invalid conditional move mnemonic " + m.name());
                    };
                    
                    return new Instruction(opcode, firstOperand, secondOperand, conditionOp.getOp(), hasFixedOperandSize);
                } else {
                    return new Instruction(opcode, firstOperand, secondOperand, hasFixedOperandSize);
                }
            }
        }
    }
    
    /**
     * Parse an operand
     * 
     * @return
     */
    private static ResolvableLocationDescriptor parseOperand(LinkedList<Symbol> symbolQueue, boolean canBeMemory) {
        Symbol nextSymbol = symbolQueue.poll();
        LOG.finest("Parsing operand with symbol: " + nextSymbol);
        
        // what we workin with
        switch(nextSymbol) {
                // actually ez now that the right things are expressive
            case RegisterSymbol rs:
                return new ResolvableLocationDescriptor(LocationType.REGISTER, Register.valueOf(rs.name()));
                
                // resolved constant
            case ConstantSymbol cs:
                return new ResolvableLocationDescriptor(LocationType.IMMEDIATE, -1, new ResolvableConstant(cs.value())); 
                
                // just a name = unresolved constant
            case NameSymbol ns:
                return new ResolvableLocationDescriptor(LocationType.IMMEDIATE, -1, new ResolvableConstant(ns.name()));
            
                // memory
            case MemorySymbol ms:
                return parseMemory(ms);
            
                // expression
            case ExpressionSymbol es:
                return parseOperandExpression(es);
                
                // size prefix overrides operand size
            case SizeSymbol ss:
                ResolvableLocationDescriptor rld = parseOperand(symbolQueue, canBeMemory);
                if(rld == null) return null;
                rld.setSize(convertSize(ss), false);
                return rld;
            
            case SpecialCharacterSymbol scs:
                // these will be expressions if they're not on their own
                if(scs.character() == '$' || scs.character() == '@') {
                    return new ResolvableLocationDescriptor(LocationType.IMMEDIATE, -1, new ResolvableConstant(scs.character() + ""));
                }
                
                throw new IllegalArgumentException("Invalid symbol while parsing operand: " + scs);
            
            case StringSymbol sts:
                // if it's one character its good
                if(sts.value().length() == 1) {
                    return new ResolvableLocationDescriptor(LocationType.IMMEDIATE, -1, new ResolvableConstant(sts.value().charAt(0)));
                }
                
                throw new IllegalArgumentException("Invalid symbol while parsing operand: " + sts);
                
            case Symbol s:
                throw new IllegalArgumentException("Invalid symbol while parsing operand: " + s);
        }
    }
    
    /**
     * Conevrts a SizeSymbol name into a size
     * 
     * @param ss
     * @return
     */
    private static int convertSize(SizeSymbol ss) {
        return switch(ss.name()) {
            case "BYTE"         -> 1;
            case "WORD"         -> 2;
            case "DWORD", "PTR" -> 4;
            default -> throw new IllegalArgumentException("Invalid size: " + ss);
        };
    }
    
    /**
     * Parses a memory symbol
     * 
     * @param ms
     * @return
     */
    private static ResolvableLocationDescriptor parseMemory(MemorySymbol ms) {
        LOG.finest("Parsing memory: " + ms);
        
        /*
         * Memory
         * 0-1 32 bit register base
         * 0-1 16 bit register index, possibly with scale in the form [1,2,4,8]*reg or reg*[1,2,4,8]
         * a constant expression offset
         * with all items separated by +
         */
        
        // defaults
        Register base = Register.NONE,
                 index = Register.NONE;
        
        int scale = 1;
        
        ResolvableValue offset = new ResolvableConstant(0);
        
        boolean assignedBase = false,
                assignedIndex = false,
                assignedOffset = false;
        
        /*
         * consuming each symbol in the MemorySymbol group, assign registers to base/index and put
         * everything else into an ExpressionSymbol which should get parsed into a value
         */
        
        List<Symbol> symbols = ms.symbols();
        ArrayList<Symbol> exprSymbols = new ArrayList<>();
        
        // simplify checks
        Set<Long> scales = Set.of(1l, 2l, 4l, 8l);
        Set<String> reg16s = Set.of("A", "B", "C", "D", "I", "J", "K", "L"),
                    reg32s = Set.of("A", "B", "C", "D", "I", "J", "K", "L", "BP", "SP", "IP");
        
        for(int i = 0; i < symbols.size(); i++) {
            switch(symbols.get(i)) {
                case ConstantSymbol cs:
                    // for a constant, we're looking for [1,2,4,8]*reg16
                    if(scales.contains(cs.value()) &&
                       symbols.size() >= i + 3 &&
                       symbols.get(i + 1) instanceof SpecialCharacterSymbol scs && scs.character() == '*' &&
                       symbols.get(i + 2) instanceof RegisterSymbol rs && reg16s.contains(rs.name())) {
                        // found
                        if(assignedIndex) throw new IllegalArgumentException("Duplicate index in memory: " + rs);
                        
                        index = Register.valueOf(rs.name());
                        scale = (int) cs.value();
                        assignedIndex = true;
                        i += 2;
                    } else {
                        exprSymbols.add(cs);
                        assignedOffset = true;
                    }
                    break;
                    
                case RegisterSymbol rs:
                    // is it a valid register
                    if(reg32s.contains(rs.name())) {
                        // index or A:B base
                        if(reg16s.contains(rs.name())) {
                            // do we have enough symbols for scale or base
                            if(symbols.size() >= i + 3) {
                                // is it reg16*[1,2,4,8]
                                if(symbols.get(i + 1) instanceof SpecialCharacterSymbol scs && scs.character() == '*' &&
                                   symbols.get(i + 2) instanceof ConstantSymbol cs && scales.contains(cs.value())) {
                                    // yes
                                    if(assignedIndex)  throw new IllegalArgumentException("Duplicate index in memory: " + rs);
                                    
                                    index = Register.valueOf(rs.name());
                                    scale = (int) cs.value();
                                    assignedIndex = true;
                                    i += 2;
                                } else {
                                    // defer to other function
                                    Register r = parseRegister(symbols, i);
                                    
                                    switch(r) {
                                            // index
                                        case A, B, C, D, I, J, K, L:
                                            if(assignedIndex) throw new IllegalArgumentException("Duplicate index in memory: " + rs);
                                        
                                            index = r;
                                            scale = 1;
                                            assignedIndex = true;
                                            break;
                                        
                                            // base
                                        case DA, AB, BC, CD, JI, LK: // separate case cause :
                                            i += 2;
                                            
                                        case BP, SP:
                                            if(assignedBase) throw new IllegalArgumentException("Duplicate base in memory: " + rs);
                                        
                                            base = r;
                                            assignedBase = true;
                                            break;
                                        
                                            // invalid
                                        default:
                                            throw new IllegalArgumentException("Invalid register in memory: " + rs);
                                    }
                                }
                            } else {
                                // must be index
                                if(assignedIndex) throw new IllegalArgumentException("Duplicate index in memory: " + rs);
                                
                                index = Register.valueOf(rs.name());
                                scale = 1;
                                assignedIndex = true;
                            }
                        } else { // base
                            if(assignedBase) throw new IllegalArgumentException("Duplicate base in memory: " + rs);
                            
                            base = Register.valueOf(rs.name());
                            assignedBase = true;
                        }
                    } else throw new IllegalArgumentException("Invalid register in memory: " + rs);
                    break;
                    
                case SpecialCharacterSymbol scs:
                    exprSymbols.add(scs);
                    if(scs.character() != '+') assignedOffset = true;
                    break;
                
                case Symbol s:
                    // symbols without specific stuff get thrown in the offset expression
                    exprSymbols.add(s);
                    assignedOffset = true;
            }
        }
        
        // offset?
        if(assignedOffset) {
            // parse the rest
            offset = parseMemoryExpression(exprSymbols);
            
            if(offset == null) {
                throw new IllegalArgumentException("Invalid offset expression parse: " + offset);
            }
        }
        
        
        return new ResolvableLocationDescriptor(LocationType.MEMORY, -1, new ResolvableMemory(base, index, scale, offset));
    }
    
    /**
     * Parses a register from a SymbolGroup
     * Since constructions like A:B are expressive, they're only found in groups
     * Assumes the Symbol at the specified index of the list is a RegisterSymbol 
     * 
     * @param symbols
     * @param index
     * @return
     */
    private static Register parseRegister(List<Symbol> symbols, int index) {
        String name = ((RegisterSymbol) symbols.get(index)).name();
        
        return switch(name) {
            // cannot be an A:B construction
            case "AH", "AL", "BH", "BL", "CH", "CL", "DH", "DL",
                 "BP", "SP", "F" -> Register.valueOf(name);
            
            default -> {
                // cannot contain
                if(symbols.size() < index + 3) {
                    yield Register.valueOf(name);
                } else if(symbols.get(index + 1) instanceof SpecialCharacterSymbol scs && scs.character() == ':' &&
                          symbols.get(index + 2) instanceof RegisterSymbol rs) {
                    String n2 = rs.name();
                    
                    Register reg = switch(name) {
                        case "A"    -> n2.equals("B") ? Register.AB : null;
                        case "B"    -> n2.equals("C") ? Register.BC : null;
                        case "C"    -> n2.equals("D") ? Register.CD : null;
                        case "D"    -> n2.equals("A") ? Register.DA : null;
                        case "I"    -> null;
                        case "J"    -> n2.equals("I") ? Register.JI : null;
                        case "K"    -> null;
                        case "L"    -> n2.equals("K") ? Register.LK :  null;
                        default     -> null;
                    };
                    
                    if(reg == null) throw new IllegalArgumentException("Invalid register pair: " + name + ":" + rs.name());
                    yield reg;
                } else {
                    yield Register.valueOf(name);
                }
            }
        };
    }
    
    /**
     * Parses an expression in the context of memory. Mainly creates a queue and passes things off
     * to the {@link ConstantExpressionParser} class
     * 
     * @param expr
     * @return
     */
    private static ResolvableValue parseMemoryExpression(List<Symbol> expr) {
        LOG.finest("Parsing memory expression: " + expr);
        
        return ConstantExpressionParser.parse(new LinkedList<Symbol>(expr));
    }
    
    /**
     * Parses an expression in the context of an operand
     * 
     * @param es
     * @return
     */
    private static ResolvableLocationDescriptor parseOperandExpression(ExpressionSymbol es) {
        LOG.finest("Parsing operand expression: " + es);
        
        // double registers
        if(es.symbols().get(0) instanceof RegisterSymbol) return new ResolvableLocationDescriptor(LocationType.REGISTER, parseRegister(es.symbols(), 0));
        
        // TODO i think there are non-constant cases for this?
        return new ResolvableLocationDescriptor(LocationType.IMMEDIATE, -1, ConstantExpressionParser.parse(new LinkedList<Symbol>(es.symbols())));
    }
    
    /**
     * Determine if op has a first operand
     * 
     * @param op
     * @return true if op has a first operand
     */
    private static boolean hasFirstOperand(Operation op) {
        return switch(op) {
            case NOP, PUSHA, POPA, RET, IRET, HLT -> false;
            default -> true;
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
                 INT, JCC -> false;
                 
            default -> true;
        };
    }
    
    /**
     * Parses a DirectiveSymbol
     * 
     * @param symbolQueue
     * @param ds
     * @return
     */
    private static Component parseDirective(LinkedList<Symbol> symbolQueue, DirectiveSymbol ds, String workingDirectory) {
        // keywords ordered by reserved_words.txt
        switch(ds.name()) {
            // reserve bytes
            case "RESB":
                return reserveData(symbolQueue.poll(), 1);
            
            // reserve words
            case "RESW":
                return reserveData(symbolQueue.poll(), 2);
            
            // reserve doublewords/pointers
            case "RESD":
            case "RESP":
                return reserveData(symbolQueue.poll(), 4);
            
            // define bytes
            case "DB":
                return defineData(symbolQueue, 1);
            
            // define words
            case "DW":
                return defineData(symbolQueue, 2);
            
            // define doublewords/pointers
            case "DD":
            case "DP":
                return defineData(symbolQueue, 4);
            
            // repeat the given line x times
            case "REPEAT":
                return repeatComponent(symbolQueue, workingDirectory);
            
            // include a file's binary
            case "INCBIN":
                return includeBinary(symbolQueue, workingDirectory);
            
            // %DEFINE is included here as definitions are handled by AssemblerLib
            // AS is expressive and part of the import/include construct so shouldn't be passed
            default:
                throw new IllegalArgumentException("Invalid directive: " + ds);
        }
    }
    
    /**
     * Includes a binary file's contents
     */
    private static InitializedData includeBinary(LinkedList<Symbol> symbolQueue, String workingDirectory) {
        Symbol s = symbolQueue.poll();
        
        String fileName = switch(s) {
            case NameSymbol ns      -> ns.name();
            case StringSymbol ss    -> ss.value();
            default -> throw new IllegalArgumentException("Invalid library description");
        };
        
        // working directory
        if(!new File(fileName).isAbsolute()) fileName = workingDirectory + fileName;
        
        return new InitializedData(fileName);
    }
    
    /**
     * Parses a REPEAT directive's contents
     * 
     * @param symbolQueue
     * @return
     */
    private static Component repeatComponent(LinkedList<Symbol> symbolQueue, String workingDirectory) {
        // we expect an expression that evaluates to the number of repetitions, a separator, and a line we can send to the normal parser
        Symbol s = symbolQueue.poll();
        ResolvableValue reps = switch(s) {
            case ExpressionSymbol expr  -> ConstantExpressionParser.parse(new LinkedList<Symbol>(expr.symbols()));
            case ConstantSymbol c       -> new ResolvableConstant(c.value());
            default -> throw new IllegalArgumentException("Invalid repetition count: " + s);
        };
        
        if(symbolQueue.peek() instanceof SeparatorSymbol) symbolQueue.poll();
        
        return new Repetition(parseLine(symbolQueue.poll(), symbolQueue, workingDirectory, -1), reps);
    }
    
    /**
     * Defines a series of words of the given size
     * 
     * @param symbolQueue
     * @param wordSize
     * @return
     */
    private static Component defineData(LinkedList<Symbol> symbolQueue, int wordSize) {
        List<ResolvableValue> data = new ArrayList<>();
        
        loop: // label because switch funni
        while(true) {
            switch(symbolQueue.peek()) {
                case ConstantSymbol cs:
                    data.add(new ResolvableConstant(cs.value()));
                    break;
                
                case ExpressionSymbol es:
                    data.add(ConstantExpressionParser.parse(es.symbols()));
                    break;
                
                case NameSymbol ns:
                    data.add(new ResolvableConstant(ns.name()));
                    break;
                
                case SeparatorSymbol sep:
                    break; // ignore
                
                case StringSymbol ss:
                    String str = ss.value();
                    int i = 0;
                    
                    while(i < str.length()) {
                        int v = 0;
                        for(int j = 0; j < wordSize; j++, i++) {
                            char c = str.charAt(i);
                            v = (v << 8) | (c & 0xFF);
                        }
                        
                        data.add(new ResolvableConstant(v));
                    }
                    break;
                
                case null:
                default: // anything unexpected just ends the definition
                    break loop;
            }
            
            symbolQueue.poll();
        }
        
        return new InitializedData(data, wordSize);
    }
    
    /**
     * Tries to convert the given Symbol into a constant and returns an UninitializedData component
     * with that many of the given word size
     * 
     * @param s
     * @param wordSize
     * @return
     */
    private static Component reserveData(Symbol s, int wordSize) {
        switch(s) {
            case ConstantSymbol cs:
                return new UninitializedData((int) cs.value(), wordSize);
            
            case ExpressionSymbol es:
                ResolvableValue val = ConstantExpressionParser.parse(es.symbols());
                if(!val.isResolved()) throw new IllegalArgumentException("Reservations cannot be unresolved");
                return new UninitializedData((int) val.value(), wordSize);
            
            default:
                throw new IllegalArgumentException("Invalid symbol for reservation: " + s);
        }
    }
    
    /**
     * Checks an expression for invalid labels
     * 
     * @param rei
     * @param labelIndexMap
     */
    private static void checkExpressionValidity(ResolvableExpression rei, Map<String, Integer> labelIndexMap) {
        ResolvableExpression re = (ResolvableExpression) rei.copy();
        List<ResolvableConstant> unresolvedValues = getUnresolvedConstants(re);
        
        // check that all labels are internal
        for(ResolvableConstant rc : unresolvedValues) {
            String n = rc.getName();
            
            if(!labelIndexMap.containsKey(n) && !n.equals("$") && !n.equals("@")) {
                throw new IllegalArgumentException("Unknown or external label " + n + " in expression " + re);
            }
        }
        
        // check that all label math is offsets
        List<ResolvableExpression> sums = getUnresolvedSums(re);
        
        for(ResolvableExpression ex : sums) {
            // since this is a sum, it can be arranged in any order
            // as long as the number of positive labels is equal to the number of negative labels, it is valid
            int c = countLabelSigns(ex);
            
            if(c != 0) throw new IllegalArgumentException("Unresolved expression cannot be made into offset: sub-expression " + ex + " of " + rei);
        }
    }
    
    /**
     * Counts the signs of references. positive = +1 negative = -1;
     * @param expr
     * @return
     */
    private static int countLabelSigns(ResolvableExpression expr) {
        ResolvableValue left = expr.getLeft(),
                        right = expr.getRight();
        int count = 0;
        
        // right side
        if(right instanceof ResolvableConstant rcr && !rcr.isResolved()) count++;
        else if(right instanceof ResolvableExpression rer) count += countLabelSigns(rer);
        
        // if the operator is SUBTRACT, invert right side count
        if(expr.getOperation() == Operator.SUBTRACT) count = -count;
        
        // left side 
        if(left instanceof ResolvableConstant rcl && !rcl.isResolved()) count++;
        else if(left instanceof ResolvableExpression rel) count += countLabelSigns(rel);
        
        return count;
    }
    
    /**
     * Determines whethere a reference is to a library
     * 
     * @param ref
     * @param libNames
     * @return
     */
    private static boolean isLibraryReference(String ref, Set<String> libNames) {
        for(String lib : libNames) {
            if(ref.startsWith(lib + ".")) return true;
        }
        
        return false;
    }
    
    /**
     * Doing math on absolute labels isn't possible with relocation, but you can do math on offsets.
     * This method extracts sub-expressions which only add or subtract and contain unresolved labels
     * 
     * @param expr
     * @return
     */
    private static List<ResolvableExpression> getUnresolvedSums(ResolvableExpression expr) {
        if(expr.isSum()) return List.of(expr);
        List<ResolvableExpression> exps = new LinkedList<>();
        
        if(expr.isResolved()) return exps;
        
        if(expr.getLeft() instanceof ResolvableExpression rel) exps.addAll(getUnresolvedSums(rel));
        if(expr.getRight() instanceof ResolvableExpression rer) exps.addAll(getUnresolvedSums(rer));
        
        return exps;
    }
    
    /**
     * Collects and returns all unresolved constants from an expression
     * 
     * @param re
     * @return
     */
    private static List<ResolvableConstant> getUnresolvedConstants(ResolvableExpression expr) {
        ResolvableValue left = expr.getLeft(),
                        right = expr.getRight();
        
        List<ResolvableConstant> consts = new LinkedList<>();
        
        switch(left) {
            case ResolvableConstant rc:
                if(!rc.isResolved()) consts.add(rc);
                break;
            
            case ResolvableExpression re:
                consts.addAll(getUnresolvedConstants(re));
                break;
            
            default:
                throw new IllegalArgumentException("Invalid ResolvableValue: " + left);
        }
        
        switch(right) {
            case ResolvableConstant rc:
                if(!rc.isResolved()) consts.add(rc);
                break;
            
            case ResolvableExpression re:
                consts.addAll(getUnresolvedConstants(re));
                break;
            
            default:
                throw new IllegalArgumentException("Invalid ResolvableValue: " + right);
        }
        
        return consts;
    }
    
    /**
     * Resolves the value of a ResolvableValue
     * 
     * @param rv
     * @param labelAddressMap
     * @param libraries
     * @param incomingReferences
     * @param relative
     * @param address
     * @return true if the value resulted in a relocation entry
     */
    private static boolean resolveValue(ResolvableValue rv, Map<String, Integer> labelAddressMap, Set<String> libraries, HashMap<String, List<Integer>> incomingReferences, String fileName, int baseAddress, int valueOffset, int size, boolean relative, boolean inExpression, int lastInstructionAddress) {
        boolean relocated = false;
        
        int address = baseAddress + (relative ? size : valueOffset);
        
        try {
            switch(rv) {
                case ResolvableExpression re:
                    ResolvableValue left = re.getLeft(),
                                    right = re.getRight();
                    
                    if(!left.isResolved()) resolveValue(left, labelAddressMap, libraries, incomingReferences, fileName, baseAddress, valueOffset, size, false, true, lastInstructionAddress);
                    if(!right.isResolved()) resolveValue(right, labelAddressMap, libraries, incomingReferences, fileName, baseAddress, valueOffset, size, false, true, lastInstructionAddress);
                    break;
                    
                case ResolvableConstant rc:
                    if(rc.isResolved()) {
                        return false;
                    }
                    
                    String name = rc.getName();
                    
                    // special relative symbols
                    if(name.equals("$")) {
                        rc.setValue(baseAddress + size);
                        return false;
                    } else if(name.equals("@")) {
                        rc.setValue(lastInstructionAddress);
                        return false;
                    }
                    
                    if(!inExpression && !relative) {
                        if(!isLibraryReference(name, libraries)) {
                            name = fileName + "." + name;
                        }
                        
                        LOG.finest("Adding incoming reference " + name + " at address " + address);
                        
                        // if we're not in an expression and it's not relative, it'll change when relocated
                        // also could be an external reference
                        if(incomingReferences.containsKey(name)) {
                            incomingReferences.get(name).add(address);
                        } else {
                            List<Integer> refs = new LinkedList<>();
                            refs.add(address);
                            incomingReferences.put(name, refs);
                        }
                        
                        relocated = true;
                    } else if(relative) {
                        // relatives are relative
                        rc.setValue(labelAddressMap.get(name) - address);
                    } else {
                        // use the value if we do math on it
                        rc.setValue(labelAddressMap.get(name));
                    }
                    break;
                
                default:
            }
        } catch(NullPointerException e) {
            // the label was not found, ignore
        }
        
        return relocated;
    }
    
    /**
     * Gets the width of a value in bytes
     * 
     * @param v
     * @param one true if 1 is allowed
     * @param three true if 3 is allowed
     * @return
     */
    private static int getValueWidth(long v, boolean one, boolean three) {
        if((v & 0x0000_0000_FFFF_FF80l) == 0l || (v & 0x0000_0000_FFFF_FF80l) == 0x0000_0000_FFFF_FF80l) { // 1 byte
            return one ? 1 : 2;
        } else if((v & 0x0000_0000_FFFF_8000l) == 0l || (v & 0x0000_0000_FFFF_8000l) == 0x0000_0000_FFFF_8000l) { // 2 bytes
            return 2;
        } else if((v & 0x0000_0000_FF80_0000l) == 0l || (v & 0x0000_0000_FF80_0000l) == 0x0000_0000_FF80_0000l) { // 3 bytes
            return three ? 3 : 4;
        } else {
            return 4;
        }
    }
    
    /**
     * Returns true if the value can be zero extended from the given number of bytes
     * 
     * @param v
     * @param len
     * @return
     */
    private static boolean canZeroExtend(long v, int len) {
        int mask = 0xFFFF_FFFF << (len * 8);
        
        return (v & mask) == 0;
    }
}
