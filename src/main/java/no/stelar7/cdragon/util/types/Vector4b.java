package no.stelar7.cdragon.util.types;

public class Vector4b
{
    private byte x;
    private byte y;
    private byte z;
    private byte w;
    
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
    
    public byte getW()
    {
        return w;
    }
    
    public void setW(byte w)
    {
        this.w = w;
    }
    
    @Override
    public String toString()
    {
        return String.format("{\"x\":%s, \"y\":%s, \"z\":%s, \"w\":%s}", x, y, z, w);
    }
}