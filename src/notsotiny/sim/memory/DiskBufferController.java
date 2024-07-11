package notsotiny.sim.memory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * DiskBuffer
 * Map:
 * 00-00:   w: write buffer to file at sector. increment sector r: read sector to buffer. increment sector
 * 01-01:
 * 02-03:   file number
 * 04-05:   sector number
 * 06-07:   
 * 08-0B:   buffer pointer
 */
public class DiskBufferController implements MemoryController {
    
    private MemoryManager mmu;
    private Path directory;
    
    long buffptr = 0;
    int fileNumber = 0,
        sectorNumber = 0;
    
    /**
     * @param directory
     */
    public DiskBufferController(MemoryManager mmu, Path directory) {
        this.mmu = mmu;
        this.directory = directory;
        
        // ensure directory exists
        if(!Files.exists(directory)) {
            try {
                Files.createDirectory(directory);
            } catch(IOException e) {
                System.out.println("DiskBuffer Error: " + e.getClass().getName() + ": " + e.getMessage());
            }
        }
    }
    
    private void writeSector() throws IOException {
        //System.out.printf("Writing file %04X sector %04X from %08X%n", this.fileNumber, this.sectorNumber, this.buffptr);
        byte[] data = new byte[1024];
        
        // read from buff ptr
        for(int i = 0; i < 1024; i += 4) {
            try {
                int d = this.mmu.read4Bytes(this.buffptr + i, true);
                
                data[i + 0] = (byte)((d >>  0) & 0xFF);
                data[i + 1] = (byte)((d >>  8) & 0xFF);
                data[i + 2] = (byte)((d >> 16) & 0xFF);
                data[i + 3] = (byte)((d >> 24) & 0xFF);
            } catch(UnprivilegedAccessException e) {
                // not possible
            }
        }
        
        // get file data
        Path f = getOrCreateFile();
        byte[] fdata = Files.readAllBytes(f);
        
        if(fdata.length < (1024 * (this.sectorNumber + 1))) {
            // not long enough. create new array
            byte[] fdata2 = new byte[1024 * (this.sectorNumber + 1)];
            System.arraycopy(fdata, 0, fdata2, 0, fdata.length);
            System.arraycopy(data, 0, fdata2, 1024 * this.sectorNumber, 1024);
            
            Files.write(f, fdata2, StandardOpenOption.TRUNCATE_EXISTING);
        } else {
            // long enough. just copy
            System.arraycopy(data, 0, fdata, 1024 * this.sectorNumber, 1024);
            
            Files.write(f, fdata, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }
    
    /**
     * Reads 1024 bytes from file n.
     * Writes them to the buffer
     * @throws IOException 
     */
    private void readSector() throws IOException {
        //System.out.printf("Reading file %04X sector %04X to %08X%n", this.fileNumber, this.sectorNumber, this.buffptr);
        byte[] data = new byte[1024];
        
        Path file = getOrCreateFile();
        
        if(file == null) {
            // zeroes
            for(int i = 0; i < 1024; i += 4) {
                try {
                    this.mmu.write4Bytes(this.buffptr + i, 0, true);
                } catch(UnprivilegedAccessException e) {
                    // not possible
                }
            }
        } else {
            // read & write
            byte[] fdata = Files.readAllBytes(file);
            
            if(fdata.length < 1024 * this.sectorNumber) {
                // sector does not exist. zeros
                for(int i = 0; i < data.length; i++) {
                    data[i] = 0;
                }
            } else if(fdata.length < 1024 * (this.sectorNumber + 1)) {
                for(int i = 0; i < data.length; i++) {
                    data[i] = 0;
                }
                
                // sector partially exists
                System.arraycopy(fdata, (1024 * this.sectorNumber), data, 0, fdata.length - (1024 * this.sectorNumber));
            } else {
                // sector exists. read
                System.arraycopy(fdata, (1024 * this.sectorNumber), data, 0, 1024);
            }
            
            // write
            for(int i = 0; i < 1024; i += 4) {
                int dword = ((data[i + 0] <<  0) & 0x0000_00FF) |
                            ((data[i + 1] <<  8) & 0x0000_FF00) |
                            ((data[i + 2] << 16) & 0x00FF_0000) |
                            ((data[i + 3] << 24) & 0xFF00_0000);
                
                try {
                    this.mmu.write4Bytes(this.buffptr + i, dword, true);
                } catch(UnprivilegedAccessException e) {
                    // not possible
                }
            }
        }
        
        this.sectorNumber++;
    }
    
    /**
     * Gets the file from fileNumber, creating it if needed
     * @return
     */
    private Path getOrCreateFile() throws IOException {
        Path f = directory.resolve(String.format("%04X.dat", this.fileNumber));
        
        if(!Files.exists(f)) {
            Files.createFile(f);
        }
        
        return f;
    }

    @Override
    public byte readByte(long address) {
        try {
            switch((int) address) {
                case 0: // read sector
                    readSector();
                    break;
                
                case 2:
                    return (byte)(this.fileNumber & 0xFF);
                    
                case 3:
                    return (byte)((this.fileNumber >> 8) & 0xFF);
                
                case 4:
                    return (byte)(this.sectorNumber & 0xFF);
                
                case 5:
                    return (byte)((this.sectorNumber >> 8) & 0xFF);
                
                case 8:
                    return (byte)(this.buffptr & 0xFF);
                
                case 9:
                    return (byte)((this.buffptr >> 8) & 0xFF);
                
                case 10:
                    return (byte)((this.buffptr >> 16) & 0xFF);
                
                case 11:
                    return (byte)((this.buffptr >> 24) & 0xFF);
                    
                default:
                    return 0;
            }
            
            return 0;
        } catch(IOException e) {
            System.out.println("DiskBuffer Error: " + e.getClass().getName() + ": " + e.getMessage());
            return 0;
        }
    }

    @Override
    public void writeByte(long address, byte value) {
        try  {
            switch((int) address) {
                case 0:
                    writeSector();
                    break;
                
                case 2:
                    this.fileNumber = (this.fileNumber & 0xFF00) | (value & 0xFF);
                    break;
                
                case 3:
                    this.fileNumber = (this.fileNumber & 0x00FF) | ((value & 0xFF) << 8);
                    break;
                
                case 4:
                    this.sectorNumber = (this.sectorNumber & 0xFF00) | (value & 0xFF);
                    break;
                
                case 5:
                    this.sectorNumber = (this.sectorNumber & 0x00FF) | ((value & 0xFF) << 8);
                    break;
                
                case 8:
                    this.buffptr = (this.buffptr & 0xFFFF_FF00l) | (value & 0xFFl);
                    break;
                
                case 9:
                    this.buffptr = (this.buffptr & 0xFFFF_00FF) | ((value & 0xFFl) << 8l);
                    break;
                
                case 10:
                    this.buffptr = (this.buffptr & 0xFF00_FFFF) | ((value & 0xFFl) << 16l);
                    break;
                
                case 11:
                    this.buffptr = (this.buffptr & 0x00FF_FFFF) | ((value & 0xFFl) << 24l);
                    break;
                
                default:
            }
        } catch(IOException e) {
            System.out.println("DiskBuffer Error: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
}
