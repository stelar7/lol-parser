package no.stelar7.cdragon.types.skn.data;

public class SKNMaterial
{
    private String name;
    private int    startVertex;
    private int    numVertex;
    private int    startIndex;
    private int    numIndex;
    
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
