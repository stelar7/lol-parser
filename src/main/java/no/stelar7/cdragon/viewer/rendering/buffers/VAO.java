package no.stelar7.cdragon.viewer.rendering.buffers;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class VAO implements AutoCloseable
{
    
    int id;
    
    public VAO()
    {
        id = glGenVertexArrays();
    }
    
    public void bind()
    {
        glBindVertexArray(id);
    }
    
    public void unbind()
    {
        glBindVertexArray(0);
    }
    
    @Override
    public void close()
    {
        glDeleteVertexArrays(id);
    }
    
    public void enableAttribIndex(int index)
    {
        glEnableVertexAttribArray(index);
    }
    
    public void setPointer(int index, int size)
    {
        glVertexAttribPointer(index, size, GL_FLOAT, false, 0, 0);
    }
}
