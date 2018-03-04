package no.stelar7.cdragon.util.types;

import lombok.Data;

@Data
public class Vector3b
{
    private byte x;
    private byte y;
    private byte z;
    
    @Override
    public String toString()
    {
        return String.format("{\"x\":%s, \"y\":%s, \"z\":%s}", x, y, z);
    }
}