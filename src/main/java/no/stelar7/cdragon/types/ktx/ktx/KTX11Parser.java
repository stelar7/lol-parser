package no.stelar7.cdragon.types.ktx.ktx;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.ktx.ktx.data.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import no.stelar7.cdragon.util.types.ByteArray;

import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.*;

public class KTX11Parser implements Parseable<KTX11File>
{
    
    @Override
    public KTX11File parse(Path path)
    {
        return parse(new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public KTX11File parse(ByteArray data)
    {
        return parse(new RandomAccessReader(data.getData(), ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public KTX11File parse(RandomAccessReader raf)
    {
        KTX11File data = new KTX11File();
        data.setHeader(parseHeader(raf));
        data.setKeyValueData(parseKeyValueData(raf, data.getHeader().getBytesOfKeyValueData()));
        data.setMipMaps(parseMipMaps(raf, data.getHeader()));
        
        return data;
    }
    
    private KTX11FileMipMap parseMipMaps(RandomAccessReader raf, KTX11FileHeader header)
    {
        boolean isGL_PALETTEFormat = header.getGlInternalFormat() >= 0x8b90 && header.getGlInternalFormat() <= 0x8b99;
        int     mipCount           = isGL_PALETTEFormat ? 1 : Math.max(1, header.getNumberOfMipmapLevels());
        
        KTX11FileMipMap map = new KTX11FileMipMap();
        for (int mipmap_level = 0; mipmap_level < mipCount; mipmap_level++)
        {
            map.setImageSize(raf.readInt());
            int faces = header.getNumberOfFaces();
            if (!(header.getNumberOfFaces() == 6 && header.getNumberOfArrayElements() == 0))
            {
                faces = 1;
            }
            
            for (int face = 0; face < faces; face++)
            {
                map.setTextureData(mipmap_level, raf.readBytes(map.getImageSize()));
            }
        }
        
        
        return map;
    }
    
    private int getFormatByteCount(int glFormat, int glInternalFormat)
    {
        if (glFormat == 0)
        {
            if (glInternalFormat == 36196)
            {
                return 1;
            }
        }
        return 4;
    }
    
    private List<KTX11FileKeyValuePair> parseKeyValueData(RandomAccessReader raf, int bytesOfKeyValueData)
    {
        List<KTX11FileKeyValuePair> values = new ArrayList<>();
        
        int read = 0;
        while (read < bytesOfKeyValueData)
        {
            KTX11FileKeyValuePair pair = new KTX11FileKeyValuePair();
            pair.setKeyValueByteSize(raf.readInt());
            pair.setKeyValue(raf.readBytes(pair.getKeyValueByteSize()));
            pair.setPadding(raf.readBytes(3 - ((pair.getKeyValueByteSize() + 3) % 4)));
            read += 4 + pair.getKeyValueByteSize() + pair.getPadding().length;
        }
        return values;
    }
    
    private KTX11FileHeader parseHeader(RandomAccessReader raf)
    {
        KTX11FileHeader header = new KTX11FileHeader();
        header.setIdentifier(raf.readBytes(12));
        header.setEndianness(raf.readInt());
        header.setGlType(raf.readInt());
        header.setGlTypeSize(raf.readInt());
        header.setGlFormat(raf.readInt());
        header.setGlInternalFormat(raf.readInt());
        header.setGlBaseInternalFormat(raf.readInt());
        header.setPixelWidth(raf.readInt());
        header.setPixelHeight(raf.readInt());
        header.setPixelDepth(raf.readInt());
        header.setNumberOfArrayElements(raf.readInt());
        header.setNumberOfFaces(raf.readInt());
        header.setNumberOfMipmapLevels(raf.readInt());
        header.setBytesOfKeyValueData(raf.readInt());
        return header;
    }
}
