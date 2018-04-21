package no.stelar7.cdragon.types.skn.data;

import no.stelar7.cdragon.util.types.Vector3f;

import java.util.List;

public class SKNFile
{
    private int   magic;
    private short version;
    private short objectCount;
    private int   indexCount;
    private int   vertexCount;
    private int   materialCount;
    
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
    
    public int getMagic()
    {
        return magic;
    }
    
    public void setMagic(int magic)
    {
        this.magic = magic;
    }
    
    public short getVersion()
    {
        return version;
    }
    
    public void setVersion(short version)
    {
        this.version = version;
    }
    
    public short getObjectCount()
    {
        return objectCount;
    }
    
    public void setObjectCount(short objectCount)
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
    
    public int getContainsTangent()
    {
        return containsTangent;
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
}
