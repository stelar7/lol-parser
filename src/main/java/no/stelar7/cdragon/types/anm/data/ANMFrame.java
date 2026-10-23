package no.stelar7.cdragon.types.anm.data;

public final class ANMFrame
{
    private final int boneHash;
    // unsigned 16 bit palette indices
    private final int positionId;
    private final int scaleId;
    private final int rotationId;
    private final int unknown;
    
    public ANMFrame(int boneHash, int positionId, int scaleId, int rotationId, int unknown)
    {
        this.boneHash = boneHash;
        this.positionId = positionId;
        this.scaleId = scaleId;
        this.rotationId = rotationId;
        this.unknown = unknown;
    }

    public int getBoneHash()
    {
        return boneHash;
    }

    public int getPositionId()
    {
        return positionId;
    }

    public int getScaleId()
    {
        return scaleId;
    }

    public int getRotationId()
    {
        return rotationId;
    }

    public int getUnknown()
    {
        return unknown;
    }
}
