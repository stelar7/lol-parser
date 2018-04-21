package no.stelar7.cdragon.types.anm.data;


import java.util.*;

public class ANMBone
{
    private String             name;
    private int                flag;
    private List<ANMBoneFrame> frames = new ArrayList<>();
    
    public String getName()
    {
        return name;
    }
    
    public void setName(String name)
    {
        this.name = name;
    }
    
    public int getFlag()
    {
        return flag;
    }
    
    public void setFlag(int flag)
    {
        this.flag = flag;
    }
    
    public List<ANMBoneFrame> getFrames()
    {
        return frames;
    }
    
    public void setFrames(List<ANMBoneFrame> frames)
    {
        this.frames = frames;
    }
}
