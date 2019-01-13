package no.stelar7.cdragon.types.mgeo.data;

import no.stelar7.cdragon.util.types.math.*;

import java.util.*;

public class MGEOFileMesh
{
    private String                    name;
    private MGEOFileMeshType          type;
    private List<Integer>             vertexBuffers;
    private List<MGeoFileMeshSubMesh> subMeshes;
    private BoundingBox               boundingBox;
    private Matrix4f                  transformationMatrix;
    private Vector3f                  unknown;
    private Matrix4f[]                unknown2;
    private String                    texture;
    private Vector4f                  color;
    
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
    
    public List<Integer> getVertexBuffers()
    {
        return vertexBuffers;
    }
    
    public void setVertexBuffers(List<Integer> vertexBuffers)
    {
        this.vertexBuffers = vertexBuffers;
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
    
    public String getTexture()
    {
        return texture;
    }
    
    public void setTexture(String texture)
    {
        this.texture = texture;
    }
    
    public Vector4f getColor()
    {
        return color;
    }
    
    public void setColor(Vector4f color)
    {
        this.color = color;
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
               Objects.equals(vertexBuffers, that.vertexBuffers) &&
               Objects.equals(subMeshes, that.subMeshes) &&
               Objects.equals(boundingBox, that.boundingBox) &&
               Objects.equals(transformationMatrix, that.transformationMatrix) &&
               Objects.equals(unknown, that.unknown) &&
               Arrays.equals(unknown2, that.unknown2) &&
               Objects.equals(texture, that.texture) &&
               Objects.equals(color, that.color);
    }
    
    @Override
    public int hashCode()
    {
        int result = Objects.hash(name, type, vertexBuffers, subMeshes, boundingBox, transformationMatrix, unknown, texture, color);
        result = 31 * result + Arrays.hashCode(unknown2);
        return result;
    }
    
    @Override
    public String toString()
    {
        return "MGEOFileMesh{" +
               "name='" + name + '\'' +
               ", type=" + type +
               ", vertexBuffers=" + vertexBuffers +
               ", subMeshes=" + subMeshes +
               ", boundingBox=" + boundingBox +
               ", transformationMatrix=" + transformationMatrix +
               ", unknown=" + unknown +
               ", unknown2=" + Arrays.toString(unknown2) +
               ", texture='" + texture + '\'' +
               ", color=" + color +
               '}';
    }
}
