package no.stelar7.cdragon.viewer.rendering.buffers;

import no.stelar7.cdragon.util.handlers.UtilHandler;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class VAO implements AutoCloseable
{
    
    private int id;
    
    public VAO()
    {
        id = glGenVertexArrays();
        UtilHandler.logToFile("gl.log", "glGenVertexArrays() = " + id);
    }
    
    public void bind()
    {
        glBindVertexArray(id);
        UtilHandler.logToFile("gl.log", String.format("glBindVertexArray(%s)", id));
    }
    
    public void unbind()
    {
        glBindVertexArray(0);
        UtilHandler.logToFile("gl.log", String.format("glBindVertexArray(%s)", 0));
    }
    
    @Override
    public void close()
    {
        glDeleteVertexArrays(id);
        UtilHandler.logToFile("gl.log", String.format("glDeleteVertexArray(%s)", id));
    }
    
    public void setPointer(int index, int size)
    {
        glEnableVertexAttribArray(index);
        glVertexAttribPointer(index, size, GL_FLOAT, false, 0, 0);
        UtilHandler.logToFile("gl.log", String.format("glEnableVertexAttribArray(%s)", index));
        UtilHandler.logToFile("gl.log", String.format("glVertexAttribPointer(%s, %s, GL_FLOAT, GL_FALSE, 0, 0)", index, size));
    }
}
