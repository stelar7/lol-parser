package no.stelar7.cdragon.types.skl.data.versioned.data;

import no.stelar7.cdragon.types.skl.data.versioned.bone.SKLBoneV1;

import java.util.*;

public class SKLDataV1
{
    private final int             designerId;
    private final int             boneCount;
    private final List<SKLBoneV1> bones;
    
    public SKLDataV1(int designerId, int boneCount, List<SKLBoneV1> bones)
    {
        this.designerId = designerId;
        this.boneCount = boneCount;
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
    
    public List<SKLBoneV1> getBones()
    {
        return bones;
    }
}
