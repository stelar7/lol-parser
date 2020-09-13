package no.stelar7.cdragon.types.bbq;

import no.stelar7.cdragon.util.readers.BinaryReader;

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
}
