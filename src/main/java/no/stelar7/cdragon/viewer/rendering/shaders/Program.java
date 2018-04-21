package no.stelar7.cdragon.viewer.rendering.shaders;


import no.stelar7.cdragon.util.handlers.UtilHandler;
import org.joml.Matrix4f;

import java.util.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class Program implements AutoCloseable
{
    public static Program current;
    
    
    private int id;
    
    public Program()
    {
        id = glCreateProgram();
        UtilHandler.logToFile("gl.log", String.format("glCreateProgram() = %s", id));
    }
    
    public void attach(Shader shader)
    {
        glAttachShader(id, shader.getId());
        UtilHandler.logToFile("gl.log", String.format("glAttachShader(%s, %s)", id, shader.getId()));
    }
    
    public void bindVertLocation(String name, int loc)
    {
        glBindAttribLocation(id, loc, name);
        UtilHandler.logToFile("gl.log", String.format("glBindAttribLocation(%s, %s, %s)", id, loc, name));
    }
    
    public void bindFragLocation(String name, int loc)
    {
        glBindFragDataLocation(id, loc, name);
        UtilHandler.logToFile("gl.log", String.format("glBindFragDataLocation(%s, %s, %s)", id, loc, name));
    }
    
    public void link()
    {
        glLinkProgram(id);
        UtilHandler.logToFile("gl.log", String.format("glLinkProgram(%s)", id));
        
        int status = glGetProgrami(id, GL_LINK_STATUS);
        UtilHandler.logToFile("gl.log", String.format("glGetProgrami(%s, GL_LINK_STATUS) = %s", id, status));
        if (status != GL_TRUE)
        {
            String log = glGetProgramInfoLog(id);
            UtilHandler.logToFile("gl.log", String.format("glGetProgramInfoLog(%s) = %s", id, log));
            System.err.println(log);
        }
    }
    
    public void bind()
    {
        glUseProgram(id);
        UtilHandler.logToFile("gl.log", String.format("glUseProgram(%s)", id));
    }
    
    public void unbind()
    {
        glUseProgram(0);
        UtilHandler.logToFile("gl.log", String.format("glUseProgram(%s)", 0));
    }
    
    @Override
    public void close()
    {
        glDeleteProgram(id);
        UtilHandler.logToFile("gl.log", String.format("glDeleteProgram(%s)", id));
    }
    
    public int getId()
    {
        return id;
    }
    
    Map<String, Integer> uniformLocations = new HashMap<>();
    
    private int getUniformLocation(String name)
    {
        return uniformLocations.computeIfAbsent(name, __ -> {
            int loc = glGetUniformLocation(id, name);
            UtilHandler.logToFile("gl.log", String.format("glGetUniformLocation(%s, %s) = %s", id, name, loc));
            return loc;
        });
    }
    
    public void setMatrix4f(String name, Matrix4f mat)
    {
        float[] data = mat.get(new float[16]);
        int     loc  = getUniformLocation(name);
        glUniformMatrix4fv(loc, false, data);
        UtilHandler.logToFile("gl.log", String.format("glUniformMatrix4fv(%s, GL_FALSE, %s)", loc, data.length > 0 ? "{data}" : "null"));
    }
    
    public void setInt(String name, int data)
    {
        int loc = getUniformLocation(name);
        glUniform1i(loc, data);
        UtilHandler.logToFile("gl.log", String.format("glUniform1i(%s, %s)", loc, data));
    }
}
