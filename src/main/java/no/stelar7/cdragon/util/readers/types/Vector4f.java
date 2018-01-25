package no.stelar7.cdragon.util.readers.types;

public class Vector4f extends org.joml.Vector4f
{
    @Override
    public String toString()
    {
        return String.format("{\"x\":%s, \"y\":%s, \"z\":%s, \"w\":%s}", x, y, z, w);
    }
}
