package no.stelar7.cdragon.types.scb.data;

import no.stelar7.cdragon.util.types.Vector3b;
import org.joml.*;

import java.util.*;

public class SCBContent
{
    private int vertexCount;
    private int faceCount;
    private int colored;
    
    private Vector3f center;
    private Vector3f pivot;
    
    private List<Vector3f> vertices = new ArrayList<>();
    private List<Integer>  indecies = new ArrayList<>();
    private List<SCBFace>  faces    = new ArrayList<>();
    private List<Vector3b> colors   = new ArrayList<>();
    
    public int getVertexCount()
    {
        return vertexCount;
    }
    
    public void setVertexCount(int vertexCount)
    {
        this.vertexCount = vertexCount;
    }
    
    public int getFaceCount()
    {
        return faceCount;
    }
    
    public void setFaceCount(int faceCount)
    {
        this.faceCount = faceCount;
    }
    
    public int getColored()
    {
        return colored;
    }
    
    public void setColored(int colored)
    {
        this.colored = colored;
    }
    
    public Vector3f getCenter()
    {
        return center;
    }
    
    public void setCenter(Vector3f center)
    {
        this.center = center;
    }
    
    public Vector3f getPivot()
    {
        return pivot;
    }
    
    public void setPivot(Vector3f pivot)
    {
        this.pivot = pivot;
    }
    
    public List<Vector3f> getVertices()
    {
        return vertices;
    }
    
    public void setVertices(List<Vector3f> vertices)
    {
        this.vertices = vertices;
    }
    
    public List<Integer> getIndecies()
    {
        return indecies;
    }
    
    public void setIndecies(List<Integer> indecies)
    {
        this.indecies = indecies;
    }
    
    public List<SCBFace> getFaces()
    {
        return faces;
    }
    
    public void setFaces(List<SCBFace> faces)
    {
        this.faces = faces;
    }
    
    public List<Vector3b> getColors()
    {
        return colors;
    }
    
    public void setColors(List<Vector3b> colors)
    {
        this.colors = colors;
    }
}
