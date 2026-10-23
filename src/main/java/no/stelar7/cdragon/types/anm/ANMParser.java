package no.stelar7.cdragon.types.anm;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.anm.data.*;
import no.stelar7.cdragon.types.anm.data.versioned.*;
import no.stelar7.cdragon.util.readers.*;
import no.stelar7.cdragon.util.types.math.*;
import org.joml.Quaternionf;

import java.util.*;

/**
 * Animation (.anm) reader.
 * <p>
 * Layout reference: LeagueToolkit Core/Animation/UncompressedAnimationAsset.cs and CompressedAnimationAsset.cs.
 * <p>
 * Every offset in the modern headers is relative to the end of the magic + version (12 bytes).
 */
public class ANMParser implements Parseable<ANMFile>
{
    public static final String MAGIC_UNCOMPRESSED = "r3d2anmd";
    public static final String MAGIC_COMPRESSED   = "r3d2canm";

    private static final int OFFSET_BASE = 12;

    @Override
    public ANMFile parse(RandomAccessReader raf)
    {
        if (raf.size() < OFFSET_BASE)
        {
            throw new ParseException("File is too small to be an animation", 0);
        }

        ANMHeader header  = parseHeader(raf);
        String    magic   = header.getMagic();
        int       version = header.getVersion();

        if (MAGIC_COMPRESSED.equals(magic))
        {
            if (version < 1 || version > 3)
            {
                throw new ParseException("Unsupported compressed anm version " + version, raf);
            }
            // all compressed versions share the same layout
            return new ANMFile(header, parseCompressed(raf), null, null, null);
        }

        if (!MAGIC_UNCOMPRESSED.equals(magic))
        {
            throw new ParseException("Invalid anm magic: " + magic, 0);
        }

        return switch (version)
        {
            case 3 -> new ANMFile(header, null, parseVersion3(raf), null, null);
            case 4 -> new ANMFile(header, null, null, parseVersion4(raf), null);
            case 5 -> new ANMFile(header, null, null, null, parseVersion5(raf));
            default -> throw new ParseException("Unsupported anm version " + version, raf);
        };
    }

    private ANMDataVersion5 parseVersion5(RandomAccessReader raf)
    {
        int fileSize = raf.readInt();

        // format token
        int unknown1 = raf.readInt();
        // version
        int unknown2 = raf.readInt();
        // flags
        int unknown3 = raf.readInt();

        int   boneCount  = raf.readInt();
        int   frameCount = raf.readInt();
        float frameDelay = raf.readFloat();
        int   hashOffset = raf.readInt();

        // asset name offset
        int unknown4 = raf.readInt();
        // time offset
        int unknown5 = raf.readInt();

        int positionOffset = raf.readInt();
        int rotationOffset = raf.readInt();
        int frameOffset    = raf.readInt();

        int unknown6 = raf.readInt();
        int unknown7 = raf.readInt();
        int unknown8 = raf.readInt();

        if (hashOffset <= 0 || positionOffset <= 0 || rotationOffset <= 0 || frameOffset <= 0)
        {
            throw new ParseException("anm is missing a data section", raf);
        }

        int hashCount     = sectionCount(raf, hashOffset, frameOffset, 4, "joint hashes");
        int positionCount = sectionCount(raf, positionOffset, rotationOffset, 12, "vector palette");
        int rotationCount = sectionCount(raf, rotationOffset, hashOffset, 6, "quaternion palette");

        seekSection(raf, hashOffset, (long) hashCount * 4, "joint hashes");
        int[]         hashes   = raf.readIntArray(hashCount);
        List<Integer> hashList = new ArrayList<>(hashCount);
        for (int hash : hashes)
        {
            hashList.add(hash);
        }

        seekSection(raf, positionOffset, (long) positionCount * 12, "vector palette");
        float[]        positionData = raf.readFloatArray(positionCount * 3);
        List<Vector3f> positions    = new ArrayList<>(positionCount);
        for (int i = 0; i < positionCount; i++)
        {
            positions.add(new Vector3f(positionData[i * 3], positionData[i * 3 + 1], positionData[i * 3 + 2]));
        }

        seekSection(raf, rotationOffset, (long) rotationCount * 6, "quaternion palette");
        int[]             rotationData = raf.readU16Array(rotationCount * 3);
        List<Vector3s>    rotations    = new ArrayList<>(rotationCount);
        List<Quaternionf> quaternions  = new ArrayList<>(rotationCount);
        for (int i = 0; i < rotationCount; i++)
        {
            int a = rotationData[i * 3];
            int b = rotationData[i * 3 + 1];
            int c = rotationData[i * 3 + 2];

            Vector3s raw = new Vector3s();
            raw.setX((short) a);
            raw.setY((short) b);
            raw.setZ((short) c);
            rotations.add(raw);
            quaternions.add(decompressQuaternion(a, b, c).normalize());
        }

        if (boneCount < 0 || frameCount < 0 || boneCount > hashCount)
        {
            throw new ParseException("Invalid anm track/frame count", raf);
        }

        seekSection(raf, frameOffset, (long) frameCount * boneCount * 6, "frames");
        int[]                        frameData = raf.readU16Array(frameCount * boneCount * 3);
        int                          index     = 0;
        Map<Integer, List<ANMFrame>> frameMap  = new HashMap<>();
        for (int i = 0; i < frameCount; i++)
        {
            List<ANMFrame> frames = new ArrayList<>(boneCount);
            for (int j = 0; j < boneCount; j++)
            {
                int positionId = frameData[index++];
                int scaleId    = frameData[index++];
                int rotationId = frameData[index++];
                frames.add(new ANMFrame(hashes[j], positionId, scaleId, rotationId, 0));
            }
            frameMap.put(i, frames);
        }

        return new ANMDataVersion5(fileSize, unknown1, unknown2, unknown3, boneCount, frameCount, frameDelay, hashOffset, unknown4, unknown5,
                                   positionOffset, rotationOffset, frameOffset, unknown6, unknown7, unknown8,
                                   positions, rotations, quaternions, hashList, frameMap);
    }

