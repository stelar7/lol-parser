package no.stelar7.cdragon.viewer.rendering.buffers;

import lombok.ToString;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

@ToString
public class VAO implements AutoCloseable
{
    
    private int id;
    
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
    
    public void setPointer(int index, int size)
    {
        glEnableVertexAttribArray(index);
        glVertexAttribPointer(index, size, GL_FLOAT, false, 0, 0);
    }
}
