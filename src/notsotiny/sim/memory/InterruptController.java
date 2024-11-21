package notsotiny.sim.memory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import notsotiny.sim.NotSoTinySimulatorV2;
import notsotiny.sim.NotSoTinySimulatorV1;

/**
 * Interrupt Controller
 * Not yet controllable.
 * Reading the first 256 addresses will yield 0 if there is no outstanding interrupt on that vector and 1 if there is
 * Reading the second 256 addresses will yield 0 if the vector is maskable and 1 if it is not
 * 
 * @author Mechafinch
 */
public class InterruptController implements MemoryController {
    
    private Set<Integer> requestedInterrupts;
    
    private Map<Integer, Boolean> maskable;
    
    /**
     * Creates a controller
     */
    public InterruptController() {
        this.requestedInterrupts = new HashSet<>();
        this.maskable = new HashMap<>();
        
        for(int i = 0; i < 256; i++) {
            this.maskable.put(i, true);
        }
    }
    
    /**
     * Sets a vector as maskable (default is maskable)
     * @param vector
     */
    public void setMaskable(byte vector) {
        this.maskable.put(vector & 0xFF, true);
    }
    
    /**
     * Sets a vector as non-maskable (default is maskable)
     * @param vector
     */
    public void setNonMaskable(byte vector) {
        this.maskable.put(vector & 0xFF, false);
    }
    
    /**
     * Attempts to fire an interrupt if any are requested
     */
    public synchronized void step(NotSoTinySimulatorV1 sim) {
        if(!this.requestedInterrupts.isEmpty()) {
            // try in whatever order we're given
            for(int i : requestedInterrupts) {
                boolean fired;
                
                if(this.maskable.get(i)) {
                    fired = sim.fireMaskableInterrupt((byte) i);
                } else {
                    sim.fireNonMaskableInterrupt((byte) i);
                    fired = true;
                }
                
                if(fired) {
                    //System.out.println("Fired interrupt " + i);
                    this.requestedInterrupts.remove(i);
                    break;
                }
            }
        }
    }
        
    /**
     * Attempts to fire an interrupt if any are requested
     */
    public synchronized void step(NotSoTinySimulatorV2 sim) {
        if(!this.requestedInterrupts.isEmpty()) {
            // try in whatever order we're given
            for(int i : requestedInterrupts) {
                boolean fired;
                
                if(this.maskable.get(i)) {
                    fired = sim.fireMaskableInterrupt((byte) i);
                } else {
                    sim.fireNonMaskableInterrupt((byte) i);
                    fired = true;
                }
                
                if(fired) {
                    //System.out.println("Fired interrupt " + i);
                    this.requestedInterrupts.remove(i);
                    break;
                }
            }
        }
    }
    
    /**
     * Adds an interrupt request
     * @param vector
     */
    public synchronized void setRequest(byte vector) {
        //System.out.println("Requested interrupt " + vector + " (" + (this.requestedInterrupts.contains(vector & 0xFF) ? "present" : "not present") + ")");
        this.requestedInterrupts.add(vector & 0xFF);
    }

    @Override
    public byte readByte(long address) {
        int local = (int) address & 0xFF;
        
        if(address < 256) {
            // read requested
            return (byte) (this.requestedInterrupts.contains(local) ? 1 : 0);
        } else {
            // read maskability
            return (byte) (this.maskable.get(local) ? 0 : 1);
        }
    }

    @Override
    public void writeByte(long address, byte value) {
        // TODO Auto-generated method stub
        
    }
    
}