    private ANMDataVersion4 parseVersion4(RandomAccessReader raf)
    {
        int dataSize   = raf.readInt();
        // format token
        int designerId = raf.readInt();

        // version
        int unknown1 = raf.readInt();
        // flags
        int unknown2 = raf.readInt();

        int boneCount  = raf.readInt();
        int frameCount = raf.readInt();

        float frameDuration = raf.readFloat();
        float fps           = frameDuration != 0 ? 1f / frameDuration : 0;

        // joint name hashes offset (unused, v4 stores the hashes in the frames)
        int unknown3 = raf.readInt();
        // asset name offset
        int unknown4 = raf.readInt();
        // time offset
        int unknown5 = raf.readInt();

        int positionOffset = raf.readInt();
        int rotationOffset = raf.readInt();
        int frameOffset    = raf.readInt();

        int unknown6 = raf.readInt();
        int unknown7 = raf.readInt();
        int unknown8 = raf.readInt();

        if (positionOffset <= 0 || rotationOffset <= 0 || frameOffset <= 0)
        {
            throw new ParseException("anm is missing a data section", raf);
        }

        int positionCount = sectionCount(raf, positionOffset, rotationOffset, 12, "vector palette");
        int rotationCount = sectionCount(raf, rotationOffset, frameOffset, 16, "quaternion palette");

        seekSection(raf, positionOffset, (long) positionCount * 12, "vector palette");
        float[]                 positionData = raf.readFloatArray(positionCount * 3);
        List<org.joml.Vector3f> positions    = new ArrayList<>(positionCount);
        for (int i = 0; i < positionCount; i++)
        {
            positions.add(new Vector3f(positionData[i * 3], positionData[i * 3 + 1], positionData[i * 3 + 2]));
        }

        seekSection(raf, rotationOffset, (long) rotationCount * 16, "quaternion palette");
        float[]           rotationData = raf.readFloatArray(rotationCount * 4);
        List<Quaternionf> rotations    = new ArrayList<>(rotationCount);
        for (int i = 0; i < rotationCount; i++)
        {
            rotations.add(new Quaternionf(rotationData[i * 4], rotationData[i * 4 + 1], rotationData[i * 4 + 2], rotationData[i * 4 + 3]));
        }

        if (boneCount < 0 || frameCount < 0)
        {
            throw new ParseException("Invalid anm track/frame count", raf);
        }

        seekSection(raf, frameOffset, (long) frameCount * boneCount * 12, "frames");
        Map<Integer, List<ANMFrame>> frameMap = new HashMap<>();
        for (int i = 0; i < frameCount; i++)
        {
            List<ANMFrame> frames = new ArrayList<>(boneCount);
            for (int j = 0; j < boneCount; j++)
            {
                int boneHash   = raf.readInt();
                int positionId = raf.readU16();
                int scaleId    = raf.readU16();
                int rotationId = raf.readU16();
                // padding
                int padding    = raf.readU16();
                frames.add(new ANMFrame(boneHash, positionId, scaleId, rotationId, padding));
            }
            frameMap.put(i, frames);
        }

        return new ANMDataVersion4(dataSize, designerId, unknown1, unknown2, boneCount, frameCount, fps, frameDuration, unknown3, unknown4, unknown5,
                                   positionOffset, rotationOffset, frameOffset, unknown6, unknown7, unknown8,
                                   positions, rotations, frameMap);
    }

