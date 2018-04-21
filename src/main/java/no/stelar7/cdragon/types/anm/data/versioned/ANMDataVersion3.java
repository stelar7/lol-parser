package no.stelar7.cdragon.types.anm.data.versioned;

import no.stelar7.cdragon.types.anm.data.ANMBone;

import java.util.*;

public class ANMDataVersion3
{
    private int designerId;
    private int boneCount;
    private int frameCount;
    private int fps;
    
    private List<ANMBone> bones = new ArrayList<>();
    
    public int getDesignerId()
    {
        return designerId;
    }
    
    public void setDesignerId(int designerId)
    {
        this.designerId = designerId;
    }
    
    public int getBoneCount()
    {
        return boneCount;
    }
    
    public void setBoneCount(int boneCount)
    {
        this.boneCount = boneCount;
    }
    
    public int getFrameCount()
    {
        return frameCount;
    }
    
    public void setFrameCount(int frameCount)
    {
        this.frameCount = frameCount;
    }
    
    public int getFps()
    {
        return fps;
    }
    
    public void setFps(int fps)
    {
        this.fps = fps;
    }
    
    public List<ANMBone> getBones()
    {
        return bones;
    }
    
    public void setBones(List<ANMBone> bones)
    {
        this.bones = bones;
    }
}
