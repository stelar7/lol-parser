package no.stelar7.cdragon.types.skl.data.versioned.data;

public class SKLDataV0Offsets
{
    private int    boneStart;
    private int    animationStart;
    private int    boneIndexStart;
    private int    boneIndexEnd;
    private int    halfwayBoneString;
    private int    boneNameStart;
    private String padding;
    
    public int getBoneStart()
    {
        return boneStart;
    }
    
    public void setBoneStart(int boneStart)
    {
        this.boneStart = boneStart;
    }
    
    public int getAnimationStart()
    {
        return animationStart;
    }
    
    public void setAnimationStart(int animationStart)
    {
        this.animationStart = animationStart;
    }
    
    public int getBoneIndexStart()
    {
        return boneIndexStart;
    }
    
    public void setBoneIndexStart(int boneIndexStart)
    {
        this.boneIndexStart = boneIndexStart;
    }
    
    public int getBoneIndexEnd()
    {
        return boneIndexEnd;
    }
    
    public void setBoneIndexEnd(int boneIndexEnd)
    {
        this.boneIndexEnd = boneIndexEnd;
    }
    
    public int getHalfwayBoneString()
    {
        return halfwayBoneString;
    }
    
    public void setHalfwayBoneString(int halfwayBoneString)
    {
        this.halfwayBoneString = halfwayBoneString;
    }
    
    public int getBoneNameStart()
    {
        return boneNameStart;
    }
    
    public void setBoneNameStart(int boneNameStart)
    {
        this.boneNameStart = boneNameStart;
    }
    
    public String getPadding()
    {
        return padding;
    }
    
    public void setPadding(String padding)
    {
        this.padding = padding;
    }
}
