package no.stelar7.cdragon.types.skl.data.versioned.data;

public final class SKLDataV0Offsets
{
    private final int   boneStart;
    private final int   animationStart;
    private final int   boneIndexStart;
    private final int   boneIndexEnd;
    private final int   halfwayBoneString;
    private final int   boneNameStart;
    private final int[] reservedOffsets;
    
    public SKLDataV0Offsets(int boneStart, int animationStart, int boneIndexStart, int boneIndexEnd, int halfwayBoneString, int boneNameStart, int[] reservedOffsets)
    {
        this.boneStart = boneStart;
        this.animationStart = animationStart;
        this.boneIndexStart = boneIndexStart;
        this.boneIndexEnd = boneIndexEnd;
        this.halfwayBoneString = halfwayBoneString;
        this.boneNameStart = boneNameStart;
        this.reservedOffsets = reservedOffsets;
    }
    
    public int getBoneStart()
    {
        return boneStart;
    }
    
    public int getAnimationStart()
    {
        return animationStart;
    }
    
    public int getBoneIndexStart()
    {
        return boneIndexStart;
    }
    
    public int getBoneIndexEnd()
    {
        return boneIndexEnd;
    }
    
    public int getHalfwayBoneString()
    {
        return halfwayBoneString;
    }
    
    public int getBoneNameStart()
    {
        return boneNameStart;
    }
    
    public int[] getReservedOffsets()
    {
        return reservedOffsets;
    }
}
