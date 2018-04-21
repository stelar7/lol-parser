package no.stelar7.cdragon.types.scb.data;
import org.joml.*;

public class SCBFace
{
    private Vector3i indecies;
    private String   material;
    
    private Vector3f U;
    private Vector3f V;
    
    public Vector3i getIndecies()
    {
        return indecies;
    }
    
    public void setIndecies(Vector3i indecies)
    {
        this.indecies = indecies;
    }
    
    public String getMaterial()
    {
        return material;
    }
    
    public void setMaterial(String material)
    {
        this.material = material;
    }
    
    public Vector3f getU()
    {
        return U;
    }
    
    public void setU(Vector3f u)
    {
        U = u;
    }
    
    public Vector3f getV()
    {
        return V;
    }
    
    public void setV(Vector3f v)
    {
        V = v;
    }
}
