package no.stelar7.cdragon.types.bbq;

import no.stelar7.cdragon.util.readers.*;

public class BBQObjectPointer
{
    BBQTypeTree type;
    BBQAsset    asset;
    int         fileId;
    long        pathId;
    
    public BBQObjectPointer(BBQTypeTree type, BBQAsset asset, BinaryReader buf)
    {
        this.type = type;
        this.asset = asset;
        load(buf);
    }
    
    boolean isValid()
    {
        return this.fileId != 0 && this.pathId != 0;
    }
    
    public void load(BinaryReader buf)
    {
        this.fileId = buf.readInt();
        this.pathId = this.asset.readId(buf);
    }
    
    public BBQObjectInfo getObject()
    {
        if (asset == null)
        {
            return null;
        }
        
        return asset.objects.get(this.pathId);
    }
    
    
}
