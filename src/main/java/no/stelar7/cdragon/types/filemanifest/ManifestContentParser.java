package no.stelar7.cdragon.types.filemanifest;

import no.stelar7.cdragon.util.readers.RandomAccessReader;
import no.stelar7.cdragon.util.types.ByteArray;

import java.nio.ByteOrder;
import java.nio.file.Path;

public class ManifestContentParser
{
    public ManifestContentFileV1 parseV1(Path path)
    {
        return parseV1(new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN));
    }
    
    public ManifestContentFileV1 parseV1(ByteArray data)
    {
        return parseV1(new RandomAccessReader(data.getData(), ByteOrder.LITTLE_ENDIAN));
    }
    
    public ManifestContentFileV1 parseV1(RandomAccessReader raf)
    {
        ManifestContentFileV1 file = new ManifestContentFileV1();
        
        int count = raf.readInt();
        if (count < 0)
        {
            throw new RuntimeException("Invalid content");
        }
        
        for (int i = 0; i < count; i++)
        {
            String data = raf.readString(raf.readInt());
            file.addItem(data);
        }
        
        return file;
    }
    
    public ManifestContentFileV2 parseV2(Path path)
    {
        return parseV2(new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN));
    }
    
    public ManifestContentFileV2 parseV2(ByteArray data)
    {
        return parseV2(new RandomAccessReader(data.getData(), ByteOrder.LITTLE_ENDIAN));
    }
    
    public ManifestContentFileV2 parseV2(RandomAccessReader raf)
    {
        ManifestContentFileV2 file = new ManifestContentFileV2();
        
        int count = raf.readInt();
        if (count < 0)
        {
            throw new RuntimeException("Invalid content");
        }
        
        for (int i = 0; i < count; i++)
        {
            String header     = raf.readString(raf.readInt());
            int    innerCount = raf.readInt();
            for (int i1 = 0; i1 < innerCount; i1++)
            {
                int    unknown = raf.readInt();
                String value   = raf.readString(raf.readInt());
                file.addItem(header, value);
            }
        }
        
        return file;
    }
}
