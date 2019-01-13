package no.stelar7.cdragon.util.types.math;

public class Vector3s
{
    private short x;
    private short y;
    private short z;
    
    public short getX()
    {
        return x;
    }
    
    public void setX(short x)
    {
        this.x = x;
    }
    
    public short getY()
    {
        return y;
    }
    
    public void setY(short y)
    {
        this.y = y;
    }
    
    public short getZ()
    {
        return z;
    }
    
    public void setZ(short z)
    {
        this.z = z;
    }
    
    @Override
    public String toString()
    {
        return String.format("{\"x\":%s, \"y\":%s, \"z\":%s}", x, y, z);
    }
}