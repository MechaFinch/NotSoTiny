package notsotiny.asm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
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
import asmlib.util.relocation.RelocatableObject;
import asmlib.util.relocation.Relocator;
import asmlib.util.relocation.RelocatableObject.Endianness;
import notsotiny.asm.components.Component;
import notsotiny.asm.components.InitializedData;
import notsotiny.asm.components.Instruction;
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
        if(args.length < 1 || args.length > 4) {
            System.out.println("Usage: Assembler [-o] <input file> [<executable file> <executable entry point>]");
            System.out.println("Flags:");
            System.out.println("\t-o\tOptimize instruction width. If enabled, immediate widths are minimized. This restricts some expressions.");
            System.exit(0);
        }
        
        boolean optimize = args[0].equals("-o"); 
        
        // from the VeryTinyAssembler
        List<RelocatableObject> objects = assemble(new File(args[optimize ? 1 : 0]), optimize);
        
        // write object files
        String directory = new File(args[optimize ? 1 : 0]).getAbsolutePath();
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
        if(args.length == (optimize ? 4 : 3)) {
            try(PrintWriter execWriter = new PrintWriter(directory + args[optimize ? 2 : 1])) {
                // entry point
                execWriter.println("#entry " + args[optimize ? 3 : 2]);
                
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
    public static List<RelocatableObject> assemble(File f, boolean optimizeInstructionWidth) throws IOException {
        LOG.info("Assembling from main file: " + f);
        
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
                
                obj = assembleObject(symbols, files, file, optimizeInstructionWidth);
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
    private static RenameableRelocatableObject assembleObject(List<Symbol> symbols, List<String> includedFiles, File file, boolean optimizeInstructionWidth) throws IOException {
        LOG.fine("Assembling file: " + file);
        
        int line = 0; // line in file
        
        // these variables are from the verytiny implementation
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
                switch(s) {
                    case DirectiveSymbol d:
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
                                    } else throw new IllegalArgumentException("Invalid library description");
                                } else throw new IllegalArgumentException("Invalid library description");
                                
                                // working directory
                                if(!new File(fileName).isAbsolute()) fileName = workingDirectory + fileName;
                                
                                // add library
                                LOG.finer("Added file " + fileName + " as " + libName);
                                includedFiles.add(fileName);
                                libraryNamesMap.put(new File(fileName), libName);
                                break;
                                
                            // set library name
                            case "%LIBNAME":
                                // expecting a name
                                libraryName = switch(symbolQueue.poll()) {
                                    case NameSymbol ns      -> ns.name();
                                    case StringSymbol ss    -> ss.value();
                                    default -> throw new IllegalArgumentException("Invalid library naem");
                                };
                                break;
                            
                            // code directives
                            default:
                                Component c = parseDirective(symbolQueue, d);
                                
                                LOG.finer("Directive resulted in: " + c);
                                
                                if(c != null) {
                                    allInstructions.add(c);
                                    if(!c.isResolved()) unresolvedInstructions.add(c);
                                } else encounteredError = true;
                        }
                        break;
                        
                        // explicit label
                        // add it to the map
                    case LabelSymbol l:
                        labelIndexMap.put(l.name(), allInstructions.size());
                        LOG.finer("Added label " + l.name() + " at index " + allInstructions.size());
                        break;
                        
                        // implicit label
                    case NameSymbol n:
                        labelIndexMap.put(n.name(), allInstructions.size());
                        LOG.finer("Added label " + n.name() + " at index " + allInstructions.size());
                        break;
                    
                    case MnemonicSymbol m:
                        Instruction inst = parseInstruction(symbolQueue, m);
                        
                        if(inst == null) {
                            // placeholder
                            LOG.warning("PARSE INSTRUCTION RETURNED NULL");
                            encounteredError = true;
                        } else {
                            LOG.finer("Parsed instruction: " + inst);
                            allInstructions.add(inst);
                            if(!inst.isResolved()) unresolvedInstructions.add(inst);
                        }
                        break;
                        
                    default: // do nothing
                        LOG.warning("Unknown construct start: " + s + " on line " + line);
                        encounteredError = true;
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
        
        LOG.fine("-- MAIN PARSE RESULTS --");
        LOG.fine(allInstructions.toString());
        LOG.fine(unresolvedInstructions.toString());
        LOG.fine(labelIndexMap.toString());
        
        if(encounteredError) {
            throw new IllegalStateException("Encountered error(s)");
        }
        
        /*
         * CONSTANT RESOLUTION
         */
        
        NavigableMap<String, Integer> labelAddressMap = new TreeMap<>();        // label -> address
        NavigableMap<Integer, Integer> instructionAddressMap = new TreeMap<>(); // index in allInstructions -> address
        Set<String> libNames = new HashSet<>(libraryNamesMap.values());
        
        // build initial address map
        int addr = 0;
        for(int i = 0; i < allInstructions.size(); i++) {
            instructionAddressMap.put(i, addr);
            
            Component c = allInstructions.get(i);
            
            // check for invalid library references
            // we can't do math on a value we cannot access by definition
            if(!c.isResolved()) {
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
                    
                    default: // not possible
                }
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
        
        LOG.fine("-- CONSTANT RESOLUTION FIRST PASS RESULTS --");
        LOG.fine(labelAddressMap.toString());
        
        // attempt to minimize parameter sizes (if enabled)
        if(optimizeInstructionWidth) {
            // TODO
        }
        
        // resolve everything
        addr = 0;
        for(int i = 0; i < allInstructions.size(); i++) {
            Component c = allInstructions.get(i);
            int len = c.getSize();
            
            if(!c.isResolved()) {
                String before = c.toString();
                boolean relocated = false;
                
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
                                    
                                    relocated |= resolveValue(source.getImmediate(), labelAddressMap, libNames, incomingReferences, libraryName, addr + (relative ? c.getSize() : inst.getImmediateOffset()), relative, false);
                                    
                                    if(relative && !source.isResolved()) throw new IllegalArgumentException("Could not resolve relative value: " + source);
                                    
                                    // infer size if not done already
                                    if(source.isResolved() && source.getSize() == -1) source.setSize(getValueWidth(source.getImmediate().value(), false, false));
                                    break;
                                    
                                case MEMORY:
                                    relocated |= resolveValue(source.getMemory().getOffset(), labelAddressMap, libNames, incomingReferences, libraryName, addr + inst.getImmediateOffset(), false, false);
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
                                    relocated |= resolveValue(dest.getMemory().getOffset(), labelAddressMap, libNames, incomingReferences, libraryName, addr + inst.getImmediateOffset(), false, false);
                                    break;
                                    
                                default:
                            }
                        }
                        break;
                        
                    case InitializedData init:
                        for(ResolvableValue rv : init.getUnresolvedData()) {
                            relocated = resolveValue(rv, labelAddressMap, libNames, incomingReferences, libraryName, addr, false, false);
                            
                            if(init.getWordSize() != 4) relocated = false;
                        }
                        break;
                    
                    default:
                }
                
                LOG.finer(before + " resolved to " + c);
                
                if(!c.isResolved() && !relocated) {
                    throw new IllegalStateException("Unable to resolve component: " + c);
                }
            }
            
            int s = c.getSize();
            
            if(s != len) throw new IllegalArgumentException("Length changed after resolution: " + len + " -> " + s);
            
            addr += s;
        }
        
        LOG.fine("-- CONSTANT RESOLUTION FINAL PASS RESULTS --");
        LOG.fine(allInstructions.toString());
        
        // collect object code
        for(int i = 0; i < allInstructions.size(); i++) {
            Component c = allInstructions.get(i);
            List<Byte> objCode = c.getObjectCode();
            
            StringBuilder sb = new StringBuilder(String.format("Encoded %-48s %04X: ", c, objectCode.size()));
            objCode.forEach(b -> sb.append(String.format("%02X ", b)));
            
            LOG.finest(sb.toString());
            
            objectCode.addAll(objCode);
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
        
        LOG.fine("-- FINAL RELOCATION INFO --");
        LOG.fine("OUTGOING REFERENCES: " + outgoingReferences);
        LOG.fine("INCOMING REFERENCES: " + incomingReferences);
        
        return new RenameableRelocatableObject(Endianness.LITTLE, libraryName, 2, incomingReferences, outgoingReferences, incomingReferenceWidths, outgoingReferenceWidths, objectCodeArray, false, libraryNamesMap);
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
        
        if(m.name().endsWith("4")) packedSize = 1;
        else if(m.name().endsWith("8")) packedSize = 2;
        
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
            ResolvableLocationDescriptor firstOperand = parseOperand(symbolQueue, true);
            Opcode opcode = null;
            
            if(firstOperand == null) return null;
            
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
                        opcode = switch(type) {
                            case REGISTER   -> switch(register) {
                                case A  -> Opcode.PUSH_A;
                                case B  -> Opcode.PUSH_B;
                                case C  -> Opcode.PUSH_C;
                                case D  -> Opcode.PUSH_D;
                                case I  -> Opcode.PUSH_I;
                                case J  -> Opcode.PUSH_J;
                                case BP -> Opcode.PUSH_BP;
                                case SP -> Opcode.PUSH_SP;
                                case F  -> Opcode.PUSH_F;
                                default -> Opcode.PUSH_RIM;
                            };
                            
                            case IMMEDIATE  -> firstOperand.getSize() == 4 ? Opcode.PUSH_I32 : Opcode.PUSH_RIM;
                            
                            default         -> Opcode.PUSH_RIM;
                        };
                        break;
                    
                    case POP:
                        opcode = switch(type) {
                            case REGISTER   -> switch(register) {
                                case A  -> Opcode.POP_A;
                                case B  -> Opcode.POP_B;
                                case C  -> Opcode.POP_C;
                                case D  -> Opcode.POP_D;
                                case I  -> Opcode.POP_I;
                                case J  -> Opcode.POP_J;
                                case BP -> Opcode.POP_BP;
                                case SP -> Opcode.POP_SP;
                                case F  -> Opcode.POP_F;
                                default -> Opcode.POP_RIM;
                            };
                            
                            default         -> Opcode.POP_RIM;
                        };
                        break;
                    
                        // I/J shortcuts
                    case INC:
                        opcode = switch(type) {
                            case REGISTER   -> switch(register) {
                                case I  -> Opcode.INC_I;
                                case J  -> Opcode.INC_J;
                                default -> Opcode.INC_RIM;
                            };
                            
                            default     -> Opcode.INC_RIM;
                        };
                        break;
                        
                    case ICC:
                        opcode = switch(type) {
                            case REGISTER   -> switch(register) {
                                case I  -> Opcode.ICC_I;
                                case J  -> Opcode.ICC_J;
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
                                default -> Opcode.DEC_RIM;
                            };
                            
                            default     -> Opcode.DEC_RIM;
                        };
                        break;
                        
                    case DCC:
                        opcode = switch(type) {
                            case REGISTER   -> switch(register) {
                                case I  -> Opcode.DCC_I;
                                case J  -> Opcode.DCC_J;
                                default -> Opcode.DCC_RIM;
                            };
                            
                            default     -> Opcode.DCC_RIM;
                        };
                        break;
                    
                        // 8, 16, 32 bit immediates
                    case JMP:
                        if(type == LocationType.IMMEDIATE) {
                            opcode = switch(firstOperand.getSize()) {
                                case 1  -> Opcode.JMP_I8;
                                case 2  -> Opcode.JMP_I16;
                                default -> Opcode.JMP_I32;
                            };
                        } else {
                            opcode = Opcode.JMP_RIM;
                        }
                        break;
                        
                        // 16 bit immediates
                    case CALL:
                        if(isImmediate && firstOperand.getSize() != -1 && firstOperand.getSize() < 4) {
                            opcode = Opcode.CALL_I16;
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
                            default -> throw new IllegalArgumentException("Invalid mnemonic " + m.name());
                        };
                        break;
                    
                        // packed rim
                    case PINC:
                        opcode = Opcode.PINC_RIMP;
                        firstOperand.setSize(packedSize);
                        break;
                        
                    case PICC:
                        opcode = Opcode.PICC_RIMP;
                        firstOperand.setSize(packedSize);
                        break;
                        
                    case PDEC:
                        opcode = Opcode.PDEC_RIMP;
                        firstOperand.setSize(packedSize);
                        break;
                        
                    case PDCC:
                        opcode = Opcode.PDCC_RIMP;
                        firstOperand.setSize(packedSize);
                        break;
                    
                        // rim + F
                    case NOT:
                        opcode = isImmediate ? Opcode.NOT_F : Opcode.NOT_RIM; 
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
                    case PUSH, JMP, JMPA, JCC, CALL, CALLA, INT -> new Instruction(opcode, firstOperand, false);
                    default                                     -> new Instruction(opcode, firstOperand, true);
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
                        firstIsHByte = firstRegister == Register.AH || firstRegister == Register.BH || firstRegister == Register.CH || firstRegister == Register.DH,
                        secondIsLByte = secondRegister == Register.AL || secondRegister == Register.BL || secondRegister == Register.CL || secondRegister == Register.DL,
                        secondIsHByte = secondRegister == Register.AH || secondRegister == Register.BH || secondRegister == Register.CH || secondRegister == Register.DH,
                        firstIsFlags = firstRegister == Register.F,
                        secondIsFlags = secondRegister == Register.F;
                
                int firstSize = firstOperand.getSize(),
                    secondSize = secondOperand.getSize(),
                    immediateSize = -1;
                
                // immediate width
                if(isImmediate) {
                    // if it's resolved we know the size, if it's not use the widest and shorten later
                    ResolvableValue imm = secondOperand.getImmediate();
                    
                    if(imm.isResolved()) {
                        // zero extend so we don't worry about sign
                        long v = imm.value();
                        
                        if(v >= -128 && v <= 127) immediateSize = 1;
                        else if(v >= -32768 && v <= 32767) immediateSize = 2;
                        else immediateSize = 4;
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
                        
                        // MOVW stuff
                        if(firstSize == 4) {
                            // BP/SP immediate shortcut
                            if(firstRegister == Register.BP && secondType == LocationType.IMMEDIATE) {
                                opcode = Opcode.MOV_BP_I32;
                            } else if(firstRegister == Register.SP && secondType == LocationType.IMMEDIATE) {
                                opcode = Opcode.MOV_SP_I32;
                            } else {
                                opcode = Opcode.MOVW_RIM;
                            }
                            
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
                                        case A  -> Opcode.MOV_A_I8;
                                        case B  -> Opcode.MOV_B_I8;
                                        case C  -> Opcode.MOV_C_I8;
                                        case D  -> Opcode.MOV_D_I8;
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
                        if(firstType != secondType && ((firstIsABCD && secondIsABCD) || (firstIsLByte && secondIsLByte))) {
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
                                
                                case AL -> switch(secondRegister) {
                                    case BL -> Opcode.MOV_AL_BL;
                                    case CL -> Opcode.MOV_AL_CL;
                                    case DL -> Opcode.MOV_AL_DL;
                                    default -> Opcode.MOV_RIM;
                                };
                                
                                case BL -> switch(secondRegister) {
                                    case AL -> Opcode.MOV_BL_AL;
                                    case CL -> Opcode.MOV_BL_CL;
                                    case DL -> Opcode.MOV_BL_DL;
                                    default -> Opcode.MOV_RIM;
                                };
                                
                                case CL -> switch(secondRegister) {
                                    case AL -> Opcode.MOV_CL_AL;
                                    case BL -> Opcode.MOV_CL_BL;
                                    case DL -> Opcode.MOV_CL_DL;
                                    default -> Opcode.MOV_RIM;
                                };
                                
                                case DL -> switch(secondRegister) {
                                    case AL -> Opcode.MOV_DL_AL;
                                    case BL -> Opcode.MOV_DL_BL;
                                    case CL -> Opcode.MOV_DL_CL;
                                    default -> Opcode.MOV_RIM;
                                };
                                
                                default -> Opcode.MOV_RIM; // fallback
                            };
                            
                            break;
                        }
                        
                        opcode = Opcode.MOV_RIM;
                        break;
                        
                    case XCHG:
                        // main shortcuts
                        if(firstIsABCD && secondIsABCD && firstType != secondType) {
                            opcode = switch(firstRegister) {
                                case A  -> switch(secondRegister) {
                                    case B  -> Opcode.XCHG_A_B;
                                    case C  -> Opcode.XCHG_A_C;
                                    case D  -> Opcode.XCHG_A_D;
                                    default -> Opcode.XCHG_RIM;
                                };
                                
                                case B  -> switch(secondRegister) {
                                    case A  -> Opcode.XCHG_A_B;
                                    case C  -> Opcode.XCHG_B_C;
                                    case D  -> Opcode.XCHG_B_D;
                                    default -> Opcode.XCHG_RIM;
                                };
                                
                                case C  -> switch(secondRegister) {
                                    case A  -> Opcode.XCHG_A_C;
                                    case B  -> Opcode.XCHG_B_C;
                                    case D  -> Opcode.XCHG_C_D;
                                    default -> Opcode.XCHG_RIM;
                                };
                                
                                case D  -> switch(secondRegister) {
                                    case A  -> Opcode.XCHG_A_D;
                                    case B  -> Opcode.XCHG_B_D;
                                    case C  -> Opcode.XCHG_C_D;
                                    default -> Opcode.XCHG_RIM;
                                };
                                
                                default -> Opcode.XCHG_RIM; // fallback
                            };
                            
                            break;
                        }
                        
                        // low-low shortcuts
                        if(firstIsLByte && secondIsLByte && firstType != secondType) {
                            opcode = switch(firstRegister) {
                                case AL -> switch(secondRegister) {
                                    case BL -> Opcode.XCHG_AL_BL;
                                    case CL -> Opcode.XCHG_AL_CL;
                                    case DL -> Opcode.XCHG_AL_DL;
                                    default -> Opcode.XCHG_RIM;
                                };
                                
                                case BL -> switch(secondRegister) {
                                    case AL -> Opcode.XCHG_AL_BL;
                                    case CL -> Opcode.XCHG_BL_CL;
                                    case DL -> Opcode.XCHG_BL_DL;
                                    default -> Opcode.XCHG_RIM;
                                };
                                
                                case CL -> switch(secondRegister) {
                                    case AL -> Opcode.XCHG_AL_CL;
                                    case BL -> Opcode.XCHG_BL_CL;
                                    case DL -> Opcode.XCHG_CL_DL;
                                    default -> Opcode.XCHG_RIM;
                                };
                                
                                case DL -> switch(secondRegister) {
                                    case AL -> Opcode.XCHG_AL_DL;
                                    case BL -> Opcode.XCHG_BL_DL;
                                    case CL -> Opcode.XCHG_CL_DL;
                                    default -> Opcode.XCHG_RIM;
                                };
                                
                                default -> Opcode.XCHG_RIM; // fallback
                            };
                            
                            break;
                        }
                            
                        // high-low shortcuts
                        if(((firstIsLByte && secondIsHByte) || (firstIsHByte && secondIsLByte)) && firstType != secondType) {
                            opcode = switch(firstRegister) {
                                case AH, AL -> switch(secondRegister) {
                                    case AH, AL -> Opcode.XCHG_AH_AL;
                                    default     -> Opcode.XCHG_RIM;
                                };
                                
                                case BH, BL -> switch(secondRegister) {
                                    case BH, BL -> Opcode.XCHG_BH_BL;
                                    default     -> Opcode.XCHG_RIM;
                                };
                                
                                case CH, CL -> switch(secondRegister) {
                                    case CH, CL -> Opcode.XCHG_CH_CL;
                                    default     -> Opcode.XCHG_RIM;
                                };
                                
                                case DH, DL -> switch(secondRegister) {
                                    case DH, DL -> Opcode.XCHG_DH_DL;
                                    default     -> Opcode.XCHG_RIM;
                                };
                                
                                default     -> Opcode.XCHG_RIM; // fallback
                            };
                            
                            break;
                        }
                        
                        opcode = Opcode.XCHG_RIM;
                        break;
                        
                        // register-immediate shortcuts
                    case ADD:
                        if(isImmediate) {
                            opcode = switch(firstRegister) {
                                case A  -> (immediateSize == 1) ? Opcode.ADD_A_I8 : Opcode.ADD_A_I16;
                                case B  -> (immediateSize == 1) ? Opcode.ADD_B_I8 : Opcode.ADD_B_I16;
                                case C  -> (immediateSize == 1) ? Opcode.ADD_C_I8 : Opcode.ADD_C_I16;
                                case D  -> (immediateSize == 1) ? Opcode.ADD_D_I8 : Opcode.ADD_D_I16;
                                default -> (immediateSize == 1) ? Opcode.ADD_RIM_I8 : Opcode.ADD_RIM;
                            };
                        } else {
                            opcode = Opcode.ADD_RIM;
                        }
                        break;
                        
                    case ADC:
                        if(isImmediate) {
                            opcode = switch(firstRegister) {
                                case A  -> (immediateSize == 1) ? Opcode.ADC_A_I8 : Opcode.ADC_A_I16;
                                case B  -> (immediateSize == 1) ? Opcode.ADC_B_I8 : Opcode.ADC_B_I16;
                                case C  -> (immediateSize == 1) ? Opcode.ADC_C_I8 : Opcode.ADC_C_I16;
                                case D  -> (immediateSize == 1) ? Opcode.ADC_D_I8 : Opcode.ADC_D_I16;
                                default -> (immediateSize == 1) ? Opcode.ADC_RIM_I8 : Opcode.ADC_RIM;
                            };
                        } else {
                            opcode = Opcode.ADC_RIM;
                        }
                        break;
                    
                    case SUB:
                        if(isImmediate) {
                            opcode = switch(firstRegister) {
                                case A  -> (immediateSize == 1) ? Opcode.SUB_A_I8 : Opcode.SUB_A_I16;
                                case B  -> (immediateSize == 1) ? Opcode.SUB_B_I8 : Opcode.SUB_B_I16;
                                case C  -> (immediateSize == 1) ? Opcode.SUB_C_I8 : Opcode.SUB_C_I16;
                                case D  -> (immediateSize == 1) ? Opcode.SUB_D_I8 : Opcode.SUB_D_I16;
                                default -> (immediateSize == 1) ? Opcode.SUB_RIM_I8 : Opcode.SUB_RIM;
                            };
                        } else {
                            opcode = Opcode.SUB_RIM;
                        }
                        break;
                        
                    case SBB:
                        if(isImmediate) {
                            opcode = switch(firstRegister) {
                                case A  -> (immediateSize == 1) ? Opcode.SBB_A_I8 : Opcode.SBB_A_I16;
                                case B  -> (immediateSize == 1) ? Opcode.SBB_B_I8 : Opcode.SBB_B_I16;
                                case C  -> (immediateSize == 1) ? Opcode.SBB_C_I8 : Opcode.SBB_C_I16;
                                case D  -> (immediateSize == 1) ? Opcode.SBB_D_I8 : Opcode.SBB_D_I16;
                                default -> (immediateSize == 1) ? Opcode.SBB_RIM_I8 : Opcode.SBB_RIM;
                            };
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
                        
                        // just RIM? convert directly
                    default:
                        String oprn = opr.toString();
                        
                        if(oprn.startsWith("P")) { // packed
                            opcode = Opcode.valueOf(oprn + "_RIMP");
                            secondOperand.setSize(packedSize);
                            
                            if(firstOperand.getType() == LocationType.REGISTER) {
                                switch(firstOperand.getRegister()) {
                                    case A, B, C, D, DA, AB, BC, CD: break;
                                    default: throw new IllegalArgumentException("Invalid packed register: " + firstOperand);
                                }
                            }
                            
                            if(secondOperand.getType() == LocationType.REGISTER) {
                                switch(secondOperand.getRegister()) {
                                    case A, B, C, D, DA, AB, BC, CD: break;
                                    default: throw new IllegalArgumentException("Invalid packed register: " + secondOperand);
                                }
                            }
                        } else { // not packed
                            opcode = Opcode.valueOf(oprn + "_RIM");
                        }
                        
                        break;
                }
                
                return new Instruction(opcode, firstOperand, secondOperand);
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
                // not actually so ez
            case RegisterSymbol rs:
                Register r = Register.valueOf(rs.name());
                
                // handle r16:r16
                if(r.size() == 2 && symbolQueue.peek() instanceof SpecialCharacterSymbol scs && scs.character() == ':') {
                    symbolQueue.poll();
                    
                    if(symbolQueue.peek() instanceof RegisterSymbol rs2) {
                        symbolQueue.poll();
                        return new ResolvableLocationDescriptor(LocationType.REGISTER, Register.valueOf(rs.name() + rs2.name()));
                    } else {
                        throw new IllegalArgumentException("Invalid symbol while parsing operand: " + scs);
                    }
                }
                
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
                rld.setSize(convertSize(ss));
                return rld;
                
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
        Set<String> reg16s = Set.of("A", "B", "C", "D", "I", "J"),
                    reg32s = Set.of("A", "B", "C", "D", "I", "J", "BP", "SP");
        
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
                                        case A, B, C, D, I, J:
                                            if(assignedIndex) throw new IllegalArgumentException("Duplicate index in memory: " + rs);
                                        
                                            index = r;
                                            scale = 1;
                                            assignedIndex = true;
                                            break;
                                        
                                            // base
                                        case DA, AB, BC, CD, JI, IJ:
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
                    
                    yield switch(name) {
                        case "A"    -> n2.equals("B") ? Register.AB : Register.A;
                        case "B"    -> n2.equals("C") ? Register.BC : Register.B;
                        case "C"    -> n2.equals("D") ? Register.CD : Register.C;
                        case "D"    -> n2.equals("A") ? Register.DA : Register.D;
                        case "I"    -> n2.equals("J") ? Register.IJ : Register.I;
                        case "J"    -> n2.equals("I") ? Register.JI : Register.J;
                        default     -> Register.valueOf(name);
                    };
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
            case NOP, RET, IRET -> false;
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
    private static Component parseDirective(LinkedList<Symbol> symbolQueue, DirectiveSymbol ds) {
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
            
            // %DEFINE is included here as definitions are handled by AssemblerLib
            // AS is expressive and part of the import/include construct so shouldn't be passed
            default:
                throw new IllegalArgumentException("Invalid directive: " + ds);
        }
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
            
            if(!labelIndexMap.containsKey(n)) {
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
    private static boolean resolveValue(ResolvableValue rv, Map<String, Integer> labelAddressMap, Set<String> libraries, HashMap<String, List<Integer>> incomingReferences, String fileName, int address, boolean relative, boolean inExpression) {
        boolean relocated = false;
        
        try {
            switch(rv) {
                case ResolvableExpression re:
                    ResolvableValue left = re.getLeft(),
                                    right = re.getRight();
                    
                    if(!left.isResolved()) resolveValue(left, labelAddressMap, libraries, incomingReferences, fileName, address, false, true);
                    if(!right.isResolved()) resolveValue(right, labelAddressMap, libraries, incomingReferences, fileName, address, false, true);
                    break;
                    
                case ResolvableConstant rc:
                    String name = rc.getName();
                    
                    if(!inExpression && !relative) {
                        if(!isLibraryReference(name, libraries)) {
                            name = fileName + "." + name;
                        }
                        
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
     * @param three true if 3 is allowed
     * @return
     */
    private static int getValueWidth(long v, boolean one, boolean three) {
        if(v >= -128 && v <= 127) { // 1 byte
            return one ? 1 : 2;
        } else if(v >= -32768 && v <= 32767) { // 2 byte
            return 2;
        } else if(v >= -8388608 && v <= 8388607) { // 3 byte
            return three ? 3 : 4;
        } else { // 4 byte
            return 4;
        }
    }
    
    /**
     * Converts an Instruction to use immediate shortcuts if applicable
     * 
     * @param inst
     */
    private static void applyImmediateShortcuts(Instruction inst) {
        // TODO
    }
}
