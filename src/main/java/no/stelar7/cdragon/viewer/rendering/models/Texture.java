package no.stelar7.cdragon.viewer.rendering.models;

import lombok.ToString;
import no.stelar7.cdragon.types.skn.data.*;
import no.stelar7.cdragon.util.readers.types.Vector2f;
import no.stelar7.cdragon.viewer.rendering.buffers.VBO;
import org.lwjgl.system.*;

import java.awt.image.*;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;

@ToString
public class Texture implements AutoCloseable
{
    private int id;
    private VBO tbo;
    
    public Texture()
    {
        this.id = glGenTextures();
        this.tbo = new VBO(GL_ARRAY_BUFFER);
    }
    
    public void bind()
    {
        tbo.bind();
        glBindTexture(GL_TEXTURE_2D, id);
    }
    
    public void unbind()
    {
        glBindTexture(GL_TEXTURE_2D, 0);
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
        
        byte[]     pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        ByteBuffer data   = MemoryUtil.memAlloc(image.getWidth() * image.getHeight() * 4);
        data.put(pixels).flip();
        
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, image.getWidth(), image.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, data);
        
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glGenerateMipmap(GL_TEXTURE_2D);
    }
    
    public static Texture loadForSKN(SKNFile data)
    {
        float[] uvs = new float[data.getVertexCount() * 2];
        for (int i = 0; i < data.getVertexCount(); i++)
        {
            SKNData  v   = data.getVertices().get(i);
            Vector2f pos = v.getUv();
            
            uvs[(i * 2) + 0] = pos.x;
            uvs[(i * 2) + 1] = pos.y;
        }
        
        Texture texture = new Texture();
        texture.getTBO().bind();
        texture.getTBO().setData(uvs);
        return texture;
    }
    
    @Override
    public void close()
    {
        glDeleteTextures(id);
    }
    
    public VBO getTBO()
    {
        return tbo;
    }
}
