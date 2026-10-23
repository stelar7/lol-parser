package no.stelar7.cdragon.types.anm.data.versioned;

import no.stelar7.cdragon.types.anm.data.ANMFrame;
import org.joml.*;

import java.util.*;

public final class ANMDataVersion4
{
    private final int dataSize;
    private final int designerId;
    
    private final int unknown1;
    private final int unknown2;
    
    private final int boneCount;
    private final int frameCount;
    // frames per second (1 / frameDuration)
    private final float FPS;
    private final float frameDuration;
    
    private final int unknown3;
    private final int unknown4;
    private final int unknown5;
    
    private final int positionOffset;
    private final int rotationOffset;
    private final int frameOffset;
    
    private final int unknown6;
    private final int unknown7;
    private final int unknown8;
    
    private final List<Vector3f>               positions;
    private final List<Quaternionf>            rotations;
    private final Map<Integer, List<ANMFrame>> frames;
    
    public ANMDataVersion4(int dataSize, int designerId, int unknown1, int unknown2, int boneCount, int frameCount, float FPS, float frameDuration, int unknown3, int unknown4, int unknown5, int positionOffset, int rotationOffset, int frameOffset, int unknown6, int unknown7, int unknown8, List<Vector3f> positions, List<Quaternionf> rotations, Map<Integer, List<ANMFrame>> frames)
    {
        this.dataSize = dataSize;
        this.designerId = designerId;
        this.unknown1 = unknown1;
        this.unknown2 = unknown2;
        this.boneCount = boneCount;
        this.frameCount = frameCount;
        this.FPS = FPS;
        this.frameDuration = frameDuration;
        this.unknown3 = unknown3;
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
        this.frames = frames;
    }
    
    public int getDataSize()
    {
        return dataSize;
    }
    
    public int getDesignerId()
    {
        return designerId;
    }
    
    public int getUnknown1()
    {
        return unknown1;
    }
    
    public int getUnknown2()
    {
        return unknown2;
    }
    
    public int getBoneCount()
    {
        return boneCount;
    }
    
    public int getFrameCount()
    {
        return frameCount;
    }
    
    public float getFPS()
    {
        return FPS;
    }
    
    /**
     * Duration of a single frame in seconds
     */
    public float getFrameDuration()
    {
        return frameDuration;
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
    
    public List<Quaternionf> getRotations()
    {
        return rotations;
    }
    
    public Map<Integer, List<ANMFrame>> getFrames()
    {
        return frames;
    }
}
