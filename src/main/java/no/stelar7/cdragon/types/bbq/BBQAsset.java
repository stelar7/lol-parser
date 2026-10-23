package no.stelar7.cdragon.types.bbq;

import no.stelar7.cdragon.util.readers.*;

import java.nio.ByteOrder;
import java.util.*;

/**
 * One file inside a bundle; either a serialized file (objects + type trees) or a raw resource file (.resS, .resource).
 * See https://github.com/K0lb3/UnityPy/blob/master/UnityPy/files/SerializedFile.py
 */
public class BBQAsset
{
    /**
     * Kept for compatibility; buf is relative to the start of this file, so this is always 0
     */
    public int bufferOffset = 0;
    /**
     * Offset of this file in the uncompressed bundle data
     */
    public long         offset = -1;
    BBQHeader bundle = null;
    /**
     * Reader over this file's bytes; positions are relative to the start of the file
     */
    public BinaryReader buf    = null;
    boolean loaded = false;

    String                    name;
    int                       nodeFlags;
    long                      size;
    int                       metadataSize;
    long                      fileSize;
    int                       format;
    long                      dataOffset;
    ByteOrder                 endian          = ByteOrder.BIG_ENDIAN;
    boolean                   longObjectIds;
    String                    userInformation;
    BBQAssetTypeMetadata      tree;
    Map<Long, Integer>        adds            = new HashMap<>();
    List<BBQAssetReference>   assetReferences = new ArrayList<>();
    Map<Integer, BBQTypeTree> types           = new HashMap<>();
    Map<Integer, String>      typeNames       = new HashMap<>();
    Map<Long, BBQObjectInfo>  objects         = new TreeMap<>();

    /**
     * Creates the asset for one node (file) of a UnityFS bundle
     */
    public static BBQAsset fromBundle(BBQBlockStore storage, BBQHeader header, BBQBundleEntry node)
    {
        BBQAsset asset = new BBQAsset();
        asset.bundle = header;
        asset.name = node.getName();
        asset.nodeFlags = node.getFlags();
        asset.offset = node.getOffset();
        asset.size = node.getSize();
        asset.buf = new BBQBlockStoreReader(storage, node.getOffset(), node.getSize());
        return asset;
    }

    /**
     * Raw files (texture data, audio, ...) that are referenced by the serialized files
     */
    public boolean isResource()
    {
        if (this.name.endsWith(".resource") || this.name.endsWith(".resS"))
        {
            return true;
        }

        // node flag 4 marks serialized files
        return this.nodeFlags != 0 && (this.nodeFlags & 4) == 0;
    }

    /**
     * Returns all bytes of this file
     */
    public byte[] readAllBytes()
    {
        buf.seek(0);
        return buf.readBytes(this.size);
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
        this.fileSize = Integer.toUnsignedLong(buf.readInt());
        this.format = buf.readInt();
        this.dataOffset = Integer.toUnsignedLong(buf.readInt());

        boolean bigEndian;
        if (this.format >= 9)
        {
            bigEndian = buf.readBoolean();
            buf.readBytes(3);

            if (this.format >= 22)
            {
                this.metadataSize = buf.readInt();
                this.fileSize = buf.readLong();
                this.dataOffset = buf.readLong();
                buf.readLong();
            }
        } else
        {
            // the metadata (and the endian flag) is at the end of the file
            buf.seek(this.fileSize - this.metadataSize);
            bigEndian = buf.readBoolean();
        }

        this.endian = bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
        buf.setEndian(this.endian);

        this.tree = new BBQAssetTypeMetadata(this);
        this.tree.load(buf);

        if (7 <= this.format && this.format < 14)
        {
            this.longObjectIds = buf.readInt() != 0;
        }

        int objectCount = buf.readInt();
        for (int i = 0; i < objectCount; i++)
        {
            BBQObjectInfo info = new BBQObjectInfo(this, buf);
            registerObject(info);
        }

        // script types: file index + object id
        if (this.format >= 11)
        {
            int addCount = buf.readInt();
            for (int i = 0; i < addCount; i++)
            {
                int fileIndex = buf.readInt();
                if (this.format >= 14)
                {
                    buf.align();
                }

                long id = readId(buf);
                this.adds.put(id, fileIndex);
            }
        }

        int refCount = buf.readInt();
        for (int i = 0; i < refCount; i++)
        {
            this.assetReferences.add(new BBQAssetReference(this, buf));
        }

        if (this.format >= 20)
        {
            this.tree.loadRefTypes(buf);
        }

        if (this.format >= 26)
        {
            this.tree.loadSharedSubtrees(buf);
        }

        if (this.format >= 5)
        {
            this.userInformation = buf.readString();
        }

        this.loaded = true;
    }

    private void registerObject(BBQObjectInfo info)
    {
        if (!this.types.containsKey(info.typeId))
        {
            this.types.put(info.typeId, info.typeTree);
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

    public long getFileSize()
    {
        return fileSize;
    }

    public int getFormat()
    {
        return format;
    }

    public long getDataOffset()
    {
        return dataOffset;
    }

    public ByteOrder getEndian()
    {
        return endian;
    }

    public boolean isLongObjectIds()
    {
        return longObjectIds;
    }

    public String getUserInformation()
    {
        return userInformation;
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
