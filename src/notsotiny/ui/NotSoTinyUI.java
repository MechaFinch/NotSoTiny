package notsotiny.ui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
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
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import notsotiny.asm.Disassembler;
import notsotiny.sim.NotSoTinySimulator;
import notsotiny.sim.memory.FlatMemoryController;
import notsotiny.sim.memory.Halter;
import notsotiny.sim.memory.MemoryManager;
import notsotiny.sim.memory.SoundInterfaceController;

public class NotSoTinyUI extends Application {

    public static void main(String[] args) {
        System.out.println("launching");
        Application.launch(args);
    }
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        this.stage = primaryStage;
        
        // TODO Auto-generated method stub
        initSimulator();
        createUI();
    }
    
    /*
     * == SIMULATION ==
     */
    
    // RAM segment constants
    private static final long //IVT_START = 0x0000_0000l,
                              //IVT_END = 0x0000_0400l,
                              PROGRAM_START = 0x0000_0000l,
                              PROGRAM_END = 0x0004_0000l,
                              HEAP_START = 0x8000_0000l,
                              HEAP_END = 0x8004_0000l,
                              VIDEO_START = 0xC000_0000l,
                              VIDEO_END = 0xC001_5000l,
                              CHARACTER_SET_START = 0xC001_3000l,
                              STACK_START = 0xFFFE_0000l,
                              STACK_END = 0x1_0000_0000l;
    
    // MMIO constants
    private static final long HALTER_ADDRESS = 0xF000_0000l,
                              SIC_ADDRESS = 0xF000_0002l,
                              KEYBOARD_BUFFER_ADDRESS = 0xF000_0006l;
    
    // other constants
    /*
    private static final String PROGRAM_DATA_FOLDER = "data\\",
                                PROGRAM_EXEC_FILE = "game.oex",
                                TEXT_FONT_FILE = "text.dat";
    */
    
    private static final String PROGRAM_DATA_FOLDER = "C:\\Users\\wetca\\Desktop\\silly  code\\architecture\\NotSoTiny\\programming\\calculator\\",
                                PROGRAM_EXEC_FILE = "test.oex",
                                TEXT_FONT_FILE = "text.dat";
    
    
    // sim vars
    private NotSoTinySimulator sim;
    
    private MemoryManager mmu;
    
    private FlatMemoryController //ivtRAMController,          // 0x0000_0000 - 0x0000_03FF
                                 programRAMController,      // 0x0000_0400 - 0x0003_FFFF
                                 heapRAMController,         // 0x8000_0000 - 0x8003_FFFF
                                 videoRAMController,        // 0xC000_0000 - 0xC001_33FF
                                 stackRAMController,        // 0xFFFE_0000 - 0xFFFF_FFFF
                                 keyboardBufferController;  // 0xF000_0006 - 0xF000_0007
    
    // halter is deprecated by a HLT instruction but legacy programs yadda yadda
    private Halter halter;                              // 0xF000_0000 - 0xF000_0001
    
    private SoundInterfaceController sic;               // 0xF000_0002 - 0xF000_0005
    
    private Relocator relocator;
    
    // actually memory arrays
    private byte[] //ivtRAM,
                   programRAM,
                   heapRAM,
                   videoRAM,
                   stackRAM,
                   keyboardBuffer;
    
    // real time clock stuff
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    private SimulatorThread simThread;
    
    /**
     * Thread for running the simulator
     * 
     * @author Mechafinch
     */
    private class SimulatorThread extends Thread {
        private long periodus = 0;
        
        private ScheduledFuture<?> clockHandler;
        
        /**
         * Constructor
         * 
         * @param periodus period in microseconds
         */
        public SimulatorThread(long periodus) {
            this.periodus = periodus;
        }
        
        @Override
        public void run() {
            while(true) {
                try {
                    // run as fast as possible or no?
                    if(this.periodus == 0) {
                        //System.out.println("running fast");
                        this.clockHandler = null;
                        
                        // just run until halted
                        while(!(NotSoTinyUI.this.halter.halted() || NotSoTinyUI.this.sim.getHalted())) {
                            step();
                        }
                    } else if(this.clockHandler == null || this.clockHandler.isDone()){
                        
                        //System.out.println("running slow");
                        // run at the given rate
                        if(!(NotSoTinyUI.this.halter.halted() || NotSoTinyUI.this.sim.getHalted())) {
                            this.clockHandler = NotSoTinyUI.this.scheduler.scheduleAtFixedRate(() -> this.step(), 0, this.periodus, TimeUnit.MICROSECONDS);
                        }
                    }
                    
                    synchronized(this) {
                        wait();
                    }
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
            if(NotSoTinyUI.this.halter.halted() || NotSoTinyUI.this.sim.getHalted()) {
                this.stopSim();
            } else {
                try { 
                    NotSoTinyUI.this.sim.step();
                } catch(Exception e) {
                    e.printStackTrace();
                    throw e;
                }
                NotSoTinyUI.this.instructionsExecuted++;
            }
            
            if(NotSoTinyUI.this.enableBreakpoints && NotSoTinyUI.this.breakpointAddress != -1l) {
                if(NotSoTinyUI.this.sim.getRegIP() == (NotSoTinyUI.this.breakpointAddress & 0xFFFF_FFFF)) {
                    //NotSoTinyUI.this.halter.writeByte(0, (byte) 0);
                    NotSoTinyUI.this.sim.setHalted(true);
                }
            }
        }
        
        //public void setPeriod(long p) { this.periodus = p; }
    }
    
    // misc
    private long instructionsExecuted, // tracks since UI was last updated
                 elapsedTimens,
                 breakpointAddress = -1l,
                 memwatchAddress = 0;
    
    private double lastAverageMIPS = 0;
    
    private boolean enableBreakpoints = false;
    
    private String breakpointSymbol = "";
    
    /**
     * Initialize the simulator
     * 
     * @throws MidiUnavailableException 
     * @throws IOException 
     */
    private void initSimulator() throws MidiUnavailableException, IOException {
        // initialize flat memory segments
        //ivtRAM = new byte[(int)(IVT_END - IVT_START)];
        programRAM = new byte[(int)(PROGRAM_END - PROGRAM_START)];
        heapRAM = new byte[(int)(HEAP_END - HEAP_START)];
        videoRAM = new byte[(int)(VIDEO_END - VIDEO_START)];
        stackRAM = new byte[(int)(STACK_END - STACK_START)];
        keyboardBuffer = new byte[2];
        
        //ivtRAMController = new FlatMemoryController(ivtRAM);
        programRAMController = new FlatMemoryController(programRAM);
        heapRAMController = new FlatMemoryController(heapRAM);
        videoRAMController = new FlatMemoryController(videoRAM);
        stackRAMController = new FlatMemoryController(stackRAM);
        keyboardBufferController = new FlatMemoryController(keyboardBuffer);
        
        // initialize other segments
        halter = new Halter();
        sic = new SoundInterfaceController();
        
        // initialize memory manager
        this.mmu = new MemoryManager();
        
        //this.mmu.registerSegment(ivtRAMController, IVT_START, IVT_END - IVT_START);
        this.mmu.registerSegment(programRAMController, PROGRAM_START, PROGRAM_END - PROGRAM_START);
        this.mmu.registerSegment(heapRAMController, HEAP_START, HEAP_END - HEAP_START);
        this.mmu.registerSegment(videoRAMController, VIDEO_START, VIDEO_END - VIDEO_START);
        this.mmu.registerSegment(stackRAMController, STACK_START, STACK_END - STACK_START);
        
        this.mmu.registerSegment(halter, HALTER_ADDRESS, 2);
        this.mmu.registerSegment(sic, SIC_ADDRESS, 4);
        this.mmu.registerSegment(keyboardBufferController, KEYBOARD_BUFFER_ADDRESS, 2);
        
        // load text font
        byte[] font = Files.readAllBytes(new File(PROGRAM_DATA_FOLDER + TEXT_FONT_FILE).toPath());
        System.arraycopy(font, 0, this.videoRAM, (int)(CHARACTER_SET_START - VIDEO_START), font.length);
        
        // Load program into memory
        List<Object> relocatorPair = ExecLoader.loadExecFileToRelocator(new File(PROGRAM_DATA_FOLDER + PROGRAM_EXEC_FILE));
        
        this.relocator = (Relocator) relocatorPair.get(0);
        String entrySymbol = (String) relocatorPair.get(1);
        
        int entry = ExecLoader.loadRelocator(this.relocator, entrySymbol, programRAM, 0, 0);
        
        // write entry vector
        this.mmu.write4Bytes(0, entry);
        
        // simulator
        this.sim = new NotSoTinySimulator(this.mmu);
        
        // timing stuff
        this.simThread = new SimulatorThread(5);
        
        this.simThread.start();
        
        // start seconds clock
        /*
        this.scheduler.scheduleAtFixedRate(() -> {
            this.sim.fireNonMaskableInterrupt((byte) 1);
            this.halter.clear();
            notifySimulatorThread();
        }, 2000l, 100l, TimeUnit.MILLISECONDS);
        */
        
        //this.halter.writeByte(0l, (byte) 0);
        //this.sim.setHalted(true);
    }
    
    /**
     * Toggles the simulator
     */
    private void toggleRunSimulator() {
        if(this.halter.halted()) {
            this.halter.clear();
            this.sim.setHalted(false);
            notifySimulatorThread();
        } else {
            this.halter.writeByte(0l, (byte) 0);
            this.sim.setHalted(true);
        }
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
     * Step the simulator
     */
    private void stepSim() {
        this.sim.step();
        this.instructionsExecuted++;
        
        if(this.enableBreakpoints && this.breakpointAddress != -1l) {
            if(this.sim.getRegIP() == (this.breakpointAddress & 0xFFFF_FFFF)) {
                //this.halter.writeByte(0, (byte) 0);
                this.sim.setHalted(true);
            }
        }
    }
    
    /**
     * Handles a key being typed
     * 
     * @param ke
     */
    private void handleKeyTyped(KeyEvent ke) {
        this.keyboardBuffer[0] = (byte) ke.getCharacter().charAt(0);
        
        this.sim.fireMaskableInterrupt((byte) 2);
        this.halter.clear();
        notifySimulatorThread();
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
                 infoProcessorState,
                 infoCurrentBreakpoint,
                 infoCurrentWatchAddress,
                 infoMemwatch;
    
    private TextField fieldBreakpoint,
                      fieldMemwatch;
    
    private Button buttonToggleAdvanced,
                   buttonToggleRunning,
                   buttonStepSim;
    
    private CheckBox checkEnableBreakpoints;
    
    private Node advancedView;
    
    private boolean advancedViewVisisble = false;
    
    /**
     * Initialize the UI 
     */
    private void createUI() {
        // layout tree
        // initialize components in post-order
        // screen
        this.screen = new Screen(320, 240, 3);
        
        // basic info
        this.infoAverageMIPS = new Text("Average MIPS:  0.00");
        this.buttonToggleAdvanced = new Button("Show debug/advanced view");
        
        VBox boxBasicInfo = new VBox(this.infoAverageMIPS, this.buttonToggleAdvanced);
        
        Region rBasicControlsSeparator = new Region();
        HBox.setHgrow(rBasicControlsSeparator, Priority.ALWAYS);
        
        HBox boxBasicControls = new HBox(rBasicControlsSeparator, boxBasicInfo);
        
        // advanced view
        this.infoProcessorState = new Text("");
        
        this.infoMemwatch = new Text("");
        
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
        
        this.buttonToggleRunning = new Button("Start CPU");
        this.buttonStepSim = new Button("Step CPU");
        HBox boxToggleStep = new HBox(buttonToggleRunning, buttonStepSim);
        this.buttonToggleRunning.setMinWidth(83);
        
        Region rAdvancedViewSeparator1 = new Region();
        Region rAdvancedViewSeparator2 = new Region();
        Region rAdvancedViewSeparator3 = new Region();
        VBox.setVgrow(rAdvancedViewSeparator1, Priority.ALWAYS);
        rAdvancedViewSeparator2.setMinHeight(10);
        rAdvancedViewSeparator3.setMinHeight(10);
        
        this.advancedView = new VBox(this.infoProcessorState, rAdvancedViewSeparator1,
                                     this.infoMemwatch, boxMemwatchField, this.infoCurrentWatchAddress, rAdvancedViewSeparator2,
                                     boxBreakpointField, this.infoCurrentBreakpoint, this.checkEnableBreakpoints, rAdvancedViewSeparator3,
                                     boxToggleStep);
        
        // grid
        GridPane pain = new GridPane();
        pain.add(this.screen, 0, 0, 1, 1);
        pain.add(boxBasicControls, 0, 1, 1, 1);
        pain.add(this.advancedView, 1, 0, 1, 2);
        
        pain.setHgap(10);
        pain.setVgap(10);
        
        // bindings
        this.buttonToggleAdvanced.setOnAction(e -> toggleAdvancedView());
        this.buttonToggleRunning.setOnAction(e -> toggleRunSimulator());
        
        this.buttonStepSim.setOnAction(e -> {
            this.halter.clear();
            this.sim.setHalted(false);
            stepSim();
        });
        
        this.checkEnableBreakpoints.setOnAction(e -> {
            this.enableBreakpoints = this.checkEnableBreakpoints.isSelected();
        });
        
        this.fieldBreakpoint.setOnAction(e -> {
            this.breakpointSymbol = this.fieldBreakpoint.getText();
            
            try {
                this.breakpointAddress = this.relocator.getReference(this.breakpointSymbol);
            } catch(Exception ex) {
                this.breakpointSymbol = "(invalid symbol)";
                this.breakpointAddress = -1l;
            }
        });
        
        this.fieldMemwatch.setOnAction(e -> {
            try {
                this.memwatchAddress = Long.parseLong(this.fieldMemwatch.getText(), 16);
            } catch(Exception ex1) {
                try {
                    this.memwatchAddress = this.relocator.getReference(this.fieldMemwatch.getText());
                } catch(Exception ex2) {
                    this.breakpointAddress = 0;
                }
            }
        });
        
        this.advancedView.managedProperty().bind(this.advancedView.visibleProperty());
        this.advancedView.setVisible(this.advancedViewVisisble);
        ((VBox)this.advancedView).setMinWidth(300);
        
        Scene scene = new Scene(pain);
        scene.getStylesheets().add(getClass().getResource("resources/application.css").toExternalForm());
        
        scene.addEventHandler(KeyEvent.KEY_TYPED, (key) -> handleKeyTyped(key));
        //scene.addEventHandler(KeyEvent.KEY_PRESSED, null);
        
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
        this.scheduler.scheduleAtFixedRate(() -> updateUI(), 0l, 1_000_000 / 30, TimeUnit.MICROSECONDS);
    }
    
    /**
     * Update information for UI objects
     */
    private void updateUI() {
        Platform.runLater(() -> {
            // calculate average mips every half second
            long time = System.nanoTime() - this.elapsedTimens;
            if(time > 1_000_000_000l) {
                long executed = this.instructionsExecuted;
                this.instructionsExecuted = 0;
                this.elapsedTimens = System.nanoTime();
                
                double mips = (((double) executed) / ((double) time)) * 1_000_000_000;
                this.lastAverageMIPS = mips;
            }
            
            // basic info
            this.infoAverageMIPS.setText(String.format("Average IPS: %6.0f", this.lastAverageMIPS));
            
            this.buttonToggleRunning.setText(this.sim.getHalted() ? "Start CPU" : "Stop CPU");
            
            this.screen.update(this.videoRAM, 0);
            
            // advanced info
            if(this.advancedViewVisisble) {
                this.buttonToggleAdvanced.setText("Hide advanced/debug view");
                
                // TODO advanced view
                // processor state
                Disassembler dis = new Disassembler();
                String state = "-- Processor State --\n";
                
                state += String.format("A    B    C    D%n%04X %04X %04X %04X%nI    J    K    L%n%04X %04X %04X %04X%nF    PF%n%04X %04X%nip       bp       sp%n%08X %08X %08X%n",
                        sim.getRegA(), sim.getRegB(), sim.getRegC(), sim.getRegD(),
                        sim.getRegI(), sim.getRegJ(), sim.getRegK(), sim.getRegL(),
                        sim.getRegF(), sim.getRegPF(),
                        sim.getRegIP(), sim.getRegBP(), sim.getRegSP());
                
                try {
                    state += dis.disassemble(this.programRAM, sim.getRegIP() - (int) PROGRAM_START) + "\n";
                } catch(ArrayIndexOutOfBoundsException e) {}
                
                for(int j = 0; j < dis.getLastInstructionLength(); j++) {
                    state += String.format("%02X ", this.programRAM[sim.getRegIP() + j - (int) PROGRAM_START]);
                }
                
                state += "\n\n" + this.relocator.getAddressName(sim.getRegIP());
                
                this.infoProcessorState.setText(state);
                
                // memwatch
                String memwatch = "";
                
                for(int i = 0; i < 16; i++) {
                    memwatch += String.format("%02X ", this.mmu.readByte(this.memwatchAddress + i));
                    if(i % 8 == 7) memwatch += "\n";
                }
                
                this.infoMemwatch.setText(memwatch);
                this.infoCurrentWatchAddress.setText(String.format("Current Memwatch Address: %08X", this.memwatchAddress));
                
                // breakpoints
                if(!this.breakpointSymbol.equals("")) {
                    this.infoCurrentBreakpoint.setText("Current Breakpoint: " + this.breakpointSymbol);
                } else {
                    this.infoCurrentBreakpoint.setText("Current Breakpoint: (none)");
                }
            } else {
                this.buttonToggleAdvanced.setText("Show advanced/debug view");
            }
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
