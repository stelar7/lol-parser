package no.stelar7.cdragon.types.skl;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.skl.data.*;
import no.stelar7.cdragon.types.skl.data.versioned.bone.*;
import no.stelar7.cdragon.types.skl.data.versioned.data.*;
import no.stelar7.cdragon.util.readers.*;
import no.stelar7.cdragon.util.types.math.*;

import java.util.*;


/**
 * Skeleton (.skl) reader.
 * <p>
 * Layout reference: LeagueToolkit Core/Animation/RigResource.cs (Read / ReadLegacy)
 */
public class SKLParser implements Parseable<SKLFile>
{
    public static final int    FORMAT_TOKEN = 0x22FD4FC3;
    public static final String LEGACY_MAGIC = "r3d2sklt";

    private static final int JOINT_SIZE = 100;

    @Override
    public SKLFile parse(RandomAccessReader raf)
    {
        if (raf.size() < 12)
        {
            throw new ParseException("File is too small to be a skeleton", 0);
        }

        raf.seek(4);
        boolean isModern = raf.readInt() == FORMAT_TOKEN;
        raf.seek(0);

        SKLHeader header  = parseHeader(raf);
        int       version = header.getVersion();

        if (isModern)
        {
            if (version != 0)
            {
                throw new ParseException("Unsupported skl version " + version, raf);
            }
            return new SKLFile(header, parseModern(raf), null, null);
        }

        if (!LEGACY_MAGIC.equals(header.getMagic()))
        {
            throw new ParseException("Invalid skl magic", 0);
        }

        if (version == 1)
        {
            int             designerId = raf.readInt();
            int             boneCount  = raf.readInt();
            List<SKLBoneV1> bones      = parseLegacyBones(raf, boneCount);
            return new SKLFile(header, null, new SKLDataV1(designerId, boneCount, bones), null);
        } else if (version == 2)
        {
            int             designerId = raf.readInt();
            int             boneCount  = raf.readInt();
            List<SKLBoneV1> bones      = parseLegacyBones(raf, boneCount);

            int boneIndexCounter = raf.readInt();
            if (boneIndexCounter < 0 || (long) boneIndexCounter * 4 > raf.remaining())
            {
                throw new ParseException("Invalid skl influence count " + boneIndexCounter, raf);
            }
            List<Integer> boneIndecies = raf.readInts(boneIndexCounter);
            return new SKLFile(header, null, null, new SKLDataV2(designerId, boneCount, bones, boneIndexCounter, boneIndecies));
        } else
        {
            throw new ParseException("Unsupported legacy skl version " + version, raf);
        }
    }

