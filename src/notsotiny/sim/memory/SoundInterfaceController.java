package notsotiny.sim.memory;

import java.util.Arrays;

import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;

/**
 * A sound device that controls a MIDI voice
 * 0    MIDI note number
 * 1    MIDI note velocity
 * 2    Command (1 = noteOn, 0 = noteOff)
 * 3    n/a
 * 
 * Writing a dword to the controller's address conveniently runs a full command
 * 
 * @author Mechafinch
 */
public class SoundInterfaceController implements MemoryController {
    
    MidiChannel channel;
    Synthesizer synth;
    
    private byte noteNumber,
                 noteVelocity;
    
    /**
     * Creates a controller
     * 
     * @param channels
     * @throws MidiUnavailableException 
     */
    public SoundInterfaceController() throws MidiUnavailableException {
        this.synth = MidiSystem.getSynthesizer();
        this.channel = this.synth.getChannels()[0];
        
        this.noteNumber = 0;
        this.noteVelocity = 0;
        
        this.synth.open();
        this.channel.programChange(80);
    }

    @Override
    public byte readByte(long address) {
        return 0;
    }

    @Override
    public void writeByte(long address, byte value) {
        switch((int) address) {
            case 0:
                this.noteNumber = value;
                break;
            
            case 1:
                this.noteVelocity = value;
                break;
                
            case 2:
                try {
                    if(value == 0) {
                        this.channel.noteOff(this.noteNumber);
                    } else {
                        this.channel.noteOn(this.noteNumber, this.noteVelocity);
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }
    
}
