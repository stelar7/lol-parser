package no.stelar7.cdragon.types.mgeo.data;

/**
 * A range of a mesh's index buffer drawn with one material
 */
public class MGeoFileMeshSubMesh
{
    // hash
    private int    unknown;
    // material
    private String name;
    private int    startIndex;
    private int    indexCount;
    // min vertex
    private int    startVertex;
    // max vertex
    private int    vertexCount;
    
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
}
