package no.stelar7.cdragon.types.bbq;

import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.readers.*;
import no.stelar7.cdragon.util.types.ByteArray;

import java.nio.ByteOrder;
import java.util.*;

public class BBQAsset
{
    public int bufferOffset = -1;
    public int offset       = -1;
    BBQHeader bundle = null;
    public BinaryReader buf = null;
    boolean loaded = false;
    
    String                    name;
    int                       metadataSize;
    int                       fileSize;
    int                       format;
    int                       dataOffset;
    boolean                   longObjectIds;
    BBQAssetTypeMetadata      tree;
    Map<Long, Integer>        adds            = new HashMap<>();
    List<BBQAssetReference>   assetReferences = new ArrayList<>();
    Map<Integer, BBQTypeTree> types           = new HashMap<>();
    Map<Integer, String>      typeNames       = new HashMap<>();
    Map<Long, BBQObjectInfo>  objects         = new TreeMap<>();
    
    public static BBQAsset fromBundle(BBQBlockStore storage, BBQHeader header)
    {
        BBQAsset            asset = new BBQAsset();
        BBQBlockStoreReader buf   = new BBQBlockStoreReader(storage);
        
        asset.bundle = header;
        int offset = storage.pos();
        asset.buf = new BBQBlockStoreReader(storage);
        
        if (header.isUnityFS())
        {
            asset.bufferOffset = (int) buf.pos();
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
        
        int currentPos = (int) buf.pos();
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
        
        if (7 <= this.format && this.format <= 13)
        {
            this.longObjectIds = buf.readInt() > 0;
        }
        
        int objectCount = buf.readInt();
        for (int i = 0; i < objectCount; i++)
        {
            if (this.format >= 14)
            {
                buf.align();
            }
            
            BBQObjectInfo info = new BBQObjectInfo(this, buf);
            registerObject(info);
        }
        
        if (this.format >= 11)
        {
            int addCount = buf.readInt();
            for (int i = 0; i < addCount; i++)
            {
                if (this.format >= 14)
                {
                    buf.align();
                }
                
                long id    = readId(buf);
                int  value = buf.readInt();
                this.adds.put(id, value);
            }
        }
        
        if (this.format >= 6)
        {
            int refCount = buf.readInt();
            for (int i = 0; i < refCount; i++)
            {
                BBQAssetReference ref = new BBQAssetReference(this, buf);
                this.assetReferences.add(ref);
            }
        }
        
        String unknown = buf.readString();
        this.loaded = true;
    }
    
    private void registerObject(BBQObjectInfo info)
    {
        if (this.tree.typeTrees.containsKey(info.typeId))
        {
            this.types.put(info.typeId, this.tree.typeTrees.get(info.typeId));
        } else if (!this.types.containsKey(info.typeId))
        {
            BBQAssetTypeMetadata trees = BBQAssetTypeMetadata.fromFile("bbq/structs.dat");
            if (trees.typeTrees.containsKey(info.typeId))
            {
                this.types.put(info.typeId, trees.typeTrees.get(info.typeId));
            } else
            {
                System.out.println("Unable to find class with id " + info.typeId + " in structs.dat");
                this.types.put(info.typeId, null);
            }
        }
        
        if (this.objects.containsKey(info.pathId))
        {
            System.out.println("Duplicate asset object: " + info.pathId);
        }
        
        this.objects.put(info.pathId, info);
    }
    
    protected long readId(BinaryReader buf)
    {
        if (this.format >= 14)
        {
            return buf.readLong();
        }
        return buf.readInt();
    }
    
    public boolean isLoaded()
    {
        return loaded;
    }
    
    public String getName()
    {
        return name;
    }
    
    public int getMetadataSize()
    {
        return metadataSize;
    }
    
    public int getFileSize()
    {
        return fileSize;
    }
    
    public int getFormat()
    {
        return format;
    }
    
    public int getDataOffset()
    {
        return dataOffset;
    }
    
    public boolean isLongObjectIds()
    {
        return longObjectIds;
    }
    
    public BBQAssetTypeMetadata getTree()
    {
        return tree;
    }
    
    public Map<Long, Integer> getAdds()
    {
        return adds;
    }
    
    public List<BBQAssetReference> getAssetReferences()
    {
        return assetReferences;
    }
    
    public Map<Integer, BBQTypeTree> getTypes()
    {
        return types;
    }
    
    public Map<Long, BBQObjectInfo> getObjects()
    {
        return objects;
    }
    
    public BBQAsset getAsset(String filePath)
    {
        if (filePath.contains(":"))
        {
            return BBQGlobalLoader.getAsset(filePath);
        } else if (filePath.equals("library/unity default resources"))
        {
            throw new UnsupportedOperationException("Unable to load unity default resources!");
        }
        
        return BBQGlobalLoader.getAssetByFilename(filePath);
    }
}
