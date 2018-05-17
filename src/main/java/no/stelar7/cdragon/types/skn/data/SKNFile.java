package no.stelar7.cdragon.types.skn.data;

import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.types.*;

import java.text.*;
import java.util.*;

public class SKNFile
{
    private int   magic;
    private short major;
    private short minor;
    private int   objectCount;
    private int   indexCount;
    private int   vertexCount;
    private int   materialCount;
    private int   vertexSize;
    
    private int    unknown;
    private byte[] unknown2;
    
    private int      containsTangent;
    private Vector3f boundingBoxMin;
    private Vector3f boundingBoxMax;
    private Vector3f boundingSphereLocation;
    private float    boundingSphereRadius;
    
    private List<Short>       indecies;
    private List<SKNData>     vertices;
    private List<SKNMaterial> materials;
    
    public int getVertexSize()
    {
        return vertexSize;
    }
    
    public void setVertexSize(int vertexSize)
    {
        this.vertexSize = vertexSize;
    }
    
    public int getMagic()
    {
        return magic;
    }
    
    public void setMagic(int magic)
    {
        this.magic = magic;
    }
    
    public short getMajor()
    {
        return major;
    }
    
    public void setMajor(short major)
    {
        this.major = major;
    }
    
    public short getMinor()
    {
        return minor;
    }
    
    public void setMinor(short minor)
    {
        this.minor = minor;
    }
    
    public int getObjectCount()
    {
        return objectCount;
    }
    
    public void setObjectCount(int objectCount)
    {
        this.objectCount = objectCount;
    }
    
    public int getIndexCount()
    {
        return indexCount;
    }
    
    public void setIndexCount(int indexCount)
    {
        this.indexCount = indexCount;
    }
    
    public int getVertexCount()
    {
        return vertexCount;
    }
    
    public void setVertexCount(int vertexCount)
    {
        this.vertexCount = vertexCount;
    }
    
    public int getMaterialCount()
    {
        return materialCount;
    }
    
    public void setMaterialCount(int materialCount)
    {
        this.materialCount = materialCount;
    }
    
    public int getUnknown()
    {
        return unknown;
    }
    
    public void setUnknown(int unknown)
    {
        this.unknown = unknown;
    }
    
    public byte[] getUnknown2()
    {
        return unknown2;
    }
    
    public void setUnknown2(byte[] unknown2)
    {
        this.unknown2 = unknown2;
    }
    
    public boolean containsTangent()
    {
        return containsTangent == 1;
    }
    
    public void setContainsTangent(int containsTangent)
    {
        this.containsTangent = containsTangent;
    }
    
    public Vector3f getBoundingBoxMin()
    {
        return boundingBoxMin;
    }
    
    public void setBoundingBoxMin(Vector3f boundingBoxMin)
    {
        this.boundingBoxMin = boundingBoxMin;
    }
    
    public Vector3f getBoundingBoxMax()
    {
        return boundingBoxMax;
    }
    
    public void setBoundingBoxMax(Vector3f boundingBoxMax)
    {
        this.boundingBoxMax = boundingBoxMax;
    }
    
    public Vector3f getBoundingSphereLocation()
    {
        return boundingSphereLocation;
    }
    
    public void setBoundingSphereLocation(Vector3f boundingSphereLocation)
    {
        this.boundingSphereLocation = boundingSphereLocation;
    }
    
    public float getBoundingSphereRadius()
    {
        return boundingSphereRadius;
    }
    
    public void setBoundingSphereRadius(float boundingSphereRadius)
    {
        this.boundingSphereRadius = boundingSphereRadius;
    }
    
    public List<Short> getIndecies()
    {
        return indecies;
    }
    
    public void setIndecies(List<Short> indecies)
    {
        this.indecies = indecies;
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
    
    public void setVertices(List<SKNData> vertices)
    {
        this.vertices = vertices;
    }
    
    public List<SKNMaterial> getMaterials()
    {
        return materials;
    }
    
    public void setMaterials(List<SKNMaterial> materials)
    {
        this.materials = materials;
    }
    
    public String toOBJ(SKNMaterial submesh)
    {
        StringBuilder        sb           = new StringBuilder();
        List<Vector3f>       scaledVerts  = UtilHandler.getScaledVertices(submesh.getVertexPositions());
        List<Integer>        indeciesList = UtilHandler.getIndeciesAsIntegerList(submesh.getIndecies());
        DecimalFormatSymbols dfs          = new DecimalFormatSymbols(Locale.ENGLISH);
        DecimalFormat        df           = new DecimalFormat("###.#######", dfs);
        
        for (Vector3f pos : scaledVerts)
        {
            sb.append(String.format("v %s %s %s%n", df.format(pos.x), df.format(pos.y), df.format(pos.z)));
        }
        
        for (SKNData vertex : vertices)
        {
            Vector2f pos = vertex.getUv();
            sb.append(String.format("vt %s %s%n", df.format(pos.x), df.format(pos.y)));
        }
        
        for (SKNData vertex : vertices)
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
