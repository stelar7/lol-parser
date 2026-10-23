package no.stelar7.cdragon.types.skl.data.versioned.bone;

public final class SKLBoneV0Extra
{
    private final int boneId;
    private final int boneHash;
    
    public SKLBoneV0Extra(int boneId, int boneHash)
    {
        this.boneId = boneId;
        this.boneHash = boneHash;
    }
    
    public int getBoneId()
    {
        return boneId;
    }
    
    public int getBoneHash()
    {
        return boneHash;
    }
}
