package no.stelar7.cdragon.types.bbq;

import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.readers.*;
import no.stelar7.cdragon.util.types.ByteArray;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class BBQTypeTree
{
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
    
    
    public BBQTypeTree(int format)
    {
        this.format = format;
    }
    
    public void load(BinaryReader buf)
    {
        if (this.format == 10 || this.format >= 12)
        {
            loadBlob(buf);
        } else
        {
            loadOld(buf);
        }
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
    
    private void loadBlob(BinaryReader buf)
    {
        int nodeCount = buf.readInt();
        this.bufferBytes = buf.readInt();
        int    nodeBytes = (this.format >= 19) ? 32 : 24;
        byte[] nodeData  = buf.readBytes(nodeBytes * nodeCount);
        this.data = buf.readBytes(this.bufferBytes);
        
        Deque<BBQTypeTree> parents = new ArrayDeque<>();
        parents.add(this);
        
        BinaryReader raf = new RandomAccessReader(nodeData);
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
            current.isArray = raf.readBoolean();
            current.type = getString(raf.readInt());
            current.name = getString(raf.readInt());
            current.size = raf.readInt();
            current.index = raf.readInt();
            current.flags = raf.readInt();
            
            raf.readBytes(nodeBytes - 24);
        }
    }
    
    private String getString(int offset)
    {
        byte[] data;
        if (offset < 0)
        {
            offset &= 0x7FFFFFFF;
            data = UtilHandler.getBBQStringData();
        } else if (offset < this.bufferBytes)
        {
            data = this.data;
        } else
        {
            return "(NULL)";
        }
        
        ByteArray  dataReader = new ByteArray(data);
        int        endIndex   = dataReader.indexOf(0, offset);
        
        if (offset > data.length) {
            return "";
        }
        
        if (endIndex == -1) {
            endIndex = data.length;
        }
        ByteBuffer bufferData = ByteBuffer.wrap(dataReader.copyOfRange(offset, endIndex).getDataRaw());
        String     value      = StandardCharsets.UTF_8.decode(bufferData).toString();
        return value;
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
