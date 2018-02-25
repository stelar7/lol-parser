package viewer.rendering.models;


public class Model
{
    Mesh mesh;
    
    public Model(float[] vertices, int[] indecies)
    {
        mesh = new Mesh(vertices, indecies);
    }
    
    public void bind()
    {
        mesh.bind();
    }
    
    public void unbind()
    {
        mesh.unbind();
    }
}
