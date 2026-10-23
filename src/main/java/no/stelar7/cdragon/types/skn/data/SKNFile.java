package no.stelar7.cdragon.types.skn.data;

import no.stelar7.cdragon.util.handlers.ModelHandler;
import no.stelar7.cdragon.util.types.math.*;

import java.text.*;
import java.util.*;

public final class SKNFile
{
    private final int   magic;
    private final short major;
    private final short minor;
    private final int   objectCount;
    private final int   indexCount;
    private final int   vertexCount;
    private final int   materialCount;
    private final int   vertexSize;
    
    private final int    unknown;
    private final byte[] unknown2;
    
    private final int vertexType;
    
    private final Vector3f boundingBoxMin;
    private final Vector3f boundingBoxMax;
    private final Vector3f boundingSphereLocation;
    private final float    boundingSphereRadius;
    
    private final int[]             indecies;
    private final List<SKNData>     vertices;
    private final List<SKNMaterial> materials;
    
    public SKNFile(int magic, short major, short minor, int objectCount, int indexCount, int vertexCount, int vertexSize, int unknown, int vertexType, Vector3f boundingBoxMin, Vector3f boundingBoxMax, Vector3f boundingSphereLocation, float boundingSphereRadius, int[] indecies, List<SKNData> vertices, List<SKNMaterial> materials)
    {
        this.magic = magic;
        this.major = major;
        this.minor = minor;
        this.objectCount = objectCount;
        this.indexCount = indexCount;
        this.vertexCount = vertexCount;
        // never read from the file
        this.materialCount = 0;
        this.vertexSize = vertexSize;
        this.unknown = unknown;
        this.unknown2 = null;
        this.vertexType = vertexType;
        this.boundingBoxMin = boundingBoxMin;
        this.boundingBoxMax = boundingBoxMax;
        this.boundingSphereLocation = boundingSphereLocation;
        this.boundingSphereRadius = boundingSphereRadius;
        this.indecies = indecies;
        this.vertices = vertices;
        this.materials = materials;
    }
    
    public int getVertexSize()
    {
        return vertexSize;
    }
    
    public int getMagic()
    {
        return magic;
    }
    
    public short getMajor()
    {
        return major;
    }
    
    public short getMinor()
    {
        return minor;
    }
    
    public int getObjectCount()
    {
        return objectCount;
    }
    
    public int getIndexCount()
    {
        return indexCount;
    }
    
    public int getVertexCount()
    {
        return vertexCount;
    }
    
    public int getMaterialCount()
    {
        return materialCount;
    }
    
    public int getUnknown()
    {
        return unknown;
    }
    
    public byte[] getUnknown2()
    {
        return unknown2;
    }
    
    public Vector3f getBoundingBoxMin()
    {
        return boundingBoxMin;
    }
    
    public Vector3f getBoundingBoxMax()
    {
        return boundingBoxMax;
    }
    
    public Vector3f getBoundingSphereLocation()
    {
        return boundingSphereLocation;
    }
    
    public float getBoundingSphereRadius()
    {
        return boundingSphereRadius;
    }
    
    public int getVertexType()
    {
        return vertexType;
    }
    
    public int[] getIndecies()
    {
        return indecies;
    }
    
    public List<SKNData> getVertices()
    {
        return vertices;
    }
    
    public List<Vector3f> getVertexPositions()
    {
        List<Vector3f> pos = new ArrayList<>();
        for (SKNData vertex : vertices)
        {
            pos.add(vertex.getPosition());
        }
        return pos;
    }
    
    public List<SKNMaterial> getMaterials()
    {
        return materials;
    }
    
    public List<SKNData> getDataForSubmesh(SKNMaterial submesh)
    {
        if (submesh.getNumVertex() == submesh.getVertices().size())
        {
            return submesh.getVertices();
        }
        
        return new ArrayList<>(vertices.subList(submesh.getStartVertex(), submesh.getStartVertex() + submesh.getNumVertex()));
    }
    
    public String toOBJ(SKNMaterial submesh)
    {
        StringBuilder        sb           = new StringBuilder();
        List<Vector3f>       scaledVerts  = ModelHandler.getScaledVertices(submesh.getVertexPositions());
        List<Integer>        indeciesList = submesh.getIndecies();
        DecimalFormatSymbols dfs          = new DecimalFormatSymbols(Locale.ENGLISH);
        DecimalFormat        df           = new DecimalFormat("###.#######", dfs);
        
        for (Vector3f pos : scaledVerts)
        {
            sb.append(String.format("v %s %s %s%n", df.format(pos.x), df.format(pos.y), df.format(pos.z)));
        }
        
        for (SKNData vertex : submesh.getVertices())
        {
            Vector2f pos = vertex.getUv();
            sb.append(String.format("vt %s %s%n", df.format(pos.x), df.format(pos.y)));
        }
        
        for (SKNData vertex : submesh.getVertices())
        {
            Vector3f pos = vertex.getNormals();
            sb.append(String.format("vn %s %s %s%n", df.format(pos.x), df.format(pos.y), df.format(pos.z)));
        }
        
        for (int i = 0; i < indeciesList.size(); i += 3)
        {
            int a = indeciesList.get(i) + 1;
            int b = indeciesList.get(i + 1) + 1;
            int c = indeciesList.get(i + 2) + 1;
            
            sb.append(String.format("f %1$s/%1$s/%1$s %2$s/%2$s/%2$s %3$s/%3$s/%3$s%n", a, b, c));
        }
        
        return sb.toString();
    }
}
