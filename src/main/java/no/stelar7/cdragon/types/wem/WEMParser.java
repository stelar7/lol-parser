package no.stelar7.cdragon.types.wem;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.wem.data.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import no.stelar7.cdragon.util.types.ByteArray;

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
    public WEMFile parse(ByteArray data)
    {
        return parse(new RandomAccessReader(data.getData(), ByteOrder.LITTLE_ENDIAN));
    }
    
    
    public WEMFile parse(RandomAccessReader raf)
    {
        WEMFile wem = new WEMFile();
        wem.setDataOffset(0);
        wem.setDataLength(raf.remaining());
        //wem.setFilename(path.getFileName().toString());
        
        byte[]             data = raf.readBytes(wem.getDataLength());
        RandomAccessReader raf2 = new RandomAccessReader(data, ByteOrder.LITTLE_ENDIAN);
        wem.setData(new WEMData(data));
        
        return wem;
    }
}
