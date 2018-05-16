package no.stelar7.cdragon.types.skl.data.versioned.data;

import no.stelar7.cdragon.types.skl.data.versioned.bone.*;

import java.util.*;

public class SKLDataV0
{
    private short                unknown1;
    private short                boneCount;
    private int                  boneIndexCount;
    private SKLDataV0Offsets     offsets;
    private List<SKLBoneV0>      bones        = new ArrayList<>();
    private List<SKLBoneV0Extra> boneExtra    = new ArrayList<>();
    private List<Short>          boneIndecies = new ArrayList<>();
    private List<String>         boneNames    = new ArrayList<>();
    
    public short getUnknown1()
    {
        return unknown1;
    }
    
    public void setUnknown1(short unknown1)
    {
        this.unknown1 = unknown1;
    }
    
    public short getBoneCount()
    {
        return boneCount;
    }
    
    public void setBoneCount(short boneCount)
    {
        this.boneCount = boneCount;
    }
    
    public int getBoneIndexCount()
    {
        return boneIndexCount;
    }
    
    public void setBoneIndexCount(int boneIndexCount)
    {
        this.boneIndexCount = boneIndexCount;
    }
    
    public SKLDataV0Offsets getOffsets()
    {
        return offsets;
    }
    
    public void setOffsets(SKLDataV0Offsets offsets)
    {
        this.offsets = offsets;
    }
    
    public List<SKLBoneV0> getBones()
    {
        return bones;
    }
    
    public void setBones(List<SKLBoneV0> bones)
    {
        this.bones = bones;
    }
    
    public List<SKLBoneV0Extra> getBoneExtra()
    {
        return boneExtra;
    }
    
    public void setBoneExtra(List<SKLBoneV0Extra> boneExtra)
    {
        this.boneExtra = boneExtra;
    }
    
    public List<Short> getBoneIndecies()
    {
        return boneIndecies;
    }
    
    public void setBoneIndecies(List<Short> boneIndecies)
    {
        this.boneIndecies = boneIndecies;
    }
    
    public List<String> getBoneNames()
    {
        return boneNames;
    }
    
    public void setBoneNames(List<String> boneNames)
    {
        this.boneNames = boneNames;
    }
}