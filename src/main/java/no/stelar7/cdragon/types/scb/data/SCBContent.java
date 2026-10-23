package no.stelar7.cdragon.types.scb.data;

import no.stelar7.cdragon.util.types.math.*;

import java.util.*;

public final class SCBContent
{
    private final int vertexCount;
    private final int faceCount;
    // flags; bit 0 = per face vertex colors (colors), bit 1 = has local origin locator and pivot
    private final int colored;
    
    private final Vector3f boundingBoxMin;
    private final Vector3f boundingBoxMax;
    
    // central point
    private final Vector3f center;
    // only present in .sco files
    private final Vector3f pivot;
    
    private final boolean hasVertexColors;
    // per vertex BGRA colors (version 3.2+, when hasVertexColors)
    private final List<Vector4b> vertexColors;
    
    private final List<Vector3f> vertices;
    private final List<Integer>  indecies;
    private final List<SCBFace>  faces;
    // per face corner RGB colors (3 per face, when flag bit 0 is set)
    private final List<Vector3b> colors;
    
    public SCBContent(int vertexCount, int faceCount, int colored, Vector3f boundingBoxMin, Vector3f boundingBoxMax, Vector3f center, Vector3f pivot, boolean hasVertexColors, List<Vector4b> vertexColors, List<Vector3f> vertices, List<SCBFace> faces, List<Vector3b> colors)
    {
        this.vertexCount = vertexCount;
        this.faceCount = faceCount;
        this.colored = colored;
        this.boundingBoxMin = boundingBoxMin;
        this.boundingBoxMax = boundingBoxMax;
        this.center = center;
        this.pivot = pivot;
        this.hasVertexColors = hasVertexColors;
        this.vertexColors = vertexColors;
        this.vertices = vertices;
        // not stored in the file, the indices are in the faces
        this.indecies = new ArrayList<>();
        this.faces = faces;
        this.colors = colors;
    }
    
    public Vector3f getBoundingBoxMin()
    {
        return boundingBoxMin;
    }
    
    public Vector3f getBoundingBoxMax()
    {
        return boundingBoxMax;
    }
    
    public boolean isHasVertexColors()
    {
        return hasVertexColors;
    }
    
    public List<Vector4b> getVertexColors()
    {
        return vertexColors;
    }
    
    public int getVertexCount()
    {
        return vertexCount;
    }
    
    public int getFaceCount()
    {
        return faceCount;
    }
    
    public int getColored()
    {
        return colored;
    }
    
    public Vector3f getCenter()
    {
        return center;
    }
    
    public Vector3f getPivot()
    {
        return pivot;
    }
    
    public List<Vector3f> getVertices()
    {
        return vertices;
    }
    
    public List<Integer> getIndecies()
    {
        return indecies;
    }
    
    public List<SCBFace> getFaces()
    {
        return faces;
    }
    
    public List<Vector3b> getColors()
    {
        return colors;
    }
}
