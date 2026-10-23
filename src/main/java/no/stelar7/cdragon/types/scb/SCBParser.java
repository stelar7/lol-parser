package no.stelar7.cdragon.types.scb;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.scb.data.*;
import no.stelar7.cdragon.util.readers.*;
import no.stelar7.cdragon.util.types.math.*;

import java.util.*;

/**
 * Binary static
 mesh (.scb) reader.
 * <p>
 * Layout reference: LeagueToolkit Core/Mesh/StaticMesh.cs (ReadBinary) and StaticMeshFace.cs
 */
public class SCBParser implements Parseable<SCBFile>
{
    public static final String MAGIC = "r3d2Mesh";

    public static final int FLAG_HAS_VCP                     = 1;
    public static final int FLAG_HAS_LOCAL_ORIGIN_AND_PIVOT = 2;

    // indices(12) + material(64) + uvs(24)
    private static final int FACE_SIZE = 100;

    /**
     * True if the data starts with "r3d2Mesh"
     */
    @Override
    public boolean canParse(byte[] data)
    {
        if (data == null || data.length < MAGIC.length())
        {
            return false;
        }
        for (int i = 0; i < MAGIC.length(); i++)
        {
            if (data[i] != MAGIC.charAt(i))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public SCBFile parse(RandomAccessReader raf)
    {
        SCBHeader header = readHeader(raf);
        return new SCBFile(header, readContent(header, raf));
    }

    private SCBContent readContent(SCBHeader header, RandomAccessReader raf)
    {
        int vertexCount = raf.readInt();
        int faceCount   = raf.readInt();
        int colored     = raf.readInt();

        Vector3f boundingBoxMin = raf.readVec3F();
        Vector3f boundingBoxMax = raf.readVec3F();

        boolean hasVertexColors = false;
        int     major           = header.getMajor();
        int     minor           = header.getMinor();
        if (major >= 3 && minor >= 2)
        {
            hasVertexColors = raf.readInt() == 1;
        }

        if (vertexCount < 0 || faceCount < 0 || (long) vertexCount * 12 + (long) faceCount * FACE_SIZE > raf.remaining())
        {
            throw new ParseException("Invalid scb vertex/face count", raf);
        }

        float[]        vertexData = raf.readFloatArray(vertexCount * 3);
        List<Vector3f> vertices   = new ArrayList<>(vertexCount);
        for (int i = 0; i < vertexCount; i++)
        {
            vertices.add(new Vector3f(vertexData[i * 3], vertexData[i * 3 + 1], vertexData[i * 3 + 2]));
        }

        List<Vector4b> vertexColors = new ArrayList<>();
        if (hasVertexColors)
        {
            // BGRA
            for (int i = 0; i < vertexCount; i++)
            {
                vertexColors.add(raf.readVec4B());
            }
        }

        Vector3f center = raf.readVec3F();

        List<SCBFace> faces = new ArrayList<>(faceCount);
        for (int i = 0; i < faceCount; i++)
        {
            faces.add(readFace(raf));
        }

        List<Vector3b> colors = new ArrayList<>();
        if ((colored & FLAG_HAS_VCP) != 0)
        {
            // RGB per face corner
            int idxCount = faceCount * 3;
            for (int i = 0; i < idxCount; i++)
            {
                colors.add(raf.readVec3B());
            }
        }

        return new SCBContent(vertexCount, faceCount, colored, boundingBoxMin, boundingBoxMax, center, null, hasVertexColors, vertexColors, vertices, faces, colors);
    }

    private SCBFace readFace(RandomAccessReader raf)
    {
        Vector3i indecies = raf.readVec3I();
        String   material = raf.readFixedString(64);

        // u0 u1 u2, v0 v1 v2
        Vector3f u = raf.readVec3F();
        Vector3f v = raf.readVec3F();

        return new SCBFace(indecies, material, u, v);
    }

    private SCBHeader readHeader(RandomAccessReader raf)
    {
        String magic = raf.readString(8);
        if (!MAGIC.equals(magic))
        {
            throw new ParseException("Invalid scb magic", 0);
        }

        short major = raf.readShort();
        short minor = raf.readShort();

        // known versions: 1.1, 2.1, 2.2, 3.1, 3.2
        if (major < 1 || major > 3)
        {
            throw new ParseException("Unsupported scb version: " + major + "." + minor, raf);
        }

        return new SCBHeader(magic, major, minor, raf.readFixedString(128));
    }
}
