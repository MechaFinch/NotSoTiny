package notsotiny.sim.memory;

public class HookController implements MemoryController {
    
    private Runnable hook;
    
    public HookController(Runnable hook) {
        this.hook = hook;
    }

    @Override
    public byte readByte(long address) {
        return 0;
    }

    @Override
    public void writeByte(long address, byte value) {
        hook.run();
    }
    
}
