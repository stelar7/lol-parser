package no.stelar7.cdragon.viewer.rendering.models;

import no.stelar7.cdragon.types.skn.data.SKNMaterial;
import no.stelar7.cdragon.util.handlers.ModelHandler;
import no.stelar7.cdragon.util.types.math.*;
import no.stelar7.cdragon.viewer.rendering.buffers.VBO;

import java.util.List;

import static org.lwjgl.opengl.GL15.*;

public class Mesh implements AutoCloseable
{
    private VBO vbo;
    private VBO nbo;
    private VBO ibo;
    private VBO BIbo;
    private VBO BWbo;
    
    
    public int indexCount;
    
    public Mesh()
    {
        vbo = new VBO(GL_ARRAY_BUFFER);
        nbo = new VBO(GL_ARRAY_BUFFER);
        ibo = new VBO(GL_ELEMENT_ARRAY_BUFFER);
        
        BIbo = new VBO(GL_ARRAY_BUFFER);
        BWbo = new VBO(GL_ARRAY_BUFFER);
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
        
        int[] inds = new int[submesh.getNumIndex()];
        for (int i = 0; i < submesh.getIndecies().size(); i++)
        {
            inds[i] = submesh.getIndecies().get(i);
        }
        
        int[] bones = new int[submesh.getNumVertex() * 4];
        for (int i = 0; i < submesh.getVertices().size(); i++)
        {
            Vector4b bone = submesh.getVertices().get(i).getBoneIndecies();
            bones[(i * 4) + 0] = bone.x;
            bones[(i * 4) + 1] = bone.y;
            bones[(i * 4) + 2] = bone.z;
            bones[(i * 4) + 3] = bone.w;
        }
        
        
        float[] weights = new float[submesh.getNumVertex() * 4];
        for (int i = 0; i < submesh.getVertices().size(); i++)
        {
            Vector4f weight = submesh.getVertices().get(i).getWeight();
            weights[(i * 4) + 0] = weight.x;
            weights[(i * 4) + 1] = weight.y;
            weights[(i * 4) + 2] = weight.z;
            weights[(i * 4) + 3] = weight.w;
        }
        
        
        setVertices(verts);
        setNormals(norms);
        setIndecies(inds);
        
        setBones(bones);
        setWeights(weights);
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
    
    public void setBones(int[] vertices)
    {
        BIbo.bind();
        BIbo.setData(vertices);
    }
    
    public void setWeights(float[] vertices)
    {
        BWbo.bind();
        BWbo.setData(vertices);
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
    
    public void bindBIbo()
    {
        BIbo.bind();
    }
    
    public void bindBWbo()
    {
        BWbo.bind();
    }
    
    public void unbindIBO()
    {
        ibo.unbind();
    }
    
    public int getIndexCount()
    {
        return indexCount;
    }
    
    @Override
    public void close()
    {
        vbo.close();
        nbo.close();
        ibo.close();
        BIbo.close();
        BWbo.close();
    }
}
