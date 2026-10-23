package no.stelar7.cdragon.types.mgeo;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.mgeo.data.*;
import no.stelar7.cdragon.util.readers.*;
import no.stelar7.cdragon.util.types.math.*;

import java.util.*;

/**
 * Map geometry (.mapgeo) reader.
 * <p>
 * Layout reference: LeagueToolkit Core/Environment/EnvironmentAsset.cs, EnvironmentAssetMesh.cs,
 * EnvironmentAssetMeshPrimitive.cs, EnvironmentAssetChannel.cs, Core/Memory/VertexBufferDescription.cs (ReadFromMapGeometry),
 * Core/SceneGraph/BucketedGeometry.cs, GeometryBucket.cs and PlanarReflector.cs.
 * <p>
 * Supported versions: 5 to 20.
 * <p>
 * LeagueToolkit (C# and the ltk_mapgeo crate) only accepts 5, 6, 7, 9, 11, 12, 13, 14, 15, 17, 18, but its gates already
 * describe the others, and these readers accept them with the same gates:
 * <ul>
 *     <li>16: v15 layout with u16 render flags (LeagueToolkit's {@code version < 16} branch, ltk_mapgeo {@code has_u16_render_flags});
 *     read by RitoShark/Aventurine-League-Tools io/import_mapgeo.py, TheKillerey/MapgeoAddon mapgeo_parser.py and TheKillerey/ReyEngine MapGeoBinary.cs</li>
 *     <li>8 and 10: no gate falls on them, so they read like 7 and 9; accepted only by TheKillerey/MapgeoAddon and TheKillerey/ReyEngine
 *     (no reference has version specific code for them, so this layout is inferred)</li>
 *     <li>19 and 20: never shipped; layout from the notes in league-toolkit crates/ltk_mapgeo/src/read/version.rs (reversed from the live client,
 *     which accepts up to 20). 19 adds one byte per mesh after the baked paint scale/bias, 20 adds a u32 length prefixed
 *     MapGeoExtension object after each mesh record. ltk_mapgeo does not read either, so the extension is kept as raw bytes</li>
 * </ul>
 * <p>
 * Version 18 bucketed geometry starts with the region path hash and then the visibility controller hash, as in ltk_mapgeo
 * read/scene_graph.rs (also the order of the same two hashes on v18 meshes). LeagueToolkit C# reads the second value as an unused float,
 * and TheKillerey/MapgeoAddon reads the two hashes in the opposite order.
 */
public class MGEOParser implements Parseable<MGEOFile>
{
    public static final String MAGIC = "OEGM";

    private static final Set<Integer> SUPPORTED_VERSIONS = Set.of(5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20);

    private static final int MAX_VERTEX_ELEMENTS = 15;

    private record BufferRange(long offset, long size) {}

    @Override
    public MGEOFile parse(RandomAccessReader raf)
    {
        MGEOFile file = new MGEOFile();
        file.setHeader(parseHeader(raf));

        if (!MAGIC.equals(file.getHeader().getMagic()))
        {
            throw new ParseException("Invalid mapgeo magic: " + file.getHeader().getMagic(), 0);
        }

        int version = file.getHeader().getVersion();
        if (!SUPPORTED_VERSIONS.contains(version))
        {
            throw new ParseException("Unsupported mapgeo version " + version, raf);
        }

        file.setUseSeparatePointLights(version < 7 && raf.readBoolean());

        parseSamplerDefs(raf, file, version);

        int declarationCount = readCount(raf, 128, "vertex declaration");
        for (int i = 0; i < declarationCount; i++)
        {
            file.getVertexDeclarations().add(parseVertexDeclaration(raf));
        }

        List<BufferRange> vertexBuffers = parseBufferRanges(raf, version, "vertex buffer");
        List<BufferRange> indexBuffers  = parseBufferRanges(raf, version, "index buffer");

        int meshCount = readCount(raf, 1, "mesh");
        for (int i = 0; i < meshCount; i++)
        {
            file.getMeshes().add(parseMesh(raf, file, i, version, vertexBuffers, indexBuffers));
        }

        if (version >= 15)
        {
            int count = readCount(raf, 1, "bucketed geometry");
            for (int i = 0; i < count; i++)
            {
                file.getBucketGeometries().add(parseGeometry(raf, version));
            }
        } else
        {
            file.getBucketGeometries().add(parseGeometry(raf, version));
        }
        file.setBucketGeometry(file.getBucketGeometries().isEmpty() ? null : file.getBucketGeometries().get(0));

        if (version >= 13)
        {
            int count = readCount(raf, 100, "planar reflector");
            for (int i = 0; i < count; i++)
            {
                MGEOPlanarReflector reflector = new MGEOPlanarReflector();
                reflector.setTransform(raf.readMatrix4x4());
                reflector.setPlane(raf.readBoundingBox());
                reflector.setNormal(raf.readVec3F());
                file.getPlanarReflectors().add(reflector);
            }
        }

        return file;
    }

