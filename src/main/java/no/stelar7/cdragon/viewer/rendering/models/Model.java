package no.stelar7.cdragon.viewer.rendering.models;


import no.stelar7.cdragon.types.skn.data.SKNData;
import no.stelar7.cdragon.viewer.rendering.buffers.VAO;
import no.stelar7.cdragon.viewer.rendering.models.texture.Texture;

import java.util.List;


public class Model implements AutoCloseable
{
    private static final int VERTEX_SIZE  = 3;
    private static final int NORMAL_SIZE  = 3;
    private static final int TEXTURE_SIZE = 2;
    
    private VAO vao;
    
    private Mesh    mesh;
    private Texture texture;
    
    private Model()
    {
        vao = new VAO();
        vao.bind();
    }
    
    public Model(Mesh mesh)
    {
        this();
        this.mesh = mesh;
        
        mesh.bindVBO();
        setPointer(0, VERTEX_SIZE);
        
        mesh.bindNBO();
        setPointer(1, NORMAL_SIZE);
    }
    
    public Model(Mesh mesh, Texture texture)
    {
        this(mesh);
        
        this.texture = texture;
        texture.bind();
        setPointer(2, TEXTURE_SIZE);
    }
    
    public Model(Mesh mesh, Texture texture, List<SKNData> modelData)
    {
        this(mesh, texture);
    }
    
    public void setPointer(int index, int size)
    {
        vao.setPointer(index, size);
    }
    
    public void bind()
    {
        vao.bind();
        texture.bind();
        mesh.bindIBO();
    }
    
    public void unbind()
    {
        vao.unbind();
        texture.unbind();
        mesh.unbindIBO();
    }
    
    public Mesh getMesh()
    {
        return mesh;
    }
    
    public Texture getTexture()
    {
        return texture;
    }
    
    @Override
    public void close()
    {
        vao.close();
        mesh.close();
        texture.close();
    }
    
    public void setMesh(Mesh mesh)
    {
        this.bind();
        this.mesh = mesh;
        mesh.bindVBO();
        vao.setPointer(0, VERTEX_SIZE);
    }
}
