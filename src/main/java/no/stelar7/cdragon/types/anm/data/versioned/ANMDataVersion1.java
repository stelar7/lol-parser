package no.stelar7.cdragon.types.anm.data.versioned;

import no.stelar7.cdragon.types.anm.data.ANMEntry;
import no.stelar7.cdragon.util.types.Vector3f;

import java.util.*;

public class ANMDataVersion1
{
    private int    dataSize;
    private String subMagic;
    private int    subVersion;
    private int    boneCount;
    private int    entryCount;
    private int    unknown1;
    
    private float animationLength;
    private float FPS;
    
    private int unknown2;
    private int unknown3;
    private int unknown4;
    private int unknown5;
    private int unknown6;
    private int unknown7;
    
    private Vector3f minTranslation;
    private Vector3f maxTranslation;
    private Vector3f minScale;
    private Vector3f maxScale;
    
    private int entryOffset;
    private int indexOffset;
    private int hashOffset;
    
    private List<ANMEntry> entries    = new ArrayList<>();
    private List<Short>    indecies   = new ArrayList<>();
    private List<Integer>  boneHashes = new ArrayList<>();
    
    public int getDataSize()
    {
        return dataSize;
    }
    
    public void setDataSize(int dataSize)
    {
        this.dataSize = dataSize;
    }
    
    public String getSubMagic()
    {
        return subMagic;
    }
    
    public void setSubMagic(String subMagic)
    {
        this.subMagic = subMagic;
    }
    
    public int getSubVersion()
    {
        return subVersion;
    }
    
    public void setSubVersion(int subVersion)
    {
        this.subVersion = subVersion;
    }
    
    public int getBoneCount()
    {
        return boneCount;
    }
    
    public void setBoneCount(int boneCount)
    {
        this.boneCount = boneCount;
    }
    
    public int getEntryCount()
    {
        return entryCount;
    }
    
    public void setEntryCount(int entryCount)
    {
        this.entryCount = entryCount;
    }
    
    public int getUnknown1()
    {
        return unknown1;
    }
    
    public void setUnknown1(int unknown1)
    {
        this.unknown1 = unknown1;
    }
    
    public float getAnimationLength()
    {
        return animationLength;
    }
    
    public void setAnimationLength(float animationLength)
    {
        this.animationLength = animationLength;
    }
    
    public float getFPS()
    {
        return FPS;
    }
    
    public void setFPS(float FPS)
    {
        this.FPS = FPS;
    }
    
    public int getUnknown2()
    {
        return unknown2;
    }
    
    public void setUnknown2(int unknown2)
    {
        this.unknown2 = unknown2;
    }
    
    public int getUnknown3()
    {
        return unknown3;
    }
    
    public void setUnknown3(int unknown3)
    {
        this.unknown3 = unknown3;
    }
    
    public int getUnknown4()
    {
        return unknown4;
    }
    
    public void setUnknown4(int unknown4)
    {
        this.unknown4 = unknown4;
    }
    
    public int getUnknown5()
    {
        return unknown5;
    }
    
    public void setUnknown5(int unknown5)
    {
        this.unknown5 = unknown5;
    }
    
    public int getUnknown6()
    {
        return unknown6;
    }
    
    public void setUnknown6(int unknown6)
    {
        this.unknown6 = unknown6;
    }
    
    public int getUnknown7()
    {
        return unknown7;
    }
    
    public void setUnknown7(int unknown7)
    {
        this.unknown7 = unknown7;
    }
    
    public Vector3f getMinTranslation()
    {
        return minTranslation;
    }
    
    public void setMinTranslation(Vector3f minTranslation)
    {
        this.minTranslation = minTranslation;
    }
    
    public Vector3f getMaxTranslation()
    {
        return maxTranslation;
    }
    
    public void setMaxTranslation(Vector3f maxTranslation)
    {
        this.maxTranslation = maxTranslation;
    }
    
    public Vector3f getMinScale()
    {
        return minScale;
    }
    
    public void setMinScale(Vector3f minScale)
    {
        this.minScale = minScale;
    }
    
    public Vector3f getMaxScale()
    {
        return maxScale;
    }
    
    public void setMaxScale(Vector3f maxScale)
    {
        this.maxScale = maxScale;
    }
    
    public int getEntryOffset()
    {
        return entryOffset;
    }
    
    public void setEntryOffset(int entryOffset)
    {
        this.entryOffset = entryOffset;
    }
    
    public int getIndexOffset()
    {
        return indexOffset;
    }
    
    public void setIndexOffset(int indexOffset)
    {
        this.indexOffset = indexOffset;
    }
    
    public int getHashOffset()
    {
        return hashOffset;
    }
    
    public void setHashOffset(int hashOffset)
    {
        this.hashOffset = hashOffset;
    }
    
    public List<ANMEntry> getEntries()
    {
        return entries;
    }
    
    public void setEntries(List<ANMEntry> entries)
    {
        this.entries = entries;
    }
    
    public List<Short> getIndecies()
    {
        return indecies;
    }
    
    public void setIndecies(List<Short> indecies)
    {
        this.indecies = indecies;
    }
    
    public List<Integer> getBoneHashes()
    {
        return boneHashes;
    }
    
    public void setBoneHashes(List<Integer> boneHashes)
    {
        this.boneHashes = boneHashes;
    }
}