    private void parseSamplerDefs(RandomAccessReader raf, MGEOFile file, int version)
    {
        if (version >= 17)
        {
            int count = readCount(raf, 8, "sampler definition");
            for (int i = 0; i < count; i++)
            {
                int index = raf.readInt();
                file.getShaderTextureOverrides().add(new MGEOTextureOverride(index, readSizedString(raf)));
            }
            return;
        }

        if (version >= 9)
        {
            file.getShaderTextureOverrides().add(new MGEOTextureOverride(0, readSizedString(raf)));
        }

        if (version >= 11)
        {
            file.getShaderTextureOverrides().add(new MGEOTextureOverride(1, readSizedString(raf)));
        }
    }

    private MGEOVertexDeclaration parseVertexDeclaration(RandomAccessReader raf)
    {
        MGEOVertexDeclaration declaration = new MGEOVertexDeclaration();
        declaration.setUsage(raf.readInt());

        int elementCount = raf.readInt();
        if (elementCount < 0 || elementCount > MAX_VERTEX_ELEMENTS)
        {
            throw new ParseException("Invalid mapgeo vertex element count " + elementCount, raf);
        }

        for (int i = 0; i < elementCount; i++)
        {
            MGEOVertexElement element = new MGEOVertexElement(raf.readInt(), raf.readInt());
            if (element.getSize() < 0)
            {
                throw new ParseException("Unknown mapgeo vertex element format " + element.getFormat(), raf);
            }
            declaration.getElements().add(element);
        }

        // unused default elements
        raf.seekFromCurrentPosition(8L * (MAX_VERTEX_ELEMENTS - elementCount));
        return declaration;
    }

    private List<BufferRange> parseBufferRanges(RandomAccessReader raf, int version, String what)
    {
        int               count  = readCount(raf, 4, what);
        List<BufferRange> ranges = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
        {
            if (version >= 13)
            {
                // visibility flags of the buffer
                raf.readByte();
            }

            long size = raf.readU32();
            if (size > raf.remaining())
            {
                throw new ParseException("mapgeo " + what + " " + i + " is truncated", raf);
            }

            ranges.add(new BufferRange(raf.pos(), size));
            raf.seekFromCurrentPosition(size);
        }
        return ranges;
    }

