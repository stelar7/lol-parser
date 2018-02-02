package no.stelar7.cdragon.types.wem;

import no.stelar7.cdragon.types.wem.data.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;

import java.nio.ByteOrder;
import java.nio.file.Path;

public class WEMParser
{
    public WEMFile parse(Path path)
    {
        RandomAccessReader raf = new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN);
        
        WEMFile wem = new WEMFile();
        wem.setDataOffset(0);
        wem.setDataLength(raf.remaining());
        wem.setFilename(path.getFileName().toString());
        wem.setData(new WEMData(raf.readBytes(wem.getDataLength())));
        
        return wem;
    }
}
