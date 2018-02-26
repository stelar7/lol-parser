package viewer.rendering.models;

import no.stelar7.cdragon.types.skn.data.SKNFile;
import viewer.rendering.buffers.*;

import static org.lwjgl.opengl.GL15.*;

public class Mesh
{
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
        vao.setPointer(0, 2);
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
    
    public static Mesh loadSKN(SKNFile skn)
    {
        return null;
    }
}
