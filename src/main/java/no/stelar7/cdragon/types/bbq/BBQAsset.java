package no.stelar7.cdragon.types.bbq;

import no.stelar7.cdragon.util.handlers.CompressionHandler;
import no.stelar7.cdragon.util.readers.*;
import no.stelar7.cdragon.util.types.ByteArray;

import java.nio.ByteOrder;

public class BBQAsset
{
    int          bufferOffset = -1;
    int          offset       = -1;
    BBQHeader    bundle       = null;
    BinaryReader buf          = null;
    boolean      loaded       = false;
    
    String               name;
    int                  metadataSize;
    int                  fileSize;
    int                  format;
    int                  dataOffset;
    boolean              longObjectIds;
    BBQAssetTypeMetadata tree;
    
    public static BBQAsset fromBundle(BBQBlockStore storage, BBQHeader header)
    {
        BBQAsset            asset = new BBQAsset();
        BBQBlockStoreReader buf   = new BBQBlockStoreReader(storage);
        
        asset.bundle = header;
        int offset = storage.pos();
        asset.buf = new BBQBlockStoreReader(storage);
        
        if (header.isUnityFS())
        {
            asset.bufferOffset = buf.pos();
            return asset;
        }
        
        int headerSize = -1;
        
        if (header.getCompressionMode() == BBQCompressionType.NONE)
        {
            asset.name = buf.readString();
            headerSize = buf.readInt();
            int size = buf.readInt();
        } else
        {
            headerSize = header.getHeaderSize();
        }
        
        int currentPos = buf.pos();
        if (header.getCompressionMode() != BBQCompressionType.NONE)
        {
            byte[]    data  = CompressionHandler.uncompressLZMA(buf.readRemaining());
            ByteArray array = new ByteArray(data);
            asset.buf = new RandomAccessReader(array.copyOfRange(headerSize).getDataRaw(), ByteOrder.BIG_ENDIAN);
            asset.bufferOffset = 0;
            buf.seek(currentPos);
        } else
        {
            asset.bufferOffset = offset + headerSize - 4;
            if (asset.isResource())
            {
                asset.bufferOffset -= asset.name.length();
            }
        }
        
        return asset;
    }
    
    public boolean isResource()
    {
        return this.name.endsWith(".resource") || this.name.endsWith(".resS");
    }
    
    public void load()
    {
        if (isResource())
        {
            this.loaded = true;
            return;
        }
        
        buf.seek(this.bufferOffset);
        buf.setEndian(ByteOrder.BIG_ENDIAN);
        
        this.metadataSize = buf.readInt();
        this.fileSize = buf.readInt();
        this.format = buf.readInt();
        this.dataOffset = buf.readInt();
        
        if (this.format >= 9)
        {
            int endian = buf.readInt();
            if (endian == 0)
            {
                buf.setEndian(ByteOrder.LITTLE_ENDIAN);
            }
        }
        
        this.tree = new BBQAssetTypeMetadata(this);
        this.tree.load(buf);
        
        System.out.println();
    }
}