    private ANMDataVersion3 parseVersion3(RandomAccessReader raf)
    {
        // skeleton id
        int designerId = raf.readInt();
        int boneCount  = raf.readInt();
        int frameCount = raf.readInt();
        int fps        = raf.readInt();

        // name(32) + flags(4) + frames * (quaternion(16) + translation(12))
        long boneSize = 36 + (long) frameCount * 28;
        if (boneCount < 0 || frameCount < 0 || boneSize * boneCount > raf.remaining())
        {
            throw new ParseException("Invalid anm track/frame count", raf);
        }

        List<ANMBone> bones = new ArrayList<>(boneCount);
        for (int i = 0; i < boneCount; i++)
        {
            String name = raf.readFixedString(32);
            int    flag = raf.readInt();

            float[]            data   = raf.readFloatArray(frameCount * 7);
            List<ANMBoneFrame> frames = new ArrayList<>(frameCount);
            for (int j = 0; j < frameCount; j++)
            {
                int o = j * 7;
                frames.add(new ANMBoneFrame(new Quaternionf(data[o], data[o + 1], data[o + 2], data[o + 3]), new org.joml.Vector3f(data[o + 4], data[o + 5], data[o + 6])));
            }

            bones.add(new ANMBone(name, flag, frames));
        }

        return new ANMDataVersion3(designerId, boneCount, frameCount, fps, bones);
    }

    /**
     * Compressed (r3d2canm) animation, versions 1-3.
     * <p>
     * Every frame is a single key for one channel of one joint; the key values are dequantized here.
     * The jump caches (only needed for random access evaluation) are kept as raw data.
     */
    private ANMDataVersion1 parseCompressed(RandomAccessReader raf)
    {
        int    dataSize       = raf.readInt();
        // format token
        String subMagic       = raf.readString(4);
        // flags
        int    subVersion     = raf.readInt();
        int    jointCount     = raf.readInt();
        int    frameCount     = raf.readInt();
        // jump cache count
        int    jumpCacheCount = raf.readInt();
        // duration in seconds
        float  duration       = raf.readFloat();
        float  fps            = raf.readFloat();

        // error metrics (margin, discontinuity threshold) for rotation, translation and scale
        int unknown2 = raf.readInt();
        int unknown3 = raf.readInt();
        int unknown4 = raf.readInt();
        int unknown5 = raf.readInt();
        int unknown6 = raf.readInt();
        int unknown7 = raf.readInt();

        Vector3f minT = raf.readVec3F();
        Vector3f maxT = raf.readVec3F();
        Vector3f minS = raf.readVec3F();
        Vector3f maxS = raf.readVec3F();

        int entryOffset = raf.readInt();
        // jump cache offset
        int indexOffset = raf.readInt();
        int hashOffset  = raf.readInt();

        if (jointCount < 0 || jointCount > 0x4000 || frameCount < 0 || jumpCacheCount < 0)
        {
            throw new ParseException("Invalid anm joint/frame count", raf);
        }
        if (entryOffset <= 0 || hashOffset <= 0)
        {
            throw new ParseException("anm is missing a data section", raf);
        }

        seekSection(raf, hashOffset, (long) jointCount * 4, "joint hashes");
        int[]         hashes     = raf.readIntArray(jointCount);
        List<Integer> boneHashes = new ArrayList<>(jointCount);
        for (int hash : hashes)
        {
            boneHashes.add(hash);
        }

        seekSection(raf, entryOffset, (long) frameCount * 10, "frames");
        int[]          frames  = raf.readU16Array(frameCount * 5);
        List<ANMEntry> entries = new ArrayList<>(frameCount);
        for (int i = 0; i < frameCount; i++)
        {
            int o         = i * 5;
            int time      = frames[o];
            int jointType = frames[o + 1];
            int a         = frames[o + 2];
            int b         = frames[o + 3];
            int c         = frames[o + 4];

            int hashId   = jointType & 0x3FFF;
            int dataType = jointType >>> 14;

            Vector3s raw = new Vector3s();
            raw.setX((short) a);
            raw.setY((short) b);
            raw.setZ((short) c);

            if (hashId >= jointCount)
            {
                throw new ParseException("anm frame " + i + " references joint " + hashId + " of " + jointCount, raf);
            }

            Quaternionf rotation = null;
            Vector3f    vector   = null;
            switch (dataType)
            {
                case ANMEntry.TYPE_ROTATION -> rotation = decompressQuaternion(a, b, c).normalize();
                case ANMEntry.TYPE_TRANSLATION -> vector = decompressVector(a, b, c, minT, maxT);
                case ANMEntry.TYPE_SCALE -> vector = decompressVector(a, b, c, minS, maxS);
                default -> throw new ParseException("anm frame " + i + " has unknown transform type " + dataType, raf);
            }

            entries.add(new ANMEntry(time, hashId, dataType, raw, hashes[hashId], time / 65535f * duration, rotation, vector));
        }

        // jump caches, 24 (u16 keys) or 48 (u32 keys) bytes per joint per cache; kept raw
        List<Short> indecies = new ArrayList<>();
        if (indexOffset > 0)
        {
            long jumpFrameSize  = frameCount < 0x10001 ? 24 : 48;
            long jumpCacheBytes = jumpFrameSize * jointCount * jumpCacheCount;
            seekSection(raf, indexOffset, jumpCacheBytes, "jump caches");
            short[] caches = raf.readShortArray((int) (jumpCacheBytes / 2));
            for (short value : caches)
            {
                indecies.add(value);
            }
        }

        return new ANMDataVersion1(dataSize, subMagic, subVersion, jointCount, frameCount, jumpCacheCount, duration, fps,
                                   unknown2, unknown3, unknown4, unknown5, unknown6, unknown7,
                                   minT, maxT, minS, maxS, entryOffset, indexOffset, hashOffset,
                                   entries, indecies, boneHashes);
    }

