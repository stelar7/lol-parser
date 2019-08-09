package no.stelar7.cdragon.viewer.rendering.models;

import no.stelar7.cdragon.types.skn.data.*;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.types.math.Vector2f;
import no.stelar7.cdragon.viewer.rendering.buffers.VBO;
import org.lwjgl.*;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;

public class Texture implements AutoCloseable
{
    private int id;
    private VBO tbo;
    
    public Texture()
    {
        this.id = glGenTextures();
        UtilHandler.logToFile("gl.log", String.format("glGenTextures() = %s", id));
        this.tbo = new VBO(GL_ARRAY_BUFFER);
    }
    
    public Texture(SKNMaterial data, BufferedImage textureImage)
    {
        this();
        
        float[] uvs = new float[data.getNumVertex() * 2];
        for (int i = 0; i < data.getNumVertex(); i++)
        {
            SKNData  v   = data.getVertices().get(i);
            Vector2f pos = v.getUv();
            
            uvs[(i * 2) + 0] = pos.x;
            uvs[(i * 2) + 1] = pos.y;
        }
        
        tbo.bind();
        tbo.setData(uvs);
        setData(textureImage);
    }
    
    public void bind()
    {
        tbo.bind();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, id);
        UtilHandler.logToFile("gl.log", "glActiveTexture(GL_TEXTURE0)");
        UtilHandler.logToFile("gl.log", String.format("glBindTexture(GL_TEXTURE_2D, %s)", id));
    }
    
    public void unbind()
    {
        glBindTexture(GL_TEXTURE_2D, 0);
        UtilHandler.logToFile("gl.log", String.format("glBindTexture(GL_TEXTURE_2D, %s)", 0));
        tbo.unbind();
    }
    
    
    public void setData(BufferedImage image)
    {
        /*
        flip y coordinate
       
        AffineTransform transform = AffineTransform.getScaleInstance(1f, -1f);
        transform.translate(0, -image.getHeight());
        AffineTransformOp operation = new AffineTransformOp(transform, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        image = operation.filter(image, null);
        
         */
        
        bind();
        
        int[] pixelData = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixelData, 0, image.getWidth());
        ByteBuffer data = BufferUtils.createByteBuffer(image.getHeight() * image.getWidth() * 4);
        
        for (int y = 0; y < image.getHeight(); y++)
        {
            for (int x = 0; x < image.getWidth(); x++)
            {
                int pixel = pixelData[y * image.getWidth() + x];
                data.put((byte) ((pixel >> 16) & 0xFF));
                data.put((byte) ((pixel >> 8) & 0xFF));
                data.put((byte) ((pixel) & 0xFF));
                data.put((byte) ((pixel >> 24) & 0xFF));
            }
        }
        data.flip();
        
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, image.getWidth(), image.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, data);
        
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glGenerateMipmap(GL_TEXTURE_2D);
        
        
        UtilHandler.logToFile("gl.log", String.format("glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, %s, %s, 0, GL_RGBA, GL_UNSIGNED_BYTE, %s)", image.getWidth(), image.getHeight(), data.remaining() > 0 ? "{data}" : null));
        UtilHandler.logToFile("gl.log", "glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)");
        UtilHandler.logToFile("gl.log", "glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)");
        UtilHandler.logToFile("gl.log", "glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)");
        UtilHandler.logToFile("gl.log", "glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)");
        UtilHandler.logToFile("gl.log", "glGenerateMipmap(GL_TEXTURE_2D)");
    }
    
    
    @Override
    public void close()
    {
        glDeleteTextures(id);
        UtilHandler.logToFile("gl.log", String.format("glDeleteTextures(%s)", id));
    }
    
    public VBO getTBO()
    {
        return tbo;
    }
    public int getId(){return id;}
}
