package no.stelar7.cdragon.types.mgeo;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.mgeo.data.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import no.stelar7.cdragon.util.types.*;
import no.stelar7.cdragon.util.types.math.*;

import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.*;

public class MGEOParser implements Parseable<MGEOFile>
{
    @Override
    public MGEOFile parse(Path path)
    {
        return parse(new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public MGEOFile parse(ByteArray data)
    {
        return parse(new RandomAccessReader(data.getData(), ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public MGEOFile parse(RandomAccessReader raf)
    {
        MGEOFile file = new MGEOFile();
        file.setHeader(parseHeader(raf));
        
        if (!file.getHeader().getMagic().equals("OEGM"))
        {
            throw new RuntimeException("Invalid magic number! " + file.getHeader().getMagic());
        }
        
        file.setMeshes(parseMeshes(raf, file.getHeader()));
        file.setBucketGeometry(parseGeometry(raf));
        
        return file;
    }
    
    private MGEOBucketGeometry parseGeometry(RandomAccessReader raf)
    {
        MGEOBucketGeometry geo = new MGEOBucketGeometry();
        geo.setMinx(raf.readFloat());
        geo.setMinz(raf.readFloat());
        geo.setMaxx(raf.readFloat());
        geo.setMaxz(raf.readFloat());
        geo.setCenterx(raf.readFloat());
        geo.setCenterz(raf.readFloat());
        geo.setMiny(raf.readFloat());
        geo.setMaxy(raf.readFloat());
        
        int bucketsPerSide = raf.readInt();
        int vertexCount    = raf.readInt();
        int indexCount     = raf.readInt();
        
        List<Vector3f> vertices = new ArrayList<>();
        for (int i = 0; i < vertexCount; i++)
        {
            vertices.add(raf.readVec3F());
        }
        geo.setVertices(vertices);
        
        List<Short> indecies = new ArrayList<>();
        for (int i = 0; i < indexCount; i++)
        {
            indecies.add(raf.readShort());
        }
        geo.setIndices(indecies);
        
        MGEOBucketGeometryBucket[][] buckets = new MGEOBucketGeometryBucket[bucketsPerSide][bucketsPerSide];
        for (int i = 0; i < bucketsPerSide; i++)
        {
            for (int j = 0; j < bucketsPerSide; j++)
            {
                buckets[i][j] = new MGEOBucketGeometryBucket();
                buckets[i][j].setMaxStickOutX(raf.readFloat());
                buckets[i][j].setMaxStickOutY(raf.readFloat());
                buckets[i][j].setStartIndex(raf.readInt());
                buckets[i][j].setVertex(raf.readInt());
                buckets[i][j].setInsideFaceCount(raf.readShort());
                buckets[i][j].setStickingOutFaceCount(raf.readShort());
            }
        }
        geo.setBuckets(buckets);
        return geo;
    }
    
    private List<MGEOFileMesh> parseMeshes(RandomAccessReader raf, MGEOFileHeader header)
    {
        boolean isUnknownSet = false;
        if (header.getVersion() != 7)
        {
            int unknownHeaderBytes = raf.readByte();
            isUnknownSet = unknownHeaderBytes == 1;
        }
        
        List<Integer> unknownList  = new ArrayList<>();
        int           unknownCount = raf.readInt();
        for (int i = 0; i < unknownCount; i++)
        {
            for (int j = 0; j < 32; j++)
            {
                unknownList.add(raf.readInt());
            }
        }
        
        
        List<Integer> vertexBufferOffsets = new ArrayList<>();
        int           vertexBufferCount   = raf.readInt();
        for (int i = 0; i < vertexBufferCount; i++)
        {
            int size = raf.readInt();
            vertexBufferOffsets.add(raf.pos() - 4);
            raf.seek(raf.pos() + size);
        }
        
        List<Integer> indexBufferOffsets = new ArrayList<>();
        int           indexBufferCount   = raf.readInt();
        for (int i = 0; i < indexBufferCount; i++)
        {
            int size = raf.readInt();
            indexBufferOffsets.add(raf.pos() - 4);
            raf.seek(raf.pos() + size);
        }
        
        int                meshCount = raf.readInt();
        List<MGEOFileMesh> meshes    = new ArrayList<>();
        for (int i = 0; i < meshCount; i++)
        {
            meshes.add(parseMesh(raf, header.getVersion(), vertexBufferOffsets, indexBufferOffsets, isUnknownSet));
        }
        
        return meshes;
    }
    
    private MGEOFileMesh parseMesh(RandomAccessReader raf, int version, List<Integer> vertexBufferOffsets, List<Integer> indexBufferOffsets, boolean unknown)
    {
        MGEOFileMesh mesh = new MGEOFileMesh();
        mesh.setName(raf.readString(raf.readInt()));
        int vertexCount       = raf.readInt();
        int vertexBufferCount = raf.readInt();
        mesh.setType(MGEOFileMeshType.getFromValue(raf.readInt()));
        
        List<MGEOFileMeshVertex> vertices = new ArrayList<>();
        List<Short>              indices  = new ArrayList<>();
        
        for (int i = 0; i < vertexBufferCount; i++)
        {
            if (i == 0)
            {
                int originalPos = raf.pos() + 4;
                raf.seek(vertexBufferOffsets.get(raf.readInt()));
                
                int vertexCountInBuffer = raf.readInt() / 32;
                for (int j = 0; j < vertexCount; j++)
                {
                    
                    MGEOFileMeshVertex vertexData = new MGEOFileMeshVertex();
                    vertexData.setPosition(raf.readVec3F());
                    vertexData.setNormal(raf.readVec3F());
                    vertices.add(vertexData);
                }
                
                raf.seek(originalPos);
            } else
            {
                int originalPos = raf.pos() + 4;
                raf.seek(vertexBufferOffsets.get(raf.readInt()));
                
                int vertexCountInBuffer = raf.readInt() / 16;
                for (int j = 0; j < vertexCount; j++)
                {
                    vertices.get(j).setUv1(raf.readVec2F());
                    vertices.get(j).setUv2(raf.readVec2F());
                }
                
                raf.seek(originalPos);
            }
        }
        mesh.setVertices(vertices);
        
        int indexCount  = raf.readInt();
        int indexBuffer = raf.readInt();
        
        int originalPos = raf.pos();
        raf.seek(indexBufferOffsets.get(indexBuffer));
        int indexCountInBuffer = raf.readInt();
        for (int i = 0; i < indexCount; i++)
        {
            indices.add(raf.readShort());
        }
        raf.seek(originalPos);
        mesh.setIndices(indices);
        
        int                       submeshCount = raf.readInt();
        List<MGeoFileMeshSubMesh> subMeshes    = new ArrayList<>();
        for (int i = 0; i < submeshCount; i++)
        {
            subMeshes.add(parseSubmesh(raf));
        }
        mesh.setSubMeshes(subMeshes);
        if (version != 5)
        {
            raf.readByte();
        }
        mesh.setBoundingBox(raf.readBoundingBox());
        mesh.setTransformationMatrix(raf.readMatrix4x4());
        byte unknownA = raf.readByte();
        
        if (version == 7)
        {
            byte unknownB = raf.readByte();
        }
        
        if (unknown)
        {
            mesh.setUnknown(raf.readVec3F());
        }
        
        Matrix4f[] unknown2 = new Matrix4f[3];
        for (int i = 0; i < 3; i++)
        {
            unknown2[i] = raf.readMatrix3x3();
        }
        mesh.setUnknown2(unknown2);
        mesh.setLightMap(raf.readString(raf.readInt()));
        mesh.setUnknown3(raf.readVec4F());
        return mesh;
    }
    
    private MGeoFileMeshSubMesh parseSubmesh(RandomAccessReader raf)
    {
        MGeoFileMeshSubMesh subMesh = new MGeoFileMeshSubMesh();
        subMesh.setUnknown(raf.readInt());
        subMesh.setName(raf.readString(raf.readInt()));
        subMesh.setStartIndex(raf.readInt());
        subMesh.setIndexCount(raf.readInt());
        subMesh.setStartVertex(raf.readInt());
        subMesh.setVertexCount(raf.readInt());
        return subMesh;
    }
    
    private MGEOFileHeader parseHeader(RandomAccessReader raf)
    {
        MGEOFileHeader header = new MGEOFileHeader();
        header.setMagic(raf.readString(4));
        header.setVersion(raf.readInt());
        return header;
    }
}
