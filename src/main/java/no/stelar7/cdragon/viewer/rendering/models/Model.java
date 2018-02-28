package no.stelar7.cdragon.viewer.rendering.models;


import lombok.ToString;
import no.stelar7.cdragon.types.dds.DDSParser;
import no.stelar7.cdragon.types.skn.SKNParser;
import no.stelar7.cdragon.types.skn.data.SKNFile;
import no.stelar7.cdragon.viewer.rendering.buffers.VAO;

import java.awt.image.BufferedImage;
import java.nio.file.*;

@ToString
public class Model implements AutoCloseable
{
    private static final int VERTEX_SIZE  = 3;
    private static final int TEXTURE_SIZE = 2;
    
    
    private VAO vao;
    
    private Mesh    mesh;
    private Texture texture;
    
    private Model()
    {
        vao = new VAO();
        vao.bind();
    }
    
    public Model(float[] vertices, int[] indecies)
    {
        this();
        
        mesh = new Mesh();
        mesh.setVertices(vertices);
        mesh.setIndecies(indecies);
    }
    
    public Model(Path base, String skn, String tex)
    {
        this();
        
        SKNParser     parser       = new SKNParser();
        SKNFile       data         = parser.parse(base.resolve(skn));
        BufferedImage textureImage = new DDSParser().parse(base.resolve(tex));
        
        vao.setPointer(0, VERTEX_SIZE);
        vao.setPointer(1, TEXTURE_SIZE);
        
        mesh = Mesh.loadSKN(data);
        
        texture = Texture.loadForSKN(data);
        texture.setData(textureImage);
    }
    
    
    public void bind()
    {
        vao.bind();
        mesh.bind();
        texture.bind();
    }
    
    public void unbind()
    {
        vao.unbind();
        mesh.unbind();
        texture.unbind();
    }
    
    public Mesh getMesh()
    {
        return mesh;
    }
    
    @Override
    public void close()
    {
        vao.close();
        mesh.close();
        texture.close();
    }
}
