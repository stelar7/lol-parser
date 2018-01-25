package no.stelar7.cdragon.types.skl;

import no.stelar7.cdragon.types.skl.data.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;

import java.nio.ByteOrder;
import java.nio.file.Path;

public class SKLParser
{
    
    public SKLFile parse(Path path)
    {
        RandomAccessReader raf  = new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN);
        SKLFile            file = new SKLFile();
        
        file.setHeader(parseHeader(raf));
        
        return file;
    }
    
    private SKLHeader parseHeader(RandomAccessReader raf)
    {
        raf.readAsString();
        return new SKLHeader();
    }
}
