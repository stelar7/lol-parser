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
        
        KTX11FileMipMap map           = new KTX11FileMipMap();
        int             width         = header.getPixelWidth();
        int             height        = header.getPixelHeight();
        int             encodedWidth  = (width + 3) & ~3;
        int             encodedHeight = (height + 3) & ~3;
        
        if (header.getNumberOfArrayElements() > 1)
        {
            throw new RuntimeException("Unable to parse images with multiple arrays");
        }
        
        if (header.getNumberOfFaces() > 1)
        {
            throw new RuntimeException("Unable to parse cubemaps");
        }
        
        if (header.getPixelDepth() > 1)
        {
            throw new RuntimeException("Unable to parse images with multiple depths");
        }
        
        for (int mipmap_level = 0; mipmap_level < mipCount; mipmap_level++)
        {
            map.setImageSize(raf.readInt());
            int nominalSize = (encodedHeight / header.getTextureFormat().getBlockHeight()) * (encodedWidth / header.getTextureFormat().getBlockWidth());
            if (map.getImageSize() != nominalSize * header.getBytesPerBlock())
            {
                throw new RuntimeException("Mipmap size does not match expected size");
            }
            
            KTX11FileMipMapTexture tex = new KTX11FileMipMapTexture();
            tex.setFormat(header.getTextureFormat());
            tex.setWidth(header.getPixelWidth());
            tex.setHeight(header.getPixelHeight());
            tex.setWidthInBlocks(encodedWidth / header.getTextureFormat().getBlockWidth());
            tex.setHeightInBlocks(encodedHeight / header.getTextureFormat().getBlockHeight());
            tex.setData(raf.readBytes(nominalSize * header.getBytesPerBlock()));
            map.setTextureData(mipmap_level, tex);
            
            if (tex.getData().length < nominalSize * header.getBytesPerBlock())
            {
                throw new RuntimeException("Read image size does not match expected size");
            }
            
            width >>= 1;
            height >>= 1;
            encodedWidth = (width + 3) & ~3;
            encodedHeight = (height + 3) & ~3;
            
            if (mipmap_level + 1 < mipCount)
            {
                int padding = 3 - ((map.getImageSize() + 3) % 4);
                raf.readBytes(padding);
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
        
        if (header.getEndianness() == 0x01020304)
        {
            header.setReversedEndian(true);
        }
        
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
        header.setTextureFormat(findTextureFormat(header.getGlInternalFormat(), header.getGlFormat(), header.getGlType()));
        header.setBytesPerBlock(header.getGlFormat() == 0 ? header.getTextureFormat().getCompressedBlockSize() : header.getTextureFormat().getPixelSize());
        return header;
    }
    
    private TextureFormat findTextureFormat(int glInternalFormat, int glFormat, int glType)
    {
        for (TextureFormat format : TextureFormat.values())
        {
            if (format.getGlInternalFormat() != 0 && format.getGlInternalFormat() == glInternalFormat)
            {
                if (format.getGlFormat() == 0)
                {
                    return format;
                }
                if (format.getGlFormat() == glFormat && format.getGlType() == glType)
                {
                    return format;
                }
            }
        }
        return null;
    }
}
