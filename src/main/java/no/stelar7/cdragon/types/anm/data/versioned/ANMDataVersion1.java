package no.stelar7.cdragon.types.anm.data.versioned;

import no.stelar7.cdragon.types.anm.data.ANMEntry;
import no.stelar7.cdragon.util.types.math.Vector3f;

import java.util.*;

public final class ANMDataVersion1
{
    private final int    dataSize;
    private final String subMagic;
    private final int    subVersion;
    private final int    boneCount;
    private final int    entryCount;
    private final int    unknown1;
    
    private final float animationLength;
    private final float FPS;
    
    private final int unknown2;
    private final int unknown3;
    private final int unknown4;
    private final int unknown5;
    private final int unknown6;
    private final int unknown7;
    
    private final Vector3f minTranslation;
    private final Vector3f maxTranslation;
    private final Vector3f minScale;
    private final Vector3f maxScale;
    
    private final int entryOffset;
    private final int indexOffset;
    private final int hashOffset;
    
    private final List<ANMEntry> entries;
    private final List<Short>    indecies;
    private final List<Integer>  boneHashes;
    
    public ANMDataVersion1(int dataSize, String subMagic, int subVersion, int boneCount, int entryCount, int unknown1, float animationLength, float FPS, int unknown2, int unknown3, int unknown4, int unknown5, int unknown6, int unknown7, Vector3f minTranslation, Vector3f maxTranslation, Vector3f minScale, Vector3f maxScale, int entryOffset, int indexOffset, int hashOffset, List<ANMEntry> entries, List<Short> indecies, List<Integer> boneHashes)
    {
        this.dataSize = dataSize;
        this.subMagic = subMagic;
        this.subVersion = subVersion;
        this.boneCount = boneCount;
        this.entryCount = entryCount;
        this.unknown1 = unknown1;
        this.animationLength = animationLength;
        this.FPS = FPS;
        this.unknown2 = unknown2;
        this.unknown3 = unknown3;
        this.unknown4 = unknown4;
        this.unknown5 = unknown5;
        this.unknown6 = unknown6;
        this.unknown7 = unknown7;
        this.minTranslation = minTranslation;
        this.maxTranslation = maxTranslation;
        this.minScale = minScale;
        this.maxScale = maxScale;
        this.entryOffset = entryOffset;
        this.indexOffset = indexOffset;
        this.hashOffset = hashOffset;
        this.entries = entries;
        this.indecies = indecies;
        this.boneHashes = boneHashes;
    }
    
    public int getDataSize()
    {
        return dataSize;
    }
    
    public String getSubMagic()
    {
        return subMagic;
    }
    
    public int getSubVersion()
    {
        return subVersion;
    }
    
    public int getBoneCount()
    {
        return boneCount;
    }
    
    public int getEntryCount()
    {
        return entryCount;
    }
    
    public int getUnknown1()
    {
        return unknown1;
    }
    
    public float getAnimationLength()
    {
        return animationLength;
    }
    
    public float getFPS()
    {
        return FPS;
    }
    
    public int getUnknown2()
    {
        return unknown2;
    }
    
    public int getUnknown3()
    {
        return unknown3;
    }
    
    public int getUnknown4()
    {
        return unknown4;
    }
    
    public int getUnknown5()
    {
        return unknown5;
    }
    
    public int getUnknown6()
    {
        return unknown6;
    }
    
    public int getUnknown7()
    {
        return unknown7;
    }
    
    public Vector3f getMinTranslation()
    {
        return minTranslation;
    }
    
    public Vector3f getMaxTranslation()
    {
        return maxTranslation;
    }
    
    public Vector3f getMinScale()
    {
        return minScale;
    }
    
    public Vector3f getMaxScale()
    {
        return maxScale;
    }
    
    public int getEntryOffset()
    {
        return entryOffset;
    }
    
    public int getIndexOffset()
    {
        return indexOffset;
    }
    
    public int getHashOffset()
    {
        return hashOffset;
    }
    
    public List<ANMEntry> getEntries()
    {
        return entries;
    }
    
    public List<Short> getIndecies()
    {
        return indecies;
    }
    
    public List<Integer> getBoneHashes()
    {
        return boneHashes;
    }
}
