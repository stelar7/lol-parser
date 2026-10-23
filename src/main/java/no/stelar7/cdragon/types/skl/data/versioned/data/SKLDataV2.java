package no.stelar7.cdragon.types.skl.data.versioned.data;


import no.stelar7.cdragon.types.skl.data.versioned.bone.SKLBoneV1;

import java.util.*;

public final class SKLDataV2 extends SKLDataV1
{
    private final int           boneIndexCounter;
    private final List<Integer> boneIndecies;
    
    public SKLDataV2(int designerId, int boneCount, List<SKLBoneV1> bones, int boneIndexCounter, List<Integer> boneIndecies)
    {
        super(designerId, boneCount, bones);
        this.boneIndexCounter = boneIndexCounter;
        this.boneIndecies = boneIndecies;
    }
    
    public int getBoneIndexCounter()
    {
        return boneIndexCounter;
    }
    
    public List<Integer> getBoneIndecies()
    {
        return boneIndecies;
    }
}
