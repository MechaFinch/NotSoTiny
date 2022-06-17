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
import asmlib.lex.symbols.Symbol;
import asmlib.lex.symbols.SymbolGroup;
import asmlib.token.Tokenizer;
import asmlib.util.relocation.RelocatableObject;
import asmlib.util.relocation.RelocatableObject.Endianness;
import notsotiny.asm.components.Component;
import notsotiny.asm.components.Instruction;
import notsotiny.asm.resolution.ResolvableConstant;
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
            
            System.out.println("Processing symbol: " + s);
            
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
                        } else if(!inst.hasValidOperands()) {
                            System.out.println("Illegal operands for instruction: " + inst + " on line " + line);
                        } else {
                            System.out.println("Parsed instruction: " + inst);
                            allInstructions.add(inst);
                            //if(!inst.isResolved()) unresolvedInstructions.add(inst);
                        }
                        break;
                        
                    default: // do nothing
                        System.out.println("Unknown construct start: " + s + " on line " + line);
                }
            } catch(IndexOutOfBoundsException e) { // these let us avoid checking that needed symbols exist
                throw new IllegalArgumentException("Missing symbol after " + s + " on line " + line);
            } catch(IllegalArgumentException e) { // add line info to parser errors
                System.out.println(e.getMessage() + " on line " + line);
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
        Operation opr = Operation.fromMnemonic(m.name());
        
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
                            
                            default     -> Opcode.PUSH_RIM;
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
                        break;
                        
                    case PICC:
                        opcode = Opcode.PICC_RIMP;
                        break;
                        
                    case PDEC:
                        opcode = Opcode.PDEC_RIMP;
                        break;
                        
                    case PDCC:
                        opcode = Opcode.PDCC_RIMP;
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
                if(firstOperand == null) return null; // TODO TMP
                
                // we expect a separator, skip it if present
                if(symbolQueue.peek() instanceof SeparatorSymbol) {
                    symbolQueue.poll();
                }
                
                // parse second operand
                boolean canBeMemory = (firstOperand.getType() != LocationType.MEMORY) && (firstOperand.getType() != LocationType.IMMEDIATE);
                ResolvableLocationDescriptor secondOperand = parseOperand(symbolQueue, canBeMemory);
                
                if(secondOperand == null) return null; // TODO TMP
                
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
                        int v = imm.value();
                        
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
                            if(mem.getBase() != Register.NONE) {
                                // offset?
                                if(mem.isResolved() && mem.getOffset() == 0) { // definitely not
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
                            if(mem.getBase() != Register.NONE) {
                                // offset?
                                if(mem.isResolved() && mem.getOffset() == 0) { // definitely not
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
        System.out.println("Parsing operand with symbol: " + nextSymbol);
        
        // what we workin with
        switch(nextSymbol) {
                // ez
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
                return parseExpression(es);
                
                // size prefix overrides operand size
            case SizeSymbol ss:
                ResolvableLocationDescriptor rld = parseOperand(symbolQueue, canBeMemory);
                rld.setSize(convertSize(ss));
                return rld;
                
            case Symbol s:
                //TODO
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
        System.out.println("Parsing memory: " + ms);
        
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
                assignedIndex = false;
        
        /*
         * consuming each symbol in the MemorySymbol group, assign what we come across and construct a ResolvableExpression for the offset
         */
        
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
     * Parses an expression
     * 
     * @param es
     * @return
     */
    private static ResolvableLocationDescriptor parseExpression(ExpressionSymbol es) {
        System.out.println("Parsing expression: " + es);
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
