package no.stelar7.cdragon.types.anm.data.versioned;

import no.stelar7.cdragon.types.anm.data.ANMFrame;
import no.stelar7.cdragon.util.types.math.*;

import java.util.*;

public class ANMDataVersion5
{
    private int unknown1;
    private int unknown2;
    private int unknown3;
    
    private int boneCount;
    private int frameCount;
    private int FPS;
    private int hashOffset;
    
    private int unknown4;
    private int unknown5;
    
    private int positionOffset;
    private int rotationOffset;
    private int frameOffset;
    
    private int unknown6;
    private int unknown7;
    private int unknown8;
    
    private List<Vector3f>               positions = new ArrayList<>();
    private List<Vector3s>               rotations = new ArrayList<>();
    private List<Integer>                hashes    = new ArrayList<>();
    private Map<Integer, List<ANMFrame>> frames    = new HashMap<>();
    
    public int getUnknown1()
    {
        return unknown1;
    }
    
    public void setUnknown1(int unknown1)
    {
        this.unknown1 = unknown1;
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
    
    public int getBoneCount()
    {
        return boneCount;
    }
    
    public void setBoneCount(int boneCount)
    {
        this.boneCount = boneCount;
    }
    
    public int getFrameCount()
    {
        return frameCount;
    }
    
    public void setFrameCount(int frameCount)
    {
        this.frameCount = frameCount;
    }
    
    public int getFPS()
    {
        return FPS;
    }
    
    public void setFPS(int FPS)
    {
        this.FPS = FPS;
    }
    
    public int getHashOffset()
    {
        return hashOffset;
    }
    
    public void setHashOffset(int hashOffset)
    {
        this.hashOffset = hashOffset;
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
    
    public int getPositionOffset()
    {
        return positionOffset;
    }
    
    public void setPositionOffset(int positionOffset)
    {
        this.positionOffset = positionOffset;
    }
    
    public int getRotationOffset()
    {
        return rotationOffset;
    }
    
    public void setRotationOffset(int rotationOffset)
    {
        this.rotationOffset = rotationOffset;
    }
    
    public int getFrameOffset()
    {
        return frameOffset;
    }
    
    public void setFrameOffset(int frameOffset)
    {
        this.frameOffset = frameOffset;
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
    
    public int getUnknown8()
    {
        return unknown8;
    }
    
    public void setUnknown8(int unknown8)
    {
        this.unknown8 = unknown8;
    }
    
    public List<Vector3f> getPositions()
    {
        return positions;
    }
    
    public void setPositions(List<Vector3f> positions)
    {
        this.positions = positions;
    }
    
    public List<Vector3s> getRotations()
    {
        return rotations;
    }
    
    public void setRotations(List<Vector3s> rotations)
    {
        this.rotations = rotations;
    }
    
    public List<Integer> getHashes()
    {
        return hashes;
    }
    
    public void setHashes(List<Integer> hashes)
    {
        this.hashes = hashes;
    }
    
    public Map<Integer, List<ANMFrame>> getFrames()
    {
        return frames;
    }
    
    public void setFrames(Map<Integer, List<ANMFrame>> frames)
    {
        this.frames = frames;
    }
}
