package no.stelar7.cdragon.viewer.rendering.shaders;

import no.stelar7.cdragon.util.handlers.UtilHandler;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

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
        glShaderSource(id, source);
        glCompileShader(id);
        
        int status = glGetShaderi(id, GL_COMPILE_STATUS);
        if (status != GL_TRUE)
        {
            String log = glGetShaderInfoLog(id);
            System.err.println(log);
        }
    }
    
    public int getId()
    {
        return id;
    }
    
    @Override
    public void close()
    {
        glDeleteShader(id);
    }
}
