package no.stelar7.cdragon.types.filemanifest;

import no.stelar7.cdragon.util.readers.RandomAccessReader;
import no.stelar7.cdragon.util.types.ByteArray;

import java.nio.ByteOrder;
import java.nio.file.Path;

public class ManifestContentParser
{
    public ManifestContentFileV0 parseV0(Path path)
    {
        return parseV0(new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN));
    }
    
    public ManifestContentFileV0 parseV0(ByteArray data)
    {
        return parseV0(new RandomAccessReader(data.getDataRaw(), ByteOrder.LITTLE_ENDIAN));
    }
    
    public ManifestContentFileV0 parseV0(RandomAccessReader raf)
    {
        ManifestContentFileV0 file = new ManifestContentFileV0();
        
        int count = raf.readInt();
        if (count < 0)
        {
            throw new RuntimeException("Invalid content");
        }
        
        for (int i = 0; i < count; i++)
        {
            int data = raf.readInt();
            file.addItem(data);
        }
        
        return file;
    }
    
    
    public ManifestContentFileV1 parseV1(Path path)
    {
        return parseV1(new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN));
    }
    
    public ManifestContentFileV1 parseV1(ByteArray data)
    {
        return parseV1(new RandomAccessReader(data.getDataRaw(), ByteOrder.LITTLE_ENDIAN));
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
        return parseV2(new RandomAccessReader(data.getDataRaw(), ByteOrder.LITTLE_ENDIAN));
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
    
    public ManifestContentFileAtlas parseAtlas(Path path)
    {
        return parseAtlas(new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN));
    }
    
    public ManifestContentFileAtlas parseAtlas(ByteArray data)
    {
        return parseAtlas(new RandomAccessReader(data.getDataRaw(), ByteOrder.LITTLE_ENDIAN));
    }
    
    public ManifestContentFileAtlas parseAtlas(RandomAccessReader raf)
    {
        ManifestContentFileAtlas file = new ManifestContentFileAtlas();
        
        int version = raf.readInt();
        if (version != 1)
        {
            throw new RuntimeException("Invalid content");
        }
        
        file.setAtlasFile(raf.readString(raf.readInt()));
        
        int count = raf.readInt();
        for (int i = 0; i < count; i++)
        {
            raf.setEndian(ByteOrder.LITTLE_ENDIAN);
            String entryName = raf.readString(raf.readInt());
            raf.setEndian(ByteOrder.BIG_ENDIAN);
            int x = raf.readInt();
            int y = raf.readInt();
            int w = raf.readInt();
            int h = raf.readInt();
            int z = raf.readInt();
            
            file.addItem(entryName, x, y, z, w, h);
        }
        
        return file;
    }
}