    private SKLDataV0 parseModern(RandomAccessReader raf)
    {
        // flags
        short unknown1       = raf.readShort();
        short boneCountRaw   = raf.readShort();
        int   boneIndexCount = raf.readInt();

        // joints
        int boneStart         = raf.readInt();
        // joint id/hash pairs, sorted by hash
        int animationStart    = raf.readInt();
        // influences
        int boneIndexStart    = raf.readInt();
        // skeleton name
        int boneIndexEnd      = raf.readInt();
        // asset name
        int halfwayBoneString = raf.readInt();
        // joint name table
        int boneNameStart     = raf.readInt();
        SKLDataV0Offsets offsets = new SKLDataV0Offsets(boneStart, animationStart, boneIndexStart, boneIndexEnd, halfwayBoneString, boneNameStart, raf.readIntArray(5));

        int boneCount      = boneCountRaw & 0xFFFF;
        int influenceCount = boneIndexCount;

        List<SKLBoneV0>      bones        = new ArrayList<>();
        List<SKLBoneV0Extra> boneExtra    = new ArrayList<>();
        List<Short>          boneIndecies = new ArrayList<>();
        List<String>         boneNames    = new ArrayList<>();
        String               name         = null;
        String               assetName    = null;

        if (offsets.getBoneStart() > 0)
        {
            checkRange(raf, offsets.getBoneStart(), (long) boneCount * JOINT_SIZE, "joints");
            raf.seek(offsets.getBoneStart());
            for (int i = 0; i < boneCount; i++)
            {
                // flags
                short boneUnknown1 = raf.readShort();
                short id           = raf.readShort();
                short parent       = raf.readShort();
                // padding
                short boneUnknown2 = raf.readShort();
                int   hash         = raf.readInt();
                // radius
                float radius       = raf.readFloat();

                // local transform
                Vector3f position = raf.readVec3F();
                Vector3f scale    = raf.readVec3F();
                Vector4f rotation = raf.readVec4F();

                // inverse bind transform
                Vector3f ct                  = raf.readVec3F();
                Vector3f inverseBindScale    = raf.readVec3F();
                Vector4f inverseBindRotation = raf.readVec4F();

                long   fieldPos   = raf.pos();
                int    nameOffset = raf.readInt();
                String boneName   = readNameAt(raf, fieldPos + nameOffset);

                bones.add(new SKLBoneV0(boneUnknown1, id, parent, boneUnknown2, hash, radius, position, scale, rotation, ct, inverseBindScale, inverseBindRotation, nameOffset, boneName));
                boneNames.add(boneName);
            }
        }

        if (offsets.getAnimationStart() > 0)
        {
            checkRange(raf, offsets.getAnimationStart(), (long) boneCount * 8, "joint hashes");
            raf.seek(offsets.getAnimationStart());
            for (int i = 0; i < boneCount; i++)
            {
                int boneId = raf.readShort();
                // padding
                raf.readShort();
                int boneHash = raf.readInt();
                boneExtra.add(new SKLBoneV0Extra(boneId, boneHash));
            }
        }

        if (offsets.getBoneIndexStart() > 0)
        {
            if (influenceCount < 0)
            {
                throw new ParseException("Invalid skl influence count " + influenceCount, raf);
            }
            checkRange(raf, offsets.getBoneIndexStart(), (long) influenceCount * 2, "influences");
            raf.seek(offsets.getBoneIndexStart());
            boneIndecies = raf.readShorts(influenceCount);
        }

        if (offsets.getBoneIndexEnd() > 0)
        {
            name = readNameAt(raf, offsets.getBoneIndexEnd());
        }

        if (offsets.getHalfwayBoneString() > 0)
        {
            assetName = readNameAt(raf, offsets.getHalfwayBoneString());
        }

        return new SKLDataV0(unknown1, boneCountRaw, boneIndexCount, offsets, bones, boneExtra, boneIndecies, boneNames, name, assetName);
    }

    private List<SKLBoneV1> parseLegacyBones(RandomAccessReader raf, int boneCount)
    {
        // name(32) + parent(4) + radius(4) + 3x4 matrix(48)
        if (boneCount < 0 || (long) boneCount * 88 > raf.remaining())
        {
            throw new ParseException("Invalid skl bone count " + boneCount, raf);
        }

        List<SKLBoneV1> bones = new ArrayList<>(boneCount);
        for (int i = 0; i < boneCount; i++)
        {
            String     name   = raf.readFixedString(32);
            int        parent = raf.readInt();
            float      scale  = raf.readFloat();
            Matrix4x3f matrix = readLegacyTransform(raf);
            bones.add(new SKLBoneV1(name, parent, scale, matrix));
        }
        return bones;
    }

    /**
     * The legacy global transform is stored as 3 rows of 4 floats (row-major 3x4, translation in the last column).
     * JOML matrices are indexed [column][row], so every value goes into the transposed cell.
     */
    private Matrix4x3f readLegacyTransform(RandomAccessReader raf)
    {
        float[] r = raf.readFloatArray(12);

        Matrix4x3f mat = new Matrix4x3f();
        mat.set(r[0], r[4], r[8],
                r[1], r[5], r[9],
                r[2], r[6], r[10],
                r[3], r[7], r[11]);
        return mat;
    }

    private String readNameAt(RandomAccessReader raf, long offset)
    {
        if (offset < 0 || offset >= raf.size())
        {
            throw new ParseException("skl name offset " + offset + " is out of bounds", raf);
        }
        return raf.readFromOffset((int) offset);
    }

    private void checkRange(RandomAccessReader raf, long offset, long length, String what)
    {
        if (offset < 0 || offset + length > raf.size())
        {
            throw new ParseException("skl " + what + " section is out of bounds", offset);
        }
    }

    private SKLHeader parseHeader(RandomAccessReader raf)
    {
        // file size + format token for the modern format
        String magic   = raf.readString(8);
        int    version = raf.readInt();
        return new SKLHeader(magic, version);
    }
}
