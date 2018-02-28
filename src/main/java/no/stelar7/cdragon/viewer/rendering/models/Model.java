package no.stelar7.cdragon.viewer.rendering.models;


import no.stelar7.cdragon.types.dds.DDSParser;
import no.stelar7.cdragon.types.skn.SKNParser;
import no.stelar7.cdragon.types.skn.data.SKNFile;
import no.stelar7.cdragon.viewer.rendering.buffers.VAO;

import java.awt.image.BufferedImage;
import java.nio.file.*;

public class Model implements AutoCloseable
{
    private static final int VERTEX_SIZE  = 3;
    private static final int TEXTURE_SIZE = 2;
    
    
    private VAO vao;
    
    private Mesh    mesh;
    private Texture texture;
    
    public Model(float[] vertices, int[] indecies)
    {
        vao = new VAO();
        vao.bind();
        
        mesh = new Mesh();
        mesh.setVertices(vertices);
        mesh.setIndecies(indecies);
    }
    
    public Model(Path path)
    {
        SKNParser     parser       = new SKNParser();
        SKNFile       data         = parser.parse(path.resolve("Brand_frostfire.skn"));
        BufferedImage textureImage = new DDSParser().parse(path.resolve("brand_frostfire_TX_CM.dds"));
        
        vao = new VAO();
        vao.bind();
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
