package no.stelar7.cdragon.types.bbq;

import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.readers.*;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class BBQTypeTree
{
    private static volatile byte[] commonStrings;

    List<BBQTypeTree> children = new ArrayList<>();
    int               version;
    boolean           isArray;
    int               size;
    int               index;
    int               flags;
    String            type     = "(NULL)";
    String            name     = "(NULL)";
    int               format;
    int               bufferBytes;
    byte[]            data;
    /**
     * Type flags: 0x01 = array, 0x02 = managed reference registry, 0x20 = the children are in a shared sub tree
     */
    int               typeFlags;
    /**
     * Blob format 19+: index into the ref hashes of the containing tree, used by nodes with type flag 0x20
     */
    long              refTypeHash;
    byte[]            hash;
    List<byte[]>      refHashes = new ArrayList<>();
    boolean           empty;

    static final int TYPE_FLAG_SHARED_SUBTREE = 0x20;

    public BBQTypeTree(int format)
    {
        this.format = format;
    }
    
    /**
     * Loads the tree, reading the blob nodes as big-endian
     */
    public void load(BinaryReader buf)
    {
        load(buf, ByteOrder.BIG_ENDIAN);
    }

    /**
     * @param endian the byte order of the serialized file the tree is in
     */
    public void load(BinaryReader buf, ByteOrder endian)
    {
        if (this.format >= 23)
        {
            loadHandler(buf, endian);
        } else if (this.format == 10 || this.format >= 12)
        {
            loadBlob(buf, endian, this.format);
        } else
        {
            loadOld(buf);
        }
    }

    /**
     * Format 23+ wraps the blob: hash (16), size (4), then if size >= 8: "mhtt", blob version (4), blob, ref hash count (4), ref hashes (16 each)
     */
    private void loadHandler(BinaryReader buf, ByteOrder endian)
    {
        this.hash = buf.readBytes(16);
        long size  = Integer.toUnsignedLong(buf.readInt());
        long start = buf.pos();

        if (size >= 8)
        {
            String magic = buf.readString(4);
            if (!"mhtt".equals(magic))
            {
                throw new ParseException("Invalid type tree blob magic \"" + magic + "\"", start);
            }

            int blobVersion = buf.readInt();
            loadBlob(buf, endian, blobVersion);

            if (buf.pos() + 4 <= start + size)
            {
                long refHashCount = Integer.toUnsignedLong(buf.readInt());
                if (buf.pos() + refHashCount * 16 > start + size)
                {
                    throw new ParseException("Type tree ref hash count " + refHashCount + " exceeds the type tree size", buf.pos() - 4);
                }

                for (long i = 0; i < refHashCount; i++)
                {
                    this.refHashes.add(buf.readBytes(16));
                }
            }
        } else
        {
            this.empty = true;
        }

        buf.seek(start + size);
    }

    /**
     * Format 23+ trees with a size below 8 have no nodes
     */
    public boolean isEmpty()
    {
        return empty;
    }

    public List<BBQTypeTree> getChildren()
    {
        return children;
    }

    public String getType()
    {
        return type;
    }

    public String getName()
    {
        return name;
    }

    public int getTypeFlags()
    {
        return typeFlags;
    }

    public long getRefTypeHash()
    {
        return refTypeHash;
    }

    /**
     * Format 23+: the hash identifying this tree
     */
    public byte[] getHash()
    {
        return hash;
    }

    /**
     * Format 23+: the hashes of the shared sub trees referenced by nodes in this tree (indexed by {@link #getRefTypeHash()})
     */
    public List<byte[]> getRefHashes()
    {
        return refHashes;
    }

    /**
     * True if the children of this node live in a shared sub tree (type flag 0x20, serialized format 26+)
     */
    public boolean usesSharedSubtree()
    {
        return (this.typeFlags & TYPE_FLAG_SHARED_SUBTREE) != 0;
    }

    private void loadOld(BinaryReader buf)
    {
        this.type = buf.readString();
        this.name = buf.readString();
        this.size = buf.readInt();
        this.index = buf.readInt();
        this.isArray = buf.readInt() > 0;
        this.version = buf.readInt();
        this.flags = buf.readInt();
        
        int fieldCount = buf.readInt();
        for (int i = 0; i < fieldCount; i++)
        {
            BBQTypeTree tree = new BBQTypeTree(this.format);
            tree.load(buf);
            this.children.add(tree);
        }
    }
    
    private void loadBlob(BinaryReader buf, ByteOrder endian, int blobFormat)
    {
        int nodeCount = buf.readInt();
        this.bufferBytes = buf.readInt();
        int    nodeBytes = (blobFormat >= 19) ? 32 : 24;
        byte[] nodeData  = buf.readBytes(nodeBytes * nodeCount);
        this.data = buf.readBytes(this.bufferBytes);
        
        Deque<BBQTypeTree> parents = new ArrayDeque<>();
        parents.add(this);
        
        BinaryReader raf = new RandomAccessReader(nodeData, endian);
        for (int i = 0; i < nodeCount; i++)
        {
            short version = raf.readShort();
            byte  depth   = raf.readByte();
            
            BBQTypeTree current;
            
            if (depth == 0)
            {
                current = this;
            } else
            {
                while (parents.size() > depth)
                {
                    parents.removeLast();
                }
                
                current = new BBQTypeTree(this.format);
                parents.getLast().children.add(current);
                parents.add(current);
            }
            
            current.version = version;
            // type flags: 0x01 = array, 0x02 = managed reference registry, 0x20 = children in a shared sub tree
            current.typeFlags = raf.readByte() & 0xFF;
            current.isArray = (current.typeFlags & 0x01) != 0;
            current.type = getString(raf.readInt());
            current.name = getString(raf.readInt());
            current.size = raf.readInt();
            current.index = raf.readInt();
            current.flags = raf.readInt();

            if (nodeBytes >= 32)
            {
                current.refTypeHash = raf.readLong();
            }
        }
    }
    
    private String getString(int offset)
    {
        byte[] data;
        if (offset < 0)
        {
            // offsets with the high bit set point into Unity's common string table
            offset &= 0x7FFFFFFF;
            data = getCommonStrings();
        } else if (offset < this.bufferBytes)
        {
            data = this.data;
        } else
        {
            return "(NULL)";
        }

        if (offset >= data.length)
        {
            return "";
        }

        int end = offset;
        while (end < data.length && data[end] != 0)
        {
            end++;
        }

        return new String(data, offset, end - offset, StandardCharsets.UTF_8);
    }

    private static byte[] getCommonStrings()
    {
        if (commonStrings == null)
        {
            commonStrings = UtilHandler.getBBQStringData();
        }
        return commonStrings;
    }
    
    boolean shouldAlign()
    {
        return (this.flags & 0x4000) > 0;
    }
    
    @Override
    public String toString()
    {
        return "BBQTypeTree{" +
               "type='" + type + '\'' +
               ", name='" + name + '\'' +
               ", size=" + size +
               ", index=" + index +
               ", isArray=" + isArray +
               ", flags=" + flags +
               '}';
    }
}
