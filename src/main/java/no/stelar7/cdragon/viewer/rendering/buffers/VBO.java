package no.stelar7.cdragon.viewer.rendering.buffers;

import lombok.ToString;

import static org.lwjgl.opengl.GL15.*;

@ToString
public class VBO implements AutoCloseable
{
    private int id;
    private int type;
    
    public VBO(int type)
    {
        this.id = glGenBuffers();
        this.type = type;
    }
    
    public void bind()
    {
        glBindBuffer(type, id);
    }
    
    public void unbind()
    {
        glBindBuffer(type, 0);
    }
    
    public void setData(float[] data)
    {
        glBufferData(type, data, GL_STATIC_DRAW);
    }
    
    public void setData(int[] indecies)
    {
        glBufferData(type, indecies, GL_STATIC_DRAW);
    }
    
    @Override
    public void close()
    {
        glDeleteBuffers(id);
    }
}
