package no.stelar7.cdragon.types.mgeo.data;

import java.util.*;

/**
 * The layout of one vertex buffer; elements are interleaved per vertex in this order
 */
public class MGEOVertexDeclaration
{
    // 0 = static, 1 = dynamic, 2 = stream
    private int                     usage;
    private List<MGEOVertexElement> elements = new ArrayList<>();
    
    public int getUsage()
    {
        return usage;
    }
    
    public void setUsage(int usage)
    {
        this.usage = usage;
    }
    
    public List<MGEOVertexElement> getElements()
    {
        return elements;
    }
    
    public void setElements(List<MGEOVertexElement> elements)
    {
        this.elements = elements;
    }
    
    /**
     * Size of a single vertex in bytes
     */
    public int getStride()
    {
        int size = 0;
        for (MGEOVertexElement element : elements)
        {
            size += element.getSize();
        }
        return size;
    }
}
