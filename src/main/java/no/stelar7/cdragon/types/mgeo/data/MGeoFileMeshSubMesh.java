package no.stelar7.cdragon.types.mgeo.data;

import java.util.*;

public class MGeoFileMeshSubMesh
{
    private int                      unknown;
    private String                   name;
    private int                      startIndex;
    private int                      indexCount;
    private int                      startVertex;
    private int                      vertexCount;
    private List<MGEOFileMeshVertex> vertices;
    private List<Short>              indices;
    
    public int getUnknown()
    {
        return unknown;
    }
    
    public void setUnknown(int unknown)
    {
        this.unknown = unknown;
    }
    
    public String getName()
    {
        return name;
    }
    
    public void setName(String name)
    {
        this.name = name;
    }
    
    public int getStartIndex()
    {
        return startIndex;
    }
    
    public void setStartIndex(int startIndex)
    {
        this.startIndex = startIndex;
    }
    
    public int getIndexCount()
    {
        return indexCount;
    }
    
    public void setIndexCount(int indexCount)
    {
        this.indexCount = indexCount;
    }
    
    public int getStartVertex()
    {
        return startVertex;
    }
    
    public void setStartVertex(int startVertex)
    {
        this.startVertex = startVertex;
    }
    
    public int getVertexCount()
    {
        return vertexCount;
    }
    
    public void setVertexCount(int vertexCount)
    {
        this.vertexCount = vertexCount;
    }
    
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
        MGeoFileMeshSubMesh that = (MGeoFileMeshSubMesh) o;
        return unknown == that.unknown &&
               startIndex == that.startIndex &&
               indexCount == that.indexCount &&
               startVertex == that.startVertex &&
               vertexCount == that.vertexCount &&
               Objects.equals(name, that.name) &&
               Objects.equals(vertices, that.vertices) &&
               Objects.equals(indices, that.indices);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(unknown, name, startIndex, indexCount, startVertex, vertexCount, vertices, indices);
    }
    
    @Override
    public String toString()
    {
        return "MGeoFileMeshSubMesh{" +
               "unknown=" + unknown +
               ", name='" + name + '\'' +
               ", startIndex=" + startIndex +
               ", indexCount=" + indexCount +
               ", startVertex=" + startVertex +
               ", vertexCount=" + vertexCount +
               ", vertices=" + vertices +
               ", indices=" + indices +
               '}';
    }
}
