package no.stelar7.cdragon.util.types;

public class Vector3b
{
    private byte x;
    private byte y;
    private byte z;
    
    public byte getX()
    {
        return x;
    }
    
    public void setX(byte x)
    {
        this.x = x;
    }
    
    public byte getY()
    {
        return y;
    }
    
    public void setY(byte y)
    {
        this.y = y;
    }
    
    public byte getZ()
    {
        return z;
    }
    
    public void setZ(byte z)
    {
        this.z = z;
    }
    
    @Override
    public String toString()
    {
        return String.format("{\"x\":%s, \"y\":%s, \"z\":%s}", x, y, z);
    }
}