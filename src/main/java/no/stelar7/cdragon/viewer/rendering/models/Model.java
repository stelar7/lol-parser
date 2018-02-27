package no.stelar7.cdragon.viewer.rendering.models;


public class Model
{
    Mesh mesh;
    
    public Model(float[] vertices, int[] indecies)
    {
//        mesh = new Mesh(vertices, indecies);
        mesh = Mesh.loadSKN();
    }
    
    public void bind()
    {
        mesh.bind();
    }
    
    public void unbind()
    {
        mesh.unbind();
    }
    
    public Mesh getMesh()
    {
        return mesh;
    }
}
