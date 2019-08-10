package no.stelar7.cdragon.viewer.rendering.models;


import no.stelar7.cdragon.viewer.rendering.buffers.VAO;
import no.stelar7.cdragon.viewer.rendering.models.texture.Texture;


public class Model implements AutoCloseable
{
    private static final int VERTEX_SIZE  = 3;
    private static final int NORMAL_SIZE  = 3;
    private static final int TEXTURE_SIZE = 2;
    private static final int SKELETON_SIZE = 4;
    
    private VAO vao;
    
    private Mesh     mesh;
    private Texture  texture;
    private Skeleton skeleton;
    
    private Model()
    {
        vao = new VAO();
        vao.bind();
    }
    
    public Model(Mesh mesh)
    {
        this();
        this.mesh = mesh;
        
        enableVertex();
        enableNormals();
    }
    
    public Model(Mesh mesh, Texture texture)
    {
        this(mesh);
        this.texture = texture;
        enableTextures();
    }
    
    public Model(Mesh mesh, Texture texture, Skeleton skeleton)
    {
        this(mesh, texture);
        enableSkeleton();
    }
    
    public void enableVertex()
    {
        vao.bind();
        mesh.bindVBO();
        setPointer(0, VERTEX_SIZE);
    }
    
    public void enableNormals()
    {
        vao.bind();
        mesh.bindNBO();
        setPointer(1, NORMAL_SIZE);
    }
    
    public void enableTextures()
    {
        vao.bind();
        texture.bind();
        setPointer(2, TEXTURE_SIZE);
    }
    
    public void enableSkeleton()
    {
        vao.bind();
        mesh.bindBIbo();
        setPointer(3, SKELETON_SIZE);
        
        mesh.bindBWbo();
        setPointer(4, SKELETON_SIZE);
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
