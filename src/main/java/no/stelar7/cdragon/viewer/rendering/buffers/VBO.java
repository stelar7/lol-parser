package no.stelar7.cdragon.viewer.rendering.buffers;

import static org.lwjgl.opengl.GL15.*;

public class VBO implements AutoCloseable
{
    private int id;
    private int buffer;
    
    public VBO(int buffer)
    {
        this.id = glGenBuffers();
        this.buffer = buffer;
    }
    
    public void bind()
    {
        glBindBuffer(buffer, id);
    }
    
    public void unbind()
    {
        glBindBuffer(buffer, 0);
    }
    
    public void setData(float[] data)
    {
        glBufferData(buffer, data, GL_STATIC_DRAW);
    }
    
    public void setData(int[] indecies)
    {
        glBufferData(buffer, indecies, GL_STATIC_DRAW);
    }
    
    @Override
    public void close()
    {
        glDeleteBuffers(id);
    }
}
