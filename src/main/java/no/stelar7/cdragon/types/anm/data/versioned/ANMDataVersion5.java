package no.stelar7.cdragon.types.anm.data.versioned;

import no.stelar7.cdragon.types.anm.data.ANMFrame;
import no.stelar7.cdragon.util.types.math.*;
import org.joml.Quaternionf;

import java.util.*;

public final class ANMDataVersion5
{
    private final int fileSize;
    
    private final int unknown1;
    private final int unknown2;
    private final int unknown3;
    
    private final int   boneCount;
    private final int   frameCount;
    private final float frameDelay;
    private final int   hashOffset;
    
    private final int unknown4;
    private final int unknown5;
    
    private final int positionOffset;
    private final int rotationOffset;
    private final int frameOffset;
    
    private final int unknown6;
    private final int unknown7;
    private final int unknown8;
    
    private final List<Vector3f> positions;
    // raw 48 bit quantized quaternions
    private final List<Vector3s> rotations;
    // the rotations, dequantized
    private final List<Quaternionf>            quaternions;
    private final List<Integer>                hashes;
    private final Map<Integer, List<ANMFrame>> frames;
    
    public ANMDataVersion5(int fileSize, int unknown1, int unknown2, int unknown3, int boneCount, int frameCount, float frameDelay, int hashOffset, int unknown4, int unknown5, int positionOffset, int rotationOffset, int frameOffset, int unknown6, int unknown7, int unknown8, List<Vector3f> positions, List<Vector3s> rotations, List<Quaternionf> quaternions, List<Integer> hashes, Map<Integer, List<ANMFrame>> frames)
    {
        this.fileSize = fileSize;
        this.unknown1 = unknown1;
        this.unknown2 = unknown2;
        this.unknown3 = unknown3;
        this.boneCount = boneCount;
        this.frameCount = frameCount;
        this.frameDelay = frameDelay;
        this.hashOffset = hashOffset;
        this.unknown4 = unknown4;
        this.unknown5 = unknown5;
        this.positionOffset = positionOffset;
        this.rotationOffset = rotationOffset;
        this.frameOffset = frameOffset;
        this.unknown6 = unknown6;
        this.unknown7 = unknown7;
        this.unknown8 = unknown8;
        this.positions = positions;
        this.rotations = rotations;
        this.quaternions = quaternions;
        this.hashes = hashes;
        this.frames = frames;
    }
    
    public int getFileSize()
    {
        return fileSize;
    }
    
    public int getUnknown1()
    {
        return unknown1;
    }
    
    public int getUnknown2()
    {
        return unknown2;
    }
    
    public int getUnknown3()
    {
        return unknown3;
    }
    
    public int getBoneCount()
    {
        return boneCount;
    }
    
    public int getFrameCount()
    {
        return frameCount;
    }
    
    public float getFrameDelay()
    {
        return frameDelay;
    }
    
    public int getHashOffset()
    {
        return hashOffset;
    }
    
    public int getUnknown4()
    {
        return unknown4;
    }
    
    public int getUnknown5()
    {
        return unknown5;
    }
    
    public int getPositionOffset()
    {
        return positionOffset;
    }
    
    public int getRotationOffset()
    {
        return rotationOffset;
    }
    
    public int getFrameOffset()
    {
        return frameOffset;
    }
    
    public int getUnknown6()
    {
        return unknown6;
    }
    
    public int getUnknown7()
    {
        return unknown7;
    }
    
    public int getUnknown8()
    {
        return unknown8;
    }
    
    public List<Vector3f> getPositions()
    {
        return positions;
    }
    
    public List<Vector3s> getRotations()
    {
        return rotations;
    }
    
    public List<Quaternionf> getQuaternions()
    {
        return quaternions;
    }
    
    public List<Integer> getHashes()
    {
        return hashes;
    }
    
    public Map<Integer, List<ANMFrame>> getFrames()
    {
        return frames;
    }
}
