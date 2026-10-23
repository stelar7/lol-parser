package no.stelar7.cdragon.types.anm.data.versioned;

import no.stelar7.cdragon.types.anm.data.ANMBone;

import java.util.*;

public final class ANMDataVersion3
{
    private final int designerId;
    private final int boneCount;
    private final int frameCount;
    private final int fps;
    
    private final List<ANMBone> bones;
    
    public ANMDataVersion3(int designerId, int boneCount, int frameCount, int fps, List<ANMBone> bones)
    {
        this.designerId = designerId;
        this.boneCount = boneCount;
        this.frameCount = frameCount;
        this.fps = fps;
        this.bones = bones;
    }
    
    public int getDesignerId()
    {
        return designerId;
    }
    
    public int getBoneCount()
    {
        return boneCount;
    }
    
    public int getFrameCount()
    {
        return frameCount;
    }
    
    public int getFps()
    {
        return fps;
    }
    
    public List<ANMBone> getBones()
    {
        return bones;
    }
}
