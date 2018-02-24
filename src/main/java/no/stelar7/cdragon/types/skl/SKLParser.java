package no.stelar7.cdragon.types.skl;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.skl.data.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;

import java.nio.ByteOrder;
import java.nio.file.Path;

// TODO
public class SKLParser implements Parseable<SKLFile>
{
    
    @Override
    public SKLFile parse(Path path)
    {
        return parse(new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public SKLFile parse(byte[] data)
    {
        return parse(new RandomAccessReader(data, ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public SKLFile parse(RandomAccessReader raf)
    {
        SKLFile file = new SKLFile();
        
        file.setHeader(parseHeader(raf));
        
        return file;
    }
    
    private SKLHeader parseHeader(RandomAccessReader raf)
    {
        raf.readAsString();
        return new SKLHeader();
    }
}
