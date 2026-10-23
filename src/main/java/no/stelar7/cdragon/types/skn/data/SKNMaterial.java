package no.stelar7.cdragon.types.skn.data;

import no.stelar7.cdragon.util.types.math.Vector3f;

import java.util.*;

public final class SKNMaterial
{
    private final String name;
    private final int    startVertex;
    private final int    numVertex;
    private final int    startIndex;
    private final int    numIndex;
    
    private final List<Integer> indecies;
    private final List<SKNData> vertices;
    
    public SKNMaterial(String name, int startVertex, int numVertex, int startIndex, int numIndex, List<Integer> indecies, List<SKNData> vertices)
    {
        this.name = name;
        this.startVertex = startVertex;
        this.numVertex = numVertex;
        this.startIndex = startIndex;
        this.numIndex = numIndex;
        this.indecies = indecies;
        this.vertices = vertices;
    }
    
    public List<Integer> getIndecies()
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
    
    public String getName()
    {
        return name;
    }
    
    public int getStartVertex()
    {
        return startVertex;
    }
    
    public int getNumVertex()
    {
        return numVertex;
    }
    
    public int getStartIndex()
    {
        return startIndex;
    }
    
    public int getNumIndex()
    {
        return numIndex;
    }
}