    /**
     * 48 bit "smallest three" quaternion: 2 bits for the index of the largest component, then 3 * 15 bits.
     */
    public static Quaternionf decompressQuaternion(int first, int second, int third)
    {
        final double SQRT_2         = 1.41421356237;
        final double ONE_OVER_SQRT2 = 0.70710678118;

        long bits     = (first & 0xFFFFL) | (second & 0xFFFFL) << 16 | (third & 0xFFFFL) << 32;
        int  maxIndex = (int) ((bits >>> 45) & 0x3);
        int  va       = (int) ((bits >>> 30) & 0x7FFF);
        int  vb       = (int) ((bits >>> 15) & 0x7FFF);
        int  vc       = (int) (bits & 0x7FFF);

        float a   = (float) (va / 32767.0 * SQRT_2 - ONE_OVER_SQRT2);
        float b   = (float) (vb / 32767.0 * SQRT_2 - ONE_OVER_SQRT2);
        float c   = (float) (vc / 32767.0 * SQRT_2 - ONE_OVER_SQRT2);
        float sub = Math.max(0, 1 - (a * a + b * b + c * c));
        float d   = (float) Math.sqrt(sub);

        return switch (maxIndex)
        {
            case 0 -> new Quaternionf(d, a, b, c);
            case 1 -> new Quaternionf(a, d, b, c);
            case 2 -> new Quaternionf(a, b, d, c);
            default -> new Quaternionf(a, b, c, d);
        };
    }

    private static Vector3f decompressVector(int x, int y, int z, Vector3f min, Vector3f max)
    {
        return new Vector3f(min.x + (max.x - min.x) * (x / 65535f),
                            min.y + (max.y - min.y) * (y / 65535f),
                            min.z + (max.z - min.z) * (z / 65535f));
    }

    /**
     * Number of elements between two (header relative) offsets
     */
    private int sectionCount(RandomAccessReader raf, int start, int end, int elementSize, String name)
    {
        long count = ((long) end - start) / elementSize;
        if (count < 0 || count > raf.size() / elementSize)
        {
            throw new ParseException("anm " + name + " section has an invalid size", raf);
        }
        return (int) count;
    }

    private void seekSection(RandomAccessReader raf, int offset, long length, String name)
    {
        long absolute = (long) offset + OFFSET_BASE;
        if (offset < 0 || length < 0 || absolute + length > raf.size())
        {
            throw new ParseException("anm " + name + " section is out of bounds", absolute);
        }
        raf.seek(absolute);
    }

    private ANMHeader parseHeader(RandomAccessReader raf)
    {
        String magic   = raf.readString(8);
        int    version = raf.readInt();
        return new ANMHeader(magic, version);

    }
}
