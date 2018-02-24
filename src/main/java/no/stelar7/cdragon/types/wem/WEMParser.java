package no.stelar7.cdragon.types.wem;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.wem.data.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;

import java.nio.ByteOrder;
import java.nio.file.Path;

public class WEMParser implements Parseable<WEMFile>
{
    @Override
    public WEMFile parse(Path path)
    {
        return parse(new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public WEMFile parse(byte[] data)
    {
        return parse(new RandomAccessReader(data, ByteOrder.LITTLE_ENDIAN));
    }
    
    public WEMFile parse(RandomAccessReader raf)
    {
        WEMFile wem = new WEMFile();
        wem.setDataOffset(0);
        wem.setDataLength(raf.remaining());
        //wem.setFilename(path.getFileName().toString());
        wem.setData(new WEMData(raf.readBytes(wem.getDataLength())));
        
        return wem;
    }
}
