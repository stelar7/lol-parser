package no.stelar7.cdragon.viewer.rendering.buffers;

import no.stelar7.cdragon.util.handlers.UtilHandler;

import static org.lwjgl.opengl.GL15.*;

public class VBO implements AutoCloseable
{
    private int id;
    private int type;
    
    public VBO(int type)
    {
        this.id = glGenBuffers();
        UtilHandler.logToFile("gl.log", String.format("glGenBuffers() = %s", id));
        this.type = type;
    }
    
    public void bind()
    {
        glBindBuffer(type, id);
        UtilHandler.logToFile("gl.log", String.format("glBindBuffer(%s, %s)", getType(type), id));
    }
    
    public void unbind()
    {
        glBindBuffer(type, 0);
        UtilHandler.logToFile("gl.log", String.format("glBindBuffer(%s, %s)", getType(type), id));
    }
    
    public void setData(float[] data)
    {
        glBufferData(type, data, GL_STATIC_DRAW);
        UtilHandler.logToFile("gl.log", String.format("glBufferData(%s, %s, %s)", getType(type), data.length > 0 ? "{data}" : null, "GL_STATIC_DRAW"));
    }
    
    private String getType(int type)
    {
        return type == GL_ARRAY_BUFFER ? "GL_ARRAY_BUFFER" : "GL_ELEMENT_ARRAY_BUFFER";
    }
    
    public void setData(int[] data)
    {
        glBufferData(type, data, GL_STATIC_DRAW);
        UtilHandler.logToFile("gl.log", String.format("glBufferData(%s, %s, %s)", getType(type), data.length > 0 ? "{data}" : null, "GL_STATIC_DRAW"));
    }
    
    @Override
    public void close()
    {
        glDeleteBuffers(id);
    }
}
