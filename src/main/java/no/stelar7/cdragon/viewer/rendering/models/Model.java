package no.stelar7.cdragon.viewer.rendering.models;


import no.stelar7.cdragon.viewer.rendering.buffers.VAO;

public class Model implements AutoCloseable
{
    private static final int VERTEX_SIZE  = 3;
    private static final int TEXTURE_SIZE = 2;
    
    public static Model current;
    
    
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
        mesh.bindForVAO();
        vao.setPointer(0, VERTEX_SIZE);
    }
    
    public Model(Mesh mesh, Texture texture)
    {
        this(mesh);
        
        this.texture = texture;
        texture.bind();
        vao.setPointer(1, TEXTURE_SIZE);
    }
    
    public void bind()
    {
        vao.bind();
        mesh.bindForDraw();
    }
    
    public void unbind()
    {
        vao.unbind();
        mesh.unbindForDraw();
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
