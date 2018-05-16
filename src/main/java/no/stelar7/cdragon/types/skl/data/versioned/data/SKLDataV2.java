package no.stelar7.cdragon.types.skl.data.versioned.data;


import java.util.*;

public class SKLDataV2 extends SKLDataV1
{
    private int boneIndexCounter;
    private List<Integer> boneIndecies = new ArrayList<>();
    
    public int getBoneIndexCounter()
    {
        return boneIndexCounter;
    }
    
    public void setBoneIndexCounter(int boneIndexCounter)
    {
        this.boneIndexCounter = boneIndexCounter;
    }
    
    public List<Integer> getBoneIndecies()
    {
        return boneIndecies;
    }
    
    public void setBoneIndecies(List<Integer> boneIndecies)
    {
        this.boneIndecies = boneIndecies;
    }
}
