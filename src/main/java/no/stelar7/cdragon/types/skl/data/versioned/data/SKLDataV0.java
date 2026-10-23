package no.stelar7.cdragon.types.skl.data.versioned.data;

import no.stelar7.cdragon.types.skl.data.versioned.bone.*;

import java.util.*;

public final class SKLDataV0
{
    private final short                unknown1;
    private final short                boneCount;
    private final int                  boneIndexCount;
    private final SKLDataV0Offsets     offsets;
    private final List<SKLBoneV0>      bones;
    private final List<SKLBoneV0Extra> boneExtra;
    private final List<Short>          boneIndecies;
    private final List<String>         boneNames;
    private final String               name;
    private final String               assetName;
    
    public SKLDataV0(short unknown1, short boneCount, int boneIndexCount, SKLDataV0Offsets offsets, List<SKLBoneV0> bones, List<SKLBoneV0Extra> boneExtra, List<Short> boneIndecies, List<String> boneNames, String name, String assetName)
    {
        this.unknown1 = unknown1;
        this.boneCount = boneCount;
        this.boneIndexCount = boneIndexCount;
        this.offsets = offsets;
        this.bones = bones;
        this.boneExtra = boneExtra;
        this.boneIndecies = boneIndecies;
        this.boneNames = boneNames;
        this.name = name;
        this.assetName = assetName;
    }
    
    public String getName()
    {
        return name;
    }
    
    public String getAssetName()
    {
        return assetName;
    }
    
    public short getUnknown1()
    {
        return unknown1;
    }
    
    public short getBoneCount()
    {
        return boneCount;
    }
    
    public int getBoneIndexCount()
    {
        return boneIndexCount;
    }
    
    public SKLDataV0Offsets getOffsets()
    {
        return offsets;
    }
    
    public List<SKLBoneV0> getBones()
    {
        return bones;
    }
    
    public List<SKLBoneV0Extra> getBoneExtra()
    {
        return boneExtra;
    }
    
    public List<Short> getBoneIndecies()
    {
        return boneIndecies;
    }
    
    public List<String> getBoneNames()
    {
        return boneNames;
    }
}