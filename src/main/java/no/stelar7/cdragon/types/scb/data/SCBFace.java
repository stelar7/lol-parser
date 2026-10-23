package no.stelar7.cdragon.types.scb.data;
import org.joml.*;

public final class SCBFace
{
    private final Vector3i indecies;
    private final String   material;
    
    private final Vector3f U;
    private final Vector3f V;
    
    public SCBFace(Vector3i indecies, String material, Vector3f U, Vector3f V)
    {
        this.indecies = indecies;
        this.material = material;
        this.U = U;
        this.V = V;
    }
    
    public Vector3i getIndecies()
    {
        return indecies;
    }
    
    public String getMaterial()
    {
        return material;
    }
    
    public Vector3f getU()
    {
        return U;
    }
    
    public Vector3f getV()
    {
        return V;
    }
}