    private MGEOFileMesh parseMesh(RandomAccessReader raf, MGEOFile file, int id, int version, List<BufferRange> vertexBuffers, List<BufferRange> indexBuffers)
    {
        MGEOFileMesh mesh = new MGEOFileMesh();
        mesh.setName(version <= 11 ? readSizedString(raf) : "MapGeo_Instance_" + id);

        int vertexCount      = raf.readInt();
        int bufferCount      = raf.readInt();
        int baseDeclaration  = raf.readInt();
        List<MGEOVertexDeclaration> declarations = file.getVertexDeclarations();

        if (vertexCount < 0 || vertexCount > raf.size() || bufferCount < 0 || baseDeclaration < 0 || (long) baseDeclaration + bufferCount > declarations.size())
        {
            throw new ParseException("Invalid mapgeo vertex declaration reference in mesh " + id, raf);
        }
        mesh.setBaseVertexDeclarationId(baseDeclaration);

        int[] bufferIds = raf.readIntArray(bufferCount);
        mesh.setVertexBufferIds(bufferIds);

        List<MGEOFileMeshVertex> vertices = new ArrayList<>(vertexCount);
        for (int i = 0; i < vertexCount; i++)
        {
            vertices.add(new MGEOFileMeshVertex());
        }

        long returnPosition = raf.pos();
        for (int i = 0; i < bufferCount; i++)
        {
            if (bufferIds[i] < 0 || bufferIds[i] >= vertexBuffers.size())
            {
                throw new ParseException("Invalid mapgeo vertex buffer id " + bufferIds[i] + " in mesh " + id, raf);
            }
            readVertexBuffer(raf, declarations.get(baseDeclaration + i), vertexBuffers.get(bufferIds[i]), vertices);
        }
        raf.seek(returnPosition);
        mesh.setVertices(vertices);

        long indexCount    = raf.readU32();
        int  indexBufferId = raf.readInt();
        if (indexBufferId < 0 || indexBufferId >= indexBuffers.size())
        {
            throw new ParseException("Invalid mapgeo index buffer id " + indexBufferId + " in mesh " + id, raf);
        }
        BufferRange indexBuffer = indexBuffers.get(indexBufferId);
        if (indexCount * 2 > indexBuffer.size())
        {
            throw new ParseException("mapgeo mesh " + id + " uses more indices than its index buffer contains", raf);
        }
        mesh.setIndexBufferId(indexBufferId);

        returnPosition = raf.pos();
        raf.seek(indexBuffer.offset());
        mesh.setIndices(raf.readU16Array((int) indexCount));
        raf.seek(returnPosition);

        if (version >= 13)
        {
            mesh.setVisibilityFlags(raf.readU8());
        }

        if (version >= 18)
        {
            mesh.setRegionHash(raf.readInt());
        }

        if (version >= 15)
        {
            mesh.setVisibilityControllerPathHash(raf.readInt());
        }

        int submeshCount = readCount(raf, 24, "submesh");
        for (int i = 0; i < submeshCount; i++)
        {
            mesh.getSubMeshes().add(parseSubmesh(raf));
        }

        if (version != 5)
        {
            mesh.setDisableBackfaceCulling(raf.readBoolean());
        }

        mesh.setBoundingBox(raf.readBoundingBox());
        mesh.setTransformationMatrix(raf.readMatrix4x4());
        mesh.setQualityFilter(raf.readU8());

        if (version >= 7 && version <= 12)
        {
            mesh.setVisibilityFlags(raf.readU8());
        }

        if (version >= 11 && version < 14)
        {
            mesh.setRenderFlags(raf.readU8());
        } else if (version >= 14)
        {
            mesh.setLayerTransitionBehavior(raf.readU8());
            mesh.setRenderFlags(version < 16 ? raf.readU8() : raf.readU16());
        }

        if (file.isUseSeparatePointLights() && version < 7)
        {
            mesh.setPointLight(raf.readVec3F());
        }

        if (version < 9)
        {
            Vector3f[] sh = new Vector3f[9];
            for (int i = 0; i < sh.length; i++)
            {
                sh[i] = raf.readVec3F();
            }
            mesh.setSphericalHarmonics(sh);
            mesh.setBakedLight(parseChannel(raf));
            return mesh;
        }

        mesh.setBakedLight(parseChannel(raf));
        mesh.setStationaryLight(parseChannel(raf));

        if (version >= 12 && version < 17)
        {
            MGEOChannel bakedPaint = parseChannel(raf);
            mesh.getTextureOverrides().add(new MGEOTextureOverride(0, bakedPaint.getTexture()));
            mesh.setBakedPaintScale(bakedPaint.getScale());
            mesh.setBakedPaintBias(bakedPaint.getBias());
        }

        if (version >= 17)
        {
            int count = readCount(raf, 8, "texture override");
            for (int i = 0; i < count; i++)
            {
                int index = raf.readInt();
                mesh.getTextureOverrides().add(new MGEOTextureOverride(index, readSizedString(raf)));
            }

            mesh.setBakedPaintScale(raf.readVec2F());
            mesh.setBakedPaintBias(raf.readVec2F());
        }

        if (version >= 19)
        {
            mesh.setVersion19Byte(raf.readU8());
        }

        if (version >= 20)
        {
            long length = raf.readU32();
            if (length > raf.remaining())
            {
                throw new ParseException("mapgeo mesh " + id + " extension object is truncated", raf);
            }
            mesh.setExtensionData(raf.readBytes((int) length));
        }

        return mesh;
    }

    /**
     * Reads every element of the buffer's declaration into the (shared) vertex objects
     */
    private void readVertexBuffer(RandomAccessReader raf, MGEOVertexDeclaration declaration, BufferRange buffer, List<MGEOFileMeshVertex> vertices)
    {
        int  stride   = declaration.getStride();
        long required = (long) stride * vertices.size();
        if (required > buffer.size())
        {
            throw new ParseException("mapgeo vertex buffer is smaller than " + vertices.size() + " vertices of " + stride + " bytes", buffer.offset());
        }

        List<MGEOVertexElement> elements = declaration.getElements();
        raf.seek(buffer.offset());
        for (MGEOFileMeshVertex vertex : vertices)
        {
            for (MGEOVertexElement element : elements)
            {
                readElement(raf, element, vertex);
            }
        }
    }

