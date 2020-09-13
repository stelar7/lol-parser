package no.stelar7.cdragon.types.bbq;

import no.stelar7.cdragon.util.readers.*;

import java.util.*;

public class BBQObjectInfo
{
    BBQAsset asset;
    long     pathId;
    int      dataOffset;
    int      size;
    int      typeId;
    int      classId;
    boolean  isDestroyed;
    short    unknown0;
    short    unknown1;
    
    public BBQObjectInfo(BBQAsset asset)
    {
        this.asset = asset;
    }
    
    public void load(BinaryReader buf)
    {
        this.pathId = this.readId(buf);
        this.dataOffset = buf.readInt() + this.asset.dataOffset;
        this.size = buf.readInt();
        if (this.asset.format < 17)
        {
            this.typeId = buf.readInt();
            this.classId = buf.readShort();
        } else
        {
            int type  = buf.readInt();
            int clazz = this.asset.tree.classIds.get(type);
            this.typeId = clazz;
            this.classId = clazz;
        }
        
        if (this.asset.format <= 10)
        {
            this.isDestroyed = buf.readShort() > 0;
        }
        
        if (this.asset.format >= 11 && this.asset.format <= 16)
        {
            this.unknown0 = buf.readShort();
        }
        
        if (this.asset.format >= 15 && this.asset.format <= 16)
        {
            this.unknown1 = buf.readByte();
        }
    }
    
    long readId(BinaryReader buf)
    {
        if (this.asset.longObjectIds)
        {
            return buf.readLong();
        }
        return this.asset.readId(buf);
    }
    
    private BBQTypeTree getTypeTree()
    {
        if (this.typeId < 0)
        {
            Map<Integer, BBQTypeTree> tree = this.asset.tree.typeTrees;
            if (tree.containsKey(this.typeId))
            {
                return tree.get(this.typeId);
            } else if (tree.containsKey(this.classId))
            {
                return tree.get(this.classId);
            }
            
            return BBQAssetTypeMetadata.fromFile("bbq/structs.dat").typeTrees.get(this.classId);
        }
        
        return this.asset.types.get(this.typeId);
    }
    
    public Object read()
    {
        this.asset.buf.seek(this.asset.bufferOffset + this.dataOffset);
        byte[] data = this.asset.buf.readBytes(this.size);
        return readValue(getTypeTree(), new RandomAccessReader(data));
    }
    
    private Object readValue(BBQTypeTree type, RandomAccessReader buf)
    {
        boolean     align        = false;
        int         expectedSize = type.size;
        int         pos          = buf.pos();
        String      t            = type.type;
        BBQTypeTree firstChild   = type.children.size() > 0 ? type.children.get(0) : new BBQTypeTree(this.asset.format);
        
        // todo
        
        return null;
    }
}
