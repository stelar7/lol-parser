package no.stelar7.cdragon.types.lockfile;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.lockfile.data.Lockfile;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import no.stelar7.cdragon.util.types.ByteArray;

import java.nio.ByteOrder;
import java.nio.file.Path;

public class LockfileParser implements Parseable<Lockfile>
{
    @Override
    public Lockfile parse(Path path)
    {
        return parse(new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public Lockfile parse(ByteArray data)
    {
        return parse(new RandomAccessReader(data.getData(), ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public Lockfile parse(RandomAccessReader raf)
    {
        Lockfile file = new Lockfile();
        
        String[] data = raf.readAsString().split(":");
        file.setProcess(data[0]);
        file.setPID(Integer.parseInt(data[1]));
        file.setPort(Integer.parseInt(data[2]));
        file.setPassword(data[3]);
        file.setProtocol(data[4]);
        
        return file;
    }
}
