package notsotiny.ui;

import java.awt.KeyboardFocusManager;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
import notsotiny.sim.memory.FlatMemoryController;
import notsotiny.sim.memory.Halter;
import notsotiny.sim.memory.MemoryManager;
import notsotiny.sim.memory.RandomController;
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
    
    // memory map constants
    private static final long LOWRAM_START =    0x0000_0000,
                              SPI_START =       0x8000_0000,
                              KEYPAD_START =    0x8001_0000,
                              CC_START =        0x8002_0000,
                              KEYBOARD_START =  0xF000_0000,
                              SOUND_START =     0xF001_0000,
                              VIDEO_START =     0xF002_0000,
                              RANDOM_START =    0xF004_0000,
                              BOOTROM_START =   0xFFFF_FC00;
    
    private static final int LOWRAM_SIZE =      0x0010_0000,
                             SPI_SIZE =         0x0000_0004,
                             KEYPAD_SIZE =      0x0000_0008,
                             CC_SIZE =          0x0000_0002,
                             KEYBOARD_SIZE =    0x0000_0002,
                             SOUND_SIZE =       0x0000_0004,
                             VIDEO_SIZE =       0x0002_0000,
                             RANDOM_SIZE =      0x0000_0004,
                             BOOTROM_SIZE =     0x0000_0400;
    
    private static final int VIDEO_BUFFER_SIZE =    0x0001_4000,
                             VIDEO_CHARSET_SIZE =   0x0000_1000,
                             VIDEO_OTHER_SIZE =     0x0002_0000 - (VIDEO_BUFFER_SIZE + VIDEO_CHARSET_SIZE);
    
    // other constants
    /*
    private static final String PROGRAM_DATA_FOLDER = "data\\",
                                PROGRAM_EXEC_FILE = "game.oex",
                                TEXT_FONT_FILE = "text.dat";
    */
    
    private static final String PROGRAM_DATA_FOLDER = "C:\\Users\\wetca\\Desktop\\silly  code\\architecture\\NotSoTiny\\programming\\high level\\test\\",
                                PROGRAM_EXEC_FILE = "test1.oex",
                                TEXT_FONT_FILE = "C:\\Users\\wetca\\Desktop\\silly  code\\architecture\\NotSoTiny\\programming\\standard library\\simvideo\\textsmall.dat";
    
    
    // sim vars
    private NotSoTinySimulator sim;
    
    private MemoryManager mmu;
    
    private FlatMemoryController lowramController,
                                 placeholder_spiController,
                                 placeholder_cacheController,
                                 keyboardBufferController,
                                 videoBufferController,
                                 videoCharsetController,
                                 videoOtherController,
                                 bootromController;
    
    // halter is deprecated by a HLT instruction but legacy code
    private Halter halter;  
    
    private SoundInterfaceController sic;
    
    private RandomController rand;
    
    private Relocator relocator;
    
    // actual memory arrays
    private byte[] lowramArray,
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
                        while(!(NotSoTinyUI.this.halter.halted() || NotSoTinyUI.this.sim.getHalted())) {
                            step();
                        }
                    } else if(this.clockHandler == null || this.clockHandler.isDone()){
                        
                        //System.out.println("running slow");
                        // run at the given rate
                        if(!(NotSoTinyUI.this.halter.halted() || NotSoTinyUI.this.sim.getHalted())) {
                            this.clockHandler = NotSoTinyUI.this.scheduler.scheduleAtFixedRate(() -> this.step(), 0, this.periodns, TimeUnit.NANOSECONDS);
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
                    NotSoTinyUI.this.sim.setHalted(true);
                    this.stopSim();
                    throw e;
                }
                NotSoTinyUI.this.instructionsExecutedLast++;
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
    private long instructionsExecutedLast, // tracks since UI was last updated
                 instructionsExecutedTotal,
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
        // initialize flat memory segments]
        lowramArray = new byte[LOWRAM_SIZE];
        placeholder_spiArray = new byte[SPI_SIZE];
        placeholder_cacheArray = new byte[CC_SIZE];
        keyboardBufferArray = new byte[KEYBOARD_SIZE];
        videoBufferArray = new byte[VIDEO_BUFFER_SIZE];
        videoCharsetArray = new byte[VIDEO_CHARSET_SIZE];
        videoOtherArray = new byte[VIDEO_OTHER_SIZE];
        bootromArray = new byte[BOOTROM_SIZE];
        
        lowramController = new FlatMemoryController(lowramArray);
        placeholder_spiController = new FlatMemoryController(placeholder_spiArray);
        placeholder_cacheController = new FlatMemoryController(placeholder_cacheArray);
        keyboardBufferController = new FlatMemoryController(keyboardBufferArray);
        videoBufferController = new FlatMemoryController(videoBufferArray);
        videoCharsetController = new FlatMemoryController(videoCharsetArray);
        videoOtherController = new FlatMemoryController(videoOtherArray);
        bootromController = new FlatMemoryController(bootromArray);
        
        // initialize other segments
        halter = new Halter();
        sic = new SoundInterfaceController();
        rand = new RandomController();
        
        // initialize memory manager
        this.mmu = new MemoryManager();
        
        this.mmu.registerSegment(lowramController, LOWRAM_START, LOWRAM_SIZE);
        this.mmu.registerSegment(placeholder_spiController, SPI_START, SPI_SIZE);
        this.mmu.registerSegment(placeholder_cacheController, CC_START, CC_SIZE);
        this.mmu.registerSegment(keyboardBufferController, KEYBOARD_START, KEYBOARD_SIZE);
        this.mmu.registerSegment(videoBufferController, VIDEO_START, VIDEO_BUFFER_SIZE);
        this.mmu.registerSegment(videoCharsetController, VIDEO_START + VIDEO_BUFFER_SIZE, VIDEO_CHARSET_SIZE);
        this.mmu.registerSegment(videoOtherController, VIDEO_START + VIDEO_BUFFER_SIZE + VIDEO_CHARSET_SIZE, VIDEO_OTHER_SIZE);
        this.mmu.registerSegment(bootromController, BOOTROM_START, BOOTROM_SIZE);
        
        this.mmu.registerSegment(sic, SOUND_START, SOUND_SIZE);
        this.mmu.registerSegment(rand, RANDOM_START, RANDOM_SIZE);
        
        //this.mmu.printMap();
        
        // load text font
        byte[] font = Files.readAllBytes(Paths.get(TEXT_FONT_FILE));
        System.arraycopy(font, 0, videoCharsetArray, 0, font.length);
        
        // Load program into memory
        List<Object> relocatorPair = ExecLoader.loadExecFileToRelocator(new File(PROGRAM_DATA_FOLDER + PROGRAM_EXEC_FILE));
        
        this.relocator = (Relocator) relocatorPair.get(0);
        String entrySymbol = (String) relocatorPair.get(1);
        
        long entry = ExecLoader.loadRelocator(this.relocator, entrySymbol, lowramArray, 0, 0);
        
        // write entry vector
        this.mmu.write4Bytes(0, (int) entry);
        
        // simulator
        this.sim = new NotSoTinySimulator(this.mmu);
        this.sim.setRegSP((int)(LOWRAM_START + LOWRAM_SIZE));
        
        // timing stuff
        this.simThread = new SimulatorThread(10_000);
        
        this.simThread.start();
        
        // start seconds clock
        /*
        this.scheduler.scheduleAtFixedRate(() -> {
            this.sim.fireMaskableInterrupt((byte) 1);
            this.halter.clear();
            notifySimulatorThread();
        }, 1_000_000l, 10_000l, TimeUnit.MICROSECONDS);
        */
        // 100 hz
        
        //this.halter.writeByte(0l, (byte) 0);
        this.sim.setHalted(true);
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
        this.infoTotalInstructions = new Text("Total instrctions: 0");
        this.infoAverageMIPS = new Text("Average MIPS:  0.00");
        this.buttonToggleAdvanced = new Button("Show debug/advanced view");
        
        VBox boxBasicInfo = new VBox(this.infoAverageMIPS, this.buttonToggleAdvanced);
        
        Region rBasicControlsSeparator = new Region();
        HBox.setHgrow(rBasicControlsSeparator, Priority.ALWAYS);
        
        HBox boxBasicControls = new HBox(this.infoTotalInstructions, rBasicControlsSeparator, boxBasicInfo);
        
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
        
        // step button
        this.buttonStepSim.setOnAction(e -> {
            this.halter.clear();
            this.sim.setHalted(false);
            stepSim();
        });
        
        // enable breakpoints button
        this.checkEnableBreakpoints.setOnAction(e -> {
            this.enableBreakpoints = this.checkEnableBreakpoints.isSelected();
        });
        
        // breakpoints textfield
        this.fieldBreakpoint.setOnAction(e -> {
            this.breakpointSymbol = this.fieldBreakpoint.getText();
            
            try {
                this.breakpointAddress = this.relocator.getReference(this.breakpointSymbol);
            } catch(Exception ex) {
                this.breakpointSymbol = "(invalid symbol)";
                this.breakpointAddress = -1l;
            }
        });
        
        // memwatch text field
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
        
        // advanced view
        this.advancedView.managedProperty().bind(this.advancedView.visibleProperty());
        this.advancedView.setVisible(this.advancedViewVisisble);
        ((VBox)this.advancedView).setMinWidth(300);
        
        Scene scene = new Scene(pain);
        scene.getStylesheets().add(getClass().getResource("resources/application.css").toExternalForm());
        
        /*
        scene.addEventHandler(KeyEvent.KEY_TYPED, (ke) -> {
            this.keyboardBuffer[0] = (byte) ke.getCharacter().charAt(0);
            
            this.sim.fireMaskableInterrupt((byte) 2);
            this.halter.clear();
            notifySimulatorThread();
        });
        */
        
        // keyboard stuff
        Map<KeyCode, Boolean> keyTracker = new HashMap<>();
        
        screen.addEventHandler(KeyEvent.KEY_PRESSED, (ke) -> {
            KeyCode code = ke.getCode();
            
            //System.out.println("key pressed " + code);
            
            if(!keyTracker.getOrDefault(code, false)) {
                keyTracker.put(code, true);
                this.keyboardBufferController.write4Bytes(0, code.getCode());
                
                //System.out.println("interrupt sent " + code);
                
                this.sim.fireMaskableInterrupt((byte) 2);
                this.halter.clear();
                notifySimulatorThread();
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
                
                this.sim.fireMaskableInterrupt((byte) 3);
                this.halter.clear();
                notifySimulatorThread();
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
        this.scheduler.scheduleAtFixedRate(() -> updateUI(), 0l, 1_000_000 / 30, TimeUnit.MICROSECONDS);
        
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
            this.infoTotalInstructions.setText(String.format("Total instructions: %d", this.instructionsExecutedTotal));
            this.infoAverageMIPS.setText(String.format("Average IPS: %6.0f", this.lastAverageMIPS));
            
            this.buttonToggleRunning.setText(this.sim.getHalted() ? "Start CPU" : "Stop CPU");
            
            this.screen.update(this.videoBufferArray, 0);
            
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
                    state += dis.disassemble(this.mmu, Integer.toUnsignedLong(sim.getRegIP())) + "\n";
                } catch(IndexOutOfBoundsException e) {}
                
                for(int j = 0; j < dis.getLastInstructionLength(); j++) {
                    byte b = this.mmu.readByte(Integer.toUnsignedLong(sim.getRegIP()) + ((long) j));
                    state += String.format("%02X ", b);
                }
                
                state += "\n\n" + this.relocator.getAddressName(Integer.toUnsignedLong(sim.getRegIP()));
                
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
