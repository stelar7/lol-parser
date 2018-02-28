package no.stelar7.cdragon.viewer.rendering.shaders;

import lombok.ToString;
import no.stelar7.cdragon.util.handlers.UtilHandler;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

@ToString
public class Shader implements AutoCloseable
{
    private int id;
    
    public Shader(String filename)
    {
        String source = UtilHandler.readInternalAsString(filename);
        
        if (filename.endsWith(".vert"))
        {
            create(GL_VERTEX_SHADER, source);
        } else if (filename.endsWith(".frag"))
        {
            create(GL_FRAGMENT_SHADER, source);
        }
    }
    
    private void create(int type, String source)
    {
        id = glCreateShader(type);
        UtilHandler.logToFile("gl.log", String.format("glCreateShader(%s) = %s", getType(type), id));
        
        glShaderSource(id, source);
        UtilHandler.logToFile("gl.log", String.format("glShaderSource(%s, %s)", id, source));
        
        glCompileShader(id);
        UtilHandler.logToFile("gl.log", String.format("glCompileShader(%s)", id));
        
        int status = glGetShaderi(id, GL_COMPILE_STATUS);
        UtilHandler.logToFile("gl.log", String.format("glGetShaderi(%s, GL_COMPILE_STATUS) = %s", id, status));
        if (status != GL_TRUE)
        {
            String log = glGetShaderInfoLog(id);
            UtilHandler.logToFile("gl.log", String.format("glGetShaderInfoLog(%s) = %s", id, log));
            System.err.println(log);
        }
    }
    
    private String getType(int type)
    {
        if (type == GL_VERTEX_SHADER)
        {
            return "GL_VERTEX_SHADER";
        }
        return "GL_FRAGMENT_SHADER";
    }
    
    public int getId()
    {
        return id;
    }
    
    @Override
    public void close()
    {
        glDeleteShader(id);
        UtilHandler.logToFile("gl.log", String.format("glDeleteShader(%s)", id));
    }
}
