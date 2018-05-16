package no.stelar7.cdragon.types.skl.data.versioned.data;

import no.stelar7.cdragon.types.skl.data.versioned.bone.SKLBoneV1;

import java.util.*;

public class SKLDataV1
{
    private int designerId;
    private int boneCount;
    private List<SKLBoneV1> bones = new ArrayList<>();
    
    
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
    
    public List<SKLBoneV1> getBones()
    {
        return bones;
    }
    
    public void setBones(List<SKLBoneV1> bones)
    {
        this.bones = bones;
    }
}
