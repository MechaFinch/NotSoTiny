package notsotiny.ui;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.sound.midi.MidiUnavailableException;

import asmlib.util.relocation.ExecLoader;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
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
    private static final long IVT_START = 0x0000_0000l,
                              IVT_END = 0x0000_03FFl,
                              PROGRAM_START = 0x0000_0400l,
                              PROGRAM_END = 0x0003_FFFFl,
                              HEAP_START = 0x8000_0000l,
                              HEAP_END = 0x8003_FFFFl,
                              VIDEO_START = 0xC000_0000l,
                              VIDEO_END = 0xC001_33FFl,
                              STACK_START = 0xFFFE_0000l,
                              STACK_END = 0xFFFF_FFFFl;
    
    // MMIO constants
    private static final long HALTER_ADDRESS = 0xF000_0000l,
                              SIC_ADDRESS = 0xF000_0002l;
    
    // other constants
    private static final String PROGRAM_EXEC_FILE = "C:\\Users\\wetca\\Desktop\\silly  code\\architecture\\NotSoTiny\\programming\\lib\\test.oex";
    
    // sim vars
    private NotSoTinySimulator sim;
    
    private MemoryManager mmu;
    
    private FlatMemoryController ivtRAMController,      // 0x0000_0000 - 0x0000_03FF
                                 programRAMController,  // 0x0000_0400 - 0x0003_FFFF
                                 heapRAMController,     // 0x8000_0000 - 0x8003_FFFF
                                 videoRAMController,    // 0xC000_0000 - 0xC001_33FF
                                 stackRAMController;    // 0xFFFE_0000 - 0xFFFF_FFFF
    
    private Halter halter;                              // 0xF000_0000 - 0xF000_0001
    
    private SoundInterfaceController sic;               // 0xF000_0002 - 0xF000_0005
    
    // actually memory arrays
    private byte[] ivtRAM,
                   programRAM,
                   heapRAM,
                   videoRAM,
                   stackRAM;
    
    // real time clock stuff
    private final ScheduledExecutorService rtcScheduler = Executors.newScheduledThreadPool(1);
    
    private ScheduledFuture<?> rtcHandler;
    
    private long rtcPeriodus = 1_000_000l / 128,
                 usSinceLastUIUpdate = 0;
    
    private boolean simRunning = false;
    
    // misc
    private long instructionsExecuted, // tracks since UI was last updated
                 elapsedTimens;
    
    /**
     * Initialize the simulator
     * 
     * @throws MidiUnavailableException 
     * @throws IOException 
     */
    private void initSimulator() throws MidiUnavailableException, IOException {
        // initialize flat memory segments
        ivtRAM = new byte[(int)(IVT_END - IVT_START)];
        programRAM = new byte[(int)(PROGRAM_END - PROGRAM_START)];
        heapRAM = new byte[(int)(HEAP_END - HEAP_START)];
        videoRAM = new byte[(int)(VIDEO_END - VIDEO_START)];
        stackRAM = new byte[(int)(STACK_END - STACK_START)];
        
        ivtRAMController = new FlatMemoryController(ivtRAM);
        programRAMController = new FlatMemoryController(programRAM);
        heapRAMController = new FlatMemoryController(heapRAM);
        videoRAMController = new FlatMemoryController(videoRAM);
        stackRAMController = new FlatMemoryController(stackRAM);
        
        // initialize other segments
        halter = new Halter();
        sic = new SoundInterfaceController();
        
        // TODO for RTC, the halter should start halted.
        
        // initialize memory manager
        this.mmu = new MemoryManager();
        
        this.mmu.registerSegment(ivtRAMController, IVT_START, IVT_END - IVT_START);
        this.mmu.registerSegment(programRAMController, PROGRAM_START, PROGRAM_END - PROGRAM_START);
        this.mmu.registerSegment(heapRAMController, HEAP_START, HEAP_END - HEAP_START);
        this.mmu.registerSegment(videoRAMController, VIDEO_START, VIDEO_END - VIDEO_START);
        this.mmu.registerSegment(stackRAMController, STACK_START, STACK_END - STACK_START);
        
        this.mmu.registerSegment(halter, HALTER_ADDRESS, 2);
        this.mmu.registerSegment(sic, SIC_ADDRESS, 4);
        
        // Load program into memory
        int entry = ExecLoader.loadExecFile(new File(PROGRAM_EXEC_FILE), programRAM, (int) PROGRAM_START, 0);
        
        // write entry vector
        this.mmu.write4Bytes(0, entry);
        
        // simulator
        this.sim = new NotSoTinySimulator(this.mmu);
    }
    
    /**
     * Toggles the simulator
     * TODO TEMPROARY REPLACE WITH RTC
     */
    private void toggleRunSimulator() {
        if(this.simRunning) {
            stopSim();
        } else {
            startSim();
        }
    }
    
    /**
     * Start running the simulator
     * TODO TEMPORARY REPLACE WITH RTC
     */
    private void startSim() {
        this.simRunning = true;
        this.rtcHandler = this.rtcScheduler.scheduleAtFixedRate(() -> stepSim(), 0l, this.rtcPeriodus, TimeUnit.MICROSECONDS);
    }
    
    /**
     * Stop running the simulator
     * TODO TEMPORARY REPLACE WITH RTC
     */
    private void stopSim() {
        if(this.rtcHandler != null) {
            while(!this.rtcHandler.isDone()) this.rtcHandler.cancel(false);
            this.simRunning = false;
            updateUI();
        }
    }
    
    /**
     * Step the simulator
     * TODO TEMPORARY REPLACE WITH INDEPENDENT THREAD THAT WATCHES THE HALTER
     */
    private void stepSim() {
        this.sim.step();
        this.instructionsExecuted++;
        
        if(this.halter.halted()) stopSim();
        
        // cap UI update rate
        if(this.simRunning && this.rtcPeriodus < 16_666) {
            if(usSinceLastUIUpdate < 16_666) {
                usSinceLastUIUpdate += this.rtcPeriodus;
            } else {
                usSinceLastUIUpdate = 0;
                updateUI();
            }
        } else {
            updateUI();
        }
        
        printState();
    }
    
    private void printState() {
        Disassembler dis = new Disassembler();
        System.out.println();
        
        System.out.println(String.format("A    B    C    D%n%04X %04X %04X %04X%nI    J    F%n%04X %04X %04X%nip       bp       sp%n%08X %08X %08X",
                                         sim.getRegA(), sim.getRegB(), sim.getRegC(), sim.getRegD(),
                                         sim.getRegI(), sim.getRegJ(), sim.getRegF(),
                                         sim.getRegIP(), sim.getRegBP(), sim.getRegSP()));
        
        System.out.println(dis.disassemble(this.programRAM, sim.getRegIP() - (int) PROGRAM_START));
        
        for(int j = 0; j < dis.getLastInstructionLength(); j++) {
            System.out.print(String.format("%02X ", this.programRAM[sim.getRegIP() + j - (int) PROGRAM_START]));
        }
        
        System.out.println();
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
    
    private Text infoAverageMIPS;
    
    private Button buttonToggleAdvanced,
                   buttonToggleRunning,
                   buttonStepSim;
    
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
        
        this.buttonToggleRunning = new Button("Start CPU");
        this.buttonStepSim = new Button("Step CPU"); // TODO MOVE TO ADVANCED VIEW
        
        Region rBasicControlsSeparator = new Region();
        HBox.setHgrow(rBasicControlsSeparator, Priority.ALWAYS);
        
        HBox boxBasicControls = new HBox(this.buttonToggleRunning, this.buttonStepSim, rBasicControlsSeparator, boxBasicInfo);
        
        // advanced view
        Text testText = new Text("kajdflgj;sldfkg");
        this.advancedView = new VBox(testText);
        
        // grid
        GridPane pain = new GridPane();
        pain.add(this.screen, 0, 0, 1, 1);
        pain.add(boxBasicControls, 0, 1, 1, 1);
        pain.add(this.advancedView, 1, 0, 1, 2);
        
        // bindings
        this.buttonToggleAdvanced.setOnAction(e -> toggleAdvancedView());
        this.buttonToggleRunning.setOnAction(e -> toggleRunSimulator());
        this.buttonStepSim.setOnAction(e -> stepSim());
        
        this.advancedView.managedProperty().bind(this.advancedView.visibleProperty());
        this.advancedView.setVisible(this.advancedViewVisisble);
        
        Scene scene = new Scene(pain);
        scene.getStylesheets().add(getClass().getResource("resources/application.css").toExternalForm());
        
        // show the window
        this.stage.setScene(scene);
        this.stage.show();
        this.stage.setResizable(false);
        
        this.stage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });
        
        updateUI();
    }
    
    /**
     * Update information for UI objects
     */
    private void updateUI() {
        Platform.runLater(() -> {
            double mips = (((double) this.instructionsExecuted) / ((double) this.elapsedTimens)) * 1000;
            this.infoAverageMIPS.setText(String.format("Average MIPS: %2.2f", mips));
            
            this.buttonToggleRunning.setText(this.simRunning ? "Stop CPU" : "Start CPU");
            
            this.screen.update(this.videoRAM, 0);
            
            if(this.advancedViewVisisble) {
                this.buttonToggleAdvanced.setText("Hide advanced/debug view");
                
                // TODO advanced view
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
        updateUI();
        
        this.stage.sizeToScene();
    }
}
