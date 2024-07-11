package notsotiny.ui;

import java.awt.KeyboardFocusManager;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.sound.midi.MidiUnavailableException;

import asmlib.util.relocation.ExecLoader;
import asmlib.util.relocation.Relocator;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import notsotiny.asm.Disassembler;
import notsotiny.sim.NotSoTinySimulator;
import notsotiny.sim.memory.DiskBufferController;
import notsotiny.sim.memory.FlatMemoryController;
import notsotiny.sim.memory.InterruptController;
import notsotiny.sim.memory.MemoryManager;
import notsotiny.sim.memory.RandomController;
import notsotiny.sim.memory.ScreenBuffer;
import notsotiny.sim.memory.SoundInterfaceController;

public class NotSoTinyUI extends Application {

    public static void main(String[] args) {
        System.out.println("launching");
        Application.launch(args);
    }
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        this.stage = primaryStage;
        
        initSimulator(true);
        createUI();
    }
    
    /*
     * PARAMETERS
     */
    
    private static final long CLOCK_PERIOD = 0l;
    
    private static final boolean USE_SCREEN = true,
                                 START_IMMEDIATELY = false,
                                 START_WITH_CLOCK = false;
    
    private static final int TRACE_SIZE = 16;
    
    /*
     * == SIMULATION ==
     */
    
    // memory map constants
    private static final long IVT_START =       0x0000_0000,
                              LOWRAM_START =    0x0000_0400,
                              CRASHRAM_START =  0x4000_0000,
                              SPI_START =       0x8000_0000,
                              KEYPAD_START =    0x8001_0000,
                              CC_START =        0x8002_0000,
                              KEYBOARD_START =  0xF000_0000,
                              SOUND_START =     0xF001_0000,
                              VIDEO_START =     0xF002_0000,
                              RANDOM_START =    0xF004_0000,
                              PIC_START =       0xF005_0000,
                              DISK_START =      0xF006_0000,
                              BOOTROM_START =   0xFFFF_FC00;
    
    private static final int IVT_SIZE =         0x0000_0400,
                             LOWRAM_SIZE =      0x000F_FC00,
                             CRASHRAM_SIZE =    0x0000_1000,
                             SPI_SIZE =         0x0000_0004,
                             KEYPAD_SIZE =      0x0000_0008,
                             CC_SIZE =          0x0000_0002,
                             KEYBOARD_SIZE =    0x0000_0004,
                             SOUND_SIZE =       0x0000_0008,
                             VIDEO_SIZE =       0x0002_0000,
                             RANDOM_SIZE =      0x0000_0010,
                             PIC_SIZE =         0x0000_0200,
                             DISK_SIZE =        0x0000_000C,
                             BOOTROM_SIZE =     0x0000_0400;
    
    private static final int VIDEO_BUFFER_SIZE =    0x0001_4000,
                             VIDEO_CHARSET_SIZE =   0x0000_1000,
                             VIDEO_OTHER_SIZE =     0x0002_0000 - (VIDEO_BUFFER_SIZE + VIDEO_CHARSET_SIZE);
    
    // other constants
    private static final byte VECTOR_RESET = 0,
                              VECTOR_NMI = 1,
                              VECTOR_KEYUP = 2,
                              VECTOR_KEYDOWN = 3,
                              VECTOR_MEMORY_ERROR = 8,
                              VECTOR_RTC = 12;
    
    /*
    private static final String PROGRAM_DATA_FOLDER = "data\\",
                                PROGRAM_EXEC_FILE = "game.oex",
                                TEXT_FONT_FILE = "data\\text.dat";
    */
    
    private static final String //PROGRAM_DATA_FOLDER = "C:\\Users\\wetca\\data\\silly  code\\architecture\\NotSoTiny\\programming\\forth\\kernel\\emu\\",
                                //PROGRAM_DATA_FOLDER = "C:\\Users\\wetca\\data\\silly  code\\architecture\\NotSoTiny\\programming\\high level\\testing\\",
                                //PROGRAM_DATA_FOLDER = "C:\\Users\\wetca\\data\\silly  code\\architecture\\NotSoTiny\\programming\\high level\\minesweeper\\",
                                PROGRAM_DATA_FOLDER = "C:\\Users\\wetca\\data\\silly  code\\architecture\\NotSoTiny\\programming\\standard library\\fakeos\\",
                                //PROGRAM_EXEC_FILE = PROGRAM_DATA_FOLDER + "forth.oex",
                                //PROGRAM_EXEC_FILE = PROGRAM_DATA_FOLDER + "testing-mdbt.oex",
                                //PROGRAM_EXEC_FILE = PROGRAM_DATA_FOLDER + "minesweeper.oex",
                                PROGRAM_EXEC_FILE = PROGRAM_DATA_FOLDER + "test_shell.oex",
                                DISK_FOLDER = PROGRAM_DATA_FOLDER + "disk\\",
                                TEXT_FONT_FILE = "C:\\Users\\wetca\\data\\silly  code\\architecture\\NotSoTiny\\programming\\standard library\\simvideo\\textsmall.dat";
    
    
    // sim vars
    private NotSoTinySimulator sim;
    
    private MemoryManager mmu;
    
    private FlatMemoryController ivtController,
                                 lowramController,
                                 crashramController,
                                 placeholder_spiController,
                                 placeholder_cacheController,
                                 keyboardBufferController,
                                 videoBufferController,
                                 videoCharsetController,
                                 videoOtherController,
                                 bootromController;
    
    private SoundInterfaceController sic;
    
    private RandomController rand;
    
    private InterruptController pic;
    
    private DiskBufferController dbc;
    
    private Relocator relocator;
    
    private String entrySymbol;
    
    private ScreenBuffer screenBufferController;
    
    // actual memory arrays
    private byte[] ivtArray,
                   lowramArray,
                   crashramArray,
                   placeholder_spiArray,
                   placeholder_cacheArray,
                   keyboardBufferArray,
                   videoBufferArray,
                   videoCharsetArray,
                   videoOtherArray,
                   bootromArray;
    
    // real time clock stuff
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    private SimulatorThread simThread;
    
    /**
     * Thread for running the simulator
     * 
     * @author Mechafinch
     */
    private class SimulatorThread extends Thread {
        private long periodns = 0;
        
        private ScheduledFuture<?> clockHandler;
        
        /**
         * Constructor
         * 
         * @param periodus period in nanoseconds
         */
        public SimulatorThread(long periodns) {
            this.periodns = periodns;
        }
        
        @Override
        public void run() {
            while(true) {
                try {
                    // run as fast as possible or no?
                    if(this.periodns == 0) {
                        //System.out.println("running fast");
                        this.clockHandler = null;
                        
                        // just run until halted
                        while(!NotSoTinyUI.this.sim.getHalted()) {
                            if(Thread.interrupted()) return;
                            step();
                        }
                    } else if(this.clockHandler == null || this.clockHandler.isDone()){
                        
                        //System.out.println("running slow");
                        // run at the given rate
                        if(!NotSoTinyUI.this.sim.getHalted()) {
                            this.clockHandler = NotSoTinyUI.this.scheduler.scheduleAtFixedRate(() -> this.step(), 0, this.periodns, TimeUnit.NANOSECONDS);
                        }
                    }
                    
                    synchronized(this) {
                        wait();
                    }
                } catch(InterruptedException ie) {
                    this.stopSim();
                    return;
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        /**
         * Stops the clock
         */
        private void stopSim() {
            if(this.clockHandler != null) {
                while(!this.clockHandler.isDone()) this.clockHandler.cancel(false);
            }
        }
        
        /**
         * Steps the simulator
         */
        private void step() {
            if(NotSoTinyUI.this.sim.getHalted() || !NotSoTinyUI.this.freerunEnabled) {
                this.stopSim();
            } else {
                try { 
                    if(NotSoTinyUI.this.traceEnabled) {
                        if(NotSoTinyUI.this.advancedViewVisisble) NotSoTinyUI.this.traceInstruction();
                        else NotSoTinyUI.this.dummyTrace();
                    }
                    
                    NotSoTinyUI.this.pic.step(NotSoTinyUI.this.sim);
                    NotSoTinyUI.this.sim.step();
                } catch(Exception e) {
                    if(e instanceof IndexOutOfBoundsException ie && ie.getMessage().contains("registered")) {
                        System.out.printf("%08X: ", NotSoTinyUI.this.sim.getRegIP() - 1);
                        System.out.println(ie.getMessage());
                        
                        // extract address
                        String m = ie.getMessage();
                        long targetAddr = Long.parseUnsignedLong(m.substring(m.lastIndexOf(" ") + 1), 16),
                             sourceAddr = (NotSoTinyUI.this.sim.getRegIP() - 1) & 0xFFFF_FFFFl;
                        
                        // segfault
                        NotSoTinyUI.this.sim.setRegD((short)(targetAddr >> 16)); // fault address in D:A
                        NotSoTinyUI.this.sim.setRegA((short)targetAddr);
                        NotSoTinyUI.this.sim.setRegB((short)(sourceAddr >> 16));
                        NotSoTinyUI.this.sim.setRegC((short)sourceAddr);
                        NotSoTinyUI.this.sim.setRegI((short) 0);
                        
                        NotSoTinyUI.this.sim.setRegSP((int)(CRASHRAM_START + CRASHRAM_SIZE)); // ensure SP is valid
                        NotSoTinyUI.this.sim.fireNonMaskableInterrupt(VECTOR_MEMORY_ERROR);
                    } else {
                        System.out.printf("%08X: ", NotSoTinyUI.this.sim.getRegIP() - 1);
                        e.printStackTrace();
                        NotSoTinyUI.this.sim.setHalted(true);
                        this.stopSim();
                        throw e;
                    }
                }
                NotSoTinyUI.this.instructionsExecutedLast++;
            }
            
            // sanity check
            if(NotSoTinyUI.this.sim.getRegIP() < 1024) {
                
                NotSoTinyUI.this.sim.setHalted(true);
                NotSoTinyUI.this.freerunEnabled = false;
                throw new IllegalStateException("Out of bounds IP: " + NotSoTinyUI.this.sim.getRegIP());
                
                
                // segfault for this too
                /*
                System.out.printf("Out of bounds IP: %08X", NotSoTinyUI.this.sim.getRegIP());
                NotSoTinyUI.this.sim.setRegD((short)(NotSoTinyUI.this.sim.getRegIP() >> 16));
                NotSoTinyUI.this.sim.setRegA((short) NotSoTinyUI.this.sim.getRegIP());
                NotSoTinyUI.this.sim.setRegI((short) 2);
                
                NotSoTinyUI.this.sim.setRegSP(1024);
                NotSoTinyUI.this.sim.fireNonMaskableInterrupt(VECTOR_MEMORY_ERROR);
                */
            }
            
            // breakpoints
            if(NotSoTinyUI.this.enableBreakpoints && NotSoTinyUI.this.breakpointAddress != -1l) {
                if(NotSoTinyUI.this.sim.getRegIP() == (NotSoTinyUI.this.breakpointAddress & 0xFFFF_FFFF)) {
                    NotSoTinyUI.this.sim.setHalted(true);
                    NotSoTinyUI.this.freerunEnabled = false;
                }
            }
        }
        
        //public void setPeriod(long p) { this.periodus = p; }
    }
    
    // misc
    private long instructionsExecutedLast, // tracks since UI was last updated
                 instructionsExecutedTotal,
                 elapsedTimens,
                 breakpointAddress,
                 memwatchAddress;
    
    private double lastAverageMIPS;
    
    private boolean enableBreakpoints,
                    freerunEnabled,
                    traceEnabled,
                    disassemblyEnabled,
                    stackTraceEnabled,
                    rtcEnabled,
                    memwatchIsNumber;
    
    private String breakpointSymbol,
                   memwatchRegister;
    
    private Deque<String> instructionTrace;
    
    /**
     * Initialize the simulator
     * 
     * @throws MidiUnavailableException 
     * @throws IOException 
     */
    private void initSimulator(boolean load) throws MidiUnavailableException, IOException {
        // initialize memory manager
        this.mmu = new MemoryManager();
        
        // initialize flat memory segments]
        ivtArray = new byte[IVT_SIZE];
        lowramArray = new byte[LOWRAM_SIZE];
        crashramArray = new byte[CRASHRAM_SIZE];
        placeholder_spiArray = new byte[SPI_SIZE];
        placeholder_cacheArray = new byte[CC_SIZE];
        keyboardBufferArray = new byte[KEYBOARD_SIZE];
        videoBufferArray = new byte[VIDEO_BUFFER_SIZE];
        videoCharsetArray = new byte[VIDEO_CHARSET_SIZE];
        videoOtherArray = new byte[VIDEO_OTHER_SIZE];
        bootromArray = new byte[BOOTROM_SIZE];
        
        ivtController = new FlatMemoryController(ivtArray, true, true);
        lowramController = new FlatMemoryController(lowramArray, false, false);
        crashramController = new FlatMemoryController(crashramArray, false, false);
        placeholder_spiController = new FlatMemoryController(placeholder_spiArray, false, false);
        placeholder_cacheController = new FlatMemoryController(placeholder_cacheArray, true, true);
        keyboardBufferController = new FlatMemoryController(keyboardBufferArray, false, true);
        videoBufferController = new FlatMemoryController(videoBufferArray, false, false);
        videoCharsetController = new FlatMemoryController(videoCharsetArray, false, false);
        videoOtherController = new FlatMemoryController(videoOtherArray, false, false);
        bootromController = new FlatMemoryController(bootromArray, false, true);
        
        // initialize other segments
        sic = new SoundInterfaceController();
        rand = new RandomController();
        pic = new InterruptController();
        dbc = new DiskBufferController(this.mmu, Paths.get(DISK_FOLDER));
        screenBufferController = new ScreenBuffer(videoBufferArray); // 3FFFC
        
        this.mmu.registerSegment(ivtController, IVT_START, IVT_SIZE);
        this.mmu.registerSegment(lowramController, LOWRAM_START, LOWRAM_SIZE);
        this.mmu.registerSegment(placeholder_spiController, SPI_START, SPI_SIZE);
        this.mmu.registerSegment(placeholder_cacheController, CC_START, CC_SIZE);
        this.mmu.registerSegment(keyboardBufferController, KEYBOARD_START, KEYBOARD_SIZE);
        this.mmu.registerSegment(videoBufferController, VIDEO_START, VIDEO_BUFFER_SIZE);
        this.mmu.registerSegment(videoCharsetController, VIDEO_START + VIDEO_BUFFER_SIZE, VIDEO_CHARSET_SIZE);
        this.mmu.registerSegment(videoOtherController, VIDEO_START + VIDEO_BUFFER_SIZE + VIDEO_CHARSET_SIZE, VIDEO_OTHER_SIZE - 4);
        this.mmu.registerSegment(pic, PIC_START, PIC_SIZE);
        this.mmu.registerSegment(dbc, DISK_START, DISK_SIZE);
        this.mmu.registerSegment(bootromController, BOOTROM_START, BOOTROM_SIZE);
        
        this.mmu.registerSegment(sic, SOUND_START, SOUND_SIZE);
        this.mmu.registerSegment(rand, RANDOM_START, RANDOM_SIZE);
        this.mmu.registerSegment(screenBufferController, VIDEO_START + VIDEO_BUFFER_SIZE + VIDEO_CHARSET_SIZE + VIDEO_OTHER_SIZE - 4, 4);
        
        // initialize tracked things
        this.instructionTrace = new ArrayDeque<>();
        this.instructionsExecutedLast = 0;
        this.instructionsExecutedTotal = 0;
        
        for(int i = 0; i < TRACE_SIZE; i++) {
            instructionTrace.push("");
        }
        
        // misc
        this.breakpointAddress = -1l;
        this.lastAverageMIPS = 0;
        this.memwatchAddress = 0;
        this.enableBreakpoints = false;
        this.freerunEnabled = false;
        this.traceEnabled = false;
        this.disassemblyEnabled = false;
        this.stackTraceEnabled = false;
        this.memwatchIsNumber = true;
        this.breakpointSymbol = "";
        this.memwatchRegister = "";
        this.pic.setNonMaskable(VECTOR_NMI);
        
        //this.mmu.printMap();
        
        // load text font
        byte[] font = Files.readAllBytes(Paths.get(TEXT_FONT_FILE));
        System.arraycopy(font, 0, videoCharsetArray, 0, font.length);
        
        // Load program into memory
        if(load) {
            List<Object> relocatorPair = ExecLoader.loadExecFileToRelocator(new File(PROGRAM_EXEC_FILE));
            
            this.relocator = (Relocator) relocatorPair.get(0);
            this.entrySymbol = (String) relocatorPair.get(1);
        }
        
        int relStart = this.relocator.hasReference("ORIGIN", false) ? 0 : 1024;
        
        byte[] relocatedData = new byte[0x10_0000];
        long entry = ExecLoader.loadRelocator(this.relocator, entrySymbol, relocatedData, relStart, relStart);
        
        System.arraycopy(relocatedData, 0, ivtArray, 0, ivtArray.length);
        System.arraycopy(relocatedData, IVT_SIZE, lowramArray, 0, LOWRAM_SIZE);
        
        // write entry vector
        this.mmu.write4BytesPrivileged(VECTOR_RESET * 4, (int) entry);
        
        // simulator
        this.sim = new NotSoTinySimulator(this.mmu);
        this.sim.setRegSP((int)(LOWRAM_START + LOWRAM_SIZE));
        
        // timing stuff
        this.simThread = new SimulatorThread(CLOCK_PERIOD);
        
        this.simThread.start();
        
        // start real time clock
        this.rtcEnabled = START_WITH_CLOCK;
        this.scheduler.scheduleAtFixedRate(() -> {
            if(this.rtcEnabled) {
                runInterrupt(VECTOR_RTC);
            }
        }, 1_000_000l, 1_000l, TimeUnit.MICROSECONDS);
        // 1000 hz
        
        //this.halter.writeByte(0l, (byte) 0);
        this.sim.setHalted(!START_IMMEDIATELY);
        this.freerunEnabled = START_IMMEDIATELY;
    }
    
    private void restartSimulator(boolean reload) {
        // kill simThread
        this.simThread.interrupt();
        
        try {
            this.simThread.join();
        } catch(InterruptedException e) {
            // that's the idea
        }
        
        // stop ui updater
        while(!this.uiUpdateFuture.isDone()) this.uiUpdateFuture.cancel(false);
        
        // reset
        try {
            initSimulator(reload);
            createUI();
        } catch(MidiUnavailableException e) {
            e.printStackTrace();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Toggles the simulator
     */
    private void toggleRunSimulator() {
        if(!this.freerunEnabled) {
            this.freerunEnabled = true;
            this.sim.setHalted(false);
            notifySimulatorThread();
        } else {
            this.freerunEnabled = false;
            this.sim.setHalted(true);
        }
    }
    
    /**
     * Toggles traing
     */
    private void toggleTrace() {
        this.traceEnabled = !this.traceEnabled;
        this.disassemblyEnabled = false;
        this.stackTraceEnabled = false;
    }
    
    /**
     * Toggles disassembly
     */
    private void toggleDisassembly() {
        this.disassemblyEnabled = !this.disassemblyEnabled;
        this.traceEnabled = false;
        this.stackTraceEnabled = false;
    }
    
    /**
     * Toggles stack trace
     */
    private void toggleStackTrace() {
        this.stackTraceEnabled = !this.stackTraceEnabled;
        this.traceEnabled = false;
        this.disassemblyEnabled = false;
    }
    
    /**
     * Toggles RTC
     */
    private void toggleRTC() {
        this.rtcEnabled = !this.rtcEnabled;
    }
    
    /**
     * Toggles maskable interrupts
     * Flips the IE bit in PF
     */
    private void toggleInterrupts() {
        this.sim.setRegPF((short)(this.sim.getRegPF() ^ 0x0001));
    }
    
    /**
     * Awakens the simulator thread
     */
    private void notifySimulatorThread() {
        synchronized(this.simThread) { 
            this.simThread.notify();
        }
    }
    
    /**
     * Fires an interrupt, and sets the cpu running if applicable
     * @param vector
     */
    private void runInterrupt(byte vector) {
        this.pic.setRequest(vector);
        this.pic.step(this.sim);
        notifySimulatorThread();
    }
    
    /**
     * Step the simulator
     */
    private void stepSim() {
        if(NotSoTinyUI.this.traceEnabled) {
            if(NotSoTinyUI.this.advancedViewVisisble) NotSoTinyUI.this.traceInstruction();
            else NotSoTinyUI.this.dummyTrace();
        }
        
        this.sim.step();
        this.instructionsExecutedLast++;
        
        if(this.enableBreakpoints && this.breakpointAddress != -1l) {
            if(this.sim.getRegIP() == (this.breakpointAddress & 0xFFFF_FFFF)) {
                //this.halter.writeByte(0, (byte) 0);
                this.sim.setHalted(true);
            }
        }
    }
    
    /*
     * == GRAPHICS ==
     */
    
    /*
     * Graphics layout
     * GridPane -> (0, 0, 1, 1) Screen
     *          -> (0, 1, 1, 1) basic controls HBox -> start/stop button
     *                                              -> basic info VBox      -> average MIPS Text
     *                                                                      -> toggle advanced Button
     *          -> (1, 0, 1, 2) advanced view VBox
     *                                      
     */
    
    // graphics vars
    private Stage stage;
    
    private Screen screen;
    
    private Text infoAverageMIPS,
                 infoTotalInstructions,
                 infoProcessorState,
                 infoCurrentBreakpoint,
                 infoCurrentWatchAddress,
                 infoMemwatch;
    
    private TextField fieldBreakpoint,
                      fieldMemwatch,
                      fieldClockSpeed;
    
    private Button buttonToggleAdvanced,
                   buttonAwaken,
                   buttonToggleRunning,
                   buttonStepSim,
                   buttonToggleTrace,
                   buttonToggleDissassembler,
                   buttonToggleStackTrace,
                   buttonReset,
                   buttonReload,
                   buttonToggleClock,
                   buttonToggleInterrupts;
    
    private CheckBox checkEnableBreakpoints;
    
    private Node advancedView;
    
    private boolean advancedViewVisisble = false;
    
    private ScheduledFuture<?> uiUpdateFuture;
    
    private synchronized void traceInstruction() {
        String descriptor;
        
        if(this.sim.hasPendingInterrupt()) {
            descriptor = String.format("INTERRUPT %02X", this.sim.getPendingInterruptVector());
        } else {
            Disassembler dis = new Disassembler();
            String disasm = dis.disassemble(this.mmu, Integer.toUnsignedLong(sim.getRegIP()));
            
            if(disasm == null) {
                System.out.println("ERROR");
                this.freerunEnabled = false;
                disasm = "ERROR";
            }
            
            descriptor = String.format("%08X: %-16s", this.sim.getRegIP(), disasm);
        }
        
        this.instructionTrace.poll();
        this.instructionTrace.offer(descriptor);
    }
    
    private synchronized void dummyTrace() {
        //this.instructionTrace.offer("");
        //this.instructionTrace.poll();
    }
    
    /**
     * Initialize the UI 
     */
    private void createUI() {
        // layout tree
        // initialize components in post-order
        // screen
        this.screen = new Screen(320, 240, 3);
        
        if(USE_SCREEN) {
            this.screen.enable();
        } else {
            this.screen.disable();
        }
        
        // basic info
        this.infoTotalInstructions = new Text("Total instrctions: 0");
        this.infoAverageMIPS = new Text("Average MIPS:  0.00");
        this.buttonAwaken = new Button("Awaken CPU");
        this.buttonToggleRunning = new Button("Start CPU");
        this.buttonToggleAdvanced = new Button("Show debug/advanced view");
        
        this.buttonAwaken.setDisable(true);
        
        Region rBasicInfoSeparator = new Region();
        HBox.setHgrow(rBasicInfoSeparator, Priority.ALWAYS);
        
        HBox boxBasicInfoHUpper = new HBox(rBasicInfoSeparator, this.infoAverageMIPS);
        HBox boxBasicInfoHLower = new HBox(this.buttonAwaken, this.buttonToggleRunning, this.buttonToggleAdvanced);
        VBox boxBasicInfoV = new VBox(boxBasicInfoHUpper, boxBasicInfoHLower);
        
        this.buttonReset = new Button("Reset");
        this.buttonReload = new Button("Reload");
        
        HBox boxBasicControlsLeftH = new HBox(this.buttonReset, this.buttonReload);
        VBox boxBasicControlsLeftV = new VBox(this.infoTotalInstructions, boxBasicControlsLeftH);
        
        Region rBasicControlsSeparator = new Region();
        HBox.setHgrow(rBasicControlsSeparator, Priority.ALWAYS);
        
        HBox boxBasicControls = new HBox(boxBasicControlsLeftV, rBasicControlsSeparator, boxBasicInfoV);
        
        // advanced view
        this.infoProcessorState = new Text("");
        
        this.infoMemwatch = new Text("                                             ");
        
        Text labelMemwatch = new Text("Memwatch Address");
        this.fieldMemwatch = new TextField();
        //Region rGapMemwatch = new Region();
        //rGapMemwatch.setPrefWidth(10);
        
        HBox boxMemwatchField = new HBox(labelMemwatch, this.fieldMemwatch);
        boxMemwatchField.setAlignment(Pos.CENTER_LEFT);
        
        this.infoCurrentWatchAddress = new Text("Memwatch Address: 00000000");
        
        Text labelBreakpoint = new Text("Breakpoint");
        this.fieldBreakpoint = new TextField();
        //Region rGapBreakpoint = new Region();
        //rGapBreakpoint.setPrefWidth(10);
        
        HBox boxBreakpointField = new HBox(labelBreakpoint, this.fieldBreakpoint);
        boxBreakpointField.setAlignment(Pos.CENTER_LEFT);
        this.fieldBreakpoint.setMinWidth(200);
        
        this.infoCurrentBreakpoint = new Text("Current Breakpoint: (none)");
        this.checkEnableBreakpoints = new CheckBox("Enable Breakpoints");
        
        // TODO
        Text labelClockSpeedName = new Text("Clock Period");
        Text labelClockSpeedUnit = new Text("us");
        this.fieldClockSpeed = new TextField("0");
        HBox boxClockField = new HBox(labelClockSpeedName, this.fieldClockSpeed, labelClockSpeedUnit);
        boxClockField.setAlignment(Pos.CENTER_LEFT);
        this.fieldClockSpeed.setMinWidth(35);
        
        this.buttonStepSim = new Button("Step CPU");
        this.buttonToggleInterrupts = new Button("Toggle Interrupts");
        this.buttonToggleClock = new Button("Toggle RTC");
        
        this.buttonToggleInterrupts.setMinWidth(147);
        this.buttonToggleClock.setMinWidth(97);
        
        Region rTogglesUpperSeparator = new Region();
        HBox boxTogglesUpper = new HBox(this.buttonStepSim, rTogglesUpperSeparator, this.buttonToggleInterrupts, this.buttonToggleClock);
        HBox.setHgrow(rTogglesUpperSeparator, Priority.ALWAYS);
        
        this.buttonToggleTrace = new Button("Start Trace");
        this.buttonToggleDissassembler = new Button("Show Dissassembly");
        this.buttonToggleStackTrace = new Button("Show Stack Trace");
        HBox boxTogglesLower = new HBox(this.buttonToggleTrace, this.buttonToggleDissassembler, this.buttonToggleStackTrace);
        this.buttonToggleRunning.setMinWidth(83);
        this.buttonToggleTrace.setMinWidth(96);
        this.buttonToggleDissassembler.setMinWidth(100);
        this.buttonToggleStackTrace.setMinWidth(100);
        
        Region rAdvancedViewSeparator1 = new Region();
        Region rAdvancedViewSeparator2 = new Region();
        Region rAdvancedViewSeparator3 = new Region();
        Region rAdvancedViewSeparator4 = new Region();
        VBox.setVgrow(rAdvancedViewSeparator1, Priority.ALWAYS);
        rAdvancedViewSeparator2.setMinHeight(10);
        rAdvancedViewSeparator3.setMinHeight(10);
        rAdvancedViewSeparator4.setMinHeight(10);
        
        this.advancedView = new VBox(this.infoProcessorState, rAdvancedViewSeparator1,
                                     this.infoMemwatch, boxMemwatchField, this.infoCurrentWatchAddress, rAdvancedViewSeparator2,
                                     boxBreakpointField, this.infoCurrentBreakpoint, this.checkEnableBreakpoints, rAdvancedViewSeparator3,
                                     boxClockField, rAdvancedViewSeparator4,
                                     boxTogglesUpper, boxTogglesLower);
        
        // grid
        GridPane pain = new GridPane();
        pain.add(this.screen, 0, 0, 1, 1);
        pain.add(boxBasicControls, 0, 1, 1, 1);
        pain.add(this.advancedView, 1, 0, 1, 2);
        
        pain.setHgap(10);
        pain.setVgap(10);
        
        // bindings
        this.buttonToggleAdvanced.setOnAction(e -> toggleAdvancedView());
        this.buttonAwaken.setOnAction(e -> runInterrupt(VECTOR_NMI));
        this.buttonToggleRunning.setOnAction(e -> toggleRunSimulator());
        this.buttonStepSim.setOnAction(e -> stepSim());
        this.buttonToggleTrace.setOnAction(e -> toggleTrace());
        this.buttonToggleDissassembler.setOnAction(e -> toggleDisassembly());
        this.buttonToggleStackTrace.setOnAction(e -> toggleStackTrace());
        this.buttonReset.setOnAction(e -> restartSimulator(false));
        this.buttonReload.setOnAction(e -> restartSimulator(true));
        this.buttonToggleClock.setOnAction(e -> toggleRTC());
        this.buttonToggleInterrupts.setOnAction(e -> toggleInterrupts());
        
        
        // enable breakpoints button
        this.checkEnableBreakpoints.setOnAction(e -> {
            this.enableBreakpoints = this.checkEnableBreakpoints.isSelected();
        });
        
        // breakpoints textfield
        this.fieldBreakpoint.setOnAction(e -> {
            this.breakpointSymbol = this.fieldBreakpoint.getText();
            
            // hex
            if(this.breakpointSymbol.toLowerCase().startsWith("0x")) {
                try {
                    this.breakpointAddress = Long.parseLong(breakpointSymbol.substring(2), 16);
                } catch(Exception ex) {
                    this.breakpointSymbol = "(invalid symbol)";
                    this.breakpointAddress = -1l;
                }
            } else {
                // label
                try {
                    this.breakpointAddress = this.relocator.getReference(this.breakpointSymbol);
                } catch(Exception ex) {
                    this.breakpointSymbol = "(invalid symbol)";
                    this.breakpointAddress = -1l;
                }
            }
        });
        
        // memwatch text field
        this.fieldMemwatch.setOnAction(e -> {
            try {
                this.memwatchAddress = Long.parseLong(this.fieldMemwatch.getText(), 16);
                this.memwatchIsNumber = true;
            } catch(Exception ex1) {
                try {
                    this.memwatchAddress = this.relocator.getReference(this.fieldMemwatch.getText());
                    this.memwatchIsNumber = true;
                } catch(Exception ex2) {
                    String str = this.fieldMemwatch.getText().toUpperCase(); 
                    
                    switch(str) {
                        case "D:A":
                        case "A:B":
                        case "B:C":
                        case "C:D":
                        case "J:I":
                        case "L:K":
                        case "BP":
                        case "SP":
                        case "IP":
                            this.memwatchRegister = str;
                            this.memwatchIsNumber = false;
                            break;
                            
                        default:
                            this.memwatchAddress = 0;
                            this.memwatchIsNumber = true;
                    }
                }
            }
        });
        
        // clock speed text field
        this.fieldClockSpeed.setOnAction(e -> {
            long period = Long.parseLong(this.fieldClockSpeed.getText());
            
            if(period >= 0) {
                long oldPeriod = this.simThread.periodns;
                this.simThread.periodns = period * 1_000l;
                
                if(this.freerunEnabled) {
                    this.freerunEnabled = false;
                    
                    this.scheduler.schedule(() -> {
                        this.freerunEnabled = true;
                        notifySimulatorThread();
                    }, 1l + (2 * oldPeriod / 1_000_000), TimeUnit.MILLISECONDS);
                }
            }
        });
        
        // advanced view
        this.advancedView.managedProperty().bind(this.advancedView.visibleProperty());
        this.advancedView.setVisible(this.advancedViewVisisble);
        ((VBox)this.advancedView).setMinWidth(300);
        
        Scene scene = new Scene(pain);
        scene.getStylesheets().add(getClass().getResource("resources/application.css").toExternalForm());
        
        // keyboard stuff
        Map<KeyCode, Boolean> keyTracker = new HashMap<>();
        
        screen.addEventHandler(KeyEvent.KEY_PRESSED, (ke) -> {
            KeyCode code = ke.getCode();
            
            //System.out.println("key pressed " + code);
            
            if(!keyTracker.getOrDefault(code, false)) {
                keyTracker.put(code, true);
                this.keyboardBufferController.write4Bytes(0, code.getCode());
                
                //System.out.printf("%04X%n", code.getCode());
                //System.out.println("interrupt sent " + code);
                runInterrupt(VECTOR_KEYDOWN);
            }
            
            // don't let traversal stuff or anything happen
            ke.consume();
        });
        
        screen.addEventHandler(KeyEvent.KEY_RELEASED, (ke) -> {
            KeyCode code = ke.getCode();
            
            //System.out.println("key released " + code);
            
            if(keyTracker.getOrDefault(code, false)) {
                keyTracker.put(code, false);
                this.keyboardBufferController.write4Bytes(0, code.getCode());
                
                runInterrupt(VECTOR_KEYUP);
            }
            
            ke.consume();
        });
        
        // make the screen properly focusable
        screen.addEventHandler(MouseEvent.MOUSE_PRESSED, (me) -> {
            screen.requestFocus();
        });
        
        // show the window
        this.stage.setTitle("NST Emulator");
        this.stage.setScene(scene);
        this.stage.show();
        this.stage.setResizable(false);
        
        this.stage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });
        
        // start periodic UI updates
        this.uiUpdateFuture = this.scheduler.scheduleAtFixedRate(() -> updateUI(), 0l, 1_000_000 / 30, TimeUnit.MICROSECONDS);
        
        screen.requestFocus();
    }
    
    /**
     * Update information for UI objects
     */
    private void updateUI() {
        Platform.runLater(() -> {
            // calculate average mips every half second
            long time = System.nanoTime() - this.elapsedTimens;
            if(time > 1_000_000_000l) {
                long executed = this.instructionsExecutedLast;
                this.instructionsExecutedLast = 0;
                this.elapsedTimens = System.nanoTime();
                
                double mips = (((double) executed) / ((double) time)) * 1_000_000_000;
                this.lastAverageMIPS = mips;
                this.instructionsExecutedTotal += executed;
            }
            
            // basic info
            this.infoTotalInstructions.setText(String.format("Total instructions: %,d", this.instructionsExecutedTotal));
            this.infoAverageMIPS.setText(String.format("Average IPS: %,6.0f", this.lastAverageMIPS));
            
            this.buttonToggleRunning.setText(this.freerunEnabled ? "Stop CPU" : "Start CPU");
            this.buttonToggleTrace.setText(this.traceEnabled ? "Stop Trace" : "Start Trace");
            this.buttonToggleStackTrace.setText(this.stackTraceEnabled ? "Hide Stack Trace" : "Show Stack Trace");
            this.buttonToggleDissassembler.setText(this.disassemblyEnabled ? "Hide Disassembly" : "Show Disassembly");
            this.buttonToggleClock.setText(this.rtcEnabled ? "Disable RTC" : "Enable RTC");
            this.buttonToggleInterrupts.setText((this.sim.getRegPF() & 0x0001) == 0 ? "Enable Interrupts" : "Disable Interrupts");
            
            if(this.freerunEnabled && this.sim.getHalted()) {
                this.buttonAwaken.setDisable(false);
            } else {
                this.buttonAwaken.setDisable(true);
            }
            
            //this.screen.update(this.videoBufferArray, 0);
            this.screen.update(this.screenBufferController, 0);
            
            // advanced info
            if(this.advancedViewVisisble) {
                this.buttonToggleAdvanced.setText("Hide advanced/debug view");
                
                // processor state
                Disassembler dis = new Disassembler();
                String state = "-- Processor State --\n";
                
                state += String.format("A    B    C    D%n%04X %04X %04X %04X%nI    J    K    L%n%04X %04X %04X %04X%nF    PF%n%04X %04X%nip       bp       sp%n%08X %08X %08X%n",
                        sim.getRegA(), sim.getRegB(), sim.getRegC(), sim.getRegD(),
                        sim.getRegI(), sim.getRegJ(), sim.getRegK(), sim.getRegL(),
                        sim.getRegF(), sim.getRegPF(),
                        sim.getRegIP(), sim.getRegBP(), sim.getRegSP());
                
                try {
                    state += dis.disassemble(this.mmu, Integer.toUnsignedLong(sim.getRegIP())) + "\n";
                    
                    for(int j = 0; j < dis.getLastInstructionLength(); j++) {
                        byte b = this.mmu.readBytePrivileged(Integer.toUnsignedLong(sim.getRegIP()) + ((long) j));
                        state += String.format("%02X ", b);
                    }
                } catch(IndexOutOfBoundsException e) {
                } catch(NullPointerException e) {}
                
                state += "\n\n" + this.relocator.getAddressName(Integer.toUnsignedLong(sim.getRegIP()));
                
                // If watching a pointer, update address
                if(!this.memwatchIsNumber) {
                    this.memwatchAddress = switch(this.memwatchRegister) {
                        case "D:A"  -> ((this.sim.getRegD() << 16) | this.sim.getRegA()) & 0xFFFF_FFFFl;
                        case "A:B"  -> ((this.sim.getRegA() << 16) | this.sim.getRegB()) & 0xFFFF_FFFFl;
                        case "B:C"  -> ((this.sim.getRegB() << 16) | this.sim.getRegC()) & 0xFFFF_FFFFl;
                        case "C:D"  -> ((this.sim.getRegC() << 16) | this.sim.getRegD()) & 0xFFFF_FFFFl;
                        case "J:I"  -> ((this.sim.getRegJ() << 16) | this.sim.getRegI()) & 0xFFFF_FFFFl;
                        case "L:K"  -> ((this.sim.getRegL() << 16) | this.sim.getRegK()) & 0xFFFF_FFFFl;
                        case "BP"   -> this.sim.getRegBP() & 0xFFFF_FFFFl;
                        case "SP"   -> this.sim.getRegSP() & 0xFFFF_FFFFl;
                        case "IP"   -> this.sim.getRegIP() & 0xFFFF_FFFFl;
                        default     -> 0l;
                    };
                }
                
                // trace/disassembly
                if(this.traceEnabled) {
                    state += "\n\n";
                    
                    ArrayList<String> traceCopy = new ArrayList<>(this.instructionTrace);
                    
                    for(String s : traceCopy) {
                        state += (s != null ? s : "") + "\n";
                    }
                } else if(this.disassemblyEnabled) {
                    state += "\n\n";
                    
                    // disassemble memwatch area
                    for(int i = 0, j = 0; i < 64 && j < TRACE_SIZE; j++) {
                        String disasm = dis.disassemble(this.mmu, this.memwatchAddress + i),
                               disBytes = "";
                        
                        int bytes = dis.getLastInstructionLength();
                        
                        for(int k = 0; k < bytes; k++) {
                            byte l;
                            
                            try {
                                l = this.mmu.readBytePrivileged(this.memwatchAddress + i + k);
                            } catch(IndexOutOfBoundsException e) {
                                l = 0;
                                j = TRACE_SIZE; // end disassembly
                                break;
                            }
                            
                            disBytes += String.format("%02X ", l);
                        }
                        
                        if(j == TRACE_SIZE) {
                            state += String.format("%08X: Out of bounds.%n", this.memwatchAddress + i);
                        } else {
                            state += String.format("%08X: %-24s%s %n", this.memwatchAddress + i, disBytes, disasm);
                        }
                        
                        i += bytes;
                    }
                } else if(this.stackTraceEnabled) {
                    state += "\n\nAddress   BP       Label\n";
                    
                    List<String> trace = new ArrayList<>();
                    
                    long bpAddr = this.sim.getRegBP() & 0xFFFFFFFFl,
                         retAddr = this.sim.getRegIP() & 0xFFFFFFFFl;
                    
                    for(int i = 0; i < TRACE_SIZE; i++) {
                        // trace
                        String functionLabel = this.relocator.getNearestBelow(retAddr);
                        
                        trace.add(String.format("%08X %08X %s%n", retAddr, bpAddr, functionLabel)); 
                        
                        // read from [BP] = previous BP
                        // read from [BP + 4] = return address
                        try {
                            retAddr = this.mmu.read4BytesPrivileged(bpAddr + 4) & 0xFFFFFFFFl;
                            bpAddr = this.mmu.read4BytesPrivileged(bpAddr) & 0xFFFFFFFFl;
                        } catch(IndexOutOfBoundsException e) {
                            break;
                        }
                        
                        if(bpAddr == 0 || retAddr == 0) break;
                    }
                    
                    for(int i = trace.size() - 1; i >= 0; i--) {
                        state += trace.get(i);
                    }
                }
                
                this.infoProcessorState.setText(state);
                
                // memwatch
                String memwatch = "";
                
                for(int i = 0; i < 64; i += 8) {
                    try {
                        int firstFour = this.mmu.read4BytesPrivileged(this.memwatchAddress + i),
                            secondFour = this.mmu.read4BytesPrivileged(this.memwatchAddress + i + 4);
                        
                        byte[] bytes = new byte[] {
                            (byte)(firstFour & 0xFF),           (byte)((firstFour >> 8) & 0xFF),
                            (byte)((firstFour >> 16) & 0xFF),   (byte)((firstFour >> 24) & 0xFF),
                            (byte)(secondFour & 0xFF),          (byte)((secondFour >> 8) & 0xFF),
                            (byte)((secondFour >> 16) & 0xFF),  (byte)((secondFour >> 24) & 0xFF)
                        };
                        
                        String chars = "";
                        
                        for(int j = 0; j < 8; j++) {
                            char c;
                            
                            if((bytes[j] & 0x7F) < 0x7F && (bytes[j] & 0x7F) > 0x1F) {
                                c = (char)(bytes[j]);
                            } else {
                                c = '.';
                            }
                            
                            chars += c;
                        }
                        
                        memwatch += String.format("%08X: %02X %02X %02X %02X %02X %02X %02X %02X |%s| %n",
                                                  this.memwatchAddress + i,
                                                  bytes[0], bytes[1], bytes[2], bytes[3],
                                                  bytes[4], bytes[5], bytes[6], bytes[7],
                                                  chars);
                    } catch(IndexOutOfBoundsException e) { 
                        memwatch += String.format("%08X: out of bounds%n", this.memwatchAddress + i);
                    }
                }
                
                String nearestSymbol = this.relocator.getNearest(this.memwatchAddress);
                long nearestAddr = this.relocator.getReference(nearestSymbol);
                
                this.infoMemwatch.setText(memwatch);
                this.infoCurrentWatchAddress.setText(String.format("Current Memwatch Address: %08X%nNearest Label: %s%n               (%08X)", this.memwatchAddress, nearestSymbol, nearestAddr));
                
                // breakpoints
                if(!this.breakpointSymbol.equals("")) {
                    this.infoCurrentBreakpoint.setText("Current Breakpoint: " + this.breakpointSymbol);
                } else {
                    this.infoCurrentBreakpoint.setText("Current Breakpoint: (none)");
                }
            } else {
                this.buttonToggleAdvanced.setText("Show advanced/debug view");
            }
            
            this.stage.sizeToScene();
        });
        
        //printState();
    }
    
    /**
     * Toggles the advanced info panel
     */
    private void toggleAdvancedView() {
        this.advancedViewVisisble = !this.advancedViewVisisble;
        this.advancedView.setVisible(this.advancedViewVisisble);
        this.stage.sizeToScene();
    }
}
