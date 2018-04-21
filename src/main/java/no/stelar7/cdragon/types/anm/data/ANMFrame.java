package no.stelar7.cdragon.types.anm.data;

public class ANMFrame
{
    private int   boneHash;
    private short positionId;
    private short scaleId;
    private short rotationId;
    private short unknown;
    
    public int getBoneHash()
    {
        return boneHash;
    }
    
    public void setBoneHash(int boneHash)
    {
        this.boneHash = boneHash;
    }
    
    public short getPositionId()
    {
        return positionId;
    }
    
    public void setPositionId(short positionId)
    {
        this.positionId = positionId;
    }
    
    public short getScaleId()
    {
        return scaleId;
    }
    
    public void setScaleId(short scaleId)
    {
        this.scaleId = scaleId;
    }
    
    public short getRotationId()
    {
        return rotationId;
    }
    
    public void setRotationId(short rotationId)
    {
        this.rotationId = rotationId;
    }
    
    public short getUnknown()
    {
        return unknown;
    }
    
    public void setUnknown(short unknown)
    {
        this.unknown = unknown;
    }
}
