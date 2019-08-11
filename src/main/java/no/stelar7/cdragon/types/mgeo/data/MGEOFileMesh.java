package no.stelar7.cdragon.types.mgeo.data;

import no.stelar7.cdragon.util.types.math.*;

import java.util.*;

public class MGEOFileMesh
{
    private String                    name;
    private MGEOFileMeshType          type;
    private List<MGEOFileMeshVertex>  vertices;
    private List<Short>               indices;
    private List<MGeoFileMeshSubMesh> subMeshes;
    private BoundingBox               boundingBox;
    private Matrix4f                  transformationMatrix;
    private Vector3f                  unknown;
    private Matrix4f[]                unknown2;
    private String                    lightMap;
    private Vector4f                  unknown3;
    
    public List<MGEOFileMeshVertex> getVertices()
    {
        return vertices;
    }
    
    public void setVertices(List<MGEOFileMeshVertex> vertices)
    {
        this.vertices = vertices;
    }
    
    public List<Short> getIndices()
    {
        return indices;
    }
    
    public void setIndices(List<Short> indices)
    {
        this.indices = indices;
    }
    
    public String getName()
    {
        return name;
    }
    
    public void setName(String name)
    {
        this.name = name;
    }
    
    public MGEOFileMeshType getType()
    {
        return type;
    }
    
    public void setType(MGEOFileMeshType type)
    {
        this.type = type;
    }
    
    public List<MGeoFileMeshSubMesh> getSubMeshes()
    {
        return subMeshes;
    }
    
    public void setSubMeshes(List<MGeoFileMeshSubMesh> subMeshes)
    {
        this.subMeshes = subMeshes;
    }
    
    public BoundingBox getBoundingBox()
    {
        return boundingBox;
    }
    
    public void setBoundingBox(BoundingBox boundingBox)
    {
        this.boundingBox = boundingBox;
    }
    
    public Matrix4f getTransformationMatrix()
    {
        return transformationMatrix;
    }
    
    public void setTransformationMatrix(Matrix4f transformationMatrix)
    {
        this.transformationMatrix = transformationMatrix;
    }
    
    public Vector3f getUnknown()
    {
        return unknown;
    }
    
    public void setUnknown(Vector3f unknown)
    {
        this.unknown = unknown;
    }
    
    public Matrix4f[] getUnknown2()
    {
        return unknown2;
    }
    
    public void setUnknown2(Matrix4f[] unknown2)
    {
        this.unknown2 = unknown2;
    }
    
    public String getLightMap()
    {
        return lightMap;
    }
    
    public void setLightMap(String lightMap)
    {
        this.lightMap = lightMap;
    }
    
    public Vector4f getUnknown3()
    {
        return unknown3;
    }
    
    public void setUnknown3(Vector4f unknown3)
    {
        this.unknown3 = unknown3;
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        MGEOFileMesh that = (MGEOFileMesh) o;
        return Objects.equals(name, that.name) &&
               type == that.type &&
               Objects.equals(vertices, that.vertices) &&
               Objects.equals(indices, that.indices) &&
               Objects.equals(subMeshes, that.subMeshes) &&
               Objects.equals(boundingBox, that.boundingBox) &&
               Objects.equals(transformationMatrix, that.transformationMatrix) &&
               Objects.equals(unknown, that.unknown) &&
               Arrays.equals(unknown2, that.unknown2) &&
               Objects.equals(lightMap, that.lightMap) &&
               Objects.equals(unknown3, that.unknown3);
    }
    
    @Override
    public int hashCode()
    {
        int result = Objects.hash(name, type, vertices, indices, subMeshes, boundingBox, transformationMatrix, unknown, lightMap, unknown3);
        result = 31 * result + Arrays.hashCode(unknown2);
        return result;
    }
    
    @Override
    public String toString()
    {
        return "MGEOFileMesh{" +
               "name='" + name + '\'' +
               ", type=" + type +
               ", vertices=" + vertices +
               ", indices=" + indices +
               ", subMeshes=" + subMeshes +
               ", boundingBox=" + boundingBox +
               ", transformationMatrix=" + transformationMatrix +
               ", unknown=" + unknown +
               ", unknown2=" + Arrays.toString(unknown2) +
               ", lightMap='" + lightMap + '\'' +
               ", unknown3=" + unknown3 +
               '}';
    }
}
