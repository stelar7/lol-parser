package no.stelar7.cdragon.types.anm.data;


import java.util.*;

public final class ANMBone
{
    private final String             name;
    private final int                flag;
    private final List<ANMBoneFrame> frames;
    
    public ANMBone(String name, int flag, List<ANMBoneFrame> frames)
    {
        this.name = name;
        this.flag = flag;
        this.frames = frames;
    }
    
    public String getName()
    {
        return name;
    }
    
    public int getFlag()
    {
        return flag;
    }
    
    public List<ANMBoneFrame> getFrames()
    {
        return frames;
    }
}
