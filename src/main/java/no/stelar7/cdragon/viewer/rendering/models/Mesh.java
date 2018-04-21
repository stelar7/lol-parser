package no.stelar7.cdragon.viewer.rendering.models;

import no.stelar7.cdragon.types.skn.data.*;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.types.Vector3f;
import no.stelar7.cdragon.viewer.rendering.buffers.VBO;

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
    
    public Mesh(SKNFile data)
    {
        this();
        
        float minx = Integer.MAX_VALUE;
        float maxx = Integer.MIN_VALUE;
        float miny = Integer.MAX_VALUE;
        float maxy = Integer.MIN_VALUE;
        float minz = Integer.MAX_VALUE;
        float maxz = Integer.MIN_VALUE;
        
        for (int i = 0; i < data.getVertexCount(); i++)
        {
            SKNData  v   = data.getVertices().get(i);
            Vector3f pos = v.getPosition();
            
            maxx = Math.max(maxx, pos.x);
            minx = Math.min(minx, pos.x);
            maxy = Math.max(maxy, pos.y);
            miny = Math.min(miny, pos.y);
            maxz = Math.max(maxz, pos.z);
            minz = Math.min(minz, pos.z);
        }
        
        float[] verts = new float[data.getVertexCount() * 3];
        for (int i = 0; i < data.getVertexCount(); i++)
        {
            SKNData  v   = data.getVertices().get(i);
            Vector3f pos = v.getPosition();
            
            verts[(i * 3) + 0] = UtilHandler.scale(pos.x, minx, maxx, -1, 1);
            verts[(i * 3) + 1] = UtilHandler.scale(pos.y, miny, maxy, -1, 1);
            verts[(i * 3) + 2] = UtilHandler.scale(pos.z, minz, maxz, -1, 1);
        }
        
        
        int[] inds = new int[data.getIndexCount()];
        for (int i = 0; i < data.getIndecies().size(); i++)
        {
            Short in = data.getIndecies().get(i);
            inds[i] = Integer.valueOf(in);
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
