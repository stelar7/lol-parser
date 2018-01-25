package no.stelar7.cdragon.util.readers.types;

import lombok.Data;

@Data
public class Vector3s
{
    private short x;
    private short y;
    private short z;
    
    @Override
    public String toString()
    {
        return String.format("{\"x\":%s, \"y\":%s, \"z\":%s}", x, y, z);
    }
}