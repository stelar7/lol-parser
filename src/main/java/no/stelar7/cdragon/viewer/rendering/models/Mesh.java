package no.stelar7.cdragon.viewer.rendering.models;

import no.stelar7.cdragon.types.skn.data.*;
import no.stelar7.cdragon.util.handlers.ModelHandler;
import no.stelar7.cdragon.util.types.math.Vector3f;
import no.stelar7.cdragon.viewer.rendering.buffers.VBO;

import java.util.List;

import static org.lwjgl.opengl.GL15.*;

public class Mesh implements AutoCloseable
{
    private VBO vbo;
    private VBO nbo;
    private VBO ibo;
    
    public int indexCount;
    
    public Mesh()
    {
        vbo = new VBO(GL_ARRAY_BUFFER);
        nbo = new VBO(GL_ARRAY_BUFFER);
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
        
        float[] norms = new float[submesh.getNumVertex() * 3];
        for (int i = 0; i < submesh.getVertices().size(); i++)
        {
            Vector3f norm = submesh.getVertices().get(i).getNormals();
            norms[(i * 3) + 0] = norm.x;
            norms[(i * 3) + 1] = norm.y;
            norms[(i * 3) + 2] = norm.z;
        }
        
        // load indecies
        int[] inds = new int[submesh.getNumIndex()];
        for (int i = 0; i < submesh.getIndecies().size(); i++)
        {
            inds[i] = submesh.getIndecies().get(i);
        }
        
        setVertices(verts);
        setNormals(norms);
        setIndecies(inds);
    }
    
    public void setVertices(float[] vertices)
    {
        vbo.bind();
        vbo.setData(vertices);
    }
    
    
    public void setNormals(float[] vertices)
    {
        nbo.bind();
        nbo.setData(vertices);
    }
    
    public void setIndecies(int[] indecies)
    {
        ibo.bind();
        ibo.setData(indecies);
        indexCount = indecies.length;
    }
    
    public void bindIBO()
    {
        ibo.bind();
    }
    
    public void bindVBO()
    {
        vbo.bind();
    }
    
    public void bindNBO()
    {
        nbo.bind();
    }
    
    public void unbindIBO()
    {
        ibo.unbind();
    }
    
    public void unbindVAO()
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
}
