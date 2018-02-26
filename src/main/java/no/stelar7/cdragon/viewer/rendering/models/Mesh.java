package no.stelar7.cdragon.viewer.rendering.models;

import no.stelar7.cdragon.types.skn.SKNParser;
import no.stelar7.cdragon.types.skn.data.*;
import no.stelar7.cdragon.viewer.rendering.buffers.*;

import java.nio.file.*;

import static org.lwjgl.opengl.GL15.*;

public class Mesh
{
    private static final int VERTEX_SIZE = 3;
    
    VAO vao;
    VBO vbo;
    VBO ibo;
    
    public int indexCount;
    
    public Mesh(float[] vertices, int[] indecies)
    {
        vao = new VAO();
        vbo = new VBO(GL_ARRAY_BUFFER);
        ibo = new VBO(GL_ELEMENT_ARRAY_BUFFER);
        
        setVertices(vertices);
        setIndecies(indecies);
    }
    
    public void setVertices(float[] vertices)
    {
        vao.bind();
        vbo.bind();
        vbo.setData(vertices);
        vao.enableAttribIndex(0);
        vao.setPointer(0, VERTEX_SIZE);
        vao.unbind();
        vbo.unbind();
    }
    
    public void setIndecies(int[] indecies)
    {
        ibo.bind();
        ibo.setData(indecies);
        indexCount = indecies.length;
        ibo.unbind();
    }
    
    public void bind()
    {
        vao.bind();
        ibo.bind();
    }
    
    public void unbind()
    {
        ibo.unbind();
        vao.unbind();
    }
    
    public static Mesh loadSKN()
    {
        SKNParser parser = new SKNParser();
        Path      path   = Paths.get(System.getProperty("user.home"), "Downloads\\lolmodelviewer\\SampleModels\\filearchives\\0.0.0.48\\DATA\\Characters\\TeemoMushroom\\SuperTrap.skn");
        SKNFile   data   = parser.parse(path);
        
        float[] verts = new float[data.getVertexCount() * 3];
        for (int i = 0; i < data.getVertexCount(); i++)
        {
            SKNData v = data.getVertices().get(i);
            verts[(i * 3) + 0] = v.getPosition().x;
            verts[(i * 3) + 1] = v.getPosition().y;
            verts[(i * 3) + 2] = v.getPosition().z;
        }
        
        int[] inds = new int[data.getIndexCount()];
        for (int i = 0; i < data.getIndecies().size(); i++)
        {
            Short in = data.getIndecies().get(i);
            inds[i] = Integer.valueOf(in);
        }
        
        return new Mesh(verts, inds);
    }
    
    public int getIndexCount()
    {
        return indexCount;
    }
}
