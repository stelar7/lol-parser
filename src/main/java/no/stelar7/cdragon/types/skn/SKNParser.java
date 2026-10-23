package no.stelar7.cdragon.types.skn;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.skn.data.*;
import no.stelar7.cdragon.util.readers.*;
import no.stelar7.cdragon.util.types.math.*;


import java.util.*;

/**
 * Simple skin (.skn) reader.
 * <p>
 * Layout reference: LeagueToolkit Core/Mesh/SkinnedMesh.cs (ReadFromSimpleSkin)
 */
public class SKNParser implements Parseable<SKNFile>
{
    public static final int MAGIC = 0x00112233;

    // position(12) + blend indices(4) + blend weights(16) + normal(12) + uv(8)
    private static final int BASIC_VERTEX_SIZE   = 52;
    // + BGRA color (4)
    private static final int COLOR_VERTEX_SIZE   = 56;
    // + tangent (16)
    private static final int TANGENT_VERTEX_SIZE = 72;

    @Override
    public SKNFile parse(RandomAccessReader raf)
    {
        int magic = raf.readInt();
        if (magic != MAGIC)
        {
            throw new ParseException("Invalid skn magic: " + Integer.toHexString(magic), 0);
        }

        short majorRaw = raf.readShort();
        short minorRaw = raf.readShort();

        int major = majorRaw & 0xFFFF;
        if (major != 0 && major != 1 && major != 2 && major != 4)
        {
            throw new ParseException("Unsupported skn version " + major + "." + (minorRaw & 0xFFFF), raf);
        }

        int             objectCount            = 0;
        int             flags                  = 0;
        int             indexCount;
        int             vertexCount;
        int             vertexSize             = 0;
        int             vertexType             = 0;
        Vector3f        boundingBoxMin         = null;
        Vector3f        boundingBoxMax         = null;
        Vector3f        boundingSphereLocation = null;
        float           boundingSphereRadius   = 0;
        List<RangeInfo> ranges;

        if (major == 0)
        {
            indexCount = raf.readInt();
            vertexCount = raf.readInt();
            ranges = new ArrayList<>(List.of(new RangeInfo("Base", 0, vertexCount, 0, indexCount)));
        } else
        {
            objectCount = raf.readInt();
            ranges = parseRanges(raf, objectCount);

            if (major == 4)
            {
                flags = raf.readInt();
            }

            indexCount = raf.readInt();
            vertexCount = raf.readInt();

            if (major == 4)
            {
                vertexSize = raf.readInt();
                vertexType = raf.readInt();
                boundingBoxMin = raf.readVec3F();
                boundingBoxMax = raf.readVec3F();
                boundingSphereLocation = raf.readVec3F();
                boundingSphereRadius = raf.readFloat();
            }
        }

        if (vertexSize == 0)
        {
            vertexSize = BASIC_VERTEX_SIZE;
        }

        if (indexCount < 0 || vertexCount < 0 || (long) indexCount * 2 > raf.remaining())
        {
            throw new ParseException("Invalid skn index/vertex count", raf);
        }

        int[]         indices  = raf.readU16Array(indexCount);
        List<SKNData> vertices = parseVertices(raf, vertexSize, vertexType, vertexCount);

        List<SKNMaterial> materials = new ArrayList<>(ranges.size());
        for (RangeInfo range : ranges)
        {
            int startVertex = range.startVertex();
            int numVertex   = range.numVertex();
            int startIndex  = range.startIndex();
            int numIndex    = range.numIndex();

            if (startVertex < 0 || numVertex < 0 || (long) startVertex + numVertex > vertices.size() ||
                startIndex < 0 || numIndex < 0 || (long) startIndex + numIndex > indices.length)
            {
                throw new ParseException("skn range \"" + range.name() + "\" is out of bounds", raf);
            }

            // indices are global into the shared vertex buffer, make them local to the range
            List<Integer> inds = new ArrayList<>(numIndex);
            for (int i = startIndex; i < startIndex + numIndex; i++)
            {
                int local = indices[i] - startVertex;
                if (local < 0 || local >= numVertex)
                {
                    throw new ParseException("skn index " + indices[i] + " is outside of range \"" + range.name() + "\"", raf);
                }
                inds.add(local);
            }

            materials.add(new SKNMaterial(range.name(), startVertex, numVertex, startIndex, numIndex, inds, vertices.subList(startVertex, startVertex + numVertex)));
        }

        return new SKNFile(magic, majorRaw, minorRaw, objectCount, indexCount, vertexCount, vertexSize, flags, vertexType,
                           boundingBoxMin, boundingBoxMax, boundingSphereLocation, boundingSphereRadius, indices, vertices, materials);
    }

    /**
     * A submesh range as stored in the file, before its vertices and indices are known
     */
    private record RangeInfo(String name, int startVertex, int numVertex, int startIndex, int numIndex)
    {
    }

    private List<RangeInfo> parseRanges(RandomAccessReader raf, int count)
    {
        if (count < 0 || (long) count * 80 > raf.remaining())
        {
            throw new ParseException("Invalid skn range count " + count, raf);
        }

        List<RangeInfo> ranges = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
        {
            String name        = raf.readFixedString(64);
            int    startVertex = raf.readInt();
            int    numVertex   = raf.readInt();
            int    startIndex  = raf.readInt();
            int    numIndex    = raf.readInt();
            ranges.add(new RangeInfo(name, startVertex, numVertex, startIndex, numIndex));
        }
        return ranges;
    }

    private List<SKNData> parseVertices(RandomAccessReader raf, int stride, int type, int vertexCount)
    {

        int required = type == 2 ? TANGENT_VERTEX_SIZE : (type == 1 ? COLOR_VERTEX_SIZE : BASIC_VERTEX_SIZE);
        if (stride < required)
        {
            throw new ParseException("skn vertex size " + stride + " is too small for vertex type " + type, raf);
        }
        if ((long) stride * vertexCount > raf.remaining())
        {
            throw new ParseException("skn vertex buffer is truncated", raf);
        }

        List<SKNData> datas = new ArrayList<>(vertexCount);
        long          start = raf.pos();
        for (int i = 0; i < vertexCount; i++)
        {
            raf.seek(start + (long) i * stride);

            Vector3f position     = raf.readVec3F();
            Vector4b boneIndecies = raf.readVec4B();
            Vector4f weight       = raf.readVec4F();
            Vector3f normals      = raf.readVec3F();
            Vector2f uv           = raf.readVec2F();

            // unknown (newer) vertex types are read as far as the known elements go, the rest of the stride is skipped
            Vector4b color   = null;
            Vector4f tangent = null;
            if (type == 1 || type == 2)
            {
                // BGRA
                color = raf.readVec4B();
            }
            if (type == 2)
            {
                tangent = raf.readVec4F();
            }

            datas.add(new SKNData(position, boneIndecies, weight, normals, uv, tangent, color));
        }
        raf.seek(start + (long) stride * vertexCount);
        return datas;
    }
}
