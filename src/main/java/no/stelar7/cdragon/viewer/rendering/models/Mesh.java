package no.stelar7.cdragon.viewer.rendering.models;

import no.stelar7.cdragon.types.skn.data.*;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.types.Vector3f;
import no.stelar7.cdragon.viewer.rendering.buffers.VBO;

import java.util.List;

import static org.lwjgl.opengl.GL15.*;

public class Mesh implements AutoCloseable
{
    private VBO vbo;
    private VBO ibo;
    
    public int indexCount;
    
    public Mesh()
    {
        vbo = new VBO(GL_ARRAY_BUFFER);
        ibo = new VBO(GL_ELEMENT_ARRAY_BUFFER);
    }
    
    public Mesh(SKNMaterial submesh)
    {
        this();
        
        float[]        verts          = new float[submesh.getNumVertex() * 3];
        List<Vector3f> scaledVertices = ModelHandler.getScaledVertices(submesh.getVertexPositions());
        for (int i = 0; i < scaledVertices.size(); i++)
        {
            Vector3f pos = scaledVertices.get(i);
            
            verts[(i * 3) + 0] = pos.x;
            verts[(i * 3) + 1] = pos.y;
            verts[(i * 3) + 2] = pos.z;
        }
        
        // load indecies
        int[] inds = new int[submesh.getNumIndex()];
        for (int i = 0; i < submesh.getIndecies().size(); i++)
        {
            inds[i] = submesh.getIndecies().get(i);
        }
        
        setVertices(verts);
        setIndecies(inds);
    }
    
    public Mesh(SKNFile data)
    {
        this();
        
        float[]        verts          = new float[data.getVertexCount() * 3];
        List<Vector3f> scaledVertices = ModelHandler.getScaledVertices(data.getVertexPositions());
        for (int i = 0; i < scaledVertices.size(); i++)
        {
            Vector3f pos = scaledVertices.get(i);
            
            verts[(i * 3) + 0] = pos.x;
            verts[(i * 3) + 1] = pos.y;
            verts[(i * 3) + 2] = pos.z;
        }
        
        
        // load indecies
        int[]         inds                  = new int[data.getIndexCount()];
        List<Integer> indeciesAsIntegerList = ModelHandler.getIndeciesAsIntegerList(data.getIndecies());
        for (int i = 0; i < indeciesAsIntegerList.size(); i++)
        {
            inds[i] = indeciesAsIntegerList.get(i);
        }
        
        
        setVertices(verts);
        setIndecies(inds);
    }
    
    public void setVertices(float[] vertices)
    {
        vbo.bind();
        vbo.setData(vertices);
    }
    
    public void setIndecies(int[] indecies)
    {
        ibo.bind();
        ibo.setData(indecies);
        indexCount = indecies.length;
    }
    
    public void bindForDraw()
    {
        ibo.bind();
    }
    
    public void unbindForDraw()
    {
        ibo.unbind();
    }
    
    public void unbindForVAO()
    {
        vbo.unbind();
    }
    
    
    public int getIndexCount()
    {
        return indexCount;
    }
    
    @Override
    public void close()
    {
        ibo.close();
        vbo.close();
    }
    
    public void bindForVAO()
    {
        vbo.bind();
    }
}