    private void readElement(RandomAccessReader raf, MGEOVertexElement element, MGEOFileMeshVertex vertex)
    {
        int name   = element.getName();
        int format = element.getFormat();

        if (format == MGEOVertexElement.FORMAT_XYZ_FLOAT32 && (name == MGEOVertexElement.POSITION || name == MGEOVertexElement.NORMAL))
        {
            Vector3f value = raf.readVec3F();
            if (name == MGEOVertexElement.POSITION)
            {
                vertex.setPosition(value);
            } else
            {
                vertex.setNormal(value);
            }
            return;
        }

        if (format == MGEOVertexElement.FORMAT_XY_FLOAT32 && (name == MGEOVertexElement.TEXCOORD_0 || name == MGEOVertexElement.TEXCOORD_1))
        {
            Vector2f value = raf.readVec2F();
            if (name == MGEOVertexElement.TEXCOORD_0)
            {
                vertex.setUv1(value);
            } else
            {
                vertex.setUv2(value);
            }
            return;
        }

        if (element.getSize() == 4 && format != MGEOVertexElement.FORMAT_X_FLOAT32 && format != MGEOVertexElement.FORMAT_XY_PACKED1616 &&
            (name == MGEOVertexElement.PRIMARY_COLOR || name == MGEOVertexElement.SECONDARY_COLOR))
        {
            // bytes in stored order (BGRA for the usual BGRA_Packed8888 format)
            Vector4b value = raf.readVec4B();
            if (name == MGEOVertexElement.PRIMARY_COLOR)
            {
                vertex.setPrimaryColor(value);
            } else
            {
                vertex.setSecondaryColor(value);
            }
            return;
        }

        if (format == MGEOVertexElement.FORMAT_XYZW_FLOAT32 && name == MGEOVertexElement.TANGENT)
        {
            vertex.setTangent(raf.readVec4F());
            return;
        }

        if (vertex.getExtra() == null)
        {
            vertex.setExtra(new LinkedHashMap<>());
        }
        vertex.getExtra().put(element.getNameString(), readGeneric(raf, format));
    }

    /**
     * Float formats are returned as is, 16 bit formats are decoded as half floats, 8 bit formats as their unsigned component values.
     * <p>
     * Formats 7-9 are half floats: LeagueToolkit C# reads 7 as (Half, Half) and 8 as (Half, Half, Half) in an 8 byte element,
     * ltk_mesh (mem/vertex_element.rs, mirroring the game's ElemFormat) calls them XY_Float16 and XYZW_Float16 (8 bytes), and
     * RitoShark/Aventurine-League-Tools reads 8 as 4 half floats ("w as padding"). All agree format 8 is 8 bytes, so all 4 halves are returned.
     */
    private float[] readGeneric(RandomAccessReader raf, int format)
    {
        return switch (format)
        {
            case MGEOVertexElement.FORMAT_X_FLOAT32 -> raf.readFloatArray(1);
            case MGEOVertexElement.FORMAT_XY_FLOAT32 -> raf.readFloatArray(2);
            case MGEOVertexElement.FORMAT_XYZ_FLOAT32 -> raf.readFloatArray(3);
            case MGEOVertexElement.FORMAT_XYZW_FLOAT32 -> raf.readFloatArray(4);
            case MGEOVertexElement.FORMAT_XY_PACKED1616 -> halfFloats(raf, 2);
            case MGEOVertexElement.FORMAT_XYZ_PACKED161616, MGEOVertexElement.FORMAT_XYZW_PACKED16161616 -> halfFloats(raf, 4);
            case MGEOVertexElement.FORMAT_XY_PACKED88 -> unsignedBytes(raf, 2);
            case MGEOVertexElement.FORMAT_XYZ_PACKED888 -> unsignedBytes(raf, 3);
            default -> unsignedBytes(raf, 4);
        };
    }

    private float[] halfFloats(RandomAccessReader raf, int count)
    {
        float[] values = new float[count];
        for (int i = 0; i < count; i++)
        {
            values[i] = Float.float16ToFloat(raf.readShort());
        }
        return values;
    }

    private float[] unsignedBytes(RandomAccessReader raf, int count)
    {
        float[] values = new float[count];
        for (int i = 0; i < count; i++)
        {
            values[i] = raf.readU8();
        }
        return values;
    }

