package viewer.rendering.shaders;

import no.stelar7.cdragon.util.handlers.UtilHandler;

import java.io.File;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

public class Shader implements AutoCloseable
{
    int id;
    
    public Shader(String filename)
    {
        File   shaderFile = new File(getClass().getClassLoader().getResource(filename).getFile());
        String source     = UtilHandler.readAsString(shaderFile.toPath());
        
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
