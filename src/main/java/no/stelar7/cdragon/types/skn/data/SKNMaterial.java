package no.stelar7.cdragon.types.skn.data;

import no.stelar7.cdragon.util.types.Vector3f;

import java.util.*;

public class SKNMaterial
{
    private String name;
    private int    startVertex;
    private int    numVertex;
    private int    startIndex;
    private int    numIndex;
    
    private List<Integer>   indecies;
    private List<SKNData> vertices;
    
    public List<Integer> getIndecies()
    {
        return indecies;
    }
    
    public void setIndecies(List<Integer> indecies)
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
    
    public String getName()
    {
        return name;
    }
    
    public void setName(String name)
    {
        this.name = name;
    }
    
    public int getStartVertex()
    {
        return startVertex;
    }
    
    public void setStartVertex(int startVertex)
    {
        this.startVertex = startVertex;
    }
    
    public int getNumVertex()
    {
        return numVertex;
    }
    
    public void setNumVertex(int numVertex)
    {
        this.numVertex = numVertex;
    }
    
    public int getStartIndex()
    {
        return startIndex;
    }
    
    public void setStartIndex(int startIndex)
    {
        this.startIndex = startIndex;
    }
    
    public int getNumIndex()
    {
        return numIndex;
    }
    
    public void setNumIndex(int numIndex)
    {
        this.numIndex = numIndex;
    }
}
