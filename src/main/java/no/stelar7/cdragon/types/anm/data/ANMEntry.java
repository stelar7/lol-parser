package no.stelar7.cdragon.types.anm.data;

import no.stelar7.cdragon.util.types.math.*;
import org.joml.Quaternionf;

/**
 * A single key frame of a compressed (r3d2canm) animation, for one channel of one joint.
 */
public final class ANMEntry
{
    public static final int TYPE_ROTATION    = 0;
    public static final int TYPE_TRANSLATION = 1;
    public static final int TYPE_SCALE       = 2;

    // u16, 0..65535 maps to 0..duration
    private final int compressedTime;
    // index into the joint hash table (14 bits)
    private final int hashId;
    // TYPE_ROTATION, TYPE_TRANSLATION or TYPE_SCALE (2 bits)
    private final int dataType;
    // raw quantized value
    private final Vector3s compressedData;

    // decoded values
    private final int         jointHash;
    private final float       time;
    private final Quaternionf rotation;
    private final Vector3f    vector;
    
    public ANMEntry(int compressedTime, int hashId, int dataType, Vector3s compressedData, int jointHash, float time, Quaternionf rotation, Vector3f vector)
    {
        this.compressedTime = compressedTime;
        this.hashId = hashId;
        this.dataType = dataType;
        this.compressedData = compressedData;
        this.jointHash = jointHash;
        this.time = time;
        this.rotation = rotation;
        this.vector = vector;
    }

    public int getCompressedTime()
    {
        return compressedTime;
    }

    public int getHashId()
    {
        return hashId;
    }

    public int getDataType()
    {
        return dataType;
    }

    public Vector3s getCompressedData()
    {
        return compressedData;
    }

    public int getJointHash()
    {
        return jointHash;
    }

    /**
     * Key time in seconds
     */
    public float getTime()
    {
        return time;
    }

    /**
     * The decoded rotation, only set when dataType is TYPE_ROTATION
     */
    public Quaternionf getRotation()
    {
        return rotation;
    }

    /**
     * The decoded translation or scale, only set when dataType is TYPE_TRANSLATION or TYPE_SCALE
     */
    public Vector3f getVector()
    {
        return vector;
    }
}
