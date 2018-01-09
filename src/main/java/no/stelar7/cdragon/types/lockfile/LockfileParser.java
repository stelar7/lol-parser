package no.stelar7.cdragon.types.lockfile;

import no.stelar7.cdragon.types.lockfile.data.Lockfile;
import no.stelar7.cdragon.util.reader.RandomAccessReader;

import java.nio.ByteOrder;
import java.nio.file.Path;

public class LockfileParser
{
    
    public Lockfile parse(Path path)
    {
        RandomAccessReader raf  = new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN);
        Lockfile           file = new Lockfile();
        
        String[] data = raf.readAsString().split(":");
        file.setProcess(data[0]);
        file.setPID(Integer.parseInt(data[1]));
        file.setPort(Integer.parseInt(data[2]));
        file.setPassword(data[3]);
        file.setProtocol(data[4]);
        
        return file;
    }
}
