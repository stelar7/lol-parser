package no.stelar7.cdragon.types.wpk;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.wem.data.*;
import no.stelar7.cdragon.types.wpk.data.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import no.stelar7.cdragon.util.types.ByteArray;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class WPKParser implements Parseable<WPKFile>
{
    @Override
    public WPKFile parse(Path path)
    {
        return parse(new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public WPKFile parse(ByteArray data)
    {
        return parse(new RandomAccessReader(data.getData(), ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public WPKFile parse(RandomAccessReader raf)
    {
        WPKFile file = new WPKFile();
        
        parseHeader(raf, file);
        parseOffsets(raf, file);
        parseData(raf, file);
        
        return file;
    }
    
    private void parseData(RandomAccessReader raf, WPKFile file)
    {
        for (Integer offset : file.getOffsets())
        {
            raf.seek(offset);
            
            WEMFile wem = new WEMFile();
            wem.setDataOffset(raf.readInt());
            wem.setDataLength(raf.readInt());
            
            int filenameSize = raf.readInt() * 2;
            wem.setFilename(raf.readString(filenameSize, StandardCharsets.UTF_16LE));
            
            raf.seek(wem.getDataOffset());
            wem.setData(new WEMData(raf.readBytes(wem.getDataLength())));
            
            file.getWEMFiles().add(wem);
        }
    }
    
    private void parseOffsets(RandomAccessReader raf, WPKFile file)
    {
        for (int i = 0; i < file.getHeader().getFileCount(); i++)
        {
            file.getOffsets().add(raf.readInt());
        }
    }
    
    private void parseHeader(RandomAccessReader raf, WPKFile file)
    {
        WPKHeader header = new WPKHeader();
        header.setMagic(raf.readString(4));
        header.setVersion(raf.readInt());
        header.setFileCount(raf.readInt());
        file.setHeader(header);
    }
}
