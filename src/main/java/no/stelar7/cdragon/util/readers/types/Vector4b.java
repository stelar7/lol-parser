package no.stelar7.cdragon.util.readers.types;

import lombok.Data;

@Data
public class Vector4b
{
    private byte x;
    private byte y;
    private byte z;
    private byte w;
    
    @Override
    public String toString()
    {
        return String.format("{\"x\":%s, \"y\":%s, \"z\":%s, \"w\":%s}", x, y, z, w);
    }
}