    private MGEOChannel parseChannel(RandomAccessReader raf)
    {
        MGEOChannel channel = new MGEOChannel();
        channel.setTexture(readSizedString(raf));
        channel.setScale(raf.readVec2F());
        channel.setBias(raf.readVec2F());
        return channel;
    }

    private MGeoFileMeshSubMesh parseSubmesh(RandomAccessReader raf)
    {
        MGeoFileMeshSubMesh subMesh = new MGeoFileMeshSubMesh();
        // hash
        subMesh.setUnknown(raf.readInt());
        // material
        subMesh.setName(readSizedString(raf));
        subMesh.setStartIndex(raf.readInt());
        subMesh.setIndexCount(raf.readInt());
        // min vertex
        subMesh.setStartVertex(raf.readInt());
        // max vertex
        subMesh.setVertexCount(raf.readInt());
        return subMesh;
    }

    private MGEOBucketGeometry parseGeometry(RandomAccessReader raf, int version)
    {
        MGEOBucketGeometry geo = new MGEOBucketGeometry();

        if (version >= 18)
        {
            geo.setRegionPathHash(raf.readInt());
        }
        if (version >= 15)
        {
            geo.setVisibilityControllerPathHash(raf.readInt());
        }

        geo.setMinx(raf.readFloat());
        geo.setMinz(raf.readFloat());
        geo.setMaxx(raf.readFloat());
        geo.setMaxz(raf.readFloat());
        // max stick out x/z
        geo.setCenterx(raf.readFloat());
        geo.setCenterz(raf.readFloat());
        // bucket size x/z
        geo.setMiny(raf.readFloat());
        geo.setMaxy(raf.readFloat());

        int bucketsPerSide = raf.readU16();
        geo.setBucketsPerSide(bucketsPerSide);
        geo.setDisabled(raf.readBoolean());
        geo.setFlags(raf.readU8());

        long vertexCount = raf.readU32();
        long indexCount  = raf.readU32();

        if (geo.isDisabled())
        {
            return geo;
        }

        long required = vertexCount * 12 + indexCount * 2 + (long) bucketsPerSide * bucketsPerSide * 20;
        if (required > raf.remaining())
        {
            throw new ParseException("mapgeo bucketed geometry is truncated", raf);
        }

        float[]        positions = raf.readFloatArray((int) vertexCount * 3);
        List<Vector3f> vertices  = new ArrayList<>((int) vertexCount);
        for (int i = 0; i < vertexCount; i++)
        {
            vertices.add(new Vector3f(positions[i * 3], positions[i * 3 + 1], positions[i * 3 + 2]));
        }
        geo.setVertices(vertices);
        geo.setIndices(raf.readU16Array((int) indexCount));

        MGEOBucketGeometryBucket[][] buckets = new MGEOBucketGeometryBucket[bucketsPerSide][bucketsPerSide];
        for (int i = 0; i < bucketsPerSide; i++)
        {
            for (int j = 0; j < bucketsPerSide; j++)
            {
                MGEOBucketGeometryBucket bucket = new MGEOBucketGeometryBucket();
                bucket.setMaxStickOutX(raf.readFloat());
                bucket.setMaxStickOutY(raf.readFloat());
                bucket.setStartIndex(raf.readInt());
                bucket.setVertex(raf.readInt());
                bucket.setInsideFaceCount(raf.readU16());
                bucket.setStickingOutFaceCount(raf.readU16());
                buckets[i][j] = bucket;
            }
        }
        geo.setBuckets(buckets);

        if ((geo.getFlags() & 1) != 0)
        {
            geo.setFaceVisibilityFlags(raf.readBytes(indexCount / 3));
        }

        return geo;
    }

    private int readCount(RandomAccessReader raf, int minElementSize, String what)
    {
        long count = raf.readU32();
        if (count * minElementSize > raf.remaining())
        {
            throw new ParseException("Invalid mapgeo " + what + " count " + count, raf);
        }
        return (int) count;
    }

    private String readSizedString(RandomAccessReader raf)
    {
        int length = raf.readInt();
        if (length < 0 || length > raf.remaining())
        {
            throw new ParseException("Invalid mapgeo string length " + length, raf);
        }
        return raf.readString(length);
    }

    private MGEOFileHeader parseHeader(RandomAccessReader raf)
    {
        MGEOFileHeader header = new MGEOFileHeader();
        header.setMagic(raf.readString(4));
        header.setVersion(raf.readInt());
        return header;
    }
}
